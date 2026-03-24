package dev.syslabs.sentio.service.classification;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for resolving the appropriate classification processor by animal type.
 */
@Component
public class ClassificationProcessorFactory {

    private final List<ClassificationProcessor> processors;

    public ClassificationProcessorFactory(List<ClassificationProcessor> processors) {
        this.processors = processors;
    }

    public ClassificationProcessor getProcessor(String animalType) {
        return processors.stream()
                .filter(processor -> processor.supports(animalType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No classification processor found for animalType=" + animalType));
    }
}
