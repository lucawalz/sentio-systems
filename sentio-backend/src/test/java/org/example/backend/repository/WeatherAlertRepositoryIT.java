package org.example.backend.repository;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.WeatherAlert;
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
 * Integration tests for {@link WeatherAlertRepository}.
 * Validates custom JPQL queries, filtering, and deletion operations with PostgreSQL.
 */
@DisplayName("WeatherAlertRepository")
class WeatherAlertRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private WeatherAlertRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private WeatherAlert createAlert(String alertId, String city, String severity, LocalDateTime effective,
            LocalDateTime expires) {
        WeatherAlert alert = new WeatherAlert();
        alert.setAlertId(alertId);
        alert.setCity(city);
        alert.setCountry("Germany");
        alert.setSeverity(severity);
        alert.setEffective(effective);
        alert.setExpires(expires);
        alert.setLatitude(52.52f);
        alert.setLongitude(13.405f);
        alert.setEventEn("Test Event");
        alert.setHeadlineEn("Test Headline");
        return alert;
    }

    private WeatherAlert createAlertWithDevice(String alertId, String deviceId, LocalDateTime effective,
            LocalDateTime expires) {
        WeatherAlert alert = createAlert(alertId, "Berlin", "moderate", effective, expires);
        alert.setDeviceId(deviceId);
        return alert;
    }

    @Nested
    @DisplayName("Basic query methods")
    class BasicQueryTests {

        @Test
        @DisplayName("should find alert by alertId")
        void shouldFindByAlertId() {
            // Given
            repository.save(createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.now(), null));

            // When
            Optional<WeatherAlert> result = repository.findByAlertId("ALERT-001");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getAlertId()).isEqualTo("ALERT-001");
        }

        @Test
        @DisplayName("should return empty when alertId not found")
        void shouldReturnEmptyWhenAlertIdNotFound() {
            // When
            Optional<WeatherAlert> result = repository.findByAlertId("NON-EXISTENT");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should check if alert exists by alertId")
        void shouldCheckExistsByAlertId() {
            // Given
            repository.save(createAlert("ALERT-002", "Munich", "minor", LocalDateTime.now(), null));

            // When
            boolean exists = repository.existsByAlertId("ALERT-002");
            boolean notExists = repository.existsByAlertId("ALERT-999");

            // Then
            assertThat(exists).isTrue();
            assertThat(notExists).isFalse();
        }
    }

    @Nested
    @DisplayName("Active alerts queries")
    class ActiveAlertsTests {

        @Test
        @DisplayName("should find active alerts with null expiry")
        void shouldFindActiveAlertsWithNullExpiry() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(1), null));
            repository.save(createAlert("ALERT-002", "Munich", "moderate", now.minusHours(2),
                    now.minusHours(1))); // Expired

            // When
            List<WeatherAlert> results = repository.findActiveAlerts(now);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-001");
        }

        @Test
        @DisplayName("should find active alerts that expire in future")
        void shouldFindActiveAlertsThatExpireInFuture() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-002", "Munich", "moderate", now.minusHours(2),
                    now.minusHours(1))); // Expired

            // When
            List<WeatherAlert> results = repository.findActiveAlerts(now);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-001");
        }

        @Test
        @DisplayName("should order active alerts by effective date descending")
        void shouldOrderActiveAlertsByEffectiveDesc() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(5), now.plusHours(1)));
            repository.save(createAlert("ALERT-002", "Munich", "moderate", now.minusHours(2), null));
            repository.save(createAlert("ALERT-003", "Hamburg", "minor", now.minusHours(1), now.plusHours(2)));

            // When
            List<WeatherAlert> results = repository.findActiveAlerts(now);

            // Then
            assertThat(results).hasSize(3);
            assertThat(results).extracting(WeatherAlert::getAlertId)
                    .containsExactly("ALERT-003", "ALERT-002", "ALERT-001");
        }
    }

    @Nested
    @DisplayName("Location-based queries")
    class LocationTests {

        @Test
        @DisplayName("should find alerts by location radius")
        void shouldFindByLocationRadius() {
            // Given
            WeatherAlert alert1 = createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.now(), null);
            alert1.setLatitude(52.52f);
            alert1.setLongitude(13.405f);
            repository.save(alert1);

            WeatherAlert alert2 = createAlert("ALERT-002", "Munich", "moderate", LocalDateTime.now(), null);
            alert2.setLatitude(48.137f);
            alert2.setLongitude(11.576f);
            repository.save(alert2);

            // When - Search around Berlin
            List<WeatherAlert> results = repository.findByLocationRadius(52.0f, 53.0f, 13.0f, 14.0f);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCity()).isEqualTo("Berlin");
        }

        @Test
        @DisplayName("should find alerts by city ignoring case")
        void shouldFindByCityIgnoreCase() {
            // Given
            repository.save(createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.now(), null));
            repository.save(createAlert("ALERT-002", "Berlin", "moderate", LocalDateTime.now(), null));
            repository.save(createAlert("ALERT-003", "Munich", "minor", LocalDateTime.now(), null));

            // When
            List<WeatherAlert> results = repository.findByCityIgnoreCase("berlin");

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WeatherAlert::getCity)
                    .containsOnly("Berlin");
        }

        @Test
        @DisplayName("should find alerts by warn cell ID")
        void shouldFindByWarnCellId() {
            // Given
            WeatherAlert alert1 = createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.now(), null);
            alert1.setWarnCellId(12345L);
            repository.save(alert1);

            WeatherAlert alert2 = createAlert("ALERT-002", "Munich", "moderate", LocalDateTime.now(), null);
            alert2.setWarnCellId(67890L);
            repository.save(alert2);

            // When
            List<WeatherAlert> results = repository.findByWarnCellId(12345L);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-001");
        }
    }

    @Nested
    @DisplayName("Severity filtering")
    class SeverityTests {

        @Test
        @DisplayName("should find alerts by severity ordered by effective date")
        void shouldFindBySeverityOrderByEffectiveDesc() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(5), null));
            repository.save(createAlert("ALERT-002", "Munich", "severe", now.minusHours(2), null));
            repository.save(createAlert("ALERT-003", "Hamburg", "moderate", now.minusHours(1), null));

            // When
            List<WeatherAlert> results = repository.findBySeverityOrderByEffectiveDesc("severe");

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WeatherAlert::getAlertId)
                    .containsExactly("ALERT-002", "ALERT-001");
        }
    }

    @Nested
    @DisplayName("Date range queries")
    class DateRangeTests {

        @Test
        @DisplayName("should find alerts by effective date range")
        void shouldFindByEffectiveRange() {
            // Given
            LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.of(2024, 1, 31, 23, 59);

            repository.save(createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.of(2024, 1, 15, 12, 0), null));
            repository.save(createAlert("ALERT-002", "Munich", "moderate", LocalDateTime.of(2024, 2, 1, 12, 0), null));
            repository.save(createAlert("ALERT-003", "Hamburg", "minor", LocalDateTime.of(2024, 1, 20, 12, 0), null));

            // When
            List<WeatherAlert> results = repository.findByEffectiveRange(start, end);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WeatherAlert::getAlertId)
                    .containsExactlyInAnyOrder("ALERT-001", "ALERT-003");
        }

        @Test
        @DisplayName("should find recent alerts")
        void shouldFindRecentAlerts() {
            // Given
            LocalDateTime since = LocalDateTime.now().minusHours(6);
            repository.save(createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.now().minusHours(2), null));
            repository.save(createAlert("ALERT-002", "Munich", "moderate", LocalDateTime.now().minusHours(10), null));

            // When
            List<WeatherAlert> results = repository.findRecentAlerts(since);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-001");
        }
    }

    @Nested
    @DisplayName("Active alerts for location")
    class ActiveAlertsForLocationTests {

        @Test
        @DisplayName("should find active alerts for city")
        void shouldFindActiveAlertsForCity() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-002", "Munich", "moderate", now.minusHours(1), now.plusHours(1)));

            // When
            List<WeatherAlert> results = repository.findActiveAlertsForLocation("Berlin", null, now);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCity()).isEqualTo("Berlin");
        }

        @Test
        @DisplayName("should find active alerts by warn cell ID")
        void shouldFindActiveAlertsByWarnCellId() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            WeatherAlert alert = createAlert("ALERT-001", "Berlin", "severe", now.minusHours(1), now.plusHours(1));
            alert.setWarnCellId(12345L);
            repository.save(alert);

            // When
            List<WeatherAlert> results = repository.findActiveAlertsForLocation("OtherCity", 12345L, now);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWarnCellId()).isEqualTo(12345L);
        }

        @Test
        @DisplayName("should order active alerts by severity and effective date")
        void shouldOrderBySeverityAndEffective() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "moderate", now.minusHours(3), now.plusHours(1)));
            repository.save(createAlert("ALERT-002", "Berlin", "severe", now.minusHours(2), now.plusHours(1)));
            repository.save(createAlert("ALERT-003", "Berlin", "severe", now.minusHours(1), now.plusHours(1)));

            // When
            List<WeatherAlert> results = repository.findActiveAlertsForLocation("Berlin", null, now);

            // Then
            assertThat(results).hasSize(3);
            // Severe alerts first, then ordered by effective DESC
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-003");
            assertThat(results.get(1).getAlertId()).isEqualTo("ALERT-002");
        }

        @Test
        @DisplayName("should exclude expired alerts")
        void shouldExcludeExpiredAlerts() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(2), now.minusHours(1)));
            repository.save(createAlert("ALERT-002", "Berlin", "moderate", now.minusHours(1), now.plusHours(1)));

            // When
            List<WeatherAlert> results = repository.findActiveAlertsForLocation("Berlin", null, now);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-002");
        }

        @Test
        @DisplayName("should exclude alerts not yet effective")
        void shouldExcludeAlertsNotYetEffective() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.plusHours(1), now.plusHours(2)));
            repository.save(createAlert("ALERT-002", "Berlin", "moderate", now.minusHours(1), now.plusHours(1)));

            // When
            List<WeatherAlert> results = repository.findActiveAlertsForLocation("Berlin", null, now);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-002");
        }
    }

    @Nested
    @DisplayName("Deletion operations")
    class DeletionTests {

        @Test
        @DisplayName("should delete expired alerts")
        void shouldDeleteExpiredAlerts() {
            // Given
            LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
            repository.save(
                    createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.now().minusDays(5), cutoff.minusHours(1)));
            repository.save(createAlert("ALERT-002", "Munich", "moderate", LocalDateTime.now().minusHours(1),
                    cutoff.plusHours(1)));

            // When
            repository.deleteExpiredAlerts(cutoff);
            repository.flush();

            // Then
            List<WeatherAlert> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getAlertId()).isEqualTo("ALERT-002");
        }

        @Test
        @DisplayName("should delete old alerts by creation date")
        void shouldDeleteOldAlerts() {
            // Given
            // Note: WeatherAlert has @PrePersist that sets createdAt automatically,
            // so we cannot manually set past timestamps. This test verifies the deletion
            // query executes without errors. Using a future cutoff will delete all alerts.

            WeatherAlert alert1 = createAlert("ALERT-001", "Berlin", "severe", LocalDateTime.now(), null);
            repository.save(alert1);

            WeatherAlert alert2 = createAlert("ALERT-002", "Munich", "moderate", LocalDateTime.now(), null);
            repository.save(alert2);

            // When - delete with a far future cutoff (will delete all alerts created before tomorrow)
            repository.deleteOldAlerts(LocalDateTime.now().plusDays(1));
            repository.flush();

            // Then - all alerts should be deleted since their createdAt is before tomorrow
            List<WeatherAlert> remaining = repository.findAll();
            assertThat(remaining).isEmpty();
        }
    }

    @Nested
    @DisplayName("Aggregate queries")
    class AggregateTests {

        @Test
        @DisplayName("should count active alerts by severity")
        void shouldCountActiveBySeverity() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-002", "Munich", "severe", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-003", "Hamburg", "moderate", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-004", "Cologne", "moderate", now.minusHours(2),
                    now.minusHours(1))); // Expired

            // When
            List<Object[]> results = repository.countActiveBySeverity(now);

            // Then
            assertThat(results).hasSize(2);
            // Results contain [severity, count]
            boolean foundSevere = false;
            boolean foundModerate = false;
            for (Object[] row : results) {
                String severity = (String) row[0];
                Long count = (Long) row[1];
                if ("severe".equals(severity)) {
                    assertThat(count).isEqualTo(2);
                    foundSevere = true;
                } else if ("moderate".equals(severity)) {
                    assertThat(count).isEqualTo(1);
                    foundModerate = true;
                }
            }
            assertThat(foundSevere).isTrue();
            assertThat(foundModerate).isTrue();
        }
    }

    @Nested
    @DisplayName("Distinct cities query")
    class DistinctCitiesTests {

        @Test
        @DisplayName("should find distinct cities with active alerts")
        void shouldFindDistinctCitiesWithActiveAlerts() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(createAlert("ALERT-001", "Berlin", "severe", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-002", "Berlin", "moderate", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-003", "Munich", "minor", now.minusHours(1), now.plusHours(1)));
            repository.save(createAlert("ALERT-004", "Hamburg", "severe", now.minusHours(2),
                    now.minusHours(1))); // Expired

            // When
            List<String> cities = repository.findDistinctCitiesWithActiveAlerts(now);

            // Then
            assertThat(cities).hasSize(2);
            assertThat(cities).containsExactlyInAnyOrder("Berlin", "Munich");
        }

        @Test
        @DisplayName("should exclude null cities")
        void shouldExcludeNullCities() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            WeatherAlert alertWithoutCity = createAlert("ALERT-001", null, "severe", now.minusHours(1),
                    now.plusHours(1));
            repository.save(alertWithoutCity);
            repository.save(createAlert("ALERT-002", "Berlin", "moderate", now.minusHours(1), now.plusHours(1)));

            // When
            List<String> cities = repository.findDistinctCitiesWithActiveAlerts(now);

            // Then
            assertThat(cities).hasSize(1);
            assertThat(cities).containsOnly("Berlin");
        }
    }

    @Nested
    @DisplayName("Device-scoped queries")
    class DeviceScopedTests {

        @Test
        @DisplayName("should find active alerts by device ID")
        void shouldFindActiveAlertsByDeviceId() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(
                    createAlertWithDevice("ALERT-001", "device-001", now.minusHours(1), now.plusHours(1)));
            repository.save(
                    createAlertWithDevice("ALERT-002", "device-001", now.minusHours(2), now.minusHours(1))); // Expired
            repository.save(
                    createAlertWithDevice("ALERT-003", "device-002", now.minusHours(1), now.plusHours(1)));

            // When
            List<WeatherAlert> results = repository.findActiveAlertsByDeviceId("device-001", now);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getAlertId()).isEqualTo("ALERT-001");
        }

        @Test
        @DisplayName("should find all alerts by device ID ordered by effective date")
        void shouldFindByDeviceIdOrderByEffectiveDesc() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            repository.save(
                    createAlertWithDevice("ALERT-001", "device-001", now.minusHours(3), now.plusHours(1)));
            repository.save(
                    createAlertWithDevice("ALERT-002", "device-001", now.minusHours(1), now.plusHours(1)));
            repository.save(
                    createAlertWithDevice("ALERT-003", "device-002", now.minusHours(2), now.plusHours(1)));

            // When
            List<WeatherAlert> results = repository.findByDeviceIdOrderByEffectiveDesc("device-001");

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(WeatherAlert::getAlertId)
                    .containsExactly("ALERT-002", "ALERT-001");
        }
    }
}
