package org.example.backend.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.example.backend.service.DeviceLocationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceStatusHandler {

    private final DeviceRepository deviceRepository;
    private final DeviceLocationService deviceLocationService;
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

            // Extract GPS coordinates if present
            Double latitude = root.has("latitude") ? root.path("latitude").asDouble() : null;
            Double longitude = root.has("longitude") ? root.path("longitude").asDouble() : null;

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

                // Process GPS coordinates if present - this triggers weather data fetch via EDA
                if (latitude != null && longitude != null) {
                    // Check if location has changed significantly (not just GPS drift)
                    // ~0.0005 degrees ≈ 50 meters - prevents unnecessary weather fetches
                    double LOCATION_CHANGE_THRESHOLD = 0.0005;
                    boolean isNewLocation = device.getLatitude() == null || device.getLongitude() == null;

                    if (!isNewLocation) {
                        double latDiff = Math.abs(device.getLatitude() - latitude);
                        double lonDiff = Math.abs(device.getLongitude() - longitude);
                        isNewLocation = latDiff > LOCATION_CHANGE_THRESHOLD || lonDiff > LOCATION_CHANGE_THRESHOLD;
                    }

                    if (isNewLocation) {
                        log.info("GPS coordinates changed significantly for device {}: ({}, {})", deviceId, latitude,
                                longitude);
                        // This fires DeviceLocationUpdatedEvent which triggers forecast/alert fetching
                        deviceLocationService.updateDeviceGpsLocation(deviceId, latitude, longitude);
                    } else {
                        log.debug("GPS coordinates unchanged (within tolerance) for device {}", deviceId);
                    }
                }

            } else {
                log.debug("Device {} not registered, ignoring status update", deviceId);
            }

        } catch (Exception e) {
            log.error("Error processing device status update: {}", e.getMessage());
        }
    }
}
