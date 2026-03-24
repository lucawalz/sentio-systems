package dev.syslabs.sentio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.service.classification.AnimalClassificationClient;
import dev.syslabs.sentio.service.classification.AnimalClassificationResponseProcessor;
import dev.syslabs.sentio.service.classification.AnimalTypePolicy;
import dev.syslabs.sentio.service.classification.ClassificationProcessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Service responsible for coordinating animal image classification workflow.
 * Delegates transport and response-mapping concerns to dedicated components.
 * <p>
 * This orchestrator supports queue-based asynchronous classification with HTTP
 * fallback and keeps external behavior stable for MQTT ingestion handlers.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalClassifierService implements IAnimalClassifierService {

    @Value("${queue.enabled:true}")
    private boolean queueEnabled;

    private final ImageStorageService imageStorageService;
    private final AnimalClassificationClient animalClassificationClient;
    private final AnimalClassificationResponseProcessor responseProcessor;
    private final AnimalTypePolicy animalTypePolicy;
    private final ClassificationProcessorFactory classificationProcessorFactory;
    private final RedisQueueService redisQueueService;
    private final ClassificationResultService resultProcessor;

    /**
     * Asynchronously classifies animal species and updates the detection record.
     * Images are sent through preprocessing service before classification.
     *
     * @param detection Detection entity to classify
     */
    @Async
    public void classifyAndUpdate(AnimalDetection detection) {
        log.debug("Starting AI classification process for detection ID: {} (Type: {})",
                detection.getId(), detection.getAnimalType());

        if (!animalTypePolicy.hasClassifierForAnimalType(detection.getAnimalType())) {
            log.info(
                    "No AI classifier available for animal type '{}' - detection ID: {}. Keeping original classification.",
                    detection.getAnimalType(), detection.getId());
            responseProcessor.markAsProcessedWithoutAIClassification(detection);
            return;
        }

        try {
            Optional<File> imageFile = getLocalImageFile(detection);
            if (imageFile.isEmpty()) {
                return;
            }

            if (queueEnabled && redisQueueService.isAvailable()) {
                log.debug("Using event-driven queue for classification");
                submitToQueueAsync(detection, imageFile.get());
                return;
            }

            log.debug("Using HTTP preprocessing service for classification");
            var responseBody = animalClassificationClient.callPreprocessingService(imageFile.get(),
                    detection.getAnimalType());
            if (responseBody != null) {
                classificationProcessorFactory
                        .getProcessor(detection.getAnimalType())
                        .process(detection, responseBody);
            }
        } catch (Exception e) {
            log.error("Error during AI classification for detection ID {}: {}",
                    detection.getId(), e.getMessage(), e);
        }
    }

    private void submitToQueueAsync(AnimalDetection detection, File imageFile) {
        try {
            byte[] imageBytes = java.nio.file.Files.readAllBytes(imageFile.toPath());

            redisQueueService.submitClassificationJobAsync(
                    imageBytes,
                    imageFile.getName(),
                    detection.getAnimalType(),
                    detection.getId(),
                    resultProcessor);

            log.info("Submitted detection {} to queue for async classification", detection.getId());

        } catch (Exception e) {
            log.warn("Queue submission failed for detection {}: {}, falling back to HTTP",
                    detection.getId(), e.getMessage());

            var responseBody = animalClassificationClient.callPreprocessingService(imageFile, detection.getAnimalType());
            if (responseBody != null) {
                classificationProcessorFactory
                        .getProcessor(detection.getAnimalType())
                        .process(detection, responseBody);
            }
        }
    }

    private Optional<File> getLocalImageFile(AnimalDetection detection) {
        Path localImagePath = imageStorageService.getLocalImagePath(detection.getImageUrl());
        File imageFileLocal = localImagePath.toFile();

        if (!imageFileLocal.exists()) {
            log.error("Image file not found for detection ID {}: {}", detection.getId(), localImagePath);
            return Optional.empty();
        }

        return Optional.of(imageFileLocal);
    }

    /**
     * Determines animal type from species name for initial classification.
     *
     * @param species Initial species label
     * @return Normalized animal type
     */
    public String determineAnimalType(String species) {
        return animalTypePolicy.determineAnimalType(species);
    }
}