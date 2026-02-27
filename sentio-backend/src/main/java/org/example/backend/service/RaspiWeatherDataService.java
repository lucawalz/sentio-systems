package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.RaspiWeatherData;
import org.example.backend.repository.RaspiWeatherDataRepository;
import org.example.backend.controller.RaspiWeatherController.WeatherStats;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Service responsible for managing weather data operations and statistics.
 * Handles persistence, retrieval, and statistical analysis of weather sensor
 * readings.
 * <p>
 * This service provides core functionality for storing weather measurements
 * from sensors and generating statistical summaries over different time
 * periods.
 * Enables strict data privacy by filtering data based on device ownership.
 * </p>
 *
 * @author Sentio Team
 * @version 1.1
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaspiWeatherDataService {

    private final RaspiWeatherDataRepository raspiWeatherDataRepository;
    private final DeviceService deviceService;

    /**
     * Persists weather data with automatic timestamp assignment if not provided.
     *
     * @param raspiWeatherData The weather data to save
     * @return The saved weather data with generated ID and timestamp
     */
    public RaspiWeatherData saveWeatherData(RaspiWeatherData raspiWeatherData) {
        // Ensure timestamp is set
        if (raspiWeatherData.getTimestamp() == null) {
            raspiWeatherData.setTimestamp(LocalDateTime.now());
        }

        RaspiWeatherData saved = raspiWeatherDataRepository.save(raspiWeatherData);
        log.info("Successfully saved weather data with ID: {} at {}",
                saved.getId(), saved.getTimestamp());
        return saved;
    }

    /**
     * Retrieves the most recent weather data reading visible to the current user.
     *
     * @return Latest weather data entry, null if no data exists or user has no
     *         devices
     */
    public RaspiWeatherData getLatestWeatherData() {
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            log.debug("User has no devices, returning no weather data");
            return null;
        }
        log.debug("Retrieving latest weather data for devices: {}", deviceIds);
        return raspiWeatherDataRepository.findTopByDeviceIdInOrderByTimestampDesc(deviceIds);
    }

    /**
     * Retrieves weather data from the last 24 hours visible to the current user.
     *
     * @return List of weather data from the past day, ordered by timestamp (newest
     *         first)
     */
    public List<RaspiWeatherData> getRecentWeatherData() {
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        log.debug("Retrieving weather data since: {} for devices: {}", dayAgo, deviceIds);
        return raspiWeatherDataRepository.findRecentDataByDevices(deviceIds, dayAgo);
    }

    /**
     * Retrieves all weather data entries ordered by timestamp visible to the
     * current user.
     *
     * @return Complete list of weather data ordered by timestamp (newest first)
     */
    public List<RaspiWeatherData> getAllWeatherData() {
        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return Collections.emptyList();
        }
        log.debug("Retrieving all weather data for devices: {}", deviceIds);
        return raspiWeatherDataRepository.findByDeviceIdInOrderByTimestampDesc(deviceIds);
    }

    /**
     * Generates comprehensive weather statistics based on recent data visible to
     * the current user.
     * Calculates averages for temperature, humidity, and pressure over the last 24
     * hours.
     *
     * @return WeatherStats object containing total readings count, latest data, and
     *         averages
     */
    public WeatherStats getWeatherStats() {
        log.debug("Calculating weather statistics");

        List<String> deviceIds = deviceService.getMyDeviceIds();
        if (deviceIds.isEmpty()) {
            return new WeatherStats(0L, null, 0.0, 0.0, 0.0);
        }

        Long totalReadings = raspiWeatherDataRepository.countByDeviceIdIn(deviceIds);
        RaspiWeatherData latest = getLatestWeatherData();

        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);

        Double avgTemp = raspiWeatherDataRepository.getAverageTemperatureSinceForDevices(deviceIds, dayAgo);
        Double avgHumidity = raspiWeatherDataRepository.getAverageHumiditySinceForDevices(deviceIds, dayAgo);
        Double avgPressure = raspiWeatherDataRepository.getAveragePressureSinceForDevices(deviceIds, dayAgo);

        // Handle nulls if no data in range
        avgTemp = avgTemp != null ? avgTemp : 0.0;
        avgHumidity = avgHumidity != null ? avgHumidity : 0.0;
        avgPressure = avgPressure != null ? avgPressure : 0.0;

        log.info("Generated weather stats for user devices - Total readings: {}, Avg temp: {:.2f}°C",
                totalReadings, avgTemp);

        return new WeatherStats(totalReadings, latest, avgTemp, avgHumidity, avgPressure);
    }


    /**
     * Retrieves the most recent weather data for a specific device.
     *
     * @param deviceId The device UUID
     * @return Latest weather data for the device, null if no data
     * @throws IllegalArgumentException if device not found or not owned by user
     */
    public RaspiWeatherData getLatestForDevice(String deviceId) {
        deviceService.getVerifiedDevice(deviceId);
        return raspiWeatherDataRepository.findTopByDeviceIdInOrderByTimestampDesc(List.of(deviceId));
    }

    /**
     * Retrieves recent weather data for a specific device (last 24 hours).
     *
     * @param deviceId The device UUID
     * @return List of recent weather data for the device
     * @throws IllegalArgumentException if device not found or not owned by user
     */
    public List<RaspiWeatherData> getRecentForDevice(String deviceId) {
        deviceService.getVerifiedDevice(deviceId);
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        return raspiWeatherDataRepository.findRecentDataByDevices(List.of(deviceId), dayAgo);
    }
}