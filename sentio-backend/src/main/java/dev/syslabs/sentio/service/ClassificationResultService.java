package dev.syslabs.sentio.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.event.AnimalDetectedEvent;
import dev.syslabs.sentio.event.ClassificationResultEvent;
import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.repository.AnimalDetectionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Processes classification results received via EDA (Event-Driven
 * Architecture).
 * 
 * Listens for ClassificationResultEvent and updates detection records.
 * Follows the same pattern as DeviceEventListener and AnimalEventListener.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClassificationResultService {

    private static final String CLASSIFICATION_KEY = "classification";
    private static final String DETECTION_KEY = "detection";
    private static final String TOP_SPECIES_KEY = "top_species";
    private static final String TOP_CONFIDENCE_KEY = "top_confidence";
    private static final String PREDICTIONS_KEY = "predictions";
    private static final String SPECIES_KEY = "species";
    private static final String CONFIDENCE_KEY = "confidence";
    private static final String BIRD_DETECTED_KEY = "bird_detected";
    private static final int MAX_ALTERNATE_SPECIES = 4;

    private final AnimalDetectionRepository animalDetectionRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Maps job IDs to detection IDs for result correlation
    private final ConcurrentHashMap<String, Long> pendingJobs = new ConcurrentHashMap<>();

    /**
     * Register a job for async processing.
     * Call this when submitting a job to associate it with a detection.
     */
    public void registerJob(String jobId, Long detectionId) {
        pendingJobs.put(jobId, detectionId);
        log.debug("Registered job {} for detection {}", jobId, detectionId);
    }

    /**
     * Get detection ID for a job (used by listener to look up pending jobs).
     */
    public Long getDetectionIdForJob(String jobId) {
        return pendingJobs.get(jobId);
    }

    /**
     * Handles classification result events.
     * Uses @Async and @EventListener to align with existing EDA pattern.
     */
    @Async
    @EventListener
    @Transactional
    public void onClassificationResult(ClassificationResultEvent event) {
        String jobId = event.getJobId();
        Long detectionId = event.getDetectionId();

        log.info("Handling ClassificationResultEvent for job: {} (detection: {})", jobId, detectionId);

        // Remove from pending jobs
        pendingJobs.remove(jobId);

        Optional<AnimalDetection> detectionOpt = animalDetectionRepository.findById(detectionId);
        if (detectionOpt.isEmpty()) {
            log.warn("Detection {} not found for job {}", detectionId, jobId);
            return;
        }

        AnimalDetection detection = detectionOpt.get();

        if (event.isSuccess()) {
            updateDetectionFromResult(detection, event.getResult());
            log.info("Updated detection {} - species: {}, confidence: {}",
                    detectionId, detection.getSpecies(), detection.getConfidence());
        } else {
            String error = (String) event.getResult().get("error");
            log.warn("Classification failed for job {}: {}", jobId, error);
            markAsProcessedWithError(detection, error);
        }

        AnimalDetection savedDetection = animalDetectionRepository.save(detection);

        // Publish AnimalDetectedEvent to trigger downstream actions (WebSocket, n8n)
        if (event.isSuccess() && savedDetection.getSpecies() != null) {
            eventPublisher.publishEvent(new AnimalDetectedEvent(this, savedDetection));
            log.debug("Published AnimalDetectedEvent for detection {}", detectionId);
        }
    }

    /**
     * Update detection with classification results.
     */
    @SuppressWarnings("unchecked")
    private void updateDetectionFromResult(AnimalDetection detection, Map<String, Object> result) {
        Map<String, Object> classification = (Map<String, Object>) result.get(CLASSIFICATION_KEY);
        Map<String, Object> detectionInfo = (Map<String, Object>) result.get(DETECTION_KEY);

        if (classification == null) {
            log.warn("No classification data in result");
            detection.setAiProcessed(true);
            return;
        }

        // Check if animal was detected
        boolean animalDetected = true;
        if (detectionInfo != null) {
            Boolean birdDetected = (Boolean) detectionInfo.get(BIRD_DETECTED_KEY);
            Boolean animalDetectedFlag = (Boolean) detectionInfo.get("animal_detected");
            animalDetected = Boolean.TRUE.equals(birdDetected) || Boolean.TRUE.equals(animalDetectedFlag);
        }

        if (!animalDetected) {
            detection.setSpecies("No animal detected");
            detection.setConfidence(0.0f);
            detection.setAiProcessed(true);
            return;
        }

        // Get top prediction
        String topSpecies = (String) classification.get(TOP_SPECIES_KEY);
        Number topConfidence = (Number) classification.get(TOP_CONFIDENCE_KEY);

        if (topSpecies != null) {
            detection.setSpecies(cleanSpeciesName(topSpecies));
        }
        if (topConfidence != null) {
            detection.setConfidence(topConfidence.floatValue());
        }

        // Get alternate predictions
        List<Map<String, Object>> predictions = (List<Map<String, Object>>) classification.get(PREDICTIONS_KEY);
        if (predictions != null && predictions.size() > 1) {
            StringBuilder alternates = new StringBuilder();
            int count = 0;
            for (int i = 1; i < predictions.size() && count < MAX_ALTERNATE_SPECIES; i++) {
                Map<String, Object> pred = predictions.get(i);
                String species = cleanSpeciesName((String) pred.get(SPECIES_KEY));
                Number confidence = (Number) pred.get(CONFIDENCE_KEY);

                if (alternates.length() > 0) {
                    alternates.append(", ");
                }
                alternates.append(String.format("%s (%.1f%%)", species, confidence.floatValue() * 100));
                count++;
            }
            detection.setAlternateSpecies(alternates.toString());
        }

        detection.setAiProcessed(true);
    }

    private void markAsProcessedWithError(AnimalDetection detection, String error) {
        detection.setAiProcessed(true);
        detection.setSpecies("Classification failed");
        detection.setAlternateSpecies(error != null ? error : "Unknown error");
    }

    /**
     * Clean up species names from SpeciesNet format.
     */
    private String cleanSpeciesName(String species) {
        if (species == null) {
            return "Unknown";
        }

        // SpeciesNet returns format like
        // "uuid;class;order;family;genus;species;common_name"
        if (species.contains(";")) {
            String[] parts = species.split(";");
            if (parts.length >= 7 && !parts[6].isEmpty()) {
                return parts[6]; // Return common name
            } else if (parts.length >= 6 && !parts[5].isEmpty()) {
                return parts[5]; // Return species name
            }
        }

        return species;
    }

    /**
     * Get the number of pending jobs (for monitoring).
     */
    public int getPendingJobCount() {
        return pendingJobs.size();
    }
}
