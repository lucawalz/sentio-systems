package org.example.backend.dto;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthDTOsTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<AuthDTOs> constructor = AuthDTOs.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InvocationTargetException e) {
            fail("Constructor threw exception: " + e.getCause());
        }
    }

    @Test
    void testLoginRequest() {
        AuthDTOs.LoginRequest loginReq = new AuthDTOs.LoginRequest();
        loginReq.setUsername("testuser");
        loginReq.setPassword("testpass");

        assertEquals("testuser", loginReq.getUsername());
        assertEquals("testpass", loginReq.getPassword());

        AuthDTOs.LoginRequest loginReq2 = new AuthDTOs.LoginRequest("testuser", "testpass");
        assertEquals(loginReq, loginReq2);
        assertEquals(loginReq.hashCode(), loginReq2.hashCode());
        assertNotNull(loginReq.toString());
    }

    @Test
    void testRegisterRequest() {
        AuthDTOs.RegisterRequest regReq = new AuthDTOs.RegisterRequest();
        regReq.setUsername("testuser");
        regReq.setPassword("testpass");
        regReq.setEmail("test@ex.com");
        regReq.setFirstName("First");
        regReq.setLastName("Last");

        assertEquals("testuser", regReq.getUsername());
        assertEquals("testpass", regReq.getPassword());
        assertEquals("test@ex.com", regReq.getEmail());
        assertEquals("First", regReq.getFirstName());
        assertEquals("Last", regReq.getLastName());

        AuthDTOs.RegisterRequest regReq2 = new AuthDTOs.RegisterRequest("testuser", "testpass", "test@ex.com", "First",
                "Last");
        assertEquals(regReq, regReq2);
        assertEquals(regReq.hashCode(), regReq2.hashCode());
        assertNotNull(regReq.toString());
    }

    @Test
    void testTokenResponse() {
        AuthDTOs.TokenResponse resp = new AuthDTOs.TokenResponse();
        resp.setAccessToken("access");
        resp.setRefreshToken("refresh");
        resp.setTokenType("Bearer");
        resp.setExpiresIn(3600L);

        assertEquals("access", resp.getAccessToken());
        assertEquals("refresh", resp.getRefreshToken());
        assertEquals("Bearer", resp.getTokenType());
        assertEquals(3600L, resp.getExpiresIn());

        AuthDTOs.TokenResponse resp2 = new AuthDTOs.TokenResponse("access", "refresh", "Bearer", 3600L);
        assertEquals(resp, resp2);
        assertEquals(resp.hashCode(), resp2.hashCode());
        assertNotNull(resp.toString());
    }

    @Test
    void testUserInfo() {
        AuthDTOs.UserInfo info = new AuthDTOs.UserInfo();
        info.setId("uuid-123");
        info.setUsername("user");
        info.setEmail("user@ex.com");
        info.setRoles(List.of("ROLE_USER"));

        assertEquals("uuid-123", info.getId());
        assertEquals("user", info.getUsername());
        assertEquals("user@ex.com", info.getEmail());
        assertEquals(List.of("ROLE_USER"), info.getRoles());

        AuthDTOs.UserInfo info2 = new AuthDTOs.UserInfo("uuid-123", "user", "user@ex.com", List.of("ROLE_USER"));
        assertEquals(info, info2);
        assertEquals(info.hashCode(), info2.hashCode());
        assertNotNull(info.toString());
    }

    @Test
    void testRecords() {
        AuthDTOs.RegisterResponse rr = new AuthDTOs.RegisterResponse(true, "msg");
        assertTrue(rr.success());
        assertEquals("msg", rr.message());

        AuthDTOs.ForgotPasswordRequest fpr = new AuthDTOs.ForgotPasswordRequest("test@ex.com");
        assertEquals("test@ex.com", fpr.email());

        AuthDTOs.TokenValidationResponse tvr = new AuthDTOs.TokenValidationResponse(true, "test@ex.com", null);
        assertTrue(tvr.valid());
        assertEquals("test@ex.com", tvr.email());
        assertNull(tvr.error());

        AuthDTOs.ResetPasswordRequest rpr = new AuthDTOs.ResetPasswordRequest("token", "pass", "pass");
        assertEquals("token", rpr.token());
        assertEquals("pass", rpr.password());
        assertEquals("pass", rpr.confirmPassword());

        AuthDTOs.MessageResponse mr = new AuthDTOs.MessageResponse(true, "Success");
        assertTrue(mr.success());
        assertEquals("Success", mr.message());

        AuthDTOs.ResendVerificationRequest rvr = new AuthDTOs.ResendVerificationRequest("test@ex.com");
        assertEquals("test@ex.com", rvr.email());
    }
}
