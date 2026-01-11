import type {WeatherForecast} from '../services/forecastService';
import { format, parseISO, isToday, isTomorrow, addDays } from 'date-fns';

export const formatForecastTime = (forecastDateTime: string): string => {
    try {
        return format(parseISO(forecastDateTime), 'HH:mm');
    } catch {
        return 'N/A';
    }
};

export const formatForecastDate = (forecastDate: string): string => {
    try {
        const date = parseISO(forecastDate);
        if (isToday(date)) return 'Today';
        if (isTomorrow(date)) return 'Tomorrow';
        return format(date, 'EEEE'); // Return day name like "Friday"
    } catch {
        return 'N/A';
    }
};

export const formatForecastDateLong = (forecastDate: string): string => {
    try {
        return format(parseISO(forecastDate), 'EEEE, MMMM dd');
    } catch {
        return 'N/A';
    }
};

export const getWeatherIcon = (iconCode: string | null): string => {
    if (!iconCode) return '01d'; // Default sunny icon
    return iconCode;
};

export const getWeatherIconUrl = (iconCode: string | null): string => {
    const icon = getWeatherIcon(iconCode);
    return `https://openweathermap.org/img/wn/${icon}@2x.png`;
};

export const formatTemperature = (temp: number | null): string => {
    if (temp === null) return 'N/A';
    return `${Math.round(temp)}°C`;
};

export const formatTemperatureRange = (min: number | null, max: number | null): string => {
    if (min === null || max === null) return 'N/A';
    return `${Math.round(min)}° - ${Math.round(max)}°`;
};

export const formatHumidity = (humidity: number | null): string => {
    if (humidity === null) return 'N/A';
    return `${Math.round(humidity)}%`;
};

export const formatPressure = (pressure: number | null): string => {
    if (pressure === null) return 'N/A';
    return `${Math.round(pressure)} hPa`;
};

export const formatWindSpeed = (speed: number | null): string => {
    if (speed === null) return 'N/A';
    return `${Math.round(speed * 3.6)} km/h`; // Convert m/s to km/h
};

export const formatRainfall = (rain: number | null): string => {
    if (rain === null || rain === 0) return 'No rain';
    return `${rain.toFixed(1)} mm`;
};

export const getWindDirection = (degrees: number | null): string => {
    if (degrees === null) return 'N/A';

    const directions = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE', 'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW'];
    const index = Math.round(degrees / 22.5) % 16;
    return directions[index];
};

export const getWeatherSeverity = (weatherMain: string | null): 'low' | 'medium' | 'high' => {
    if (!weatherMain) return 'low';

    const severe = ['Thunderstorm', 'Tornado', 'Hurricane'];
    const moderate = ['Rain', 'Drizzle', 'Snow', 'Sleet', 'Hail'];

    if (severe.some(w => weatherMain.includes(w))) return 'high';
    if (moderate.some(w => weatherMain.includes(w))) return 'medium';
    return 'low';
};

export const filterTodayForecasts = (forecasts: WeatherForecast[]): WeatherForecast[] => {
    const today = format(new Date(), 'yyyy-MM-dd');
    return forecasts.filter(f => f.forecastDate === today);
};

export const filterNextDaysForecasts = (forecasts: WeatherForecast[], days: number = 5): WeatherForecast[] => {
    const startDate = format(new Date(), 'yyyy-MM-dd');
    const endDate = format(addDays(new Date(), days), 'yyyy-MM-dd');

    return forecasts.filter(f => f.forecastDate >= startDate && f.forecastDate <= endDate);
};

export const groupForecastsByHour = (forecasts: WeatherForecast[]): Record<string, WeatherForecast[]> => {
    return forecasts.reduce((acc, forecast) => {
        const hour = formatForecastTime(forecast.forecastDateTime);
        if (!acc[hour]) {
            acc[hour] = [];
        }
        acc[hour].push(forecast);
        return acc;
    }, {} as Record<string, WeatherForecast[]>);
};

export const getNextForecast = (forecasts: WeatherForecast[]): WeatherForecast | null => {
    const now = new Date();
    const sortedForecasts = forecasts
        .filter(f => new Date(f.forecastDateTime) > now)
        .sort((a, b) => new Date(a.forecastDateTime).getTime() - new Date(b.forecastDateTime).getTime());

    return sortedForecasts[0] || null;
};

export const getCurrentForecast = (forecasts: WeatherForecast[]): WeatherForecast | null => {
    const now = new Date();

    // Find the forecast closest to current time
    return forecasts.reduce((closest, forecast) => {
        const forecastTime = new Date(forecast.forecastDateTime);
        const closestTime = closest ? new Date(closest.forecastDateTime) : null;

        if (!closestTime) return forecast;

        const diffForecast = Math.abs(now.getTime() - forecastTime.getTime());
        const diffClosest = Math.abs(now.getTime() - closestTime.getTime());

        return diffForecast < diffClosest ? forecast : closest;
    }, null as WeatherForecast | null);
};