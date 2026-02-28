package org.example.backend.model;

import org.example.backend.BaseDataJpaTest;
import org.example.backend.repository.WeatherAlertRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JPA slice tests for {@link WeatherAlert} entity and repository.
 * Uses shared Testcontainers PostgreSQL via {@link BaseDataJpaTest}.
 */
class WeatherAlertRepositoryTest extends BaseDataJpaTest {

    @Autowired
    private WeatherAlertRepository repository;

    @Autowired
    private EntityManager em;

    /**
     * Helper method to create a base WeatherAlert entity.
     * Fills only the required mandatory fields for testing.
     * This keeps individual tests small and focused.
     */
    private WeatherAlert createBaseAlert() {
        WeatherAlert wa = new WeatherAlert();
        wa.setAlertId("alert-12345"); // Required unique identifier
        wa.setBrightSkyId(42);// Additional sample metadata
        wa.setStatus("actual");
        LocalDateTime now = LocalDateTime.now(); // Use "now" so tests behave consistently
        // Set effective/expiry times to make alert active by default
        wa.setEffective(now.minusMinutes(5));
        wa.setOnset(now.minusMinutes(5));
        wa.setExpires(now.plusHours(1));
        // Location
        wa.setCity("Berlin");
        wa.setCountry("Germany");
        // Multilingual text content
        wa.setHeadlineEn("Heat warning");
        wa.setHeadlineDe("Hitzewarnung");
        wa.setDescriptionEn("High temperatures expected");
        wa.setDescriptionDe("Hohe Temperaturen erwartet");
        return wa;
    }

    /**
     * Tests that @PrePersist sets createdAt and updatedAt
     * before the entity is stored in the database.
     */
    @Test
    void testPrePersistSetsTimestamps() {
        WeatherAlert wa = createBaseAlert();
        // saveAndFlush forces Hibernate to execute SQL immediately
        WeatherAlert saved = repository.saveAndFlush(wa);

        assertNotNull(saved.getId(), "ID should be generated");
        assertNotNull(saved.getCreatedAt(), "createdAt should be set");
        assertNotNull(saved.getUpdatedAt(), "updatedAt should be set");
        // On creation, both timestamps must be exactly the same
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt(), "createdAt and updatedAt should be equal on create");
    }

    /**
     * Tests that @PreUpdate updates updatedAt while keeping createdAt unchanged.
     */
    @Test
    void testPreUpdateUpdatesTimestamp() throws InterruptedException {
        WeatherAlert wa = repository.saveAndFlush(createBaseAlert());

        LocalDateTime created = wa.getCreatedAt();
        LocalDateTime updated = wa.getUpdatedAt();

        // Small delay to ensure timestamp difference
        Thread.sleep(1100); // Wait briefly so timestamps differ

        // Trigger update
        wa.setHeadlineEn("Updated headline");
        WeatherAlert saved = repository.saveAndFlush(wa);
        // createdAt must never change
        assertEquals(created, saved.getCreatedAt(), "createdAt must remain unchanged");
        // updatedAt must be newer than the previous value
        assertTrue(saved.getUpdatedAt().isAfter(updated), "updatedAt must be after previous updated timestamp");
    }

    /**
     * Tests that isActive() returns true when the current time lies
     * between effective and expires timestamps.
     */
    @Test
    void testIsActiveTrueWhenWithinEffectiveExpires() {
        WeatherAlert wa = createBaseAlert();
        wa.setEffective(LocalDateTime.now().minusMinutes(10));
        wa.setExpires(LocalDateTime.now().plusMinutes(10));

        assertTrue(wa.isActive(), "Alert should be active when now ∈ [effective, expires)");
    }

    /**
     * Tests that isActive() returns false when the alert has expired.
     */
    @Test
    void testIsActiveFalseWhenExpired() {
        WeatherAlert wa = createBaseAlert();
        wa.setEffective(LocalDateTime.now().minusHours(2));
        wa.setExpires(LocalDateTime.now().minusHours(1));

        assertFalse(wa.isActive(), "Alert should be inactive when expires is in the past");
    }

    /**
     * Tests edge cases where effective or expires is null.
     * - null effective → alert considered active (no start restriction)
     * - null expires → alert never expires
     */
    @Test
    void testIsActiveTrueWhenNoEffectiveOrNoExpires() {
        WeatherAlert wa1 = createBaseAlert();
        wa1.setEffective(null); // no effective -> treated as active start
        wa1.setExpires(LocalDateTime.now().plusHours(1));
        assertTrue(wa1.isActive(), "Alert with null effective and future expires should be active");

        WeatherAlert wa2 = createBaseAlert();
        wa2.setEffective(LocalDateTime.now().minusHours(1));
        wa2.setExpires(null); // no expires -> treated as no expiry
        assertTrue(wa2.isActive(), "Alert with null expires should be active");
    }

    /**
     * Tests behavior of localized headline/description getter methods.
     * If German version exists and preferGerman = true → return German.
     * Otherwise → fallback to English.
     */
    @Test
    void testLocalizedHeadlineAndDescription() {
        WeatherAlert wa = createBaseAlert();

        // prefer German -> German headline/description present
        assertEquals("Hitzewarnung", wa.getLocalizedHeadline(true));
        assertEquals("Heat warning", wa.getLocalizedHeadline(false));

        assertEquals("Hohe Temperaturen erwartet", wa.getLocalizedDescription(true));
        assertEquals("High temperatures expected", wa.getLocalizedDescription(false));

        // if German is missing => fallback to English
        wa.setHeadlineDe(null);
        wa.setDescriptionDe(null);
        assertEquals("Heat warning", wa.getLocalizedHeadline(true), "fallback to English headline");
        assertEquals("High temperatures expected", wa.getLocalizedDescription(true), "fallback to English description");
    }

    /**
     * Tests that the unique constraint on alertId is enforced.
     * Two alerts with the same alertId must cause a
     * DataIntegrityViolationException.
     */
    @Test
    void testUniqueConstraintOnAlertId() {
        WeatherAlert wa1 = createBaseAlert();
        WeatherAlert wa2 = createBaseAlert();

        wa1.setAlertId("unique-alert-1");
        wa1.setDeviceId("device-1");
        wa2.setAlertId("unique-alert-1"); // duplicate → DB should reject it
        wa2.setDeviceId("device-1");

        repository.saveAndFlush(wa1);

        // Spring Data translates the DB/Hibernate exception into
        // DataIntegrityViolationException
        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.saveAndFlush(wa2);
        });
    }
}
