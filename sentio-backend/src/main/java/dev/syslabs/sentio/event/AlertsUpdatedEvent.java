package dev.syslabs.sentio.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when weather alerts are updated for a location.
 * Used to push real-time alert notifications to connected frontend clients.
 */
@Getter
public class AlertsUpdatedEvent extends ApplicationEvent {

    private final String deviceId;
    private final int alertCount;
    private final boolean hasActiveAlerts;

    public AlertsUpdatedEvent(Object source, String deviceId, int alertCount, boolean hasActiveAlerts) {
        super(source);
        this.deviceId = deviceId;
        this.alertCount = alertCount;
        this.hasActiveAlerts = hasActiveAlerts;
    }
}
