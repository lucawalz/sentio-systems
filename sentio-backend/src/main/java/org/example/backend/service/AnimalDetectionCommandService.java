package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.event.AnimalDetectedEvent;
import org.example.backend.model.AnimalDetection;
import org.example.backend.repository.AnimalDetectionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Command service for animal detection write operations (CQRS pattern).
 * Handles: save, update, delete operations.
 * Separated from query operations for better scalability and maintainability.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalDetectionCommandService {

    private final AnimalDetectionRepository animalDetectionRepository;
    private final ImageStorageService imageStorageService;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Persists a new animal detection record to the database.
     * Publishes AnimalDetectedEvent for reactive notifications.
     *
     * @param animalDetection The animal detection data to save
     * @return The saved animal detection with generated ID and timestamps
     */
    public AnimalDetection saveAnimalDetection(AnimalDetection animalDetection) {
        log.info("Saving animal detection - Type: {}, Species: {} with confidence: {}",
                animalDetection.getAnimalType(), animalDetection.getSpecies(),
                animalDetection.getConfidence());
        AnimalDetection saved = animalDetectionRepository.save(animalDetection);
        log.debug("Successfully saved animal detection with ID: {}", saved.getId());

        // Publish event for reactive notifications (WebSocket, n8n workflows)
        eventPublisher.publishEvent(new AnimalDetectedEvent(this, saved));

        return saved;
    }

    /**
     * Deletes an animal detection and its associated image file.
     *
     * @param id The unique detection ID to delete
     * @return true if deletion was successful, false if detection not found
     */
    public boolean deleteDetection(Long id) {
        log.debug("Attempting to delete animal detection with ID: {}", id);
        return animalDetectionRepository.findById(id)
                .map(detection -> {
                    if (!deviceService.hasAccessToDevice(detection.getDeviceId())) {
                        log.warn("User attempted to delete detection {} without ownership of device", id);
                        throw new org.springframework.security.access.AccessDeniedException(
                                "Access denied to delete detection");
                    }
                    // Delete associated image if it exists
                    if (detection.getImageUrl() != null) {
                        boolean imageDeleted = imageStorageService.deleteImage(detection.getImageUrl());
                        log.debug("Associated image deletion result: {}", imageDeleted);
                    }
                    animalDetectionRepository.deleteById(id);
                    log.info("Successfully deleted animal detection with ID: {}", id);
                    return true;
                })
                .orElseGet(() -> {
                    log.warn("Animal detection with ID {} not found for deletion", id);
                    return false;
                });
    }
}
