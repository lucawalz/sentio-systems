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
 * Unit tests for {@link PasswordResetService}.
 * Tests token generation, validation, and email sending.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class PasswordResetServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ResendEmailService emailService;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        passwordResetService = new PasswordResetService(redisTemplate, emailService, 1);
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:3000");
    }

    @Nested
    @DisplayName("createResetToken")
    class CreateResetTokenTests {

        @Test
        @DisplayName("should generate token and store in Redis")
        void shouldGenerateTokenAndStore() {
            // Given
            String email = "user@example.com";

            // When
            String token = passwordResetService.createResetToken(email);

            // Then
            assertThat(token).isNotBlank();
            assertThat(token).hasSize(43); // Base64 URL-encoded 32 bytes = 43 chars

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

            verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());

            assertThat(keyCaptor.getValue()).startsWith("password_reset:");
            assertThat(valueCaptor.getValue()).isEqualTo(email);
            assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofHours(1));
        }

        @Test
        @DisplayName("should send password reset email")
        void shouldSendPasswordResetEmail() {
            // Given
            String email = "user@example.com";

            // When
            passwordResetService.createResetToken(email);

            // Then
            verify(emailService).sendHtmlEmail(
                    eq(email),
                    eq("Reset your password"),
                    contains("http://localhost:3000/reset-password?token="),
                    isNull()
            );
        }

        @Test
        @DisplayName("should generate unique tokens for same email")
        void shouldGenerateUniqueTokens() {
            // Given
            String email = "user@example.com";

            // When
            String token1 = passwordResetService.createResetToken(email);
            String token2 = passwordResetService.createResetToken(email);

            // Then
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("should handle email sending failure gracefully")
        void shouldHandleEmailFailureGracefully() {
            // Given
            String email = "user@example.com";
            doThrow(new RuntimeException("Email service down"))
                    .when(emailService).sendHtmlEmail(anyString(), anyString(), anyString(), any());

            // When/Then - should not throw, just log
            assertThatCode(() -> passwordResetService.createResetToken(email))
                    .doesNotThrowAnyException();

            // Verify token was still stored
            verify(valueOperations).set(anyString(), eq(email), any(Duration.class));
        }

        @Test
        @DisplayName("should handle null email gracefully")
        void shouldHandleNullEmail() {
            // When/Then
            assertThatCode(() -> passwordResetService.createResetToken(null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("should return email for valid token")
        void shouldReturnEmailForValidToken() {
            // Given
            String token = "valid-token-abc123";
            String email = "user@example.com";
            when(valueOperations.get("password_reset:" + token)).thenReturn(email);

            // When
            String result = passwordResetService.validateToken(token);

            // Then
            assertThat(result).isEqualTo(email);
            verify(valueOperations).get("password_reset:" + token);
        }

        @Test
        @DisplayName("should return null for expired token")
        void shouldReturnNullForExpiredToken() {
            // Given
            String token = "expired-token";
            when(valueOperations.get("password_reset:" + token)).thenReturn(null);

            // When
            String result = passwordResetService.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null for non-existent token")
        void shouldReturnNullForNonExistentToken() {
            // Given
            String token = "nonexistent-token";
            when(valueOperations.get("password_reset:" + token)).thenReturn(null);

            // When
            String result = passwordResetService.validateToken(token);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should construct correct Redis key")
        void shouldConstructCorrectRedisKey() {
            // Given
            String token = "test-token-123";

            // When
            passwordResetService.validateToken(token);

            // Then
            verify(valueOperations).get("password_reset:test-token-123");
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
            passwordResetService.invalidateToken(token);

            // Then
            verify(redisTemplate).delete("password_reset:" + token);
        }

        @Test
        @DisplayName("should handle deletion of non-existent token")
        void shouldHandleNonExistentToken() {
            // Given
            String token = "nonexistent-token";
            when(redisTemplate.delete(anyString())).thenReturn(Boolean.FALSE);

            // When/Then
            assertThatCode(() -> passwordResetService.invalidateToken(token))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should construct correct Redis key for deletion")
        void shouldConstructCorrectRedisKeyForDeletion() {
            // Given
            String token = "test-token-456";

            // When
            passwordResetService.invalidateToken(token);

            // Then
            verify(redisTemplate).delete("password_reset:test-token-456");
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
            String token = "lifecycle-token";

            // Simulate token creation
            when(valueOperations.get("password_reset:" + token)).thenReturn(email);

            // When: Validate token
            String validatedEmail = passwordResetService.validateToken(token);

            // Then
            assertThat(validatedEmail).isEqualTo(email);

            // When: Invalidate token
            passwordResetService.invalidateToken(token);

            // Then
            verify(redisTemplate).delete("password_reset:" + token);
        }

        @Test
        @DisplayName("should not validate token after invalidation")
        void shouldNotValidateAfterInvalidation() {
            // Given
            String token = "will-be-invalidated";
            when(valueOperations.get("password_reset:" + token))
                    .thenReturn("user@example.com")
                    .thenReturn(null); // After invalidation

            // When
            String validBefore = passwordResetService.validateToken(token);
            passwordResetService.invalidateToken(token);
            String validAfter = passwordResetService.validateToken(token);

            // Then
            assertThat(validBefore).isNotNull();
            assertThat(validAfter).isNull();
        }
    }
}
