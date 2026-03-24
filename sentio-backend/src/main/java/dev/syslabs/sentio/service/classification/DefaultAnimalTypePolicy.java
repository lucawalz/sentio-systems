package dev.syslabs.sentio.service.classification;

import org.springframework.stereotype.Component;

/**
 * Default implementation of animal type policy rules used by classification services.
 * Provides deterministic mapping for currently supported detection labels.
 * <p>
 * This component centralizes animal-type heuristics so future rule changes
 * can be applied without touching orchestration or transport logic.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class DefaultAnimalTypePolicy implements AnimalTypePolicy {

    /**
     * Evaluates whether the given animal type is supported by AI classifiers.
     *
     * @param animalType Animal type identifier to validate
     * @return true when classifier support exists, false otherwise
     */
    @Override
    public boolean hasClassifierForAnimalType(String animalType) {
        if (animalType == null) {
            return false;
        }

        return switch (animalType.toLowerCase()) {
            case "bird", "mammal", "human" -> true;
            default -> false;
        };
    }

    /**
     * Maps initial species labels into normalized animal types.
     *
     * @param species Initial species label from the upstream detector
     * @return Normalized animal type used by classification pipeline
     */
    @Override
    public String determineAnimalType(String species) {
        if (species == null) {
            return "unknown";
        }

        String lowerSpecies = species.toLowerCase();
        return switch (lowerSpecies) {
            case "bird" -> "bird";
            case "cat", "dog", "squirrel" -> "mammal";
            case "person" -> "human";
            default -> "unknown";
        };
    }
}