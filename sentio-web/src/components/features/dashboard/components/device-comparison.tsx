import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Cpu, Thermometer, Droplets, Gauge, Clock, Wifi, WifiOff } from 'lucide-react'
import type { RaspiWeather, Device } from '@/types/api'

interface DeviceComparisonProps {
    devices: Device[]
    readings: RaspiWeather[]
    loading: boolean
}

export function DeviceComparison({ devices, readings, loading }: DeviceComparisonProps) {
    const safeDevices = Array.isArray(devices) ? devices : []
    const safeReadings = Array.isArray(readings) ? readings : []

    // Get latest reading for each device
    const deviceReadings = safeDevices.map(device => {
        const latestReading = safeReadings
            .filter(r => r.deviceId === device.id)
            .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())[0]

        return {
            device,
            reading: latestReading || null,
        }
    })

    if (loading) {
        return (
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-lg font-semibold flex items-center gap-2">
                        <Cpu className="h-5 w-5" />
                        Device Comparison
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {[...Array(2)].map((_, i) => (
                            <Skeleton key={i} className="h-40 rounded-xl" />
                        ))}
                    </div>
                </CardContent>
            </Card>
        )
    }

    if (deviceReadings.length === 0) {
        return (
            <Card>
                <CardHeader className="pb-3">
                    <CardTitle className="text-lg font-semibold flex items-center gap-2">
                        <Cpu className="h-5 w-5" />
                        Device Comparison
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex items-center justify-center h-32 text-muted-foreground text-sm">
                        No devices registered
                    </div>
                </CardContent>
            </Card>
        )
    }

    // Calculate averages for comparison
    const temps = deviceReadings.filter(d => d.reading?.temperature != null).map(d => d.reading!.temperature)
    const avgTemp = temps.length > 0 ? temps.reduce((a, b) => a + b, 0) / temps.length : null

    return (
        <Card>
            <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle className="text-lg font-semibold flex items-center gap-2">
                            <Cpu className="h-5 w-5" />
                            Device Comparison
                        </CardTitle>
                        <CardDescription className="mt-1">
                            {deviceReadings.length} device{deviceReadings.length !== 1 ? 's' : ''} registered
                        </CardDescription>
                    </div>
                    {avgTemp !== null && (
                        <Badge variant="outline" className="text-sm font-medium">
                            Avg: {avgTemp.toFixed(1)}°C
                        </Badge>
                    )}
                </div>
            </CardHeader>
            <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {deviceReadings.map(({ device, reading }) => {
                        const isOnline = device.lastSeen
                            ? new Date(device.lastSeen.endsWith('Z') ? device.lastSeen : `${device.lastSeen}Z`).getTime() > Date.now() - 5 * 60 * 1000
                            : false

                        return (
                            <div
                                key={device.id}
                                className="group rounded-xl border bg-gradient-to-br from-muted/30 to-muted/10 p-5 transition-all hover:shadow-md hover:border-primary/20"
                            >
                                {/* Header */}
                                <div className="flex items-center justify-between mb-4">
                                    <div className="flex items-center gap-3">
                                        <div className={`p-2 rounded-lg ${isOnline ? 'bg-green-500/10' : 'bg-muted'}`}>
                                            {isOnline ? (
                                                <Wifi className="h-4 w-4 text-green-500" />
                                            ) : (
                                                <WifiOff className="h-4 w-4 text-muted-foreground" />
                                            )}
                                        </div>
                                        <div className="min-w-0">
                                            <p className="font-semibold truncate">{device.name}</p>
                                            <p className="text-xs text-muted-foreground font-mono truncate max-w-[140px]">{device.id}</p>
                                        </div>
                                    </div>
                                    <Badge variant={isOnline ? 'default' : 'secondary'} className="text-xs">
                                        {isOnline ? 'Online' : 'Offline'}
                                    </Badge>
                                </div>

                                {reading ? (
                                    <>
                                        {/* Main Stats Grid */}
                                        <div className="grid grid-cols-3 gap-3 mb-4">
                                            <div className="text-center p-3 rounded-lg bg-background/50">
                                                <Thermometer className="h-4 w-4 mx-auto mb-1 text-orange-500" />
                                                <p className="text-lg font-bold">{reading.temperature.toFixed(1)}°</p>
                                                <p className="text-[10px] text-muted-foreground uppercase tracking-wide">Temp</p>
                                            </div>
                                            <div className="text-center p-3 rounded-lg bg-background/50">
                                                <Droplets className="h-4 w-4 mx-auto mb-1 text-blue-500" />
                                                <p className="text-lg font-bold">{reading.humidity.toFixed(0)}%</p>
                                                <p className="text-[10px] text-muted-foreground uppercase tracking-wide">Humidity</p>
                                            </div>
                                            <div className="text-center p-3 rounded-lg bg-background/50">
                                                <Gauge className="h-4 w-4 mx-auto mb-1 text-purple-500" />
                                                <p className="text-lg font-bold">{reading.pressure.toFixed(0)}</p>
                                                <p className="text-[10px] text-muted-foreground uppercase tracking-wide">hPa</p>
                                            </div>
                                        </div>

                                        {/* Footer */}
                                        <div className="flex items-center justify-center gap-1.5 text-xs text-muted-foreground pt-3 border-t border-border/50">
                                            <Clock className="h-3 w-3" />
                                            Last updated {new Date(reading.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </div>
                                    </>
                                ) : (
                                    <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
                                        <Cpu className="h-8 w-8 mb-2 opacity-30" />
                                        <p className="text-sm">No readings available</p>
                                    </div>
                                )}
                            </div>
                        )
                    })}
                </div>
            </CardContent>
        </Card>
    )
}
