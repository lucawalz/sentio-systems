package org.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * JPA entity representing weather alert data from BrightSky API.
 * Stores weather warnings and alerts for specific locations with full
 * multilingual support.
 * <p>
 * This entity includes alert metadata (severity, urgency, certainty), timing
 * information,
 * and location details. It implements automatic timestamping for creation and
 * update tracking,
 * with a unique constraint on alert_id to prevent duplicates.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "weather_alerts", indexes = {
        @Index(name = "idx_alert_unique", columnList = "alert_id, device_id", unique = true),
        @Index(name = "idx_alert_location", columnList = "warn_cell_id, city"),
        @Index(name = "idx_alert_effective", columnList = "effective"),
        @Index(name = "idx_alert_expires", columnList = "expires"),
        @Index(name = "idx_alert_device", columnList = "device_id"),
        @Index(name = "idx_alert_device_active", columnList = "device_id, expires")
})
public class WeatherAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Device ID that this alert belongs to (for user data isolation) */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    /** Bright Sky-internal ID for this alert */
    private Integer brightSkyId;

    /** Unique CAP message identifier */
    @Column(nullable = false)
    private String alertId;

    /** Alert status (actual, test) */
    private String status;

    /** Alert issue time */
    @Column(nullable = false)
    private LocalDateTime effective;

    /** Expected event begin time */
    private LocalDateTime onset;

    /** Expected event end time */
    private LocalDateTime expires;

    /** Alert category (met, health, null) */
    private String category;

    /** Code denoting type of action recommended */
    private String responseType;

    /** Alert time frame (immediate, future, null) */
    private String urgency;

    /** Alert severity (minor, moderate, severe, extreme, null) */
    private String severity;

    /** Alert certainty (observed, likely, null) */
    private String certainty;

    /** DWD event code */
    private Integer eventCode;

    /** Label for DWD event code (English) */
    private String eventEn;

    /** Label for DWD event code (German) */
    private String eventDe;

    /** Alert headline (English) */
    @Column(columnDefinition = "TEXT")
    private String headlineEn;

    /** Alert headline (German) */
    @Column(columnDefinition = "TEXT")
    private String headlineDe;

    /** Alert description (English) */
    @Column(columnDefinition = "TEXT")
    private String descriptionEn;

    /** Alert description (German) */
    @Column(columnDefinition = "TEXT")
    private String descriptionDe;

    /** Additional instructions and safety advice (English) */
    @Column(columnDefinition = "TEXT")
    private String instructionEn;

    /** Additional instructions and safety advice (German) */
    @Column(columnDefinition = "TEXT")
    private String instructionDe;

    // Location information
    /** Municipality warn cell ID */
    private Long warnCellId;

    /** Municipality name */
    private String name;

    /** Shortened municipality name */
    private String nameShort;

    /** District name */
    private String district;

    /** Federal state name */
    private String state;

    /** Shortened federal state name */
    private String stateShort;

    /** City name for the alert location */
    private String city;

    /** Country name for the alert location */
    private String country;

    /** Geographic latitude coordinate */
    private Float latitude;

    /** Geographic longitude coordinate */
    private Float longitude;

    /** Timestamp when this record was created in the database */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Timestamp when this record was last updated */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback executed before entity persistence.
     * Automatically sets creation and update timestamps.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        log.debug("WeatherAlert entity created with timestamps: {}", now);
    }

    /**
     * JPA lifecycle callback executed before entity updates.
     * Automatically updates the modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        log.debug("WeatherAlert entity updated at: {}", this.updatedAt);
    }

    /**
     * Checks if the alert is currently active based on effective and expiry times.
     * 
     * @return true if alert is currently active, false otherwise
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return (effective == null || effective.isBefore(now) || effective.isEqual(now)) &&
                (expires == null || expires.isAfter(now));
    }

    /**
     * Gets the appropriate headline based on language preference.
     * 
     * @param preferGerman true for German, false for English
     * @return localized headline
     */
    public String getLocalizedHeadline(boolean preferGerman) {
        return preferGerman && headlineDe != null ? headlineDe : headlineEn;
    }

    /**
     * Gets the appropriate description based on language preference.
     * 
     * @param preferGerman true for German, false for English
     * @return localized description
     */
    public String getLocalizedDescription(boolean preferGerman) {
        return preferGerman && descriptionDe != null ? descriptionDe : descriptionEn;
    }
}