package dev.syslabs.sentio.event;

import lombok.Getter;
import dev.syslabs.sentio.model.Device;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a device is registered by a user.
 */
@Getter
public class DeviceRegisteredEvent extends ApplicationEvent {

    private final Device device;
    private final String username;

    public DeviceRegisteredEvent(Object source, Device device, String username) {
        super(source);
        this.device = device;
        this.username = username;
    }
}
