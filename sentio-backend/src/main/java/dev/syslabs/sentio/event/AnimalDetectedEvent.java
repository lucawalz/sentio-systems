package dev.syslabs.sentio.event;

import lombok.Getter;
import dev.syslabs.sentio.model.AnimalDetection;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when an animal is detected by the AI classifier.
 * Triggers downstream actions like n8n workflows and WebSocket notifications.
 */
@Getter
public class AnimalDetectedEvent extends ApplicationEvent {

    private final AnimalDetection detection;
    private final String deviceId;
    private final String species;
    private final float confidence;
    private final boolean isRareSpecies;

    public AnimalDetectedEvent(Object source, AnimalDetection detection) {
        super(source);
        this.detection = detection;
        this.deviceId = detection.getDeviceId();
        this.species = detection.getSpecies();
        this.confidence = detection.getConfidence();
        // Consider a species "rare" if it's on a special list (could be configurable)
        this.isRareSpecies = isRareSpeciesCheck(detection.getSpecies());
    }

    private boolean isRareSpeciesCheck(String species) {
        // Could be loaded from config or database
        return species != null && (species.toLowerCase().contains("owl") ||
                species.toLowerCase().contains("eagle") ||
                species.toLowerCase().contains("falcon") ||
                species.toLowerCase().contains("heron"));
    }
}
