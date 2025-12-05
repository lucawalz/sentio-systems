package org.example.backend.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.AuthDTOs;
import org.example.backend.service.AuthService;
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

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody AuthDTOs.RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTOs.UserInfo> login(
            @RequestBody AuthDTOs.LoginRequest request,
            HttpServletResponse response) {

        // Get tokens from Keycloak
        AuthDTOs.TokenResponse tokens = authService.login(request);

        if (tokens == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Set httpOnly cookies
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieAuthService.createAccessTokenCookie(
                        tokens.getAccessToken(),
                        tokens.getExpiresIn()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieAuthService.createRefreshTokenCookie(
                        tokens.getRefreshToken()).toString());

        // Return user info (not tokens) - extract from the token we just got
        AuthDTOs.UserInfo userInfo = authService.getUserFromToken(tokens.getAccessToken());
        return ResponseEntity.ok(userInfo);
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

        // Revoke the token in Keycloak if we have a refresh token
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                authService.logout(refreshToken);
            } catch (Exception e) {
                // Log but continue - we still want to clear cookies
            }
        }

        // Clear cookies
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
}
