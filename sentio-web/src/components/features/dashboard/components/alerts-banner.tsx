import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/components/ui/collapsible'
import { AlertTriangle, Info, AlertCircle, XCircle, ChevronDown, Clock, MapPin } from 'lucide-react'
import { Button } from '@/components/ui/button'
import type { WeatherAlert } from '@/types/api'
import { useState } from 'react'

interface AlertsBannerProps {
    alerts: WeatherAlert[]
    loading: boolean
    onAlertClick?: (alert: WeatherAlert) => void
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

function formatDateTime(dateString: string) {
    try {
        return new Date(dateString).toLocaleString('en-GB', {
            weekday: 'short',
            day: 'numeric',
            month: 'short',
            hour: '2-digit',
            minute: '2-digit'
        })
    } catch {
        return dateString
    }
}

export function AlertsBanner({ alerts, loading, onAlertClick }: AlertsBannerProps) {
    const safeAlerts = Array.isArray(alerts) ? alerts : []
    const [expandedAlerts, setExpandedAlerts] = useState<Set<number>>(new Set())

    const toggleAlert = (id: number) => {
        setExpandedAlerts(prev => {
            const next = new Set(prev)
            if (next.has(id)) {
                next.delete(id)
            } else {
                next.add(id)
            }
            return next
        })
    }

    const handleZoomToAlert = (e: React.MouseEvent, alert: WeatherAlert) => {
        e.stopPropagation()
        if (onAlertClick && alert.latitude && alert.longitude) {
            onAlertClick(alert)
        }
    }

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
                            const isExpanded = expandedAlerts.has(alert.id || i)
                            const hasLocation = alert.latitude && alert.longitude
                            return (
                                <Collapsible
                                    key={alert.id || i}
                                    open={isExpanded}
                                    onOpenChange={() => toggleAlert(alert.id || i)}
                                >
                                    <div className={`rounded-lg border p-3 ${config.bg}`}>
                                        <CollapsibleTrigger className="w-full text-left">
                                            <div className="flex items-start gap-3">
                                                <Icon className={`h-5 w-5 mt-0.5 flex-shrink-0 ${config.text}`} />
                                                <div className="flex-1 min-w-0">
                                                    <div className="flex items-center gap-2 flex-wrap">
                                                        <Badge className={config.badge}>{alert.severity.toUpperCase()}</Badge>
                                                        <span className={`font-medium text-sm ${config.text}`}>
                                                            {alert.localizedEvent || alert.eventEn || 'Weather Alert'}
                                                        </span>
                                                        <ChevronDown className={`h-4 w-4 ml-auto transition-transform ${isExpanded ? 'rotate-180' : ''}`} />
                                                    </div>
                                                    {(alert.localizedHeadline || alert.headlineEn) && (
                                                        <p className="text-sm font-medium mt-1 line-clamp-2">{alert.localizedHeadline || alert.headlineEn}</p>
                                                    )}
                                                </div>
                                            </div>
                                        </CollapsibleTrigger>
                                        <CollapsibleContent>
                                            <div className="mt-3 ml-8 space-y-2 text-sm">
                                                {/* Time range */}
                                                {(alert.onset || alert.effective || alert.expires) && (
                                                    <div className="flex items-center gap-2 text-muted-foreground">
                                                        <Clock className="h-4 w-4" />
                                                        <span>
                                                            {(alert.onset || alert.effective) && formatDateTime(alert.onset || alert.effective)}
                                                            {(alert.onset || alert.effective) && alert.expires && ' → '}
                                                            {alert.expires && formatDateTime(alert.expires)}
                                                        </span>
                                                    </div>
                                                )}
                                                {/* Description */}
                                                {(alert.localizedDescription || alert.descriptionEn) && (
                                                    <p className="text-muted-foreground">{alert.localizedDescription || alert.descriptionEn}</p>
                                                )}
                                                {/* Instruction */}
                                                {(alert.localizedInstruction || alert.instructionEn) && (
                                                    <div className="mt-2 p-2 bg-background/50 rounded text-xs">
                                                        <span className="font-medium">What to do: </span>
                                                        {alert.localizedInstruction || alert.instructionEn}
                                                    </div>
                                                )}
                                                {/* Location + Zoom button */}
                                                <div className="flex items-center justify-between">
                                                    {alert.city && (
                                                        <p className="text-xs text-muted-foreground italic">
                                                            Location: {alert.city}
                                                        </p>
                                                    )}
                                                    {hasLocation && onAlertClick && (
                                                        <Button
                                                            size="sm"
                                                            variant="outline"
                                                            className="h-7 text-xs gap-1"
                                                            onClick={(e) => handleZoomToAlert(e, alert)}
                                                        >
                                                            <MapPin className="h-3 w-3" />
                                                            View on map
                                                        </Button>
                                                    )}
                                                </div>
                                            </div>
                                        </CollapsibleContent>
                                    </div>
                                </Collapsible>
                            )
                        })}
                    </div>
                </ScrollArea>
            </CardContent>
        </Card>
    )
}
