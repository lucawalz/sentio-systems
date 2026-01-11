import React, { createContext, useContext, useState, useEffect, useRef, useCallback, type ReactNode } from 'react';
import { weatherService, type WeatherData, type WeatherStats } from '../services/weatherService';
import { useDeviceContext } from './DeviceContext';
import { useWebSocketSubscription } from './WebSocketContext';

interface WeatherContextType {
    latestWeather: WeatherData | null;
    recentWeather: WeatherData[];
    allWeather: WeatherData[];
    weatherStats: WeatherStats | null;
    loading: boolean;
    error: string | null;
    isEmpty: boolean;
    noDevices: boolean;
    refetch: () => Promise<void>;
}

const WeatherContext = createContext<WeatherContextType | undefined>(undefined);

export const useWeatherContext = () => {
    const context = useContext(WeatherContext);
    if (!context) {
        throw new Error('useWeatherContext must be used within a WeatherProvider');
    }
    return context;
};

interface WeatherProviderProps {
    children: ReactNode;
    refreshInterval?: number;
}

export const WeatherProvider: React.FC<WeatherProviderProps> = ({ children, refreshInterval = 30000 }) => {
    const { hasDevices, loading: devicesLoading } = useDeviceContext();
    const [latestWeather, setLatestWeather] = useState<WeatherData | null>(null);
    const [recentWeather, setRecentWeather] = useState<WeatherData[]>([]);
    const [allWeather, setAllWeather] = useState<WeatherData[]>([]);
    const [weatherStats, setWeatherStats] = useState<WeatherStats | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [isEmpty, setIsEmpty] = useState(false);
    const [noDevices, setNoDevices] = useState(false);

    const intervalRef = useRef<number | null>(null);

    const fetchWeatherData = useCallback(async () => {
        // Skip API call if user has no devices
        if (!hasDevices) {
            setLatestWeather(null);
            setRecentWeather([]);
            setAllWeather([]);
            setWeatherStats(null);
            setNoDevices(true);
            setLoading(false);
            return;
        }

        try {
            setError(null);
            setIsEmpty(false);
            setNoDevices(false);

            const [latest, recent, all, stats] = await Promise.all([
                weatherService.getLatestWeather(),
                weatherService.getRecentWeather(),
                weatherService.getAllWeather(),
                weatherService.getWeatherStats(),
            ]);

            setLatestWeather(prev => {
                // Only show loading on first fetch
                if (prev === null && latest === null) {
                    setLoading(false);
                }
                return latest;
            });
            setRecentWeather(recent || []);
            setAllWeather(all || []);
            setWeatherStats(stats);

            const hasData = latest !== null || (recent && recent.length > 0) || (all && all.length > 0);
            setIsEmpty(!hasData);

        } catch (err) {
            console.error("Failed to fetch weather data:", err);
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
    }, [hasDevices]); // Removed latestWeather - was causing infinite loop!

    // Listen for WebSocket weather updates
    const handleWeatherUpdate = useCallback(() => {
        console.log('[WeatherContext] Received WEATHER_UPDATED event, refetching...');
        fetchWeatherData();
    }, [fetchWeatherData]);

    useWebSocketSubscription('WEATHER_UPDATED', handleWeatherUpdate);

    useEffect(() => {
        // Wait for device check to complete
        if (devicesLoading) return;

        fetchWeatherData();

        // Only set up polling if user has devices
        if (hasDevices) {
            intervalRef.current = setInterval(fetchWeatherData, refreshInterval);
        }

        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
                intervalRef.current = null;
            }
        };
    }, [refreshInterval, hasDevices, devicesLoading, fetchWeatherData]);

    return (
        <WeatherContext.Provider value={{
            latestWeather,
            recentWeather,
            allWeather,
            weatherStats,
            loading: loading || devicesLoading,
            error,
            isEmpty,
            noDevices,
            refetch: fetchWeatherData
        }}>
            {children}
        </WeatherContext.Provider>
    );
};
