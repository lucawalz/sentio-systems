package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Distributed rate limiter backed by Redis.
 *
 * <h3>Strategy</h3>
 * Uses a <b>fixed-window counter</b> per (purpose, identifier) pair.
 * Each window is a Redis key with a TTL equal to the window size.
 * The key is auto-incremented on each request; when the count exceeds
 * the configured maximum the request is rejected.
 *
 * <h3>Redis Key Schema</h3>
 * 
 * <pre>
 *   rate_limit:{purpose}:{identifier}
 *
 *   Examples:
 *     rate_limit:pairing:192.168.1.10       → device pairing (10 req / 60 s)
 *     rate_limit:stream_auth:192.168.1.10   → RTMP auth failures (5 req / 60 s)
 * </pre>
 *
 * <h3>Why Redis?</h3>
 * In-memory counters (ConcurrentHashMap) are local to each JVM instance
 * and not shared across replicas. Under horizontal scaling the effective
 * limit becomes {@code limit × replica_count}, making protection meaningless.
 * Redis provides a single shared counter that works across all replicas.
 *
 * @see org.example.backend.controller.DeviceController — uses pairing rate
 *      limit
 * @see StreamService — uses stream auth rate limit
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "rate_limit:";

    // Pairing rate limit: 10 requests per 60-second window
    private static final int MAX_PAIRING_REQUESTS = 10;
    private static final Duration PAIRING_WINDOW = Duration.ofSeconds(60);

    // Stream auth rate limit: 5 failed attempts per 60-second window
    private static final int MAX_STREAM_AUTH_ATTEMPTS = 5;
    private static final Duration STREAM_AUTH_WINDOW = Duration.ofSeconds(60);

    /**
     * Check if a device-pairing request is allowed for this IP.
     * Limit: 10 requests per minute per IP.
     *
     * @param clientIp the client IP address
     * @return {@code true} if request is allowed, {@code false} if rate-limited
     */
    public boolean allowPairingRequest(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) {
            return true; // Don't block if we can't identify the client
        }
        return isAllowed("pairing", clientIp, MAX_PAIRING_REQUESTS, PAIRING_WINDOW);
    }

    /**
     * Check if a stream-auth attempt is allowed for this IP.
     * Limit: 5 failed attempts per minute per IP.
     * Only failed attempts count toward the limit.
     *
     * @param ip the source IP address
     * @return {@code true} if request is allowed, {@code false} if rate-limited
     */
    public boolean allowStreamAuthAttempt(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        return isAllowed("stream_auth", ip, MAX_STREAM_AUTH_ATTEMPTS, STREAM_AUTH_WINDOW);
    }

    /**
     * Record a failed stream-auth attempt, incrementing the counter.
     * Call this <em>after</em> authentication fails.
     *
     * @param ip the source IP address
     */
    public void recordStreamAuthFailure(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        increment("stream_auth", ip, STREAM_AUTH_WINDOW);
    }

    /**
     * Reset the stream-auth counter for this IP on successful auth.
     *
     * @param ip the source IP address
     */
    public void resetStreamAuthCounter(String ip) {
        if (ip == null || ip.isEmpty()) {
            return;
        }
        String key = KEY_PREFIX + "stream_auth:" + ip;
        redisTemplate.delete(key);
    }

    // ── internal ──────────────────────────────────────────────────────────

    /**
     * Core rate-limit check: increment the counter and compare against the max.
     */
    private boolean isAllowed(String purpose, String identifier, int maxRequests, Duration window) {
        Long count = increment(purpose, identifier, window);
        boolean allowed = count != null && count <= maxRequests;

        if (!allowed) {
            log.warn("Rate limit exceeded for {}:{} ({} requests in window)",
                    purpose, identifier, count);
        }

        return allowed;
    }

    /**
     * Atomically increment the counter and set its TTL if this is the first hit.
     */
    private Long increment(String purpose, String identifier, Duration window) {
        String key = KEY_PREFIX + purpose + ":" + identifier;
        Long count = redisTemplate.opsForValue().increment(key);

        // Set TTL only when the key is new (count == 1)
        if (count != null && count == 1) {
            redisTemplate.expire(key, window);
        }

        return count;
    }
}
