package org.example.backend.service;

import org.example.backend.model.AnimalDetection;

/**
 * Interface contract for animal classification orchestration.
 */
public interface IAnimalClassifierService {
    void classifyAndUpdate(AnimalDetection detection);

    String determineAnimalType(String species);
}
