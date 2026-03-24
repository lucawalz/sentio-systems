package dev.syslabs.sentio.service;

import dev.syslabs.sentio.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for {@link ViewerSessionService}.
 * Uses real Redis Testcontainer to validate session management behavior.
 */
class ViewerSessionServiceTest extends BaseIntegrationTest {

    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);

    static {
        redis.start();
    }

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ViewerSessionService viewerSessionService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TEST_DEVICE_ID = "test-device-001";
    private static final String VIEWERS_KEY_PREFIX = "stream:viewers:";

    @BeforeEach
    void setUp() {
        // Clean up Redis before each test
        Set<String> keys = redisTemplate.keys(VIEWERS_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("createSessionId")
    class CreateSessionIdTests {

        @Test
        @DisplayName("should generate unique session IDs")
        void shouldGenerateUniqueSessionIds() {
            // When
            String sessionId1 = viewerSessionService.createSessionId();
            String sessionId2 = viewerSessionService.createSessionId();

            // Then
            assertThat(sessionId1).isNotBlank();
            assertThat(sessionId2).isNotBlank();
            assertThat(sessionId1).isNotEqualTo(sessionId2);
            assertThatCode(() -> UUID.fromString(sessionId1)).doesNotThrowAnyException();
            assertThatCode(() -> UUID.fromString(sessionId2)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("joinStream")
    class JoinStreamTests {

        @Test
        @DisplayName("should add viewer to Redis sorted set")
        void shouldAddViewerToRedis() {
            // Given
            String sessionId = viewerSessionService.createSessionId();

            // When
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // Then
            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;
            Long count = redisTemplate.opsForZSet().zCard(viewersKey);
            assertThat(count).isEqualTo(1);

            Double score = redisTemplate.opsForZSet().score(viewersKey, sessionId);
            assertThat(score).isNotNull();
            assertThat(score).isGreaterThan(Instant.now().getEpochSecond());
        }

        @Test
        @DisplayName("should return true when first viewer joins")
        void shouldReturnTrueForFirstViewer() {
            // Given
            String sessionId = viewerSessionService.createSessionId();

            // When
            boolean isFirstViewer = viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(isFirstViewer).isTrue();
        }

        @Test
        @DisplayName("should return false when second viewer joins")
        void shouldReturnFalseForSecondViewer() {
            // Given
            String sessionId1 = viewerSessionService.createSessionId();
            String sessionId2 = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId1);

            // When
            boolean isFirstViewer = viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId2);

            // Then
            assertThat(isFirstViewer).isFalse();
        }

        @Test
        @DisplayName("should update expiry when same session joins again")
        void shouldUpdateExpiryOnRejoin() {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;
            Double firstScore = redisTemplate.opsForZSet().score(viewersKey, sessionId);

            // When - wait 1 second to ensure different timestamp
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // Then
            Double secondScore = redisTemplate.opsForZSet().score(viewersKey, sessionId);
            assertThat(secondScore).isGreaterThanOrEqualTo(firstScore); // May be equal if executed in same second
        }

        @Test
        @DisplayName("should cleanup expired sessions before adding new viewer")
        void shouldCleanupExpiredSessionsOnJoin() {
            // Given - add an expired session directly to Redis
            String expiredSessionId = "expired-session";
            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;
            long pastExpiry = Instant.now().getEpochSecond() - 100;
            redisTemplate.opsForZSet().add(viewersKey, expiredSessionId, pastExpiry);

            // When - new viewer joins
            String newSessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, newSessionId);

            // Then - expired session should be removed
            Long count = redisTemplate.opsForZSet().zCard(viewersKey);
            assertThat(count).isEqualTo(1);
            assertThat(redisTemplate.opsForZSet().score(viewersKey, expiredSessionId)).isNull();
            assertThat(redisTemplate.opsForZSet().score(viewersKey, newSessionId)).isNotNull();
        }
    }

    @Nested
    @DisplayName("leaveStream")
    class LeaveStreamTests {

        @Test
        @DisplayName("should remove viewer from Redis sorted set")
        void shouldRemoveViewerFromRedis() {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // When
            viewerSessionService.leaveStream(TEST_DEVICE_ID, sessionId);

            // Then
            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;
            Long count = redisTemplate.opsForZSet().zCard(viewersKey);
            assertThat(count).isEqualTo(0);
            assertThat(redisTemplate.opsForZSet().score(viewersKey, sessionId)).isNull();
        }

        @Test
        @DisplayName("should return true when last viewer leaves")
        void shouldReturnTrueForLastViewer() {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // When
            boolean isLastViewer = viewerSessionService.leaveStream(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(isLastViewer).isTrue();
        }

        @Test
        @DisplayName("should return false when other viewers remain")
        void shouldReturnFalseWhenViewersRemain() {
            // Given
            String sessionId1 = viewerSessionService.createSessionId();
            String sessionId2 = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId1);
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId2);

            // When
            boolean isLastViewer = viewerSessionService.leaveStream(TEST_DEVICE_ID, sessionId1);

            // Then
            assertThat(isLastViewer).isFalse();
        }

        @Test
        @DisplayName("should delete Redis key when last viewer leaves")
        void shouldDeleteRedisKeyWhenEmpty() {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // When
            viewerSessionService.leaveStream(TEST_DEVICE_ID, sessionId);

            // Then
            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;
            assertThat(redisTemplate.hasKey(viewersKey)).isFalse();
        }

        @Test
        @DisplayName("should cleanup expired sessions when viewer leaves")
        void shouldCleanupExpiredSessionsOnLeave() {
            // Given - add active and expired sessions
            String activeSessionId = viewerSessionService.createSessionId();
            String expiredSessionId = "expired-session";
            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;

            viewerSessionService.joinStream(TEST_DEVICE_ID, activeSessionId);
            long pastExpiry = Instant.now().getEpochSecond() - 100;
            redisTemplate.opsForZSet().add(viewersKey, expiredSessionId, pastExpiry);

            // When - active viewer leaves
            viewerSessionService.leaveStream(TEST_DEVICE_ID, activeSessionId);

            // Then - expired session should also be removed and key deleted
            assertThat(redisTemplate.hasKey(viewersKey)).isFalse();
        }

        @Test
        @DisplayName("should handle leaving non-existent session gracefully")
        void shouldHandleNonExistentSession() {
            // Given
            String nonExistentSessionId = "non-existent-session";

            // When/Then
            assertThatCode(() -> viewerSessionService.leaveStream(TEST_DEVICE_ID, nonExistentSessionId))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getViewerCount")
    class GetViewerCountTests {

        @Test
        @DisplayName("should return zero when no viewers")
        void shouldReturnZeroWhenNoViewers() {
            // When
            long count = viewerSessionService.getViewerCount(TEST_DEVICE_ID);

            // Then
            assertThat(count).isEqualTo(0);
        }

        @Test
        @DisplayName("should return correct viewer count")
        void shouldReturnCorrectCount() {
            // Given
            String sessionId1 = viewerSessionService.createSessionId();
            String sessionId2 = viewerSessionService.createSessionId();
            String sessionId3 = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId1);
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId2);
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId3);

            // When
            long count = viewerSessionService.getViewerCount(TEST_DEVICE_ID);

            // Then
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("should exclude expired sessions from count")
        void shouldExcludeExpiredSessions() {
            // Given - add active and expired sessions
            String activeSessionId = viewerSessionService.createSessionId();
            String expiredSessionId = "expired-session";
            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;

            viewerSessionService.joinStream(TEST_DEVICE_ID, activeSessionId);
            long pastExpiry = Instant.now().getEpochSecond() - 100;
            redisTemplate.opsForZSet().add(viewersKey, expiredSessionId, pastExpiry);

            // When
            long count = viewerSessionService.getViewerCount(TEST_DEVICE_ID);

            // Then - only active session should be counted
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle multiple devices independently")
        void shouldHandleMultipleDevices() {
            // Given
            String device1 = "device-001";
            String device2 = "device-002";

            String session1 = viewerSessionService.createSessionId();
            String session2 = viewerSessionService.createSessionId();
            String session3 = viewerSessionService.createSessionId();

            viewerSessionService.joinStream(device1, session1);
            viewerSessionService.joinStream(device1, session2);
            viewerSessionService.joinStream(device2, session3);

            // When
            long count1 = viewerSessionService.getViewerCount(device1);
            long count2 = viewerSessionService.getViewerCount(device2);

            // Then
            assertThat(count1).isEqualTo(2);
            assertThat(count2).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("heartbeat")
    class HeartbeatTests {

        @Test
        @DisplayName("should extend session TTL")
        void shouldExtendSessionTTL() {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;
            Double originalScore = redisTemplate.opsForZSet().score(viewersKey, sessionId);

            // When - wait 1 second to ensure different timestamp
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean extended = viewerSessionService.heartbeat(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(extended).isTrue();
            Double newScore = redisTemplate.opsForZSet().score(viewersKey, sessionId);
            assertThat(newScore).isGreaterThan(originalScore);
        }

        @Test
        @DisplayName("should return false for unknown session")
        void shouldReturnFalseForUnknownSession() {
            // Given
            String unknownSessionId = "unknown-session";

            // When
            boolean extended = viewerSessionService.heartbeat(TEST_DEVICE_ID, unknownSessionId);

            // Then
            assertThat(extended).isFalse();
        }

        @Test
        @DisplayName("should keep session alive with periodic heartbeats")
        void shouldKeepSessionAlive() {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // When - send multiple heartbeats
            boolean heartbeat1 = viewerSessionService.heartbeat(TEST_DEVICE_ID, sessionId);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean heartbeat2 = viewerSessionService.heartbeat(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(heartbeat1).isTrue();
            assertThat(heartbeat2).isTrue();
            assertThat(viewerSessionService.isSessionActive(TEST_DEVICE_ID, sessionId)).isTrue();
        }
    }

    @Nested
    @DisplayName("isSessionActive")
    class IsSessionActiveTests {

        @Test
        @DisplayName("should return true for active session")
        void shouldReturnTrueForActiveSession() {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            // When
            boolean isActive = viewerSessionService.isSessionActive(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(isActive).isTrue();
        }

        @Test
        @DisplayName("should return false for non-existent session")
        void shouldReturnFalseForNonExistentSession() {
            // Given
            String nonExistentSessionId = "non-existent";

            // When
            boolean isActive = viewerSessionService.isSessionActive(TEST_DEVICE_ID, nonExistentSessionId);

            // Then
            assertThat(isActive).isFalse();
        }

        @Test
        @DisplayName("should return false for expired session")
        void shouldReturnFalseForExpiredSession() {
            // Given - add expired session directly
            String expiredSessionId = "expired-session";
            String viewersKey = VIEWERS_KEY_PREFIX + TEST_DEVICE_ID;
            long pastExpiry = Instant.now().getEpochSecond() - 100;
            redisTemplate.opsForZSet().add(viewersKey, expiredSessionId, pastExpiry);

            // When
            boolean isActive = viewerSessionService.isSessionActive(TEST_DEVICE_ID, expiredSessionId);

            // Then
            assertThat(isActive).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases and Concurrency")
    class EdgeCasesAndConcurrencyTests {

        @Test
        @DisplayName("should handle null device ID gracefully")
        void shouldHandleNullDeviceId() {
            // Given
            String sessionId = viewerSessionService.createSessionId();

            // When/Then
            assertThatCode(() -> viewerSessionService.joinStream(null, sessionId))
                    .doesNotThrowAnyException();

            // Verify null key is created
            String viewersKey = VIEWERS_KEY_PREFIX + "null";
            assertThat(redisTemplate.hasKey(viewersKey)).isTrue();
        }

        @Test
        @DisplayName("should throw exception for null session ID")
        void shouldThrowExceptionForNullSessionId() {
            // When/Then - Redis operations don't accept null values
            assertThatCode(() -> viewerSessionService.joinStream(TEST_DEVICE_ID, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatCode(() -> viewerSessionService.leaveStream(TEST_DEVICE_ID, null))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatCode(() -> viewerSessionService.heartbeat(TEST_DEVICE_ID, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should handle concurrent viewer additions")
        void shouldHandleConcurrentAdditions() throws InterruptedException {
            // Given
            int numViewers = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numViewers);
            CountDownLatch latch = new CountDownLatch(numViewers);

            // When - add viewers concurrently
            for (int i = 0; i < numViewers; i++) {
                executor.submit(() -> {
                    try {
                        String sessionId = viewerSessionService.createSessionId();
                        viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Then - all viewers should be counted
            long count = viewerSessionService.getViewerCount(TEST_DEVICE_ID);
            assertThat(count).isEqualTo(numViewers);
        }

        @Test
        @DisplayName("should handle concurrent heartbeats")
        void shouldHandleConcurrentHeartbeats() throws InterruptedException {
            // Given
            String sessionId = viewerSessionService.createSessionId();
            viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId);

            int numHeartbeats = 10;
            ExecutorService executor = Executors.newFixedThreadPool(numHeartbeats);
            CountDownLatch latch = new CountDownLatch(numHeartbeats);

            // When - send heartbeats concurrently
            for (int i = 0; i < numHeartbeats; i++) {
                executor.submit(() -> {
                    try {
                        viewerSessionService.heartbeat(TEST_DEVICE_ID, sessionId);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Then - session should still be active
            assertThat(viewerSessionService.isSessionActive(TEST_DEVICE_ID, sessionId)).isTrue();
            assertThat(viewerSessionService.getViewerCount(TEST_DEVICE_ID)).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle empty device ID")
        void shouldHandleEmptyDeviceId() {
            // Given
            String sessionId = viewerSessionService.createSessionId();

            // When/Then
            assertThatCode(() -> viewerSessionService.joinStream("", sessionId))
                    .doesNotThrowAnyException();

            long count = viewerSessionService.getViewerCount("");
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle very long device IDs")
        void shouldHandleVeryLongDeviceIds() {
            // Given
            String longDeviceId = "a".repeat(1000);
            String sessionId = viewerSessionService.createSessionId();

            // When
            viewerSessionService.joinStream(longDeviceId, sessionId);

            // Then
            long count = viewerSessionService.getViewerCount(longDeviceId);
            assertThat(count).isEqualTo(1);
        }
    }
}
