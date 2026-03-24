package dev.syslabs.sentio.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.syslabs.sentio.model.RaspiWeatherData;
import dev.syslabs.sentio.service.RaspiWeatherDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RaspiWeatherDataHandler}.
 * Tests weather data parsing, validation, and persistence.
 */
@ExtendWith(MockitoExtension.class)
class RaspiWeatherDataHandlerTest {

    @Mock
    private RaspiWeatherDataService raspiWeatherDataService;

    private RaspiWeatherDataHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RaspiWeatherDataHandler(raspiWeatherDataService);
    }

    @Nested
    @DisplayName("processWeatherData - valid payloads")
    class ValidPayloadTests {

        @Test
        @DisplayName("should process complete weather data payload")
        void shouldProcessCompleteWeatherData() {
            // Given
            LocalDateTime timestamp = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            String payload = String.format("""
                    {
                        "device_id": "weather-001",
                        "location": "Garden Station",
                        "timestamp": "%s",
                        "temperature": 22.5,
                        "humidity": 65.3,
                        "pressure": 1013.25,
                        "lux": 45000.0,
                        "uvi": 5.2,
                        "gas_resistance": 250000
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(1L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When
            handler.processWeatherData(payload);

            // Then
            ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
            verify(raspiWeatherDataService).saveWeatherData(captor.capture());

            RaspiWeatherData captured = captor.getValue();
            assertThat(captured.getDeviceId()).isEqualTo("weather-001");
            assertThat(captured.getLocation()).isEqualTo("Garden Station");
            assertThat(captured.getTimestamp()).isEqualTo(timestamp);
            assertThat(captured.getTemperature()).isEqualTo(22.5f);
            assertThat(captured.getHumidity()).isEqualTo(65.3f);
            assertThat(captured.getPressure()).isEqualTo(1013.25f);
            assertThat(captured.getLux()).isEqualTo(45000.0f);
            assertThat(captured.getUvi()).isEqualTo(5.2f);
            assertThat(captured.getGasResistance()).isEqualTo(250000);
        }

        @Test
        @DisplayName("should process weather data without gas resistance")
        void shouldProcessWeatherDataWithoutGasResistance() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-002",
                        "location": "Roof Station",
                        "timestamp": "%s",
                        "temperature": 18.7,
                        "humidity": 72.1,
                        "pressure": 1015.5,
                        "lux": 12000.0,
                        "uvi": 2.8
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(2L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When
            handler.processWeatherData(payload);

            // Then
            ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
            verify(raspiWeatherDataService).saveWeatherData(captor.capture());

            assertThat(captor.getValue().getGasResistance()).isNull();
        }

        @Test
        @DisplayName("should process weather data with null gas resistance")
        void shouldProcessWeatherDataWithNullGasResistance() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-003",
                        "location": "Balcony",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0,
                        "gas_resistance": null
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(3L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When
            handler.processWeatherData(payload);

            // Then
            ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
            verify(raspiWeatherDataService).saveWeatherData(captor.capture());

            assertThat(captor.getValue().getGasResistance()).isNull();
        }

        @Test
        @DisplayName("should handle extreme temperature values")
        void shouldHandleExtremeTemperatures() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String[] payloads = {
                    String.format("""
                            {
                                "device_id": "weather-004",
                                "location": "Arctic",
                                "timestamp": "%s",
                                "temperature": -40.0,
                                "humidity": 10.0,
                                "pressure": 1030.0,
                                "lux": 5000.0,
                                "uvi": 1.0
                            }
                            """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                    String.format("""
                            {
                                "device_id": "weather-005",
                                "location": "Desert",
                                "timestamp": "%s",
                                "temperature": 50.0,
                                "humidity": 5.0,
                                "pressure": 990.0,
                                "lux": 100000.0,
                                "uvi": 11.0
                            }
                            """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            };

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(1L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When/Then
            for (String payload : payloads) {
                handler.processWeatherData(payload);
            }

            verify(raspiWeatherDataService, times(2)).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle zero and boundary values")
        void shouldHandleZeroAndBoundaryValues() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-006",
                        "location": "Test Station",
                        "timestamp": "%s",
                        "temperature": 0.0,
                        "humidity": 0.0,
                        "pressure": 0.0,
                        "lux": 0.0,
                        "uvi": 0.0,
                        "gas_resistance": 0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(6L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When
            handler.processWeatherData(payload);

            // Then
            ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
            verify(raspiWeatherDataService).saveWeatherData(captor.capture());

            RaspiWeatherData captured = captor.getValue();
            assertThat(captured.getTemperature()).isZero();
            assertThat(captured.getHumidity()).isZero();
            assertThat(captured.getPressure()).isZero();
            assertThat(captured.getLux()).isZero();
            assertThat(captured.getUvi()).isZero();
            assertThat(captured.getGasResistance()).isZero();
        }

        @Test
        @DisplayName("should handle decimal precision")
        void shouldHandleDecimalPrecision() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-007",
                        "location": "Precision Station",
                        "timestamp": "%s",
                        "temperature": 22.123456,
                        "humidity": 65.789012,
                        "pressure": 1013.456789,
                        "lux": 45678.901234,
                        "uvi": 5.234567
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(7L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When
            handler.processWeatherData(payload);

            // Then
            ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
            verify(raspiWeatherDataService).saveWeatherData(captor.capture());

            RaspiWeatherData captured = captor.getValue();
            assertThat(captured.getTemperature()).isEqualTo(22.123456f);
            assertThat(captured.getHumidity()).isEqualTo(65.789012f);
        }

        @Test
        @DisplayName("should handle different location names")
        void shouldHandleDifferentLocationNames() {
            // Given
            String[] locations = {
                    "Garden",
                    "Roof-Top Station #1",
                    "Location with spaces and Special-Chars!@#",
                    "位置", // Unicode characters
                    ""
            };

            LocalDateTime timestamp = LocalDateTime.now();

            for (String location : locations) {
                String payload = String.format("""
                        {
                            "device_id": "weather-008",
                            "location": "%s",
                            "timestamp": "%s",
                            "temperature": 20.0,
                            "humidity": 50.0,
                            "pressure": 1000.0,
                            "lux": 30000.0,
                            "uvi": 4.0
                        }
                        """, location, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

                RaspiWeatherData savedData = new RaspiWeatherData();
                savedData.setId(8L);

                when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

                // When
                handler.processWeatherData(payload);

                // Then
                ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
                verify(raspiWeatherDataService, atLeastOnce()).saveWeatherData(captor.capture());

                assertThat(captor.getValue().getLocation()).isEqualTo(location);
            }
        }
    }

    @Nested
    @DisplayName("processWeatherData - validation")
    class ValidationTests {

        @Test
        @DisplayName("should reject payload missing device_id")
        void shouldRejectPayloadMissingDeviceId() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload missing location")
        void shouldRejectPayloadMissingLocation() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-009",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload missing timestamp")
        void shouldRejectPayloadMissingTimestamp() {
            // Given
            String payload = """
                    {
                        "device_id": "weather-010",
                        "location": "Test",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """;

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload missing temperature")
        void shouldRejectPayloadMissingTemperature() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-011",
                        "location": "Test",
                        "timestamp": "%s",
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload missing humidity")
        void shouldRejectPayloadMissingHumidity() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-012",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload missing pressure")
        void shouldRejectPayloadMissingPressure() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-013",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload missing lux")
        void shouldRejectPayloadMissingLux() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-014",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload missing uvi")
        void shouldRejectPayloadMissingUvi() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-015",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload with null device_id")
        void shouldRejectPayloadWithNullDeviceId() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": null,
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject payload with null required fields")
        void shouldRejectPayloadWithNullRequiredFields() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-016",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": null,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }
    }

    @Nested
    @DisplayName("processWeatherData - timestamp handling")
    class TimestampHandlingTests {

        @Test
        @DisplayName("should parse valid ISO_LOCAL_DATE_TIME timestamp")
        void shouldParseValidIsoLocalDateTime() {
            // Given
            LocalDateTime expectedTimestamp = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
            String payload = String.format("""
                    {
                        "device_id": "weather-017",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, expectedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(17L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When
            handler.processWeatherData(payload);

            // Then
            ArgumentCaptor<RaspiWeatherData> captor = ArgumentCaptor.forClass(RaspiWeatherData.class);
            verify(raspiWeatherDataService).saveWeatherData(captor.capture());

            assertThat(captor.getValue().getTimestamp()).isEqualTo(expectedTimestamp);
        }

        @Test
        @DisplayName("should reject malformed timestamp")
        void shouldRejectMalformedTimestamp() {
            // Given
            String payload = """
                    {
                        "device_id": "weather-018",
                        "location": "Test",
                        "timestamp": "invalid-timestamp",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """;

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should reject timestamp in wrong format")
        void shouldRejectTimestampInWrongFormat() {
            // Given
            String payload = """
                    {
                        "device_id": "weather-019",
                        "location": "Test",
                        "timestamp": "2024-06-15 14:30:45",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """;

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle timestamp with microseconds")
        void shouldHandleTimestampWithMicroseconds() {
            // Given
            String payload = """
                    {
                        "device_id": "weather-020",
                        "location": "Test",
                        "timestamp": "2024-06-15T14:30:45.123456",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """;

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(20L);

            when(raspiWeatherDataService.saveWeatherData(any())).thenReturn(savedData);

            // When
            handler.processWeatherData(payload);

            // Then
            verify(raspiWeatherDataService).saveWeatherData(any());
        }
    }

    @Nested
    @DisplayName("processWeatherData - error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle malformed JSON gracefully")
        void shouldHandleMalformedJson() {
            // Given
            String payload = "{ invalid json }{";

            // When/Then
            assertThatCode(() -> handler.processWeatherData(payload))
                    .doesNotThrowAnyException();

            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle null payload")
        void shouldHandleNullPayload() {
            // When/Then
            assertThatCode(() -> handler.processWeatherData(null))
                    .doesNotThrowAnyException();

            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle empty payload")
        void shouldHandleEmptyPayload() {
            // When/Then
            assertThatCode(() -> handler.processWeatherData(""))
                    .doesNotThrowAnyException();

            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle service exception")
        void shouldHandleServiceException() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-021",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            when(raspiWeatherDataService.saveWeatherData(any()))
                    .thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatCode(() -> handler.processWeatherData(payload))
                    .doesNotThrowAnyException();

            verify(raspiWeatherDataService).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle non-JSON payload")
        void shouldHandleNonJsonPayload() {
            // Given
            String payload = "This is not JSON at all!";

            // When/Then
            assertThatCode(() -> handler.processWeatherData(payload))
                    .doesNotThrowAnyException();

            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle incomplete JSON object")
        void shouldHandleIncompleteJsonObject() {
            // Given
            String payload = """
                    {
                        "device_id": "weather-022",
                        "location": "Test"
                    """; // Missing closing brace

            // When/Then
            assertThatCode(() -> handler.processWeatherData(payload))
                    .doesNotThrowAnyException();

            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }

        @Test
        @DisplayName("should handle JSON with wrong data types")
        void shouldHandleJsonWithWrongDataTypes() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-023",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": "not-a-number",
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When/Then
            assertThatCode(() -> handler.processWeatherData(payload))
                    .doesNotThrowAnyException();

            verify(raspiWeatherDataService, never()).saveWeatherData(any());
        }
    }

    @Nested
    @DisplayName("constructor initialization")
    class ConstructorTests {

        @Test
        @DisplayName("should initialize ObjectMapper with JavaTimeModule")
        void shouldInitializeObjectMapperWithJavaTimeModule() {
            // Given
            RaspiWeatherDataService mockService = mock(RaspiWeatherDataService.class);

            // When
            RaspiWeatherDataHandler testHandler = new RaspiWeatherDataHandler(mockService);

            // Then - verify by processing a payload with LocalDateTime
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "device_id": "weather-024",
                        "location": "Test",
                        "timestamp": "%s",
                        "temperature": 20.0,
                        "humidity": 50.0,
                        "pressure": 1000.0,
                        "lux": 30000.0,
                        "uvi": 4.0
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            RaspiWeatherData savedData = new RaspiWeatherData();
            savedData.setId(24L);

            when(mockService.saveWeatherData(any())).thenReturn(savedData);

            testHandler.processWeatherData(payload);

            verify(mockService).saveWeatherData(any());
        }
    }
}
