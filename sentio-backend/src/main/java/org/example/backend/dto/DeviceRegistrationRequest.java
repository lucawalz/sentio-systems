package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeviceRegistrationRequest {

    @Size(max = 255, message = "Device name must be at most 255 characters")
    private String name;

    @Pattern(regexp = "true|false", message = "isPrimary must be true or false")
    private String isPrimary = "false";
}
