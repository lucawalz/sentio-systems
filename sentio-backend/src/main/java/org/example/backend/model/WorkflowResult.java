package org.example.backend.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

/**
 * JPA entity representing results from n8n workflow executions.
 * Stores AI-generated content including summaries of weather and bird activity
 * data,
 * as well as AI agent responses to user queries.
 * <p>
 * This entity captures outputs from n8n workflows that use Gemini AI to
 * generate
 * insights, including weather patterns, bird activity trends, and predictive
 * insights.
 * Each result represents a snapshot in time with actionable information for
 * both casual users and citizen science contributors.
 * </p>
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "workflow_results", indexes = {
        @Index(name = "idx_workflow_user_type", columnList = "user_id, workflow_type")
})
public class WorkflowResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Type of workflow that generated this result */
    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_type", nullable = false)
    private WorkflowType workflowType = WorkflowType.WEATHER_SUMMARY;

    /**
     * User ID (Keycloak subject) who owns this result. Null for global summaries.
     */
    @Column(name = "user_id")
    private String userId;

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

    /** Timestamp when the result was generated */
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
        if (workflowType == null) {
            workflowType = WorkflowType.WEATHER_SUMMARY;
        }
        if (lastUpdated == null) {
            lastUpdated = timestamp;
        }

        log.debug("WorkflowResult entity created - Type: {}, Confidence: {}, Analysis length: {}",
                workflowType, dataConfidence, analysisText != null ? analysisText.length() : 0);
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();

        log.debug("WorkflowResult entity updated - ID: {}, Type: {}", id, workflowType);
    }
}
