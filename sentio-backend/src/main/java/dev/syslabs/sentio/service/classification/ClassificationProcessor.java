package dev.syslabs.sentio.service.classification;

import dev.syslabs.sentio.model.AnimalDetection;

import java.util.Map;

/**
 * Contract for animal-type-specific classification response processors.
 */
public interface ClassificationProcessor {
    boolean supports(String animalType);

    void process(AnimalDetection detection, Map<String, Object> responseBody);
}
