package at.co.brandstetter.aircontrol.controller;

import at.co.brandstetter.aircontrol.driver.ConnectionSnapshot;
import at.co.brandstetter.aircontrol.driver.ConnectionState;
import at.co.brandstetter.aircontrol.driver.ConnectionSupervisor;
import at.co.brandstetter.aircontrol.service.RegisterServiceInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(APIController.class)
@Import(at.co.brandstetter.aircontrol.config.WebSecurityConfiguration.class)
class APIControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RegisterServiceInterface registerServiceInterface;

    @MockBean
    private ConnectionSupervisor connectionSupervisor;

    @Test
    void putApiWriteDoesNotRequireCsrfToken() throws Exception {
        when(registerServiceInterface.writeRegister(eq(5002), any()))
                .thenReturn(new at.co.brandstetter.aircontrol.controller.dto.RegisterWriteResponse(
                        5002,
                        true,
                        ConnectionState.CONNECTED,
                        "Write sent"
                ));
        when(connectionSupervisor.snapshot())
                .thenReturn(new ConnectionSnapshot(
                        ConnectionState.CONNECTED,
                        "/dev/ttyUSB0",
                        Instant.now(),
                        Duration.ofMinutes(2),
                        0,
                        null
                ));

        mockMvc.perform(put("/api/v1/registers/5002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"3\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
