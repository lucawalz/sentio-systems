package org.example.backend.listener;

import org.example.backend.event.AnimalDetectedEvent;
import org.example.backend.model.AnimalDetection;
import org.example.backend.service.N8nWorkflowTriggerService;
import org.example.backend.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnimalEventListener Unit Tests")
class AnimalEventListenerTest {

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private N8nWorkflowTriggerService n8nService;

    @InjectMocks
    private AnimalEventListener listener;

    private AnimalDetection detection;
    private static final String DEVICE_ID = "test-device-123";
    private static final String SPECIES = "European Robin";
    private static final float CONFIDENCE = 0.95f;

    @BeforeEach
    void setUp() {
        detection = new AnimalDetection();
        detection.setId(1L);
        detection.setDeviceId(DEVICE_ID);
        detection.setSpecies(SPECIES);
        detection.setConfidence(CONFIDENCE);
        detection.setAnimalType("bird");
    }

    @Nested
    @DisplayName("onAnimalDetected Tests")
    class OnAnimalDetectedTests {

        @Test
        @DisplayName("Should broadcast animal detection via WebSocket")
        void shouldBroadcastAnimalDetection() {
            // Given
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, SPECIES, CONFIDENCE);
            verifyNoMoreInteractions(webSocketService);
        }

        @Test
        @DisplayName("Should handle high confidence detection")
        void shouldHandleHighConfidenceDetection() {
            // Given
            detection.setConfidence(0.99f);
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, SPECIES, 0.99f);
        }

        @Test
        @DisplayName("Should handle low confidence detection")
        void shouldHandleLowConfidenceDetection() {
            // Given
            detection.setConfidence(0.51f);
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, SPECIES, 0.51f);
        }

        @Test
        @DisplayName("Should handle rare species detection - Owl")
        void shouldHandleRareSpeciesOwl() {
            // Given
            detection.setSpecies("Great Horned Owl");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "Great Horned Owl", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle rare species detection - Eagle")
        void shouldHandleRareSpeciesEagle() {
            // Given
            detection.setSpecies("Bald Eagle");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "Bald Eagle", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle rare species detection - Falcon")
        void shouldHandleRareSpeciesFalcon() {
            // Given
            detection.setSpecies("Peregrine Falcon");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "Peregrine Falcon", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle rare species detection - Heron")
        void shouldHandleRareSpeciesHeron() {
            // Given
            detection.setSpecies("Grey Heron");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "Grey Heron", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle common species detection")
        void shouldHandleCommonSpecies() {
            // Given
            detection.setSpecies("House Sparrow");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "House Sparrow", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle WebSocket exception gracefully")
        void shouldHandleWebSocketException() {
            // Given
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);
            doThrow(new RuntimeException("WebSocket error"))
                    .when(webSocketService).broadcastAnimalDetected(anyString(), anyString(), anyFloat());

            // When & Then - should not throw
            listener.onAnimalDetected(event);

            // Verify exception was caught
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, SPECIES, CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle multiple detections from same device")
        void shouldHandleMultipleDetections() {
            // Given
            AnimalDetectedEvent event1 = new AnimalDetectedEvent(this, detection);

            AnimalDetection detection2 = new AnimalDetection();
            detection2.setDeviceId(DEVICE_ID);
            detection2.setSpecies("Blue Jay");
            detection2.setConfidence(0.88f);
            AnimalDetectedEvent event2 = new AnimalDetectedEvent(this, detection2);

            // When
            listener.onAnimalDetected(event1);
            listener.onAnimalDetected(event2);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, SPECIES, CONFIDENCE);
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, "Blue Jay", 0.88f);
        }

        @Test
        @DisplayName("Should handle detections from different devices")
        void shouldHandleDetectionsFromDifferentDevices() {
            // Given
            AnimalDetectedEvent event1 = new AnimalDetectedEvent(this, detection);

            AnimalDetection detection2 = new AnimalDetection();
            detection2.setDeviceId("device-456");
            detection2.setSpecies("Cardinal");
            detection2.setConfidence(0.92f);
            AnimalDetectedEvent event2 = new AnimalDetectedEvent(this, detection2);

            // When
            listener.onAnimalDetected(event1);
            listener.onAnimalDetected(event2);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, SPECIES, CONFIDENCE);
            verify(webSocketService).broadcastAnimalDetected("device-456", "Cardinal", 0.92f);
        }

        @Test
        @DisplayName("Should test triggerWorkflow via reflection")
        void testTriggerWorkflow() throws Exception {
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);
            java.lang.reflect.Method method = AnimalEventListener.class.getDeclaredMethod("triggerWorkflow",
                    AnimalDetectedEvent.class);
            method.setAccessible(true);
            method.invoke(listener, event);
            verify(n8nService).triggerAnimalDetectionWorkflow(DEVICE_ID, SPECIES, CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle n8n exception gracefully via reflection")
        void testTriggerWorkflowException() throws Exception {
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);
            doThrow(new RuntimeException("n8n error")).when(n8nService).triggerAnimalDetectionWorkflow(anyString(),
                    anyString(), anyFloat());
            java.lang.reflect.Method method = AnimalEventListener.class.getDeclaredMethod("triggerWorkflow",
                    AnimalDetectedEvent.class);
            method.setAccessible(true);
            method.invoke(listener, event);
            verify(n8nService).triggerAnimalDetectionWorkflow(DEVICE_ID, SPECIES, CONFIDENCE);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null species name")
        void shouldHandleNullSpecies() {
            // Given
            detection.setSpecies(null);
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, null, CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle empty species name")
        void shouldHandleEmptySpecies() {
            // Given
            detection.setSpecies("");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, "", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle null device ID")
        void shouldHandleNullDeviceId() {
            // Given
            detection.setDeviceId(null);
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(null, SPECIES, CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle zero confidence")
        void shouldHandleZeroConfidence() {
            // Given
            detection.setConfidence(0.0f);
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, SPECIES, 0.0f);
        }

        @Test
        @DisplayName("Should handle confidence value of 1.0")
        void shouldHandleMaxConfidence() {
            // Given
            detection.setConfidence(1.0f);
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, SPECIES, 1.0f);
        }

        @Test
        @DisplayName("Should handle species name with special characters")
        void shouldHandleSpecialCharacters() {
            // Given
            detection.setSpecies("Bird's-nest Orchid");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "Bird's-nest Orchid", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle very long species name")
        void shouldHandleLongSpeciesName() {
            // Given
            String longName = "Very Long Scientific Name Avius Observus Maximus Extraordinarius";
            detection.setSpecies(longName);
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, longName, CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle species name with mixed case for rare species check")
        void shouldHandleMixedCaseRareSpecies() {
            // Given
            detection.setSpecies("GREAT HORNED OWL");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then - Should still be detected as rare (case-insensitive)
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "GREAT HORNED OWL", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle mammal detection")
        void shouldHandleMammalDetection() {
            // Given
            detection.setSpecies("Red Fox");
            detection.setAnimalType("mammal");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(DEVICE_ID, "Red Fox", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle reptile detection")
        void shouldHandleReptileDetection() {
            // Given
            detection.setSpecies("European Grass Snake");
            detection.setAnimalType("reptile");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "European Grass Snake", CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle unknown animal type")
        void shouldHandleUnknownAnimalType() {
            // Given
            detection.setSpecies("Unknown Creature");
            detection.setAnimalType("unknown");
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When
            listener.onAnimalDetected(event);

            // Then
            verify(webSocketService).broadcastAnimalDetected(
                    DEVICE_ID, "Unknown Creature", CONFIDENCE);
        }
    }

    @Nested
    @DisplayName("Concurrent Event Handling")
    class ConcurrentEventTests {

        @Test
        @DisplayName("Should handle rapid successive detections")
        void shouldHandleRapidDetections() {
            // Given
            AnimalDetectedEvent event = new AnimalDetectedEvent(this, detection);

            // When - simulate rapid events
            for (int i = 0; i < 10; i++) {
                listener.onAnimalDetected(event);
            }

            // Then
            verify(webSocketService, times(10)).broadcastAnimalDetected(
                    DEVICE_ID, SPECIES, CONFIDENCE);
        }

        @Test
        @DisplayName("Should handle interleaved events from multiple devices")
        void shouldHandleInterleavedEvents() {
            // Given
            AnimalDetection detection1 = new AnimalDetection();
            detection1.setDeviceId("device-1");
            detection1.setSpecies("Robin");
            detection1.setConfidence(0.9f);

            AnimalDetection detection2 = new AnimalDetection();
            detection2.setDeviceId("device-2");
            detection2.setSpecies("Sparrow");
            detection2.setConfidence(0.85f);

            AnimalDetectedEvent event1 = new AnimalDetectedEvent(this, detection1);
            AnimalDetectedEvent event2 = new AnimalDetectedEvent(this, detection2);

            // When
            listener.onAnimalDetected(event1);
            listener.onAnimalDetected(event2);
            listener.onAnimalDetected(event1);
            listener.onAnimalDetected(event2);

            // Then
            verify(webSocketService, times(2)).broadcastAnimalDetected("device-1", "Robin", 0.9f);
            verify(webSocketService, times(2)).broadcastAnimalDetected("device-2", "Sparrow", 0.85f);
        }
    }
}
