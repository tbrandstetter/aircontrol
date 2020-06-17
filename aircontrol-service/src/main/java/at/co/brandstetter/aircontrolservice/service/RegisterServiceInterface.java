package at.co.brandstetter.aircontrolservice.service;

import at.co.brandstetter.aircontrolservice.model.RegisterEntity;

import java.util.List;
import java.util.Optional;

public interface RegisterServiceInterface {
    Boolean writeRegister(int registerId, RegisterEntity registerEntity);
    Optional<RegisterEntity> readRegister(int registerId);
    List<RegisterEntity> listRegisters();
}
