import { useState, useEffect, useRef, useMemo } from "react"
import { Volume2, Maximize, Camera, AlertCircle, Loader2, Signal } from "lucide-react"
import { Button } from "../ui/button"
import { useDevices } from "../../hooks/useDevices"

export function AnimalLivestream() {
  const [isLoading, setIsLoading] = useState(true)
  const [hasError, setHasError] = useState(false)
  const [isPlaying, setIsPlaying] = useState(false)
  const [aspectRatio, setAspectRatio] = useState(16 / 9)
  const imgRef = useRef<HTMLImageElement>(null)

  const { devices, loading: devicesLoading } = useDevices()

  const activeDevice = useMemo(() => {
    return devices.find(d => d.ipAddress)
  }, [devices])

  const streamUrl = activeDevice
    ? `http://${activeDevice.ipAddress}:8080/video_feed`
    : undefined

  const webInterface = activeDevice
    ? `http://${activeDevice.ipAddress}:8080`
    : undefined

  const handleImageLoad = () => {
    setIsLoading(false)
    setHasError(false)
    setIsPlaying(true)

    if (imgRef.current) {
      const { naturalWidth, naturalHeight } = imgRef.current
      if (naturalWidth && naturalHeight) {
        setAspectRatio(naturalWidth / naturalHeight)
      }
    }
  }

  const handleImageError = () => {
    setIsLoading(false)
    setHasError(true)
    setIsPlaying(false)
  }

  const openFullscreen = () => {
    if (webInterface) {
      window.open(webInterface, '_blank')
    }
  }

  useEffect(() => {
    if (streamUrl) {
      setIsLoading(true)
      setHasError(false)
      setIsPlaying(false)
      setAspectRatio(16 / 9)
    }
  }, [streamUrl])

  // Cleanup effect to prevent memory leaks
  useEffect(() => {
    return () => {
      if (imgRef.current) {
        imgRef.current.src = ''
        imgRef.current.removeAttribute('src')
      }
    }
  }, [])

  useEffect(() => {
    const currentImg = imgRef.current

    return () => {
      // Force garbage collection of previous image
      if (currentImg && currentImg.src !== streamUrl) {
        currentImg.src = ''
        currentImg.removeAttribute('src')
      }
    }
  }, [streamUrl])

  if (devicesLoading) {
    return (
      <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[400px] flex items-center justify-center">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 md:p-8 min-h-[400px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300">
      {/* Background gradient */}
      <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-green-500/5 pointer-events-none rounded-3xl" />

      <div className="relative z-10 h-full flex flex-col">
        {/* Header */}
        <div className="flex items-start justify-between mb-6 flex-shrink-0">
          <div className="flex items-center space-x-3">
            <Camera className="w-6 h-6 text-primary" />
            <h2 className="text-2xl md:text-3xl font-bold text-foreground">
              {activeDevice ? activeDevice.name : "Animal Livestream"}
            </h2>
          </div>
          <div className="flex items-center space-x-2">
            {isPlaying && (
              <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                <span>Live Stream</span>
              </div>
            )}
            <div className="flex items-center space-x-2">
              {isPlaying && (
                <>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-muted-foreground hover:text-foreground hover:bg-card/60"
                  >
                    <Volume2 className="w-4 h-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={openFullscreen}
                    className="text-muted-foreground hover:text-foreground hover:bg-card/60"
                  >
                    <Maximize className="w-4 h-4" />
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Stream Container */}
        <div className="flex-1 min-h-0">
          <div
            className="relative bg-secondary/20 rounded-2xl overflow-hidden h-full"
            style={{ aspectRatio }}
          >
            {(!activeDevice) && (
              <div className="absolute inset-0 flex items-center justify-center bg-card/60 backdrop-blur-sm">
                <div className="flex flex-col items-center space-y-4 text-center p-6">
                  <AlertCircle className="w-16 h-16 text-muted-foreground" />
                  <div>
                    <div className="text-xl font-semibold text-muted-foreground mb-2">No Active Device</div>
                    <div className="text-sm text-muted-foreground mb-1">Register a device or ensure it is online</div>
                  </div>
                </div>
              </div>
            )}

            {activeDevice && isLoading && (
              <div className="absolute inset-0 flex items-center justify-center bg-card/60 backdrop-blur-sm">
                <div className="flex flex-col items-center space-y-4">
                  <Loader2 className="w-12 h-12 animate-spin text-primary" />
                  <div className="text-center">
                    <div className="text-lg font-semibold text-foreground mb-1">Loading Stream</div>
                    <div className="text-sm text-muted-foreground">Connecting to {activeDevice.ipAddress}...</div>
                  </div>
                </div>
              </div>
            )}

            {activeDevice && hasError && (
              <div className="absolute inset-0 flex items-center justify-center bg-card/60 backdrop-blur-sm">
                <div className="flex flex-col items-center space-y-4 text-center p-6">
                  <AlertCircle className="w-16 h-16 text-destructive" />
                  <div>
                    <div className="text-xl font-semibold text-destructive mb-2">Stream Unavailable</div>
                    <div className="text-sm text-muted-foreground mb-1">Unable to connect to {activeDevice.ipAddress}</div>
                    <div className="text-xs text-muted-foreground opacity-60">Check camera connection and network</div>
                  </div>
                </div>
              </div>
            )}

            {streamUrl && (
              <img
                ref={imgRef}
                src={streamUrl}
                alt="Animal livestream"
                className="w-full h-full object-cover"
                onLoad={handleImageLoad}
                onError={handleImageError}
                style={{ display: hasError ? 'none' : 'block' }}
                loading="lazy"
                decoding="async"
              />
            )}

            <img
              ref={imgRef}
              src={streamUrl}
              alt="Animal livestream"
              className="w-full h-full object-cover"
              onLoad={handleImageLoad}
              onError={handleImageError}
              style={{ display: hasError ? 'none' : 'block' }}
              loading="lazy"
              decoding="async"
            />

            {isPlaying && (
              <div className="absolute bottom-4 left-4 flex items-center space-x-2">
                <div className="bg-black/70 backdrop-blur-sm rounded-full px-3 py-1.5 flex items-center space-x-2">
                  <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                  <span className="text-xs font-medium text-white">LIVE</span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Compact Stats Section */}
        {isPlaying && (
          <div className="mt-4 flex-shrink-0">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <div className="flex items-center space-x-1">
                <Signal className="w-3 h-3" />
                <span>1920×1080</span>
              </div>
              <div className="flex items-center space-x-1">
                <Camera className="w-3 h-3" />
                <span>2.5 Mbps</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}