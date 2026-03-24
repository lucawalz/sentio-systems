package dev.syslabs.sentio.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.syslabs.sentio.service.DeviceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal controller for MQTT broker authentication.
 * Called by mosquitto-go-auth plugin to validate device credentials.
 */
@RestController
@RequestMapping("/api/internal/mqtt")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MQTT Auth (Internal)", description = "Internal API for MQTT broker authentication")
public class DeviceMqttAuthController {

    private final DeviceService deviceService;

    // Backend service account credentials (used by Spring backend to subscribe)
    @Value("${mqtt.username:}")
    private String backendUsername;

    @Value("${mqtt.password:}")
    private String backendPassword;

    /**
     * Validate device credentials for MQTT connection.
     * Called by mosquitto-go-auth HTTP backend.
     * 
     * @param username Device ID or backend service account
     * @param password MQTT code or backend password
     * @return 200 if valid, 403 if invalid
     */
    @PostMapping("/auth")
    @Operation(summary = "Validate MQTT credentials", description = "Internal endpoint for MQTT broker auth")
    public ResponseEntity<Void> authenticate(
            @RequestParam String username,
            @RequestParam String password) {
        log.debug("MQTT auth request for: {}", username);

        // Check if it's the backend service account (for subscribing to topics)
        if (!backendUsername.isEmpty() && username.equals(backendUsername)) {
            if (password.equals(backendPassword)) {
                log.debug("MQTT auth successful for backend service account");
                return ResponseEntity.ok().build();
            }
            log.warn("MQTT auth failed for backend service account - wrong password");
            return ResponseEntity.status(403).build();
        }

        // Otherwise validate as a device
        boolean valid = deviceService.validateMqttToken(username, password);

        if (valid) {
            log.debug("MQTT auth successful for device: {}", username);
            return ResponseEntity.ok().build();
        } else {
            log.warn("MQTT auth failed for device: {}", username);
            return ResponseEntity.status(403).build();
        }
    }

    /**
     * Check if user is a superuser (admin access).
     * Backend service account is a superuser.
     */
    @PostMapping("/superuser")
    @Operation(summary = "Check superuser status", description = "Backend is superuser, devices are not")
    public ResponseEntity<Void> checkSuperuser(@RequestParam String username) {
        // Backend service account is superuser
        if (!backendUsername.isEmpty() && username.equals(backendUsername)) {
            log.debug("MQTT superuser check for backend: approved");
            return ResponseEntity.ok().build();
        }
        log.debug("MQTT superuser check for: {} - denied", username);
        return ResponseEntity.status(403).build();
    }

    /**
     * Check ACL permissions for publish/subscribe.
     * Devices can only access topics for their active services.
     * 
     * Topic → Service mapping:
     * - weather/* → weather_station
     * - animal_detection/* → animal_detector
     * 
     * @param username Device ID
     * @param topic    MQTT topic
     * @param acc      Access type (1=subscribe, 2=publish)
     * @return 200 if allowed, 403 if denied
     */
    @PostMapping("/acl")
    @Operation(summary = "Check ACL permissions", description = "Validate topic access for device")
    public ResponseEntity<Void> checkAcl(
            @RequestParam String username,
            @RequestParam String topic,
            @RequestParam int acc) {
        log.debug("MQTT ACL check: device={}, topic={}, acc={}", username, topic, acc);

        // Backend superuser can access all topics
        if (!backendUsername.isEmpty() && username.equals(backendUsername)) {
            log.debug("MQTT ACL: backend superuser - allowing all topics");
            return ResponseEntity.ok().build();
        }

        // Security check for device-specific topics: device/{deviceId}/...
        // Devices can only access their own topics
        if (topic.startsWith("device/")) {
            String[] parts = topic.split("/");
            if (parts.length >= 2) {
                String topicDeviceId = parts[1];
                if (!topicDeviceId.equals(username)) {
                    log.warn("MQTT ACL denied: device={} tried to access topic for device={}",
                            username, topicDeviceId);
                    return ResponseEntity.status(403).build();
                }
            }
        }

        // Determine required service for this topic
        String requiredService = getRequiredServiceForTopic(topic);

        if (requiredService == null) {
            // Unknown topic - deny by default
            log.warn("MQTT ACL denied: device={}, topic={} - unknown topic pattern", username, topic);
            return ResponseEntity.status(403).build();
        }

        // Empty string means always allowed (status topics for bootstrapping)
        if (requiredService.isEmpty()) {
            log.debug("MQTT ACL allowed: device={}, topic={} - status topic (bootstrap)", username, topic);
            return ResponseEntity.ok().build();
        }

        // Check if device has the required service active
        var deviceServices = deviceService.getDeviceServices(username);

        if (deviceServices.contains(requiredService)) {
            log.debug("MQTT ACL allowed: device={}, topic={}, service={}", username, topic, requiredService);
            return ResponseEntity.ok().build();
        }

        log.warn("MQTT ACL denied: device={}, topic={} - requires service '{}' but has {}",
                username, topic, requiredService, deviceServices);
        return ResponseEntity.status(403).build();
    }

    /**
     * Map topic to required service.
     * Status topics are allowed unconditionally for bootstrapping (that's how
     * services register).
     * Data/event topics require the service to be registered first.
     * 
     * @param topic MQTT topic
     * @return Required service name, null if unknown, or empty string "" if always
     *         allowed
     */
    private String getRequiredServiceForTopic(String topic) {
        if (topic == null) {
            return null;
        }

        // Status topics are always allowed - this is how devices register their
        // services
        // The DeviceStatusHandler will add the service to activeServices when it
        // receives these
        // Unified device status topic: device/{deviceId}/status
        if (topic.startsWith("device/") && topic.endsWith("/status")) {
            return ""; // Always allowed (any service can publish)
        }

        // Stream command topics: device/{deviceId}/command
        // Allow devices to subscribe to receive commands from backend
        if (topic.startsWith("device/") && topic.endsWith("/command")) {
            return ""; // Always allowed for command subscriptions
        }

        // Data topics require the corresponding service
        if (topic.equals("weather/data")) {
            return "weather_station";
        }
        if (topic.equals("animals/data")) {
            return "animal_detector";
        }

        return null;
    }
}
