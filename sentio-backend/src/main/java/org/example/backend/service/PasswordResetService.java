package org.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Service for managing password reset tokens.
 * Tokens are stored in Redis with a configurable expiration time.
 */
@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String TOKEN_PREFIX = "password_reset:";
    private static final int TOKEN_LENGTH = 32;

    private final StringRedisTemplate redisTemplate;
    private final ResendEmailService emailService;
    private final Duration tokenExpiration;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public PasswordResetService(
            StringRedisTemplate redisTemplate,
            ResendEmailService emailService,
            @Value("${password-reset.token-expiration-hours:1}") int expirationHours) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.tokenExpiration = Duration.ofHours(expirationHours);
    }

    /**
     * Generate a password reset token for the given email and send the reset email.
     * 
     * @param email The user's email address
     * @return The generated token (for testing purposes; in production, only send
     *         via email)
     */
    public String createResetToken(String email) {
        // Generate cryptographically secure random token
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Store token -> email mapping in Redis with expiration
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, email, tokenExpiration);

        log.info("Created password reset token for email: {}", email);

        // Send the reset email
        sendResetEmail(email, token);

        return token;
    }

    /**
     * Validate a password reset token.
     * 
     * @param token The token to validate
     * @return The email address associated with the token, or null if
     *         invalid/expired
     */
    public String validateToken(String token) {
        String key = TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            log.warn("Invalid or expired password reset token");
            return null;
        }

        log.info("Validated password reset token for email: {}", email);
        return email;
    }

    /**
     * Invalidate a password reset token (after successful password change).
     * 
     * @param token The token to invalidate
     */
    public void invalidateToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Invalidated password reset token");
    }

    /**
     * Send the password reset email with a custom template.
     */
    private void sendResetEmail(String email, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;

        String subject = "Reset your password";
        String htmlContent = org.example.backend.util.EmailTemplateBuilder.buildPasswordResetEmail(resetUrl);

        try {
            emailService.sendHtmlEmail(email, subject, htmlContent, null);
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }
}
