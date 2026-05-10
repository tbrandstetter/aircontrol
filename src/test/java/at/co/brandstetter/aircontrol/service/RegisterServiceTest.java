package at.co.brandstetter.aircontrol.service;

import at.co.brandstetter.aircontrol.config.ModbusProperties;
import at.co.brandstetter.aircontrol.config.RegisterCatalog;
import at.co.brandstetter.aircontrol.config.RegisterConfiguration;
import at.co.brandstetter.aircontrol.driver.ConnectionSnapshot;
import at.co.brandstetter.aircontrol.driver.ConnectionState;
import at.co.brandstetter.aircontrol.driver.ConnectionSupervisor;
import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.model.RegisterRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterServiceTest {

    @Test
    void readRegisterReturnsCachedValueBeforeDeviceRecognitionCompletes() {
        RegisterRepository repository = mock(RegisterRepository.class);
        ConnectionSupervisor supervisor = mock(ConnectionSupervisor.class);
        ModbusProperties properties = new ModbusProperties();
        RegisterService service = new RegisterService(
                repository,
                catalogWith(register(202, "Aussenluft", "17")),
                supervisor,
                properties
        );

        RegisterEntity cached = cachedRegister(202, "16533", LocalDateTime.now());
        when(repository.findById(202)).thenReturn(Optional.of(cached));
        when(supervisor.currentDeviceId()).thenReturn("0");

        assertThat(service.readRegister(202))
                .hasValueSatisfying(register -> {
                    assertThat(register.getRegister()).isEqualTo(202);
                    assertThat(register.getValue()).isEqualTo("16533");
                    assertThat(register.getDescription()).isEqualTo("Aussenluft");
                });

        verify(supervisor, never()).requestRead(202);
    }

    @Test
    void readRegisterStatusReturnsCachedValueBeforeDeviceRecognitionCompletes() {
        RegisterRepository repository = mock(RegisterRepository.class);
        ConnectionSupervisor supervisor = mock(ConnectionSupervisor.class);
        ModbusProperties properties = new ModbusProperties();
        RegisterService service = new RegisterService(
                repository,
                catalogWith(register(202, "Aussenluft", "17")),
                supervisor,
                properties
        );

        LocalDateTime updatedAt = LocalDateTime.now().minusSeconds(30);
        RegisterEntity cached = cachedRegister(202, "16533", updatedAt);
        when(repository.findById(202)).thenReturn(Optional.of(cached));
        when(supervisor.currentDeviceId()).thenReturn("0");
        when(supervisor.snapshot()).thenReturn(new ConnectionSnapshot(
                ConnectionState.CONNECTED,
                "/tmp/ttyUSB0",
                Instant.now(),
                properties.getStaleTimeout(),
                0,
                null
        ));

        assertThat(service.readRegisterStatus(202))
                .hasValueSatisfying(status -> {
                    assertThat(status.register()).isEqualTo(202);
                    assertThat(status.value()).isEqualTo("16533");
                    assertThat(status.description()).isEqualTo("Aussenluft");
                    assertThat(status.stale()).isFalse();
                });

        verify(supervisor, never()).requestRead(202);
    }

    @Test
    void readRegisterRequestsRefreshForStaleCachedValueWhenDeviceIsKnown() {
        RegisterRepository repository = mock(RegisterRepository.class);
        ConnectionSupervisor supervisor = mock(ConnectionSupervisor.class);
        ModbusProperties properties = new ModbusProperties();
        RegisterService service = new RegisterService(
                repository,
                catalogWith(register(202, "Aussenluft", "17")),
                supervisor,
                properties
        );

        RegisterEntity cached = cachedRegister(202, "16533", LocalDateTime.now().minusMinutes(3));
        when(repository.findById(202)).thenReturn(Optional.of(cached));
        when(supervisor.currentDeviceId()).thenReturn("17");

        assertThat(service.readRegister(202)).isPresent();

        verify(supervisor).requestRead(202);
    }

    private RegisterCatalog catalogWith(RegisterConfiguration.Register... registers) {
        RegisterConfiguration configuration = new RegisterConfiguration();
        configuration.setRegister(List.of(registers));
        return new RegisterCatalog(configuration);
    }

    private RegisterConfiguration.Register register(int id, String description, String deviceId) {
        RegisterConfiguration.Register register = new RegisterConfiguration.Register();
        register.setId(id);
        register.setDescription(description);
        register.setMin(-28000);
        register.setMax(100000);
        register.setDivisor(100);
        register.setDevices(List.of(deviceId));
        return register;
    }

    private RegisterEntity cachedRegister(int id, String value, LocalDateTime updatedAt) {
        RegisterEntity register = new RegisterEntity();
        register.setRegister(id);
        register.setValue(value);
        register.setLastupdate(updatedAt);
        return register;
    }
}
