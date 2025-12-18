package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.RadarMetadataDTO;
import org.example.backend.dto.WeatherRadarDTO;
import org.example.backend.model.LocationData;
import org.example.backend.model.WeatherAlert;
import org.example.backend.model.WeatherRadarMetadata;
import org.example.backend.repository.WeatherAlertRepository;
import org.example.backend.repository.WeatherRadarMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for integrating with BrightSky API for weather alerts and
 * radar data.
 * Handles fetching alerts from BrightSky API, processing and persisting them.
 * Provides radar data endpoint information for frontend integration.
 * <p>
 * This service integrates with the BrightSky API for German weather data
 * and provides methods for retrieving, updating, and managing weather alerts.
 * Radar data is not persisted due to its large size but endpoint information is
 * provided.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrightSkyService {

    private final WeatherAlertRepository weatherAlertRepository;
    private final WeatherRadarMetadataRepository weatherRadarMetadataRepository;
    private final IpLocationService ipLocationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private volatile boolean isUpdating = false;

    @Value("${brightsky.api.base-url:https://api.brightsky.dev}")
    private String baseUrl;

    /**
     * Retrieves weather alerts for the current user's location based on IP
     * geolocation.
     *
     * @return List of weather alerts for current location, empty list if location
     *         cannot be determined
     */
    public List<WeatherAlert> getAlertsForCurrentLocation() {
        log.debug("Retrieving alerts for current location");
        Optional<LocationData> currentLocation = ipLocationService.getCurrentLocation();
        if (currentLocation.isPresent()) {
            return getAlertsForLocation(currentLocation.get().getLatitude(),
                    currentLocation.get().getLongitude());
        }
        log.warn("Unable to determine current location for alert retrieval");
        return new ArrayList<>();
    }

    /**
     * Fetches and processes weather alerts for a specific geographic location.
     * This method integrates with the BrightSky Alerts API to retrieve current
     * alerts,
     * processes the data, and stores it in the database with intelligent
     * update/insert logic.
     *
     * @param latitude  Geographic latitude coordinate
     * @param longitude Geographic longitude coordinate
     * @return List of processed and persisted weather alerts
     */
    @Transactional
    public List<WeatherAlert> getAlertsForLocation(Float latitude, Float longitude) {
        log.info("Processing weather alerts for location: lat: {}, lon: {}", latitude, longitude);

        try {
            // Clean up expired alerts before fetching new data
            cleanupExpiredAlerts();

            // BrightSky Alerts API URL
            String url = String.format("%s/alerts?lat=%f&lon=%f&tz=Europe/Berlin",
                    baseUrl, latitude, longitude);

            log.info("Fetching weather alerts from BrightSky: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            List<WeatherAlert> processedAlerts = new ArrayList<>();

            // Process alerts array
            JsonNode alertsNode = jsonNode.get("alerts");
            JsonNode locationNode = jsonNode.get("location");

            if (alertsNode != null && alertsNode.isArray()) {
                for (JsonNode alertNode : alertsNode) {
                    WeatherAlert alert = processAlertNode(alertNode, locationNode);
                    if (alert != null) {
                        processedAlerts.add(alert);
                    }
                }
            }

            // Persist all processed alerts
            List<WeatherAlert> savedAlerts = weatherAlertRepository.saveAll(processedAlerts);

            log.info("Successfully processed {} weather alerts for location: lat: {}, lon: {}",
                    savedAlerts.size(), latitude, longitude);

            return savedAlerts;

        } catch (Exception e) {
            log.error("Failed to fetch weather alerts for location: lat: {}, lon: {}", latitude, longitude, e);
            return new ArrayList<>();
        }
    }

    /**
     * Processes a single alert node from the BrightSky API response.
     */
    private WeatherAlert processAlertNode(JsonNode alertNode, JsonNode locationNode) {
        try {
            String alertId = alertNode.get("alert_id").asText();

            // Check if alert already exists
            Optional<WeatherAlert> existingAlert = weatherAlertRepository.findByAlertId(alertId);
            WeatherAlert alert = existingAlert.orElse(new WeatherAlert());

            // Set alert identification
            alert.setAlertId(alertId);
            alert.setBrightSkyId(alertNode.get("id").asInt());
            alert.setStatus(getTextValue(alertNode, "status"));

            // Set timing information
            alert.setEffective(parseDateTime(alertNode, "effective"));
            alert.setOnset(parseDateTime(alertNode, "onset"));
            alert.setExpires(parseDateTime(alertNode, "expires"));

            // Set alert metadata
            alert.setCategory(getTextValue(alertNode, "category"));
            alert.setResponseType(getTextValue(alertNode, "response_type"));
            alert.setUrgency(getTextValue(alertNode, "urgency"));
            alert.setSeverity(getTextValue(alertNode, "severity"));
            alert.setCertainty(getTextValue(alertNode, "certainty"));

            // Set event information
            if (alertNode.has("event_code") && !alertNode.get("event_code").isNull()) {
                alert.setEventCode(alertNode.get("event_code").asInt());
            }
            alert.setEventEn(getTextValue(alertNode, "event_en"));
            alert.setEventDe(getTextValue(alertNode, "event_de"));

            // Set multilingual content
            alert.setHeadlineEn(getTextValue(alertNode, "headline_en"));
            alert.setHeadlineDe(getTextValue(alertNode, "headline_de"));
            alert.setDescriptionEn(getTextValue(alertNode, "description_en"));
            alert.setDescriptionDe(getTextValue(alertNode, "description_de"));
            alert.setInstructionEn(getTextValue(alertNode, "instruction_en"));
            alert.setInstructionDe(getTextValue(alertNode, "instruction_de"));

            // Set location information from location node
            if (locationNode != null) {
                if (locationNode.has("warn_cell_id") && !locationNode.get("warn_cell_id").isNull()) {
                    alert.setWarnCellId(locationNode.get("warn_cell_id").asLong());
                }
                alert.setName(getTextValue(locationNode, "name"));
                alert.setNameShort(getTextValue(locationNode, "name_short"));
                alert.setDistrict(getTextValue(locationNode, "district"));
                alert.setState(getTextValue(locationNode, "state"));
                alert.setStateShort(getTextValue(locationNode, "state_short"));

                // Use name_short as city for consistency with other services
                alert.setCity(getTextValue(locationNode, "name_short"));
                alert.setCountry("Germany"); // BrightSky is Germany-specific
            }

            return alert;

        } catch (Exception e) {
            log.error("Error processing alert node", e);
            return null;
        }
    }

    /**
     * Safely extracts text value from JSON node.
     */
    private String getTextValue(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return null;
    }

    /**
     * Safely parses datetime from JSON node.
     */
    private LocalDateTime parseDateTime(JsonNode node, String fieldName) {
        String dateTimeStr = getTextValue(node, fieldName);
        if (dateTimeStr != null) {
            try {
                // Parse ISO 8601 datetime with offset
                return LocalDateTime.parse(dateTimeStr.substring(0, 19),
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                log.warn("Failed to parse datetime: {}", dateTimeStr);
                return null;
            }
        }
        return null;
    }

    /**
     * Gets all active weather alerts (not expired).
     *
     * @return List of currently active weather alerts
     */
    public List<WeatherAlert> getActiveAlerts() {
        log.debug("Retrieving active weather alerts");
        return weatherAlertRepository.findActiveAlerts(LocalDateTime.now());
    }

    /**
     * Gets weather alerts by warn cell ID.
     *
     * @param warnCellId Municipality warn cell ID
     * @return List of weather alerts for the specified warn cell
     */
    public List<WeatherAlert> getAlertsByWarnCellId(Long warnCellId) {
        log.debug("Retrieving alerts for warn cell ID: {}", warnCellId);
        return weatherAlertRepository.findByWarnCellId(warnCellId);
    }

    /**
     * Gets weather alerts by city name.
     *
     * @param city City name
     * @return List of weather alerts for the specified city
     */
    public List<WeatherAlert> getAlertsByCity(String city) {
        log.debug("Retrieving alerts for city: {}", city);
        return weatherAlertRepository.findByCityIgnoreCase(city);
    }

    /**
     * Gets weather alerts by severity level.
     *
     * @param severity Severity level (minor, moderate, severe, extreme)
     * @return List of weather alerts with the specified severity
     */
    public List<WeatherAlert> getAlertsBySeverity(String severity) {
        log.debug("Retrieving alerts for severity: {}", severity);
        return weatherAlertRepository.findBySeverityOrderByEffectiveDesc(severity);
    }

    /**
     * Gets currently active alerts for a specific location.
     *
     * @param city       City name
     * @param warnCellId Warn cell ID (can be null)
     * @return List of active alerts for the location
     */
    public List<WeatherAlert> getActiveAlertsForLocation(String city, Long warnCellId) {
        log.debug("Retrieving active alerts for location: {}, warn cell: {}", city, warnCellId);
        return weatherAlertRepository.findActiveAlertsForLocation(city, warnCellId, LocalDateTime.now());
    }

    /**
     * Updates weather alerts for the current user's location.
     * Includes automatic cleanup and prevents concurrent updates using a
     * thread-safe flag.
     */
    @Transactional
    public void updateAlertsForCurrentLocation() {
        if (isUpdating) {
            log.warn("Alert update already in progress, skipping concurrent update request");
            return;
        }

        isUpdating = true;
        log.info("Starting alert update for current location");

        try {
            // Update alerts for current location
            Optional<LocationData> currentLocation = ipLocationService.getCurrentLocation();
            if (currentLocation.isPresent()) {
                LocationData location = currentLocation.get();
                getAlertsForLocation(location.getLatitude(), location.getLongitude());
                log.info("Successfully updated alerts for current location: {}, {}",
                        location.getCity(), location.getCountry());
            } else {
                log.warn("Unable to determine current location for alert update");
            }
        } catch (Exception e) {
            log.error("Error occurred during alert update for current location", e);
        } finally {
            isUpdating = false;
        }
    }

    /**
     * Performs cleanup of expired weather alerts.
     */
    @Transactional
    public void cleanupExpiredAlerts() {
        log.info("Starting cleanup of expired weather alerts");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysAgo = now.minusDays(2);

        try {
            // Remove expired alerts
            weatherAlertRepository.deleteExpiredAlerts(now);
            log.debug("Deleted expired alerts (expired before: {})", now);

            // Remove old alerts (older than 2 days)
            weatherAlertRepository.deleteOldAlerts(twoDaysAgo);
            log.debug("Deleted old alerts (created before: {})", twoDaysAgo);

            log.info("Successfully completed cleanup of expired weather alerts");
        } catch (Exception e) {
            log.error("Error occurred during alert cleanup", e);
        }
    }

    /**
     * Provides radar data endpoint configuration for frontend integration.
     * The radar data itself is not stored in the backend due to its large size.
     *
     * @param latitude  Geographic latitude coordinate
     * @param longitude Geographic longitude coordinate
     * @param distance  Distance in meters to each side of location (default:
     *                  200000)
     * @param format    Data format (compressed, bytes, plain - default: compressed)
     * @return Radar endpoint configuration for frontend use
     */
    public String getRadarEndpointUrl(Float latitude, Float longitude, Integer distance, String format) {
        if (distance == null)
            distance = 200000;
        if (format == null)
            format = "compressed";

        String url = String.format("%s/radar?lat=%f&lon=%f&distance=%d&format=%s&tz=Europe/Berlin",
                baseUrl, latitude, longitude, distance, format);

        log.debug("Generated radar endpoint URL: {}", url);
        return url;
    }

    /**
     * Provides radar data endpoint configuration for current location.
     */
    public String getRadarEndpointUrlForCurrentLocation(Integer distance, String format) {
        Optional<LocationData> currentLocation = ipLocationService.getCurrentLocation();
        if (currentLocation.isPresent()) {
            LocationData location = currentLocation.get();
            return getRadarEndpointUrl(location.getLatitude(), location.getLongitude(), distance, format);
        }
        return null;
    }

    /**
     * Gets recent weather alerts (last 24 hours).
     *
     * @return List of recent weather alerts
     */
    public List<WeatherAlert> getRecentAlerts() {
        log.debug("Retrieving recent alerts from last 24 hours");
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        return weatherAlertRepository.findRecentAlerts(yesterday);
    }

    /**
     * Gets list of cities with active alerts.
     *
     * @return List of city names that currently have active alerts
     */
    public List<String> getCitiesWithActiveAlerts() {
        log.debug("Retrieving cities with active alerts");
        return weatherAlertRepository.findDistinctCitiesWithActiveAlerts(LocalDateTime.now());
    }

    // ==================== RADAR METADATA METHODS ====================

    /**
     * Fetches radar data from BrightSky and stores metadata for AI analysis.
     * Only metadata (precipitation stats) is stored, not the raw grid data.
     *
     * @param latitude  Geographic latitude coordinate
     * @param longitude Geographic longitude coordinate
     * @param distance  Distance in meters (default: 100000 for ~100km radius)
     * @return RadarMetadataDTO with statistics and direct API URL
     */
    @Transactional
    public RadarMetadataDTO fetchAndStoreRadarMetadata(Float latitude, Float longitude, Integer distance) {
        if (distance == null)
            distance = 100000;

        log.info("Fetching radar data for lat: {}, lon: {}, distance: {}", latitude, longitude, distance);

        try {
            // Fetch radar data in plain format for easier parsing
            String url = String.format("%s/radar?lat=%f&lon=%f&distance=%d&format=plain&tz=Europe/Berlin",
                    baseUrl, latitude, longitude, distance);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            JsonNode radarArray = jsonNode.get("radar");
            if (radarArray == null || !radarArray.isArray() || radarArray.isEmpty()) {
                log.warn("No radar data returned from BrightSky");
                return null;
            }

            // Get the most recent radar entry (first in array is most recent)
            JsonNode latestRadar = radarArray.get(0);
            JsonNode precipitation = latestRadar.get("precipitation_5");

            if (precipitation == null || !precipitation.isArray()) {
                log.warn("No precipitation data in radar response");
                return null;
            }

            // Calculate statistics from the precipitation grid
            int totalCells = 0;
            int cellsWithPrecip = 0;
            int significantCells = 0;
            float minPrecip = Float.MAX_VALUE;
            float maxPrecip = 0;
            float sumPrecip = 0;

            for (JsonNode row : precipitation) {
                for (JsonNode cell : row) {
                    int value = cell.asInt();
                    float mmPer5min = value / 100.0f; // Convert from 0.01mm units

                    totalCells++;
                    if (value > 0) {
                        cellsWithPrecip++;
                        sumPrecip += mmPer5min;
                        if (mmPer5min < minPrecip)
                            minPrecip = mmPer5min;
                        if (mmPer5min > maxPrecip)
                            maxPrecip = mmPer5min;
                        if (mmPer5min >= 1.0f)
                            significantCells++; // Significant rain threshold
                    }
                }
            }

            // Avoid division by zero
            if (cellsWithPrecip == 0) {
                minPrecip = 0;
            }
            float avgPrecip = cellsWithPrecip > 0 ? sumPrecip / cellsWithPrecip : 0;
            float coveragePercent = totalCells > 0 ? (cellsWithPrecip * 100.0f / totalCells) : 0;

            // Parse timestamp
            String timestampStr = latestRadar.get("timestamp").asText();
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr.substring(0, 19),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Create and save metadata entity
            WeatherRadarMetadata metadata = new WeatherRadarMetadata();
            metadata.setTimestamp(timestamp);
            metadata.setSource(latestRadar.has("source") ? latestRadar.get("source").asText() : null);
            metadata.setLatitude(latitude);
            metadata.setLongitude(longitude);
            metadata.setDistance(distance);
            metadata.setPrecipitationMin(minPrecip);
            metadata.setPrecipitationMax(maxPrecip);
            metadata.setPrecipitationAvg(avgPrecip);
            metadata.setCoveragePercent(coveragePercent);
            metadata.setSignificantRainCells(significantCells);
            metadata.setTotalCells(totalCells);

            // Store geometry and bbox if present
            if (jsonNode.has("geometry")) {
                metadata.setGeometryJson(jsonNode.get("geometry").toString());
            }
            if (jsonNode.has("bbox")) {
                JsonNode bbox = jsonNode.get("bbox");
                metadata.setBboxPixels(String.format("%d,%d,%d,%d",
                        bbox.get(0).asInt(), bbox.get(1).asInt(),
                        bbox.get(2).asInt(), bbox.get(3).asInt()));
            }

            WeatherRadarMetadata savedMetadata = weatherRadarMetadataRepository.save(metadata);
            log.info("Saved radar metadata: coverage={}%, max={}mm, significant cells={}",
                    String.format("%.1f", coveragePercent),
                    String.format("%.2f", maxPrecip),
                    significantCells);

            // Build DTO response
            return RadarMetadataDTO.builder()
                    .timestamp(savedMetadata.getTimestamp())
                    .source(savedMetadata.getSource())
                    .latitude(latitude)
                    .longitude(longitude)
                    .distance(distance)
                    .precipitationMin(savedMetadata.getPrecipitationMin())
                    .precipitationMax(savedMetadata.getPrecipitationMax())
                    .precipitationAvg(savedMetadata.getPrecipitationAvg())
                    .coveragePercent(savedMetadata.getCoveragePercent())
                    .significantRainCells(savedMetadata.getSignificantRainCells())
                    .totalCells(savedMetadata.getTotalCells())
                    .directApiUrl(getRadarEndpointUrl(latitude, longitude, distance, "compressed"))
                    .createdAt(savedMetadata.getCreatedAt())
                    .hasActivePrecipitation(cellsWithPrecip > 0)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch and store radar metadata", e);
            return null;
        }
    }

    /**
     * Fetches and stores radar metadata for current location.
     */
    @Transactional
    public RadarMetadataDTO fetchAndStoreRadarMetadataForCurrentLocation(Integer distance) {
        Optional<LocationData> currentLocation = ipLocationService.getCurrentLocation();
        if (currentLocation.isPresent()) {
            LocationData location = currentLocation.get();
            return fetchAndStoreRadarMetadata(location.getLatitude(), location.getLongitude(), distance);
        }
        log.warn("Unable to determine current location for radar metadata fetch");
        return null;
    }

    /**
     * Gets the latest stored radar metadata.
     */
    public Optional<WeatherRadarMetadata> getLatestRadarMetadata() {
        return weatherRadarMetadataRepository.findTopByOrderByTimestampDesc();
    }

    /**
     * Gets radar metadata with statistics for a time period.
     */
    public List<WeatherRadarMetadata> getRecentRadarMetadata(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return weatherRadarMetadataRepository.findRecentMetadata(since);
    }

    /**
     * Cleans up old radar metadata (older than 7 days).
     */
    @Transactional
    public void cleanupOldRadarMetadata() {
        log.info("Cleaning up old radar metadata");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        weatherRadarMetadataRepository.deleteOldMetadata(cutoff);
        log.info("Cleaned up radar metadata older than {}", cutoff);
    }
}