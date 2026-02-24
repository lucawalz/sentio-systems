package org.example.backend.service;

import org.example.backend.event.StreamStopScheduledEvent;
import org.example.backend.listener.StreamEventListener;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StreamService}.
 * Tests stream authentication, rate limiting, and MQTT command sending.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class StreamServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private ViewerSessionService viewerSessionService;

    @Mock
    private MessageChannel mqttOutboundChannel;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StreamEventListener streamEventListener;

    private StreamService streamService;

    private static final String TEST_DEVICE_ID = "test-device-123";
    private static final String TEST_DEVICE_TOKEN = "valid-device-token";
    private static final String TEST_SOURCE_IP = "192.168.1.100";
    private static final String TEST_USER_ID = "user-uuid-123";
    private static final String TEST_JWT_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test";

    @BeforeEach
    void setUp() {
        streamService = new StreamService(
                deviceRepository,
                deviceService,
                jwtDecoder,
                viewerSessionService,
                mqttOutboundChannel,
                eventPublisher,
                streamEventListener
        );
        ReflectionTestUtils.setField(streamService, "mediamtxBaseUrl", "https://media.syslabs.dev");
    }

    @Nested
    @DisplayName("validatePublishAuth")
    class ValidatePublishAuthTests {

        @Test
        @DisplayName("should succeed for valid device token")
        void shouldSucceedForValidDeviceToken() {
            // Given
            Device device = createTestDevice();
            when(deviceService.validateMqttToken(TEST_DEVICE_ID, TEST_DEVICE_TOKEN)).thenReturn(true);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, TEST_DEVICE_TOKEN, TEST_SOURCE_IP);

            // Then
            assertThat(result).isTrue();
            verify(deviceService).validateMqttToken(TEST_DEVICE_ID, TEST_DEVICE_TOKEN);
            verify(deviceRepository).findById(TEST_DEVICE_ID);
            verify(deviceRepository).save(argThat(d -> Boolean.TRUE.equals(d.getStreamActive())));
        }

        @Test
        @DisplayName("should fail when deviceId is null")
        void shouldFailWhenDeviceIdIsNull() {
            // When
            boolean result = streamService.validatePublishAuth(null, TEST_DEVICE_TOKEN, TEST_SOURCE_IP);

            // Then
            assertThat(result).isFalse();
            verify(deviceService, never()).validateMqttToken(anyString(), anyString());
        }

        @Test
        @DisplayName("should fail when deviceToken is null")
        void shouldFailWhenDeviceTokenIsNull() {
            // When
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, null, TEST_SOURCE_IP);

            // Then
            assertThat(result).isFalse();
            verify(deviceService, never()).validateMqttToken(anyString(), anyString());
        }

        @Test
        @DisplayName("should fail when token validation fails")
        void shouldFailWhenTokenValidationFails() {
            // Given
            when(deviceService.validateMqttToken(TEST_DEVICE_ID, "invalid-token")).thenReturn(false);

            // When
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, "invalid-token", TEST_SOURCE_IP);

            // Then
            assertThat(result).isFalse();
            verify(deviceService).validateMqttToken(TEST_DEVICE_ID, "invalid-token");
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should block after 5 failed attempts from same IP")
        void shouldBlockAfterMaxAttempts() {
            // Given
            when(deviceService.validateMqttToken(anyString(), anyString())).thenReturn(false);

            // When - Make 5 failed attempts
            for (int i = 0; i < 5; i++) {
                streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong-token", TEST_SOURCE_IP);
            }

            // Try 6th attempt
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, TEST_DEVICE_TOKEN, TEST_SOURCE_IP);

            // Then
            assertThat(result).isFalse();
            // Only first 5 attempts should call validateMqttToken
            verify(deviceService, times(5)).validateMqttToken(anyString(), anyString());
        }

        @Test
        @DisplayName("should warn on IP mismatch but still allow")
        void shouldWarnOnIpMismatchButAllow() {
            // Given
            Device device = createTestDevice();
            device.setIpAddress("10.0.0.50"); // Different from TEST_SOURCE_IP
            when(deviceService.validateMqttToken(TEST_DEVICE_ID, TEST_DEVICE_TOKEN)).thenReturn(true);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, TEST_DEVICE_TOKEN, TEST_SOURCE_IP);

            // Then
            assertThat(result).isTrue(); // Still allows despite IP mismatch
            verify(deviceRepository).save(any(Device.class));
        }

        @Test
        @DisplayName("should reset rate limit on successful auth")
        void shouldResetRateLimitOnSuccess() {
            // Given
            Device device = createTestDevice();
            when(deviceService.validateMqttToken(anyString(), anyString()))
                    .thenReturn(false) // First attempt fails
                    .thenReturn(true);  // Second attempt succeeds
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong-token", TEST_SOURCE_IP);
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, TEST_DEVICE_TOKEN, TEST_SOURCE_IP);

            // Then
            assertThat(result).isTrue();

            // Now can make more attempts without being blocked
            when(deviceService.validateMqttToken(anyString(), anyString())).thenReturn(false);
            for (int i = 0; i < 5; i++) {
                streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong", TEST_SOURCE_IP);
            }
            verify(deviceService, times(7)).validateMqttToken(anyString(), anyString());
        }

        @Test
        @DisplayName("should handle device not found in repository")
        void shouldHandleDeviceNotFound() {
            // Given
            when(deviceService.validateMqttToken(TEST_DEVICE_ID, TEST_DEVICE_TOKEN)).thenReturn(true);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.empty());

            // When
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, TEST_DEVICE_TOKEN, TEST_SOURCE_IP);

            // Then
            assertThat(result).isTrue(); // Token validation still succeeds
            verify(deviceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("validatePlaybackAuth")
    class ValidatePlaybackAuthTests {

        @Test
        @DisplayName("should succeed for valid Keycloak JWT")
        void shouldSucceedForValidJwt() {
            // Given
            Jwt jwt = createMockJwt(TEST_USER_ID);
            Device device = createTestDevice();
            device.setOwnerId(TEST_USER_ID);

            when(jwtDecoder.decode(TEST_JWT_TOKEN)).thenReturn(jwt);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            boolean result = streamService.validatePlaybackAuth(TEST_DEVICE_ID, TEST_JWT_TOKEN);

            // Then
            assertThat(result).isTrue();
            verify(jwtDecoder).decode(TEST_JWT_TOKEN);
            verify(deviceRepository).findById(TEST_DEVICE_ID);
        }

        @Test
        @DisplayName("should fail when deviceId is null")
        void shouldFailWhenDeviceIdIsNull() {
            // When
            boolean result = streamService.validatePlaybackAuth(null, TEST_JWT_TOKEN);

            // Then
            assertThat(result).isFalse();
            verify(jwtDecoder, never()).decode(anyString());
        }

        @Test
        @DisplayName("should fail when token is null")
        void shouldFailWhenTokenIsNull() {
            // When
            boolean result = streamService.validatePlaybackAuth(TEST_DEVICE_ID, null);

            // Then
            assertThat(result).isFalse();
            verify(jwtDecoder, never()).decode(anyString());
        }

        @Test
        @DisplayName("should fail when JWT is invalid")
        void shouldFailWhenJwtIsInvalid() {
            // Given
            when(jwtDecoder.decode("invalid-jwt")).thenThrow(new JwtException("Invalid token"));

            // When
            boolean result = streamService.validatePlaybackAuth(TEST_DEVICE_ID, "invalid-jwt");

            // Then
            assertThat(result).isFalse();
            verify(jwtDecoder).decode("invalid-jwt");
        }

        @Test
        @DisplayName("should fail when JWT has no subject")
        void shouldFailWhenJwtHasNoSubject() {
            // Given
            Jwt jwt = createMockJwt(null);
            when(jwtDecoder.decode(TEST_JWT_TOKEN)).thenReturn(jwt);

            // When
            boolean result = streamService.validatePlaybackAuth(TEST_DEVICE_ID, TEST_JWT_TOKEN);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should fail when device not found")
        void shouldFailWhenDeviceNotFound() {
            // Given
            Jwt jwt = createMockJwt(TEST_USER_ID);
            when(jwtDecoder.decode(TEST_JWT_TOKEN)).thenReturn(jwt);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.empty());

            // When
            boolean result = streamService.validatePlaybackAuth(TEST_DEVICE_ID, TEST_JWT_TOKEN);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should fail when user does not own device")
        void shouldFailWhenUserDoesNotOwnDevice() {
            // Given
            Jwt jwt = createMockJwt(TEST_USER_ID);
            Device device = createTestDevice();
            device.setOwnerId("different-user-id");

            when(jwtDecoder.decode(TEST_JWT_TOKEN)).thenReturn(jwt);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            boolean result = streamService.validatePlaybackAuth(TEST_DEVICE_ID, TEST_JWT_TOKEN);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should fail when JWT is expired")
        void shouldFailWhenJwtIsExpired() {
            // Given
            when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("Token expired"));

            // When
            boolean result = streamService.validatePlaybackAuth(TEST_DEVICE_ID, TEST_JWT_TOKEN);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getStreamUrl")
    class GetStreamUrlTests {

        @Test
        @DisplayName("should return HLS URL with correct format")
        void shouldReturnHlsUrlWithCorrectFormat() {
            // When
            String url = streamService.getStreamUrl(TEST_DEVICE_ID);

            // Then
            assertThat(url).isEqualTo("https://media.syslabs.dev/live/test-device-123/index.m3u8");
        }

        @Test
        @DisplayName("should use mediamtxBaseUrl from configuration")
        void shouldUseMediamtxBaseUrlFromConfig() {
            // Given
            ReflectionTestUtils.setField(streamService, "mediamtxBaseUrl", "https://custom.media.server");

            // When
            String url = streamService.getStreamUrl(TEST_DEVICE_ID);

            // Then
            assertThat(url).startsWith("https://custom.media.server/live/");
        }

        @Test
        @DisplayName("should include deviceId in path")
        void shouldIncludeDeviceIdInPath() {
            // When
            String url = streamService.getStreamUrl("my-custom-device-id");

            // Then
            assertThat(url).contains("my-custom-device-id");
            assertThat(url).endsWith("index.m3u8");
        }
    }

    @Nested
    @DisplayName("Rate Limiting")
    class RateLimitingTests {

        @Test
        @DisplayName("should track failed attempts per IP")
        void shouldTrackFailedAttemptsPerIp() {
            // Given
            String ip1 = "192.168.1.1";
            String ip2 = "192.168.1.2";
            when(deviceService.validateMqttToken(anyString(), anyString())).thenReturn(false);

            // When - Make 3 failed attempts from ip1
            for (int i = 0; i < 3; i++) {
                streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong", ip1);
            }

            // Make 2 failed attempts from ip2
            for (int i = 0; i < 2; i++) {
                streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong", ip2);
            }

            // Then - Both IPs should still be allowed (under limit)
            verify(deviceService, times(5)).validateMqttToken(anyString(), anyString());
        }

        @Test
        @DisplayName("should handle null IP gracefully for failed auth")
        void shouldHandleNullIpGracefully() {
            // Given
            when(deviceService.validateMqttToken(TEST_DEVICE_ID, "wrong-token")).thenReturn(false);

            // When
            boolean result = streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong-token", null);

            // Then
            assertThat(result).isFalse();
            verify(deviceService).validateMqttToken(TEST_DEVICE_ID, "wrong-token");
            // Null IP should not cause rate limiting issues on failed auth
        }

        @Test
        @DisplayName("should reset rate limit window after successful auth")
        void shouldResetWindowOnSuccess() {
            // Given
            when(deviceService.validateMqttToken(anyString(), anyString()))
                    .thenReturn(false, false, true); // 2 fails, then success
            Device device = createTestDevice();
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong1", TEST_SOURCE_IP);
            streamService.validatePublishAuth(TEST_DEVICE_ID, "wrong2", TEST_SOURCE_IP);
            streamService.validatePublishAuth(TEST_DEVICE_ID, TEST_DEVICE_TOKEN, TEST_SOURCE_IP);

            // Then - Should be able to make new attempts without being blocked
            when(deviceService.validateMqttToken(anyString(), anyString())).thenReturn(false);
            for (int i = 0; i < 5; i++) {
                streamService.validatePublishAuth(TEST_DEVICE_ID, "attempt", TEST_SOURCE_IP);
            }

            verify(deviceService, times(8)).validateMqttToken(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Stream Control")
    class StreamControlTests {

        @Test
        @DisplayName("should send MQTT command when first viewer joins")
        void shouldSendMqttCommandOnFirstViewer() {
            // Given
            String sessionId = "session-123";
            when(viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId)).thenReturn(true);
            when(mqttOutboundChannel.send(any(Message.class))).thenReturn(true);

            // When
            boolean result = streamService.requestStreamStart(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(result).isTrue();
            verify(streamEventListener).cancelPendingStop(TEST_DEVICE_ID);
            verify(viewerSessionService).joinStream(TEST_DEVICE_ID, sessionId);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(mqttOutboundChannel).send(messageCaptor.capture());

            Message sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getPayload()).isEqualTo("{\"service\": \"stream\", \"command\": \"start\"}");
            assertThat(sentMessage.getHeaders().get(MqttHeaders.TOPIC))
                    .isEqualTo("device/test-device-123/command");
        }

        @Test
        @DisplayName("should not send MQTT command when subsequent viewers join")
        void shouldNotSendMqttCommandOnSubsequentViewers() {
            // Given
            String sessionId = "session-456";
            when(viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId)).thenReturn(false);

            // When
            boolean result = streamService.requestStreamStart(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(result).isTrue();
            verify(mqttOutboundChannel, never()).send(any(Message.class));
        }

        @Test
        @DisplayName("should publish event when last viewer leaves")
        void shouldPublishEventOnLastViewer() {
            // Given
            String sessionId = "session-789";
            when(viewerSessionService.leaveStream(TEST_DEVICE_ID, sessionId)).thenReturn(true);

            // When
            boolean result = streamService.requestStreamStop(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(result).isTrue();
            ArgumentCaptor<StreamStopScheduledEvent> eventCaptor = ArgumentCaptor.forClass(StreamStopScheduledEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            StreamStopScheduledEvent event = eventCaptor.getValue();
            assertThat(event.getDeviceId()).isEqualTo(TEST_DEVICE_ID);
        }

        @Test
        @DisplayName("should handle MQTT send failure gracefully")
        void shouldHandleMqttSendFailure() {
            // Given
            String sessionId = "session-fail";
            when(viewerSessionService.joinStream(TEST_DEVICE_ID, sessionId)).thenReturn(true);
            when(mqttOutboundChannel.send(any(Message.class))).thenThrow(new RuntimeException("MQTT broker down"));

            // When
            boolean result = streamService.requestStreamStart(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Viewer Session Management")
    class ViewerSessionManagementTests {

        @Test
        @DisplayName("should create unique session IDs")
        void shouldCreateUniqueSessionIds() {
            // Given
            when(viewerSessionService.createSessionId()).thenReturn("session-1", "session-2");

            // When
            String sessionId1 = streamService.createViewerSession();
            String sessionId2 = streamService.createViewerSession();

            // Then
            assertThat(sessionId1).isEqualTo("session-1");
            assertThat(sessionId2).isEqualTo("session-2");
            verify(viewerSessionService, times(2)).createSessionId();
        }

        @Test
        @DisplayName("should extend session TTL on heartbeat")
        void shouldExtendSessionOnHeartbeat() {
            // Given
            String sessionId = "session-hb";
            when(viewerSessionService.heartbeat(TEST_DEVICE_ID, sessionId)).thenReturn(true);

            // When
            boolean result = streamService.heartbeat(TEST_DEVICE_ID, sessionId);

            // Then
            assertThat(result).isTrue();
            verify(viewerSessionService).heartbeat(TEST_DEVICE_ID, sessionId);
        }

        @Test
        @DisplayName("should get viewer count for device")
        void shouldGetViewerCount() {
            // Given
            when(viewerSessionService.getViewerCount(TEST_DEVICE_ID)).thenReturn(3L);

            // When
            long count = streamService.getViewerCount(TEST_DEVICE_ID);

            // Then
            assertThat(count).isEqualTo(3L);
            verify(viewerSessionService).getViewerCount(TEST_DEVICE_ID);
        }
    }

    @Nested
    @DisplayName("Stream Status")
    class StreamStatusTests {

        @Test
        @DisplayName("should return true when device is streaming")
        void shouldReturnTrueWhenDeviceIsStreaming() {
            // Given
            Device device = createTestDevice();
            device.setStreamActive(true);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            boolean result = streamService.isDeviceStreaming(TEST_DEVICE_ID);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when device is not streaming")
        void shouldReturnFalseWhenDeviceNotStreaming() {
            // Given
            Device device = createTestDevice();
            device.setStreamActive(false);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            boolean result = streamService.isDeviceStreaming(TEST_DEVICE_ID);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when device not found")
        void shouldReturnFalseWhenDeviceNotFound() {
            // Given
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.empty());

            // When
            boolean result = streamService.isDeviceStreaming(TEST_DEVICE_ID);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should mark stream as ended")
        void shouldMarkStreamAsEnded() {
            // Given
            Device device = createTestDevice();
            device.setStreamActive(true);
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.of(device));

            // When
            streamService.markStreamEnded(TEST_DEVICE_ID);

            // Then
            verify(deviceRepository).save(argThat(d -> Boolean.FALSE.equals(d.getStreamActive())));
        }

        @Test
        @DisplayName("should handle marking stream ended for non-existent device")
        void shouldHandleMarkStreamEndedForNonExistentDevice() {
            // Given
            when(deviceRepository.findById(TEST_DEVICE_ID)).thenReturn(Optional.empty());

            // When
            streamService.markStreamEnded(TEST_DEVICE_ID);

            // Then
            verify(deviceRepository, never()).save(any());
        }
    }

    // --- Helper Methods ---

    private Device createTestDevice() {
        Device device = new Device();
        device.setId(TEST_DEVICE_ID);
        device.setOwnerId(TEST_USER_ID);
        device.setName("Test Device");
        device.setIpAddress(TEST_SOURCE_IP);
        device.setStreamActive(false);
        return device;
    }

    private Jwt createMockJwt(String subject) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getTokenValue()).thenReturn(TEST_JWT_TOKEN);
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        return jwt;
    }
}
