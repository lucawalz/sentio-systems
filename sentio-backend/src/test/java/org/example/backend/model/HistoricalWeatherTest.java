package org.example.backend.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link HistoricalWeather} JPA entity.
 *
 * <p>
 * These tests verify the correct behavior of JPA lifecycle callback methods
 * {@code @PrePersist} and {@code @PreUpdate}.
 * </p>
 *
 * <p>
 * Since lifecycle callbacks are normally invoked by the JPA provider
 * (e.g. Hibernate), we trigger them manually using reflection.
 * This allows us to keep the tests lightweight and independent
 * of any database or JPA configuration.
 * </p>
 */

class HistoricalWeatherTest {

    @Test
    void onCreate_setsCreatedAtAndUpdatedAt() throws Exception {
        // Arrange:
        // Create a new HistoricalWeather entity with a valid weather date.
        HistoricalWeather weather = new HistoricalWeather();
        weather.setWeatherDate(LocalDate.of(2024, 1, 15));

        // Act:
        // Simulate the @PrePersist lifecycle callback.
        invokeLifecycle(weather, "onCreate");

        // Assert:
        assertNotNull(weather.getCreatedAt(),
                "createdAt should be initialized during entity creation");

        assertNotNull(weather.getUpdatedAt(),
                "updatedAt should be initialized during entity creation");

        // Sanity check:
        // Both timestamps should be identical immediately after creation.
        assertEquals(weather.getCreatedAt(), weather.getUpdatedAt(),
                "createdAt and updatedAt should be equal right after creation");
    }

    @Test
    void onUpdate_updatesUpdatedAt() throws Exception {
        // Arrange:
        HistoricalWeather weather = new HistoricalWeather();
        weather.setWeatherDate(LocalDate.of(2023, 8, 1));

        LocalDateTime oldUpdatedAt = LocalDateTime.of(2023, 8, 2, 10, 0);
        weather.setUpdatedAt(oldUpdatedAt);

        // Act:
        // Simulate the @PreUpdate lifecycle callback.
        invokeLifecycle(weather, "onUpdate");

        // Assert:
        assertNotNull(weather.getUpdatedAt(),
                "updatedAt should not be null after update");

        assertTrue(weather.getUpdatedAt().isAfter(oldUpdatedAt),
                "updatedAt should be updated to a more recent timestamp");
    }

    private static void invokeLifecycle(HistoricalWeather target, String methodName) throws Exception {
        Method method = HistoricalWeather.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}