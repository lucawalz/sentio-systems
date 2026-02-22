package org.example.backend.controller;

import org.example.backend.dto.AuthDTOs;
import org.example.backend.service.AuthService;
import org.example.backend.service.impl.CookieAuthService;
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
class AuthControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        AuthService authService;
        @Autowired
        CookieAuthService cookieAuthService;

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
                org.example.backend.service.PasswordResetService passwordResetService() {
                        return mock(org.example.backend.service.PasswordResetService.class);
                }

                @Bean
                org.example.backend.service.EmailVerificationService emailVerificationService() {
                        return mock(org.example.backend.service.EmailVerificationService.class);
                }
        }

        @AfterEach
        void resetMocks() {
                reset(authService, cookieAuthService);
        }

        @Test
        void register_returns201_andCallsService() throws Exception {
                doNothing().when(authService).register(any(AuthDTOs.RegisterRequest.class));

                mockMvc.perform(post("/api/auth/register")
                                .contentType("application/json")
                                .content("""
                                                {"username":"lilly","password":"pw","email":"lilly@test.de"}
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

                when(authService.login(eq("lilly"), eq("pw"))).thenReturn(tokens);

                ResponseCookie access = ResponseCookie.from("access_token", "access-123")
                                .httpOnly(true).path("/").build();
                ResponseCookie refresh = ResponseCookie.from("refresh_token", "refresh-456")
                                .httpOnly(true).path("/").build();

                when(cookieAuthService.createAccessTokenCookie("access-123", 3600L)).thenReturn(access);
                when(cookieAuthService.createRefreshTokenCookie("refresh-456")).thenReturn(refresh);

                mockMvc.perform(post("/api/auth/login")
                                .contentType("application/json")
                                .content("""
                                                {"username":"lilly","password":"pw"}
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, access.toString(),
                                                refresh.toString()));

                verify(authService).login("lilly", "pw");
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
}