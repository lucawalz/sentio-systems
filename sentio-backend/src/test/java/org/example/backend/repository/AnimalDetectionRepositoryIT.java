package org.example.backend.repository;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.AnimalDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AnimalDetectionRepository}.
 * Validates custom JPQL and native queries with PostgreSQL.
 */
class AnimalDetectionRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private AnimalDetectionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private AnimalDetection createDetection(String species, String animalType, String deviceId, float confidence,
            LocalDateTime timestamp) {
        AnimalDetection detection = new AnimalDetection();
        detection.setSpecies(species);
        detection.setAnimalType(animalType);
        detection.setDeviceId(deviceId);
        detection.setConfidence(confidence);
        detection.setTimestamp(timestamp);
        detection.setX(0.1f);
        detection.setY(0.1f);
        detection.setWidth(0.5f);
        detection.setHeight(0.5f);
        return detection;
    }

    @Nested
    @DisplayName("Native query - findTopNByOrderByTimestampDesc")
    class NativeQueryTests {

        @Test
        @DisplayName("should return limited results with native LIMIT")
        void shouldLimitResults() {
            // Given
            for (int i = 0; i < 5; i++) {
                repository.save(
                        createDetection("Sparrow", "bird", "device-001", 0.9f, LocalDateTime.now().minusMinutes(i)));
            }

            // When
            List<AnimalDetection> result = repository.findTopNByOrderByTimestampDesc(3);

            // Then
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Device filtering queries")
    class DeviceFilteringTests {

        @Test
        @DisplayName("should filter by device IDs")
        void shouldFilterByDeviceIds() {
            // Given
            repository.save(createDetection("Sparrow", "bird", "device-001", 0.9f, LocalDateTime.now()));
            repository.save(createDetection("Robin", "bird", "device-002", 0.85f, LocalDateTime.now()));
            repository.save(createDetection("Fox", "mammal", "device-003", 0.75f, LocalDateTime.now()));

            // When
            List<AnimalDetection> result = repository.findRecentDetectionsByDeviceIds(
                    List.of("device-001", "device-002"),
                    LocalDateTime.now().minusHours(1));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AnimalDetection::getDeviceId)
                    .containsExactlyInAnyOrder("device-001", "device-002");
        }
    }

    @Nested
    @DisplayName("Aggregate queries")
    class AggregateQueryTests {

        @Test
        @DisplayName("should count unique species")
        void shouldCountUniqueSpecies() {
            // Given
            repository.save(createDetection("Sparrow", "bird", "device-001", 0.9f, LocalDateTime.now()));
            repository.save(createDetection("Sparrow", "bird", "device-002", 0.85f, LocalDateTime.now()));
            repository.save(createDetection("Robin", "bird", "device-001", 0.8f, LocalDateTime.now()));

            // When
            long uniqueCount = repository.countUniqueSpeciesSince(LocalDateTime.now().minusHours(1));

            // Then
            assertThat(uniqueCount).isEqualTo(2);
        }

        @Test
        @DisplayName("should calculate average confidence")
        void shouldCalculateAverageConfidence() {
            // Given
            repository.save(createDetection("Sparrow", "bird", "device-001", 0.8f, LocalDateTime.now()));
            repository.save(createDetection("Robin", "bird", "device-002", 0.9f, LocalDateTime.now()));

            // When
            Double avgConfidence = repository.findAverageConfidenceSince(LocalDateTime.now().minusHours(1));

            // Then
            assertThat(avgConfidence).isCloseTo(0.85, org.assertj.core.data.Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("Animal type filtering")
    class AnimalTypeTests {

        @Test
        @DisplayName("should find bird detections only")
        void shouldFindBirdDetections() {
            // Given
            repository.save(createDetection("Sparrow", "bird", "device-001", 0.9f, LocalDateTime.now()));
            repository.save(createDetection("Fox", "mammal", "device-002", 0.85f, LocalDateTime.now()));

            // When
            List<AnimalDetection> birds = repository.findBirdDetections(LocalDateTime.now().minusHours(1));

            // Then
            assertThat(birds).hasSize(1);
            assertThat(birds.get(0).getAnimalType()).isEqualTo("bird");
        }

        @Test
        @DisplayName("should find mammal detections only")
        void shouldFindMammalDetections() {
            // Given
            repository.save(createDetection("Sparrow", "bird", "device-001", 0.9f, LocalDateTime.now()));
            repository.save(createDetection("Fox", "mammal", "device-002", 0.85f, LocalDateTime.now()));

            // When
            List<AnimalDetection> mammals = repository.findMammalDetections(LocalDateTime.now().minusHours(1));

            // Then
            assertThat(mammals).hasSize(1);
            assertThat(mammals.get(0).getAnimalType()).isEqualTo("mammal");
        }
    }

    @Nested
    @DisplayName("High confidence filtering")
    class ConfidenceFilteringTests {

        @Test
        @DisplayName("should filter by minimum confidence")
        void shouldFilterByMinConfidence() {
            // Given
            repository.save(createDetection("Sparrow", "bird", "device-001", 0.95f, LocalDateTime.now()));
            repository.save(createDetection("Robin", "bird", "device-002", 0.7f, LocalDateTime.now()));

            // When
            List<AnimalDetection> result = repository.findHighConfidenceDetections(0.9f,
                    LocalDateTime.now().minusHours(1));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getConfidence()).isGreaterThanOrEqualTo(0.9f);
        }
    }
}
