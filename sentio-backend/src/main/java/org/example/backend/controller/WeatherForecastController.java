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
import org.example.backend.dto.WeatherForecastDTO;
import org.example.backend.mapper.WeatherForecastMapper;
import org.example.backend.model.WeatherForecast;
import org.example.backend.service.IWeatherForecastService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for weather forecast management and retrieval.
 * Provides endpoints for fetching forecasts by location, date ranges, and
 * update operations.
 * Integrates with Open-Meteo API for current location-based weather data.
 */
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Weather Forecast", description = "API for retrieving and managing weather forecast data from Open-Meteo")
public class WeatherForecastController {

        private static final Logger logger = LoggerFactory.getLogger(WeatherForecastController.class);

        private final IWeatherForecastService weatherForecastService;
        private final WeatherForecastMapper weatherForecastMapper;

        /**
         * Retrieves weather forecast data for the current user's device location.
         * Returns 404 with helpful message if user has no registered devices.
         * 
         * @return List of weather forecast data for user's device location
         */
        @Operation(summary = "Get forecast for current location", description = "Retrieves weather forecast data for the current user's device location. Requires at least one registered device.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved forecast for device location", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherForecastDTO.class)))),
                        @ApiResponse(responseCode = "404", description = "No devices registered - register a device first", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/current-location")
        public ResponseEntity<?> getCurrentLocationForecast() {
                logger.info("Retrieving forecast for current user's device location");
                List<WeatherForecast> forecasts = weatherForecastService.getForecastForCurrentLocation();

                if (forecasts.isEmpty()) {
                        logger.debug("No device locations available, returning 404");
                        return ResponseEntity.status(404).body(java.util.Map.of(
                                        "error", "NO_DEVICES_REGISTERED",
                                        "message",
                                        "You don't have any registered devices. Register a device to see weather data for your location.",
                                        "action",
                                        "Call GET /api/devices/has-any to check device registration, then POST /api/devices/register to register a device."));
                }

                List<WeatherForecastDTO> dtos = weatherForecastMapper.toDTOList(forecasts);
                logger.debug("Retrieved {} forecasts for device location", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves all upcoming weather forecasts starting from today.
         * 
         * @return List of upcoming weather forecasts
         */
        @Operation(summary = "Get upcoming forecasts", description = "Retrieves all upcoming weather forecasts starting from today")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved upcoming forecasts", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherForecastDTO.class))))
        })
        @GetMapping("/upcoming")
        public ResponseEntity<List<WeatherForecastDTO>> getUpcomingForecasts() {
                logger.info("Retrieving upcoming weather forecasts");
                List<WeatherForecast> forecasts = weatherForecastService.getUpcomingForecasts();
                List<WeatherForecastDTO> dtos = weatherForecastMapper.toDTOList(forecasts);
                logger.debug("Retrieved {} upcoming forecasts", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves weather forecasts within a specified date range.
         * 
         * @param startDate Beginning of the date range (inclusive)
         * @param endDate   End of the date range (inclusive)
         * @return List of weather forecasts within the specified range
         */
        @Operation(summary = "Get forecasts for date range", description = "Retrieves weather forecasts within a specified date range")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved forecasts for date range", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherForecastDTO.class))))
        })
        @GetMapping("/date-range")
        public ResponseEntity<List<WeatherForecastDTO>> getForecastsForDateRange(
                        @Parameter(description = "Beginning of the date range (inclusive, ISO format: yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @Parameter(description = "End of the date range (inclusive, ISO format: yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                logger.info("Retrieving forecasts for date range: {} to {}", startDate, endDate);
                List<WeatherForecast> forecasts = weatherForecastService.getForecastsForDateRange(startDate, endDate);
                List<WeatherForecastDTO> dtos = weatherForecastMapper.toDTOList(forecasts);
                logger.debug("Retrieved {} forecasts for date range", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves all weather forecasts for a specific date.
         * 
         * @param date The target date for forecasts
         * @return List of weather forecasts for the specified date
         */
        @Operation(summary = "Get forecasts for specific date", description = "Retrieves all weather forecasts for a specific date")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved forecasts for date", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherForecastDTO.class))))
        })
        @GetMapping("/date/{date}")
        public ResponseEntity<List<WeatherForecastDTO>> getForecastsForDate(
                        @Parameter(description = "Target date for forecasts (ISO format: yyyy-MM-dd)") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                logger.info("Retrieving forecasts for date: {}", date);
                List<WeatherForecast> forecasts = weatherForecastService.getForecastsForDate(date);
                List<WeatherForecastDTO> dtos = weatherForecastMapper.toDTOList(forecasts);
                logger.debug("Retrieved {} forecasts for date {}", dtos.size(), date);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves the most recent forecast for a specific date.
         * 
         * @param date The target date for forecast
         * @return Most recently created weather forecast for the specified date
         */
        @Operation(summary = "Get latest forecast for specific date", description = "Retrieves the most recent forecast for a specific date")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved latest forecast for date", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WeatherForecastDTO.class))),
                        @ApiResponse(responseCode = "404", description = "No forecast found for date", content = @Content)
        })
        @GetMapping("/latest/{date}")
        public ResponseEntity<WeatherForecastDTO> getLatestForecastForDate(
                        @Parameter(description = "Target date for forecast (ISO format: yyyy-MM-dd)") @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
                logger.info("Retrieving latest forecast for date: {}", date);
                WeatherForecast forecast = weatherForecastService.getLatestForecastForDate(date);
                if (forecast != null) {
                        WeatherForecastDTO dto = weatherForecastMapper.toDTO(forecast);
                        logger.debug("Found latest forecast for date {}", date);
                        return ResponseEntity.ok(dto);
                }
                logger.debug("No forecast found for date {}", date);
                return ResponseEntity.notFound().build();
        }

        /**
         * Retrieves weather forecasts created within the specified number of hours.
         * 
         * @param hours Number of hours to look back from current time (default: 24)
         * @return List of recent weather forecasts
         */
        @Operation(summary = "Get recent forecasts", description = "Retrieves weather forecasts created within the specified number of hours")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved recent forecasts", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherForecastDTO.class))))
        })
        @GetMapping("/recent")
        public ResponseEntity<List<WeatherForecastDTO>> getRecentForecasts(
                        @Parameter(description = "Number of hours to look back from current time (default: 24)") @RequestParam(defaultValue = "24") int hours) {
                logger.info("Retrieving forecasts from the last {} hours", hours);
                List<WeatherForecast> forecasts = weatherForecastService.getRecentForecasts(hours);
                List<WeatherForecastDTO> dtos = weatherForecastMapper.toDTOList(forecasts);
                logger.debug("Retrieved {} recent forecasts", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Triggers update of weather forecasts for the current user's location.
         * 
         * @return Success message
         */
        @Operation(summary = "Update weather forecasts", description = "Triggers update of weather forecasts for the current user's location")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated weather forecasts", content = @Content(mediaType = "text/plain")),
                        @ApiResponse(responseCode = "500", description = "Failed to update weather forecasts", content = @Content(mediaType = "text/plain"))
        })
        @PostMapping("/update")
        public ResponseEntity<String> updateForecastsForCurrentLocation() {
                logger.info("Initiating forecast update for current location");
                try {
                        weatherForecastService.updateForecastsForCurrentLocation();
                        logger.info("Successfully updated forecasts for current location");
                        return ResponseEntity.ok("Weather forecasts updated successfully");
                } catch (Exception e) {
                        logger.error("Failed to update forecasts for current location", e);
                        return ResponseEntity.internalServerError().body("Failed to update weather forecasts");
                }
        }

        /**
         * Retrieves a list of cities that have available weather forecasts.
         * 
         * @return List of distinct city names with forecast data
         */
        @Operation(summary = "Get available cities with forecasts", description = "Retrieves a list of cities that have available weather forecasts")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cities", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class))))
        })
        @GetMapping("/cities")
        public ResponseEntity<List<String>> getAvailableCities() {
                logger.info("Retrieving available cities with forecasts");
                List<String> cities = weatherForecastService.getAvailableCities();
                logger.debug("Found {} cities with available forecasts", cities.size());
                return ResponseEntity.ok(cities);
        }

        /**
         * Retrieves information about the last forecast update and next expected
         * update.
         * 
         * @return Map containing last update timestamps and next update estimate
         */
        @Operation(summary = "Get last update information", description = "Retrieves information about the last forecast update and next expected update")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved last update information", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/last-update")
        public ResponseEntity<Map<String, Object>> getLastUpdateInfo() {
                logger.info("Retrieving last update information");
                try {
                        // Get the most recent forecast (today's date)
                        LocalDate today = LocalDate.now();
                        WeatherForecast latestForecast = weatherForecastService.getLatestForecastForDate(today);

                        Map<String, Object> response = new HashMap<>();

                        if (latestForecast != null) {
                                response.put("lastUpdated", latestForecast.getUpdatedAt());
                                response.put("createdAt", latestForecast.getCreatedAt());
                                response.put("forecastDate", latestForecast.getForecastDate());
                                logger.debug("Found latest forecast from {}", latestForecast.getUpdatedAt());
                        } else {
                                response.put("lastUpdated", null);
                                response.put("createdAt", null);
                                response.put("forecastDate", null);
                                logger.debug("No recent forecast found");
                        }

                        // Calculate next expected update time (OpenWeatherMap updates every 3 hours)
                        LocalDateTime now = LocalDateTime.now();
                        int currentHour = now.getHour();
                        int nextUpdateHour = ((currentHour / 3) + 1) * 3;
                        if (nextUpdateHour >= 24) {
                                nextUpdateHour = 0;
                        }

                        LocalDateTime nextUpdate = now.withHour(nextUpdateHour).withMinute(5).withSecond(0).withNano(0);
                        if (nextUpdate.isBefore(now)) {
                                nextUpdate = nextUpdate.plusDays(1);
                        }

                        response.put("nextUpdateEstimate", nextUpdate);
                        response.put("hasRecentData", latestForecast != null &&
                                        latestForecast.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(6)));

                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        logger.error("Error retrieving last update information", e);
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Unable to fetch last update info");
                        errorResponse.put("lastUpdated", null);
                        return ResponseEntity.ok(errorResponse);
                }
        }

        /**
         * Performs cleanup of outdated weather forecast data.
         * 
         * @return Success message
         */
        @Operation(summary = "Cleanup old forecasts", description = "Performs cleanup of outdated weather forecast data")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully cleaned up old forecasts", content = @Content(mediaType = "text/plain")),
                        @ApiResponse(responseCode = "500", description = "Failed to cleanup old forecasts", content = @Content(mediaType = "text/plain"))
        })
        @DeleteMapping("/cleanup")
        public ResponseEntity<String> cleanupOldForecasts() {
                logger.info("Initiating cleanup of old forecasts");
                try {
                        weatherForecastService.cleanupOldForecasts();
                        logger.info("Successfully completed cleanup of old forecasts");
                        return ResponseEntity.ok("Old forecasts cleaned up successfully");
                } catch (Exception e) {
                        logger.error("Failed to cleanup old forecasts", e);
                        return ResponseEntity.internalServerError().body("Failed to cleanup old forecasts");
                }
        }
}