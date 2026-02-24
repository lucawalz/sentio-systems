package org.example.backend.repository;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.HistoricalWeather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HistoricalWeatherRepository}.
 */
@DisplayName("HistoricalWeatherRepository")
class HistoricalWeatherRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private HistoricalWeatherRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private HistoricalWeather createWeather(LocalDate date, String city, String country, float temp) {
        HistoricalWeather weather = new HistoricalWeather();
        weather.setWeatherDate(date);
        weather.setCity(city);
        weather.setCountry(country);
        weather.setMaxTemperature(temp);
        weather.setMinTemperature(temp - 2);
        weather.setCreatedAt(LocalDateTime.now());
        weather.setUpdatedAt(LocalDateTime.now());
        return weather;
    }

    private HistoricalWeather createWeatherForDevice(LocalDate date, String deviceId, float temp) {
        HistoricalWeather weather = new HistoricalWeather();
        weather.setWeatherDate(date);
        weather.setDeviceId(deviceId);
        weather.setMaxTemperature(temp);
        weather.setMinTemperature(temp - 2);
        weather.setCreatedAt(LocalDateTime.now());
        weather.setUpdatedAt(LocalDateTime.now());
        return weather;
    }

    @Nested
    @DisplayName("Date range queries")
    class DateRangeTests {

        @Test
        @DisplayName("should find weather by date range")
        void shouldFindByDateRange() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 5), "Berlin", "Germany", 3.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 10), "Berlin", "Germany", 7.0f));

            // When
            List<HistoricalWeather> results = repository.findByDateRange(
                    LocalDate.of(2024, 1, 3),
                    LocalDate.of(2024, 1, 8));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWeatherDate()).isEqualTo(LocalDate.of(2024, 1, 5));
        }

        @Test
        @DisplayName("should find weather by specific date")
        void shouldFindByWeatherDate() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Munich", "Germany", 6.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 2), "Berlin", "Germany", 4.0f));

            // When
            List<HistoricalWeather> results = repository.findByWeatherDate(LocalDate.of(2024, 1, 1));

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(HistoricalWeather::getWeatherDate)
                    .containsOnly(LocalDate.of(2024, 1, 1));
        }
    }

    @Nested
    @DisplayName("Location-based queries")
    class LocationTests {

        @Test
        @DisplayName("should find by city and date after")
        void shouldFindByCityAndDateAfter() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 5), "Berlin", "Germany", 3.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 10), "Munich", "Germany", 7.0f));

            // When
            List<HistoricalWeather> results = repository.findByCityAndDateAfter(
                    "Berlin",
                    LocalDate.of(2024, 1, 3));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCity()).isEqualTo("Berlin");
            assertThat(results.get(0).getWeatherDate()).isEqualTo(LocalDate.of(2024, 1, 5));
        }

        @Test
        @DisplayName("should find by weather date and location")
        void shouldFindByWeatherDateAndLocation() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Munich", "Germany", 6.0f));

            // When
            Optional<HistoricalWeather> result = repository.findByWeatherDateAndLocation(
                    LocalDate.of(2024, 1, 1),
                    "Berlin",
                    "Germany");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCity()).isEqualTo("Berlin");
            assertThat(result.get().getMaxTemperature()).isEqualTo(5.0f);
        }

        @Test
        @DisplayName("should find by location and date range")
        void shouldFindByLocationAndDateRange() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 5), "Berlin", "Germany", 3.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 10), "Berlin", "Germany", 7.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 5), "Munich", "Germany", 6.0f));

            // When
            List<HistoricalWeather> results = repository.findByLocationAndDateRange(
                    "Berlin",
                    "Germany",
                    LocalDate.of(2024, 1, 3),
                    LocalDate.of(2024, 1, 8));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWeatherDate()).isEqualTo(LocalDate.of(2024, 1, 5));
        }

        @Test
        @DisplayName("should count by location")
        void shouldCountByLocation() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 2), "Berlin", "Germany", 3.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 3), "Munich", "Germany", 6.0f));

            // When
            long berlinCount = repository.countByLocation("Berlin", "Germany");
            long munichCount = repository.countByLocation("Munich", "Germany");

            // Then
            assertThat(berlinCount).isEqualTo(2);
            assertThat(munichCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Deletion queries")
    class DeletionTests {

        @Test
        @DisplayName("should delete old historical weather by created date")
        void shouldDeleteOldHistoricalWeather() {
            // Given
            HistoricalWeather old = createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f);
            old.setCreatedAt(LocalDateTime.now().minusDays(10));
            repository.save(old);

            HistoricalWeather recent = createWeather(LocalDate.of(2024, 1, 2), "Berlin", "Germany", 6.0f);
            recent.setCreatedAt(LocalDateTime.now().minusDays(1));
            repository.save(recent);

            // When
            repository.deleteOldHistoricalWeather(LocalDateTime.now().minusDays(5));
            repository.flush();

            // Then
            List<HistoricalWeather> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getWeatherDate()).isEqualTo(LocalDate.of(2024, 1, 2));
        }

        @Test
        @DisplayName("should delete expired historical weather by weather date")
        void shouldDeleteExpiredHistoricalWeather() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 2, 1), "Berlin", "Germany", 6.0f));

            // When
            repository.deleteExpiredHistoricalWeather(LocalDate.of(2024, 1, 15));
            repository.flush();

            // Then
            List<HistoricalWeather> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getWeatherDate()).isEqualTo(LocalDate.of(2024, 2, 1));
        }
    }

    @Nested
    @DisplayName("Utility queries")
    class UtilityTests {

        @Test
        @DisplayName("should find top by weather date ordered by created at")
        void shouldFindTopByWeatherDateOrderByCreatedAt() {
            // Given
            HistoricalWeather old = createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f);
            old.setCreatedAt(LocalDateTime.now().minusHours(2));
            repository.save(old);

            HistoricalWeather recent = createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 6.0f);
            recent.setCreatedAt(LocalDateTime.now());
            repository.save(recent);

            // When
            HistoricalWeather result = repository.findTopByWeatherDateOrderByCreatedAtDesc(LocalDate.of(2024, 1, 1));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMaxTemperature()).isEqualTo(6.0f);
        }

        @Test
        @DisplayName("should find recent historical weather")
        void shouldFindRecentHistoricalWeather() {
            // Given
            HistoricalWeather old = createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f);
            old.setCreatedAt(LocalDateTime.now().minusDays(2));
            repository.save(old);

            HistoricalWeather recent = createWeather(LocalDate.of(2024, 1, 2), "Berlin", "Germany", 6.0f);
            recent.setCreatedAt(LocalDateTime.now().minusHours(1));
            repository.save(recent);

            // When
            List<HistoricalWeather> results = repository.findRecentHistoricalWeather(
                    LocalDateTime.now().minusDays(1));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWeatherDate()).isEqualTo(LocalDate.of(2024, 1, 2));
        }

        @Test
        @DisplayName("should find distinct cities with historical weather")
        void shouldFindDistinctCities() {
            // Given
            repository.save(createWeather(LocalDate.now(), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.now(), "Berlin", "Germany", 6.0f));
            repository.save(createWeather(LocalDate.now(), "Munich", "Germany", 7.0f));
            repository.save(createWeather(LocalDate.now(), "Munich", "Germany", 4.0f));

            // When
            List<String> cities = repository.findDistinctCitiesWithHistoricalWeather(
                    LocalDate.now().minusDays(5));

            // Then
            assertThat(cities).hasSize(2);
            assertThat(cities).containsExactlyInAnyOrder("Berlin", "Munich");
        }

        @Test
        @DisplayName("should find by dates and location")
        void shouldFindByDatesAndLocation() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 5), "Berlin", "Germany", 6.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 10), "Berlin", "Germany", 7.0f));

            // When
            List<HistoricalWeather> results = repository.findByDatesAndLocation(
                    List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10)),
                    "Berlin",
                    "Germany");

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(HistoricalWeather::getWeatherDate)
                    .containsExactlyInAnyOrder(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        }

        @Test
        @DisplayName("should find existing dates in range")
        void shouldFindExistingDatesInRange() {
            // Given
            repository.save(createWeather(LocalDate.of(2024, 1, 1), "Berlin", "Germany", 5.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 5), "Berlin", "Germany", 6.0f));
            repository.save(createWeather(LocalDate.of(2024, 1, 10), "Berlin", "Germany", 7.0f));

            // When
            List<LocalDate> existingDates = repository.findExistingDatesInRange(
                    "Berlin",
                    "Germany",
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 12));

            // Then
            assertThat(existingDates).hasSize(3);
            assertThat(existingDates).contains(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 5),
                    LocalDate.of(2024, 1, 10));
        }
    }

    @Nested
    @DisplayName("Device-specific queries")
    class DeviceSpecificTests {

        @Test
        @DisplayName("should find existing dates for device")
        void shouldFindExistingDatesForDevice() {
            // Given
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 1), "device-001", 5.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 5), "device-001", 6.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 10), "device-002", 7.0f));

            // When
            List<LocalDate> existingDates = repository.findExistingDatesForDevice(
                    "device-001",
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 12));

            // Then
            assertThat(existingDates).hasSize(2);
            assertThat(existingDates).contains(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5));
        }

        @Test
        @DisplayName("should find by weather date and device ID")
        void shouldFindByWeatherDateAndDeviceId() {
            // Given
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 1), "device-001", 5.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 1), "device-002", 6.0f));

            // When
            Optional<HistoricalWeather> result = repository.findByWeatherDateAndDeviceId(
                    LocalDate.of(2024, 1, 1),
                    "device-001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getDeviceId()).isEqualTo("device-001");
            assertThat(result.get().getMaxTemperature()).isEqualTo(5.0f);
        }

        @Test
        @DisplayName("should find by dates and device ID")
        void shouldFindByDatesAndDeviceId() {
            // Given
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 1), "device-001", 5.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 5), "device-001", 6.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 10), "device-001", 7.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 5), "device-002", 8.0f));

            // When
            List<HistoricalWeather> results = repository.findByDatesAndDeviceId(
                    List.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10)),
                    "device-001");

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(HistoricalWeather::getWeatherDate)
                    .containsExactlyInAnyOrder(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 10));
        }

        @Test
        @DisplayName("should count by device ID")
        void shouldCountByDeviceId() {
            // Given
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 1), "device-001", 5.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 2), "device-001", 6.0f));
            repository.save(createWeatherForDevice(LocalDate.of(2024, 1, 3), "device-002", 7.0f));

            // When
            long device1Count = repository.countByDeviceId("device-001");
            long device2Count = repository.countByDeviceId("device-002");

            // Then
            assertThat(device1Count).isEqualTo(2);
            assertThat(device2Count).isEqualTo(1);
        }
    }
}
