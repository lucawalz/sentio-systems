package org.example.backend.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.example.backend.service.DeviceLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeviceStatusHandler}.
 * Tests device status updates, IP tracking, service management, and GPS location handling.
 */
@ExtendWith(MockitoExtension.class)
class DeviceStatusHandlerTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceLocationService deviceLocationService;

    private DeviceStatusHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeviceStatusHandler(deviceRepository, deviceLocationService);
    }

    @Nested
    @DisplayName("processStatusUpdate - valid payloads")
    class ValidPayloadTests {

        @Test
        @DisplayName("should update device IP address when changed")
        void shouldUpdateDeviceIpAddress() {
            // Given
            String payload = """
                    {
                        "device_id": "device-001",
                        "ip": "192.168.1.100"
                    }
                    """;

            Device device = new Device();
            device.setId("device-001");
            device.setIpAddress("192.168.1.50");
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-001")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
            verify(deviceRepository).save(captor.capture());

            assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.1.100");
            assertThat(captor.getValue().getLastSeen()).isNotNull();
        }

        @Test
        @DisplayName("should not update when IP address unchanged")
        void shouldNotUpdateWhenIpUnchanged() {
            // Given
            String payload = """
                    {
                        "device_id": "device-002",
                        "ip": "192.168.1.100"
                    }
                    """;

            Device device = new Device();
            device.setId("device-002");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>());
            device.setLastSeen(LocalDateTime.now().minusSeconds(30));

            when(deviceRepository.findById("device-002")).thenReturn(Optional.of(device));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update device with services array")
        void shouldUpdateDeviceWithServicesArray() {
            // Given
            String payload = """
                    {
                        "device_id": "device-003",
                        "ip": "192.168.1.100",
                        "services": ["animal_detector", "weather_station"]
                    }
                    """;

            Device device = new Device();
            device.setId("device-003");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>(Set.of("old_service")));

            when(deviceRepository.findById("device-003")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
            verify(deviceRepository).save(captor.capture());

            assertThat(captor.getValue().getActiveServices())
                    .hasSize(2)
                    .contains("animal_detector", "weather_station")
                    .doesNotContain("old_service");
        }

        @Test
        @DisplayName("should add single service in legacy format")
        void shouldAddSingleServiceLegacyFormat() {
            // Given
            String payload = """
                    {
                        "device_id": "device-004",
                        "ip": "192.168.1.100",
                        "service": "weather_station"
                    }
                    """;

            Device device = new Device();
            device.setId("device-004");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-004")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
            verify(deviceRepository).save(captor.capture());

            assertThat(captor.getValue().getActiveServices())
                    .hasSize(1)
                    .contains("weather_station");
        }

        @Test
        @DisplayName("should not duplicate service in legacy format")
        void shouldNotDuplicateServiceInLegacyFormat() {
            // Given
            String payload = """
                    {
                        "device_id": "device-005",
                        "ip": "192.168.1.100",
                        "service": "weather_station"
                    }
                    """;

            Device device = new Device();
            device.setId("device-005");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>(Set.of("weather_station")));
            device.setLastSeen(LocalDateTime.now().minusSeconds(30));

            when(deviceRepository.findById("device-005")).thenReturn(Optional.of(device));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should update GPS coordinates when changed significantly")
        void shouldUpdateGpsCoordinatesWhenChangedSignificantly() {
            // Given
            String payload = """
                    {
                        "device_id": "device-006",
                        "ip": "192.168.1.100",
                        "latitude": 52.5200,
                        "longitude": 13.4050
                    }
                    """;

            Device device = new Device();
            device.setId("device-006");
            device.setIpAddress("192.168.1.100");
            device.setLatitude(52.5100);
            device.setLongitude(13.4000);
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-006")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceLocationService).updateDeviceGpsLocation("device-006", 52.5200, 13.4050);
        }

        @Test
        @DisplayName("should not update GPS when change is within tolerance")
        void shouldNotUpdateGpsWhenWithinTolerance() {
            // Given
            String payload = """
                    {
                        "device_id": "device-007",
                        "ip": "192.168.1.100",
                        "latitude": 52.5100,
                        "longitude": 13.4000
                    }
                    """;

            Device device = new Device();
            device.setId("device-007");
            device.setIpAddress("192.168.1.100");
            device.setLatitude(52.5100);
            device.setLongitude(13.4000);
            device.setActiveServices(new HashSet<>());
            device.setLastSeen(LocalDateTime.now().minusSeconds(30));

            when(deviceRepository.findById("device-007")).thenReturn(Optional.of(device));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceLocationService, never()).updateDeviceGpsLocation(anyString(), any(Double.class), any(Double.class));
        }

        @Test
        @DisplayName("should update GPS when device has no previous location")
        void shouldUpdateGpsWhenNoPreviousLocation() {
            // Given
            String payload = """
                    {
                        "device_id": "device-008",
                        "ip": "192.168.1.100",
                        "latitude": 48.8566,
                        "longitude": 2.3522
                    }
                    """;

            Device device = new Device();
            device.setId("device-008");
            device.setIpAddress("192.168.1.100");
            device.setLatitude(null);
            device.setLongitude(null);
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-008")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceLocationService).updateDeviceGpsLocation("device-008", 48.8566, 2.3522);
        }

        @Test
        @DisplayName("should update when lastSeen is old even if no changes")
        void shouldUpdateWhenLastSeenIsOld() {
            // Given
            String payload = """
                    {
                        "device_id": "device-009",
                        "ip": "192.168.1.100"
                    }
                    """;

            Device device = new Device();
            device.setId("device-009");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>());
            device.setLastSeen(LocalDateTime.now().minusSeconds(120));

            when(deviceRepository.findById("device-009")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository).save(any());
        }

        @Test
        @DisplayName("should update when lastSeen is null")
        void shouldUpdateWhenLastSeenIsNull() {
            // Given
            String payload = """
                    {
                        "device_id": "device-010",
                        "ip": "192.168.1.100"
                    }
                    """;

            Device device = new Device();
            device.setId("device-010");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>());
            device.setLastSeen(null);

            when(deviceRepository.findById("device-010")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository).save(any());
        }

        @Test
        @DisplayName("should set IP when device has null IP")
        void shouldSetIpWhenDeviceHasNullIp() {
            // Given
            String payload = """
                    {
                        "device_id": "device-011",
                        "ip": "192.168.1.100"
                    }
                    """;

            Device device = new Device();
            device.setId("device-011");
            device.setIpAddress(null);
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-011")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
            verify(deviceRepository).save(captor.capture());

            assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.1.100");
        }
    }

    @Nested
    @DisplayName("processStatusUpdate - edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should ignore status for non-existent device")
        void shouldIgnoreNonExistentDevice() {
            // Given
            String payload = """
                    {
                        "device_id": "device-999",
                        "ip": "192.168.1.100"
                    }
                    """;

            when(deviceRepository.findById("device-999")).thenReturn(Optional.empty());

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository, never()).save(any());
            verify(deviceLocationService, never()).updateDeviceGpsLocation(anyString(), any(Double.class), any(Double.class));
        }

        @Test
        @DisplayName("should handle empty services array")
        void shouldHandleEmptyServicesArray() {
            // Given
            String payload = """
                    {
                        "device_id": "device-012",
                        "ip": "192.168.1.100",
                        "services": []
                    }
                    """;

            Device device = new Device();
            device.setId("device-012");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>(Set.of("old_service")));

            when(deviceRepository.findById("device-012")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            ArgumentCaptor<Device> captor = ArgumentCaptor.forClass(Device.class);
            verify(deviceRepository).save(captor.capture());

            assertThat(captor.getValue().getActiveServices()).isEmpty();
        }

        @Test
        @DisplayName("should handle GPS coordinates at boundaries")
        void shouldHandleGpsBoundaryCoordinates() {
            // Given
            String payload = """
                    {
                        "device_id": "device-013",
                        "ip": "192.168.1.100",
                        "latitude": 90.0,
                        "longitude": 180.0
                    }
                    """;

            Device device = new Device();
            device.setId("device-013");
            device.setIpAddress("192.168.1.100");
            device.setLatitude(null);
            device.setLongitude(null);
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-013")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceLocationService).updateDeviceGpsLocation("device-013", 90.0, 180.0);
        }

        @Test
        @DisplayName("should handle negative GPS coordinates")
        void shouldHandleNegativeGpsCoordinates() {
            // Given
            String payload = """
                    {
                        "device_id": "device-014",
                        "ip": "192.168.1.100",
                        "latitude": -33.8688,
                        "longitude": -151.2093
                    }
                    """;

            Device device = new Device();
            device.setId("device-014");
            device.setIpAddress("192.168.1.100");
            device.setLatitude(null);
            device.setLongitude(null);
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-014")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceLocationService).updateDeviceGpsLocation("device-014", -33.8688, -151.2093);
        }

        @Test
        @DisplayName("should handle only latitude provided")
        void shouldHandleOnlyLatitudeProvided() {
            // Given
            String payload = """
                    {
                        "device_id": "device-015",
                        "ip": "192.168.1.100",
                        "latitude": 52.5200
                    }
                    """;

            Device device = new Device();
            device.setId("device-015");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>());
            device.setLastSeen(LocalDateTime.now().minusSeconds(30));

            when(deviceRepository.findById("device-015")).thenReturn(Optional.of(device));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceLocationService, never()).updateDeviceGpsLocation(anyString(), any(Double.class), any(Double.class));
        }

        @Test
        @DisplayName("should handle only longitude provided")
        void shouldHandleOnlyLongitudeProvided() {
            // Given
            String payload = """
                    {
                        "device_id": "device-016",
                        "ip": "192.168.1.100",
                        "longitude": 13.4050
                    }
                    """;

            Device device = new Device();
            device.setId("device-016");
            device.setIpAddress("192.168.1.100");
            device.setActiveServices(new HashSet<>());
            device.setLastSeen(LocalDateTime.now().minusSeconds(30));

            when(deviceRepository.findById("device-016")).thenReturn(Optional.of(device));

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceLocationService, never()).updateDeviceGpsLocation(anyString(), any(Double.class), any(Double.class));
        }

        @Test
        @DisplayName("should handle GPS change at or above threshold")
        void shouldHandleGpsChangeAtThreshold() {
            // Given - 0.0005 degrees is the threshold, use 0.001 to ensure it triggers
            String payload = """
                    {
                        "device_id": "device-017",
                        "ip": "192.168.1.100",
                        "latitude": 52.5100,
                        "longitude": 13.4010
                    }
                    """;

            Device device = new Device();
            device.setId("device-017");
            device.setIpAddress("192.168.1.100");
            device.setLatitude(52.5100);
            device.setLongitude(13.4000);
            device.setActiveServices(new HashSet<>());
            device.setLastSeen(LocalDateTime.now().minusMinutes(5)); // Set lastSeen to avoid throttling

            when(deviceRepository.findById("device-017")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            handler.processStatusUpdate(payload);

            // Then - 0.001 difference is above threshold (0.0005) so should trigger update
            verify(deviceLocationService).updateDeviceGpsLocation(eq("device-017"), eq(52.5100), eq(13.4010));
        }
    }

    @Nested
    @DisplayName("processStatusUpdate - validation")
    class ValidationTests {

        @Test
        @DisplayName("should reject payload missing device_id")
        void shouldRejectPayloadMissingDeviceId() {
            // Given
            String payload = """
                    {
                        "ip": "192.168.1.100"
                    }
                    """;

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository, never()).findById(anyString());
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject payload missing ip")
        void shouldRejectPayloadMissingIp() {
            // Given
            String payload = """
                    {
                        "device_id": "device-018"
                    }
                    """;

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository, never()).findById(anyString());
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject payload with null device_id")
        void shouldRejectPayloadWithNullDeviceId() {
            // Given
            String payload = """
                    {
                        "device_id": null,
                        "ip": "192.168.1.100"
                    }
                    """;

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository, never()).findById(anyString());
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject payload with null ip")
        void shouldRejectPayloadWithNullIp() {
            // Given
            String payload = """
                    {
                        "device_id": "device-019",
                        "ip": null
                    }
                    """;

            // When
            handler.processStatusUpdate(payload);

            // Then
            verify(deviceRepository, never()).findById(anyString());
            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle empty device_id string")
        void shouldHandleEmptyDeviceIdString() {
            // Given
            String payload = """
                    {
                        "device_id": "",
                        "ip": "192.168.1.100"
                    }
                    """;

            // When
            handler.processStatusUpdate(payload);

            // Then - empty device_id should be rejected before repository call
            verify(deviceRepository, never()).findById(anyString());
            verify(deviceRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processStatusUpdate - error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle malformed JSON gracefully")
        void shouldHandleMalformedJson() {
            // Given
            String payload = "{ invalid json }{";

            // When/Then
            assertThatCode(() -> handler.processStatusUpdate(payload))
                    .doesNotThrowAnyException();

            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle null payload")
        void shouldHandleNullPayload() {
            // When/Then
            assertThatCode(() -> handler.processStatusUpdate(null))
                    .doesNotThrowAnyException();

            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle empty payload")
        void shouldHandleEmptyPayload() {
            // When/Then
            assertThatCode(() -> handler.processStatusUpdate(""))
                    .doesNotThrowAnyException();

            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle repository exception")
        void shouldHandleRepositoryException() {
            // Given
            String payload = """
                    {
                        "device_id": "device-020",
                        "ip": "192.168.1.100"
                    }
                    """;

            when(deviceRepository.findById("device-020"))
                    .thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatCode(() -> handler.processStatusUpdate(payload))
                    .doesNotThrowAnyException();

            verify(deviceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle deviceLocationService exception")
        void shouldHandleDeviceLocationServiceException() {
            // Given
            String payload = """
                    {
                        "device_id": "device-021",
                        "ip": "192.168.1.100",
                        "latitude": 52.5200,
                        "longitude": 13.4050
                    }
                    """;

            Device device = new Device();
            device.setId("device-021");
            device.setIpAddress("192.168.1.100");
            device.setLatitude(null);
            device.setLongitude(null);
            device.setActiveServices(new HashSet<>());

            when(deviceRepository.findById("device-021")).thenReturn(Optional.of(device));
            when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("Location service error"))
                    .when(deviceLocationService).updateDeviceGpsLocation(anyString(), any(Double.class), any(Double.class));

            // When/Then
            assertThatCode(() -> handler.processStatusUpdate(payload))
                    .doesNotThrowAnyException();

            // Should still save device despite location service failure
            verify(deviceRepository).save(any());
        }

        @Test
        @DisplayName("should handle non-JSON payload")
        void shouldHandleNonJsonPayload() {
            // Given
            String payload = "This is not JSON at all!";

            // When/Then
            assertThatCode(() -> handler.processStatusUpdate(payload))
                    .doesNotThrowAnyException();

            verify(deviceRepository, never()).save(any());
        }
    }
}
