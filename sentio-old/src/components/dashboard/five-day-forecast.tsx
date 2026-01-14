
import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import {
    Sun,
    Cloud,
    CloudRain,
    Cloudy,
    CloudSnow,
    Zap,
    Wind,
    CloudDrizzle,
    Database,
    WifiOff,
    Calendar,
    Droplets,
    RefreshCw,
    Umbrella,
    Minus,
    TrendingUp,
    TrendingDown,
    Gauge,
    MapPin
} from "lucide-react"
import { useForecastData } from '../../hooks/useForecastData'
import { formatForecastDate, formatWindSpeed, getWindDirection } from '../../utils/forecastUtils'

// Enhanced weather icon mapping with more precise conditions
const getWeatherIcon = (weatherMain: string | null, description: string | null) => {
    if (!weatherMain) return { icon: Sun, color: "from-amber-400 to-amber-600", shadow: "shadow-amber-500/30" };

    const main = weatherMain.toLowerCase();
    const desc = description?.toLowerCase() || "";

    switch (main) {
        case 'clear':
            return {
                icon: Sun,
                color: "from-amber-400 to-amber-600",
                shadow: "shadow-amber-500/30"
            };
        case 'clouds':
            if (desc.includes('few') || desc.includes('scattered')) {
                return {
                    icon: Cloud,
                    color: "from-gray-400 to-gray-600",
                    shadow: "shadow-gray-500/30"
                };
            } else if (desc.includes('broken') || desc.includes('overcast')) {
                return {
                    icon: Cloudy,
                    color: "from-slate-500 to-slate-700",
                    shadow: "shadow-slate-500/30"
                };
            }
            return {
                icon: Cloud,
                color: "from-gray-500 to-gray-700",
                shadow: "shadow-gray-500/30"
            };
        case 'rain':
            if (desc.includes('light') || desc.includes('drizzle')) {
                return {
                    icon: CloudDrizzle,
                    color: "from-blue-400 to-blue-600",
                    shadow: "shadow-blue-500/30"
                };
            }
            return {
                icon: CloudRain,
                color: "from-blue-500 to-blue-700",
                shadow: "shadow-blue-500/30"
            };
        case 'drizzle':
            return {
                icon: CloudDrizzle,
                color: "from-blue-400 to-blue-600",
                shadow: "shadow-blue-500/30"
            };
        case 'snow':
            return {
                icon: CloudSnow,
                color: "from-indigo-400 to-indigo-600",
                shadow: "shadow-indigo-500/30"
            };
        case 'thunderstorm':
            return {
                icon: Zap,
                color: "from-purple-500 to-purple-700",
                shadow: "shadow-purple-500/30"
            };
        case 'mist':
        case 'fog':
        case 'haze':
            return {
                icon: Wind,
                color: "from-gray-400 to-gray-600",
                shadow: "shadow-gray-500/30"
            };
        default:
            return {
                icon: Sun,
                color: "from-amber-400 to-amber-600",
                shadow: "shadow-amber-500/30"
            };
    }
};

// Helper function to get trend icon
const getTrendIcon = (trend: string) => {
    switch (trend) {
        case 'rising':
            return TrendingUp;
        case 'falling':
            return TrendingDown;
        case 'stable':
        default:
            return Minus;
    }
};

// Helper function to get trend color
const getTrendColor = (trend: string) => {
    switch (trend) {
        case 'rising':
            return 'text-green-500';
        case 'falling':
            return 'text-red-500';
        case 'stable':
        default:
            return 'text-gray-500';
    }
};

