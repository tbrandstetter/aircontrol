package at.co.brandstetter.aircontrolservice.driver;

import at.co.brandstetter.aircontrolservice.config.RegisterConfiguration;
import at.co.brandstetter.aircontrolservice.model.RegisterEntity;
import at.co.brandstetter.aircontrolservice.model.RegisterRepository;
import ch.qos.logback.classic.Logger;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import com.google.common.util.concurrent.RateLimiter;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

public class Modbus implements SerialPortMessageListener {

    private final RegisterRepository registerRepository;
    private final RegisterConfiguration registerConfiguration;

    @Value("${modbus.port}")
    private String port;

    @Value("${modbus.baudrate}")
    private int baudrate;

    @Value("${modbus.retrycount}")
    private int retrycount;

    @Value("${modbus.retrytimeout}")
    private int retrytimeout;

    private boolean tryOpen = false;
    private boolean tryClose = false;

    private SerialPort serial;
    private DataOutputStream outs;

    RateLimiter rateLimiter = RateLimiter.create(1);

    Logger logger = (Logger) LoggerFactory.getLogger(Modbus.class);

    public Modbus(RegisterRepository registerRepository, RegisterConfiguration registerConfiguration) {
        this.registerRepository = registerRepository;
        this.registerConfiguration = registerConfiguration;
    }

    public void open() {

        if (!tryOpen) {

            tryOpen = true;

            serial = SerialPort.getCommPort(port);
            serial.setBaudRate(baudrate);
            serial.setNumStopBits(1);
            serial.setNumDataBits(8);

            if (!serial.openPort()) {
                logger.error("Unable to connect to serial port {}", port);
            } else {
                serial.addDataListener(this);
                outs = new DataOutputStream(serial.getOutputStream());
                logger.info("Connected to serial port {}", port);
            }
            tryOpen = false;
        }

    }

    public void close() {

        if (!tryClose) {

            tryClose = true;

            if (!serial.closePort()) {
                logger.error("Unable to disconnect serial port {}", port);
            } else {
                serial.removeDataListener();
                logger.info("Disconnected port {}", port);
            }
            tryClose = false;
        }
    }

    public Boolean write(int registerId, RegisterEntity registerEntity) {
        logger.info("Change register value");

        // Write new register value to device
        boolean writeStatus = writeSerial(registerId, registerEntity.getValue());

        return writeStatus;
    }

    public Optional<RegisterEntity> read(int registerId) {

        // 1. Try to find cached value
        Optional<RegisterEntity> registerValue = registerRepository.findById(registerId);

        logger.info("Read register " + registerId );

        // 2. Nothing found - Get value for register
        if (registerValue.isEmpty()) {
            writeSerial(registerId, "x");
        }

        // 3. Re-Read value
        RetryPolicy<Optional> retryPolicy = new RetryPolicy<Optional>()
                .withDelay(Duration.ofSeconds(retrytimeout))
                .withMaxRetries(retrycount)
                .handleResultIf(result -> {
                    logger.trace("Retry register read...");
                    return !result.isPresent();
                });

        registerValue = Failsafe.with(retryPolicy).get(() -> registerRepository.findById(registerId));

        logger.trace(String.valueOf(registerValue));
        return registerValue;


    }

    public RegisterConfiguration.Register search(Integer registerId) {

        RegisterConfiguration.Register registerEntry = registerConfiguration.getRegister().stream()
                .filter(register -> register.getId().equals(registerId))
                .findAny()
                .orElse(null);

        return registerEntry;

    }

    private boolean writeSerial(int registerId, String registerValue) {

        // Aquire Ratelimit
        rateLimiter.acquire();
        boolean isFree = rateLimiter.tryAcquire(1, 3, TimeUnit.SECONDS);

        logger.debug("Serial free: " + isFree);

        if (serial.isOpen() && isFree) {
            try {
                /* Write for reading
                   <D&W Device ID><Space><RegisterId+1> */
                String mValue = "130 " + (registerId + 1) + "\r\n";

                /* Write for writing
                   <D&W Device ID><Space><RegisterId><Space><Value><CR><LF> */
                if (registerValue != "x") {
                    logger.info("Writing register: " + registerId + " Value: " + registerValue);
                    mValue = "130 " + registerId + " " + registerValue + "\r\n";
                    // ToDo - Workaround/Bug?
                    outs.write(mValue.getBytes(StandardCharsets.US_ASCII));
                }

                outs.write(mValue.getBytes(StandardCharsets.US_ASCII));
                outs.flush();

                logger.debug("Write to serial port successful");
                return true;

            } catch (IOException e) {
                logger.error("Error writing to serial port");
                try {
                    this.close();
                    Thread.sleep(200);
                    this.open();
                } catch (Exception ex) {
                    logger.trace(String.valueOf(ex));
                }

            }
        }

        return false;

    }

    @Override
    public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }

    @Override
    public byte[] getMessageDelimiter() { return new byte[] { (byte)0x0A }; }

    @Override
    public boolean delimiterIndicatesEndOfMessage() { return true; }

    @Override
    public void serialEvent(SerialPortEvent event) {
        logger.trace("Event happen " + event.getEventType());

        if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {

            logger.debug("Data available");

            byte[] newData = event.getReceivedData();
            String s = new String(newData, StandardCharsets.UTF_8);
            StringTokenizer st = new StringTokenizer(s, " ");

            // data from serial
            int device = Integer.parseInt(st.nextToken());
            int registerId = Integer.parseInt(st.nextToken());
            String registerValue = st.nextToken();
            registerValue = registerValue.replaceAll("(\\r|\\n)", "");

            // Correct register
            if (search(registerId) == null ) {
                registerId = (registerId -1);
            };

            // Update register
            RegisterEntity registerEntity = new RegisterEntity();
            registerEntity.setRegister(registerId);
            registerEntity.setValue(registerValue);
            registerRepository.save(registerEntity);

            logger.info("Device: " +  device + " " + "Register: " + (registerId) + " " + "Value: " + registerValue);
        }
    }

}
