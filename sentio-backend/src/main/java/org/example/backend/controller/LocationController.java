package org.example.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.LocationDataDTO;
import org.example.backend.mapper.LocationDataMapper;
import org.example.backend.model.LocationData;
import org.example.backend.service.IpLocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * REST controller for IP-based location detection and management.
 * Provides endpoints for retrieving user location data using IP geolocation services
 * and managing location data cleanup operations.
 */
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Location Services", description = "API for IP-based location detection and management")
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    private final IpLocationService ipLocationService;
    private final LocationDataMapper locationDataMapper;

    /**
     * Retrieves location data for the current user based on their IP address.
     * @return Current user's location information
     */
    @Operation(summary = "Get current user location",
            description = "Retrieves location data for the current user based on their IP address using geolocation services")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved current location",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LocationDataDTO.class))),
            @ApiResponse(responseCode = "404", description = "Unable to determine current location",
                    content = @Content)
    })
    @GetMapping("/current")
    public ResponseEntity<LocationDataDTO> getCurrentLocation() {
        logger.info("Retrieving current location data");
        Optional<LocationData> locationData = ipLocationService.getCurrentLocation();
        if (locationData.isPresent()) {
            LocationDataDTO dto = locationDataMapper.toDTO(locationData.get());
            logger.debug("Retrieved current location: {}, {}", dto.getCity(), dto.getCountry());
            return ResponseEntity.ok(dto);
        }
        logger.debug("Unable to determine current location");
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves location data for a specific IP address.
     * @param ip IP address to lookup (optional, defaults to request IP)
     * @param request HTTP request for IP extraction if not provided
     * @return Location information for the specified IP address
     */
    @Operation(summary = "Get location by IP address",
            description = "Retrieves location data for a specific IP address. If no IP is provided, uses the client's IP from the request headers")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved location for IP address",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LocationDataDTO.class))),
            @ApiResponse(responseCode = "404", description = "No location data found for the specified IP address",
                    content = @Content)
    })
    @GetMapping("/by-ip")
    public ResponseEntity<LocationDataDTO> getLocationByIp(
            @Parameter(description = "IP address to lookup (optional, defaults to client IP from request headers)")
            @RequestParam(required = false) String ip,
            HttpServletRequest request) {

        // If no IP provided, try to get from request
        if (ip == null || ip.isEmpty()) {
            ip = getClientIpAddress(request);
            logger.debug("Using client IP from request: {}", ip);
        }

        logger.info("Retrieving location data for IP: {}", ip);
        Optional<LocationData> locationData = ipLocationService.getLocationByIp(ip);
        if (locationData.isPresent()) {
            LocationDataDTO dto = locationDataMapper.toDTO(locationData.get());
            logger.debug("Retrieved location for IP {}: {}, {}", ip, dto.getCity(), dto.getCountry());
            return ResponseEntity.ok(dto);
        }
        logger.debug("No location data found for IP: {}", ip);
        return ResponseEntity.notFound().build();
    }

    /**
     * Performs cleanup of outdated location data entries.
     * @return Success message
     */
    @Operation(summary = "Cleanup old location data",
            description = "Performs cleanup of outdated location data entries to maintain database performance and storage efficiency")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully cleaned up old location data",
                    content = @Content(mediaType = "text/plain")),
            @ApiResponse(responseCode = "500", description = "Failed to cleanup old location data",
                    content = @Content(mediaType = "text/plain"))
    })
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupOldLocationData() {
        logger.info("Initiating cleanup of old location data");
        try {
            ipLocationService.cleanupOldLocationData();
            logger.info("Successfully completed cleanup of old location data");
            return ResponseEntity.ok("Old location data cleaned up successfully");
        } catch (Exception e) {
            logger.error("Failed to cleanup old location data", e);
            return ResponseEntity.internalServerError().body("Failed to cleanup old location data");
        }
    }

    /**
     * Extracts the client's real IP address from HTTP headers.
     * Handles various proxy and load balancer headers.
     * @param request HTTP request containing headers
     * @return Client's real IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String clientIp = xForwardedFor.split(",")[0];
            logger.debug("Extracted IP from X-Forwarded-For: {}", clientIp);
            return clientIp;
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            logger.debug("Extracted IP from X-Real-IP: {}", xRealIp);
            return xRealIp;
        }

        String remoteAddr = request.getRemoteAddr();
        logger.debug("Using remote address: {}", remoteAddr);
        return remoteAddr;
    }
}