package at.co.brandstetter.aircontrol.driver;

import at.co.brandstetter.aircontrol.config.RegisterCatalog;
import at.co.brandstetter.aircontrol.config.RegisterConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModbusFrameParserTest {

    @Test
    void parsesKnownRegister() {
        ModbusFrameParser parser = new ModbusFrameParser(catalogWith(1032));

        assertThat(parser.parse("130 1032 42\r\n"))
                .hasValue(new ModbusFrame(130, 1032, "42"));
    }

    @Test
    void correctsReadResponseRegisterOffset() {
        ModbusFrameParser parser = new ModbusFrameParser(catalogWith(1032));

        assertThat(parser.parse("130 1033 1\n"))
                .hasValue(new ModbusFrame(130, 1032, "1"));
    }

    @Test
    void ignoresMalformedOrUnknownFrames() {
        ModbusFrameParser parser = new ModbusFrameParser(catalogWith(1032));

        assertThat(parser.parse("bad-data")).isEmpty();
        assertThat(parser.parse("130 9999 1")).isEmpty();
    }

    private RegisterCatalog catalogWith(Integer... ids) {
        RegisterConfiguration configuration = new RegisterConfiguration();
        configuration.setRegister(List.of(ids).stream()
                .map(this::register)
                .toList());
        return new RegisterCatalog(configuration);
    }

    private RegisterConfiguration.Register register(int id) {
        RegisterConfiguration.Register register = new RegisterConfiguration.Register();
        register.setId(id);
        register.setDescription("Register " + id);
        register.setDevices(List.of("17"));
        return register;
    }
}
