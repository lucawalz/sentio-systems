package org.example.backend.service.classification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AnimalDetectionDTO;
import org.example.backend.model.AnimalDetection;
import org.example.backend.repository.AnimalDetectionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Component responsible for transforming classifier responses into persisted detection updates.
 * Handles bird-specific and generic classifier payload structures, including alternate species.
 * <p>
 * This processor encapsulates response parsing and domain update rules so the
 * orchestration service remains focused on workflow coordination.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class AnimalClassificationResponseProcessor {

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
    private final ObjectMapper objectMapper;

    public AnimalClassificationResponseProcessor(
            AnimalDetectionRepository animalDetectionRepository,
            ObjectMapper objectMapper) {
        this.animalDetectionRepository = animalDetectionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Marks a detection as handled without AI classification if no classifier is available.
     *
     * @param detection Detection entity to update
     */
    public void markAsProcessedWithoutAIClassification(AnimalDetection detection) {
        detection.setAiProcessed(false);
        detection.setProcessedAt(LocalDateTime.now());
        animalDetectionRepository.save(detection);

        log.info(
                "Detection ID {} marked as processed without AI classification - keeping original classification: {} ({})",
                detection.getId(), detection.getSpecies(), detection.getAnimalType());
    }

    /**
     * Processes classifier response payload and updates detection state accordingly.
     * Applies animal-type-specific parsing for bird and generic models.
     *
     * @param detection    Detection entity being enriched
     * @param responseBody Raw response payload from preprocessing/classification API
     */
    public void processClassificationResponse(AnimalDetection detection, Map<String, Object> responseBody) {
        try {
            if ("bird".equalsIgnoreCase(detection.getAnimalType())) {
                processBirdClassificationResponse(detection, responseBody);
                return;
            }
            processGenericClassificationResponse(detection, responseBody);
        } catch (Exception e) {
            log.error("Error processing classification response for detection ID {}: {}",
                    detection.getId(), e.getMessage(), e);
        }
    }

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

    private void updateGenericClassificationResults(AnimalDetection detection, Map<String, Object> classificationMap)
            throws JsonProcessingException {

        storeOriginalValues(detection);

        String topSpecies = (String) classificationMap.get(TOP_SPECIES_KEY);
        Double topConfidence = ((Number) classificationMap.get(TOP_CONFIDENCE_KEY)).doubleValue();

        String displaySpecies = extractCommonName(topSpecies);

        if (!isValidAnimalSpecies(displaySpecies)) {
            log.debug("AI returned invalid species '{}', searching for better candidate...", displaySpecies);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> predictionsList = (List<Map<String, Object>>) classificationMap
                    .get(PREDICTIONS_KEY);

            boolean foundBetterCandidate = false;

            if (predictionsList != null) {
                for (Map<String, Object> pred : predictionsList) {
                    String predSpecies = (String) pred.get(SPECIES_KEY);
                    String predCommon = extractCommonName(predSpecies);

                    if (isValidAnimalSpecies(predCommon)) {
                        displaySpecies = predCommon;
                        Double conf = ((Number) pred.get(CONFIDENCE_KEY)).doubleValue();
                        topConfidence = conf;
                        topSpecies = predSpecies;
                        foundBetterCandidate = true;
                        log.info("Promoted candidate '{}' ({:.2f}) over invalid species", displaySpecies,
                                topConfidence);
                        break;
                    }
                }
            }

            if (!foundBetterCandidate) {
                log.warn("No valid species found in AI predictions. Falling back to original: {}",
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

    private boolean isValidAnimalSpecies(String species) {
        if (species == null || species.isEmpty()) {
            return false;
        }

        String lower = species.toLowerCase().trim();

        return switch (lower) {
            case "blank", "vehicle", "unknown", "error", "none", "n/a" -> false;
            default -> true;
        };
    }

    @SuppressWarnings("unchecked")
    private void processAlternateSpecies(AnimalDetection detection, Map<String, Object> classificationMap,
            String currentTopSpecies)
            throws JsonProcessingException {

        List<Map<String, Object>> predictionsList = (List<Map<String, Object>>) classificationMap.get(PREDICTIONS_KEY);
        if (predictionsList == null || predictionsList.isEmpty()) {
            return;
        }

        List<AnimalDetectionDTO.AlternateSpeciesDTO> alternateSpeciesList = new ArrayList<>();

        int count = 0;
        for (Map<String, Object> pred : predictionsList) {
            if (count >= MAX_ALTERNATE_SPECIES) {
                break;
            }

            String species = (String) pred.get(SPECIES_KEY);
            Double confidence = ((Number) pred.get(CONFIDENCE_KEY)).doubleValue();

            String displayAlternate = extractCommonName(species);

            if (displayAlternate.equalsIgnoreCase(currentTopSpecies) || "blank".equalsIgnoreCase(displayAlternate)) {
                continue;
            }

            alternateSpeciesList.add(new AnimalDetectionDTO.AlternateSpeciesDTO(
                    displayAlternate, confidence.floatValue()));
            count++;
        }

        if (!alternateSpeciesList.isEmpty()) {
            String alternateSpeciesJson = objectMapper.writeValueAsString(alternateSpeciesList);
            detection.setAlternateSpecies(alternateSpeciesJson);
        }
    }

    private void storeOriginalValues(AnimalDetection detection) {
        if (detection.getOriginalSpecies() == null) {
            detection.setOriginalSpecies(detection.getSpecies());
            detection.setOriginalConfidence(detection.getConfidence());
        }
    }

    private void markAsAiProcessed(AnimalDetection detection) {
        detection.setAiProcessed(true);
        detection.setAiClassifiedAt(LocalDateTime.now());
        animalDetectionRepository.save(detection);
    }

    private void updateBirdDetectionWithClassification(AnimalDetection detection, Map<String, Object> classificationMap)
            throws JsonProcessingException {

        storeOriginalValues(detection);

        String topSpecies = (String) classificationMap.get(TOP_SPECIES_KEY);
        Double topConfidence = ((Number) classificationMap.get(TOP_CONFIDENCE_KEY)).doubleValue();

        detection.setSpecies(topSpecies);
        detection.setConfidence(topConfidence.floatValue());

        processAlternateSpecies(detection, classificationMap, topSpecies);
        markAsAiProcessed(detection);

        log.info("Successfully updated detection ID {} - Type: {}, Primary: {} ({:.2f})",
                detection.getId(), detection.getAnimalType(), topSpecies, topConfidence);
    }

    private void handleNoAnimalDetected(AnimalDetection detection) {
        log.warn("AI classification found no {} in image for detection ID {}",
                detection.getAnimalType(), detection.getId());

        detection.setAiProcessed(true);
        detection.setAiClassifiedAt(LocalDateTime.now());
        animalDetectionRepository.save(detection);
    }

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
}