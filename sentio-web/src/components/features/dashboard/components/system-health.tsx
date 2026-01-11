import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Cpu, Wifi, WifiOff, Activity } from 'lucide-react'
import type { Device } from '@/types/api'

interface SystemHealthProps {
    devices: Device[]
    loading: boolean
}

export function SystemHealth({ devices, loading }: SystemHealthProps) {
    const safeDevices = Array.isArray(devices) ? devices : []

    const onlineCount = safeDevices.filter((d) => {
        if (!d.lastSeen) return false
        // Backend sends LocalDateTime which implies UTC but lacks offset
        // We append 'Z' to ensure browser treats it as UTC
        const lastSeenTime = d.lastSeen.endsWith('Z') ? d.lastSeen : `${d.lastSeen}Z`
        return Date.now() - new Date(lastSeenTime).getTime() < 5 * 60 * 1000
    }).length

    const totalCount = safeDevices.length
    const healthPercent = totalCount > 0 ? Math.round((onlineCount / totalCount) * 100) : 0

    const getHealthColor = () => {
        if (healthPercent === 100) return 'text-green-500'
        if (healthPercent >= 50) return 'text-yellow-500'
        return 'text-red-500'
    }

    const getHealthStatus = () => {
        if (totalCount === 0) return 'No devices'
        if (healthPercent === 100) return 'All systems operational'
        if (healthPercent >= 50) return 'Partial connectivity'
        return 'Systems degraded'
    }

    return (
        <Card className="h-full">
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                    <Activity className="h-4 w-4" />
                    System Health
                </CardTitle>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="space-y-3">
                        <Skeleton className="h-4 w-full" />
                        <Skeleton className="h-4 w-3/4" />
                    </div>
                ) : (
                    <div className="space-y-2">
                        {/* Health Bar */}
                        <div className="flex items-center gap-3">
                            <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                                <div
                                    className={`h-full transition-all duration-1000 ${healthPercent === 100 ? 'bg-green-500' :
                                        healthPercent >= 50 ? 'bg-yellow-500' : 'bg-red-500'
                                        }`}
                                    style={{ width: `${healthPercent}%` }}
                                />
                            </div>
                            <span className={`text-sm font-medium ${getHealthColor()}`}>
                                {healthPercent}%
                            </span>
                        </div>

                        {/* Status Text */}
                        <p className={`text-sm ${getHealthColor()}`}>
                            {getHealthStatus()}
                        </p>

                        {/* Device List */}
                        <div className="space-y-2 pt-2 border-t">
                            {safeDevices.length === 0 ? (
                                <p className="text-xs text-muted-foreground">No devices registered</p>
                            ) : (
                                safeDevices.slice(0, 3).map((device) => {
                                    const lastSeenTime = device.lastSeen && (device.lastSeen.endsWith('Z') ? device.lastSeen : `${device.lastSeen}Z`)
                                    const isOnline = lastSeenTime &&
                                        Date.now() - new Date(lastSeenTime).getTime() < 5 * 60 * 1000

                                    return (
                                        <div key={device.id} className="flex items-center justify-between text-xs">
                                            <div className="flex items-center gap-2">
                                                <Cpu className="h-3 w-3 text-muted-foreground" />
                                                <span className="truncate max-w-[120px]">{device.name}</span>
                                            </div>
                                            {isOnline ? (
                                                <div className="flex items-center gap-1 text-green-500">
                                                    <Wifi className="h-3 w-3" />
                                                    <span>Online</span>
                                                </div>
                                            ) : (
                                                <div className="flex items-center gap-1 text-muted-foreground">
                                                    <WifiOff className="h-3 w-3" />
                                                    <span>Offline</span>
                                                </div>
                                            )}
                                        </div>
                                    )
                                })
                            )}
                            {safeDevices.length > 3 && (
                                <p className="text-xs text-muted-foreground">
                                    +{safeDevices.length - 3} more devices
                                </p>
                            )}
                        </div>
                    </div>
                )}
            </CardContent>
        </Card>
    )
}
