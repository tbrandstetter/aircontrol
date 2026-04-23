package at.co.brandstetter.aircontrol.driver;

import at.co.brandstetter.aircontrol.config.ModbusProperties;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JSerialCommModbusClient implements ModbusClient, SerialPortMessageListener {
    private final ModbusProperties properties;
    private SerialPort serial;
    private DataOutputStream outs;
    private ModbusDataListener listener;

    public JSerialCommModbusClient(ModbusProperties properties) {
        this.properties = properties;
    }

    @Override
    public synchronized void open(ModbusDataListener listener) throws IOException {
        close();
        this.listener = listener;

        try {
            serial = SerialPort.getCommPort(properties.getPort());
            serial.setBaudRate(properties.getBaudrate());
            serial.setNumStopBits(SerialPort.ONE_STOP_BIT);
            serial.setNumDataBits(8);
            serial.setParity(SerialPort.NO_PARITY);

            if (!serial.openPort()) {
                throw new IOException("Unable to connect to serial port " + properties.getPort());
            }

            serial.addDataListener(this);
            outs = new DataOutputStream(serial.getOutputStream());
        } catch (RuntimeException e) {
            close();
            throw new IOException("Unable to create serial port " + properties.getPort(), e);
        }
    }

    @Override
    @PreDestroy
    public synchronized void close() {
        if (serial != null) {
            try {
                serial.removeDataListener();
                serial.closePort();
            } finally {
                serial = null;
                outs = null;
            }
        }
    }

    @Override
    public synchronized boolean isOpen() {
        return serial != null && serial.isOpen();
    }

    @Override
    public void requestRegister(int registerId) throws IOException {
        writeLine("130 " + (registerId + 1) + "\r\n");
    }

    @Override
    public void writeRegister(int registerId, String value) throws IOException {
        writeLine("130 " + registerId + " " + value + "\r\n");
    }

    private synchronized void writeLine(String line) throws IOException {
        if (outs == null) {
            throw new IOException("Serial port is not open");
        }
        outs.write(line.getBytes(StandardCharsets.US_ASCII));
        outs.flush();
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
    }

    @Override
    public byte[] getMessageDelimiter() {
        return new byte[] { (byte) 0x0A };
    }

    @Override
    public boolean delimiterIndicatesEndOfMessage() {
        return true;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if ((event.getEventType() & SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) != 0) {
            if (listener != null) {
                listener.onDisconnect();
            }
            return;
        }

        if ((event.getEventType() & SerialPort.LISTENING_EVENT_DATA_RECEIVED) != 0 && listener != null) {
            listener.onData(new String(event.getReceivedData(), StandardCharsets.UTF_8));
        }
    }
}
