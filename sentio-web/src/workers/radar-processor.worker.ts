/**
 * Web Worker for processing weather radar data off the main thread.
 *
 * Handles zlib decompression, turbo-colormap pixel mapping, and canvas
 * rendering for each radar frame, posting progress back incrementally.
 */
import pako from 'pako'

// ── Turbo colormap (inlined to keep worker self-contained) ──────────
const kRedVec4 = [0.13572138, 4.6153926, -42.66032258, 132.13108234]
const kRedVec2 = [-152.94239396, 59.28637943]
const kGreenVec4 = [0.09140261, 2.19418839, 4.84296658, -14.18503333]
const kGreenVec2 = [4.27729857, 2.82956604]
const kBlueVec4 = [0.1066733, 12.64194608, -60.58204836, 110.36276771]
const kBlueVec2 = [-89.90310912, 27.34824973]

function saturate(x: number): number {
    return Math.max(0, Math.min(1, x))
}

function dot4(a: number[], x: number): number {
    return a[0] + a[1] * x + a[2] * x * x + a[3] * x * x * x
}

function dot2(a: number[], x: number): number {
    return a[0] * x * x * x * x + a[1] * x * x * x * x * x
}

function precipitationToRGBA(precipValue: number): [number, number, number, number] {
    const normalized = Math.min(precipValue, 250) / 250
    const x = saturate(normalized)
    const r = saturate(dot4(kRedVec4, x) + dot2(kRedVec2, x))
    const g = saturate(dot4(kGreenVec4, x) + dot2(kGreenVec2, x))
    const b = saturate(dot4(kBlueVec4, x) + dot2(kBlueVec2, x))
    const alpha = Math.max(
        Math.min(normalized * 10, 0.8) * 255,
        precipValue ? 50 : 0
    )
    return [
        Math.round(r * 255),
        Math.round(g * 255),
        Math.round(b * 255),
        Math.round(alpha),
    ]
}

// ── DWD grid constants ──────────────────────────────────────────────
const GRID_WIDTH = 1100
const GRID_HEIGHT = 1200

// ── Core processing functions ───────────────────────────────────────

function decompressRadarData(base64Data: string): Uint16Array {
    const binaryString = atob(base64Data)
    const len = binaryString.length
    const compressed = new Uint8Array(len)
    for (let i = 0; i < len; i++) {
        compressed[i] = binaryString.charCodeAt(i)
    }
    const decompressed = pako.inflate(compressed)
    return new Uint16Array(decompressed.buffer)
}

function createRadarImageUrl(precipitationData: Uint16Array): string {
    // Try OffscreenCanvas first (better perf in workers), fall back to regular
    if (typeof OffscreenCanvas !== 'undefined') {
        const canvas = new OffscreenCanvas(GRID_WIDTH, GRID_HEIGHT)
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

        // OffscreenCanvas doesn't have toDataURL; we build a BMP-style data URI
        // by reading the pixel buffer directly.
        // Actually, let's use a simpler approach: transfer ImageData back to main
        // thread and let it render. But that defeats the purpose.
        // Instead we'll convert to blob synchronously isn't possible, so we fall
        // back to building the data URL from raw pixel data.

        // Fallback: build a minimal BMP data URL
        return buildBmpDataUrl(imageData.data, GRID_WIDTH, GRID_HEIGHT)
    }

    // Fallback for environments without OffscreenCanvas (shouldn't happen in modern browsers)
    return buildBmpDataUrl(
        renderToPixelArray(precipitationData),
        GRID_WIDTH,
        GRID_HEIGHT
    )
}

function renderToPixelArray(precipitationData: Uint16Array): Uint8ClampedArray {
    const pixels = new Uint8ClampedArray(GRID_WIDTH * GRID_HEIGHT * 4)
    for (let i = 0; i < precipitationData.length; i++) {
        const [r, g, b, a] = precipitationToRGBA(precipitationData[i])
        pixels[i * 4] = r
        pixels[i * 4 + 1] = g
        pixels[i * 4 + 2] = b
        pixels[i * 4 + 3] = a
    }
    return pixels
}

/**
 * Build a BMP data URL from raw RGBA pixel data.
 * This avoids needing canvas.toDataURL() which isn't available in workers.
 */
function buildBmpDataUrl(pixels: Uint8ClampedArray | Uint8Array, width: number, height: number): string {
    const rowSize = width * 4
    const pixelDataSize = rowSize * height
    const headerSize = 54
    const fileSize = headerSize + pixelDataSize

    const buffer = new ArrayBuffer(fileSize)
    const view = new DataView(buffer)

    // BMP file header
    view.setUint8(0, 0x42) // 'B'
    view.setUint8(1, 0x4D) // 'M'
    view.setUint32(2, fileSize, true)
    view.setUint32(10, headerSize, true)

    // DIB header (BITMAPINFOHEADER)
    view.setUint32(14, 40, true) // header size
    view.setInt32(18, width, true)
    view.setInt32(22, -height, true) // negative = top-down
    view.setUint16(26, 1, true) // color planes
    view.setUint16(28, 32, true) // bits per pixel (BGRA)
    view.setUint32(30, 0, true) // no compression
    view.setUint32(34, pixelDataSize, true)

    // Write pixel data (convert RGBA to BGRA for BMP)
    const data = new Uint8Array(buffer)
    for (let i = 0; i < width * height; i++) {
        const srcOffset = i * 4
        const dstOffset = headerSize + i * 4
        data[dstOffset] = pixels[srcOffset + 2]     // B
        data[dstOffset + 1] = pixels[srcOffset + 1] // G
        data[dstOffset + 2] = pixels[srcOffset]     // R
        data[dstOffset + 3] = pixels[srcOffset + 3] // A
    }

    // Convert to base64 data URL
    let binary = ''
    const bytes = new Uint8Array(buffer)
    for (let i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i])
    }
    return 'data:image/bmp;base64,' + btoa(binary)
}

// ── Message handler ─────────────────────────────────────────────────

interface RadarRecord {
    timestamp: string
    precipitation_5: string
}

self.onmessage = (event: MessageEvent<{ records: RadarRecord[] }>) => {
    const { records } = event.data
    const totalFrames = records.length

    for (let i = 0; i < records.length; i++) {
        const record = records[i]
        const data = decompressRadarData(record.precipitation_5)
        const imageUrl = createRadarImageUrl(data)
        const label = record.timestamp.substring(11, 16)

        self.postMessage({
            type: 'progress',
            frameIndex: i,
            totalFrames,
            frame: {
                label,
                timestamp: record.timestamp,
                imageUrl,
            },
        })
    }

    self.postMessage({ type: 'complete' })
}