export function FiveDayForecast() {
    const cardRef = useRef<HTMLDivElement>(null)
    const { dailyForecasts, loading, error, noDevices, refetch } = useForecastData()

    useEffect(() => {
        if (cardRef.current) {
            gsap.fromTo(
                cardRef.current,
                { y: 30, opacity: 0, scale: 0.95 },
                { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out", delay: 0.2 }
            )
        }
    }, [])

    // Prepare forecast data with new structure
    const forecast = dailyForecasts.slice(0, 5).map((daily) => {
        const { icon, color, shadow } = getWeatherIcon(
            daily.summary.mostCommonWeather,
            daily.summary.mostCommonDescription
        );

        // Format day name with better logic
        let dayName = formatForecastDate(daily.date);
        const today = new Date().toISOString().split('T')[0];
        const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];

        if (daily.date === today) dayName = 'Today';
        else if (daily.date === tomorrow) dayName = 'Tomorrow';

        return {
            day: dayName,
            icon,
            high: Math.round(daily.summary.maxTemp),
            low: Math.round(daily.summary.minTemp),
            feelsLike: Math.round(daily.summary.avgFeelsLike),
            condition: daily.summary.mostCommonDescription || 'Unknown',
            color,
            shadow,

            // Enhanced data with trends
            humidity: {
                value: Math.round(daily.summary.avgHumidity),
                trend: daily.summary.humidityTrend,
                icon: getTrendIcon(daily.summary.humidityTrend),
                color: getTrendColor(daily.summary.humidityTrend)
            },
            pressure: {
                value: Math.round(daily.summary.avgPressure),
                trend: daily.summary.pressureTrend,
                icon: getTrendIcon(daily.summary.pressureTrend),
                color: getTrendColor(daily.summary.pressureTrend)
            },
            cloudCover: {
                value: Math.round(daily.summary.avgCloudCover),
                weather: daily.summary.mostCommonWeather,
                trend: daily.summary.cloudTrend,
                icon: getTrendIcon(daily.summary.cloudTrend),
                color: getTrendColor(daily.summary.cloudTrend)
            },
            precipitationProbability: {
                value: Math.round(daily.summary.avgPrecipitationProbability),
                max: Math.round(daily.summary.maxPrecipitationProbability),
                trend: daily.summary.precipitationTrend,
                icon: getTrendIcon(daily.summary.precipitationTrend),
                color: getTrendColor(daily.summary.precipitationTrend)
            },
            wind: {
                speed: formatWindSpeed(daily.summary.avgWindSpeed),
                direction: getWindDirection(daily.summary.avgWindDirection),
                directionDegrees: Math.round(daily.summary.avgWindDirection),
                gusts: formatWindSpeed(daily.summary.maxWindGusts)
            }
        };
    });

    if (loading) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[500px] relative overflow-hidden shadow-xl">
                <div className="flex items-center justify-center h-full flex-col space-y-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <div className="text-sm text-muted-foreground">Loading forecast data...</div>
                </div>
            </div>
        )
    }

    if (noDevices) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[500px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <div className="w-16 h-16 rounded-full bg-blue-500/10 flex items-center justify-center mb-4">
                        <MapPin className="w-8 h-8 text-blue-500" />
                    </div>
                    <div className="text-xl font-semibold mb-2 text-foreground">No Devices Registered</div>
                    <p className="text-sm text-muted-foreground max-w-xs mb-4">
                        Register a device to see weather forecasts for your location.
                    </p>
                    <a
                        href="/dashboard#devices"
                        className="px-6 py-3 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors font-medium flex items-center gap-2"
                    >
                        <MapPin className="w-4 h-4" />
                        Register Device
                    </a>
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[500px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <WifiOff className="w-16 h-16 text-destructive mb-4" />
                    <div className="text-xl font-semibold mb-2 text-destructive">Forecast Unavailable</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">
                        Unable to load weather forecast data. Please check your internet connection and try again.
                    </div>
                    <div className="text-xs text-muted-foreground mb-4 opacity-70">{error}</div>
                    <button
                        onClick={refetch}
                        className="px-6 py-3 bg-destructive text-destructive-foreground rounded-lg hover:bg-destructive/90 transition-colors font-medium flex items-center gap-2"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Retry
                    </button>
                </div>
            </div>
        )
    }

    if (forecast.length === 0) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[500px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <Database className="w-16 h-16 text-muted-foreground mb-4" />
                    <div className="text-xl font-semibold mb-2 text-foreground">No Forecast Data</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">
                        Weather forecast data is not available yet. The system may still be collecting data from external weather services.
                    </div>
                    <button
                        onClick={refetch}
                        className="px-6 py-3 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors font-medium flex items-center gap-2"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Check Again
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div
            ref={cardRef}
            className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 hover:shadow-lg transition-shadow duration-300"
        >
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-chart-2/5 pointer-events-none rounded-3xl" />

            <div className="relative z-10">
                {/* Header */}
                <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center space-x-3">
                        <Calendar className="w-6 h-6 text-primary" />
                        <h3 className="text-2xl font-bold text-foreground">5-Day Forecast</h3>
                    </div>
                    <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                        <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                        <span>Updated</span>
                    </div>
                </div>

                {/* Forecast Grid */}
                <div className="grid grid-cols-5 gap-4">
                    {forecast.map((day, index) => (
                        <div
                            key={index}
                            className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105"
                        >
                            {/* Hover gradient background */}
                            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

                            <div className="relative z-10 p-5 text-center">
                                {/* Day */}
                                <div className="text-sm font-medium text-muted-foreground mb-4 tracking-wide">
                                    {day.day}
                                </div>

                                {/* Weather Icon */}
                                <div className={`w-16 h-16 rounded-2xl bg-gradient-to-br ${day.color} flex items-center justify-center mx-auto mb-5 ${day.shadow} group-hover:shadow-lg transition-all duration-300`}>
                                    <day.icon className="w-8 h-8 text-white drop-shadow-sm" />
                                </div>

                                {/* Temperature with feels like */}
                                <div className="mb-4">
                                    <div className="text-lg font-bold text-foreground mb-1">
                                        {day.high}° {day.low}°
                                    </div>
                                    <div className="text-xs text-muted-foreground">
                                        feels {day.feelsLike}°
                                    </div>
                                </div>

                                {/* Condition */}
                                <div className="text-xs text-muted-foreground mb-3 capitalize leading-tight">
                                    {day.condition}
                                </div>

                                {/* Basic Wind Info (Always Visible) */}
                                <div className="mb-3">
                                    <div className="flex items-center justify-center space-x-1 text-xs text-muted-foreground">
                                        <Wind className="w-3 h-3" />
                                        <span>{day.wind.speed} {day.wind.direction}</span>
                                    </div>
                                </div>

                                {/* Detailed Weather Info (Hover Only) - Uses max-height for smooth expansion */}
                                <div className="max-h-0 group-hover:max-h-32 overflow-hidden transition-all duration-300 ease-in-out">
                                    <div className="border-t border-border/30 pt-3 space-y-2 text-xs opacity-0 group-hover:opacity-100 transition-opacity duration-300 delay-100">
                                        {/* Humidity with trend */}
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center space-x-1">
                                                <Droplets className="w-3 h-3 text-blue-500" />
                                                <span>{day.humidity.value}%</span>
                                                <span className="text-muted-foreground text-xs">
                                                    {day.humidity.trend}
                                                </span>
                                            </div>
                                            <day.humidity.icon className={`w-3 h-3 ${day.humidity.color}`} />
                                        </div>

                                        {/* Pressure with barometer icon and trend */}
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center space-x-1">
                                                <Gauge className="w-3 h-3 text-gray-500" />
                                                <span>{day.pressure.value} hPa</span>
                                                <span className="text-muted-foreground text-xs">
                                                    {day.pressure.trend}
                                                </span>
                                            </div>
                                            <day.pressure.icon className={`w-3 h-3 ${day.pressure.color}`} />
                                        </div>

                                        {/* Cloud cover with weather main */}
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center space-x-1">
                                                <Cloud className="w-3 h-3" />
                                                <span>{day.cloudCover.value}%</span>
                                                <span className="text-muted-foreground text-xs">
                                                    {day.cloudCover.weather.toLowerCase()}
                                                </span>
                                            </div>
                                            <day.cloudCover.icon className={`w-3 h-3 ${day.cloudCover.color}`} />
                                        </div>

                                        {/* Precipitation probability with trend */}
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center space-x-1">
                                                <Umbrella className="w-3 h-3 text-blue-500" />
                                                <span>{day.precipitationProbability.value}%</span>
                                                <span className="text-muted-foreground text-xs">
                                                    {day.precipitationProbability.trend}
                                                </span>
                                            </div>
                                            <day.precipitationProbability.icon className={`w-3 h-3 ${day.precipitationProbability.color}`} />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Footer Info */}
                <div className="mt-6 text-center">
                    <div className="text-xs text-muted-foreground opacity-60">
                        Powered by Open-Meteo API • Hover for detailed forecast
                    </div>
                </div>
            </div>
        </div>
    )
}