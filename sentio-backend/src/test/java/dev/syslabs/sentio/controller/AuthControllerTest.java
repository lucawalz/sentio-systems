package dev.syslabs.sentio.controller;

import dev.syslabs.sentio.dto.AuthDTOs;
import dev.syslabs.sentio.service.AuthService;
import dev.syslabs.sentio.service.EmailVerificationService;
import dev.syslabs.sentio.service.PasswordResetService;
import dev.syslabs.sentio.service.impl.CookieAuthService;
import org.junit.jupiter.api.Test;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.AfterEach;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
})
@Import(AuthControllerTest.TestBeans.class)
@org.springframework.test.context.TestPropertySource(properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class AuthControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        AuthService authService;
        @Autowired
        CookieAuthService cookieAuthService;
        @Autowired
        PasswordResetService passwordResetService;
        @Autowired
        EmailVerificationService emailVerificationService;

        @TestConfiguration
        static class TestBeans {
                @Bean
                AuthService authService() {
                        return mock(AuthService.class);
                }

                @Bean
                CookieAuthService cookieAuthService() {
                        return mock(CookieAuthService.class);
                }

                @Bean
                PasswordResetService passwordResetService() {
                        return mock(PasswordResetService.class);
                }

                @Bean
                EmailVerificationService emailVerificationService() {
                        return mock(EmailVerificationService.class);
                }
        }

        @AfterEach
        void resetMocks() {
                reset(authService, cookieAuthService, passwordResetService, emailVerificationService);
        }

        @Test
        void register_returns201_andCallsService() throws Exception {
                doNothing().when(authService).register(any(AuthDTOs.RegisterRequest.class));

                mockMvc.perform(post("/api/auth/register")
                                .contentType("application/json")
                                .content("""
                                                {"username":"lilly","password":"Password1!","email":"lilly@test.de","firstName":"Lilly","lastName":"Test"}
                                                """))
                                .andExpect(status().isCreated());

                verify(authService, times(1)).register(any(AuthDTOs.RegisterRequest.class));
                verifyNoMoreInteractions(authService);
                verifyNoInteractions(cookieAuthService);
        }

        @Test
        void login_setsCookies_andReturns200() throws Exception {
                AuthDTOs.TokenResponse tokens = new AuthDTOs.TokenResponse();
                tokens.setAccessToken("access-123");
                tokens.setRefreshToken("refresh-456");
                tokens.setExpiresIn(3600L);

                when(authService.login(eq("lilly"), eq("Password1!"))).thenReturn(tokens);

                ResponseCookie access = ResponseCookie.from("access_token", "access-123")
                                .httpOnly(true).path("/").build();
                ResponseCookie refresh = ResponseCookie.from("refresh_token", "refresh-456")
                                .httpOnly(true).path("/").build();

                when(cookieAuthService.createAccessTokenCookie("access-123", 3600L)).thenReturn(access);
                when(cookieAuthService.createRefreshTokenCookie("refresh-456")).thenReturn(refresh);

                mockMvc.perform(post("/api/auth/login")
                                .contentType("application/json")
                                .content("""
                                                {"username":"lilly","password":"Password1!"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, access.toString(),
                                                refresh.toString()));

                verify(authService).login("lilly", "Password1!");
                verify(cookieAuthService).createAccessTokenCookie("access-123", 3600L);
                verify(cookieAuthService).createRefreshTokenCookie("refresh-456");
                verifyNoMoreInteractions(authService, cookieAuthService);
        }

        @Test
        void refresh_withoutCookie_returns401() throws Exception {
                mockMvc.perform(post("/api/auth/refresh"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(authService, cookieAuthService);
        }

        @Test
        void refresh_withCookie_setsNewCookies_andReturns200() throws Exception {
                AuthDTOs.TokenResponse tokens = new AuthDTOs.TokenResponse();
                tokens.setAccessToken("new-access");
                tokens.setRefreshToken("new-refresh");
                tokens.setExpiresIn(1800L);

                when(authService.refreshToken("good-refresh")).thenReturn(tokens);

                ResponseCookie access = ResponseCookie.from("access_token", "new-access")
                                .httpOnly(true).path("/").build();
                ResponseCookie refresh = ResponseCookie.from("refresh_token", "new-refresh")
                                .httpOnly(true).path("/").build();

                when(cookieAuthService.createAccessTokenCookie("new-access", 1800L)).thenReturn(access);
                when(cookieAuthService.createRefreshTokenCookie("new-refresh")).thenReturn(refresh);

                mockMvc.perform(post("/api/auth/refresh")
                                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "good-refresh")))
                                .andExpect(status().isOk())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, access.toString(),
                                                refresh.toString()));

                verify(authService).refreshToken("good-refresh");
                verify(cookieAuthService).createAccessTokenCookie("new-access", 1800L);
                verify(cookieAuthService).createRefreshTokenCookie("new-refresh");
                verifyNoMoreInteractions(authService, cookieAuthService);
        }

        @Test
        void refresh_whenServiceThrows_clearsCookies_andReturns401() throws Exception {
                when(authService.refreshToken("bad-refresh"))
                                .thenThrow(new RuntimeException("invalid_grant"));

                ResponseCookie c1 = ResponseCookie.from("access_token", "")
                                .maxAge(0).path("/").build();
                ResponseCookie c2 = ResponseCookie.from("refresh_token", "")
                                .maxAge(0).path("/").build();
                when(cookieAuthService.createLogoutCookies()).thenReturn(new ResponseCookie[] { c1, c2 });

                mockMvc.perform(post("/api/auth/refresh")
                                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "bad-refresh")))
                                .andExpect(status().isUnauthorized())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(allOf(containsString("access_token="),
                                                                containsString("Max-Age=0"),
                                                                containsString("Path=/")))))
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(allOf(containsString("refresh_token="),
                                                                containsString("Max-Age=0"),
                                                                containsString("Path=/")))));

                verify(authService).refreshToken("bad-refresh");
                verify(cookieAuthService).createLogoutCookies();
                verifyNoMoreInteractions(authService, cookieAuthService);
        }

        @Test
        void me_withoutCookie_returns401() throws Exception {
                mockMvc.perform(get("/api/auth/me"))
                                .andExpect(status().isUnauthorized());

                verifyNoInteractions(authService, cookieAuthService);
        }

        @Test
        void getCurrentUser_returns401_whenServiceThrows() throws Exception {
                when(authService.getUserFromToken("bad-token"))
                                .thenThrow(new RuntimeException("invalid"));

                mockMvc.perform(get("/api/auth/me")
                                .cookie(new Cookie("access_token", "bad-token")))
                                .andExpect(status().isUnauthorized());

                verify(authService).getUserFromToken("bad-token");
        }

        @Test
        void me_whenServiceThrows_returns401() throws Exception {
                when(authService.getUserFromToken("bad-access"))
                                .thenThrow(new RuntimeException("bad token"));

                mockMvc.perform(get("/api/auth/me")
                                .cookie(new jakarta.servlet.http.Cookie("access_token", "bad-access")))
                                .andExpect(status().isUnauthorized());

                verify(authService).getUserFromToken("bad-access");
                verifyNoMoreInteractions(authService);
                verifyNoInteractions(cookieAuthService);
        }

        @Test
        void logout_withoutRefreshToken_stillClearsCookies_andReturns204() throws Exception {
                ResponseCookie c1 = ResponseCookie.from("access_token", "").maxAge(0).path("/").build();
                ResponseCookie c2 = ResponseCookie.from("refresh_token", "").maxAge(0).path("/").build();
                when(cookieAuthService.createLogoutCookies()).thenReturn(new ResponseCookie[] { c1, c2 });

                mockMvc.perform(post("/api/auth/logout"))
                                .andExpect(status().isNoContent())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(allOf(containsString("access_token="),
                                                                containsString("Max-Age=0"),
                                                                containsString("Path=/")))))
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(allOf(containsString("refresh_token="),
                                                                containsString("Max-Age=0"),
                                                                containsString("Path=/")))));

                verify(cookieAuthService).createLogoutCookies();
                verifyNoInteractions(authService);
                verifyNoMoreInteractions(cookieAuthService);
        }

        @Test
        void logout_withRefreshToken_callsService_andClearsCookies() throws Exception {
                doNothing().when(authService).logout("refresh-xyz");

                ResponseCookie c1 = ResponseCookie.from("access_token", "").maxAge(0).path("/").build();
                ResponseCookie c2 = ResponseCookie.from("refresh_token", "").maxAge(0).path("/").build();
                when(cookieAuthService.createLogoutCookies()).thenReturn(new ResponseCookie[] { c1, c2 });

                mockMvc.perform(post("/api/auth/logout")
                                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "refresh-xyz")))
                                .andExpect(status().isNoContent())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(allOf(containsString("access_token="),
                                                                containsString("Max-Age=0"),
                                                                containsString("Path=/")))))
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                                                hasItem(allOf(containsString("refresh_token="),
                                                                containsString("Max-Age=0"),
                                                                containsString("Path=/")))));

                verify(authService).logout("refresh-xyz");
                verify(cookieAuthService).createLogoutCookies();
                verifyNoMoreInteractions(authService, cookieAuthService);
        }

        @Test
        void deleteUser_returns204_andCallsService() throws Exception {
                doNothing().when(authService).deleteUser("abc");

                mockMvc.perform(delete("/api/auth/user/abc"))
                                .andExpect(status().isNoContent());

                verify(authService).deleteUser("abc");
                verifyNoMoreInteractions(authService);
                verifyNoInteractions(cookieAuthService);
        }

        @Test
        void forgotPassword_callsService_andReturns200() throws Exception {
                when(authService.userExistsByEmail(eq("test@test.com"))).thenReturn(true);
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType("application/json")
                                .content("{\"email\":\"test@test.com\"}"))
                                .andExpect(status().isOk());
                verify(authService).userExistsByEmail("test@test.com");
                verify(passwordResetService).createResetToken("test@test.com");
        }

        @Test
        void forgotPassword_whenUserDoesNotExist_returns200() throws Exception {
                when(authService.userExistsByEmail(eq("test@test.com"))).thenReturn(false);
                mockMvc.perform(post("/api/auth/forgot-password")
                                .contentType("application/json")
                                .content("{\"email\":\"test@test.com\"}"))
                                .andExpect(status().isOk());
                verify(authService).userExistsByEmail("test@test.com");
                verifyNoInteractions(passwordResetService);
        }

        @Test
        void validateResetToken_validToken_returns200() throws Exception {
                when(passwordResetService.validateToken(eq("valid-token"))).thenReturn("user@example.com");
                mockMvc.perform(get("/api/auth/validate-reset-token").param("token", "valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true))
                                .andExpect(jsonPath("$.email").value("us***@example.com"));
        }

        @Test
        void validateResetToken_validTokenShortEmail_returns200() throws Exception {
                when(passwordResetService.validateToken(eq("valid-token"))).thenReturn("a@b.com");
                mockMvc.perform(get("/api/auth/validate-reset-token").param("token", "valid-token"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true))
                                .andExpect(jsonPath("$.email").value("***@b.com"));
        }

        @Test
        void validateResetToken_invalidToken_returns400() throws Exception {
                when(passwordResetService.validateToken(eq("invalid-token"))).thenReturn(null);
                mockMvc.perform(get("/api/auth/validate-reset-token").param("token", "invalid-token"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.valid").value(false));
        }

        @Test
        void resetPassword_invalidToken_returns400() throws Exception {
                when(passwordResetService.validateToken(eq("invalid-token"))).thenReturn(null);
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType("application/json")
                                .content("{\"token\":\"invalid-token\",\"password\":\"NewPass1!\",\"confirmPassword\":\"NewPass1!\"}"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void resetPassword_shortPassword_returns400() throws Exception {
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType("application/json")
                                .content("{\"token\":\"valid-token\",\"password\":\"short\",\"confirmPassword\":\"short\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void resetPassword_mismatchedPasswords_returns400() throws Exception {
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType("application/json")
                                .content("{\"token\":\"valid-token\",\"password\":\"NewPass1!\",\"confirmPassword\":\"NewPass2!\"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void resetPassword_validRequest_returns200() throws Exception {
                when(passwordResetService.validateToken(eq("valid-token"))).thenReturn("user@example.com");
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType("application/json")
                                .content("{\"token\":\"valid-token\",\"password\":\"NewPass1!\",\"confirmPassword\":\"NewPass1!\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
                verify(authService).updatePassword("user@example.com", "NewPass1!");
                verify(passwordResetService).invalidateToken("valid-token");
        }

        @Test
        void resetPassword_serviceThrows_returns500() throws Exception {
                when(passwordResetService.validateToken(eq("valid-token"))).thenReturn("user@example.com");
                doThrow(new RuntimeException("DB error")).when(authService).updatePassword(eq("user@example.com"),
                                eq("NewPass1!"));
                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType("application/json")
                                .content("{\"token\":\"valid-token\",\"password\":\"NewPass1!\",\"confirmPassword\":\"NewPass1!\"}"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void verifyEmail_invalidToken_returns400() throws Exception {
                when(emailVerificationService.validateToken(eq("invalid"))).thenReturn(null);
                mockMvc.perform(get("/api/auth/verify-email").param("token", "invalid"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void verifyEmail_validToken_returns200() throws Exception {
                when(emailVerificationService.validateToken(eq("valid"))).thenReturn("user@example.com");
                mockMvc.perform(get("/api/auth/verify-email").param("token", "valid"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
                verify(authService).markEmailVerified("user@example.com");
                verify(emailVerificationService).invalidateToken("valid");
        }

        @Test
        void verifyEmail_serviceThrows_returns500() throws Exception {
                when(emailVerificationService.validateToken(eq("valid"))).thenReturn("user@example.com");
                doThrow(new RuntimeException("DB error")).when(authService).markEmailVerified(eq("user@example.com"));
                mockMvc.perform(get("/api/auth/verify-email").param("token", "valid"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        void resendVerification_userExists_returns200() throws Exception {
                when(authService.userExistsByEmail(eq("user@example.com"))).thenReturn(true);
                mockMvc.perform(post("/api/auth/resend-verification")
                                .contentType("application/json")
                                .content("{\"email\":\"user@example.com\"}"))
                                .andExpect(status().isOk());
                verify(emailVerificationService).createVerificationToken("user@example.com");
        }

        @Test
        void resendVerification_userDoesNotExist_returns200() throws Exception {
                when(authService.userExistsByEmail(eq("user@example.com"))).thenReturn(false);
                mockMvc.perform(post("/api/auth/resend-verification")
                                .contentType("application/json")
                                .content("{\"email\":\"user@example.com\"}"))
                                .andExpect(status().isOk());
                verifyNoInteractions(emailVerificationService);
        }
}