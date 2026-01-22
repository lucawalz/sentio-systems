package org.example.backend.repository;

import org.example.backend.model.LocationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LocationDataRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private LocationDataRepository repository;

    private LocalDateTime now;

    @BeforeEach
    void setUp() throws InterruptedException {
        repository.deleteAll();
        now = LocalDateTime.now();
    }

    @Test
    void testSaveAndFind() {
        LocationData location = createLocation("192.168.1.1", "Berlin", "Germany");
        LocationData saved = repository.save(location);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    void testFindLatestByIpAddress() throws InterruptedException {
        // Create 2 records with same IP but different timestamps
        LocationData older = createLocation("10.0.0.1", "Munich", "Germany");
        repository.save(older);

        // Wait a bit to ensure timestamp difference (H2 precision can be tricky)
        Thread.sleep(10);

        LocationData newer = createLocation("10.0.0.1", "Berlin", "Germany");
        repository.save(newer);

        Optional<LocationData> result = repository.findLatestByIpAddress("10.0.0.1");

        assertThat(result).isPresent();
        assertThat(result.get().getCity()).isEqualTo("Berlin");
    }

    @Test
    void testFindRecentLocationData() {
        LocationData old = createLocation("10.0.0.2", "Paris", "France");
        old.setCreatedAt(now.minusDays(2));
        repository.save(old);

        LocationData recent = createLocation("10.0.0.3", "London", "UK");
        recent.setCreatedAt(now.minusHours(1));
        repository.save(recent);

        List<LocationData> result = repository.findRecentLocationData(now.minusDays(1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCity()).isEqualTo("London");
    }

    @Test
    void testFindByCoordinateRange() {
        // Berlin: 52.52, 13.41
        repository.save(createLocationWithCoords("1.1.1.1", "Berlin", 52.52f, 13.41f));
        // Munich: 48.14, 11.58
        repository.save(createLocationWithCoords("2.2.2.2", "Munich", 48.14f, 11.58f));
        // New York: 40.71, -74.01
        repository.save(createLocationWithCoords("3.3.3.3", "New York", 40.71f, -74.01f));

        // Search for locations in Germany (roughly)
        List<LocationData> germanyLocations = repository.findByCoordinateRange(
                47.0f, 55.0f, 5.0f, 15.0f);

        assertThat(germanyLocations).hasSize(2);
        assertThat(germanyLocations)
                .extracting(LocationData::getCity)
                .containsExactlyInAnyOrder("Berlin", "Munich");
    }

    @Test
    void testFindDistinctCitiesSince() {
        repository.save(createLocation("1.1.1.1", "Berlin", "Germany"));
        repository.save(createLocation("2.2.2.2", "Berlin", "Germany")); // Duplicate city
        repository.save(createLocation("3.3.3.3", "Hamburg", "Germany"));

        List<String> cities = repository.findDistinctCitiesSince(now.minusHours(1));

        assertThat(cities).hasSize(2);
        assertThat(cities).containsExactlyInAnyOrder("Berlin", "Hamburg");
    }

    private LocationData createLocation(String ip, String city, String country) {
        LocationData loc = new LocationData();
        loc.setIpAddress(ip);
        loc.setCity(city);
        loc.setCountry(country);
        loc.setRegion("Region");
        loc.setLatitude(0.0f);
        loc.setLongitude(0.0f);
        loc.setCreatedAt(LocalDateTime.now());
        loc.setUpdatedAt(LocalDateTime.now());
        return loc;
    }

    private LocationData createLocationWithCoords(String ip, String city, float lat, float lon) {
        LocationData loc = createLocation(ip, city, "Country");
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        return loc;
    }
}
