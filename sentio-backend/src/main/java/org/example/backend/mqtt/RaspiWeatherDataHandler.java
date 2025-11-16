package org.example.backend.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.RaspiWeatherData;
import org.example.backend.service.RaspiWeatherDataService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MQTT message handler responsible for processing incoming weather sensor data.
 * Handles JSON payload parsing and persistence of weather measurements.
 */
@Component
@Slf4j
public class RaspiWeatherDataHandler {

    private final RaspiWeatherDataService raspiWeatherDataService;
    private final ObjectMapper objectMapper;

    public RaspiWeatherDataHandler(RaspiWeatherDataService raspiWeatherDataService) {
        this.raspiWeatherDataService = raspiWeatherDataService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        log.info("WeatherDataHandler initialized with JavaTime module support");
    }

    public void processWeatherData(String payload) {
        log.debug("Processing weather data payload of length: {}", payload.length());

        try {
            log.info("Processing incoming weather data from MQTT");
            JsonNode rootNode = objectMapper.readTree(payload);

            // Extract metadata (like AnimalDetection does)
            String deviceId = rootNode.get("device_id").asText();
            String location = rootNode.get("location").asText();
            String timestampStr = rootNode.get("timestamp").asText();
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Extract sensor readings
            float temperature = rootNode.get("temperature").floatValue();
            float humidity = rootNode.get("humidity").floatValue();
            float pressure = rootNode.get("pressure").floatValue();
            float lux = rootNode.get("lux").floatValue();
            float uvi = rootNode.get("uvi").floatValue();

            // Create weather data object
            RaspiWeatherData raspiWeatherData = new RaspiWeatherData();
            raspiWeatherData.setDeviceId(deviceId);
            raspiWeatherData.setLocation(location);
            raspiWeatherData.setTimestamp(timestamp);
            raspiWeatherData.setTemperature(temperature);
            raspiWeatherData.setHumidity(humidity);
            raspiWeatherData.setPressure(pressure);
            raspiWeatherData.setLux(lux);
            raspiWeatherData.setUvi(uvi);

            // Log parsed weather measurements
            log.info("Parsed weather data - Temperature: {}°C, Humidity: {}%, Pressure: {} hPa, Lux: {} lux, UVI: {}, Timestamp: {}",
                    temperature, humidity, pressure, lux, uvi, timestamp);

            // Persist weather data through service
            RaspiWeatherData saved = raspiWeatherDataService.saveWeatherData(raspiWeatherData);
            log.info("Successfully processed and saved weather data with ID: {}", saved.getId());

        } catch (Exception e) {
            log.error("Error processing weather data payload: {}", e.getMessage(), e);
        }
    }
}