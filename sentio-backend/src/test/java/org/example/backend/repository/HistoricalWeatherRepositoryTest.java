package org.example.backend.repository;

import org.example.backend.model.HistoricalWeather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalWeatherRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private HistoricalWeatherRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testFindExistingDatesForDevice() {
        String deviceId = "device-1";
        LocalDate d1 = LocalDate.now().minusDays(5);
        LocalDate d2 = LocalDate.now().minusDays(3);
        LocalDate d3 = LocalDate.now().minusDays(1);

        repository.save(createHistoricalRecord(d1, deviceId));
        repository.save(createHistoricalRecord(d2, deviceId));
        repository.save(createHistoricalRecord(d3, "other-device")); // Should not be found

        List<LocalDate> existingDates = repository.findExistingDatesForDevice(
                deviceId,
                LocalDate.now().minusDays(10),
                LocalDate.now());

        assertThat(existingDates).hasSize(2);
        assertThat(existingDates).containsExactlyInAnyOrder(d1, d2);
    }

    @Test
    void testFindByWeatherDateAndDeviceId() {
        String deviceId = "device-ABC";
        LocalDate date = LocalDate.of(2023, 10, 15);

        repository.save(createHistoricalRecord(date, deviceId));

        Optional<HistoricalWeather> result = repository.findByWeatherDateAndDeviceId(date, deviceId);

        assertThat(result).isPresent();
        assertThat(result.get().getDeviceId()).isEqualTo(deviceId);
        assertThat(result.get().getWeatherDate()).isEqualTo(date);
    }

    @Test
    void testFindByDatesAndDeviceId() {
        String deviceId = "device-X";
        LocalDate d1 = LocalDate.of(2023, 1, 1);
        LocalDate d2 = LocalDate.of(2023, 1, 2);
        LocalDate d3 = LocalDate.of(2023, 1, 3);

        repository.save(createHistoricalRecord(d1, deviceId));
        repository.save(createHistoricalRecord(d2, deviceId));
        repository.save(createHistoricalRecord(d3, "other"));

        List<HistoricalWeather> result = repository.findByDatesAndDeviceId(
                Arrays.asList(d1, d2, LocalDate.of(2023, 1, 5)), // include non-existent date
                deviceId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(HistoricalWeather::getWeatherDate).containsExactlyInAnyOrder(d1, d2);
    }

    @Test
    void testCountByDeviceId() {
        repository.save(createHistoricalRecord(LocalDate.now(), "dev1"));
        repository.save(createHistoricalRecord(LocalDate.now().minusDays(1), "dev1"));
        repository.save(createHistoricalRecord(LocalDate.now(), "dev2"));

        long count = repository.countByDeviceId("dev1");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void testFindByLocationAndDateRange() {
        HistoricalWeather h1 = createHistoricalRecord(LocalDate.of(2023, 5, 1), "d1");
        h1.setCity("Berlin");
        h1.setCountry("Germany");
        repository.save(h1);

        HistoricalWeather h2 = createHistoricalRecord(LocalDate.of(2023, 5, 5), "d1");
        h2.setCity("Berlin");
        h2.setCountry("Germany");
        repository.save(h2);

        HistoricalWeather h3 = createHistoricalRecord(LocalDate.of(2023, 5, 5), "d2");
        h3.setCity("Paris"); // Wrong city
        h3.setCountry("France");
        repository.save(h3);

        List<HistoricalWeather> result = repository.findByLocationAndDateRange(
                "Berlin", "Germany",
                LocalDate.of(2023, 4, 30),
                LocalDate.of(2023, 5, 6));

        assertThat(result).hasSize(2);
    }

    private HistoricalWeather createHistoricalRecord(LocalDate date, String deviceId) {
        HistoricalWeather hw = new HistoricalWeather();
        hw.setWeatherDate(date);
        hw.setDeviceId(deviceId);
        hw.setCreatedAt(LocalDateTime.now());
        hw.setUpdatedAt(LocalDateTime.now());
        hw.setCity("City"); // Dummy
        hw.setCountry("Country"); // Dummy
        return hw;
    }
}
