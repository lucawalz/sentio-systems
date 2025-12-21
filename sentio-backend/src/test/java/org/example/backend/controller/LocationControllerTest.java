package org.example.backend.controller;

import org.example.backend.dto.LocationDataDTO;
import org.example.backend.mapper.LocationDataMapper;
import org.example.backend.model.LocationData;
import org.example.backend.service.IpLocationService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = LocationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@Import(LocationControllerTest.TestBeans.class)
class LocationControllerTest {

    @Autowired MockMvc mockMvc;

    @Autowired IpLocationService ipLocationService;
    @Autowired LocationDataMapper locationDataMapper;

    @AfterEach
    void resetMocks() {
        reset(ipLocationService, locationDataMapper);
    }
    @TestConfiguration
    static class TestBeans {
        @Bean IpLocationService ipLocationService() { return mock(IpLocationService.class); }
        @Bean LocationDataMapper locationDataMapper() { return mock(LocationDataMapper.class); }
    }

    @Test
    void getCurrentLocation_returns200_whenPresent() throws Exception {
        LocationData entity = new LocationData();
        LocationDataDTO dto = new LocationDataDTO();
        dto.setCity("Berlin");
        dto.setCountry("DE");

        when(ipLocationService.getCurrentLocation()).thenReturn(Optional.of(entity));
        when(locationDataMapper.toDTO(entity)).thenReturn(dto);

        mockMvc.perform(get("/api/location/current"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("Berlin"))
                .andExpect(jsonPath("$.country").value("DE"));

        verify(ipLocationService).getCurrentLocation();
        verify(locationDataMapper).toDTO(entity);
        verifyNoMoreInteractions(ipLocationService, locationDataMapper);
    }

    @Test
    void getCurrentLocation_returns404_whenEmpty() throws Exception {
        when(ipLocationService.getCurrentLocation()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/location/current"))
                .andExpect(status().isNotFound());

        verify(ipLocationService).getCurrentLocation();
        verifyNoMoreInteractions(ipLocationService);
        verifyNoInteractions(locationDataMapper);
    }

    @Test
    void getLocationByIp_withQueryParam_returns200_whenPresent() throws Exception {
        LocationData entity = new LocationData();
        LocationDataDTO dto = new LocationDataDTO();
        dto.setCity("Hamburg");
        dto.setCountry("DE");

        when(ipLocationService.getLocationByIp("1.2.3.4")).thenReturn(Optional.of(entity));
        when(locationDataMapper.toDTO(entity)).thenReturn(dto);

        mockMvc.perform(get("/api/location/by-ip")
                        .param("ip", "1.2.3.4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("Hamburg"))
                .andExpect(jsonPath("$.country").value("DE"));

        verify(ipLocationService).getLocationByIp("1.2.3.4");
        verify(locationDataMapper).toDTO(entity);
        verifyNoMoreInteractions(ipLocationService, locationDataMapper);
    }

    @Test
    void getLocationByIp_withQueryParam_returns404_whenEmpty() throws Exception {
        when(ipLocationService.getLocationByIp("1.2.3.4")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/location/by-ip")
                        .param("ip", "1.2.3.4"))
                .andExpect(status().isNotFound());

        verify(ipLocationService).getLocationByIp("1.2.3.4");
        verifyNoMoreInteractions(ipLocationService);
        verifyNoInteractions(locationDataMapper);
    }

    @Test
    void getLocationByIp_withoutParam_usesXForwardedForFirstIp() throws Exception {
        // X-Forwarded-For kann mehrere IPs enthalten: "client, proxy1, proxy2"
        String forwarded = "9.9.9.9, 10.0.0.1";

        LocationData entity = new LocationData();
        LocationDataDTO dto = new LocationDataDTO();
        dto.setCity("Munich");
        dto.setCountry("DE");

        when(ipLocationService.getLocationByIp("9.9.9.9")).thenReturn(Optional.of(entity));
        when(locationDataMapper.toDTO(entity)).thenReturn(dto);

        mockMvc.perform(get("/api/location/by-ip")
                        .header("X-Forwarded-For", forwarded))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Munich"))
                .andExpect(jsonPath("$.country").value("DE"));

        verify(ipLocationService).getLocationByIp("9.9.9.9");
        verify(locationDataMapper).toDTO(entity);
        verifyNoMoreInteractions(ipLocationService, locationDataMapper);
    }

    @Test
    void cleanupOldLocationData_returns200_whenOk() throws Exception {
        doNothing().when(ipLocationService).cleanupOldLocationData();

        mockMvc.perform(post("/api/location/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("Old location data cleaned up successfully"));

        verify(ipLocationService).cleanupOldLocationData();
        verifyNoMoreInteractions(ipLocationService);
        verifyNoInteractions(locationDataMapper);
    }

    @Test
    void cleanupOldLocationData_returns500_whenServiceThrows() throws Exception {
        doThrow(new RuntimeException("boom")).when(ipLocationService).cleanupOldLocationData();

        mockMvc.perform(post("/api/location/cleanup"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to cleanup old location data"));

        verify(ipLocationService).cleanupOldLocationData();
        verifyNoMoreInteractions(ipLocationService);
        verifyNoInteractions(locationDataMapper);
    }
}