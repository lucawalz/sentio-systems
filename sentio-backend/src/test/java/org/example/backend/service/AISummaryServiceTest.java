package org.example.backend.service;

import org.example.backend.model.AISummary;
import org.example.backend.repository.AISummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AISummaryService}.
 * 
 * <p>
 * Following FIRST principles:
 * </p>
 * <ul>
 * <li><b>Fast</b> - Repository is mocked, no database access</li>
 * <li><b>Independent</b> - Each test has isolated setup</li>
 * <li><b>Repeatable</b> - Deterministic mock behavior</li>
 * <li><b>Self-validating</b> - Clear assertions</li>
 * <li><b>Timely</b> - Tests all service methods</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AISummaryServiceTest {

    @Mock
    private AISummaryRepository aiSummaryRepository;

    @InjectMocks
    private AISummaryService aiSummaryService;

    @Nested
    @DisplayName("saveAISummary")
    class SaveAISummaryTests {

        @Test
        @DisplayName("should save AI summary and return saved entity")
        void shouldSaveAISummaryAndReturnSavedEntity() {
            // Given
            AISummary inputSummary = new AISummary();
            inputSummary.setAnalysisText("Test analysis");

            AISummary savedSummary = new AISummary();
            savedSummary.setId(1L);
            savedSummary.setAnalysisText("Test analysis");
            savedSummary.setTimestamp(LocalDateTime.now());

            when(aiSummaryRepository.save(any(AISummary.class))).thenReturn(savedSummary);

            // When
            AISummary result = aiSummaryService.saveAISummary(inputSummary);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAnalysisText()).isEqualTo("Test analysis");
        }

        @Test
        @DisplayName("should cleanup old summaries before saving")
        void shouldCleanupOldSummariesBeforeSaving() {
            // Given
            AISummary summary = new AISummary();
            summary.setAnalysisText("New analysis");

            when(aiSummaryRepository.save(any(AISummary.class))).thenReturn(summary);

            // When
            aiSummaryService.saveAISummary(summary);

            // Then
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(aiSummaryRepository).deleteOldSummaries(cutoffCaptor.capture());

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertThat(capturedCutoff).isBefore(LocalDateTime.now().minusHours(23));
        }
    }

    @Nested
    @DisplayName("getCurrentSummary")
    class GetCurrentSummaryTests {

        @Test
        @DisplayName("should return most recent summary when exists")
        void shouldReturnMostRecentSummaryWhenExists() {
            // Given
            AISummary latestSummary = new AISummary();
            latestSummary.setId(1L);
            latestSummary.setAnalysisText("Latest analysis");
            latestSummary.setTimestamp(LocalDateTime.now());

            when(aiSummaryRepository.findTopByOrderByTimestampDesc())
                    .thenReturn(Optional.of(latestSummary));

            // When
            Optional<AISummary> result = aiSummaryService.getCurrentSummary();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getAnalysisText()).isEqualTo("Latest analysis");
        }

        @Test
        @DisplayName("should return empty optional when no summaries exist")
        void shouldReturnEmptyWhenNoSummariesExist() {
            // Given
            when(aiSummaryRepository.findTopByOrderByTimestampDesc())
                    .thenReturn(Optional.empty());

            // When
            Optional<AISummary> result = aiSummaryService.getCurrentSummary();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRecentSummaries")
    class GetRecentSummariesTests {

        @Test
        @DisplayName("should return summaries from last 24 hours")
        void shouldReturnSummariesFromLast24Hours() {
            // Given
            AISummary summary1 = new AISummary();
            summary1.setId(1L);
            summary1.setAnalysisText("Summary 1");

            AISummary summary2 = new AISummary();
            summary2.setId(2L);
            summary2.setAnalysisText("Summary 2");

            when(aiSummaryRepository.findByTimestampAfterOrderByTimestampDesc(any(LocalDateTime.class)))
                    .thenReturn(List.of(summary1, summary2));

            // When
            List<AISummary> result = aiSummaryService.getRecentSummaries();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(AISummary::getAnalysisText)
                    .containsExactly("Summary 1", "Summary 2");
        }

        @Test
        @DisplayName("should return empty list when no recent summaries")
        void shouldReturnEmptyListWhenNoRecentSummaries() {
            // Given
            when(aiSummaryRepository.findByTimestampAfterOrderByTimestampDesc(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<AISummary> result = aiSummaryService.getRecentSummaries();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should query with correct time threshold")
        void shouldQueryWithCorrectTimeThreshold() {
            // Given
            when(aiSummaryRepository.findByTimestampAfterOrderByTimestampDesc(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            aiSummaryService.getRecentSummaries();

            // Then
            ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(aiSummaryRepository).findByTimestampAfterOrderByTimestampDesc(sinceCaptor.capture());

            LocalDateTime capturedSince = sinceCaptor.getValue();
            assertThat(capturedSince).isBefore(LocalDateTime.now().minusHours(23));
            assertThat(capturedSince).isAfter(LocalDateTime.now().minusHours(25));
        }
    }

    @Nested
    @DisplayName("cleanupOldSummaries")
    class CleanupOldSummariesTests {

        @Test
        @DisplayName("should delete summaries older than 7 days")
        void shouldDeleteSummariesOlderThan7Days() {
            // Given - service is set up

            // When
            aiSummaryService.cleanupOldSummaries();

            // Then
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(aiSummaryRepository).deleteOldSummaries(cutoffCaptor.capture());

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertThat(capturedCutoff).isBefore(LocalDateTime.now().minusDays(6));
            assertThat(capturedCutoff).isAfter(LocalDateTime.now().minusDays(8));
        }
    }
}
