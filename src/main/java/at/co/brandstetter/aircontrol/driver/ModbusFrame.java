package at.co.brandstetter.aircontrol.driver;

public record ModbusFrame(
        int device,
        int register,
        String value
) {
}
