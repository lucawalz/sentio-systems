import { Thermometer, Droplets, Gauge, CloudDrizzle, Umbrella, Wind } from "lucide-react"
import { useEffect, useRef, useState } from "react"
import { gsap } from "gsap"
import { forecastService, type WeatherForecast } from "../../services/forecastService"

// Function to find the current weather entry closest to current date and hour
function findCurrentWeatherEntry(forecasts: WeatherForecast[]): WeatherForecast | null {
    if (!forecasts || forecasts.length === 0) return null

    const now = new Date()
    const currentDate = now.toISOString().split('T')[0] // Get current date in YYYY-MM-DD format
    const currentHour = now.getHours()

    // Filter forecasts for today
    const todaysForecasts = forecasts.filter(forecast =>
        forecast.forecastDate === currentDate
    )

    if (todaysForecasts.length === 0) {
        // If no forecasts for today, return the closest future forecast
        const futureForecasts = forecasts.filter(forecast =>
            new Date(forecast.forecastDateTime) > now
        )
        return futureForecasts.length > 0 ? futureForecasts[0] : forecasts[0]
    }

    // Find the forecast closest to current hour
    let closest = todaysForecasts[0]
    let smallestHourDiff = Math.abs(currentHour - new Date(closest.forecastDateTime).getHours())

    for (const forecast of todaysForecasts) {
        const forecastHour = new Date(forecast.forecastDateTime).getHours()
        const hourDiff = Math.abs(currentHour - forecastHour)
        if (hourDiff < smallestHourDiff) {
            smallestHourDiff = hourDiff
            closest = forecast
        }
    }

    return closest
}

// Function to calculate trends by comparing current with next few hours
function calculateTrend(currentValue: number | null, nextValues: (number | null)[]): 'rising' | 'falling' | 'stable' {
    if (!currentValue || nextValues.length === 0) return 'stable'

    // Filter out null values and take next 3-6 hours
    const validValues = nextValues.slice(0, 6).filter(val => val !== null) as number[]
    if (validValues.length === 0) return 'stable'

    // Calculate average of next few hours
    const avgNext = validValues.reduce((sum, val) => sum + val, 0) / validValues.length
    const diff = avgNext - currentValue

    // Use threshold based on value type
    const threshold = Math.abs(currentValue) * 0.03 // 3% threshold

    if (Math.abs(diff) < Math.max(threshold, 0.1)) return 'stable'
    return diff > 0 ? 'rising' : 'falling'
}

// Function to convert wind degree to compass direction
function getWindDirection(degree: number | null): string {
    if (!degree) return "N/A"
    const directions = ["N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"]
    const index = Math.round(degree / 22.5) % 16
    return directions[index]
}

// Function to predict wind direction shift
function predictWindShift(currentDir: number | null, nextDirections: (number | null)[]): string {
    if (!currentDir || nextDirections.length === 0) return "No data"

    const validDirs = nextDirections.slice(0, 6).filter(dir => dir !== null) as number[]
    if (validDirs.length === 0) return "Steady direction"

    // Calculate average of next directions (handling circular nature of degrees)
    const avgNextDir = validDirs.reduce((sum, dir) => sum + dir, 0) / validDirs.length
    const diff = avgNextDir - currentDir

    // Handle circular nature of wind direction (0° = 360°)
    let normalizedDiff = diff
    if (Math.abs(diff) > 180) {
        normalizedDiff = diff > 0 ? diff - 360 : diff + 360
    }

    if (Math.abs(normalizedDiff) < 15) return "Steady direction"

    const nextDirName = getWindDirection(avgNextDir)
    return normalizedDiff > 0 ? `shifting to ${nextDirName}` : `backing to ${nextDirName}`
}

// Helper functions for classifications
function getHumidityLevel(humidity: number | null): string {
    if (!humidity) return "No data"
    if (humidity >= 70) return "High humidity"
    if (humidity >= 30) return "Normal humidity"
    return "Low humidity"
}

function getPressureLevel(pressure: number | null): string {
    if (!pressure) return "No data"
    if (pressure >= 1020) return "High pressure"
    if (pressure >= 1000) return "Normal pressure"
    return "Low pressure"
}

function getPrecipitationLevel(precipProb: number | null): string {
    if (!precipProb) return "No rain expected"
    if (precipProb >= 70) return "High chance"
    if (precipProb >= 30) return "Moderate chance"
    return "Low chance"
}

