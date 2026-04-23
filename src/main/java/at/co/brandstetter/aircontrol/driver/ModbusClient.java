package at.co.brandstetter.aircontrol.driver;

import java.io.IOException;

public interface ModbusClient {
    void open(ModbusDataListener listener) throws IOException;

    void close();

    boolean isOpen();

    void requestRegister(int registerId) throws IOException;

    void writeRegister(int registerId, String value) throws IOException;
}
