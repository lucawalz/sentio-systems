package org.example.backend.interceptor;

import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor Unit Tests")
class RateLimitInterceptorTest {

    @Mock
    private ProxyManager<String> proxyManager;

    @SuppressWarnings("rawtypes")
    @Mock
    private RemoteBucketBuilder remoteBucketBuilder;

    @Mock
    private BucketProxy bucketProxy;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        interceptor = new RateLimitInterceptor(proxyManager);

        ReflectionTestUtils.setField(interceptor, "authCapacity", 5);
        ReflectionTestUtils.setField(interceptor, "authRefillTokens", 5);
        ReflectionTestUtils.setField(interceptor, "authRefillMinutes", 1);
        ReflectionTestUtils.setField(interceptor, "apiCapacity", 100);
        ReflectionTestUtils.setField(interceptor, "apiRefillTokens", 100);
        ReflectionTestUtils.setField(interceptor, "apiRefillMinutes", 1);
        ReflectionTestUtils.setField(interceptor, "publicCapacity", 10);
        ReflectionTestUtils.setField(interceptor, "publicRefillTokens", 10);
        ReflectionTestUtils.setField(interceptor, "publicRefillMinutes", 1);

        lenient().when(proxyManager.builder()).thenReturn(remoteBucketBuilder);
        lenient().when(remoteBucketBuilder.build(anyString(), any(Supplier.class))).thenReturn(bucketProxy);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Skipped paths (no rate limiting) ---

    @Test
    @DisplayName("Internal API paths skip rate limiting")
    void preHandle_internalPath_skipsRateLimiting() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/internal/mqtt/auth");

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verifyNoInteractions(proxyManager);
    }

    @Test
    @DisplayName("Swagger UI paths skip rate limiting")
    void preHandle_swaggerPath_skipsRateLimiting() throws Exception {
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verifyNoInteractions(proxyManager);
    }

    @Test
    @DisplayName("API docs paths skip rate limiting")
    void preHandle_apiDocsPath_skipsRateLimiting() throws Exception {
        when(request.getRequestURI()).thenReturn("/v3/api-docs/swagger-config");

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verifyNoInteractions(proxyManager);
    }

    // --- AUTH endpoints ---

    @Test
    @DisplayName("Auth path within limit allows request")
    @SuppressWarnings("unchecked")
    void preHandle_authPath_withinLimit_allowsRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("auth:10.0.0.1"), any(Supplier.class));
    }

    @Test
    @DisplayName("Auth path over limit returns 429 with error body")
    void preHandle_authPath_exceededLimit_blocksRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(bucketProxy.tryConsume(1)).thenReturn(false);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        assertThat(interceptor.preHandle(request, response, null)).isFalse();
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");
        assertThat(sw.toString()).contains("Too Many Requests").contains("AUTH");
    }

    // --- PUBLIC endpoints ---

    @Test
    @DisplayName("Device pair path uses PUBLIC bucket keyed by IP")
    @SuppressWarnings("unchecked")
    void preHandle_devicesPairPath_usesPublicBucket() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/devices/pair");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("public:10.0.0.2"), any(Supplier.class));
    }

    @Test
    @DisplayName("Contact path uses PUBLIC bucket keyed by IP")
    @SuppressWarnings("unchecked")
    void preHandle_contactPath_usesPublicBucket() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/contact/submit");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("public:10.0.0.3"), any(Supplier.class));
    }

    // --- API endpoints ---

    @Test
    @DisplayName("Authenticated API request uses user ID as bucket key")
    @SuppressWarnings("unchecked")
    void preHandle_apiPath_withAuthenticatedUser_usesUserKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/devices");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        Authentication auth = new UsernamePasswordAuthenticationToken("user-abc", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("user:user-abc"), any(Supplier.class));
    }

    @Test
    @DisplayName("API path with anonymous principal falls back to IP key")
    @SuppressWarnings("unchecked")
    void preHandle_apiPath_withAnonymousPrincipal_usesIpKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/devices");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("172.16.0.5");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("api:172.16.0.5"), any(Supplier.class));
    }

    @Test
    @DisplayName("API path with no authentication falls back to IP key")
    @SuppressWarnings("unchecked")
    void preHandle_apiPath_withNoAuthentication_usesIpKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/devices");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("172.16.0.6");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        // SecurityContextHolder is empty (cleared in setUp)
        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("api:172.16.0.6"), any(Supplier.class));
    }

    // --- X-Forwarded-For header handling ---

    @Test
    @DisplayName("X-Forwarded-For single IP is used as rate limit key")
    @SuppressWarnings("unchecked")
    void preHandle_withXForwardedFor_usesForwardedIp() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("auth:203.0.113.1"), any(Supplier.class));
    }

    @Test
    @DisplayName("X-Forwarded-For with multiple IPs uses first (client) IP")
    @SuppressWarnings("unchecked")
    void preHandle_withMultipleXForwardedFor_usesFirstIp() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1, 192.168.1.1");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("auth:203.0.113.1"), any(Supplier.class));
    }

    @Test
    @DisplayName("Empty X-Forwarded-For falls back to RemoteAddr")
    @SuppressWarnings("unchecked")
    void preHandle_withEmptyXForwardedFor_usesRemoteAddr() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");
        when(bucketProxy.tryConsume(1)).thenReturn(true);

        assertThat(interceptor.preHandle(request, response, null)).isTrue();
        verify(remoteBucketBuilder).build(eq("auth:192.168.1.50"), any(Supplier.class));
    }

    // --- Error handling ---

    @Test
    @DisplayName("IOException writing rate-limit response is swallowed gracefully")
    void preHandle_rateLimitExceeded_writerThrows_handlesGracefully() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("1.2.3.4");
        when(bucketProxy.tryConsume(1)).thenReturn(false);
        when(response.getWriter()).thenThrow(new IOException("test error"));

        // Should not throw – exception is caught and logged
        assertThat(interceptor.preHandle(request, response, null)).isFalse();
        verify(response).setStatus(429);
    }
}
