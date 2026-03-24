package dev.syslabs.sentio.service.historical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Client component for retrieving historical weather payloads from Open-Meteo Archive API.
 * Encapsulates request URL construction and JSON response parsing.
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class OpenMeteoArchiveClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openmeteo.archive.base-url:https://archive-api.open-meteo.com/v1}")
    private String archiveBaseUrl;

    public OpenMeteoArchiveClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Fetches daily archive weather data for the specified date range and coordinates.
     *
     * @param latitude  Geographic latitude coordinate
     * @param longitude Geographic longitude coordinate
     * @param startDate Range start date (inclusive)
     * @param endDate   Range end date (inclusive)
     * @return Parsed JSON response payload
     * @throws Exception if API call or JSON parsing fails
     */
    public JsonNode fetchHistoricalRange(Float latitude, Float longitude, LocalDate startDate, LocalDate endDate)
            throws Exception {
        String url = String.format(
                "%s/forecast?latitude=%f&longitude=%f&start_date=%s&end_date=%s&daily=weather_code,temperature_2m_max,temperature_2m_min,sunset,sunrise,daylight_duration,sunshine_duration,uv_index_max,precipitation_sum,precipitation_hours,wind_speed_10m_max,wind_direction_10m_dominant&timezone=auto",
                archiveBaseUrl, latitude, longitude,
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

        log.info("Fetching historical weather from OpenMeteo Archive: {}", url);
        String response = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(response);
    }
}