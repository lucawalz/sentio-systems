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
import org.example.backend.dto.RadarMetadataDTO;
import org.example.backend.dto.WeatherAlertDTO;
import org.example.backend.mapper.WeatherAlertMapper;
import org.example.backend.model.WeatherAlert;
import org.example.backend.service.IBrightSkyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for weather alert management and retrieval.
 * Provides endpoints for fetching alerts by location, severity, and managing
 * alert updates.
 * Integrates with BrightSky API for German weather alert data.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Weather Alerts", description = "API for retrieving and managing weather alerts from BrightSky")
public class WeatherAlertController {

        private static final Logger logger = LoggerFactory.getLogger(WeatherAlertController.class);

        private final IBrightSkyService brightSkyService;
        private final WeatherAlertMapper weatherAlertMapper;

        /**
         * Retrieves weather alerts for the current user's device location.
         * Returns 404 with helpful message if user has no registered devices.
         * 
         * @param lang Language preference (de for German, en for English, default: en)
         * @return List of weather alerts for device location
         */
        @Operation(summary = "Get alerts for current location", description = "Retrieves weather alerts for the current user's device location. Requires at least one registered device.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved alerts for device location", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherAlertDTO.class)))),
                        @ApiResponse(responseCode = "404", description = "No devices registered - register a device first", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/current-location")
        public ResponseEntity<?> getCurrentLocationAlerts(
                        @Parameter(description = "Language preference (de for German, en for English)") @RequestParam(defaultValue = "en") String lang) {
                logger.info("Retrieving alerts for current user's device location with language: {}", lang);

                boolean preferGerman = "de".equalsIgnoreCase(lang);
                List<WeatherAlert> alerts = brightSkyService.getAlertsForCurrentLocation();

                if (alerts.isEmpty()) {
                        logger.debug("No alerts or no device locations available");
                }

                List<WeatherAlertDTO> dtos = weatherAlertMapper.toDTOList(alerts, preferGerman);
                logger.debug("Retrieved {} alerts for device location", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves all currently active weather alerts (not expired).
         * 
         * @param lang Language preference (de for German, en for English, default: en)
         * @return List of active weather alerts
         */
        @Operation(summary = "Get active weather alerts", description = "Retrieves all currently active weather alerts (not expired)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved active alerts", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherAlertDTO.class))))
        })
        @GetMapping("/active")
        public ResponseEntity<List<WeatherAlertDTO>> getActiveAlerts(
                        @Parameter(description = "Language preference (de for German, en for English)") @RequestParam(defaultValue = "en") String lang) {
                logger.info("Retrieving active weather alerts with language: {}", lang);

                boolean preferGerman = "de".equalsIgnoreCase(lang);
                List<WeatherAlert> alerts = brightSkyService.getActiveAlerts();
                List<WeatherAlertDTO> dtos = weatherAlertMapper.toDTOList(alerts, preferGerman);

                logger.debug("Retrieved {} active alerts", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves weather alerts by warn cell ID.
         * 
         * @param warnCellId Municipality warn cell ID
         * @param lang       Language preference (de for German, en for English,
         *                   default: en)
         * @return List of weather alerts for the specified warn cell
         */
        @Operation(summary = "Get alerts by warn cell ID", description = "Retrieves weather alerts by municipality warn cell ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved alerts for warn cell", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherAlertDTO.class))))
        })
        @GetMapping("/warn-cell/{warnCellId}")
        public ResponseEntity<List<WeatherAlertDTO>> getAlertsByWarnCellId(
                        @Parameter(description = "Municipality warn cell ID") @PathVariable Long warnCellId,
                        @Parameter(description = "Language preference (de for German, en for English)") @RequestParam(defaultValue = "en") String lang) {
                logger.info("Retrieving alerts for warn cell ID: {} with language: {}", warnCellId, lang);

                boolean preferGerman = "de".equalsIgnoreCase(lang);
                List<WeatherAlert> alerts = brightSkyService.getAlertsByWarnCellId(warnCellId);
                List<WeatherAlertDTO> dtos = weatherAlertMapper.toDTOList(alerts, preferGerman);

                logger.debug("Retrieved {} alerts for warn cell {}", dtos.size(), warnCellId);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves weather alerts by city name.
         * 
         * @param city City name
         * @param lang Language preference (de for German, en for English, default: en)
         * @return List of weather alerts for the specified city
         */
        @Operation(summary = "Get alerts by city", description = "Retrieves weather alerts by city name")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved alerts for city", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherAlertDTO.class))))
        })
        @GetMapping("/city/{city}")
        public ResponseEntity<List<WeatherAlertDTO>> getAlertsByCity(
                        @Parameter(description = "City name") @PathVariable String city,
                        @Parameter(description = "Language preference (de for German, en for English)") @RequestParam(defaultValue = "en") String lang) {
                logger.info("Retrieving alerts for city: {} with language: {}", city, lang);

                boolean preferGerman = "de".equalsIgnoreCase(lang);
                List<WeatherAlert> alerts = brightSkyService.getAlertsByCity(city);
                List<WeatherAlertDTO> dtos = weatherAlertMapper.toDTOList(alerts, preferGerman);

                logger.debug("Retrieved {} alerts for city {}", dtos.size(), city);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves weather alerts by severity level.
         * 
         * @param severity Severity level (minor, moderate, severe, extreme)
         * @param lang     Language preference (de for German, en for English, default:
         *                 en)
         * @return List of weather alerts with the specified severity
         */
        @Operation(summary = "Get alerts by severity", description = "Retrieves weather alerts by severity level")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved alerts for severity level", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherAlertDTO.class))))
        })
        @GetMapping("/severity/{severity}")
        public ResponseEntity<List<WeatherAlertDTO>> getAlertsBySeverity(
                        @Parameter(description = "Severity level (minor, moderate, severe, extreme)") @PathVariable String severity,
                        @Parameter(description = "Language preference (de for German, en for English)") @RequestParam(defaultValue = "en") String lang) {
                logger.info("Retrieving alerts for severity: {} with language: {}", severity, lang);

                boolean preferGerman = "de".equalsIgnoreCase(lang);
                List<WeatherAlert> alerts = brightSkyService.getAlertsBySeverity(severity);
                List<WeatherAlertDTO> dtos = weatherAlertMapper.toDTOList(alerts, preferGerman);

                logger.debug("Retrieved {} alerts for severity {}", dtos.size(), severity);
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves currently active alerts for a specific location.
         * 
         * @param city       City name
         * @param warnCellId Warn cell ID (optional)
         * @param lang       Language preference (de for German, en for English,
         *                   default: en)
         * @return List of active alerts for the location
         */
        @Operation(summary = "Get active alerts for location", description = "Retrieves currently active alerts for a specific location")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved active alerts for location", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherAlertDTO.class))))
        })
        @GetMapping("/location")
        public ResponseEntity<List<WeatherAlertDTO>> getActiveAlertsForLocation(
                        @Parameter(description = "City name") @RequestParam String city,
                        @Parameter(description = "Warn cell ID (optional)") @RequestParam(required = false) Long warnCellId,
                        @Parameter(description = "Language preference (de for German, en for English)") @RequestParam(defaultValue = "en") String lang) {
                logger.info("Retrieving active alerts for location: {}, warn cell: {} with language: {}",
                                city, warnCellId, lang);

                boolean preferGerman = "de".equalsIgnoreCase(lang);
                List<WeatherAlert> alerts = brightSkyService.getActiveAlertsForLocation(city, warnCellId);
                List<WeatherAlertDTO> dtos = weatherAlertMapper.toDTOList(alerts, preferGerman);

                logger.debug("Retrieved {} active alerts for location", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Retrieves recent weather alerts (last 24 hours).
         * 
         * @param lang Language preference (de for German, en for English, default: en)
         * @return List of recent weather alerts
         */
        @Operation(summary = "Get recent weather alerts", description = "Retrieves recent weather alerts (last 24 hours)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved recent alerts", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = WeatherAlertDTO.class))))
        })
        @GetMapping("/recent")
        public ResponseEntity<List<WeatherAlertDTO>> getRecentAlerts(
                        @Parameter(description = "Language preference (de for German, en for English)") @RequestParam(defaultValue = "en") String lang) {
                logger.info("Retrieving recent alerts with language: {}", lang);

                boolean preferGerman = "de".equalsIgnoreCase(lang);
                List<WeatherAlert> alerts = brightSkyService.getRecentAlerts();
                List<WeatherAlertDTO> dtos = weatherAlertMapper.toDTOList(alerts, preferGerman);

                logger.debug("Retrieved {} recent alerts", dtos.size());
                return ResponseEntity.ok(dtos);
        }

        /**
         * Triggers update of weather alerts for the current user's location.
         * 
         * @return Success message
         */
        @Operation(summary = "Update weather alerts", description = "Triggers update of weather alerts for the current user's location")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated weather alerts", content = @Content(mediaType = "text/plain")),
                        @ApiResponse(responseCode = "500", description = "Failed to update weather alerts", content = @Content(mediaType = "text/plain"))
        })
        @PostMapping("/update")
        public ResponseEntity<String> updateAlertsForCurrentLocation() {
                logger.info("Initiating alert update for current location");
                try {
                        brightSkyService.updateAlertsForCurrentLocation();
                        logger.info("Successfully updated alerts for current location");
                        return ResponseEntity.ok("Weather alerts updated successfully");
                } catch (Exception e) {
                        logger.error("Failed to update alerts for current location", e);
                        return ResponseEntity.internalServerError().body("Failed to update weather alerts");
                }
        }

        /**
         * Performs cleanup of expired weather alerts.
         * 
         * @return Success message
         */
        @Operation(summary = "Cleanup expired alerts", description = "Performs cleanup of expired weather alerts")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully cleaned up expired alerts", content = @Content(mediaType = "text/plain")),
                        @ApiResponse(responseCode = "500", description = "Failed to cleanup expired alerts", content = @Content(mediaType = "text/plain"))
        })
        @DeleteMapping("/cleanup")
        public ResponseEntity<String> cleanupExpiredAlerts() {
                logger.info("Initiating cleanup of expired alerts");
                try {
                        brightSkyService.cleanupExpiredAlerts();
                        logger.info("Successfully completed cleanup of expired alerts");
                        return ResponseEntity.ok("Expired alerts cleaned up successfully");
                } catch (Exception e) {
                        logger.error("Failed to cleanup expired alerts", e);
                        return ResponseEntity.internalServerError().body("Failed to cleanup expired alerts");
                }
        }

        /**
         * Retrieves radar data endpoint configuration for the current location.
         * 
         * @param distance Distance in meters to each side of location (default: 200000)
         * @param format   Data format (compressed, bytes, plain - default: compressed)
         * @return Radar endpoint configuration
         */
        @Operation(summary = "Get radar endpoint configuration", description = "Retrieves radar data endpoint configuration for the current location")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully generated radar endpoint configuration", content = @Content(mediaType = "application/json"))
        })
        @GetMapping("/radar/endpoint")
        public ResponseEntity<Map<String, Object>> getRadarEndpoint(
                        @Parameter(description = "Distance in meters to each side of location (default: 200000)") @RequestParam(required = false) Integer distance,
                        @Parameter(description = "Data format (compressed, bytes, plain - default: compressed)") @RequestParam(defaultValue = "compressed") String format) {
                logger.info("Generating radar endpoint for current location");

                try {
                        String endpointUrl = brightSkyService.getRadarEndpointUrlForCurrentLocation(distance, format);

                        Map<String, Object> response = new HashMap<>();
                        if (endpointUrl != null) {
                                response.put("radarEndpoint", endpointUrl);
                                response.put("format", format);
                                response.put("distance", distance != null ? distance : 200000);
                                response.put("documentation", "https://brightsky.dev/docs/#radar");
                                response.put("note", "Fetch this URL directly from frontend to get radar data. " +
                                                "Data is base64-encoded and may need decompression based on format.");
                                logger.debug("Generated radar endpoint: {}", endpointUrl);
                        } else {
                                response.put("error", "Unable to determine current location");
                                response.put("radarEndpoint", null);
                        }

                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        logger.error("Error generating radar endpoint", e);
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Unable to generate radar endpoint");
                        return ResponseEntity.ok(errorResponse);
                }
        }

        /**
         * Retrieves a list of cities that currently have active weather alerts.
         * 
         * @return List of city names with active alerts
         */
        @Operation(summary = "Get cities with active alerts", description = "Retrieves a list of cities that currently have active weather alerts")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved cities with active alerts", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = String.class))))
        })
        @GetMapping("/cities")
        public ResponseEntity<List<String>> getCitiesWithActiveAlerts() {
                logger.info("Retrieving cities with active alerts");
                List<String> cities = brightSkyService.getCitiesWithActiveAlerts();
                logger.debug("Found {} cities with active alerts", cities.size());
                return ResponseEntity.ok(cities);
        }

        /**
         * Fetches fresh radar data and stores metadata for AI analysis.
         * 
         * @param distance Distance in meters (default: 100000)
         * @return Radar metadata including precipitation statistics
         */
        @Operation(summary = "Fetch and store radar metadata", description = "Fetches fresh radar data from BrightSky and stores metadata for AI analysis. Requires at least one registered device.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully fetched and stored radar metadata", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RadarMetadataDTO.class))),
                        @ApiResponse(responseCode = "404", description = "No devices registered - register a device first", content = @Content(mediaType = "application/json"))
        })
        @PostMapping("/radar/fetch")
        public ResponseEntity<?> fetchRadarMetadata(
                        @Parameter(description = "Distance in meters (default: 100000)") @RequestParam(required = false) Integer distance) {
                logger.info("Fetching and storing radar metadata with distance: {}", distance);

                RadarMetadataDTO metadata = brightSkyService.fetchAndStoreRadarMetadataForCurrentLocation(distance);
                if (metadata != null) {
                        logger.info("Successfully fetched radar metadata: coverage={}%",
                                        String.format("%.1f", metadata.getCoveragePercent()));
                        return ResponseEntity.ok(metadata);
                }
                logger.debug("No device locations available for radar metadata fetch");
                return ResponseEntity.status(404).body(java.util.Map.of(
                                "error", "NO_DEVICES_REGISTERED",
                                "message",
                                "You don't have any registered devices. Register a device to fetch radar data for your location.",
                                "action",
                                "Call GET /api/devices/has-any to check device registration, then POST /api/devices/register to register a device."));
        }

        /**
         * Gets the latest stored radar metadata.
         * 
         * @return Latest radar metadata or 404 if none available
         */
        @Operation(summary = "Get latest radar metadata", description = "Retrieves the most recent stored radar metadata")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved latest radar metadata", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RadarMetadataDTO.class))),
                        @ApiResponse(responseCode = "404", description = "No radar metadata available", content = @Content)
        })
        @GetMapping("/radar/latest")
        public ResponseEntity<RadarMetadataDTO> getLatestRadarMetadata() {
                logger.info("Retrieving latest radar metadata");

                return brightSkyService.getLatestRadarMetadata()
                                .map(metadata -> {
                                        RadarMetadataDTO dto = RadarMetadataDTO.builder()
                                                        .timestamp(metadata.getTimestamp())
                                                        .source(metadata.getSource())
                                                        .latitude(metadata.getLatitude())
                                                        .longitude(metadata.getLongitude())
                                                        .distance(metadata.getDistance())
                                                        .precipitationMin(metadata.getPrecipitationMin())
                                                        .precipitationMax(metadata.getPrecipitationMax())
                                                        .precipitationAvg(metadata.getPrecipitationAvg())
                                                        .coveragePercent(metadata.getCoveragePercent())
                                                        .significantRainCells(metadata.getSignificantRainCells())
                                                        .totalCells(metadata.getTotalCells())
                                                        .directApiUrl(brightSkyService.getRadarEndpointUrl(
                                                                        metadata.getLatitude(), metadata.getLongitude(),
                                                                        metadata.getDistance(), "compressed"))
                                                        .createdAt(metadata.getCreatedAt())
                                                        .hasActivePrecipitation(metadata.getCoveragePercent() > 0)
                                                        .build();
                                        return ResponseEntity.ok(dto);
                                })
                                .orElseGet(() -> {
                                        logger.debug("No radar metadata available");
                                        return ResponseEntity.notFound().build();
                                });
        }
}