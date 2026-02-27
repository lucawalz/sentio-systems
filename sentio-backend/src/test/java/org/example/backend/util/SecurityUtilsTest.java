package org.example.backend.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityUtils")
class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUserId should return subject when JWT is present")
    void getCurrentUserId_withJwt_returnsSubject() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user-123");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String userId = SecurityUtils.getCurrentUserId();

        // Assert
        assertThat(userId).isEqualTo("user-123");
    }

    @Test
    @DisplayName("getCurrentUserId should return null when not authenticated")
    void getCurrentUserId_notAuthenticated_returnsNull() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String userId = SecurityUtils.getCurrentUserId();

        // Assert
        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId should return null when authentication is null")
    void getCurrentUserId_nullAuthentication_returnsNull() {
        // Arrange
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(context);

        // Act
        String userId = SecurityUtils.getCurrentUserId();

        // Assert
        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId should return null when principal is not JWT")
    void getCurrentUserId_notJwt_returnsNull() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("some-string-principal");
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String userId = SecurityUtils.getCurrentUserId();

        // Assert
        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("getCurrentUserEmail should return email claim when JWT is present")
    void getCurrentUserEmail_withJwt_returnsEmail() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("email")).thenReturn("user@example.com");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String email = SecurityUtils.getCurrentUserEmail();

        // Assert
        assertThat(email).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("getCurrentUserEmail should return null when not authenticated")
    void getCurrentUserEmail_notAuthenticated_returnsNull() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String email = SecurityUtils.getCurrentUserEmail();

        // Assert
        assertThat(email).isNull();
    }

    @Test
    @DisplayName("getCurrentUsername should return preferred_username claim when JWT is present")
    void getCurrentUsername_withJwt_returnsUsername() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("preferred_username")).thenReturn("testuser");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String username = SecurityUtils.getCurrentUsername();

        // Assert
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getCurrentUsername should return null when not authenticated")
    void getCurrentUsername_notAuthenticated_returnsNull() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String username = SecurityUtils.getCurrentUsername();

        // Assert
        assertThat(username).isNull();
    }

    @Test
    @DisplayName("getCurrentToken should return token value when JWT is present")
    void getCurrentToken_withJwt_returnsTokenValue() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(jwt);
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String token = SecurityUtils.getCurrentToken();

        // Assert
        assertThat(token).isEqualTo("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
    }

    @Test
    @DisplayName("getCurrentToken should return null when principal is not JWT")
    void getCurrentToken_notJwt_returnsNull() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("some-string-principal");
        when(auth.isAuthenticated()).thenReturn(true);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String token = SecurityUtils.getCurrentToken();

        // Assert
        assertThat(token).isNull();
    }

    @Test
    @DisplayName("getCurrentToken should return null when not authenticated")
    void getCurrentToken_notAuthenticated_returnsNull() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Act
        String token = SecurityUtils.getCurrentToken();

        // Assert
        assertThat(token).isNull();
    }
}
