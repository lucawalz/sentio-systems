package org.example.backend.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://sentio.syslabs.dev}")
    private String allowedOrigins;

    @Value("${security.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${security.cors.allowed-headers:Authorization,Content-Type,X-Requested-With}")
    private String allowedHeaders;

    @Value("${security.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    "/api/auth/**",
                    "/api/internal/mqtt/**",
                    "/api/stream/**",
                    "/api/devices/pair",
                    "/api/contact/**",
                    "/ws/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .preload(true)
                    .maxAgeInSeconds(31536000))
                .permissionsPolicy(permissions -> permissions
                    .policy("geolocation=(), microphone=(), camera=()")))
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
        configuration.setAllowedOrigins(splitCsv(allowedOrigins));
        configuration.setAllowedMethods(splitCsv(allowedMethods));
        configuration.setAllowedHeaders(splitCsv(allowedHeaders));
        configuration.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> splitCsv(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }
}