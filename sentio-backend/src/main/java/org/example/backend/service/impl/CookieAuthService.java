package org.example.backend.service.impl;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service for creating and managing secure authentication cookies.
 * All cookies are httpOnly to prevent XSS attacks.
 */
@Service
public class CookieAuthService {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    @org.springframework.beans.factory.annotation.Value("${sentio.auth.secure-cookies:false}")
    private boolean secureCookies;

    /**
     * Creates an httpOnly cookie for the access token.
     * 
     * @param token         The JWT access token
     * @param maxAgeSeconds Token lifetime in seconds
     * @return ResponseCookie configured for secure access token storage
     */
    public ResponseCookie createAccessTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
    }

    /**
     * Creates an httpOnly cookie for the refresh token.
     * Path is restricted to /api/auth to minimize exposure.
     * 
     * @param token The refresh token
     * @return ResponseCookie configured for secure refresh token storage
     */
    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookies)
                .path("/api/auth") // Only sent to auth endpoints
                .maxAge(Duration.ofDays(7).getSeconds())
                .sameSite("Lax")
                .build();
    }

    /**
     * Creates a cookie that clears the specified auth cookie.
     * 
     * @param cookieName Name of the cookie to clear
     * @return ResponseCookie with maxAge=0 to delete the cookie
     */
    public ResponseCookie createLogoutCookie(String cookieName) {
        String path = REFRESH_TOKEN_COOKIE.equals(cookieName) ? "/api/auth" : "/";
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secureCookies)
                .path(path)
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    /**
     * Creates both logout cookies for access and refresh tokens.
     * 
     * @return Array of ResponseCookies to clear both tokens
     */
    public ResponseCookie[] createLogoutCookies() {
        return new ResponseCookie[] {
                createLogoutCookie(ACCESS_TOKEN_COOKIE),
                createLogoutCookie(REFRESH_TOKEN_COOKIE)
        };
    }
}
