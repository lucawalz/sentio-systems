package org.example.backend.service;

import org.example.backend.dto.AuthDTOs;

public interface AuthService {
    void register(AuthDTOs.RegisterRequest request);

    // Auth Code Flow methods
    String getLoginUrl();

    String getRegisterUrl();

    AuthDTOs.TokenResponse exchangeCodeForTokens(String code);

    AuthDTOs.TokenResponse refreshToken(String refreshToken);

    // Deprecated: Password login (removing if possible, but keeping for reference
    // if needed? No, user wants industry standard)
    // AuthDTOs.TokenResponse login(AuthDTOs.LoginRequest request);

    void logout(String refreshToken);

    void deleteUser(String userId);

    /**
     * Extracts user information from a JWT access token.
     * 
     * @param accessToken The JWT access token
     * @return UserInfo containing username and email
     */
    AuthDTOs.UserInfo getUserFromToken(String accessToken);
}
