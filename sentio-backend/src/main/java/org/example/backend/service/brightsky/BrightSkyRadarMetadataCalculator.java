package org.example.backend.service.brightsky;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.backend.model.WeatherRadarMetadata;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Calculator component for deriving radar precipitation statistics from BrightSky payloads.
 * Produces populated radar metadata entities ready for persistence.
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class BrightSkyRadarMetadataCalculator {

    public RadarCalculation calculate(JsonNode payload, Float latitude, Float longitude, Integer distance, String deviceId)
            throws Exception {
        JsonNode radarArray = payload.get("radar");
        if (radarArray == null || !radarArray.isArray() || radarArray.isEmpty()) {
            return null;
        }

        JsonNode latestRadar = radarArray.get(0);
        JsonNode precipitation = latestRadar.get("precipitation_5");

        if (precipitation == null || !precipitation.isArray()) {
            return null;
        }

        int totalCells = 0;
        int cellsWithPrecip = 0;
        int significantCells = 0;
        float minPrecip = Float.MAX_VALUE;
        float maxPrecip = 0;
        float sumPrecip = 0;

        for (JsonNode row : precipitation) {
            for (JsonNode cell : row) {
                int value = cell.asInt();
                float mmPer5min = value / 100.0f;

                totalCells++;
                if (value > 0) {
                    cellsWithPrecip++;
                    sumPrecip += mmPer5min;
                    if (mmPer5min < minPrecip) {
                        minPrecip = mmPer5min;
                    }
                    if (mmPer5min > maxPrecip) {
                        maxPrecip = mmPer5min;
                    }
                    if (mmPer5min >= 1.0f) {
                        significantCells++;
                    }
                }
            }
        }

        if (cellsWithPrecip == 0) {
            minPrecip = 0;
        }
        float avgPrecip = cellsWithPrecip > 0 ? sumPrecip / cellsWithPrecip : 0;
        float coveragePercent = totalCells > 0 ? (cellsWithPrecip * 100.0f / totalCells) : 0;

        String timestampStr = latestRadar.get("timestamp").asText();
        LocalDateTime timestamp = LocalDateTime.parse(timestampStr.substring(0, 19), DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        WeatherRadarMetadata metadata = new WeatherRadarMetadata();
        metadata.setTimestamp(timestamp);
        metadata.setSource(latestRadar.has("source") ? latestRadar.get("source").asText() : null);
        metadata.setLatitude(latitude);
        metadata.setLongitude(longitude);
        metadata.setDistance(distance);
        metadata.setDeviceId(deviceId);
        metadata.setPrecipitationMin(minPrecip);
        metadata.setPrecipitationMax(maxPrecip);
        metadata.setPrecipitationAvg(avgPrecip);
        metadata.setCoveragePercent(coveragePercent);
        metadata.setSignificantRainCells(significantCells);
        metadata.setTotalCells(totalCells);

        if (payload.has("geometry")) {
            metadata.setGeometryJson(payload.get("geometry").toString());
        }
        if (payload.has("bbox")) {
            JsonNode bbox = payload.get("bbox");
            metadata.setBboxPixels(String.format("%d,%d,%d,%d",
                    bbox.get(0).asInt(), bbox.get(1).asInt(), bbox.get(2).asInt(), bbox.get(3).asInt()));
        }

        return new RadarCalculation(metadata, cellsWithPrecip > 0);
    }

    @Data
    @AllArgsConstructor
    public static class RadarCalculation {
        private WeatherRadarMetadata metadata;
        private boolean hasActivePrecipitation;
    }
}