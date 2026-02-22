package org.example.backend.service;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.dto.AuthDTOs;
import org.example.backend.dto.DeviceRegistrationResponse;
import org.example.backend.event.DeviceRegisteredEvent;
import org.example.backend.event.DeviceUnregisteredEvent;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link DeviceService}.
 * Validates device management with real database persistence.
 */
class DeviceServiceIT extends BaseIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    private static final String USER_ID = "user-123";
    private static final String OTHER_USER_ID = "user-456";

    private AuthDTOs.UserInfo testUser;
    private AuthDTOs.UserInfo otherUser;

    @BeforeEach
    void setUp() {
        deviceRepository.deleteAll();
        testUser = new AuthDTOs.UserInfo(USER_ID, "testuser", "test@example.com", List.of("user"));
        otherUser = new AuthDTOs.UserInfo(OTHER_USER_ID, "otheruser", "other@example.com", List.of("user"));
        when(authService.getCurrentUser()).thenReturn(testUser);
        reset(eventPublisher);
    }

    @Nested
    @DisplayName("registerDevice")
    class RegisterDeviceTests {

        @Test
        @DisplayName("should persist new device to database")
        void shouldPersistNewDevice() {
            // When
            Device result = deviceService.registerDevice("device-001", "My Pi");

            // Then
            assertThat(result.getId()).isEqualTo("device-001");
            assertThat(result.getName()).isEqualTo("My Pi");
            assertThat(result.getOwnerId()).isEqualTo(USER_ID);
            assertThat(result.getCreatedAt()).isNotNull();

            // Verify in database
            Device fromDb = deviceRepository.findById("device-001").orElseThrow();
            assertThat(fromDb.getOwnerId()).isEqualTo(USER_ID);
            assertThat(fromDb.getName()).isEqualTo("My Pi");
        }

        @Test
        @DisplayName("should update existing device owned by same user")
        void shouldUpdateExistingDevice() {
            // Given
            deviceService.registerDevice("device-001", "Original Name");

            // When
            Device result = deviceService.registerDevice("device-001", "Updated Name");

            // Then
            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(deviceRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject device owned by another user")
        void shouldRejectDeviceOwnedByOther() {
            // Given
            Device existing = new Device();
            existing.setId("device-001");
            existing.setName("Other's Device");
            existing.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(existing);

            // When/Then
            assertThatThrownBy(() -> deviceService.registerDevice("device-001", "My Device"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already belongs to another user");
        }

        @Test
        @DisplayName("should publish DeviceRegisteredEvent for new device")
        void shouldPublishEventForNewDevice() {
            // When
            Device result = deviceService.registerDevice("device-001", "My Pi");

            // Then
            verify(eventPublisher).publishEvent(any(DeviceRegisteredEvent.class));
        }

        @Test
        @DisplayName("should not publish event when updating existing device")
        void shouldNotPublishEventForUpdate() {
            // Given
            deviceService.registerDevice("device-001", "Original Name");
            reset(eventPublisher);

            // When
            deviceService.registerDevice("device-001", "Updated Name");

            // Then
            verify(eventPublisher, never()).publishEvent(any(DeviceRegisteredEvent.class));
        }
    }

    @Nested
    @DisplayName("getMyDevices")
    class GetMyDevicesTests {

        @Test
        @DisplayName("should return only devices owned by current user")
        void shouldReturnOnlyOwnedDevices() {
            // Given
            Device myDevice = new Device();
            myDevice.setId("device-001");
            myDevice.setName("My Device");
            myDevice.setOwnerId(USER_ID);
            deviceRepository.save(myDevice);

            Device otherDevice = new Device();
            otherDevice.setId("device-002");
            otherDevice.setName("Other Device");
            otherDevice.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(otherDevice);

            // When
            List<Device> result = deviceService.getMyDevices();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("device-001");
        }
    }

    @Nested
    @DisplayName("hasAccessToDevice")
    class HasAccessTests {

        @Test
        @DisplayName("should return true for owned device")
        void shouldReturnTrueForOwnedDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setOwnerId(USER_ID);
            deviceRepository.save(device);

            // When
            boolean result = deviceService.hasAccessToDevice("device-001");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false for unowned device")
        void shouldReturnFalseForUnownedDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(device);

            // When
            boolean result = deviceService.hasAccessToDevice("device-001");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null deviceId")
        void shouldReturnFalseForNullDeviceId() {
            // When
            boolean result = deviceService.hasAccessToDevice(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for non-existent device")
        void shouldReturnFalseForNonExistentDevice() {
            // When
            boolean result = deviceService.hasAccessToDevice("non-existent");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when authService throws exception")
        void shouldReturnFalseWhenAuthFails() {
            // Given
            when(authService.getCurrentUser()).thenThrow(new RuntimeException("Auth failed"));

            // When
            boolean result = deviceService.hasAccessToDevice("device-001");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("registerDeviceWithCredentials")
    class RegisterDeviceWithCredentialsTests {

        @Test
        @DisplayName("should create device with pairing code")
        void shouldCreateDeviceWithPairingCode() {
            // When
            DeviceRegistrationResponse response = deviceService.registerDeviceWithCredentials("Kitchen Pi");

            // Then
            assertThat(response.getDeviceId()).isNotNull();
            assertThat(response.getName()).isEqualTo("Kitchen Pi");
            assertThat(response.getPairingCode()).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
            assertThat(response.getPairingCodeExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(response.getPairingCodeExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(16));
            assertThat(response.getMqttUrl()).isNotNull();

            // Verify in database
            Device fromDb = deviceRepository.findById(response.getDeviceId()).orElseThrow();
            assertThat(fromDb.getOwnerId()).isEqualTo(USER_ID);
            assertThat(fromDb.getPairingCode()).isEqualTo(response.getPairingCode());
            assertThat(fromDb.getMqttTokenHash()).isNull();
        }

        @Test
        @DisplayName("should publish DeviceRegisteredEvent")
        void shouldPublishEvent() {
            // When
            deviceService.registerDeviceWithCredentials("Test Device");

            // Then
            verify(eventPublisher).publishEvent(any(DeviceRegisteredEvent.class));
        }
    }

    @Nested
    @DisplayName("regeneratePairingCode")
    class RegeneratePairingCodeTests {

        @Test
        @DisplayName("should regenerate pairing code for owned device")
        void shouldRegeneratePairingCode() {
            // Given
            DeviceRegistrationResponse initial = deviceService.registerDeviceWithCredentials("Test Device");
            String initialCode = initial.getPairingCode();

            // When
            DeviceRegistrationResponse response = deviceService.regeneratePairingCode(initial.getDeviceId());

            // Then
            assertThat(response.getDeviceId()).isEqualTo(initial.getDeviceId());
            assertThat(response.getPairingCode()).isNotEqualTo(initialCode);
            assertThat(response.getPairingCode()).matches("[A-Z2-9]{4}-[A-Z2-9]{4}");
            assertThat(response.getPairingCodeExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("should clear existing MQTT token hash")
        void shouldClearExistingToken() {
            // Given
            DeviceRegistrationResponse initial = deviceService.registerDeviceWithCredentials("Test Device");
            Device device = deviceRepository.findById(initial.getDeviceId()).orElseThrow();
            device.setMqttTokenHash("existing-hash");
            deviceRepository.save(device);

            // When
            deviceService.regeneratePairingCode(initial.getDeviceId());

            // Then
            Device updated = deviceRepository.findById(initial.getDeviceId()).orElseThrow();
            assertThat(updated.getMqttTokenHash()).isNull();
        }

        @Test
        @DisplayName("should throw exception for non-existent device")
        void shouldThrowForNonExistentDevice() {
            // When/Then
            assertThatThrownBy(() -> deviceService.regeneratePairingCode("non-existent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found or not owned by user");
        }

        @Test
        @DisplayName("should throw exception for device owned by another user")
        void shouldThrowForOtherUsersDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Other's Device");
            device.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(device);

            // When/Then
            assertThatThrownBy(() -> deviceService.regeneratePairingCode("device-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found or not owned by user");
        }
    }

    @Nested
    @DisplayName("exchangePairingCode")
    class ExchangePairingCodeTests {

        @Test
        @DisplayName("should exchange valid pairing code for device token")
        void shouldExchangeValidPairingCode() {
            // Given
            DeviceRegistrationResponse registration = deviceService.registerDeviceWithCredentials("Test Device");

            // When
            String deviceToken = deviceService.exchangePairingCode(
                    registration.getDeviceId(),
                    registration.getPairingCode()
            );

            // Then
            assertThat(deviceToken).isNotNull();
            assertThat(deviceToken).hasSize(32);

            // Verify token hash is stored and pairing code is cleared
            Device device = deviceRepository.findById(registration.getDeviceId()).orElseThrow();
            assertThat(device.getMqttTokenHash()).isNotNull();
            assertThat(device.getPairingCode()).isNull();
            assertThat(device.getPairingCodeExpiresAt()).isNull();

            // Verify the token can be validated
            assertThat(BCrypt.checkpw(deviceToken, device.getMqttTokenHash())).isTrue();
        }

        @Test
        @DisplayName("should throw exception for null deviceId")
        void shouldThrowForNullDeviceId() {
            // When/Then
            assertThatThrownBy(() -> deviceService.exchangePairingCode(null, "ABCD-1234"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device ID and pairing code are required");
        }

        @Test
        @DisplayName("should throw exception for null pairing code")
        void shouldThrowForNullPairingCode() {
            // When/Then
            assertThatThrownBy(() -> deviceService.exchangePairingCode("device-001", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device ID and pairing code are required");
        }

        @Test
        @DisplayName("should throw exception for non-existent device")
        void shouldThrowForNonExistentDevice() {
            // When/Then
            assertThatThrownBy(() -> deviceService.exchangePairingCode("non-existent", "ABCD-1234"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found");
        }

        @Test
        @DisplayName("should throw exception for invalid pairing code")
        void shouldThrowForInvalidPairingCode() {
            // Given
            DeviceRegistrationResponse registration = deviceService.registerDeviceWithCredentials("Test Device");

            // When/Then
            assertThatThrownBy(() -> deviceService.exchangePairingCode(
                    registration.getDeviceId(),
                    "WRONG-CODE"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid pairing code");
        }

        @Test
        @DisplayName("should throw exception for expired pairing code")
        void shouldThrowForExpiredPairingCode() {
            // Given
            DeviceRegistrationResponse registration = deviceService.registerDeviceWithCredentials("Test Device");
            Device device = deviceRepository.findById(registration.getDeviceId()).orElseThrow();
            device.setPairingCodeExpiresAt(LocalDateTime.now().minusMinutes(1));
            deviceRepository.save(device);

            // When/Then
            assertThatThrownBy(() -> deviceService.exchangePairingCode(
                    registration.getDeviceId(),
                    registration.getPairingCode()
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Pairing code has expired");
        }

        @Test
        @DisplayName("should throw exception when pairing code is null on device")
        void shouldThrowWhenPairingCodeNull() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Test");
            device.setOwnerId(USER_ID);
            device.setPairingCode(null);
            deviceRepository.save(device);

            // When/Then
            assertThatThrownBy(() -> deviceService.exchangePairingCode("device-001", "ABCD-1234"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid pairing code");
        }

        @Test
        @DisplayName("should throw exception when expiration time is null")
        void shouldThrowWhenExpirationNull() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Test");
            device.setOwnerId(USER_ID);
            device.setPairingCode("ABCD-1234");
            device.setPairingCodeExpiresAt(null);
            deviceRepository.save(device);

            // When/Then
            assertThatThrownBy(() -> deviceService.exchangePairingCode("device-001", "ABCD-1234"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Pairing code has expired");
        }
    }

    @Nested
    @DisplayName("validateMqttToken")
    class ValidateMqttTokenTests {

        @Test
        @DisplayName("should validate correct MQTT token")
        void shouldValidateCorrectToken() {
            // Given
            DeviceRegistrationResponse registration = deviceService.registerDeviceWithCredentials("Test Device");
            String deviceToken = deviceService.exchangePairingCode(
                    registration.getDeviceId(),
                    registration.getPairingCode()
            );

            // When
            boolean result = deviceService.validateMqttToken(registration.getDeviceId(), deviceToken);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should reject incorrect MQTT token")
        void shouldRejectIncorrectToken() {
            // Given
            DeviceRegistrationResponse registration = deviceService.registerDeviceWithCredentials("Test Device");
            deviceService.exchangePairingCode(registration.getDeviceId(), registration.getPairingCode());

            // When
            boolean result = deviceService.validateMqttToken(registration.getDeviceId(), "wrong-token");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null deviceId")
        void shouldReturnFalseForNullDeviceId() {
            // When
            boolean result = deviceService.validateMqttToken(null, "some-token");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for null deviceToken")
        void shouldReturnFalseForNullToken() {
            // When
            boolean result = deviceService.validateMqttToken("device-001", null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false for non-existent device")
        void shouldReturnFalseForNonExistentDevice() {
            // When
            boolean result = deviceService.validateMqttToken("non-existent", "some-token");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when device has no token hash")
        void shouldReturnFalseWhenNoTokenHash() {
            // Given
            DeviceRegistrationResponse registration = deviceService.registerDeviceWithCredentials("Test Device");

            // When (no pairing code exchange, so no token hash)
            boolean result = deviceService.validateMqttToken(registration.getDeviceId(), "some-token");

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getDeviceServices")
    class GetDeviceServicesTests {

        @Test
        @DisplayName("should return active services for device")
        void shouldReturnActiveServices() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Test");
            device.setOwnerId(USER_ID);
            Set<String> services = new HashSet<>();
            services.add("animal_detector");
            services.add("weather_station");
            device.setActiveServices(services);
            deviceRepository.save(device);

            // When
            Set<String> result = deviceService.getDeviceServices("device-001");

            // Then
            assertThat(result).containsExactlyInAnyOrder("animal_detector", "weather_station");
        }

        @Test
        @DisplayName("should return empty set for null deviceId")
        void shouldReturnEmptySetForNullDeviceId() {
            // When
            Set<String> result = deviceService.getDeviceServices(null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty set for non-existent device")
        void shouldReturnEmptySetForNonExistentDevice() {
            // When
            Set<String> result = deviceService.getDeviceServices("non-existent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty set when device has no services")
        void shouldReturnEmptySetWhenNoServices() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Test");
            device.setOwnerId(USER_ID);
            deviceRepository.save(device);

            // When
            Set<String> result = deviceService.getDeviceServices("device-001");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("unregisterDevice")
    class UnregisterDeviceTests {

        @Test
        @DisplayName("should delete owned device")
        void shouldDeleteOwnedDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("My Device");
            device.setOwnerId(USER_ID);
            deviceRepository.save(device);

            // When
            deviceService.unregisterDevice("device-001");

            // Then
            assertThat(deviceRepository.findById("device-001")).isEmpty();
            verify(eventPublisher).publishEvent(any(DeviceUnregisteredEvent.class));
        }

        @Test
        @DisplayName("should not delete device owned by another user")
        void shouldNotDeleteOtherUsersDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Other's Device");
            device.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(device);

            // When
            deviceService.unregisterDevice("device-001");

            // Then
            assertThat(deviceRepository.findById("device-001")).isPresent();
            verify(eventPublisher, never()).publishEvent(any(DeviceUnregisteredEvent.class));
        }

        @Test
        @DisplayName("should handle non-existent device gracefully")
        void shouldHandleNonExistentDevice() {
            // When/Then (should not throw exception)
            deviceService.unregisterDevice("non-existent");
            verify(eventPublisher, never()).publishEvent(any(DeviceUnregisteredEvent.class));
        }
    }

    @Nested
    @DisplayName("getMyDeviceIds")
    class GetMyDeviceIdsTests {

        @Test
        @DisplayName("should return device IDs for current user")
        void shouldReturnDeviceIds() {
            // Given
            Device device1 = new Device();
            device1.setId("device-001");
            device1.setName("Device 1");
            device1.setOwnerId(USER_ID);
            deviceRepository.save(device1);

            Device device2 = new Device();
            device2.setId("device-002");
            device2.setName("Device 2");
            device2.setOwnerId(USER_ID);
            deviceRepository.save(device2);

            Device otherDevice = new Device();
            otherDevice.setId("device-003");
            otherDevice.setName("Other Device");
            otherDevice.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(otherDevice);

            // When
            List<String> result = deviceService.getMyDeviceIds();

            // Then
            assertThat(result).containsExactlyInAnyOrder("device-001", "device-002");
        }

        @Test
        @DisplayName("should return empty list when user has no devices")
        void shouldReturnEmptyListWhenNoDevices() {
            // When
            List<String> result = deviceService.getMyDeviceIds();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasAnyDevices")
    class HasAnyDevicesTests {

        @Test
        @DisplayName("should return true when user has devices")
        void shouldReturnTrueWhenHasDevices() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("My Device");
            device.setOwnerId(USER_ID);
            deviceRepository.save(device);

            // When
            boolean result = deviceService.hasAnyDevices();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when user has no devices")
        void shouldReturnFalseWhenNoDevices() {
            // When
            boolean result = deviceService.hasAnyDevices();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getMyDevice")
    class GetMyDeviceTests {

        @Test
        @DisplayName("should return device when owned by current user")
        void shouldReturnOwnedDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("My Device");
            device.setOwnerId(USER_ID);
            deviceRepository.save(device);

            // When
            Optional<Device> result = deviceService.getMyDevice("device-001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("device-001");
        }

        @Test
        @DisplayName("should return empty when device owned by another user")
        void shouldReturnEmptyForOtherUsersDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Other's Device");
            device.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(device);

            // When
            Optional<Device> result = deviceService.getMyDevice("device-001");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for non-existent device")
        void shouldReturnEmptyForNonExistentDevice() {
            // When
            Optional<Device> result = deviceService.getMyDevice("non-existent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when authService throws exception")
        void shouldReturnEmptyWhenAuthFails() {
            // Given
            when(authService.getCurrentUser()).thenThrow(new RuntimeException("Auth failed"));

            // When
            Optional<Device> result = deviceService.getMyDevice("device-001");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPrimaryDevice")
    class GetPrimaryDeviceTests {

        @Test
        @DisplayName("should return explicitly set primary device")
        void shouldReturnExplicitPrimaryDevice() {
            // Given
            Device device1 = new Device();
            device1.setId("device-001");
            device1.setName("Device 1");
            device1.setOwnerId(USER_ID);
            device1.setIsPrimary(false);
            deviceRepository.save(device1);

            Device device2 = new Device();
            device2.setId("device-002");
            device2.setName("Device 2");
            device2.setOwnerId(USER_ID);
            device2.setIsPrimary(true);
            deviceRepository.save(device2);

            // When
            Optional<Device> result = deviceService.getPrimaryDevice();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("device-002");
        }

        @Test
        @DisplayName("should fallback to first device with coordinates")
        void shouldFallbackToDeviceWithCoordinates() {
            // Given
            Device device1 = new Device();
            device1.setId("device-001");
            device1.setName("Device 1");
            device1.setOwnerId(USER_ID);
            deviceRepository.save(device1);

            Device device2 = new Device();
            device2.setId("device-002");
            device2.setName("Device 2");
            device2.setOwnerId(USER_ID);
            device2.setLatitude(52.5200);
            device2.setLongitude(13.4050);
            deviceRepository.save(device2);

            // When
            Optional<Device> result = deviceService.getPrimaryDevice();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo("device-002");
        }

        @Test
        @DisplayName("should fallback to first device when none have coordinates")
        void shouldFallbackToFirstDevice() {
            // Given
            Device device1 = new Device();
            device1.setId("device-001");
            device1.setName("Device 1");
            device1.setOwnerId(USER_ID);
            deviceRepository.save(device1);

            Device device2 = new Device();
            device2.setId("device-002");
            device2.setName("Device 2");
            device2.setOwnerId(USER_ID);
            deviceRepository.save(device2);

            // When
            Optional<Device> result = deviceService.getPrimaryDevice();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isIn("device-001", "device-002");
        }

        @Test
        @DisplayName("should return empty when user has no devices")
        void shouldReturnEmptyWhenNoDevices() {
            // When
            Optional<Device> result = deviceService.getPrimaryDevice();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("setPrimaryDevice")
    class SetPrimaryDeviceTests {

        @Test
        @DisplayName("should set device as primary")
        void shouldSetPrimaryDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("My Device");
            device.setOwnerId(USER_ID);
            device.setIsPrimary(false);
            deviceRepository.save(device);

            // When
            Device result = deviceService.setPrimaryDevice("device-001");

            // Then
            assertThat(result.getIsPrimary()).isTrue();

            Device fromDb = deviceRepository.findById("device-001").orElseThrow();
            assertThat(fromDb.getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("should unset previous primary device")
        void shouldUnsetPreviousPrimaryDevice() {
            // Given
            Device device1 = new Device();
            device1.setId("device-001");
            device1.setName("Device 1");
            device1.setOwnerId(USER_ID);
            device1.setIsPrimary(true);
            deviceRepository.save(device1);

            Device device2 = new Device();
            device2.setId("device-002");
            device2.setName("Device 2");
            device2.setOwnerId(USER_ID);
            device2.setIsPrimary(false);
            deviceRepository.save(device2);

            // When
            deviceService.setPrimaryDevice("device-002");

            // Then
            Device oldPrimary = deviceRepository.findById("device-001").orElseThrow();
            assertThat(oldPrimary.getIsPrimary()).isFalse();

            Device newPrimary = deviceRepository.findById("device-002").orElseThrow();
            assertThat(newPrimary.getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("should throw exception for non-existent device")
        void shouldThrowForNonExistentDevice() {
            // When/Then
            assertThatThrownBy(() -> deviceService.setPrimaryDevice("non-existent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found or not owned by user");
        }

        @Test
        @DisplayName("should throw exception for device owned by another user")
        void shouldThrowForOtherUsersDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Other's Device");
            device.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(device);

            // When/Then
            assertThatThrownBy(() -> deviceService.setPrimaryDevice("device-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found or not owned by user");
        }
    }

    @Nested
    @DisplayName("getVerifiedDevice")
    class GetVerifiedDeviceTests {

        @Test
        @DisplayName("should return device when owned by current user")
        void shouldReturnOwnedDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("My Device");
            device.setOwnerId(USER_ID);
            deviceRepository.save(device);

            // When
            Device result = deviceService.getVerifiedDevice("device-001");

            // Then
            assertThat(result.getId()).isEqualTo("device-001");
            assertThat(result.getOwnerId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw exception for non-existent device")
        void shouldThrowForNonExistentDevice() {
            // When/Then
            assertThatThrownBy(() -> deviceService.getVerifiedDevice("non-existent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found or not owned by user");
        }

        @Test
        @DisplayName("should throw exception for device owned by another user")
        void shouldThrowForOtherUsersDevice() {
            // Given
            Device device = new Device();
            device.setId("device-001");
            device.setName("Other's Device");
            device.setOwnerId(OTHER_USER_ID);
            deviceRepository.save(device);

            // When/Then
            assertThatThrownBy(() -> deviceService.getVerifiedDevice("device-001"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found or not owned by user");
        }
    }
}
