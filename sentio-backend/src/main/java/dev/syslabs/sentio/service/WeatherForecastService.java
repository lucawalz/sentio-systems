package dev.syslabs.sentio.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.model.LocationData;
import dev.syslabs.sentio.model.WeatherForecast;
import dev.syslabs.sentio.repository.WeatherForecastRepository;
import dev.syslabs.sentio.service.forecast.ForecastEntityMapper;
import dev.syslabs.sentio.service.forecast.OpenMeteoForecastClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Service responsible for managing weather forecast data operations.
 * Updated to use Open-Meteo hourly forecast API exclusively.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherForecastService implements IWeatherForecastService {

    private final WeatherForecastRepository weatherForecastRepository;
    private final DeviceLocationService deviceLocationService;
    private final DeviceService deviceService;
    private final OpenMeteoForecastClient openMeteoForecastClient;
    private final ForecastEntityMapper forecastEntityMapper;
    private volatile boolean isUpdating = false;
    private final ConcurrentHashMap<String, ReentrantLock> forecastLocks = new ConcurrentHashMap<>();

    /**
     * Retrieves weather forecasts for the current user's device location.
     * Uses the first registered device's location.
     * Enforces strict device-only policy - never uses server or browser IP.
     */
    public List<WeatherForecast> getForecastForCurrentLocation() {
        log.debug("Retrieving forecast for current user's device location");
        Optional<LocationData> deviceLocation = deviceLocationService.getFirstUserDeviceLocation();
        if (deviceLocation.isPresent()) {
            return getForecastForLocation(deviceLocation.get().getLatitude(),
                    deviceLocation.get().getLongitude(),
                    deviceLocation.get());
        }
        log.warn("User has no registered devices, cannot retrieve weather forecast");
        return new ArrayList<>();
    }

    /**
     * Fetches and processes hourly weather forecasts for a specific geographic
     * location.
     * Uses the new Open-Meteo hourly API parameters.
     */
    @CircuitBreaker(name = "openMeteo", fallbackMethod = "getForecastForLocationFallback")
    public List<WeatherForecast> getForecastForLocation(Float latitude, Float longitude, LocationData locationData) {
        String lockKey = locationData.getDeviceId() != null ? locationData.getDeviceId() : latitude + "," + longitude;
        ReentrantLock lock = forecastLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            log.debug("Forecast update already in progress for device {}, skipping duplicate request", locationData.getDeviceId());
            return new ArrayList<>();
        }
        try {
            log.info("Processing hourly weather forecast for location: {}, {} (lat: {}, lon: {})",
                    locationData.getCity(), locationData.getCountry(), latitude, longitude);

            // Clean up expired forecasts before fetching new data
            weatherForecastRepository.deleteExpiredForecasts(LocalDate.now().minusDays(1));
            log.debug("Cleaned up expired forecasts before processing new data");

            var jsonNode = openMeteoForecastClient.fetchHourlyForecast(latitude, longitude);

            // Get existing forecasts for intelligent update/insert logic
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            List<WeatherForecast> existingForecasts;
            if (locationData.getDeviceId() != null) {
                existingForecasts = weatherForecastRepository.findByDeviceIdAndDateRange(
                        locationData.getDeviceId(), startDate, endDate);
            } else {
                existingForecasts = weatherForecastRepository.findByLocationAndDateRange(
                        locationData.getCity(), locationData.getCountry(), startDate, endDate);
            }

            Map<LocalDateTime, WeatherForecast> existingMap = existingForecasts.stream()
                    .collect(Collectors.toMap(WeatherForecast::getForecastDateTime, f -> f));

            var hourlyNode = jsonNode.get("hourly");
            List<WeatherForecast> processedForecasts = new ArrayList<>();
            if (hourlyNode != null) {
                processedForecasts = forecastEntityMapper.processHourlyForecast(hourlyNode, locationData, existingMap);
            }

            List<WeatherForecast> savedForecasts = weatherForecastRepository.saveAll(processedForecasts);
            log.info("Successfully processed {} hourly weather forecasts for location: {}, {}",
                    savedForecasts.size(), locationData.getCity(), locationData.getCountry());
            return savedForecasts;

        } catch (Exception e) {
            log.error("Failed to fetch weather forecast for location: {}, {} (lat: {}, lon: {})",
                    locationData.getCity(), locationData.getCountry(), latitude, longitude, e);
            return new ArrayList<>();
        } finally {
            lock.unlock();
        }
    }

    // ===== Device-scoped methods =====

    /**
     * Retrieves forecasts for a specific device after verifying ownership.
     *
     * @param deviceId The device UUID
     * @return List of upcoming forecasts for the device
     * @throws IllegalArgumentException if device not found or not owned by user
     */
    public List<WeatherForecast> getForecastForDevice(String deviceId) {
        deviceService.getVerifiedDevice(deviceId);
        return weatherForecastRepository.findByDeviceIdAndForecastDateGreaterThanEqual(
                deviceId, LocalDate.now());
    }

    // Keep all existing service methods unchanged
    public List<WeatherForecast> getUpcomingForecasts() {
        log.debug("Retrieving upcoming forecasts from today");
        return weatherForecastRepository.findUpcomingForecasts(LocalDate.now());
    }

    public List<WeatherForecast> getForecastsForDateRange(LocalDate startDate, LocalDate endDate) {
        log.debug("Retrieving forecasts for date range: {} to {}", startDate, endDate);
        return weatherForecastRepository.findByDateRange(startDate, endDate);
    }

    public List<WeatherForecast> getForecastsForDate(LocalDate date) {
        log.debug("Retrieving forecasts for date: {}", date);
        return weatherForecastRepository.findByForecastDate(date);
    }

    public WeatherForecast getLatestForecastForDate(LocalDate date) {
        log.debug("Retrieving latest forecast for date: {}", date);
        return weatherForecastRepository.findTopByForecastDateOrderByCreatedAtDesc(date);
    }

    public List<WeatherForecast> getRecentForecasts(int hours) {
        log.debug("Retrieving forecasts from the last {} hours", hours);
        LocalDateTime startDate = LocalDateTime.now().minusHours(hours);
        return weatherForecastRepository.findRecentForecasts(startDate);
    }

    @Transactional
    public void cleanupOldForecasts() {
        log.info("Starting cleanup of old weather forecasts");
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        LocalDate yesterday = LocalDate.now().minusDays(1);

        try {
            weatherForecastRepository.deleteOldForecasts(twoDaysAgo);
            log.debug("Deleted forecasts older than 2 days (created before: {})", twoDaysAgo);

            weatherForecastRepository.deleteExpiredForecasts(yesterday);
            log.debug("Deleted expired forecasts (forecast date before: {})", yesterday);

            log.info("Successfully completed cleanup of old weather forecasts");
        } catch (Exception e) {
            log.error("Error occurred during forecast cleanup", e);
        }
    }

    @Transactional
    public void updateForecastsForCurrentLocation() {
        if (isUpdating) {
            log.warn("Forecast update already in progress, skipping concurrent update request");
            return;
        }

        isUpdating = true;
        log.info("Starting forecast update for current device location");

        try {
            cleanupOldForecasts();
            // Use device location instead of IP-based location
            Optional<LocationData> currentLocation = deviceLocationService.getFirstUserDeviceLocation();
            if (currentLocation.isPresent()) {
                LocationData location = currentLocation.get();
                getForecastForLocation(location.getLatitude(), location.getLongitude(), location);
                log.info("Successfully updated forecasts for device location: ({}, {})",
                        location.getLatitude(), location.getLongitude());
            } else {
                log.warn("No device with GPS coordinates found for forecast update");
            }
        } catch (Exception e) {
            log.error("Error occurred during forecast update for device location", e);
        } finally {
            isUpdating = false;
        }
    }

    /**
     * Updates weather forecasts for all registered device locations.
     * This is the new method used by scheduled tasks to update forecasts only for
     * device locations.
     * Replaces updateForecastsForCurrentLocation() for scheduled background
     * updates.
     * <p>
     * Enforces strict device-only policy:
     * - Only updates weather for registered device locations
     * - Never uses server or browser IP
     * - Skips gracefully if no devices are registered
     * </p>
     */
    @Transactional
    public void updateForecastsForAllDeviceLocations() {
        if (isUpdating) {
            log.warn("Forecast update already in progress, skipping concurrent update request");
            return;
        }

        isUpdating = true;
        log.info("Starting forecast update for all device locations");

        try {
            cleanupOldForecasts();

            // Get all unique device locations
            List<LocationData> deviceLocations = deviceLocationService.getAllUniqueDeviceLocations();

            if (deviceLocations.isEmpty()) {
                log.debug("No registered devices found, skipping weather forecast update");
                return;
            }

            log.info("Updating weather forecasts for {} unique device locations", deviceLocations.size());

            // Update forecasts for each unique device location
            int successCount = 0;
            for (LocationData location : deviceLocations) {
                try {
                    getForecastForLocation(location.getLatitude(), location.getLongitude(), location);
                    log.debug("Updated forecasts for device location: {}, {}",
                            location.getCity(), location.getCountry());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to update forecasts for location: {}, {} - {}",
                            location.getCity(), location.getCountry(), e.getMessage());
                }
            }

            log.info("Successfully updated forecasts for {}/{} device locations",
                    successCount, deviceLocations.size());
        } catch (Exception e) {
            log.error("Error occurred during forecast update for device locations", e);
        } finally {
            isUpdating = false;
        }
    }

    public List<String> getAvailableCities() {
        log.debug("Retrieving available cities with upcoming forecasts");
        return weatherForecastRepository.findDistinctCitiesWithUpcomingForecasts(LocalDate.now());
    }


    @SuppressWarnings("unused")
    private List<WeatherForecast> getForecastForLocationFallback(Float latitude, Float longitude,
            LocationData locationData, Exception ex) {
        log.warn("Open-Meteo API unavailable for forecast at {}, {}: {}. Using cached data.",
                latitude, longitude, ex.getMessage());
        return new ArrayList<>();
    }
}