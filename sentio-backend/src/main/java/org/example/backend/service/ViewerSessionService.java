package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for managing viewer sessions with Redis-based reference counting.
 * 
 * Uses Redis to track active viewers per device stream with TTL-based
 * auto-cleanup.
 * This ensures proper handling of multi-user scenarios and unexpected
 * disconnections.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewerSessionService {

    private final StringRedisTemplate redisTemplate;

    private static final Duration SESSION_TTL = Duration.ofSeconds(120);
    private static final String SESSION_KEY_PREFIX = "stream:session:";
    private static final String COUNT_KEY_PREFIX = "stream:count:";

    /**
     * Generate a new unique session ID for a viewer.
     */
    public String createSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Register a viewer joining a stream.
     * 
     * @param deviceId  The device being watched
     * @param sessionId Unique session ID for this viewer
     * @return true if this is the first viewer (stream should start)
     */
    public boolean joinStream(String deviceId, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + deviceId + ":" + sessionId;
        String countKey = COUNT_KEY_PREFIX + deviceId;

        // Check if this session already exists (duplicate join)
        Boolean exists = redisTemplate.hasKey(sessionKey);
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Session {} already exists for device {}, extending TTL", sessionId, deviceId);
            redisTemplate.expire(sessionKey, SESSION_TTL);
            return false;
        }

        // Create session with TTL
        redisTemplate.opsForValue().set(sessionKey, "active", SESSION_TTL);

        // Increment viewer count atomically
        Long count = redisTemplate.opsForValue().increment(countKey);
        log.info("Viewer joined stream for device {}: {} viewer(s) now (session: {})",
                deviceId, count, sessionId);

        return count != null && count == 1; // First viewer
    }

    /**
     * Register a viewer leaving a stream.
     * 
     * @param deviceId  The device being watched
     * @param sessionId Unique session ID for this viewer
     * @return true if this was the last viewer (stream should stop)
     */
    public boolean leaveStream(String deviceId, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + deviceId + ":" + sessionId;
        String countKey = COUNT_KEY_PREFIX + deviceId;

        // Check if session exists
        Boolean exists = redisTemplate.hasKey(sessionKey);
        if (!Boolean.TRUE.equals(exists)) {
            log.debug("Session {} not found for device {} (already expired or never existed)",
                    sessionId, deviceId);
            return false;
        }

        // Delete session
        redisTemplate.delete(sessionKey);

        // Decrement viewer count atomically (don't go below 0)
        Long count = redisTemplate.opsForValue().decrement(countKey);
        if (count == null || count < 0) {
            count = 0L;
            redisTemplate.delete(countKey);
        }

        log.info("Viewer left stream for device {}: {} viewer(s) remaining (session: {})",
                deviceId, count, sessionId);

        if (count == 0) {
            redisTemplate.delete(countKey);
            return true; // Last viewer left
        }

        return false;
    }

    /**
     * Extend a viewer's session TTL (called periodically by frontend).
     * 
     * @param deviceId  The device being watched
     * @param sessionId Unique session ID for this viewer
     * @return true if session was extended, false if session not found
     */
    public boolean heartbeat(String deviceId, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + deviceId + ":" + sessionId;

        Boolean exists = redisTemplate.hasKey(sessionKey);
        if (!Boolean.TRUE.equals(exists)) {
            log.debug("Heartbeat for unknown session {} on device {}", sessionId, deviceId);
            return false;
        }

        redisTemplate.expire(sessionKey, SESSION_TTL);
        log.debug("Heartbeat extended session {} for device {}", sessionId, deviceId);
        return true;
    }

    /**
     * Get current viewer count for a device.
     * 
     * @param deviceId The device ID
     * @return Number of active viewers
     */
    public long getViewerCount(String deviceId) {
        String countKey = COUNT_KEY_PREFIX + deviceId;
        String count = redisTemplate.opsForValue().get(countKey);
        return count != null ? Long.parseLong(count) : 0;
    }

    /**
     * Check if a specific session is active.
     * 
     * @param deviceId  The device ID
     * @param sessionId The session ID
     * @return true if session is active
     */
    public boolean isSessionActive(String deviceId, String sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + deviceId + ":" + sessionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }
}
