
import { useWeatherContext } from '../context/WeatherContext';

export const useWeatherData = (_refreshInterval: number = 30000) => {
    // refreshInterval ignored, managed by Provider
    const {
        latestWeather,
        recentWeather,
        allWeather,
        weatherStats,
        loading,
        error,
        isEmpty,
        refetch
    } = useWeatherContext();

    return {
        latestWeather,
        recentWeather,
        allWeather,
        weatherStats,
        loading,
        error,
        isEmpty,
        refetch
    };
};