import type React from "react"
import { createContext, useContext, useEffect, useState } from "react"
import { useWeatherData } from "../hooks/useWeatherData"

type ThemeMode = "daylight" | "twilight" | "auto"
type Theme = "daylight" | "twilight"

interface ThemeContextType {
    theme: Theme
    themeMode: ThemeMode
    toggleThemeMode: () => void
    isLoading: boolean
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

// Lux threshold for auto mode (adjust based on your sensor and preferences)
const LUX_THRESHOLD = 500 // Below this value, switch to twilight mode

export function ThemeProvider({ children }: { children: React.ReactNode }) {
    const [themeMode, setThemeMode] = useState<ThemeMode>("daylight")
    const [theme, setTheme] = useState<Theme>("daylight")
    const [isLoading, setIsLoading] = useState(true)
    const { latestWeather } = useWeatherData(30000) // Poll every 30 seconds

    useEffect(() => {
        // Check for saved theme preference or default to daylight
        const savedThemeMode = localStorage.getItem("intellisky-theme-mode") as ThemeMode
        if (savedThemeMode) {
            setThemeMode(savedThemeMode)
        }
        setIsLoading(false)
    }, [])

    // Auto theme logic based on lux sensor
    useEffect(() => {
        if (themeMode === "auto" && latestWeather) {
            const autoTheme = latestWeather.lux < LUX_THRESHOLD ? "twilight" : "daylight"
            setTheme(autoTheme)
        } else if (themeMode !== "auto") {
            setTheme(themeMode as Theme)
        }
    }, [themeMode, latestWeather])

    useEffect(() => {
        // Apply theme to document
        document.documentElement.classList.remove("daylight", "twilight")
        document.documentElement.classList.add(theme)
        localStorage.setItem("intellisky-theme-mode", themeMode)
    }, [theme, themeMode])

    const toggleThemeMode = () => {
        setThemeMode((prev) => {
            switch (prev) {
                case "daylight":
                    return "twilight"
                case "twilight":
                    return "auto"
                case "auto":
                    return "daylight"
                default:
                    return "daylight"
            }
        })
    }

    return (
        <ThemeContext.Provider value={{ theme, themeMode, toggleThemeMode, isLoading }}>
            {children}
        </ThemeContext.Provider>
    )
}

export function useTheme() {
    const context = useContext(ThemeContext)
    if (context === undefined) {
        throw new Error("useTheme must be used within a ThemeProvider")
    }
    return context
}