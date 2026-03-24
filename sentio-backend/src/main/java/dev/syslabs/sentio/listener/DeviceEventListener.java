package dev.syslabs.sentio.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.event.DeviceLocationUpdatedEvent;
import dev.syslabs.sentio.event.DeviceRegisteredEvent;
import dev.syslabs.sentio.event.DeviceUnregisteredEvent;
import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.model.LocationData;
import dev.syslabs.sentio.model.WeatherAlert;
import dev.syslabs.sentio.service.BrightSkyService;
import dev.syslabs.sentio.service.HistoricalWeatherService;
import dev.syslabs.sentio.service.WeatherForecastService;
import dev.syslabs.sentio.service.WebSocketService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Listens for device-related events and triggers appropriate actions.
 * Handles device registration, unregistration, and location updates.
 * Uses EDA pattern to trigger weather data fetching reactively.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceEventListener {

    private final WeatherForecastService weatherForecastService;
    private final HistoricalWeatherService historicalWeatherService;
    private final BrightSkyService brightSkyService;
    private final WebSocketService webSocketService;

    /**
     * When a device is registered, notify frontend and fetch weather if coordinates
     * exist.
     */
    @Async
    @EventListener
    public void onDeviceRegistered(DeviceRegisteredEvent event) {
        Device device = event.getDevice();
        String username = event.getUsername();

        log.info("Handling DeviceRegisteredEvent for device {} (user: {})", device.getId(), username);
        webSocketService.broadcastDeviceRegistered(device.getId(), username);

        // If device has coordinates, fetch weather data
        if (device.getLatitude() != null && device.getLongitude() != null) {
            fetchWeatherForDevice(device.getId(), device.getLatitude(), device.getLongitude());
        } else {
            log.info("Device {} has no GPS coordinates - will fetch weather when location is set", device.getId());
        }
    }

    /**
     * When a device's GPS location is updated, fetch weather immediately.
     * This is the core EDA handler - no polling needed.
     */
    @Async
    @EventListener
    public void onDeviceLocationUpdated(DeviceLocationUpdatedEvent event) {
        log.info("Handling DeviceLocationUpdatedEvent for device {} at ({}, {}), firstLocation={}",
                event.getDeviceId(), event.getLatitude(), event.getLongitude(), event.isFirstLocation());

        fetchWeatherForDevice(event.getDeviceId(), event.getLatitude(), event.getLongitude());
    }

    /**
     * When a device is unregistered, notify frontend.
     */
    @EventListener
    public void onDeviceUnregistered(DeviceUnregisteredEvent event) {
        log.info("Device {} unregistered by user {}", event.getDeviceId(), event.getUsername());
        webSocketService.broadcastDeviceUnregistered(event.getDeviceId(), event.getUsername());
    }

    /**
     * Fetches weather forecast, alerts, and historical data for a device location.
     * Private helper used by both registration and location update handlers.
     */
    private void fetchWeatherForDevice(String deviceId, Double latitude, Double longitude) {
        try {
            log.info("Fetching weather data for device {} at ({}, {})", deviceId, latitude, longitude);

            // Build location data
            LocationData location = new LocationData();
            location.setLatitude(latitude.floatValue());
            location.setLongitude(longitude.floatValue());
            location.setDeviceId(deviceId);
            location.setCity("GPS Location");
            location.setCountry("Unknown");

            // Fetch forecast
            weatherForecastService.getForecastForLocation(
                    location.getLatitude(), location.getLongitude(), location);
            webSocketService.broadcastWeatherUpdated("FORECAST");

            // Fetch alerts
            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    location.getLatitude(), location.getLongitude(), deviceId);
            webSocketService.broadcastAlertsUpdated(alerts.size(), !alerts.isEmpty());

            // Fetch historical weather for comparison charts
            historicalWeatherService.getHistoricalWeatherForLocation(
                    location.getLatitude(), location.getLongitude(), location);
            webSocketService.broadcastWeatherUpdated("HISTORICAL");

            log.info("Successfully fetched all weather data for device {} and notified frontend", deviceId);

        } catch (Exception e) {
            log.error("Failed to fetch weather data for device {}: {}", deviceId, e.getMessage(), e);
        }
    }
}
