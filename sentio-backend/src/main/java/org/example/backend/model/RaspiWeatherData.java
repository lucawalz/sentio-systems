package org.example.backend.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * JPA entity representing real-time weather sensor measurements.
 * Stores environmental data collected from IoT weather monitoring devices.
 * <p>
 * This entity captures essential weather parameters including temperature, humidity,
 * atmospheric pressure, and light sensor readings (lux and UV index). The data is
 * timestamped for temporal analysis and statistical processing. It serves as the
 * primary data model for real-time weather monitoring and historical data analysis.
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
public class RaspiWeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Temperature measurement in Celsius */
    private Float temperature;

    /** Relative humidity as percentage (0-100) */
    private Float humidity;

    /** Atmospheric pressure in hectopascals (hPa) */
    private Float pressure;

    /** Light intensity measurement in lux units */
    private Float lux;

    /** UV index measurement (0-11+ scale) */
    private Float uvi;

    /** Timestamp when the measurement was recorded by the sensor */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime timestamp;

    /**
     * JPA lifecycle callback executed before entity persistence.
     * Automatically sets timestamp if not provided by the sensor data.
     */
    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
            log.debug("Auto-assigned timestamp to WeatherData: {}", this.timestamp);
        }
    }
}