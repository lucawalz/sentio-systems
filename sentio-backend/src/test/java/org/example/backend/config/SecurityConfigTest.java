package org.example.backend.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityConfig Unit Tests")
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    // --- Helper ---

    /**
     * Sets the private {@code allowedOriginsRaw} field that is normally
     * populated by {@code @Value}.
     */
    private void setAllowedOrigins(String origins) throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("allowedOriginsRaw");
        field.setAccessible(true);
        field.set(securityConfig, origins);
    }

    // ------------------------------------------------------------------
    // BearerTokenResolver tests (existing)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("BearerTokenResolver")
    class BearerTokenResolverTests {

        @Test
        @DisplayName("should resolve from header first")
        void testResolver_FromHeader() {
            BearerTokenResolver resolver = securityConfig.bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer header-token");

            // Also add cookie to make sure header takes precedence
            request.setCookies(new Cookie("access_token", "cookie-token"));

            String token = resolver.resolve(request);
            assertThat(token).isEqualTo("header-token");
        }

        @Test
        @DisplayName("should resolve from cookie if header is absent")
        void testResolver_FromCookie() {
            BearerTokenResolver resolver = securityConfig.bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();

            request.setCookies(new Cookie("access_token", "cookie-token"), new Cookie("other_cookie", "value"));

            String token = resolver.resolve(request);
            assertThat(token).isEqualTo("cookie-token");
        }

        @Test
        @DisplayName("should return null if both are absent")
        void testResolver_FromNeither() {
            BearerTokenResolver resolver = securityConfig.bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();

            request.setCookies(new Cookie("other_cookie", "value"));

            String token = resolver.resolve(request);
            assertThat(token).isNull();
        }

        @Test
        @DisplayName("should return null if no cookies at all")
        void testResolver_NoCookies() {
            BearerTokenResolver resolver = securityConfig.bearerTokenResolver();
            MockHttpServletRequest request = new MockHttpServletRequest();

            String token = resolver.resolve(request);
            assertThat(token).isNull();
        }
    }

    // ------------------------------------------------------------------
    // CORS configuration tests (REQ-042)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("CORS Configuration")
    class CorsConfigurationTests {

        @BeforeEach
        void setUp() throws Exception {
            setAllowedOrigins("http://localhost:5173,http://localhost:3000,https://sentio.syslabs.dev");
        }

        @Test
        @DisplayName("should return correct CORS headers for an allowed origin")
        void allowedOriginGetsCorsHeaders() {
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
            request.addHeader("Origin", "http://localhost:5173");

            CorsConfiguration config = source.getCorsConfiguration(request);

            assertThat(config).isNotNull();
            assertThat(config.getAllowedOrigins())
                    .contains("http://localhost:5173", "http://localhost:3000", "https://sentio.syslabs.dev");
        }

        @Test
        @DisplayName("should not include a disallowed origin in allowed origins list")
        void disallowedOriginIsRejected() {
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
            request.addHeader("Origin", "http://evil.example.com");

            CorsConfiguration config = source.getCorsConfiguration(request);

            assertThat(config).isNotNull();
            assertThat(config.getAllowedOrigins()).doesNotContain("http://evil.example.com");

            // Verify that checkOrigin returns null for the disallowed origin
            String checkedOrigin = config.checkOrigin("http://evil.example.com");
            assertThat(checkedOrigin).isNull();
        }

        @Test
        @DisplayName("should include PATCH in allowed HTTP methods")
        void patchMethodIsAllowed() {
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/test");

            CorsConfiguration config = source.getCorsConfiguration(request);

            assertThat(config).isNotNull();
            assertThat(config.getAllowedMethods())
                    .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        }

        @Test
        @DisplayName("should use explicit allowed headers instead of wildcard")
        void allowedHeadersAreExplicit() {
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

            CorsConfiguration config = source.getCorsConfiguration(request);

            assertThat(config).isNotNull();
            assertThat(config.getAllowedHeaders())
                    .contains("Authorization", "Content-Type", "Accept", "X-Requested-With", "Cache-Control")
                    .doesNotContain("*");
        }

        @Test
        @DisplayName("should set maxAge for preflight caching")
        void maxAgeIsSet() {
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

            CorsConfiguration config = source.getCorsConfiguration(request);

            assertThat(config).isNotNull();
            assertThat(config.getMaxAge()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("should allow credentials for cookie-based auth")
        void credentialsAreAllowed() {
            CorsConfigurationSource source = securityConfig.corsConfigurationSource();
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

            CorsConfiguration config = source.getCorsConfiguration(request);

            assertThat(config).isNotNull();
            assertThat(config.getAllowCredentials()).isTrue();
        }
    }
}
