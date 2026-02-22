package org.example.backend.repository;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.WeatherRadarMetadata;
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
 * Integration tests for {@link WeatherRadarMetadataRepository}.
 * Validates custom queries for weather radar metadata storage and retrieval.
 */
@DisplayName("WeatherRadarMetadataRepository")
class WeatherRadarMetadataRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private WeatherRadarMetadataRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private WeatherRadarMetadata createMetadata(LocalDateTime timestamp, float latitude, float longitude,
            float precipMax, float precipAvg) {
        WeatherRadarMetadata metadata = new WeatherRadarMetadata();
        metadata.setTimestamp(timestamp);
        metadata.setLatitude(latitude);
        metadata.setLongitude(longitude);
        metadata.setDistance(10000);
        metadata.setPrecipitationMin(0.0f);
        metadata.setPrecipitationMax(precipMax);
        metadata.setPrecipitationAvg(precipAvg);
        metadata.setCoveragePercent(50.0f);
        metadata.setSignificantRainCells(10);
        metadata.setTotalCells(100);
        return metadata;
    }

    @Nested
    @DisplayName("Most recent metadata queries")
    class MostRecentTests {

        @Test
        @DisplayName("should find top metadata by timestamp")
        void shouldFindTopByOrderByTimestampDesc() {
            // Given
            repository.save(createMetadata(LocalDateTime.now().minusHours(2), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 48.137f, 11.576f, 3.0f, 1.5f));

            // When
            Optional<WeatherRadarMetadata> result = repository.findTopByOrderByTimestampDesc();

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getLatitude()).isEqualTo(48.137f);
        }

        @Test
        @DisplayName("should return empty when no metadata exists")
        void shouldReturnEmptyWhenNoMetadata() {
            // When
            Optional<WeatherRadarMetadata> result = repository.findTopByOrderByTimestampDesc();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Time range queries")
    class TimeRangeTests {

        @Test
        @DisplayName("should find metadata by timestamp between")
        void shouldFindByTimestampBetween() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

            repository.save(createMetadata(LocalDateTime.of(2024, 1, 15, 12, 0), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.of(2024, 2, 1, 12, 0), 48.137f, 11.576f, 3.0f, 1.5f));
            repository.save(createMetadata(LocalDateTime.of(2024, 1, 20, 12, 0), 53.551f, 9.993f, 4.0f, 1.8f));

            // When
            List<WeatherRadarMetadata> results = repository.findByTimestampBetweenOrderByTimestampDesc(start, end);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WeatherRadarMetadata::getLatitude)
                    .containsExactlyInAnyOrder(52.52f, 53.551f);
        }

        @Test
        @DisplayName("should order metadata by timestamp descending")
        void shouldOrderByTimestampDesc() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

            repository.save(createMetadata(LocalDateTime.of(2024, 1, 10, 12, 0), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.of(2024, 1, 20, 12, 0), 48.137f, 11.576f, 3.0f, 1.5f));

            // When
            List<WeatherRadarMetadata> results = repository.findByTimestampBetweenOrderByTimestampDesc(start, end);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getTimestamp()).isAfter(results.get(1).getTimestamp());
        }

        @Test
        @DisplayName("should count metadata by timestamp between")
        void shouldCountByTimestampBetween() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

            repository.save(createMetadata(LocalDateTime.of(2024, 1, 15, 12, 0), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.of(2024, 2, 1, 12, 0), 48.137f, 11.576f, 3.0f, 1.5f));
            repository.save(createMetadata(LocalDateTime.of(2024, 1, 20, 12, 0), 53.551f, 9.993f, 4.0f, 1.8f));

            // When
            long count = repository.countByTimestampBetween(start, end);

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Recent metadata queries")
    class RecentMetadataTests {

        @Test
        @DisplayName("should find recent metadata since timestamp")
        void shouldFindRecentMetadata() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createMetadata(LocalDateTime.now().minusHours(2), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(10), 48.137f, 11.576f, 3.0f, 1.5f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 53.551f, 9.993f, 4.0f, 1.8f));

            // When
            List<WeatherRadarMetadata> results = repository.findRecentMetadata(since);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WeatherRadarMetadata::getLatitude)
                    .containsExactlyInAnyOrder(52.52f, 53.551f);
        }

        @Test
        @DisplayName("should order recent metadata by timestamp descending")
        void shouldOrderRecentMetadataByTimestampDesc() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createMetadata(LocalDateTime.now().minusHours(5), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 48.137f, 11.576f, 3.0f, 1.5f));

            // When
            List<WeatherRadarMetadata> results = repository.findRecentMetadata(since);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getTimestamp()).isAfter(results.get(1).getTimestamp());
        }
    }

    @Nested
    @DisplayName("Location-based queries")
    class LocationTests {

        @Test
        @DisplayName("should find metadata by approximate location")
        void shouldFindByLocationApprox() {
            // Given
            repository.save(createMetadata(LocalDateTime.now(), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.now(), 52.51f, 13.395f, 3.0f, 1.5f)); // Close to first
            repository.save(createMetadata(LocalDateTime.now(), 48.137f, 11.576f, 4.0f, 1.8f)); // Far away

            // When - Search around Berlin (52.52, 13.405)
            List<WeatherRadarMetadata> results = repository.findByLocationApprox(52.52f, 13.405f);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).allMatch(m -> Math.abs(m.getLatitude() - 52.52f) < 0.1f);
        }

        @Test
        @DisplayName("should order location results by timestamp descending")
        void shouldOrderLocationResultsByTimestampDesc() {
            // Given
            repository.save(createMetadata(LocalDateTime.now().minusHours(2), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 52.51f, 13.395f, 3.0f, 1.5f));

            // When
            List<WeatherRadarMetadata> results = repository.findByLocationApprox(52.52f, 13.405f);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getTimestamp()).isAfter(results.get(1).getTimestamp());
        }
    }

    @Nested
    @DisplayName("Precipitation filtering")
    class PrecipitationTests {

        @Test
        @DisplayName("should find metadata with significant precipitation")
        void shouldFindWithSignificantPrecipitation() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createMetadata(LocalDateTime.now().minusHours(2), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 48.137f, 11.576f, 1.0f, 0.5f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(3), 53.551f, 9.993f, 8.0f, 3.0f));

            // When
            List<WeatherRadarMetadata> results = repository.findWithSignificantPrecipitation(4.0f, since);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WeatherRadarMetadata::getPrecipitationMax)
                    .allMatch(max -> max > 4.0f);
        }

        @Test
        @DisplayName("should order significant precipitation results by timestamp descending")
        void shouldOrderSignificantPrecipitationByTimestampDesc() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createMetadata(LocalDateTime.now().minusHours(5), 52.52f, 13.405f, 5.0f, 2.0f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 48.137f, 11.576f, 8.0f, 3.0f));

            // When
            List<WeatherRadarMetadata> results = repository.findWithSignificantPrecipitation(4.0f, since);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getTimestamp()).isAfter(results.get(1).getTimestamp());
        }

        @Test
        @DisplayName("should exclude old data when finding significant precipitation")
        void shouldExcludeOldDataForSignificantPrecipitation() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createMetadata(LocalDateTime.now().minusHours(10), 52.52f, 13.405f, 10.0f, 5.0f));
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 48.137f, 11.576f, 8.0f, 3.0f));

            // When
            List<WeatherRadarMetadata> results = repository.findWithSignificantPrecipitation(4.0f, since);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getLatitude()).isEqualTo(48.137f);
        }
    }

    @Nested
    @DisplayName("Average statistics queries")
    class AverageStatsTests {

        @Test
        @DisplayName("should get average statistics since timestamp")
        void shouldGetAverageStats() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);

            WeatherRadarMetadata metadata1 = createMetadata(LocalDateTime.now().minusHours(2), 52.52f, 13.405f, 5.0f,
                    2.0f);
            metadata1.setCoveragePercent(40.0f);
            repository.save(metadata1);

            WeatherRadarMetadata metadata2 = createMetadata(LocalDateTime.now().minusHours(1), 48.137f, 11.576f, 3.0f,
                    1.0f);
            metadata2.setCoveragePercent(60.0f);
            repository.save(metadata2);

            WeatherRadarMetadata old = createMetadata(LocalDateTime.now().minusHours(10), 53.551f, 9.993f, 10.0f,
                    5.0f);
            old.setCoveragePercent(80.0f);
            repository.save(old);

            // When
            Object[] stats = repository.getAverageStats(since);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats).hasSize(2);

            Double avgPrecipitation = (Double) stats[0];
            Double avgCoverage = (Double) stats[1];

            assertThat(avgPrecipitation).isCloseTo(1.5, org.assertj.core.data.Offset.offset(0.1));
            assertThat(avgCoverage).isCloseTo(50.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("should return null values when no data for average stats")
        void shouldReturnNullWhenNoDataForStats() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);

            // When
            Object[] stats = repository.getAverageStats(since);

            // Then
            assertThat(stats).isNotNull();
            assertThat(stats).hasSize(2);
            assertThat(stats[0]).isNull();
            assertThat(stats[1]).isNull();
        }
    }

    @Nested
    @DisplayName("Deletion operations")
    class DeletionTests {

        @Test
        @DisplayName("should delete old metadata by created date")
        void shouldDeleteOldMetadata() {
            // Given
            LocalDateTime before = LocalDateTime.now().minusDays(30);

            WeatherRadarMetadata old = createMetadata(LocalDateTime.now(), 52.52f, 13.405f, 5.0f, 2.0f);
            old.setCreatedAt(before.minusDays(10));
            repository.save(old);

            WeatherRadarMetadata recent = createMetadata(LocalDateTime.now(), 48.137f, 11.576f, 3.0f, 1.5f);
            recent.setCreatedAt(before.plusDays(1));
            repository.save(recent);

            // When
            repository.deleteOldMetadata(before);
            repository.flush();

            // Then
            List<WeatherRadarMetadata> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getLatitude()).isEqualTo(48.137f);
        }

        @Test
        @DisplayName("should not delete metadata created after cutoff date")
        void shouldNotDeleteRecentMetadata() {
            // Given
            LocalDateTime before = LocalDateTime.now().minusDays(30);

            WeatherRadarMetadata recent1 = createMetadata(LocalDateTime.now(), 52.52f, 13.405f, 5.0f, 2.0f);
            recent1.setCreatedAt(before.plusDays(1));
            repository.save(recent1);

            WeatherRadarMetadata recent2 = createMetadata(LocalDateTime.now(), 48.137f, 11.576f, 3.0f, 1.5f);
            recent2.setCreatedAt(before.plusDays(5));
            repository.save(recent2);

            // When
            repository.deleteOldMetadata(before);
            repository.flush();

            // Then
            List<WeatherRadarMetadata> remaining = repository.findAll();
            assertThat(remaining).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty results for location query")
        void shouldHandleEmptyResultsForLocation() {
            // Given
            repository.save(createMetadata(LocalDateTime.now(), 52.52f, 13.405f, 5.0f, 2.0f));

            // When - Search far away
            List<WeatherRadarMetadata> results = repository.findByLocationApprox(10.0f, 10.0f);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle zero precipitation values")
        void shouldHandleZeroPrecipitation() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 52.52f, 13.405f, 0.0f, 0.0f));

            // When
            List<WeatherRadarMetadata> results = repository.findWithSignificantPrecipitation(0.1f, since);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle exact threshold for significant precipitation")
        void shouldHandleExactThreshold() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createMetadata(LocalDateTime.now().minusHours(1), 52.52f, 13.405f, 5.0f, 2.0f));

            // When
            List<WeatherRadarMetadata> results = repository.findWithSignificantPrecipitation(5.0f, since);

            // Then
            assertThat(results).isEmpty(); // Query uses > not >=
        }
    }
}
