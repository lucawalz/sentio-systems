import { useEffect, useRef } from "react"
import { gsap } from "gsap"
import { Button } from "../ui/button"
import { ArrowLeft, Settings, Bell, Wifi, Battery, Sun, Moon, Zap } from "lucide-react"
import { useTheme } from "../theme-context"
import { Link } from "react-router-dom"
import { useWeatherData } from "../../hooks/useWeatherData"

export function DashboardHeader() {
  const headerRef = useRef<HTMLElement>(null)
  const statusRef = useRef<HTMLDivElement>(null)
  const { theme, themeMode, toggleThemeMode, isLoading } = useTheme()
  const { latestWeather } = useWeatherData(30000)

  useEffect(() => {
    // Only run animations when component is fully loaded and elements exist
    if (isLoading) return

    // Header slide-in animation - check if element exists
    if (headerRef.current) {
      gsap.fromTo(
          headerRef.current,
          { y: -100, opacity: 0 },
          { y: 0, opacity: 1, duration: 1, ease: "power3.out", delay: 0.2 },
      )
    }

    // Status indicators animation - check if element exists and has children
    if (statusRef.current?.children && statusRef.current.children.length > 0) {
      gsap.fromTo(
          Array.from(statusRef.current.children),
          { scale: 0, opacity: 0 },
          {
            scale: 1,
            opacity: 1,
            duration: 0.6,
            ease: "back.out(1.7)",
            stagger: 0.1,
            delay: 0.8,
          },
      )
    }

    // Pulsing WiFi indicator - use a slight delay to ensure DOM is ready
    const wifiTimer = setTimeout(() => {
      const wifiIndicator = document.querySelector(".wifi-indicator")
      if (wifiIndicator) {
        gsap.to(wifiIndicator, {
          scale: 1.2,
          duration: 2,
          ease: "power2.inOut",
          yoyo: true,
          repeat: -1,
        })
      }
    }, 100)

    return () => {
      clearTimeout(wifiTimer)
    }
  }, [isLoading]) // Run when isLoading changes

  // Don't render until theme is loaded
  if (isLoading) {
    return null
  }

  const getThemeIcon = () => {
    switch (themeMode) {
      case "daylight":
        return <Sun className="w-4 h-4 mr-2" />
      case "twilight":
        return <Moon className="w-4 h-4 mr-2" />
      case "auto":
        return <Zap className="w-4 h-4 mr-2" />
      default:
        return <Sun className="w-4 h-4 mr-2" />
    }
  }

  const getThemeLabel = () => {
    switch (themeMode) {
      case "daylight":
        return "Daylight"
      case "twilight":
        return "Twilight"
      case "auto":
        return "Auto"
      default:
        return "Daylight"
    }
  }

  const getAutoStatus = () => {
    if (themeMode === "auto" && latestWeather) {
      const luxValue = latestWeather.lux
      const autoTheme = luxValue < 500 ? "twilight" : "daylight"
      return ` (${Math.round(luxValue)} lux → ${autoTheme})`
    }
    return ""
  }

  return (
      <header ref={headerRef} className="bg-card/90 backdrop-blur-sm border-b border-border sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-6 flex items-center justify-between">
          <div className="flex items-center space-x-6">
            <Link to="/">
              <Button
                  variant="ghost"
                  size="sm"
                  className="text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-all duration-300"
              >
                <ArrowLeft className="w-4 h-4 mr-2" />
                Back to Landing
              </Button>
            </Link>

            <div className="text-2xl font-bold">
              <span className="text-primary">
                Sentio
              </span>
              <span className="text-sm font-normal text-muted-foreground ml-2">
                Dashboard{getAutoStatus()}
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-4">
            <div ref={statusRef} className="hidden md:flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <div className="wifi-indicator w-3 h-3 rounded-full animate-pulse bg-primary" />
                <Wifi className="w-4 h-4 text-primary" />
              </div>
              <div className="flex items-center space-x-2">
                <Battery className="w-4 h-4 text-success" />
                <span className="text-sm text-muted-foreground">85%</span>
              </div>
            </div>

            <Button
                variant="ghost"
                size="sm"
                onClick={toggleThemeMode}
                className={`text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-all duration-300 ${
                    themeMode === "auto" ? "bg-accent/30" : ""
                }`}
                title={themeMode === "auto"
                    ? `Auto mode: ${latestWeather ? Math.round(latestWeather.lux) + " lux" : "Loading..."}`
                    : `Current: ${getThemeLabel()}`
                }
            >
              {getThemeIcon()}
              <span className="flex items-center gap-1">
                {getThemeLabel()}
                {themeMode === "auto" && (
                    <span className="text-xs opacity-60">
                      ({theme === "daylight" ? "☀️" : "🌙"})
                    </span>
                )}
              </span>
            </Button>

            <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-all duration-300"
            >
              <Bell className="w-4 h-4" />
            </Button>

            <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground hover:text-foreground hover:bg-accent/60 transition-all duration-300"
            >
              <Settings className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </header>
  )
}