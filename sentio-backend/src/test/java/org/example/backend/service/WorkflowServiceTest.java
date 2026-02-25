package org.example.backend.service;

import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
import org.example.backend.repository.WorkflowResultRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WorkflowService}.
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
class WorkflowServiceTest {

    @Mock
    private WorkflowResultRepository workflowResultRepository;

    @InjectMocks
    private WorkflowService workflowService;

    @Nested
    @DisplayName("saveWorkflowResult")
    class SaveWorkflowResultTests {

        @Test
        @DisplayName("should save workflow result and return saved entity")
        void shouldSaveWorkflowResultAndReturnSavedEntity() {
            // Given
            WorkflowResult inputResult = new WorkflowResult();
            inputResult.setAnalysisText("Test analysis");
            inputResult.setWorkflowType(WorkflowType.WEATHER_SUMMARY);

            WorkflowResult savedResult = new WorkflowResult();
            savedResult.setId(1L);
            savedResult.setAnalysisText("Test analysis");
            savedResult.setWorkflowType(WorkflowType.WEATHER_SUMMARY);
            savedResult.setTimestamp(LocalDateTime.now());

            when(workflowResultRepository.save(any(WorkflowResult.class))).thenReturn(savedResult);

            // When
            WorkflowResult result = workflowService.saveWorkflowResult(inputResult);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAnalysisText()).isEqualTo("Test analysis");
            assertThat(result.getWorkflowType()).isEqualTo(WorkflowType.WEATHER_SUMMARY);
        }

