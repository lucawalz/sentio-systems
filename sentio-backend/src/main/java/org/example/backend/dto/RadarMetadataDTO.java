package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for radar metadata response to frontend.
 * Includes summary statistics and the direct BrightSky API URL for live data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RadarMetadataDTO {

    /** Timestamp of the radar data */
    private LocalDateTime timestamp;

    /** DWD source identifier */
    private String source;

    /** Center coordinates */
    private Float latitude;
    private Float longitude;

    /** Distance used for bounding box in meters */
    private Integer distance;

    /** Precipitation statistics in mm/5min */
    private Float precipitationMin;
    private Float precipitationMax;
    private Float precipitationAvg;

    /** Coverage percentage (cells with precipitation) */
    private Float coveragePercent;

    /** Count of cells with significant rain (>1mm) */
    private Integer significantRainCells;

    /** Total cell count */
    private Integer totalCells;

    /** Direct URL to BrightSky API for live data */
    private String directApiUrl;

    /** When this snapshot was recorded */
    private LocalDateTime createdAt;

    /** Whether there is any active precipitation */
    private Boolean hasActivePrecipitation;
}
