package dev.syslabs.sentio.service;

import dev.syslabs.sentio.model.AnimalDetection;

/**
 * Interface contract for animal classification orchestration.
 */
public interface IAnimalClassifierService {
    void classifyAndUpdate(AnimalDetection detection);

    String determineAnimalType(String species);
}
