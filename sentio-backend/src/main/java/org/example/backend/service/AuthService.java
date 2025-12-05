package org.example.backend.service;

import org.example.backend.dto.AuthDTOs;

public interface AuthService {
    void register(AuthDTOs.RegisterRequest request);

    AuthDTOs.TokenResponse login(AuthDTOs.LoginRequest request);

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
