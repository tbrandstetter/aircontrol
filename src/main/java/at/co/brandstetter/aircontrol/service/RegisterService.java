package at.co.brandstetter.aircontrol.service;

import at.co.brandstetter.aircontrol.config.ModbusProperties;
import at.co.brandstetter.aircontrol.config.RegisterCatalog;
import at.co.brandstetter.aircontrol.controller.dto.RegisterValueResponse;
import at.co.brandstetter.aircontrol.controller.dto.RegisterWriteResponse;
import at.co.brandstetter.aircontrol.driver.ConnectionSnapshot;
import at.co.brandstetter.aircontrol.driver.ConnectionSupervisor;
import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.model.RegisterRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class RegisterService implements RegisterServiceInterface {

    private final RegisterRepository registerRepository;
    private final RegisterCatalog registerCatalog;
    private final ConnectionSupervisor connectionSupervisor;
    private final ModbusProperties modbusProperties;

    public RegisterService(
            RegisterRepository registerRepository,
            RegisterCatalog registerCatalog,
            ConnectionSupervisor connectionSupervisor,
            ModbusProperties modbusProperties
    ) {
        this.registerRepository = registerRepository;
        this.registerCatalog = registerCatalog;
        this.connectionSupervisor = connectionSupervisor;
        this.modbusProperties = modbusProperties;
    }

    @Override
    public RegisterWriteResponse writeRegister(int registerId, RegisterEntity registerEntity) {
        ConnectionSnapshot snapshot = connectionSupervisor.snapshot();
        if (!isSupported(registerId)) {
            return new RegisterWriteResponse(registerId, false, snapshot.state(), "Register is not supported by this device");
        }

        if (!connectionSupervisor.isConnected()) {
            return new RegisterWriteResponse(registerId, false, snapshot.state(), "Connection is not ready for writes");
        }

        boolean accepted = connectionSupervisor.writeRegister(registerId, registerEntity.getValue());
        return new RegisterWriteResponse(
                registerId,
                accepted,
                connectionSupervisor.snapshot().state(),
                accepted ? "Write sent" : "Write failed"
        );
    }

    @Override
    public Optional<RegisterEntity> readRegister(int registerId) {
        if (!isSupported(registerId)) {
            return Optional.empty();
        }

        Optional<RegisterEntity> cached = registerRepository.findById(registerId).map(this::withMetadata);
        if (cached.isEmpty() || isStale(cached.get())) {
            connectionSupervisor.requestRead(registerId);
        }
        return cached;
    }

    @Override
    public Optional<RegisterValueResponse> readRegisterStatus(int registerId) {
        if (!isSupported(registerId)) {
            return Optional.empty();
        }

        ConnectionSnapshot snapshot = connectionSupervisor.snapshot();
        RegisterEntity register = registerRepository.findById(registerId)
                .map(this::withMetadata)
                .orElseGet(() -> withMetadata(registerId));

        if (isStale(register)) {
            connectionSupervisor.requestRead(registerId);
        }

        Duration age = ageOf(register.getLastupdate());
        return Optional.of(new RegisterValueResponse(
                register.getRegister(),
                register.getValue(),
                register.getDescription(),
                register.getMin(),
                register.getMax(),
                register.getDivisor(),
                register.getLastupdate(),
                isStale(register),
                age,
                snapshot.state()
        ));
    }

    @Override
    public List<RegisterEntity> listRegisters() {
        return StreamSupport.stream(registerRepository.findAll().spliterator(), false)
                .map(this::withMetadata)
                .toList();
    }

    private boolean isSupported(int id) {
        return registerCatalog.supportsDevice(id, connectionSupervisor.currentDeviceId());
    }

    private RegisterEntity withMetadata(RegisterEntity register) {
        RegisterEntity enriched = new RegisterEntity();
        enriched.setRegister(register.getRegister());
        enriched.setValue(register.getValue());
        enriched.setLastupdate(register.getLastupdate());

        registerCatalog.find(register.getRegister()).ifPresent(registerEntry -> {
            enriched.setDescription(registerEntry.getDescription());
            enriched.setMin(registerEntry.getMin());
            enriched.setMax(registerEntry.getMax());
            enriched.setDivisor(registerEntry.getDivisor());
        });

        return enriched;
    }

    private RegisterEntity withMetadata(int registerId) {
        RegisterEntity register = new RegisterEntity();
        register.setRegister(registerId);
        return withMetadata(register);
    }

    private boolean isStale(RegisterEntity register) {
        return register.getLastupdate() == null
                || ageOf(register.getLastupdate()).compareTo(modbusProperties.getStaleTimeout()) > 0;
    }

    private Duration ageOf(LocalDateTime lastUpdate) {
        if (lastUpdate == null) {
            return modbusProperties.getStaleTimeout().plusSeconds(1);
        }
        return Duration.between(lastUpdate, LocalDateTime.now());
    }
}
