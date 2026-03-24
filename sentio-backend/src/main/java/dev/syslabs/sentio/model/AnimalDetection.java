package dev.syslabs.sentio.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * JPA entity representing animal detection results from computer vision
 * systems.
 * Unified entity that stores both initial detection and AI classification
 * results.
 * Supports multiple animal types including birds, mammals, and other wildlife.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "animal_detections", indexes = {
        @Index(name = "idx_detection_device", columnList = "device_id"),
        @Index(name = "idx_detection_device_time", columnList = "device_id, timestamp DESC")
})
public class AnimalDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Primary species identification (updated by AI classification) */
    @Column(nullable = false)
    private String species;

    /** Animal type category (e.g., "bird", "mammal", "reptile", etc.) */
    @Column(nullable = false, name = "animal_type")
    private String animalType;

    /** Confidence score for the primary species (updated by AI classification) */
    @Column(nullable = false)
    private float confidence;

    /** Alternate species classifications from AI (JSON format) */
    @Column(name = "alternate_species", columnDefinition = "TEXT")
    private String alternateSpecies;

    /** Original species from initial detection (before AI classification) */
    @Column(name = "original_species")
    private String originalSpecies;

    /** Original confidence from initial detection */
    @Column(name = "original_confidence")
    private Float originalConfidence;

    /** Bounding box coordinates */
    @Column(nullable = false)
    private float x;

    @Column(nullable = false)
    private float y;

    @Column(nullable = false)
    private float width;

    @Column(nullable = false)
    private float height;

    /** YOLO model class identifier */
    @Column(name = "class_id")
    private Integer classId;

    /** URL path to the detection image file */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Timestamp when the detection occurred */
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Device and location information */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(length = 200)
    private String location;

    @Column(name = "trigger_reason", length = 50)
    private String triggerReason;

    /** Processing timestamps */
    @Column(name = "processed_at")
    private LocalDateTime processedAt = LocalDateTime.now();

    @Column(name = "ai_classified_at")
    private LocalDateTime aiClassifiedAt;

    /** AI classification status */
    @Column(name = "ai_processed")
    private boolean aiProcessed = false;

    /**
     * Utility method to check if this is a bird detection
     */
    @Transient
    public boolean isBird() {
        return "bird".equalsIgnoreCase(animalType);
    }

    /**
     * Utility method to check if this is a mammal detection
     */
    @Transient
    public boolean isMammal() {
        return "mammal".equalsIgnoreCase(animalType);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (timestamp == null) {
            timestamp = now;
        }
        if (processedAt == null) {
            processedAt = now;
        }
        log.debug("AnimalDetection entity created - Type: {}, Species: {}, Confidence: {:.3f}, Device: {}",
                this.animalType, this.species, this.confidence, this.deviceId);
    }

    @PreUpdate
    protected void onUpdate() {
        log.debug("AnimalDetection entity updated - ID: {}, Type: {}, Species: {}, AI Processed: {}",
                this.id, this.animalType, this.species, this.aiProcessed);
    }
}