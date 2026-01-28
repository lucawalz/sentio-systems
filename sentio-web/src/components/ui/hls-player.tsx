import { useEffect, useRef, useState } from 'react'
import Hls from 'hls.js'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Video, AlertCircle, RefreshCcw } from 'lucide-react'
import { devicesApi } from '@/lib/api'

interface HlsPlayerProps {
    deviceId: string
    className?: string
}

type StreamState = 'loading' | 'connecting' | 'playing' | 'offline' | 'error'

export function HlsPlayer({ deviceId, className }: HlsPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null)
    const hlsRef = useRef<Hls | null>(null)
    const [state, setState] = useState<StreamState>('loading')
    const [errorMessage, setErrorMessage] = useState<string>('')

    const loadStream = async () => {
        setState('loading')
        setErrorMessage('')

        try {
            // Fetch stream URL and access token from backend
            // The backend extracts the access token from the httpOnly cookie
            // and returns it for use in the stream URL
            const response = await devicesApi.getStreamUrl(deviceId)
            const { streamUrl, isStreaming, accessToken } = response.data

            if (!isStreaming) {
                setState('offline')
                return
            }

            if (!accessToken) {
                setErrorMessage('Not authenticated - please log in')
                setState('error')
                return
            }

            // Use base stream URL - token will be appended via xhrSetup
            console.log('[HLS] Loading stream:', streamUrl, 'isStreaming:', isStreaming)
            setState('connecting')

            const video = videoRef.current
            if (!video) {
                console.error('[HLS] No video element ref')
                return
            }

            // Cleanup previous HLS instance
            if (hlsRef.current) {
                hlsRef.current.destroy()
                hlsRef.current = null
            }

            console.log('[HLS] Hls.isSupported():', Hls.isSupported())

            if (Hls.isSupported()) {
                console.log('[HLS] Creating HLS.js instance with xhrSetup')
                const hls = new Hls({
                    enableWorker: true,
                    lowLatencyMode: true,
                    // Low-latency settings - start from live edge
                    liveSyncDurationCount: 1,        // Sync to 1 segment behind live (was 3)
                    liveMaxLatencyDurationCount: 3,  // Max 3 segments behind before catching up
                    liveDurationInfinity: true,      // Treat as infinite live stream
                    highBufferWatchdogPeriod: 1,     // Check buffer every 1s
                    // Minimal buffering
                    maxBufferLength: 2,              // Only buffer 2 seconds ahead
                    maxMaxBufferLength: 4,           // Hard limit at 4 seconds
                    backBufferLength: 2,             // Keep only 2 seconds of past video
                    // Append token to ALL XHR requests (playlists and segments)
                    xhrSetup: (xhr: XMLHttpRequest, url: string) => {
                        // Append token to URL if not already present
                        const separator = url.includes('?') ? '&' : '?'
                        const urlWithToken = `${url}${separator}token=${encodeURIComponent(accessToken)}`
                        xhr.open('GET', urlWithToken, true)
                    },
                })

                console.log('[HLS] Loading source:', streamUrl)
                hls.loadSource(streamUrl)
                hls.attachMedia(video)

                hls.on(Hls.Events.MANIFEST_PARSED, () => {
                    video.play().catch(() => {
                        // Autoplay blocked, user needs to click
                        setState('playing')
                    })
                    setState('playing')
                })

                hls.on(Hls.Events.ERROR, (_event, data) => {
                    if (data.fatal) {
                        switch (data.type) {
                            case Hls.ErrorTypes.NETWORK_ERROR:
                                if (data.response?.code === 403) {
                                    setErrorMessage('Access denied - please try logging in again')
                                } else {
                                    setErrorMessage('Network error - device may be offline')
                                }
                                setState('error')
                                break
                            case Hls.ErrorTypes.MEDIA_ERROR:
                                hls.recoverMediaError()
                                break
                            default:
                                setErrorMessage('Playback error')
                                setState('error')
                                break
                        }
                    }
                })

                hlsRef.current = hls
            } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                // Native HLS support (Safari) - append token directly to URL
                const authenticatedUrl = `${streamUrl}?token=${encodeURIComponent(accessToken)}`
                video.src = authenticatedUrl
                video.addEventListener('loadedmetadata', () => {
                    video.play().catch(() => { })
                    setState('playing')
                })
                video.addEventListener('error', () => {
                    setErrorMessage('Playback error')
                    setState('error')
                })
            } else {
                setErrorMessage('HLS not supported in this browser')
                setState('error')
            }
        } catch (err) {
            console.error('Failed to load stream:', err)
            setErrorMessage('Failed to get stream URL')
            setState('error')
        }
    }

    useEffect(() => {
        let sessionId: string | null = null
        let heartbeatInterval: ReturnType<typeof setInterval> | null = null
        let isTabVisible = true

        const sendHeartbeat = () => {
            if (sessionId && isTabVisible) {
                devicesApi.heartbeat(deviceId, sessionId)
                    .then(() => console.debug('[HLS] Heartbeat sent'))
                    .catch((err) => console.warn('[HLS] Heartbeat failed:', err))
            }
        }

        const startHeartbeat = () => {
            if (heartbeatInterval) clearInterval(heartbeatInterval)
            // Heartbeat every 45 seconds (TTL is 120s, so 2-3 missed is OK)
            heartbeatInterval = setInterval(sendHeartbeat, 45000)
        }

        // Handle tab visibility - pause heartbeat when hidden
        const handleVisibilityChange = () => {
            isTabVisible = !document.hidden
            if (isTabVisible) {
                // Tab visible again - send immediate heartbeat and restart interval
                console.debug('[HLS] Tab visible - resuming heartbeat')
                sendHeartbeat()
                startHeartbeat()
            } else {
                // Tab hidden - stop heartbeat to save resources
                // Session TTL (120s) is long enough to survive being hidden
                console.debug('[HLS] Tab hidden - pausing heartbeat')
                if (heartbeatInterval) {
                    clearInterval(heartbeatInterval)
                    heartbeatInterval = null
                }
            }
        }
        document.addEventListener('visibilitychange', handleVisibilityChange)

        // Start stream and get session ID
        devicesApi.startStream(deviceId)
            .then((response) => {
                sessionId = response.data.sessionId
                console.log('[HLS] Stream started, sessionId:', sessionId, 'viewers:', response.data.viewerCount)

                // Start heartbeat
                startHeartbeat()

                // Wait a bit for stream to be ready, then load
                setTimeout(() => {
                    loadStream()
                }, 2000)
            })
            .catch((err) => {
                console.warn('[HLS] Failed to request stream start:', err)
                loadStream() // Try to load anyway
            })

        // Handle page unload - send stop beacon with sessionId
        const handleBeforeUnload = () => {
            if (sessionId) {
                const url = `/api/stream/${deviceId}/stop?sessionId=${encodeURIComponent(sessionId)}`
                navigator.sendBeacon(url)
            }
        }
        window.addEventListener('beforeunload', handleBeforeUnload)

        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload)
            document.removeEventListener('visibilitychange', handleVisibilityChange)

            // Clear heartbeat interval
            if (heartbeatInterval) {
                clearInterval(heartbeatInterval)
            }

            // Destroy HLS instance
            if (hlsRef.current) {
                hlsRef.current.destroy()
                hlsRef.current = null
            }

            // Send stop request with sessionId
            if (sessionId) {
                devicesApi.stopStream(deviceId, sessionId)
                    .then((res) => console.log('[HLS] Stream stop requested, viewers remaining:', res.data?.viewerCount))
                    .catch((err) => console.warn('[HLS] Failed to request stream stop:', err))
            }
        }
    }, [deviceId])

    // Always render the video element so the ref is available
    // We just show different overlays based on state
    return (
        <div className={`relative aspect-video bg-black rounded-lg overflow-hidden ${className}`}>
            {/* Video element is always in DOM for ref availability */}
            <video
                ref={videoRef}
                className={`w-full h-full object-contain ${state === 'playing' ? '' : 'hidden'}`}
                playsInline
                muted
                controls
            />

            {state === 'loading' && (
                <div className="absolute inset-0 flex items-center justify-center bg-muted">
                    <Skeleton className="w-full h-full rounded-lg" />
                </div>
            )}

            {state === 'offline' && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-muted">
                    <Video className="h-12 w-12 text-muted-foreground mb-4" />
                    <p className="text-muted-foreground mb-2">Device is not streaming</p>
                    <p className="text-xs text-muted-foreground mb-4">Start the camera on your device to begin streaming</p>
                    <Button variant="outline" size="sm" onClick={loadStream}>
                        <RefreshCcw className="h-4 w-4 mr-2" />
                        Check Again
                    </Button>
                </div>
            )}

            {state === 'error' && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-muted">
                    <AlertCircle className="h-12 w-12 text-destructive mb-4" />
                    <p className="text-muted-foreground mb-2">{errorMessage || 'Stream unavailable'}</p>
                    <Button variant="outline" size="sm" onClick={loadStream}>
                        <RefreshCcw className="h-4 w-4 mr-2" />
                        Retry
                    </Button>
                </div>
            )}

            {state === 'connecting' && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/50">
                    <div className="text-white text-sm">Connecting...</div>
                </div>
            )}
        </div>
    )
}
