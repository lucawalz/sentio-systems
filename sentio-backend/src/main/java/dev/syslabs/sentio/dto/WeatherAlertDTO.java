package dev.syslabs.sentio.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for weather alert information from BrightSky API.
 * Contains comprehensive alert data including severity levels, timing, and multilingual content
 * for API communication and frontend integration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Weather alert information from BrightSky API with comprehensive alert data including severity levels and multilingual content")
public class WeatherAlertDTO {

    @Schema(description = "Unique identifier for the alert record", example = "1")
    private Long id;

    @Schema(description = "BrightSky-internal ID for this alert", example = "12345")
    private Integer brightSkyId;

    @Schema(description = "Unique CAP message identifier", example = "2.49.0.1.276.0.DWD.PVW.1642089600000.ec7d68e0-a4b7-4b5a-9c7e-8e5f7e7a2b3c")
    private String alertId;

    @Schema(description = "Alert status", example = "actual", allowableValues = {"actual", "test"})
    private String status;

    @Schema(description = "Alert issue time", example = "2024-01-15T08:00:00")
    private LocalDateTime effective;

    @Schema(description = "Expected event begin time", example = "2024-01-15T12:00:00")
    private LocalDateTime onset;

    @Schema(description = "Expected event end time", example = "2024-01-15T20:00:00")
    private LocalDateTime expires;

    @Schema(description = "Alert category", example = "met", allowableValues = {"met", "health", "null"})
    private String category;

    @Schema(description = "Code denoting type of action recommended", example = "prepare", allowableValues = {"prepare", "execute", "avoid", "monitor"})
    private String responseType;

    @Schema(description = "Alert time frame", example = "immediate", allowableValues = {"immediate", "future", "null"})
    private String urgency;

    @Schema(description = "Alert severity", example = "severe", allowableValues = {"minor", "moderate", "severe", "extreme", "null"})
    private String severity;

    @Schema(description = "Alert certainty", example = "likely", allowableValues = {"observed", "likely", "null"})
    private String certainty;

    @Schema(description = "DWD event code", example = "42")
    private Integer eventCode;

    @Schema(description = "Label for DWD event code in English", example = "Heavy thunderstorms")
    private String eventEn;

    @Schema(description = "Label for DWD event code in German", example = "Schwere Gewitter")
    private String eventDe;

    @Schema(description = "Alert headline in English", example = "Severe thunderstorms expected")
    private String headlineEn;

    @Schema(description = "Alert headline in German", example = "Schwere Gewitter erwartet")
    private String headlineDe;

    @Schema(description = "Alert description in English", example = "Heavy thunderstorms with potential for damaging winds and hail are expected this afternoon.")
    private String descriptionEn;

    @Schema(description = "Alert description in German", example = "Schwere Gewitter mit Potenzial für schädliche Winde und Hagel werden heute Nachmittag erwartet.")
    private String descriptionDe;

    @Schema(description = "Additional instructions and safety advice in English", example = "Avoid outdoor activities and secure loose objects.")
    private String instructionEn;

    @Schema(description = "Additional instructions and safety advice in German", example = "Vermeiden Sie Aktivitäten im Freien und sichern Sie lose Gegenstände.")
    private String instructionDe;

    // Location information
    @Schema(description = "Municipality warn cell ID", example = "109077000")
    private Long warnCellId;

    @Schema(description = "Municipality name", example = "Stadt Böblingen")
    private String name;

    @Schema(description = "Shortened municipality name", example = "Böblingen")
    private String nameShort;

    @Schema(description = "District name", example = "Böblingen")
    private String district;

    @Schema(description = "Federal state name", example = "Baden-Württemberg")
    private String state;

    @Schema(description = "Shortened federal state name", example = "BW")
    private String stateShort;

    @Schema(description = "City name for the alert location", example = "Böblingen")
    private String city;

    @Schema(description = "Country name for the alert location", example = "Germany")
    private String country;

    @Schema(description = "Geographic latitude coordinate", example = "48.6856", minimum = "-90.0", maximum = "90.0")
    private Float latitude;

    @Schema(description = "Geographic longitude coordinate", example = "9.0098", minimum = "-180.0", maximum = "180.0")
    private Float longitude;

    @Schema(description = "Record creation timestamp", example = "2024-01-15T08:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "Record last update timestamp", example = "2024-01-15T08:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Whether the alert is currently active", example = "true")
    private Boolean isActive;

    @Schema(description = "Localized headline based on user preference", example = "Severe thunderstorms expected")
    private String localizedHeadline;

    @Schema(description = "Localized description based on user preference", example = "Heavy thunderstorms with potential for damaging winds and hail are expected this afternoon.")
    private String localizedDescription;

    @Schema(description = "Localized event name based on user preference", example = "Heavy thunderstorms")
    private String localizedEvent;

    @Schema(description = "Localized instruction based on user preference", example = "Avoid outdoor activities and secure loose objects.")
    private String localizedInstruction;
}