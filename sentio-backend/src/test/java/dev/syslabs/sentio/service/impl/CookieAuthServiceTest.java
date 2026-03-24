package dev.syslabs.sentio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CookieAuthService}.
 * 
 * <p>
 * Following FIRST principles:
 * </p>
 * <ul>
 * <li><b>Fast</b> - No external dependencies or Spring context</li>
 * <li><b>Independent</b> - Each test creates fresh service instance</li>
 * <li><b>Repeatable</b> - No random or time-dependent behavior</li>
 * <li><b>Self-validating</b> - Clear assertions with meaningful messages</li>
 * <li><b>Timely</b> - Written alongside the service implementation</li>
 * </ul>
 */
class CookieAuthServiceTest {

    private CookieAuthService cookieAuthService;

    @BeforeEach
    void setUp() {
        cookieAuthService = new CookieAuthService();
        // Set default secureCookies to false for testing
        ReflectionTestUtils.setField(cookieAuthService, "secureCookies", false);
    }

    @Nested
    @DisplayName("createAccessTokenCookie")
    class CreateAccessTokenCookieTests {

        @Test
        @DisplayName("should create cookie with correct name and value")
        void shouldCreateCookieWithCorrectNameAndValue() {
            // Given
            String token = "test-access-token";
            long maxAgeSeconds = 3600L;

            // When
            ResponseCookie cookie = cookieAuthService.createAccessTokenCookie(token, maxAgeSeconds);

            // Then
            assertThat(cookie.getName()).isEqualTo("access_token");
            assertThat(cookie.getValue()).isEqualTo(token);
        }

        @Test
        @DisplayName("should create httpOnly cookie to prevent XSS")
        void shouldCreateHttpOnlyCookie() {
            // Given
            String token = "test-token";

            // When
            ResponseCookie cookie = cookieAuthService.createAccessTokenCookie(token, 3600L);

            // Then
            assertThat(cookie.isHttpOnly()).isTrue();
        }

        @Test
        @DisplayName("should set cookie path to root")
        void shouldSetCookiePathToRoot() {
            // Given
            String token = "test-token";

            // When
            ResponseCookie cookie = cookieAuthService.createAccessTokenCookie(token, 3600L);

            // Then
            assertThat(cookie.getPath()).isEqualTo("/");
        }

        @Test
        @DisplayName("should set correct maxAge from parameter")
        void shouldSetCorrectMaxAge() {
            // Given
            String token = "test-token";
            long expectedMaxAge = 7200L;

            // When
            ResponseCookie cookie = cookieAuthService.createAccessTokenCookie(token, expectedMaxAge);

            // Then
            assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(expectedMaxAge);
        }

        @Test
        @DisplayName("should set SameSite to Lax for CSRF protection")
        void shouldSetSameSiteToLax() {
            // Given
            String token = "test-token";

            // When
            ResponseCookie cookie = cookieAuthService.createAccessTokenCookie(token, 3600L);

            // Then
            assertThat(cookie.getSameSite()).isEqualTo("Lax");
        }

        @Test
        @DisplayName("should respect secureCookies configuration when false")
        void shouldRespectSecureCookiesConfigurationWhenFalse() {
            // Given
            ReflectionTestUtils.setField(cookieAuthService, "secureCookies", false);

            // When
            ResponseCookie cookie = cookieAuthService.createAccessTokenCookie("token", 3600L);

            // Then
            assertThat(cookie.isSecure()).isFalse();
        }

        @Test
        @DisplayName("should respect secureCookies configuration when true")
        void shouldRespectSecureCookiesConfigurationWhenTrue() {
            // Given
            ReflectionTestUtils.setField(cookieAuthService, "secureCookies", true);

            // When
            ResponseCookie cookie = cookieAuthService.createAccessTokenCookie("token", 3600L);

            // Then
            assertThat(cookie.isSecure()).isTrue();
        }
    }

    @Nested
    @DisplayName("createRefreshTokenCookie")
    class CreateRefreshTokenCookieTests {

        @Test
        @DisplayName("should create cookie with correct name and value")
        void shouldCreateCookieWithCorrectNameAndValue() {
            // Given
            String refreshToken = "test-refresh-token";

            // When
            ResponseCookie cookie = cookieAuthService.createRefreshTokenCookie(refreshToken);

            // Then
            assertThat(cookie.getName()).isEqualTo("refresh_token");
            assertThat(cookie.getValue()).isEqualTo(refreshToken);
        }

