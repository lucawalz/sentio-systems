package org.example.backend.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.event.StreamStopScheduledEvent;
import org.example.backend.service.ViewerSessionService;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for stream-related events and handles graceful stop with delay.
 * Uses EDA pattern consistent with other listeners in the codebase.
 * 
 * The graceful stop mechanism:
 * 1. When last viewer leaves, StreamStopScheduledEvent is published
 * 2. This listener waits 60 seconds (async)
 * 3. If no new viewers joined during delay, sends stop command
 * 4. If new viewers joined, the stop is cancelled via timestamp comparison
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreamEventListener {

    private final ViewerSessionService viewerSessionService;
    private final MessageChannel mqttOutboundChannel;

    private static final long STOP_DELAY_MS = 60_000; // 60 seconds

    // Track when stops were scheduled - allows cancellation by publishing new event
    private final ConcurrentHashMap<String, Instant> scheduledStops = new ConcurrentHashMap<>();

    /**
     * Handle scheduled stream stop with graceful delay.
     * Waits 60 seconds before actually stopping to allow new viewers to join.
     */
    @Async
    @EventListener
    public void onStreamStopScheduled(StreamStopScheduledEvent event) {
        String deviceId = event.getDeviceId();
        Instant scheduledAt = event.getScheduledAt();

        // Record this stop request
        scheduledStops.put(deviceId, scheduledAt);
        log.info("Stream stop scheduled for device {} at {} - waiting {}s",
                deviceId, scheduledAt, STOP_DELAY_MS / 1000);

        try {
            // Wait for the delay period
            Thread.sleep(STOP_DELAY_MS);

            // Check if this stop request is still valid (not superseded by new viewers)
            Instant currentScheduled = scheduledStops.get(deviceId);
            if (currentScheduled == null || !currentScheduled.equals(scheduledAt)) {
                log.info("Stream stop cancelled for device {} - new viewer joined during delay", deviceId);
                return;
            }

            // Double-check no viewers currently watching
            if (viewerSessionService.getViewerCount(deviceId) > 0) {
                log.info("Stream stop cancelled for device {} - viewers present after delay", deviceId);
                scheduledStops.remove(deviceId);
                return;
            }

            // Send stop command
            log.info("No viewers after {}s delay - stopping stream for device {}",
                    STOP_DELAY_MS / 1000, deviceId);
            sendStreamCommand(deviceId, "stop");
            scheduledStops.remove(deviceId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Stream stop interrupted for device {}", deviceId);
        }
    }

    /**
     * Cancel a pending stop for a device (called when new viewer joins).
     */
    public void cancelPendingStop(String deviceId) {
        Instant removed = scheduledStops.remove(deviceId);
        if (removed != null) {
            log.info("Cancelled pending stop for device {} - new viewer joined", deviceId);
        }
    }

    /**
     * Check if a stop is currently pending for a device.
     */
    public boolean isStopPending(String deviceId) {
        return scheduledStops.containsKey(deviceId);
    }

    private void sendStreamCommand(String deviceId, String command) {
        try {
            String topic = String.format("device/%s/command", deviceId);
            String payload = String.format("{\"service\": \"stream\", \"command\": \"%s\"}", command);

            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .build());

            log.info("Sent stream command '{}' to device {} on topic {}", command, deviceId, topic);
        } catch (Exception e) {
            log.error("Failed to send stream command to device {}: {}", deviceId, e.getMessage());
        }
    }
}
