package dev.syslabs.sentio.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link LocationData} JPA entity.
 *
 * <p>
 * These tests verify the correct behavior of JPA lifecycle callback methods
 * {@code @PrePersist} and {@code @PreUpdate}.
 * </p>
 *
 * <p>
 * Because lifecycle callbacks are normally executed by the JPA provider,
 * they are invoked manually using reflection to keep the tests
 * database-independent and fast.
 * </p>
 */
class LocationDataTest {

    @Test
    void onCreate_setsCreatedAtAndUpdatedAt() throws Exception {
        // Arrange:
        // Create a new LocationData entity with required fields populated.
        LocationData location = new LocationData();
        location.setIpAddress("192.168.0.1");
        location.setCity("Berlin");
        location.setCountry("Germany");
        location.setRegion("Berlin");
        location.setLatitude(52.5200f);
        location.setLongitude(13.4050f);

        // Act:
        // Simulate the @PrePersist lifecycle callback.
        invokeLifecycle(location, "onCreate");

        // Assert:
        assertNotNull(location.getCreatedAt(),
                "createdAt should be initialized during entity creation");

        assertNotNull(location.getUpdatedAt(),
                "updatedAt should be initialized during entity creation");

        // Sanity check:
        // Both timestamps should be identical immediately after creation.
        assertEquals(location.getCreatedAt(), location.getUpdatedAt(),
                "createdAt and updatedAt should be equal on creation");
    }

    @Test
    void onUpdate_updatesUpdatedAt() throws Exception {
        // Arrange:
        LocationData location = new LocationData();
        location.setIpAddress("10.0.0.5");

        LocalDateTime oldUpdatedAt = LocalDateTime.of(2023, 5, 1, 12, 0);
        location.setUpdatedAt(oldUpdatedAt);

        // Act:
        // Simulate the @PreUpdate lifecycle callback.
        invokeLifecycle(location, "onUpdate");

        // Assert:
        assertNotNull(location.getUpdatedAt(),
                "updatedAt should not be null after update");

        assertTrue(location.getUpdatedAt().isAfter(oldUpdatedAt),
                "updatedAt should be updated to a more recent timestamp");
    }

    private static void invokeLifecycle(LocationData target, String methodName) throws Exception {
        Method method = LocationData.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}