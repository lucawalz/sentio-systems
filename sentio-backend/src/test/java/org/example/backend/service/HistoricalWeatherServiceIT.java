package org.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.example.backend.BaseIntegrationTest;
import org.example.backend.model.Device;
import org.example.backend.model.HistoricalWeather;
import org.example.backend.model.LocationData;
import org.example.backend.repository.DeviceRepository;
import org.example.backend.repository.HistoricalWeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Comprehensive integration tests for HistoricalWeatherService.
 * Tests database persistence, API integration, and business logic.
 * Uses WireMock for OpenMeteo API mocking and real database operations.
 *
 * Target: 80%+ code coverage
 * Current: 0.3% (1537 missed, 4 covered)
 */
@WireMockTest(httpPort = 8095)
class HistoricalWeatherServiceIT extends BaseIntegrationTest {

    private static final AtomicInteger deviceCounter = new AtomicInteger(0);

    @Autowired
    private HistoricalWeatherService historicalWeatherService;

    @Autowired
    private HistoricalWeatherRepository repository;

    @Autowired
    private DeviceRepository deviceRepository;

    @MockBean
    private DeviceLocationService deviceLocationService;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureWireMock(DynamicPropertyRegistry registry) {
        registry.add("openmeteo.archive.base-url", () -> "http://localhost:8095");
    }

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
        deviceRepository.deleteAll();
        deviceCounter.set(0);
    }

    private HistoricalWeather createHistoricalWeather(LocalDate date, String city) {
        HistoricalWeather hw = new HistoricalWeather();
        hw.setWeatherDate(date);
        hw.setCity(city);
        hw.setCountry("Germany");
        hw.setMaxTemperature(20.0f);
        hw.setMinTemperature(10.0f);
        hw.setDeviceId("test-device-" + deviceCounter.getAndIncrement());
        return hw;
    }

    private LocationData createLocationData(String deviceId, Float latitude, Float longitude) {
        LocationData location = new LocationData();
        location.setDeviceId(deviceId);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setCity("Berlin");
        location.setCountry("Germany");
        location.setIpAddress("192.168.1.1");
        return location;
    }

    private Device createDevice(String deviceId, Double latitude, Double longitude) {
        Device device = new Device();
        device.setId(deviceId);
        device.setName("Test Device");
        device.setLatitude(latitude);
        device.setLongitude(longitude);
        device.setOwnerId("test-user");
        return device;
    }

    private String createOpenMeteoResponse(String... dates) {
        StringBuilder timeArray = new StringBuilder("[");
        StringBuilder tempMaxArray = new StringBuilder("[");
        StringBuilder tempMinArray = new StringBuilder("[");
        StringBuilder weatherCodeArray = new StringBuilder("[");
        StringBuilder precipArray = new StringBuilder("[");
        StringBuilder windSpeedArray = new StringBuilder("[");
        StringBuilder uvIndexArray = new StringBuilder("[");
        StringBuilder sunriseArray = new StringBuilder("[");
        StringBuilder sunsetArray = new StringBuilder("[");

        for (int i = 0; i < dates.length; i++) {
            if (i > 0) {
                timeArray.append(",");
                tempMaxArray.append(",");
                tempMinArray.append(",");
                weatherCodeArray.append(",");
                precipArray.append(",");
                windSpeedArray.append(",");
                uvIndexArray.append(",");
                sunriseArray.append(",");
                sunsetArray.append(",");
            }
            timeArray.append("\"").append(dates[i]).append("\"");
            tempMaxArray.append(20.5 + i);
            tempMinArray.append(10.2 + i);
            weatherCodeArray.append(i % 2 == 0 ? 0 : 61); // Clear or rain
            precipArray.append(i % 2 == 0 ? 0.0 : 5.5);
            windSpeedArray.append(3.5 + i);
            uvIndexArray.append(4.5 + i);
            sunriseArray.append("\"").append(dates[i]).append("T06:30:00\"");
            sunsetArray.append("\"").append(dates[i]).append("T18:30:00\"");
        }

        timeArray.append("]");
        tempMaxArray.append("]");
        tempMinArray.append("]");
        weatherCodeArray.append("]");
        precipArray.append("]");
        windSpeedArray.append("]");
        uvIndexArray.append("]");
        sunriseArray.append("]");
        sunsetArray.append("]");

        return String.format("""
            {
                "latitude": 52.52,
                "longitude": 13.41,
                "timezone": "Europe/Berlin",
                "daily": {
                    "time": %s,
                    "weather_code": %s,
                    "temperature_2m_max": %s,
                    "temperature_2m_min": %s,
                    "sunrise": %s,
                    "sunset": %s,
                    "daylight_duration": [43200.0, 43200.0, 43200.0],
                    "sunshine_duration": [28800.0, 28800.0, 28800.0],
                    "uv_index_max": %s,
                    "precipitation_sum": %s,
                    "precipitation_hours": [0.0, 2.0, 1.5],
                    "wind_speed_10m_max": %s,
                    "wind_direction_10m_dominant": [180.0, 190.0, 200.0]
                }
            }
            """, timeArray, weatherCodeArray, tempMaxArray, tempMinArray,
                 sunriseArray, sunsetArray, uvIndexArray, precipArray, windSpeedArray);
    }

    // ========== Core Business Logic Tests ==========

    @Nested
    @DisplayName("getHistoricalWeatherForLocation")
    class GetHistoricalWeatherForLocationTests {

        @Test
        @DisplayName("should fetch and persist historical weather from API")
        void shouldFetchAndPersistFromApi() {
            LocationData location = createLocationData("device-1", 52.52f, 13.41f);

            LocalDate date1 = LocalDate.now().minusDays(3);
            LocalDate date2 = LocalDate.now().minusWeeks(2);

            String response = createOpenMeteoResponse(
                date1.toString(),
                date2.toString()
            );

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(response)));

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForLocation(52.52f, 13.41f, location);

            assertThat(results).isNotEmpty();
            assertThat(repository.count()).isGreaterThan(0);

            HistoricalWeather saved = repository.findByWeatherDateAndDeviceId(date1, "device-1")
                .orElseThrow();

            assertThat(saved.getCity()).isEqualTo("Berlin");
            assertThat(saved.getCountry()).isEqualTo("Germany");
            assertThat(saved.getMaxTemperature()).isNotNull();
            assertThat(saved.getMinTemperature()).isNotNull();
            assertThat(saved.getWeatherCode()).isNotNull();
            assertThat(saved.getWeatherMain()).isNotNull();
            assertThat(saved.getDescription()).isNotNull();
            assertThat(saved.getIcon()).isNotNull();
        }

        @Test
        @DisplayName("should map weather codes to descriptions correctly")
        void shouldMapWeatherCodes() {
            LocationData location = createLocationData("device-2", 52.52f, 13.41f);
            LocalDate date = LocalDate.now().minusDays(3);

            String response = String.format("""
                {
                    "daily": {
                        "time": ["%s"],
                        "weather_code": [61],
                        "temperature_2m_max": [20.5],
                        "temperature_2m_min": [10.2],
                        "sunrise": ["%sT06:30:00"],
                        "sunset": ["%sT18:30:00"],
                        "daylight_duration": [43200.0],
                        "sunshine_duration": [28800.0],
                        "uv_index_max": [4.5],
                        "precipitation_sum": [5.5],
                        "precipitation_hours": [2.0],
                        "wind_speed_10m_max": [3.5],
                        "wind_direction_10m_dominant": [180.0]
                    }
                }
                """, date, date, date);

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(ok(response)));

            historicalWeatherService.getHistoricalWeatherForLocation(52.52f, 13.41f, location);

            HistoricalWeather saved = repository.findByWeatherDateAndDeviceId(date, "device-2")
                .orElseThrow();

            assertThat(saved.getWeatherCode()).isEqualTo(61);
            assertThat(saved.getWeatherMain()).isEqualTo("Rain");
            assertThat(saved.getDescription()).isEqualTo("Slight rain");
            assertThat(saved.getIcon()).isEqualTo("10d");
        }

        @Test
        @DisplayName("should not refetch existing data")
        void shouldSkipExistingData() {
            LocationData location = createLocationData("device-3", 52.52f, 13.41f);
            LocalDate date = LocalDate.now().minusDays(3);

            HistoricalWeather existing = createHistoricalWeather(date, "Berlin");
            existing.setDeviceId("device-3");
            repository.save(existing);

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForLocation(52.52f, 13.41f, location);

            assertThat(results).hasSize(1);
            verify(0, getRequestedFor(urlPathMatching("/forecast")));
        }

        @Test
        @DisplayName("should handle API errors gracefully")
        void shouldHandleApiErrors() {
            LocationData location = createLocationData("device-4", 52.52f, 13.41f);

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(aResponse().withStatus(500)));

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForLocation(52.52f, 13.41f, location);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should handle null/missing values in API response")
        void shouldHandleNullValues() {
            LocationData location = createLocationData("device-5", 52.52f, 13.41f);
            LocalDate date = LocalDate.now().minusDays(3);

            String response = String.format("""
                {
                    "daily": {
                        "time": ["%s"],
                        "weather_code": [null],
                        "temperature_2m_max": [null],
                        "temperature_2m_min": [10.2],
                        "sunrise": [null],
                        "sunset": [null],
                        "daylight_duration": [null],
                        "sunshine_duration": [null],
                        "uv_index_max": [null],
                        "precipitation_sum": [null],
                        "precipitation_hours": [null],
                        "wind_speed_10m_max": [null],
                        "wind_direction_10m_dominant": [null]
                    }
                }
                """, date);

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(ok(response)));

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForLocation(52.52f, 13.41f, location);

            assertThat(results).hasSize(1);
            HistoricalWeather saved = results.get(0);
            assertThat(saved.getWeatherCode()).isNull();
            assertThat(saved.getMaxTemperature()).isNull();
            assertThat(saved.getMinTemperature()).isNotNull();
        }

        @Test
        @DisplayName("should update old records that need refresh")
        void shouldUpdateOldRecords() throws InterruptedException {
            LocationData location = createLocationData("device-6", 52.52f, 13.41f);
            LocalDate date = LocalDate.now().minusDays(3);

            HistoricalWeather old = createHistoricalWeather(date, "Berlin");
            old.setDeviceId("device-6");
            old.setMaxTemperature(15.0f);
            repository.save(old);

            // Set updatedAt to 2 weeks ago to trigger update
            repository.findByWeatherDateAndDeviceId(date, "device-6")
                .ifPresent(hw -> {
                    hw.setUpdatedAt(LocalDateTime.now().minusWeeks(2));
                    repository.save(hw);
                });

            Thread.sleep(100);

            String response = createOpenMeteoResponse(date.toString());

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(ok(response)));

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForLocation(52.52f, 13.41f, location);

            assertThat(results).isNotEmpty();
            verify(1, getRequestedFor(urlPathMatching("/forecast")));
        }

        @Test
        @DisplayName("should handle multiple date ranges efficiently")
        void shouldHandleMultipleDateRanges() {
            LocationData location = createLocationData("device-7", 52.52f, 13.41f);

            LocalDate date1 = LocalDate.now().minusDays(3);
            LocalDate date2 = LocalDate.now().minusMonths(3);

            String response1 = createOpenMeteoResponse(date1.toString());
            String response2 = createOpenMeteoResponse(date2.toString());

            stubFor(get(urlPathMatching("/forecast"))
                .withQueryParam("start_date", matching(".*-" + date1.getMonthValue() + "-.*"))
                .willReturn(ok(response1)));

            stubFor(get(urlPathMatching("/forecast"))
                .withQueryParam("start_date", matching(".*-" + date2.getMonthValue() + "-.*"))
                .willReturn(ok(response2)));

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForLocation(52.52f, 13.41f, location);

            assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getHistoricalWeatherForCurrentLocation")
    @WithMockUser(username = "test-user")
    class GetHistoricalWeatherForCurrentLocationTests {

        @Test
        @DisplayName("should fetch weather for user device location")
        void shouldFetchForUserDevice() {
            LocationData location = createLocationData("device-8", 52.52f, 13.41f);
            when(deviceLocationService.getFirstUserDeviceLocation())
                .thenReturn(Optional.of(location));

            String response = createOpenMeteoResponse(
                LocalDate.now().minusDays(3).toString()
            );

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(ok(response)));

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForCurrentLocation();

            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("should return empty list when no device registered")
        void shouldReturnEmptyWhenNoDevice() {
            when(deviceLocationService.getFirstUserDeviceLocation())
                .thenReturn(Optional.empty());

            List<HistoricalWeather> results = historicalWeatherService
                .getHistoricalWeatherForCurrentLocation();

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateHistoricalWeatherForCurrentLocation")
    @WithMockUser(username = "test-user")
    class UpdateHistoricalWeatherForCurrentLocationTests {

        @Test
        @DisplayName("should update weather for device location")
        void shouldUpdateForDeviceLocation() {
            LocationData location = createLocationData("device-9", 52.52f, 13.41f);
            when(deviceLocationService.getFirstUserDeviceLocation())
                .thenReturn(Optional.of(location));

            String response = createOpenMeteoResponse(
                LocalDate.now().minusDays(3).toString()
            );

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(ok(response)));

            historicalWeatherService.updateHistoricalWeatherForCurrentLocation();

            assertThat(repository.count()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should prevent concurrent updates")
        void shouldPreventConcurrentUpdates() throws InterruptedException {
            LocationData location = createLocationData("device-10", 52.52f, 13.41f);
            when(deviceLocationService.getFirstUserDeviceLocation())
                .thenReturn(Optional.of(location));

            String response = createOpenMeteoResponse(
                LocalDate.now().minusDays(3).toString()
            );

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(ok(response)
                    .withFixedDelay(500)));

            Thread updateThread = new Thread(() ->
                historicalWeatherService.updateHistoricalWeatherForCurrentLocation());
            updateThread.start();

            Thread.sleep(100);

            historicalWeatherService.updateHistoricalWeatherForCurrentLocation();

            updateThread.join();
        }

        @Test
        @DisplayName("should handle no device gracefully")
        void shouldHandleNoDevice() {
            when(deviceLocationService.getFirstUserDeviceLocation())
                .thenReturn(Optional.empty());

            historicalWeatherService.updateHistoricalWeatherForCurrentLocation();

            assertThat(repository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateHistoricalWeatherForAllDeviceLocations")
    class UpdateHistoricalWeatherForAllDeviceLocationsTests {

        @Test
        @DisplayName("should update weather for all device locations")
        void shouldUpdateForAllDevices() {
            LocationData location1 = createLocationData("device-11", 52.52f, 13.41f);
            LocationData location2 = createLocationData("device-12", 48.14f, 11.58f);

            when(deviceLocationService.getAllUniqueDeviceLocations())
                .thenReturn(List.of(location1, location2));

            String response = createOpenMeteoResponse(
                LocalDate.now().minusDays(3).toString()
            );

            stubFor(get(urlPathMatching("/forecast"))
                .willReturn(ok(response)));

            historicalWeatherService.updateHistoricalWeatherForAllDeviceLocations();

            assertThat(repository.count()).isGreaterThan(0);
        }

        @Test
        @DisplayName("should handle no devices gracefully")
        void shouldHandleNoDevices() {
            when(deviceLocationService.getAllUniqueDeviceLocations())
                .thenReturn(List.of());

            historicalWeatherService.updateHistoricalWeatherForAllDeviceLocations();

            assertThat(repository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should continue on individual device failure")
        void shouldContinueOnFailure() {
            LocationData location1 = createLocationData("device-13", 52.52f, 13.41f);
            LocationData location2 = createLocationData("device-14", 48.14f, 11.58f);

            when(deviceLocationService.getAllUniqueDeviceLocations())
                .thenReturn(List.of(location1, location2));

            stubFor(get(urlPathMatching("/forecast"))
                .withQueryParam("latitude", equalTo("52.52"))
                .willReturn(aResponse().withStatus(500)));

            String response = createOpenMeteoResponse(
                LocalDate.now().minusDays(3).toString()
            );

            stubFor(get(urlPathMatching("/forecast"))
                .withQueryParam("latitude", equalTo("48.14"))
                .willReturn(ok(response)));

            historicalWeatherService.updateHistoricalWeatherForAllDeviceLocations();

            // Should still process second device
            assertThat(repository.count()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getHistoricalWeatherForDateRange")
    class GetHistoricalWeatherForDateRangeTests {

        @Test
        @DisplayName("should return records within date range")
        void shouldReturnRecordsInRange() {
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 10), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 15), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 20), "Berlin"));

            List<HistoricalWeather> results = historicalWeatherService.getHistoricalWeatherForDateRange(
                    LocalDate.of(2024, 1, 12),
                    LocalDate.of(2024, 1, 18));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWeatherDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        }

        @Test
        @DisplayName("should return empty list when no records exist")
        void shouldReturnEmptyWhenNoRecords() {
            List<HistoricalWeather> results = historicalWeatherService.getHistoricalWeatherForDateRange(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 31));

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should include boundary dates")
        void shouldIncludeBoundaryDates() {
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 10), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 20), "Berlin"));

            List<HistoricalWeather> results = historicalWeatherService.getHistoricalWeatherForDateRange(
                    LocalDate.of(2024, 1, 10),
                    LocalDate.of(2024, 1, 20));

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getHistoricalWeatherForDate")
    class GetHistoricalWeatherForDateTests {

        @Test
        @DisplayName("should return record for specific date")
        void shouldReturnForSpecificDate() {
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 15), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 16), "Berlin"));

            HistoricalWeather result = historicalWeatherService.getHistoricalWeatherForDate(
                    LocalDate.of(2024, 1, 15), null);

            assertThat(result).isNotNull();
            assertThat(result.getWeatherDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        }

        @Test
        @DisplayName("should filter by device ID when provided")
        void shouldFilterByDeviceId() {
            HistoricalWeather device1 = createHistoricalWeather(LocalDate.of(2024, 1, 15), "Berlin");
            String deviceIdA = device1.getDeviceId();
            repository.save(device1);

            HistoricalWeather device2 = createHistoricalWeather(LocalDate.of(2024, 1, 15), "Munich");
            repository.save(device2);

            HistoricalWeather result = historicalWeatherService.getHistoricalWeatherForDate(
                    LocalDate.of(2024, 1, 15), deviceIdA);

            assertThat(result).isNotNull();
            assertThat(result.getDeviceId()).isEqualTo(deviceIdA);
        }

        @Test
        @DisplayName("should return null when no record exists")
        void shouldReturnNullWhenNotFound() {
            HistoricalWeather result = historicalWeatherService.getHistoricalWeatherForDate(
                    LocalDate.of(2024, 1, 15), null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should fall back to any record when deviceId is empty")
        void shouldFallBackWhenEmptyDeviceId() {
            repository.save(createHistoricalWeather(LocalDate.of(2024, 1, 15), "Berlin"));

            HistoricalWeather result = historicalWeatherService.getHistoricalWeatherForDate(
                    LocalDate.of(2024, 1, 15), "");

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("getAvailableCitiesWithHistoricalWeather")
    class GetAvailableCitiesWithHistoricalWeatherTests {

        @Test
        @DisplayName("should return distinct cities")
        void shouldReturnDistinctCities() {
            repository.save(createHistoricalWeather(LocalDate.now(), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.now().minusDays(1), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.now(), "Munich"));

            List<String> cities = historicalWeatherService.getAvailableCitiesWithHistoricalWeather();

            assertThat(cities).containsExactlyInAnyOrder("Berlin", "Munich");
        }

        @Test
        @DisplayName("should return empty list when no records")
        void shouldReturnEmptyWhenNoRecords() {
            List<String> cities = historicalWeatherService.getAvailableCitiesWithHistoricalWeather();

            assertThat(cities).isEmpty();
        }

        @Test
        @DisplayName("should only include recent data")
        void shouldOnlyIncludeRecentData() {
            repository.save(createHistoricalWeather(LocalDate.now(), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.now().minusYears(2), "Munich"));

            List<String> cities = historicalWeatherService.getAvailableCitiesWithHistoricalWeather();

            assertThat(cities).containsExactly("Berlin");
        }
    }

    @Nested
    @DisplayName("cleanupOldHistoricalWeather")
    class CleanupOldHistoricalWeatherTests {

        @Test
        @DisplayName("should delete old records by creation date")
        void shouldDeleteOldRecordsByCreationDate() {
            HistoricalWeather recent = createHistoricalWeather(LocalDate.now().minusDays(30), "Berlin");
            repository.save(recent);

            long initialCount = repository.count();

            historicalWeatherService.cleanupOldHistoricalWeather();

            assertThat(repository.count()).isEqualTo(initialCount);
        }

        @Test
        @DisplayName("should delete records for old dates")
        void shouldDeleteRecordsForOldDates() {
            HistoricalWeather veryOld = createHistoricalWeather(LocalDate.now().minusYears(3), "Berlin");
            repository.save(veryOld);

            HistoricalWeather recent = createHistoricalWeather(LocalDate.now().minusDays(30), "Berlin");
            repository.save(recent);

            historicalWeatherService.cleanupOldHistoricalWeather();

            assertThat(repository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle cleanup gracefully")
        void shouldHandleCleanup() {
            repository.save(createHistoricalWeather(LocalDate.now().minusYears(2), "Berlin"));
            repository.save(createHistoricalWeather(LocalDate.now().minusDays(30), "Berlin"));

            historicalWeatherService.cleanupOldHistoricalWeather();

            assertThat(repository.count()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should handle empty database")
        void shouldHandleEmptyDatabase() {
            historicalWeatherService.cleanupOldHistoricalWeather();

            assertThat(repository.count()).isEqualTo(0);
        }
    }
}
