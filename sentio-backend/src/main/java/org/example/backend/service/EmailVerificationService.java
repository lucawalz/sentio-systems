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
 * Service for managing email verification tokens.
 * Tokens are stored in Redis with a configurable expiration time.
 */
@Service
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final String TOKEN_PREFIX = "email_verify:";
    private static final int TOKEN_LENGTH = 32;

    private final StringRedisTemplate redisTemplate;
    private final ResendEmailService emailService;
    private final Duration tokenExpiration;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailVerificationService(
            StringRedisTemplate redisTemplate,
            ResendEmailService emailService,
            @Value("${email-verification.token-expiration-hours:24}") int expirationHours) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.tokenExpiration = Duration.ofHours(expirationHours);
    }

    /**
     * Generate an email verification token and send the verification email.
     * 
     * @param email The user's email address
     * @return The generated token
     */
    public String createVerificationToken(String email) {
        // Generate cryptographically secure random token
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Store token -> email mapping in Redis with expiration
        String key = TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, email, tokenExpiration);

        log.info("Created email verification token for: {}", email);

        // Send the verification email
        sendVerificationEmail(email, token);

        return token;
    }

    /**
     * Validate an email verification token.
     * 
     * @param token The token to validate
     * @return The email address associated with the token, or null if
     *         invalid/expired
     */
    public String validateToken(String token) {
        String key = TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);

        if (email == null) {
            log.warn("Invalid or expired email verification token");
            return null;
        }

        log.info("Validated email verification token for: {}", email);
        return email;
    }

    /**
     * Invalidate an email verification token (after successful verification).
     * 
     * @param token The token to invalidate
     */
    public void invalidateToken(String token) {
        String key = TOKEN_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Invalidated email verification token");
    }

    /**
     * Send the verification email with a custom template.
     */
    private void sendVerificationEmail(String email, String token) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;

        String subject = "Verify your email address";
        String htmlContent = org.example.backend.util.EmailTemplateBuilder.buildVerificationEmail(verifyUrl);

        try {
            emailService.sendHtmlEmail(email, subject, htmlContent, null);
            log.info("Verification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
        }
    }
}
