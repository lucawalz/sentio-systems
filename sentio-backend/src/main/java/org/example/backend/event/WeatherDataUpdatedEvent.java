package org.example.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when weather data is updated for a location.
 * Used to push real-time updates to connected frontend clients.
 */
@Getter
public class WeatherDataUpdatedEvent extends ApplicationEvent {

    public enum DataType {
        FORECAST,
        HISTORICAL,
        CURRENT
    }

    private final DataType dataType;
    private final String deviceId;
    private final Double latitude;
    private final Double longitude;

    public WeatherDataUpdatedEvent(Object source, DataType dataType, String deviceId,
            Double latitude, Double longitude) {
        super(source);
        this.dataType = dataType;
        this.deviceId = deviceId;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
