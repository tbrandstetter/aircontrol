package at.co.brandstetter.aircontrol.service;

import at.co.brandstetter.aircontrol.controller.dto.RegisterValueResponse;
import at.co.brandstetter.aircontrol.controller.dto.RegisterWriteResponse;
import at.co.brandstetter.aircontrol.model.RegisterEntity;

import java.util.List;
import java.util.Optional;

public interface RegisterServiceInterface {
    RegisterWriteResponse writeRegister(int registerId, RegisterEntity registerEntity);

    Optional<RegisterEntity> readRegister(int registerId);

    Optional<RegisterValueResponse> readRegisterStatus(int registerId);

    List<RegisterEntity> listRegisters();
}
