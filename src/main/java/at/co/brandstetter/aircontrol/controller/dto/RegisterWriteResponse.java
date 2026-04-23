package at.co.brandstetter.aircontrol.controller.dto;

import at.co.brandstetter.aircontrol.driver.ConnectionState;

public record RegisterWriteResponse(
        int register,
        boolean accepted,
        ConnectionState connectionState,
        String message
) {
}
