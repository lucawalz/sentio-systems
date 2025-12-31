import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { TrendingUp, TrendingDown, Minus, Calendar } from 'lucide-react'
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
    { label: '1 Week Ago', shortLabel: '1w', key: 'oneWeekAgo' },
    { label: '1 Month Ago', shortLabel: '1mo', key: 'oneMonthAgo' },
    { label: '3 Months Ago', shortLabel: '3mo', key: 'threeMonthsAgo' },
    { label: '1 Year Ago', shortLabel: '1y', key: 'oneYearAgo' },
]

function getTrendIcon(diff: number) {
    if (diff > 1) return <TrendingUp className="h-4 w-4 text-red-500" />
    if (diff < -1) return <TrendingDown className="h-4 w-4 text-blue-500" />
    return <Minus className="h-4 w-4 text-muted-foreground" />
}

function getTrendText(diff: number): string {
    if (Math.abs(diff) < 0.5) return 'Similar'
    const direction = diff > 0 ? 'warmer' : 'cooler'
    return `${Math.abs(diff).toFixed(1)}° ${direction}`
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
                    <div className="grid grid-cols-5 gap-2">
                        {[...Array(5)].map((_, i) => <Skeleton key={i} className="h-20" />)}
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
                    <div className="flex items-center justify-center h-20 text-muted-foreground text-sm">No historical data available</div>
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
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-2">
                    {periods.map((period) => {
                        const data = comparison[period.key]
                        if (!data) {
                            return (
                                <div key={period.key} className="rounded-lg border p-2 opacity-50">
                                    <p className="text-xs text-muted-foreground">{period.shortLabel}</p>
                                    <p className="text-sm text-muted-foreground">No data</p>
                                </div>
                            )
                        }
                        const pastTemp = data.temperatureMean
                        const diff = currentTemp - pastTemp
                        return (
                            <div key={period.key} className="rounded-lg border p-2">
                                <div className="flex items-center justify-between">
                                    <Badge variant="outline" className="text-xs">{period.shortLabel}</Badge>
                                    {getTrendIcon(diff)}
                                </div>
                                <p className="text-lg font-bold mt-1">{pastTemp.toFixed(1)}°</p>
                                <p className={`text-xs ${diff > 0 ? 'text-red-500' : diff < 0 ? 'text-blue-500' : 'text-muted-foreground'}`}>
                                    {getTrendText(diff)}
                                </p>
                            </div>
                        )
                    })}
                </div>
            </CardContent>
        </Card>
    )
}
