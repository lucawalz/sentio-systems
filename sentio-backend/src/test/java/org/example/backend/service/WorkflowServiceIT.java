package org.example.backend.service;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.WorkflowResult;
import org.example.backend.model.WorkflowType;
import org.example.backend.repository.WorkflowResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WorkflowService}.
 * Validates workflow result persistence and time-based queries.
 */
class WorkflowServiceIT extends BaseIntegrationTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowResultRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private WorkflowResult createResult(WorkflowType type, String text, LocalDateTime timestamp) {
        WorkflowResult result = new WorkflowResult();
        result.setWorkflowType(type);
        result.setAnalysisText(text);
        result.setTimestamp(timestamp);
        return result;
    }

    @Nested
    @DisplayName("saveWorkflowResult")
    class SaveWorkflowResultTests {

        @Test
        @DisplayName("should persist result to database")
        void shouldPersistResult() {
            // Given
            WorkflowResult result = createResult(
                    WorkflowType.WEATHER_SUMMARY,
                    "Test summary",
                    LocalDateTime.now());

            // When
            WorkflowResult saved = workflowService.saveWorkflowResult(result);

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("getCurrentResult")
    class GetCurrentResultTests {

        @Test
        @DisplayName("should return most recent result")
        void shouldReturnMostRecent() {
            // Given
            WorkflowResult older = createResult(WorkflowType.WEATHER_SUMMARY, "Older",
                    LocalDateTime.now().minusHours(2));
            WorkflowResult newer = createResult(WorkflowType.WEATHER_SUMMARY, "Newer", LocalDateTime.now());
            repository.save(older);
            repository.save(newer);

            // When
            Optional<WorkflowResult> result = workflowService.getCurrentResult();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getAnalysisText()).isEqualTo("Newer");
        }

        @Test
        @DisplayName("should return empty when no results")
        void shouldReturnEmptyWhenNone() {
            // When
            Optional<WorkflowResult> result = workflowService.getCurrentResult();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRecentResults")
    class GetRecentResultsTests {

        @Test
        @DisplayName("should return results from last 24 hours")
        void shouldReturnRecentResults() {
            // Given
            WorkflowResult recent = createResult(WorkflowType.WEATHER_SUMMARY, "Recent",
                    LocalDateTime.now().minusHours(1));
            WorkflowResult old = createResult(WorkflowType.WEATHER_SUMMARY, "Old", LocalDateTime.now().minusDays(2));
            repository.save(recent);
            repository.save(old);

            // When
            List<WorkflowResult> results = workflowService.getRecentResults();

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAnalysisText()).isEqualTo("Recent");
        }
    }

    @Nested
    @DisplayName("cleanupOldResults")
    class CleanupTests {

        @Test
        @DisplayName("should delete results older than 7 days")
        void shouldDeleteOldResults() {
            // Given
            WorkflowResult recent = createResult(WorkflowType.WEATHER_SUMMARY, "Recent", LocalDateTime.now());
            WorkflowResult old = createResult(WorkflowType.WEATHER_SUMMARY, "Old", LocalDateTime.now().minusDays(10));
            repository.save(recent);
            repository.save(old);

            // When
            workflowService.cleanupOldResults();

            // Then
            List<WorkflowResult> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getAnalysisText()).isEqualTo("Recent");
        }
    }
}