        @Test
        @DisplayName("should cleanup old results of same type before saving")
        void shouldCleanupOldResultsBeforeSaving() {
            // Given
            WorkflowResult result = new WorkflowResult();
            result.setAnalysisText("New analysis");
            result.setWorkflowType(WorkflowType.WEATHER_SUMMARY);

            when(workflowResultRepository.save(any(WorkflowResult.class))).thenReturn(result);

            // When
            workflowService.saveWorkflowResult(result);

            // Then
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(workflowResultRepository).deleteOldResultsByType(eq(WorkflowType.WEATHER_SUMMARY),
                    cutoffCaptor.capture());

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertThat(capturedCutoff).isBefore(LocalDateTime.now().minusHours(23));
        }
    }

    @Nested
    @DisplayName("getCurrentResult")
    class GetCurrentResultTests {

        @Test
        @DisplayName("should return most recent result when exists")
        void shouldReturnMostRecentResultWhenExists() {
            // Given
            WorkflowResult latestResult = new WorkflowResult();
            latestResult.setId(1L);
            latestResult.setAnalysisText("Latest analysis");
            latestResult.setTimestamp(LocalDateTime.now());

            when(workflowResultRepository.findTopByOrderByTimestampDesc())
                    .thenReturn(Optional.of(latestResult));

            // When
            Optional<WorkflowResult> result = workflowService.getCurrentResult();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getAnalysisText()).isEqualTo("Latest analysis");
        }

        @Test
        @DisplayName("should return empty optional when no results exist")
        void shouldReturnEmptyWhenNoResultsExist() {
            // Given
            when(workflowResultRepository.findTopByOrderByTimestampDesc())
                    .thenReturn(Optional.empty());

            // When
            Optional<WorkflowResult> result = workflowService.getCurrentResult();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCurrentSummary")
    class GetCurrentSummaryTests {

        @Test
        @DisplayName("should return most recent summary")
        void shouldReturnMostRecentSummary() {
            // Given
            WorkflowResult summary = new WorkflowResult();
            summary.setId(1L);
            summary.setWorkflowType(WorkflowType.WEATHER_SUMMARY);
            summary.setAnalysisText("Summary text");

            when(workflowResultRepository.findTopByWorkflowTypeOrderByTimestampDesc(WorkflowType.WEATHER_SUMMARY))
                    .thenReturn(Optional.of(summary));

            // When
            Optional<WorkflowResult> result = workflowService.getCurrentSummary();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWorkflowType()).isEqualTo(WorkflowType.WEATHER_SUMMARY);
        }
    }

    @Nested
    @DisplayName("getRecentResults")
    class GetRecentResultsTests {

        @Test
        @DisplayName("should return results from last 24 hours")
        void shouldReturnResultsFromLast24Hours() {
            // Given
            WorkflowResult result1 = new WorkflowResult();
            result1.setId(1L);
            result1.setAnalysisText("Result 1");

            WorkflowResult result2 = new WorkflowResult();
            result2.setId(2L);
            result2.setAnalysisText("Result 2");

            when(workflowResultRepository.findByTimestampAfterOrderByTimestampDesc(any(LocalDateTime.class)))
                    .thenReturn(List.of(result1, result2));

            // When
            List<WorkflowResult> results = workflowService.getRecentResults();

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WorkflowResult::getAnalysisText)
                    .containsExactly("Result 1", "Result 2");
        }

        @Test
        @DisplayName("should return empty list when no recent results")
        void shouldReturnEmptyListWhenNoRecentResults() {
            // Given
            when(workflowResultRepository.findByTimestampAfterOrderByTimestampDesc(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            List<WorkflowResult> results = workflowService.getRecentResults();

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should query with correct time threshold")
        void shouldQueryWithCorrectTimeThreshold() {
            // Given
            when(workflowResultRepository.findByTimestampAfterOrderByTimestampDesc(any(LocalDateTime.class)))
                    .thenReturn(Collections.emptyList());

            // When
            workflowService.getRecentResults();

            // Then
            ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(workflowResultRepository).findByTimestampAfterOrderByTimestampDesc(sinceCaptor.capture());

            LocalDateTime capturedSince = sinceCaptor.getValue();
            assertThat(capturedSince).isBefore(LocalDateTime.now().minusHours(23));
            assertThat(capturedSince).isAfter(LocalDateTime.now().minusHours(25));
        }
    }

    @Nested
    @DisplayName("cleanupOldResults")
    class CleanupOldResultsTests {

        @Test
        @DisplayName("should delete results older than 7 days")
        void shouldDeleteResultsOlderThan7Days() {
            // When
            workflowService.cleanupOldResults();

            // Then
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(workflowResultRepository).deleteOldResults(cutoffCaptor.capture());

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertThat(capturedCutoff).isBefore(LocalDateTime.now().minusDays(6));
            assertThat(capturedCutoff).isAfter(LocalDateTime.now().minusDays(8));
        }
    }

    @Nested
    @DisplayName("cleanupOldResultsByType")
    class CleanupOldResultsByTypeTests {

        @Test
        @DisplayName("should delete results older than 7 days for type")
        void shouldDeleteResultsOlderThan7DaysForType() {
            // When
            workflowService.cleanupOldResultsByType(WorkflowType.WEATHER_SUMMARY);

            // Then
            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(workflowResultRepository).deleteOldResultsByType(eq(WorkflowType.WEATHER_SUMMARY),
                    cutoffCaptor.capture());

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertThat(capturedCutoff).isBefore(LocalDateTime.now().minusDays(6));
            assertThat(capturedCutoff).isAfter(LocalDateTime.now().minusDays(8));
        }
    }

    @Nested
    @DisplayName("saveUserWorkflowResult")
    class SaveUserWorkflowResultTests {

        @Test
        @DisplayName("should save user workflow result and cleanup old ones")
        void shouldSaveUserWorkflowResultAndCleanupOldOnes() {
            // Given
            String userId = "user-123";
            WorkflowResult inputResult = new WorkflowResult();
            inputResult.setAnalysisText("Test analysis");
            inputResult.setWorkflowType(WorkflowType.WEATHER_SUMMARY);

            WorkflowResult savedResult = new WorkflowResult();
            savedResult.setId(1L);
            savedResult.setUserId(userId);
            savedResult.setAnalysisText("Test analysis");
            savedResult.setWorkflowType(WorkflowType.WEATHER_SUMMARY);

            when(workflowResultRepository.save(any(WorkflowResult.class))).thenReturn(savedResult);

            // When
            WorkflowResult result = workflowService.saveUserWorkflowResult(userId, inputResult);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getId()).isEqualTo(1L);

            ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(workflowResultRepository).deleteOldResultsByUser(eq(userId), cutoffCaptor.capture());

            LocalDateTime capturedCutoff = cutoffCaptor.getValue();
            assertThat(capturedCutoff).isBefore(LocalDateTime.now().minusHours(23));
        }
    }

    @Nested
    @DisplayName("getCurrentWeatherSummary")
    class GetCurrentWeatherSummaryTests {

        @Test
        @DisplayName("should return most recent weather summary for user")
        void shouldReturnMostRecentWeatherSummaryForUser() {
            // Given
            String userId = "user-123";
            WorkflowResult summary = new WorkflowResult();
            summary.setId(1L);
            summary.setUserId(userId);
            summary.setWorkflowType(WorkflowType.WEATHER_SUMMARY);

            when(workflowResultRepository.findTopByUserIdAndWorkflowTypeOrderByTimestampDesc(userId,
                    WorkflowType.WEATHER_SUMMARY))
                    .thenReturn(Optional.of(summary));

            // When
            Optional<WorkflowResult> result = workflowService.getCurrentWeatherSummary(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWorkflowType()).isEqualTo(WorkflowType.WEATHER_SUMMARY);
            assertThat(result.get().getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("getCurrentSightingsSummary")
    class GetCurrentSightingsSummaryTests {

        @Test
        @DisplayName("should return most recent sightings summary for user")
        void shouldReturnMostRecentSightingsSummaryForUser() {
            // Given
            String userId = "user-123";
            WorkflowResult summary = new WorkflowResult();
            summary.setId(1L);
            summary.setUserId(userId);
            summary.setWorkflowType(WorkflowType.SIGHTINGS_SUMMARY);

            when(workflowResultRepository.findTopByUserIdAndWorkflowTypeOrderByTimestampDesc(userId,
                    WorkflowType.SIGHTINGS_SUMMARY))
                    .thenReturn(Optional.of(summary));

            // When
            Optional<WorkflowResult> result = workflowService.getCurrentSightingsSummary(userId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWorkflowType()).isEqualTo(WorkflowType.SIGHTINGS_SUMMARY);
            assertThat(result.get().getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("getUserRecentResults")
    class GetUserRecentResultsTests {

        @Test
        @DisplayName("should return user results from last 24 hours")
        void shouldReturnUserResultsFromLast24Hours() {
            // Given
            String userId = "user-123";
            WorkflowResult result1 = new WorkflowResult();
            result1.setUserId(userId);

            when(workflowResultRepository.findByUserIdAndTimestampAfterOrderByTimestampDesc(eq(userId),
                    any(LocalDateTime.class)))
                    .thenReturn(List.of(result1));

            // When
            List<WorkflowResult> results = workflowService.getUserRecentResults(userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUserId()).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("getUserRecentResultsByType")
    class GetUserRecentResultsByTypeTests {

        @Test
        @DisplayName("should return user results of type from last 24 hours")
        void shouldReturnUserResultsOfTypeFromLast24Hours() {
            // Given
            String userId = "user-123";
            WorkflowResult result1 = new WorkflowResult();
            result1.setUserId(userId);
            result1.setWorkflowType(WorkflowType.AGENT_RESPONSE);

            when(workflowResultRepository.findByUserIdAndWorkflowTypeAndTimestampAfterOrderByTimestampDesc(eq(userId),
                    eq(WorkflowType.AGENT_RESPONSE), any(LocalDateTime.class)))
                    .thenReturn(List.of(result1));

            // When
            List<WorkflowResult> results = workflowService.getUserRecentResultsByType(userId,
                    WorkflowType.AGENT_RESPONSE);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getUserId()).isEqualTo(userId);
            assertThat(results.get(0).getWorkflowType()).isEqualTo(WorkflowType.AGENT_RESPONSE);
        }
    }
}
