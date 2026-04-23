package at.co.brandstetter.aircontrol.controller;

import at.co.brandstetter.aircontrol.controller.dto.RegisterValueResponse;
import at.co.brandstetter.aircontrol.driver.ConnectionSnapshot;
import at.co.brandstetter.aircontrol.driver.ConnectionSupervisor;
import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.service.RegisterServiceInterface;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class APIController {

    private final RegisterServiceInterface registerServiceInterface;
    private final ConnectionSupervisor connectionSupervisor;

    public APIController(RegisterServiceInterface registerServiceInterface, ConnectionSupervisor connectionSupervisor) {
        this.registerServiceInterface = registerServiceInterface;
        this.connectionSupervisor = connectionSupervisor;
    }

    @GetMapping("/registers")
    public List<RegisterEntity> list() {
        return registerServiceInterface.listRegisters();
    }

    @GetMapping("/registers/{registerId}")
    public Optional<RegisterEntity> read(@PathVariable int registerId) {
        return registerServiceInterface.readRegister(registerId);
    }

    @GetMapping("/registers/{registerId}/status")
    public Optional<RegisterValueResponse> readStatus(@PathVariable int registerId) {
        return registerServiceInterface.readRegisterStatus(registerId);
    }

    @GetMapping("/connection")
    public ConnectionSnapshot connection() {
        return connectionSupervisor.snapshot();
    }

    @PutMapping("/registers/{registerId}")
    public ResponseEntity<Boolean> write(
            @PathVariable int registerId,
            @RequestBody RegisterEntity registerEntity
    ) {
        var response = registerServiceInterface.writeRegister(registerId, registerEntity);
        HttpStatus status = response.accepted() ? HttpStatus.OK : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(response.accepted());
    }
}
