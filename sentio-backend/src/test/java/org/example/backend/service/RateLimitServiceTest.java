package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RateLimitService}.
 * Tests Redis-backed rate limiting logic with mocked StringRedisTemplate.
 *
 * <p>
 * The tests use Mockito mocks for StringRedisTemplate to verify that
 * the correct Redis keys are used and that TTL is set appropriately.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Nested
    @DisplayName("allowPairingRequest")
    class AllowPairingRequestTests {

        @Test
        @DisplayName("should allow first request from IP")
        void shouldAllowFirstRequest() {
            // Given
            String clientIp = "192.168.1.100";
            when(valueOps.increment("rate_limit:pairing:192.168.1.100")).thenReturn(1L);

            // When
            boolean allowed = rateLimitService.allowPairingRequest(clientIp);

            // Then
            assertThat(allowed).isTrue();
            verify(redisTemplate).expire(eq("rate_limit:pairing:192.168.1.100"), any());
        }

        @Test
        @DisplayName("should allow requests up to limit (count=10)")
        void shouldAllowRequestsAtLimit() {
            // Given
            String clientIp = "192.168.1.101";
            when(valueOps.increment("rate_limit:pairing:192.168.1.101")).thenReturn(10L);

            // When
            boolean allowed = rateLimitService.allowPairingRequest(clientIp);

            // Then
            assertThat(allowed).isTrue();
            // TTL should NOT be set again (count != 1)
            verify(redisTemplate, never()).expire(any(), any());
        }

        @Test
        @DisplayName("should block request exceeding limit (count=11)")
        void shouldBlockRequestExceedingLimit() {
            // Given
            String clientIp = "192.168.1.102";
            when(valueOps.increment("rate_limit:pairing:192.168.1.102")).thenReturn(11L);

            // When
            boolean allowed = rateLimitService.allowPairingRequest(clientIp);

            // Then
            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("should use correct Redis key format")
        void shouldUseCorrectRedisKey() {
            // Given
            String clientIp = "10.0.0.1";
            when(valueOps.increment("rate_limit:pairing:10.0.0.1")).thenReturn(1L);

            // When
            rateLimitService.allowPairingRequest(clientIp);

            // Then
            verify(valueOps).increment("rate_limit:pairing:10.0.0.1");
        }

        @Test
        @DisplayName("should set TTL only on first request (count=1)")
        void shouldSetTtlOnlyOnFirstRequest() {
            // Given
            when(valueOps.increment("rate_limit:pairing:192.168.1.103")).thenReturn(1L);

            // When
            rateLimitService.allowPairingRequest("192.168.1.103");

            // Then
            verify(redisTemplate).expire(eq("rate_limit:pairing:192.168.1.103"), any());
        }

        @Test
        @DisplayName("should NOT set TTL on subsequent requests (count>1)")
        void shouldNotSetTtlOnSubsequentRequests() {
            // Given
            when(valueOps.increment("rate_limit:pairing:192.168.1.104")).thenReturn(5L);

            // When
            rateLimitService.allowPairingRequest("192.168.1.104");

            // Then
            verify(redisTemplate, never()).expire(any(), any());
        }

        @Test
        @DisplayName("should allow null IP address")
        void shouldAllowNullIp() {
            // When
            boolean allowed = rateLimitService.allowPairingRequest(null);

            // Then — null IPs are allowed to avoid blocking unidentifiable clients
            assertThat(allowed).isTrue();
            verifyNoInteractions(valueOps);
        }

        @Test
        @DisplayName("should allow empty IP address")
        void shouldAllowEmptyIp() {
            // When
            boolean allowed = rateLimitService.allowPairingRequest("");

            // Then
            assertThat(allowed).isTrue();
            verifyNoInteractions(valueOps);
        }

        @Test
        @DisplayName("should handle IPv6 addresses")
        void shouldHandleIpv6() {
            // Given
            String ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";
            when(valueOps.increment("rate_limit:pairing:" + ipv6)).thenReturn(1L);

            // When
            boolean allowed = rateLimitService.allowPairingRequest(ipv6);

            // Then
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should isolate rate limits per IP")
        void shouldIsolateRateLimitsPerIp() {
            // Given
            when(valueOps.increment("rate_limit:pairing:192.168.1.1")).thenReturn(11L);
            when(valueOps.increment("rate_limit:pairing:192.168.1.2")).thenReturn(1L);

            // When
            boolean blockedIp1 = rateLimitService.allowPairingRequest("192.168.1.1");
            boolean allowedIp2 = rateLimitService.allowPairingRequest("192.168.1.2");

            // Then
            assertThat(blockedIp1).isFalse();
            assertThat(allowedIp2).isTrue();
        }
    }

    @Nested
    @DisplayName("allowStreamAuthAttempt")
    class AllowStreamAuthAttemptTests {

        @Test
        @DisplayName("should allow first stream auth attempt")
        void shouldAllowFirstAttempt() {
            // Given
            when(valueOps.increment("rate_limit:stream_auth:192.168.1.50")).thenReturn(1L);

            // When
            boolean allowed = rateLimitService.allowStreamAuthAttempt("192.168.1.50");

            // Then
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should block stream auth after 5 attempts")
        void shouldBlockAfterMaxAttempts() {
            // Given
            when(valueOps.increment("rate_limit:stream_auth:192.168.1.51")).thenReturn(6L);

            // When
            boolean allowed = rateLimitService.allowStreamAuthAttempt("192.168.1.51");

            // Then
            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("should allow null IP")
        void shouldAllowNullIp() {
            // When
            boolean allowed = rateLimitService.allowStreamAuthAttempt(null);

            // Then
            assertThat(allowed).isTrue();
            verifyNoInteractions(valueOps);
        }
    }

    @Nested
    @DisplayName("recordStreamAuthFailure")
    class RecordStreamAuthFailureTests {

        @Test
        @DisplayName("should increment failure counter for IP")
        void shouldIncrementFailureCounter() {
            // Given
            when(valueOps.increment("rate_limit:stream_auth:192.168.1.60")).thenReturn(1L);

            // When
            rateLimitService.recordStreamAuthFailure("192.168.1.60");

            // Then
            verify(valueOps).increment("rate_limit:stream_auth:192.168.1.60");
        }

        @Test
        @DisplayName("should set TTL on first failure")
        void shouldSetTtlOnFirstFailure() {
            // Given
            when(valueOps.increment("rate_limit:stream_auth:192.168.1.61")).thenReturn(1L);

            // When
            rateLimitService.recordStreamAuthFailure("192.168.1.61");

            // Then
            verify(redisTemplate).expire(eq("rate_limit:stream_auth:192.168.1.61"), any());
        }

        @Test
        @DisplayName("should handle null IP gracefully")
        void shouldHandleNullIp() {
            // When
            rateLimitService.recordStreamAuthFailure(null);

            // Then — no Redis interaction expected
            verifyNoInteractions(valueOps);
        }
    }

    @Nested
    @DisplayName("resetStreamAuthCounter")
    class ResetStreamAuthCounterTests {

        @Test
        @DisplayName("should delete the Redis key for IP")
        void shouldDeleteRedisKey() {
            // When
            rateLimitService.resetStreamAuthCounter("192.168.1.70");

            // Then
            verify(redisTemplate).delete("rate_limit:stream_auth:192.168.1.70");
        }

        @Test
        @DisplayName("should handle null IP gracefully")
        void shouldHandleNullIp() {
            // When
            rateLimitService.resetStreamAuthCounter(null);

            // Then — no Redis interaction
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("should handle empty IP gracefully")
        void shouldHandleEmptyIp() {
            // When
            rateLimitService.resetStreamAuthCounter("");

            // Then
            verify(redisTemplate, never()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle exact boundary at limit (count=10 pairing)")
        void shouldHandleExactBoundary() {
            // Given
            when(valueOps.increment("rate_limit:pairing:192.168.1.250")).thenReturn(10L);

            // When
            boolean allowed = rateLimitService.allowPairingRequest("192.168.1.250");

            // Then
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should handle exact boundary at limit (count=5 stream_auth)")
        void shouldHandleStreamAuthBoundary() {
            // Given
            when(valueOps.increment("rate_limit:stream_auth:192.168.1.251")).thenReturn(5L);

            // When
            boolean allowed = rateLimitService.allowStreamAuthAttempt("192.168.1.251");

            // Then
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should handle special characters in IP")
        void shouldHandleSpecialCharacters() {
            // Given
            String specialIp = "192.168.1.1:8080";
            when(valueOps.increment("rate_limit:pairing:" + specialIp)).thenReturn(1L);

            // When
            boolean allowed = rateLimitService.allowPairingRequest(specialIp);

            // Then
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should handle null increment return value")
        void shouldHandleNullIncrementReturn() {
            // Given - Redis returns null (unusual but possible edge case)
            when(valueOps.increment("rate_limit:pairing:192.168.1.252")).thenReturn(null);

            // When
            boolean allowed = rateLimitService.allowPairingRequest("192.168.1.252");

            // Then - Should be blocked when count is null (fail-safe)
            assertThat(allowed).isFalse();
        }
    }
}
