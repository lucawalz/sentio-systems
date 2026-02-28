package org.example.backend.config;

import org.example.backend.service.BrightSkyService;
import org.example.backend.service.DeviceService;
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
    private DeviceService deviceService;

    @InjectMocks
    private SchedulingConfig schedulingConfig;

    @Test
    @DisplayName("onApplicationReady should skip if no devices exist")
    void onApplicationReady_NoDevices() {
        when(deviceService.existsAnyDevice()).thenReturn(false);

        schedulingConfig.onApplicationReady();

        verify(weatherForecastService, never()).updateForecastsForAllDeviceLocations();
        verify(brightSkyService, never()).updateAlertsForAllDeviceLocations();
    }

    @Test
    @DisplayName("onApplicationReady should fetch initial data if devices exist")
    void onApplicationReady_WithDevices() {
        when(deviceService.existsAnyDevice()).thenReturn(true);

        schedulingConfig.onApplicationReady();

        verify(weatherForecastService).updateForecastsForAllDeviceLocations();
        verify(brightSkyService).updateAlertsForAllDeviceLocations();
    }

    @Test
    @DisplayName("onApplicationReady should handle exceptions gracefully")
    void onApplicationReady_HandlesException() {
        when(deviceService.existsAnyDevice()).thenReturn(true);
        doThrow(new RuntimeException("API down")).when(weatherForecastService).updateForecastsForAllDeviceLocations();

        schedulingConfig.onApplicationReady();

        verify(weatherForecastService).updateForecastsForAllDeviceLocations();
        // The exception should be caught, not propagated
    }

    @Test
    @DisplayName("updateDailyWeatherForecasts should skip if no devices")
    void updateDailyWeatherForecasts_NoDevices() {
        when(deviceService.existsAnyDevice()).thenReturn(false);

        schedulingConfig.updateDailyWeatherForecasts();

        verify(weatherForecastService, never()).updateForecastsForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateDailyWeatherForecasts should update if devices exist")
    void updateDailyWeatherForecasts_WithDevices() {
        when(deviceService.existsAnyDevice()).thenReturn(true);

        schedulingConfig.updateDailyWeatherForecasts();

        verify(weatherForecastService).updateForecastsForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateDailyHistoricalWeather should execute condition correctly")
    void updateDailyHistoricalWeather() {
        when(deviceService.existsAnyDevice()).thenReturn(false).thenReturn(true);

        schedulingConfig.updateDailyHistoricalWeather(); // skips
        schedulingConfig.updateDailyHistoricalWeather(); // executes

        verify(historicalWeatherService, times(1)).updateHistoricalWeatherForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateWeatherAlerts should execute condition correctly")
    void updateWeatherAlerts() {
        when(deviceService.existsAnyDevice()).thenReturn(false).thenReturn(true);

        schedulingConfig.updateWeatherAlerts(); // skips
        schedulingConfig.updateWeatherAlerts(); // executes

        verify(brightSkyService, times(1)).updateAlertsForAllDeviceLocations();
    }

    @Test
    @DisplayName("updateAlertsAtPeakHours should execute condition correctly")
    void updateAlertsAtPeakHours() {
        when(deviceService.existsAnyDevice()).thenReturn(false).thenReturn(true);

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
