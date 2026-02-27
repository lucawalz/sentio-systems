package org.example.backend.service;

import org.example.backend.dto.websocket.WebSocketMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WebSocketService}.
 * Tests broadcast messaging, user-specific messaging, and topic-based routing.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService webSocketService;

    @Nested
    @DisplayName("broadcast")
    class BroadcastTests {

        @Test
        @DisplayName("should broadcast message to topic")
        void shouldBroadcastMessageToTopic() {
            // Given
            String topic = "/topic/weather";
            WebSocketMessage message = WebSocketMessage.weatherUpdated("forecast");

            // When
            webSocketService.broadcast(topic, message);

            // Then
            verify(messagingTemplate).convertAndSend(topic, message);
        }

        @Test
        @DisplayName("should broadcast to multiple different topics")
        void shouldBroadcastToMultipleTopics() {
            // Given
            String topic1 = "/topic/weather";
            String topic2 = "/topic/alerts";
            WebSocketMessage message1 = WebSocketMessage.weatherUpdated("current");
            WebSocketMessage message2 = WebSocketMessage.alertsUpdated(5, true);

            // When
            webSocketService.broadcast(topic1, message1);
            webSocketService.broadcast(topic2, message2);

            // Then
            verify(messagingTemplate).convertAndSend(topic1, message1);
            verify(messagingTemplate).convertAndSend(topic2, message2);
        }

        @Test
        @DisplayName("should handle null topic gracefully")
        void shouldHandleNullTopic() {
            // Given
            WebSocketMessage message = WebSocketMessage.weatherUpdated("forecast");

            // When/Then - Should not throw, delegate to template
            assertThatCode(() -> webSocketService.broadcast(null, message))
                    .doesNotThrowAnyException();

            verify(messagingTemplate).convertAndSend(isNull(), eq(message));
        }

        @Test
        @DisplayName("should throw exception for null message")
        void shouldThrowExceptionForNullMessage() {
            // Given
            String topic = "/topic/weather";

            // When/Then - Should throw NullPointerException
            assertThatCode(() -> webSocketService.broadcast(topic, null))
                    .isInstanceOf(NullPointerException.class);

            verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        }
    }

    @Nested
    @DisplayName("sendToUser")
    class SendToUserTests {

        @Test
        @DisplayName("should send message to specific user")
        void shouldSendMessageToSpecificUser() {
            // Given
            String username = "testuser";
            String destination = "/queue/devices";
            WebSocketMessage message = WebSocketMessage.deviceRegistered("device-123", username);

            // When
            webSocketService.sendToUser(username, destination, message);

            // Then
            verify(messagingTemplate).convertAndSendToUser(username, destination, message);
        }

        @Test
        @DisplayName("should send different messages to different users")
        void shouldSendDifferentMessagesToDifferentUsers() {
            // Given
            String user1 = "alice";
            String user2 = "bob";
            String destination = "/queue/devices";
            WebSocketMessage message1 = WebSocketMessage.deviceRegistered("device-1", user1);
            WebSocketMessage message2 = WebSocketMessage.deviceRegistered("device-2", user2);

            // When
            webSocketService.sendToUser(user1, destination, message1);
            webSocketService.sendToUser(user2, destination, message2);

            // Then
            verify(messagingTemplate).convertAndSendToUser(user1, destination, message1);
            verify(messagingTemplate).convertAndSendToUser(user2, destination, message2);
        }

        @Test
        @DisplayName("should handle null username gracefully")
        void shouldHandleNullUsername() {
            // Given
            String destination = "/queue/devices";
            WebSocketMessage message = WebSocketMessage.deviceRegistered("device-123", "user");

            // When/Then - Should not throw, delegate to template
            assertThatCode(() -> webSocketService.sendToUser(null, destination, message))
                    .doesNotThrowAnyException();

            verify(messagingTemplate).convertAndSendToUser(isNull(), eq(destination), eq(message));
        }

        @Test
        @DisplayName("should handle null destination gracefully")
        void shouldHandleNullDestination() {
            // Given
            String username = "testuser";
            WebSocketMessage message = WebSocketMessage.deviceRegistered("device-123", username);

            // When/Then - Should not throw, delegate to template
            assertThatCode(() -> webSocketService.sendToUser(username, null, message))
                    .doesNotThrowAnyException();

            verify(messagingTemplate).convertAndSendToUser(eq(username), isNull(), eq(message));
        }
    }

    @Nested
    @DisplayName("broadcastDeviceRegistered")
    class BroadcastDeviceRegisteredTests {

        @Test
        @DisplayName("should broadcast device registered event to user")
        void shouldBroadcastDeviceRegisteredEvent() {
            // Given
            String deviceId = "device-abc-123";
            String username = "testuser";

            // When
            webSocketService.broadcastDeviceRegistered(deviceId, username);

            // Then
            ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq(username),
                    eq("/queue/devices"),
                    messageCaptor.capture()
            );

            WebSocketMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getType()).isEqualTo("DEVICE_REGISTERED");
            assertThat(capturedMessage.getPayload()).containsEntry("deviceId", deviceId);
            assertThat(capturedMessage.getPayload()).containsEntry("username", username);
        }
    }

    @Nested
    @DisplayName("broadcastDeviceUnregistered")
    class BroadcastDeviceUnregisteredTests {

        @Test
        @DisplayName("should broadcast device unregistered event to user")
        void shouldBroadcastDeviceUnregisteredEvent() {
            // Given
            String deviceId = "device-xyz-789";
            String username = "testuser";

            // When
            webSocketService.broadcastDeviceUnregistered(deviceId, username);

            // Then
            ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSendToUser(
                    eq(username),
                    eq("/queue/devices"),
                    messageCaptor.capture()
            );

            WebSocketMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getType()).isEqualTo("DEVICE_UNREGISTERED");
            assertThat(capturedMessage.getPayload()).containsEntry("deviceId", deviceId);
            assertThat(capturedMessage.getPayload()).containsEntry("username", username);
        }
    }

    @Nested
    @DisplayName("broadcastWeatherUpdated")
    class BroadcastWeatherUpdatedTests {

        @Test
        @DisplayName("should broadcast weather updated event to topic")
        void shouldBroadcastWeatherUpdatedEvent() {
            // Given
            String dataType = "forecast";

            // When
            webSocketService.broadcastWeatherUpdated(dataType);

            // Then
            ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/weather"),
                    messageCaptor.capture()
            );

            WebSocketMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getType()).isEqualTo("WEATHER_UPDATED");
            assertThat(capturedMessage.getPayload()).containsEntry("dataType", dataType);
        }

        @Test
        @DisplayName("should broadcast different weather data types")
        void shouldBroadcastDifferentDataTypes() {
            // When
            webSocketService.broadcastWeatherUpdated("current");
            webSocketService.broadcastWeatherUpdated("forecast");

            // Then
            verify(messagingTemplate, times(2)).convertAndSend(
                    eq("/topic/weather"),
                    any(WebSocketMessage.class)
            );
        }
    }

    @Nested
    @DisplayName("broadcastAlertsUpdated")
    class BroadcastAlertsUpdatedTests {

        @Test
        @DisplayName("should broadcast alerts updated event with active alerts")
        void shouldBroadcastAlertsUpdatedWithActiveAlerts() {
            // Given
            int alertCount = 3;
            boolean hasActive = true;

            // When
            webSocketService.broadcastAlertsUpdated(alertCount, hasActive);

            // Then
            ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/alerts"),
                    messageCaptor.capture()
            );

            WebSocketMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getType()).isEqualTo("ALERTS_UPDATED");
            assertThat(capturedMessage.getPayload()).containsEntry("alertCount", alertCount);
            assertThat(capturedMessage.getPayload()).containsEntry("hasActiveAlerts", hasActive);
        }

        @Test
        @DisplayName("should broadcast alerts updated event with no active alerts")
        void shouldBroadcastAlertsUpdatedWithNoActiveAlerts() {
            // Given
            int alertCount = 0;
            boolean hasActive = false;

            // When
            webSocketService.broadcastAlertsUpdated(alertCount, hasActive);

            // Then
            ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/alerts"),
                    messageCaptor.capture()
            );

            WebSocketMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getType()).isEqualTo("ALERTS_UPDATED");
            assertThat(capturedMessage.getPayload()).containsEntry("alertCount", 0);
            assertThat(capturedMessage.getPayload()).containsEntry("hasActiveAlerts", false);
        }
    }

    @Nested
    @DisplayName("broadcastAnimalDetected")
    class BroadcastAnimalDetectedTests {

        @Test
        @DisplayName("should broadcast animal detected event")
        void shouldBroadcastAnimalDetectedEvent() {
            // Given
            String deviceId = "camera-001";
            String species = "deer";
            float confidence = 0.95f;

            // When
            webSocketService.broadcastAnimalDetected(deviceId, species, confidence);

            // Then
            ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/animals"),
                    messageCaptor.capture()
            );

            WebSocketMessage capturedMessage = messageCaptor.getValue();
            assertThat(capturedMessage.getType()).isEqualTo("ANIMAL_DETECTED");
            assertThat(capturedMessage.getPayload()).containsEntry("deviceId", deviceId);
            assertThat(capturedMessage.getPayload()).containsEntry("species", species);
            assertThat(capturedMessage.getPayload()).containsEntry("confidence", confidence);
        }

        @Test
        @DisplayName("should broadcast multiple animal detections")
        void shouldBroadcastMultipleDetections() {
            // When
            webSocketService.broadcastAnimalDetected("camera-001", "deer", 0.95f);
            webSocketService.broadcastAnimalDetected("camera-002", "fox", 0.87f);

            // Then
            verify(messagingTemplate, times(2)).convertAndSend(
                    eq("/topic/animals"),
                    any(WebSocketMessage.class)
            );
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle messaging template exception gracefully")
        void shouldHandleMessagingTemplateException() {
            // Given
            String topic = "/topic/weather";
            WebSocketMessage message = WebSocketMessage.weatherUpdated("forecast");
            doThrow(new RuntimeException("Connection lost")).when(messagingTemplate)
                    .convertAndSend(anyString(), any(WebSocketMessage.class));

            // When/Then - Should throw (no error handling in service layer)
            assertThatCode(() -> webSocketService.broadcast(topic, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Connection lost");
        }

        @Test
        @DisplayName("should handle user messaging exception gracefully")
        void shouldHandleUserMessagingException() {
            // Given
            String username = "testuser";
            String destination = "/queue/devices";
            WebSocketMessage message = WebSocketMessage.deviceRegistered("device-123", username);
            doThrow(new RuntimeException("User not connected")).when(messagingTemplate)
                    .convertAndSendToUser(anyString(), anyString(), any(WebSocketMessage.class));

            // When/Then - Should throw (no error handling in service layer)
            assertThatCode(() -> webSocketService.sendToUser(username, destination, message))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not connected");
        }
    }

    @Nested
    @DisplayName("topic subscription")
    class TopicSubscriptionTests {

        @Test
        @DisplayName("should route weather messages to weather topic")
        void shouldRouteWeatherMessagesToWeatherTopic() {
            // When
            webSocketService.broadcastWeatherUpdated("forecast");

            // Then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/weather"),
                    any(WebSocketMessage.class)
            );
        }

        @Test
        @DisplayName("should route alerts messages to alerts topic")
        void shouldRouteAlertsMessagesToAlertsTopic() {
            // When
            webSocketService.broadcastAlertsUpdated(5, true);

            // Then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/alerts"),
                    any(WebSocketMessage.class)
            );
        }

        @Test
        @DisplayName("should route animal messages to animals topic")
        void shouldRouteAnimalMessagesToAnimalsTopic() {
            // When
            webSocketService.broadcastAnimalDetected("camera-001", "deer", 0.95f);

            // Then
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/animals"),
                    any(WebSocketMessage.class)
            );
        }

        @Test
        @DisplayName("should route device messages to user queue")
        void shouldRouteDeviceMessagesToUserQueue() {
            // Given
            String username = "testuser";

            // When
            webSocketService.broadcastDeviceRegistered("device-123", username);

            // Then
            verify(messagingTemplate).convertAndSendToUser(
                    eq(username),
                    eq("/queue/devices"),
                    any(WebSocketMessage.class)
            );
        }
    }
}
