package at.co.brandstetter.aircontrol.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RegisterCatalog {
    private final Map<Integer, RegisterConfiguration.Register> registersById;

    public RegisterCatalog(RegisterConfiguration registerConfiguration) {
        this.registersById = registerConfiguration.getRegister().stream()
                .collect(Collectors.toUnmodifiableMap(RegisterConfiguration.Register::getId, Function.identity()));
    }

    public Optional<RegisterConfiguration.Register> find(Integer registerId) {
        return Optional.ofNullable(registersById.get(registerId));
    }

    public boolean supportsDevice(Integer registerId, String deviceId) {
        return find(registerId)
                .map(register -> register.getDevices().contains(deviceId))
                .orElse(false);
    }
}
