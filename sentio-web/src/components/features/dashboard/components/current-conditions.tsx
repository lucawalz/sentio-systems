import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import {
    Droplets,
    Wind,
    Gauge,
    Cloud,
    CloudRain,
    Cpu,
    RefreshCw,
} from 'lucide-react'
import type { RaspiWeather, WeatherForecast } from '@/types/api'

interface CurrentConditionsProps {
    sensorData: RaspiWeather | null
    forecastData: WeatherForecast | null
    loading: boolean
    lastUpdated?: string
}

// Helper to get weather icon based on weather code
function getWeatherIcon(code: number | null | undefined) {
    if (!code) return <Cloud className="h-12 w-12 text-muted-foreground" />
    // Simplified weather code mapping (WMO codes)
    if (code <= 3) return <Cloud className="h-12 w-12 text-yellow-500" /> // Clear/Partly cloudy
    if (code <= 49) return <Cloud className="h-12 w-12 text-gray-400" /> // Fog/Cloudy
    if (code <= 69) return <CloudRain className="h-12 w-12 text-blue-500" /> // Rain
    if (code <= 79) return <CloudRain className="h-12 w-12 text-blue-300" /> // Snow
    return <CloudRain className="h-12 w-12 text-purple-500" /> // Thunderstorm
}

// Helper for weather description
function getWeatherDescription(code: number | null | undefined, description: string | null | undefined): string {
    if (description) return description
    if (!code) return 'No data'
    if (code === 0) return 'Clear sky'
    if (code <= 3) return 'Partly cloudy'
    if (code <= 49) return 'Foggy conditions'
    if (code <= 55) return 'Drizzle'
    if (code <= 65) return 'Rain'
    if (code <= 75) return 'Snow'
    if (code <= 82) return 'Rain showers'
    return 'Thunderstorm'
}

export function CurrentConditions({ sensorData, forecastData, loading, lastUpdated }: CurrentConditionsProps) {
    // Use sensor data for temp/humidity/pressure when available, fallback to forecast
    const temperature = sensorData?.temperature ?? forecastData?.temperature
    const humidity = sensorData?.humidity ?? forecastData?.humidity
    const pressure = sensorData?.pressure ?? forecastData?.pressure

    const feelsLike = forecastData?.apparentTemperature
    const windSpeed = forecastData?.windSpeed
    const windGusts = forecastData?.windGusts
    const precipitation = forecastData?.precipitationProbability
    const weatherCode = forecastData?.weatherCode
    const description = forecastData?.description

    // Determine data sources
    const hasSensorData = sensorData !== null
    const hasApiData = forecastData !== null

    return (
        <Card className="h-full">
            <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                    <CardTitle className="text-lg font-semibold">Right Now</CardTitle>
                    {lastUpdated && (
                        <div className="flex items-center gap-1 text-xs text-muted-foreground">
                            <RefreshCw className="h-3 w-3" />
                            Updated {new Date(lastUpdated).toLocaleTimeString()}
                        </div>
                    )}
                </div>
                {/* Data source badges */}
                <div className="flex gap-2 mt-1">
                    {hasSensorData && (
                        <Badge variant="outline" className="text-xs gap-1 text-emerald-600 border-emerald-300">
                            <Cpu className="h-3 w-3" /> Sensor
                        </Badge>
                    )}
                    {hasApiData && (
                        <Badge variant="outline" className="text-xs gap-1 text-blue-600 border-blue-300">
                            <Cloud className="h-3 w-3" /> OpenMeteo
                        </Badge>
                    )}
                </div>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="space-y-4">
                        <Skeleton className="h-20 w-32" />
                        <div className="grid grid-cols-2 gap-4">
                            {[...Array(4)].map((_, i) => (
                                <Skeleton key={i} className="h-20" />
                            ))}
                        </div>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {/* Main temperature display */}
                        <div className="flex items-start gap-4">
                            {getWeatherIcon(weatherCode)}
                            <div>
                                <div className="text-5xl font-bold tracking-tighter">
                                    {temperature != null ? `${temperature.toFixed(0)}°` : '--°'}
                                </div>
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    {feelsLike != null && (
                                        <span>Feels like {feelsLike.toFixed(0)}°</span>
                                    )}
                                    {hasSensorData && (
                                        <Badge variant="secondary" className="text-xs">
                                            <Cpu className="h-2.5 w-2.5 mr-1" />
                                            Sensor
                                        </Badge>
                                    )}
                                </div>
                                <p className="text-sm mt-1">{getWeatherDescription(weatherCode, description)}</p>
                            </div>
                        </div>

                        {/* 2x2 Grid of metrics */}
                        <div className="grid grid-cols-2 gap-3">
                            {/* Humidity - from sensor or API */}
                            <div className="rounded-lg border p-3">
                                <div className="flex items-center gap-2 text-xs text-muted-foreground uppercase tracking-wide">
                                    <Droplets className="h-4 w-4" />
                                    Humidity
                                    {hasSensorData && sensorData?.humidity != null && (
                                        <Cpu className="h-2.5 w-2.5 text-emerald-500" />
                                    )}
                                </div>
                                <div className="text-2xl font-bold mt-1">
                                    {humidity != null ? `${humidity.toFixed(0)}%` : '--'}
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    {humidity != null && humidity > 70 ? 'High humidity' :
                                        humidity != null && humidity < 30 ? 'Low humidity' : 'Comfortable'}
                                </p>
                            </div>

                            {/* Wind - API only */}
                            <div className="rounded-lg border p-3">
                                <div className="flex items-center gap-2 text-xs text-muted-foreground uppercase tracking-wide">
                                    <Wind className="h-4 w-4" />
                                    Wind
                                    <Cloud className="h-2.5 w-2.5 text-blue-500" />
                                </div>
                                <div className="text-2xl font-bold mt-1">
                                    {windSpeed != null ? `${(windSpeed * 3.6).toFixed(0)} km/h` : '--'}
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    {windGusts != null ? `Gusts ${(windGusts * 3.6).toFixed(0)} km/h` : 'Calm conditions'}
                                </p>
                            </div>

                            {/* Pressure - from sensor or API */}
                            <div className="rounded-lg border p-3">
                                <div className="flex items-center gap-2 text-xs text-muted-foreground uppercase tracking-wide">
                                    <Gauge className="h-4 w-4" />
                                    Pressure
                                    {hasSensorData && sensorData?.pressure != null && (
                                        <Cpu className="h-2.5 w-2.5 text-emerald-500" />
                                    )}
                                </div>
                                <div className="text-2xl font-bold mt-1">
                                    {pressure != null ? `${pressure.toFixed(0)} hPa` : '--'}
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    {pressure != null && pressure > 1020 ? 'High pressure' :
                                        pressure != null && pressure < 1000 ? 'Low pressure' : 'Stable'}
                                </p>
                            </div>

                            {/* Precipitation - API only */}
                            <div className="rounded-lg border p-3">
                                <div className="flex items-center gap-2 text-xs text-muted-foreground uppercase tracking-wide">
                                    <CloudRain className="h-4 w-4" />
                                    Precipitation
                                    <Cloud className="h-2.5 w-2.5 text-blue-500" />
                                </div>
                                <div className="text-2xl font-bold mt-1">
                                    {precipitation != null ? `${precipitation.toFixed(0)}%` : '--'}
                                </div>
                                <p className="text-xs text-muted-foreground">
                                    {precipitation != null && precipitation > 50 ? 'Likely rain' :
                                        precipitation != null && precipitation > 20 ? 'Possible rain' : 'Dry conditions'}
                                </p>
                            </div>
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    )
}
