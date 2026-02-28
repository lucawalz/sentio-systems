
package org.example.backend.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.HistoricalWeather;
import org.example.backend.model.LocationData;
import org.example.backend.repository.HistoricalWeatherRepository;
import org.example.backend.service.historical.HistoricalDateStrategy;
import org.example.backend.service.historical.HistoricalWeatherEntityMapper;
import org.example.backend.service.historical.OpenMeteoArchiveClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for managing historical weather data operations.
 * Handles fetching historical weather from OpenMeteo Historical API and
 * database persistence.
 * <p>
 * This service integrates with the OpenMeteo Historical/Archive API to retrieve
 * past weather data for comparison with forecasts and trend analysis.
 * It implements intelligent caching and automatic daily updates for historical
 * data.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HistoricalWeatherService implements IHistoricalWeatherService {

    private final HistoricalWeatherRepository historicalWeatherRepository;
    private final DeviceLocationService deviceLocationService;
    private final OpenMeteoArchiveClient openMeteoArchiveClient;
    private final HistoricalDateStrategy historicalDateStrategy;
    private final HistoricalWeatherEntityMapper historicalWeatherEntityMapper;
    private volatile boolean isUpdating = false;

    /**
     * Retrieves historical weather for the current user's device location.
     * Uses the first registered device's location.
     * Enforces strict device-only policy - never uses server or browser IP.
     * Fetches data for predefined intervals: 3 days ago, 2 weeks ago, 1 month ago,
     * 3 months ago, 1 year ago.
     *
     * @return List of historical weather data for user's device location
     */
    public List<HistoricalWeather> getHistoricalWeatherForCurrentLocation() {
        log.debug("Retrieving historical weather for current user's device location");
        Optional<LocationData> deviceLocation = deviceLocationService.getPrimaryUserDeviceLocation();
        if (deviceLocation.isPresent()) {
            return getHistoricalWeatherForLocation(deviceLocation.get().getLatitude(),
                    deviceLocation.get().getLongitude(),
                    deviceLocation.get());
        }
        log.warn("User has no registered devices, cannot retrieve historical weather");
        return new ArrayList<>();
    }

    /**
     * Fetches and processes historical weather for specific geographic coordinates.
     * Retrieves data for key historical intervals and stores it in the database.
     *
     * @param latitude     Geographic latitude coordinate
     * @param longitude    Geographic longitude coordinate
     * @param locationData Complete location information
     * @return List of processed historical weather records
     */
    @CircuitBreaker(name = "openMeteo", fallbackMethod = "getHistoricalWeatherForLocationFallback")
    @Transactional
    public List<HistoricalWeather> getHistoricalWeatherForLocation(Float latitude, Float longitude,
            LocationData locationData) {
        String deviceId = locationData.getDeviceId();
        log.info("Processing historical weather for device {} (lat: {}, lon: {})",
                deviceId, latitude, longitude);

        List<HistoricalWeather> allHistoricalData = new ArrayList<>();

        // Define historical intervals
        List<LocalDate> historicalDates = historicalDateStrategy.getHistoricalDates();

        // Check which dates we already have in the database FOR THIS DEVICE
        List<LocalDate> existingDates = deviceId != null
                ? historicalWeatherRepository.findExistingDatesForDevice(
                        deviceId,
                        historicalDates.get(historicalDates.size() - 1), // oldest date
                        historicalDates.get(0)) // newest date
                : historicalWeatherRepository.findExistingDatesInRange(
                        locationData.getCity(), locationData.getCountry(),
                        historicalDates.get(historicalDates.size() - 1),
                        historicalDates.get(0));

        // Filter out dates we already have (unless they're very old records that need
        // updating)
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<LocalDate> datesToFetch = historicalDates.stream()
                .filter(date -> !existingDates.contains(date) || needsUpdate(date, locationData, oneWeekAgo))
                .collect(Collectors.toList());

        log.debug("historicalDates: {}", historicalDates);
        log.debug("existingDates: {}", existingDates);
        log.debug("datesToFetch: {}", datesToFetch);

        if (datesToFetch.isEmpty()) {
            log.info("All historical data is up to date for device: {}", deviceId);
            return deviceId != null
                    ? historicalWeatherRepository.findByDatesAndDeviceId(historicalDates, deviceId)
                    : historicalWeatherRepository.findByDatesAndLocation(historicalDates,
                            locationData.getCity(), locationData.getCountry());
        }

        log.info("Fetching historical weather for {} dates", datesToFetch.size());

        // Group dates by month-year to minimize API calls (OpenMeteo allows date
        // ranges)
        Map<String, List<LocalDate>> dateGroups = historicalDateStrategy.groupDatesByMonthYear(datesToFetch);

        for (Map.Entry<String, List<LocalDate>> group : dateGroups.entrySet()) {
            List<LocalDate> groupDates = group.getValue();
            LocalDate startDate = Collections.min(groupDates);
            LocalDate endDate = Collections.max(groupDates);

            try {
                var jsonNode = openMeteoArchiveClient.fetchHistoricalRange(latitude, longitude, startDate, endDate);
                List<HistoricalWeather> groupData = historicalWeatherEntityMapper
                    .processHistoricalWeatherResponse(jsonNode, locationData, groupDates);
                allHistoricalData.addAll(groupData);

                // Small delay to be respectful to the API
                Thread.sleep(100);

            } catch (Exception e) {
                log.error("Failed to fetch historical weather for date range {} to {} at location: {}, {}",
                        startDate, endDate, locationData.getCity(), locationData.getCountry(), e);
            }
        }

        log.info("Successfully processed {} historical weather records for location: {}, {}",
                allHistoricalData.size(), locationData.getCity(), locationData.getCountry());

        return allHistoricalData;
    }

    /**
     * Checks if a historical record needs updating based on its age.
     */
    private boolean needsUpdate(LocalDate date, LocationData locationData, LocalDateTime cutoff) {
        String deviceId = locationData.getDeviceId();
        Optional<HistoricalWeather> existing = deviceId != null
                ? historicalWeatherRepository.findByWeatherDateAndDeviceId(date, deviceId)
                : historicalWeatherRepository.findByWeatherDateAndLocation(
                        date, locationData.getCity(), locationData.getCountry());

        boolean needIt = existing.isEmpty() || existing.get().getUpdatedAt().isBefore(cutoff);
        log.debug("needsUpdate - date: {}, existing: {}, updatedAt: {}, cutoff: {}, result: {}",
                date, existing.isPresent(), existing.map(HistoricalWeather::getUpdatedAt).orElse(null), cutoff, needIt);
        return needIt;
    }


    /**
     * Updates historical weather for the current location.
     * This method is called daily to ensure we have recent historical data.
     */
    @Transactional
    public void updateHistoricalWeatherForCurrentLocation() {
        if (isUpdating) {
            log.warn("Historical weather update already in progress, skipping concurrent update request");
            return;
        }

        isUpdating = true;
        log.info("Starting historical weather update for device location");

        try {
            // Perform cleanup before updating
            cleanupOldHistoricalWeather();

            // Update historical weather for first user device location
            Optional<LocationData> currentLocation = deviceLocationService.getPrimaryUserDeviceLocation();
            if (currentLocation.isPresent()) {
                LocationData location = currentLocation.get();
                getHistoricalWeatherForLocation(location.getLatitude(), location.getLongitude(), location);
                log.info("Successfully updated historical weather for device location: ({}, {})",
                        location.getLatitude(), location.getLongitude());
            } else {
                log.warn("No device with GPS coordinates found for historical weather update");
            }
        } catch (Exception e) {
            log.error("Error occurred during historical weather update for device location", e);
        } finally {
            isUpdating = false;
        }
    }

    /**
     * Updates historical weather for all registered device locations.
     * This is the new method used by scheduled tasks to update historical data only
     * for device locations.
     * Replaces updateHistoricalWeatherForCurrentLocation() for scheduled background
     * updates.
     * <p>
     * Enforces strict device-only policy:
     * - Only updates weather for registered device locations
     * - Never uses server or browser IP
     * - Skips gracefully if no devices are registered
     * </p>
     */
    @Transactional
    public void updateHistoricalWeatherForAllDeviceLocations() {
        if (isUpdating) {
            log.warn("Historical weather update already in progress, skipping concurrent update request");
            return;
        }

        isUpdating = true;
        log.info("Starting historical weather update for all device locations");

        try {
            cleanupOldHistoricalWeather();

            // Get all unique device locations
            List<LocationData> deviceLocations = deviceLocationService.getAllUniqueDeviceLocations();

            if (deviceLocations.isEmpty()) {
                log.debug("No registered devices found, skipping historical weather update");
                return;
            }

            log.info("Updating historical weather for {} unique device locations", deviceLocations.size());

            // Update historical weather for each unique device location
            int successCount = 0;
            for (LocationData location : deviceLocations) {
                try {
                    getHistoricalWeatherForLocation(location.getLatitude(), location.getLongitude(), location);
                    log.debug("Updated historical weather for device location: {}, {}",
                            location.getCity(), location.getCountry());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to update historical weather for location: {}, {} - {}",
                            location.getCity(), location.getCountry(), e.getMessage());
                }
            }

            log.info("Successfully updated historical weather for {}/{} device locations",
                    successCount, deviceLocations.size());
        } catch (Exception e) {
            log.error("Error occurred during historical weather update for device locations", e);
        } finally {
            isUpdating = false;
        }
    }

    /**
     * Retrieves historical weather within a specified date range.
     */
    public List<HistoricalWeather> getHistoricalWeatherForDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Retrieving historical weather for date range: {} to {}", startDate, endDate);
        return historicalWeatherRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Retrieves historical weather for a specific date (legacy - no device filter).
     */
    public HistoricalWeather getHistoricalWeatherForDate(LocalDate date) {
        log.debug("Retrieving historical weather for date: {}", date);
        return historicalWeatherRepository.findTopByWeatherDateOrderByCreatedAtDesc(date);
    }

    /**
     * Retrieves historical weather for a specific date and device.
     * If deviceId is null, falls back to returning any record for that date.
     */
    public HistoricalWeather getHistoricalWeatherForDate(LocalDate date, String deviceId) {
        log.debug("Retrieving historical weather for date: {} and device: {}", date, deviceId);
        if (deviceId != null && !deviceId.isEmpty()) {
            return historicalWeatherRepository.findByWeatherDateAndDeviceId(date, deviceId)
                    .orElse(null);
        }
        return historicalWeatherRepository.findTopByWeatherDateOrderByCreatedAtDesc(date);
    }

    /**
     * Performs cleanup of outdated historical weather data.
     * Keeps historical data for 2 years by default.
     */
    @Transactional
    public void cleanupOldHistoricalWeather() {
        log.info("Starting cleanup of old historical weather data");

        LocalDateTime twoYearsAgo = LocalDateTime.now().minusYears(2);
        LocalDate twoYearsAgoDate = LocalDate.now().minusYears(2);

        try {
            // Remove historical weather older than 2 years based on creation time
            historicalWeatherRepository.deleteOldHistoricalWeather(twoYearsAgo);
            log.debug("Deleted historical weather older than 2 years (created before: {})", twoYearsAgo);

            // Remove historical weather for dates older than 2 years
            historicalWeatherRepository.deleteExpiredHistoricalWeather(twoYearsAgoDate);
            log.debug("Deleted expired historical weather (weather date before: {})", twoYearsAgoDate);

            log.info("Successfully completed cleanup of old historical weather data");
        } catch (Exception e) {
            log.error("Error occurred during historical weather cleanup", e);
        }
    }

    /**
     * Retrieves available cities with historical weather data.
     */
    public List<String> getAvailableCitiesWithHistoricalWeather() {
        log.debug("Retrieving available cities with historical weather");
        return historicalWeatherRepository.findDistinctCitiesWithHistoricalWeather(LocalDate.now().minusYears(1));
    }

    @SuppressWarnings("unused")
    private List<HistoricalWeather> getHistoricalWeatherForLocationFallback(Float latitude, Float longitude,
            LocationData locationData, Exception ex) {
        log.warn("Open-Meteo Archive API unavailable at {}, {}: {}. Using cached data.",
                latitude, longitude, ex.getMessage());
        return new ArrayList<>();
    }
}