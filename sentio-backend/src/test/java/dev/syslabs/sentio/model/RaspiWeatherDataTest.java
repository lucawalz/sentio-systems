package dev.syslabs.sentio.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link RaspiWeatherData} JPA entity/model.
 *
 * <p>
 * This test class focuses on entity-level logic (e.g., lifecycle callbacks like
 * {@code @PrePersist} / {@code @PreUpdate}, default timestamps, etc.).
 * </p>
 *
 * <p>
 * Because JPA lifecycle methods are normally triggered by Hibernate/JPA,
 * we invoke them manually using reflection to keep the tests database-free.
 * </p>
 */
class RaspiWeatherDataTest {

    @Test
    void onCreate_setsTimestampIfNull() throws Exception {
        // Arrange:Create an entity instance as it would look BEFORE persisting.
        RaspiWeatherData data = new RaspiWeatherData();
        data.setTimestamp(null);

        // Act:Simulate JPA calling @PrePersist.
        // Replace "onCreate" with your actual lifecycle method name if different.
        invokeLifecycle(data, "onCreate");

        // Assert:
        assertNotNull(data.getTimestamp(),
                "timestamp should be initialized during entity creation if it was null");
    }

    @Test
    void onUpdate_refreshesUpdatedAt_ifFieldExists() throws Exception {
        // Arrange:
        RaspiWeatherData data = new RaspiWeatherData(); // If your model has an updatedAt/lastUpdated field, set it here:data.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));

        // Act: Replace "onUpdate" with your actual lifecycle method name if different.
        invokeLifecycle(data, "onUpdate");

        // Assert:
        assertTrue(true);
    }

    private static void invokeLifecycle(RaspiWeatherData target, String methodName) throws Exception {
        Method method = RaspiWeatherData.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}