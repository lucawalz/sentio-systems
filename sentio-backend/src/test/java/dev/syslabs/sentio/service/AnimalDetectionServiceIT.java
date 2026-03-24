package dev.syslabs.sentio.service;

import dev.syslabs.sentio.BaseIntegrationTest;
import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.repository.AnimalDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Animal Detection services (CQRS pattern).
 * Tests both QueryService (reads) and CommandService (writes).
 */
class AnimalDetectionServiceIT extends BaseIntegrationTest {

    @Autowired
    private AnimalDetectionQueryService queryService;

    @Autowired
    private AnimalDetectionCommandService commandService;

    @Autowired
    private AnimalDetectionRepository repository;

    @MockitoBean
    private DeviceService deviceService;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
        // Mock device service to return test devices for authenticated user
        when(deviceService.getMyDeviceIds()).thenReturn(List.of("test-device-1", "device-A", "device-B"));
        when(deviceService.hasAccessToDevice(anyString())).thenReturn(true);
    }

    private AnimalDetection createDetection(String species, String animalType, float confidence) {
        AnimalDetection detection = new AnimalDetection();
        detection.setSpecies(species);
        detection.setAnimalType(animalType);
        detection.setConfidence(confidence);
        detection.setTimestamp(LocalDateTime.now());
        detection.setDeviceId("test-device-1");
        detection.setX(0.0f);
        detection.setY(0.0f);
        detection.setWidth(100.0f);
        detection.setHeight(100.0f);
        return detection;
    }

    @Nested
    @DisplayName("CommandService: saveAnimalDetection")
    class SaveAnimalDetectionTests {

        @Test
        @DisplayName("should persist detection with generated ID")
        void shouldPersistDetection() {
            AnimalDetection detection = createDetection("Robin", "bird", 0.95f);

            AnimalDetection saved = commandService.saveAnimalDetection(detection);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getSpecies()).isEqualTo("Robin");
            assertThat(saved.getAnimalType()).isEqualTo("bird");
            assertThat(saved.getConfidence()).isEqualTo(0.95f);
        }

        @Test
        @DisplayName("should set timestamps on save")
        void shouldSetTimestamps() {
            AnimalDetection detection = createDetection("Fox", "mammal", 0.88f);

            AnimalDetection saved = commandService.saveAnimalDetection(detection);

            assertThat(saved.getProcessedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("CommandService: deleteDetection")
    @WithMockUser(username = "test-user")
    class DeleteDetectionTests {

        @Test
        @DisplayName("should return false for non-existent detection")
        void shouldReturnFalseForNonExistent() {
            boolean result = commandService.deleteDetection(99999L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("QueryService: getLatestDetections")
    @WithMockUser(username = "test-user")
    class GetLatestDetectionsTests {

        @Test
        @DisplayName("should return empty list when no detections")
        void shouldReturnEmptyWhenNoDetections() {
            List<AnimalDetection> results = queryService.getLatestDetections(10);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should return detections ordered by most recent")
        void shouldReturnOrderedByRecent() {
            AnimalDetection older = createDetection("Sparrow", "bird", 0.85f);
            older.setTimestamp(LocalDateTime.now().minusHours(2));
            repository.save(older);

            AnimalDetection newer = createDetection("Robin", "bird", 0.90f);
            newer.setTimestamp(LocalDateTime.now());
            repository.save(newer);

            List<AnimalDetection> results = queryService.getLatestDetections(10);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getSpecies()).isEqualTo("Robin");
        }

        @Test
        @DisplayName("should respect limit parameter")
        void shouldRespectLimit() {
            for (int i = 0; i < 5; i++) {
                repository.save(createDetection("Bird" + i, "bird", 0.80f));
            }

            List<AnimalDetection> results = queryService.getLatestDetections(3);

            assertThat(results).hasSize(3);
        }
    }

    @Nested
    @DisplayName("QueryService: getRecentDetections")
    @WithMockUser(username = "test-user")
    class GetRecentDetectionsTests {

        @Test
        @DisplayName("should filter by hours threshold")
        void shouldFilterByHours() {
            AnimalDetection recent = createDetection("Robin", "bird", 0.90f);
            recent.setTimestamp(LocalDateTime.now().minusMinutes(30));
            repository.save(recent);

            AnimalDetection old = createDetection("Sparrow", "bird", 0.85f);
            old.setTimestamp(LocalDateTime.now().minusHours(5));
            repository.save(old);

            List<AnimalDetection> results = queryService.getRecentDetections(1);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getSpecies()).isEqualTo("Robin");
        }
    }

    @Nested
    @DisplayName("QueryService: getDetectionsBySpecies")
    @WithMockUser(username = "test-user")
    class GetDetectionsBySpeciesTests {

        @Test
        @DisplayName("should filter by species with pagination")
        void shouldFilterBySpecies() {
            repository.save(createDetection("Robin", "bird", 0.90f));
            repository.save(createDetection("Robin", "bird", 0.85f));
            repository.save(createDetection("Sparrow", "bird", 0.88f));

            List<AnimalDetection> results = queryService.getDetectionsBySpecies(
                    "Robin", PageRequest.of(0, 10));

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(d -> d.getSpecies().equals("Robin"));
        }
    }

    @Nested
    @DisplayName("QueryService: getDetectionsByDevice")
    @WithMockUser(username = "test-user")
    class GetDetectionsByDeviceTests {

        @Test
        @DisplayName("should filter by device ID")
        void shouldFilterByDevice() {
            AnimalDetection det1 = createDetection("Robin", "bird", 0.90f);
            det1.setDeviceId("device-A");
            repository.save(det1);

            AnimalDetection det2 = createDetection("Fox", "mammal", 0.85f);
            det2.setDeviceId("device-B");
            repository.save(det2);

            List<AnimalDetection> results = queryService.getDetectionsByDevice(
                    "device-A", PageRequest.of(0, 10));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDeviceId()).isEqualTo("device-A");
        }
    }

    @Nested
    @DisplayName("QueryService: getSpeciesCount")
    @WithMockUser(username = "test-user")
    class GetSpeciesCountTests {

        @Test
        @DisplayName("should aggregate species counts")
        void shouldAggregateSpeciesCounts() {
            AnimalDetection det1 = createDetection("Robin", "bird", 0.90f);
            det1.setTimestamp(LocalDateTime.now().minusMinutes(30));
            repository.save(det1);

            AnimalDetection det2 = createDetection("Robin", "bird", 0.85f);
            det2.setTimestamp(LocalDateTime.now().minusMinutes(20));
            repository.save(det2);

            AnimalDetection det3 = createDetection("Sparrow", "bird", 0.88f);
            det3.setTimestamp(LocalDateTime.now().minusMinutes(10));
            repository.save(det3);

            Map<String, Long> counts = queryService.getSpeciesCount(1);

            assertThat(counts.get("Robin")).isEqualTo(2);
            assertThat(counts.get("Sparrow")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("QueryService: getHighConfidenceDetections")
    @WithMockUser(username = "test-user")
    class GetHighConfidenceDetectionsTests {

        @Test
        @DisplayName("should filter by minimum confidence")
        void shouldFilterByConfidence() {
            AnimalDetection high = createDetection("Robin", "bird", 0.95f);
            high.setTimestamp(LocalDateTime.now().minusMinutes(10));
            repository.save(high);

            AnimalDetection low = createDetection("Sparrow", "bird", 0.50f);
            low.setTimestamp(LocalDateTime.now().minusMinutes(10));
            repository.save(low);

            List<AnimalDetection> results = queryService.getHighConfidenceDetections(0.80f, 1);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getConfidence()).isGreaterThanOrEqualTo(0.80f);
        }
    }
}
