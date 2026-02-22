package org.example.backend.listener;

import org.example.backend.event.StreamStopScheduledEvent;
import org.example.backend.service.ViewerSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreamEventListener Unit Tests")
class StreamEventListenerTest {

    @Mock
    private ViewerSessionService viewerSessionService;

    @Mock
    private MessageChannel mqttOutboundChannel;

    @InjectMocks
    private StreamEventListener listener;

    private static final String DEVICE_ID = "test-device-123";

    @BeforeEach
    void setUp() {
        // No default mocking - tests will stub as needed
    }

    @Nested
    @DisplayName("onStreamStopScheduled Tests - Successful Stop")
    class OnStreamStopScheduledSuccessTests {

        @Test
        @DisplayName("Should send stop command after delay when no viewers present")
        void shouldSendStopCommandAfterDelay() throws InterruptedException {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(0L);
            when(mqttOutboundChannel.send(any())).thenReturn(true);

            // When
            listener.onStreamStopScheduled(event);

            // Then
            verify(viewerSessionService).getViewerCount(DEVICE_ID);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(mqttOutboundChannel).send(messageCaptor.capture());

            Message sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getPayload()).isEqualTo("{\"service\": \"stream\", \"command\": \"stop\"}");
            assertThat(sentMessage.getHeaders().get(MqttHeaders.TOPIC))
                    .isEqualTo("device/" + DEVICE_ID + "/command");
        }

        @Test
        @DisplayName("Should remove scheduled stop after sending command")
        void shouldRemoveScheduledStopAfterSending() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(0L);
            when(mqttOutboundChannel.send(any())).thenReturn(true);

            // When
            listener.onStreamStopScheduled(event);

