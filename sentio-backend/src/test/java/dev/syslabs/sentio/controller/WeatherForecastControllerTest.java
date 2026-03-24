package dev.syslabs.sentio.controller;

import dev.syslabs.sentio.dto.WeatherForecastDTO;
import dev.syslabs.sentio.mapper.WeatherForecastMapper;
import dev.syslabs.sentio.model.WeatherForecast;
import dev.syslabs.sentio.service.IWeatherForecastService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(controllers = WeatherForecastController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(WeatherForecastControllerTest.TestBeans.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class WeatherForecastControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired IWeatherForecastService weatherForecastService;
    @Autowired WeatherForecastMapper weatherForecastMapper;

    @TestConfiguration
    static class TestBeans {
        @Bean IWeatherForecastService weatherForecastService() { return mock(IWeatherForecastService.class); }
        @Bean WeatherForecastMapper weatherForecastMapper() { return mock(WeatherForecastMapper.class); }
    }

    @AfterEach
    void resetMocks() {
        reset(weatherForecastService, weatherForecastMapper);
    }

    @Test
    void getCurrentLocationForecast_returns200() throws Exception {
        List<WeatherForecast> forecasts = List.of(new WeatherForecast(), new WeatherForecast());
        List<WeatherForecastDTO> dtos = List.of(new WeatherForecastDTO(), new WeatherForecastDTO());

        when(weatherForecastService.getForecastForCurrentLocation()).thenReturn(forecasts);
        when(weatherForecastMapper.toDTOList(forecasts)).thenReturn(dtos);

        mockMvc.perform(get("/api/forecast/current-location"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(weatherForecastService).getForecastForCurrentLocation();
        verify(weatherForecastMapper).toDTOList(forecasts);
        verifyNoMoreInteractions(weatherForecastService, weatherForecastMapper);
    }

    @Test
    void getUpcomingForecasts_returns200() throws Exception {
        List<WeatherForecast> forecasts = List.of(new WeatherForecast());
        List<WeatherForecastDTO> dtos = List.of(new WeatherForecastDTO());

        when(weatherForecastService.getUpcomingForecasts()).thenReturn(forecasts);
        when(weatherForecastMapper.toDTOList(forecasts)).thenReturn(dtos);

        mockMvc.perform(get("/api/forecast/upcoming"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(weatherForecastService).getUpcomingForecasts();
        verify(weatherForecastMapper).toDTOList(forecasts);
        verifyNoMoreInteractions(weatherForecastService, weatherForecastMapper);
    }

    @Test
    void getForecastsForDateRange_returns200() throws Exception {
        LocalDate start = LocalDate.of(2025, 12, 1);
        LocalDate end = LocalDate.of(2025, 12, 3);

        List<WeatherForecast> forecasts = List.of(new WeatherForecast(), new WeatherForecast());
        List<WeatherForecastDTO> dtos = List.of(new WeatherForecastDTO(), new WeatherForecastDTO());

        when(weatherForecastService.getForecastsForDateRange(start, end)).thenReturn(forecasts);
        when(weatherForecastMapper.toDTOList(forecasts)).thenReturn(dtos);

        mockMvc.perform(get("/api/forecast/date-range")
                .param("startDate", "2025-12-01")
                .param("endDate", "2025-12-03"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(weatherForecastService).getForecastsForDateRange(start, end);
        verify(weatherForecastMapper).toDTOList(forecasts);
        verifyNoMoreInteractions(weatherForecastService, weatherForecastMapper);
    }

    @Test
    void getForecastsForDate_returns200() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 18);

        List<WeatherForecast> forecasts = List.of(new WeatherForecast());
        List<WeatherForecastDTO> dtos = List.of(new WeatherForecastDTO());

        when(weatherForecastService.getForecastsForDate(date)).thenReturn(forecasts);
        when(weatherForecastMapper.toDTOList(forecasts)).thenReturn(dtos);

        mockMvc.perform(get("/api/forecast/date/2025-12-18"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        verify(weatherForecastService).getForecastsForDate(date);
        verify(weatherForecastMapper).toDTOList(forecasts);
        verifyNoMoreInteractions(weatherForecastService, weatherForecastMapper);
    }

    @Test
    void getLatestForecastForDate_returns200_whenPresent() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 18);

        WeatherForecast forecast = new WeatherForecast();
        WeatherForecastDTO dto = new WeatherForecastDTO();

        when(weatherForecastService.getLatestForecastForDate(date)).thenReturn(forecast);
        when(weatherForecastMapper.toDTO(forecast)).thenReturn(dto);

        mockMvc.perform(get("/api/forecast/latest/2025-12-18"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(weatherForecastService).getLatestForecastForDate(date);
        verify(weatherForecastMapper).toDTO(forecast);
        verifyNoMoreInteractions(weatherForecastService, weatherForecastMapper);
    }

    @Test
    void getLatestForecastForDate_returns404_whenNull() throws Exception {
        LocalDate date = LocalDate.of(2025, 12, 18);

        when(weatherForecastService.getLatestForecastForDate(date)).thenReturn(null);

        mockMvc.perform(get("/api/forecast/latest/2025-12-18"))
                .andExpect(status().isNotFound());

        verify(weatherForecastService).getLatestForecastForDate(date);
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }

    @Test
    void getRecentForecasts_returns200_andPassesHours() throws Exception {
        List<WeatherForecast> forecasts = List.of(new WeatherForecast(), new WeatherForecast(), new WeatherForecast());
        List<WeatherForecastDTO> dtos = List.of(new WeatherForecastDTO(), new WeatherForecastDTO(),
                new WeatherForecastDTO());

        when(weatherForecastService.getRecentForecasts(12)).thenReturn(forecasts);
        when(weatherForecastMapper.toDTOList(forecasts)).thenReturn(dtos);

        mockMvc.perform(get("/api/forecast/recent").param("hours", "12"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));

        verify(weatherForecastService).getRecentForecasts(12);
        verify(weatherForecastMapper).toDTOList(forecasts);
        verifyNoMoreInteractions(weatherForecastService, weatherForecastMapper);
    }

    @Test
    void updateForecastsForCurrentLocation_returns200_whenOk() throws Exception {
        doNothing().when(weatherForecastService).updateForecastsForCurrentLocation();

        mockMvc.perform(post("/api/forecast/update"))
                .andExpect(status().isOk())
                .andExpect(content().string("Weather forecasts updated successfully"));

        verify(weatherForecastService).updateForecastsForCurrentLocation();
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }

    @Test
    void updateForecastsForCurrentLocation_returns500_whenServiceThrows() throws Exception {
        doThrow(new RuntimeException("boom")).when(weatherForecastService).updateForecastsForCurrentLocation();

        mockMvc.perform(post("/api/forecast/update"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to update weather forecasts"));

        verify(weatherForecastService).updateForecastsForCurrentLocation();
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }

    @Test
    void getAvailableCities_returns200() throws Exception {
        when(weatherForecastService.getAvailableCities()).thenReturn(List.of("Berlin", "Hamburg"));

        mockMvc.perform(get("/api/forecast/cities"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Berlin"))
                .andExpect(jsonPath("$[1]").value("Hamburg"));

        verify(weatherForecastService).getAvailableCities();
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }

    @Test
    void getLastUpdateInfo_returns200_andHasNextUpdateEstimate() throws Exception {
        // Wir testen hier nur "stabile" Teile: JSON Keys existieren.
        // nextUpdateEstimate hängt von LocalDateTime.now() ab, daher nicht auf exakten
        // Wert prüfen.

        LocalDate today = LocalDate.now();
        WeatherForecast latest = new WeatherForecast();
        // damit "hasRecentData" stabil true wird:
        latest.setUpdatedAt(LocalDateTime.now().minusHours(1));
        latest.setCreatedAt(LocalDateTime.now().minusHours(2));
        latest.setForecastDate(today);

        when(weatherForecastService.getLatestForecastForDate(today)).thenReturn(latest);

        mockMvc.perform(get("/api/forecast/last-update"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.forecastDate").exists())
                .andExpect(jsonPath("$.nextUpdateEstimate").exists())
                .andExpect(jsonPath("$.hasRecentData").value(true));

        verify(weatherForecastService).getLatestForecastForDate(today);
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }

    @Test
    void getLastUpdateInfo_returns200_withError_whenServiceThrows() throws Exception {
        when(weatherForecastService.getLatestForecastForDate(any(LocalDate.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/forecast/last-update"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unable to fetch last update info"))
                .andExpect(jsonPath("$.lastUpdated").value(nullValue()));

        verify(weatherForecastService).getLatestForecastForDate(any(LocalDate.class));
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }

    @Test
    void cleanupOldForecasts_returns200_whenOk() throws Exception {
        doNothing().when(weatherForecastService).cleanupOldForecasts();

        mockMvc.perform(delete("/api/forecast/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("Old forecasts cleaned up successfully"));

        verify(weatherForecastService).cleanupOldForecasts();
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }

    @Test
    void cleanupOldForecasts_returns500_whenServiceThrows() throws Exception {
        doThrow(new RuntimeException("boom")).when(weatherForecastService).cleanupOldForecasts();

        mockMvc.perform(delete("/api/forecast/cleanup"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to cleanup old forecasts"));

        verify(weatherForecastService).cleanupOldForecasts();
        verifyNoMoreInteractions(weatherForecastService);
        verifyNoInteractions(weatherForecastMapper);
    }
}