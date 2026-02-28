package org.example.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the state of pending stream stop operations.
 * Shared between StreamService (to cancel) and StreamEventListener (to schedule/check).
 */
@Component
@Slf4j
public class StreamStopCoordinator {

    private final ConcurrentHashMap<String, Instant> scheduledStops = new ConcurrentHashMap<>();

    public void schedule(String deviceId, Instant scheduledAt) {
        scheduledStops.put(deviceId, scheduledAt);
    }

    public Instant get(String deviceId) {
        return scheduledStops.get(deviceId);
    }

    public void remove(String deviceId) {
        scheduledStops.remove(deviceId);
    }

    public void cancelPendingStop(String deviceId) {
        Instant removed = scheduledStops.remove(deviceId);
        if (removed != null) {
            log.info("Cancelled pending stop for device {} - new viewer joined", deviceId);
        }
    }

    public boolean isStopPending(String deviceId) {
        return scheduledStops.containsKey(deviceId);
    }
}
