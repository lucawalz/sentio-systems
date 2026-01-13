package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.model.Device;
import org.example.backend.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

/**
 * Service for video stream authentication.
 * Validates device tokens for RTMP publish and Keycloak tokens for HLS
 * playback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamService {

    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final JwtDecoder jwtDecoder;

    @Value("${mediamtx.base-url:https://media.syslabs.dev}")
    private String mediamtxBaseUrl;

    // Rate limiting: track failed auth attempts per IP
    private static final int MAX_ATTEMPTS_PER_MINUTE = 5;
    private final ConcurrentHashMap<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    private static class RateLimitEntry {
        int attempts = 0;
        Instant windowStart = Instant.now();
    }

    /**
     * Validate device token for RTMP publish.
     * Called by MediaMTX auth webhook when a device tries to push a stream.
     * 
     * @param deviceId    Device ID from stream path
     * @param deviceToken Device token from query parameter
     * @param sourceIp    Source IP address of the connection
     * @return true if the device is authorized to publish
     */
    public boolean validatePublishAuth(String deviceId, String deviceToken, String sourceIp) {
        if (deviceId == null || deviceToken == null) {
            log.warn("Stream publish auth failed: missing deviceId or token");
            return false;
        }

        // Check rate limit first
        if (isRateLimited(sourceIp)) {
            log.warn("Stream publish auth blocked: rate limit exceeded for IP {}", sourceIp);
            return false;
        }

        // Reuse the existing MQTT token validation
        boolean valid = deviceService.validateMqttToken(deviceId, deviceToken);

        if (valid) {
            // Verify IP matches device's last known IP (soft check - warn only)
            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                String knownIp = device.getIpAddress();
                if (knownIp != null && !knownIp.isEmpty() && !knownIp.equals(sourceIp)) {
                    log.warn("Stream publish: IP mismatch for device {} - known: {}, actual: {}",
                            deviceId, knownIp, sourceIp);
                    // Still allow - IP may have changed legitimately
                }

                device.setStreamActive(true);
                deviceRepository.save(device);
            }

            log.info("Stream publish auth successful for device: {} from IP: {}", deviceId, sourceIp);
            recordSuccessfulAuth(sourceIp);
        } else {
            log.warn("Stream publish auth FAILED for device: {} from IP: {} - invalid token",
                    deviceId, sourceIp);
            recordFailedAuth(sourceIp);
        }

        return valid;
    }

    /**
     * Validate Keycloak access token for HLS viewing.
     * Called by MediaMTX auth webhook when a user tries to view a stream.
     * Uses Spring Security's JwtDecoder to validate the token.
     * 
     * @param deviceId      Device ID being viewed
     * @param keycloakToken Keycloak access token
     * @return true if the user is authorized to view this device's stream
     */
    public boolean validatePlaybackAuth(String deviceId, String keycloakToken) {
        if (deviceId == null || keycloakToken == null) {
            log.warn("Stream playback auth failed: missing deviceId or token");
            return false;
        }

        try {
            // Validate token using Spring Security's configured JwtDecoder
            // This automatically validates against Keycloak's public key
            Jwt jwt = jwtDecoder.decode(keycloakToken);

            String userId = jwt.getSubject();
            if (userId == null) {
                log.warn("Stream playback auth failed: token has no subject");
                return false;
            }

            // Verify user owns this device
            Optional<Device> deviceOpt = deviceRepository.findById(deviceId);
            if (deviceOpt.isEmpty()) {
                log.warn("Stream playback auth failed: device {} not found", deviceId);
                return false;
            }

            Device device = deviceOpt.get();
            if (!device.getOwnerId().equals(userId)) {
                log.warn("Stream playback auth failed: user {} does not own device {}", userId, deviceId);
                return false;
            }

            log.debug("Stream playback auth successful for user {} viewing device {}", userId, deviceId);
            return true;

        } catch (JwtException e) {
            log.warn("Stream playback auth failed: invalid Keycloak token - {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the HLS stream URL for a device.
     * The token will be passed separately by the frontend.
     * 
     * @param deviceId Device ID
     * @return HLS URL (token will be appended by frontend)
     */
    public String getStreamUrl(String deviceId) {
        // URL format: https://media.syslabs.dev/live/{deviceId}/index.m3u8
        // Token will be appended by frontend: ?token={keycloak_access_token}
        return String.format("%s/live/%s/index.m3u8", mediamtxBaseUrl, deviceId);
    }

    /**
     * Check if a device is currently streaming.
     * 
     * @param deviceId Device ID
     * @return true if the device has an active stream
     */
    public boolean isDeviceStreaming(String deviceId) {
        return deviceRepository.findById(deviceId)
                .map(device -> Boolean.TRUE.equals(device.getStreamActive()))
                .orElse(false);
    }

    /**
     * Mark a device as not streaming.
     * Called when MediaMTX notifies that a stream has ended.
     * 
     * @param deviceId Device ID
     */
    public void markStreamEnded(String deviceId) {
        deviceRepository.findById(deviceId).ifPresent(device -> {
            device.setStreamActive(false);
            deviceRepository.save(device);
            log.info("Stream ended for device: {}", deviceId);
        });
    }

    // --- Rate Limiting Helpers ---

    private boolean isRateLimited(String ip) {
        if (ip == null)
            return false;

        RateLimitEntry entry = rateLimitMap.computeIfAbsent(ip, k -> new RateLimitEntry());

        // Reset window if expired (1 minute)
        if (Instant.now().isAfter(entry.windowStart.plusSeconds(60))) {
            entry.attempts = 0;
            entry.windowStart = Instant.now();
        }

        return entry.attempts >= MAX_ATTEMPTS_PER_MINUTE;
    }

    private void recordSuccessfulAuth(String ip) {
        // Reset on success
        rateLimitMap.remove(ip);
    }

    private void recordFailedAuth(String ip) {
        if (ip == null)
            return;

        RateLimitEntry entry = rateLimitMap.computeIfAbsent(ip, k -> new RateLimitEntry());
        entry.attempts++;
    }
}
