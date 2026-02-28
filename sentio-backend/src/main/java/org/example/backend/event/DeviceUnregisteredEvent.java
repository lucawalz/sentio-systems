package org.example.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a device is unregistered by a user.
 */
@Getter
public class DeviceUnregisteredEvent extends ApplicationEvent {

    private final String deviceId;
    private final String username;

    public DeviceUnregisteredEvent(Object source, String deviceId, String username) {
        super(source);
        this.deviceId = deviceId;
        this.username = username;
    }
}
