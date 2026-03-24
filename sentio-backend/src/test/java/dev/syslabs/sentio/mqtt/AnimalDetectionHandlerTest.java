package dev.syslabs.sentio.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.syslabs.sentio.model.AnimalDetection;
import dev.syslabs.sentio.service.AnimalClassifierService;
import dev.syslabs.sentio.service.AnimalDetectionCommandService;
import dev.syslabs.sentio.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AnimalDetectionHandler}.
 * Tests MQTT message parsing, animal detection creation, and AI classification triggering.
 */
@ExtendWith(MockitoExtension.class)
class AnimalDetectionHandlerTest {

    @Mock
    private AnimalDetectionCommandService commandService;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private AnimalClassifierService animalClassifierService;

    @InjectMocks
    private AnimalDetectionHandler handler;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new AnimalDetectionHandler(
                commandService,
                imageStorageService,
                objectMapper,
                animalClassifierService
        );
    }

    @Nested
    @DisplayName("processAnimalDetection - valid payloads")
    class ValidPayloadTests {

        @Test
        @DisplayName("should process single animal detection with all fields")
        void shouldProcessSingleDetection() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test-image-bytes".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-001",
                        "location": "Garden Camera",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [100.0, 200.0, 300.0, 400.0],
                                "confidence": 0.95,
                                "class_id": 14,
                                "species": "Parus major"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);
            savedDetection.setAnimalType("bird");
            savedDetection.setSpecies("Parus major");

            when(imageStorageService.saveImage(any(), eq("jpg"), any(), eq("device-001")))
                    .thenReturn("/images/device-001/detection-001.jpg");
            when(animalClassifierService.determineAnimalType("Parus major")).thenReturn("bird");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

            // When
            handler.processAnimalDetection(payload);

            // Then
            ArgumentCaptor<AnimalDetection> detectionCaptor = ArgumentCaptor.forClass(AnimalDetection.class);
            verify(commandService).saveAnimalDetection(detectionCaptor.capture());
            verify(animalClassifierService).classifyAndUpdate(savedDetection);

            AnimalDetection captured = detectionCaptor.getValue();
            assertThat(captured.getX()).isEqualTo(100.0f);
            assertThat(captured.getY()).isEqualTo(200.0f);
            assertThat(captured.getWidth()).isEqualTo(200.0f); // x2 - x1
            assertThat(captured.getHeight()).isEqualTo(200.0f); // y2 - y1
            assertThat(captured.getConfidence()).isEqualTo(0.95f);
            assertThat(captured.getClassId()).isEqualTo(14);
            assertThat(captured.getSpecies()).isEqualTo("Parus major");
            assertThat(captured.getAnimalType()).isEqualTo("bird");
            assertThat(captured.getImageUrl()).isEqualTo("/images/device-001/detection-001.jpg");
            assertThat(captured.getDeviceId()).isEqualTo("device-001");
            assertThat(captured.getLocation()).isEqualTo("Garden Camera");
            assertThat(captured.getTriggerReason()).isEqualTo("motion");
            assertThat(captured.isAiProcessed()).isFalse();
        }

        @Test
        @DisplayName("should process multiple animal detections in single message")
        void shouldProcessMultipleDetections() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test-image".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 2,
                        "trigger_reason": "scheduled",
                        "device_id": "device-002",
                        "location": "Forest Camera",
                        "image_data": "%s",
                        "image_format": "png",
                        "detections": [
                            {
                                "bbox": [50.0, 60.0, 150.0, 160.0],
                                "confidence": 0.88,
                                "class_id": 10,
                                "species": "Corvus corone"
                            },
                            {
                                "bbox": [200.0, 210.0, 300.0, 310.0],
                                "confidence": 0.92,
                                "class_id": 11,
                                "species": "Columba palumbus"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection detection1 = new AnimalDetection();
            detection1.setId(1L);
            detection1.setAnimalType("bird");

            AnimalDetection detection2 = new AnimalDetection();
            detection2.setId(2L);
            detection2.setAnimalType("bird");

            when(imageStorageService.saveImage(any(), eq("png"), any(), eq("device-002")))
                    .thenReturn("/images/device-002/detection.png");
            when(animalClassifierService.determineAnimalType(anyString())).thenReturn("bird");
            when(commandService.saveAnimalDetection(any()))
                    .thenReturn(detection1)
                    .thenReturn(detection2);

            // When
            handler.processAnimalDetection(payload);

            // Then
            verify(commandService, times(2)).saveAnimalDetection(any());
            verify(animalClassifierService, times(2)).classifyAndUpdate(any());
            verify(animalClassifierService).classifyAndUpdate(detection1);
            verify(animalClassifierService).classifyAndUpdate(detection2);
        }

        @Test
        @DisplayName("should handle detection without species field")
        void shouldHandleDetectionWithoutSpecies() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-003",
                        "location": "Backyard",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [10.0, 20.0, 30.0, 40.0],
                                "confidence": 0.75,
                                "class_id": 5
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");
            when(animalClassifierService.determineAnimalType("unknown")).thenReturn("unknown");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

            // When
            handler.processAnimalDetection(payload);

            // Then
            ArgumentCaptor<AnimalDetection> captor = ArgumentCaptor.forClass(AnimalDetection.class);
            verify(commandService).saveAnimalDetection(captor.capture());

            assertThat(captor.getValue().getSpecies()).isEqualTo("unknown");
            verify(animalClassifierService).determineAnimalType("unknown");
        }

        @Test
        @DisplayName("should handle detection with explicit animal_type field")
        void shouldHandleExplicitAnimalType() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-004",
                        "location": "Lake",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [0.0, 0.0, 100.0, 100.0],
                                "confidence": 0.99,
                                "class_id": 20,
                                "species": "Anas platyrhynchos",
                                "animal_type": "bird"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

            // When
            handler.processAnimalDetection(payload);

            // Then
            ArgumentCaptor<AnimalDetection> captor = ArgumentCaptor.forClass(AnimalDetection.class);
            verify(commandService).saveAnimalDetection(captor.capture());

            assertThat(captor.getValue().getAnimalType()).isEqualTo("bird");
            verify(animalClassifierService, never()).determineAnimalType(anyString());
        }

        @Test
        @DisplayName("should handle different image formats")
        void shouldHandleDifferentImageFormats() {
            // Given
            String[] formats = {"jpg", "png", "jpeg", "webp"};

            for (String format : formats) {
                String imageData = Base64.getEncoder().encodeToString(("test-" + format).getBytes());
                LocalDateTime timestamp = LocalDateTime.now();
                String payload = String.format("""
                        {
                            "timestamp": "%s",
                            "detection_count": 1,
                            "trigger_reason": "motion",
                            "device_id": "device-005",
                            "location": "Test",
                            "image_data": "%s",
                            "image_format": "%s",
                            "detections": [
                                {
                                    "bbox": [0.0, 0.0, 10.0, 10.0],
                                    "confidence": 0.8,
                                    "class_id": 1,
                                    "species": "test"
                                }
                            ]
                        }
                        """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData, format);

                AnimalDetection savedDetection = new AnimalDetection();
                savedDetection.setId((long) java.util.Arrays.asList(formats).indexOf(format) + 1);

                when(imageStorageService.saveImage(any(), eq(format), any(), any()))
                        .thenReturn("/images/test." + format);
                when(animalClassifierService.determineAnimalType(anyString())).thenReturn("unknown");
                when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

                // When
                handler.processAnimalDetection(payload);

                // Then
                verify(imageStorageService).saveImage(any(), eq(format), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("processAnimalDetection - edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle empty detections array")
        void shouldHandleEmptyDetectionsArray() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 0,
                        "trigger_reason": "motion",
                        "device_id": "device-006",
                        "location": "Empty",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": []
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");

            // When
            handler.processAnimalDetection(payload);

            // Then
            verify(commandService, never()).saveAnimalDetection(any());
            verify(animalClassifierService, never()).classifyAndUpdate(any());
        }

        @Test
        @DisplayName("should handle bbox with less than 4 elements")
        void shouldHandleIncompleteBbox() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-007",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [10.0, 20.0],
                                "confidence": 0.8,
                                "class_id": 1,
                                "species": "test"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");
            when(animalClassifierService.determineAnimalType(anyString())).thenReturn("unknown");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

            // When
            handler.processAnimalDetection(payload);

            // Then
            ArgumentCaptor<AnimalDetection> captor = ArgumentCaptor.forClass(AnimalDetection.class);
            verify(commandService).saveAnimalDetection(captor.capture());

            // Bbox should not be set, so x, y, width, height should be 0 (default float value)
            assertThat(captor.getValue().getX()).isZero();
            assertThat(captor.getValue().getY()).isZero();
            assertThat(captor.getValue().getWidth()).isZero();
            assertThat(captor.getValue().getHeight()).isZero();
        }

        @Test
        @DisplayName("should handle non-array detections field")
        void shouldHandleNonArrayDetections() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-008",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": "not-an-array"
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");

            // When
            handler.processAnimalDetection(payload);

            // Then
            verify(commandService, never()).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should calculate correct bbox dimensions with negative coordinates")
        void shouldHandleNegativeBboxCoordinates() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-009",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [-10.0, -20.0, 50.0, 80.0],
                                "confidence": 0.9,
                                "class_id": 1,
                                "species": "test"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");
            when(animalClassifierService.determineAnimalType(anyString())).thenReturn("unknown");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

            // When
            handler.processAnimalDetection(payload);

            // Then
            ArgumentCaptor<AnimalDetection> captor = ArgumentCaptor.forClass(AnimalDetection.class);
            verify(commandService).saveAnimalDetection(captor.capture());

            assertThat(captor.getValue().getX()).isEqualTo(-10.0f);
            assertThat(captor.getValue().getY()).isEqualTo(-20.0f);
            assertThat(captor.getValue().getWidth()).isEqualTo(60.0f); // 50 - (-10)
            assertThat(captor.getValue().getHeight()).isEqualTo(100.0f); // 80 - (-20)
        }

        @Test
        @DisplayName("should handle high detection count")
        void shouldHandleHighDetectionCount() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            StringBuilder detectionsBuilder = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                if (i > 0) detectionsBuilder.append(",");
                detectionsBuilder.append(String.format("""
                        {
                            "bbox": [%d.0, %d.0, %d.0, %d.0],
                            "confidence": 0.8,
                            "class_id": %d,
                            "species": "bird-%d"
                        }
                        """, i * 10, i * 10, i * 10 + 50, i * 10 + 50, i, i));
            }

            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 10,
                        "trigger_reason": "motion",
                        "device_id": "device-010",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [%s]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData, detectionsBuilder);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");
            when(animalClassifierService.determineAnimalType(anyString())).thenReturn("bird");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

            // When
            handler.processAnimalDetection(payload);

            // Then
            verify(commandService, times(10)).saveAnimalDetection(any());
            verify(animalClassifierService, times(10)).classifyAndUpdate(any());
        }
    }

    @Nested
    @DisplayName("processAnimalDetection - error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle malformed JSON gracefully")
        void shouldHandleMalformedJson() {
            // Given
            String payload = "{ invalid json }{";

            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(payload))
                    .doesNotThrowAnyException();

            verify(commandService, never()).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should handle missing required fields")
        void shouldHandleMissingRequiredFields() {
            // Given
            String payload = """
                    {
                        "timestamp": "2024-01-01T12:00:00"
                    }
                    """;

            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(payload))
                    .doesNotThrowAnyException();

            verify(commandService, never()).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should handle invalid timestamp format")
        void shouldHandleInvalidTimestamp() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            String payload = String.format("""
                    {
                        "timestamp": "invalid-timestamp",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-011",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": []
                    }
                    """, imageData);

            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(payload))
                    .doesNotThrowAnyException();

            verify(commandService, never()).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should handle invalid Base64 image data")
        void shouldHandleInvalidBase64() {
            // Given
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-012",
                        "location": "Test",
                        "image_data": "not-valid-base64!!!",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [0.0, 0.0, 10.0, 10.0],
                                "confidence": 0.8,
                                "class_id": 1,
                                "species": "test"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(payload))
                    .doesNotThrowAnyException();

            verify(commandService, never()).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should handle imageStorageService failure")
        void shouldHandleImageStorageFailure() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-013",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [0.0, 0.0, 10.0, 10.0],
                                "confidence": 0.8,
                                "class_id": 1,
                                "species": "test"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            when(imageStorageService.saveImage(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("Storage service unavailable"));

            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(payload))
                    .doesNotThrowAnyException();

            verify(commandService, never()).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should handle commandService failure")
        void shouldHandleCommandServiceFailure() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-014",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [0.0, 0.0, 10.0, 10.0],
                                "confidence": 0.8,
                                "class_id": 1,
                                "species": "test"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");
            when(animalClassifierService.determineAnimalType(anyString())).thenReturn("unknown");
            when(commandService.saveAnimalDetection(any()))
                    .thenThrow(new RuntimeException("Database error"));

            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(payload))
                    .doesNotThrowAnyException();

            verify(animalClassifierService, never()).classifyAndUpdate(any());
        }

        @Test
        @DisplayName("should handle animalClassifierService failure")
        void shouldHandleClassifierServiceFailure() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime timestamp = LocalDateTime.now();
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-015",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [0.0, 0.0, 10.0, 10.0],
                                "confidence": 0.8,
                                "class_id": 1,
                                "species": "test"
                            }
                        ]
                    }
                    """, timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);

            when(imageStorageService.saveImage(any(), any(), any(), any())).thenReturn("/images/test.jpg");
            when(animalClassifierService.determineAnimalType(anyString())).thenReturn("unknown");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);
            doThrow(new RuntimeException("AI service unavailable"))
                    .when(animalClassifierService).classifyAndUpdate(any());

            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(payload))
                    .doesNotThrowAnyException();

            verify(commandService).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should handle null payload")
        void shouldHandleNullPayload() {
            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(null))
                    .doesNotThrowAnyException();

            verify(commandService, never()).saveAnimalDetection(any());
        }

        @Test
        @DisplayName("should handle empty payload")
        void shouldHandleEmptyPayload() {
            // When/Then
            assertThatCode(() -> handler.processAnimalDetection(""))
                    .doesNotThrowAnyException();

            verify(commandService, never()).saveAnimalDetection(any());
        }
    }

    @Nested
    @DisplayName("processAnimalDetection - timestamp handling")
    class TimestampHandlingTests {

        @Test
        @DisplayName("should parse ISO_LOCAL_DATE_TIME format")
        void shouldParseIsoLocalDateTime() {
            // Given
            String imageData = Base64.getEncoder().encodeToString("test".getBytes());
            LocalDateTime expectedTimestamp = LocalDateTime.of(2024, 6, 15, 14, 30, 45);
            String payload = String.format("""
                    {
                        "timestamp": "%s",
                        "detection_count": 1,
                        "trigger_reason": "motion",
                        "device_id": "device-016",
                        "location": "Test",
                        "image_data": "%s",
                        "image_format": "jpg",
                        "detections": [
                            {
                                "bbox": [0.0, 0.0, 10.0, 10.0],
                                "confidence": 0.8,
                                "class_id": 1,
                                "species": "test"
                            }
                        ]
                    }
                    """, expectedTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), imageData);

            AnimalDetection savedDetection = new AnimalDetection();
            savedDetection.setId(1L);

            when(imageStorageService.saveImage(any(), any(), eq(expectedTimestamp), any()))
                    .thenReturn("/images/test.jpg");
            when(animalClassifierService.determineAnimalType(anyString())).thenReturn("unknown");
            when(commandService.saveAnimalDetection(any())).thenReturn(savedDetection);

            // When
            handler.processAnimalDetection(payload);

            // Then
            ArgumentCaptor<AnimalDetection> captor = ArgumentCaptor.forClass(AnimalDetection.class);
            verify(commandService).saveAnimalDetection(captor.capture());

            assertThat(captor.getValue().getTimestamp()).isEqualTo(expectedTimestamp);
        }
    }
}
