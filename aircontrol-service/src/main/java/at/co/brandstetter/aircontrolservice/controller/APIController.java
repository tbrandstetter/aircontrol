package at.co.brandstetter.aircontrolservice.controller;

import at.co.brandstetter.aircontrolservice.model.RegisterEntity;
import at.co.brandstetter.aircontrolservice.service.RegisterServiceInterface;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class APIController {

    private final RegisterServiceInterface registerServiceInterface;

    public APIController(RegisterServiceInterface registerServiceInterface) {
        this.registerServiceInterface = registerServiceInterface;
    }

    @GetMapping("/registers")
    public List<RegisterEntity> list() {
        return registerServiceInterface.listRegisters();
    }

    @GetMapping("/registers/{registerId}")
    public Optional<RegisterEntity> read(@PathVariable int registerId) {
        return registerServiceInterface.readRegister(registerId);
    }

    @PutMapping("/registers/{registerId}")
    public boolean write(@PathVariable int registerId, @RequestBody RegisterEntity registerEntity) {
        if (registerServiceInterface.writeRegister(registerId, registerEntity)) {
            return true;
        }
        return false;
    }
}
