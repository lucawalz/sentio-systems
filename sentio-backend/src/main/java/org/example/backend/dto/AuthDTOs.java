package org.example.backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.validation.PasswordMatch;

public class AuthDTOs {

    private AuthDTOs() {
        // Utility class
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotNull(message = "Username is required")
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 255, message = "Username must be between 3 and 255 characters")
        private String username;

        @NotNull(message = "Password is required")
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotNull(message = "Username is required")
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 255, message = "Username must be between 3 and 255 characters")
        private String username;

        @NotNull(message = "Password is required")
        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                message = "Password must contain uppercase, lowercase, number and special character")
        private String password;

        @NotNull(message = "Email is required")
        @Email(message = "Email must be valid")
        private String email;

        @NotNull(message = "First name is required")
        @NotBlank(message = "First name cannot be blank")
        @Size(max = 255, message = "First name must be at most 255 characters")
        private String firstName;

        @NotNull(message = "Last name is required")
        @NotBlank(message = "Last name cannot be blank")
        @Size(max = 255, message = "Last name must be at most 255 characters")
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
            @NotNull(message = "Email is required")
            @Email(message = "Email must be valid")
            String email
    ) {
    }

    public record TokenValidationResponse(boolean valid, String email, String error) {
    }

    @PasswordMatch
    public record ResetPasswordRequest(
            @NotNull(message = "Token is required")
            @NotBlank(message = "Token cannot be blank")
            String token,
            
            @NotNull(message = "Password is required")
            @NotBlank(message = "Password cannot be blank")
            @Size(min = 8, max = 255, message = "Password must be at least 8 characters")
            @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
                    message = "Password must contain uppercase, lowercase, number and special character")
            String password,
            
            @NotNull(message = "Confirm password is required")
            @NotBlank(message = "Confirm password cannot be blank")
            String confirmPassword
    ) {
    }

    public record MessageResponse(boolean success, String message) {
    }

    public record ResendVerificationRequest(
            @NotNull(message = "Email is required")
            @Email(message = "Email must be valid")
            String email
    ) {
    }
}
