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
import java.time.LocalDateTime;
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

    @Value("${modbus.updaterange}")
    private int updaterange;

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
            serial.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serial.setNumDataBits(8);
            serial.setParity(SerialPort.NO_PARITY);

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

    private void reconnect () {
        logger.info("Trying to reconnect to serial port {}", port);

        this.close();
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            logger.trace(String.valueOf(e));
        }
        this.open();
    }

    public Boolean write(int registerId, RegisterEntity registerEntity) {
        logger.info("Change register value");

        // Write new register value to device
        return writeSerial(registerId, registerEntity.getValue());
    }

    public Optional<RegisterEntity> read(int registerId) {

        // Aquire Ratelimit
        rateLimiter.acquire();
        boolean isFree = rateLimiter.tryAcquire(1, 3, TimeUnit.SECONDS);

        // 1. Try to find cached value
        Optional<RegisterEntity> registerValue = registerRepository.findById(registerId);

        // Run only on free slot
        if (isFree) {

            logger.debug("Serial free");

            // 2. Nothing found - Get value for register

            if (registerValue.isEmpty()) {
                writeSerial(registerId, "x");

                RetryPolicy<Optional> retryPolicy = new RetryPolicy<Optional>()
                        .withDelay(Duration.ofSeconds(retrytimeout))
                        .withMaxRetries(retrycount)
                        .handleResultIf(result -> {
                            logger.trace("Retry register read...");
                            return result.isEmpty();
                        });

                registerValue = Failsafe.with(retryPolicy).get(() -> registerRepository.findById(registerId));

                logger.info("Read register id " + registerId + " from serial with value " + registerValue );

            } else {

                logger.info("Read register id " + registerId + " from cache with value " + registerValue );

                // Check for last update of value
                LocalDateTime lastupdate = registerValue.get().getLastupdate();
                LocalDateTime now = LocalDateTime.now();
                Duration duration = Duration.between(now, lastupdate);
                long diff = Math.abs(duration.toMinutes());

                if (diff > updaterange) {
                    //logger.trace("Serial connection seems to be broken");
                    //this.reconnect();
                    logger.info("Renew value of register " + registerId);
                    writeSerial(registerId, "x");
                }
            }

        }

       //logger.trace(String.valueOf(registerValue));
        return registerValue;
    }

    public RegisterConfiguration.Register search(Integer registerId) {

        return registerConfiguration.getRegister().stream()
                .filter(register -> register.getId().equals(registerId))
                .findAny()
                .orElse(null);

    }

    private boolean writeSerial(int registerId, String registerValue) {

        try {
            /* Write for reading
               <D&W Device ID><Space><RegisterId+1> */
            String mValue = "130 " + (registerId + 1) + "\r\n";
            logger.debug("Write string: " + mValue);


            /* Write for writing
               <D&W Device ID><Space><RegisterId><Space><Value><CR><LF> */
            if (!registerValue.equals("x")) {
                logger.info("Writing register: " + registerId + " Value: " + registerValue);
                mValue = "130 " + registerId + " " + registerValue + "\r\n";
                // ToDo - Workaround/Bug?
                outs.write(mValue.getBytes(StandardCharsets.US_ASCII));
            }

            outs.writeBytes(mValue);
            //outs.write(mValue.getBytes(StandardCharsets.US_ASCII));
            outs.flush();

            logger.debug("Write to serial port successful");
            return true;

        } catch (IOException e) {
            logger.error("Error writing to serial port");
            this.reconnect();
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
        logger.trace("Event happen: " + event.getEventType());

        byte[] newData = event.getReceivedData();

        String s = new String(newData, StandardCharsets.UTF_8);
        logger.debug("Data available: " + s);

        StringTokenizer st = new StringTokenizer(s, " ");

        // data from serial
        int device = Integer.parseInt(st.nextToken());
        int registerId = Integer.parseInt(st.nextToken());
        String registerValue = st.nextToken();
        registerValue = registerValue.replaceAll("([\r\n])", "");

        // Correct register
        if (search(registerId) == null ) {
            registerId = (registerId -1);
        }

        // Update register
        RegisterEntity registerEntity = new RegisterEntity();
        registerEntity.setRegister(registerId);
        registerEntity.setValue(registerValue);
        registerEntity.setLastupdate(LocalDateTime.now());
        registerRepository.save(registerEntity);

        logger.info("Device: " +  device + " " + "Register: " + (registerId) + " " + "Value: " + registerValue);
    }
}
