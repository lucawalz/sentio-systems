package org.example.backend.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    // --- JWT role extraction ---

    @Test
    @DisplayName("JWT with valid realm_access roles maps them to ROLE_ authorities")
    void jwtAuthenticationConverter_withValidRoles_extractsAuthorities() throws Exception {
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("admin", "user")));
        when(jwt.getSubject()).thenReturn("test-user");
        when(jwt.getHeaders()).thenReturn(Map.of("alg", "RS256"));
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(jwt.getTokenValue()).thenReturn("dummy-token");
        when(jwt.getClaims()).thenReturn(Map.of("sub", "test-user", "realm_access", Map.of("roles", List.of("admin", "user"))));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        Collection<GrantedAuthority> authorities = token.getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("JWT with missing realm_access claim returns empty authorities")
    void jwtAuthenticationConverter_withNullRealmAccess_returnsEmptyAuthorities() throws Exception {
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access")).thenReturn(null);
        when(jwt.getSubject()).thenReturn("test-user");
        when(jwt.getHeaders()).thenReturn(Map.of("alg", "RS256"));
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(jwt.getTokenValue()).thenReturn("dummy-token");
        when(jwt.getClaims()).thenReturn(Map.of("sub", "test-user"));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("JWT with realm_access but no roles list returns empty authorities")
    void jwtAuthenticationConverter_withMissingRolesList_returnsEmptyAuthorities() throws Exception {
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter = securityConfig.jwtAuthenticationConverter();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("other_key", "value"));
        when(jwt.getSubject()).thenReturn("test-user");
        when(jwt.getHeaders()).thenReturn(Map.of("alg", "RS256"));
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        when(jwt.getTokenValue()).thenReturn("dummy-token");
        when(jwt.getClaims()).thenReturn(Map.of("sub", "test-user", "realm_access", Map.of("other_key", "value")));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }
}
