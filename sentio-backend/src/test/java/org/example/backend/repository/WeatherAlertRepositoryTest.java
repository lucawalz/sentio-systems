package org.example.backend.repository;

import org.example.backend.model.WeatherAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherAlertRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private WeatherAlertRepository repository;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        now = LocalDateTime.now();
    }

    @Test
    void testFindActiveAlertsByDeviceId() {
        String deviceId = "test-device";
        // Active
        repository.save(createAlert(deviceId, "A1", now.minusHours(1), now.plusHours(1), "Minor"));
        // Expired
        repository.save(createAlert(deviceId, "A2", now.minusHours(5), now.minusHours(1), "Minor"));
        // Other device
        repository.save(createAlert("other", "A3", now.minusHours(1), now.plusHours(1), "Minor"));

        List<WeatherAlert> active = repository.findActiveAlertsByDeviceId(deviceId, now);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getAlertId()).isEqualTo("A1");
    }

    @Test
    void testFindActiveAlertsForLocation() {
        WeatherAlert a1 = createAlert("d1", "L1", now.minusHours(1), now.plusHours(1), "Severe");
        a1.setCity("Berlin");
        repository.save(a1);

        WeatherAlert a2 = createAlert("d2", "L2", now.minusHours(1), now.plusHours(1), "Moderate");
        a2.setWarnCellId(12345L);
        a2.setCity("Unknown");
        repository.save(a2);

        // Expired
        WeatherAlert a3 = createAlert("d3", "L3", now.minusHours(5), now.minusHours(1), "Severe");
        a3.setCity("Berlin");
        repository.save(a3);

        List<WeatherAlert> activeBerlin = repository.findActiveAlertsForLocation("Berlin", null, now);
        assertThat(activeBerlin).hasSize(1);
        assertThat(activeBerlin.get(0).getAlertId()).isEqualTo("L1");

        List<WeatherAlert> activeCell = repository.findActiveAlertsForLocation(null, 12345L, now);
        assertThat(activeCell).hasSize(1);
        assertThat(activeCell.get(0).getAlertId()).isEqualTo("L2");
    }

    @Test
    void testDeleteExpiredAlerts() {
        // Expired
        repository.save(createAlert("d1", "A1", now.minusDays(2), now.minusDays(1), "Minor"));
        // Active
        repository.save(createAlert("d1", "A2", now.minusHours(1), now.plusHours(1), "Minor"));

        repository.deleteExpiredAlerts(now);

        List<WeatherAlert> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getAlertId()).isEqualTo("A2");
    }

    @Test
    void testCountActiveBySeverity() {
        repository.save(createAlert("d1", "A1", now.minusHours(1), now.plusHours(2), "Minor"));
        repository.save(createAlert("d1", "A2", now.minusHours(1), now.plusHours(2), "Minor"));
        repository.save(createAlert("d1", "A3", now.minusHours(1), now.plusHours(2), "Severe"));
        repository.save(createAlert("d1", "A4", now.minusHours(5), now.minusHours(1), "Severe")); // Expired

        List<Object[]> stats = repository.countActiveBySeverity(now);

        assertThat(stats).hasSize(2); // Minor and Severe

        // Convert to map or iterate to verify
        boolean foundMinor = false;
        boolean foundSevere = false;

        for (Object[] row : stats) {
            String severity = (String) row[0];
            long count = (Long) row[1];
            if ("Minor".equals(severity)) {
                assertThat(count).isEqualTo(2);
                foundMinor = true;
            } else if ("Severe".equals(severity)) {
                assertThat(count).isEqualTo(1);
                foundSevere = true;
            }
        }
        assertThat(foundMinor).isTrue();
        assertThat(foundSevere).isTrue();
    }

    private WeatherAlert createAlert(String deviceId, String alertId, LocalDateTime effective, LocalDateTime expires,
            String severity) {
        WeatherAlert alert = new WeatherAlert();
        alert.setDeviceId(deviceId);
        alert.setAlertId(alertId);
        alert.setEffective(effective);
        alert.setExpires(expires);
        alert.setSeverity(severity);
        alert.setHeadlineEn("Warning"); // mandatory mainly for demo
        alert.setCreatedAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());
        return alert;
    }
}
