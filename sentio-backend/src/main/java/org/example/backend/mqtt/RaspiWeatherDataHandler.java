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
        // Null check for payload
        if (payload == null) {
            log.warn("Received null weather data payload, skipping processing");
            return;
        }

        log.debug("Processing weather data payload of length: {}", payload.length());

        try {
            log.info("Processing incoming weather data from MQTT");
            JsonNode rootNode = objectMapper.readTree(payload);

            // Validate required fields before accessing them
            if (!hasNonNull(rootNode, "device_id")
                    || !hasNonNull(rootNode, "location")
                    || !hasNonNull(rootNode, "timestamp")
                    || !hasNonNull(rootNode, "temperature")
                    || !hasNonNull(rootNode, "humidity")
                    || !hasNonNull(rootNode, "pressure")
                    || !hasNonNull(rootNode, "lux")
                    || !hasNonNull(rootNode, "uvi")) {

                log.warn("Missing one or more required fields in weather data payload: {}",
                        rootNode.toString());
                return;
            }

            // Extract metadata
            String deviceId = rootNode.get("device_id").asText();
            String location = rootNode.get("location").asText();
            String timestampStr = rootNode.get("timestamp").asText();
            LocalDateTime timestamp;
            try {
                timestamp = LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (java.time.format.DateTimeParseException dtpe) {
                log.error("Malformed timestamp '{}': {}", timestampStr, dtpe.getMessage());
                return;
            }

            // Extract sensor readings with validation
            JsonNode tempNode = rootNode.get("temperature");
            JsonNode humNode = rootNode.get("humidity");
            JsonNode pressNode = rootNode.get("pressure");
            JsonNode luxNode = rootNode.get("lux");
            JsonNode uviNode = rootNode.get("uvi");

            // Validate that numeric fields are actually numeric
            if (!tempNode.isNumber() || !humNode.isNumber() || !pressNode.isNumber() ||
                !luxNode.isNumber() || !uviNode.isNumber()) {
                log.warn("One or more sensor readings have invalid (non-numeric) values in payload");
                return;
            }

            float temperature = (float) tempNode.asDouble();
            float humidity = (float) humNode.asDouble();
            float pressure = (float) pressNode.asDouble();
            float lux = (float) luxNode.asDouble();
            float uvi = (float) uviNode.asDouble();

            // Gas resistance from BME688 (optional - may not be present in older data)
            Integer gasResistance = null;
            if (rootNode.has("gas_resistance") && !rootNode.get("gas_resistance").isNull()) {
                gasResistance = rootNode.get("gas_resistance").asInt();
            }

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
            raspiWeatherData.setGasResistance(gasResistance);

            // Log parsed weather measurements
            log.info(
                    "Parsed weather data - Temperature: {}°C, Humidity: {}%, Pressure: {} hPa, Lux: {} lux, UVI: {}, Gas: {} Ω, Timestamp: {}",
                    temperature, humidity, pressure, lux, uvi, gasResistance, timestamp);

            // Persist weather data through service
            RaspiWeatherData saved = raspiWeatherDataService.saveWeatherData(raspiWeatherData);
            log.info("Successfully processed and saved weather data with ID: {}", saved.getId());

        } catch (Exception e) {
            log.error("Error processing weather data payload: {}", e.getMessage(), e);
        }
    }

    /**
     * Convenience method to check if a required field exists and is non-null.
     */
    private boolean hasNonNull(JsonNode node, String fieldName) {
        if (!node.has(fieldName) || node.get(fieldName).isNull()) {
            log.warn("Required field '{}' is missing or null in payload: {}", fieldName, node.toString());
            return false;
        }
        return true;
    }
}