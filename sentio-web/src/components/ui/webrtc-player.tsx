import { useEffect, useRef, useState, useCallback } from 'react'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Video, AlertCircle, RefreshCcw, Zap } from 'lucide-react'
import { devicesApi } from '@/lib/api'

interface WebRTCPlayerProps {
    deviceId: string
    className?: string
}

type StreamState = 'loading' | 'connecting' | 'playing' | 'offline' | 'error'

export function WebRTCPlayer({ deviceId, className }: WebRTCPlayerProps) {
    const videoRef = useRef<HTMLVideoElement>(null)
    const pcRef = useRef<RTCPeerConnection | null>(null)
    const [state, setState] = useState<StreamState>('loading')
    const [errorMessage, setErrorMessage] = useState<string>('')

    const connect = useCallback(async () => {
        setState('loading')
        setErrorMessage('')

        try {
            // Get stream URL and access token from backend
            const response = await devicesApi.getStreamUrl(deviceId)
            const { isStreaming, accessToken } = response.data

            if (!isStreaming) {
                setState('offline')
                return
            }

            if (!accessToken) {
                setErrorMessage('Not authenticated - please log in')
                setState('error')
                return
            }

            setState('connecting')

            // WHEP endpoint URL
            const whepUrl = `https://media.syslabs.dev/live/${deviceId}/whep?token=${encodeURIComponent(accessToken)}`

            // Cleanup previous connection
            if (pcRef.current) {
                pcRef.current.close()
                pcRef.current = null
            }

            // Create peer connection with ICE-TCP preference
            const pc = new RTCPeerConnection({
                iceServers: [], // No STUN needed for TCP-only
                iceTransportPolicy: 'all',
            })
            pcRef.current = pc

            // Handle incoming tracks
            pc.ontrack = (event) => {
                console.log('[WebRTC] Track received:', event.track.kind)
                if (videoRef.current && event.streams[0]) {
                    videoRef.current.srcObject = event.streams[0]
                    setState('playing')
                }
            }

            // Log ICE connection state
            pc.oniceconnectionstatechange = () => {
                console.log('[WebRTC] ICE state:', pc.iceConnectionState)
                if (pc.iceConnectionState === 'failed') {
                    setErrorMessage('Connection failed - network issue')
                    setState('error')
                }
            }

            // Add transceivers for receiving
            pc.addTransceiver('video', { direction: 'recvonly' })
            pc.addTransceiver('audio', { direction: 'recvonly' })

            // Create offer
            const offer = await pc.createOffer()
            await pc.setLocalDescription(offer)

            // Wait for ICE gathering to complete
            await new Promise<void>((resolve) => {
                if (pc.iceGatheringState === 'complete') {
                    resolve()
                } else {
                    const checkState = () => {
                        if (pc.iceGatheringState === 'complete') {
                            pc.removeEventListener('icegatheringstatechange', checkState)
                            resolve()
                        }
                    }
                    pc.addEventListener('icegatheringstatechange', checkState)
                    // Timeout after 5 seconds
                    setTimeout(resolve, 5000)
                }
            })

            // Send offer to WHEP endpoint
            const res = await fetch(whepUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/sdp' },
                body: pc.localDescription?.sdp,
            })

            if (res.status === 403) {
                setErrorMessage('Access denied - please try logging in again')
                setState('error')
                return
            }

            if (res.status === 404) {
                setState('offline')
                return
            }

            if (!res.ok) {
                throw new Error(`WHEP failed: ${res.status}`)
            }

            // Set remote answer
            const answerSdp = await res.text()
            await pc.setRemoteDescription({ type: 'answer', sdp: answerSdp })

            console.log('[WebRTC] Connection established')

        } catch (err) {
            console.error('[WebRTC] Connection failed:', err)
            setErrorMessage('Failed to connect')
            setState('error')
        }
    }, [deviceId])

    useEffect(() => {
        // Request stream start, then connect
        devicesApi.startStream(deviceId)
            .then(() => {
                console.log('[WebRTC] Stream start requested')
                setTimeout(connect, 1500) // Wait for stream to initialize
            })
            .catch(() => connect())

        // Handle page unload
        const handleBeforeUnload = () => {
            const url = `/api/stream/${deviceId}/stop`
            navigator.sendBeacon(url)
        }
        window.addEventListener('beforeunload', handleBeforeUnload)

        // Cleanup on unmount
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload)
            if (pcRef.current) {
                pcRef.current.close()
                pcRef.current = null
            }
            devicesApi.stopStream(deviceId).catch(() => { })
        }
    }, [deviceId, connect])

    return (
        <div className={`relative aspect-video bg-black rounded-lg overflow-hidden ${className}`}>
            <video
                ref={videoRef}
                className={`w-full h-full object-contain ${state === 'playing' ? '' : 'hidden'}`}
                autoPlay
                playsInline
                muted
                controls
            />

            {/* Low latency indicator */}
            {state === 'playing' && (
                <div className="absolute top-2 right-2 flex items-center gap-1 bg-green-500/80 text-white text-xs px-2 py-1 rounded">
                    <Zap className="h-3 w-3" />
                    Low Latency
                </div>
            )}

            {state === 'loading' && (
                <div className="absolute inset-0 flex items-center justify-center bg-muted">
                    <Skeleton className="w-full h-full rounded-lg" />
                </div>
            )}

            {state === 'connecting' && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/50">
                    <div className="text-white text-sm">Connecting...</div>
                </div>
            )}

            {state === 'offline' && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-muted">
                    <Video className="h-12 w-12 text-muted-foreground mb-4" />
                    <p className="text-muted-foreground mb-2">Device is not streaming</p>
                    <p className="text-xs text-muted-foreground mb-4">Start the camera on your device to begin streaming</p>
                    <Button variant="outline" size="sm" onClick={connect}>
                        <RefreshCcw className="h-4 w-4 mr-2" />
                        Check Again
                    </Button>
                </div>
            )}

            {state === 'error' && (
                <div className="absolute inset-0 flex flex-col items-center justify-center bg-muted">
                    <AlertCircle className="h-12 w-12 text-destructive mb-4" />
                    <p className="text-muted-foreground mb-2">{errorMessage || 'Stream unavailable'}</p>
                    <Button variant="outline" size="sm" onClick={connect}>
                        <RefreshCcw className="h-4 w-4 mr-2" />
                        Retry
                    </Button>
                </div>
            )}
        </div>
    )
}
