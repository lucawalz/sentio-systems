package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for device pairing.
 * Sent by the device during setup to exchange pairing code for permanent token.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePairRequest {

    /**
     * The device ID from the dashboard.
     */
    private String deviceId;

    /**
     * The pairing code shown in the dashboard (format: XXXX-XXXX).
     */
    private String pairingCode;
}
