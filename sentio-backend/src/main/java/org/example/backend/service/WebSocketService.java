package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.websocket.WebSocketMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting messages to connected WebSocket clients.
 * Provides methods for both broadcast (all clients) and user-specific
 * messaging.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a message to all connected clients on a topic.
     * 
     * @param topic   The topic to broadcast to (e.g., "/topic/weather")
     * @param message The message payload
     */
    public void broadcast(String topic, WebSocketMessage message) {
        log.debug("Broadcasting to {}: {}", topic, message.getType());
        messagingTemplate.convertAndSend(topic, message);
    }

    /**
     * Send a message to a specific user.
     * 
     * @param username    The username to send to
     * @param destination The destination queue (e.g., "/queue/devices")
     * @param message     The message payload
     */
    public void sendToUser(String username, String destination, WebSocketMessage message) {
        log.debug("Sending to user {} at {}: {}", username, destination, message.getType());
        messagingTemplate.convertAndSendToUser(username, destination, message);
    }

    /**
     * Broadcast a device registration event.
     */
    public void broadcastDeviceRegistered(String deviceId, String username) {
        WebSocketMessage message = WebSocketMessage.deviceRegistered(deviceId, username);
        sendToUser(username, "/queue/devices", message);
    }

    /**
     * Broadcast a device unregistration event.
     */
    public void broadcastDeviceUnregistered(String deviceId, String username) {
        WebSocketMessage message = WebSocketMessage.deviceUnregistered(deviceId, username);
        sendToUser(username, "/queue/devices", message);
    }

    /**
     * Broadcast weather data update to all connected clients.
     */
    public void broadcastWeatherUpdated(String dataType) {
        WebSocketMessage message = WebSocketMessage.weatherUpdated(dataType);
        broadcast("/topic/weather", message);
    }

    /**
     * Broadcast alerts update to all connected clients.
     */
    public void broadcastAlertsUpdated(int alertCount, boolean hasActive) {
        WebSocketMessage message = WebSocketMessage.alertsUpdated(alertCount, hasActive);
        broadcast("/topic/alerts", message);
    }
}
