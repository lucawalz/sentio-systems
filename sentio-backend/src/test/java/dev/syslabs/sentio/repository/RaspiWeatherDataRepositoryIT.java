package dev.syslabs.sentio.repository;

import dev.syslabs.sentio.BaseIntegrationTest;
import dev.syslabs.sentio.model.RaspiWeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RaspiWeatherDataRepository}.
 * Validates custom queries for sensor data retrieval and aggregation.
 */
@DisplayName("RaspiWeatherDataRepository")
class RaspiWeatherDataRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private RaspiWeatherDataRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private RaspiWeatherData createWeatherData(String deviceId, LocalDateTime timestamp, float temperature,
            float humidity, float pressure) {
        RaspiWeatherData data = new RaspiWeatherData();
        data.setDeviceId(deviceId);
        data.setTimestamp(timestamp);
        data.setTemperature(temperature);
        data.setHumidity(humidity);
        data.setPressure(pressure);
        data.setLux(500.0f);
        data.setUvi(3.0f);
        return data;
    }

    @Nested
    @DisplayName("Recent data queries")
    class RecentDataTests {

        @Test
        @DisplayName("should find recent data since timestamp")
        void shouldFindRecentData() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(
                    createWeatherData("device-001", LocalDateTime.now().minusHours(10), 19.0f, 65.0f, 1012.0f));

            // When
            List<RaspiWeatherData> results = repository.findRecentData(start);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTemperature()).isEqualTo(20.0f);
        }

        @Test
        @DisplayName("should order recent data by timestamp descending")
        void shouldOrderRecentDataByTimestampDesc() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(5), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(3), 21.0f, 58.0f, 1014.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(1), 22.0f, 55.0f, 1015.0f));

            // When
            List<RaspiWeatherData> results = repository.findRecentData(start);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results.get(0).getTemperature()).isEqualTo(22.0f);
            assertThat(results.get(1).getTemperature()).isEqualTo(21.0f);
            assertThat(results.get(2).getTemperature()).isEqualTo(20.0f);
        }
    }

    @Nested
    @DisplayName("Latest data queries")
    class LatestDataTests {

        @Test
        @DisplayName("should find top data by timestamp")
        void shouldFindTopByOrderByTimestampDesc() {
            // Given
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(1), 21.0f, 58.0f, 1014.0f));

            // When
            RaspiWeatherData result = repository.findTopByOrderByTimestampDesc();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDeviceId()).isEqualTo("device-002");
            assertThat(result.getTemperature()).isEqualTo(21.0f);
        }

        @Test
        @DisplayName("should find all data ordered by timestamp descending")
        void shouldFindAllByOrderByTimestampDesc() {
            // Given
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(3), 19.0f, 65.0f, 1012.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(1), 21.0f, 58.0f, 1014.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));

            // When
            List<RaspiWeatherData> results = repository.findAllByOrderByTimestampDesc();

            // Then
            assertThat(results).hasSize(3);
            assertThat(results.get(0).getTemperature()).isEqualTo(21.0f);
            assertThat(results.get(1).getTemperature()).isEqualTo(20.0f);
            assertThat(results.get(2).getTemperature()).isEqualTo(19.0f);
        }
    }

    @Nested
    @DisplayName("Time range queries")
    class TimeRangeTests {

        @Test
        @DisplayName("should find data between timestamps")
        void shouldFindDataBetween() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 1, 23, 59);

            repository.save(
                    createWeatherData("device-001", LocalDateTime.of(2024, 1, 1, 12, 0), 20.0f, 60.0f, 1013.0f));
            repository.save(
                    createWeatherData("device-001", LocalDateTime.of(2024, 1, 2, 12, 0), 21.0f, 58.0f, 1014.0f));
            repository.save(
                    createWeatherData("device-001", LocalDateTime.of(2024, 1, 1, 18, 0), 19.0f, 65.0f, 1012.0f));

            // When
            List<RaspiWeatherData> results = repository.findDataBetween(start, end);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(RaspiWeatherData::getTemperature)
                    .containsExactlyInAnyOrder(20.0f, 19.0f);
        }

        @Test
        @DisplayName("should order data between timestamps by timestamp descending")
        void shouldOrderDataBetweenByTimestampDesc() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 1, 23, 59);

            repository.save(
                    createWeatherData("device-001", LocalDateTime.of(2024, 1, 1, 10, 0), 19.0f, 65.0f, 1012.0f));
            repository.save(
                    createWeatherData("device-001", LocalDateTime.of(2024, 1, 1, 18, 0), 21.0f, 58.0f, 1014.0f));

            // When
            List<RaspiWeatherData> results = repository.findDataBetween(start, end);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getTimestamp()).isAfter(results.get(1).getTimestamp());
        }
    }

    @Nested
    @DisplayName("Average calculation queries")
    class AverageCalculationTests {

        @Test
        @DisplayName("should calculate average temperature since timestamp")
        void shouldGetAverageTemperatureSince() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(1), 22.0f, 58.0f, 1014.0f));
            repository.save(
                    createWeatherData("device-001", LocalDateTime.now().minusHours(10), 18.0f, 65.0f, 1012.0f));

            // When
            Double avgTemp = repository.getAverageTemperatureSince(start);

            // Then
            assertThat(avgTemp).isCloseTo(21.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("should calculate average humidity since timestamp")
        void shouldGetAverageHumiditySince() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(1), 22.0f, 70.0f, 1014.0f));

            // When
            Double avgHumidity = repository.getAverageHumiditySince(start);

            // Then
            assertThat(avgHumidity).isCloseTo(65.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("should calculate average pressure since timestamp")
        void shouldGetAveragePressureSince() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(1), 22.0f, 58.0f, 1015.0f));

            // When
            Double avgPressure = repository.getAveragePressureSince(start);

            // Then
            assertThat(avgPressure).isCloseTo(1014.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("should return null when no data for average calculation")
        void shouldReturnNullWhenNoDataForAverage() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);

            // When
            Double avgTemp = repository.getAverageTemperatureSince(start);

            // Then
            assertThat(avgTemp).isNull();
        }
    }

    @Nested
    @DisplayName("Device-specific queries")
    class DeviceSpecificTests {

        @Test
        @DisplayName("should find top data by device IDs")
        void shouldFindTopByDeviceIdIn() {
            // Given
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(1), 21.0f, 58.0f, 1014.0f));
            repository.save(createWeatherData("device-003", LocalDateTime.now().minusHours(3), 19.0f, 65.0f, 1012.0f));

            // When
            RaspiWeatherData result = repository.findTopByDeviceIdInOrderByTimestampDesc(
                    List.of("device-001", "device-002"));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getDeviceId()).isEqualTo("device-002");
        }

        @Test
        @DisplayName("should find recent data by devices")
        void shouldFindRecentDataByDevices() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(1), 21.0f, 58.0f, 1014.0f));
            repository.save(createWeatherData("device-003", LocalDateTime.now().minusHours(1), 19.0f, 65.0f, 1012.0f));
            repository.save(
                    createWeatherData("device-001", LocalDateTime.now().minusHours(10), 18.0f, 70.0f, 1011.0f));

            // When
            List<RaspiWeatherData> results = repository.findRecentDataByDevices(
                    List.of("device-001", "device-002"), start);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(RaspiWeatherData::getDeviceId)
                    .containsExactlyInAnyOrder("device-001", "device-002");
        }

        @Test
        @DisplayName("should find all data by device IDs ordered by timestamp")
        void shouldFindByDeviceIdInOrderByTimestampDesc() {
            // Given
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(3), 19.0f, 65.0f, 1012.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(1), 21.0f, 58.0f, 1014.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-003", LocalDateTime.now().minusHours(1), 22.0f, 55.0f, 1015.0f));

            // When
            List<RaspiWeatherData> results = repository.findByDeviceIdInOrderByTimestampDesc(
                    List.of("device-001", "device-002"));

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).extracting(RaspiWeatherData::getDeviceId)
                    .containsExactlyInAnyOrder("device-001", "device-001", "device-002");
            assertThat(results.get(0).getTimestamp()).isAfter(results.get(1).getTimestamp());
        }

        @Test
        @DisplayName("should count data by device IDs")
        void shouldCountByDeviceIdIn() {
            // Given
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(3), 19.0f, 65.0f, 1012.0f));
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(1), 21.0f, 58.0f, 1014.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-003", LocalDateTime.now().minusHours(1), 22.0f, 55.0f, 1015.0f));

            // When
            Long count = repository.countByDeviceIdIn(List.of("device-001", "device-002"));

            // Then
            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Device-specific average queries")
    class DeviceAverageTests {

        @Test
        @DisplayName("should calculate average temperature for devices since timestamp")
        void shouldGetAverageTemperatureSinceForDevices() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(1), 22.0f, 58.0f, 1014.0f));
            repository.save(createWeatherData("device-003", LocalDateTime.now().minusHours(1), 18.0f, 65.0f, 1012.0f));
            repository.save(
                    createWeatherData("device-001", LocalDateTime.now().minusHours(10), 16.0f, 70.0f, 1011.0f));

            // When
            Double avgTemp = repository.getAverageTemperatureSinceForDevices(
                    List.of("device-001", "device-002"), start);

            // Then
            assertThat(avgTemp).isCloseTo(21.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("should calculate average humidity for devices since timestamp")
        void shouldGetAverageHumiditySinceForDevices() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(1), 22.0f, 70.0f, 1014.0f));
            repository.save(createWeatherData("device-003", LocalDateTime.now().minusHours(1), 18.0f, 50.0f, 1012.0f));

            // When
            Double avgHumidity = repository.getAverageHumiditySinceForDevices(
                    List.of("device-001", "device-002"), start);

            // Then
            assertThat(avgHumidity).isCloseTo(65.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("should calculate average pressure for devices since timestamp")
        void shouldGetAveragePressureSinceForDevices() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);
            repository.save(createWeatherData("device-001", LocalDateTime.now().minusHours(2), 20.0f, 60.0f, 1013.0f));
            repository.save(createWeatherData("device-002", LocalDateTime.now().minusHours(1), 22.0f, 58.0f, 1015.0f));
            repository.save(createWeatherData("device-003", LocalDateTime.now().minusHours(1), 18.0f, 65.0f, 1012.0f));

            // When
            Double avgPressure = repository.getAveragePressureSinceForDevices(
                    List.of("device-001", "device-002"), start);

            // Then
            assertThat(avgPressure).isCloseTo(1014.0, org.assertj.core.data.Offset.offset(0.1));
        }

        @Test
        @DisplayName("should return null when no data for devices")
        void shouldReturnNullWhenNoDataForDevices() {
            // Given
            LocalDateTime start = LocalDateTime.now().minusHours(6);

            // When
            Double avgTemp = repository.getAverageTemperatureSinceForDevices(
                    List.of("device-001", "device-002"), start);

            // Then
            assertThat(avgTemp).isNull();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty device list")
        void shouldHandleEmptyDeviceList() {
            // Given
            repository.save(createWeatherData("device-001", LocalDateTime.now(), 20.0f, 60.0f, 1013.0f));

            // When
            List<RaspiWeatherData> results = repository.findByDeviceIdInOrderByTimestampDesc(List.of());

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle non-existent device IDs")
        void shouldHandleNonExistentDeviceIds() {
            // Given
            repository.save(createWeatherData("device-001", LocalDateTime.now(), 20.0f, 60.0f, 1013.0f));

            // When
            List<RaspiWeatherData> results = repository.findByDeviceIdInOrderByTimestampDesc(
                    List.of("non-existent"));

            // Then
            assertThat(results).isEmpty();
        }
    }
}
