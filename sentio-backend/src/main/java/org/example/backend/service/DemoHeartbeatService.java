package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service that simulates live devices by periodically publishing MQTT status
 * heartbeats.
 * Only active in the 'demo' profile.
 */
@Service
@Profile("demo")
@Slf4j
@RequiredArgsConstructor
public class DemoHeartbeatService {

    private final MessageChannel mqttOutboundChannel;
    private final DeviceRepository deviceRepository;

    private static final String DEVICE_1 = "demo-device-001";
    private static final String DEVICE_2 = "demo-device-002";

    /**
     * Publishes a status heartbeat every 30 seconds for the demo devices
     * to keep them marked as "Active" in the system.
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeats() {
        sendHeartbeatForDevice(DEVICE_1, "192.168.1.101", "weather");
        sendHeartbeatForDevice(DEVICE_2, "192.168.1.102", "camera");
    }

    private void sendHeartbeatForDevice(String deviceId, String ip, String serviceType) {
        Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
        if (deviceOpt.isEmpty()) {
            return;
        }

        Device device = deviceOpt.get();
        if (device.getLatitude() == null || device.getLongitude() == null) {
            return;
        }

        try {
            String topic = "device/" + deviceId + "/status";
            String payload = String.format(
                    "{\"device_id\":\"%s\",\"ip\":\"%s\",\"service\":\"%s\",\"latitude\":%f,\"longitude\":%f}",
                    deviceId, ip, serviceType, device.getLatitude(), device.getLongitude());

            Message<String> message = MessageBuilder.withPayload(payload)
                    .setHeader("mqtt_topic", topic)
                    .build();

            mqttOutboundChannel.send(message);
            log.debug("Sent demo heartbeat for device {}", deviceId);
        } catch (Exception e) {
            log.error("Failed to send demo heartbeat for device {}: {}", deviceId, e.getMessage());
        }
    }
}
