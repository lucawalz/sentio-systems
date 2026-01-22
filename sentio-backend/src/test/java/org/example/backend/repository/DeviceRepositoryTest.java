package org.example.backend.repository;

import org.example.backend.model.Device;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private DeviceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testFindAllByOwnerId() {
        repository.save(createDevice("d1", "user1", true));
        repository.save(createDevice("d2", "user1", false));
        repository.save(createDevice("d3", "user2", true));

        List<Device> user1Devices = repository.findAllByOwnerId("user1");

        assertThat(user1Devices).hasSize(2);
        assertThat(user1Devices).extracting(Device::getId).containsExactlyInAnyOrder("d1", "d2");
    }

    @Test
    void testFindByOwnerIdAndIsPrimaryTrue() {
        repository.save(createDevice("d1", "user1", true));
        repository.save(createDevice("d2", "user1", false));

        Optional<Device> primary = repository.findByOwnerIdAndIsPrimaryTrue("user1");

        assertThat(primary).isPresent();
        assertThat(primary.get().getId()).isEqualTo("d1");
    }

    @Test
    void testFindByOwnerIdAndIsPrimaryTrue_NoneFound() {
        repository.save(createDevice("d2", "user1", false));

        Optional<Device> primary = repository.findByOwnerIdAndIsPrimaryTrue("user1");

        assertThat(primary).isEmpty();
    }

    private Device createDevice(String id, String ownerId, boolean isPrimary) {
        Device device = new Device();
        device.setId(id);
        device.setName("Test Device " + id);
        device.setOwnerId(ownerId);
        device.setIsPrimary(isPrimary);
        // Nullable fields safe to ignore for minimal test
        return device;
    }
}
