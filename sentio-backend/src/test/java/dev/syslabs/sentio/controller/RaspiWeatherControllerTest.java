package dev.syslabs.sentio.controller;

import dev.syslabs.sentio.dto.WeatherStats;
import dev.syslabs.sentio.model.RaspiWeatherData;
import dev.syslabs.sentio.service.RaspiWeatherDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RaspiWeatherController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Import(RaspiWeatherControllerTest.TestBeans.class)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class RaspiWeatherControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    RaspiWeatherDataService raspiWeatherDataService;

    @TestConfiguration
    static class TestBeans {
        @Bean
        RaspiWeatherDataService raspiWeatherDataService() {
            return mock(RaspiWeatherDataService.class);
        }
    }

    @AfterEach
    void resetMocks() {
        reset(raspiWeatherDataService);
    }

    @Test
    void getLatestWeather_returns200_whenPresent() throws Exception {
        RaspiWeatherData latest = new RaspiWeatherData();
        latest.setId(1L);
        latest.setTimestamp(LocalDateTime.of(2025, 12, 18, 10, 0));

        when(raspiWeatherDataService.getLatestWeatherData()).thenReturn(latest);

        mockMvc.perform(get("/api/weather/latest"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1));

        verify(raspiWeatherDataService).getLatestWeatherData();
        verifyNoMoreInteractions(raspiWeatherDataService);
    }

    @Test
    void getLatestWeather_returns404_whenNull() throws Exception {
        when(raspiWeatherDataService.getLatestWeatherData()).thenReturn(null);

        mockMvc.perform(get("/api/weather/latest"))
                .andExpect(status().isNotFound());

        verify(raspiWeatherDataService).getLatestWeatherData();
        verifyNoMoreInteractions(raspiWeatherDataService);
    }

    @Test
    void getRecentWeather_returns200_andArray() throws Exception {
        RaspiWeatherData a = new RaspiWeatherData();
        a.setId(1L);
        a.setTimestamp(LocalDateTime.of(2025, 12, 18, 10, 0));

        RaspiWeatherData b = new RaspiWeatherData();
        b.setId(2L);
        b.setTimestamp(LocalDateTime.of(2025, 12, 18, 9, 0));

        when(raspiWeatherDataService.getRecentWeatherData()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/weather/recent"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(raspiWeatherDataService).getRecentWeatherData();
        verifyNoMoreInteractions(raspiWeatherDataService);
    }

    @Test
    void getAllWeather_returns200_andArray() throws Exception {
        RaspiWeatherData a = new RaspiWeatherData();
        a.setId(1L);
        RaspiWeatherData b = new RaspiWeatherData();
        b.setId(2L);

        when(raspiWeatherDataService.getAllWeatherData()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/weather/all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(raspiWeatherDataService).getAllWeatherData();
        verifyNoMoreInteractions(raspiWeatherDataService);
    }

    @Test
    void addWeatherData_setsTimestamp_whenNull_andReturns200() throws Exception {
        // Service gibt das gespeicherte Objekt zurück
        when(raspiWeatherDataService.saveWeatherData(any(RaspiWeatherData.class)))
                .thenAnswer(inv -> {
                    RaspiWeatherData arg = inv.getArgument(0);
                    // simulieren: DB setzt ID
                    arg.setId(99L);
                    return arg;
                });

        mockMvc.perform(post("/api/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(99));

        ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
        verify(raspiWeatherDataService).saveWeatherData(captor.capture());

        assertThat(captor.getValue().getTimestamp()).isNotNull();

        verifyNoMoreInteractions(raspiWeatherDataService);
    }

    @Test
    void getWeatherStats_returns200_andStatsJson() throws Exception {
        RaspiWeatherData latest = new RaspiWeatherData();
        latest.setId(5L);
        latest.setTimestamp(LocalDateTime.of(2025, 12, 18, 10, 0));

        WeatherStats stats = new WeatherStats(123L, latest, 12.3, 45.6,
                1013.2);

        when(raspiWeatherDataService.getWeatherStats()).thenReturn(stats);

        mockMvc.perform(get("/api/weather/stats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalReadings").value(123))
                .andExpect(jsonPath("$.latest.id").value(5));

        verify(raspiWeatherDataService).getWeatherStats();
        verifyNoMoreInteractions(raspiWeatherDataService);
    }
}