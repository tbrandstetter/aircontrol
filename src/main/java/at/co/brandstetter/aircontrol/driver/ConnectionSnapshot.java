package at.co.brandstetter.aircontrol.driver;

import java.time.Duration;
import java.time.Instant;

public record ConnectionSnapshot(
        ConnectionState state,
        String port,
        Instant lastDataAt,
        Duration staleTimeout,
        int reconnectAttempts,
        String lastError
) {
}
