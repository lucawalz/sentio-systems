package org.example.backend.controller;

import org.example.backend.service.DeviceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeviceMqttAuthController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(DeviceMqttAuthControllerTest.TestBeans.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "mqtt.username=backend-service",
        "mqtt.password=secret-backend-password"
})
@DisplayName("DeviceMqttAuthController")
class DeviceMqttAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceService deviceService;

    @TestConfiguration
    static class TestBeans {
        @Bean
        DeviceService deviceService() {
            return mock(DeviceService.class);
        }
    }

    @AfterEach
    void resetMocks() {
        reset(deviceService);
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("authenticate should return 200 with valid backend credentials")
        void authenticateUser_withValidBackendCredentials_returns200() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/auth")
                            .param("username", "backend-service")
                            .param("password", "secret-backend-password"))
                    .andExpect(status().isOk());

            // Backend auth doesn't call device service
            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authenticate should return 403 with invalid backend password")
        void authenticateUser_withInvalidPassword_returns403() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/auth")
                            .param("username", "backend-service")
                            .param("password", "wrong-password"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authenticate should return 200 with valid device token")
        void authenticateDevice_withValidToken_returns200() throws Exception {
            // Arrange
            when(deviceService.validateMqttToken("device-123", "valid-token"))
                    .thenReturn(true);

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/auth")
                            .param("username", "device-123")
                            .param("password", "valid-token"))
                    .andExpect(status().isOk());

            verify(deviceService, times(1)).validateMqttToken("device-123", "valid-token");
        }

        @Test
        @DisplayName("authenticate should return 403 with invalid device token")
        void authenticateDevice_withInvalidToken_returns403() throws Exception {
            // Arrange
            when(deviceService.validateMqttToken("device-123", "invalid-token"))
                    .thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/auth")
                            .param("username", "device-123")
                            .param("password", "invalid-token"))
                    .andExpect(status().isForbidden());

            verify(deviceService, times(1)).validateMqttToken("device-123", "invalid-token");
        }

        @Test
        @DisplayName("authenticate should return 403 for expired device token")
        void authenticateDevice_withExpiredToken_returns403() throws Exception {
            // Arrange
            when(deviceService.validateMqttToken("device-456", "expired-token"))
                    .thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/auth")
                            .param("username", "device-456")
                            .param("password", "expired-token"))
                    .andExpect(status().isForbidden());

            verify(deviceService, times(1)).validateMqttToken("device-456", "expired-token");
        }
    }

    @Nested
    @DisplayName("Superuser Tests")
    class SuperuserTests {

        @Test
        @DisplayName("checkSuperuser should return 200 for backend service account")
        void checkSuperuser_backendUser_returns200() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/superuser")
                            .param("username", "backend-service"))
                    .andExpect(status().isOk());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("checkSuperuser should return 403 for device token")
        void checkSuperuser_deviceToken_returns403() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/superuser")
                            .param("username", "device-123"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("checkSuperuser should return 403 for random username")
        void checkSuperuser_randomUser_returns403() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/superuser")
                            .param("username", "random-user"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(deviceService);
        }
    }

    @Nested
    @DisplayName("ACL Authorization Tests")
    class AclTests {

        @Test
        @DisplayName("authorizeAcl should allow backend user to access all topics")
        void authorizeAcl_backendUser_allowsAllTopics() throws Exception {
            // Act & Assert - test various topics
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "backend-service")
                            .param("topic", "weather/data")
                            .param("acc", "1")) // subscribe
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "backend-service")
                            .param("topic", "animals/data")
                            .param("acc", "2")) // publish
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "backend-service")
                            .param("topic", "device/device-123/status")
                            .param("acc", "1"))
                    .andExpect(status().isOk());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authorizeAcl should allow device to publish to its own topic")
        void authorizeAcl_device_canPublishToOwnTopic() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-123")
                            .param("topic", "device/device-123/status")
                            .param("acc", "2")) // publish
                    .andExpect(status().isOk());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authorizeAcl should deny device from publishing to another device's topic")
        void authorizeAcl_device_cannotPublishToOtherDeviceTopic() throws Exception {
            // Act & Assert - CRITICAL SECURITY TEST
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-123")
                            .param("topic", "device/device-456/status")
                            .param("acc", "2"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authorizeAcl should allow device to subscribe to its own command topic")
        void authorizeAcl_device_canSubscribeToOwnCommandTopic() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-789")
                            .param("topic", "device/device-789/command")
                            .param("acc", "1")) // subscribe
                    .andExpect(status().isOk());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authorizeAcl should require weather_station service for weather topic")
        void authorizeAcl_device_weatherTopicRequiresService() throws Exception {
            // Arrange
            when(deviceService.getDeviceServices("device-123"))
                    .thenReturn(new HashSet<>(java.util.Arrays.asList("weather_station", "animal_detector")));

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-123")
                            .param("topic", "weather/data")
                            .param("acc", "2"))
                    .andExpect(status().isOk());

            verify(deviceService, times(1)).getDeviceServices("device-123");
        }

        @Test
        @DisplayName("authorizeAcl should deny weather topic if service not active")
        void authorizeAcl_device_weatherTopicDeniedWithoutService() throws Exception {
            // Arrange - device only has animal_detector, not weather_station
            when(deviceService.getDeviceServices("device-456"))
                    .thenReturn(Collections.singleton("animal_detector"));

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-456")
                            .param("topic", "weather/data")
                            .param("acc", "2"))
                    .andExpect(status().isForbidden());

            verify(deviceService, times(1)).getDeviceServices("device-456");
        }

        @Test
        @DisplayName("authorizeAcl should require animal_detector service for animals topic")
        void authorizeAcl_device_animalsTopicRequiresService() throws Exception {
            // Arrange
            when(deviceService.getDeviceServices("device-789"))
                    .thenReturn(Collections.singleton("animal_detector"));

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-789")
                            .param("topic", "animals/data")
                            .param("acc", "2"))
                    .andExpect(status().isOk());

            verify(deviceService, times(1)).getDeviceServices("device-789");
        }

        @Test
        @DisplayName("authorizeAcl should deny animals topic if service not active")
        void authorizeAcl_device_animalsTopicDeniedWithoutService() throws Exception {
            // Arrange - device only has weather_station, not animal_detector
            when(deviceService.getDeviceServices("device-999"))
                    .thenReturn(Collections.singleton("weather_station"));

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-999")
                            .param("topic", "animals/data")
                            .param("acc", "2"))
                    .andExpect(status().isForbidden());

            verify(deviceService, times(1)).getDeviceServices("device-999");
        }

        @Test
        @DisplayName("authorizeAcl should always allow status topic for bootstrapping")
        void authorizeAcl_statusTopicAlwaysAllowed() throws Exception {
            // Act & Assert - status topics don't require service check
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-new")
                            .param("topic", "device/device-new/status")
                            .param("acc", "2"))
                    .andExpect(status().isOk());

            // Should NOT call getDeviceServices for status topics
            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authorizeAcl should always allow command topic subscriptions")
        void authorizeAcl_commandTopicAlwaysAllowed() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-cmd")
                            .param("topic", "device/device-cmd/command")
                            .param("acc", "1")) // subscribe
                    .andExpect(status().isOk());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authorizeAcl should deny unknown topics")
        void authorizeAcl_unknownTopic_denied() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-123")
                            .param("topic", "unknown/topic")
                            .param("acc", "2"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("authorizeAcl should handle device with multiple services")
        void authorizeAcl_deviceWithMultipleServices_works() throws Exception {
            // Arrange
            Set<String> services = new HashSet<>(java.util.Arrays.asList("weather_station", "animal_detector"));
            when(deviceService.getDeviceServices("device-multi"))
                    .thenReturn(services);

            // Act & Assert - should allow both topics
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-multi")
                            .param("topic", "weather/data")
                            .param("acc", "2"))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-multi")
                            .param("topic", "animals/data")
                            .param("acc", "2"))
                    .andExpect(status().isOk());

            verify(deviceService, times(2)).getDeviceServices("device-multi");
        }

        @Test
        @DisplayName("authorizeAcl should handle device with no services")
        void authorizeAcl_deviceWithNoServices_deniesDataTopics() throws Exception {
            // Arrange
            when(deviceService.getDeviceServices("device-empty"))
                    .thenReturn(Collections.emptySet());

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-empty")
                            .param("topic", "weather/data")
                            .param("acc", "2"))
                    .andExpect(status().isForbidden());

            verify(deviceService, times(1)).getDeviceServices("device-empty");
        }

        @Test
        @DisplayName("authorizeAcl should support both publish and subscribe access types")
        void authorizeAcl_supportsPublishAndSubscribe() throws Exception {
            // Arrange
            when(deviceService.getDeviceServices("device-pub-sub"))
                    .thenReturn(Collections.singleton("weather_station"));

            // Act & Assert - acc=1 is subscribe, acc=2 is publish
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-pub-sub")
                            .param("topic", "weather/data")
                            .param("acc", "1")) // subscribe
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-pub-sub")
                            .param("topic", "weather/data")
                            .param("acc", "2")) // publish
                    .andExpect(status().isOk());

            verify(deviceService, times(2)).getDeviceServices("device-pub-sub");
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class SecurityEdgeCases {

        @Test
        @DisplayName("should prevent device topic hijacking with partial ID match")
        void authorizeAcl_preventsPartialIdMatch() throws Exception {
            // CRITICAL: Ensure device-12 cannot access device-123's topics
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-12")
                            .param("topic", "device/device-123/status")
                            .param("acc", "2"))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(deviceService);
        }

        @Test
        @DisplayName("should handle malformed device topic paths")
        void authorizeAcl_handlesMalformedPaths() throws Exception {
            // Arrange
            when(deviceService.getDeviceServices("device-xyz"))
                    .thenReturn(Collections.emptySet());

            // Act & Assert - malformed device path
            mockMvc.perform(post("/api/internal/mqtt/acl")
                            .param("username", "device-xyz")
                            .param("topic", "device/")
                            .param("acc", "2"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should handle empty username")
        void authenticate_withEmptyUsername_returns403() throws Exception {
            // Arrange
            when(deviceService.validateMqttToken("", "some-token"))
                    .thenReturn(false);

            // Act & Assert
            mockMvc.perform(post("/api/internal/mqtt/auth")
                            .param("username", "")
                            .param("password", "some-token"))
                    .andExpect(status().isForbidden());
        }
    }
}
