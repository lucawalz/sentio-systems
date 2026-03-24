package dev.syslabs.sentio.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard WebSocket message format for frontend communication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

        /**
         * Message type identifier for frontend routing.
         */
        private String type;

        /**
         * Timestamp when the message was created.
         */
        @Builder.Default
        private Instant timestamp = Instant.now();

        /**
         * Message payload with additional data.
         */
        private Map<String, Object> payload;

        // Factory methods for common message types

        public static WebSocketMessage deviceRegistered(String deviceId, String username) {
                return WebSocketMessage.builder()
                                .type("DEVICE_REGISTERED")
                                .payload(Map.of(
                                                "deviceId", deviceId,
                                                "username", username))
                                .build();
        }

        public static WebSocketMessage deviceUnregistered(String deviceId, String username) {
                return WebSocketMessage.builder()
                                .type("DEVICE_UNREGISTERED")
                                .payload(Map.of(
                                                "deviceId", deviceId,
                                                "username", username))
                                .build();
        }

        public static WebSocketMessage weatherUpdated(String dataType) {
                return WebSocketMessage.builder()
                                .type("WEATHER_UPDATED")
                                .payload(Map.of("dataType", dataType))
                                .build();
        }

        public static WebSocketMessage alertsUpdated(int alertCount, boolean hasActive) {
                return WebSocketMessage.builder()
                                .type("ALERTS_UPDATED")
                                .payload(Map.of(
                                                "alertCount", alertCount,
                                                "hasActiveAlerts", hasActive))
                                .build();
        }

        public static WebSocketMessage animalDetected(String deviceId, String species, float confidence) {
                return WebSocketMessage.builder()
                                .type("ANIMAL_DETECTED")
                                .payload(Map.of(
                                                "deviceId", deviceId,
                                                "species", species,
                                                "confidence", confidence))
                                .build();
        }
}
