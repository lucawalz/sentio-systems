package dev.syslabs.sentio.service;

import dev.syslabs.sentio.dto.RadarMetadataDTO;
import dev.syslabs.sentio.model.WeatherAlert;
import dev.syslabs.sentio.model.WeatherRadarMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Interface contract for BrightSky weather alert and radar operations.
 */
public interface IBrightSkyService {
    List<WeatherAlert> getAlertsForCurrentLocation();

    List<WeatherAlert> getAlertsForLocation(Float latitude, Float longitude, String deviceId);

    List<WeatherAlert> getActiveAlerts();

    List<WeatherAlert> getAlertsByWarnCellId(Long warnCellId);

    List<WeatherAlert> getAlertsByCity(String city);

    List<WeatherAlert> getAlertsBySeverity(String severity);

    List<WeatherAlert> getActiveAlertsForLocation(String city, Long warnCellId);

    List<WeatherAlert> getAlertsForDevice(String deviceId);

    void updateAlertsForCurrentLocation();

    void updateAlertsForAllDeviceLocations();

    void cleanupExpiredAlerts();

    String getRadarEndpointUrl(Float latitude, Float longitude, Integer distance, String format);

    String getRadarEndpointUrlForCurrentLocation(Integer distance, String format);

    List<WeatherAlert> getRecentAlerts();

    List<String> getCitiesWithActiveAlerts();

    RadarMetadataDTO fetchAndStoreRadarMetadata(Float latitude, Float longitude, Integer distance, String format);

    RadarMetadataDTO fetchAndStoreRadarMetadataForCurrentLocation(Integer distance);

    String getRadarEndpointForDevice(String deviceId);

    RadarMetadataDTO fetchRadarMetadataForDevice(String deviceId, Integer distance);

    Optional<WeatherRadarMetadata> getLatestRadarMetadata();

    List<WeatherRadarMetadata> getRecentRadarMetadata(int hours);

    void cleanupOldRadarMetadata();
}
