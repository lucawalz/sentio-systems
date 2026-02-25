import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { ScrollArea } from '@/components/ui/scroll-area'
import { Bird, AlertTriangle, Cpu, Activity } from 'lucide-react'
import type { AnimalDetection, WeatherAlert, Device } from '@/types/api'
import { capitalizeSpecies, getSpeciesEmoji } from './species-gallery'

type ActivityEvent =
    | { type: 'detection'; data: AnimalDetection; timestamp: Date }
    | { type: 'alert'; data: WeatherAlert; timestamp: Date }
    | { type: 'device'; data: { device: Device; status: 'online' | 'offline' }; timestamp: Date }

interface ActivityFeedProps {
    detections: AnimalDetection[]
    alerts: WeatherAlert[]
    devices: Device[]
    loading: boolean
}

export function ActivityFeed({ detections, alerts, loading }: ActivityFeedProps) {
    // Combine all events into a unified timeline
    const events: ActivityEvent[] = []

    // Add detections
    const safeDetections = Array.isArray(detections) ? detections : []
    safeDetections.slice(0, 10).forEach((d) => {
        events.push({
            type: 'detection',
            data: d,
            timestamp: new Date(d.timestamp),
        })
    })

    // Add alerts
    const safeAlerts = Array.isArray(alerts) ? alerts : []
    safeAlerts.slice(0, 5).forEach((a) => {
        events.push({
            type: 'alert',
            data: a,
            timestamp: new Date(a.onset || a.effective),
        })
    })

    // Sort by timestamp (most recent first)
    events.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime())

    const getRelativeTime = (date: Date): string => {
        const now = new Date()
        const diffMs = now.getTime() - date.getTime()
        const diffMins = Math.floor(diffMs / 60000)
        const diffHours = Math.floor(diffMins / 60)
        const diffDays = Math.floor(diffHours / 24)

        if (diffMins < 1) return 'Just now'
        if (diffMins < 60) return `${diffMins}m ago`
        if (diffHours < 24) return `${diffHours}h ago`
        return `${diffDays}d ago`
    }

    const getEventIcon = (event: ActivityEvent) => {
        switch (event.type) {
            case 'detection':
                return <Bird className="h-4 w-4 text-emerald-500" />
            case 'alert':
                return <AlertTriangle className="h-4 w-4 text-orange-500" />
            case 'device':
                return <Cpu className="h-4 w-4 text-blue-500" />
        }
    }

    const getEventTitle = (event: ActivityEvent): string => {
        switch (event.type) {
            case 'detection':
                return `${getSpeciesEmoji(event.data.species)} ${capitalizeSpecies(event.data.species)}`
            case 'alert':
                return event.data.localizedEvent || event.data.eventEn || 'Weather Alert'
            case 'device':
                return `${event.data.device.name} ${event.data.status}`
        }
    }

    const getEventSubtitle = (event: ActivityEvent): string => {
        switch (event.type) {
            case 'detection':
                return `${(event.data.confidence * 100).toFixed(0)}% confidence`
            case 'alert':
                return event.data.severity
            case 'device':
                return event.data.device.id
        }
    }

    const getEventBadgeColor = (event: ActivityEvent): string => {
        switch (event.type) {
            case 'detection':
                return 'bg-emerald-500/10 text-emerald-500'
            case 'alert': {
                const severity = event.data.severity
                if (severity === 'extreme' || severity === 'severe') return 'bg-red-500/10 text-red-500'
                if (severity === 'moderate') return 'bg-orange-500/10 text-orange-500'
                return 'bg-yellow-500/10 text-yellow-500'
            }
            case 'device':
                return event.data.status === 'online'
                    ? 'bg-green-500/10 text-green-500'
                    : 'bg-gray-500/10 text-gray-500'
        }
    }

    return (
        <Card className="h-full">
            <CardHeader className="pb-2">
                <CardTitle className="text-sm font-medium flex items-center gap-2">
                    <Activity className="h-4 w-4" />
                    Activity Feed
                </CardTitle>
            </CardHeader>
            <CardContent>
                {loading ? (
                    <div className="space-y-3">
                        {[...Array(5)].map((_, i) => (
                            <div key={i} className="flex items-start gap-3">
                                <Skeleton className="h-8 w-8 rounded-full" />
                                <div className="flex-1 space-y-1">
                                    <Skeleton className="h-4 w-3/4" />
                                    <Skeleton className="h-3 w-1/2" />
                                </div>
                            </div>
                        ))}
                    </div>
                ) : events.length === 0 ? (
                    <div className="flex flex-col items-center justify-center py-4 text-center text-muted-foreground">
                        <Activity className="h-8 w-8 mb-2" />
                        <p className="text-sm">No recent activity</p>
                        <p className="text-xs">Detections and alerts will appear here</p>
                    </div>
                ) : (
                    <ScrollArea className="h-[180px] pr-3">
                        <div className="space-y-3">
                            {events.slice(0, 15).map((event, i) => (
                                <div key={i} className="flex items-start gap-3 group">
                                    {/* Icon */}
                                    <div className={`p-2 rounded-full ${getEventBadgeColor(event)}`}>
                                        {getEventIcon(event)}
                                    </div>

                                    {/* Content */}
                                    <div className="flex-1 min-w-0">
                                        <div className="flex items-center justify-between gap-2">
                                            <p className="text-sm font-medium truncate">
                                                {getEventTitle(event)}
                                            </p>
                                            <span className="text-xs text-muted-foreground whitespace-nowrap">
                                                {getRelativeTime(event.timestamp)}
                                            </span>
                                        </div>
                                        <p className="text-xs text-muted-foreground capitalize">
                                            {getEventSubtitle(event)}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </ScrollArea>
                )}
            </CardContent>
        </Card>
    )
}