// Function to get trend icon and color
function getTrendIcon(trend: 'rising' | 'falling' | 'stable'): { symbol: string, color: string } {
    switch (trend) {
        case 'rising':
            return { symbol: "↗", color: "text-green-500" }
        case 'falling':
            return { symbol: "↘", color: "text-red-500" }
        case 'stable':
        default:
            return { symbol: "→", color: "text-gray-500" }
    }
}

export function MicroCards() {
    const cardRef = useRef<HTMLDivElement>(null)
    const [forecasts, setForecasts] = useState<WeatherForecast[]>([])
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        if (cardRef.current) {
            gsap.fromTo(
                cardRef.current,
                { y: 30, opacity: 0, scale: 0.95 },
                { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out", delay: 0.1 }
            )
        }
    }, [])

    useEffect(() => {
        const fetchForecasts = async () => {
            try {
                setLoading(true)
                const data = await forecastService.getUpcomingForecasts()
                setForecasts(data)
            } catch (error) {
                console.error('Failed to fetch weather forecasts:', error)
            } finally {
                setLoading(false)
            }
        }

        fetchForecasts()
        // Refresh every 5 minutes
        const interval = setInterval(fetchForecasts, 5 * 60 * 1000)
        return () => clearInterval(interval)
    }, [])

    // Get the current weather entry for current date and hour
    const currentWeather = findCurrentWeatherEntry(forecasts)

    // Get next few hours for trend analysis
    const nextHours = currentWeather ? forecasts.filter(forecast =>
        new Date(forecast.forecastDateTime) > new Date(currentWeather.forecastDateTime)
    ).slice(0, 8) : [] // Take next 8 hours for trend analysis

    // Create micro data based on your requirements
    const microData = currentWeather ? [
        {
            icon: Thermometer,
            label: "Feels Like",
            value: currentWeather.apparentTemperature ? `${Math.round(currentWeather.apparentTemperature)}°C` : "N/A",
            color: "from-orange-400 to-orange-600",
            shadow: "shadow-orange-500/30",
            description: "Apparent temperature",
            trend: calculateTrend(currentWeather.apparentTemperature, nextHours.map(f => f.apparentTemperature))
        },
        {
            icon: Droplets,
            label: "Humidity",
            value: currentWeather.humidity ? `${currentWeather.humidity}%` : "N/A",
            color: "from-blue-400 to-blue-600",
            shadow: "shadow-blue-500/30",
            description: getHumidityLevel(currentWeather.humidity),
            trend: calculateTrend(currentWeather.humidity, nextHours.map(f => f.humidity))
        },
        {
            icon: Gauge,
            label: "Pressure",
            value: currentWeather.pressure ? `${Math.round(currentWeather.pressure)} hPa` : "N/A",
            color: "from-purple-400 to-purple-600",
            shadow: "shadow-purple-500/30",
            description: getPressureLevel(currentWeather.pressure),
            trend: calculateTrend(currentWeather.pressure, nextHours.map(f => f.pressure))
        },
        {
            icon: CloudDrizzle,
            label: "Dew Point",
            value: currentWeather.dewPoint ? `${Math.round(currentWeather.dewPoint)}°C` : "N/A",
            color: "from-teal-400 to-teal-600",
            shadow: "shadow-teal-500/30",
            description: "Dew point temperature",
            trend: calculateTrend(currentWeather.dewPoint, nextHours.map(f => f.dewPoint))
        },
        {
            icon: Umbrella,
            label: "Rain Chance",
            value: currentWeather.precipitationProbability ? `${currentWeather.precipitationProbability}%` : "0%",
            color: "from-indigo-400 to-indigo-600",
            shadow: "shadow-indigo-500/30",
            description: getPrecipitationLevel(currentWeather.precipitationProbability),
            trend: calculateTrend(currentWeather.precipitationProbability, nextHours.map(f => f.precipitationProbability))
        },
        {
            icon: Wind,
            label: "Wind",
            value: currentWeather.windSpeed ? `${Math.round(currentWeather.windSpeed * 3.6)} km/h` : "N/A", // Convert m/s to km/h
            color: "from-green-400 to-green-600",
            shadow: "shadow-green-500/30",
            description: currentWeather.windDirection ?
                `${getWindDirection(currentWeather.windDirection)} ${predictWindShift(currentWeather.windDirection, nextHours.map(f => f.windDirection))}` :
                "No wind data",
            trend: calculateTrend(currentWeather.windSpeed, nextHours.map(f => f.windSpeed)),
            isWind: true,
            windDetails: {
                direction: currentWeather.windDirection ? `${getWindDirection(currentWeather.windDirection)} (${Math.round(currentWeather.windDirection)}°)` : "",
                gusts: currentWeather.windGusts ? `Gusts: ${Math.round(currentWeather.windGusts * 3.6)} km/h` : "",
                shift: predictWindShift(currentWeather.windDirection, nextHours.map(f => f.windDirection))
            }
        },
    ] : Array(6).fill(null).map((_, index) => {
        const labels = ["Feels Like", "Humidity", "Pressure", "Dew Point", "Rain Chance", "Wind"]
        const colors = [
            "from-orange-400 to-orange-600",
            "from-blue-400 to-blue-600",
            "from-purple-400 to-purple-600",
            "from-teal-400 to-teal-600",
            "from-indigo-400 to-indigo-600",
            "from-green-400 to-green-600"
        ]
        const shadows = [
            "shadow-orange-500/30",
            "shadow-blue-500/30",
            "shadow-purple-500/30",
            "shadow-teal-500/30",
            "shadow-indigo-500/30",
            "shadow-green-500/30"
        ]
        const icons = [Thermometer, Droplets, Gauge, CloudDrizzle, Umbrella, Wind]

        return {
            icon: icons[index],
            label: labels[index],
            value: loading ? "Loading..." : "No Data",
            color: colors[index],
            shadow: shadows[index],
            description: loading ? "Fetching data..." : "Data unavailable",
            trend: 'stable' as const
        }
    })

    return (
        <div ref={cardRef} className="grid grid-cols-3 grid-rows-2 gap-4 h-full">
            {microData.map((item, index) => {
                const trendDisplay = getTrendIcon(item.trend)

                return (
                    <div
                        key={index}
                        className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105"
                    >
                        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

                        <div className="relative z-10 p-4 text-center h-full flex flex-col justify-center">
                            {/* Icon - Fixed position */}
                            <div className={`w-10 h-10 rounded-2xl bg-gradient-to-br ${item.color} flex items-center justify-center mx-auto mb-3 ${item.shadow} group-hover:shadow-lg transition-all duration-300`}>
                                <item.icon className="w-5 h-5 text-white drop-shadow-sm" />
                            </div>

                            {/* Label - Fixed position */}
                            <div className="text-xs font-medium text-muted-foreground mb-1 tracking-wide">
                                {item.label}
                            </div>

                            {/* Value - Fixed position */}
                            <div className="text-sm font-bold text-foreground mb-2 leading-tight">
                                {item.value}
                            </div>

                            {/* Content that slides up on hover for wind card */}
                            {item.isWind ? (
                                <div className="relative overflow-hidden">
                                    {/* Content container that slides up */}
                                    <div className="transform transition-transform duration-300 ease-out group-hover:-translate-y-6">
                                        {/* Normal description - visible by default */}
                                        <div className="text-xs text-muted-foreground/80 mb-1 leading-relaxed">
                                            {item.description}
                                        </div>

                                        {/* Trend - positioned where it normally would be */}
                                        <div className={`text-xs font-medium ${trendDisplay.color} flex items-center justify-center gap-1 mt-1`}>
                                            <span>{trendDisplay.symbol}</span>
                                            <span className="capitalize">{item.trend}</span>
                                        </div>
                                    </div>

                                    {/* Additional details that slide up from below */}
                                    <div className="absolute top-full left-0 right-0 transform transition-transform duration-300 ease-out group-hover:-translate-y-6 space-y-1 pt-2">
                                        {item.windDetails?.direction && (
                                            <div className="text-xs text-muted-foreground/60">
                                                {item.windDetails.direction}
                                            </div>
                                        )}
                                        {item.windDetails?.gusts && (
                                            <div className="text-xs text-muted-foreground/60">
                                                {item.windDetails.gusts}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ) : (
                                <>
                                    {/* Non-wind cards - normal layout */}
                                    <div className="text-xs text-muted-foreground/80 mb-1 leading-relaxed">
                                        {item.description}
                                    </div>

                                    {/* Trend */}
                                    <div className={`text-xs font-medium ${trendDisplay.color} flex items-center justify-center gap-1`}>
                                        <span>{trendDisplay.symbol}</span>
                                        <span className="capitalize">{item.trend}</span>
                                    </div>
                                </>
                            )}
                        </div>
                    </div>
                )
            })}
        </div>
    )
}