package dev.syslabs.sentio.service.brightsky;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client component for BrightSky API communication.
 * Handles weather alerts and radar payload retrieval plus endpoint URL generation.
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class BrightSkyApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${brightsky.api.base-url:https://api.brightsky.dev}")
    private String baseUrl;

    public BrightSkyApiClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public JsonNode fetchAlerts(Float latitude, Float longitude) throws Exception {
        String url = String.format("%s/alerts?lat=%f&lon=%f&tz=Europe/Berlin", baseUrl, latitude, longitude);
        log.info("Fetching weather alerts from BrightSky: {}", url);
        String response = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(response);
    }

    public JsonNode fetchRadar(Float latitude, Float longitude, Integer distance, String format) throws Exception {
        String url = String.format("%s/radar?lat=%f&lon=%f&distance=%d&format=%s&tz=Europe/Berlin",
                baseUrl, latitude, longitude, distance, format);
        log.info("Fetching radar data from BrightSky: {}", url);
        String response = restTemplate.getForObject(url, String.class);
        return objectMapper.readTree(response);
    }

    public String buildRadarUrl(Float latitude, Float longitude, Integer distance, String format) {
        return String.format("%s/radar?lat=%f&lon=%f&distance=%d&format=%s&tz=Europe/Berlin",
                baseUrl, latitude, longitude, distance, format);
    }
}