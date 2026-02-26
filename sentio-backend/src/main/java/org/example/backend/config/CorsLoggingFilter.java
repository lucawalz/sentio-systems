package org.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that logs CORS-related information for debugging.
 * Runs before the Spring Security filter chain to capture origin headers
 * on cross-origin requests and warn about rejected origins.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorsLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String origin = request.getHeader("Origin");

        if (origin != null) {
            // Let the request proceed through the CORS filter first
            filterChain.doFilter(request, response);

            // After the response has been processed, check if CORS headers were set
            String allowedOrigin = response.getHeader("Access-Control-Allow-Origin");
            if (allowedOrigin == null) {
                log.warn("CORS rejected request from origin '{}' to {} {}",
                        origin, request.getMethod(), request.getRequestURI());
            } else {
                log.debug("CORS allowed request from origin '{}' to {} {}",
                        origin, request.getMethod(), request.getRequestURI());
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
