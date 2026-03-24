package dev.syslabs.sentio.service.historical;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.model.HistoricalWeather;
import dev.syslabs.sentio.model.LocationData;
import dev.syslabs.sentio.repository.HistoricalWeatherRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Mapper component for converting Open-Meteo archive payloads into historical weather entities.
 * Handles update/insert mapping, value extraction, and location enrichment.
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class HistoricalWeatherEntityMapper {

    private final HistoricalWeatherRepository historicalWeatherRepository;

    public HistoricalWeatherEntityMapper(HistoricalWeatherRepository historicalWeatherRepository) {
        this.historicalWeatherRepository = historicalWeatherRepository;
    }

    /**
     * Processes archive API response and persists mapped historical records.
     *
     * @param jsonNode     Archive API response
     * @param locationData Device/location metadata
     * @param targetDates  Target dates to include from the response
     * @return Persisted historical weather records
     */
    public List<HistoricalWeather> processHistoricalWeatherResponse(JsonNode jsonNode,
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

        Map<LocalDate, HistoricalWeather> existingMap = new HashMap<>();
        String deviceId = locationData.getDeviceId();
        for (LocalDate date : targetDates) {
            Optional<HistoricalWeather> existing = deviceId != null
                    ? historicalWeatherRepository.findByWeatherDateAndDeviceId(date, deviceId)
                    : historicalWeatherRepository.findByWeatherDateAndLocation(
                            date, locationData.getCity(), locationData.getCountry());
            existing.ifPresent(hw -> existingMap.put(date, hw));
        }

        int newCount = 0;
        int updatedCount = 0;

        for (int i = 0; i < timeArray.size(); i++) {
            try {
                String dateStr = timeArray.get(i).asText();
                LocalDate weatherDate = LocalDate.parse(dateStr);

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

                historical.setWeatherDate(weatherDate);

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

                extractTimeValue(dailyNode, "sunrise", i, historical::setSunrise);
                extractTimeValue(dailyNode, "sunset", i, historical::setSunset);

                if (historical.getWeatherCode() != null) {
                    String[] weatherInfo = mapWeatherCode(historical.getWeatherCode());
                    historical.setWeatherMain(weatherInfo[0]);
                    historical.setDescription(weatherInfo[1]);
                    historical.setIcon(weatherInfo[2]);
                }

                setLocationData(historical, locationData);
                historicalData.add(historical);

            } catch (Exception e) {
                log.error("Error processing historical weather data at index {}", i, e);
            }
        }

        List<HistoricalWeather> savedRecords = historicalWeatherRepository.saveAll(historicalData);
        log.info("Processed {} historical weather records - {} new, {} updated",
                savedRecords.size(), newCount, updatedCount);

        return savedRecords;
    }

    private void extractValue(JsonNode dailyNode, String fieldName, int index, Consumer<Float> setter) {
        JsonNode array = dailyNode.get(fieldName);
        if (array != null && array.isArray() && index < array.size() && !array.get(index).isNull()) {
            setter.accept(array.get(index).floatValue());
        }
    }

    private void extractTimeValue(JsonNode dailyNode, String fieldName, int index, Consumer<LocalDateTime> setter) {
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

    private void setLocationData(HistoricalWeather historical, LocationData locationData) {
        if (locationData != null) {
            historical.setCity(locationData.getCity());
            historical.setCountry(locationData.getCountry());
            historical.setLatitude(locationData.getLatitude());
            historical.setLongitude(locationData.getLongitude());
            historical.setIpAddress(locationData.getIpAddress());
            historical.setDetectedLocation(locationData.getCity() + ", " + locationData.getCountry());
            if (locationData.getDeviceId() != null) {
                historical.setDeviceId(locationData.getDeviceId());
            }
        }
    }

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
}