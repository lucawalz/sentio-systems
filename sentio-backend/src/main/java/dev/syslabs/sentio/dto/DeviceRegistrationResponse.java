package dev.syslabs.sentio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for device registration.
 * Contains the generated device ID and pairing code (expires in 15 minutes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationResponse {

    /**
     * Generated UUID for the device.
     */
    private String deviceId;

    /**
     * Device name provided by user.
     */
    private String name;

    /**
     * Temporary pairing code (format: XXXX-XXXX).
     * Expires after 15 minutes. Enter on device to complete pairing.
     */
    private String pairingCode;

    /**
     * When the pairing code expires.
     */
    private LocalDateTime pairingCodeExpiresAt;

    /**
     * MQTT broker URL for production.
     */
    private String mqttUrl;
}
