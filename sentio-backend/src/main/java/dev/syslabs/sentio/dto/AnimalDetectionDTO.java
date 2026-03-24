package dev.syslabs.sentio.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for unified animal detection and classification data.
 * Supports multiple animal types including birds, mammals, and other wildlife.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Data Transfer Object for unified animal detection and classification data")
public class AnimalDetectionDTO {

    @Schema(description = "Unique identifier for the animal detection", example = "1")
    private Long id;

    @Schema(description = "Detected or classified animal species", example = "Robin")
    @NotBlank(message = "Species is required")
    @Size(max = 255, message = "Species must be at most 255 characters")
    private String species;

    @Schema(description = "Category of the detected animal", example = "bird", allowableValues = {"bird", "mammal", "reptile", "amphibian", "insect", "other"})
    @NotBlank(message = "Animal type is required")
    private String animalType;

    @Schema(description = "Confidence score of the detection/classification", example = "0.95", minimum = "0.0", maximum = "1.0")
    @DecimalMin(value = "0.0", message = "Confidence must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Confidence must be between 0 and 1")
    private float confidence;

    @Schema(description = "List of alternate species classifications from AI processing")
    @Valid
    private List<AlternateSpeciesDTO> alternateSpecies;

    @Schema(description = "Original species detected before AI processing", example = "Unknown Bird")
    private String originalSpecies;

    @Schema(description = "Original confidence score before AI processing", example = "0.75", minimum = "0.0", maximum = "1.0")
    private Float originalConfidence;

    // Bounding box coordinates
    @Schema(description = "X coordinate of the bounding box (left edge)", example = "100.5")
    private float x;

    @Schema(description = "Y coordinate of the bounding box (top edge)", example = "50.2")
    private float y;

    @Schema(description = "Width of the bounding box", example = "200.0")
    private float width;

    @Schema(description = "Height of the bounding box", example = "150.0")
    private float height;

    @Schema(description = "Classification ID from the detection model", example = "16")
    private Integer classId;

    @Schema(description = "URL path to the detection image", example = "/images/detections/detection_123.jpg")
    private String imageUrl;

    @Schema(description = "Timestamp when the detection occurred", example = "2024-01-15T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    // Device information
    @Schema(description = "Identifier of the detection device/camera", example = "camera_001")
    private String deviceId;

    @Schema(description = "Location where the detection occurred", example = "Garden Feeder")
    private String location;

    @Schema(description = "Reason that triggered the detection", example = "motion_detected", allowableValues = {"motion_detected", "scheduled_capture", "manual_trigger"})
    private String triggerReason;

    @Schema(description = "Timestamp when the detection was processed", example = "2024-01-15T14:30:05")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    @Schema(description = "Timestamp when AI classification was completed", example = "2024-01-15T14:30:10")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime aiClassifiedAt;

    @Schema(description = "Whether the detection has been processed by AI classification", example = "true")
    private boolean aiProcessed;

    /**
     * DTO for alternate species classifications
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Alternate species classification with confidence score")
    public static class AlternateSpeciesDTO {

        @Schema(description = "Alternate species name", example = "Blue Jay")
        @NotBlank(message = "Alternate species is required")
        @Size(max = 255, message = "Alternate species must be at most 255 characters")
        private String species;

        @Schema(description = "Confidence score for this alternate classification", example = "0.82", minimum = "0.0", maximum = "1.0")
        @DecimalMin(value = "0.0", message = "Alternate confidence must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "Alternate confidence must be between 0 and 1")
        private float confidence;
    }
}