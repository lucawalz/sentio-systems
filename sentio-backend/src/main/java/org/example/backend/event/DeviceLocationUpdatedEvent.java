package org.example.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a device's GPS coordinates are updated.
 * Triggers downstream services to fetch weather data for the new location.
 */
@Getter
public class DeviceLocationUpdatedEvent extends ApplicationEvent {

    private final String deviceId;
    private final Double latitude;
    private final Double longitude;
    private final boolean isFirstLocation;

    public DeviceLocationUpdatedEvent(Object source, String deviceId,
            Double latitude, Double longitude, boolean isFirstLocation) {
        super(source);
        this.deviceId = deviceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFirstLocation = isFirstLocation;
    }
}
