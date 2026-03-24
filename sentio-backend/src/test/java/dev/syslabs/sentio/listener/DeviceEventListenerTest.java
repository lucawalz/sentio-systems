package dev.syslabs.sentio.listener;

import dev.syslabs.sentio.event.DeviceLocationUpdatedEvent;
import dev.syslabs.sentio.event.DeviceRegisteredEvent;
import dev.syslabs.sentio.event.DeviceUnregisteredEvent;
import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.model.LocationData;
import dev.syslabs.sentio.model.WeatherAlert;
import dev.syslabs.sentio.service.BrightSkyService;
import dev.syslabs.sentio.service.HistoricalWeatherService;
import dev.syslabs.sentio.service.WeatherForecastService;
import dev.syslabs.sentio.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceEventListener Unit Tests")
class DeviceEventListenerTest {

    @Mock
    private WeatherForecastService weatherForecastService;

    @Mock
    private HistoricalWeatherService historicalWeatherService;

    @Mock
    private BrightSkyService brightSkyService;

    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private DeviceEventListener listener;

    private Device device;
    private static final String DEVICE_ID = "test-device-123";
    private static final String USERNAME = "test-user";
    private static final Double LATITUDE = 52.5200;
    private static final Double LONGITUDE = 13.4050;

    @BeforeEach
    void setUp() {
        device = new Device();
        device.setId(DEVICE_ID);
        device.setName("Test Device");
        device.setOwnerId("owner-123");
    }

    @Nested
    @DisplayName("onDeviceRegistered Tests")
    class OnDeviceRegisteredTests {

        @Test
        @DisplayName("Should broadcast device registered and fetch weather when device has coordinates")
        void shouldBroadcastAndFetchWeatherWhenDeviceHasCoordinates() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            List<WeatherAlert> alerts = new ArrayList<>();
            alerts.add(new WeatherAlert());
            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(alerts);

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(webSocketService).broadcastDeviceRegistered(DEVICE_ID, USERNAME);

            // Verify weather data was fetched
            ArgumentCaptor<Float> latCaptor = ArgumentCaptor.forClass(Float.class);
            ArgumentCaptor<Float> lonCaptor = ArgumentCaptor.forClass(Float.class);
            ArgumentCaptor<LocationData> locationCaptor = ArgumentCaptor.forClass(LocationData.class);

            verify(weatherForecastService).getForecastForLocation(
                    latCaptor.capture(), lonCaptor.capture(), locationCaptor.capture());

            assertThat(latCaptor.getValue()).isEqualTo(LATITUDE.floatValue());
            assertThat(lonCaptor.getValue()).isEqualTo(LONGITUDE.floatValue());
            assertThat(locationCaptor.getValue().getDeviceId()).isEqualTo(DEVICE_ID);

