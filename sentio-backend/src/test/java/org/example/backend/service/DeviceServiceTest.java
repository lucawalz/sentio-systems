package org.example.backend.service;

import org.example.backend.dto.AuthDTOs;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeviceService} with single-owner model.
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AuthService authService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeviceService deviceService;

    private AuthDTOs.UserInfo testUser;

    private static final String TEST_USER_ID = "test-user-uuid-123";
    private static final String TEST_USERNAME = "testuser";
    private static final String OTHER_USER_ID = "other-user-uuid-456";

    @BeforeEach
    void setUp() {
        testUser = new AuthDTOs.UserInfo(TEST_USER_ID, TEST_USERNAME, "test@example.com", List.of("user"));
    }

    @Nested
    @DisplayName("registerDevice")
    class RegisterDeviceTests {

        @Test
        @DisplayName("should create new device when it does not exist")
        void shouldCreateNewDeviceWhenNotExists() {
            // Given
            String deviceId = "device-123";
            String deviceName = "My Raspberry Pi";

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());
            when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Device result = deviceService.registerDevice(deviceId, deviceName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(deviceId);
            assertThat(result.getName()).isEqualTo(deviceName);
            assertThat(result.getOwnerId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("should update name when device already belongs to user")
        void shouldUpdateNameWhenDeviceAlreadyBelongsToUser() {
            // Given
            String deviceId = "device-123";
            String deviceName = "Updated Name";

            Device existingDevice = new Device();
            existingDevice.setId(deviceId);
            existingDevice.setName("Old Name");
            existingDevice.setOwnerId(TEST_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));
            when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Device result = deviceService.registerDevice(deviceId, deviceName);

            // Then
            assertThat(result.getOwnerId()).isEqualTo(TEST_USER_ID);
            assertThat(result.getName()).isEqualTo(deviceName);
        }

        @Test
        @DisplayName("should throw exception when device belongs to another user")
        void shouldThrowWhenDeviceBelongsToAnotherUser() {
            // Given
            String deviceId = "device-123";

            Device existingDevice = new Device();
            existingDevice.setId(deviceId);
            existingDevice.setOwnerId(OTHER_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));

            // When/Then
            assertThatThrownBy(() -> deviceService.registerDevice(deviceId, "My Device"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already belongs to another user");
        }

        @Test
        @DisplayName("should set createdAt timestamp for new device")
        void shouldSetCreatedAtForNewDevice() {
            // Given
            String deviceId = "device-new";
            LocalDateTime beforeTest = LocalDateTime.now();

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());
            when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Device result = deviceService.registerDevice(deviceId, "Test Device");

            // Then
            assertThat(result.getCreatedAt()).isAfterOrEqualTo(beforeTest);
        }
    }

    @Nested
    @DisplayName("unregisterDevice")
    class UnregisterDeviceTests {

        @Test
        @DisplayName("should delete device when user owns it")
        void shouldDeleteDeviceWhenUserOwnsIt() {
            // Given
            String deviceId = "device-123";

            Device device = new Device();
            device.setId(deviceId);
            device.setOwnerId(TEST_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // When
            deviceService.unregisterDevice(deviceId);

            // Then
            verify(deviceRepository).delete(device);
        }

        @Test
        @DisplayName("should not delete device when user does not own it")
        void shouldNotDeleteDeviceWhenUserDoesNotOwnIt() {
            // Given
            String deviceId = "device-123";

            Device device = new Device();
            device.setId(deviceId);
            device.setOwnerId(OTHER_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // When
            deviceService.unregisterDevice(deviceId);

            // Then
            verify(deviceRepository, never()).delete(any(Device.class));
        }

        @Test
        @DisplayName("should do nothing when device not found")
        void shouldDoNothingWhenDeviceNotFound() {
            // Given
            String deviceId = "nonexistent-device";

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

            // When
            deviceService.unregisterDevice(deviceId);

            // Then
            verify(deviceRepository, never()).delete(any(Device.class));
        }
    }

    @Nested
    @DisplayName("getMyDevices")
    class GetMyDevicesTests {

        @Test
        @DisplayName("should return devices owned by current user")
        void shouldReturnDevicesOwnedByCurrentUser() {
            // Given
            Device device1 = new Device();
            device1.setId("device-1");
            device1.setName("Device 1");
            device1.setOwnerId(TEST_USER_ID);

            Device device2 = new Device();
            device2.setId("device-2");
            device2.setName("Device 2");
            device2.setOwnerId(TEST_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findAllByOwnerId(TEST_USER_ID)).thenReturn(List.of(device1, device2));

            // When
            List<Device> result = deviceService.getMyDevices();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Device::getId)
                    .containsExactly("device-1", "device-2");
        }

        @Test
        @DisplayName("should return empty list when user has no devices")
        void shouldReturnEmptyListWhenNoDevices() {
            // Given
            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findAllByOwnerId(TEST_USER_ID)).thenReturn(List.of());

            // When
            List<Device> result = deviceService.getMyDevices();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasAccessToDevice")
    class HasAccessToDeviceTests {

        @Test
        @DisplayName("should return true when user owns device")
        void shouldReturnTrueWhenUserOwnsDevice() {
            // Given
            String deviceId = "device-123";

            Device device = new Device();
            device.setId(deviceId);
            device.setOwnerId(TEST_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // When
            boolean result = deviceService.hasAccessToDevice(deviceId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when user does not own device")
        void shouldReturnFalseWhenUserDoesNotOwnDevice() {
            // Given
            String deviceId = "device-123";

            Device device = new Device();
            device.setId(deviceId);
            device.setOwnerId(OTHER_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));

            // When
            boolean result = deviceService.hasAccessToDevice(deviceId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when device does not exist")
        void shouldReturnFalseWhenDeviceNotFound() {
            // Given
            String deviceId = "nonexistent";

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

            // When
            boolean result = deviceService.hasAccessToDevice(deviceId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when deviceId is null")
        void shouldReturnFalseWhenDeviceIdIsNull() {
            // When
            boolean result = deviceService.hasAccessToDevice(null);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when no authenticated user")
        void shouldReturnFalseWhenNoAuthenticatedUser() {
            // Given
            String deviceId = "device-123";
            when(authService.getCurrentUser()).thenThrow(new RuntimeException("No authenticated user"));

            // When
            boolean result = deviceService.hasAccessToDevice(deviceId);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getMyDeviceIds")
    class GetMyDeviceIdsTests {

        @Test
        @DisplayName("should return list of device IDs")
        void shouldReturnListOfDeviceIds() {
            // Given
            Device device1 = new Device();
            device1.setId("device-1");
            device1.setOwnerId(TEST_USER_ID);

            Device device2 = new Device();
            device2.setId("device-2");
            device2.setOwnerId(TEST_USER_ID);

            when(authService.getCurrentUser()).thenReturn(testUser);
            when(deviceRepository.findAllByOwnerId(TEST_USER_ID)).thenReturn(List.of(device1, device2));

            // When
            List<String> result = deviceService.getMyDeviceIds();

            // Then
            assertThat(result).containsExactly("device-1", "device-2");
        }
    }
}
