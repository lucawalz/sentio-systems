
package org.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * JPA entity representing IP-based geographic location data from geolocation
 * services.
 * Stores comprehensive location information resolved from IP addresses for
 * caching and analysis.
 * <p>
 * This entity caches geolocation data from external APIs (like ip-api.com) to
 * minimize
 * API calls and improve performance. It includes geographic coordinates,
 * administrative
 * regions, network provider information, and automatic timestamping for cache
 * management.
 * The data is primarily used for location-based weather services and analytics.
 * </p>
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@Table(name = "location_data")
public class LocationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Device ID that this location data belongs to */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    /** IP address used for geolocation lookup */
    @Column(nullable = false)
    private String ipAddress;

    /** City name from geolocation service */
    @Column(nullable = false)
    private String city;

    /** Country name from geolocation service */
    @Column(nullable = false)
    private String country;

    /** Region/state name from geolocation service */
    @Column(nullable = false)
    private String region;

    /** Geographic latitude coordinate (decimal degrees) */
    @Column(nullable = false)
    private Float latitude;

    /** Geographic longitude coordinate (decimal degrees) */
    @Column(nullable = false)
    private Float longitude;

    /** Timezone identifier (e.g., "America/New_York") */
    private String timezone;

    /** Internet Service Provider name */
    private String isp;

    /** Organization name associated with the IP */
    private String organization;

    /** Timestamp when this record was first created */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Timestamp when this record was last updated */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback executed before entity persistence.
     * Automatically sets creation and update timestamps.
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        log.debug("LocationData entity created for IP: {} at {}", this.ipAddress, now);
    }

    /**
     * JPA lifecycle callback executed before entity updates.
     * Automatically updates the modification timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        log.debug("LocationData entity updated for IP: {} at {}", this.ipAddress, this.updatedAt);
    }
}