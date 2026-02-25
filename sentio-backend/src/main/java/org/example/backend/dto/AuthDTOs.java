package org.example.backend.dto;

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
        private String username;
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String firstName;
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

    public record ForgotPasswordRequest(String email) {
    }

    public record TokenValidationResponse(boolean valid, String email, String error) {
    }

    public record ResetPasswordRequest(String token, String password, String confirmPassword) {
    }

    public record MessageResponse(boolean success, String message) {
    }

    public record ResendVerificationRequest(String email) {
    }
}
