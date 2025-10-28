
import { useState, useEffect, useRef } from 'react';
import { weatherService, type WeatherData, type WeatherStats } from '../services/weatherService';

export const useWeatherData = (refreshInterval: number = 300000) => {
    const [latestWeather, setLatestWeather] = useState<WeatherData | null>(null);
    const [recentWeather, setRecentWeather] = useState<WeatherData[]>([]);
    const [allWeather, setAllWeather] = useState<WeatherData[]>([]); // Add this
    const [weatherStats, setWeatherStats] = useState<WeatherStats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isEmpty, setIsEmpty] = useState(false);

    // Use ref to store the current interval ID (browser environment)
    const intervalRef = useRef<number | null>(null);

    const fetchWeatherData = async () => {
        try {
            setLoading(true);
            setError(null);
            setIsEmpty(false);

            const [latest, recent, all, stats] = await Promise.all([
                weatherService.getLatestWeather(),
                weatherService.getRecentWeather(),
                weatherService.getAllWeather(), // Add this
                weatherService.getWeatherStats(),
            ]);

            setLatestWeather(latest);
            setRecentWeather(recent || []);
            setAllWeather(all || []); // Add this
            setWeatherStats(stats);

            // Check if database is empty
            const hasData = latest !== null || (recent && recent.length > 0) || (all && all.length > 0);
            setIsEmpty(!hasData);

        } catch (err) {
            const errorMessage = err instanceof Error ? err.message : 'Failed to fetch weather data';

            if (errorMessage.includes('database might be empty')) {
                setIsEmpty(true);
                setError('No weather data available yet');
            } else if (errorMessage.includes('Cannot connect to server')) {
                setError('Cannot connect to weather station - check if backend is running');
            } else {
                setError(errorMessage);
            }
        } finally {
            setLoading(false);
        }
    };


    useEffect(() => {
        // Clear existing interval first
        if (intervalRef.current !== null) {
            clearInterval(intervalRef.current);
        }

        // Fetch data immediately
        fetchWeatherData();

        // Set up new interval
        intervalRef.current = setInterval(fetchWeatherData, refreshInterval);

        // Cleanup function
        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
                intervalRef.current = null;
            }
        };
    }, [refreshInterval]);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
            }
        };
    }, []);

    return {
        latestWeather,
        allWeather,
        recentWeather,
        weatherStats,
        loading,
        error,
        isEmpty,
        refetch: fetchWeatherData,
    };
};