package org.example.backend.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://sentio.syslabs.dev}")
    private String allowedOriginsRaw;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // CSRF disabled for now, relying on SameSite=Lax cookies
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/internal/mqtt/**").permitAll() // Called by Mosquitto auth plugin
                        .requestMatchers("/api/stream/auth").permitAll() // Called by MediaMTX auth webhook
                        .requestMatchers("/api/stream/ready").permitAll() // Stream ready notification
                        .requestMatchers("/api/stream/not-ready").permitAll() // Stream ended notification
                        .requestMatchers("/api/devices/pair").permitAll() // Device pairing (no auth required)
                        .requestMatchers("/api/contact/**").permitAll() // Contact form (public)
                        .requestMatchers("/ws/**").permitAll() // WebSocket handshake
                        .requestMatchers("/api/**").authenticated() // Require auth for other API endpoints
                        .requestMatchers("/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(org.springframework.security.config.Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        BearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return new BearerTokenResolver() {
            @Override
            public String resolve(HttpServletRequest request) {
                // First check header (default behavior)
                String token = defaultResolver.resolve(request);
                if (token != null) {
                    return token;
                }
                // Then check cookie
                if (request.getCookies() != null) {
                    for (Cookie cookie : request.getCookies()) {
                        if ("access_token".equals(cookie.getName())) {
                            return cookie.getValue();
                        }
                    }
                }
                return null;
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins from configuration / environment variable
        List<String> origins = Arrays.asList(allowedOriginsRaw.split("\\s*,\\s*"));
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "X-Requested-With", "Cache-Control"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cache preflight responses for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}