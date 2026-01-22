package org.example.backend.repository;

import org.example.backend.model.RaspiWeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RaspiWeatherDataRepository.
 */
class RaspiWeatherDataRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private RaspiWeatherDataRepository repository;

    private LocalDateTime now;
    private LocalDateTime oneHourAgo;
    private LocalDateTime oneDayAgo;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        now = LocalDateTime.now();
        oneHourAgo = now.minusHours(1);
        oneDayAgo = now.minusDays(1);
    }

    @Test
    void testSaveAndFindById() {
        RaspiWeatherData data = createWeatherData(20f, 60f, 1013f, "device-1");
        RaspiWeatherData saved = repository.save(data);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindRecentData() {
        repository.save(createWeatherDataWithTimestamp(20f, 60f, 1013f, oneDayAgo, "device-1"));
        repository.save(createWeatherDataWithTimestamp(21f, 61f, 1014f, oneHourAgo, "device-1"));
        repository.save(createWeatherDataWithTimestamp(22f, 62f, 1015f, now, "device-1"));

        List<RaspiWeatherData> recent = repository.findRecentData(oneHourAgo);

        assertThat(recent).hasSize(2);
    }

    @Test
    void testFindDataBetween() {
        repository.save(createWeatherDataWithTimestamp(20f, 60f, 1013f, oneDayAgo.minusDays(1), "device-1"));
        repository.save(createWeatherDataWithTimestamp(21f, 61f, 1014f, oneHourAgo, "device-1"));
        repository.save(createWeatherDataWithTimestamp(22f, 62f, 1015f, now, "device-1"));

        List<RaspiWeatherData> inRange = repository.findDataBetween(oneDayAgo, now);

        assertThat(inRange).hasSize(2);
    }

    @Test
    void testGetAverageTemperatureSince() {
        repository.save(createWeatherDataWithTimestamp(20f, 60f, 1013f, oneHourAgo, "device-1"));
        repository.save(createWeatherDataWithTimestamp(22f, 61f, 1014f, oneHourAgo, "device-1"));

        Double avgTemp = repository.getAverageTemperatureSince(oneDayAgo);

        assertThat(avgTemp).isEqualTo(21.0);
    }

    @Test
    void testFindByDeviceIdIn() {
        repository.save(createWeatherDataWithTimestamp(20f, 60f, 1013f, now, "device-1"));
        repository.save(createWeatherDataWithTimestamp(21f, 61f, 1014f, now, "device-2"));
        repository.save(createWeatherDataWithTimestamp(22f, 62f, 1015f, now, "device-3"));

        List<RaspiWeatherData> results = repository.findByDeviceIdInOrderByTimestampDesc(
                Arrays.asList("device-1", "device-2"));

        assertThat(results).hasSize(2);
    }

    @Test
    void testCountByDeviceIdIn() {
        repository.save(createWeatherData(20f, 60f, 1013f, "device-1"));
        repository.save(createWeatherData(21f, 61f, 1014f, "device-1"));
        repository.save(createWeatherData(22f, 62f, 1015f, "device-2"));
        repository.save(createWeatherData(23f, 63f, 1016f, "device-3"));

        Long count = repository.countByDeviceIdIn(Arrays.asList("device-1", "device-2"));

        assertThat(count).isEqualTo(3);
    }

    private RaspiWeatherData createWeatherData(float temp, float humidity, float pressure, String deviceId) {
        RaspiWeatherData data = new RaspiWeatherData();
        data.setTemperature(temp);
        data.setHumidity(humidity);
        data.setPressure(pressure);
        data.setLux(500f);
        data.setUvi(3f);
        data.setTimestamp(now);
        data.setDeviceId(deviceId);
        data.setLocation("Test Location");
        return data;
    }

    private RaspiWeatherData createWeatherDataWithTimestamp(float temp, float humidity, float pressure,
            LocalDateTime timestamp, String deviceId) {
        RaspiWeatherData data = createWeatherData(temp, humidity, pressure, deviceId);
        data.setTimestamp(timestamp);
        return data;
    }
}
