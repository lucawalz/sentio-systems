import { useEffect, useState } from 'react'
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { RefreshCw, Cloud, Droplets, Activity, MapPin } from 'lucide-react'
import { alertsApi, devicesApi } from '@/lib/api'
import { WeatherRadarMap } from './weather-radar-map'
import { useDevices } from '@/context/device-context'
import type { RadarMetadata } from '@/types/api'

interface WeatherRadarProps {
    loading?: boolean
}

export function WeatherRadar({ loading: parentLoading }: WeatherRadarProps) {
    const { devices, selectedDevice, focusLocation, setFocusLocation } = useDevices()
    const [metadata, setMetadata] = useState<RadarMetadata | null>(null)
    const [loading, setLoading] = useState(true)
    const [refreshing, setRefreshing] = useState(false)
    const [refreshKey, setRefreshKey] = useState(0)

    // Determine which devices to show on map
    // If a specific device is selected, show only that one
    // If unified view (null), use primary device (or first) for centering, but still show all device markers
    const safeDevices = Array.isArray(devices) ? devices : []

    // For map centering: use selected device, or primary device in unified view
    const primaryDevice = safeDevices.find(d => d.isPrimary) || safeDevices[0]
    const centerDevice = selectedDevice || primaryDevice

    // Show all devices as markers in unified view, or just selected device
    const displayDevices = selectedDevice
        ? [selectedDevice]
        : (centerDevice ? [centerDevice] : safeDevices)

    const fetchData = async () => {
        try {
            setLoading(true)
            // Use device-scoped radar when a device is selected
            if (selectedDevice?.id) {
                const radarRes = await devicesApi.getRadar(selectedDevice.id)
                if (radarRes.data?.radarEndpoint) {
                    // Store the radar endpoint for the map to use
                    setMetadata({
                        latitude: selectedDevice.latitude,
                        longitude: selectedDevice.longitude,
                        radarEndpoint: radarRes.data.radarEndpoint,
                        distance: 50, // Default distance in km
                        coveragePercent: 0,
                    } as unknown as RadarMetadata)
                }
            } else {
                // Unified view: use global radar (primary device)
                const metadataRes = await alertsApi.radarLatest()
                if (metadataRes.data) {
                    setMetadata(metadataRes.data as RadarMetadata)
                }
            }
        } catch (err) {
            console.log('Radar metadata not available:', err)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => { fetchData() }, [selectedDevice])

    // Refetch when device selection changes
    useEffect(() => {
        setRefreshKey(prev => prev + 1)
    }, [selectedDevice])

    const handleRefresh = async () => {
        setRefreshing(true)
        try {
            await alertsApi.fetchRadar(50)
            await fetchData()
            setRefreshKey(prev => prev + 1)
        } catch (err) {
            console.error('Failed to refresh radar:', err)
        } finally {
            setRefreshing(false)
        }
    }

    // Clear focus location after it's been used by the map
    const handleFocusComplete = () => {
        setFocusLocation(null)
    }

    const isLoading = loading || parentLoading

    // Show device name in subtitle if specific device selected
    const subtitle = selectedDevice
        ? `${selectedDevice.name} area`
        : 'BrightSky precipitation radar'

    return (
        <Card className="h-full flex flex-col">
            <CardHeader className="pb-2 flex-none">
                <div className="flex items-center justify-between">
                    <div>
                        <CardTitle className="text-lg font-semibold flex items-center gap-2">
                            <Cloud className="h-5 w-5" />
                            Weather Radar
                        </CardTitle>
                        <CardDescription>{subtitle}</CardDescription>
                    </div>
                    <Button size="sm" variant="outline" onClick={handleRefresh} disabled={refreshing}>
                        <RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} />
                    </Button>
                </div>
                {/* Radar metadata from backend */}
                {metadata && (
                    <div className="flex flex-wrap gap-2 mt-2">
                        {metadata.hasActivePrecipitation && (
                            <Badge variant="destructive" className="text-xs gap-1">
                                <Activity className="h-3 w-3" /> Active Rain
                            </Badge>
                        )}
                        <Badge variant="outline" className="text-xs gap-1">
                            <Droplets className="h-3 w-3" />
                            {metadata.coveragePercent?.toFixed(0) || 0}% coverage
                        </Badge>
                        {metadata.precipitationMax != null && metadata.precipitationMax > 0 && (
                            <Badge variant="outline" className="text-xs gap-1">
                                Max: {metadata.precipitationMax.toFixed(1)} mm/h
                            </Badge>
                        )}
                        <Badge variant="secondary" className="text-xs gap-1">
                            <MapPin className="h-3 w-3" />
                            {metadata.distance}km radius
                        </Badge>
                    </div>
                )}
            </CardHeader>
            <CardContent className="flex-1 flex flex-col">
                {isLoading ? (
                    <Skeleton className="flex-1 w-full rounded-lg min-h-[200px]" />
                ) : (
                    <WeatherRadarMap
                        key={refreshKey}
                        devices={displayDevices}
                        coveragePercent={metadata?.coveragePercent}
                        focusLocation={focusLocation}
                        onFocusComplete={handleFocusComplete}
                        className="flex-1 min-h-[200px]"
                    />
                )}
            </CardContent>
        </Card>
    )
}
