package dev.syslabs.sentio.service.classification;

import lombok.RequiredArgsConstructor;
import dev.syslabs.sentio.model.AnimalDetection;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Processor for bird-specific classification payload handling.
 */
@Component
@RequiredArgsConstructor
public class BirdClassificationProcessor implements ClassificationProcessor {

    private final AnimalClassificationResponseProcessor responseProcessor;

    @Override
    public boolean supports(String animalType) {
        return "bird".equalsIgnoreCase(animalType);
    }

    @Override
    public void process(AnimalDetection detection, Map<String, Object> responseBody) {
        responseProcessor.processBirdClassification(detection, responseBody);
    }
}
