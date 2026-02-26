package org.example.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTOs {

    private AuthDTOs() {
        // Utility class
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        private String email;

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must be at most 50 characters")
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must be at most 50 characters")
        private String lastName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id; // Keycloak user UUID (from JWT 'sub' claim)
        private String username;
        private String email;
        private java.util.List<String> roles;
    }

    // Password Reset & Email Verification DTOs

    public record RegisterResponse(boolean success, String message) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required") @Email(message = "Email must be a valid email address") String email) {
    }

    public record TokenValidationResponse(boolean valid, String email, String error) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Token is required") String token,

            @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters") String password,

            @NotBlank(message = "Password confirmation is required") String confirmPassword) {
    }

    public record MessageResponse(boolean success, String message) {
    }

    public record ResendVerificationRequest(
            @NotBlank(message = "Email is required") @Email(message = "Email must be a valid email address") String email) {
    }
}
