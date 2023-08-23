package at.co.brandstetter.aircontrol.service;

import at.co.brandstetter.aircontrol.config.RegisterConfiguration;
import at.co.brandstetter.aircontrol.driver.Modbus;
import at.co.brandstetter.aircontrol.model.RegisterEntity;
import at.co.brandstetter.aircontrol.model.RegisterRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class RegisterService implements RegisterServiceInterface {

    private final Modbus modbus;
    private final RegisterRepository registerRepository;

    public RegisterService(Modbus modbus, RegisterRepository registerRepository) {
        this.modbus = modbus;
        this.registerRepository = registerRepository;
    }

    @Override
    public Boolean writeRegister(int registerId, RegisterEntity registerEntity) {
        return modbus.write(registerId, registerEntity);
    }

    @Override
    public Optional<RegisterEntity> readRegister(int registerId) {

        Optional<RegisterEntity> register = Optional.empty();

        if (validateRegister(registerId)) {

            register = modbus.read(registerId);

            if (register.isPresent()) {
                this.extendRegister(register.get());
            }
        }

        return register;
    }

    @Override
    public List<RegisterEntity> listRegisters() {
        return (List<RegisterEntity>) registerRepository.findAll();
    }

    private void extendRegister(RegisterEntity register) {

        // Data from register datasource (file)
        RegisterConfiguration.Register registerEntry = modbus.search(register.getRegister());
        String description = registerEntry.getDescription();
        int min = registerEntry.getMin();
        int max = registerEntry.getMax();
        int divisor = registerEntry.getDivisor();

        // Update register
        //RegisterEntity registerEntity = new RegisterEntity();
        register.setRegister(register.getRegister());
        register.setDescription(description);
        register.setMin(min);
        register.setMax(max);
        register.setDivisor(divisor);
        registerRepository.save(register);

    }

    private Boolean validateRegister(int id) throws NoSuchElementException {

        String device = modbus.getDeviceId();

        List<String> supportedDevices = modbus.search(id).getDevices();

        // Device supported for this register?
        if (supportedDevices.contains(device)) {
            return true;
        };

        return false;
    }

}
