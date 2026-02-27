package org.example.backend.service.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.LocationData;
import org.example.backend.model.WeatherForecast;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Mapper component for transforming Open-Meteo hourly JSON payloads into forecast entities.
 * Applies time filtering, weather-code mapping, and location enrichment.
 * <p>
 * This component contains data transformation logic only and does not perform
 * persistence or external API calls.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ForecastEntityMapper {

    private final ForecastWeatherCodeMapper weatherCodeMapper;

    /**
     * Converts Open-Meteo hourly forecast data into weather forecast entities.
     *
     * @param hourlyNode   Open-Meteo hourly node
     * @param locationData Location metadata for forecast isolation
     * @param existingMap  Existing forecasts indexed by forecast datetime
     * @return List of mapped forecast entities
     */
    public List<WeatherForecast> processHourlyForecast(JsonNode hourlyNode, LocationData locationData,
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

            if (forecastDate.isAfter(LocalDate.now().plusDays(7)) ||
                    forecastDateTime.isBefore(LocalDateTime.now().minusHours(3))) {
                continue;
            }

            WeatherForecast forecast = existingMap.get(forecastDateTime);
            if (forecast == null) {
                forecast = new WeatherForecast();
            }

            forecast.setForecastDate(forecastDate);
            forecast.setForecastDateTime(forecastDateTime);

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

            JsonNode weatherCodeArray = hourlyNode.get("weather_code");
            if (weatherCodeArray != null && weatherCodeArray.isArray() && i < weatherCodeArray.size()
                    && !weatherCodeArray.get(i).isNull()) {
                int weatherCode = weatherCodeArray.get(i).asInt();
                forecast.setWeatherCode(weatherCode);

                String[] weatherInfo = weatherCodeMapper.mapWeatherCode(weatherCode);
                forecast.setWeatherMain(weatherInfo[0]);
                forecast.setDescription(weatherInfo[1]);
                forecast.setIcon(weatherInfo[2]);
            }

            setLocationData(forecast, locationData);
            forecasts.add(forecast);
        }

        log.info("Processed {} hourly forecasts", forecasts.size());
        return forecasts;
    }

    private void extractHourlyValue(JsonNode hourlyNode, String fieldName, int index, Consumer<Float> setter) {
        JsonNode array = hourlyNode.get(fieldName);
        if (array != null && array.isArray() && index < array.size() && !array.get(index).isNull()) {
            setter.accept(array.get(index).floatValue());
        }
    }

    private void setLocationData(WeatherForecast forecast, LocationData locationData) {
        if (locationData != null) {
            forecast.setCity(locationData.getCity());
            forecast.setCountry(locationData.getCountry());
            forecast.setLatitude(locationData.getLatitude());
            forecast.setLongitude(locationData.getLongitude());
            forecast.setIpAddress(locationData.getIpAddress());
            forecast.setDeviceId(locationData.getDeviceId());
            forecast.setDetectedLocation(locationData.getCity() + ", " + locationData.getCountry());
        }
    }
}