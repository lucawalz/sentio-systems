package org.example.backend.repository;

import org.example.backend.model.WeatherRadarMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherRadarMetadataRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private WeatherRadarMetadataRepository repository;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        now = LocalDateTime.now();
    }

    @Test
    void testFindTopByOrderByTimestampDesc() {
        repository.save(createMetadata(now.minusMinutes(20)));
        repository.save(createMetadata(now.minusMinutes(10)));

        Optional<WeatherRadarMetadata> result = repository.findTopByOrderByTimestampDesc();

        assertThat(result).isPresent();
        assertThat(result.get().getTimestamp()).isEqualTo(now.minusMinutes(10));
    }

    @Test
    void testFindWithSignificantPrecipitation() {
        repository.save(createMetadataPrecip(now.minusMinutes(5), 0.5f)); // Low
        repository.save(createMetadataPrecip(now.minusMinutes(10), 5.0f)); // High
        repository.save(createMetadataPrecip(now.minusMinutes(15), 10.0f)); // High

        List<WeatherRadarMetadata> significant = repository.findWithSignificantPrecipitation(1.0f, now.minusHours(1));

        assertThat(significant).hasSize(2);
    }

    @Test
    void testGetAverageStats() {
        repository.save(createMetadataStats(now.minusMinutes(10), 2.0f, 50.0f));
        repository.save(createMetadataStats(now.minusMinutes(20), 4.0f, 100.0f));

        Object[] stats = (Object[]) repository.getAverageStats(now.minusHours(1))[0];

        assertThat((Double) stats[0]).isEqualTo(3.0); // Avg precipitation
        assertThat((Double) stats[1]).isEqualTo(75.0); // Avg coverage
    }

    private WeatherRadarMetadata createMetadata(LocalDateTime timestamp) {
        WeatherRadarMetadata meta = new WeatherRadarMetadata();
        meta.setTimestamp(timestamp);
        meta.setCreatedAt(LocalDateTime.now());
        meta.setSource("DWD-" + timestamp.toString());
        // Mandatory fields based on entity def - Latitude/Longitude/Distance
        meta.setLatitude(52.0f);
        meta.setLongitude(13.0f);
        meta.setDistance(1000);
        return meta;
    }

    private WeatherRadarMetadata createMetadataPrecip(LocalDateTime timestamp, float maxPrecip) {
        WeatherRadarMetadata meta = createMetadata(timestamp);
        meta.setPrecipitationMax(maxPrecip);
        return meta;
    }

    private WeatherRadarMetadata createMetadataStats(LocalDateTime timestamp, float avgPrecip, float coverage) {
        WeatherRadarMetadata meta = createMetadata(timestamp);
        meta.setPrecipitationAvg(avgPrecip);
        meta.setCoveragePercent(coverage);
        return meta;
    }
}
