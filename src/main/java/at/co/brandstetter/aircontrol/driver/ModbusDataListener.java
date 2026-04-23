package at.co.brandstetter.aircontrol.driver;

public interface ModbusDataListener {
    void onData(String line);

    void onDisconnect();
}
