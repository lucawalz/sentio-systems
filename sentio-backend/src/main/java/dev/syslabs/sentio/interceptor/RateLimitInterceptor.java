package dev.syslabs.sentio.interceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Interceptor for API rate limiting using Bucket4j with Redis backend.
 * Applies different rate limits based on endpoint type and user authentication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ProxyManager<String> proxyManager;

    @Value("${rate-limit.auth.capacity:5}")
    private int authCapacity;

    @Value("${rate-limit.auth.refill-tokens:5}")
    private int authRefillTokens;

    @Value("${rate-limit.auth.refill-period-minutes:1}")
    private int authRefillMinutes;

    @Value("${rate-limit.api.capacity:100}")
    private int apiCapacity;

    @Value("${rate-limit.api.refill-tokens:100}")
    private int apiRefillTokens;

    @Value("${rate-limit.api.refill-period-minutes:1}")
    private int apiRefillMinutes;

    @Value("${rate-limit.public.capacity:10}")
    private int publicCapacity;

    @Value("${rate-limit.public.refill-tokens:10}")
    private int publicRefillTokens;

    @Value("${rate-limit.public.refill-period-minutes:1}")
    private int publicRefillMinutes;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        // Skip rate limiting for internal endpoints
        if (path.startsWith("/api/internal/") || path.startsWith("/swagger-") || path.startsWith("/v3/api-docs")) {
            return true;
        }

        // Determine rate limit type and key
        RateLimitType type = determineRateLimitType(path);
        String key = generateRateLimitKey(request, type);

        // Get or create bucket
        Bucket bucket = proxyManager.builder().build(key, getBucketConfiguration(type));

        // Try to consume 1 token
        if (bucket.tryConsume(1)) {
            return true;
        }

        // Rate limit exceeded
        log.warn("Rate limit exceeded for key: {} (type: {}, path: {})", key, type, path);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        try {
            response.getWriter().write(String.format(
                    "{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again later.\", \"type\": \"%s\"}",
                    type.name()
            ));
        } catch (Exception e) {
            log.error("Failed to write rate limit response", e);
        }
        return false;
    }

    /**
     * Determine the rate limit type based on the request path.
     */
    private RateLimitType determineRateLimitType(String path) {
        if (path.startsWith("/api/auth/")) {
            return RateLimitType.AUTH;
        } else if (path.startsWith("/api/devices/pair") || path.startsWith("/api/contact")) {
            return RateLimitType.PUBLIC;
        } else {
            return RateLimitType.API;
        }
    }

    /**
     * Generate a rate limit key based on user or IP address.
     */
    private String generateRateLimitKey(HttpServletRequest request, RateLimitType type) {
        // For authenticated API requests, use user ID
        if (type == RateLimitType.API) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                return "user:" + auth.getName();
            }
        }

        // For auth and public endpoints, use IP address
        String ip = getClientIp(request);
        return type.name().toLowerCase() + ":" + ip;
    }

    /**
     * Extract client IP address, handling proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Get bucket configuration based on rate limit type.
     */
    private Supplier<BucketConfiguration> getBucketConfiguration(RateLimitType type) {
        return () -> {
            Bandwidth bandwidth = switch (type) {
                case AUTH -> Bandwidth.builder()
                        .capacity(authCapacity)
                        .refillGreedy(authRefillTokens, Duration.ofMinutes(authRefillMinutes))
                        .build();
                case PUBLIC -> Bandwidth.builder()
                        .capacity(publicCapacity)
                        .refillGreedy(publicRefillTokens, Duration.ofMinutes(publicRefillMinutes))
                        .build();
                case API -> Bandwidth.builder()
                        .capacity(apiCapacity)
                        .refillGreedy(apiRefillTokens, Duration.ofMinutes(apiRefillMinutes))
                        .build();
            };
            return BucketConfiguration.builder().addLimit(bandwidth).build();
        };
    }

    private enum RateLimitType {
        AUTH,    // Authentication endpoints
        PUBLIC,  // Public endpoints (contact, device pairing)
        API      // General authenticated API endpoints
    }
}
