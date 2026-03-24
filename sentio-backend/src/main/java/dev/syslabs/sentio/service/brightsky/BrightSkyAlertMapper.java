package dev.syslabs.sentio.service.brightsky;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.model.WeatherAlert;
import dev.syslabs.sentio.repository.WeatherAlertRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mapper component for converting BrightSky alerts payloads into weather alert
 * entities.
 * Handles update-or-insert mapping based on BrightSky alert identifiers.
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
public class BrightSkyAlertMapper {

    private final WeatherAlertRepository weatherAlertRepository;

    public BrightSkyAlertMapper(WeatherAlertRepository weatherAlertRepository) {
        this.weatherAlertRepository = weatherAlertRepository;
    }

    public List<WeatherAlert> mapAlerts(JsonNode payload, String deviceId) {
        List<WeatherAlert> processedAlerts = new ArrayList<>();

        JsonNode alertsNode = payload.get("alerts");
        JsonNode locationNode = payload.get("location");

        if (alertsNode != null && alertsNode.isArray()) {
            for (JsonNode alertNode : alertsNode) {
                WeatherAlert alert = processAlertNode(alertNode, locationNode, deviceId);
                if (alert != null) {
                    processedAlerts.add(alert);
                }
            }
        }

        return processedAlerts;
    }

    private WeatherAlert processAlertNode(JsonNode alertNode, JsonNode locationNode, String deviceId) {
        try {
            String alertId = alertNode.get("alert_id").asText();
            Optional<WeatherAlert> existingAlert = weatherAlertRepository.findByAlertIdAndDeviceId(alertId, deviceId);
            WeatherAlert alert = existingAlert.orElse(new WeatherAlert());

            alert.setDeviceId(deviceId);
            alert.setAlertId(alertId);
            alert.setBrightSkyId(alertNode.get("id").asInt());
            alert.setStatus(getTextValue(alertNode, "status"));

            alert.setEffective(parseDateTime(alertNode, "effective"));
            alert.setOnset(parseDateTime(alertNode, "onset"));
            alert.setExpires(parseDateTime(alertNode, "expires"));

            alert.setCategory(getTextValue(alertNode, "category"));
            alert.setResponseType(getTextValue(alertNode, "response_type"));
            alert.setUrgency(getTextValue(alertNode, "urgency"));
            alert.setSeverity(getTextValue(alertNode, "severity"));
            alert.setCertainty(getTextValue(alertNode, "certainty"));

            if (alertNode.has("event_code") && !alertNode.get("event_code").isNull()) {
                alert.setEventCode(alertNode.get("event_code").asInt());
            }
            alert.setEventEn(getTextValue(alertNode, "event_en"));
            alert.setEventDe(getTextValue(alertNode, "event_de"));

            alert.setHeadlineEn(getTextValue(alertNode, "headline_en"));
            alert.setHeadlineDe(getTextValue(alertNode, "headline_de"));
            alert.setDescriptionEn(getTextValue(alertNode, "description_en"));
            alert.setDescriptionDe(getTextValue(alertNode, "description_de"));
            alert.setInstructionEn(getTextValue(alertNode, "instruction_en"));
            alert.setInstructionDe(getTextValue(alertNode, "instruction_de"));

            if (locationNode != null) {
                if (locationNode.has("warn_cell_id") && !locationNode.get("warn_cell_id").isNull()) {
                    alert.setWarnCellId(locationNode.get("warn_cell_id").asLong());
                }
                alert.setName(getTextValue(locationNode, "name"));
                alert.setNameShort(getTextValue(locationNode, "name_short"));
                alert.setDistrict(getTextValue(locationNode, "district"));
                alert.setState(getTextValue(locationNode, "state"));
                alert.setStateShort(getTextValue(locationNode, "state_short"));

                alert.setCity(getTextValue(locationNode, "name_short"));
                alert.setCountry("Germany");
            }

            return alert;
        } catch (Exception e) {
            log.error("Error processing alert node", e);
            return null;
        }
    }

    private String getTextValue(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText();
        }
        return null;
    }

    private LocalDateTime parseDateTime(JsonNode node, String fieldName) {
        String dateTimeStr = getTextValue(node, fieldName);
        if (dateTimeStr != null) {
            try {
                return LocalDateTime.parse(dateTimeStr.substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                log.warn("Failed to parse datetime: {}", dateTimeStr);
                return null;
            }
        }
        return null;
    }
}