package at.co.brandstetter.aircontrol.driver;

import at.co.brandstetter.aircontrol.config.RegisterCatalog;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ModbusFrameParser {
    private final RegisterCatalog registerCatalog;

    public ModbusFrameParser(RegisterCatalog registerCatalog) {
        this.registerCatalog = registerCatalog;
    }

    public Optional<ModbusFrame> parse(String line) {
        if (line == null || line.isBlank()) {
            return Optional.empty();
        }

        String[] parts = line.strip().split("\\s+", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }

        try {
            int device = Integer.parseInt(parts[0]);
            int register = normalizeRegister(Integer.parseInt(parts[1]));
            if (registerCatalog.find(register).isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new ModbusFrame(device, register, parts[2].strip()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private int normalizeRegister(int register) {
        if (registerCatalog.find(register).isEmpty() && registerCatalog.find(register - 1).isPresent()) {
            return register - 1;
        }
        return register;
    }
}
