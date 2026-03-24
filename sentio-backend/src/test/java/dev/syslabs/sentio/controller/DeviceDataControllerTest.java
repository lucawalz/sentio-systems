package dev.syslabs.sentio.controller;

import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.model.RaspiWeatherData;
import dev.syslabs.sentio.model.WeatherAlert;
import dev.syslabs.sentio.model.WeatherForecast;
import dev.syslabs.sentio.service.AnimalDetectionQueryService;
import dev.syslabs.sentio.service.BrightSkyService;
import dev.syslabs.sentio.service.RaspiWeatherDataService;
import dev.syslabs.sentio.service.WeatherForecastService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DeviceDataController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(DeviceDataControllerTest.TestBeans.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
@DisplayName("DeviceDataController")
class DeviceDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeatherForecastService forecastService;

    @Autowired
    private BrightSkyService alertService;

    @Autowired
    private RaspiWeatherDataService sensorService;

    @Autowired
    private AnimalDetectionQueryService sightingsService;

    @TestConfiguration
    static class TestBeans {
        @Bean
        WeatherForecastService weatherForecastService() {
            return mock(WeatherForecastService.class);
        }

        @Bean
        BrightSkyService brightSkyService() {
            return mock(BrightSkyService.class);
        }

        @Bean
        RaspiWeatherDataService raspiWeatherDataService() {
            return mock(RaspiWeatherDataService.class);
        }

        @Bean
        AnimalDetectionQueryService animalDetectionQueryService() {
            return mock(AnimalDetectionQueryService.class);
        }
    }

    @AfterEach
    void resetMocks() {
        reset(forecastService, alertService, sensorService, sightingsService);
    }

    @Test
    @DisplayName("GET /forecasts should return 200 with forecasts when device exists")
    void getDeviceForecasts_withValidDevice_returns200() throws Exception {
        // Arrange
        String deviceId = "device-123";
        WeatherForecast forecast1 = new WeatherForecast();
        forecast1.setId(1L);
        forecast1.setTemperature(22.5F);
        forecast1.setPrecipitationProbability(30F);

        WeatherForecast forecast2 = new WeatherForecast();
        forecast2.setId(2L);
        forecast2.setTemperature(21.0F);
        forecast2.setPrecipitationProbability(40F);

        when(forecastService.getForecastForDevice(deviceId))
                .thenReturn(Arrays.asList(forecast1, forecast2));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/forecasts", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].temperature", is(22.5)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].temperature", is(21.0)));

        verify(forecastService).getForecastForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /forecasts should return 404 when device not found")
    void getDeviceForecasts_withInvalidDevice_returns404() throws Exception {
        // Arrange
        String deviceId = "nonexistent-device";
        when(forecastService.getForecastForDevice(deviceId))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/forecasts", deviceId))
                .andExpect(status().isNotFound());

        verify(forecastService).getForecastForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /forecasts should return 200 with empty list when no forecasts")
    void getDeviceForecasts_withNoForecasts_returns200WithEmptyList() throws Exception {
        // Arrange
        String deviceId = "device-123";
        when(forecastService.getForecastForDevice(deviceId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/forecasts", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(forecastService).getForecastForDevice(deviceId);
    }

    // ==================== GET /api/devices/{deviceId}/alerts ====================

    @Test
    @DisplayName("GET /alerts should return 200 with alerts when device exists")
    void getDeviceAlerts_withValidDevice_returns200() throws Exception {
        // Arrange
        String deviceId = "device-123";
        WeatherAlert alert1 = new WeatherAlert();
        alert1.setId(1L);
        alert1.setEventEn("THUNDERSTORM");
        alert1.setSeverity("WARNING");
        alert1.setHeadlineEn("Thunderstorm Warning");
        alert1.setDescriptionEn("Severe thunderstorm expected");

        WeatherAlert alert2 = new WeatherAlert();
        alert2.setId(2L);
        alert2.setEventEn("HEAVY_RAIN");
        alert2.setSeverity("WATCH");
        alert2.setHeadlineEn("Heavy Rain Watch");
        alert2.setDescriptionEn("Heavy rainfall expected");

        when(alertService.getAlertsForDevice(deviceId))
                .thenReturn(Arrays.asList(alert1, alert2));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/alerts", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].eventEn", is("THUNDERSTORM")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].eventEn", is("HEAVY_RAIN")));

        verify(alertService).getAlertsForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /alerts should return 404 when device not found")
    void getDeviceAlerts_withInvalidDevice_returns404() throws Exception {
        // Arrange
        String deviceId = "nonexistent-device";
        when(alertService.getAlertsForDevice(deviceId))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/alerts", deviceId))
                .andExpect(status().isNotFound());

        verify(alertService).getAlertsForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /alerts should return 200 with empty list when no alerts")
    void getDeviceAlerts_withNoAlerts_returns200WithEmptyList() throws Exception {
        // Arrange
        String deviceId = "device-123";
        when(alertService.getAlertsForDevice(deviceId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/alerts", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(alertService).getAlertsForDevice(deviceId);
    }

    // ==================== GET /api/devices/{deviceId}/sensors/latest ====================

    @Test
    @DisplayName("GET /sensors/latest should return 200 with data when available")
    void getDeviceSensorLatest_withData_returns200() throws Exception {
        // Arrange
        String deviceId = "device-123";
        RaspiWeatherData data = new RaspiWeatherData();
        data.setId(1L);
        data.setTemperature(23.5F);
        data.setHumidity(65.0F);
        data.setPressure(1013.25F);
        data.setTimestamp(LocalDateTime.now());

        when(sensorService.getLatestForDevice(deviceId)).thenReturn(data);

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sensors/latest", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.temperature", is(23.5)))
                .andExpect(jsonPath("$.humidity", is(65.0)))
                .andExpect(jsonPath("$.pressure", is(1013.25)));

        verify(sensorService).getLatestForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /sensors/latest should return 200 with message when no data")
    void getDeviceSensorLatest_withNoData_returns200WithMessage() throws Exception {
        // Arrange
        String deviceId = "device-123";
        when(sensorService.getLatestForDevice(deviceId)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sensors/latest", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("No sensor data available for device")));

        verify(sensorService).getLatestForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /sensors/latest should return 404 when device not found")
    void getDeviceSensorLatest_withInvalidDevice_returns404() throws Exception {
        // Arrange
        String deviceId = "nonexistent-device";
        when(sensorService.getLatestForDevice(deviceId))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sensors/latest", deviceId))
                .andExpect(status().isNotFound());

        verify(sensorService).getLatestForDevice(deviceId);
    }

    // ==================== GET /api/devices/{deviceId}/sensors ====================

    @Test
    @DisplayName("GET /sensors should return 200 with sensor data list")
    void getDeviceSensors_withValidDevice_returns200() throws Exception {
        // Arrange
        String deviceId = "device-123";
        RaspiWeatherData data1 = new RaspiWeatherData();
        data1.setId(1L);
        data1.setTemperature(23.5F);
        data1.setHumidity(65.0F);
        data1.setTimestamp(LocalDateTime.now());

        RaspiWeatherData data2 = new RaspiWeatherData();
        data2.setId(2L);
        data2.setTemperature(22.8F);
        data2.setHumidity(68.0F);
        data2.setTimestamp(LocalDateTime.now().minusHours(1));

        when(sensorService.getRecentForDevice(deviceId))
                .thenReturn(Arrays.asList(data1, data2));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sensors", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].temperature", is(23.5)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].temperature", is(22.8)));

        verify(sensorService).getRecentForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /sensors should return 404 when device not found")
    void getDeviceSensors_withInvalidDevice_returns404() throws Exception {
        // Arrange
        String deviceId = "nonexistent-device";
        when(sensorService.getRecentForDevice(deviceId))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sensors", deviceId))
                .andExpect(status().isNotFound());

        verify(sensorService).getRecentForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /sensors should return 200 with empty list when no data")
    void getDeviceSensors_withNoData_returns200WithEmptyList() throws Exception {
        // Arrange
        String deviceId = "device-123";
        when(sensorService.getRecentForDevice(deviceId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sensors", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(sensorService).getRecentForDevice(deviceId);
    }

    // ==================== GET /api/devices/{deviceId}/radar ====================

    @Test
    @DisplayName("GET /radar should return 200 with radar endpoint")
    void getDeviceRadar_withValidDevice_returns200() throws Exception {
        // Arrange
        String deviceId = "device-123";
        String radarUrl = "https://api.brightsky.dev/radar?lat=52.52&lon=13.41";

        when(alertService.getRadarEndpointForDevice(deviceId)).thenReturn(radarUrl);

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/radar", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radarEndpoint", is(radarUrl)))
                .andExpect(jsonPath("$.deviceId", is(deviceId)));

        verify(alertService).getRadarEndpointForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /radar should return 404 when device not found")
    void getDeviceRadar_withInvalidDevice_returns404() throws Exception {
        // Arrange
        String deviceId = "nonexistent-device";
        when(alertService.getRadarEndpointForDevice(deviceId))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/radar", deviceId))
                .andExpect(status().isNotFound());

        verify(alertService).getRadarEndpointForDevice(deviceId);
    }

    @Test
    @DisplayName("GET /radar should return 404 when device has no GPS")
    void getDeviceRadar_withNoGps_returns404() throws Exception {
        // Arrange
        String deviceId = "device-no-gps";
        when(alertService.getRadarEndpointForDevice(deviceId))
                .thenThrow(new IllegalArgumentException("Device has no GPS coordinates"));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/radar", deviceId))
                .andExpect(status().isNotFound());

        verify(alertService).getRadarEndpointForDevice(deviceId);
    }

    // ==================== GET /api/devices/{deviceId}/sightings ====================

    @Test
    @DisplayName("GET /sightings should return 200 with sightings using default limit")
    void getDeviceSightings_withDefaultLimit_returns200() throws Exception {
        // Arrange
        String deviceId = "device-123";
        AnimalDetection detection1 = new AnimalDetection();
        detection1.setId(1L);
        detection1.setSpecies("fox");
        detection1.setConfidence(0.95f);
        detection1.setTimestamp(LocalDateTime.now());

        AnimalDetection detection2 = new AnimalDetection();
        detection2.setId(2L);
        detection2.setSpecies("deer");
        detection2.setConfidence(0.88f);
        detection2.setTimestamp(LocalDateTime.now().minusHours(2));

        when(sightingsService.getDetectionsByDevice(eq(deviceId), any(PageRequest.class)))
                .thenReturn(Arrays.asList(detection1, detection2));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sightings", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].species", is("fox")))
                .andExpect(jsonPath("$[0].confidence", is(0.95)))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].species", is("deer")));

        verify(sightingsService).getDetectionsByDevice(eq(deviceId), any(PageRequest.class));
    }

    @Test
    @DisplayName("GET /sightings should return 200 with sightings using custom limit")
    void getDeviceSightings_withCustomLimit_returns200() throws Exception {
        // Arrange
        String deviceId = "device-123";
        int customLimit = 5;
        AnimalDetection detection = new AnimalDetection();
        detection.setId(1L);
        detection.setSpecies("fox");
        detection.setConfidence(0.95f);
        List<AnimalDetection> detections = Collections.nCopies(customLimit, detection);

        when(sightingsService.getDetectionsByDevice(eq(deviceId), any(PageRequest.class)))
                .thenReturn(detections);

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sightings", deviceId)
                        .param("limit", String.valueOf(customLimit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(customLimit)));

        verify(sightingsService).getDetectionsByDevice(eq(deviceId), any(PageRequest.class));
    }

    @Test
    @DisplayName("GET /sightings should return 404 when device not found")
    void getDeviceSightings_withInvalidDevice_returns404() throws Exception {
        // Arrange
        String deviceId = "nonexistent-device";
        when(sightingsService.getDetectionsByDevice(eq(deviceId), any(PageRequest.class)))
                .thenThrow(new IllegalArgumentException("Device not found"));

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sightings", deviceId))
                .andExpect(status().isNotFound());

        verify(sightingsService).getDetectionsByDevice(eq(deviceId), any(PageRequest.class));
    }

    @Test
    @DisplayName("GET /sightings should return 200 with empty list when no sightings")
    void getDeviceSightings_withNoSightings_returns200WithEmptyList() throws Exception {
        // Arrange
        String deviceId = "device-123";
        when(sightingsService.getDetectionsByDevice(eq(deviceId), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/devices/{deviceId}/sightings", deviceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(sightingsService).getDetectionsByDevice(eq(deviceId), any(PageRequest.class));
    }
}
