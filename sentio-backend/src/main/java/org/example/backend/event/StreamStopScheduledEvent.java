package org.example.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * Event published when the last viewer leaves a stream.
 * The listener will wait for a delay period before stopping the stream,
 * allowing new viewers to join without a full stream restart.
 */
@Getter
public class StreamStopScheduledEvent extends ApplicationEvent {

    private final String deviceId;
    private final Instant scheduledAt;

    public StreamStopScheduledEvent(Object source, String deviceId) {
        super(source);
        this.deviceId = deviceId;
        this.scheduledAt = Instant.now();
    }
}
