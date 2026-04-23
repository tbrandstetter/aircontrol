package at.co.brandstetter.aircontrol.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "modbus")
public class ModbusProperties {
    private String port = "/dev/ttyUSB0";
    private int baudrate = 115200;
    private int retrycount = 20;
    private int retrytimeout = 3;
    private int updaterange = 240;
    private Duration staleTimeout = Duration.ofMinutes(2);
    private Duration reconnectDelay = Duration.ofSeconds(5);
    private Duration staleCheckDelay = Duration.ofSeconds(10);
}
