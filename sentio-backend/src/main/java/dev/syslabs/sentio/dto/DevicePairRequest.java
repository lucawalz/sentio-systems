package dev.syslabs.sentio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Device ID is required")
    @NotBlank(message = "Device ID cannot be blank")
    private String deviceId;

    /**
     * The pairing code shown in the dashboard (format: XXXX-XXXX).
     */
    @NotNull(message = "Pairing code is required")
    @NotBlank(message = "Pairing code cannot be blank")
    @Pattern(regexp = "^[A-Z0-9]{4}-[A-Z0-9]{4}$", message = "Pairing code must be in format XXXX-XXXX")
    private String pairingCode;
}
