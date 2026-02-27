package org.example.backend.repository;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DeviceRepository}.
 */
@DisplayName("DeviceRepository")
class DeviceRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private DeviceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private Device createDevice(String id, String ownerId, String name, boolean isPrimary) {
        Device device = new Device();
        device.setId(id);
        device.setOwnerId(ownerId);
        device.setName(name);
        device.setIsPrimary(isPrimary);
        return device;
    }

    @Test
    @DisplayName("should find all devices by owner ID")
    void shouldFindAllByOwnerId() {
        // Given
        repository.save(createDevice("device-001", "user-1", "Device 1", true));
        repository.save(createDevice("device-002", "user-1", "Device 2", false));
        repository.save(createDevice("device-003", "user-2", "Device 3", true));

        // When
        List<Device> user1Devices = repository.findAllByOwnerId("user-1");
        List<Device> user2Devices = repository.findAllByOwnerId("user-2");

        // Then
        assertThat(user1Devices).hasSize(2);
        assertThat(user1Devices).extracting(Device::getId)
                .containsExactlyInAnyOrder("device-001", "device-002");

        assertThat(user2Devices).hasSize(1);
        assertThat(user2Devices).extracting(Device::getId).containsExactly("device-003");
    }

    @Test
    @DisplayName("should find primary device by owner ID")
    void shouldFindPrimaryDeviceByOwnerId() {
        // Given
        repository.save(createDevice("device-001", "user-1", "Device 1", true));
        repository.save(createDevice("device-002", "user-1", "Device 2", false));

        // When
        Optional<Device> primaryDevice = repository.findByOwnerIdAndIsPrimaryTrue("user-1");

        // Then
        assertThat(primaryDevice).isPresent();
        assertThat(primaryDevice.get().getId()).isEqualTo("device-001");
        assertThat(primaryDevice.get().getIsPrimary()).isTrue();
    }

    @Test
    @DisplayName("should return empty when no primary device exists")
    void shouldReturnEmptyWhenNoPrimaryDevice() {
        // Given
        repository.save(createDevice("device-001", "user-1", "Device 1", false));
        repository.save(createDevice("device-002", "user-1", "Device 2", false));

        // When
        Optional<Device> primaryDevice = repository.findByOwnerIdAndIsPrimaryTrue("user-1");

        // Then
        assertThat(primaryDevice).isEmpty();
    }

    @Test
    @DisplayName("should find device by ID")
    void shouldFindDeviceById() {
        // Given
        repository.save(createDevice("device-123", "user-1", "Test Device", false));

        // When
        Optional<Device> foundDevice = repository.findById("device-123");

        // Then
        assertThat(foundDevice).isPresent();
        assertThat(foundDevice.get().getId()).isEqualTo("device-123");
        assertThat(foundDevice.get().getName()).isEqualTo("Test Device");
    }

    @Test
    @DisplayName("should return empty list when owner has no devices")
    void shouldReturnEmptyListWhenNoDevices() {
        // When
        List<Device> devices = repository.findAllByOwnerId("non-existent-user");

        // Then
        assertThat(devices).isEmpty();
    }

    @Test
    @DisplayName("should handle multiple users with primary devices")
    void shouldHandleMultipleUsersWithPrimaryDevices() {
        // Given
        repository.save(createDevice("device-001", "user-1", "User 1 Primary", true));
        repository.save(createDevice("device-002", "user-1", "User 1 Secondary", false));
        repository.save(createDevice("device-003", "user-2", "User 2 Primary", true));

        // When
        Optional<Device> user1Primary = repository.findByOwnerIdAndIsPrimaryTrue("user-1");
        Optional<Device> user2Primary = repository.findByOwnerIdAndIsPrimaryTrue("user-2");

        // Then
        assertThat(user1Primary).isPresent();
        assertThat(user1Primary.get().getId()).isEqualTo("device-001");

        assertThat(user2Primary).isPresent();
        assertThat(user2Primary.get().getId()).isEqualTo("device-003");
    }
}