        @Test
        @DisplayName("should restrict path to /api/auth to minimize exposure")
        void shouldRestrictPathToAuthEndpoints() {
            // Given
            String refreshToken = "test-refresh-token";

            // When
            ResponseCookie cookie = cookieAuthService.createRefreshTokenCookie(refreshToken);

            // Then
            assertThat(cookie.getPath()).isEqualTo("/api/auth");
        }

        @Test
        @DisplayName("should set maxAge to 7 days")
        void shouldSetMaxAgeToSevenDays() {
            // Given
            String refreshToken = "test-refresh-token";
            long sevenDaysInSeconds = 7 * 24 * 60 * 60;

            // When
            ResponseCookie cookie = cookieAuthService.createRefreshTokenCookie(refreshToken);

            // Then
            assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(sevenDaysInSeconds);
        }

        @Test
        @DisplayName("should create httpOnly cookie")
        void shouldCreateHttpOnlyCookie() {
            // Given
            String refreshToken = "test-refresh-token";

            // When
            ResponseCookie cookie = cookieAuthService.createRefreshTokenCookie(refreshToken);

            // Then
            assertThat(cookie.isHttpOnly()).isTrue();
        }
    }

    @Nested
    @DisplayName("createLogoutCookie")
    class CreateLogoutCookieTests {

        @Test
        @DisplayName("should set maxAge to 0 for immediate deletion")
        void shouldSetMaxAgeToZeroForDeletion() {
            // Given
            String cookieName = "access_token";

            // When
            ResponseCookie cookie = cookieAuthService.createLogoutCookie(cookieName);

            // Then
            assertThat(cookie.getMaxAge().getSeconds()).isZero();
        }

        @Test
        @DisplayName("should set empty value for logout cookie")
        void shouldSetEmptyValue() {
            // Given
            String cookieName = "access_token";

            // When
            ResponseCookie cookie = cookieAuthService.createLogoutCookie(cookieName);

            // Then
            assertThat(cookie.getValue()).isEmpty();
        }

        @Test
        @DisplayName("should use root path for access_token cookie")
        void shouldUseRootPathForAccessToken() {
            // Given
            String cookieName = "access_token";

            // When
            ResponseCookie cookie = cookieAuthService.createLogoutCookie(cookieName);

            // Then
            assertThat(cookie.getPath()).isEqualTo("/");
        }

        @Test
        @DisplayName("should use /api/auth path for refresh_token cookie")
        void shouldUseAuthPathForRefreshToken() {
            // Given
            String cookieName = "refresh_token";

            // When
            ResponseCookie cookie = cookieAuthService.createLogoutCookie(cookieName);

            // Then
            assertThat(cookie.getPath()).isEqualTo("/api/auth");
        }
    }

    @Nested
    @DisplayName("createLogoutCookies")
    class CreateLogoutCookiesTests {

        @Test
        @DisplayName("should return array with two cookies")
        void shouldReturnArrayWithTwoCookies() {
            // Given - service is already set up

            // When
            ResponseCookie[] cookies = cookieAuthService.createLogoutCookies();

            // Then
            assertThat(cookies).hasSize(2);
        }

        @Test
        @DisplayName("should include access_token logout cookie")
        void shouldIncludeAccessTokenLogoutCookie() {
            // Given - service is already set up

            // When
            ResponseCookie[] cookies = cookieAuthService.createLogoutCookies();

            // Then
            assertThat(cookies)
                    .extracting(ResponseCookie::getName)
                    .contains("access_token");
        }

        @Test
        @DisplayName("should include refresh_token logout cookie")
        void shouldIncludeRefreshTokenLogoutCookie() {
            // Given - service is already set up

            // When
            ResponseCookie[] cookies = cookieAuthService.createLogoutCookies();

            // Then
            assertThat(cookies)
                    .extracting(ResponseCookie::getName)
                    .contains("refresh_token");
        }

        @Test
        @DisplayName("all logout cookies should have maxAge of 0")
        void allLogoutCookiesShouldHaveMaxAgeZero() {
            // Given - service is already set up

            // When
            ResponseCookie[] cookies = cookieAuthService.createLogoutCookies();

            // Then
            assertThat(cookies)
                    .allMatch(cookie -> cookie.getMaxAge().getSeconds() == 0);
        }
    }
}