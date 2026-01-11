import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { TrendingUp, TrendingDown, Minus, Calendar, Cloud, CloudRain, Sun, Snowflake, CloudFog, Droplets, Wind, Thermometer, Clock, Sunrise, Sunset } from 'lucide-react'
import type { HistoricalComparison as HistoricalComparisonType } from '@/types/api'

interface HistoricalComparisonProps {
    comparison: HistoricalComparisonType | null
    currentTemp: number | null
    loading: boolean
}

interface ComparisonPeriod {
    label: string
    shortLabel: string
    key: keyof HistoricalComparisonType
}

const periods: ComparisonPeriod[] = [
    { label: '3 Days Ago', shortLabel: '3d', key: 'threeDaysAgo' },
    { label: '2 Weeks Ago', shortLabel: '2w', key: 'twoWeeksAgo' },
    { label: '1 Month Ago', shortLabel: '1mo', key: 'oneMonthAgo' },
    { label: '3 Months Ago', shortLabel: '3mo', key: 'threeMonthsAgo' },
]

function getTrendIcon(diff: number) {
    const className = "h-3 w-3"
    if (diff > 1) return <TrendingUp className={`${className} text-red-500`} />
    if (diff < -1) return <TrendingDown className={`${className} text-blue-500`} />
    return <Minus className={`${className} text-muted-foreground`} />
}

function getTrendText(diff: number): string {
    if (Math.abs(diff) < 0.5) return 'Similar'
    const direction = diff > 0 ? 'warmer' : 'cooler'
    return `${Math.abs(diff).toFixed(1)}° ${direction}`
}

function getWeatherIcon(weatherMain?: string, weatherCode?: number) {
    const iconClass = "h-5 w-5"
    const main = weatherMain?.toLowerCase() || ''

    if (weatherCode) {
        if (weatherCode === 0 || weatherCode === 1) return <Sun className={`${iconClass} text-yellow-500`} />
        if (weatherCode >= 2 && weatherCode <= 3) return <Cloud className={iconClass} />
        if (weatherCode >= 45 && weatherCode <= 48) return <CloudFog className={iconClass} />
        if (weatherCode >= 51 && weatherCode <= 67) return <CloudRain className={`${iconClass} text-blue-400`} />
        if (weatherCode >= 71 && weatherCode <= 86) return <Snowflake className={`${iconClass} text-blue-200`} />
        if (weatherCode >= 80 && weatherCode <= 82) return <CloudRain className={`${iconClass} text-blue-500`} />
        if (weatherCode >= 95) return <Cloud className={`${iconClass} text-yellow-600`} />
    }

    if (main.includes('clear') || main.includes('sun')) return <Sun className={`${iconClass} text-yellow-500`} />
    if (main.includes('rain') || main.includes('drizzle')) return <CloudRain className={`${iconClass} text-blue-400`} />
    if (main.includes('snow')) return <Snowflake className={`${iconClass} text-blue-200`} />
    if (main.includes('fog') || main.includes('mist')) return <CloudFog className={iconClass} />
    return <Cloud className={iconClass} />
}

function formatDuration(seconds: number | null): string {
    if (seconds == null) return '--'
    const hours = Math.floor(seconds / 3600)
    const mins = Math.floor((seconds % 3600) / 60)
    return `${hours}h ${mins}m`
}

function formatTime(isoString: string | null): string {
    if (!isoString) return '--'
    try {
        const date = new Date(isoString)
        return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
    } catch {
        return '--'
    }
}

