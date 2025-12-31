import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ScrollArea, ScrollBar } from '@/components/ui/scroll-area'
import { Clock, CloudRain, Sun, Cloud, CloudSnow } from 'lucide-react'
import type { WeatherForecast } from '@/types/api'

interface HourlyForecastProps {
    forecast: WeatherForecast[]
    loading: boolean
}

function getWeatherIcon(code: number | null | undefined, size = 'h-6 w-6') {
    if (!code) return <Cloud className={`${size} text-muted-foreground`} />
    if (code === 0) return <Sun className={`${size} text-yellow-500`} />
    if (code <= 3) return <Cloud className={`${size} text-gray-400`} />
    if (code <= 65) return <CloudRain className={`${size} text-blue-500`} />
    if (code <= 77) return <CloudSnow className={`${size} text-blue-300`} />
    return <CloudRain className={`${size} text-purple-500`} />
}

export function HourlyForecast({ forecast, loading }: HourlyForecastProps) {
    const safeForecast = Array.isArray(forecast) ? forecast : []
    const now = new Date()
    const next24Hours = safeForecast
        .filter(f => {
            const forecastTime = new Date(f.forecastDateTime || f.forecastDate)
            return forecastTime >= now
        })
        .slice(0, 24)

    return (
        <Card>
            <CardHeader className="pb-2">
                <CardTitle className="text-lg font-semibold flex items-center gap-2">
                    <Clock className="h-5 w-5" /> 24-Hour Forecast
                </CardTitle>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="flex gap-3 overflow-hidden">
                        {[...Array(8)].map((_, i) => <Skeleton key={i} className="h-24 w-16 flex-shrink-0" />)}
                    </div>
                ) : next24Hours.length === 0 ? (
                    <div className="flex items-center justify-center h-24 text-muted-foreground text-sm">No hourly forecast available</div>
                ) : (
                    <ScrollArea className="w-full">
                        <div className="flex gap-2 pb-3">
                            {next24Hours.map((hour, i) => {
                                const time = new Date(hour.forecastDateTime || hour.forecastDate)
                                const isNow = i === 0
                                const temp = hour.temperature ?? hour.temperatureMax
                                return (
                                    <div key={i} className={`flex flex-col items-center rounded-lg border p-3 min-w-[70px] ${isNow ? 'bg-primary/10 border-primary' : ''}`}>
                                        <span className={`text-xs font-medium ${isNow ? 'text-primary' : 'text-muted-foreground'}`}>
                                            {isNow ? 'Now' : time.toLocaleTimeString('en-US', { hour: 'numeric', hour12: true })}
                                        </span>
                                        <div className="my-2">{getWeatherIcon(hour.weatherCode)}</div>
                                        <span className="text-sm font-bold">{temp?.toFixed(0) ?? '--'}°</span>
                                        {hour.precipitationProbability > 0 && (
                                            <span className="text-xs text-blue-500 flex items-center gap-0.5">
                                                <CloudRain className="h-3 w-3" />{hour.precipitationProbability}%
                                            </span>
                                        )}
                                    </div>
                                )
                            })}
                        </div>
                        <ScrollBar orientation="horizontal" />
                    </ScrollArea>
                )}
            </CardContent>
        </Card>
    )
}
