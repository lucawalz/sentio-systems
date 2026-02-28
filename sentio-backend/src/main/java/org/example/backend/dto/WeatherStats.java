package org.example.backend.dto;

import org.example.backend.model.RaspiWeatherData;

/**
 * Data Transfer Object for weather statistics and analytics.
 * Contains aggregated weather data including totals and averages.
 */
public class WeatherStats {
    private Long totalReadings;
    private RaspiWeatherData latest;
    private Double avgTemperature;
    private Double avgHumidity;
    private Double avgPressure;

    public WeatherStats(Long totalReadings, RaspiWeatherData latest,
                        Double avgTemperature, Double avgHumidity, Double avgPressure) {
        this.totalReadings = totalReadings;
        this.latest = latest;
        this.avgTemperature = avgTemperature;
        this.avgHumidity = avgHumidity;
        this.avgPressure = avgPressure;
    }

    public Long getTotalReadings() { return totalReadings; }
    public void setTotalReadings(Long totalReadings) { this.totalReadings = totalReadings; }

    public RaspiWeatherData getLatest() { return latest; }
    public void setLatest(RaspiWeatherData latest) { this.latest = latest; }

    public Double getAvgTemperature() { return avgTemperature; }
    public void setAvgTemperature(Double avgTemperature) { this.avgTemperature = avgTemperature; }

    public Double getAvgHumidity() { return avgHumidity; }
    public void setAvgHumidity(Double avgHumidity) { this.avgHumidity = avgHumidity; }

    public Double getAvgPressure() { return avgPressure; }
    public void setAvgPressure(Double avgPressure) { this.avgPressure = avgPressure; }
}
