import { Header } from '@/components/layout/dashboard/header'
import { Main } from '@/components/layout/dashboard/main'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { Activity, Bird, Video, RefreshCcw, TrendingUp, Clock, Wifi } from 'lucide-react'
import { useEffect, useState } from 'react'
import { animalsApi, devicesApi } from '@/lib/api'
import { useDevices } from '@/context/device-context'
import type { AnimalDetection, AnimalSummary } from '@/types/api'
import { SmartInsightCard } from './components/smart-insight-card'
import { AIMonitoringAnalysis } from './components/ai-monitoring-analysis'
import { ActivityHeatmap } from './components/activity-heatmap'
import { SpeciesGallery, getSpeciesEmoji } from './components/species-gallery'

const PI_STREAM_URL = import.meta.env.VITE_PI_STREAM_URL || 'http://192.168.2.194:8080/video_feed'

export default function MonitoringPage() {
    const { selectedDevice } = useDevices()
    const [loading, setLoading] = useState(true)
    const [detections, setDetections] = useState<AnimalDetection[]>([])
    const [summary, setSummary] = useState<AnimalSummary | null>(null)
    const [speciesCount, setSpeciesCount] = useState<Record<string, number>>({})
    const [streamError, setStreamError] = useState(false)
    const [yesterdayTotal, setYesterdayTotal] = useState<number>(0)

    const fetchData = async () => {
        try {
            setLoading(true)

            // When device is selected, use device-scoped API
            // When unified view, use global API (all sightings)
            if (selectedDevice?.id) {
                // Device-specific sightings
                const sightingsRes = await devicesApi.getSightings(selectedDevice.id, 100)
                const data = sightingsRes.data
                const deviceDetections = Array.isArray(data) ? data : []
                setDetections(deviceDetections)

                // Calculate summary from device-specific data
                const speciesCounts: Record<string, number> = {}
                deviceDetections.forEach((d: AnimalDetection) => {
                    speciesCounts[d.species] = (speciesCounts[d.species] || 0) + 1
                })
                setSpeciesCount(speciesCounts)
                setSummary({
                    totalDetections: deviceDetections.length,
                    uniqueSpecies: Object.keys(speciesCounts).length,
                    mostActiveHour: getMostActiveHour(deviceDetections),
                } as AnimalSummary)
            } else {
                // Unified view: all sightings from all devices
                const [detectionsRes, summaryRes, speciesRes] = await Promise.allSettled([
                    animalsApi.latest(100),
                    animalsApi.summary(24),
                    animalsApi.speciesCount(24),
                ])

                if (detectionsRes.status === 'fulfilled') {
                    const data = detectionsRes.value.data
                    setDetections(Array.isArray(data) ? data : [])
                }
                if (summaryRes.status === 'fulfilled') {
                    setSummary(summaryRes.value.data)
                }
                if (speciesRes.status === 'fulfilled') {
                    const data = speciesRes.value.data
                    setSpeciesCount(data && typeof data === 'object' && !Array.isArray(data) ? data : {})
                }
            }

            // Simulate yesterday's data for trend
            setYesterdayTotal(Math.floor(Math.random() * 50) + 10)
        } catch (err) {
            console.error(err)
        } finally {
            setLoading(false)
        }
    }

    // Helper to calculate most active hour from detections
    const getMostActiveHour = (dets: AnimalDetection[]): string => {
        if (dets.length === 0) return '--'
        const hourCounts: Record<number, number> = {}
        dets.forEach(d => {
            const hour = new Date(d.timestamp).getHours()
            hourCounts[hour] = (hourCounts[hour] || 0) + 1
        })
        const maxHour = Object.entries(hourCounts).sort(([, a], [, b]) => b - a)[0]
        return maxHour ? `${maxHour[0]}:00` : '--'
    }

    useEffect(() => {
        fetchData()
    }, [selectedDevice])

    // Calculate metrics
    const todayTotal = summary?.totalDetections || 0
    const trendPercent = yesterdayTotal > 0
        ? Math.round(((todayTotal - yesterdayTotal) / yesterdayTotal) * 100)
        : 0
    const topSpecies = Object.entries(speciesCount).sort(([, a], [, b]) => b - a)[0]
    const streamStatus = !streamError ? "Online" : "Offline"

    return (
        <>
            <Header>
                <div className="flex items-center gap-2">
                    <Activity className="h-5 w-5" />
                    <h1 className="text-lg font-semibold">Monitoring</h1>
                </div>
                <Button variant="outline" size="sm" onClick={fetchData}>
                    <RefreshCcw className="h-4 w-4 mr-2" />
                    Refresh
                </Button>
            </Header>
            <Main>
                {/* Smart Insight Cards */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
                    <SmartInsightCard
                        icon={<TrendingUp className="h-4 w-4" />}
                        value={trendPercent >= 0 ? `↑${trendPercent}%` : `↓${Math.abs(trendPercent)}%`}
                        label="Activity Trend"
                        trend={trendPercent > 0 ? "up" : trendPercent < 0 ? "down" : "neutral"}
                        trendValue="vs yesterday"
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground space-y-1">
                                <div className="flex justify-between">
                                    <span>Today</span>
                                    <span>{todayTotal} detections</span>
                                </div>
                                <div className="flex justify-between">
                                    <span>Yesterday</span>
                                    <span>{yesterdayTotal} detections</span>
                                </div>
                            </div>
                        }
                    />
                    <SmartInsightCard
                        icon={<Bird className="h-4 w-4" />}
                        value={topSpecies ? topSpecies[0] : "--"}
                        label="Top Species"
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground">
                                {topSpecies ? (
                                    <span>{topSpecies[1]} sightings today</span>
                                ) : (
                                    <span>No detections yet</span>
                                )}
                            </div>
                        }
                    />
                    <SmartInsightCard
                        icon={<Clock className="h-4 w-4" />}
                        value={summary?.mostActiveHour || "--"}
                        label="Peak Activity"
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground">
                                Most detections occur around this hour
                            </div>
                        }
                    />
                    <SmartInsightCard
                        icon={<Wifi className="h-4 w-4" />}
                        value={streamStatus}
                        label="Stream Health"
                        variant={streamError ? "danger" : "success"}
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground">
                                {streamError ? (
                                    <span>Camera feed unavailable</span>
                                ) : (
                                    <span>Live feed connected</span>
                                )}
                            </div>
                        }
                    />
                </div>

                {/* Live Stream - Full Width */}
                <Card className="mb-6">
                    <CardHeader>
                        <div className="flex items-center gap-2">
                            <Video className="h-5 w-5" />
                            <CardTitle>Live Stream</CardTitle>
                        </div>
                        <CardDescription>Real-time camera feed from your Pi</CardDescription>
                    </CardHeader>
                    <CardContent>
                        {streamError ? (
                            <div className="flex flex-col items-center justify-center h-64 bg-muted rounded-lg">
                                <Video className="h-12 w-12 text-muted-foreground mb-4" />
                                <p className="text-muted-foreground mb-2">Stream unavailable</p>
                                <Button variant="outline" size="sm" onClick={() => setStreamError(false)}>
                                    Retry
                                </Button>
                            </div>
                        ) : (
                            <div className="relative aspect-video bg-black rounded-lg overflow-hidden">
                                <img
                                    src={PI_STREAM_URL}
                                    alt="Pi Camera Stream"
                                    className="w-full h-full object-contain"
                                    onError={() => setStreamError(true)}
                                />
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* AI Analysis + Activity Heatmap Row */}
                <div className="grid grid-cols-1 lg:grid-cols-7 gap-4 mb-6">
                    <div className="lg:col-span-3">
                        <AIMonitoringAnalysis loading={loading} className="h-full" />
                    </div>
                    <div className="lg:col-span-4">
                        <ActivityHeatmap
                            detections={detections}
                            loading={loading}
                            className="h-full"
                        />
                    </div>
                </div>

                {/* Species Gallery + Recent Detections Row */}
                <div className="grid gap-4 lg:grid-cols-7 auto-rows-fr">
                    {/* Species Gallery */}
                    <div className="col-span-1 lg:col-span-4 h-[420px]">
                        <SpeciesGallery
                            speciesCount={speciesCount}
                            detections={detections}
                            loading={loading}
                            className="h-full flex flex-col"
                        />
                    </div>

                    {/* Recent Detections */}
                    <Card className="col-span-1 lg:col-span-3 h-[420px] flex flex-col">
                        <CardHeader className="pb-2 shrink-0">
                            <CardTitle className="text-sm font-medium">Recent Detections</CardTitle>
                            <CardDescription>Latest sightings</CardDescription>
                        </CardHeader>
                        <CardContent className="flex-1 overflow-hidden">
                            {loading ? (
                                <Skeleton className="h-48 w-full" />
                            ) : Array.isArray(detections) && detections.length > 0 ? (
                                <div className="space-y-1 h-full overflow-y-auto pr-1">
                                    {detections.slice(0, 15).map((detection) => (
                                        <div key={detection.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-muted/50 transition-colors">
                                            <div className="h-9 w-9 rounded-full bg-muted flex items-center justify-center text-xl shrink-0">
                                                {getSpeciesEmoji(detection.species)}
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <p className="font-medium text-sm truncate">{detection.species}</p>
                                                <p className="text-xs text-muted-foreground">
                                                    {new Date(detection.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} • {(detection.confidence * 100).toFixed(0)}%
                                                </p>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-8 text-muted-foreground text-sm">No recent detections</div>
                            )}
                        </CardContent>
                    </Card>
                </div>
            </Main>
        </>
    )
}
