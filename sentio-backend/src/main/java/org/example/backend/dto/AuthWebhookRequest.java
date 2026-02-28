package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthWebhookRequest {
    @NotBlank(message = "Path is required")
    private String path;

    @NotBlank(message = "Action is required")
    private String action;

    private String query;

    private String protocol;
    private String ip;
}
