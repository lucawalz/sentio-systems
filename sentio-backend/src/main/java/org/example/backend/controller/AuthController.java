package org.example.backend.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.AuthDTOs;
import org.example.backend.service.AuthService;
import org.example.backend.service.EmailVerificationService;
import org.example.backend.service.PasswordResetService;
import org.example.backend.service.impl.CookieAuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieAuthService cookieAuthService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody AuthDTOs.RegisterRequest request) {
        authService.register(request);
        // Send custom verification email
        emailVerificationService.createVerificationToken(request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(true, "Account created! Please check your email to verify your account."));
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody AuthDTOs.LoginRequest request, HttpServletResponse response) {
        AuthDTOs.TokenResponse tokens = authService.login(request.getUsername(), request.getPassword());

        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieAuthService.createAccessTokenCookie(
                        tokens.getAccessToken(),
                        tokens.getExpiresIn()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieAuthService.createRefreshTokenCookie(
                        tokens.getRefreshToken()).toString());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AuthDTOs.TokenResponse tokens = authService.refreshToken(refreshToken);

            response.addHeader(HttpHeaders.SET_COOKIE,
                    cookieAuthService.createAccessTokenCookie(
                            tokens.getAccessToken(),
                            tokens.getExpiresIn()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE,
                    cookieAuthService.createRefreshTokenCookie(
                            tokens.getRefreshToken()).toString());

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            for (ResponseCookie cookie : cookieAuthService.createLogoutCookies()) {
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDTOs.UserInfo> getCurrentUser(
            @CookieValue(name = "access_token", required = false) String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AuthDTOs.UserInfo userInfo = authService.getUserFromToken(accessToken);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                authService.logout(refreshToken);
            } catch (Exception e) {
                // Continue to clear cookies
            }
        }

        for (ResponseCookie cookie : cookieAuthService.createLogoutCookies()) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        authService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== PASSWORD RESET ====================

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (authService.userExistsByEmail(request.email())) {
            passwordResetService.createResetToken(request.email());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<TokenValidationResponse> validateResetToken(@RequestParam String token) {
        String email = passwordResetService.validateToken(token);
        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(new TokenValidationResponse(false, null, "Invalid or expired token"));
        }
        return ResponseEntity.ok(new TokenValidationResponse(true, maskEmail(email), null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        String email = passwordResetService.validateToken(request.token());
        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Invalid or expired token. Please request a new reset link."));
        }

        if (request.password() == null || request.password().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Password must be at least 8 characters"));
        }

        if (!request.password().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Passwords do not match"));
        }

        try {
            authService.updatePassword(email, request.password());
            passwordResetService.invalidateToken(request.token());
            return ResponseEntity.ok(new MessageResponse(true, "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse(false, "Failed to update password. Please try again."));
        }
    }

    // ==================== EMAIL VERIFICATION ====================

    @GetMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@RequestParam String token) {
        String email = emailVerificationService.validateToken(token);
        if (email == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(false, "Invalid or expired verification link."));
        }

        try {
            authService.markEmailVerified(email);
            emailVerificationService.invalidateToken(token);
            return ResponseEntity.ok(new MessageResponse(true, "Email verified successfully! You can now sign in."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse(false, "Failed to verify email. Please try again."));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody ResendVerificationRequest request) {
        if (authService.userExistsByEmail(request.email())) {
            emailVerificationService.createVerificationToken(request.email());
        }
        return ResponseEntity.ok().build();
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

    // DTOs
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
