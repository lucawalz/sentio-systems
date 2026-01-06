package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.RaspiWeatherData;
import org.example.backend.model.WeatherAlert;
import org.example.backend.model.WeatherForecast;
import org.example.backend.service.AnimalDetectionQueryService;
import org.example.backend.service.BrightSkyService;
import org.example.backend.service.RaspiWeatherDataService;
import org.example.backend.service.WeatherForecastService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for device-scoped data access.
 * Provides resource-based routing: /api/devices/{deviceId}/...
 * All endpoints verify device ownership before returning data.
 */
@RestController
@RequestMapping("/api/devices/{deviceId}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Data", description = "Device-scoped weather, forecast, and alert data")
public class DeviceDataController {

    private final WeatherForecastService forecastService;
    private final BrightSkyService alertService;
    private final RaspiWeatherDataService sensorService;
    private final AnimalDetectionQueryService sightingsService;

    @Operation(summary = "Get forecasts for device", description = "Retrieves weather forecasts for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Forecasts retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WeatherForecast.class)))),
            @ApiResponse(responseCode = "404", description = "Device not found or not owned by user")
    })
    @GetMapping("/forecasts")
    public ResponseEntity<?> getDeviceForecasts(
            @Parameter(description = "Device UUID") @PathVariable String deviceId) {
        try {
            List<WeatherForecast> forecasts = forecastService.getForecastForDevice(deviceId);
            log.info("Retrieved {} forecasts for device {}", forecasts.size(), deviceId);
            return ResponseEntity.ok(forecasts);
        } catch (IllegalArgumentException e) {
            log.warn("Device not found or not owned: {}", deviceId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get alerts for device", description = "Retrieves active weather alerts for a specific device location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = WeatherAlert.class)))),
            @ApiResponse(responseCode = "404", description = "Device not found or not owned by user")
    })
    @GetMapping("/alerts")
    public ResponseEntity<?> getDeviceAlerts(
            @Parameter(description = "Device UUID") @PathVariable String deviceId) {
        try {
            List<WeatherAlert> alerts = alertService.getAlertsForDevice(deviceId);
            log.info("Retrieved {} alerts for device {}", alerts.size(), deviceId);
            return ResponseEntity.ok(alerts);
        } catch (IllegalArgumentException e) {
            log.warn("Device not found or not owned: {}", deviceId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get latest sensor data for device", description = "Retrieves the most recent sensor reading for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sensor data retrieved successfully", content = @Content(schema = @Schema(implementation = RaspiWeatherData.class))),
            @ApiResponse(responseCode = "404", description = "Device not found or no data available")
    })
    @GetMapping("/sensors/latest")
    public ResponseEntity<?> getDeviceSensorLatest(
            @Parameter(description = "Device UUID") @PathVariable String deviceId) {
        try {
            RaspiWeatherData latest = sensorService.getLatestForDevice(deviceId);
            if (latest == null) {
                return ResponseEntity.ok(Map.of("message", "No sensor data available for device"));
            }
            log.debug("Retrieved latest sensor data for device {}", deviceId);
            return ResponseEntity.ok(latest);
        } catch (IllegalArgumentException e) {
            log.warn("Device not found or not owned: {}", deviceId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get recent sensor data for device", description = "Retrieves sensor readings from the last 24 hours for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sensor data retrieved successfully", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RaspiWeatherData.class)))),
            @ApiResponse(responseCode = "404", description = "Device not found or not owned by user")
    })
    @GetMapping("/sensors")
    public ResponseEntity<?> getDeviceSensors(
            @Parameter(description = "Device UUID") @PathVariable String deviceId) {
        try {
            List<RaspiWeatherData> sensors = sensorService.getRecentForDevice(deviceId);
            log.info("Retrieved {} sensor readings for device {}", sensors.size(), deviceId);
            return ResponseEntity.ok(sensors);
        } catch (IllegalArgumentException e) {
            log.warn("Device not found or not owned: {}", deviceId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get radar endpoint for device", description = "Retrieves radar data endpoint URL for a specific device location")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Radar endpoint retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found or has no GPS coordinates")
    })
    @GetMapping("/radar")
    public ResponseEntity<?> getDeviceRadar(
            @Parameter(description = "Device UUID") @PathVariable String deviceId) {
        try {
            String radarUrl = alertService.getRadarEndpointForDevice(deviceId);
            log.info("Generated radar endpoint for device {}", deviceId);
            return ResponseEntity.ok(Map.of(
                    "radarEndpoint", radarUrl,
                    "deviceId", deviceId));
        } catch (IllegalArgumentException e) {
            log.warn("Device not found or no GPS: {}", deviceId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get sightings for device", description = "Retrieves animal sightings for a specific device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sightings retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found or not owned by user")
    })
    @GetMapping("/sightings")
    public ResponseEntity<?> getDeviceSightings(
            @Parameter(description = "Device UUID") @PathVariable String deviceId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            var sightings = sightingsService.getDetectionsByDevice(deviceId, PageRequest.of(0, limit));
            log.info("Retrieved {} sightings for device {}", sightings.size(), deviceId);
            return ResponseEntity.ok(sightings);
        } catch (IllegalArgumentException e) {
            log.warn("Device not found or not owned: {}", deviceId);
            return ResponseEntity.notFound().build();
        }
    }
}
