package org.example.backend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.DeviceService;
import org.example.backend.service.StreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for video stream authentication.
 * Provides endpoints for MediaMTX auth webhooks and frontend stream URL
 * requests.
 */
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Stream", description = "Video stream authentication and URL management")
public class StreamAuthController {

    private final StreamService streamService;
    private final DeviceService deviceService;

    /**
     * MediaMTX auth webhook for all authentication requests.
     * MediaMTX calls this endpoint to validate publish/read access.
     * 
     * Request body from MediaMTX:
     * {
     * "ip": "...",
     * "user": "...", // not used
     * "password": "...", // not used
     * "path": "live/{deviceId}",
     * "protocol": "rtmp|hls",
     * "id": "...",
     * "action": "publish|read|playback",
     * "query": "token=..."
     * }
     * 
     * @return 200 OK if authorized, 403 Forbidden if not
     */
    @PostMapping("/auth")
    @Operation(summary = "MediaMTX auth webhook", description = "Validates stream publish/playback access")
    public ResponseEntity<Void> authenticate(@Valid @RequestBody AuthWebhookRequest request) {
        String path = request.getPath();
        String action = request.getAction();
        String query = request.getQuery();
        String protocol = request.getProtocol();
        String sourceIp = request.getIp();

        // Log request without sensitive token data (DEBUG level to avoid clutter)
        log.debug("Stream auth: action={}, path={}, protocol={}, ip={}", action, path, protocol, sourceIp);

        // Extract device ID from path (format: live/{deviceId})
        String deviceId = extractDeviceIdFromPath(path);
        if (deviceId == null) {
            log.warn("Stream auth failed: could not extract deviceId from path: {}", path);
            return ResponseEntity.status(403).build();
        }

        // Extract token from query string
        String token = extractTokenFromQuery(query);
        if (token == null) {
            log.warn("Stream auth failed: no token in query for path: {}", path);
            return ResponseEntity.status(403).build();
        }

        boolean authorized;
        if ("publish".equals(action)) {
            // Device pushing RTMP stream - validate device token with IP check
            authorized = streamService.validatePublishAuth(deviceId, token, sourceIp);
        } else if ("read".equals(action) || "playback".equals(action)) {
            // User watching HLS stream - validate playback token
            authorized = streamService.validatePlaybackAuth(deviceId, token);
        } else {
            log.warn("Stream auth failed: unknown action: {}", action);
            authorized = false;
        }

        if (authorized) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(403).build();
        }
    }

    /**
     * Called by MediaMTX when a stream is ready (device connected).
     */
    @PostMapping("/ready")
    @Operation(summary = "Stream ready notification", description = "Called by MediaMTX when stream starts")
    public ResponseEntity<Void> onStreamReady(@Valid @RequestBody PathWebhookRequest request) {
        String path = request.getPath();
        String deviceId = extractDeviceIdFromPath(path);

        if (deviceId != null) {
            log.info("Stream ready for device: {}", deviceId);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Called by MediaMTX when a stream ends (device disconnected).
     */
    @PostMapping("/not-ready")
    @Operation(summary = "Stream ended notification", description = "Called by MediaMTX when stream ends")
    public ResponseEntity<Void> onStreamNotReady(@Valid @RequestBody PathWebhookRequest request) {
        String path = request.getPath();
        String deviceId = extractDeviceIdFromPath(path);

        if (deviceId != null) {
            streamService.markStreamEnded(deviceId);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Get HLS stream URL for a device.
     * Requires the user to own the device.
     * Returns the access token so frontend can append it to the stream URL.
     * 
     * @param deviceId    Device ID
     * @param accessToken Access token from httpOnly cookie
     * @return Stream URL and access token
     */
    @GetMapping("/url/{deviceId}")
    @Operation(summary = "Get stream URL", description = "Returns HLS URL and access token for device stream")
    public ResponseEntity<Map<String, Object>> getStreamUrl(
            @PathVariable String deviceId,
            @CookieValue(name = "access_token", required = false) String accessToken) {

        // Check if user has valid access token
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("Stream URL request denied: no access token provided");
            return ResponseEntity.status(401).build();
        }

        // Verify user owns this device
        if (!deviceService.hasAccessToDevice(deviceId)) {
            log.warn("Stream URL request denied: user does not own device {}", deviceId);
            return ResponseEntity.status(403).build();
        }

        String streamUrl = streamService.getStreamUrl(deviceId);
        boolean isStreaming = streamService.isDeviceStreaming(deviceId);

        return ResponseEntity.ok(Map.of(
                "streamUrl", streamUrl,
                "isStreaming", isStreaming,
                "deviceId", deviceId,
                "accessToken", accessToken));
    }

    /**
     * Request device to start streaming (on-demand).
     * Called by frontend when user opens the stream viewer.
     * Returns a sessionId that must be used for heartbeat and stop requests.
     */
    @PostMapping("/{deviceId}/start")
    @Operation(summary = "Start stream", description = "Request device to start RTMP streaming")
    public ResponseEntity<Map<String, Object>> startStream(
            @PathVariable String deviceId,
            @CookieValue(name = "access_token", required = false) String accessToken) {

        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!deviceService.hasAccessToDevice(deviceId)) {
            log.warn("Stream start denied: user does not own device {}", deviceId);
            return ResponseEntity.status(403).build();
        }

        // Create a unique session ID for this viewer
        String sessionId = streamService.createViewerSession();
        boolean success = streamService.requestStreamStart(deviceId, sessionId);
        long viewerCount = streamService.getViewerCount(deviceId);

        log.info("Stream start requested for device {}: session={}, viewers={}",
                deviceId, sessionId, viewerCount);

        return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "sessionId", sessionId,
                "viewerCount", viewerCount,
                "success", success));
    }

    /**
     * Request device to stop streaming (on-demand).
     * Called by frontend when user leaves the stream viewer.
     */
    @PostMapping("/{deviceId}/stop")
    @Operation(summary = "Stop stream", description = "Request device to stop RTMP streaming")
    public ResponseEntity<Map<String, Object>> stopStream(
            @PathVariable String deviceId,
            @RequestParam(required = false) String sessionId,
            @CookieValue(name = "access_token", required = false) String accessToken) {

        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!deviceService.hasAccessToDevice(deviceId)) {
            log.warn("Stream stop denied: user does not own device {}", deviceId);
            return ResponseEntity.status(403).build();
        }

        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("Stream stop request missing sessionId for device {}", deviceId);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "sessionId is required"));
        }

        boolean success = streamService.requestStreamStop(deviceId, sessionId);
        long viewerCount = streamService.getViewerCount(deviceId);

        log.info("Stream stop requested for device {}: session={}, viewers={}",
                deviceId, sessionId, viewerCount);

        return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "sessionId", sessionId,
                "viewerCount", viewerCount,
                "success", success));
    }

    /**
     * Heartbeat to keep viewer session alive.
     * Called by frontend every 15 seconds while viewing.
     */
    @PostMapping("/{deviceId}/heartbeat")
    @Operation(summary = "Stream heartbeat", description = "Keep viewer session alive")
    public ResponseEntity<Map<String, Object>> heartbeat(
            @PathVariable String deviceId,
            @RequestParam String sessionId,
            @CookieValue(name = "access_token", required = false) String accessToken) {

        if (accessToken == null || accessToken.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        if (!deviceService.hasAccessToDevice(deviceId)) {
            return ResponseEntity.status(403).build();
        }

        boolean extended = streamService.heartbeat(deviceId, sessionId);

        return ResponseEntity.ok(Map.of(
                "deviceId", deviceId,
                "sessionId", sessionId,
                "extended", extended));
    }

    /**
     * Extract device ID from MediaMTX path.
     * Path format: live/{deviceId} or live/{deviceId}/...
     */
    private String extractDeviceIdFromPath(String path) {
        if (path == null || !path.startsWith("live/")) {
            return null;
        }

        String afterLive = path.substring(5); // Remove "live/"
        int slashIndex = afterLive.indexOf('/');

        if (slashIndex > 0) {
            return afterLive.substring(0, slashIndex);
        } else if (!afterLive.isEmpty()) {
            return afterLive;
        }

        return null;
    }

    /**
     * Extract token from query string.
     * Query format: token={value} or token={value}&other=...
     */
    private String extractTokenFromQuery(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }

        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring(6);
            }
        }

        return null;
    }

    @lombok.Data
    public static class AuthWebhookRequest {
        @NotBlank(message = "Path is required")
        private String path;

        @NotBlank(message = "Action is required")
        private String action;

        @NotBlank(message = "Query is required")
        private String query;

        private String protocol;
        private String ip;
    }

    @lombok.Data
    public static class PathWebhookRequest {
        @NotBlank(message = "Path is required")
        private String path;
    }
}
