package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.HistoricalWeatherDTO;
import org.example.backend.mapper.HistoricalWeatherMapper;
import org.example.backend.model.HistoricalWeather;
import org.example.backend.service.IHistoricalWeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for historical weather data management and retrieval.
 * Provides endpoints for fetching historical weather by date ranges and update
 * operations.
 * Integrates with OpenMeteo Archive API for past weather data.
 */
@RestController
@RequestMapping("/api/historical")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Historical Weather", description = "API for retrieving and managing historical weather data")
public class HistoricalWeatherController {

        private static final Logger logger = LoggerFactory.getLogger(HistoricalWeatherController.class);

        private final IHistoricalWeatherService historicalWeatherService;
        private final HistoricalWeatherMapper historicalWeatherMapper;

        /**
         * Retrieves historical weather data for the current user's location.
         * Fetches data for key intervals: 3 days ago, 1 week ago, 1 month ago, 3 months
         * ago, 1 year ago.
         * 
         * @return List of historical weather data for current location
         */
        @Operation(summary = "Get historical weather for current location", description = "Retrieves historical weather data for the current user's location at key intervals")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved historical weather data", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = HistoricalWeatherDTO.class))))
        })
        @GetMapping("/current-location")
        public ResponseEntity<List<HistoricalWeatherDTO>> getHistoricalWeatherForCurrentLocation() {
                logger.info("Retrieving historical weather for current location");
                List<HistoricalWeather> historicalWeather = historicalWeatherService
                                .getHistoricalWeatherForCurrentLocation();
                List<HistoricalWeatherDTO> dtos = historicalWeatherMapper.toDTOList(historicalWeather);
                logger.debug("Retrieved {} historical weather records for current location", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves historical weather data within a specified date range.
         * 
         * @param startDate Beginning of the date range (inclusive)
         * @param endDate   End of the date range (inclusive)
         * @return List of historical weather data within the specified range
         */
        @Operation(summary = "Get historical weather for date range", description = "Retrieves historical weather data within a specified date range")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved historical weather for date range", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = HistoricalWeatherDTO.class))))
        })
        @GetMapping("/date-range")
        public ResponseEntity<List<HistoricalWeatherDTO>> getHistoricalWeatherForDateRange(
                        @Parameter(description = "Beginning of the date range (inclusive, ISO format: yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @Parameter(description = "End of the date range (inclusive, ISO format: yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                logger.info("Retrieving historical weather for date range: {} to {}", startDate, endDate);
                List<HistoricalWeather> historicalWeather = historicalWeatherService
                                .getHistoricalWeatherForDateRange(startDate, endDate);
                List<HistoricalWeatherDTO> dtos = historicalWeatherMapper.toDTOList(historicalWeather);
                logger.debug("Retrieved {} historical weather records for date range", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves historical weather data for a specific date.
         * 
         * @param date The target date for historical weather
         * @return Historical weather data for the specified date
         */
        @Operation(summary = "Get historical weather for specific date", description = "Retrieves historical weather data for a specific date")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved historical weather for date", content = @Content(mediaType = "application/json", schema = @Schema(implementation = HistoricalWeatherDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Historical weather not found for date", content = @Content)
        })
        @GetMapping("/date/{date}")
        public ResponseEntity<HistoricalWeatherDTO> getHistoricalWeatherForDate(
                        @Parameter(description = "Target date for historical weather (ISO format: yyyy-MM-dd)") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                logger.info("Retrieving historical weather for date: {}", date);
                HistoricalWeather historicalWeather = historicalWeatherService.getHistoricalWeatherForDate(date);
                if (historicalWeather != null) {
                        HistoricalWeatherDTO dto = historicalWeatherMapper.toDTO(historicalWeather);
                        logger.debug("Found historical weather for date {}", date);
                        return ResponseEntity.ok(dto);
                }
                logger.debug("No historical weather found for date {}", date);
                return ResponseEntity.notFound().build();
        }

        /**
         * Triggers update of historical weather data for the current user's location.
         * 
         * @return Success message
         */
        @Operation(summary = "Update historical weather data", description = "Triggers update of historical weather data for the current user's location")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated historical weather data", content = @Content(mediaType = "text/plain")),
                        @ApiResponse(responseCode = "500", description = "Failed to update historical weather data", content = @Content(mediaType = "text/plain"))
        })
        @PostMapping("/update")
        public ResponseEntity<String> updateHistoricalWeatherForAllLocations() {
                logger.info("Initiating historical weather update for all device locations");
                try {
                        historicalWeatherService.updateHistoricalWeatherForAllDeviceLocations();
                        logger.info("Successfully updated historical weather for all device locations");
                        return ResponseEntity.ok("Historical weather data updated successfully for all devices");
                } catch (Exception e) {
                        logger.error("Failed to update historical weather for all device locations", e);
                        return ResponseEntity.internalServerError().body("Failed to update historical weather data");
                }
        }

        /**
         * Retrieves a list of cities that have available historical weather data.
         * 
         * @return List of distinct city names with historical weather data
         */
        @Operation(summary = "Get available cities with historical weather", description = "Retrieves a list of cities that have available historical weather data")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cities", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class))))
        })
        @GetMapping("/cities")
        public ResponseEntity<List<String>> getAvailableCitiesWithHistoricalWeather() {
                logger.info("Retrieving available cities with historical weather");
                List<String> cities = historicalWeatherService.getAvailableCitiesWithHistoricalWeather();
                logger.debug("Found {} cities with available historical weather", cities.size());
                return ResponseEntity.ok(cities);
        }

        /**
         * Retrieves information about the last historical weather update.
         * 
         * @return Map containing last update information
         */
        @Operation(summary = "Get last update information", description = "Retrieves information about the last historical weather update")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved last update information", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/last-update")
        public ResponseEntity<Map<String, Object>> getLastUpdateInfo() {
                logger.info("Retrieving last historical weather update information");
                try {
                        // Get the most recent historical weather record (3 days ago, which should be
                        // most recent)
                        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
                        HistoricalWeather latestHistorical = historicalWeatherService
                                        .getHistoricalWeatherForDate(threeDaysAgo);

                        Map<String, Object> response = new HashMap<>();

                        if (latestHistorical != null) {
                                response.put("lastUpdated", latestHistorical.getUpdatedAt());
                                response.put("createdAt", latestHistorical.getCreatedAt());
                                response.put("weatherDate", latestHistorical.getWeatherDate());
                                logger.debug("Found latest historical weather from {}",
                                                latestHistorical.getUpdatedAt());
                        } else {
                                response.put("lastUpdated", null);
                                response.put("createdAt", null);
                                response.put("weatherDate", null);
                                logger.debug("No recent historical weather found");
                        }

                        response.put("hasRecentData", latestHistorical != null &&
                                        latestHistorical.getUpdatedAt()
                                                        .isAfter(LocalDate.now().minusWeeks(1).atStartOfDay()));

                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        logger.error("Error retrieving last historical weather update information", e);
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Unable to fetch last update info");
                        errorResponse.put("lastUpdated", null);
                        return ResponseEntity.ok(errorResponse);
                }
        }

        /**
         * Performs cleanup of outdated historical weather data.
         * 
         * @return Success message
         */
        @Operation(summary = "Cleanup old historical weather", description = "Performs cleanup of outdated historical weather data")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully cleaned up old historical weather", content = @Content(mediaType = "text/plain")),
                        @ApiResponse(responseCode = "500", description = "Failed to cleanup old historical weather", content = @Content(mediaType = "text/plain"))
        })
        @DeleteMapping("/cleanup")
        public ResponseEntity<String> cleanupOldHistoricalWeather() {
                logger.info("Initiating cleanup of old historical weather");
                try {
                        historicalWeatherService.cleanupOldHistoricalWeather();
                        logger.info("Successfully completed cleanup of old historical weather");
                        return ResponseEntity.ok("Old historical weather cleaned up successfully");
                } catch (Exception e) {
                        logger.error("Failed to cleanup old historical weather", e);
                        return ResponseEntity.internalServerError().body("Failed to cleanup old historical weather");
                }
        }

        /**
         * Retrieves key historical dates comparison data for a specific device.
         * Returns weather for 3 days ago, 2 weeks ago, 1 month ago, and 3 months ago.
         * 
         * @param deviceId Optional device ID to filter by specific device
         * @return Map with labeled historical weather data
         */
        @Operation(summary = "Get historical weather comparison", description = "Retrieves key historical dates comparison data for analysis and comparison")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved historical comparison data", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/comparison")
        public ResponseEntity<Map<String, Object>> getHistoricalComparison(
                        @RequestParam(required = false) String deviceId) {
                logger.info("Retrieving historical weather comparison data for device: {}", deviceId);

                Map<String, Object> comparison = new HashMap<>();
                LocalDate now = LocalDate.now();

                // Define historical comparison points
                Map<String, LocalDate> comparisonDates = new HashMap<>();
                comparisonDates.put("threeDaysAgo", now.minusDays(3));
                comparisonDates.put("twoWeeksAgo", now.minusWeeks(2));
                comparisonDates.put("oneMonthAgo", now.minusMonths(1));
                comparisonDates.put("threeMonthsAgo", now.minusMonths(3));
                comparisonDates.put("oneYearAgo", now.minusYears(1));

                // Fetch historical data for each comparison point
                for (Map.Entry<String, LocalDate> entry : comparisonDates.entrySet()) {
                        HistoricalWeather historical = historicalWeatherService
                                        .getHistoricalWeatherForDate(entry.getValue(), deviceId);
                        if (historical != null) {
                                comparison.put(entry.getKey(), historicalWeatherMapper.toDTO(historical));
                        } else {
                                comparison.put(entry.getKey(), null);
                        }
                }

                logger.debug("Retrieved historical comparison data for {} periods (device: {})", comparison.size(),
                                deviceId);
                return ResponseEntity.ok(comparison);
        }
}