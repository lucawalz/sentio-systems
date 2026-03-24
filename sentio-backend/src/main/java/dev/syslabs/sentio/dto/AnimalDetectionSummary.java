package dev.syslabs.sentio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for aggregated animal detection statistics.
 * Provides comprehensive analytics and summary data for detection events
 * including temporal patterns, species distribution, and animal type breakdown.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated animal detection statistics and analytics")
public class AnimalDetectionSummary {

    @Schema(description = "Total number of animal detections in the analyzed period", example = "1250")
    private long totalDetections;

    @Schema(description = "Number of unique species detected", example = "15")
    private long uniqueSpecies;

    @Schema(description = "Number of unique animal types detected (birds, mammals, etc.)", example = "3")
    private long uniqueAnimalTypes;

    @Schema(description = "Average confidence score across all detections", example = "0.87", minimum = "0.0", maximum = "1.0")
    private double averageConfidence;

    @Schema(description = "Timestamp of the earliest detection in the period", example = "2024-01-01T08:15:30")
    private LocalDateTime firstDetection;

    @Schema(description = "Timestamp of the most recent detection in the period", example = "2024-01-15T16:45:22")
    private LocalDateTime lastDetection;

    @Schema(description = "Count of detections per species",
            example = "{\"Robin\": 125, \"Blue Jay\": 89, \"Cardinal\": 67, \"Squirrel\": 45}")
    private Map<String, Long> speciesBreakdown;

    @Schema(description = "Count of detections per animal type",
            example = "{\"bird\": 850, \"mammal\": 380, \"other\": 20}")
    private Map<String, Long> animalTypeBreakdown;

    @Schema(description = "Count of detections per device/camera",
            example = "{\"camera_001\": 680, \"camera_002\": 420, \"camera_003\": 150}")
    private Map<String, Long> deviceBreakdown;

    @Schema(description = "Hour of day with highest detection activity", example = "07:00")
    private String mostActiveHour;

    @Schema(description = "Number of detections recorded in the last hour", example = "12")
    private long detectionsInLastHour;

    @Schema(description = "Total number of bird detections (backward compatibility)", example = "850")
    private long birdDetections;

    @Schema(description = "Total number of mammal detections", example = "380")
    private long mammalDetections;

    @Schema(description = "Total number of other animal detections (non-bird, non-mammal)", example = "20")
    private long otherAnimalDetections;
}