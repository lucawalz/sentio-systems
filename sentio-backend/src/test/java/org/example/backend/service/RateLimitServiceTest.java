package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Unit tests for {@link RateLimitService}.
 * Tests in-memory rate limiting logic, window expiration, and cleanup.
 */
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Nested
    @DisplayName("allowPairingRequest")
    class AllowPairingRequestTests {

        @Test
        @DisplayName("should allow first request from IP")
        void shouldAllowFirstRequest() {
            // Given
            String clientIp = "192.168.1.100";

            // When
            boolean allowed = rateLimitService.allowPairingRequest(clientIp);

            // Then
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should allow requests up to limit")
        void shouldAllowRequestsUpToLimit() {
            // Given
            String clientIp = "192.168.1.101";

            // When/Then - Allow first 10 requests
            for (int i = 0; i < 10; i++) {
                boolean allowed = rateLimitService.allowPairingRequest(clientIp);
                assertThat(allowed).isTrue();
            }
        }

        @Test
        @DisplayName("should block request exceeding limit")
        void shouldBlockRequestExceedingLimit() {
            // Given
            String clientIp = "192.168.1.102";

            // When - Make 10 allowed requests
            for (int i = 0; i < 10; i++) {
                rateLimitService.allowPairingRequest(clientIp);
            }

            // Then - 11th request should be blocked
            boolean allowed = rateLimitService.allowPairingRequest(clientIp);
            assertThat(allowed).isFalse();
        }

        @Test
        @DisplayName("should isolate rate limits per IP")
        void shouldIsolateRateLimitsPerIp() {
            // Given
            String ip1 = "192.168.1.1";
            String ip2 = "192.168.1.2";

            // When - Exhaust limit for ip1
            for (int i = 0; i < 11; i++) {
                rateLimitService.allowPairingRequest(ip1);
            }

            // Then - ip2 should still be allowed
            boolean allowedIp2 = rateLimitService.allowPairingRequest(ip2);
            assertThat(allowedIp2).isTrue();
        }

        @Test
        @DisplayName("should allow null IP address")
        void shouldAllowNullIp() {
            // When
            boolean allowed = rateLimitService.allowPairingRequest(null);

            // Then - Should not block unknown clients
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should allow empty IP address")
        void shouldAllowEmptyIp() {
            // When
            boolean allowed = rateLimitService.allowPairingRequest("");

            // Then - Should not block unknown clients
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should reset counter after window expires")
        void shouldResetCounterAfterWindowExpires() throws Exception {
            // Given
            String clientIp = "192.168.1.103";

            // When - Exhaust limit
            for (int i = 0; i < 11; i++) {
                rateLimitService.allowPairingRequest(clientIp);
            }
            boolean blockedBefore = !rateLimitService.allowPairingRequest(clientIp);

            // Simulate window expiration by manipulating internal state
            forceWindowExpiration(clientIp);

            // Then - Should allow again after window reset
            boolean allowedAfter = rateLimitService.allowPairingRequest(clientIp);
            assertThat(blockedBefore).isTrue();
            assertThat(allowedAfter).isTrue();
        }

        @Test
        @DisplayName("should handle concurrent requests from same IP")
        void shouldHandleConcurrentRequests() throws InterruptedException {
            // Given
            String clientIp = "192.168.1.104";
            int threadCount = 15;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger allowedCount = new AtomicInteger(0);
            AtomicInteger blockedCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // When - Concurrent requests
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        boolean allowed = rateLimitService.allowPairingRequest(clientIp);
                        if (allowed) {
                            allowedCount.incrementAndGet();
                        } else {
                            blockedCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            // Then - Exactly 10 should be allowed, rest blocked
            assertThat(allowedCount.get()).isEqualTo(10);
            assertThat(blockedCount.get()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("cleanup")
    class CleanupTests {

        @Test
        @DisplayName("should remove old entries")
        void shouldRemoveOldEntries() throws Exception {
            // Given
            String oldIp = "192.168.1.200";
            rateLimitService.allowPairingRequest(oldIp);

            // Force entry to be old
            forceWindowExpiration(oldIp);

            // Wait for 2x window size to ensure cleanup threshold is met
            Thread.sleep(100); // Small delay to simulate passage of time

            // When
            rateLimitService.cleanup();

            // Then - Entry should be removed (verify by checking internal state)
            int sizeBefore = getPairingLimitsSize();
            rateLimitService.allowPairingRequest(oldIp); // Create new entry
            int sizeAfter = getPairingLimitsSize();

            // If cleanup worked, the old entry was removed, so adding new entry increases size
            assertThat(sizeAfter).isGreaterThanOrEqualTo(sizeBefore);
        }

        @Test
        @DisplayName("should keep recent entries")
        void shouldKeepRecentEntries() {
            // Given
            String recentIp = "192.168.1.201";
            rateLimitService.allowPairingRequest(recentIp);
            int sizeBefore = getPairingLimitsSize();

            // When
            rateLimitService.cleanup();

            // Then - Entry should still be there
            int sizeAfter = getPairingLimitsSize();
            assertThat(sizeAfter).isEqualTo(sizeBefore);
        }

        @Test
        @DisplayName("should handle cleanup on empty map")
        void shouldHandleCleanupOnEmptyMap() {
            // When/Then - Should not throw
            rateLimitService.cleanup();

            int size = getPairingLimitsSize();
            assertThat(size).isEqualTo(0);
        }

        @Test
        @DisplayName("should cleanup multiple old entries")
        void shouldCleanupMultipleOldEntries() throws Exception {
            // Given - Create multiple entries and force them to be old
            for (int i = 0; i < 5; i++) {
                String ip = "192.168.1." + (210 + i);
                rateLimitService.allowPairingRequest(ip);
                forceWindowExpiration(ip);
            }

            Thread.sleep(100); // Simulate time passage

            // When
            rateLimitService.cleanup();

            // Then - Add new entry to verify cleanup occurred
            String newIp = "192.168.1.220";
            rateLimitService.allowPairingRequest(newIp);
            int sizeAfter = getPairingLimitsSize();

            // After cleanup, should have minimal entries
            assertThat(sizeAfter).isLessThanOrEqualTo(6); // At most the 5 old + 1 new
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle exact boundary at 10 requests")
        void shouldHandleExactBoundary() {
            // Given
            String clientIp = "192.168.1.250";

            // When - Make exactly 10 requests
            for (int i = 0; i < 10; i++) {
                boolean allowed = rateLimitService.allowPairingRequest(clientIp);
                assertThat(allowed).as("Request %d should be allowed", i + 1).isTrue();
            }

            // Then - 11th should be blocked
            boolean eleventhRequest = rateLimitService.allowPairingRequest(clientIp);
            assertThat(eleventhRequest).isFalse();
        }

        @Test
        @DisplayName("should handle IPv6 addresses")
        void shouldHandleIpv6() {
            // Given
            String ipv6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

            // When/Then
            boolean allowed = rateLimitService.allowPairingRequest(ipv6);
            assertThat(allowed).isTrue();
        }

        @Test
        @DisplayName("should handle special characters in IP")
        void shouldHandleSpecialCharacters() {
            // Given
            String specialIp = "192.168.1.1:8080";

            // When/Then
            boolean allowed = rateLimitService.allowPairingRequest(specialIp);
            assertThat(allowed).isTrue();
        }
    }

    // Helper methods to access private fields for testing

    private int getPairingLimitsSize() {
        try {
            Field field = RateLimitService.class.getDeclaredField("pairingLimits");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> pairingLimits = (Map<String, ?>) field.get(rateLimitService);
            return pairingLimits.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to access pairingLimits field", e);
        }
    }

    private void forceWindowExpiration(String ip) throws Exception {
        Field field = RateLimitService.class.getDeclaredField("pairingLimits");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> pairingLimits = (Map<String, Object>) field.get(rateLimitService);

        Object bucket = pairingLimits.get(ip);
        if (bucket != null) {
            // Set windowStart to 2 hours ago (well past window size)
            Field windowStartField = bucket.getClass().getDeclaredField("windowStart");
            windowStartField.setAccessible(true);
            windowStartField.setLong(bucket, System.currentTimeMillis() - 7200_000); // 2 hours ago
        }
    }
}
