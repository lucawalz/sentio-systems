import { useEffect, useRef, useState, useCallback } from 'react'
import Map from 'ol/Map'
import View from 'ol/View'
import TileLayer from 'ol/layer/Tile'
import ImageLayer from 'ol/layer/Image'
import OSM from 'ol/source/OSM'
import ImageStatic from 'ol/source/ImageStatic'
import { fromLonLat } from 'ol/proj'
import { register } from 'ol/proj/proj4'
import { get as getProjection } from 'ol/proj'
import Overlay from 'ol/Overlay'
import proj4 from 'proj4'
import pako from 'pako'
import { Play, Pause, Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'
import { precipitationToRGBA } from '@/lib/turbo-colormap'
import { weatherService } from '@/services/api/weather'
import type { Device, BrightSkyRadarResponse, BrightSkyRadarRecord } from '@/types/api'
import 'ol/ol.css'

// DWD DE1200 projection constants
const GRID_WIDTH = 1100
const GRID_HEIGHT = 1200
const GRID_PROJECTION_NAME = 'DE1200'
const GRID_PROJ_STRING = '+proj=stere +lat_0=90 +lat_ts=60 +lon_0=10 +a=6378137 +b=6356752.3142451802 +no_defs +x_0=543196.83521776402 +y_0=3622588.8619310018'
const GRID_EXTENT: [number, number, number, number] = [-500, -1199500, 1099500, 500]

// Germany center coordinates
const GERMANY_CENTER: [number, number] = [10.4515, 51.1657] // [lon, lat]

interface RadarFrame {
    label: string
    timestamp: string
    imageUrl: string
}

// Location to focus/zoom on
interface FocusLocation {
    latitude: number
    longitude: number
    label?: string
}

interface WeatherRadarMapProps {
    devices: Device[]
    coveragePercent?: number
    focusLocation?: FocusLocation | null
    onFocusComplete?: () => void
    className?: string
}

// Register the DWD projection
proj4.defs(GRID_PROJECTION_NAME, GRID_PROJ_STRING)
register(proj4)

/**
 * Decompresses base64-encoded, zlib-compressed radar data to Uint16Array.
 */
function decompressRadarData(base64Data: string): Uint16Array {
    // Decode base64 to bytes
    const compressed = Uint8Array.from(atob(base64Data), c => c.charCodeAt(0))
    // Decompress zlib
    const decompressed = pako.inflate(compressed)
    // Interpret as 2-byte integers
    return new Uint16Array(decompressed.buffer)
}

/**
 * Creates a canvas data URL from radar precipitation data.
 */
function createRadarImageUrl(precipitationData: Uint16Array): string {
    const canvas = document.createElement('canvas')
    canvas.width = GRID_WIDTH
    canvas.height = GRID_HEIGHT
    const ctx = canvas.getContext('2d')!
    const imageData = ctx.createImageData(GRID_WIDTH, GRID_HEIGHT)

    for (let i = 0; i < precipitationData.length; i++) {
        const [r, g, b, a] = precipitationToRGBA(precipitationData[i])
        imageData.data[i * 4] = r
        imageData.data[i * 4 + 1] = g
        imageData.data[i * 4 + 2] = b
        imageData.data[i * 4 + 3] = a
    }

    ctx.putImageData(imageData, 0, 0)
    return canvas.toDataURL()
}

/**
 * Processes radar records into displayable frames.
 */
function processRadarRecords(records: BrightSkyRadarRecord[]): RadarFrame[] {
    return records.map(record => {
        const data = decompressRadarData(record.precipitation_5)
        const imageUrl = createRadarImageUrl(data)
        // Extract time HH:MM from ISO timestamp
        const label = record.timestamp.substring(11, 16)
        return {
            label,
            timestamp: record.timestamp,
            imageUrl
        }
    })
}

/**
 * Calculate map center from device locations.
 */
function getMapCenter(devices: Device[]): [number, number] {
    // Ensure devices is an array
    const deviceList = Array.isArray(devices) ? devices : []
    const devicesWithLocation = deviceList.filter(d => d.latitude != null && d.longitude != null)

    if (devicesWithLocation.length === 0) {
        return GERMANY_CENTER
    }

    // Calculate centroid of all device locations
    const avgLon = devicesWithLocation.reduce((s, d) => s + d.longitude!, 0) / devicesWithLocation.length
    const avgLat = devicesWithLocation.reduce((s, d) => s + d.latitude!, 0) / devicesWithLocation.length

    return [avgLon, avgLat]
}

/**
 * Weather radar map component with animated precipitation overlay and device markers.
 */
export function WeatherRadarMap({ devices = [], coveragePercent, focusLocation, onFocusComplete, className }: WeatherRadarMapProps) {
    const mapRef = useRef<HTMLDivElement>(null)
    const mapInstanceRef = useRef<Map | null>(null)
    const radarLayerRef = useRef<ImageLayer<ImageStatic> | null>(null)
    const animationRef = useRef<number | null>(null)

    const [frames, setFrames] = useState<RadarFrame[]>([])
    const [currentFrameIndex, setCurrentFrameIndex] = useState(0)
    const [isPlaying, setIsPlaying] = useState(false)
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    // Effect to handle focusLocation - zoom and pan to the location
    useEffect(() => {
        if (!focusLocation || !mapInstanceRef.current) return

        const map = mapInstanceRef.current
        const view = map.getView()

        // Animate to the focus location
        view.animate({
            center: fromLonLat([focusLocation.longitude, focusLocation.latitude]),
            zoom: 10,
            duration: 500
        }, () => {
            // Call completion callback after animation
            onFocusComplete?.()
        })
    }, [focusLocation, onFocusComplete])

    // Fetch radar data from BrightSky
    useEffect(() => {
        const fetchRadarData = async () => {
            try {
                setIsLoading(true)
                setError(null)

                const data: BrightSkyRadarResponse = await weatherService.fetchRadarData()

                if (!data.radar || data.radar.length === 0) {
                    throw new Error('No radar data available')
                }

                // Process frames (this can take a moment)
                const processedFrames = processRadarRecords(data.radar)
                setFrames(processedFrames)
                setCurrentFrameIndex(0)
            } catch (err) {
                console.error('Error fetching radar data:', err)
                setError(err instanceof Error ? err.message : 'Failed to load radar data')
            } finally {
                setIsLoading(false)
            }
        }

        fetchRadarData()
    }, [])

    // Initialize OpenLayers map
    useEffect(() => {
        if (!mapRef.current || mapInstanceRef.current) return

        const center = getMapCenter(devices)

        // Create radar layer (initially empty)
        const radarLayer = new ImageLayer<ImageStatic>()
        radarLayerRef.current = radarLayer

        // Create device markers as HTML overlays for pulsing animation
        const deviceList = Array.isArray(devices) ? devices : []
        const overlays: Overlay[] = []

        deviceList
            .filter(d => d.latitude != null && d.longitude != null)
            .forEach(d => {
                // Create marker element
                const markerEl = document.createElement('div')
                markerEl.className = 'device-marker'
                markerEl.innerHTML = `
                    <div class="device-marker-ping"></div>
                    <div class="device-marker-dot"></div>
                `
                markerEl.title = d.name

                const overlay = new Overlay({
                    position: fromLonLat([d.longitude!, d.latitude!]),
                    element: markerEl,
                    positioning: 'center-center',
                    stopEvent: false
                })
                overlays.push(overlay)
            })

        // Create map
        const map = new Map({
            target: mapRef.current,
            layers: [
                new TileLayer({
                    source: new OSM()
                }),
                radarLayer
            ],
            view: new View({
                center: fromLonLat(center),
                zoom: deviceList.filter(d => d.latitude && d.longitude).length > 0 ? 8 : 6
            }),
            controls: []
        })

        // Add marker overlays to map
        overlays.forEach(overlay => map.addOverlay(overlay))

        mapInstanceRef.current = map

        return () => {
            map.setTarget(undefined)
            mapInstanceRef.current = null
        }
    }, [devices])

    // Update radar layer when frame changes
    useEffect(() => {
        if (!radarLayerRef.current || frames.length === 0) return

        const frame = frames[currentFrameIndex]
        if (!frame) return

        const projection = getProjection(GRID_PROJECTION_NAME)
        if (!projection) return

        const source = new ImageStatic({
            url: frame.imageUrl,
            projection: projection,
            imageExtent: GRID_EXTENT,
            interpolate: false,
            attributions: '© <a href="https://www.dwd.de/">DWD</a>'
        })

        radarLayerRef.current.setSource(source)
    }, [currentFrameIndex, frames])

    // Animation loop
    const nextFrame = useCallback(() => {
        setCurrentFrameIndex(prev => {
            if (prev >= frames.length - 1) {
                return 0
            }
            return prev + 1
        })
    }, [frames.length])

    useEffect(() => {
        if (isPlaying && frames.length > 0) {
            animationRef.current = window.setInterval(nextFrame, 500)
        }

        return () => {
            if (animationRef.current) {
                clearInterval(animationRef.current)
                animationRef.current = null
            }
        }
    }, [isPlaying, nextFrame, frames.length])

    // Auto-start playback when frames load
    useEffect(() => {
        if (frames.length > 0 && !isPlaying) {
            setIsPlaying(true)
        }
    }, [frames.length])

    const handlePlayPause = () => {
        setIsPlaying(prev => !prev)
    }

    const handleSliderChange = (value: number[]) => {
        setIsPlaying(false)
        setCurrentFrameIndex(value[0])
    }

    if (error) {
        return (
            <div className={cn("flex items-center justify-center bg-muted rounded-lg text-muted-foreground text-sm", className)}>
                {error}
            </div>
        )
    }

    return (
        <div className={cn("relative w-full h-full rounded-lg overflow-hidden", className)}>
            {/* Loading overlay */}
            {isLoading && (
                <div className="absolute inset-0 flex items-center justify-center bg-muted/80 z-20">
                    <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
            )}

            {/* Map container */}
            <div ref={mapRef} className="w-full h-full" />

            {/* Minimal controls overlay */}
            {frames.length > 0 && (
                <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/40 to-transparent px-2 py-1.5 z-10">
                    <div className="flex items-center gap-2">
                        <button
                            onClick={handlePlayPause}
                            className="text-white/90 hover:text-white transition-colors"
                            aria-label={isPlaying ? 'Pause' : 'Play'}
                        >
                            {isPlaying ? (
                                <Pause className="h-4 w-4" />
                            ) : (
                                <Play className="h-4 w-4" />
                            )}
                        </button>

                        <div className="flex-1 flex items-center gap-1">
                            {frames.map((_, idx) => (
                                <button
                                    key={idx}
                                    onClick={() => handleSliderChange([idx])}
                                    className={`h-1 flex-1 rounded-full transition-colors ${idx === currentFrameIndex
                                        ? 'bg-white'
                                        : idx < currentFrameIndex
                                            ? 'bg-white/60'
                                            : 'bg-white/30'
                                        }`}
                                    aria-label={`Frame ${idx + 1}`}
                                />
                            ))}
                        </div>

                        <span className="text-[10px] text-white/90 font-mono">
                            {frames[currentFrameIndex]?.label || '--:--'}
                        </span>
                    </div>
                </div>
            )}

            {/* Coverage badge */}
            {coveragePercent != null && (
                <div className="absolute top-1 left-1 bg-black/50 text-white text-[10px] px-1.5 py-0.5 rounded z-10">
                    {coveragePercent.toFixed(0)}% coverage
                </div>
            )}
        </div>
    )
}
