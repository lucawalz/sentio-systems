import { useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import { Area, AreaChart, ResponsiveContainer, XAxis, YAxis, Tooltip, Legend } from 'recharts'
import { TrendingUp, TrendingDown, Minus } from 'lucide-react'
import type { RaspiWeather, Device } from '@/types/api'

interface SensorChartProps {
    devices: Device[]
    readings: RaspiWeather[]
    loading: boolean
}

export function SensorChart({ devices, readings, loading }: SensorChartProps) {
    const [selectedDevice, setSelectedDevice] = useState<string>('all')
    const safeReadings = Array.isArray(readings) ? readings : []
    const filteredReadings = selectedDevice === 'all' ? safeReadings : safeReadings.filter(r => r.deviceId === selectedDevice)

    // Sort by timestamp and take last 48 readings
    const sortedReadings = [...filteredReadings]
        .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
        .slice(-48)

    const chartData = sortedReadings.map(reading => ({
        time: new Date(reading.timestamp).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false }),
        temperature: Number(reading.temperature.toFixed(1)),
        humidity: Number(reading.humidity.toFixed(0)),
    }))

    const safeDevices = Array.isArray(devices) ? devices : []

    // Calculate trends
    const getTrend = () => {
        if (chartData.length < 2) return null
        const recent = chartData.slice(-6)
        const older = chartData.slice(-12, -6)
        if (recent.length === 0 || older.length === 0) return null

        const recentAvg = recent.reduce((a, b) => a + b.temperature, 0) / recent.length
        const olderAvg = older.reduce((a, b) => a + b.temperature, 0) / older.length
        const diff = recentAvg - olderAvg

        if (Math.abs(diff) < 0.5) return { direction: 'stable', diff: 0 }
        return { direction: diff > 0 ? 'up' : 'down', diff }
    }

    const trend = getTrend()

    // Custom tooltip
    const CustomTooltip = ({ active, payload, label }: { active?: boolean; payload?: Array<{ value: number; name: string; color: string }>; label?: string }) => {
        if (!active || !payload) return null
        return (
            <div className="rounded-lg border bg-background p-3 shadow-lg">
                <p className="text-xs text-muted-foreground mb-2">{label}</p>
                {payload.map((entry, index) => (
                    <p key={index} className="text-sm font-medium" style={{ color: entry.color }}>
                        {entry.name}: <span className="font-bold">{entry.value}{entry.name.includes('Temp') ? '°C' : '%'}</span>
                    </p>
                ))}
            </div>
        )
    }

    return (
        <Card className="h-full">
            <CardHeader className="pb-2">
                <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
                    <div className="flex items-start gap-3">
                        <div>
                            <CardTitle className="flex items-center gap-2">
                                Sensor Readings
                                {trend && (
                                    <Badge variant="outline" className="gap-1 text-xs font-normal">
                                        {trend.direction === 'up' && <TrendingUp className="h-3 w-3 text-orange-500" />}
                                        {trend.direction === 'down' && <TrendingDown className="h-3 w-3 text-blue-500" />}
                                        {trend.direction === 'stable' && <Minus className="h-3 w-3 text-muted-foreground" />}
                                        {trend.direction === 'stable' ? 'Stable' : `${trend.diff > 0 ? '+' : ''}${trend.diff.toFixed(1)}°C`}
                                    </Badge>
                                )}
                            </CardTitle>
                            <CardDescription className="mt-1">Temperature and humidity over time</CardDescription>
                        </div>
                    </div>
                    {safeDevices.length > 0 && (
                        <Tabs value={selectedDevice} onValueChange={setSelectedDevice}>
                            <TabsList className="h-8">
                                <TabsTrigger value="all" className="text-xs px-3">All</TabsTrigger>
                                {safeDevices.map(device => (
                                    <TabsTrigger key={device.id} value={device.id} className="text-xs px-3">
                                        {device.name}
                                    </TabsTrigger>
                                ))}
                            </TabsList>
                        </Tabs>
                    )}
                </div>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <Skeleton className="h-[280px] w-full rounded-lg" />
                ) : chartData.length === 0 ? (
                    <div className="flex flex-col items-center justify-center h-[280px] text-muted-foreground">
                        <div className="text-4xl mb-2">📊</div>
                        <p className="text-sm">No sensor readings available</p>
                    </div>
                ) : (
                    <ResponsiveContainer width="100%" height={280}>
                        <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                            <defs>
                                <linearGradient id="tempGradient" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stopColor="#f97316" stopOpacity={0.3} />
                                    <stop offset="100%" stopColor="#f97316" stopOpacity={0.05} />
                                </linearGradient>
                                <linearGradient id="humidityGradient" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stopColor="#3b82f6" stopOpacity={0.2} />
                                    <stop offset="100%" stopColor="#3b82f6" stopOpacity={0.05} />
                                </linearGradient>
                            </defs>
                            <XAxis
                                dataKey="time"
                                stroke="hsl(var(--muted-foreground))"
                                fontSize={10}
                                tickLine={false}
                                axisLine={false}
                                interval="preserveStartEnd"
                                tickMargin={8}
                            />
                            <YAxis
                                yAxisId="temp"
                                orientation="left"
                                stroke="#f97316"
                                fontSize={10}
                                tickLine={false}
                                axisLine={false}
                                tickFormatter={(v) => `${v}°`}
                                domain={['dataMin - 2', 'dataMax + 2']}
                                tickMargin={4}
                            />
                            <YAxis
                                yAxisId="humidity"
                                orientation="right"
                                stroke="#3b82f6"
                                fontSize={10}
                                tickLine={false}
                                axisLine={false}
                                tickFormatter={(v) => `${v}%`}
                                domain={[0, 100]}
                                tickMargin={4}
                            />
                            <Tooltip content={<CustomTooltip />} />
                            <Legend
                                verticalAlign="top"
                                height={36}
                                iconType="circle"
                                iconSize={8}
                                formatter={(value) => <span className="text-xs text-muted-foreground">{value}</span>}
                            />
                            {/* Temperature - fills down to its own dataMin */}
                            <Area
                                yAxisId="temp"
                                type="monotone"
                                dataKey="temperature"
                                stroke="#f97316"
                                strokeWidth={2}
                                fill="url(#tempGradient)"
                                baseValue="dataMin"
                                name="Temperature"
                                dot={false}
                                activeDot={{ r: 4, strokeWidth: 0, fill: "#f97316" }}
                                isAnimationActive={false}
                            />
                            {/* Humidity - fills down to 0 (its natural base) */}
                            <Area
                                yAxisId="humidity"
                                type="monotone"
                                dataKey="humidity"
                                stroke="#3b82f6"
                                strokeWidth={2}
                                fill="url(#humidityGradient)"
                                baseValue={0}
                                name="Humidity"
                                dot={false}
                                activeDot={{ r: 4, strokeWidth: 0, fill: "#3b82f6" }}
                                isAnimationActive={false}
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                )}
            </CardContent>
        </Card>
    )
}
