package org.example.backend.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityConfig Unit Tests")
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("BearerTokenResolver should resolve from header first")
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
    @DisplayName("BearerTokenResolver should resolve from cookie if header is absent")
    void testResolver_FromCookie() {
        BearerTokenResolver resolver = securityConfig.bearerTokenResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setCookies(new Cookie("access_token", "cookie-token"), new Cookie("other_cookie", "value"));

        String token = resolver.resolve(request);
        assertThat(token).isEqualTo("cookie-token");
    }

    @Test
    @DisplayName("BearerTokenResolver should return null if both are absent")
    void testResolver_FromNeither() {
        BearerTokenResolver resolver = securityConfig.bearerTokenResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setCookies(new Cookie("other_cookie", "value"));

        String token = resolver.resolve(request);
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("BearerTokenResolver should return null if no cookies at all")
    void testResolver_NoCookies() {
        BearerTokenResolver resolver = securityConfig.bearerTokenResolver();
        MockHttpServletRequest request = new MockHttpServletRequest();

        String token = resolver.resolve(request);
        assertThat(token).isNull();
    }
}
