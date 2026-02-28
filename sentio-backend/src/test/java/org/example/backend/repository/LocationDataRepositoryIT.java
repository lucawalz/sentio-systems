package org.example.backend.repository;

import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.LocationData;
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
 * Integration tests for {@link LocationDataRepository}.
 * Validates custom queries for IP-based geolocation data storage and retrieval.
 */
@DisplayName("LocationDataRepository")
class LocationDataRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private LocationDataRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private LocationData createLocationData(String ipAddress, String city, String country, float latitude,
            float longitude) {
        LocationData data = new LocationData();
        data.setIpAddress(ipAddress);
        data.setCity(city);
        data.setCountry(country);
        data.setRegion("Test Region");
        data.setLatitude(latitude);
        data.setLongitude(longitude);
        return data;
    }

    @Nested
    @DisplayName("IP address queries")
    class IpAddressTests {

        @Test
        @DisplayName("should find location by IP address")
        void shouldFindByIpAddress() {
            // Given
            repository.save(createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f));

            // When
            Optional<LocationData> result = repository.findByIpAddress("192.168.1.1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCity()).isEqualTo("Berlin");
        }

        @Test
        @DisplayName("should return empty when IP address not found")
        void shouldReturnEmptyWhenIpNotFound() {
            // When
            Optional<LocationData> result = repository.findByIpAddress("10.0.0.1");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should find latest location by IP address")
        void shouldFindLatestByIpAddress() {
            // Given
            LocationData old = createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f);
            old.setUpdatedAt(LocalDateTime.now().minusHours(2));
            repository.save(old);

            LocationData recent = createLocationData("192.168.1.1", "Munich", "Germany", 48.137f, 11.576f);
            recent.setUpdatedAt(LocalDateTime.now());
            repository.save(recent);

            // When
            Optional<LocationData> result = repository.findLatestByIpAddress("192.168.1.1");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCity()).isEqualTo("Munich");
        }

        @Test
        @DisplayName("should return empty when no latest IP found")
        void shouldReturnEmptyWhenNoLatestIp() {
            // When
            Optional<LocationData> result = repository.findLatestByIpAddress("10.0.0.1");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Recent data queries")
    class RecentDataTests {

        @Test
        @DisplayName("should find recent location data")
        void shouldFindRecentLocationData() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusHours(6);

            LocationData old = createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f);
            old.setCreatedAt(LocalDateTime.now().minusHours(10));
            repository.save(old);

            LocationData recent1 = createLocationData("192.168.1.2", "Munich", "Germany", 48.137f, 11.576f);
            recent1.setCreatedAt(LocalDateTime.now().minusHours(2));
            repository.save(recent1);

            LocationData recent2 = createLocationData("192.168.1.3", "Hamburg", "Germany", 53.551f, 9.993f);
            recent2.setCreatedAt(LocalDateTime.now().minusHours(1));
            repository.save(recent2);

            // When
            List<LocationData> results = repository.findRecentLocationData(startDate);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(LocationData::getCity)
                    .containsExactlyInAnyOrder("Munich", "Hamburg");
        }

        @Test
        @DisplayName("should order recent location data by created date descending")
        void shouldOrderRecentDataByCreatedDesc() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusHours(6);

            LocationData data1 = createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f);
            data1.setCreatedAt(LocalDateTime.now().minusHours(5));
            repository.save(data1);

            LocationData data2 = createLocationData("192.168.1.2", "Munich", "Germany", 48.137f, 11.576f);
            data2.setCreatedAt(LocalDateTime.now().minusHours(1));
            repository.save(data2);

            // When
            List<LocationData> results = repository.findRecentLocationData(startDate);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).getCity()).isEqualTo("Munich");
            assertThat(results.get(1).getCity()).isEqualTo("Berlin");
        }
    }

    @Nested
    @DisplayName("Deletion operations")
    class DeletionTests {

        @Test
        @DisplayName("should delete old location data")
        void shouldDeleteOldLocationData() {
            // Given
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

            LocationData old = createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f);
            old.setCreatedAt(cutoffDate.minusDays(10));
            repository.save(old);

            LocationData recent = createLocationData("192.168.1.2", "Munich", "Germany", 48.137f, 11.576f);
            recent.setCreatedAt(cutoffDate.plusDays(1));
            repository.save(recent);

            // When
            repository.deleteOldLocationData(cutoffDate);
            repository.flush();

            // Then
            List<LocationData> remaining = repository.findAll();
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getCity()).isEqualTo("Munich");
        }
    }

    @Nested
    @DisplayName("Distinct cities queries")
    class DistinctCitiesTests {

        @Test
        @DisplayName("should find distinct cities since date")
        void shouldFindDistinctCitiesSince() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);

            LocationData data1 = createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f);
            data1.setCreatedAt(LocalDateTime.now().minusDays(2));
            repository.save(data1);

            LocationData data2 = createLocationData("192.168.1.2", "Berlin", "Germany", 52.52f, 13.405f);
            data2.setCreatedAt(LocalDateTime.now().minusDays(1));
            repository.save(data2);

            LocationData data3 = createLocationData("192.168.1.3", "Munich", "Germany", 48.137f, 11.576f);
            data3.setCreatedAt(LocalDateTime.now().minusDays(3));
            repository.save(data3);

            LocationData old = createLocationData("192.168.1.4", "Hamburg", "Germany", 53.551f, 9.993f);
            old.setCreatedAt(LocalDateTime.now().minusDays(10));
            repository.save(old);

            // When
            List<String> cities = repository.findDistinctCitiesSince(startDate);

            // Then
            assertThat(cities).hasSize(2);
            assertThat(cities).containsExactlyInAnyOrder("Berlin", "Munich");
        }
    }

    @Nested
    @DisplayName("Location filtering")
    class LocationFilteringTests {

        @Test
        @DisplayName("should find location data by city")
        void shouldFindByCity() {
            // Given
            repository.save(createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f));
            repository.save(createLocationData("192.168.1.2", "Berlin", "Germany", 52.52f, 13.405f));
            repository.save(createLocationData("192.168.1.3", "Munich", "Germany", 48.137f, 11.576f));

            // When
            List<LocationData> results = repository.findByCity("Berlin");

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(LocationData::getCity)
                    .containsOnly("Berlin");
        }

        @Test
        @DisplayName("should find location data by country")
        void shouldFindByCountry() {
            // Given
            repository.save(createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f));
            repository.save(createLocationData("192.168.1.2", "Munich", "Germany", 48.137f, 11.576f));
            repository.save(createLocationData("192.168.1.3", "Paris", "France", 48.856f, 2.352f));

            // When
            List<LocationData> results = repository.findByCountry("Germany");

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(LocationData::getCountry)
                    .containsOnly("Germany");
        }
    }

    @Nested
    @DisplayName("Coordinate range queries")
    class CoordinateRangeTests {

        @Test
        @DisplayName("should find location data by coordinate range")
        void shouldFindByCoordinateRange() {
            // Given
            repository.save(createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f));
            repository.save(createLocationData("192.168.1.2", "Munich", "Germany", 48.137f, 11.576f));
            repository.save(createLocationData("192.168.1.3", "Hamburg", "Germany", 53.551f, 9.993f));

            // When - Search around Berlin area
            List<LocationData> results = repository.findByCoordinateRange(52.0f, 53.0f, 13.0f, 14.0f);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCity()).isEqualTo("Berlin");
        }

        @Test
        @DisplayName("should find multiple locations within coordinate range")
        void shouldFindMultipleLocationsInRange() {
            // Given
            repository.save(createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f));
            repository.save(createLocationData("192.168.1.2", "Potsdam", "Germany", 52.39f, 13.065f));
            repository.save(createLocationData("192.168.1.3", "Munich", "Germany", 48.137f, 11.576f));

            // When - Search around Berlin area (larger range)
            List<LocationData> results = repository.findByCoordinateRange(52.0f, 53.0f, 13.0f, 14.0f);

            // Then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(LocationData::getCity)
                    .containsExactlyInAnyOrder("Berlin", "Potsdam");
        }

        @Test
        @DisplayName("should return empty list when no locations in range")
        void shouldReturnEmptyWhenNoLocationsInRange() {
            // Given
            repository.save(createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f));

            // When - Search in different area
            List<LocationData> results = repository.findByCoordinateRange(48.0f, 49.0f, 11.0f, 12.0f);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle duplicate IP addresses with different timestamps")
        void shouldHandleDuplicateIpsWithDifferentTimestamps() {
            // Given
            LocationData first = createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f);
            first.setCreatedAt(LocalDateTime.now().minusDays(1));
            repository.save(first);

            LocationData second = createLocationData("192.168.1.1", "Berlin", "Germany", 52.52f, 13.405f);
            second.setCreatedAt(LocalDateTime.now());
            repository.save(second);

            // When
            List<LocationData> all = repository.findAll();

            // Then
            assertThat(all).hasSize(2);
            assertThat(all).extracting(LocationData::getIpAddress)
                    .containsOnly("192.168.1.1");
        }

        @Test
        @DisplayName("should handle empty results for city query")
        void shouldHandleEmptyResultsForCity() {
            // When
            List<LocationData> results = repository.findByCity("NonExistentCity");

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle empty results for country query")
        void shouldHandleEmptyResultsForCountry() {
            // When
            List<LocationData> results = repository.findByCountry("NonExistentCountry");

            // Then
            assertThat(results).isEmpty();
        }
    }
}
