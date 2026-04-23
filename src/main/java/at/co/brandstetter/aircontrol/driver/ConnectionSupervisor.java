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
        attemptOpen("startup");
    }

    @Scheduled(fixedDelayString = "${modbus.stale-check-delay:10s}")
    public void maintainConnection() {
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTED && isStale()) {
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
            return false;
        }

        try {
            client.writeRegister(registerId, value);
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

            lastDataAt = Instant.now();
            state.set(ConnectionState.CONNECTED);
            lastError = null;

            if (frame.register() == DEVICE_ID_REGISTER) {
                deviceId = frame.value();
            }
        });
    }

    @Override
    public void onDisconnect() {
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
            client.open(this);
            state.set(lastDataAt == null ? ConnectionState.STALE : ConnectionState.CONNECTED);
            lastError = null;

            if (!registerConfiguration.isDeviceregognition()) {
                deviceId = registerConfiguration.getDevicetype();
            } else {
                requestRead(DEVICE_ID_REGISTER);
            }
        } catch (IOException e) {
            reconnectAttempts++;
            lastError = e.getMessage();
            state.set(ConnectionState.DISCONNECTED);
            logger.debug("Unable to open Modbus connection during {}: {}", reason, e.getMessage());
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
        client.close();
    }

    @PreDestroy
    public void shutdown() {
        readExecutor.shutdownNow();
        client.close();
    }
}
