import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { ScrollArea } from '@/components/ui/scroll-area'
import { AlertTriangle, Info, AlertCircle, XCircle } from 'lucide-react'
import type { WeatherAlert } from '@/types/api'

interface AlertsBannerProps {
    alerts: WeatherAlert[]
    loading: boolean
}

function getSeverityConfig(severity: string) {
    switch (severity) {
        case 'extreme':
            return {
                icon: XCircle,
                bg: 'bg-red-500/10 border-red-500',
                badge: 'bg-red-500 text-white',
                text: 'text-red-700 dark:text-red-400',
            }
        case 'severe':
            return {
                icon: AlertCircle,
                bg: 'bg-orange-500/10 border-orange-500',
                badge: 'bg-orange-500 text-white',
                text: 'text-orange-700 dark:text-orange-400',
            }
        case 'moderate':
            return {
                icon: AlertTriangle,
                bg: 'bg-yellow-500/10 border-yellow-500',
                badge: 'bg-yellow-500 text-black',
                text: 'text-yellow-700 dark:text-yellow-400',
            }
        default:
            return {
                icon: Info,
                bg: 'bg-blue-500/10 border-blue-500',
                badge: 'bg-blue-500 text-white',
                text: 'text-blue-700 dark:text-blue-400',
            }
    }
}

export function AlertsBanner({ alerts, loading }: AlertsBannerProps) {
    const safeAlerts = Array.isArray(alerts) ? alerts : []

    if (loading) {
        return (
            <Card className="h-full border-dashed">
                <CardHeader className="pb-2">
                    <CardTitle className="text-lg font-semibold flex items-center gap-2">
                        <AlertTriangle className="h-5 w-5" />
                        Weather Alerts
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="h-32 flex items-center justify-center text-muted-foreground text-sm">
                        Loading alerts...
                    </div>
                </CardContent>
            </Card>
        )
    }

    if (safeAlerts.length === 0) {
        return (
            <Card className="h-full border-dashed border-green-300 bg-green-500/5">
                <CardHeader className="pb-2">
                    <CardTitle className="text-lg font-semibold flex items-center gap-2 text-green-700 dark:text-green-400">
                        <AlertTriangle className="h-5 w-5" />
                        Weather Alerts
                    </CardTitle>
                </CardHeader>
                <CardContent>
                    <div className="flex flex-col items-center justify-center py-8 text-center">
                        <Info className="h-10 w-10 text-green-500 mb-2" />
                        <p className="font-medium text-green-700 dark:text-green-400">No active alerts</p>
                        <p className="text-sm text-muted-foreground">Weather conditions are normal</p>
                    </div>
                </CardContent>
            </Card>
        )
    }

    return (
        <Card className="h-full border-orange-300 bg-orange-500/5">
            <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                    <CardTitle className="text-lg font-semibold flex items-center gap-2 text-orange-700 dark:text-orange-400">
                        <AlertTriangle className="h-5 w-5" />
                        Weather Alerts
                    </CardTitle>
                    <Badge variant="destructive">{safeAlerts.length}</Badge>
                </div>
            </CardHeader>
            <CardContent>
                <ScrollArea className="h-[200px] pr-3">
                    <div className="space-y-3">
                        {safeAlerts.map((alert, i) => {
                            const config = getSeverityConfig(alert.severity)
                            const Icon = config.icon
                            return (
                                <div key={alert.id || i} className={`rounded-lg border p-3 ${config.bg}`}>
                                    <div className="flex items-start gap-3">
                                        <Icon className={`h-5 w-5 mt-0.5 ${config.text}`} />
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2 flex-wrap">
                                                <Badge className={config.badge}>{alert.severity.toUpperCase()}</Badge>
                                                <span className="text-xs text-muted-foreground">{alert.event}</span>
                                            </div>
                                            <p className={`font-medium mt-1 ${config.text}`}>{alert.headline}</p>
                                        </div>
                                    </div>
                                </div>
                            )
                        })}
                    </div>
                </ScrollArea>
            </CardContent>
        </Card>
    )
}
