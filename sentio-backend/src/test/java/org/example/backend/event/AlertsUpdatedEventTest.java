package org.example.backend.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlertsUpdatedEvent Unit Tests")
class AlertsUpdatedEventTest {

    @Test
    @DisplayName("Should correctly instantiate and return properties")
    void shouldReturnCorrectProperties() {
        Object source = new Object();
        String deviceId = "device-123";
        int alertCount = 5;
        boolean hasActiveAlerts = true;

        AlertsUpdatedEvent event = new AlertsUpdatedEvent(source, deviceId, alertCount, hasActiveAlerts);

        assertThat(event.getSource()).isEqualTo(source);
        assertThat(event.getDeviceId()).isEqualTo(deviceId);
        assertThat(event.getAlertCount()).isEqualTo(alertCount);
        assertThat(event.isHasActiveAlerts()).isEqualTo(hasActiveAlerts);
    }
}
