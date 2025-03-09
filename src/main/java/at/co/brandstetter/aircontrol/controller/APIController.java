package at.co.brandstetter.aircontrol.controller;

import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.service.RegisterServiceInterface;
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
        return registerServiceInterface.writeRegister(registerId, registerEntity);
    }
}
