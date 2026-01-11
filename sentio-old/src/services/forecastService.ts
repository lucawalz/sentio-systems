
import { get } from './api';

// Updated Weather forecast interface matching your Open-Meteo database structure
export interface WeatherForecast {
    id: number;
    forecastDate: string; // LocalDate as string (YYYY-MM-DD)
    forecastDateTime: string; // LocalDateTime as ISO string
    createdAt: string;
    updatedAt: string;

    // Core weather conditions from Open-Meteo
    temperature: number | null; // Main temperature
    apparentTemperature: number | null; // Feels like temperature
    humidity: number | null;
    pressure: number | null;
    description: string | null;
    weatherMain: string | null;
    icon: string | null;

    // Wind data
    windSpeed: number | null;
    windDirection: number | null; // Changed from windDegree to windDirection
    windGusts: number | null;

    // Sky conditions
    cloudCover: number | null; // Changed from cloudiness to cloudCover
    visibility: number | null;

    // Precipitation data
    precipitation: number | null;
    rain: number | null; // Changed from rainVolume
    showers: number | null;
    snowfall: number | null; // Changed from snowVolume
    snowDepth: number | null;
    precipitationProbability: number | null;

    // Additional weather data
    dewPoint: number | null;
    weatherCode: number | null;

    // Location information
    city: string | null;
    country: string | null;
    latitude: number | null;
    longitude: number | null;
    detectedLocation: string | null;
    ipAddress: string | null;
}

export interface DailyForecast {
    date: string;
    forecasts: WeatherForecast[];
    summary: {
        // Temperature summary with feels like
        maxTemp: number;
        minTemp: number;
        maxFeelsLike: number;
        minFeelsLike: number;
        avgFeelsLike: number;

        // Humidity with trend indication
        avgHumidity: number;
        humidityTrend: 'stable' | 'rising' | 'falling';
        minHumidity: number;
        maxHumidity: number;

        // Pressure with trend indication
        avgPressure: number;
        pressureTrend: 'stable' | 'rising' | 'falling';
        minPressure: number;
        maxPressure: number;

        // Cloud cover with weather main
        avgCloudCover: number;
        cloudTrend: 'stable' | 'rising' | 'falling';
        mostCommonWeather: string;
        mostCommonDescription: string;
        icon: string;

        // Precipitation probability with trend
        avgPrecipitationProbability: number;
        maxPrecipitationProbability: number;
        precipitationTrend: 'stable' | 'rising' | 'falling';

        // Wind (average calculated)
        avgWindSpeed: number;
        avgWindDirection: number;
        maxWindGusts: number;

        // Totals
        totalRain: number;
        totalSnow: number;
        totalPrecipitation: number;
    };
}

export interface ForecastStats {
    totalForecasts: number;
    daysAvailable: number;
    citiesCovered: string[];
    lastUpdated: string;
    oldestForecast: string;
    newestForecast: string;
}

export interface LastUpdateInfo {
    lastUpdated: string | null;
    createdAt: string | null;
    forecastDate: string | null;
    nextUpdateEstimate: string;
    hasRecentData: boolean;
}

const FORECAST_ENDPOINT = import.meta.env.VITE_API_FORECAST_ENDPOINT || '/api/forecast';

export const forecastService = {
    getCurrentLocationForecast: (): Promise<WeatherForecast[]> =>
        get<WeatherForecast[]>(`${FORECAST_ENDPOINT}/current-location`),

    getUpcomingForecasts: (): Promise<WeatherForecast[]> =>
        get<WeatherForecast[]>(`${FORECAST_ENDPOINT}/upcoming`),

    getForecastsForDateRange: (startDate: string, endDate: string): Promise<WeatherForecast[]> =>
        get<WeatherForecast[]>(`${FORECAST_ENDPOINT}/date-range?startDate=${startDate}&endDate=${endDate}`),

    getForecastsForDate: (date: string): Promise<WeatherForecast[]> =>
        get<WeatherForecast[]>(`${FORECAST_ENDPOINT}/date/${date}`),

    getLatestForecastForDate: (date: string): Promise<WeatherForecast> =>
        get<WeatherForecast>(`${FORECAST_ENDPOINT}/latest/${date}`),

    getRecentForecasts: (hours: number = 24): Promise<WeatherForecast[]> =>
        get<WeatherForecast[]>(`${FORECAST_ENDPOINT}/recent?hours=${hours}`),

    getAvailableCities: (): Promise<string[]> =>
        get<string[]>(`${FORECAST_ENDPOINT}/cities`),

    updateForecasts: (): Promise<string> =>
        get<string>(`${FORECAST_ENDPOINT}/update`, { method: 'POST' }),

    getLastUpdateInfo: (): Promise<LastUpdateInfo> =>
        get<LastUpdateInfo>(`${FORECAST_ENDPOINT}/last-update`),

    cleanupOldForecasts: (): Promise<string> =>
        get<string>(`${FORECAST_ENDPOINT}/cleanup`, { method: 'DELETE' }),
};