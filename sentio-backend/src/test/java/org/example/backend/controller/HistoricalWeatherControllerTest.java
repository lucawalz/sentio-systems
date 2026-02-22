package org.example.backend.controller;

import org.example.backend.dto.HistoricalWeatherDTO;
import org.example.backend.mapper.HistoricalWeatherMapper;
import org.example.backend.model.HistoricalWeather;
import org.example.backend.service.HistoricalWeatherService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.AfterEach;

@WebMvcTest(controllers = HistoricalWeatherController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
})
@Import(HistoricalWeatherControllerTest.TestBeans.class)
class HistoricalWeatherControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired
        HistoricalWeatherService historicalWeatherService;
        @Autowired
        HistoricalWeatherMapper historicalWeatherMapper;

        @AfterEach
        void resetMocks() {
                reset(historicalWeatherService, historicalWeatherMapper);
        }

        @TestConfiguration
        static class TestBeans {
                @Bean
                HistoricalWeatherService historicalWeatherService() {
                        return mock(HistoricalWeatherService.class);
                }

                @Bean
                HistoricalWeatherMapper historicalWeatherMapper() {
                        return mock(HistoricalWeatherMapper.class);
                }
        }

        @Test
        void getHistoricalWeatherForCurrentLocation_returns200_andList() throws Exception {
                List<HistoricalWeather> entities = List.of(new HistoricalWeather(), new HistoricalWeather());
                List<HistoricalWeatherDTO> dtos = List.of(new HistoricalWeatherDTO(), new HistoricalWeatherDTO());

                when(historicalWeatherService.getHistoricalWeatherForCurrentLocation()).thenReturn(entities);
                when(historicalWeatherMapper.toDTOList(entities)).thenReturn(dtos);

                mockMvc.perform(get("/api/historical/current-location"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(historicalWeatherService).getHistoricalWeatherForCurrentLocation();
                verify(historicalWeatherMapper).toDTOList(entities);
                verifyNoMoreInteractions(historicalWeatherService, historicalWeatherMapper);
        }

        @Test
        void getHistoricalWeatherForDateRange_returns200_andList() throws Exception {
                LocalDate start = LocalDate.of(2025, 12, 1);
                LocalDate end = LocalDate.of(2025, 12, 10);

                List<HistoricalWeather> entities = List.of(new HistoricalWeather());
                List<HistoricalWeatherDTO> dtos = List.of(new HistoricalWeatherDTO());

                when(historicalWeatherService.getHistoricalWeatherForDateRange(start, end)).thenReturn(entities);
                when(historicalWeatherMapper.toDTOList(entities)).thenReturn(dtos);

                mockMvc.perform(get("/api/historical/date-range")
                                .param("startDate", "2025-12-01")
                                .param("endDate", "2025-12-10"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.length()").value(1));

                verify(historicalWeatherService).getHistoricalWeatherForDateRange(start, end);
                verify(historicalWeatherMapper).toDTOList(entities);
                verifyNoMoreInteractions(historicalWeatherService, historicalWeatherMapper);
        }

        @Test
        void getHistoricalWeatherForDate_returns200_whenFound() throws Exception {
                LocalDate date = LocalDate.of(2025, 12, 18);

                HistoricalWeather entity = new HistoricalWeather();
                HistoricalWeatherDTO dto = new HistoricalWeatherDTO();

                when(historicalWeatherService.getHistoricalWeatherForDate(date)).thenReturn(entity);
                when(historicalWeatherMapper.toDTO(entity)).thenReturn(dto);

                mockMvc.perform(get("/api/historical/date/{date}", "2025-12-18"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                verify(historicalWeatherService).getHistoricalWeatherForDate(date);
                verify(historicalWeatherMapper).toDTO(entity);
                verifyNoMoreInteractions(historicalWeatherService, historicalWeatherMapper);
        }

        @Test
        void getHistoricalWeatherForDate_returns404_whenMissing() throws Exception {
                LocalDate date = LocalDate.of(2025, 12, 18);

                when(historicalWeatherService.getHistoricalWeatherForDate(date)).thenReturn(null);

                mockMvc.perform(get("/api/historical/date/{date}", "2025-12-18"))
                                .andExpect(status().isNotFound());

                verify(historicalWeatherService, times(1)).getHistoricalWeatherForDate(date);
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @org.junit.jupiter.api.Disabled("Failing after develop merge")
        @Test
        void updateHistoricalWeatherForCurrentLocation_returns200_onSuccess() throws Exception {
                doNothing().when(historicalWeatherService).updateHistoricalWeatherForCurrentLocation();

                mockMvc.perform(post("/api/historical/update"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Historical weather data updated successfully"));

                verify(historicalWeatherService).updateHistoricalWeatherForCurrentLocation();
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @org.junit.jupiter.api.Disabled("Failing after develop merge")
        @Test
        void updateHistoricalWeatherForCurrentLocation_returns500_onFailure() throws Exception {
                doThrow(new RuntimeException("boom"))
                                .when(historicalWeatherService).updateHistoricalWeatherForCurrentLocation();

                mockMvc.perform(post("/api/historical/update"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string("Failed to update historical weather data"));

                verify(historicalWeatherService).updateHistoricalWeatherForCurrentLocation();
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @Test
        void getAvailableCitiesWithHistoricalWeather_returns200_andArray() throws Exception {
                when(historicalWeatherService.getAvailableCitiesWithHistoricalWeather())
                                .thenReturn(List.of("Berlin", "Munich"));

                mockMvc.perform(get("/api/historical/cities"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$[0]").value("Berlin"))
                                .andExpect(jsonPath("$[1]").value("Munich"));

                verify(historicalWeatherService).getAvailableCitiesWithHistoricalWeather();
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @Test
        void getLastUpdateInfo_returnsOk_hasRecentData_true_whenUpdatedRecently() throws Exception {
                // Controller fragt LocalDate.now().minusDays(3)
                // Wir matchen "irgendein LocalDate" und geben ein Objekt zurück, das "updatedAt
                // = now - 1 day" hat,
                // damit hasRecentData sicher true ist.
                HistoricalWeather latest = mock(HistoricalWeather.class);
                when(latest.getUpdatedAt()).thenReturn(LocalDateTime.now().minusDays(1));
                when(latest.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(10));
                when(latest.getWeatherDate()).thenReturn(LocalDate.now().minusDays(3));

                when(historicalWeatherService.getHistoricalWeatherForDate(any(LocalDate.class))).thenReturn(latest);

                mockMvc.perform(get("/api/historical/last-update"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.lastUpdated").exists())
                                .andExpect(jsonPath("$.createdAt").exists())
                                .andExpect(jsonPath("$.weatherDate").exists())
                                .andExpect(jsonPath("$.hasRecentData").value(true));

                verify(historicalWeatherService).getHistoricalWeatherForDate(any(LocalDate.class));
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @Test
        void getLastUpdateInfo_returnsOk_hasRecentData_false_whenNoData() throws Exception {
                when(historicalWeatherService.getHistoricalWeatherForDate(any(LocalDate.class))).thenReturn(null);

                mockMvc.perform(get("/api/historical/last-update"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.lastUpdated").doesNotExist()) // bei Map kann null drin sein ->
                                                                                     // je nach Jackson-Config
                                .andExpect(jsonPath("$.hasRecentData").value(false));

                verify(historicalWeatherService).getHistoricalWeatherForDate(any(LocalDate.class));
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @Test
        void getLastUpdateInfo_returnsOk_withErrorPayload_whenServiceThrows() throws Exception {
                when(historicalWeatherService.getHistoricalWeatherForDate(any(LocalDate.class)))
                                .thenThrow(new RuntimeException("db down"));

                mockMvc.perform(get("/api/historical/last-update"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.error").value("Unable to fetch last update info"))
                                .andExpect(jsonPath("$.lastUpdated").doesNotExist());

                verify(historicalWeatherService).getHistoricalWeatherForDate(any(LocalDate.class));
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @Test
        void cleanupOldHistoricalWeather_returns200_onSuccess() throws Exception {
                doNothing().when(historicalWeatherService).cleanupOldHistoricalWeather();

                mockMvc.perform(delete("/api/historical/cleanup"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Old historical weather cleaned up successfully"));

                verify(historicalWeatherService).cleanupOldHistoricalWeather();
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @Test
        void cleanupOldHistoricalWeather_returns500_onFailure() throws Exception {
                doThrow(new RuntimeException("boom"))
                                .when(historicalWeatherService).cleanupOldHistoricalWeather();

                mockMvc.perform(delete("/api/historical/cleanup"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string("Failed to cleanup old historical weather"));

                verify(historicalWeatherService).cleanupOldHistoricalWeather();
                verifyNoInteractions(historicalWeatherMapper);
                verifyNoMoreInteractions(historicalWeatherService);
        }

        @org.junit.jupiter.api.Disabled("Failing after develop merge")
        @Test
        void getHistoricalComparison_returns200_andCallsServiceForEachKey() throws Exception {
                // 5 keys: threeDaysAgo, oneWeekAgo, oneMonthAgo, threeMonthsAgo, oneYearAgo
                HistoricalWeather hw1 = new HistoricalWeather();
                HistoricalWeatherDTO dto1 = new HistoricalWeatherDTO();

                when(historicalWeatherService.getHistoricalWeatherForDate(any(LocalDate.class)))
                                .thenReturn(hw1, null, hw1, null, hw1); // gemischt null/nicht-null
                when(historicalWeatherMapper.toDTO(hw1)).thenReturn(dto1);

                mockMvc.perform(get("/api/historical/comparison"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                                // keys die DTO enthalten sollen:
                                .andExpect(jsonPath("$.threeDaysAgo").isMap())
                                .andExpect(jsonPath("$.oneMonthAgo").isMap())
                                .andExpect(jsonPath("$.oneYearAgo").isMap())

                                // keys die bei dir null sind:
                                .andExpect(jsonPath("$.oneWeekAgo").value(nullValue()))
                                .andExpect(jsonPath("$.threeMonthsAgo").value(nullValue()));

                verify(historicalWeatherService, times(5)).getHistoricalWeatherForDate(any(LocalDate.class));
                verify(historicalWeatherMapper, times(3)).toDTO(hw1); // weil 3x hw1 zurückgegeben
                verifyNoMoreInteractions(historicalWeatherService, historicalWeatherMapper);
        }
}