package dev.syslabs.sentio.service.forecast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client component for retrieving hourly forecast data from Open-Meteo API.
 * Encapsulates URL construction and JSON parsing for forecast endpoints.
 * <p>
 * This client isolates external transport concerns from forecast business logic,
 * improving separation of concerns and service testability.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class OpenMeteoForecastClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openmeteo.api.base-url:https://api.open-meteo.com/v1}")
    private String baseUrl;

    public OpenMeteoForecastClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches hourly weather forecast payload from Open-Meteo API.
     *
     * @param latitude  Geographic latitude coordinate
     * @param longitude Geographic longitude coordinate
     * @return Parsed JSON response from Open-Meteo API
     * @throws Exception when API call or JSON parsing fails
     */
    public JsonNode fetchHourlyForecast(Float latitude, Float longitude) throws Exception {
        String url = String.format(
                "%s/forecast?latitude=%f&longitude=%f&hourly=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,snow_depth,weather_code,surface_pressure,cloud_cover,visibility,wind_speed_10m,rain,showers,snowfall,dew_point_2m,precipitation_probability,wind_direction_10m,wind_gusts_10m&timezone=auto&forecast_days=7",
                baseUrl, latitude, longitude);

        log.info("Fetching weather forecast from Open-Meteo: {}", url);
        String response = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(response);
    }
}