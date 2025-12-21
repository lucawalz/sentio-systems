package org.example.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing weather radar metadata from BrightSky API.
 * Stores summary statistics rather than raw precipitation grid data
 * to enable AI analysis while minimizing storage requirements.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "weather_radar_metadata", indexes = {
        @Index(name = "idx_radar_timestamp", columnList = "timestamp"),
        @Index(name = "idx_radar_created_at", columnList = "createdAt")
})
public class WeatherRadarMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Timestamp of the radar snapshot from BrightSky
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * DWD radar product source identifier
     */
    @Column(length = 100)
    private String source;

    /**
     * Center latitude of the radar query
     */
    @Column(nullable = false)
    private Float latitude;

    /**
     * Center longitude of the radar query
     */
    @Column(nullable = false)
    private Float longitude;

    /**
     * Distance in meters used for bounding box
     */
    @Column(nullable = false)
    private Integer distance;

    /**
     * Minimum precipitation value in mm/5min (0.01 mm units from API)
     */
    private Float precipitationMin;

    /**
     * Maximum precipitation value in mm/5min
     */
    private Float precipitationMax;

    /**
     * Average precipitation value across the grid in mm/5min
     */
    private Float precipitationAvg;

    /**
     * Percentage of grid cells with any precipitation (value > 0)
     */
    private Float coveragePercent;

    /**
     * Number of grid cells with precipitation above 1mm/5min (significant rain)
     */
    private Integer significantRainCells;

    /**
     * Total grid cell count for this bounding box
     */
    private Integer totalCells;

    /**
     * GeoJSON bounding box geometry as JSON string
     */
    @Column(columnDefinition = "TEXT")
    private String geometryJson;

    /**
     * Bounding box as "top,left,bottom,right" pixel coordinates
     */
    @Column(length = 50)
    private String bboxPixels;

    /**
     * When this record was created in our database
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
