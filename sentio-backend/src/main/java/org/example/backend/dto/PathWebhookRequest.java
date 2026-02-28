package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PathWebhookRequest {
    @NotBlank(message = "Path is required")
    private String path;
}
