package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.LocationData;
import org.example.backend.model.WeatherForecast;
import org.example.backend.repository.WeatherForecastRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for managing weather forecast data operations.
 * Updated to use Open-Meteo hourly forecast API exclusively.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherForecastService {

    private final WeatherForecastRepository weatherForecastRepository;
    private final IpLocationService ipLocationService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private volatile boolean isUpdating = false;

    @Value("${openmeteo.api.base-url:https://api.open-meteo.com/v1}")
    private String baseUrl;

    /**
     * Retrieves weather forecasts for the current user's location based on IP geolocation.
     */
    public List<WeatherForecast> getForecastForCurrentLocation() {
        log.debug("Retrieving forecast for current location");
        Optional<LocationData> currentLocation = ipLocationService.getCurrentLocation();
        if (currentLocation.isPresent()) {
            return getForecastForLocation(currentLocation.get().getLatitude(),
                    currentLocation.get().getLongitude(),
                    currentLocation.get());
        }
        log.warn("Unable to determine current location for forecast retrieval");
        return new ArrayList<>();
    }

    /**
     * Fetches and processes hourly weather forecasts for a specific geographic location.
     * Uses the new Open-Meteo hourly API parameters.
     */
    @Transactional
    public List<WeatherForecast> getForecastForLocation(Float latitude, Float longitude, LocationData locationData) {
        log.info("Processing hourly weather forecast for location: {}, {} (lat: {}, lon: {})",
                locationData.getCity(), locationData.getCountry(), latitude, longitude);

        try {
            // Clean up expired forecasts before fetching new data
            weatherForecastRepository.deleteExpiredForecasts(LocalDate.now().minusDays(1));
            log.debug("Cleaned up expired forecasts before processing new data");

            // Updated Open-Meteo API URL with the specified hourly parameters
            String url = String.format(
                    "%s/forecast?latitude=%f&longitude=%f&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,snow_depth,weather_code,surface_pressure,cloud_cover,visibility,wind_speed_10m,rain,showers,snowfall,dew_point_2m,precipitation_probability,wind_direction_10m,wind_gusts_10m&timezone=auto&forecast_days=7",
                    baseUrl, latitude, longitude);

            log.info("Fetching weather forecast from Open-Meteo: {}", url);
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            // Get existing forecasts for intelligent update/insert logic
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = LocalDate.now().plusDays(7);
            List<WeatherForecast> existingForecasts = weatherForecastRepository.findByLocationAndDateRange(
                    locationData.getCity(), locationData.getCountry(), startDate, endDate);

            // Create lookup map for existing forecasts
            Map<LocalDateTime, WeatherForecast> existingMap = existingForecasts.stream()
                    .collect(Collectors.toMap(WeatherForecast::getForecastDateTime, f -> f));

            // Process hourly data from Open-Meteo
            JsonNode hourlyNode = jsonNode.get("hourly");
            List<WeatherForecast> processedForecasts = new ArrayList<>();

            if (hourlyNode != null) {
                processedForecasts = processHourlyForecast(hourlyNode, locationData, existingMap);
            }

            // Persist all processed forecasts
            List<WeatherForecast> savedForecasts = weatherForecastRepository.saveAll(processedForecasts);

            log.info("Successfully processed {} hourly weather forecasts for location: {}, {}",
                    savedForecasts.size(), locationData.getCity(), locationData.getCountry());

            return savedForecasts;

        } catch (Exception e) {
            log.error("Failed to fetch weather forecast for location: {}, {} (lat: {}, lon: {})",
                    locationData.getCity(), locationData.getCountry(), latitude, longitude, e);
            return new ArrayList<>();
        }
    }

    /**
     * Processes hourly forecast data from Open-Meteo API response.
     */
    private List<WeatherForecast> processHourlyForecast(JsonNode hourlyNode, LocationData locationData,
                                                        Map<LocalDateTime, WeatherForecast> existingMap) {

        List<WeatherForecast> forecasts = new ArrayList<>();
        JsonNode timeArray = hourlyNode.get("time");

        if (timeArray == null || !timeArray.isArray()) {
            return forecasts;
        }

        for (int i = 0; i < timeArray.size(); i++) {
            String dateTimeStr = timeArray.get(i).asText();
            LocalDateTime forecastDateTime = LocalDateTime.parse(dateTimeStr);
            LocalDate forecastDate = forecastDateTime.toLocalDate();

            // Skip forecasts beyond 7-day range or more than 3 hours old
            if (forecastDate.isAfter(LocalDate.now().plusDays(7)) ||
                    forecastDateTime.isBefore(LocalDateTime.now().minusHours(3))) {
                continue;
            }

            WeatherForecast forecast = existingMap.get(forecastDateTime);
            boolean isUpdate = forecast != null;

            if (!isUpdate) {
                forecast = new WeatherForecast();
            }

            // Set forecast timing
            forecast.setForecastDate(forecastDate);
            forecast.setForecastDateTime(forecastDateTime);

            // Extract all the new hourly parameters
            extractHourlyValue(hourlyNode, "temperature_2m", i, forecast::setTemperature);
            extractHourlyValue(hourlyNode, "relative_humidity_2m", i, forecast::setHumidity);
            extractHourlyValue(hourlyNode, "apparent_temperature", i, forecast::setApparentTemperature);
            extractHourlyValue(hourlyNode, "surface_pressure", i, forecast::setPressure);
            extractHourlyValue(hourlyNode, "wind_speed_10m", i, forecast::setWindSpeed);
            extractHourlyValue(hourlyNode, "wind_direction_10m", i, forecast::setWindDirection);
            extractHourlyValue(hourlyNode, "wind_gusts_10m", i, forecast::setWindGusts);
            extractHourlyValue(hourlyNode, "cloud_cover", i, forecast::setCloudCover);
            extractHourlyValue(hourlyNode, "visibility", i, forecast::setVisibility);
            extractHourlyValue(hourlyNode, "precipitation", i, forecast::setPrecipitation);
            extractHourlyValue(hourlyNode, "rain", i, forecast::setRain);
            extractHourlyValue(hourlyNode, "showers", i, forecast::setShowers);
            extractHourlyValue(hourlyNode, "snowfall", i, forecast::setSnowfall);
            extractHourlyValue(hourlyNode, "snow_depth", i, forecast::setSnowDepth);
            extractHourlyValue(hourlyNode, "dew_point_2m", i, forecast::setDewPoint);
            extractHourlyValue(hourlyNode, "precipitation_probability", i, forecast::setPrecipitationProbability);

            // Extract weather code and map to description
            JsonNode weatherCodeArray = hourlyNode.get("weather_code");
            if (weatherCodeArray != null && weatherCodeArray.isArray() && i < weatherCodeArray.size() && !weatherCodeArray.get(i).isNull()) {
                int weatherCode = weatherCodeArray.get(i).asInt();
                forecast.setWeatherCode(weatherCode);

                String[] weatherInfo = mapWeatherCode(weatherCode);
                forecast.setWeatherMain(weatherInfo[0]);
                forecast.setDescription(weatherInfo[1]);
                forecast.setIcon(weatherInfo[2]);
            }

            // Set location information
            setLocationData(forecast, locationData);

            forecasts.add(forecast);
        }

        log.info("Processed {} hourly forecasts", forecasts.size());
        return forecasts;
    }

    /**
     * Extracts an hourly value from the JSON node safely.
     */
    private void extractHourlyValue(JsonNode hourlyNode, String fieldName, int index, java.util.function.Consumer<Float> setter) {
        JsonNode array = hourlyNode.get(fieldName);
        if (array != null && array.isArray() && index < array.size() && !array.get(index).isNull()) {
            setter.accept(array.get(index).floatValue());
        }
    }

    /**
     * Sets location data on the forecast entity.
     */
    private void setLocationData(WeatherForecast forecast, LocationData locationData) {
        if (locationData != null) {
            forecast.setCity(locationData.getCity());
            forecast.setCountry(locationData.getCountry());
            forecast.setLatitude(locationData.getLatitude());
            forecast.setLongitude(locationData.getLongitude());
            forecast.setIpAddress(locationData.getIpAddress());
            forecast.setDetectedLocation(locationData.getCity() + ", " + locationData.getCountry());
        }
    }

    /**
     * Maps Open-Meteo weather codes to descriptive weather information.
     * Based on WMO Weather interpretation codes.
     */
    private String[] mapWeatherCode(int code) {
        return switch (code) {
            case 0 -> new String[]{"Clear", "Clear sky", "01d"};
            case 1 -> new String[]{"Clouds", "Mainly clear", "02d"};
            case 2 -> new String[]{"Clouds", "Partly cloudy", "03d"};
            case 3 -> new String[]{"Clouds", "Overcast", "04d"};
            case 45 -> new String[]{"Mist", "Fog", "50d"};
            case 48 -> new String[]{"Mist", "Depositing rime fog", "50d"};
            case 51 -> new String[]{"Drizzle", "Light drizzle", "09d"};
            case 53 -> new String[]{"Drizzle", "Moderate drizzle", "09d"};
            case 55 -> new String[]{"Drizzle", "Dense drizzle", "09d"};
            case 56 -> new String[]{"Drizzle", "Light freezing drizzle", "09d"};
            case 57 -> new String[]{"Drizzle", "Dense freezing drizzle", "09d"};
            case 61 -> new String[]{"Rain", "Slight rain", "10d"};
            case 63 -> new String[]{"Rain", "Moderate rain", "10d"};
            case 65 -> new String[]{"Rain", "Heavy rain", "10d"};
            case 66 -> new String[]{"Rain", "Light freezing rain", "10d"};
            case 67 -> new String[]{"Rain", "Heavy freezing rain", "10d"};
            case 71 -> new String[]{"Snow", "Slight snow fall", "13d"};
            case 73 -> new String[]{"Snow", "Moderate snow fall", "13d"};
            case 75 -> new String[]{"Snow", "Heavy snow fall", "13d"};
            case 77 -> new String[]{"Snow", "Snow grains", "13d"};
            case 80 -> new String[]{"Rain", "Slight rain showers", "09d"};
            case 81 -> new String[]{"Rain", "Moderate rain showers", "09d"};
            case 82 -> new String[]{"Rain", "Violent rain showers", "09d"};
            case 85 -> new String[]{"Snow", "Slight snow showers", "13d"};
            case 86 -> new String[]{"Snow", "Heavy snow showers", "13d"};
            case 95 -> new String[]{"Thunderstorm", "Thunderstorm", "11d"};
            case 96 -> new String[]{"Thunderstorm", "Thunderstorm with slight hail", "11d"};
            case 99 -> new String[]{"Thunderstorm", "Thunderstorm with heavy hail", "11d"};
            default -> new String[]{"Unknown", "Unknown weather condition", "01d"};
        };
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
        log.info("Starting forecast update for current location");

        try {
            cleanupOldForecasts();
            Optional<LocationData> currentLocation = ipLocationService.getCurrentLocation();
            if (currentLocation.isPresent()) {
                LocationData location = currentLocation.get();
                getForecastForLocation(location.getLatitude(), location.getLongitude(), location);
                log.info("Successfully updated forecasts for current location: {}, {}",
                        location.getCity(), location.getCountry());
            } else {
                log.warn("Unable to determine current location for forecast update");
            }
        } catch (Exception e) {
            log.error("Error occurred during forecast update for current location", e);
        } finally {
            isUpdating = false;
        }
    }

    public List<String> getAvailableCities() {
        log.debug("Retrieving available cities with upcoming forecasts");
        return weatherForecastRepository.findDistinctCitiesWithUpcomingForecasts(LocalDate.now());
    }
}