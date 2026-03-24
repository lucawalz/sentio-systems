package dev.syslabs.sentio.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j circuit breakers and retry policies.
 * Provides graceful degradation when external services fail.
 *
 * Circuit Breakers:
 * - brightSky: For BrightSky weather API
 * - openMeteo: For Open-Meteo forecast API
 * - n8n: For n8n workflow triggers
 * - aiClassifier: For AI classification service
 * - keycloak: For Keycloak authentication
 */
@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Default config for weather APIs
        CircuitBreakerConfig weatherApiConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit when 50% of calls fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Evaluate last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls to evaluate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 test calls in half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        // Stricter config for critical services like Keycloak
        CircuitBreakerConfig authConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(30) // Open faster for auth failures
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .build();

        // Create registry with configurations
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(weatherApiConfig);
        registry.addConfiguration("weatherApi", weatherApiConfig);
        registry.addConfiguration("auth", authConfig);
        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(java.io.IOException.class,
                        org.springframework.web.client.ResourceAccessException.class)
                .build();

        return RetryRegistry.of(retryConfig);
    }
}
