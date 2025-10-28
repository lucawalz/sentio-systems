package org.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for IP-based location information.
 * Contains geolocation data derived from IP address lookup services
 * for user location detection and geographic features.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "IP-based location information derived from geolocation lookup services")
public class LocationDataDTO {

    @Schema(description = "Unique identifier for the location record", example = "1")
    private Long id;

    @Schema(description = "IP address used for geolocation lookup", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "City name from geolocation service", example = "Berlin")
    private String city;

    @Schema(description = "Country name from geolocation service", example = "Germany")
    private String country;

    @Schema(description = "Region or state name from geolocation service", example = "Baden-Württemberg")
    private String region;

    @Schema(description = "Geographic latitude coordinate", example = "52.5200", minimum = "-90.0", maximum = "90.0")
    private Float latitude;

    @Schema(description = "Geographic longitude coordinate", example = "13.4050", minimum = "-180.0", maximum = "180.0")
    private Float longitude;

    @Schema(description = "Timezone identifier", example = "Europe/Berlin")
    private String timezone;

    @Schema(description = "Internet Service Provider name", example = "Deutsche Telekom AG")
    private String isp;

    @Schema(description = "Organization associated with the IP address", example = "T-Online")
    private String organization;

    @Schema(description = "Record creation timestamp", example = "2024-01-15T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Record last update timestamp", example = "2024-01-15T08:30:00")
    private LocalDateTime updatedAt;
}