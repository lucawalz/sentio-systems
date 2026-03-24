package dev.syslabs.sentio.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.event.AnimalDetectedEvent;
import dev.syslabs.sentio.service.N8nWorkflowTriggerService;
import dev.syslabs.sentio.service.WebSocketService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for animal detection events.
 * Handles notifications, workflow triggers, and WebSocket broadcasts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnimalEventListener {

    private final WebSocketService webSocketService;
    private final N8nWorkflowTriggerService n8nService;

    /**
     * Handles animal detection events.
     * Broadcasts to WebSocket and optionally triggers n8n workflows.
     */
    @Async
    @EventListener
    public void onAnimalDetected(AnimalDetectedEvent event) {
        log.info("Handling AnimalDetectedEvent: {} detected on device {} with confidence {}",
                event.getSpecies(), event.getDeviceId(), event.getConfidence());

        // Broadcast to WebSocket clients
        try {
            webSocketService.broadcastAnimalDetected(
                    event.getDeviceId(),
                    event.getSpecies(),
                    event.getConfidence());
        } catch (Exception e) {
            log.error("Failed to broadcast animal detection via WebSocket: {}", e.getMessage());
        }
    }

    private void triggerWorkflow(AnimalDetectedEvent event) {
        try {
            log.info("Triggering n8n workflow for rare/high-confidence detection: {}", event.getSpecies());
            n8nService.triggerAnimalDetectionWorkflow(
                    event.getDeviceId(),
                    event.getSpecies(),
                    event.getConfidence());
        } catch (Exception e) {
            log.error("Failed to trigger n8n workflow for animal detection: {}", e.getMessage());
        }
    }
}
