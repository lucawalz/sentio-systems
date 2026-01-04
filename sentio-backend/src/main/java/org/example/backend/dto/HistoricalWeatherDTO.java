package org.example.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for historical weather information.
 * Contains comprehensive past weather conditions, temporal data, and location
 * details
 * for API communication and frontend integration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historical weather information with comprehensive past weather conditions and location details")
public class HistoricalWeatherDTO {

        @Schema(description = "Unique identifier for the historical weather record", example = "1")
        private Long id;

        @Schema(description = "Date when the weather conditions occurred", example = "2024-01-15")
        private LocalDate weatherDate;

        @Schema(description = "Record creation timestamp", example = "2024-01-15T08:00:00")
        private LocalDateTime createdAt;

        @Schema(description = "Record last update timestamp", example = "2024-01-15T08:30:00")
        private LocalDateTime updatedAt;

        // Weather conditions
        @Schema(description = "Weather code following WMO standards", example = "3", allowableValues = { "0", "1", "2",
                        "3", "45", "48", "51", "53", "55", "56", "57", "61", "63", "65", "66", "67", "71", "73", "75",
                        "77", "80", "81", "82", "85", "86", "95", "96", "99" })
        private Integer weatherCode;

        @Schema(description = "Maximum temperature in Celsius", example = "25.5")
        private Float maxTemperature;

        @Schema(description = "Minimum temperature in Celsius", example = "15.2")
        private Float minTemperature;

        @Schema(description = "Mean/average temperature in Celsius (calculated from max and min)", example = "20.35")
        private Float temperatureMean;

        @Schema(description = "Sunrise time", example = "2024-01-15T07:15:00")
        private LocalDateTime sunrise;

        @Schema(description = "Sunset time", example = "2024-01-15T18:45:00")
        private LocalDateTime sunset;

        @Schema(description = "Daylight duration in seconds", example = "41400")
        private Float daylightDuration;

        @Schema(description = "Sunshine duration in seconds", example = "28800")
        private Float sunshineDuration;

        @Schema(description = "Maximum UV index for the day", example = "7.5", minimum = "0.0", maximum = "12.0")
        private Float uvIndexMax;

        @Schema(description = "Total precipitation in mm", example = "12.5", minimum = "0.0")
        private Float precipitationSum;

        @Schema(description = "Number of hours with precipitation", example = "3.5", minimum = "0.0", maximum = "24.0")
        private Float precipitationHours;

        @Schema(description = "Maximum wind speed at 10m in m/s", example = "15.7", minimum = "0.0")
        private Float windSpeedMax;

        @Schema(description = "Dominant wind direction in degrees", example = "270.0", minimum = "0.0", maximum = "360.0")
        private Float windDirectionDominant;

        @Schema(description = "Human-readable weather description", example = "Partly cloudy with light rain")
        private String description;

        @Schema(description = "Main weather category", example = "Rain", allowableValues = { "Clear", "Clouds", "Rain",
                        "Drizzle", "Thunderstorm", "Snow", "Mist", "Fog" })
        private String weatherMain;

        @Schema(description = "Weather condition icon code", example = "10d")
        private String icon;

        // Location information
        @Schema(description = "City name for the weather location", example = "Berlin")
        private String city;

        @Schema(description = "Country name for the weather location", example = "Germany")
        private String country;

        @Schema(description = "Geographic latitude coordinate", example = "52.5200", minimum = "-90.0", maximum = "90.0")
        private Float latitude;

        @Schema(description = "Geographic longitude coordinate", example = "13.4050", minimum = "-180.0", maximum = "180.0")
        private Float longitude;

        @Schema(description = "Formatted location string", example = "Berlin, Germany")
        private String detectedLocation;
}