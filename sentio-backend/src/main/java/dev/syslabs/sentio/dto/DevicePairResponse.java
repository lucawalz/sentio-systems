package dev.syslabs.sentio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for successful device pairing.
 * Contains the permanent device token for MQTT authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePairResponse {

    /**
     * The device ID.
     */
    private String deviceId;

    /**
     * Permanent device token for MQTT authentication.
     * Store securely - this is only returned once.
     */
    private String deviceToken;

    /**
     * MQTT broker URL.
     */
    private String mqttUrl;

    /**
     * Success message.
     */
    private String message;
}
