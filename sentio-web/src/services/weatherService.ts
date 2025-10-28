import { get } from './api';

// Weather data interfaces matching your backend DTOs
export interface WeatherData {
    id: number;
    temperature: number;
    humidity: number;
    pressure: number;
    lux: number;
    uvi: number;
    timestamp: string;
}

export interface WeatherStats {
    totalReadings: number;
    averageTemperature: number;
    averageHumidity: number;
    averagePressure: number;
    maxTemperature: number;
    minTemperature: number;
    maxHumidity: number;
    minHumidity: number;
    lastUpdated: string;
}

const WEATHER_ENDPOINT = import.meta.env.VITE_API_WEATHER_ENDPOINT || '/api/weather';

export const weatherService = {
    // Get latest weather data
    getLatestWeather: async (): Promise<WeatherData | null> => {
        try {
            const result = await get<WeatherData>(`${WEATHER_ENDPOINT}/latest`);
            return result;
        } catch (error) {
            if (error instanceof Error && error.message.includes('database might be empty')) {
                return null; // Return null for empty database
            }
            throw error;
        }
    },

    // Get recent weather data
    getRecentWeather: async (): Promise<WeatherData[]> => {
        try {
            const result = await get<WeatherData[]>(`${WEATHER_ENDPOINT}/recent`);
            return Array.isArray(result) ? result : [];
        } catch (error) {
            if (error instanceof Error && error.message.includes('database might be empty')) {
                return []; // Return empty array for empty database
            }
            throw error;
        }
    },

    // Get all weather data
    getAllWeather: async (): Promise<WeatherData[]> => {
        try {
            const result = await get<WeatherData[]>(`${WEATHER_ENDPOINT}/all`);
            return Array.isArray(result) ? result : [];
        } catch (error) {
            if (error instanceof Error && error.message.includes('database might be empty')) {
                return []; // Return empty array for empty database
            }
            throw error;
        }
    },

    // Get weather statistics
    getWeatherStats: async (): Promise<WeatherStats | null> => {
        try {
            const result = await get<WeatherStats>(`${WEATHER_ENDPOINT}/stats`);
            return result;
        } catch (error) {
            if (error instanceof Error && error.message.includes('database might be empty')) {
                return null; // Return null for empty database
            }
            throw error;
        }
    },
};