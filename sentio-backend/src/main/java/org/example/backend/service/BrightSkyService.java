package org.example.backend.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.RadarMetadataDTO;
import org.example.backend.model.Device;
import org.example.backend.model.LocationData;
import org.example.backend.model.WeatherAlert;
import org.example.backend.model.WeatherRadarMetadata;
import org.example.backend.repository.WeatherAlertRepository;
import org.example.backend.repository.WeatherRadarMetadataRepository;
import org.example.backend.service.brightsky.BrightSkyAlertMapper;
import org.example.backend.service.brightsky.BrightSkyApiClient;
import org.example.backend.service.brightsky.BrightSkyRadarMetadataCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final DeviceLocationService deviceLocationService;
    private final DeviceService deviceService;
    private final BrightSkyApiClient brightSkyApiClient;
    private final BrightSkyAlertMapper brightSkyAlertMapper;
    private final BrightSkyRadarMetadataCalculator radarMetadataCalculator;
    private volatile boolean isUpdating = false;

    /**
     * Retrieves weather alerts for the current user's device location.
     * Uses the first registered device's location.
     * Enforces strict device-only policy - never uses server or browser IP.
     *
     * @return List of weather alerts for user's device location, empty list if user
     *         has no devices
     */
    public List<WeatherAlert> getAlertsForCurrentLocation() {
        log.debug("Retrieving alerts for current user's device location");
        Optional<LocationData> deviceLocation = deviceLocationService.getPrimaryUserDeviceLocation();
        if (deviceLocation.isPresent()) {
            LocationData loc = deviceLocation.get();
            return getAlertsForLocation(loc.getLatitude(), loc.getLongitude(), loc.getDeviceId());
        }
        log.warn("User has no registered devices, cannot retrieve weather alerts");
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
    @CircuitBreaker(name = "brightSky", fallbackMethod = "getAlertsForLocationFallback")
    public List<WeatherAlert> getAlertsForLocation(Float latitude, Float longitude, String deviceId) {
        log.info("Processing weather alerts for location: lat: {}, lon: {}, device: {}", latitude, longitude, deviceId);

        try {
            // Clean up expired alerts before fetching new data
            cleanupExpiredAlerts();
            var payload = brightSkyApiClient.fetchAlerts(latitude, longitude);
            List<WeatherAlert> processedAlerts = brightSkyAlertMapper.mapAlerts(payload, deviceId);

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

    // ===== Device-scoped methods =====

    /**
     * Retrieves active alerts for a specific device after verifying ownership.
     *
     * @param deviceId The device UUID
     * @return List of active alerts for the device
     * @throws IllegalArgumentException if device not found or not owned by user
     */
    public List<WeatherAlert> getAlertsForDevice(String deviceId) {
        deviceService.getVerifiedDevice(deviceId);
        return weatherAlertRepository.findActiveAlertsByDeviceId(deviceId, LocalDateTime.now());
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
            // Update alerts for current user's device location (not server IP)
            Optional<LocationData> deviceLocation = deviceLocationService.getPrimaryUserDeviceLocation();
            if (deviceLocation.isPresent()) {
                LocationData location = deviceLocation.get();
                getAlertsForLocation(location.getLatitude(), location.getLongitude(), location.getDeviceId());
                log.info("Successfully updated alerts for device location: {}, {}",
                        location.getCity(), location.getCountry());
            } else {
                log.warn("No device location available for alert update");
            }
        } catch (Exception e) {
            log.error("Error occurred during alert update for current location", e);
        } finally {
            isUpdating = false;
        }
    }

    /**
     * Updates weather alerts for all registered device locations.
     * This is the new method used by scheduled tasks to update alerts only for
     * device locations.
     * Replaces updateAlertsForCurrentLocation() for scheduled background updates.
     * <p>
     * Enforces strict device-only policy:
     * - Only updates weather for registered device locations
     * - Never uses server or browser IP
     * - Skips gracefully if no devices are registered
     * </p>
     */
    @Transactional
    public void updateAlertsForAllDeviceLocations() {
        if (isUpdating) {
            log.warn("Alert update already in progress, skipping concurrent update request");
            return;
        }

        isUpdating = true;
        log.info("Starting alert update for all device locations");

        try {
            // Get all unique device locations
            List<LocationData> deviceLocations = deviceLocationService.getAllUniqueDeviceLocations();

            if (deviceLocations.isEmpty()) {
                log.debug("No registered devices found, skipping weather alert update");
                return;
            }

            log.info("Updating weather alerts for {} unique device locations", deviceLocations.size());

            // Update alerts for each unique device location
            int successCount = 0;
            for (LocationData location : deviceLocations) {
                try {
                    getAlertsForLocation(location.getLatitude(), location.getLongitude(), location.getDeviceId());
                    log.debug("Updated alerts for device location: {}, {}",
                            location.getCity(), location.getCountry());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to update alerts for location: {}, {} - {}",
                            location.getCity(), location.getCountry(), e.getMessage());
                }
            }

            log.info("Successfully updated alerts for {}/{} device locations",
                    successCount, deviceLocations.size());
        } catch (Exception e) {
            log.error("Error occurred during alert update for device locations", e);
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

        String url = brightSkyApiClient.buildRadarUrl(latitude, longitude, distance, format);

        log.debug("Generated radar endpoint URL: {}", url);
        return url;
    }

    /**
     * Provides radar data endpoint configuration for current user's device
     * location.
     * Enforces strict device-only policy.
     * 
     * @return Radar endpoint URL for user's device location, null if user has no
     *         devices
     */
    public String getRadarEndpointUrlForCurrentLocation(Integer distance, String format) {
        Optional<LocationData> deviceLocation = deviceLocationService.getPrimaryUserDeviceLocation();
        if (deviceLocation.isPresent()) {
            LocationData location = deviceLocation.get();
            return getRadarEndpointUrl(location.getLatitude(), location.getLongitude(), distance, format);
        }
        log.warn("User has no registered devices, cannot generate radar endpoint URL");
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
    public RadarMetadataDTO fetchAndStoreRadarMetadata(Float latitude, Float longitude, Integer distance,
            String deviceId) {
        if (distance == null)
            distance = 100000;

        log.info("Fetching radar data for device: {}, lat: {}, lon: {}, distance: {}", deviceId, latitude, longitude,
                distance);

        try {
            var payload = brightSkyApiClient.fetchRadar(latitude, longitude, distance, "plain");
            BrightSkyRadarMetadataCalculator.RadarCalculation calculation = radarMetadataCalculator
                    .calculate(payload, latitude, longitude, distance, deviceId);

            if (calculation == null) {
                log.warn("No radar data returned from BrightSky");
                return null;
            }

            WeatherRadarMetadata savedMetadata = weatherRadarMetadataRepository.save(calculation.getMetadata());
            log.info("Saved radar metadata: coverage={}%, max={}mm, significant cells={}",
                    String.format("%.1f", savedMetadata.getCoveragePercent()),
                    String.format("%.2f", savedMetadata.getPrecipitationMax()),
                    savedMetadata.getSignificantRainCells());

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
                    .hasActivePrecipitation(calculation.isHasActivePrecipitation())
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch and store radar metadata", e);
            return null;
        }
    }

    /**
     * Fetches and stores radar metadata for current user's device location.
     * Enforces strict device-only policy.
     */
    @Transactional
    public RadarMetadataDTO fetchAndStoreRadarMetadataForCurrentLocation(Integer distance) {
        Optional<LocationData> deviceLocation = deviceLocationService.getPrimaryUserDeviceLocation();
        if (deviceLocation.isPresent()) {
            LocationData location = deviceLocation.get();
            return fetchAndStoreRadarMetadata(location.getLatitude(), location.getLongitude(), distance,
                    location.getDeviceId());
        }
        log.warn("No device location available for radar fetch");
        return null;
    }

    /**
     * Gets radar endpoint URL for a specific device.
     * Verifies device ownership before returning.
     */
    public String getRadarEndpointForDevice(String deviceId) {
        Device device = deviceService.getVerifiedDevice(deviceId);
        if (device.getLatitude() == null || device.getLongitude() == null) {
            throw new IllegalArgumentException("Device has no GPS coordinates");
        }
        return getRadarEndpointUrl(
                device.getLatitude().floatValue(),
                device.getLongitude().floatValue(),
                null, null);
    }

    /**
     * Fetches and stores radar metadata for a specific device.
     */
    @Transactional
    public RadarMetadataDTO fetchRadarMetadataForDevice(String deviceId, Integer distance) {
        Device device = deviceService.getVerifiedDevice(deviceId);
        if (device.getLatitude() == null || device.getLongitude() == null) {
            throw new IllegalArgumentException("Device has no GPS coordinates");
        }
        return fetchAndStoreRadarMetadata(
                device.getLatitude().floatValue(),
                device.getLongitude().floatValue(),
                distance, deviceId);
    }

    /**
     * @deprecated Use getPrimaryUserDeviceLocation() flow instead
     */
    @Deprecated
    private RadarMetadataDTO fetchAndStoreRadarMetadataForCurrentLocationLegacy(Integer distance) {
        Optional<LocationData> deviceLocation = deviceLocationService.getPrimaryUserDeviceLocation();
        if (deviceLocation.isPresent()) {
            LocationData location = deviceLocation.get();
            return fetchAndStoreRadarMetadata(location.getLatitude(), location.getLongitude(), distance,
                    location.getDeviceId());
        }
        log.warn("User has no registered devices, cannot fetch radar metadata");
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

    // ==================== Circuit Breaker Fallback Methods ====================

    /**
     * Fallback for getAlertsForLocation when BrightSky API is unavailable.
     * Returns empty list - alerts are cached in DB from previous successful calls.
     */
    @SuppressWarnings("unused")
    private List<WeatherAlert> getAlertsForLocationFallback(Float latitude, Float longitude,
            String deviceId, Exception ex) {
        log.warn("BrightSky API unavailable for device {}: {}. Using cached data if available.",
                deviceId, ex.getMessage());
        return new ArrayList<>();
    }
}