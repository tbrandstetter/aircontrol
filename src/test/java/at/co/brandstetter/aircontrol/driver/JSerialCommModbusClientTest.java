package at.co.brandstetter.aircontrol.driver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JSerialCommModbusClientTest {

    @Test
    void buildsReadCommandWithRegisterPlusOneAndCrLf() {
        assertThat(JSerialCommModbusClient.buildReadCommand(1032))
                .isEqualTo("130 1033\r\n");
    }

    @Test
    void buildsWriteCommandWithExactRegisterAndCrLf() {
        assertThat(JSerialCommModbusClient.buildWriteCommand(5002, "3"))
                .isEqualTo("130 5002 3\r\n");
    }

    @Test
    void stripsEmbeddedLineBreaksFromWriteValues() {
        assertThat(JSerialCommModbusClient.buildWriteCommand(5002, " 3\r\n"))
                .isEqualTo("130 5002 3\r\n");
    }
}
