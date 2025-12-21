package org.example.backend.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceStatusHandler {

    private final DeviceRepository deviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void processStatusUpdate(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (!root.has("device_id") || !root.has("ip")) {
                log.warn("Invalid status payload: {}", payload);
                return;
            }

            String deviceId = root.path("device_id").asText();
            String ipAddress = root.path("ip").asText();
            String service = root.has("service") ? root.path("service").asText() : null;

            if (deviceId == null || ipAddress == null) {
                log.warn("Missing device_id or ip in payload: {}", payload);
                return;
            }

            log.debug("Received status for device {}: {} (Service: {})", deviceId, ipAddress, service);

            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                boolean needsUpdate = false;

                if (device.getIpAddress() == null || !device.getIpAddress().equals(ipAddress)) {
                    device.setIpAddress(ipAddress);
                    needsUpdate = true;
                    log.info("Device {} IP changed to {}", deviceId, ipAddress);
                }

                if (service != null && !device.getActiveServices().contains(service)) {
                    device.getActiveServices().add(service);
                    needsUpdate = true;
                    log.info("New service '{}' detected on device {}", service, deviceId);
                }

                if (needsUpdate || device.getLastSeen() == null ||
                        device.getLastSeen().isBefore(LocalDateTime.now().minusSeconds(60))) {

                    device.setLastSeen(LocalDateTime.now());
                    deviceRepository.save(device);
                    log.info("Updated status for device {} (Service: {})", deviceId, service);
                } else {
                    log.debug("Ignoring status update for {} - throttled", deviceId);
                }

            } else {
                log.debug("Device {} not registered, ignoring status update", deviceId);
            }

        } catch (Exception e) {
            log.error("Error processing device status update: {}", e.getMessage());
        }
    }
}
