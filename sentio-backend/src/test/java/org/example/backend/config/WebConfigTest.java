package org.example.backend.config;

import org.example.backend.interceptor.RateLimitInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebConfig Unit Tests")
class WebConfigTest {

    @Mock
    private RateLimitInterceptor rateLimitInterceptor;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration registration;

    @Test
    @DisplayName("addInterceptors registers RateLimitInterceptor for /api/** paths")
    void addInterceptors_registersRateLimitInterceptorForApiPaths() {
        when(registry.addInterceptor(rateLimitInterceptor)).thenReturn(registration);
        when(registration.addPathPatterns(anyString())).thenReturn(registration);
        when(registration.excludePathPatterns(anyString(), anyString(), anyString())).thenReturn(registration);

        WebConfig config = new WebConfig(rateLimitInterceptor);
        config.addInterceptors(registry);

        verify(registry).addInterceptor(rateLimitInterceptor);
        verify(registration).addPathPatterns("/api/**");
        verify(registration).excludePathPatterns("/api/internal/**", "/swagger-ui/**", "/v3/api-docs/**");
    }
}
