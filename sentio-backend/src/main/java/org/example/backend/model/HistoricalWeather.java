package org.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing historical weather data from OpenMeteo Historical
 * API.
 * Stores actual weather conditions that occurred on specific dates for
 * comparison
 * with forecasts and trend analysis.
 * <p>
 * This entity contains weather conditions (temperature, precipitation, wind),
 * sunrise/sunset times, UV index, and other meteorological data for past dates.
 * It implements automatic timestamping and ensures unique records per date and
 * location.
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
@Table(name = "historical_weather", indexes = {
        @Index(name = "idx_historical_device_date", columnList = "device_id, weatherDate", unique = true),
        @Index(name = "idx_historical_date", columnList = "weatherDate"),
        @Index(name = "idx_historical_device", columnList = "device_id")
})
public class HistoricalWeather {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Device ID that this historical data belongs to (for user data isolation) */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    /** Date when the weather conditions occurred */
    @Column(nullable = false)
    private LocalDate weatherDate;

    /** Timestamp when this record was created in the database */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Timestamp when this record was last updated */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Weather condition fields from OpenMeteo Historical API
    /** Weather code following WMO standards */
    private Integer weatherCode;

    /** Maximum temperature in Celsius */
    private Float maxTemperature;

    /** Minimum temperature in Celsius */
    private Float minTemperature;

    /** Sunrise time as LocalDateTime */
    private LocalDateTime sunrise;

    /** Sunset time as LocalDateTime */
    private LocalDateTime sunset;

    /** Daylight duration in seconds */
    private Float daylightDuration;

    /** Sunshine duration in seconds */
    private Float sunshineDuration;

    /** Maximum UV index for the day */
    private Float uvIndexMax;

    /** Total precipitation in mm */
    private Float precipitationSum;

    /** Number of hours with precipitation */
    private Float precipitationHours;

    /** Maximum wind speed at 10m in m/s */
    private Float windSpeedMax;

    /** Dominant wind direction in degrees (0-360) */
    private Float windDirectionDominant;

    // Derived weather information
    /** Human-readable weather description */
    private String description;

    /** Main weather category (e.g., "Rain", "Clear", "Clouds") */
    private String weatherMain;

    /** Weather condition icon code */
    private String icon;

    // Location fields
    /** City name for the weather location */
    private String city;

    /** Country name for the weather location */
    private String country;

    /** Geographic latitude coordinate */
    private Float latitude;

    /** Geographic longitude coordinate */
    private Float longitude;

    /** IP address used for location detection */
    private String ipAddress;

    /** Formatted location string (e.g., "New York, USA") */
    private String detectedLocation;

    /**
     * JPA lifecycle callback executed before entity persistence.
     * Automatically sets creation and update timestamps.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        log.debug("HistoricalWeather entity created with timestamps: {}", now);
    }

    /**
     * JPA lifecycle callback executed before entity updates.
     * Automatically updates the modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        log.debug("HistoricalWeather entity updated at: {}", this.updatedAt);
    }
}