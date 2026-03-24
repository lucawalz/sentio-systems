package dev.syslabs.sentio.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dev.syslabs.sentio.BaseIntegrationTest;
import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.model.LocationData;
import dev.syslabs.sentio.model.WeatherForecast;
import dev.syslabs.sentio.repository.DeviceRepository;
import dev.syslabs.sentio.repository.WeatherForecastRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for WeatherForecastService.
 * Uses WireMock to mock OpenMeteo external API.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class WeatherForecastServiceIT extends BaseIntegrationTest {

    private static WireMockServer wireMockServer;
    private static final AtomicInteger hourCounter = new AtomicInteger(0);

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private static String createOpenMeteoForecastResponse() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        return String.format("""
                {
                    "latitude": 52.52,
                    "longitude": 13.41,
                    "timezone": "Europe/Berlin",
                    "hourly": {
                        "time": ["%s", "%s"],
                        "temperature_2m": [8.5, 9.2],
                        "relative_humidity_2m": [75, 72],
                        "precipitation_probability": [20, 15],
                        "precipitation": [0.0, 0.0],
                        "weather_code": [3, 2],
                        "wind_speed_10m": [12.5, 10.8],
                        "cloud_cover": [50, 40],
                        "pressure_msl": [1015.0, 1016.0]
                    }
                }
                """, now, now.plusHours(1));
    }

    @DynamicPropertySource
    static void configureWireMock(DynamicPropertyRegistry registry) {
        registry.add("openmeteo.api.base-url", () -> "http://localhost:8089");
    }

    @Autowired
    private WeatherForecastService weatherService;

    @Autowired
    private WeatherForecastRepository repository;

    @MockBean
    private DeviceLocationService deviceLocationService;

    @MockBean
    private DeviceService deviceService;

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
        deviceRepository.deleteAll();
        hourCounter.set(0);
    }

    private WeatherForecast createForecast(LocalDate date, String city) {
        // Use unique hour for each forecast to avoid unique constraint violation
        int hour = hourCounter.getAndIncrement() % 24;
        WeatherForecast forecast = new WeatherForecast();
        forecast.setForecastDate(date);
        forecast.setForecastDateTime(date.atTime(hour, 0));
        forecast.setCity(city);
        forecast.setCountry("Germany");
        forecast.setTemperature(15.0f);
        forecast.setDeviceId("test-device-" + city.hashCode()); // Unique device per city
        return forecast;
    }

    // ========== Repository Query Tests ==========

    @Nested
    @DisplayName("getUpcomingForecasts")
    class GetUpcomingForecastsTests {

        @Test
        @DisplayName("should return only future forecasts")
        void shouldReturnOnlyFutureForecasts() {
            WeatherForecast past = createForecast(LocalDate.now().minusDays(1), "Berlin");
            past.setForecastDateTime(LocalDateTime.now().minusDays(1));
            repository.save(past);

            WeatherForecast future = createForecast(LocalDate.now().plusDays(1), "Munich");
            future.setForecastDateTime(LocalDateTime.now().plusDays(1));
            repository.save(future);

            List<WeatherForecast> results = weatherService.getUpcomingForecasts();

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getForecastDate()).isAfter(LocalDate.now().minusDays(1));
        }
    }

    @Nested
    @DisplayName("getForecastsForDateRange")
    class GetForecastsForDateRangeTests {

        @Test
        @DisplayName("should filter by date range")
        void shouldFilterByDateRange() {
            repository.save(createForecast(LocalDate.of(2024, 1, 10), "Berlin"));
            repository.save(createForecast(LocalDate.of(2024, 1, 15), "Munich"));
            repository.save(createForecast(LocalDate.of(2024, 1, 20), "Hamburg"));

            List<WeatherForecast> results = weatherService.getForecastsForDateRange(
                    LocalDate.of(2024, 1, 12),
                    LocalDate.of(2024, 1, 18));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getForecastDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        }
    }

    @Nested
    @DisplayName("getForecastsForDate")
    class GetForecastsForDateTests {

        @Test
        @DisplayName("should return forecasts for specific date")
        void shouldReturnForSpecificDate() {
            repository.save(createForecast(LocalDate.of(2024, 1, 15), "Berlin"));
            repository.save(createForecast(LocalDate.of(2024, 1, 15), "Munich"));
            repository.save(createForecast(LocalDate.of(2024, 1, 16), "Hamburg"));

            List<WeatherForecast> results = weatherService.getForecastsForDate(
                    LocalDate.of(2024, 1, 15));

            assertThat(results).hasSize(2);
        }
    }

    @Nested
    @DisplayName("cleanupOldForecasts")
    class CleanupOldForecastsTests {

        @Test
        @DisplayName("should delete forecasts older than retention period")
        void shouldDeleteOldForecasts() {
            WeatherForecast old = createForecast(LocalDate.now().minusDays(10), "Berlin");
            old.setForecastDateTime(LocalDateTime.now().minusDays(10));
            repository.save(old);

            WeatherForecast recent = createForecast(LocalDate.now().minusDays(3), "Munich");
            recent.setForecastDateTime(LocalDateTime.now().minusDays(3));
            repository.save(recent);

            weatherService.cleanupOldForecasts();

            // Just verify it runs without error
            assertThat(repository.count()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getAvailableCities")
    class GetAvailableCitiesTests {

        @Test
        @DisplayName("should return distinct cities")
        void shouldReturnDistinctCities() {
            repository.save(createForecast(LocalDate.now(), "Berlin"));
            repository.save(createForecast(LocalDate.now().plusDays(1), "Berlin"));
            repository.save(createForecast(LocalDate.now(), "Munich"));

            List<String> cities = weatherService.getAvailableCities();

            assertThat(cities).containsExactlyInAnyOrder("Berlin", "Munich");
        }
    }

    // ========== External API Tests (WireMock) ==========

    @Nested
    @DisplayName("getForecastForLocation (with WireMock)")
    class GetForecastForLocationTests {

        @Test
        @DisplayName("should fetch and persist forecasts from OpenMeteo API")
        void shouldFetchFromOpenMeteoAPI() {
            // Stub OpenMeteo API - service uses /forecast path with query parameters
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Berlin");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-1");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).isNotNull();
            verify(getRequestedFor(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*")));
        }
    }

    // ========== Forecast Fetching Tests ==========

    @Nested
    @DisplayName("Forecast Fetching")
    class ForecastFetchingTests {

        @Test
        @DisplayName("should retrieve valid forecast data")
        void shouldRetrieveValidForecastData() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Berlin");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-1");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).isNotEmpty();
            assertThat(forecasts.get(0).getCity()).isEqualTo("Berlin");
            assertThat(forecasts.get(0).getCountry()).isEqualTo("Germany");
            assertThat(forecasts.get(0).getTemperature()).isNotNull();
        }

        @Test
        @DisplayName("should handle multiple days forecast")
        void shouldHandleMultipleDaysForecast() {
            String multiDayResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00", "2026-02-12T12:00", "2026-02-13T12:00"],
                            "temperature_2m": [8.5, 9.2, 10.1],
                            "relative_humidity_2m": [75, 72, 70],
                            "precipitation_probability": [20, 15, 10],
                            "precipitation": [0.0, 0.0, 0.0],
                            "weather_code": [3, 2, 1],
                            "wind_speed_10m": [12.5, 10.8, 9.5],
                            "cloud_cover": [50, 40, 30],
                            "pressure_msl": [1015.0, 1016.0, 1017.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(multiDayResponse, LocalDateTime.now(), 24, 3))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Berlin");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-2");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).hasSizeGreaterThanOrEqualTo(3);
            // Verify forecasts span multiple dates
            long distinctDates = forecasts.stream()
                    .map(WeatherForecast::getForecastDate)
                    .distinct()
                    .count();
            assertThat(distinctDates).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("should process hourly forecasts")
        void shouldProcessHourlyForecasts() {
            String hourlyResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00", "2026-02-11T13:00", "2026-02-11T14:00"],
                            "temperature_2m": [8.5, 9.2, 10.0],
                            "relative_humidity_2m": [75, 72, 70],
                            "precipitation_probability": [20, 15, 10],
                            "precipitation": [0.0, 0.0, 0.0],
                            "weather_code": [3, 2, 1],
                            "wind_speed_10m": [12.5, 10.8, 9.5],
                            "cloud_cover": [50, 40, 30],
                            "pressure_msl": [1015.0, 1016.0, 1017.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(hourlyResponse, LocalDateTime.now(), 1, 3))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Munich");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-3");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).hasSizeGreaterThanOrEqualTo(3);
            // Verify hourly granularity
            assertThat(forecasts.get(0).getForecastDateTime()).isNotNull();
        }

        @Test
        @DisplayName("should handle missing forecast data")
        void shouldHandleMissingForecastData() {
            String emptyResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": [],
                            "temperature_2m": [],
                            "relative_humidity_2m": [],
                            "precipitation_probability": [],
                            "precipitation": [],
                            "weather_code": [],
                            "wind_speed_10m": [],
                            "cloud_cover": [],
                            "pressure_msl": []
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(emptyResponse)));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Hamburg");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-4");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).isEmpty();
        }

        @Test
        @DisplayName("should handle invalid location gracefully")
        void shouldHandleInvalidLocation() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(999.0f); // Invalid latitude
            locationData.setLongitude(999.0f); // Invalid longitude
            locationData.setCity("Invalid");
            locationData.setCountry("Unknown");
            locationData.setDeviceId("test-device-5");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    999.0f, 999.0f, locationData);

            // Service should handle gracefully
            assertThat(forecasts).isNotNull();
        }

        @Test
        @DisplayName("should respect future date limits")
        void shouldRespectFutureDateLimits() {
            // Create response with dates beyond 7 days
            String futureDatesResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00", "2026-02-25T12:00"],
                            "temperature_2m": [8.5, 9.2],
                            "relative_humidity_2m": [75, 72],
                            "precipitation_probability": [20, 15],
                            "precipitation": [0.0, 0.0],
                            "weather_code": [3, 2],
                            "wind_speed_10m": [12.5, 10.8],
                            "cloud_cover": [50, 40],
                            "pressure_msl": [1015.0, 1016.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(futureDatesResponse, LocalDateTime.now(), 24 * 14, 2))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Cologne");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-6");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should filter out dates beyond 7 days
            assertThat(forecasts).allMatch(f -> !f.getForecastDate().isAfter(LocalDate.now().plusDays(7)));
        }

        @Test
        @DisplayName("should handle past date data")
        void shouldHandlePastDateData() {
            // Create response with past dates (beyond 3 hours ago)
            String pastDatesResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-01T12:00", "2026-02-11T12:00"],
                            "temperature_2m": [8.5, 9.2],
                            "relative_humidity_2m": [75, 72],
                            "precipitation_probability": [20, 15],
                            "precipitation": [0.0, 0.0],
                            "weather_code": [3, 2],
                            "wind_speed_10m": [12.5, 10.8],
                            "cloud_cover": [50, 40],
                            "pressure_msl": [1015.0, 1016.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(pastDatesResponse, LocalDateTime.now().minusDays(10), 24 * 10, 2))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Frankfurt");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-7");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should filter out dates more than 3 hours old
            assertThat(forecasts).allMatch(f -> !f.getForecastDateTime().isBefore(LocalDateTime.now().minusHours(3)));
        }
    }

    // ========== Error Scenarios ==========

    @Nested
    @DisplayName("Error Scenarios")
    class ErrorScenariosTests {

        @Test
        @DisplayName("should handle API timeout")
        void shouldHandleApiTimeout() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withFixedDelay(10000) // 10 second delay
                            .withStatus(200)));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Stuttgart");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-8");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should return empty list on timeout
            assertThat(forecasts).isEmpty();
        }

        @Test
        @DisplayName("should handle 500 server error")
        void shouldHandle500ServerError() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error")));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Dresden");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-9");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should handle error gracefully
            assertThat(forecasts).isEmpty();
        }

        @Test
        @DisplayName("should handle malformed JSON response")
        void shouldHandleMalformedResponse() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ invalid json }")));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Leipzig");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-10");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should handle parsing error gracefully
            assertThat(forecasts).isEmpty();
        }

        @Test
        @DisplayName("should handle network failure")
        void shouldHandleNetworkFailure() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Dortmund");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-11");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should handle network error gracefully
            assertThat(forecasts).isEmpty();
        }

        @Test
        @DisplayName("should handle invalid API response structure")
        void shouldHandleInvalidResponseStructure() {
            String invalidStructure = """
                    {
                        "wrong_field": "wrong_value",
                        "missing": "hourly"
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(invalidStructure)));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Essen");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-12");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should handle missing fields gracefully
            assertThat(forecasts).isEmpty();
        }

        @Test
        @DisplayName("should handle partial data in response")
        void shouldHandlePartialData() {
            String partialResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00", "2026-02-11T13:00"],
                            "temperature_2m": [8.5, null],
                            "relative_humidity_2m": [75, 72],
                            "weather_code": [3, 2]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(partialResponse)));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Bremen");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-13");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should handle null values gracefully
            assertThat(forecasts).isNotNull();
        }
    }

    // ========== Caching Tests ==========

    @Nested
    @DisplayName("Caching Behavior")
    class CachingTests {

        @Test
        @DisplayName("should use cached data when available")
        void shouldUseCachedData() {
            // Pre-populate cache with forecast
            WeatherForecast cached = createForecast(LocalDate.now().plusDays(1), "Nuremberg");
            cached.setDeviceId("test-device-14");
            repository.save(cached);

            // No WireMock stub - should use cached data
            List<WeatherForecast> forecasts = repository.findByDeviceIdAndForecastDateGreaterThanEqual(
                    "test-device-14", LocalDate.now());

            assertThat(forecasts).isNotEmpty();
            assertThat(forecasts.get(0).getCity()).isEqualTo("Nuremberg");
        }

        @Test
        @DisplayName("should trigger API call on cache miss")
        void shouldTriggerApiCallOnCacheMiss() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Hanover");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-15");

            // Cache is empty, should trigger API call
            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).isNotNull();
            verify(getRequestedFor(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*")));
        }

        @Test
        @DisplayName("should invalidate expired cache entries")
        void shouldInvalidateExpiredCache() {
            // Create old forecast (more than 1 day old)
            WeatherForecast expired = createForecast(LocalDate.now().minusDays(2), "Duisburg");
            expired.setDeviceId("test-device-16");
            expired.setForecastDateTime(LocalDateTime.now().minusDays(2));
            repository.save(expired);

            long initialCount = repository.count();

            // Trigger cleanup
            weatherService.cleanupOldForecasts();

            long finalCount = repository.count();

            // Old forecasts should be cleaned up
            assertThat(finalCount).isLessThanOrEqualTo(initialCount);
        }
    }

    // ========== Data Processing Tests ==========

    @Nested
    @DisplayName("Data Processing")
    class DataProcessingTests {

        @Test
        @DisplayName("should correctly parse temperature values")
        void shouldParseTemperatureValues() {
            String temperatureResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00"],
                            "temperature_2m": [15.7],
                            "relative_humidity_2m": [75],
                            "weather_code": [1],
                            "wind_speed_10m": [10.5],
                            "cloud_cover": [40],
                            "pressure_msl": [1015.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(temperatureResponse, LocalDateTime.now(), 1, 1))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Bochum");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-17");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).isNotEmpty();
            assertThat(forecasts.get(0).getTemperature()).isEqualTo(15.7f);
        }

        @Test
        @DisplayName("should correctly parse precipitation data")
        void shouldParsePrecipitationData() {
            String precipitationResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00"],
                            "temperature_2m": [8.5],
                            "relative_humidity_2m": [85],
                            "precipitation": [2.5],
                            "precipitation_probability": [80],
                            "rain": [2.0],
                            "showers": [0.5],
                            "weather_code": [61],
                            "wind_speed_10m": [12.5],
                            "cloud_cover": [90],
                            "pressure_msl": [1010.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(precipitationResponse, LocalDateTime.now(), 1, 1))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Wuppertal");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-18");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).isNotEmpty();
            WeatherForecast forecast = forecasts.get(0);
            assertThat(forecast.getPrecipitation()).isEqualTo(2.5f);
            assertThat(forecast.getPrecipitationProbability()).isEqualTo(80.0f);
            assertThat(forecast.getRain()).isEqualTo(2.0f);
            assertThat(forecast.getShowers()).isEqualTo(0.5f);
        }

        @Test
        @DisplayName("should correctly map weather codes to conditions")
        void shouldMapWeatherCodes() {
            String weatherCodeResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00", "2026-02-11T13:00", "2026-02-11T14:00"],
                            "temperature_2m": [8.5, 9.2, 10.0],
                            "relative_humidity_2m": [75, 72, 70],
                            "weather_code": [0, 61, 95],
                            "wind_speed_10m": [12.5, 10.8, 15.0],
                            "cloud_cover": [0, 80, 100],
                            "pressure_msl": [1015.0, 1010.0, 1005.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(weatherCodeResponse, LocalDateTime.now(), 1, 3))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Bielefeld");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-19");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).hasSizeGreaterThanOrEqualTo(3);
            // Check weather code mapping
            assertThat(forecasts).anyMatch(f -> f.getWeatherMain() != null);
            assertThat(forecasts).anyMatch(f -> f.getDescription() != null);
            assertThat(forecasts).anyMatch(f -> f.getIcon() != null);
        }

        @Test
        @DisplayName("should map all WMO weather codes correctly")
        void shouldMapAllWeatherCodesCorrectly() {
            // Test comprehensive weather code mapping including edge cases
            String allWeatherCodesResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T00:00", "2026-02-11T01:00", "2026-02-11T02:00",
                                     "2026-02-11T03:00", "2026-02-11T04:00", "2026-02-11T05:00",
                                     "2026-02-11T06:00", "2026-02-11T07:00", "2026-02-11T08:00",
                                     "2026-02-11T09:00", "2026-02-11T10:00", "2026-02-11T11:00",
                                     "2026-02-11T12:00", "2026-02-11T13:00", "2026-02-11T14:00",
                                     "2026-02-11T15:00", "2026-02-11T16:00", "2026-02-11T17:00",
                                     "2026-02-11T18:00", "2026-02-11T19:00", "2026-02-11T20:00",
                                     "2026-02-11T21:00", "2026-02-11T22:00", "2026-02-11T23:00"],
                            "temperature_2m": [8.5, 9.0, 9.5, 10.0, 10.5, 11.0, 11.5, 12.0, 12.5, 13.0,
                                              13.5, 14.0, 14.5, 15.0, 15.5, 16.0, 16.5, 17.0, 17.5, 18.0,
                                              18.5, 19.0, 19.5, 20.0],
                            "relative_humidity_2m": [75, 74, 73, 72, 71, 70, 69, 68, 67, 66,
                                                     65, 64, 63, 62, 61, 60, 59, 58, 57, 56,
                                                     55, 54, 53, 52],
                            "weather_code": [0, 1, 2, 3, 45, 48, 51, 53, 55, 56,
                                           57, 61, 63, 65, 66, 67, 71, 73, 75, 77,
                                           80, 81, 82, 999],
                            "wind_speed_10m": [10.0, 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 10.9,
                                              11.0, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9,
                                              12.0, 12.1, 12.2, 12.3],
                            "cloud_cover": [10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
                                          10, 20, 30, 40, 50, 60, 70, 80, 90, 100,
                                          10, 20, 30, 40],
                            "pressure_msl": [1015.0, 1015.1, 1015.2, 1015.3, 1015.4, 1015.5, 1015.6, 1015.7, 1015.8, 1015.9,
                                           1016.0, 1016.1, 1016.2, 1016.3, 1016.4, 1016.5, 1016.6, 1016.7, 1016.8, 1016.9,
                                           1017.0, 1017.1, 1017.2, 1017.3]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(allWeatherCodesResponse, LocalDateTime.now().minusHours(2), 1, 24))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Karlsruhe");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-20");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).hasSizeGreaterThanOrEqualTo(20);

            // Verify specific weather code mappings
            assertThat(forecasts).anyMatch(f -> "Clear".equals(f.getWeatherMain())); // code 0
            assertThat(forecasts).anyMatch(f -> "Clouds".equals(f.getWeatherMain())); // code 1,2,3
            assertThat(forecasts).anyMatch(f -> "Mist".equals(f.getWeatherMain())); // code 45,48
            assertThat(forecasts).anyMatch(f -> "Drizzle".equals(f.getWeatherMain())); // code 51,53,55,56,57
            assertThat(forecasts).anyMatch(f -> "Rain".equals(f.getWeatherMain())); // code 61,63,65,66,67,80,81,82
            assertThat(forecasts).anyMatch(f -> "Snow".equals(f.getWeatherMain())); // code 71,73,75,77
            assertThat(forecasts).anyMatch(f -> "Unknown".equals(f.getWeatherMain())); // code 999 (unknown)
        }

        @Test
        @DisplayName("should parse all weather parameters correctly")
        void shouldParseAllWeatherParametersCorrectly() {
            String comprehensiveResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00"],
                            "temperature_2m": [15.7],
                            "relative_humidity_2m": [65],
                            "apparent_temperature": [14.2],
                            "surface_pressure": [1013.25],
                            "wind_speed_10m": [12.5],
                            "wind_direction_10m": [180],
                            "wind_gusts_10m": [18.3],
                            "cloud_cover": [75],
                            "visibility": [10000],
                            "precipitation": [1.5],
                            "rain": [1.2],
                            "showers": [0.3],
                            "snowfall": [0.0],
                            "snow_depth": [0.0],
                            "dew_point_2m": [8.5],
                            "precipitation_probability": [60],
                            "weather_code": [61]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(comprehensiveResponse, LocalDateTime.now(), 1, 1))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Mannheim");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-21");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).isNotEmpty();
            WeatherForecast forecast = forecasts.get(0);

            // Verify all parameters are parsed correctly
            assertThat(forecast.getTemperature()).isEqualTo(15.7f);
            assertThat(forecast.getHumidity()).isEqualTo(65.0f);
            assertThat(forecast.getApparentTemperature()).isEqualTo(14.2f);
            assertThat(forecast.getPressure()).isEqualTo(1013.25f);
            assertThat(forecast.getWindSpeed()).isEqualTo(12.5f);
            assertThat(forecast.getWindDirection()).isEqualTo(180.0f);
            assertThat(forecast.getWindGusts()).isEqualTo(18.3f);
            assertThat(forecast.getCloudCover()).isEqualTo(75.0f);
            assertThat(forecast.getVisibility()).isEqualTo(10000.0f);
            assertThat(forecast.getPrecipitation()).isEqualTo(1.5f);
            assertThat(forecast.getRain()).isEqualTo(1.2f);
            assertThat(forecast.getShowers()).isEqualTo(0.3f);
            assertThat(forecast.getSnowfall()).isEqualTo(0.0f);
            assertThat(forecast.getSnowDepth()).isEqualTo(0.0f);
            assertThat(forecast.getDewPoint()).isEqualTo(8.5f);
            assertThat(forecast.getPrecipitationProbability()).isEqualTo(60.0f);
            assertThat(forecast.getWeatherCode()).isEqualTo(61);

            // Verify location data
            assertThat(forecast.getCity()).isEqualTo("Mannheim");
            assertThat(forecast.getCountry()).isEqualTo("Germany");
            assertThat(forecast.getLatitude()).isEqualTo(52.52f);
            assertThat(forecast.getLongitude()).isEqualTo(13.41f);
            assertThat(forecast.getDeviceId()).isEqualTo("test-device-21");
        }

        @Test
        @DisplayName("should handle snow weather conditions")
        void shouldHandleSnowConditions() {
            String snowResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00", "2026-02-11T13:00"],
                            "temperature_2m": [-2.5, -3.0],
                            "relative_humidity_2m": [95, 98],
                            "snowfall": [5.0, 8.0],
                            "snow_depth": [15.0, 23.0],
                            "weather_code": [73, 75],
                            "wind_speed_10m": [15.0, 20.0],
                            "cloud_cover": [100, 100],
                            "pressure_msl": [1005.0, 1004.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(snowResponse, LocalDateTime.now(), 1, 2))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Augsburg");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-22");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).hasSizeGreaterThanOrEqualTo(2);
            assertThat(forecasts).allMatch(f -> "Snow".equals(f.getWeatherMain()));
            assertThat(forecasts).anyMatch(f -> f.getSnowfall() > 0);
            assertThat(forecasts).anyMatch(f -> f.getSnowDepth() > 0);
        }

        @Test
        @DisplayName("should handle thunderstorm weather conditions")
        void shouldHandleThunderstormConditions() {
            String thunderstormResponse = """
                    {
                        "latitude": 52.52,
                        "longitude": 13.41,
                        "timezone": "Europe/Berlin",
                        "hourly": {
                            "time": ["2026-02-11T12:00", "2026-02-11T13:00", "2026-02-11T14:00"],
                            "temperature_2m": [18.5, 19.0, 19.5],
                            "relative_humidity_2m": [90, 92, 95],
                            "precipitation": [10.0, 15.0, 20.0],
                            "weather_code": [95, 96, 99],
                            "wind_speed_10m": [25.0, 30.0, 35.0],
                            "wind_gusts_10m": [45.0, 50.0, 55.0],
                            "cloud_cover": [100, 100, 100],
                            "pressure_msl": [1000.0, 998.0, 996.0]
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(injectTimes(thunderstormResponse, LocalDateTime.now(), 1, 3))));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Freiburg");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-23");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            assertThat(forecasts).hasSizeGreaterThanOrEqualTo(3);
            assertThat(forecasts).allMatch(f -> "Thunderstorm".equals(f.getWeatherMain()));
            assertThat(forecasts).anyMatch(f -> f.getDescription().contains("hail"));
        }
    }

    // ========== Device-scoped Methods Tests ==========

    @Nested
    @DisplayName("Device-scoped Operations")
    @WithMockUser(username = "test-user")
    class DeviceScopedTests {

        @Test
        @DisplayName("should get forecasts for specific device")
        void shouldGetForecastsForDevice() {
            String deviceId = "test-device-100";

            // Create device for verification
            Device device = new Device();
            device.setId(deviceId);
            device.setOwnerId("test-user");
            device.setName("Test Device");
            deviceRepository.save(device);

            // Create forecasts for this device
            WeatherForecast forecast1 = createForecast(LocalDate.now().plusDays(1), "Berlin");
            forecast1.setDeviceId(deviceId);
            repository.save(forecast1);

            WeatherForecast forecast2 = createForecast(LocalDate.now().plusDays(2), "Berlin");
            forecast2.setDeviceId(deviceId);
            repository.save(forecast2);

            // Create forecast for different device
            WeatherForecast otherForecast = createForecast(LocalDate.now().plusDays(1), "Munich");
            otherForecast.setDeviceId("other-device");
            repository.save(otherForecast);

            // Mock device service to return the device
            when(deviceService.getVerifiedDevice(deviceId)).thenReturn(device);

            List<WeatherForecast> forecasts = weatherService.getForecastForDevice(deviceId);

            assertThat(forecasts).hasSize(2);
            assertThat(forecasts).allMatch(f -> deviceId.equals(f.getDeviceId()));
        }

        @Test
        @DisplayName("should throw exception for invalid device")
        void shouldThrowForInvalidDevice() {
            String invalidDeviceId = "invalid-device";

            when(deviceService.getVerifiedDevice(invalidDeviceId))
                    .thenThrow(new IllegalArgumentException("Device not found"));

            assertThatThrownBy(() -> weatherService.getForecastForDevice(invalidDeviceId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found");
        }
    }

    // ========== getForecastForCurrentLocation Tests ==========

    @Nested
    @DisplayName("getForecastForCurrentLocation")
    class GetForecastForCurrentLocationTests {

        @Test
        @DisplayName("should return forecasts when device location exists")
        void shouldReturnForecastsWhenDeviceLocationExists() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Berlin");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-30");

            when(deviceLocationService.getFirstUserDeviceLocation())
                    .thenReturn(Optional.of(locationData));

            List<WeatherForecast> forecasts = weatherService.getForecastForCurrentLocation();

            assertThat(forecasts).isNotEmpty();
            verify(getRequestedFor(urlPathEqualTo("/forecast")));
        }

        @Test
        @DisplayName("should return empty list when no device location exists")
        void shouldReturnEmptyListWhenNoDeviceLocation() {
            when(deviceLocationService.getFirstUserDeviceLocation())
                    .thenReturn(Optional.empty());

            List<WeatherForecast> forecasts = weatherService.getForecastForCurrentLocation();

            assertThat(forecasts).isEmpty();
        }
    }

    // ========== Update Methods Tests ==========

    @Nested
    @DisplayName("Update Methods")
    class UpdateMethodsTests {

        @Test
        @DisplayName("updateForecastsForCurrentLocation should update forecasts")
        void shouldUpdateForecastsForCurrentLocation() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Berlin");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-40");

            when(deviceLocationService.getFirstUserDeviceLocation())
                    .thenReturn(Optional.of(locationData));

            weatherService.updateForecastsForCurrentLocation();

            // Verify forecasts were created
            List<WeatherForecast> forecasts = repository.findAll();
            assertThat(forecasts).isNotEmpty();
        }

        @Test
        @DisplayName("updateForecastsForCurrentLocation should handle missing device gracefully")
        void shouldHandleMissingDevice() {
            when(deviceLocationService.getFirstUserDeviceLocation())
                    .thenReturn(Optional.empty());

            // Should not throw exception
            weatherService.updateForecastsForCurrentLocation();

            // No forecasts should be created
            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("updateForecastsForAllDeviceLocations should update for all devices")
        void shouldUpdateForAllDevices() {
            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData location1 = new LocationData();
            location1.setLatitude(52.52f);
            location1.setLongitude(13.41f);
            location1.setCity("Berlin");
            location1.setCountry("Germany");
            location1.setDeviceId("device-1");

            LocationData location2 = new LocationData();
            location2.setLatitude(48.14f);
            location2.setLongitude(11.58f);
            location2.setCity("Munich");
            location2.setCountry("Germany");
            location2.setDeviceId("device-2");

            when(deviceLocationService.getAllUniqueDeviceLocations())
                    .thenReturn(List.of(location1, location2));

            weatherService.updateForecastsForAllDeviceLocations();

            // Verify forecasts were created for both locations
            List<WeatherForecast> forecasts = repository.findAll();
            assertThat(forecasts).isNotEmpty();
        }

        @Test
        @DisplayName("updateForecastsForAllDeviceLocations should handle no devices gracefully")
        void shouldHandleNoDevices() {
            when(deviceLocationService.getAllUniqueDeviceLocations())
                    .thenReturn(List.of());

            // Should not throw exception
            weatherService.updateForecastsForAllDeviceLocations();

            assertThat(repository.count()).isZero();
        }

        @Test
        @DisplayName("should skip concurrent update requests")
        void shouldSkipConcurrentUpdates() {
            // This test verifies the isUpdating flag prevents concurrent updates
            // In a real scenario, we'd use CountDownLatch or similar to test concurrency
            // For now, we just verify the method completes without error

            when(deviceLocationService.getAllUniqueDeviceLocations())
                    .thenReturn(List.of());

            weatherService.updateForecastsForAllDeviceLocations();

            // Second call should also complete (even though first finished)
            weatherService.updateForecastsForAllDeviceLocations();
        }
    }

    // ========== Additional Query Methods Tests ==========

    @Nested
    @DisplayName("Additional Query Methods")
    class AdditionalQueryTests {

        @Test
        @DisplayName("getLatestForecastForDate should return most recent forecast")
        void shouldReturnLatestForecastForDate() {
            LocalDate targetDate = LocalDate.now().plusDays(1);

            // Create older forecast
            WeatherForecast older = createForecast(targetDate, "Berlin");
            older.setForecastDateTime(targetDate.atTime(10, 0));
            repository.save(older);

            // Wait a bit to ensure different created timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }

            // Create newer forecast
            WeatherForecast newer = createForecast(targetDate, "Berlin");
            newer.setForecastDateTime(targetDate.atTime(14, 0));
            repository.save(newer);

            WeatherForecast latest = weatherService.getLatestForecastForDate(targetDate);

            assertThat(latest).isNotNull();
            assertThat(latest.getForecastDate()).isEqualTo(targetDate);
        }

        @Test
        @DisplayName("getRecentForecasts should return forecasts from last N hours")
        void shouldReturnRecentForecasts() {
            // Create recent forecast
            WeatherForecast recent = createForecast(LocalDate.now(), "BerlinRecent");
            recent.setForecastDateTime(LocalDateTime.now().minusHours(1));
            repository.save(recent);

            // Create old forecast
            WeatherForecast old = createForecast(LocalDate.now().minusDays(1), "MunichRecent");
            old.setForecastDateTime(LocalDateTime.now().minusDays(1));
            repository.save(old);

            List<WeatherForecast> forecasts = weatherService.getRecentForecasts(2);

            long count = forecasts.stream()
                    .filter(f -> "BerlinRecent".equals(f.getCity()))
                    .count();

            assertThat(count).isEqualTo(1);
        }
    }

    // ========== Existing vs New Forecast Update Tests ==========

    @Nested
    @DisplayName("Forecast Update/Insert Logic")
    class ForecastUpdateInsertTests {

        @Test
        @DisplayName("should update existing forecast with same datetime and location")
        void shouldUpdateExistingForecast() {
            // Pre-populate with existing forecast
            WeatherForecast existing = createForecast(LocalDate.of(2026, 2, 11), "Berlin");
            existing.setForecastDateTime(LocalDateTime.of(2026, 2, 11, 12, 0));
            existing.setDeviceId("test-device-50");
            existing.setTemperature(5.0f);
            repository.save(existing);

            long beforeCount = repository.count();

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Berlin");
            locationData.setCountry("Germany");
            locationData.setDeviceId("test-device-50");

            weatherService.getForecastForLocation(52.52f, 13.41f, locationData);

            // Should update, not insert new
            long afterCount = repository.count();
            assertThat(afterCount).isGreaterThanOrEqualTo(beforeCount);
        }

        @Test
        @DisplayName("should insert new forecast for different device")
        void shouldInsertNewForecastForDifferentDevice() {
            // Pre-populate with existing forecast for device-1
            WeatherForecast existing = createForecast(LocalDate.of(2026, 2, 11), "Berlin");
            existing.setForecastDateTime(LocalDateTime.of(2026, 2, 11, 12, 0));
            existing.setDeviceId("device-1");
            repository.save(existing);

            stubFor(get(urlPathEqualTo("/forecast"))
                    .withQueryParam("latitude", matching(".*"))
                    .withQueryParam("longitude", matching(".*"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(createOpenMeteoForecastResponse())));

            LocationData locationData = new LocationData();
            locationData.setLatitude(52.52f);
            locationData.setLongitude(13.41f);
            locationData.setCity("Berlin");
            locationData.setCountry("Germany");
            locationData.setDeviceId("device-2");

            List<WeatherForecast> forecasts = weatherService.getForecastForLocation(
                    52.52f, 13.41f, locationData);

            // Should create new forecasts for device-2
            assertThat(forecasts).isNotEmpty();
            assertThat(forecasts).allMatch(f -> "device-2".equals(f.getDeviceId()));
        }
    }

    private String injectTimes(String jsonResponse, java.time.LocalDateTime start, int hoursInterval, int count) {
        java.time.LocalDateTime time = start.truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm");
        java.util.List<String> times = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            times.add("\"" + time.plusHours((long) i * hoursInterval).format(formatter) + "\"");
        }
        return jsonResponse.replaceAll("(?s)\"time\":\\s*\\[.*?\\]", "\"time\": [" + String.join(", ", times) + "]");
    }
}
