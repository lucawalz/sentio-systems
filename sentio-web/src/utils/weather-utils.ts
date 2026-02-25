import pako from 'pako'
import { precipitationToRGBA } from '@/lib/turbo-colormap'
import type { BrightSkyRadarRecord, Device } from '@/types/api'

// DWD DE1200 projection constants
export const GRID_WIDTH = 1100
export const GRID_HEIGHT = 1200
export const GRID_PROJECTION_NAME = 'DE1200'
export const GRID_PROJ_STRING = '+proj=stere +lat_0=90 +lat_ts=60 +lon_0=10 +a=6378137 +b=6356752.3142451802 +no_defs +x_0=543196.83521776402 +y_0=3622588.8619310018'
export const GRID_EXTENT: [number, number, number, number] = [-500, -1199500, 1099500, 500]

// Germany center coordinates
export const GERMANY_CENTER: [number, number] = [10.4515, 51.1657] // [lon, lat]

export interface RadarFrame {
    label: string
    timestamp: string
    imageUrl: string
}

/**
 * Decompresses base64-encoded, zlib-compressed radar data to Uint16Array.
 */
export function decompressRadarData(base64Data: string): Uint16Array {
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
export function createRadarImageUrl(precipitationData: Uint16Array): string {
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
export function processRadarRecords(records: BrightSkyRadarRecord[]): RadarFrame[] {
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
export function getMapCenter(devices: Device[]): [number, number] {
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
