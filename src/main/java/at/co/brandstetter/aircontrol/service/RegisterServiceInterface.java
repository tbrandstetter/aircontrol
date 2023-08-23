package at.co.brandstetter.aircontrol.service;

import at.co.brandstetter.aircontrol.model.RegisterEntity;

import java.util.List;
import java.util.Optional;

public interface RegisterServiceInterface {
    Boolean writeRegister(int registerId, RegisterEntity registerEntity);
    Optional<RegisterEntity> readRegister(int registerId);
    List<RegisterEntity> listRegisters();
}
