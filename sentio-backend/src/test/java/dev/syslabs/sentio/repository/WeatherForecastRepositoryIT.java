package dev.syslabs.sentio.repository;

import dev.syslabs.sentio.BaseIntegrationTest;
import dev.syslabs.sentio.model.WeatherForecast;
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
 * Integration tests for {@link WeatherForecastRepository}.
 * Validates custom queries work correctly with PostgreSQL.
 */
class WeatherForecastRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private WeatherForecastRepository repository;

    private WeatherForecast createForecast(String city, LocalDate date, LocalDateTime dateTime) {
        WeatherForecast forecast = new WeatherForecast();
        forecast.setCity(city);
        forecast.setCountry("Germany");
        forecast.setForecastDate(date);
        forecast.setForecastDateTime(dateTime);
        forecast.setTemperature(20.0f);
        forecast.setHumidity(60.0f);
        return forecast;
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("findByDateRange")
    class FindByDateRangeTests {

        @Test
        @DisplayName("should return forecasts within date range")
        void shouldReturnForecastsWithinRange() {
            // Given
            LocalDate today = LocalDate.now();
            repository.save(createForecast("Berlin", today, today.atTime(12, 0)));
            repository.save(createForecast("Berlin", today.plusDays(1), today.plusDays(1).atTime(12, 0)));
            repository.save(createForecast("Berlin", today.plusDays(5), today.plusDays(5).atTime(12, 0)));

            // When
            List<WeatherForecast> result = repository.findByDateRange(today, today.plusDays(2));

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should return empty list when no forecasts in range")
        void shouldReturnEmptyWhenNoMatch() {
            // Given
            LocalDate today = LocalDate.now();
            repository.save(createForecast("Berlin", today.minusDays(10), today.minusDays(10).atTime(12, 0)));

            // When
            List<WeatherForecast> result = repository.findByDateRange(today, today.plusDays(7));

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByForecastDateTimeAndLocation")
    class FindByForecastDateTimeAndLocationTests {

        @Test
        @DisplayName("should find forecast by unique key")
        void shouldFindByUniqueKey() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDateTime dateTime = today.atTime(14, 0);
            repository.save(createForecast("Munich", today, dateTime));

            // When
            Optional<WeatherForecast> result = repository.findByForecastDateTimeAndLocation(
                    dateTime, "Munich", "Germany");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCity()).isEqualTo("Munich");
        }

        @Test
        @DisplayName("should return empty when location differs")
        void shouldReturnEmptyWhenLocationDiffers() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDateTime dateTime = today.atTime(14, 0);
            repository.save(createForecast("Munich", today, dateTime));

            // When
            Optional<WeatherForecast> result = repository.findByForecastDateTimeAndLocation(
                    dateTime, "Berlin", "Germany");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Device-scoped queries")
    class DeviceScopedTests {

        @Test
        @DisplayName("should find forecasts by device ID and date range")
        void shouldFindByDeviceIdAndDateRange() {
            // Given
            LocalDate today = LocalDate.now();
            WeatherForecast forecast = createForecast("Berlin", today, today.atTime(12, 0));
            forecast.setDeviceId("device-123");
            repository.save(forecast);

            WeatherForecast otherDevice = createForecast("Berlin", today, today.atTime(13, 0));
            otherDevice.setDeviceId("device-456");
            repository.save(otherDevice);

            // When
            List<WeatherForecast> result = repository.findByDeviceIdAndDateRange(
                    "device-123", today, today.plusDays(1));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeviceId()).isEqualTo("device-123");
        }
    }

    @Nested
    @DisplayName("Cleanup operations")
    class CleanupTests {

        @Test
        @DisplayName("should delete expired forecasts")
        void shouldDeleteExpiredForecasts() {
            // Given
            LocalDate today = LocalDate.now();
            repository.save(createForecast("Berlin", today.minusDays(10), today.minusDays(10).atTime(12, 0)));
            repository.save(createForecast("Berlin", today, today.atTime(12, 0)));

            // When
            repository.deleteExpiredForecasts(today.minusDays(5));

            // Then
            List<WeatherForecast> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getForecastDate()).isEqualTo(today);
        }
    }
}
