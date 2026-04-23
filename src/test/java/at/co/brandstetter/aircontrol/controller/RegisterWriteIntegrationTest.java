package at.co.brandstetter.aircontrol.controller;

import at.co.brandstetter.aircontrol.driver.ConnectionSnapshot;
import at.co.brandstetter.aircontrol.driver.ConnectionState;
import at.co.brandstetter.aircontrol.driver.ConnectionSupervisor;
import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.model.RegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "duw.deviceregognition=false",
        "duw.devicetype=17"
})
class RegisterWriteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisterRepository registerRepository;

    @MockitoBean
    private ConnectionSupervisor connectionSupervisor;

    @BeforeEach
    void setUp() {
        registerRepository.deleteAll();

        when(connectionSupervisor.currentDeviceId()).thenReturn("17");
        when(connectionSupervisor.isConnected()).thenReturn(true);
        when(connectionSupervisor.writeRegister(5002, "3")).thenReturn(true);
        when(connectionSupervisor.snapshot()).thenReturn(new ConnectionSnapshot(
                ConnectionState.CONNECTED,
                "/dev/ttyUSB0",
                Instant.now(),
                Duration.ofMinutes(2),
                0,
                null
        ));
    }

    @Test
    void putRegister5002UpdatesCacheAndReturnsTrue() throws Exception {
        mockMvc.perform(put("/api/v1/registers/5002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"3\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        Optional<RegisterEntity> cached = registerRepository.findById(5002);
        assertThat(cached).isPresent();
        assertThat(cached.get().getValue()).isEqualTo("3");
        assertThat(cached.get().getLastupdate()).isNotNull();
        verify(connectionSupervisor).requestRead(5002);
    }
}
