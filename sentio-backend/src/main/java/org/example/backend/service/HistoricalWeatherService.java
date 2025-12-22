
package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.HistoricalWeather;
import org.example.backend.model.LocationData;
import org.example.backend.repository.HistoricalWeatherRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
public class HistoricalWeatherService {

    private final HistoricalWeatherRepository historicalWeatherRepository;
    private final IpLocationService ipLocationService;
    private final DeviceLocationService deviceLocationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private volatile boolean isUpdating = false;

    @Value("${openmeteo.archive.base-url:https://archive-api.open-meteo.com/v1}")
    private String archiveBaseUrl;

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
        Optional<LocationData> deviceLocation = deviceLocationService.getFirstUserDeviceLocation();
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
    @Transactional
    public List<HistoricalWeather> getHistoricalWeatherForLocation(Float latitude, Float longitude,
            LocationData locationData) {
        log.info("Processing historical weather for location: {}, {} (lat: {}, lon: {})",
                locationData.getCity(), locationData.getCountry(), latitude, longitude);

        List<HistoricalWeather> allHistoricalData = new ArrayList<>();

        // Define historical intervals
        List<LocalDate> historicalDates = getHistoricalDates();

        // Check which dates we already have in the database
        List<LocalDate> existingDates = historicalWeatherRepository.findExistingDatesInRange(
                locationData.getCity(), locationData.getCountry(),
                historicalDates.get(historicalDates.size() - 1), // oldest date
                historicalDates.get(0) // newest date
        );

        // Filter out dates we already have (unless they're very old records that need
        // updating)
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<LocalDate> datesToFetch = historicalDates.stream()
                .filter(date -> !existingDates.contains(date) || needsUpdate(date, locationData, oneWeekAgo))
                .collect(Collectors.toList());

        if (datesToFetch.isEmpty()) {
            log.info("All historical data is up to date for location: {}, {}",
                    locationData.getCity(), locationData.getCountry());
            return historicalWeatherRepository.findByDatesAndLocation(historicalDates,
                    locationData.getCity(), locationData.getCountry());
        }

        log.info("Fetching historical weather for {} dates", datesToFetch.size());

        // Group dates by month-year to minimize API calls (OpenMeteo allows date
        // ranges)
        Map<String, List<LocalDate>> dateGroups = groupDatesByMonthYear(datesToFetch);

        for (Map.Entry<String, List<LocalDate>> group : dateGroups.entrySet()) {
            List<LocalDate> groupDates = group.getValue();
            LocalDate startDate = Collections.min(groupDates);
            LocalDate endDate = Collections.max(groupDates);

            try {
                List<HistoricalWeather> groupData = fetchHistoricalWeatherForDateRange(
                        latitude, longitude, startDate, endDate, locationData, groupDates);
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
     * Defines the historical dates to fetch: 3 days ago, 2 weeks ago, 1 month ago,
     * 3 months ago, 1 year ago.
     */
    private List<LocalDate> getHistoricalDates() {
        LocalDate now = LocalDate.now();
        return Arrays.asList(
                now.minusDays(3), // 3 days ago
                now.minusWeeks(2), // 2 weeks ago (was 1 week)
                now.minusMonths(1), // 1 month ago
                now.minusMonths(3), // 3 months ago
                now.minusYears(1) // 1 year ago
        );
    }

    /**
     * Groups dates by month-year to optimize API calls.
     */
    private Map<String, List<LocalDate>> groupDatesByMonthYear(List<LocalDate> dates) {
        return dates.stream()
                .collect(Collectors
                        .groupingBy(date -> date.getYear() + "-" + String.format("%02d", date.getMonthValue())));
    }

    /**
     * Checks if a historical record needs updating based on its age.
     */
    private boolean needsUpdate(LocalDate date, LocationData locationData, LocalDateTime cutoff) {
        Optional<HistoricalWeather> existing = historicalWeatherRepository.findByWeatherDateAndLocation(
                date, locationData.getCity(), locationData.getCountry());

        return existing.isEmpty() || existing.get().getUpdatedAt().isBefore(cutoff);
    }

    /**
     * Fetches historical weather data from OpenMeteo Archive API for a specific
     * date range.
     */
    private List<HistoricalWeather> fetchHistoricalWeatherForDateRange(Float latitude, Float longitude,
            LocalDate startDate, LocalDate endDate,
            LocationData locationData,
            List<LocalDate> targetDates) {

        // OpenMeteo Historical API URL with the parameters you specified
        String url = String.format(
                "%s/forecast?latitude=%f&longitude=%f&start_date=%s&end_date=%s&daily=weather_code,temperature_2m_max,temperature_2m_min,sunset,sunrise,daylight_duration,sunshine_duration,uv_index_max,precipitation_sum,precipitation_hours,wind_speed_10m_max,wind_direction_10m_dominant&timezone=auto",
                archiveBaseUrl, latitude, longitude,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

        log.info("Fetching historical weather from OpenMeteo Archive: {}", url);

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            return processHistoricalWeatherResponse(jsonNode, locationData, targetDates);

        } catch (Exception e) {
            log.error("Failed to fetch historical weather from OpenMeteo Archive API", e);
            return new ArrayList<>();
        }
    }

    /**
     * Processes the JSON response from OpenMeteo Archive API.
     */
    private List<HistoricalWeather> processHistoricalWeatherResponse(JsonNode jsonNode,
            LocationData locationData,
            List<LocalDate> targetDates) {
        List<HistoricalWeather> historicalData = new ArrayList<>();

        JsonNode dailyNode = jsonNode.get("daily");
        if (dailyNode == null) {
            log.warn("No daily data found in OpenMeteo Archive response");
            return historicalData;
        }

        JsonNode timeArray = dailyNode.get("time");
        if (timeArray == null || !timeArray.isArray()) {
            log.warn("No time array found in daily data");
            return historicalData;
        }

        // Get existing records for update/insert logic
        Map<LocalDate, HistoricalWeather> existingMap = new HashMap<>();
        for (LocalDate date : targetDates) {
            historicalWeatherRepository.findByWeatherDateAndLocation(
                    date, locationData.getCity(), locationData.getCountry())
                    .ifPresent(existing -> existingMap.put(date, existing));
        }

        int newCount = 0;
        int updatedCount = 0;

        for (int i = 0; i < timeArray.size(); i++) {
            try {
                String dateStr = timeArray.get(i).asText();
                LocalDate weatherDate = LocalDate.parse(dateStr);

                // Only process target dates
                if (!targetDates.contains(weatherDate)) {
                    continue;
                }

                HistoricalWeather historical = existingMap.get(weatherDate);
                boolean isUpdate = historical != null;

                if (!isUpdate) {
                    historical = new HistoricalWeather();
                    newCount++;
                } else {
                    updatedCount++;
                }

                // Set basic information
                historical.setWeatherDate(weatherDate);

                // Extract weather data using direct method calls instead of lambdas
                JsonNode weatherCodeArray = dailyNode.get("weather_code");
                if (weatherCodeArray != null && weatherCodeArray.isArray() && i < weatherCodeArray.size()
                        && !weatherCodeArray.get(i).isNull()) {
                    historical.setWeatherCode(weatherCodeArray.get(i).intValue());
                }

                extractValue(dailyNode, "temperature_2m_max", i, historical::setMaxTemperature);
                extractValue(dailyNode, "temperature_2m_min", i, historical::setMinTemperature);
                extractValue(dailyNode, "uv_index_max", i, historical::setUvIndexMax);
                extractValue(dailyNode, "precipitation_sum", i, historical::setPrecipitationSum);
                extractValue(dailyNode, "precipitation_hours", i, historical::setPrecipitationHours);
                extractValue(dailyNode, "wind_speed_10m_max", i, historical::setWindSpeedMax);
                extractValue(dailyNode, "wind_direction_10m_dominant", i, historical::setWindDirectionDominant);
                extractValue(dailyNode, "daylight_duration", i, historical::setDaylightDuration);
                extractValue(dailyNode, "sunshine_duration", i, historical::setSunshineDuration);

                // Handle sunrise and sunset times
                extractTimeValue(dailyNode, "sunrise", i, historical::setSunrise);
                extractTimeValue(dailyNode, "sunset", i, historical::setSunset);

                // Map weather code to descriptive information
                if (historical.getWeatherCode() != null) {
                    String[] weatherInfo = mapWeatherCode(historical.getWeatherCode());
                    historical.setWeatherMain(weatherInfo[0]);
                    historical.setDescription(weatherInfo[1]);
                    historical.setIcon(weatherInfo[2]);
                }

                // Set location information
                setLocationData(historical, locationData);

                historicalData.add(historical);

            } catch (Exception e) {
                log.error("Error processing historical weather data at index {}", i, e);
            }
        }

        // Save all processed records
        List<HistoricalWeather> savedRecords = historicalWeatherRepository.saveAll(historicalData);

        log.info("Processed {} historical weather records - {} new, {} updated",
                savedRecords.size(), newCount, updatedCount);

        return savedRecords;
    }

    /**
     * Safely extracts a numeric value from the JSON response.
     */
    private void extractValue(JsonNode dailyNode, String fieldName, int index,
            java.util.function.Consumer<Float> setter) {
        JsonNode array = dailyNode.get(fieldName);
        if (array != null && array.isArray() && index < array.size() && !array.get(index).isNull()) {
            setter.accept(array.get(index).floatValue());
        }
    }

    /**
     * Safely extracts a time value and converts it to LocalDateTime.
     */
    private void extractTimeValue(JsonNode dailyNode, String fieldName, int index,
            java.util.function.Consumer<LocalDateTime> setter) {
        JsonNode array = dailyNode.get(fieldName);
        if (array != null && array.isArray() && index < array.size() && !array.get(index).isNull()) {
            try {
                String timeStr = array.get(index).asText();
                LocalDateTime dateTime = LocalDateTime.parse(timeStr);
                setter.accept(dateTime);
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse time value for field {}: {}", fieldName, array.get(index).asText());
            }
        }
    }

    /**
     * Sets location data on the historical weather entity.
     */
    private void setLocationData(HistoricalWeather historical, LocationData locationData) {
        if (locationData != null) {
            historical.setCity(locationData.getCity());
            historical.setCountry(locationData.getCountry());
            historical.setLatitude(locationData.getLatitude());
            historical.setLongitude(locationData.getLongitude());
            historical.setIpAddress(locationData.getIpAddress());
            historical.setDetectedLocation(locationData.getCity() + ", " + locationData.getCountry());
        }
    }

    /**
     * Maps weather codes to human-readable descriptions using the same logic as
     * forecasts.
     */
    private String[] mapWeatherCode(int code) {
        return switch (code) {
            case 0 -> new String[] { "Clear", "Clear sky", "01d" };
            case 1 -> new String[] { "Clouds", "Mainly clear", "02d" };
            case 2 -> new String[] { "Clouds", "Partly cloudy", "03d" };
            case 3 -> new String[] { "Clouds", "Overcast", "04d" };
            case 45 -> new String[] { "Mist", "Fog", "50d" };
            case 48 -> new String[] { "Mist", "Depositing rime fog", "50d" };
            case 51 -> new String[] { "Drizzle", "Light drizzle", "09d" };
            case 53 -> new String[] { "Drizzle", "Moderate drizzle", "09d" };
            case 55 -> new String[] { "Drizzle", "Dense drizzle", "09d" };
            case 56 -> new String[] { "Drizzle", "Light freezing drizzle", "09d" };
            case 57 -> new String[] { "Drizzle", "Dense freezing drizzle", "09d" };
            case 61 -> new String[] { "Rain", "Slight rain", "10d" };
            case 63 -> new String[] { "Rain", "Moderate rain", "10d" };
            case 65 -> new String[] { "Rain", "Heavy rain", "10d" };
            case 66 -> new String[] { "Rain", "Light freezing rain", "10d" };
            case 67 -> new String[] { "Rain", "Heavy freezing rain", "10d" };
            case 71 -> new String[] { "Snow", "Slight snow fall", "13d" };
            case 73 -> new String[] { "Snow", "Moderate snow fall", "13d" };
            case 75 -> new String[] { "Snow", "Heavy snow fall", "13d" };
            case 77 -> new String[] { "Snow", "Snow grains", "13d" };
            case 80 -> new String[] { "Rain", "Slight rain showers", "09d" };
            case 81 -> new String[] { "Rain", "Moderate rain showers", "09d" };
            case 82 -> new String[] { "Rain", "Violent rain showers", "09d" };
            case 85 -> new String[] { "Snow", "Slight snow showers", "13d" };
            case 86 -> new String[] { "Snow", "Heavy snow showers", "13d" };
            case 95 -> new String[] { "Thunderstorm", "Thunderstorm", "11d" };
            case 96 -> new String[] { "Thunderstorm", "Thunderstorm with slight hail", "11d" };
            case 99 -> new String[] { "Thunderstorm", "Thunderstorm with heavy hail", "11d" };
            default -> new String[] { "Unknown", "Unknown weather condition", "01d" };
        };
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
        log.info("Starting historical weather update for current location");

        try {
            // Perform cleanup before updating
            cleanupOldHistoricalWeather();

            // Update historical weather for current location
            Optional<LocationData> currentLocation = ipLocationService.getCurrentLocation();
            if (currentLocation.isPresent()) {
                LocationData location = currentLocation.get();
                getHistoricalWeatherForLocation(location.getLatitude(), location.getLongitude(), location);
                log.info("Successfully updated historical weather for current location: {}, {}",
                        location.getCity(), location.getCountry());
            } else {
                log.warn("Unable to determine current location for historical weather update");
            }
        } catch (Exception e) {
            log.error("Error occurred during historical weather update for current location", e);
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
     * Retrieves historical weather for a specific date.
     */
    public HistoricalWeather getHistoricalWeatherForDate(LocalDate date) {
        log.debug("Retrieving historical weather for date: {}", date);
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
}