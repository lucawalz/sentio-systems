import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import { Cloud, Droplets, Thermometer, Gauge, Sun, Zap, Database, WifiOff, RefreshCw, Activity } from "lucide-react"
import { useWeatherData } from "../../hooks/useWeatherData"

export function EnhancedWeatherStation() {
    const cardRef = useRef<HTMLDivElement>(null)
    const iconRef = useRef<HTMLDivElement>(null)
    const aiIconRef = useRef<HTMLDivElement>(null)
    const { latestWeather, loading, error, isEmpty, refetch } = useWeatherData(30000)

    useEffect(() => {
        if (cardRef.current) {
            gsap.fromTo(
                cardRef.current,
                { y: 30, opacity: 0, scale: 0.95 },
                { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out" }
            )
        }

        if (iconRef.current) {
            gsap.to(iconRef.current, {
                y: -10,
                duration: 4,
                ease: "power2.inOut",
                yoyo: true,
                repeat: -1,
            })
        }

        if (aiIconRef.current) {
            gsap.to(aiIconRef.current, {
                scale: 1.1,
                duration: 2,
                ease: "power2.inOut",
                yoyo: true,
                repeat: -1,
            })
        }
    }, [])

    const formatTemperature = (temp: number | null) => {
        if (temp === null) return '--';
        return temp.toFixed(2);
    }

    const formatValue = (value: number | null, decimals: number = 2) => {
        if (value === null) return '--';
        return value.toFixed(decimals);
    }

    const formatLastUpdated = (timestamp: string) => {
        if (!timestamp) return 'Never'
        const date = new Date(timestamp)
        const now = new Date()
        const diffMs = now.getTime() - date.getTime()
        const diffMins = Math.floor(diffMs / 60000)

        if (diffMins < 1) return 'Just now'
        if (diffMins < 60) return `${diffMins}m ago`
        const diffHours = Math.floor(diffMins / 60)
        if (diffHours < 24) return `${diffHours}h ago`
        return date.toLocaleDateString()
    }

    const getWeatherCondition = (temperature: number, humidity: number) => {
        if (humidity > 80) return "Humid"
        if (temperature > 25) return "Sunny"
        if (humidity > 60) return "Partly Cloudy"
        return "Clear"
    }

    if (loading) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[480px] relative overflow-hidden shadow-xl">
                <div className="flex items-center justify-center h-full flex-col space-y-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <div className="text-sm text-muted-foreground">Loading weather data...</div>
                </div>
            </div>
        )
    }

    if (isEmpty) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[480px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <Database className="w-16 h-16 text-muted-foreground mb-4" />
                    <div className="text-xl font-semibold mb-2 text-foreground">No Weather Data Available</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">
                        The weather station hasn't collected any data yet. Please wait for the first MQTT data to arrive, or check if your weather sensors are connected and sending data.
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

    if (error && !isEmpty) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[480px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center min-h-[400px]">
                    <WifiOff className="w-16 h-16 text-destructive mb-4" />
                    <div className="text-xl font-semibold mb-2 text-destructive">Connection Error</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">{error}</div>
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

    return (
        <div
            ref={cardRef}
            className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[480px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300"
        >
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-chart-2/5 pointer-events-none rounded-3xl" />

            <div className="relative z-10 h-full flex gap-8">
                {/* Left Side - Weather Data */}
                <div className="flex-1 flex flex-col">
                    {/* Header */}
                    <div className="flex items-start justify-between mb-8">
                        <div className="flex items-center space-x-3">
                            <Activity className="w-6 h-6 text-primary" />
                            <h2 className="text-2xl md:text-3xl font-bold text-foreground">
                                Weather Station
                            </h2>
                        </div>
                        <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                            <span>Last updated {formatLastUpdated(latestWeather?.timestamp || '')}</span>
                        </div>
                    </div>

                    {/* Main Content - Temperature and Weather Icon */}
                    <div className="flex items-center gap-16 mb-8">
                        {/* Temperature Display */}
                        <div>
                            <div className="flex items-baseline space-x-2 mb-2">
                                <div className="text-6xl md:text-7xl font-light text-foreground tracking-tight">
                                    {latestWeather ? formatTemperature(latestWeather.temperature) : '--'}°C
                                </div>
                                <Thermometer className="w-8 h-8 text-muted-foreground" />
                            </div>
                            <div className="text-xl text-muted-foreground font-medium mb-1">
                                {latestWeather ? getWeatherCondition(latestWeather.temperature, latestWeather.humidity) : 'Unknown'}
                            </div>
                            <div className="text-sm text-muted-foreground">
                                Feels like {latestWeather ? formatTemperature(latestWeather.temperature + 2) : '--'}°C
                            </div>
                        </div>

                        {/* Weather Icon */}
                        <div className="flex items-center">
                            <div
                                ref={iconRef}
                                className="relative w-24 h-24 md:w-28 md:h-28 rounded-2xl bg-gradient-to-br from-primary/20 to-primary/40 backdrop-blur-sm border border-border/50 flex items-center justify-center shadow-lg shadow-primary/30 group-hover:shadow-xl transition-all duration-300"
                            >
                                <Cloud className="w-12 h-12 md:w-14 md:h-14 text-white drop-shadow-sm" />
                            </div>
                        </div>
                    </div>

                    {/* Weather Metrics Grid - 2x2 Layout */}
                    <div className="grid grid-cols-2 gap-4 max-w-md">
                        <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                            <div className="relative z-10 flex items-center space-x-3">
                                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-400 to-blue-600 flex items-center justify-center shadow-blue-500/30 shadow-lg">
                                    <Droplets className="w-5 h-5 text-white" />
                                </div>
                                <div>
                                    <div className="text-xs text-muted-foreground uppercase tracking-wide font-medium">
                                        Humidity
                                    </div>
                                    <div className="font-bold text-foreground text-lg">
                                        {latestWeather ? formatValue(latestWeather.humidity) : '--'}%
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                            <div className="relative z-10 flex items-center space-x-3">
                                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-green-400 to-green-600 flex items-center justify-center shadow-green-500/30 shadow-lg">
                                    <Gauge className="w-5 h-5 text-white" />
                                </div>
                                <div>
                                    <div className="text-xs text-muted-foreground uppercase tracking-wide font-medium">
                                        Pressure
                                    </div>
                                    <div className="font-bold text-foreground text-lg">
                                        {latestWeather ? formatValue(latestWeather.pressure) : '--'} hPa
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                            <div className="relative z-10 flex items-center space-x-3">
                                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center shadow-amber-500/30 shadow-lg">
                                    <Sun className="w-5 h-5 text-white" />
                                </div>
                                <div>
                                    <div className="text-xs text-muted-foreground uppercase tracking-wide font-medium">
                                        Light
                                    </div>
                                    <div className="font-bold text-foreground text-lg">
                                        {latestWeather ? formatValue(latestWeather.lux) : '--'} lux
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 p-4">
                            <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                            <div className="relative z-10 flex items-center space-x-3">
                                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-purple-400 to-purple-600 flex items-center justify-center shadow-purple-500/30 shadow-lg">
                                    <Zap className="w-5 h-5 text-white" />
                                </div>
                                <div>
                                    <div className="text-xs text-muted-foreground uppercase tracking-wide font-medium">
                                        UV Index
                                    </div>
                                    <div className="font-bold text-foreground text-lg">
                                        {latestWeather ? (latestWeather.uvi?.toFixed(2) || '--') : '--'}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Footer Info */}
            <div className="absolute bottom-6 left-6 right-6">
                <div className="text-xs text-muted-foreground opacity-60 text-center">
                    Real-time sensor data • MQTT updates every 5 minutes
                </div>
            </div>
        </div>
    )
}