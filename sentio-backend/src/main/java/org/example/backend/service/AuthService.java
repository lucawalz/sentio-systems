package org.example.backend.service;

import org.example.backend.dto.AuthDTOs;

public interface AuthService {
    void register(AuthDTOs.RegisterRequest request);

    // Native Auth Logic
    AuthDTOs.TokenResponse login(String username, String password);

    // AuthDTOs.TokenResponse exchangeCodeForTokens(String code);

    AuthDTOs.TokenResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

    void deleteUser(String userId);

    /**
     * Extracts user information from a JWT access token.
     * 
     * @param accessToken The JWT access token
     * @return UserInfo containing username and email
     */
    AuthDTOs.UserInfo getUserFromToken(String accessToken);

    /**
     * Gets the currently authenticated user from the security context.
     * 
     * @return UserInfo containing username and email
     */
    AuthDTOs.UserInfo getCurrentUser();

    /**
     * Checks if a user exists with the given email address.
     * 
     * @param email The email to check
     * @return true if a user with this email exists
     */
    boolean userExistsByEmail(String email);

    /**
     * Updates the password for a user identified by email.
     * 
     * @param email       The user's email address
     * @param newPassword The new password to set
     */
    void updatePassword(String email, String newPassword);

    /**
     * Marks a user's email as verified.
     * 
     * @param email The user's email address to mark as verified
     */
    void markEmailVerified(String email);
}
