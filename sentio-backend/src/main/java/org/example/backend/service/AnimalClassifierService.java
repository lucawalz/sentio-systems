package org.example.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AnimalDetectionDTO;
import org.example.backend.model.AnimalDetection;
import org.example.backend.repository.AnimalDetectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for unified animal detection and AI classification.
 * Handles classification for multiple animal types including birds, mammals,
 * and others.
 * Routes images through preprocessing service before classification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnimalClassifierService {

    @Value("${preprocessing.service.url}")
    private String preprocessingServiceUrl;
    private static final int MAX_ALTERNATE_SPECIES = 4;
    private static final String DETECTION_KEY = "detection";
    private static final String CLASSIFICATION_KEY = "classification";
    private static final String TOP_SPECIES_KEY = "top_species";
    private static final String TOP_CONFIDENCE_KEY = "top_confidence";
    private static final String PREDICTIONS_KEY = "predictions";
    private static final String SPECIES_KEY = "species";
    private static final String CONFIDENCE_KEY = "confidence";
    private static final String BIRD_DETECTED_KEY = "bird_detected";
    private static final String UNKNOWN_SPECIES = "Unknown";

    private final AnimalDetectionRepository animalDetectionRepository;
    private final RestTemplate restTemplate;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    /**
     * Asynchronously classifies animal species and updates the detection record.
     * Images are sent through preprocessing service before classification.
     */
    @Async
    public void classifyAndUpdate(AnimalDetection detection) {
        log.debug("Starting AI classification process for detection ID: {} (Type: {})",
                detection.getId(), detection.getAnimalType());

        // Check if we have a classifier for this animal type
        if (!hasClassifierForAnimalType(detection.getAnimalType())) {
            log.info(
                    "No AI classifier available for animal type '{}' - detection ID: {}. Keeping original classification.",
                    detection.getAnimalType(), detection.getId());
            markAsProcessedWithoutAIClassification(detection);
            return;
        }

        try {
            // Get and validate image file
            Optional<File> imageFile = getLocalImageFile(detection);
            if (imageFile.isEmpty()) {
                return;
            }

            // Call preprocessing service which will forward to appropriate classifier
            Map<String, Object> responseBody = callPreprocessingService(imageFile.get(), detection.getAnimalType());
            if (responseBody != null) {
                processClassificationResponse(detection, responseBody);
            }
        } catch (Exception e) {
            log.error("Error during AI classification for detection ID {}: {}",
                    detection.getId(), e.getMessage(), e);
        }
    }

    /**
     * Checks if we have an AI classifier for the given animal type
     */
    private boolean hasClassifierForAnimalType(String animalType) {
        if (animalType == null) {
            return false;
        }

        return switch (animalType.toLowerCase()) {
            case "bird", "mammal", "human" -> true; // human still for testing
            default -> false;
        };
    }

    /**
     * Retrieves and validates the local image file
     */
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
     * Calls the preprocessing service which handles image enhancement and
     * forwarding to classifier
     */
    @CircuitBreaker(name = "aiClassifier", fallbackMethod = "callPreprocessingServiceFallback")
    private Map<String, Object> callPreprocessingService(File imageFile, String animalType) {
        try {
            // Prepare request with image and animal type
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(imageFile));
            body.add("animal_type", animalType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.debug("Calling preprocessing service at: {} for animal type: {}",
                    preprocessingServiceUrl, animalType);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    preprocessingServiceUrl,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<>() {
                    });

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // Log preprocessing metadata if available
                if (responseBody.containsKey("preprocessing_applied")) {
                    log.info("Image preprocessing applied - Original: {} bytes, Enhanced: {} bytes",
                            responseBody.get("original_size_bytes"),
                            responseBody.get("enhanced_size_bytes"));
                }

                return responseBody;
            } else {
                log.warn("Preprocessing service returned unsuccessful response: HTTP {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error calling preprocessing service: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Marks detection as processed without AI classification for unsupported animal
     * types
     */
    private void markAsProcessedWithoutAIClassification(AnimalDetection detection) {
        // Keep the original species and confidence as-is
        detection.setAiProcessed(false); // No AI processing occurred
        detection.setProcessedAt(LocalDateTime.now());
        animalDetectionRepository.save(detection);

        log.info(
                "Detection ID {} marked as processed without AI classification - keeping original classification: {} ({})",
                detection.getId(), detection.getSpecies(), detection.getAnimalType());
    }

    /**
     * Processes the classification response and updates the detection record.
     */
    private void processClassificationResponse(AnimalDetection detection, Map<String, Object> responseBody) {
        try {
            // Handle different response formats based on animal type
            if ("bird".equalsIgnoreCase(detection.getAnimalType())) {
                processBirdClassificationResponse(detection, responseBody);
            } else {
                // Generic processing for other animal types
                processGenericClassificationResponse(detection, responseBody);
            }
        } catch (Exception e) {
            log.error("Error processing classification response for detection ID {}: {}",
                    detection.getId(), e.getMessage(), e);
        }
    }

    /**
     * Processes bird classification response (backward compatibility with existing
     * API).
     */
    private void processBirdClassificationResponse(AnimalDetection detection, Map<String, Object> responseBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> detectionMap = (Map<String, Object>) responseBody.get(DETECTION_KEY);
            @SuppressWarnings("unchecked")
            Map<String, Object> classificationMap = (Map<String, Object>) responseBody.get(CLASSIFICATION_KEY);

            if (detectionMap == null || classificationMap == null) {
                log.error("Invalid API response structure for detection ID {}: missing required fields",
                        detection.getId());
                return;
            }

            Boolean animalDetected = (Boolean) detectionMap.get(BIRD_DETECTED_KEY);

            if (Boolean.TRUE.equals(animalDetected)) {
                updateBirdDetectionWithClassification(detection, classificationMap);
                log.info("Successfully updated bird detection ID {} with AI classification", detection.getId());
            } else {
                handleNoAnimalDetected(detection);
            }
        } catch (ClassCastException | NullPointerException e) {
            log.error("Error processing bird classification response for detection ID {}: Invalid data format - {}",
                    detection.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing bird classification response for detection ID {}: {}",
                    detection.getId(), e.getMessage(), e);
        }
    }

    /**
     * Processes generic animal classification response for non-bird animals.
     */
    private void processGenericClassificationResponse(AnimalDetection detection, Map<String, Object> responseBody) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> classificationMap = (Map<String, Object>) responseBody.get(CLASSIFICATION_KEY);

            if (classificationMap != null) {
                updateGenericClassificationResults(detection, classificationMap);
                log.info("Successfully updated {} detection ID {} with AI classification",
                        detection.getAnimalType(), detection.getId());
            } else {
                handleNoAnimalDetected(detection);
            }
        } catch (ClassCastException | NullPointerException e) {
            log.error("Error processing generic classification response for detection ID {}: Invalid data format - {}",
                    detection.getId(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing generic classification response for detection ID {}: {}",
                    detection.getId(), e.getMessage(), e);
        }
    }

    /**
     * Updates the detection record with classification results from the generic
     * classifier.
     * Extracts common names from the taxonomic format and cleans up results.
     */
    private void updateGenericClassificationResults(AnimalDetection detection, Map<String, Object> classificationMap)
            throws JsonProcessingException {

        storeOriginalValues(detection);

        String topSpecies = (String) classificationMap.get(TOP_SPECIES_KEY);
        Double topConfidence = ((Number) classificationMap.get(TOP_CONFIDENCE_KEY)).doubleValue();

        String displaySpecies = extractCommonName(topSpecies);

        if ("blank".equalsIgnoreCase(displaySpecies)) {
            log.debug("Top species is 'blank', searching for better candidate in predictions...");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> predictionsList = (List<Map<String, Object>>) classificationMap
                    .get(PREDICTIONS_KEY);

            boolean foundBetterCandidate = false;

            if (predictionsList != null) {
                // Iterate through predictions to find first non-blank
                for (Map<String, Object> pred : predictionsList) {
                    String predSpecies = (String) pred.get(SPECIES_KEY);
                    String predCommon = extractCommonName(predSpecies);

                    if (!"blank".equalsIgnoreCase(predCommon)) {
                        // Found a better candidate
                        displaySpecies = predCommon;
                        Double conf = ((Number) pred.get(CONFIDENCE_KEY)).doubleValue();
                        topConfidence = conf;
                        topSpecies = predSpecies; // Update for alternate processing check
                        foundBetterCandidate = true;
                        log.info("Promoted candidate '{}' ({:.2f}) over 'blank'", displaySpecies, topConfidence);
                        break;
                    }
                }
            }

            if (!foundBetterCandidate) {
                log.warn("All predictions are 'blank'. Falling back to original species: {}",
                        detection.getOriginalSpecies());
                displaySpecies = detection.getOriginalSpecies() != null ? detection.getOriginalSpecies()
                        : displaySpecies;
                if (detection.getOriginalConfidence() != null) {
                    topConfidence = detection.getOriginalConfidence().doubleValue();
                }
            }
        }

        detection.setSpecies(displaySpecies);
        detection.setConfidence(topConfidence.floatValue());

        processAlternateSpecies(detection, classificationMap, displaySpecies);
        markAsAiProcessed(detection);

        log.info("Successfully updated detection ID {} - Type: {}, Primary: {} ({:.2f})",
                detection.getId(), detection.getAnimalType(), displaySpecies, topConfidence);
    }

    /**
     * Process and store alternate species from classification results
     */
    @SuppressWarnings("unchecked")
    private void processAlternateSpecies(AnimalDetection detection, Map<String, Object> classificationMap,
            String currentTopSpecies)
            throws JsonProcessingException {

        List<Map<String, Object>> predictionsList = (List<Map<String, Object>>) classificationMap.get(PREDICTIONS_KEY);
        if (predictionsList == null || predictionsList.isEmpty()) {
            return;
        }

        List<AnimalDetectionDTO.AlternateSpeciesDTO> alternateSpeciesList = new ArrayList<>();

        // Iterate through predictions
        int count = 0;
        for (Map<String, Object> pred : predictionsList) {
            if (count >= MAX_ALTERNATE_SPECIES)
                break;

            String species = (String) pred.get(SPECIES_KEY);
            Double confidence = ((Number) pred.get(CONFIDENCE_KEY)).doubleValue();

            // Extract common name
            String displayAlternate = extractCommonName(species);

            // Skip if it's the same as our selected top species, or if it's "blank"
            if (displayAlternate.equalsIgnoreCase(currentTopSpecies) || "blank".equalsIgnoreCase(displayAlternate)) {
                continue;
            }

            alternateSpeciesList.add(new AnimalDetectionDTO.AlternateSpeciesDTO(
                    displayAlternate, confidence.floatValue()));
            count++;
        }

        // Convert to JSON string for storage
        if (!alternateSpeciesList.isEmpty()) {
            String alternateSpeciesJson = objectMapper.writeValueAsString(alternateSpeciesList);
            detection.setAlternateSpecies(alternateSpeciesJson);
        }
    }

    /**
     * Stores original values if not already stored
     */
    private void storeOriginalValues(AnimalDetection detection) {
        if (detection.getOriginalSpecies() == null) {
            detection.setOriginalSpecies(detection.getSpecies());
            detection.setOriginalConfidence(detection.getConfidence());
        }
    }

    /**
     * Marks the detection as processed by AI
     */
    private void markAsAiProcessed(AnimalDetection detection) {
        detection.setAiProcessed(true);
        detection.setAiClassifiedAt(LocalDateTime.now());
        animalDetectionRepository.save(detection);
    }

    /**
     * Extracts the common name from a taxonomic species string.
     * Format example:
     * "990ae9dd-7a59-4344-afcb-1b7b21368000;mammalia;primates;hominidae;homo;sapiens;human"
     * Returns: "human"
     */
    private String extractCommonName(String taxonomicSpecies) {
        if (taxonomicSpecies == null || taxonomicSpecies.isEmpty()) {
            return UNKNOWN_SPECIES;
        }

        String[] parts = taxonomicSpecies.split(";");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1].trim();
            return lastPart.isEmpty() ? taxonomicSpecies : lastPart;
        }

        return taxonomicSpecies;
    }

    /**
     * Updates the detection record with bird classification results.
     */
    private void updateBirdDetectionWithClassification(AnimalDetection detection, Map<String, Object> classificationMap)
            throws JsonProcessingException {

        storeOriginalValues(detection);

        // Update primary species and confidence
        String topSpecies = (String) classificationMap.get(TOP_SPECIES_KEY);
        Double topConfidence = ((Number) classificationMap.get(TOP_CONFIDENCE_KEY)).doubleValue();

        detection.setSpecies(topSpecies);
        detection.setConfidence(topConfidence.floatValue());

        // Process alternate species
        processAlternateSpecies(detection, classificationMap, topSpecies);

        // Mark as AI processed
        markAsAiProcessed(detection);

        log.info("Successfully updated detection ID {} - Type: {}, Primary: {} ({:.2f})",
                detection.getId(), detection.getAnimalType(), topSpecies, topConfidence);
    }

    /**
     * Handles cases where no animal is detected by the classifier.
     */
    private void handleNoAnimalDetected(AnimalDetection detection) {
        log.warn("AI classification found no {} in image for detection ID {}",
                detection.getAnimalType(), detection.getId());

        // Still mark as processed even if no animal found
        detection.setAiProcessed(true);
        detection.setAiClassifiedAt(LocalDateTime.now());
        animalDetectionRepository.save(detection);
    }

    /**
     * Determines animal type from species name for initial classification.
     * This method can be used when the initial detection doesn't specify animal
     * type.
     */
    public String determineAnimalType(String species) {
        if (species == null) {
            return "unknown";
        }

        String lowerSpecies = species.toLowerCase();

        // Simple matching for YOLO classes
        return switch (lowerSpecies) {
            case "bird" -> "bird";
            // Common mammals in YOLO
            case "cat", "dog", "squirrel" -> "mammal";
            // People --> this is just for testing
            case "person" -> "human";
            // Default to the original classification or unknown
            default -> "unknown";
        };
    }

    // ==================== Circuit Breaker Fallback Methods ====================

    @SuppressWarnings("unused")
    private Map<String, Object> callPreprocessingServiceFallback(File imageFile, String animalType, Exception ex) {
        log.warn("AI preprocessing service unavailable for {}: {}. Skipping AI classification.",
                animalType, ex.getMessage());
        return null;
    }
}