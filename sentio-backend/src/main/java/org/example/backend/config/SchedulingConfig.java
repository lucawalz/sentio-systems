package org.example.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.BrightSkyService;
import org.example.backend.service.HistoricalWeatherService;
import org.example.backend.service.IpLocationService;
import org.example.backend.service.WeatherForecastService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {

    private final WeatherForecastService weatherForecastService;
    private final HistoricalWeatherService historicalWeatherService;
    private final IpLocationService ipLocationService;
    private final BrightSkyService brightSkyService;

    /**
     * Update weather on application ready.
     * Weather updates now happen via scheduled tasks using device locations only.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready - weather updates will be handled by scheduled tasks for device locations");
        // Removed immediate updates to avoid API calls before devices are registered
        // All weather updates now happen via scheduled tasks using device-only
        // locations
    }

    /**
     * Update weather forecasts daily at midnight (00:00) for all device locations
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateDailyWeatherForecasts() {
        log.info("Starting daily weather forecast update for device locations");
        weatherForecastService.updateForecastsForAllDeviceLocations();
    }

    /**
     * Update historical weather data daily at 1:00 AM for all device locations
     * This ensures we have the latest historical data for comparison
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void updateDailyHistoricalWeather() {
        log.info("Starting daily historical weather update for device locations");
        historicalWeatherService.updateHistoricalWeatherForAllDeviceLocations();
    }

    /**
     * Update weather alerts every 30 minutes for all device locations
     * Weather alerts can change frequently, so we check more often than forecasts
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void updateWeatherAlerts() {
        log.info("Starting weather alerts update for device locations (every 30 minutes)");
        brightSkyService.updateAlertsForAllDeviceLocations();
    }

    /**
     * Clean up old forecast data every day at 2:00 AM (2-day retention for
     * forecasts)
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldForecastData() {
        log.info("Starting daily cleanup of old forecast data (2-day retention for forecasts)");
        weatherForecastService.cleanupOldForecasts();
        ipLocationService.cleanupOldLocationData(); // Still 14 days for location data
    }

    /**
     * Clean up old historical weather data every week at 3:00 AM (2-year retention)
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void cleanupOldHistoricalWeatherData() {
        log.info("Starting weekly cleanup of old historical weather data (2-year retention)");
        historicalWeatherService.cleanupOldHistoricalWeather();
    }

    /**
     * Clean up expired weather alerts every day at 4:00 AM
     * Alerts have specific expiry times and can accumulate quickly
     */
    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredAlerts() {
        log.info("Starting daily cleanup of expired weather alerts");
        brightSkyService.cleanupExpiredAlerts();
    }

    /**
     * Additional alert update during peak hours (6 AM, 12 PM, 6 PM) for all device
     * locations
     * These are times when weather conditions often change
     */
    @Scheduled(cron = "0 0 6,12,18 * * *")
    public void updateAlertsAtPeakHours() {
        log.info("Starting peak hour weather alerts update for device locations (6 AM, 12 PM, 6 PM)");
        brightSkyService.updateAlertsForAllDeviceLocations();
    }
}