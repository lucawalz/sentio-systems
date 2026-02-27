package org.example.backend.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.model.RaspiWeatherData;
import org.example.backend.service.RaspiWeatherDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for current weather data management.
 * Provides endpoints for retrieving and storing real-time weather measurements
 * and weather statistics from IoT devices and sensors.
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Validated
@Tag(name = "Weather Data", description = "API for retrieving and managing real-time weather data from IoT devices")
public class RaspiWeatherController {

    private static final Logger logger = LoggerFactory.getLogger(RaspiWeatherController.class);

    private final RaspiWeatherDataService raspiWeatherDataService;

    /**
     * Retrieves the most recent weather data reading.
     * @return Latest weather data entry
     */
    @Operation(summary = "Get latest weather data",
            description = "Retrieves the most recent weather data reading from IoT sensors")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved latest weather data",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RaspiWeatherData.class))),
            @ApiResponse(responseCode = "404", description = "No weather data found",
                    content = @Content)
    })
    @GetMapping("/latest")
    public ResponseEntity<RaspiWeatherData> getLatestWeather() {
        logger.info("Retrieving latest weather data");
        RaspiWeatherData latest = raspiWeatherDataService.getLatestWeatherData();
        if (latest != null) {
            logger.debug("Retrieved latest weather data from {}", latest.getTimestamp());
            return ResponseEntity.ok(latest);
        }
        logger.debug("No weather data found");
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves recent weather data entries.
     * @return List of recent weather data readings
     */
    @Operation(summary = "Get recent weather data",
            description = "Retrieves recent weather data entries from IoT sensors")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved recent weather data",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RaspiWeatherData.class))))
    })
    @GetMapping("/recent")
    public ResponseEntity<List<RaspiWeatherData>> getRecentWeather() {
        logger.info("Retrieving recent weather data");
        List<RaspiWeatherData> recent = raspiWeatherDataService.getRecentWeatherData();
        logger.debug("Retrieved {} recent weather readings", recent.size());
        return ResponseEntity.ok(recent);
    }

    /**
     * Retrieves all available weather data entries.
     * @return List of all weather data readings
     */
    @Operation(summary = "Get all weather data",
            description = "Retrieves all available weather data entries from the database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all weather data",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = RaspiWeatherData.class))))
    })
    @GetMapping("/all")
    public ResponseEntity<List<RaspiWeatherData>> getAllWeather() {
        logger.info("Retrieving all weather data");
        List<RaspiWeatherData> all = raspiWeatherDataService.getAllWeatherData();
        logger.debug("Retrieved {} total weather readings", all.size());
        return ResponseEntity.ok(all);
    }

    /**
     * Adds new weather data reading to the system.
     * @param raspiWeatherData Weather data to be stored
     * @return Saved weather data with generated ID and timestamp
     */
    @Operation(summary = "Add new weather data",
            description = "Adds new weather data reading to the system from IoT devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added weather data",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RaspiWeatherData.class))),
            @ApiResponse(responseCode = "400", description = "Invalid weather data provided",
                    content = @Content)
    })
    @PostMapping
        public ResponseEntity<RaspiWeatherData> addWeatherData(@Valid @RequestBody RaspiWeatherData raspiWeatherData) {
        logger.info("Adding new weather data reading");
        // Ensure timestamp is set if not provided
        if (raspiWeatherData.getTimestamp() == null) {
            raspiWeatherData.setTimestamp(java.time.LocalDateTime.now());
            logger.debug("Set timestamp to current time for weather data");
        }
        RaspiWeatherData saved = raspiWeatherDataService.saveWeatherData(raspiWeatherData);
        logger.debug("Saved weather data with ID {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    /**
     * Retrieves comprehensive weather statistics and analytics.
     * @return Weather statistics including averages and totals
     */
    @Operation(summary = "Get weather statistics",
            description = "Retrieves comprehensive weather statistics and analytics including averages and totals")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved weather statistics",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = WeatherStats.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<WeatherStats> getWeatherStats() {
        logger.info("Retrieving weather statistics");
        WeatherStats stats = raspiWeatherDataService.getWeatherStats();
        logger.debug("Retrieved weather statistics with {} total readings", stats.getTotalReadings());
        return ResponseEntity.ok(stats);
    }

    /**
     * Data Transfer Object for weather statistics and analytics.
     * Contains aggregated weather data including totals and averages.
     */
    public static class WeatherStats {
        /** Total number of weather readings */
        private Long totalReadings;
        /** Most recent weather reading */
        private RaspiWeatherData latest;
        /** Average temperature across all readings */
        private Double avgTemperature;
        /** Average humidity across all readings */
        private Double avgHumidity;
        /** Average pressure across all readings */
        private Double avgPressure;

        public WeatherStats(Long totalReadings, RaspiWeatherData latest,
                            Double avgTemperature, Double avgHumidity, Double avgPressure) {
            this.totalReadings = totalReadings;
            this.latest = latest;
            this.avgTemperature = avgTemperature;
            this.avgHumidity = avgHumidity;
            this.avgPressure = avgPressure;
        }

        public Long getTotalReadings() { return totalReadings; }
        public void setTotalReadings(Long totalReadings) { this.totalReadings = totalReadings; }

        public RaspiWeatherData getLatest() { return latest; }
        public void setLatest(RaspiWeatherData latest) { this.latest = latest; }

        public Double getAvgTemperature() { return avgTemperature; }
        public void setAvgTemperature(Double avgTemperature) { this.avgTemperature = avgTemperature; }

        public Double getAvgHumidity() { return avgHumidity; }
        public void setAvgHumidity(Double avgHumidity) { this.avgHumidity = avgHumidity; }

        public Double getAvgPressure() { return avgPressure; }
        public void setAvgPressure(Double avgPressure) { this.avgPressure = avgPressure; }
    }
}