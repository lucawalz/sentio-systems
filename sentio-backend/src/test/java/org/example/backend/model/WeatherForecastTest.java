package org.example.backend.model;

import org.example.backend.repository.WeatherForecastRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class WeatherForecastRepositoryTest {

    @Autowired
    private WeatherForecastRepository repository;

    @Autowired
    private EntityManager em;

    private WeatherForecast createBaseForecast() {
        WeatherForecast wf = new WeatherForecast();
        wf.setForecastDate(LocalDate.now());
        wf.setForecastDateTime(LocalDateTime.of(2025, 1, 1, 12, 0));
        wf.setCity("Berlin");
        wf.setCountry("Germany");
        return wf;
    }

    @Test
    void testPrePersistSetsTimestamps() {
        WeatherForecast wf = createBaseForecast();

        WeatherForecast saved = repository.save(wf);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
    }

    @Test
    void testPreUpdateUpdatesTimestamp() throws InterruptedException {
        WeatherForecast wf = repository.save(createBaseForecast());

        LocalDateTime created = wf.getCreatedAt();
        LocalDateTime updated = wf.getUpdatedAt();

        Thread.sleep(1100); // <--Large enough (1.1s) for DB precision

        wf.setDescription("Updated");
        WeatherForecast saved = repository.saveAndFlush(wf); //<-- flush forces @PreUpdate

        assertEquals(created, saved.getCreatedAt());
        assertTrue(saved.getUpdatedAt().isAfter(updated));
    }

    @Test
    void testUniqueConstraint() {
        WeatherForecast wf1 = createBaseForecast();
        WeatherForecast wf2 = createBaseForecast();

        repository.saveAndFlush(wf1);

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.saveAndFlush(wf2); // This immediately triggers the DB constraint.
        });
    }
}