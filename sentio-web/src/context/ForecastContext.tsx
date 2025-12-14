import React, { createContext, useContext, useState, useEffect, useRef, type ReactNode } from 'react';
import { forecastService, type WeatherForecast, type DailyForecast, type LastUpdateInfo } from '../services/forecastService';

interface ForecastContextType {
    currentLocationForecast: WeatherForecast[];
    upcomingForecasts: WeatherForecast[];
    dailyForecasts: DailyForecast[];
    availableCities: string[];
    loading: boolean;
    error: string | null;
    lastFetchTime: Date | null;
    lastUpdateInfo: LastUpdateInfo | null;
    refetch: () => Promise<void>;
    updateForecasts: () => Promise<boolean>;
    // Pass-through helper methods if needed, though usually direct service calls are fine if stateless
    // but context provides stateful data
}

const ForecastContext = createContext<ForecastContextType | undefined>(undefined);

export const useForecastContext = () => {
    const context = useContext(ForecastContext);
    if (!context) {
        throw new Error('useForecastContext must be used within a ForecastProvider');
    }
    return context;
};

interface ForecastProviderProps {
    children: ReactNode;
    fallbackRefreshInterval?: number;
}

export const ForecastProvider: React.FC<ForecastProviderProps> = ({ children, fallbackRefreshInterval = 300000 }) => {
    const [currentLocationForecast, setCurrentLocationForecast] = useState<WeatherForecast[]>([]);
    const [upcomingForecasts, setUpcomingForecasts] = useState<WeatherForecast[]>([]);
    const [dailyForecasts, setDailyForecasts] = useState<DailyForecast[]>([]);
    const [availableCities, setAvailableCities] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [lastFetchTime, setLastFetchTime] = useState<Date | null>(null);
    const [lastUpdateInfo, setLastUpdateInfo] = useState<LastUpdateInfo | null>(null);

    const intervalRef = useRef<number | null>(null);

    // --- Helper Logic from original hook ---

    const shouldFetchFreshData = async (): Promise<boolean> => {
        if (!lastFetchTime) return true;

        const now = new Date();
        const timeSinceLastFetch = now.getTime() - lastFetchTime.getTime();

        if (timeSinceLastFetch > fallbackRefreshInterval) return true;

        try {
            const updateInfo = await forecastService.getLastUpdateInfo();
            setLastUpdateInfo(updateInfo);

            if (!updateInfo.hasRecentData) return true;

            if (updateInfo.lastUpdated && lastFetchTime) {
                const serverUpdateTime = new Date(updateInfo.lastUpdated);
                return serverUpdateTime > lastFetchTime;
            }

            return false;
        } catch (err) {
            console.warn('Could not check update info, using fallback logic:', err);
            return timeSinceLastFetch > fallbackRefreshInterval;
        }
    };

    const calculateNextRefreshTime = (): number => {
        const now = new Date();
        const currentHour = now.getHours();
        const currentMinutes = now.getMinutes();
        const forecastUpdateHours = [0, 3, 6, 9, 12, 15, 18, 21];

        let nextUpdateHour = forecastUpdateHours.find(hour => hour > currentHour);
        if (!nextUpdateHour) {
            nextUpdateHour = forecastUpdateHours[0];
        }

        let minutesUntilUpdate;
        if (nextUpdateHour > currentHour) {
            minutesUntilUpdate = (nextUpdateHour - currentHour) * 60 - currentMinutes + 5;
        } else {
            minutesUntilUpdate = (24 - currentHour + nextUpdateHour) * 60 - currentMinutes + 5;
        }

        const calculatedTime = minutesUntilUpdate * 60 * 1000;
        return Math.max(5 * 60 * 1000, Math.min(calculatedTime, fallbackRefreshInterval));
    };

    const calculateTrend = (values: number[]): 'stable' | 'rising' | 'falling' => {
        if (values.length < 3) return 'stable';
        const firstHalf = values.slice(0, Math.floor(values.length / 2));
        const secondHalf = values.slice(Math.floor(values.length / 2));
        const firstAvg = firstHalf.reduce((sum, val) => sum + val, 0) / firstHalf.length;
        const secondAvg = secondHalf.reduce((sum, val) => sum + val, 0) / secondHalf.length;
        const threshold = Math.abs(firstAvg) * 0.05;
        const diff = secondAvg - firstAvg;
        if (Math.abs(diff) < threshold) return 'stable';
        return diff > 0 ? 'rising' : 'falling';
    };

    const groupForecastsByDay = (forecasts: WeatherForecast[]): DailyForecast[] => {
        const grouped = forecasts.reduce((acc, forecast) => {
            const date = forecast.forecastDate;
            if (!acc[date]) {
                acc[date] = [];
            }
            acc[date].push(forecast);
            return acc;
        }, {} as Record<string, WeatherForecast[]>);

        return Object.entries(grouped).map(([date, forecasts]) => {
            const sortedForecasts = forecasts.sort((a, b) =>
                new Date(a.forecastDateTime).getTime() - new Date(b.forecastDateTime).getTime()
            );

            // ... (Simplified logic for brevity, relying on correct implementation assumption or copy-paste if needed)
            // Ideally we could move this logic to a utility function if it's complex, 
            // but for now I will assume the original logic is needed.
            // COPYING LOGIC from original hook to ensure functionality match:

            const validTemps = sortedForecasts.filter(f => f.temperature !== null).map(f => f.temperature!);
            const validFeelsLike = sortedForecasts.filter(f => f.apparentTemperature !== null).map(f => f.apparentTemperature!);

            const maxTemp = validTemps.length > 0 ? Math.max(...validTemps) : 0;
            const minTemp = validTemps.length > 0 ? Math.min(...validTemps) : 0;
            const maxFeelsLike = validFeelsLike.length > 0 ? Math.max(...validFeelsLike) : 0;
            const minFeelsLike = validFeelsLike.length > 0 ? Math.min(...validFeelsLike) : 0;
            const avgFeelsLike = validFeelsLike.length > 0 ? validFeelsLike.reduce((sum, temp) => sum + temp, 0) / validFeelsLike.length : 0;

            const validHumidity = sortedForecasts.filter(f => f.humidity !== null).map(f => f.humidity!);
            const avgHumidity = validHumidity.length > 0 ? validHumidity.reduce((sum, h) => sum + h, 0) / validHumidity.length : 0;
            const maxHumidity = validHumidity.length > 0 ? Math.max(...validHumidity) : 0;
            const minHumidity = validHumidity.length > 0 ? Math.min(...validHumidity) : 0;
            const humidityTrend = calculateTrend(validHumidity);

            const validPressure = sortedForecasts.filter(f => f.pressure !== null).map(f => f.pressure!);
            const avgPressure = validPressure.length > 0 ? validPressure.reduce((sum, p) => sum + p, 0) / validPressure.length : 0;
            const maxPressure = validPressure.length > 0 ? Math.max(...validPressure) : 0;
            const minPressure = validPressure.length > 0 ? Math.min(...validPressure) : 0;
            const pressureTrend = calculateTrend(validPressure);

            const validCloudCover = sortedForecasts.filter(f => f.cloudCover !== null).map(f => f.cloudCover!);
            const avgCloudCover = validCloudCover.length > 0 ? validCloudCover.reduce((sum, c) => sum + c, 0) / validCloudCover.length : 0;
            const cloudTrend = calculateTrend(validCloudCover);

            const validPrecipProb = sortedForecasts.filter(f => f.precipitationProbability !== null).map(f => f.precipitationProbability!);
            const avgPrecipitationProbability = validPrecipProb.length > 0 ? validPrecipProb.reduce((sum, p) => sum + p, 0) / validPrecipProb.length : 0;
            const maxPrecipitationProbability = validPrecipProb.length > 0 ? Math.max(...validPrecipProb) : 0;
            const precipitationTrend = calculateTrend(validPrecipProb);

            const validWindSpeed = sortedForecasts.filter(f => f.windSpeed !== null).map(f => f.windSpeed!);
            const validWindDir = sortedForecasts.filter(f => f.windDirection !== null).map(f => f.windDirection!);
            const validWindGusts = sortedForecasts.filter(f => f.windGusts !== null).map(f => f.windGusts!);

            const avgWindSpeed = validWindSpeed.length > 0 ? validWindSpeed.reduce((sum, w) => sum + w, 0) / validWindSpeed.length : 0;
            const avgWindDirection = validWindDir.length > 0 ? validWindDir.reduce((sum, d) => sum + d, 0) / validWindDir.length : 0;
            const maxWindGusts = validWindGusts.length > 0 ? Math.max(...validWindGusts) : 0;

            const totalRain = sortedForecasts.reduce((sum, f) => sum + (f.rain || 0), 0);
            const totalSnow = sortedForecasts.reduce((sum, f) => sum + (f.snowfall || 0), 0);
            const totalPrecipitation = sortedForecasts.reduce((sum, f) => sum + (f.precipitation || 0), 0);

            const weatherCounts = sortedForecasts.reduce((acc, f, index) => {
                if (f.weatherMain) {
                    const weight = 1 + (index / sortedForecasts.length) * 0.2;
                    acc[f.weatherMain] = (acc[f.weatherMain] || 0) + weight;
                }
                return acc;
            }, {} as Record<string, number>);

            const mostCommonWeather = Object.entries(weatherCounts).sort(([, a], [, b]) => b - a)[0]?.[0] || 'Unknown';

            const descriptionCounts = sortedForecasts.reduce((acc, f, index) => {
                if (f.description) {
                    const weight = 1 + (index / sortedForecasts.length) * 0.2;
                    acc[f.description] = (acc[f.description] || 0) + weight;
                }
                return acc;
            }, {} as Record<string, number>);
            const mostCommonDescription = Object.entries(descriptionCounts).sort(([, a], [, b]) => b - a)[0]?.[0] || 'No description';

            const iconCounts = sortedForecasts.reduce((acc, f, index) => {
                if (f.icon) {
                    const weight = 1 + (index / sortedForecasts.length) * 0.2;
                    acc[f.icon] = (acc[f.icon] || 0) + weight;
                }
                return acc;
            }, {} as Record<string, number>);
            const mostCommonIcon = Object.entries(iconCounts).sort(([, a], [, b]) => b - a)[0]?.[0] || '01d';

            return {
                date,
                forecasts: sortedForecasts,
                summary: {
                    maxTemp, minTemp, maxFeelsLike, minFeelsLike, avgFeelsLike,
                    avgHumidity, humidityTrend, minHumidity, maxHumidity,
                    avgPressure, pressureTrend, minPressure, maxPressure,
                    avgCloudCover, cloudTrend, mostCommonWeather, mostCommonDescription, icon: mostCommonIcon,
                    avgPrecipitationProbability, maxPrecipitationProbability, precipitationTrend,
                    avgWindSpeed, avgWindDirection, maxWindGusts,
                    totalRain, totalSnow, totalPrecipitation
                }
            };
        }).sort((a, b) => a.date.localeCompare(b.date));
    };

    const fetchForecastData = async () => {
        try {
            if (currentLocationForecast.length === 0) setLoading(true);
            setError(null);

            const [currentLocation, upcoming, cities] = await Promise.all([
                forecastService.getCurrentLocationForecast(),
                forecastService.getUpcomingForecasts(),
                forecastService.getAvailableCities(),
            ]);

            setCurrentLocationForecast(currentLocation);
            setUpcomingForecasts(upcoming);
            setAvailableCities(cities);

            const grouped = groupForecastsByDay(upcoming);
            setDailyForecasts(grouped);
            setLastFetchTime(new Date());

        } catch (err) {
            console.error("Failed to fetch forecast data:", err);
            setError(err instanceof Error ? err.message : 'Failed to fetch forecast data');
        } finally {
            setLoading(false);
        }
    };

    const scheduleNextFetch = async () => {
        if (intervalRef.current !== null) {
            clearTimeout(intervalRef.current);
        }

        const nextRefreshTime = calculateNextRefreshTime();

        intervalRef.current = setTimeout(async () => {
            const needsUpdate = await shouldFetchFreshData();
            if (needsUpdate) {
                await fetchForecastData();
            }
            scheduleNextFetch();
        }, nextRefreshTime);
    };

    const updateForecasts = async (): Promise<boolean> => {
        try {
            await forecastService.updateForecasts();
            await fetchForecastData();
            return true;
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to update forecasts');
            return false;
        }
    };

    useEffect(() => {
        fetchForecastData();
        scheduleNextFetch();

        return () => {
            if (intervalRef.current !== null) {
                clearTimeout(intervalRef.current);
                intervalRef.current = null;
            }
        };
    }, [fallbackRefreshInterval]);

    return (
        <ForecastContext.Provider value={{
            currentLocationForecast,
            upcomingForecasts,
            dailyForecasts,
            availableCities,
            loading,
            error,
            lastFetchTime,
            lastUpdateInfo,
            refetch: fetchForecastData,
            updateForecasts
        }}>
            {children}
        </ForecastContext.Provider>
    );
};
