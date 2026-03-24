package dev.syslabs.sentio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for hourly weather forecast information from Open-Meteo API.
 * Contains comprehensive hourly weather conditions, temporal data, and location details
 * for API communication and frontend integration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hourly weather forecast information from Open-Meteo API with comprehensive weather conditions and location details")
public class WeatherForecastDTO {

    @Schema(description = "Unique identifier for the forecast record", example = "1")
    private Long id;

    @Schema(description = "Date component of the forecast for daily grouping", example = "2024-01-15")
    private LocalDate forecastDate;

    @Schema(description = "Complete forecast timestamp including time (hourly precision)", example = "2024-01-15T14:00:00")
    private LocalDateTime forecastDateTime;

    @Schema(description = "Record creation timestamp", example = "2024-01-15T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Record last update timestamp", example = "2024-01-15T08:30:00")
    private LocalDateTime updatedAt;

    // Core temperature fields
    @Schema(description = "Temperature at 2m height in Celsius", example = "22.5")
    private Float temperature;

    @Schema(description = "Relative humidity percentage", example = "65.0", minimum = "0.0", maximum = "100.0")
    private Float humidity;

    @Schema(description = "Apparent temperature (feels like) in Celsius", example = "24.2")
    private Float apparentTemperature;

    @Schema(description = "Surface pressure in hPa", example = "1013.25", minimum = "900.0", maximum = "1100.0")
    private Float pressure;

    @Schema(description = "Human-readable weather description", example = "Partly cloudy")
    private String description;

    @Schema(description = "Main weather category", example = "Clouds",
            allowableValues = {"Clear", "Clouds", "Rain", "Drizzle", "Thunderstorm", "Snow", "Mist", "Fog"})
    private String weatherMain;

    @Schema(description = "Weather condition icon code", example = "02d")
    private String icon;

    // Wind data
    @Schema(description = "Wind speed at 10m height in m/s", example = "5.2", minimum = "0.0")
    private Float windSpeed;

    @Schema(description = "Wind direction at 10m height in degrees", example = "270.0", minimum = "0.0", maximum = "360.0")
    private Float windDirection;

    @Schema(description = "Wind gusts at 10m height in m/s", example = "8.5", minimum = "0.0")
    private Float windGusts;

    @Schema(description = "Cloud coverage percentage", example = "45.0", minimum = "0.0", maximum = "100.0")
    private Float cloudCover;

    @Schema(description = "Visibility in meters", example = "10000.0", minimum = "0.0")
    private Float visibility;

    // Precipitation data
    @Schema(description = "Total precipitation in mm", example = "2.5", minimum = "0.0")
    private Float precipitation;

    @Schema(description = "Rain volume in mm", example = "2.0", minimum = "0.0")
    private Float rain;

    @Schema(description = "Showers volume in mm", example = "0.5", minimum = "0.0")
    private Float showers;

    @Schema(description = "Snowfall volume in mm", example = "0.0", minimum = "0.0")
    private Float snowfall;

    @Schema(description = "Snow depth in meters", example = "0.0", minimum = "0.0")
    private Float snowDepth;

    @Schema(description = "Dew point temperature at 2m height in Celsius", example = "15.8")
    private Float dewPoint;

    @Schema(description = "Precipitation probability percentage", example = "30.0", minimum = "0.0", maximum = "100.0")
    private Float precipitationProbability;

    @Schema(description = "WMO Weather interpretation code", example = "3",
            allowableValues = {"0", "1", "2", "3", "45", "48", "51", "53", "55", "56", "57", "61", "63", "65", "66", "67", "71", "73", "75", "77", "80", "81", "82", "85", "86", "95", "96", "99"})
    private Integer weatherCode;

    // Location information
    @Schema(description = "City name for the forecast location", example = "Berlin")
    private String city;

    @Schema(description = "Country name for the forecast location", example = "Germany")
    private String country;

    @Schema(description = "Geographic latitude coordinate", example = "52.5200", minimum = "-90.0", maximum = "90.0")
    private Float latitude;

    @Schema(description = "Geographic longitude coordinate", example = "13.4050", minimum = "-180.0", maximum = "180.0")
    private Float longitude;

    @Schema(description = "Formatted location string", example = "Berlin, Germany")
    private String detectedLocation;
}