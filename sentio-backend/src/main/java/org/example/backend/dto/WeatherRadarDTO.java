package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for weather radar data from BrightSky API.
 * This DTO is used for frontend communication - the actual radar data
 * is fetched directly from BrightSky API by the frontend to avoid
 * storing large binary data in the backend.
 */

// TODO: Check if this is needed for AI prediction
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherRadarDTO {

    /** List of radar records */
    private List<RadarRecord> radar;

    /** GeoJSON-formatted bounding box geometry */
    private Geometry geometry;

    /** Calculated bounding box coordinates */
    private List<Integer> bbox;

    /** Exact position information if coordinates were provided */
    private LatLonPosition latlonPosition;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RadarRecord {
        /** ISO 8601-formatted timestamp of this radar record */
        private LocalDateTime timestamp;

        /** Unique identifier for DWD radar product source */
        private String source;

        /** 5-minute precipitation data (base64 encoded or array based on format) */
        private Object precipitation5;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Geometry {
        /** GeoJSON coordinates array */
        private List<List<List<Double>>> coordinates;

        /** GeoJSON type (typically "Polygon") */
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatLonPosition {
        /** X-position */
        private Double x;

        /** Y-position */
        private Double y;
    }
}