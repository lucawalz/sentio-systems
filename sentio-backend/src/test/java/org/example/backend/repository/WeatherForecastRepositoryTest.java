package org.example.backend.repository;

import org.example.backend.model.WeatherForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for WeatherForecastRepository.
 */
class WeatherForecastRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private WeatherForecastRepository repository;

    private LocalDate today;
    private LocalDate tomorrow;
    private LocalDate nextWeek;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        today = LocalDate.now();
        tomorrow = today.plusDays(1);
        nextWeek = today.plusDays(7);
        now = LocalDateTime.now();
    }

    @Test
    void testSaveAndFindById() {
        WeatherForecast forecast = createForecast(today, "Berlin", "Germany", "device-1");
        WeatherForecast saved = repository.save(forecast);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindByDateRange() {
        repository.save(createForecast(today, "Berlin", "Germany", "device-1"));
        repository.save(createForecast(tomorrow, "Berlin", "Germany", "device-1"));
        repository.save(createForecast(nextWeek, "Berlin", "Germany", "device-1"));

        List<WeatherForecast> forecasts = repository.findByDateRange(today, tomorrow.plusDays(1));

        assertThat(forecasts).hasSize(2);
    }

    @Test
    void testFindUpcomingForecasts() {
        repository.save(createForecast(today.minusDays(1), "Berlin", "Germany", "device-1"));
        repository.save(createForecast(today, "Berlin", "Germany", "device-1"));
        repository.save(createForecast(tomorrow, "Berlin", "Germany", "device-1"));

        List<WeatherForecast> upcoming = repository.findUpcomingForecasts(today);

        assertThat(upcoming).hasSize(2); // today and tomorrow
    }

    @Test
    void testFindByForecastDateTimeAndLocation() {
        LocalDateTime forecastTime = now.plusHours(3);
        repository.save(createForecastWithDateTime(forecastTime, "Berlin", "Germany", "device-1"));

        Optional<WeatherForecast> found = repository.findByForecastDateTimeAndLocation(
                forecastTime, "Berlin", "Germany");

        assertThat(found).isPresent();
        assertThat(found.get().getCity()).isEqualTo("Berlin");
    }

    @Test
    void testFindByDeviceIdAndDateRange() {
        repository.save(createForecast(today, "Berlin", "Germany", "device-1"));
        repository.save(createForecast(tomorrow, "Berlin", "Germany", "device-1"));
        repository.save(createForecast(today, "Munich", "Germany", "device-2"));

        List<WeatherForecast> device1Forecasts = repository.findByDeviceIdAndDateRange(
                "device-1", today, tomorrow.plusDays(1));

        assertThat(device1Forecasts).hasSize(2);
        assertThat(device1Forecasts).allMatch(f -> f.getDeviceId().equals("device-1"));
    }

    @Test
    void testCountByLocation() {
        repository.save(createForecast(today, "Berlin", "Germany", "device-1"));
        repository.save(createForecast(tomorrow, "Berlin", "Germany", "device-1"));
        repository.save(createForecast(today, "Munich", "Germany", "device-2"));

        long count = repository.countByLocation("Berlin", "Germany");

        assertThat(count).isEqualTo(2);
    }

    private WeatherForecast createForecast(LocalDate date, String city, String country, String deviceId) {
        WeatherForecast forecast = new WeatherForecast();
        forecast.setForecastDate(date);
        forecast.setForecastDateTime(date.atStartOfDay().plusHours(12));
        forecast.setCity(city);
        forecast.setCountry(country);
        forecast.setDeviceId(deviceId);
        forecast.setTemperature(20f);
        forecast.setHumidity(60f);
        forecast.setLatitude(52.52f);
        forecast.setLongitude(13.41f);
        return forecast;
    }

    private WeatherForecast createForecastWithDateTime(LocalDateTime dateTime, String city, String country,
            String deviceId) {
        WeatherForecast forecast = new WeatherForecast();
        forecast.setForecastDate(dateTime.toLocalDate());
        forecast.setForecastDateTime(dateTime);
        forecast.setCity(city);
        forecast.setCountry(country);
        forecast.setDeviceId(deviceId);
        forecast.setTemperature(20f);
        forecast.setHumidity(60f);
        forecast.setLatitude(52.52f);
        forecast.setLongitude(13.41f);
        return forecast;
    }
}
