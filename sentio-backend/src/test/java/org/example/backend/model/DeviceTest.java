package org.example.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Device Model")
class DeviceTest {

    @Test
    @DisplayName("Device should initialize with default values")
    void device_shouldInitializeWithDefaults() {
        // Act
        Device device = new Device();

        // Assert
        assertThat(device.getActiveServices()).isNotNull();
        assertThat(device.getActiveServices()).isEmpty();
        assertThat(device.getIsPrimary()).isFalse();
        assertThat(device.getStreamActive()).isFalse();
    }

    @Test
    @DisplayName("Device constructor should accept all parameters")
    void device_constructorWithAllParameters_setsFieldsCorrectly() {
        // Arrange
        String id = "device-123";
        String name = "Test Device";
        String ownerId = "owner-456";
        Set<String> services = Set.of("weather_station", "animal_detector");
        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 1, 10, 0);
        String ipAddress = "192.168.1.100";
        Double latitude = 52.52;
        Double longitude = 13.41;
        Boolean isPrimary = true;
        LocalDateTime lastSeen = LocalDateTime.of(2025, 1, 2, 10, 0);
        String mqttTokenHash = "hashed_token";
        String pairingCode = "1234-5678";
        LocalDateTime pairingExpiresAt = LocalDateTime.of(2025, 1, 1, 10, 15);
        Boolean streamActive = true;

        // Act
        Device device = new Device(
                id, name, ownerId, services, createdAt, ipAddress,
                latitude, longitude, isPrimary, lastSeen, mqttTokenHash,
                pairingCode, pairingExpiresAt, streamActive
        );

