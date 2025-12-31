package org.example.backend.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.event.DeviceRegisteredEvent;
import org.example.backend.event.DeviceUnregisteredEvent;
import org.example.backend.model.Device;
import org.example.backend.model.LocationData;
import org.example.backend.service.BrightSkyService;
import org.example.backend.service.DeviceLocationService;
import org.example.backend.service.WeatherForecastService;
import org.example.backend.service.WebSocketService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for device-related events and triggers appropriate actions.
 * When a device is registered, we immediately fetch weather data for that
 * device's location and notify the frontend via WebSocket.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceEventListener {

    private final WeatherForecastService weatherForecastService;
    private final BrightSkyService brightSkyService;
    private final DeviceLocationService deviceLocationService;
    private final WebSocketService webSocketService;

    /**
     * When a device is registered, immediately fetch weather data for its location
     * and notify the frontend via WebSocket.
     * This runs async to not block the registration response.
     */
    @Async
    @EventListener
    public void onDeviceRegistered(DeviceRegisteredEvent event) {
        Device device = event.getDevice();
        String username = event.getUsername();

        log.info("Handling DeviceRegisteredEvent for device {} (user: {})", device.getId(), username);

        // Notify frontend immediately about new device (even before weather data)
        webSocketService.broadcastDeviceRegistered(device.getId(), username);

        // Try to get the device's IP address to determine location
        String ipAddress = device.getIpAddress();
        if (ipAddress == null || ipAddress.isEmpty()) {
            log.info("Device {} has no IP address yet - weather data will be fetched when device reports location",
                    device.getId());
            return;
        }

        try {
            // Get location for this device
            java.util.Optional<LocationData> locationOpt = deviceLocationService.getDeviceLocation(device.getId());
            if (locationOpt.isEmpty()) {
                log.warn("Could not determine location for device {}", device.getId());
                return;
            }
            LocationData location = locationOpt.get();
            location.setDeviceId(device.getId());

            log.info("Fetching initial weather data for device {} at location ({}, {})",
                    device.getId(), location.getLatitude(), location.getLongitude());

            // Fetch forecast data (uses existing method that fetches and stores)
            weatherForecastService.getForecastForLocation(
                    location.getLatitude().floatValue(),
                    location.getLongitude().floatValue(),
                    location);

            // Notify frontend about weather update
            webSocketService.broadcastWeatherUpdated("FORECAST");

            // Fetch alerts (uses existing method that fetches and stores)
            java.util.List<org.example.backend.model.WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    location.getLatitude().floatValue(),
                    location.getLongitude().floatValue(),
                    device.getId());

            // Notify frontend about alerts update
            webSocketService.broadcastAlertsUpdated(alerts.size(), !alerts.isEmpty());

            log.info("Successfully fetched initial weather data for device {} and notified frontend", device.getId());

        } catch (Exception e) {
            log.error("Failed to fetch weather data for newly registered device {}: {}",
                    device.getId(), e.getMessage(), e);
        }
    }

    /**
     * When a device is unregistered, notify frontend via WebSocket.
     */
    @EventListener
    public void onDeviceUnregistered(DeviceUnregisteredEvent event) {
        log.info("Device {} unregistered by user {}", event.getDeviceId(), event.getUsername());

        // Notify frontend about device removal
        webSocketService.broadcastDeviceUnregistered(event.getDeviceId(), event.getUsername());
    }
}
