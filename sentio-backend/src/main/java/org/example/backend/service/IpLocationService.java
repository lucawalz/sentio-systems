package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.LocationData;
import org.example.backend.repository.LocationDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service responsible for IP-based geolocation operations and data management.
 * Integrates with ip-api.com to resolve geographic locations from IP addresses.
 * <p>
 * This service implements intelligent caching to minimize API calls by storing
 * location data for 24 hours and provides automatic cleanup of outdated records.
 * It handles both specific IP lookups and current location detection.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IpLocationService {

    private final LocationDataRepository locationDataRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ip-location.api.url:http://ip-api.com/json/}")
    private String ipLocationApiUrl;

    /**
     * Resolves geographic location data for a specific IP address.
     * Uses intelligent caching to return stored data if less than 24 hours old.
     *
     * @param ipAddress The IP address to resolve location for
     * @return Optional containing LocationData if successful, empty if resolution fails
     */
    public Optional<LocationData> getLocationByIp(String ipAddress) {
        log.debug("Resolving location for IP address: {}", ipAddress);

        try {
            // Check for cached data within 24-hour window
            Optional<LocationData> existingLocation = locationDataRepository.findLatestByIpAddress(ipAddress);
            if (existingLocation.isPresent()) {
                LocationData location = existingLocation.get();
                if (location.getUpdatedAt().isAfter(LocalDateTime.now().minusHours(24))) {
                    log.info("Using cached location data for IP: {} - {}, {}",
                            ipAddress, location.getCity(), location.getCountry());
                    return existingLocation;
                }
                log.debug("Cached data for IP {} is older than 24 hours, fetching fresh data", ipAddress);
            }

            // Fetch fresh location data from API
            String url = ipLocationApiUrl + ipAddress;
            log.info("Fetching location data from: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.get("status").asText().equals("success")) {
                LocationData locationData = new LocationData();
                locationData.setIpAddress(ipAddress);
                locationData.setCity(jsonNode.get("city").asText());
                locationData.setCountry(jsonNode.get("country").asText());
                locationData.setRegion(jsonNode.get("regionName").asText());
                locationData.setLatitude(jsonNode.get("lat").floatValue());
                locationData.setLongitude(jsonNode.get("lon").floatValue());
                locationData.setTimezone(jsonNode.get("timezone").asText());
                locationData.setIsp(jsonNode.get("isp").asText());
                locationData.setOrganization(jsonNode.get("org").asText());

                LocationData savedLocation = locationDataRepository.save(locationData);
                log.info("Saved new location data for IP: {} - {}, {}", ipAddress,
                        savedLocation.getCity(), savedLocation.getCountry());

                return Optional.of(savedLocation);
            } else {
                String errorMessage = jsonNode.has("message") ? jsonNode.get("message").asText() : "Unknown error";
                log.warn("Failed to get location for IP: {} - {}", ipAddress, errorMessage);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error fetching location data for IP: {}", ipAddress, e);
            return Optional.empty();
        }
    }

    /**
     * Determines the current user's geographic location based on their public IP.
     * Makes an API call without IP parameter to auto-detect the caller's location.
     *
     * @return Optional containing current LocationData if successful, empty if detection fails
     */
    public Optional<LocationData> getCurrentLocation() {
        log.debug("Determining current location via IP detection");

        try {
            // Use API without IP parameter for auto-detection
            String url = ipLocationApiUrl;
            log.info("Fetching current location from: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.get("status").asText().equals("success")) {
                String currentIp = jsonNode.get("query").asText();
                log.debug("Auto-detected current IP: {}", currentIp);
                return getLocationByIp(currentIp);
            } else {
                String errorMessage = jsonNode.has("message") ? jsonNode.get("message").asText() : "Unknown error";
                log.warn("Failed to get current location: {}", errorMessage);
                return Optional.empty();
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Network error fetching current location - service may be unavailable: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching current location", e);
            return Optional.empty();
        }
    }

    /**
     * Removes location data older than 14 days to maintain database efficiency.
     * This maintenance operation helps prevent unlimited data accumulation.
     */
    public void cleanupOldLocationData() {
        log.info("Starting cleanup of old location data");

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(14);
            locationDataRepository.deleteOldLocationData(cutoffDate);
            log.info("Successfully cleaned up location data older than 14 days (before: {})", cutoffDate);
        } catch (Exception e) {
            log.error("Error occurred during location data cleanup", e);
        }
    }
}