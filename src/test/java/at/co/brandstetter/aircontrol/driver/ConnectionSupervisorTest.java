package at.co.brandstetter.aircontrol.driver;

import at.co.brandstetter.aircontrol.config.ModbusProperties;
import at.co.brandstetter.aircontrol.config.RegisterCatalog;
import at.co.brandstetter.aircontrol.config.RegisterConfiguration;
import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.model.RegisterRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionSupervisorTest {

    @Test
    void startupFailureDoesNotEscape() {
        FakeModbusClient client = new FakeModbusClient();
        client.openFailure = new IOException("missing serial port");
        ConnectionSupervisor supervisor = supervisor(client, mock(RegisterRepository.class));

        supervisor.start();

        assertThat(supervisor.snapshot().state()).isEqualTo(ConnectionState.DISCONNECTED);
        assertThat(supervisor.snapshot().lastError()).isEqualTo("missing serial port");
    }

    @Test
    void dataFramesUpdateCacheAndConnectionState() {
        RegisterRepository repository = mock(RegisterRepository.class);
        ConnectionSupervisor supervisor = supervisor(new FakeModbusClient(), repository);

        supervisor.onData("130 1033 42\r\n");

        ArgumentCaptor<RegisterEntity> saved = ArgumentCaptor.forClass(RegisterEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRegister()).isEqualTo(1032);
        assertThat(saved.getValue().getValue()).isEqualTo("42");
        assertThat(supervisor.snapshot().state()).isEqualTo(ConnectionState.CONNECTED);
        assertThat(supervisor.snapshot().lastDataAt()).isNotNull();
    }

    @Test
    void writesAreRejectedUntilFreshDataMarksConnectionConnected() {
        FakeModbusClient client = new FakeModbusClient();
        ConnectionSupervisor supervisor = supervisor(client, mock(RegisterRepository.class));

        assertThat(supervisor.writeRegister(1032, "1")).isFalse();
        assertThat(client.writes).isEmpty();

        supervisor.onData("130 1033 42\r\n");

        assertThat(supervisor.writeRegister(1032, "1")).isTrue();
        assertThat(client.writes).containsExactly("1032=1");
    }

    @Test
    void maintainConnectionRetriesDeviceDetectionWhileConnected() throws IOException {
        ModbusClient client = mock(ModbusClient.class);
        when(client.isOpen()).thenReturn(true);

        ConnectionSupervisor supervisor = supervisor(client, mock(RegisterRepository.class), true);

        supervisor.onData("130 1033 42\r\n");
        supervisor.maintainConnection();

        verify(client, timeout(1000)).requestRegister(ConnectionSupervisor.DEVICE_ID_REGISTER);
    }

    private ConnectionSupervisor supervisor(FakeModbusClient client, RegisterRepository repository) {
        return supervisor(client, repository, false);
    }

    private ConnectionSupervisor supervisor(ModbusClient client, RegisterRepository repository, boolean deviceRecognition) {
        RegisterConfiguration configuration = new RegisterConfiguration();
        configuration.setDeviceregognition(deviceRecognition);
        configuration.setDevicetype("17");
        configuration.setRegister(List.of(register(1032), register(ConnectionSupervisor.DEVICE_ID_REGISTER)));

        RegisterCatalog catalog = new RegisterCatalog(configuration);
        ModbusProperties properties = new ModbusProperties();
        properties.setStaleTimeout(Duration.ofMinutes(2));
        properties.setStaleCheckDelay(Duration.ofSeconds(10));

        return new ConnectionSupervisor(
                client,
                new ModbusFrameParser(catalog),
                repository,
                configuration,
                properties
        );
    }

    private RegisterConfiguration.Register register(int id) {
        RegisterConfiguration.Register register = new RegisterConfiguration.Register();
        register.setId(id);
        register.setDescription("Register " + id);
        register.setDevices(List.of("17"));
        return register;
    }

    private static class FakeModbusClient implements ModbusClient {
        private final List<String> writes = new ArrayList<>();
        private IOException openFailure;
        private boolean open;

        @Override
        public void open(ModbusDataListener listener) throws IOException {
            if (openFailure != null) {
                throw openFailure;
            }
            open = true;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void requestRegister(int registerId) {
        }

        @Override
        public void writeRegister(int registerId, String value) {
            writes.add(registerId + "=" + value);
        }
    }
}