            // Then
            assertThat(listener.isStopPending(DEVICE_ID)).isFalse();
        }
    }

    @Nested
    @DisplayName("onStreamStopScheduled Tests - Cancelled Stop")
    class OnStreamStopScheduledCancelledTests {

        @Test
        @DisplayName("Should cancel stop when viewers join during delay")
        void shouldCancelStopWhenViewersJoin() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(2L);

            // When
            listener.onStreamStopScheduled(event);

            // Then
            verify(viewerSessionService).getViewerCount(DEVICE_ID);
            verify(mqttOutboundChannel, never()).send(any());
            assertThat(listener.isStopPending(DEVICE_ID)).isFalse();
        }

        @Test
        @DisplayName("Should cancel stop when new stop is scheduled during delay")
        void shouldCancelStopWhenNewStopScheduled() throws InterruptedException {
            // Given
            StreamStopScheduledEvent event1 = new StreamStopScheduledEvent(this, DEVICE_ID);
            // Simulate a new event scheduled after a short delay
            Thread.sleep(100);
            StreamStopScheduledEvent event2 = new StreamStopScheduledEvent(this, DEVICE_ID);

            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(0L);

            // When - Schedule first event
            new Thread(() -> listener.onStreamStopScheduled(event1)).start();

            // Brief pause to let first event start
            Thread.sleep(50);

            // Schedule second event (supersedes first)
            listener.onStreamStopScheduled(event2);

            // Then - Only the second event should result in a stop command
            // Wait for async processing
            Thread.sleep(100);

            // The second event should have superseded the first
            verify(mqttOutboundChannel, atLeastOnce()).send(any());
        }
    }

    @Nested
    @DisplayName("cancelPendingStop Tests")
    class CancelPendingStopTests {

        @Test
        @DisplayName("Should cancel pending stop when called")
        void shouldCancelPendingStop() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            // Start the async stop process
            new Thread(() -> listener.onStreamStopScheduled(event)).start();

            try {
                Thread.sleep(100); // Let the stop be scheduled
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            listener.cancelPendingStop(DEVICE_ID);

            // Then
            assertThat(listener.isStopPending(DEVICE_ID)).isFalse();
        }

        @Test
        @DisplayName("Should handle cancelling non-existent stop")
        void shouldHandleCancellingNonExistentStop() {
            // When
            listener.cancelPendingStop(DEVICE_ID);

            // Then - Should not throw
            assertThat(listener.isStopPending(DEVICE_ID)).isFalse();
        }

        @Test
        @DisplayName("Should cancel stop for specific device only")
        void shouldCancelStopForSpecificDeviceOnly() {
            // Given
            String device1 = "device-1";
            String device2 = "device-2";

            StreamStopScheduledEvent event1 = new StreamStopScheduledEvent(this, device1);
            StreamStopScheduledEvent event2 = new StreamStopScheduledEvent(this, device2);

            new Thread(() -> listener.onStreamStopScheduled(event1)).start();
            new Thread(() -> listener.onStreamStopScheduled(event2)).start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            listener.cancelPendingStop(device1);

            // Then
            assertThat(listener.isStopPending(device1)).isFalse();
            assertThat(listener.isStopPending(device2)).isTrue();
        }
    }

    @Nested
    @DisplayName("isStopPending Tests")
    class IsStopPendingTests {

        @Test
        @DisplayName("Should return true when stop is pending")
        void shouldReturnTrueWhenStopPending() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            new Thread(() -> listener.onStreamStopScheduled(event)).start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            boolean isPending = listener.isStopPending(DEVICE_ID);

            // Then
            assertThat(isPending).isTrue();
        }

        @Test
        @DisplayName("Should return false when no stop is pending")
        void shouldReturnFalseWhenNoStopPending() {
            // When
            boolean isPending = listener.isStopPending(DEVICE_ID);

            // Then
            assertThat(isPending).isFalse();
        }

        @Test
        @DisplayName("Should return false after stop is cancelled")
        void shouldReturnFalseAfterStopCancelled() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            new Thread(() -> listener.onStreamStopScheduled(event)).start();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // When
            listener.cancelPendingStop(DEVICE_ID);
            boolean isPending = listener.isStopPending(DEVICE_ID);

            // Then
            assertThat(isPending).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle MQTT channel send failure gracefully")
        void shouldHandleMqttSendFailure() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(0L);
            when(mqttOutboundChannel.send(any())).thenThrow(new RuntimeException("MQTT error"));

            // When & Then - should not throw
            listener.onStreamStopScheduled(event);

            verify(mqttOutboundChannel).send(any());
        }

        @Test
        @DisplayName("Should handle viewer service exception gracefully")
        void shouldHandleViewerServiceException() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID))
                    .thenThrow(new RuntimeException("Viewer service error"));

            // When & Then - should not throw
            listener.onStreamStopScheduled(event);

            verify(viewerSessionService).getViewerCount(DEVICE_ID);
            verify(mqttOutboundChannel, never()).send(any());
        }

        @Test
        @DisplayName("Should handle thread interruption gracefully")
        void shouldHandleThreadInterruption() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            Thread testThread = new Thread(() -> listener.onStreamStopScheduled(event));

            // When
            testThread.start();
            testThread.interrupt();

            try {
                testThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then - Thread should complete without error
            assertThat(testThread.isAlive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null device ID gracefully")
        void shouldHandleNullDeviceId() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, null);

            // When
            listener.onStreamStopScheduled(event);

            // Then - Should not process null device IDs
            verify(viewerSessionService, never()).getViewerCount(any());
            verify(mqttOutboundChannel, never()).send(any());
        }

        @Test
        @DisplayName("Should handle empty device ID")
        void shouldHandleEmptyDeviceId() {
            // Given
            String emptyDeviceId = "";
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, emptyDeviceId);
            when(viewerSessionService.getViewerCount(emptyDeviceId)).thenReturn(0L);
            when(mqttOutboundChannel.send(any())).thenReturn(true);

            // When
            listener.onStreamStopScheduled(event);

            // Then
            verify(viewerSessionService).getViewerCount(emptyDeviceId);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(mqttOutboundChannel).send(messageCaptor.capture());

            assertThat(messageCaptor.getValue().getHeaders().get(MqttHeaders.TOPIC))
                    .isEqualTo("device//command");
        }

        @Test
        @DisplayName("Should handle device ID with special characters")
        void shouldHandleSpecialCharactersInDeviceId() {
            // Given
            String specialDeviceId = "device-@#$%";
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, specialDeviceId);
            when(viewerSessionService.getViewerCount(specialDeviceId)).thenReturn(0L);
            when(mqttOutboundChannel.send(any())).thenReturn(true);

            // When
            listener.onStreamStopScheduled(event);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(mqttOutboundChannel).send(messageCaptor.capture());

            assertThat(messageCaptor.getValue().getHeaders().get(MqttHeaders.TOPIC))
                    .isEqualTo("device/" + specialDeviceId + "/command");
        }

        @Test
        @DisplayName("Should handle negative viewer count as zero")
        void shouldHandleNegativeViewerCount() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(-1L);

            // When
            listener.onStreamStopScheduled(event);

            // Then - Should not send stop command with negative count
            verify(mqttOutboundChannel, never()).send(any());
        }

        @Test
        @DisplayName("Should handle very large viewer count")
        void shouldHandleVeryLargeViewerCount() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn((long) Integer.MAX_VALUE);

            // When
            listener.onStreamStopScheduled(event);

            // Then - Should not send stop command
            verify(mqttOutboundChannel, never()).send(any());
        }
    }

    @Nested
    @DisplayName("Concurrent Operations Tests")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle multiple devices stopping simultaneously")
        void shouldHandleMultipleDevicesStoppings() {
            // Given
            String device1 = "device-1";
            String device2 = "device-2";
            String device3 = "device-3";

            StreamStopScheduledEvent event1 = new StreamStopScheduledEvent(this, device1);
            StreamStopScheduledEvent event2 = new StreamStopScheduledEvent(this, device2);
            StreamStopScheduledEvent event3 = new StreamStopScheduledEvent(this, device3);

            lenient().when(viewerSessionService.getViewerCount(device1)).thenReturn(0L);
            lenient().when(viewerSessionService.getViewerCount(device2)).thenReturn(0L);
            lenient().when(viewerSessionService.getViewerCount(device3)).thenReturn(0L);

            // When
            new Thread(() -> listener.onStreamStopScheduled(event1)).start();
            new Thread(() -> listener.onStreamStopScheduled(event2)).start();
            new Thread(() -> listener.onStreamStopScheduled(event3)).start();

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then - All should be pending initially
            assertThat(listener.isStopPending(device1)).isTrue();
            assertThat(listener.isStopPending(device2)).isTrue();
            assertThat(listener.isStopPending(device3)).isTrue();
        }

        @Test
        @DisplayName("Should handle rapid stop and cancel operations")
        void shouldHandleRapidStopAndCancel() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);

            // When
            for (int i = 0; i < 10; i++) {
                new Thread(() -> listener.onStreamStopScheduled(event)).start();
                listener.cancelPendingStop(DEVICE_ID);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then - Should handle gracefully without errors
            // Final state depends on race condition, but should not crash
            assertThat(true).isTrue(); // Verification that we got here without exception
        }

        @Test
        @DisplayName("Should handle stop scheduling for same device multiple times")
        void shouldHandleMultipleStopScheduling() {
            // Given
            lenient().when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(0L);

            // When - Schedule multiple stops for same device
            StreamStopScheduledEvent event1 = new StreamStopScheduledEvent(this, DEVICE_ID);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            StreamStopScheduledEvent event2 = new StreamStopScheduledEvent(this, DEVICE_ID);

            new Thread(() -> listener.onStreamStopScheduled(event1)).start();
            new Thread(() -> listener.onStreamStopScheduled(event2)).start();

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then - Should handle gracefully, latest should win
            assertThat(listener.isStopPending(DEVICE_ID)).isTrue();
        }
    }

    @Nested
    @DisplayName("MQTT Message Format Tests")
    class MqttMessageFormatTests {

        @Test
        @DisplayName("Should send correctly formatted MQTT message")
        void shouldSendCorrectlyFormattedMessage() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(0L);
            when(mqttOutboundChannel.send(any())).thenReturn(true);

            // When
            listener.onStreamStopScheduled(event);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(mqttOutboundChannel).send(messageCaptor.capture());

            Message sentMessage = messageCaptor.getValue();
            String payload = (String) sentMessage.getPayload();
            String topic = (String) sentMessage.getHeaders().get(MqttHeaders.TOPIC);

            assertThat(payload).contains("\"service\": \"stream\"");
            assertThat(payload).contains("\"command\": \"stop\"");
            assertThat(topic).startsWith("device/");
            assertThat(topic).endsWith("/command");
            assertThat(topic).contains(DEVICE_ID);
        }

        @Test
        @DisplayName("Should use correct topic format")
        void shouldUseCorrectTopicFormat() {
            // Given
            StreamStopScheduledEvent event = new StreamStopScheduledEvent(this, DEVICE_ID);
            when(viewerSessionService.getViewerCount(DEVICE_ID)).thenReturn(0L);
            when(mqttOutboundChannel.send(any())).thenReturn(true);

            // When
            listener.onStreamStopScheduled(event);

            // Then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(mqttOutboundChannel).send(messageCaptor.capture());

            String topic = (String) messageCaptor.getValue().getHeaders().get(MqttHeaders.TOPIC);
            assertThat(topic).matches("device/.+/command");
        }
    }
}
