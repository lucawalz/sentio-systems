package org.example.backend.config;

import org.example.backend.repository.DeviceRepository;
import org.example.backend.service.BrightSkyService;
import org.example.backend.service.HistoricalWeatherService;
import org.example.backend.service.WeatherForecastService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulingConfig Unit Tests")
class SchedulingConfigTest {

    @Mock
    private WeatherForecastService weatherForecastService;

    @Mock
    private HistoricalWeatherService historicalWeatherService;

    @Mock
    private BrightSkyService brightSkyService;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private SchedulingConfig schedulingConfig;

    @Test
    @DisplayName("onApplicationReady should skip if no devices exist")
    void onApplicationReady_NoDevices() {
        when(deviceRepository.count()).thenReturn(0L);

        schedulingConfig.onApplicationReady();

        verify(weatherForecastService, never()).updateForecastsForAllDeviceLocations();
        verify(brightSkyService, never()).updateAlertsForAllDeviceLocations();
    }

    @Test
    @DisplayName("onApplicationReady should fetch initial data if devices exist")
    void onApplicationReady_WithDevices() {
        when(deviceRepository.count()).thenReturn(1L);

        schedulingConfig.onApplicationReady();

        verify(weatherForecastService).updateForecastsForAllDeviceLocations();
        verify(brightSkyService).updateAlertsForAllDeviceLocations();
    }

    @Test
    @DisplayName("onApplicationReady should handle exceptions gracefully")
    void onApplicationReady_HandlesException() {
        when(deviceRepository.count()).thenReturn(1L);
        doThrow(new RuntimeException("API down")).when(weatherForecastService).updateForecastsForAllDeviceLocations();

        schedulingConfig.onApplicationReady();

        verify(weatherForecastService).updateForecastsForAllDeviceLocations();
        // The exception should be caught, not propagated
    }

    @Test
    @DisplayName("updateDailyWeatherForecasts should skip if no devices")
    void updateDailyWeatherForecasts_NoDevices() {
        when(deviceRepository.count()).thenReturn(0L);

        schedulingConfig.updateDailyWeatherForecasts();

        verify(weatherForecastService, never()).updateForecastsForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateDailyWeatherForecasts should update if devices exist")
    void updateDailyWeatherForecasts_WithDevices() {
        when(deviceRepository.count()).thenReturn(2L);

        schedulingConfig.updateDailyWeatherForecasts();

        verify(weatherForecastService).updateForecastsForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateDailyHistoricalWeather should execute condition correctly")
    void updateDailyHistoricalWeather() {
        when(deviceRepository.count()).thenReturn(0L).thenReturn(1L);

        schedulingConfig.updateDailyHistoricalWeather(); // skips
        schedulingConfig.updateDailyHistoricalWeather(); // executes

        verify(historicalWeatherService, times(1)).updateHistoricalWeatherForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateWeatherAlerts should execute condition correctly")
    void updateWeatherAlerts() {
        when(deviceRepository.count()).thenReturn(0L).thenReturn(1L);

        schedulingConfig.updateWeatherAlerts(); // skips
        schedulingConfig.updateWeatherAlerts(); // executes

        verify(brightSkyService, times(1)).updateAlertsForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateAlertsAtPeakHours should execute condition correctly")
    void updateAlertsAtPeakHours() {
        when(deviceRepository.count()).thenReturn(0L).thenReturn(1L);

        schedulingConfig.updateAlertsAtPeakHours(); // skips
        schedulingConfig.updateAlertsAtPeakHours(); // executes

        verify(brightSkyService, times(1)).updateAlertsForAllDeviceLocations();
    }

    @Test
    @DisplayName("cleanupOldForecastData should always execute")
    void cleanupOldForecastData() {
        schedulingConfig.cleanupOldForecastData();
        verify(weatherForecastService).cleanupOldForecasts();
    }

    @Test
    @DisplayName("cleanupOldHistoricalWeatherData should always execute")
    void cleanupOldHistoricalWeatherData() {
        schedulingConfig.cleanupOldHistoricalWeatherData();
        verify(historicalWeatherService).cleanupOldHistoricalWeather();
    }

    @Test
    @DisplayName("cleanupExpiredAlerts should always execute")
    void cleanupExpiredAlerts() {
        schedulingConfig.cleanupExpiredAlerts();
        verify(brightSkyService).cleanupExpiredAlerts();
    }
}
