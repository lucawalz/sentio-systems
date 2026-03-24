package dev.syslabs.sentio.controller;

import dev.syslabs.sentio.dto.DevicePairResponse;
import dev.syslabs.sentio.dto.DeviceRegistrationResponse;
import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.service.DeviceLocationService;
import dev.syslabs.sentio.service.DeviceService;
import dev.syslabs.sentio.service.RateLimitService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DeviceController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(DeviceControllerTest.TestBeans.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "mqtt.external-url=mqtt://test-mqtt.example.com:1883"
})
@DisplayName("DeviceController")
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceLocationService deviceLocationService;

    @Autowired
    private RateLimitService rateLimitService;

    @TestConfiguration
    static class TestBeans {
        @Bean
        DeviceService deviceService() {
            return mock(DeviceService.class);
        }

        @Bean
        DeviceLocationService deviceLocationService() {
            return mock(DeviceLocationService.class);
        }

        @Bean
        RateLimitService rateLimitService() {
            return mock(RateLimitService.class);
        }
    }

    @AfterEach
    void resetMocks() {
        reset(deviceService, deviceLocationService, rateLimitService);
    }

    @Test
    @DisplayName("registerDevice should return 200 with valid name")
    void registerDevice_withValidName_returns200() throws Exception {
        // Arrange
        DeviceRegistrationResponse response = DeviceRegistrationResponse.builder()
                .deviceId("device-123")
                .name("My Test Device")
                .pairingCode("1234-5678")
                .pairingCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .mqttUrl("mqtt://test-mqtt.example.com:1883")
                .build();

        when(deviceService.registerDeviceWithCredentials("My Test Device")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/devices/register")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "My Test Device"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.name").value("My Test Device"))
                .andExpect(jsonPath("$.pairingCode").value("1234-5678"))
                .andExpect(jsonPath("$.mqttUrl").value("mqtt://test-mqtt.example.com:1883"));

        verify(deviceService, times(1)).registerDeviceWithCredentials("My Test Device");
        verify(deviceService, never()).setPrimaryDevice(anyString());
    }

    @Test
    @DisplayName("registerDevice should use default name when not provided")
    void registerDevice_withoutName_usesDefaultName() throws Exception {
        // Arrange
        DeviceRegistrationResponse response = DeviceRegistrationResponse.builder()
                .deviceId("device-456")
                .name("My Device")
                .pairingCode("AAAA-BBBB")
                .pairingCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(deviceService.registerDeviceWithCredentials("My Device")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/devices/register")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Device"));

        verify(deviceService, times(1)).registerDeviceWithCredentials("My Device");
    }

    @Test
    @DisplayName("registerDevice should set as primary when isPrimary flag is true")
    void registerDevice_withIsPrimaryFlag_setsPrimary() throws Exception {
        // Arrange
        DeviceRegistrationResponse response = DeviceRegistrationResponse.builder()
                .deviceId("device-primary")
                .name("Primary Device")
                .pairingCode("1111-2222")
                .pairingCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(deviceService.registerDeviceWithCredentials("Primary Device")).thenReturn(response);
        when(deviceService.setPrimaryDevice("device-primary")).thenReturn(new Device());

        // Act & Assert
        mockMvc.perform(post("/api/devices/register")
                        .contentType("application/json")
                        .content("""
                                {
                                    "name": "Primary Device",
                                    "isPrimary": "true"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-primary"));

        verify(deviceService, times(1)).registerDeviceWithCredentials("Primary Device");
        verify(deviceService, times(1)).setPrimaryDevice("device-primary");
    }

    @Test
    @DisplayName("pairDevice should return 200 with valid pairing code")
    void pairDevice_withValidCode_returns200WithToken() throws Exception {
        // Arrange
        when(rateLimitService.allowPairingRequest(anyString())).thenReturn(true);
        when(deviceService.exchangePairingCode("device-123", "1234-5678"))
                .thenReturn("device-token-abc123");

        // Act & Assert
        mockMvc.perform(post("/api/devices/pair")
                        .contentType("application/json")
                        .content("""
                                {
                                    "deviceId": "device-123",
                                    "pairingCode": "1234-5678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.deviceToken").value("device-token-abc123"))
                .andExpect(jsonPath("$.mqttUrl").value("mqtt://test-mqtt.example.com:1883"))
                .andExpect(jsonPath("$.message").value("Device paired successfully"));

        verify(rateLimitService, times(1)).allowPairingRequest(anyString());
        verify(deviceService, times(1)).exchangePairingCode("device-123", "1234-5678");
    }

    @Test
    @DisplayName("pairDevice should return 429 when rate limit exceeded")
    void pairDevice_exceedingRateLimit_returns429() throws Exception {
        // Arrange
        when(rateLimitService.allowPairingRequest(anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/devices/pair")
                        .contentType("application/json")
                        .content("""
                                {
                                    "deviceId": "device-123",
                                    "pairingCode": "1234-5678"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many pairing attempts. Please wait 1 minute."));

        verify(rateLimitService, times(1)).allowPairingRequest(anyString());
        verify(deviceService, never()).exchangePairingCode(anyString(), anyString());
    }

    @Test
    @DisplayName("pairDevice should extract client IP from X-Forwarded-For header")
    void pairDevice_extractsClientIpFromXForwardedFor() throws Exception {
        // Arrange
        when(rateLimitService.allowPairingRequest("203.0.113.45")).thenReturn(true);
        when(deviceService.exchangePairingCode(anyString(), anyString()))
                .thenReturn("device-token-xyz");

        // Act & Assert
        mockMvc.perform(post("/api/devices/pair")
                        .header("X-Forwarded-For", "203.0.113.45, 192.168.1.1")
                        .contentType("application/json")
                        .content("""
                                {
                                    "deviceId": "device-999",
                                    "pairingCode": "9999-9999"
                                }
                                """))
                .andExpect(status().isOk());

        verify(rateLimitService, times(1)).allowPairingRequest("203.0.113.45");
    }

    @Test
    @DisplayName("pairDevice should return 400 with invalid pairing code")
    void pairDevice_withInvalidCode_returns400() throws Exception {
        // Arrange
        when(rateLimitService.allowPairingRequest(anyString())).thenReturn(true);
        when(deviceService.exchangePairingCode("device-123", "WRNG-CODE"))
                .thenThrow(new IllegalArgumentException("Invalid or expired pairing code"));

        // Act & Assert
        mockMvc.perform(post("/api/devices/pair")
                        .contentType("application/json")
                        .content("""
                                {
                                    "deviceId": "device-123",
                                    "pairingCode": "WRNG-CODE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired pairing code"));

        verify(deviceService, times(1)).exchangePairingCode("device-123", "WRNG-CODE");
    }

    @Test
    @DisplayName("getMyDevices should return list of user devices")
    void getMyDevices_returnsUserDevices() throws Exception {
        // Arrange
        Device device1 = new Device();
        device1.setId("device-1");
        device1.setName("Device One");

        Device device2 = new Device();
        device2.setId("device-2");
        device2.setName("Device Two");

        List<Device> devices = Arrays.asList(device1, device2);
        when(deviceService.getMyDevices()).thenReturn(devices);

        // Act & Assert
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("device-1"))
                .andExpect(jsonPath("$[0].name").value("Device One"))
                .andExpect(jsonPath("$[1].id").value("device-2"))
                .andExpect(jsonPath("$[1].name").value("Device Two"));

        verify(deviceService, times(1)).getMyDevices();
    }

    @Test
    @DisplayName("unregisterDevice should return 204")
    void unregisterDevice_returns204() throws Exception {
        // Arrange
        doNothing().when(deviceService).unregisterDevice("device-123");

        // Act & Assert
        mockMvc.perform(delete("/api/devices/device-123"))
                .andExpect(status().isNoContent());

        verify(deviceService, times(1)).unregisterDevice("device-123");
    }

    @Test
    @DisplayName("hasAnyDevices should return true when devices exist")
    void hasAnyDevices_whenDevicesExist_returnsTrue() throws Exception {
        // Arrange
        when(deviceService.hasAnyDevices()).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/devices/has-any"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(deviceService, times(1)).hasAnyDevices();
    }

    @Test
    @DisplayName("hasAnyDevices should return false when no devices exist")
    void hasAnyDevices_whenNoDevices_returnsFalse() throws Exception {
        // Arrange
        when(deviceService.hasAnyDevices()).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/devices/has-any"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(deviceService, times(1)).hasAnyDevices();
    }

    @Test
    @DisplayName("getPrimaryDevice should return 200 when primary device exists")
    void getPrimaryDevice_whenExists_returns200() throws Exception {
        // Arrange
        Device primaryDevice = new Device();
        primaryDevice.setId("primary-device");
        primaryDevice.setName("Primary Device");
        primaryDevice.setIsPrimary(true);

        when(deviceService.getPrimaryDevice()).thenReturn(Optional.of(primaryDevice));

        // Act & Assert
        mockMvc.perform(get("/api/devices/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("primary-device"))
                .andExpect(jsonPath("$.name").value("Primary Device"))
                .andExpect(jsonPath("$.isPrimary").value(true));

        verify(deviceService, times(1)).getPrimaryDevice();
    }

    @Test
    @DisplayName("getPrimaryDevice should return 404 when no primary device exists")
    void getPrimaryDevice_whenNotExists_returns404() throws Exception {
        // Arrange
        when(deviceService.getPrimaryDevice()).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/devices/primary"))
                .andExpect(status().isNotFound());

        verify(deviceService, times(1)).getPrimaryDevice();
    }

    @Test
    @DisplayName("setPrimaryDevice should return 200 with valid device ID")
    void setPrimaryDevice_withValidId_returns200() throws Exception {
        // Arrange
        Device device = new Device();
        device.setId("device-123");
        device.setName("Test Device");
        device.setIsPrimary(true);

        when(deviceService.setPrimaryDevice("device-123")).thenReturn(device);

        // Act & Assert
        mockMvc.perform(put("/api/devices/device-123/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("device-123"))
                .andExpect(jsonPath("$.isPrimary").value(true));

        verify(deviceService, times(1)).setPrimaryDevice("device-123");
    }

    @Test
    @DisplayName("setPrimaryDevice should return 404 when device not found")
    void setPrimaryDevice_whenNotFound_returns404() throws Exception {
        // Arrange
        when(deviceService.setPrimaryDevice("invalid-device"))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(put("/api/devices/invalid-device/primary"))
                .andExpect(status().isNotFound());

        verify(deviceService, times(1)).setPrimaryDevice("invalid-device");
    }

    @Test
    @DisplayName("updateDeviceLocation should return 200 with valid coordinates")
    void updateDeviceLocation_withValidCoords_returns200() throws Exception {
        // Arrange
        when(deviceLocationService.updateDeviceGpsLocation("device-123", 47.3769, 8.5417))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(put("/api/devices/device-123/location")
                        .param("latitude", "47.3769")
                        .param("longitude", "8.5417"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("GPS location updated, weather data fetch triggered"))
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.latitude").value(47.3769))
                .andExpect(jsonPath("$.longitude").value(8.5417));

        verify(deviceLocationService, times(1))
                .updateDeviceGpsLocation("device-123", 47.3769, 8.5417);
    }

    @Test
    @DisplayName("updateDeviceLocation should return 404 when device not found")
    void updateDeviceLocation_whenNotFound_returns404() throws Exception {
        // Arrange
        when(deviceLocationService.updateDeviceGpsLocation("invalid-device", 47.3769, 8.5417))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(put("/api/devices/invalid-device/location")
                        .param("latitude", "47.3769")
                        .param("longitude", "8.5417"))
                .andExpect(status().isNotFound());

        verify(deviceLocationService, times(1))
                .updateDeviceGpsLocation("invalid-device", 47.3769, 8.5417);
    }

    @Test
    @DisplayName("updateDeviceLocation should return 400 when coordinates are invalid")
    void updateDeviceLocation_withInvalidCoords_returns400() throws Exception {
        // Arrange
        when(deviceLocationService.updateDeviceGpsLocation(eq("device-123"), anyDouble(), anyDouble()))
                .thenThrow(new IllegalArgumentException("Invalid coordinates"));

        // Act & Assert
        mockMvc.perform(put("/api/devices/device-123/location")
                        .param("latitude", "999")
                        .param("longitude", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid coordinates"));

        verify(deviceLocationService, times(1))
                .updateDeviceGpsLocation("device-123", 999.0, 999.0);
    }

    @Test
    @DisplayName("regeneratePairingCode should return 200 with valid device ID")
    void regeneratePairingCode_withValidDevice_returns200() throws Exception {
        // Arrange
        DeviceRegistrationResponse response = DeviceRegistrationResponse.builder()
                .deviceId("device-123")
                .name("Test Device")
                .pairingCode("NEW1-NEW2")
                .pairingCodeExpiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(deviceService.regeneratePairingCode("device-123")).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/devices/device-123/regenerate-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceId").value("device-123"))
                .andExpect(jsonPath("$.pairingCode").value("NEW1-NEW2"));

        verify(deviceService, times(1)).regeneratePairingCode("device-123");
    }

    @Test
    @DisplayName("regeneratePairingCode should return 404 when device not found")
    void regeneratePairingCode_whenNotFound_returns404() throws Exception {
        // Arrange
        when(deviceService.regeneratePairingCode("invalid-device"))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(post("/api/devices/invalid-device/regenerate-code"))
                .andExpect(status().isNotFound());

        verify(deviceService, times(1)).regeneratePairingCode("invalid-device");
    }
}
