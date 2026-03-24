package dev.syslabs.sentio.repository;

import dev.syslabs.sentio.BaseIntegrationTest;
import dev.syslabs.sentio.model.WorkflowResult;
import dev.syslabs.sentio.model.WorkflowType;
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
 * Integration tests for {@link WorkflowResultRepository}.
 * Validates custom JPQL queries for workflow result storage and retrieval.
 */
@DisplayName("WorkflowResultRepository")
class WorkflowResultRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private WorkflowResultRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private WorkflowResult createResult(WorkflowType type, LocalDateTime timestamp, String userId) {
        WorkflowResult result = new WorkflowResult();
        result.setWorkflowType(type);
        result.setTimestamp(timestamp);
        result.setUserId(userId);
        result.setAnalysisText("Test analysis");
        result.setDataConfidence(0.85f);
        return result;
    }

    @Nested
    @DisplayName("Most recent result queries")
    class MostRecentTests {

        @Test
        @DisplayName("should find top result by timestamp")
        void shouldFindTopByOrderByTimestampDesc() {
            // Given
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(2), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.now().minusHours(1), null));

            // When
            Optional<WorkflowResult> result = repository.findTopByOrderByTimestampDesc();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getWorkflowType()).isEqualTo(WorkflowType.SIGHTINGS_SUMMARY);
        }

        @Test
        @DisplayName("should return empty when no results exist")
        void shouldReturnEmptyWhenNoResults() {
            // When
            Optional<WorkflowResult> result = repository.findTopByOrderByTimestampDesc();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should find top result by workflow type")
        void shouldFindTopByWorkflowTypeOrderByTimestampDesc() {
            // Given
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(3), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(1), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.now().minusHours(2), null));

            // When
            Optional<WorkflowResult> result = repository
                    .findTopByWorkflowTypeOrderByTimestampDesc(WorkflowType.WEATHER_SUMMARY);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTimestamp()).isAfter(LocalDateTime.now().minusHours(2));
        }

        @Test
        @DisplayName("should return empty when no results for workflow type")
        void shouldReturnEmptyWhenNoResultsForType() {
            // Given
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now(), null));

            // When
            Optional<WorkflowResult> result = repository
                    .findTopByWorkflowTypeOrderByTimestampDesc(WorkflowType.AGENT_RESPONSE);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Time range queries")
    class TimeRangeTests {

        @Test
        @DisplayName("should find results by timestamp between")
        void shouldFindByTimestampBetween() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.of(2024, 1, 15, 12, 0), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.of(2024, 2, 1, 12, 0), null));
            repository.save(createResult(WorkflowType.AGENT_RESPONSE, LocalDateTime.of(2024, 1, 20, 12, 0), null));

            // When
            List<WorkflowResult> results = repository.findByTimestampBetweenOrderByTimestampDesc(start, end);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WorkflowResult::getWorkflowType)
                    .containsExactlyInAnyOrder(WorkflowType.WEATHER_SUMMARY, WorkflowType.AGENT_RESPONSE);
        }

        @Test
        @DisplayName("should find results by workflow type and timestamp between")
        void shouldFindByWorkflowTypeAndTimestampBetween() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.of(2024, 1, 15, 12, 0), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.of(2024, 1, 20, 12, 0), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.of(2024, 1, 10, 12, 0), null));

            // When
            List<WorkflowResult> results = repository
                    .findByWorkflowTypeAndTimestampBetweenOrderByTimestampDesc(WorkflowType.WEATHER_SUMMARY, start,
                            end);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(r -> r.getWorkflowType() == WorkflowType.WEATHER_SUMMARY);
        }

        @Test
        @DisplayName("should order results by timestamp descending")
        void shouldOrderResultsByTimestampDesc() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.of(2024, 1, 15, 12, 0), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.of(2024, 1, 25, 12, 0), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.of(2024, 1, 5, 12, 0), null));

            // When
            List<WorkflowResult> results = repository.findByTimestampBetweenOrderByTimestampDesc(start, end);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results.get(0).getTimestamp()).isAfter(results.get(1).getTimestamp());
            assertThat(results.get(1).getTimestamp()).isAfter(results.get(2).getTimestamp());
        }
    }

    @Nested
    @DisplayName("Recent results queries")
    class RecentResultsTests {

        @Test
        @DisplayName("should find results after timestamp")
        void shouldFindByTimestampAfter() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(2), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.now().minusHours(10), null));
            repository.save(createResult(WorkflowType.AGENT_RESPONSE, LocalDateTime.now().minusHours(1), null));

            // When
            List<WorkflowResult> results = repository.findByTimestampAfterOrderByTimestampDesc(since);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WorkflowResult::getWorkflowType)
                    .containsExactlyInAnyOrder(WorkflowType.WEATHER_SUMMARY, WorkflowType.AGENT_RESPONSE);
        }

        @Test
        @DisplayName("should find results by workflow type after timestamp")
        void shouldFindByWorkflowTypeAndTimestampAfter() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(2), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(10), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.now().minusHours(1), null));

            // When
            List<WorkflowResult> results = repository
                    .findByWorkflowTypeAndTimestampAfterOrderByTimestampDesc(WorkflowType.WEATHER_SUMMARY, since);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWorkflowType()).isEqualTo(WorkflowType.WEATHER_SUMMARY);
        }
    }

    @Nested
    @DisplayName("Deletion operations")
    class DeletionTests {

        @Test
        @DisplayName("should delete old results")
        void shouldDeleteOldResults() {
            // Given
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, cutoff.minusDays(1), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, cutoff.plusDays(1), null));

            // When
            repository.deleteOldResults(cutoff);
            repository.flush();

            // Then
            List<WorkflowResult> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getWorkflowType()).isEqualTo(WorkflowType.SIGHTINGS_SUMMARY);
        }

        @Test
        @DisplayName("should delete old results by type")
        void shouldDeleteOldResultsByType() {
            // Given
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, cutoff.minusDays(1), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, cutoff.plusDays(1), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, cutoff.minusDays(1), null));

            // When
            repository.deleteOldResultsByType(WorkflowType.WEATHER_SUMMARY, cutoff);
            repository.flush();

            // Then
            List<WorkflowResult> remaining = repository.findAll();
            assertThat(remaining).hasSize(2);
            assertThat(remaining).extracting(WorkflowResult::getWorkflowType)
                    .containsExactlyInAnyOrder(WorkflowType.WEATHER_SUMMARY, WorkflowType.SIGHTINGS_SUMMARY);
        }
    }

    @Nested
    @DisplayName("Count queries")
    class CountTests {

        @Test
        @DisplayName("should count today's results")
        void shouldCountTodaysResults() {
            // Given
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1);

            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, startOfDay.plusHours(10), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, startOfDay.plusHours(14), null));
            repository.save(createResult(WorkflowType.AGENT_RESPONSE, startOfDay.minusDays(1), null));

            // When
            long count = repository.countTodaysResults(startOfDay, endOfDay);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should count today's results by type")
        void shouldCountTodaysResultsByType() {
            // Given
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1);

            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, startOfDay.plusHours(10), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, startOfDay.plusHours(14), null));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, startOfDay.plusHours(12), null));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, startOfDay.minusDays(1), null));

            // When
            long count = repository.countTodaysResultsByType(WorkflowType.WEATHER_SUMMARY, startOfDay, endOfDay);

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("User-scoped queries")
    class UserScopedTests {

        @Test
        @DisplayName("should find top result by user ID and workflow type")
        void shouldFindTopByUserIdAndWorkflowType() {
            // Given
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(3), "user-001"));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(1), "user-001"));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(2), "user-002"));

            // When
            Optional<WorkflowResult> result = repository
                    .findTopByUserIdAndWorkflowTypeOrderByTimestampDesc("user-001", WorkflowType.WEATHER_SUMMARY);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo("user-001");
            assertThat(result.get().getTimestamp()).isAfter(LocalDateTime.now().minusHours(2));
        }

        @Test
        @DisplayName("should find recent results by user ID")
        void shouldFindByUserIdAndTimestampAfter() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(2), "user-001"));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.now().minusHours(10), "user-001"));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(1), "user-002"));

            // When
            List<WorkflowResult> results = repository.findByUserIdAndTimestampAfterOrderByTimestampDesc("user-001",
                    since);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWorkflowType()).isEqualTo(WorkflowType.WEATHER_SUMMARY);
        }

        @Test
        @DisplayName("should find recent results by user ID and workflow type")
        void shouldFindByUserIdAndWorkflowTypeAndTimestampAfter() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(2), "user-001"));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, LocalDateTime.now().minusHours(10), "user-001"));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, LocalDateTime.now().minusHours(1), "user-001"));

            // When
            List<WorkflowResult> results = repository
                    .findByUserIdAndWorkflowTypeAndTimestampAfterOrderByTimestampDesc("user-001",
                            WorkflowType.WEATHER_SUMMARY, since);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWorkflowType()).isEqualTo(WorkflowType.WEATHER_SUMMARY);
        }

        @Test
        @DisplayName("should delete old results by user")
        void shouldDeleteOldResultsByUser() {
            // Given
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, cutoff.minusDays(1), "user-001"));
            repository.save(createResult(WorkflowType.WEATHER_SUMMARY, cutoff.plusDays(1), "user-001"));
            repository.save(createResult(WorkflowType.SIGHTINGS_SUMMARY, cutoff.minusDays(1), "user-002"));

            // When
            repository.deleteOldResultsByUser("user-001", cutoff);
            repository.flush();

            // Then
            List<WorkflowResult> remaining = repository.findAll();
            assertThat(remaining).hasSize(2);
            // Should keep recent user-001 result and all user-002 results
            boolean hasUser001Recent = remaining.stream()
                    .anyMatch(r -> "user-001".equals(r.getUserId()) && r.getTimestamp().isAfter(cutoff));
            boolean hasUser002Old = remaining.stream()
                    .anyMatch(r -> "user-002".equals(r.getUserId()) && r.getTimestamp().isBefore(cutoff));
            assertThat(hasUser001Recent).isTrue();
            assertThat(hasUser002Old).isTrue();
        }
    }
}
