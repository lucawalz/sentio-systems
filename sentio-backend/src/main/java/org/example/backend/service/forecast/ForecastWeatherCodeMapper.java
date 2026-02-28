package org.example.backend.service.forecast;

import org.springframework.stereotype.Component;

/**
 * Mapper component for translating Open-Meteo weather codes into domain display values.
 * Converts numeric weather codes into main category, description, and icon tuple.
 *
 * @author Sentio Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class ForecastWeatherCodeMapper {

    /**
     * Maps Open-Meteo weather interpretation code to human-readable metadata.
     *
     * @param code Open-Meteo/WMO weather code
     * @return Array containing weather main, description, and icon identifier
     */
    public String[] mapWeatherCode(int code) {
        return switch (code) {
            case 0 -> new String[] { "Clear", "Clear sky", "01d" };
            case 1 -> new String[] { "Clouds", "Mainly clear", "02d" };
            case 2 -> new String[] { "Clouds", "Partly cloudy", "03d" };
            case 3 -> new String[] { "Clouds", "Overcast", "04d" };
            case 45 -> new String[] { "Mist", "Fog", "50d" };
            case 48 -> new String[] { "Mist", "Depositing rime fog", "50d" };
            case 51 -> new String[] { "Drizzle", "Light drizzle", "09d" };
            case 53 -> new String[] { "Drizzle", "Moderate drizzle", "09d" };
            case 55 -> new String[] { "Drizzle", "Dense drizzle", "09d" };
            case 56 -> new String[] { "Drizzle", "Light freezing drizzle", "09d" };
            case 57 -> new String[] { "Drizzle", "Dense freezing drizzle", "09d" };
            case 61 -> new String[] { "Rain", "Slight rain", "10d" };
            case 63 -> new String[] { "Rain", "Moderate rain", "10d" };
            case 65 -> new String[] { "Rain", "Heavy rain", "10d" };
            case 66 -> new String[] { "Rain", "Light freezing rain", "10d" };
            case 67 -> new String[] { "Rain", "Heavy freezing rain", "10d" };
            case 71 -> new String[] { "Snow", "Slight snow fall", "13d" };
            case 73 -> new String[] { "Snow", "Moderate snow fall", "13d" };
            case 75 -> new String[] { "Snow", "Heavy snow fall", "13d" };
            case 77 -> new String[] { "Snow", "Snow grains", "13d" };
            case 80 -> new String[] { "Rain", "Slight rain showers", "09d" };
            case 81 -> new String[] { "Rain", "Moderate rain showers", "09d" };
            case 82 -> new String[] { "Rain", "Violent rain showers", "09d" };
            case 85 -> new String[] { "Snow", "Slight snow showers", "13d" };
            case 86 -> new String[] { "Snow", "Heavy snow showers", "13d" };
            case 95 -> new String[] { "Thunderstorm", "Thunderstorm", "11d" };
            case 96 -> new String[] { "Thunderstorm", "Thunderstorm with slight hail", "11d" };
            case 99 -> new String[] { "Thunderstorm", "Thunderstorm with heavy hail", "11d" };
            default -> new String[] { "Unknown", "Unknown weather condition", "01d" };
        };
    }
}