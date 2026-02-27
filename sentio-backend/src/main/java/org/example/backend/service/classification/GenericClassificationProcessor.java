package org.example.backend.service.classification;

import lombok.RequiredArgsConstructor;
import org.example.backend.model.AnimalDetection;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Default processor for non-bird classification payload handling.
 */
@Component
@RequiredArgsConstructor
public class GenericClassificationProcessor implements ClassificationProcessor {

    private final AnimalClassificationResponseProcessor responseProcessor;

    @Override
    public boolean supports(String animalType) {
        return true;
    }

    @Override
    public void process(AnimalDetection detection, Map<String, Object> responseBody) {
        responseProcessor.processGenericClassification(detection, responseBody);
    }
}