        // Assert
        assertThat(device.getId()).isEqualTo(id);
        assertThat(device.getName()).isEqualTo(name);
        assertThat(device.getOwnerId()).isEqualTo(ownerId);
        assertThat(device.getActiveServices()).hasSize(2);
        assertThat(device.getActiveServices()).contains("weather_station", "animal_detector");
        assertThat(device.getCreatedAt()).isEqualTo(createdAt);
        assertThat(device.getIpAddress()).isEqualTo(ipAddress);
        assertThat(device.getLatitude()).isEqualTo(latitude);
        assertThat(device.getLongitude()).isEqualTo(longitude);
        assertThat(device.getIsPrimary()).isTrue();
        assertThat(device.getLastSeen()).isEqualTo(lastSeen);
        assertThat(device.getMqttTokenHash()).isEqualTo(mqttTokenHash);
        assertThat(device.getPairingCode()).isEqualTo(pairingCode);
        assertThat(device.getPairingCodeExpiresAt()).isEqualTo(pairingExpiresAt);
        assertThat(device.getStreamActive()).isTrue();
    }

    @Test
    @DisplayName("@PrePersist onCreate should set createdAt when null")
    void prePersist_onCreate_setsCreatedAtWhenNull() throws Exception {
        // Arrange
        Device device = new Device();
        device.setCreatedAt(null);

        // Get the @PrePersist method via reflection
        Method onCreateMethod = Device.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);

        // Act
        onCreateMethod.invoke(device);

        // Assert
        assertThat(device.getCreatedAt()).isNotNull();
        assertThat(device.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("@PrePersist onCreate should not overwrite existing createdAt")
    void prePersist_onCreate_doesNotOverwriteExistingCreatedAt() throws Exception {
        // Arrange
        LocalDateTime existingCreatedAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        Device device = new Device();
        device.setCreatedAt(existingCreatedAt);

        // Get the @PrePersist method via reflection
        Method onCreateMethod = Device.class.getDeclaredMethod("onCreate");
        onCreateMethod.setAccessible(true);

        // Act
        onCreateMethod.invoke(device);

        // Assert
        assertThat(device.getCreatedAt()).isEqualTo(existingCreatedAt);
    }

    @Test
    @DisplayName("Device should allow setting and getting all fields")
    void device_settersAndGetters_workCorrectly() {
        // Arrange
        Device device = new Device();
        LocalDateTime now = LocalDateTime.now();

        // Act
        device.setId("dev-999");
        device.setName("My Device");
        device.setOwnerId("owner-999");
        device.setActiveServices(Set.of("service1", "service2"));
        device.setCreatedAt(now);
        device.setIpAddress("10.0.0.1");
        device.setLatitude(51.5074);
        device.setLongitude(-0.1278);
        device.setIsPrimary(true);
        device.setLastSeen(now);
        device.setMqttTokenHash("hash123");
        device.setPairingCode("ABCD-1234");
        device.setPairingCodeExpiresAt(now.plusMinutes(15));
        device.setStreamActive(true);

        // Assert
        assertThat(device.getId()).isEqualTo("dev-999");
        assertThat(device.getName()).isEqualTo("My Device");
        assertThat(device.getOwnerId()).isEqualTo("owner-999");
        assertThat(device.getActiveServices()).containsExactlyInAnyOrder("service1", "service2");
        assertThat(device.getCreatedAt()).isEqualTo(now);
        assertThat(device.getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(device.getLatitude()).isEqualTo(51.5074);
        assertThat(device.getLongitude()).isEqualTo(-0.1278);
        assertThat(device.getIsPrimary()).isTrue();
        assertThat(device.getLastSeen()).isEqualTo(now);
        assertThat(device.getMqttTokenHash()).isEqualTo("hash123");
        assertThat(device.getPairingCode()).isEqualTo("ABCD-1234");
        assertThat(device.getPairingCodeExpiresAt()).isEqualTo(now.plusMinutes(15));
        assertThat(device.getStreamActive()).isTrue();
    }

    @Test
    @DisplayName("Device should handle null GPS coordinates")
    void device_handleNullGpsCoordinates() {
        // Arrange
        Device device = new Device();

        // Act
        device.setLatitude(null);
        device.setLongitude(null);

        // Assert
        assertThat(device.getLatitude()).isNull();
        assertThat(device.getLongitude()).isNull();
    }

    @Test
    @DisplayName("Device should handle modifying activeServices set")
    void device_activeServices_isModifiable() {
        // Arrange
        Device device = new Device();
        device.setActiveServices(new HashSet<>());

        // Act
        device.getActiveServices().add("weather_station");
        device.getActiveServices().add("animal_detector");
        device.getActiveServices().remove("weather_station");

        // Assert
        assertThat(device.getActiveServices()).hasSize(1);
        assertThat(device.getActiveServices()).containsExactly("animal_detector");
    }

    @Test
    @DisplayName("Device should handle pairing code lifecycle")
    void device_pairingCode_lifecycle() {
        // Arrange
        Device device = new Device();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // Act - Set pairing code
        device.setPairingCode("1111-2222");
        device.setPairingCodeExpiresAt(expiresAt);

        // Assert - Pairing code set
        assertThat(device.getPairingCode()).isEqualTo("1111-2222");
        assertThat(device.getPairingCodeExpiresAt()).isEqualTo(expiresAt);

        // Act - Clear pairing code after use
        device.setPairingCode(null);
        device.setPairingCodeExpiresAt(null);

        // Assert - Pairing code cleared
        assertThat(device.getPairingCode()).isNull();
        assertThat(device.getPairingCodeExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Device equality should work with Lombok @Data")
    void device_equality_worksWithLombok() {
        // Arrange
        LocalDateTime fixedTime = LocalDateTime.of(2025, 1, 1, 10, 0);

        Device device1 = new Device();
        device1.setId("dev-123");
        device1.setName("Device 1");
        device1.setOwnerId("owner-1");
        device1.setCreatedAt(fixedTime);

        Device device2 = new Device();
        device2.setId("dev-123");
        device2.setName("Device 1");
        device2.setOwnerId("owner-1");
        device2.setCreatedAt(fixedTime);

        Device device3 = new Device();
        device3.setId("dev-456");
        device3.setName("Device 2");
        device3.setOwnerId("owner-2");
        device3.setCreatedAt(fixedTime);

        // Assert
        assertThat(device1).isEqualTo(device2);
        assertThat(device1).isNotEqualTo(device3);
        assertThat(device1.hashCode()).isEqualTo(device2.hashCode());
    }
}
