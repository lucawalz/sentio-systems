package org.example.backend.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link AnimalDetection} JPA entity.
 *
 * <p>
 * These tests validate:
 * <ul>
 *   <li>Utility methods for animal type checks ({@code isBird}, {@code isMammal})</li>
 *   <li>JPA lifecycle callback behavior ({@code @PrePersist})</li>
 *   <li>Correct handling of timestamp initialization</li>
 * </ul>
 * </p>
 *
 * <p>
 * Lifecycle callbacks are triggered manually via reflection,
 * because they are normally executed by the JPA provider.
 * This keeps the tests lightweight and database-independent.
 * </p>
 */
class AnimalDetectionTest {
    @Test
    void isBird_returnsTrueForBirdType_caseInsensitive() {
        // Arrange:
        // Create a detection with animalType "bird"
        AnimalDetection detection = new AnimalDetection();
        detection.setAnimalType("BiRd");

        // Act & Assert:
        assertTrue(detection.isBird(),
                "isBird() should return true when animalType is 'bird' (case-insensitive)");
    }

    @Test
    void isBird_returnsFalseForNonBirdType() {
        // Arrange:
        AnimalDetection detection = new AnimalDetection();
        detection.setAnimalType("mammal");

        // Act & Assert:
        assertFalse(detection.isBird(),
                "isBird() should return false when animalType is not 'bird'");
    }

    @Test
    void isMammal_returnsTrueForMammalType_caseInsensitive() {
        // Arrange:
        AnimalDetection detection = new AnimalDetection();
        detection.setAnimalType("MAMMAL");

        // Act & Assert:
        assertTrue(detection.isMammal(),
                "isMammal() should return true when animalType is 'mammal' (case-insensitive)");
    }

    @Test
    void isMammal_returnsFalseForNonMammalType() {
        // Arrange:
        AnimalDetection detection = new AnimalDetection();
        detection.setAnimalType("bird");

        // Act & Assert:
        assertFalse(detection.isMammal(),
                "isMammal() should return false when animalType is not 'mammal'");
    }

    @Test
    void onCreate_setsTimestampAndProcessedAtIfNull() throws Exception {
        // Arrange:
        // Create a new entity with missing timestamps
        AnimalDetection detection = new AnimalDetection();
        detection.setAnimalType("bird");
        detection.setSpecies("sparrow");
        detection.setConfidence(0.92f);

        detection.setTimestamp(null);
        detection.setProcessedAt(null);

        // Act:
        // Simulate the @PrePersist lifecycle callback
        invokeLifecycle(detection, "onCreate");

        // Assert:
        // Both timestamps must be initialized
        assertNotNull(detection.getTimestamp(),
                "timestamp should be initialized if it is null during creation");

        assertNotNull(detection.getProcessedAt(),
                "processedAt should be initialized if it is null during creation");
    }

    @Test
    void onCreate_doesNotOverrideExistingTimestamps() throws Exception {
        // Arrange:
        LocalDateTime fixedTime = LocalDateTime.of(2021, 6, 1, 10, 0);

        AnimalDetection detection = new AnimalDetection();
        detection.setAnimalType("mammal");
        detection.setSpecies("fox");
        detection.setConfidence(0.88f);

        detection.setTimestamp(fixedTime);
        detection.setProcessedAt(fixedTime);

        // Act:
        invokeLifecycle(detection, "onCreate");

        // Assert:
        assertEquals(fixedTime, detection.getTimestamp(),
                "timestamp must not be overridden if already set");

        assertEquals(fixedTime, detection.getProcessedAt(),
                "processedAt must not be overridden if already set");
    }

    private static void invokeLifecycle(AnimalDetection target, String methodName) throws Exception {
        Method method = AnimalDetection.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target);
    }
}