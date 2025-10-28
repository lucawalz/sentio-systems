package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.RaspiWeatherData;
import org.example.backend.repository.RaspiWeatherDataRepository;
import org.example.backend.controller.RaspiWeatherController.WeatherStats;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for managing weather data operations and statistics.
 * Handles persistence, retrieval, and statistical analysis of weather sensor readings.
 * <p>
 * This service provides core functionality for storing weather measurements
 * from sensors and generating statistical summaries over different time periods.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RaspiWeatherDataService {

    private final RaspiWeatherDataRepository raspiWeatherDataRepository;

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
     * Retrieves the most recent weather data reading.
     *
     * @return Latest weather data entry, null if no data exists
     */
    public RaspiWeatherData getLatestWeatherData() {
        log.debug("Retrieving latest weather data");
        return raspiWeatherDataRepository.findTopByOrderByTimestampDesc();
    }

    /**
     * Retrieves weather data from the last 24 hours.
     *
     * @return List of weather data from the past day, ordered by timestamp (newest first)
     */
    public List<RaspiWeatherData> getRecentWeatherData() {
        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        log.debug("Retrieving weather data since: {}", dayAgo);
        return raspiWeatherDataRepository.findRecentData(dayAgo);
    }

    /**
     * Retrieves all weather data entries ordered by timestamp.
     *
     * @return Complete list of weather data ordered by timestamp (newest first)
     */
    public List<RaspiWeatherData> getAllWeatherData() {
        log.debug("Retrieving all weather data");
        return raspiWeatherDataRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Generates comprehensive weather statistics based on recent data.
     * Calculates averages for temperature, humidity, and pressure over the last 24 hours.
     *
     * @return WeatherStats object containing total readings count, latest data, and averages
     */
    public WeatherStats getWeatherStats() {
        log.debug("Calculating weather statistics");

        Long totalReadings = raspiWeatherDataRepository.count();
        RaspiWeatherData latest = getLatestWeatherData();

        // Calculate averages from last 24 hours
        List<RaspiWeatherData> recentData = getRecentWeatherData();

        log.debug("Processing {} recent readings for statistics", recentData.size());

        Double avgTemp = recentData.stream()
                .filter(w -> w.getTemperature() != null)
                .mapToDouble(RaspiWeatherData::getTemperature)
                .average()
                .orElse(0.0);

        Double avgHumidity = recentData.stream()
                .filter(w -> w.getHumidity() != null)
                .mapToDouble(RaspiWeatherData::getHumidity)
                .average()
                .orElse(0.0);

        Double avgPressure = recentData.stream()
                .filter(w -> w.getPressure() != null)
                .mapToDouble(RaspiWeatherData::getPressure)
                .average()
                .orElse(0.0);

        log.info("Generated weather stats - Total readings: {}, Avg temp: {:.2f}°C, Avg humidity: {:.1f}%, Avg pressure: {:.1f} hPa",
                totalReadings, avgTemp, avgHumidity, avgPressure);

        return new WeatherStats(totalReadings, latest, avgTemp, avgHumidity, avgPressure);
    }
}