export function HistoricalComparison({ comparison, currentTemp, loading }: HistoricalComparisonProps) {
    if (loading) {
        return (
            <Card className="h-full">
                <CardHeader className="pb-2">
                    <CardTitle className="text-lg font-semibold flex items-center gap-2">
                        <Calendar className="h-5 w-5" /> Historical Comparison
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-4 gap-3">
                        {[...Array(4)].map((_, i) => <Skeleton key={i} className="h-52" />)}
                    </div>
                </CardContent>
            </Card>
        )
    }

    if (!comparison || currentTemp === null) {
        return (
            <Card className="h-full">
                <CardHeader className="pb-2">
                    <CardTitle className="text-lg font-semibold flex items-center gap-2">
                        <Calendar className="h-5 w-5" /> Historical Comparison
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex items-center justify-center h-52 text-muted-foreground text-sm">No historical data available</div>
                </CardContent>
            </Card>
        )
    }

    return (
        <Card className="h-full">
            <CardHeader className="pb-2">
                <CardTitle className="text-lg font-semibold flex items-center gap-2">
                    <Calendar className="h-5 w-5" /> Compared to the Past
                </CardTitle>
                <CardDescription>Current: {currentTemp.toFixed(1)}°C</CardDescription>
            </CardHeader>
            <CardContent>
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                    {periods.map((period) => {
                        const data = comparison[period.key]

                        if (!data || data.temperatureMean == null) {
                            return (
                                <div key={period.key} className="rounded-lg border p-3 opacity-50 flex flex-col min-h-[220px]">
                                    <Badge variant="outline" className="w-fit text-xs">{period.shortLabel}</Badge>
                                    <div className="flex-1 flex items-center justify-center">
                                        <p className="text-sm text-muted-foreground">No data</p>
                                    </div>
                                </div>
                            )
                        }

                        const pastTemp = data.temperatureMean
                        const diff = currentTemp - pastTemp

                        return (
                            <div key={period.key} className="rounded-lg border p-3 bg-card flex flex-col min-h-[220px]">
                                {/* Header */}
                                <div className="flex items-center justify-between mb-1">
                                    <Badge variant="outline" className="text-xs">{period.shortLabel}</Badge>
                                    {getWeatherIcon(data.weatherMain ?? undefined, data.weatherCode ?? undefined)}
                                </div>

                                {/* Temperature */}
                                <p className="text-2xl font-bold">{pastTemp.toFixed(1)}°</p>

                                {/* Temperature range */}
                                <p className="text-xs text-muted-foreground flex items-center gap-1">
                                    <Thermometer className="h-3 w-3" />
                                    {data.maxTemperature != null && data.minTemperature != null
                                        ? `H: ${data.maxTemperature.toFixed(0)}° / L: ${data.minTemperature.toFixed(0)}°`
                                        : '--'}
                                </p>

                                {/* Trend indicator */}
                                <div className="flex items-center gap-1 mt-1 mb-2">
                                    {getTrendIcon(diff)}
                                    <span className={`text-xs ${diff > 0 ? 'text-red-500' : diff < 0 ? 'text-blue-500' : 'text-muted-foreground'}`}>
                                        {getTrendText(diff)}
                                    </span>
                                </div>

                                {/* All details - always show */}
                                <div className="mt-auto pt-2 border-t border-border/50 space-y-1.5 text-xs text-muted-foreground">
                                    {/* Precipitation */}
                                    <div className="flex items-center gap-1">
                                        <Droplets className="h-3 w-3 flex-shrink-0" />
                                        <span>
                                            {data.precipitationSum != null && data.precipitationSum > 0
                                                ? `${data.precipitationSum.toFixed(1)}mm (${data.precipitationHours?.toFixed(0) ?? 0}h)`
                                                : 'No rain'}
                                        </span>
                                    </div>

                                    {/* Wind */}
                                    <div className="flex items-center gap-1">
                                        <Wind className="h-3 w-3 flex-shrink-0" />
                                        <span>
                                            {data.windSpeedMax != null
                                                ? `${(data.windSpeedMax * 3.6).toFixed(0)} km/h`
                                                : '--'}
                                        </span>
                                    </div>

                                    {/* UV Index */}
                                    <div className="flex items-center gap-1">
                                        <Sun className="h-3 w-3 flex-shrink-0" />
                                        <span>UV: {data.uvIndexMax != null ? data.uvIndexMax.toFixed(1) : '--'}</span>
                                    </div>

                                    {/* Sunshine duration */}
                                    <div className="flex items-center gap-1">
                                        <Clock className="h-3 w-3 flex-shrink-0" />
                                        <span>Sun: {formatDuration(data.sunshineDuration)}</span>
                                    </div>

                                    {/* Sunrise/Sunset */}
                                    <div className="flex items-center gap-1">
                                        <Sunrise className="h-3 w-3 flex-shrink-0" />
                                        <span>{formatTime(data.sunrise)}</span>
                                        <Sunset className="h-3 w-3 flex-shrink-0 ml-1" />
                                        <span>{formatTime(data.sunset)}</span>
                                    </div>
                                </div>
                            </div>
                        )
                    })}
                </div>
            </CardContent>
        </Card>
    )
}
