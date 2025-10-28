package org.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

/**
 * JPA entity representing AI-generated summaries of weather and bird activity data.
 * Stores comprehensive analysis combining weather conditions, bird detection patterns,
 * and environmental insights to provide meaningful context for biodiversity monitoring.
 * <p>
 * This entity captures AI-powered interpretations of collected sensor data,
 * including weather patterns, bird activity trends, and predictive insights.
 * Each summary represents a snapshot in time with actionable information for
 * both casual users and citizen science contributors.
 * </p>
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "ai_summaries")
public class AISummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** AI-generated comprehensive analysis text */
    @Column(columnDefinition = "TEXT", name = "analysis_text")
    private String analysisText;

    /** Data confidence score (0.0 to 1.0) */
    @Column(name = "data_confidence")
    private Float dataConfidence;

    /** JSON string containing source data summary */
    @Column(columnDefinition = "TEXT", name = "source_data_summary")
    private String sourceDataSummary;

    /** JSON string containing analysis metadata */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /** Timestamp when the summary was generated */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Timestamp when data was last updated */
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    /** Brief weather condition summary */
    @Column(name = "weather_condition")
    private String weatherCondition;

    /** Predicted peak bird activity time */
    @Column(name = "peak_activity_time")
    private String peakActivityTime;

    /** Expected number of species for the day */
    @Column(name = "expected_species")
    private Integer expectedSpecies;

    /** Current accuracy percentage of AI predictions */
    @Column(name = "accuracy_percentage")
    private Float accuracyPercentage;

    /** Number of bird detections in the last period */
    @Column(name = "recent_bird_count")
    private Integer recentBirdCount;

    /** Most commonly detected species */
    @Column(name = "dominant_species")
    private String dominantSpecies;

    /** Temperature range for the summary period */
    @Column(name = "temperature_range")
    private String temperatureRange;

    /** Humidity range for the summary period */
    @Column(name = "humidity_range")
    private String humidityRange;

    /** Atmospheric pressure range for the summary period */
    @Column(name = "pressure_range")
    private String pressureRange;

    /** Wind conditions range description */
    @Column(name = "wind_condition_range")
    private String windConditionRange;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        lastUpdated = LocalDateTime.now();

        log.debug("AISummary entity created - Confidence: {}, Analysis length: {}",
                dataConfidence, analysisText != null ? analysisText.length() : 0);
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();

        log.debug("AISummary entity updated - ID: {}", id);
    }
}