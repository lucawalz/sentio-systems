package org.example.backend.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.event.StreamStopScheduledEvent;
import org.example.backend.service.StreamStopCoordinator;
import org.example.backend.service.ViewerSessionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;

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
    private final StreamStopCoordinator stopCoordinator;

    @Value("${sentio.stream.stop-delay-ms:60000}")
    private long stopDelayMs;

    /**
     * Handle scheduled stream stop with graceful delay.
     * Waits before actually stopping to allow new viewers to join.
     */
    @Async
    @EventListener
    public void onStreamStopScheduled(StreamStopScheduledEvent event) {
        String deviceId = event.getDeviceId();
        Instant scheduledAt = event.getScheduledAt();

        if (deviceId == null) {
            log.warn("Received StreamStopScheduledEvent with null deviceId - ignoring");
            return;
        }

        stopCoordinator.schedule(deviceId, scheduledAt);
        log.info("Stream stop scheduled for device {} at {} - waiting {}s",
                deviceId, scheduledAt, stopDelayMs / 1000);

        try {
            Thread.sleep(stopDelayMs);

            Instant currentScheduled = stopCoordinator.get(deviceId);
            if (currentScheduled == null || !currentScheduled.equals(scheduledAt)) {
                log.info("Stream stop cancelled for device {} - new viewer joined during delay", deviceId);
                return;
            }

            long viewerCount;
            try {
                viewerCount = viewerSessionService.getViewerCount(deviceId);
            } catch (Exception e) {
                log.error("Failed to get viewer count for device {} - aborting stop: {}", deviceId, e.getMessage());
                stopCoordinator.remove(deviceId);
                return;
            }

            if (viewerCount < 0) {
                log.warn("Invalid negative viewer count ({}) for device {} - aborting stop", viewerCount, deviceId);
                stopCoordinator.remove(deviceId);
                return;
            }

            if (viewerCount > 0) {
                log.info("Stream stop cancelled for device {} - viewers present after delay", deviceId);
                stopCoordinator.remove(deviceId);
                return;
            }

            log.info("No viewers after {}s delay - stopping stream for device {}",
                    stopDelayMs / 1000, deviceId);
            sendStreamCommand(deviceId, "stop");
            stopCoordinator.remove(deviceId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Stream stop interrupted for device {}", deviceId);
        }
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