            verify(webSocketService).broadcastWeatherUpdated("FORECAST");
            verify(brightSkyService).getAlertsForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), eq(DEVICE_ID));
            verify(webSocketService).broadcastAlertsUpdated(1, true);
            verify(historicalWeatherService).getHistoricalWeatherForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), any(LocationData.class));
            verify(webSocketService).broadcastWeatherUpdated("HISTORICAL");
        }

        @Test
        @DisplayName("Should only broadcast registration when device has no coordinates")
        void shouldOnlyBroadcastWhenNoCoordinates() {
            // Given
            device.setLatitude(null);
            device.setLongitude(null);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(webSocketService).broadcastDeviceRegistered(DEVICE_ID, USERNAME);
            verifyNoInteractions(weatherForecastService);
            verifyNoInteractions(brightSkyService);
            verifyNoInteractions(historicalWeatherService);
        }

        @Test
        @DisplayName("Should not fetch weather when only latitude is present")
        void shouldNotFetchWeatherWhenOnlyLatitude() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(null);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(webSocketService).broadcastDeviceRegistered(DEVICE_ID, USERNAME);
            verifyNoInteractions(weatherForecastService);
        }

        @Test
        @DisplayName("Should not fetch weather when only longitude is present")
        void shouldNotFetchWeatherWhenOnlyLongitude() {
            // Given
            device.setLatitude(null);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(webSocketService).broadcastDeviceRegistered(DEVICE_ID, USERNAME);
            verifyNoInteractions(weatherForecastService);
        }

        @Test
        @DisplayName("Should handle weather service exception gracefully")
        void shouldHandleWeatherServiceException() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            when(weatherForecastService.getForecastForLocation(anyFloat(), anyFloat(), any()))
                    .thenThrow(new RuntimeException("Weather service error"));

            // When & Then - should not throw
            listener.onDeviceRegistered(event);

            // Verify broadcast still happened
            verify(webSocketService).broadcastDeviceRegistered(DEVICE_ID, USERNAME);
        }

        @Test
        @DisplayName("Should broadcast alerts with correct count when multiple alerts exist")
        void shouldBroadcastAlertsWithCorrectCount() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            List<WeatherAlert> alerts = new ArrayList<>();
            alerts.add(new WeatherAlert());
            alerts.add(new WeatherAlert());
            alerts.add(new WeatherAlert());
            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(alerts);

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(webSocketService).broadcastAlertsUpdated(3, true);
        }

        @Test
        @DisplayName("Should broadcast alerts with false when no alerts exist")
        void shouldBroadcastNoAlerts() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(new ArrayList<>());

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(webSocketService).broadcastAlertsUpdated(0, false);
        }
    }

    @Nested
    @DisplayName("onDeviceLocationUpdated Tests")
    class OnDeviceLocationUpdatedTests {

        @Test
        @DisplayName("Should fetch weather when location is updated")
        void shouldFetchWeatherOnLocationUpdate() {
            // Given
            DeviceLocationUpdatedEvent event = new DeviceLocationUpdatedEvent(
                    this, DEVICE_ID, LATITUDE, LONGITUDE, false);

            List<WeatherAlert> alerts = new ArrayList<>();
            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(alerts);

            // When
            listener.onDeviceLocationUpdated(event);

            // Then
            verify(weatherForecastService).getForecastForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), any(LocationData.class));
            verify(webSocketService).broadcastWeatherUpdated("FORECAST");
            verify(brightSkyService).getAlertsForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), eq(DEVICE_ID));
            verify(webSocketService).broadcastAlertsUpdated(0, false);
            verify(historicalWeatherService).getHistoricalWeatherForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), any(LocationData.class));
            verify(webSocketService).broadcastWeatherUpdated("HISTORICAL");
        }

        @Test
        @DisplayName("Should fetch weather on first location")
        void shouldFetchWeatherOnFirstLocation() {
            // Given
            DeviceLocationUpdatedEvent event = new DeviceLocationUpdatedEvent(
                    this, DEVICE_ID, LATITUDE, LONGITUDE, true);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(new ArrayList<>());

            // When
            listener.onDeviceLocationUpdated(event);

            // Then
            verify(weatherForecastService).getForecastForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), any(LocationData.class));
            verify(brightSkyService).getAlertsForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), eq(DEVICE_ID));
            verify(historicalWeatherService).getHistoricalWeatherForLocation(
                    eq(LATITUDE.floatValue()), eq(LONGITUDE.floatValue()), any(LocationData.class));
        }

        @Test
        @DisplayName("Should handle exception during weather fetch")
        void shouldHandleExceptionDuringWeatherFetch() {
            // Given
            DeviceLocationUpdatedEvent event = new DeviceLocationUpdatedEvent(
                    this, DEVICE_ID, LATITUDE, LONGITUDE, false);

            when(weatherForecastService.getForecastForLocation(anyFloat(), anyFloat(), any()))
                    .thenThrow(new RuntimeException("Service unavailable"));

            // When & Then - should not throw
            listener.onDeviceLocationUpdated(event);

            // Verify it attempted to fetch
            verify(weatherForecastService).getForecastForLocation(anyFloat(), anyFloat(), any());
        }

        @Test
        @DisplayName("Should create location data with correct properties")
        void shouldCreateLocationDataWithCorrectProperties() {
            // Given
            DeviceLocationUpdatedEvent event = new DeviceLocationUpdatedEvent(
                    this, DEVICE_ID, LATITUDE, LONGITUDE, false);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(new ArrayList<>());

            // When
            listener.onDeviceLocationUpdated(event);

            // Then
            ArgumentCaptor<LocationData> locationCaptor = ArgumentCaptor.forClass(LocationData.class);
            verify(weatherForecastService).getForecastForLocation(
                    anyFloat(), anyFloat(), locationCaptor.capture());

            LocationData capturedLocation = locationCaptor.getValue();
            assertThat(capturedLocation.getDeviceId()).isEqualTo(DEVICE_ID);
            assertThat(capturedLocation.getLatitude()).isEqualTo(LATITUDE.floatValue());
            assertThat(capturedLocation.getLongitude()).isEqualTo(LONGITUDE.floatValue());
            assertThat(capturedLocation.getCity()).isEqualTo("GPS Location");
            assertThat(capturedLocation.getCountry()).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("onDeviceUnregistered Tests")
    class OnDeviceUnregisteredTests {

        @Test
        @DisplayName("Should broadcast device unregistered")
        void shouldBroadcastDeviceUnregistered() {
            // Given
            DeviceUnregisteredEvent event = new DeviceUnregisteredEvent(
                    this, DEVICE_ID, USERNAME);

            // When
            listener.onDeviceUnregistered(event);

            // Then
            verify(webSocketService).broadcastDeviceUnregistered(DEVICE_ID, USERNAME);
            verifyNoInteractions(weatherForecastService);
            verifyNoInteractions(brightSkyService);
            verifyNoInteractions(historicalWeatherService);
        }

        @Test
        @DisplayName("Should handle null username")
        void shouldHandleNullUsername() {
            // Given
            DeviceUnregisteredEvent event = new DeviceUnregisteredEvent(
                    this, DEVICE_ID, null);

            // When
            listener.onDeviceUnregistered(event);

            // Then
            verify(webSocketService).broadcastDeviceUnregistered(DEVICE_ID, null);
        }

        @Test
        @DisplayName("Should handle null device ID")
        void shouldHandleNullDeviceId() {
            // Given
            DeviceUnregisteredEvent event = new DeviceUnregisteredEvent(
                    this, null, USERNAME);

            // When
            listener.onDeviceUnregistered(event);

            // Then
            verify(webSocketService).broadcastDeviceUnregistered(null, USERNAME);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle BrightSky service exception")
        void shouldHandleBrightSkyException() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenThrow(new RuntimeException("BrightSky error"));

            // When & Then - should not throw
            listener.onDeviceRegistered(event);

            // Verify other services were still called
            verify(weatherForecastService).getForecastForLocation(anyFloat(), anyFloat(), any());
        }

        @Test
        @DisplayName("Should handle historical weather service exception")
        void shouldHandleHistoricalWeatherException() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(new ArrayList<>());
            when(historicalWeatherService.getHistoricalWeatherForLocation(anyFloat(), anyFloat(), any()))
                    .thenThrow(new RuntimeException("Historical weather error"));

            // When & Then - should not throw
            listener.onDeviceRegistered(event);

            // Verify other services were called
            verify(weatherForecastService).getForecastForLocation(anyFloat(), anyFloat(), any());
            verify(brightSkyService).getAlertsForLocation(anyFloat(), anyFloat(), anyString());
        }

        @Test
        @DisplayName("Should handle extreme latitude values")
        void shouldHandleExtremeLatitude() {
            // Given
            device.setLatitude(90.0);
            device.setLongitude(LONGITUDE);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(new ArrayList<>());

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(weatherForecastService).getForecastForLocation(
                    eq(90.0f), anyFloat(), any());
        }

        @Test
        @DisplayName("Should handle extreme longitude values")
        void shouldHandleExtremeLongitude() {
            // Given
            device.setLatitude(LATITUDE);
            device.setLongitude(180.0);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(new ArrayList<>());

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(weatherForecastService).getForecastForLocation(
                    anyFloat(), eq(180.0f), any());
        }

        @Test
        @DisplayName("Should handle zero coordinates")
        void shouldHandleZeroCoordinates() {
            // Given
            device.setLatitude(0.0);
            device.setLongitude(0.0);
            DeviceRegisteredEvent event = new DeviceRegisteredEvent(this, device, USERNAME);

            when(brightSkyService.getAlertsForLocation(anyFloat(), anyFloat(), anyString()))
                    .thenReturn(new ArrayList<>());

            // When
            listener.onDeviceRegistered(event);

            // Then
            verify(weatherForecastService).getForecastForLocation(
                    eq(0.0f), eq(0.0f), any());
        }
    }
}
