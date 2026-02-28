
package org.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing hourly weather forecast data from Open-Meteo API.
 * Stores comprehensive hourly weather predictions with location information and
 * temporal data.
 * <p>
 * This entity is designed for hourly forecasts and includes weather conditions
 * (temperature, humidity, pressure),
 * precipitation data, wind information, and location details. It implements
 * automatic timestamping
 * for creation and update tracking, with a unique constraint on forecast
 * datetime
 * combined with location to prevent duplicates.
 * </p>
 *
 * @author Sentio Team
 * @version 2.0 - Updated for Open-Meteo hourly API
 * @since 1.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "weather_forecasts", indexes = {
        // Include device_id in unique constraint so each device can have its own
        // forecasts
        @Index(name = "idx_forecast_unique", columnList = "forecast_date_time, device_id", unique = true),
        @Index(name = "idx_forecast_device", columnList = "device_id"),
        @Index(name = "idx_forecast_device_date", columnList = "device_id, forecast_date")
})
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Device ID that this forecast belongs to (for user data isolation) */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    /** Date component of the forecast (used for daily grouping and queries) */
    @Column(nullable = false)
    private LocalDate forecastDate;

    /** Complete forecast timestamp including time (hourly precision) */
    @Column(nullable = false)
    private LocalDateTime forecastDateTime;

    /** Timestamp when this record was created in the database */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Timestamp when this record was last updated */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Core temperature fields
    /** Temperature at 2m height in Celsius */
    private Float temperature;

    /** Relative humidity percentage (0-100) */
    private Float humidity;

    /** Apparent temperature (feels like) in Celsius */
    private Float apparentTemperature;

    /** Surface pressure in hPa */
    private Float pressure;

    /** Human-readable weather description */
    private String description;

    /** Main weather category (e.g., "Rain", "Clear", "Clouds") */
    private String weatherMain;

    /** Weather condition icon code */
    private String icon;

    // Wind data
    /** Wind speed at 10m height in m/s */
    private Float windSpeed;

    /** Wind direction at 10m height in degrees (0-360) */
    private Float windDirection;

    /** Wind gusts at 10m height in m/s */
    private Float windGusts;

    /** Cloud coverage percentage (0-100) */
    private Float cloudCover;

    /** Visibility in meters */
    private Float visibility;

    // Precipitation data
    /** Total precipitation in mm */
    private Float precipitation;

    /** Rain volume in mm */
    private Float rain;

    /** Showers volume in mm */
    private Float showers;

    /** Snowfall volume in mm */
    private Float snowfall;

    /** Snow depth in meters */
    private Float snowDepth;

    /** Dew point temperature at 2m height in Celsius */
    private Float dewPoint;

    /** Precipitation probability percentage (0-100) */
    private Float precipitationProbability;

    /** WMO Weather interpretation code */
    private Integer weatherCode;

    // Location fields
    /** City name for the forecast location */
    private String city;

    /** Country name for the forecast location */
    private String country;

    /** Geographic latitude coordinate */
    private Float latitude;

    /** Geographic longitude coordinate */
    private Float longitude;

    // IP-based location tracking
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
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        log.debug("WeatherForecast entity created with timestamps: {}", now);
    }

    /**
     * JPA lifecycle callback executed before entity updates.
     * Automatically updates the modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        log.debug("WeatherForecast entity updated at: {}", this.updatedAt);
    }
}