import { useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { RefreshCw, ExternalLink, Cloud } from 'lucide-react'
import { alertsApi } from '@/lib/api'
import type { RadarMetadata, RadarEndpointConfig } from '@/types/api'

interface WeatherRadarProps {
    loading?: boolean
}

export function WeatherRadar({ loading: parentLoading }: WeatherRadarProps) {
    const [radarUrl, setRadarUrl] = useState<string | null>(null)
    const [metadata, setMetadata] = useState<RadarMetadata | null>(null)
    const [loading, setLoading] = useState(true)
    const [refreshing, setRefreshing] = useState(false)

    const fetchRadar = async () => {
        try {
            const [endpointRes, latestRes] = await Promise.allSettled([
                alertsApi.radarEndpoint(50, 'compressed'),
                alertsApi.radarLatest(),
            ])
            if (endpointRes.status === 'fulfilled') {
                const config = endpointRes.value.data as RadarEndpointConfig
                setRadarUrl(config.radarEndpoint)
            }
            if (latestRes.status === 'fulfilled') {
                setMetadata(latestRes.value.data as RadarMetadata)
            }
        } catch (err) {
            console.error('Failed to fetch radar data:', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { fetchRadar() }, [])

    const handleRefresh = async () => {
        setRefreshing(true)
        try {
            await alertsApi.fetchRadar(50)
            await fetchRadar()
        } catch (err) {
            console.error('Failed to refresh radar:', err)
        } finally {
            setRefreshing(false)
        }
    }

    const isLoading = loading || parentLoading

    return (
        <Card className="h-full">
            <CardHeader className="pb-2">
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle className="text-lg font-semibold flex items-center gap-2">
                            <Cloud className="h-5 w-5" />
                            Weather Radar
                        </CardTitle>
                        <CardDescription>BrightSky precipitation radar</CardDescription>
                    </div>
                    <div className="flex gap-2">
                        <Button size="sm" variant="outline" onClick={handleRefresh} disabled={refreshing}>
                            <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
                        </Button>
                        {radarUrl && (
                            <Button size="sm" variant="outline" asChild>
                                <a href={radarUrl} target="_blank" rel="noopener noreferrer">
                                    <ExternalLink className="h-4 w-4" />
                                </a>
                            </Button>
                        )}
                    </div>
                </div>
            </CardHeader>
            <CardContent>
                {isLoading ? (
                    <Skeleton className="h-[200px] w-full rounded-lg" />
                ) : radarUrl ? (
                    <div className="space-y-3">
                        <div className="relative rounded-lg overflow-hidden bg-muted h-[180px]">
                            <iframe src={radarUrl} className="w-full h-full border-0" title="Weather Radar" loading="lazy" />
                        </div>
                        {metadata && (
                            <div className="flex flex-wrap gap-2">
                                {metadata.hasActivePrecipitation && <Badge variant="destructive">Active Precipitation</Badge>}
                                <Badge variant="secondary">{metadata.coveragePercent?.toFixed(0) || 0}% coverage</Badge>
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="flex items-center justify-center h-[200px] text-muted-foreground">Radar data unavailable</div>
                )}
            </CardContent>
        </Card>
    )
}
