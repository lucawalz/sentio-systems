package dev.syslabs.sentio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.dto.DevicePairRequest;
import dev.syslabs.sentio.dto.DevicePairResponse;
import dev.syslabs.sentio.dto.DeviceRegistrationRequest;
import dev.syslabs.sentio.dto.DeviceRegistrationResponse;
import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.service.DeviceLocationService;
import dev.syslabs.sentio.service.DeviceService;
import dev.syslabs.sentio.service.RateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Device Management", description = "API for managing IoT devices")
public class DeviceController {

    private final DeviceService deviceService;
    private final DeviceLocationService deviceLocationService;
    private final RateLimitService rateLimitService;

    @Value("${mqtt.external-url:mqtt://localhost:1883}")
    private String mqttExternalUrl;

    @Operation(summary = "Register a new device", description = "Create a new device with 15-min expiring pairing code")
    @PostMapping("/register")
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request) {
        String name = (request.getName() != null && !request.getName().isBlank()) ? request.getName() : "My Device";
        log.info("Request to register new device with name: {}", name);

        DeviceRegistrationResponse response = deviceService.registerDeviceWithCredentials(name);

        // If isPrimary flag is set, also set this as the primary device
        if (Boolean.parseBoolean(request.getIsPrimary())) {
            deviceService.setPrimaryDevice(response.getDeviceId());
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Regenerate pairing code", description = "Generate a new 15-min pairing code for existing device")
    @PostMapping("/{deviceId}/regenerate-code")
    public ResponseEntity<DeviceRegistrationResponse> regeneratePairingCode(@PathVariable String deviceId) {
        log.info("Request to regenerate pairing code for device: {}", deviceId);
        try {
            DeviceRegistrationResponse response = deviceService.regeneratePairingCode(deviceId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Exchange pairing code for token", description = "Public endpoint - device calls this to exchange pairing code for permanent token")
    @PostMapping("/pair")
    public ResponseEntity<?> pairDevice(@Valid @RequestBody DevicePairRequest request, HttpServletRequest httpRequest) {
        // Rate limiting: 10 requests per minute per IP
        String clientIp = getClientIp(httpRequest);
        if (!rateLimitService.allowPairingRequest(clientIp)) {
            log.warn("Rate limit exceeded for pairing from IP: {}", clientIp);
            return ResponseEntity.status(429).body(Map.of("error", "Too many pairing attempts. Please wait 1 minute."));
        }

        log.info("Device pairing request for device: {}", request.getDeviceId());
        try {
            String deviceToken = deviceService.exchangePairingCode(request.getDeviceId(), request.getPairingCode());

            DevicePairResponse response = DevicePairResponse.builder()
                    .deviceId(request.getDeviceId())
                    .deviceToken(deviceToken)
                    .mqttUrl(mqttExternalUrl)
                    .message("Device paired successfully")
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Pairing failed for device {}: {}", request.getDeviceId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Extract client IP from request, handling proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take first IP in chain
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
