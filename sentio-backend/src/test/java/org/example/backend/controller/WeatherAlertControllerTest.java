package org.example.backend.controller;

import org.example.backend.dto.WeatherAlertDTO;
import org.example.backend.mapper.WeatherAlertMapper;
import org.example.backend.model.WeatherAlert;
import org.example.backend.service.IBrightSkyService;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WeatherAlertController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
})
@Import(WeatherAlertControllerTest.TestBeans.class)
@org.springframework.test.context.TestPropertySource(properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class WeatherAlertControllerTest {

        @Autowired
        MockMvc mockMvc;

        @Autowired IBrightSkyService brightSkyService;
    @Autowired WeatherAlertMapper weatherAlertMapper;

    @TestConfiguration
    static class TestBeans {
                @Bean IBrightSkyService brightSkyService() { return mock(IBrightSkyService.class); }
        @Bean WeatherAlertMapper weatherAlertMapper() { return mock(WeatherAlertMapper.class); }
    }

        @AfterEach
        void resetMocks() {
                reset(brightSkyService, weatherAlertMapper);
        }

        @Test
        void getCurrentLocationAlerts_returns200_andUsesGermanWhenLangDe() throws Exception {
                List<WeatherAlert> alerts = List.of(new WeatherAlert(), new WeatherAlert());
                List<WeatherAlertDTO> dtos = List.of(new WeatherAlertDTO(), new WeatherAlertDTO());

                when(brightSkyService.getAlertsForCurrentLocation()).thenReturn(alerts);
                when(weatherAlertMapper.toDTOList(alerts, true)).thenReturn(dtos);

                mockMvc.perform(get("/api/alerts/current-location")
                                .param("lang", "de"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(brightSkyService).getAlertsForCurrentLocation();
                verify(weatherAlertMapper).toDTOList(alerts, true);
                verifyNoMoreInteractions(brightSkyService, weatherAlertMapper);
        }

        @Test
        void getActiveAlerts_returns200_andUsesEnglishByDefault() throws Exception {
                List<WeatherAlert> alerts = List.of(new WeatherAlert());
                List<WeatherAlertDTO> dtos = List.of(new WeatherAlertDTO());

                when(brightSkyService.getActiveAlerts()).thenReturn(alerts);
                when(weatherAlertMapper.toDTOList(alerts, false)).thenReturn(dtos);

                mockMvc.perform(get("/api/alerts/active")) // default lang=en
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(brightSkyService).getActiveAlerts();
                verify(weatherAlertMapper).toDTOList(alerts, false);
                verifyNoMoreInteractions(brightSkyService, weatherAlertMapper);
        }

        @Test
        void getAlertsByWarnCellId_returns200() throws Exception {
                List<WeatherAlert> alerts = List.of(new WeatherAlert());
                List<WeatherAlertDTO> dtos = List.of(new WeatherAlertDTO());

                when(brightSkyService.getAlertsByWarnCellId(123L)).thenReturn(alerts);
                when(weatherAlertMapper.toDTOList(alerts, false)).thenReturn(dtos);

                mockMvc.perform(get("/api/alerts/warn-cell/123")
                                .param("lang", "en"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(brightSkyService).getAlertsByWarnCellId(123L);
                verify(weatherAlertMapper).toDTOList(alerts, false);
                verifyNoMoreInteractions(brightSkyService, weatherAlertMapper);
        }

        @Test
        void getAlertsByCity_returns200() throws Exception {
                List<WeatherAlert> alerts = List.of(new WeatherAlert(), new WeatherAlert());
                List<WeatherAlertDTO> dtos = List.of(new WeatherAlertDTO(), new WeatherAlertDTO());

                when(brightSkyService.getAlertsByCity("Berlin")).thenReturn(alerts);
                when(weatherAlertMapper.toDTOList(alerts, false)).thenReturn(dtos);

                mockMvc.perform(get("/api/alerts/city/Berlin"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(brightSkyService).getAlertsByCity("Berlin");
                verify(weatherAlertMapper).toDTOList(alerts, false);
                verifyNoMoreInteractions(brightSkyService, weatherAlertMapper);
        }

        @Test
        void getAlertsBySeverity_returns200_andPreferGerman() throws Exception {
                List<WeatherAlert> alerts = List.of(new WeatherAlert());
                List<WeatherAlertDTO> dtos = List.of(new WeatherAlertDTO());

                when(brightSkyService.getAlertsBySeverity("severe")).thenReturn(alerts);
                when(weatherAlertMapper.toDTOList(alerts, true)).thenReturn(dtos);

                mockMvc.perform(get("/api/alerts/severity/severe")
                                .param("lang", "de"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(brightSkyService).getAlertsBySeverity("severe");
                verify(weatherAlertMapper).toDTOList(alerts, true);
                verifyNoMoreInteractions(brightSkyService, weatherAlertMapper);
        }

        @Test
        void getActiveAlertsForLocation_passesWarnCellId_andReturns200() throws Exception {
                List<WeatherAlert> alerts = List.of(new WeatherAlert());
                List<WeatherAlertDTO> dtos = List.of(new WeatherAlertDTO());

                when(brightSkyService.getActiveAlertsForLocation("Hamburg", 999L)).thenReturn(alerts);
                when(weatherAlertMapper.toDTOList(alerts, false)).thenReturn(dtos);

                mockMvc.perform(get("/api/alerts/location")
                                .param("city", "Hamburg")
                                .param("warnCellId", "999")
                                .param("lang", "en"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(brightSkyService).getActiveAlertsForLocation("Hamburg", 999L);
                verify(weatherAlertMapper).toDTOList(alerts, false);
                verifyNoMoreInteractions(brightSkyService, weatherAlertMapper);
        }

        @Test
        void getRecentAlerts_returns200() throws Exception {
                List<WeatherAlert> alerts = List.of(new WeatherAlert());
                List<WeatherAlertDTO> dtos = List.of(new WeatherAlertDTO());

                when(brightSkyService.getRecentAlerts()).thenReturn(alerts);
                when(weatherAlertMapper.toDTOList(alerts, false)).thenReturn(dtos);

                mockMvc.perform(get("/api/alerts/recent"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

                verify(brightSkyService).getRecentAlerts();
                verify(weatherAlertMapper).toDTOList(alerts, false);
                verifyNoMoreInteractions(brightSkyService, weatherAlertMapper);
        }

        @Test
        void updateAlertsForCurrentLocation_returns200_whenOk() throws Exception {
                doNothing().when(brightSkyService).updateAlertsForCurrentLocation();

                mockMvc.perform(post("/api/alerts/update"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Weather alerts updated successfully"));

                verify(brightSkyService).updateAlertsForCurrentLocation();
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void updateAlertsForCurrentLocation_returns500_whenServiceThrows() throws Exception {
                doThrow(new RuntimeException("boom")).when(brightSkyService).updateAlertsForCurrentLocation();

                mockMvc.perform(post("/api/alerts/update"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string("Failed to update weather alerts"));

                verify(brightSkyService).updateAlertsForCurrentLocation();
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void cleanupExpiredAlerts_returns200_whenOk() throws Exception {
                doNothing().when(brightSkyService).cleanupExpiredAlerts();

                mockMvc.perform(delete("/api/alerts/cleanup"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Expired alerts cleaned up successfully"));

                verify(brightSkyService).cleanupExpiredAlerts();
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void cleanupExpiredAlerts_returns500_whenServiceThrows() throws Exception {
                doThrow(new RuntimeException("boom")).when(brightSkyService).cleanupExpiredAlerts();

                mockMvc.perform(delete("/api/alerts/cleanup"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(content().string("Failed to cleanup expired alerts"));

                verify(brightSkyService).cleanupExpiredAlerts();
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void getRadarEndpoint_returns200_andPayload_whenServiceReturnsUrl() throws Exception {
                when(brightSkyService.getRadarEndpointUrlForCurrentLocation(null, "compressed"))
                                .thenReturn("https://example.test/radar");

                mockMvc.perform(get("/api/alerts/radar/endpoint"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.radarEndpoint").value("https://example.test/radar"))
                                .andExpect(jsonPath("$.format").value("compressed"))
                                .andExpect(jsonPath("$.distance").value(200000))
                                .andExpect(jsonPath("$.documentation").isString())
                                .andExpect(jsonPath("$.note").isString());

                verify(brightSkyService).getRadarEndpointUrlForCurrentLocation(null, "compressed");
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void getRadarEndpoint_usesProvidedDistanceAndFormat() throws Exception {
                when(brightSkyService.getRadarEndpointUrlForCurrentLocation(12345, "plain"))
                                .thenReturn("https://example.test/radar2");

                mockMvc.perform(get("/api/alerts/radar/endpoint")
                                .param("distance", "12345")
                                .param("format", "plain"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.radarEndpoint").value("https://example.test/radar2"))
                                .andExpect(jsonPath("$.format").value("plain"))
                                .andExpect(jsonPath("$.distance").value(12345));

                verify(brightSkyService).getRadarEndpointUrlForCurrentLocation(12345, "plain");
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void getRadarEndpoint_returns200_withError_whenServiceThrows() throws Exception {
                when(brightSkyService.getRadarEndpointUrlForCurrentLocation(null, "compressed"))
                                .thenThrow(new RuntimeException("boom"));

                mockMvc.perform(get("/api/alerts/radar/endpoint"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.error").value("Unable to generate radar endpoint"));

                verify(brightSkyService).getRadarEndpointUrlForCurrentLocation(null, "compressed");
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void getCitiesWithActiveAlerts_returns200() throws Exception {
                when(brightSkyService.getCitiesWithActiveAlerts()).thenReturn(List.of("Berlin", "Hamburg"));

                mockMvc.perform(get("/api/alerts/cities"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0]").value("Berlin"))
                                .andExpect(jsonPath("$[1]").value("Hamburg"));

                verify(brightSkyService).getCitiesWithActiveAlerts();
                verifyNoMoreInteractions(brightSkyService);
                verifyNoInteractions(weatherAlertMapper);
        }

        @Test
        void fetchRadarMetadata_returns200_whenNotNull() throws Exception {
                org.example.backend.dto.RadarMetadataDTO metadata = org.example.backend.dto.RadarMetadataDTO.builder()
                                .coveragePercent(50.5f)
                                .build();
                when(brightSkyService.fetchAndStoreRadarMetadataForCurrentLocation(null)).thenReturn(metadata);

                mockMvc.perform(post("/api/alerts/radar/fetch"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.coveragePercent").value(50.5));
        }

        @Test
        void fetchRadarMetadata_withDistance_returns200_whenNotNull() throws Exception {
                org.example.backend.dto.RadarMetadataDTO metadata = org.example.backend.dto.RadarMetadataDTO.builder()
                                .coveragePercent(50.5f)
                                .build();
                when(brightSkyService.fetchAndStoreRadarMetadataForCurrentLocation(100)).thenReturn(metadata);

                mockMvc.perform(post("/api/alerts/radar/fetch").param("distance", "100"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.coveragePercent").value(50.5));
        }

        @Test
        void fetchRadarMetadata_returns404_whenNull() throws Exception {
                when(brightSkyService.fetchAndStoreRadarMetadataForCurrentLocation(null)).thenReturn(null);

                mockMvc.perform(post("/api/alerts/radar/fetch"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error").value("NO_DEVICES_REGISTERED"));
        }

        @Test
        void getLatestRadarMetadata_returns200_whenPresent() throws Exception {
                org.example.backend.model.WeatherRadarMetadata metadata = new org.example.backend.model.WeatherRadarMetadata();
                metadata.setCoveragePercent(80.0f);
                metadata.setLatitude(52.52f);
                metadata.setLongitude(13.40f);
                metadata.setDistance(100000);

                when(brightSkyService.getLatestRadarMetadata()).thenReturn(java.util.Optional.of(metadata));
                when(brightSkyService.getRadarEndpointUrl(52.52f, 13.40f, 100000, "compressed"))
                                .thenReturn("http://radarurl");

                mockMvc.perform(get("/api/alerts/radar/latest"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.coveragePercent").value(80.0))
                                .andExpect(jsonPath("$.hasActivePrecipitation").value(true))
                                .andExpect(jsonPath("$.directApiUrl").value("http://radarurl"));
        }

        @Test
        void getLatestRadarMetadata_returns404_whenEmpty() throws Exception {
                when(brightSkyService.getLatestRadarMetadata()).thenReturn(java.util.Optional.empty());

                mockMvc.perform(get("/api/alerts/radar/latest"))
                                .andExpect(status().isNotFound());
        }
}