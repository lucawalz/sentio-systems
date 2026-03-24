package dev.syslabs.sentio.service;

import dev.syslabs.sentio.model.HistoricalWeather;
import dev.syslabs.sentio.model.LocationData;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface contract for historical weather operations.
 */
public interface IHistoricalWeatherService {
    List<HistoricalWeather> getHistoricalWeatherForCurrentLocation();

    List<HistoricalWeather> getHistoricalWeatherForLocation(Float latitude, Float longitude, LocationData locationData);

    void updateHistoricalWeatherForCurrentLocation();

    void updateHistoricalWeatherForAllDeviceLocations();

    List<HistoricalWeather> getHistoricalWeatherForDateRange(LocalDate startDate, LocalDate endDate);

    HistoricalWeather getHistoricalWeatherForDate(LocalDate date);

    HistoricalWeather getHistoricalWeatherForDate(LocalDate date, String deviceId);

    void cleanupOldHistoricalWeather();

    List<String> getAvailableCitiesWithHistoricalWeather();
}
