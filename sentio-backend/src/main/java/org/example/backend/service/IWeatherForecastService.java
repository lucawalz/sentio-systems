package org.example.backend.service;

import org.example.backend.model.LocationData;
import org.example.backend.model.WeatherForecast;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface contract for weather forecast operations.
 */
public interface IWeatherForecastService {
    List<WeatherForecast> getForecastForCurrentLocation();

    List<WeatherForecast> getForecastForLocation(Float latitude, Float longitude, LocationData locationData);

    List<WeatherForecast> getForecastForDevice(String deviceId);

    List<WeatherForecast> getUpcomingForecasts();

    List<WeatherForecast> getForecastsForDateRange(LocalDate startDate, LocalDate endDate);

    List<WeatherForecast> getForecastsForDate(LocalDate date);

    WeatherForecast getLatestForecastForDate(LocalDate date);

    List<WeatherForecast> getRecentForecasts(int hours);

    void cleanupOldForecasts();

    void updateForecastsForCurrentLocation();

    void updateForecastsForAllDeviceLocations();

    List<String> getAvailableCities();
}
