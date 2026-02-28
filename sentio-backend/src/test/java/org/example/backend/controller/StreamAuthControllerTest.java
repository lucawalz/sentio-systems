package org.example.backend.controller;

import org.example.backend.service.DeviceService;
import org.example.backend.service.StreamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StreamAuthController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(StreamAuthControllerTest.TestBeans.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@DisplayName("StreamAuthController")
class StreamAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StreamService streamService;

    @Autowired
    private DeviceService deviceService;

    @TestConfiguration
    static class TestBeans {
        @Bean
        StreamService streamService() {
            return mock(StreamService.class);
        }

        @Bean
        DeviceService deviceService() {
            return mock(DeviceService.class);
        }
    }

    @AfterEach
    void resetMocks() {
        reset(streamService, deviceService);
    }

    // --- /api/stream/auth Tests ---

    @Test
    @DisplayName("authenticate should return 200 for valid publish action")
    void authenticate_withValidPublishAction_returns200() throws Exception {
        // Arrange
        when(streamService.validatePublishAuth("device-123", "device-token-abc", "192.168.1.1"))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123",
                                    "action": "publish",
                                    "query": "token=device-token-abc",
                                    "protocol": "rtmp",
                                    "ip": "192.168.1.1"
                                }
                                """))
                .andExpect(status().isOk());

        verify(streamService, times(1))
                .validatePublishAuth("device-123", "device-token-abc", "192.168.1.1");
    }

    @Test
    @DisplayName("authenticate should return 200 for valid read action")
    void authenticate_withValidReadAction_returns200() throws Exception {
        // Arrange
        when(streamService.validatePlaybackAuth("device-123", "keycloak-token-xyz"))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123",
                                    "action": "read",
                                    "query": "token=keycloak-token-xyz",
                                    "protocol": "hls"
                                }
                                """))
                .andExpect(status().isOk());

        verify(streamService, times(1))
                .validatePlaybackAuth("device-123", "keycloak-token-xyz");
    }

    @Test
    @DisplayName("authenticate should return 200 for valid playback action")
    void authenticate_withValidPlaybackAction_returns200() throws Exception {
        // Arrange
        when(streamService.validatePlaybackAuth("device-456", "keycloak-token-abc"))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-456",
                                    "action": "playback",
                                    "query": "token=keycloak-token-abc",
                                    "protocol": "hls"
                                }
                                """))
                .andExpect(status().isOk());

        verify(streamService, times(1))
                .validatePlaybackAuth("device-456", "keycloak-token-abc");
    }

    @Test
    @DisplayName("authenticate should return 403 when device token is invalid")
    void authenticate_withInvalidDeviceToken_returns403() throws Exception {
        // Arrange
        when(streamService.validatePublishAuth("device-123", "invalid-token", "192.168.1.1"))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123",
                                    "action": "publish",
                                    "query": "token=invalid-token",
                                    "ip": "192.168.1.1"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(streamService, times(1))
                .validatePublishAuth("device-123", "invalid-token", "192.168.1.1");
    }

    @Test
    @DisplayName("authenticate should return 403 when playback token is invalid")
    void authenticate_withInvalidPlaybackToken_returns403() throws Exception {
        // Arrange
        when(streamService.validatePlaybackAuth("device-123", "invalid-keycloak-token"))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123",
                                    "action": "read",
                                    "query": "token=invalid-keycloak-token"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(streamService, times(1))
                .validatePlaybackAuth("device-123", "invalid-keycloak-token");
    }

    @Test
    @DisplayName("authenticate should return 403 when path is invalid")
    void authenticate_withInvalidPath_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "invalid-path",
                                    "action": "publish",
                                    "query": "token=some-token"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(streamService, never()).validatePublishAuth(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("authenticate should return 403 when token is missing from query")
    void authenticate_withMissingToken_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123",
                                    "action": "publish",
                                    "query": ""
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(streamService, never()).validatePublishAuth(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("authenticate should return 403 for unknown action")
    void authenticate_withUnknownAction_returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123",
                                    "action": "unknown",
                                    "query": "token=some-token"
                                }
                                """))
                .andExpect(status().isForbidden());

        verify(streamService, never()).validatePublishAuth(anyString(), anyString(), anyString());
        verify(streamService, never()).validatePlaybackAuth(anyString(), anyString());
    }

    @Test
    @DisplayName("authenticate should extract token from query with multiple parameters")
    void authenticate_withMultipleQueryParams_extractsTokenCorrectly() throws Exception {
        // Arrange
        when(streamService.validatePlaybackAuth("device-123", "my-token"))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/stream/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123",
                                    "action": "read",
                                    "query": "foo=bar&token=my-token&baz=qux"
                                }
                                """))
                .andExpect(status().isOk());

        verify(streamService, times(1))
                .validatePlaybackAuth("device-123", "my-token");
    }

    // --- /api/stream/ready Tests ---

    @Test
    @DisplayName("onStreamReady should return 200 with valid path")
    void onStreamReady_withValidPath_returns200() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("onStreamReady should return 200 even with invalid path")
    void onStreamReady_withInvalidPath_returns200() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "invalid-path"
                                }
                                """))
                .andExpect(status().isOk());
    }

    // --- /api/stream/not-ready Tests ---

    @Test
    @DisplayName("onStreamNotReady should call markStreamEnded with valid device ID")
    void onStreamNotReady_withValidPath_callsMarkStreamEnded() throws Exception {
        // Arrange
        doNothing().when(streamService).markStreamEnded("device-123");

        // Act & Assert
        mockMvc.perform(post("/api/stream/not-ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "live/device-123"
                                }
                                """))
                .andExpect(status().isOk());

        verify(streamService, times(1)).markStreamEnded("device-123");
    }

    @Test
    @DisplayName("onStreamNotReady should not call markStreamEnded with invalid path")
    void onStreamNotReady_withInvalidPath_doesNotCallMarkStreamEnded() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/not-ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "path": "invalid-path"
                                }
                                """))
                .andExpect(status().isOk());

        verify(streamService, never()).markStreamEnded(anyString());
    }

    // --- /api/stream/url/{deviceId} Tests ---

    @Test
    @DisplayName("getStreamUrl should return 200 with valid access token and device ownership")
    void getStreamUrl_withValidAccessTokenAndOwnership_returns200() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(true);
        when(streamService.getStreamUrl("device-123")).thenReturn("https://media.example.com/live/device-123/index.m3u8");
        when(streamService.isDeviceStreaming("device-123")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/stream/url/device-123")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamUrl").value("https://media.example.com/live/device-123/index.m3u8"))
                .andExpect(jsonPath("$.isStreaming").value(true))
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.accessToken").value("my-access-token"));

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, times(1)).getStreamUrl("device-123");
        verify(streamService, times(1)).isDeviceStreaming("device-123");
    }

    @Test
    @DisplayName("getStreamUrl should return 401 when access token is missing")
    void getStreamUrl_withMissingAccessToken_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/stream/url/device-123"))
                .andExpect(status().isUnauthorized());

        verify(deviceService, never()).hasAccessToDevice(anyString());
    }

    @Test
    @DisplayName("getStreamUrl should return 403 when user does not own device")
    void getStreamUrl_whenUserDoesNotOwnDevice_returns403() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/stream/url/device-123")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isForbidden());

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, never()).getStreamUrl(anyString());
    }

    // --- /api/stream/{deviceId}/start Tests ---

    @Test
    @DisplayName("startStream should return 200 with valid access token and device ownership")
    void startStream_withValidAccessTokenAndOwnership_returns200() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(true);
        when(streamService.createViewerSession()).thenReturn("session-abc-123");
        when(streamService.requestStreamStart("device-123", "session-abc-123")).thenReturn(true);
        when(streamService.getViewerCount("device-123")).thenReturn(1L);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/start")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.sessionId").value("session-abc-123"))
                .andExpect(jsonPath("$.viewerCount").value(1))
                .andExpect(jsonPath("$.success").value(true));

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, times(1)).createViewerSession();
        verify(streamService, times(1)).requestStreamStart("device-123", "session-abc-123");
        verify(streamService, times(1)).getViewerCount("device-123");
    }

    @Test
    @DisplayName("startStream should return 401 when access token is missing")
    void startStream_withMissingAccessToken_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/start"))
                .andExpect(status().isUnauthorized());

        verify(deviceService, never()).hasAccessToDevice(anyString());
    }

    @Test
    @DisplayName("startStream should return 403 when user does not own device")
    void startStream_whenUserDoesNotOwnDevice_returns403() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/start")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isForbidden());

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, never()).createViewerSession();
    }

    // --- /api/stream/{deviceId}/stop Tests ---

    @Test
    @DisplayName("stopStream should return 200 with valid session ID")
    void stopStream_withValidSessionId_returns200() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(true);
        when(streamService.requestStreamStop("device-123", "session-abc-123")).thenReturn(true);
        when(streamService.getViewerCount("device-123")).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/stop")
                        .param("sessionId", "session-abc-123")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.sessionId").value("session-abc-123"))
                .andExpect(jsonPath("$.viewerCount").value(0))
                .andExpect(jsonPath("$.success").value(true));

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, times(1)).requestStreamStop("device-123", "session-abc-123");
        verify(streamService, times(1)).getViewerCount("device-123");
    }

    @Test
    @DisplayName("stopStream should return 401 when access token is missing")
    void stopStream_withMissingAccessToken_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/stop")
                        .param("sessionId", "session-abc-123"))
                .andExpect(status().isUnauthorized());

        verify(deviceService, never()).hasAccessToDevice(anyString());
    }

    @Test
    @DisplayName("stopStream should return 403 when user does not own device")
    void stopStream_whenUserDoesNotOwnDevice_returns403() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/stop")
                        .param("sessionId", "session-abc-123")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isForbidden());

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, never()).requestStreamStop(anyString(), anyString());
    }

    @Test
    @DisplayName("stopStream should return 400 when sessionId is missing")
    void stopStream_withMissingSessionId_returns400() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/stop")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("sessionId is required"));

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, never()).requestStreamStop(anyString(), anyString());
    }

    // --- /api/stream/{deviceId}/heartbeat Tests ---

    @Test
    @DisplayName("heartbeat should return 200 with valid session ID")
    void heartbeat_withValidSessionId_returns200() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(true);
        when(streamService.heartbeat("device-123", "session-abc-123")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/heartbeat")
                        .param("sessionId", "session-abc-123")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.sessionId").value("session-abc-123"))
                .andExpect(jsonPath("$.extended").value(true));

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, times(1)).heartbeat("device-123", "session-abc-123");
    }

    @Test
    @DisplayName("heartbeat should return 401 when access token is missing")
    void heartbeat_withMissingAccessToken_returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/heartbeat")
                        .param("sessionId", "session-abc-123"))
                .andExpect(status().isUnauthorized());

        verify(deviceService, never()).hasAccessToDevice(anyString());
    }

    @Test
    @DisplayName("heartbeat should return 403 when user does not own device")
    void heartbeat_whenUserDoesNotOwnDevice_returns403() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/heartbeat")
                        .param("sessionId", "session-abc-123")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isForbidden());

        verify(deviceService, times(1)).hasAccessToDevice("device-123");
        verify(streamService, never()).heartbeat(anyString(), anyString());
    }

    @Test
    @DisplayName("heartbeat should return 200 even when session extension fails")
    void heartbeat_whenSessionExtensionFails_returns200() throws Exception {
        // Arrange
        when(deviceService.hasAccessToDevice("device-123")).thenReturn(true);
        when(streamService.heartbeat("device-123", "invalid-session")).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/stream/device-123/heartbeat")
                        .param("sessionId", "invalid-session")
                        .cookie(new jakarta.servlet.http.Cookie("access_token", "my-access-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extended").value(false));

        verify(streamService, times(1)).heartbeat("device-123", "invalid-session");
    }
}
