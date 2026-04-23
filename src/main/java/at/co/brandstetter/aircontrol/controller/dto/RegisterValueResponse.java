package at.co.brandstetter.aircontrol.controller.dto;

import at.co.brandstetter.aircontrol.driver.ConnectionState;

import java.time.Duration;
import java.time.LocalDateTime;

public record RegisterValueResponse(
        int register,
        String value,
        String description,
        int min,
        int max,
        int divisor,
        LocalDateTime lastupdate,
        boolean stale,
        Duration age,
        ConnectionState connectionState
) {
}
