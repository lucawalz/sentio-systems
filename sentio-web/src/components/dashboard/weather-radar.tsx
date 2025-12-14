import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import {
    CloudRain,
    RefreshCw,
    WifiOff,
    Radar,
    Clock,
    Droplets
} from "lucide-react"
import { MapContainer, TileLayer, CircleMarker, useMap, WMSTileLayer } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'
import { useRadarData } from "../../hooks/useRadarData"

// Map center updater component
const MapCenterUpdater = ({ lat, lon }: { lat: number; lon: number }) => {
    const map = useMap();
    useEffect(() => {
        map.setView([lat, lon], 8);
    }, [lat, lon, map]);
    return null;
};

// Format timestamp for display
const formatTimestamp = (timestamp: string) => {
    if (!timestamp) return 'Unknown';
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });
};

// Legend component
const PrecipitationLegend = () => (
    <div className="absolute bottom-4 right-4 bg-card/90 backdrop-blur-sm rounded-lg p-3 border border-border/50 z-[1000]">
        <p className="text-xs font-medium text-foreground mb-2">Precipitation (mm/5min)</p>
        <div className="space-y-1">
            {[
                { color: 'rgba(0, 255, 255, 0.5)', label: '< 0.1' },
                { color: 'rgba(0, 150, 255, 0.7)', label: '0.1 - 1.0' },
                { color: 'rgba(255, 200, 0, 0.8)', label: '1.0 - 4.0' },
                { color: 'rgba(255, 100, 0, 0.9)', label: '4.0 - 8.0' },
                { color: 'rgba(255, 0, 0, 1)', label: '> 8.0' },
            ].map((item, idx) => (
                <div key={idx} className="flex items-center gap-2">
                    <div
                        className="w-4 h-3 rounded-sm"
                        style={{ backgroundColor: item.color }}
                    />
                    <span className="text-xs text-muted-foreground">{item.label}</span>
                </div>
            ))}
        </div>
    </div>
);

export function WeatherRadar() {
    const cardRef = useRef<HTMLDivElement>(null)
    const { metadata, loading, error, refetch } = useRadarData(300000)

    // Default to Germany center if no metadata
    const lat = metadata?.latitude || 51.1657;
    const lon = metadata?.longitude || 10.4515;

    useEffect(() => {
        if (cardRef.current) {
            gsap.fromTo(
                cardRef.current,
                { y: 30, opacity: 0, scale: 0.95 },
                { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out", delay: 0.4 }
            )
        }
    }, [])

    if (loading) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex items-center justify-center h-full flex-col space-y-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <div className="text-sm text-muted-foreground">Loading radar data...</div>
                </div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 min-h-[400px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <WifiOff className="w-12 h-12 text-destructive mb-4" />
                    <div className="text-lg font-semibold mb-2 text-destructive">Radar Unavailable</div>
                    <div className="text-sm text-muted-foreground mb-4 max-w-md">{error}</div>
                    <button
                        onClick={refetch}
                        className="px-4 py-2 bg-destructive text-destructive-foreground rounded-lg hover:bg-destructive/90 transition-colors flex items-center gap-2"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Retry
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div
            ref={cardRef}
            className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 min-h-[400px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300"
        >
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-blue-500/5 via-transparent to-cyan-500/5 pointer-events-none rounded-3xl" />

            <div className="relative z-10 h-full flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center space-x-3">
                        <CloudRain className="w-6 h-6 text-blue-500" />
                        <h3 className="text-xl font-bold text-foreground">Weather Radar</h3>
                    </div>
                    <div className="flex items-center gap-2">
                        {metadata && (
                            <div className="flex items-center text-xs text-muted-foreground">
                                <Clock className="w-3 h-3 mr-1" />
                                {formatTimestamp(metadata.timestamp)}
                            </div>
                        )}
                        <button
                            onClick={refetch}
                            className="p-2 hover:bg-card rounded-lg transition-colors"
                            title="Refresh radar"
                        >
                            <RefreshCw className="w-4 h-4 text-muted-foreground" />
                        </button>
                    </div>
                </div>

                {/* Stats Bar */}
                {metadata && (
                    <div className="flex items-center gap-4 mb-4 text-sm">
                        <div className="flex items-center gap-2 px-3 py-1.5 bg-card/60 rounded-lg">
                            <Droplets className="w-4 h-4 text-blue-500" />
                            <span className="text-muted-foreground">Coverage:</span>
                            <span className="font-medium text-foreground">
                                {metadata.coveragePercent?.toFixed(1) || 0}%
                            </span>
                        </div>
                        {metadata.hasActivePrecipitation && (
                            <div className="flex items-center gap-2 px-3 py-1.5 bg-blue-500/10 rounded-lg border border-blue-500/30">
                                <CloudRain className="w-4 h-4 text-blue-400" />
                                <span className="text-blue-400 text-xs font-medium">
                                    Max: {metadata.precipitationMax?.toFixed(2) || 0} mm/5min
                                </span>
                            </div>
                        )}
                    </div>
                )}

                {/* Map Container */}
                <div className="relative rounded-xl overflow-hidden border border-border/30" style={{ height: '320px' }}>
                    <MapContainer
                        center={[lat, lon]}
                        zoom={8}
                        style={{ height: '320px', width: '100%' }}
                        className="rounded-xl"
                        zoomControl={false}
                    >
                        <TileLayer
                            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />

                        {/* DWD Radar WMS Layer */}
                        <WMSTileLayer
                            url="https://maps.dwd.de/geoserver/dwd/wms"
                            layers="dwd:RX-Produkt"
                            format="image/png"
                            transparent={true}
                            opacity={0.7}
                        />

                        {/* User location marker */}
                        <CircleMarker
                            center={[lat, lon]}
                            radius={8}
                            pathOptions={{
                                fillColor: '#3b82f6',
                                fillOpacity: 1,
                                color: '#ffffff',
                                weight: 3
                            }}
                        />

                        <MapCenterUpdater lat={lat} lon={lon} />
                    </MapContainer>

                    {/* Legend */}
                    <PrecipitationLegend />

                    {/* No precipitation overlay */}
                    {metadata && !metadata.hasActivePrecipitation && (
                        <div className="absolute top-4 left-4 bg-green-500/10 backdrop-blur-sm border border-green-500/30 rounded-lg px-3 py-2 z-[1000]">
                            <div className="flex items-center gap-2">
                                <Radar className="w-4 h-4 text-green-500" />
                                <span className="text-xs text-green-400 font-medium">No precipitation in your area</span>
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="mt-4 text-center">
                    <p className="text-xs text-muted-foreground">
                        Powered by BrightSky • DWD Radar Data • Updates every 5 minutes
                    </p>
                </div>
            </div>
        </div>
    )
}
