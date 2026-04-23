package at.co.brandstetter.aircontrol.driver;

import at.co.brandstetter.aircontrol.config.ModbusProperties;
import at.co.brandstetter.aircontrol.config.RegisterConfiguration;
import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.model.RegisterRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ConnectionSupervisor implements ModbusDataListener {
    public static final int DEVICE_ID_REGISTER = 5000;

    private static final Logger logger = LoggerFactory.getLogger(ConnectionSupervisor.class);

    private final ModbusClient client;
    private final ModbusFrameParser frameParser;
    private final RegisterRepository registerRepository;
    private final RegisterConfiguration registerConfiguration;
    private final ModbusProperties properties;
    private final AtomicReference<ConnectionState> state = new AtomicReference<>(ConnectionState.DISCONNECTED);
    private final AtomicBoolean opening = new AtomicBoolean(false);
    private final Set<Integer> pendingReads = ConcurrentHashMap.newKeySet();
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor();

    private volatile Instant lastDataAt;
    private volatile int reconnectAttempts;
    private volatile String lastError;
    private volatile String deviceId;

    public ConnectionSupervisor(
            ModbusClient client,
            ModbusFrameParser frameParser,
            RegisterRepository registerRepository,
            RegisterConfiguration registerConfiguration,
            ModbusProperties properties
    ) {
        this.client = client;
        this.frameParser = frameParser;
        this.registerRepository = registerRepository;
        this.registerConfiguration = registerConfiguration;
        this.properties = properties;
        if (!registerConfiguration.isDeviceregognition()) {
            this.deviceId = registerConfiguration.getDevicetype();
        }
    }

    public void start() {
        logger.info("Starting Modbus connection supervisor");
        attemptOpen("startup");
    }

    @Scheduled(fixedDelayString = "${modbus.stale-check-delay:10s}")
    public void maintainConnection() {
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTED && isStale()) {
            logger.warn("No fresh Modbus data for {}, marking connection stale", properties.getStaleTimeout());
            state.set(ConnectionState.STALE);
            currentState = ConnectionState.STALE;
        }

        if (currentState == ConnectionState.STARTING
                || currentState == ConnectionState.STALE
                || currentState == ConnectionState.RECONNECTING
                || currentState == ConnectionState.DISCONNECTED) {
            attemptOpen("connection maintenance");
        }
    }

    public void requestRead(int registerId) {
        if (!pendingReads.add(registerId)) {
            return;
        }

        logger.debug("Queueing background refresh for register {}", registerId);

        readExecutor.submit(() -> {
            try {
                if (ensureOpenForRequest()) {
                    client.requestRegister(registerId);
                }
            } catch (IOException e) {
                markFailure(e);
            } finally {
                pendingReads.remove(registerId);
            }
        });
    }

    public boolean writeRegister(int registerId, String value) {
        if (state.get() != ConnectionState.CONNECTED) {
            logger.warn("Rejecting write for register {} because connection state is {}", registerId, state.get());
            return false;
        }

        try {
            client.writeRegister(registerId, value);
            logger.info("Write command queued for register {}", registerId);
            return true;
        } catch (IOException e) {
            markFailure(e);
            return false;
        }
    }

    @Override
    public void onData(String line) {
        frameParser.parse(line).ifPresent(frame -> {
            RegisterEntity entity = new RegisterEntity();
            entity.setRegister(frame.register());
            entity.setValue(frame.value());
            entity.setLastupdate(LocalDateTime.now());
            registerRepository.save(entity);

            if (state.get() != ConnectionState.CONNECTED) {
                logger.info("Modbus connection established");
            }
            lastDataAt = Instant.now();
            state.set(ConnectionState.CONNECTED);
            lastError = null;
            logger.debug("Updated register {} with value {}", frame.register(), frame.value());

            if (frame.register() == DEVICE_ID_REGISTER) {
                deviceId = frame.value();
                logger.info("Detected device id {}", deviceId);
            }
        });
    }

    @Override
    public void onDisconnect() {
        logger.warn("Modbus connection lost");
        state.set(ConnectionState.DISCONNECTED);
        lastError = "Serial port disconnected";
        client.close();
    }

    public ConnectionSnapshot snapshot() {
        return new ConnectionSnapshot(
                state.get(),
                properties.getPort(),
                lastDataAt,
                properties.getStaleTimeout(),
                reconnectAttempts,
                lastError
        );
    }

    public String currentDeviceId() {
        if (!registerConfiguration.isDeviceregognition()) {
            return registerConfiguration.getDevicetype();
        }
        return deviceId == null ? "0" : deviceId;
    }

    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    private void attemptOpen(String reason) {
        if (!opening.compareAndSet(false, true)) {
            return;
        }

        try {
            ConnectionState openingState = reconnectAttempts == 0 ? ConnectionState.STARTING : ConnectionState.RECONNECTING;
            state.set(openingState);
            logger.info("Opening Modbus connection on {} ({})", properties.getPort(), reason);
            client.open(this);
            state.set(lastDataAt == null ? ConnectionState.STALE : ConnectionState.CONNECTED);
            lastError = null;
            reconnectAttempts = 0;
            logger.info("Modbus port {} is open", properties.getPort());

            if (!registerConfiguration.isDeviceregognition()) {
                deviceId = registerConfiguration.getDevicetype();
            } else {
                requestRead(DEVICE_ID_REGISTER);
            }
        } catch (IOException e) {
            reconnectAttempts++;
            lastError = e.getMessage();
            state.set(ConnectionState.DISCONNECTED);
            logger.warn("Unable to open Modbus connection during {}: {}", reason, e.getMessage());
        } finally {
            opening.set(false);
        }
    }

    private boolean ensureOpenForRequest() {
        if (client.isOpen()) {
            return true;
        }
        attemptOpen("request");
        return client.isOpen();
    }

    private boolean isStale() {
        Instant lastSeen = lastDataAt;
        return lastSeen == null || Duration.between(lastSeen, Instant.now()).compareTo(properties.getStaleTimeout()) > 0;
    }

    private void markFailure(IOException e) {
        reconnectAttempts++;
        lastError = e.getMessage();
        state.set(ConnectionState.DISCONNECTED);
        logger.warn("Modbus I/O failure: {}", e.getMessage());
        client.close();
    }

    @PreDestroy
    public void shutdown() {
        readExecutor.shutdownNow();
        client.close();
    }
}
