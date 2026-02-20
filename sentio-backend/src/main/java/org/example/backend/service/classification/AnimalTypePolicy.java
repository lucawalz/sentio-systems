package org.example.backend.service.classification;

/**
 * Policy contract for animal type handling in the AI classification workflow.
 * Defines supported classifier types and species-to-animal-type mapping behavior.
 * <p>
 * Implementations encapsulate type resolution rules so orchestration services
 * remain independent from concrete classification heuristics.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
public interface AnimalTypePolicy {
    /**
     * Checks whether an AI classifier exists for the given animal type.
     *
     * @param animalType Animal type identifier (e.g., "bird", "mammal")
     * @return true if the type can be classified by AI, false otherwise
     */
    boolean hasClassifierForAnimalType(String animalType);

    /**
     * Derives the normalized animal type from an initial species label.
     *
     * @param species Initial species label from detection pipeline
     * @return Normalized animal type or "unknown" if no mapping exists
     */
    String determineAnimalType(String species);
}