package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EmailVerificationService}.
 * Tests token generation, validation, and verification email sending.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EmailVerificationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ResendEmailService emailService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        emailVerificationService = new EmailVerificationService(redisTemplate, emailService, 24);
        ReflectionTestUtils.setField(emailVerificationService, "frontendUrl", "http://localhost:3000");
    }

    @Nested
    @DisplayName("createVerificationToken")
    class CreateVerificationTokenTests {

        @Test
        @DisplayName("should generate token and store in Redis")
        void shouldGenerateTokenAndStore() {
            // Given
            String email = "newuser@example.com";

            // When
            String token = emailVerificationService.createVerificationToken(email);

            // Then
            assertThat(token).isNotBlank();
            assertThat(token).hasSize(43); // Base64 URL-encoded 32 bytes = 43 chars

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

            verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());

            assertThat(keyCaptor.getValue()).startsWith("email_verify:");
            assertThat(valueCaptor.getValue()).isEqualTo(email);
            assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofHours(24));
        }

        @Test
        @DisplayName("should send verification email")
        void shouldSendVerificationEmail() {
            // Given
            String email = "verify@example.com";

            // When
            emailVerificationService.createVerificationToken(email);

            // Then
            verify(emailService).sendHtmlEmail(
                    eq(email),
                    eq("Verify your email address"),
                    contains("http://localhost:3000/verify-email?token="),
                    isNull()
            );
        }

        @Test
        @DisplayName("should generate unique tokens for same email")
        void shouldGenerateUniqueTokens() {
            // Given
            String email = "user@example.com";

            // When
            String token1 = emailVerificationService.createVerificationToken(email);
            String token2 = emailVerificationService.createVerificationToken(email);

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should handle email sending failure gracefully")
        void shouldHandleEmailFailureGracefully() {
            // Given
            String email = "failing@example.com";
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService).sendHtmlEmail(anyString(), anyString(), anyString(), any());

            // When/Then - should not throw, just log
            assertThatCode(() -> emailVerificationService.createVerificationToken(email))
                    .doesNotThrowAnyException();

            // Verify token was still stored
            verify(valueOperations).set(anyString(), eq(email), any(Duration.class));
        }

        @Test
        @DisplayName("should handle null email gracefully")
        void shouldHandleNullEmail() {
            // When/Then
            assertThatCode(() -> emailVerificationService.createVerificationToken(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should use 24-hour expiration by default")
        void shouldUse24HourExpiration() {
            // Given
            String email = "user@example.com";

            // When
            emailVerificationService.createVerificationToken(email);

            // Then
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
            verify(valueOperations).set(anyString(), anyString(), durationCaptor.capture());
            assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofHours(24));
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("should return email for valid token")
        void shouldReturnEmailForValidToken() {
            // Given
            String token = "valid-verification-token";
            String email = "verified@example.com";
            when(valueOperations.get("email_verify:" + token)).thenReturn(email);

            // When
            String result = emailVerificationService.validateToken(token);

            // Then
            assertThat(result).isEqualTo(email);
            verify(valueOperations).get("email_verify:" + token);
        }

        @Test
        @DisplayName("should return null for expired token")
        void shouldReturnNullForExpiredToken() {
            // Given
            String token = "expired-token";
            when(valueOperations.get("email_verify:" + token)).thenReturn(null);

            // When
            String result = emailVerificationService.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for non-existent token")
        void shouldReturnNullForNonExistentToken() {
            // Given
            String token = "nonexistent-token";
            when(valueOperations.get("email_verify:" + token)).thenReturn(null);

            // When
            String result = emailVerificationService.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should construct correct Redis key")
        void shouldConstructCorrectRedisKey() {
            // Given
            String token = "test-verify-123";

            // When
            emailVerificationService.validateToken(token);

            // Then
            verify(valueOperations).get("email_verify:test-verify-123");
        }

        @Test
        @DisplayName("should handle malformed tokens")
        void shouldHandleMalformedTokens() {
            // Given
            String malformedToken = "invalid!@#$%token";
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            String result = emailVerificationService.validateToken(malformedToken);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("invalidateToken")
    class InvalidateTokenTests {

        @Test
        @DisplayName("should delete token from Redis")
        void shouldDeleteTokenFromRedis() {
            // Given
            String token = "valid-token-to-delete";

            // When
            emailVerificationService.invalidateToken(token);

            // Then
            verify(redisTemplate).delete("email_verify:" + token);
        }

        @Test
        @DisplayName("should handle deletion of non-existent token")
        void shouldHandleNonExistentToken() {
            // Given
            String token = "nonexistent-token";
            when(redisTemplate.delete(anyString())).thenReturn(Boolean.FALSE);

            // When/Then
            assertThatCode(() -> emailVerificationService.invalidateToken(token))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should construct correct Redis key for deletion")
        void shouldConstructCorrectRedisKeyForDeletion() {
            // Given
            String token = "delete-me-789";

            // When
            emailVerificationService.invalidateToken(token);

            // Then
            verify(redisTemplate).delete("email_verify:delete-me-789");
        }
    }

    @Nested
    @DisplayName("integration scenarios")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("should create, validate, and invalidate token lifecycle")
        void shouldHandleCompleteTokenLifecycle() {
            // Given
            String email = "lifecycle@example.com";
            String token = "lifecycle-verification-token";

            // Simulate token creation
            when(valueOperations.get("email_verify:" + token)).thenReturn(email);

            // When: Validate token
            String validatedEmail = emailVerificationService.validateToken(token);

            // Then
            assertThat(validatedEmail).isEqualTo(email);

            // When: Invalidate token
            emailVerificationService.invalidateToken(token);

            // Then
            verify(redisTemplate).delete("email_verify:" + token);
        }

        @Test
        @DisplayName("should not validate token after invalidation")
        void shouldNotValidateAfterInvalidation() {
            // Given
            String token = "will-be-invalidated";
            when(valueOperations.get("email_verify:" + token))
                    .thenReturn("user@example.com")
                    .thenReturn(null); // After invalidation

            // When
            String validBefore = emailVerificationService.validateToken(token);
            emailVerificationService.invalidateToken(token);
            String validAfter = emailVerificationService.validateToken(token);

            // Then
            assertThat(validBefore).isNotNull();
            assertThat(validAfter).isNull();
        }

        @Test
        @DisplayName("should allow multiple email verifications for different users")
        void shouldAllowMultipleVerifications() {
            // Given
            String email1 = "user1@example.com";
            String email2 = "user2@example.com";

            // When
            String token1 = emailVerificationService.createVerificationToken(email1);
            String token2 = emailVerificationService.createVerificationToken(email2);

            // Then
            assertThat(token1).isNotEqualTo(token2);
            verify(valueOperations, times(2)).set(anyString(), anyString(), any(Duration.class));
        }
    }
}
