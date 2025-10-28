package org.example.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.RaspiWeatherData;
import org.example.backend.service.RaspiWeatherDataService;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * MQTT message handler responsible for processing incoming weather sensor data.
 * Handles JSON payload deserialization and persistence of weather measurements.
 * <p>
 * This handler processes real-time weather data received via MQTT from IoT sensors,
 * deserializes the JSON payloads, and persists the data through the weather service.
 * It includes comprehensive error handling and logging for monitoring data flow.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class RaspiWeatherDataHandler {

    private final RaspiWeatherDataService raspiWeatherDataService;
    private final ObjectMapper objectMapper;

    /**
     * Initializes the weather data handler with required dependencies.
     * Configures ObjectMapper with JavaTime module for LocalDateTime support.
     *
     * @param raspiWeatherDataService Service for weather data persistence operations
     */
    public RaspiWeatherDataHandler(RaspiWeatherDataService raspiWeatherDataService) {
        this.raspiWeatherDataService = raspiWeatherDataService;
        this.objectMapper = new ObjectMapper();
        // Register JavaTimeModule to handle LocalDateTime serialization
        this.objectMapper.registerModule(new JavaTimeModule());
        log.info("WeatherDataHandler initialized with JavaTime module support");
    }

    /**
     * Processes incoming weather data payload from MQTT messages.
     * Deserializes JSON payload, validates data, and persists through weather service.
     *
     * @param payload JSON string containing weather sensor measurements
     */
    public void processWeatherData(String payload) {
        log.debug("Processing weather data payload of length: {}", payload.length());

        try {
            log.info("Processing incoming weather data from MQTT");
            log.debug("Raw payload: {}", payload);

            // Deserialize JSON payload to WeatherData object
            RaspiWeatherData raspiWeatherData = objectMapper.readValue(payload, RaspiWeatherData.class);

            // Log parsed weather measurements
            log.info("Parsed weather data - Temperature: {}°C, Humidity: {}%, Pressure: {} hPa, Lux: {} lux, UVI: {}, Timestamp: {}",
                    raspiWeatherData.getTemperature(), raspiWeatherData.getHumidity(), raspiWeatherData.getPressure(),
                    raspiWeatherData.getLux(), raspiWeatherData.getUvi(), raspiWeatherData.getTimestamp());

            // Persist weather data through service
            RaspiWeatherData saved = raspiWeatherDataService.saveWeatherData(raspiWeatherData);
            log.info("Successfully processed and saved weather data with ID: {}", saved.getId());

        } catch (IOException e) {
            log.error("JSON deserialization failed for weather data payload - Error: {}, Payload: {}",
                    e.getMessage(), payload);
        } catch (Exception e) {
            log.error("Unexpected error processing weather data payload - Error: {}, Payload: {}",
                    e.getMessage(), payload, e);
        }
    }
}