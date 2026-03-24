package dev.syslabs.sentio.service;

import dev.syslabs.sentio.event.DeviceLocationUpdatedEvent;
import dev.syslabs.sentio.model.Device;
import dev.syslabs.sentio.model.LocationData;
import dev.syslabs.sentio.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for DeviceLocationService.
 * Tests all public methods, edge cases, and error handling.
 * Uses Mockito to isolate the service layer from dependencies.
 *
 * Target: 80%+ code coverage
 *
 * @author Sentio Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceLocationService Unit Tests")
class DeviceLocationServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DeviceLocationService deviceLocationService;

    private Device createDevice(String id, String name, Double lat, Double lon) {
        Device device = new Device();
        device.setId(id);
        device.setName(name);
        device.setLatitude(lat);
        device.setLongitude(lon);
        device.setOwnerId("owner-123");
        device.setCreatedAt(LocalDateTime.now());
        device.setIsPrimary(false);
        return device;
    }

    private Device createDeviceWithPrimary(String id, String name, Double lat, Double lon, boolean isPrimary) {
        Device device = createDevice(id, name, lat, lon);
        device.setIsPrimary(isPrimary);
        return device;
    }

    @Nested
    @DisplayName("getAllUniqueDeviceLocations() Tests")
    class GetAllUniqueDeviceLocationsTests {

        @Test
        @DisplayName("Should return empty list when no devices exist")
        void shouldReturnEmptyListWhenNoDevices() {
            // Arrange
            when(deviceRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<LocationData> result = deviceLocationService.getAllUniqueDeviceLocations();

            // Assert
            assertThat(result).isEmpty();
            verify(deviceRepository).findAll();
        }

        @Test
        @DisplayName("Should return locations for all devices with valid coordinates")
        void shouldReturnLocationsForDevicesWithValidCoordinates() {
            // Arrange
            Device device1 = createDevice("device-1", "Device 1", 52.52, 13.405);
            Device device2 = createDevice("device-2", "Device 2", 48.8566, 2.3522);
            Device device3 = createDevice("device-3", "Device 3", 40.7128, -74.0060);

            when(deviceRepository.findAll()).thenReturn(Arrays.asList(device1, device2, device3));

            // Act
            List<LocationData> result = deviceLocationService.getAllUniqueDeviceLocations();

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).extracting(LocationData::getDeviceId)
                    .containsExactly("device-1", "device-2", "device-3");
            assertThat(result).extracting(LocationData::getLatitude)
                    .containsExactly(52.52f, 48.8566f, 40.7128f);
            assertThat(result).extracting(LocationData::getLongitude)
                    .containsExactly(13.405f, 2.3522f, -74.0060f);
            verify(deviceRepository).findAll();
        }

        @Test
        @DisplayName("Should filter out devices without coordinates")
        void shouldFilterOutDevicesWithoutCoordinates() {
            // Arrange
            Device deviceWithCoords = createDevice("device-1", "Device 1", 52.52, 13.405);
            Device deviceNoLat = createDevice("device-2", "Device 2", null, 13.405);
            Device deviceNoLon = createDevice("device-3", "Device 3", 52.52, null);
            Device deviceNoCoords = createDevice("device-4", "Device 4", null, null);

            when(deviceRepository.findAll())
                    .thenReturn(Arrays.asList(deviceWithCoords, deviceNoLat, deviceNoLon, deviceNoCoords));

            // Act
            List<LocationData> result = deviceLocationService.getAllUniqueDeviceLocations();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeviceId()).isEqualTo("device-1");
        }

        @Test
        @DisplayName("Should set correct metadata for location data")
        void shouldSetCorrectMetadataForLocationData() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", 52.52, 13.405);
            when(deviceRepository.findAll()).thenReturn(Collections.singletonList(device));

            // Act
            List<LocationData> result = deviceLocationService.getAllUniqueDeviceLocations();

            // Assert
            assertThat(result).hasSize(1);
            LocationData locationData = result.get(0);
            assertThat(locationData.getDeviceId()).isEqualTo("device-1");
            assertThat(locationData.getCity()).isEqualTo("GPS Location");
            assertThat(locationData.getCountry()).isEqualTo("Unknown");
            assertThat(locationData.getCreatedAt()).isNotNull();
            assertThat(locationData.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getCurrentUserDeviceLocations() Tests")
    class GetCurrentUserDeviceLocationsTests {

        @Test
        @DisplayName("Should return empty list when user has no devices")
        void shouldReturnEmptyListWhenUserHasNoDevices() {
            // Arrange
            when(deviceService.getMyDevices()).thenReturn(Collections.emptyList());

            // Act
            List<LocationData> result = deviceLocationService.getCurrentUserDeviceLocations();

            // Assert
            assertThat(result).isEmpty();
            verify(deviceService).getMyDevices();
        }

        @Test
        @DisplayName("Should return locations for current user's devices")
        void shouldReturnLocationsForCurrentUserDevices() {
            // Arrange
            Device device1 = createDevice("device-1", "Device 1", 52.52, 13.405);
            Device device2 = createDevice("device-2", "Device 2", 48.8566, 2.3522);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(device1, device2));

            // Act
            List<LocationData> result = deviceLocationService.getCurrentUserDeviceLocations();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(LocationData::getDeviceId)
                    .containsExactly("device-1", "device-2");
        }

        @Test
        @DisplayName("Should filter out user devices without valid coordinates")
        void shouldFilterOutUserDevicesWithoutValidCoordinates() {
            // Arrange
            Device deviceWithCoords = createDevice("device-1", "Device 1", 52.52, 13.405);
            Device deviceNoCoords = createDevice("device-2", "Device 2", null, null);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(deviceWithCoords, deviceNoCoords));

            // Act
            List<LocationData> result = deviceLocationService.getCurrentUserDeviceLocations();

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeviceId()).isEqualTo("device-1");
        }
    }

    @Nested
    @DisplayName("getCurrentUserDevicesWithLocations() Tests")
    class GetCurrentUserDevicesWithLocationsTests {

        @Test
        @DisplayName("Should return empty map when user has no devices")
        void shouldReturnEmptyMapWhenUserHasNoDevices() {
            // Arrange
            when(deviceService.getMyDevices()).thenReturn(Collections.emptyList());

            // Act
            Map<String, LocationData> result = deviceLocationService.getCurrentUserDevicesWithLocations();

            // Assert
            assertThat(result).isEmpty();
            verify(deviceService).getMyDevices();
        }

        @Test
        @DisplayName("Should return map of device IDs to locations")
        void shouldReturnMapOfDeviceIdsToLocations() {
            // Arrange
            Device device1 = createDevice("device-1", "Device 1", 52.52, 13.405);
            Device device2 = createDevice("device-2", "Device 2", 48.8566, 2.3522);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(device1, device2));

            // Act
            Map<String, LocationData> result = deviceLocationService.getCurrentUserDevicesWithLocations();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).containsKeys("device-1", "device-2");
            assertThat(result.get("device-1").getLatitude()).isEqualTo(52.52f);
            assertThat(result.get("device-2").getLatitude()).isEqualTo(48.8566f);
        }

        @Test
        @DisplayName("Should maintain order and exclude devices without coordinates")
        void shouldMaintainOrderAndExcludeDevicesWithoutCoordinates() {
            // Arrange
            Device device1 = createDevice("device-1", "Device 1", 52.52, 13.405);
            Device device2 = createDevice("device-2", "Device 2", null, null);
            Device device3 = createDevice("device-3", "Device 3", 48.8566, 2.3522);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(device1, device2, device3));

            // Act
            Map<String, LocationData> result = deviceLocationService.getCurrentUserDevicesWithLocations();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).containsKeys("device-1", "device-3");
            assertThat(result).doesNotContainKey("device-2");
            // Verify order is maintained (LinkedHashMap)
            assertThat(result.keySet()).containsExactly("device-1", "device-3");
        }
    }

    @Nested
    @DisplayName("getDeviceLocation() Tests")
    class GetDeviceLocationTests {

        @Test
        @DisplayName("Should return empty when device not found")
        void shouldReturnEmptyWhenDeviceNotFound() {
            // Arrange
            when(deviceRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act
            Optional<LocationData> result = deviceLocationService.getDeviceLocation("non-existent");

            // Assert
            assertThat(result).isEmpty();
            verify(deviceRepository).findById("non-existent");
        }

        @Test
        @DisplayName("Should return location data for existing device with coordinates")
        void shouldReturnLocationDataForExistingDevice() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", 52.52, 13.405);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));

            // Act
            Optional<LocationData> result = deviceLocationService.getDeviceLocation("device-1");

            // Assert
            assertThat(result).isPresent();
            LocationData locationData = result.get();
            assertThat(locationData.getDeviceId()).isEqualTo("device-1");
            assertThat(locationData.getLatitude()).isEqualTo(52.52f);
            assertThat(locationData.getLongitude()).isEqualTo(13.405f);
        }

        @Test
        @DisplayName("Should return empty when device has no coordinates")
        void shouldReturnEmptyWhenDeviceHasNoCoordinates() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", null, null);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));

            // Act
            Optional<LocationData> result = deviceLocationService.getDeviceLocation("device-1");

            // Assert
            assertThat(result).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("provideInvalidCoordinateCombinations")
        @DisplayName("Should return empty for devices with partial coordinates")
        void shouldReturnEmptyForDevicesWithPartialCoordinates(Double lat, Double lon) {
            // Arrange
            Device device = createDevice("device-1", "Device 1", lat, lon);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));

            // Act
            Optional<LocationData> result = deviceLocationService.getDeviceLocation("device-1");

            // Assert
            assertThat(result).isEmpty();
        }

        static Stream<Arguments> provideInvalidCoordinateCombinations() {
            return Stream.of(
                    Arguments.of(null, 13.405),
                    Arguments.of(52.52, null),
                    Arguments.of(null, null)
            );
        }
    }

    @Nested
    @DisplayName("getPrimaryUserDeviceLocation() Tests")
    class GetPrimaryUserDeviceLocationTests {

        @Test
        @DisplayName("Should return empty when user has no devices")
        void shouldReturnEmptyWhenUserHasNoDevices() {
            // Arrange
            when(deviceService.getMyDevices()).thenReturn(Collections.emptyList());

            // Act
            Optional<LocationData> result = deviceLocationService.getPrimaryUserDeviceLocation();

            // Assert
            assertThat(result).isEmpty();
            verify(deviceService).getMyDevices();
        }

        @Test
        @DisplayName("Should return primary device when explicitly set")
        void shouldReturnPrimaryDeviceWhenExplicitlySet() {
            // Arrange
            Device device1 = createDeviceWithPrimary("device-1", "Device 1", 52.52, 13.405, false);
            Device device2 = createDeviceWithPrimary("device-2", "Device 2", 48.8566, 2.3522, true);
            Device device3 = createDeviceWithPrimary("device-3", "Device 3", 40.7128, -74.0060, false);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(device1, device2, device3));

            // Act
            Optional<LocationData> result = deviceLocationService.getPrimaryUserDeviceLocation();

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getDeviceId()).isEqualTo("device-2");
            assertThat(result.get().getLatitude()).isEqualTo(48.8566f);
        }

        @Test
        @DisplayName("Should fallback to first device when no primary set")
        void shouldFallbackToFirstDeviceWhenNoPrimarySet() {
            // Arrange
            Device device1 = createDeviceWithPrimary("device-1", "Device 1", 52.52, 13.405, false);
            Device device2 = createDeviceWithPrimary("device-2", "Device 2", 48.8566, 2.3522, false);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(device1, device2));

            // Act
            Optional<LocationData> result = deviceLocationService.getPrimaryUserDeviceLocation();

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getDeviceId()).isEqualTo("device-1");
        }

        @Test
        @DisplayName("Should return empty when primary device has no coordinates")
        void shouldReturnEmptyWhenPrimaryDeviceHasNoCoordinates() {
            // Arrange
            Device device1 = createDeviceWithPrimary("device-1", "Device 1", null, null, true);

            when(deviceService.getMyDevices()).thenReturn(Collections.singletonList(device1));

            // Act
            Optional<LocationData> result = deviceLocationService.getPrimaryUserDeviceLocation();

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should skip primary without coords and fallback to first with coords")
        void shouldSkipPrimaryWithoutCoordsAndFallbackToFirstWithCoords() {
            // Arrange
            Device devicePrimaryNoCoords = createDeviceWithPrimary("device-1", "Device 1", null, null, true);
            Device deviceWithCoords = createDeviceWithPrimary("device-2", "Device 2", 48.8566, 2.3522, false);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(devicePrimaryNoCoords, deviceWithCoords));

            // Act
            Optional<LocationData> result = deviceLocationService.getPrimaryUserDeviceLocation();

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getDeviceId()).isEqualTo("device-2");
        }

        @Test
        @DisplayName("Should handle isPrimary being null as false")
        void shouldHandleIsPrimaryBeingNullAsFalse() {
            // Arrange
            Device device1 = createDevice("device-1", "Device 1", 52.52, 13.405);
            device1.setIsPrimary(null);
            Device device2 = createDevice("device-2", "Device 2", 48.8566, 2.3522);
            device2.setIsPrimary(null);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(device1, device2));

            // Act
            Optional<LocationData> result = deviceLocationService.getPrimaryUserDeviceLocation();

            // Assert
            assertThat(result).isPresent();
            // Should fallback to first device with coords
            assertThat(result.get().getDeviceId()).isEqualTo("device-1");
        }
    }

    @Nested
    @DisplayName("getFirstUserDeviceLocation() Tests - Deprecated")
    class GetFirstUserDeviceLocationTests {

        @Test
        @DisplayName("Should delegate to getPrimaryUserDeviceLocation()")
        void shouldDelegateToGetPrimaryUserDeviceLocation() {
            // Arrange
            Device device = createDeviceWithPrimary("device-1", "Device 1", 52.52, 13.405, true);
            when(deviceService.getMyDevices()).thenReturn(Collections.singletonList(device));

            // Act
            Optional<LocationData> result = deviceLocationService.getFirstUserDeviceLocation();

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getDeviceId()).isEqualTo("device-1");
        }
    }

    @Nested
    @DisplayName("updateDeviceGpsLocation() Tests")
    class UpdateDeviceGpsLocationTests {

        @Test
        @DisplayName("Should return false when device not found")
        void shouldReturnFalseWhenDeviceNotFound() {
            // Arrange
            when(deviceRepository.findById("non-existent")).thenReturn(Optional.empty());

            // Act
            boolean result = deviceLocationService.updateDeviceGpsLocation("non-existent", 52.52, 13.405);

            // Assert
            assertThat(result).isFalse();
            verify(deviceRepository).findById("non-existent");
            verify(deviceRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should update device coordinates and publish event for first location")
        void shouldUpdateDeviceCoordinatesAndPublishEventForFirstLocation() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", null, null);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = deviceLocationService.updateDeviceGpsLocation("device-1", 52.52, 13.405);

            // Assert
            assertThat(result).isTrue();
            assertThat(device.getLatitude()).isEqualTo(52.52);
            assertThat(device.getLongitude()).isEqualTo(13.405);

            ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
            verify(deviceRepository).save(deviceCaptor.capture());
            Device savedDevice = deviceCaptor.getValue();
            assertThat(savedDevice.getLatitude()).isEqualTo(52.52);
            assertThat(savedDevice.getLongitude()).isEqualTo(13.405);

            ArgumentCaptor<DeviceLocationUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeviceLocationUpdatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            DeviceLocationUpdatedEvent event = eventCaptor.getValue();
            assertThat(event.getDeviceId()).isEqualTo("device-1");
            assertThat(event.getLatitude()).isEqualTo(52.52);
            assertThat(event.getLongitude()).isEqualTo(13.405);
            assertThat(event.isFirstLocation()).isTrue();
        }

        @Test
        @DisplayName("Should update device coordinates and publish event for location update")
        void shouldUpdateDeviceCoordinatesAndPublishEventForLocationUpdate() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", 48.8566, 2.3522);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = deviceLocationService.updateDeviceGpsLocation("device-1", 52.52, 13.405);

            // Assert
            assertThat(result).isTrue();
            assertThat(device.getLatitude()).isEqualTo(52.52);
            assertThat(device.getLongitude()).isEqualTo(13.405);

            ArgumentCaptor<DeviceLocationUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeviceLocationUpdatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            DeviceLocationUpdatedEvent event = eventCaptor.getValue();
            assertThat(event.isFirstLocation()).isFalse();
        }

        @Test
        @DisplayName("Should handle null latitude in existing device")
        void shouldHandleNullLatitudeInExistingDevice() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", null, 13.405);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = deviceLocationService.updateDeviceGpsLocation("device-1", 52.52, 13.405);

            // Assert
            assertThat(result).isTrue();
            ArgumentCaptor<DeviceLocationUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeviceLocationUpdatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().isFirstLocation()).isTrue();
        }

        @Test
        @DisplayName("Should handle null longitude in existing device")
        void shouldHandleNullLongitudeInExistingDevice() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", 52.52, null);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = deviceLocationService.updateDeviceGpsLocation("device-1", 52.52, 13.405);

            // Assert
            assertThat(result).isTrue();
            ArgumentCaptor<DeviceLocationUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(DeviceLocationUpdatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().isFirstLocation()).isTrue();
        }

        @ParameterizedTest
        @MethodSource("provideValidCoordinates")
        @DisplayName("Should accept various valid coordinate values")
        void shouldAcceptVariousValidCoordinateValues(Double lat, Double lon) {
            // Arrange
            Device device = createDevice("device-1", "Device 1", null, null);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = deviceLocationService.updateDeviceGpsLocation("device-1", lat, lon);

            // Assert
            assertThat(result).isTrue();
            assertThat(device.getLatitude()).isEqualTo(lat);
            assertThat(device.getLongitude()).isEqualTo(lon);
        }

        static Stream<Arguments> provideValidCoordinates() {
            return Stream.of(
                    Arguments.of(0.0, 0.0),           // Equator and Prime Meridian
                    Arguments.of(90.0, 180.0),        // North Pole, Date Line
                    Arguments.of(-90.0, -180.0),      // South Pole, Date Line
                    Arguments.of(52.5200, 13.4050),   // Berlin
                    Arguments.of(-33.8688, 151.2093), // Sydney
                    Arguments.of(40.7128, -74.0060)   // New York
            );
        }

        @Test
        @DisplayName("Should verify event publisher is called exactly once")
        void shouldVerifyEventPublisherIsCalledExactlyOnce() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", null, null);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            deviceLocationService.updateDeviceGpsLocation("device-1", 52.52, 13.405);

            // Assert
            verify(eventPublisher, times(1)).publishEvent(any(DeviceLocationUpdatedEvent.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle empty device list in multiple methods")
        void shouldHandleEmptyDeviceListInMultipleMethods() {
            // Arrange
            when(deviceRepository.findAll()).thenReturn(Collections.emptyList());
            when(deviceService.getMyDevices()).thenReturn(Collections.emptyList());

            // Act & Assert
            assertThat(deviceLocationService.getAllUniqueDeviceLocations()).isEmpty();
            assertThat(deviceLocationService.getCurrentUserDeviceLocations()).isEmpty();
            assertThat(deviceLocationService.getCurrentUserDevicesWithLocations()).isEmpty();
            assertThat(deviceLocationService.getPrimaryUserDeviceLocation()).isEmpty();
        }

        @Test
        @DisplayName("Should handle large number of devices")
        void shouldHandleLargeNumberOfDevices() {
            // Arrange
            List<Device> devices = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                devices.add(createDevice("device-" + i, "Device " + i, 52.52 + i * 0.001, 13.405 + i * 0.001));
            }
            when(deviceRepository.findAll()).thenReturn(devices);

            // Act
            List<LocationData> result = deviceLocationService.getAllUniqueDeviceLocations();

            // Assert
            assertThat(result).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle devices with extreme coordinate values")
        void shouldHandleDevicesWithExtremeCoordinateValues() {
            // Arrange
            Device device1 = createDevice("device-1", "North Pole", 90.0, 0.0);
            Device device2 = createDevice("device-2", "South Pole", -90.0, 0.0);
            Device device3 = createDevice("device-3", "Date Line", 0.0, 180.0);

            when(deviceRepository.findAll()).thenReturn(Arrays.asList(device1, device2, device3));

            // Act
            List<LocationData> result = deviceLocationService.getAllUniqueDeviceLocations();

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).extracting(LocationData::getLatitude)
                    .containsExactly(90.0f, -90.0f, 0.0f);
        }

        @Test
        @DisplayName("Should convert Double coordinates to Float in LocationData")
        void shouldConvertDoubleCoordinatesToFloatInLocationData() {
            // Arrange
            Device device = createDevice("device-1", "Device 1", 52.52000123456789, 13.40500123456789);
            when(deviceRepository.findById("device-1")).thenReturn(Optional.of(device));

            // Act
            Optional<LocationData> result = deviceLocationService.getDeviceLocation("device-1");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getLatitude()).isInstanceOf(Float.class);
            assertThat(result.get().getLongitude()).isInstanceOf(Float.class);
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid devices")
        void shouldHandleMixedValidAndInvalidDevices() {
            // Arrange
            Device validDevice1 = createDevice("device-1", "Valid 1", 52.52, 13.405);
            Device invalidDevice1 = createDevice("device-2", "Invalid 1", null, null);
            Device validDevice2 = createDevice("device-3", "Valid 2", 48.8566, 2.3522);
            Device invalidDevice2 = createDevice("device-4", "Invalid 2", 52.52, null);

            when(deviceRepository.findAll())
                    .thenReturn(Arrays.asList(validDevice1, invalidDevice1, validDevice2, invalidDevice2));

            // Act
            List<LocationData> result = deviceLocationService.getAllUniqueDeviceLocations();

            // Assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(LocationData::getDeviceId)
                    .containsExactly("device-1", "device-3");
        }
    }

    @Nested
    @DisplayName("Integration between methods")
    class IntegrationBetweenMethodsTests {

        @Test
        @DisplayName("Should maintain consistency across different query methods")
        void shouldMaintainConsistencyAcrossDifferentQueryMethods() {
            // Arrange
            Device device1 = createDevice("device-1", "Device 1", 52.52, 13.405);
            Device device2 = createDevice("device-2", "Device 2", 48.8566, 2.3522);

            when(deviceService.getMyDevices()).thenReturn(Arrays.asList(device1, device2));

            // Act
            List<LocationData> listResult = deviceLocationService.getCurrentUserDeviceLocations();
            Map<String, LocationData> mapResult = deviceLocationService.getCurrentUserDevicesWithLocations();

            // Assert
            assertThat(listResult).hasSize(2);
            assertThat(mapResult).hasSize(2);
            assertThat(listResult).extracting(LocationData::getDeviceId)
                    .containsExactlyElementsOf(mapResult.keySet());
        }
    }
}
