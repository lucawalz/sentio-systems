package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "Device ID is required")
    private String deviceId;

    /**
     * The pairing code shown in the dashboard (format: XXXX-XXXX).
     */
    @NotBlank(message = "Pairing code is required")
    @Pattern(regexp = "[A-Z0-9]{4}-[A-Z0-9]{4}", message = "Pairing code must match format XXXX-XXXX (uppercase letters and digits)")
    private String pairingCode;
}
