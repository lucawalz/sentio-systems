import { useForecastContext } from '../context/ForecastContext';

export function useForecastData(_fallbackRefreshInterval: number = 300000) {
    // interval ignored, managed by Provider
    const {
        currentLocationForecast,
        upcomingForecasts,
        dailyForecasts,
        availableCities,
        loading,
        error,
        lastFetchTime,
        lastUpdateInfo,
        refetch,
        updateForecasts
    } = useForecastContext();

    return {
        currentLocationForecast,
        upcomingForecasts,
        dailyForecasts,
        availableCities,
        loading,
        error,
        lastFetchTime,
        lastUpdateInfo,
        refetch,
        updateForecasts
    };
}