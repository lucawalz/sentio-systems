package dev.syslabs.sentio.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter service.
 * For production scale, use Redis-based rate limiting.
 */
@Service
@Slf4j
public class RateLimitService {

    // Max requests per window
    private static final int MAX_PAIRING_REQUESTS = 10;
    // Window size in milliseconds (1 minute)
    private static final long WINDOW_SIZE_MS = 60_000;

    // Track requests per IP: IP -> (count, windowStart)
    private final Map<String, RateLimitBucket> pairingLimits = new ConcurrentHashMap<>();

    /**
     * Check if pairing request is allowed for this IP.
     * Limit: 10 requests per minute per IP.
     * 
     * @param clientIp The client IP address
     * @return true if request is allowed, false if rate limited
     */
    public boolean allowPairingRequest(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) {
            return true; // Don't block if we can't identify the client
        }

        long now = System.currentTimeMillis();
        RateLimitBucket bucket = pairingLimits.compute(clientIp, (ip, existing) -> {
            if (existing == null || existing.windowStart + WINDOW_SIZE_MS < now) {
                // New window
                return new RateLimitBucket(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        boolean allowed = bucket.count.get() <= MAX_PAIRING_REQUESTS;

        if (!allowed) {
            log.warn("Rate limit exceeded for pairing from IP: {} ({} requests in window)",
                    clientIp, bucket.count.get());
        }

        return allowed;
    }

    /**
     * Clean up old entries (call periodically)
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        pairingLimits.entrySet().removeIf(e -> e.getValue().windowStart + WINDOW_SIZE_MS * 2 < now);
    }

    private static class RateLimitBucket {
        final long windowStart;
        final AtomicInteger count;

        RateLimitBucket(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
