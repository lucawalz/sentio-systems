package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.Device;
import org.example.backend.service.DeviceLocationService;
import org.example.backend.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Device Management", description = "API for managing IoT devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceLocationService deviceLocationService;

    @Operation(summary = "Register a device", description = "Register an existing embedded device to the current user")
    @PostMapping("/register")
    public ResponseEntity<Device> registerDevice(@RequestParam String deviceId,
            @RequestParam(defaultValue = "My Device") String name,
            @RequestParam(defaultValue = "false") boolean isPrimary) {
        log.info("Request to register device: {} (isPrimary: {})", deviceId, isPrimary);
        Device device = deviceService.registerDevice(deviceId, name);

        // If isPrimary flag is set, also set this as the primary device
        if (isPrimary) {
            device = deviceService.setPrimaryDevice(deviceId);
        }

        return ResponseEntity.ok(device);
    }

    @Operation(summary = "List my devices", description = "List all devices owned by the current user")
    @GetMapping
    public ResponseEntity<List<Device>> getMyDevices() {
        return ResponseEntity.ok(deviceService.getMyDevices());
    }

    @Operation(summary = "Unregister a device", description = "Remove the current user from the device owners")
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> unregisterDevice(@PathVariable String deviceId) {
        deviceService.unregisterDevice(deviceId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check device registration", description = "Check if user has any registered devices. Use this before calling weather APIs.")
    @GetMapping("/has-any")
    public ResponseEntity<Boolean> hasAnyDevices() {
        return ResponseEntity.ok(deviceService.hasAnyDevices());
    }

    @Operation(summary = "Get primary device", description = "Get the user's primary device for forecasts and radar")
    @GetMapping("/primary")
    public ResponseEntity<Device> getPrimaryDevice() {
        return deviceService.getPrimaryDevice()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Set primary device", description = "Set a device as the user's primary device for forecasts and radar")
    @PutMapping("/{deviceId}/primary")
    public ResponseEntity<Device> setPrimaryDevice(@PathVariable String deviceId) {
        try {
            Device device = deviceService.setPrimaryDevice(deviceId);
            return ResponseEntity.ok(device);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Update device GPS", description = "Update device GPS coordinates and trigger weather data fetch")
    @PutMapping("/{deviceId}/location")
    public ResponseEntity<?> updateDeviceLocation(
            @PathVariable String deviceId,
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        log.info("Updating GPS location for device {}: ({}, {})", deviceId, latitude, longitude);
        try {
            boolean updated = deviceLocationService.updateDeviceGpsLocation(deviceId, latitude, longitude);
            if (updated) {
                return ResponseEntity.ok(java.util.Map.of(
                        "message", "GPS location updated, weather data fetch triggered",
                        "deviceId", deviceId,
                        "latitude", latitude,
                        "longitude", longitude));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to update device location: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
