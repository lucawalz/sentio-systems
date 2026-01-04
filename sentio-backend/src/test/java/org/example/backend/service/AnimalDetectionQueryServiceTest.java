package org.example.backend.service;

import org.example.backend.dto.AnimalDetectionSummary;
import org.example.backend.model.AnimalDetection;
import org.example.backend.repository.AnimalDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnimalDetectionQueryService}.
 * 
 * <p>
 * Following FIRST principles with Given/When/Then format.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AnimalDetectionQueryServiceTest {

    @Mock
    private AnimalDetectionRepository animalDetectionRepository;

    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private AnimalDetectionQueryService queryService;

    private AnimalDetection createTestDetection(Long id, String species, String animalType) {
        AnimalDetection detection = new AnimalDetection();
        detection.setId(id);
        detection.setSpecies(species);
        detection.setAnimalType(animalType);
        detection.setConfidence(0.9f);
        detection.setTimestamp(LocalDateTime.now());
        detection.setDeviceId("device-1");
        return detection;
    }

    @Nested
    @DisplayName("getLatestDetections")
    class GetLatestDetectionsTests {

        @Test
        @DisplayName("should return latest detections for user's devices")
        void shouldReturnLatestDetectionsForUserDevices() {
            // Given
            int limit = 10;
            List<String> deviceIds = List.of("device-1", "device-2");
            List<AnimalDetection> detections = List.of(
                    createTestDetection(1L, "robin", "bird"),
                    createTestDetection(2L, "fox", "mammal"));

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(animalDetectionRepository.findTopNByDeviceIdInOrderByTimestampDesc(deviceIds, limit))
                    .thenReturn(detections);

            // When
            List<AnimalDetection> result = queryService.getLatestDetections(limit);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getSpecies()).isEqualTo("robin");
        }

        @Test
        @DisplayName("should return empty list when user has no devices")
        void shouldReturnEmptyListWhenNoDevices() {
            // Given
            when(deviceService.getMyDeviceIds()).thenReturn(Collections.emptyList());

            // When
            List<AnimalDetection> result = queryService.getLatestDetections(10);

            // Then
            assertThat(result).isEmpty();
            verify(animalDetectionRepository, never()).findTopNByDeviceIdInOrderByTimestampDesc(any(), anyInt());
        }
    }

    @Nested
    @DisplayName("getRecentDetections")
    class GetRecentDetectionsTests {

        @Test
        @DisplayName("should return detections from specified hours ago")
        void shouldReturnDetectionsFromSpecifiedHours() {
            // Given
            int hours = 24;
            List<String> deviceIds = List.of("device-1");
            List<AnimalDetection> detections = List.of(
                    createTestDetection(1L, "eagle", "bird"));

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(animalDetectionRepository.findRecentDetectionsByDeviceIds(eq(deviceIds), any(LocalDateTime.class)))
                    .thenReturn(detections);

            // When
            List<AnimalDetection> result = queryService.getRecentDetections(hours);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should return empty list when no devices")
        void shouldReturnEmptyListWhenNoDevices() {
            // Given
            when(deviceService.getMyDeviceIds()).thenReturn(Collections.emptyList());

            // When
            List<AnimalDetection> result = queryService.getRecentDetections(24);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDetectionsByDate")
    class GetDetectionsByDateTests {

        @Test
        @DisplayName("should return detections for specific date")
        void shouldReturnDetectionsForSpecificDate() {
            // Given
            LocalDate date = LocalDate.of(2025, 12, 21);
            List<String> deviceIds = List.of("device-1");
            List<AnimalDetection> detections = List.of(
                    createTestDetection(1L, "owl", "bird"));

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(animalDetectionRepository.findByDeviceIdInAndTimestampBetweenOrderByTimestampDesc(
                    eq(deviceIds), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(detections);

            // When
            List<AnimalDetection> result = queryService.getDetectionsByDate(date);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSpecies()).isEqualTo("owl");
        }
    }

    @Nested
    @DisplayName("getDetectionsBySpecies")
    class GetDetectionsBySpeciesTests {

        @Test
        @DisplayName("should return detections filtered by species with pagination")
        void shouldReturnDetectionsFilteredBySpecies() {
            // Given
            String species = "sparrow";
            Pageable pageable = PageRequest.of(0, 10);
            List<String> deviceIds = List.of("device-1");
            List<AnimalDetection> detections = List.of(
                    createTestDetection(1L, "sparrow", "bird"));

            when(deviceService.getMyDeviceIds()).thenReturn(deviceIds);
            when(animalDetectionRepository.findByDeviceIdInAndSpeciesIgnoreCaseOrderByTimestampDesc(
                    deviceIds, species, pageable))
                    .thenReturn(detections);

            // When
            List<AnimalDetection> result = queryService.getDetectionsBySpecies(species, pageable);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSpecies()).isEqualTo("sparrow");
        }
    }

    @Nested
    @DisplayName("getDetectionsByDevice")
    class GetDetectionsByDeviceTests {

        @Test
        @DisplayName("should return detections when user has access to device")
        void shouldReturnDetectionsWhenUserHasAccess() {
            // Given
            String deviceId = "device-1";
            Pageable pageable = PageRequest.of(0, 10);
            List<AnimalDetection> detections = List.of(
                    createTestDetection(1L, "magpie", "bird"));

            when(deviceService.hasAccessToDevice(deviceId)).thenReturn(true);
            when(animalDetectionRepository.findByDeviceIdOrderByTimestampDesc(deviceId, pageable))
                    .thenReturn(detections);

            // When
            List<AnimalDetection> result = queryService.getDetectionsByDevice(deviceId, pageable);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when user has no access")
        void shouldThrowExceptionWhenNoAccess() {
            // Given
            String deviceId = "device-1";
            Pageable pageable = PageRequest.of(0, 10);

            when(deviceService.hasAccessToDevice(deviceId)).thenReturn(false);

            // When / Then
            assertThatThrownBy(() -> queryService.getDetectionsByDevice(deviceId, pageable))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Access denied");
        }
    }

    @Nested
    @DisplayName("getDetectionById")
    class GetDetectionByIdTests {

        @Test
        @DisplayName("should return detection when exists and user has access")
        void shouldReturnDetectionWhenExistsAndHasAccess() {
            // Given
            Long id = 1L;
            AnimalDetection detection = createTestDetection(id, "crow", "bird");

            when(animalDetectionRepository.findById(id)).thenReturn(Optional.of(detection));
            when(deviceService.hasAccessToDevice("device-1")).thenReturn(true);

            // When
            Optional<AnimalDetection> result = queryService.getDetectionById(id);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getSpecies()).isEqualTo("crow");
        }

        @Test
        @DisplayName("should return empty when detection exists but no access")
        void shouldReturnEmptyWhenNoAccess() {
            // Given
            Long id = 1L;
            AnimalDetection detection = createTestDetection(id, "crow", "bird");

            when(animalDetectionRepository.findById(id)).thenReturn(Optional.of(detection));
            when(deviceService.hasAccessToDevice("device-1")).thenReturn(false);

            // When
            Optional<AnimalDetection> result = queryService.getDetectionById(id);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when detection does not exist")
        void shouldReturnEmptyWhenNotExists() {
            // Given
            Long id = 999L;
            when(animalDetectionRepository.findById(id)).thenReturn(Optional.empty());

            // When
            Optional<AnimalDetection> result = queryService.getDetectionById(id);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDetectionSummary")
    class GetDetectionSummaryTests {

        @Test
        @DisplayName("should generate summary with all metrics")
        void shouldGenerateSummaryWithAllMetrics() {
            // Given
            int hours = 24;
            AnimalDetection bird1 = createTestDetection(1L, "sparrow", "bird");
            bird1.setConfidence(0.9f);
            bird1.setTimestamp(LocalDateTime.now().minusHours(1));

            AnimalDetection bird2 = createTestDetection(2L, "robin", "bird");
            bird2.setConfidence(0.8f);
            bird2.setTimestamp(LocalDateTime.now().minusHours(2));

            AnimalDetection mammal = createTestDetection(3L, "fox", "mammal");
            mammal.setConfidence(0.95f);
            mammal.setTimestamp(LocalDateTime.now().minusMinutes(30));

            List<AnimalDetection> detections = List.of(bird1, bird2, mammal);

            when(animalDetectionRepository.findRecentDetections(any(LocalDateTime.class)))
                    .thenReturn(detections);

            // When
            AnimalDetectionSummary summary = queryService.getDetectionSummary(hours);

            // Then
            assertThat(summary.getTotalDetections()).isEqualTo(3);
            assertThat(summary.getUniqueSpecies()).isEqualTo(3); // sparrow, robin, fox
            assertThat(summary.getUniqueAnimalTypes()).isEqualTo(2); // bird, mammal
            assertThat(summary.getBirdDetections()).isEqualTo(2);
            assertThat(summary.getMammalDetections()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty summary when no detections")
        void shouldReturnEmptySummaryWhenNoDetections() {
            // Given
            when(animalDetectionRepository.findRecentDetections(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            AnimalDetectionSummary summary = queryService.getDetectionSummary(24);

            // Then
            assertThat(summary.getTotalDetections()).isZero();
        }
    }

    @Nested
    @DisplayName("getSpeciesCount")
    class GetSpeciesCountTests {

        @Test
        @DisplayName("should return species count map")
        void shouldReturnSpeciesCountMap() {
            // Given
            AnimalDetection d1 = createTestDetection(1L, "sparrow", "bird");
            AnimalDetection d2 = createTestDetection(2L, "sparrow", "bird");
            AnimalDetection d3 = createTestDetection(3L, "robin", "bird");

            when(animalDetectionRepository.findRecentDetections(any(LocalDateTime.class)))
                    .thenReturn(List.of(d1, d2, d3));

            // When
            var result = queryService.getSpeciesCount(24);

            // Then
            assertThat(result).containsEntry("sparrow", 2L);
            assertThat(result).containsEntry("robin", 1L);
        }
    }

    @Nested
    @DisplayName("getAllSpecies")
    class GetAllSpeciesTests {

        @Test
        @DisplayName("should return distinct species list")
        void shouldReturnDistinctSpeciesList() {
            // Given
            when(animalDetectionRepository.findAllDistinctSpecies())
                    .thenReturn(List.of("crow", "fox", "sparrow"));

            // When
            List<String> result = queryService.getAllSpecies();

            // Then
            assertThat(result).containsExactly("crow", "fox", "sparrow");
        }
    }

    @Nested
    @DisplayName("getBirdDetections")
    class GetBirdDetectionsTests {

        @Test
        @DisplayName("should return bird detections from specified timeframe")
        void shouldReturnBirdDetections() {
            // Given
            int hours = 24;
            List<AnimalDetection> birds = List.of(
                    createTestDetection(1L, "sparrow", "bird"));

            when(animalDetectionRepository.findBirdDetections(any(LocalDateTime.class)))
                    .thenReturn(birds);

            // When
            List<AnimalDetection> result = queryService.getBirdDetections(hours);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAnimalType()).isEqualTo("bird");
        }
    }
}
