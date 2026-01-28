package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing viewer sessions using Redis Sorted Sets.
 * 
 * Uses a sorted set per device where:
 * - Score = expiration timestamp (Unix epoch seconds)
 * - Member = session ID
 * 
 * This design ensures the viewer count is always accurate by atomically
 * removing expired sessions before counting. No separate counter needed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ViewerSessionService {

    private final StringRedisTemplate redisTemplate;

    private static final long SESSION_TTL_SECONDS = 120;
    private static final String VIEWERS_KEY_PREFIX = "stream:viewers:";

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
        String viewersKey = VIEWERS_KEY_PREFIX + deviceId;
        long expiryTime = Instant.now().getEpochSecond() + SESSION_TTL_SECONDS;

        // Remove expired sessions before adding new one
        cleanupExpiredSessions(deviceId);

        // Add session to sorted set (or update expiry if already exists)
        Boolean added = redisTemplate.opsForZSet().add(viewersKey, sessionId, expiryTime);
        
        // Get current count after cleanup
        Long count = redisTemplate.opsForZSet().zCard(viewersKey);
        
        log.info("Viewer joined stream for device {}: {} viewer(s) now (session: {}, new: {})",
                deviceId, count, sessionId, added);

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
        String viewersKey = VIEWERS_KEY_PREFIX + deviceId;

        // Remove the session from sorted set
        Long removed = redisTemplate.opsForZSet().remove(viewersKey, sessionId);
        
        // Clean up expired sessions
        cleanupExpiredSessions(deviceId);
        
        // Get remaining count
        Long count = redisTemplate.opsForZSet().zCard(viewersKey);
        long viewerCount = count != null ? count : 0;

        log.info("Viewer left stream for device {}: {} viewer(s) remaining (session: {}, removed: {})",
                deviceId, viewerCount, sessionId, removed);

        if (viewerCount == 0) {
            redisTemplate.delete(viewersKey);
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
        String viewersKey = VIEWERS_KEY_PREFIX + deviceId;
        long expiryTime = Instant.now().getEpochSecond() + SESSION_TTL_SECONDS;

        // Update the session's expiry time (score) in sorted set
        Double oldScore = redisTemplate.opsForZSet().score(viewersKey, sessionId);
        
        if (oldScore == null) {
            log.debug("Heartbeat for unknown session {} on device {}", sessionId, deviceId);
            return false;
        }

        // Update expiry timestamp
        redisTemplate.opsForZSet().add(viewersKey, sessionId, expiryTime);
        log.debug("Heartbeat extended session {} for device {} (new expiry: {})", 
                sessionId, deviceId, expiryTime);
        return true;
    }

    /**
     * Get current viewer count for a device (automatically removes expired sessions).
     * 
     * @param deviceId The device ID
     * @return Number of active viewers
     */
    public long getViewerCount(String deviceId) {
        cleanupExpiredSessions(deviceId);
        String viewersKey = VIEWERS_KEY_PREFIX + deviceId;
        Long count = redisTemplate.opsForZSet().zCard(viewersKey);
        return count != null ? count : 0;
    }

    /**
     * Check if a specific session is active.
     * 
     * @param deviceId  The device ID
     * @param sessionId The session ID
     * @return true if session is active
     */
    public boolean isSessionActive(String deviceId, String sessionId) {
        String viewersKey = VIEWERS_KEY_PREFIX + deviceId;
        Double score = redisTemplate.opsForZSet().score(viewersKey, sessionId);
        
        if (score == null) {
            return false;
        }
        
        // Check if session has expired
        long now = Instant.now().getEpochSecond();
        return score > now;
    }

    /**
     * Remove expired sessions from the sorted set for a device.
     * This is called automatically by other methods to keep data clean.
     * 
     * @param deviceId The device ID
     */
    private void cleanupExpiredSessions(String deviceId) {
        String viewersKey = VIEWERS_KEY_PREFIX + deviceId;
        long now = Instant.now().getEpochSecond();
        
        // Remove all sessions with score (expiry time) less than current time
        Long removed = redisTemplate.opsForZSet().removeRangeByScore(viewersKey, 0, now);
        
        if (removed != null && removed > 0) {
            log.debug("Cleaned up {} expired session(s) for device {}", removed, deviceId);
        }
    }
}
