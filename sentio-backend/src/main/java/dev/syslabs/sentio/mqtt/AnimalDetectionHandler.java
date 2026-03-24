package dev.syslabs.sentio.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.service.AnimalDetectionCommandService;
import dev.syslabs.sentio.service.IAnimalClassifierService;
import dev.syslabs.sentio.service.ImageStorageService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * MQTT handler for processing animal detection events from Raspberry Pi devices.
 *
 * <p>Expected JSON payload structure:
 * <pre>
 * {
 *   "device_id": "string",
 *   "animal_type": "string",
 *   "species": "string" (optional),
 *   "confidence": float,
 *   "image": "base64 string",
 *   "timestamp": "ISO8601 string"
 * }
 * </pre>
 * </p>
 *
 * Handles multiple animal types and creates initial detection records for AI classification.
 * Uses CQRS CommandService for write operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AnimalDetectionHandler {

    private final AnimalDetectionCommandService commandService;
    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;
    private final IAnimalClassifierService animalClassifierService;

    public void processAnimalDetection(String payload) {
        log.debug("Starting processing of animal detection payload");

        try {
            log.info("Processing animal detection payload from MQTT");
            JsonNode rootNode = objectMapper.readTree(payload);

            // Extract metadata
            String timestampStr = rootNode.get("timestamp").asText();
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            int detectionCount = rootNode.get("detection_count").asInt();
            String triggerReason = rootNode.get("trigger_reason").asText();
            String deviceId = rootNode.get("device_id").asText();
            String location = rootNode.get("location").asText();

            // Process image
            String imageBase64 = rootNode.get("image_data").asText();
            String imageFormat = rootNode.get("image_format").asText();
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            String imageUrl = imageStorageService.saveImage(imageBytes, imageFormat, timestamp, deviceId);

            log.info("Received animal detection from device: {} at location: {} with {} detections",
                    deviceId, location, detectionCount);

            // Process detections
            JsonNode detectionsArray = rootNode.get("detections");
            if (detectionsArray.isArray()) {
                for (JsonNode detectionNode : detectionsArray) {
                    AnimalDetection animalDetection = createAnimalDetection(
                            detectionNode, imageUrl, timestamp, deviceId, location, triggerReason);

                    AnimalDetection savedDetection = commandService.saveAnimalDetection(animalDetection);
                    log.info("Saved initial detection with ID: {} - Type: {}, will be classified by AI",
                            savedDetection.getId(), savedDetection.getAnimalType());

                    // Trigger AI classification
                    animalClassifierService.classifyAndUpdate(savedDetection);
                }
            }

        } catch (Exception e) {
            log.error("Error processing animal detection payload: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates an animal detection record based on MQTT payload data.
     * Determines animal type from species classification and sets initial values.
     */
    private AnimalDetection createAnimalDetection(JsonNode detectionNode, String imageUrl,
            LocalDateTime timestamp, String deviceId,
            String location, String triggerReason) {
        AnimalDetection detection = new AnimalDetection();

        // Extract bounding box
        JsonNode bboxNode = detectionNode.get("bbox");
        if (bboxNode.isArray() && bboxNode.size() >= 4) {
            float x1 = bboxNode.get(0).floatValue();
            float y1 = bboxNode.get(1).floatValue();
            float x2 = bboxNode.get(2).floatValue();
            float y2 = bboxNode.get(3).floatValue();

            detection.setX(x1);
            detection.setY(y1);
            detection.setWidth(x2 - x1);
            detection.setHeight(y2 - y1);
        }

        // Set initial detection data
        detection.setConfidence(detectionNode.get("confidence").floatValue());
        detection.setClassId(detectionNode.get("class_id").asInt());

        // Get species from detection node or use generic classification
        String initialSpecies = detectionNode.has("species") ? detectionNode.get("species").asText() : "unknown";
        detection.setSpecies(initialSpecies);

        // Determine animal type based on initial classification or device configuration
        String animalType = determineAnimalType(initialSpecies, detectionNode);
        detection.setAnimalType(animalType);

        // Set metadata
        detection.setImageUrl(imageUrl);
        detection.setTimestamp(timestamp);
        detection.setDeviceId(deviceId);
        detection.setLocation(location);
        detection.setTriggerReason(triggerReason);
        detection.setAiProcessed(false); // Will be updated after AI classification

        log.debug("Created initial detection - Type: {}, Species: {}, Confidence: {:.2f}, awaiting AI classification",
                animalType, initialSpecies, detection.getConfidence());

        return detection;
    }

    /**
     * Determines the animal type based on the species and detection context.
     * This method can be enhanced as you add more sophisticated detection logic.
     */
    private String determineAnimalType(String species, JsonNode detectionNode) {
        // If the detection node explicitly specifies animal type, use it
        if (detectionNode.has("animal_type")) {
            return detectionNode.get("animal_type").asText();
        }

        // Use the classifier service to determine type from species
        return animalClassifierService.determineAnimalType(species);
    }
}