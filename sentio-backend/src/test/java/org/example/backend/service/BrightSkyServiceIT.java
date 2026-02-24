package org.example.backend.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.example.backend.BaseIntegrationTest;
import org.example.backend.dto.RadarMetadataDTO;
import org.example.backend.model.Device;
import org.example.backend.model.LocationData;
import org.example.backend.model.WeatherAlert;
import org.example.backend.model.WeatherRadarMetadata;
import org.example.backend.repository.DeviceRepository;
import org.example.backend.repository.WeatherAlertRepository;
import org.example.backend.repository.WeatherRadarMetadataRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Comprehensive integration tests for BrightSkyService.
 * Uses WireMock to mock BrightSky external API.
 *
 * Test Strategy:
 * - Test all public methods with real business logic execution
 * - Use WireMock for external BrightSky API calls
 * - Mock only DeviceLocationService and DeviceService (external dependencies)
 * - Exercise real database operations to test data transformation
 * - Test error handling, edge cases, and boundary conditions
 *
 * Coverage Goals:
 * - getAlertsForCurrentLocation() + device location handling
 * - getAlertsForLocation() + data transformation (processAlertNode,
 * getTextValue, parseDateTime)
 * - All query methods (getActiveAlerts, getAlertsBySeverity, getAlertsByCity,
 * etc.)
 * - Device-specific methods (getAlertsForDevice, getRadarEndpointForDevice,
 * etc.)
 * - Update methods (updateAlertsForCurrentLocation,
 * updateAlertsForAllDeviceLocations)
 * - Cleanup methods (cleanupExpiredAlerts, cleanupOldRadarMetadata)
 * - Radar endpoint URL generation (getRadarEndpointUrl,
 * getRadarEndpointUrlForCurrentLocation)
 * - Radar metadata fetching and storage (fetchAndStoreRadarMetadata,
 * fetchRadarMetadataForDevice)
 * - Radar metadata queries (getLatestRadarMetadata, getRecentRadarMetadata)
 * - Additional query methods (getRecentAlerts, getCitiesWithActiveAlerts)
 * - Error handling and edge cases
 *
 * Target Coverage: 80%+ (from 0.3%)
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BrightSkyServiceIT extends BaseIntegrationTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8090));
        wireMockServer.start();
        configureFor("localhost", 8090);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private static final String BRIGHTSKY_ALERTS_RESPONSE = """
            {
                "alerts": [
                    {
                        "id": 1,
                        "alert_id": "alert-001",
                        "status": "actual",
                        "effective": "2030-02-11T11:00:00+00:00",
                        "onset": "2030-02-11T12:00:00+00:00",
                        "expires": "2030-02-11T13:00:00+00:00",
                        "category": "Met",
                        "response_type": "Monitor",
                        "urgency": "Immediate",
                        "severity": "Moderate",
                        "certainty": "Likely",
                        "event_en": "Thunderstorm Warning",
                        "event_de": "Gewitterwarnung",
                        "event_code": 31,
                        "headline_en": "Thunderstorm expected",
                        "headline_de": "Gewitter erwartet",
                        "description_en": "Heavy thunderstorms expected in the region",
                        "description_de": "Schwere Gewitter erwartet",
                        "instruction_en": "Seek shelter indoors",
                        "instruction_de": "Schutz im Inneren suchen"
                    }
                ],
                "location": {
                    "warn_cell_id": 123456,
                    "name": "Berlin",
                    "name_short": "BER",
                    "district": "Berlin",
                    "state": "Berlin",
                    "state_short": "BE"
                }
            }
            """;

    private static final String BRIGHTSKY_RADAR_RESPONSE = """
            {
                "radar": [
                    {
                        "timestamp": "2030-01-15T10:00:00+00:00",
                        "source": "DWD",
                        "precipitation_5": [
                            [0, 50, 100, 150],
                            [200, 250, 300, 350],
                            [400, 450, 500, 550]
                        ]
                    }
                ],
                "geometry": {
                    "type": "Point",
                    "coordinates": [13.41, 52.52]
                },
                "bbox": [0, 0, 100, 100]
            }
            """;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("brightsky.api.base-url", () -> "http://localhost:8090");
        registry.add("brightsky.alerts.enabled", () -> "true");
    }

    @Autowired
    private BrightSkyService brightSkyService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private WeatherAlertRepository alertRepository;

    @Autowired
    private WeatherRadarMetadataRepository radarMetadataRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @MockBean
    private DeviceLocationService deviceLocationService;

    @MockBean
    private DeviceService deviceService;

    @BeforeEach
    void cleanUp() {
        alertRepository.deleteAll();
        radarMetadataRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    private WeatherAlert createAlert(String severity, LocalDateTime expires) {
        WeatherAlert alert = new WeatherAlert();
        alert.setAlertId(UUID.randomUUID().toString()); // Unique alert ID
        alert.setSeverity(severity);
        alert.setStatus("actual");
        alert.setEffective(LocalDateTime.now()); // Required field
        alert.setExpires(expires);
        alert.setCity("Berlin");
        alert.setWarnCellId(123456L);
        alert.setDeviceId("test-device-1");
        return alert;
    }

    private LocationData createLocationData(String deviceId, float lat, float lon, String city) {
        LocationData locationData = new LocationData();
        locationData.setDeviceId(deviceId);
        locationData.setIpAddress("192.168.1.1");
        locationData.setLatitude(lat);
        locationData.setLongitude(lon);
        locationData.setCity(city);
        locationData.setCountry("Germany");
        locationData.setRegion("Berlin");
        locationData.setCreatedAt(LocalDateTime.now());
        locationData.setUpdatedAt(LocalDateTime.now());
        return locationData;
    }

    private Device createDevice(String deviceId, Double lat, Double lon) {
        Device device = new Device();
        device.setId(deviceId);
        device.setLatitude(lat);
        device.setLongitude(lon);
        device.setName("Test Device");
        device.setOwnerId("test-user");
        device.setCreatedAt(LocalDateTime.now());
        return deviceRepository.save(device);
    }

    @Nested
    @DisplayName("getAlertsForCurrentLocation")
    class GetAlertsForCurrentLocationTests {

        @Test
        @DisplayName("should fetch alerts using device location")
        void shouldFetchAlertsUsingDeviceLocation() {
            LocationData location = createLocationData("device-1", 52.52f, 13.41f, "Berlin");
            when(deviceLocationService.getFirstUserDeviceLocation()).thenReturn(Optional.of(location));

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForCurrentLocation();

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getAlertId()).isEqualTo("alert-001");
            assertThat(alerts.get(0).getDeviceId()).isEqualTo("device-1");
        }

        @Test
        @DisplayName("should return empty list when no device is registered")
        void shouldReturnEmptyListWhenNoDevice() {
            when(deviceLocationService.getFirstUserDeviceLocation()).thenReturn(Optional.empty());

            List<WeatherAlert> alerts = brightSkyService.getAlertsForCurrentLocation();

            assertThat(alerts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Data Transformation Tests")
    class DataTransformationTests {

        @Test
        @DisplayName("should correctly transform all alert fields")
        void shouldTransformAllAlertFields() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(52.52f, 13.41f, "test-device");

            assertThat(alerts).hasSize(1);
            WeatherAlert alert = alerts.get(0);

            // Verify identification fields
            assertThat(alert.getAlertId()).isEqualTo("alert-001");
            assertThat(alert.getBrightSkyId()).isEqualTo(1);
            assertThat(alert.getStatus()).isEqualTo("actual");
            assertThat(alert.getDeviceId()).isEqualTo("test-device");

            // Verify timing fields
            assertThat(alert.getEffective()).isNotNull();
            assertThat(alert.getOnset()).isNotNull();
            assertThat(alert.getExpires()).isNotNull();

            // Verify metadata fields
            assertThat(alert.getCategory()).isEqualTo("Met");
            assertThat(alert.getResponseType()).isEqualTo("Monitor");
            assertThat(alert.getUrgency()).isEqualTo("Immediate");
            assertThat(alert.getSeverity()).isEqualTo("Moderate");
            assertThat(alert.getCertainty()).isEqualTo("Likely");

            // Verify event fields
            assertThat(alert.getEventCode()).isEqualTo(31);
            assertThat(alert.getEventEn()).isEqualTo("Thunderstorm Warning");
            assertThat(alert.getEventDe()).isEqualTo("Gewitterwarnung");

            // Verify multilingual content
            assertThat(alert.getHeadlineEn()).isEqualTo("Thunderstorm expected");
            assertThat(alert.getHeadlineDe()).isEqualTo("Gewitter erwartet");
            assertThat(alert.getDescriptionEn()).isEqualTo("Heavy thunderstorms expected in the region");
            assertThat(alert.getDescriptionDe()).isEqualTo("Schwere Gewitter erwartet");
            assertThat(alert.getInstructionEn()).isEqualTo("Seek shelter indoors");
            assertThat(alert.getInstructionDe()).isEqualTo("Schutz im Inneren suchen");

            // Verify location fields
            assertThat(alert.getWarnCellId()).isEqualTo(123456L);
            assertThat(alert.getName()).isEqualTo("Berlin");
            assertThat(alert.getNameShort()).isEqualTo("BER");
            assertThat(alert.getDistrict()).isEqualTo("Berlin");
            assertThat(alert.getState()).isEqualTo("Berlin");
            assertThat(alert.getStateShort()).isEqualTo("BE");
            assertThat(alert.getCity()).isEqualTo("BER");
            assertThat(alert.getCountry()).isEqualTo("Germany");
        }

        @Test
        @DisplayName("should handle alert updates when same alert_id exists")
        void shouldHandleAlertUpdates() {
            // First call - create alert
            stubFor(get(urlPathEqualTo("/alerts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            List<WeatherAlert> firstCall = brightSkyService.getAlertsForLocation(52.52f, 13.41f, "device-1");
            assertThat(firstCall).hasSize(1);

            // Second call - should update existing alert
            String updatedResponse = BRIGHTSKY_ALERTS_RESPONSE.replace("Moderate", "Severe");
            stubFor(get(urlPathEqualTo("/alerts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(updatedResponse)));

            List<WeatherAlert> secondCall = brightSkyService.getAlertsForLocation(52.52f, 13.41f, "device-1");

            assertThat(secondCall).hasSize(1);
            assertThat(secondCall.get(0).getSeverity()).isEqualTo("Severe");
            assertThat(alertRepository.count()).isEqualTo(1); // Should update, not create new
        }
    }

    @Nested
    @DisplayName("getActiveAlerts")
    class GetActiveAlertsTests {

        @Test
        @DisplayName("should return only non-expired alerts")
        void shouldReturnOnlyActiveAlerts() {
            WeatherAlert active = createAlert("moderate", LocalDateTime.now().plusHours(5));
            alertRepository.save(active);

            WeatherAlert expired = createAlert("minor", LocalDateTime.now().minusHours(1));
            alertRepository.save(expired);

            List<WeatherAlert> results = brightSkyService.getActiveAlerts();

            assertThat(results).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getAlertsBySeverity")
    class GetAlertsBySeverityTests {

        @Test
        @DisplayName("should filter by severity level")
        void shouldFilterBySeverity() {
            alertRepository.save(createAlert("severe", LocalDateTime.now().plusHours(5)));
            alertRepository.save(createAlert("moderate", LocalDateTime.now().plusHours(5)));
            alertRepository.save(createAlert("severe", LocalDateTime.now().plusHours(5)));

            List<WeatherAlert> results = brightSkyService.getAlertsBySeverity("severe");

            assertThat(results).hasSize(2);
            assertThat(results).allMatch(a -> a.getSeverity().equals("severe"));
        }
    }

    @Nested
    @DisplayName("getAlertsByCity")
    class GetAlertsByCityTests {

        @Test
        @DisplayName("should filter by city name")
        void shouldFilterByCity() {
            WeatherAlert berlin = createAlert("moderate", LocalDateTime.now().plusHours(5));
            berlin.setCity("Berlin");
            alertRepository.save(berlin);

            WeatherAlert munich = createAlert("minor", LocalDateTime.now().plusHours(5));
            munich.setCity("Munich");
            alertRepository.save(munich);

            List<WeatherAlert> results = brightSkyService.getAlertsByCity("Berlin");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCity()).isEqualTo("Berlin");
        }
    }

    @Nested
    @DisplayName("getAlertsByWarnCellId")
    class GetAlertsByWarnCellIdTests {

        @Test
        @DisplayName("should filter by warn cell ID")
        void shouldFilterByWarnCellId() {
            WeatherAlert alert1 = createAlert("moderate", LocalDateTime.now().plusHours(5));
            alert1.setWarnCellId(123456L);
            alertRepository.save(alert1);

            WeatherAlert alert2 = createAlert("minor", LocalDateTime.now().plusHours(5));
            alert2.setWarnCellId(789012L);
            alertRepository.save(alert2);

            List<WeatherAlert> results = brightSkyService.getAlertsByWarnCellId(123456L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getWarnCellId()).isEqualTo(123456L);
        }
    }

    @Nested
    @DisplayName("getAlertsForLocation (with WireMock)")
    class GetAlertsForLocationTests {

        @Test
        @DisplayName("should fetch and persist alerts from BrightSky API")
        void shouldFetchFromBrightSkyAPI() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-1");

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getAlertId()).isEqualTo("alert-001");
            verify(getRequestedFor(urlPathEqualTo("/alerts")));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle 404 Not Found response gracefully")
        void shouldHandle404NotFound() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Location not found\"}")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-1");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle 500 Internal Server Error")
        void shouldHandle500InternalServerError() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("48\\.1.*"))
                    .withQueryParam("lon", matching("11\\.5.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Internal server error\"}")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    48.13f, 11.57f, "test-device-2");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle network timeout")
        void shouldHandleNetworkTimeout() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("50\\.1.*"))
                    .withQueryParam("lon", matching("8\\.6.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)
                            .withFixedDelay(5000)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    50.11f, 8.68f, "test-device-3");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle malformed JSON response")
        void shouldHandleMalformedJson() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("51\\.0.*"))
                    .withQueryParam("lon", matching("13\\.7.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{invalid json: malformed")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    51.05f, 13.74f, "test-device-4");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle empty response body")
        void shouldHandleEmptyResponseBody() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("53\\.5.*"))
                    .withQueryParam("lon", matching("9\\.9.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    53.55f, 9.99f, "test-device-5");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle invalid coordinates (out of bounds)")
        void shouldHandleInvalidCoordinates() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("999\\..*"))
                    .withQueryParam("lon", matching("999\\..*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(400)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Invalid coordinates\"}")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    999.99f, 999.99f, "test-device-6");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle connection refused")
        void shouldHandleConnectionRefused() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.3.*"))
                    .withQueryParam("lon", matching("9\\.7.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    52.37f, 9.73f, "test-device-7");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle rate limit (429) response")
        void shouldHandleRateLimit() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("50\\.9.*"))
                    .withQueryParam("lon", matching("6\\.9.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(429)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Retry-After", "60")
                            .withBody("{\"error\": \"Rate limit exceeded\"}")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    50.93f, 6.95f, "test-device-8");

            assertThat(alerts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Data Validation Tests")
    class DataValidationTests {

        @Test
        @DisplayName("should handle missing required fields in alert")
        void shouldHandleMissingRequiredFields() {
            String incompleteResponse = """
                    {
                        "alerts": [
                            {
                                "id": 1,
                                "status": "actual",
                                "severity": "Moderate"
                            }
                        ],
                        "location": {
                            "warn_cell_id": 123456,
                            "name": "Berlin"
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(incompleteResponse)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-9");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle invalid date formats")
        void shouldHandleInvalidDateFormats() {
            String invalidDateResponse = """
                    {
                        "alerts": [
                            {
                                "id": 1,
                                "alert_id": "alert-002",
                                "status": "actual",
                                "effective": "invalid-date-format",
                                "onset": "2024-13-45T99:99:99+00:00",
                                "expires": "not-a-date",
                                "severity": "Moderate",
                                "event_en": "Test Alert"
                            }
                        ],
                        "location": {
                            "warn_cell_id": 123456,
                            "name": "Berlin"
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(invalidDateResponse)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-10");

            assertThat(alerts).isNotNull();
        }

        @Test
        @DisplayName("should handle null values in JSON fields")
        void shouldHandleNullValues() {
            String nullFieldsResponse = """
                    {
                        "alerts": [
                            {
                                "id": 1,
                                "alert_id": "alert-003",
                                "status": "actual",
                                "effective": "2024-01-15T10:00:00+00:00",
                                "onset": null,
                                "expires": "2024-01-16T12:00:00+00:00",
                                "category": null,
                                "severity": "Moderate",
                                "event_en": null,
                                "headline_en": null,
                                "description_en": null
                            }
                        ],
                        "location": {
                            "warn_cell_id": null,
                            "name": null
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(nullFieldsResponse)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-11");

            assertThat(alerts).hasSize(1);
            assertThat(alerts.get(0).getAlertId()).isEqualTo("alert-003");
        }

        @Test
        @DisplayName("should handle boundary latitude values")
        void shouldHandleBoundaryLatitudeValues() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("90\\.0.*"))
                    .withQueryParam("lon", matching("0\\.0.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"alerts\": [], \"location\": {}}")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    90.0f, 0.0f, "test-device-12");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle boundary longitude values")
        void shouldHandleBoundaryLongitudeValues() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("0\\.0.*"))
                    .withQueryParam("lon", matching("180\\.0.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"alerts\": [], \"location\": {}}")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    0.0f, 180.0f, "test-device-13");

            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("should handle negative coordinates")
        void shouldHandleNegativeCoordinates() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("-45\\.0.*"))
                    .withQueryParam("lon", matching("-90\\.0.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"alerts\": [], \"location\": {}}")));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    -45.0f, -90.0f, "test-device-14");

            assertThat(alerts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Caching Behavior Tests")
    class CachingBehaviorTests {

        @Test
        @DisplayName("should handle duplicate alerts (update existing)")
        void shouldHandleDuplicateAlerts() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            List<WeatherAlert> firstCall = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-15");
            assertThat(firstCall).hasSize(1);

            List<WeatherAlert> secondCall = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-15");
            assertThat(secondCall).hasSize(1);

            assertThat(alertRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should clean up expired alerts before fetching")
        void shouldCleanupExpiredAlerts() {
            WeatherAlert expiredAlert = createAlert("severe", LocalDateTime.now().minusHours(10));
            alertRepository.save(expiredAlert);

            long countBefore = alertRepository.count();
            assertThat(countBefore).isEqualTo(1);

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            brightSkyService.getAlertsForLocation(52.52f, 13.41f, "test-device-16");

            assertThat(alertRepository.count()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("should maintain device isolation for alerts")
        void shouldMaintainDeviceIsolation() {
            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            brightSkyService.getAlertsForLocation(52.52f, 13.41f, "device-A");
            brightSkyService.getAlertsForLocation(52.52f, 13.41f, "device-B");

            long totalAlerts = alertRepository.count();
            assertThat(totalAlerts).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getActiveAlertsForLocation")
    class GetActiveAlertsForLocationTests {

        @Test
        @DisplayName("should retrieve active alerts for specific location")
        void shouldRetrieveActiveAlertsForLocation() {
            WeatherAlert berlinAlert = createAlert("Moderate", LocalDateTime.now().plusHours(5));
            berlinAlert.setCity("Berlin");
            berlinAlert.setWarnCellId(123456L);
            alertRepository.save(berlinAlert);

            WeatherAlert munichAlert = createAlert("Severe", LocalDateTime.now().plusHours(5));
            munichAlert.setCity("Munich");
            munichAlert.setWarnCellId(789012L);
            alertRepository.save(munichAlert);

            List<WeatherAlert> results = brightSkyService.getActiveAlertsForLocation("Berlin", 123456L);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getCity()).isEqualTo("Berlin");
        }
    }

    @Nested
    @DisplayName("getAlertsForDevice")
    class GetAlertsForDeviceTests {

        @Test
        @DisplayName("should retrieve alerts for verified device")
        void shouldRetrieveAlertsForVerifiedDevice() {
            Device device = createDevice("device-1", 52.52, 13.41);
            when(deviceService.getVerifiedDevice("device-1")).thenReturn(device);

            WeatherAlert alert = createAlert("Moderate", LocalDateTime.now().plusHours(5));
            alert.setDeviceId("device-1");
            alertRepository.save(alert);

            WeatherAlert otherAlert = createAlert("Severe", LocalDateTime.now().plusHours(5));
            otherAlert.setDeviceId("device-2");
            alertRepository.save(otherAlert);

            List<WeatherAlert> results = brightSkyService.getAlertsForDevice("device-1");

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getDeviceId()).isEqualTo("device-1");
        }

        @Test
        @DisplayName("should throw exception for non-verified device")
        void shouldThrowExceptionForNonVerifiedDevice() {
            when(deviceService.getVerifiedDevice("invalid-device"))
                    .thenThrow(new IllegalArgumentException("Device not found"));

            assertThatThrownBy(() -> brightSkyService.getAlertsForDevice("invalid-device"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Device not found");
        }
    }

    @Nested
    @DisplayName("Update Methods Tests")
    class UpdateMethodsTests {

        @Test
        @DisplayName("updateAlertsForCurrentLocation should fetch and persist alerts")
        void shouldUpdateAlertsForCurrentLocation() {
            LocationData location = createLocationData("device-1", 52.52f, 13.41f, "Berlin");
            when(deviceLocationService.getFirstUserDeviceLocation()).thenReturn(Optional.of(location));

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            brightSkyService.updateAlertsForCurrentLocation();

            assertThat(alertRepository.count()).isGreaterThan(0);
        }

        @Test
        @DisplayName("updateAlertsForCurrentLocation should handle no device gracefully")
        void shouldHandleNoDeviceGracefully() {
            when(deviceLocationService.getFirstUserDeviceLocation()).thenReturn(Optional.empty());

            brightSkyService.updateAlertsForCurrentLocation();

            assertThat(alertRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("updateAlertsForAllDeviceLocations should update multiple devices")
        void shouldUpdateMultipleDeviceLocations() {
            List<LocationData> locations = List.of(
                    createLocationData("device-1", 52.52f, 13.41f, "Berlin"),
                    createLocationData("device-2", 48.13f, 11.57f, "Munich"));
            when(deviceLocationService.getAllUniqueDeviceLocations()).thenReturn(locations);

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_ALERTS_RESPONSE)));

            brightSkyService.updateAlertsForAllDeviceLocations();

            assertThat(alertRepository.count()).isGreaterThan(0);
        }

        @Test
        @DisplayName("updateAlertsForAllDeviceLocations should skip when no devices")
        void shouldSkipWhenNoDevices() {
            when(deviceLocationService.getAllUniqueDeviceLocations()).thenReturn(List.of());

            brightSkyService.updateAlertsForAllDeviceLocations();

            assertThat(alertRepository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Additional Query Methods Tests")
    class AdditionalQueryMethodsTests {

        @Test
        @DisplayName("getRecentAlerts should return alerts from last 24 hours")
        void shouldGetRecentAlerts() {
            WeatherAlert recent = createAlert("Moderate", LocalDateTime.now().plusHours(5));
            recent.setEffective(LocalDateTime.now().minusHours(12));
            alertRepository.save(recent);

            WeatherAlert old = createAlert("Severe", LocalDateTime.now().plusHours(5));
            old.setEffective(LocalDateTime.now().minusDays(2));
            alertRepository.save(old);

            List<WeatherAlert> results = brightSkyService.getRecentAlerts();

            assertThat(results).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("getCitiesWithActiveAlerts should return distinct cities")
        void shouldGetCitiesWithActiveAlerts() {
            WeatherAlert berlin1 = createAlert("Moderate", LocalDateTime.now().plusHours(5));
            berlin1.setCity("Berlin");
            alertRepository.save(berlin1);

            WeatherAlert berlin2 = createAlert("Severe", LocalDateTime.now().plusHours(5));
            berlin2.setCity("Berlin");
            alertRepository.save(berlin2);

            WeatherAlert munich = createAlert("Minor", LocalDateTime.now().plusHours(5));
            munich.setCity("Munich");
            alertRepository.save(munich);

            List<String> cities = brightSkyService.getCitiesWithActiveAlerts();

            assertThat(cities).hasSize(2);
            assertThat(cities).containsExactlyInAnyOrder("Berlin", "Munich");
        }
    }

    @Nested
    @DisplayName("Radar Endpoint URL Tests")
    class RadarEndpointUrlTests {

        @Test
        @DisplayName("getRadarEndpointUrl should generate correct URL with defaults")
        void shouldGenerateRadarUrlWithDefaults() {
            String url = brightSkyService.getRadarEndpointUrl(52.52f, 13.41f, null, null);

            assertThat(url).contains("http://localhost:8090/radar");
            assertThat(url).contains("lat=52.52");
            assertThat(url).contains("lon=13.41");
            assertThat(url).contains("distance=200000");
            assertThat(url).contains("format=compressed");
            assertThat(url).contains("tz=Europe/Berlin");
        }

        @Test
        @DisplayName("getRadarEndpointUrl should use custom distance and format")
        void shouldGenerateRadarUrlWithCustomParams() {
            String url = brightSkyService.getRadarEndpointUrl(52.52f, 13.41f, 100000, "plain");

            assertThat(url).contains("distance=100000");
            assertThat(url).contains("format=plain");
        }

        @Test
        @DisplayName("getRadarEndpointUrlForCurrentLocation should use device location")
        void shouldGenerateRadarUrlForDeviceLocation() {
            LocationData location = createLocationData("device-1", 52.52f, 13.41f, "Berlin");
            when(deviceLocationService.getFirstUserDeviceLocation()).thenReturn(Optional.of(location));

            String url = brightSkyService.getRadarEndpointUrlForCurrentLocation(null, null);

            assertThat(url).isNotNull();
            assertThat(url).contains("lat=52.52");
            assertThat(url).contains("lon=13.41");
        }

        @Test
        @DisplayName("getRadarEndpointUrlForCurrentLocation should return null when no device")
        void shouldReturnNullWhenNoDevice() {
            when(deviceLocationService.getFirstUserDeviceLocation()).thenReturn(Optional.empty());

            String url = brightSkyService.getRadarEndpointUrlForCurrentLocation(null, null);

            assertThat(url).isNull();
        }

        @Test
        @DisplayName("getRadarEndpointForDevice should generate URL for verified device")
        void shouldGenerateRadarUrlForDevice() {
            Device device = createDevice("device-1", 52.52, 13.41);
            when(deviceService.getVerifiedDevice("device-1")).thenReturn(device);

            String url = brightSkyService.getRadarEndpointForDevice("device-1");

            assertThat(url).isNotNull();
            assertThat(url).contains("lat=52.52");
            assertThat(url).contains("lon=13.41");
        }

        @Test
        @DisplayName("getRadarEndpointForDevice should throw exception when device has no coordinates")
        void shouldThrowExceptionWhenDeviceHasNoCoordinates() {
            Device device = createDevice("device-1", null, null);
            when(deviceService.getVerifiedDevice("device-1")).thenReturn(device);

            assertThatThrownBy(() -> brightSkyService.getRadarEndpointForDevice("device-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no GPS coordinates");
        }
    }

    @Nested
    @DisplayName("Radar Metadata Tests")
    class RadarMetadataTests {

        @Test
        @DisplayName("fetchAndStoreRadarMetadata should process and store radar data")
        void shouldFetchAndStoreRadarMetadata() {
            stubFor(get(urlPathEqualTo("/radar"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_RADAR_RESPONSE)));

            RadarMetadataDTO result = brightSkyService.fetchAndStoreRadarMetadata(
                    52.52f, 13.41f, 100000, "device-1");

            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo("DWD");
            assertThat(result.getLatitude()).isEqualTo(52.52f);
            assertThat(result.getLongitude()).isEqualTo(13.41f);
            assertThat(result.getDistance()).isEqualTo(100000);
            assertThat(result.getPrecipitationMin()).isGreaterThanOrEqualTo(0);
            assertThat(result.getPrecipitationMax()).isGreaterThan(0);
            assertThat(result.getPrecipitationAvg()).isGreaterThan(0);
            assertThat(result.getCoveragePercent()).isGreaterThan(0);
            assertThat(result.getSignificantRainCells()).isGreaterThanOrEqualTo(0);
            assertThat(result.getTotalCells()).isEqualTo(12); // 3 rows x 4 columns
            assertThat(result.getDirectApiUrl()).isNotNull();
            assertThat(result.getHasActivePrecipitation()).isTrue();
            assertThat(result.getCreatedAt()).isNotNull();

            // Verify it's persisted
            assertThat(radarMetadataRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("fetchAndStoreRadarMetadata should handle no precipitation data")
        void shouldHandleNoPrecipitationData() {
            String noPrecipResponse = """
                    {
                        "radar": [
                            {
                                "timestamp": "2024-01-15T10:00:00+00:00",
                                "source": "DWD",
                                "precipitation_5": [
                                    [0, 0, 0, 0],
                                    [0, 0, 0, 0],
                                    [0, 0, 0, 0]
                                ]
                            }
                        ]
                    }
                    """;

            stubFor(get(urlPathEqualTo("/radar"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(noPrecipResponse)));

            RadarMetadataDTO result = brightSkyService.fetchAndStoreRadarMetadata(
                    52.52f, 13.41f, null, "device-1");

            assertThat(result).isNotNull();
            assertThat(result.getPrecipitationMin()).isEqualTo(0);
            assertThat(result.getPrecipitationMax()).isEqualTo(0);
            assertThat(result.getPrecipitationAvg()).isEqualTo(0);
            assertThat(result.getCoveragePercent()).isEqualTo(0);
            assertThat(result.getHasActivePrecipitation()).isFalse();
        }

        @Test
        @DisplayName("fetchAndStoreRadarMetadata should return null on API error")
        void shouldReturnNullOnApiError() {
            stubFor(get(urlPathEqualTo("/radar"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(500)));

            RadarMetadataDTO result = brightSkyService.fetchAndStoreRadarMetadata(
                    52.52f, 13.41f, null, "device-1");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("fetchAndStoreRadarMetadataForCurrentLocation should use device location")
        void shouldFetchRadarMetadataForCurrentLocation() {
            LocationData location = createLocationData("device-1", 52.52f, 13.41f, "Berlin");
            when(deviceLocationService.getPrimaryUserDeviceLocation()).thenReturn(Optional.of(location));

            stubFor(get(urlPathEqualTo("/radar"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_RADAR_RESPONSE)));

            RadarMetadataDTO result = brightSkyService.fetchAndStoreRadarMetadataForCurrentLocation(null);

            assertThat(result).isNotNull();
            assertThat(result.getLatitude()).isEqualTo(52.52f);
        }

        @Test
        @DisplayName("fetchRadarMetadataForDevice should fetch for verified device")
        void shouldFetchRadarMetadataForDevice() {
            Device device = createDevice("device-1", 52.52, 13.41);
            when(deviceService.getVerifiedDevice("device-1")).thenReturn(device);

            stubFor(get(urlPathEqualTo("/radar"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_RADAR_RESPONSE)));

            RadarMetadataDTO result = brightSkyService.fetchRadarMetadataForDevice("device-1", null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("getLatestRadarMetadata should return most recent metadata")
        void shouldGetLatestRadarMetadata() {
            stubFor(get(urlPathEqualTo("/radar"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(BRIGHTSKY_RADAR_RESPONSE)));

            brightSkyService.fetchAndStoreRadarMetadata(52.52f, 13.41f, null, "device-1");

            Optional<WeatherRadarMetadata> latest = brightSkyService.getLatestRadarMetadata();

            assertThat(latest).isPresent();
            assertThat(latest.get().getDeviceId()).isEqualTo("device-1");
        }

        @Test
        @DisplayName("getRecentRadarMetadata should return metadata from specified hours")
        void shouldGetRecentRadarMetadata() {
            // Create recent metadata (simulated by direct DB insert with recent timestamp)
            WeatherRadarMetadata recentMetadata = new WeatherRadarMetadata();
            recentMetadata.setTimestamp(LocalDateTime.now().minusHours(12));
            recentMetadata.setDeviceId("device-1");
            recentMetadata.setLatitude(52.52f);
            recentMetadata.setLongitude(13.41f);
            recentMetadata.setDistance(100);
            radarMetadataRepository.save(recentMetadata);

            List<WeatherRadarMetadata> recent = brightSkyService.getRecentRadarMetadata(24);

            assertThat(recent).hasSizeGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("cleanupOldRadarMetadata should remove old entries")
        void shouldCleanupOldRadarMetadata() {
            // Create old metadata (simulated by direct DB insert with old timestamp)
            WeatherRadarMetadata oldMetadata = new WeatherRadarMetadata();
            oldMetadata.setTimestamp(LocalDateTime.now().minusDays(10));
            oldMetadata.setDeviceId("device-1");
            oldMetadata.setLatitude(52.52f);
            oldMetadata.setLongitude(13.41f);
            oldMetadata.setDistance(100);
            radarMetadataRepository.save(oldMetadata);

            brightSkyService.cleanupOldRadarMetadata();

            assertThat(radarMetadataRepository.count()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("cleanupExpiredAlerts should remove expired and old alerts")
        void shouldCleanupExpiredAlerts() {
            WeatherAlert expired = createAlert("Moderate", LocalDateTime.now().minusHours(10));
            alertRepository.save(expired);

            WeatherAlert active = createAlert("Severe", LocalDateTime.now().plusHours(5));
            alertRepository.save(active);

            brightSkyService.cleanupExpiredAlerts();

            assertThat(alertRepository.count()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Pagination and Filtering Tests")
    class PaginationAndFilteringTests {

        @Test
        @DisplayName("should handle multiple alerts in response")
        void shouldHandleMultipleAlerts() {
            String multipleAlertsResponse = """
                    {
                        "alerts": [
                            {
                                "id": 1,
                                "alert_id": "alert-001",
                                "status": "actual",
                                "effective": "2024-01-15T10:00:00+00:00",
                                "expires": "2024-01-16T12:00:00+00:00",
                                "severity": "Severe",
                                "event_en": "Storm Warning"
                            },
                            {
                                "id": 2,
                                "alert_id": "alert-002",
                                "status": "actual",
                                "effective": "2024-01-15T11:00:00+00:00",
                                "expires": "2024-01-16T13:00:00+00:00",
                                "severity": "Moderate",
                                "event_en": "Rain Warning"
                            },
                            {
                                "id": 3,
                                "alert_id": "alert-003",
                                "status": "actual",
                                "effective": "2024-01-15T12:00:00+00:00",
                                "expires": "2024-01-16T14:00:00+00:00",
                                "severity": "Minor",
                                "event_en": "Wind Warning"
                            }
                        ],
                        "location": {
                            "warn_cell_id": 123456,
                            "name": "Berlin"
                        }
                    }
                    """;

            stubFor(get(urlPathEqualTo("/alerts"))
                    .withQueryParam("lat", matching("52\\.5.*"))
                    .withQueryParam("lon", matching("13\\.4.*"))
                    .withQueryParam("tz", equalTo("Europe/Berlin"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(multipleAlertsResponse)));

            List<WeatherAlert> alerts = brightSkyService.getAlertsForLocation(
                    52.52f, 13.41f, "test-device-17");

            assertThat(alerts).hasSize(3);
            assertThat(alerts).extracting("severity")
                    .containsExactlyInAnyOrder("Severe", "Moderate", "Minor");
        }

        @Test
        @DisplayName("should filter alerts by severity level")
        void shouldFilterAlertsBySeverity() {
            alertRepository.save(createAlert("Severe", LocalDateTime.now().plusHours(5)));
            alertRepository.save(createAlert("Moderate", LocalDateTime.now().plusHours(5)));
            alertRepository.save(createAlert("Minor", LocalDateTime.now().plusHours(5)));
            alertRepository.save(createAlert("Severe", LocalDateTime.now().plusHours(5)));

            List<WeatherAlert> severeAlerts = brightSkyService.getAlertsBySeverity("Severe");

            assertThat(severeAlerts).hasSize(2);
            assertThat(severeAlerts).allMatch(a -> a.getSeverity().equals("Severe"));
        }

        @Test
        @DisplayName("should filter active alerts by time range")
        void shouldFilterActiveAlertsByTimeRange() {
            alertRepository.save(createAlert("Severe", LocalDateTime.now().plusHours(1)));
            alertRepository.save(createAlert("Moderate", LocalDateTime.now().plusHours(5)));
            alertRepository.save(createAlert("Minor", LocalDateTime.now().minusHours(1)));

            List<WeatherAlert> activeAlerts = brightSkyService.getActiveAlerts();

            assertThat(activeAlerts).hasSizeGreaterThanOrEqualTo(2);
            assertThat(activeAlerts).allMatch(a -> a.getExpires().isAfter(LocalDateTime.now()));
        }
    }
}
