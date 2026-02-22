import type React from "react"
import { createContext, useContext, useEffect, useState } from "react"

type Theme = "daylight" | "twilight"

interface ThemeContextType {
  theme: Theme
  toggleTheme: () => void
  isLoading: boolean
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<Theme>("daylight")
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    // Check for saved theme preference or default to daylight
    const savedTheme = localStorage.getItem("intellisky-theme") as Theme
    if (savedTheme) {
      setTheme(savedTheme)
    }
    setIsLoading(false)
  }, [])

  useEffect(() => {
    // Apply theme to document
    document.documentElement.classList.remove("daylight", "twilight")
    document.documentElement.classList.add(theme)
    localStorage.setItem("intellisky-theme", theme)
  }, [theme])

  const toggleTheme = () => {
    setTheme((prev) => (prev === "daylight" ? "twilight" : "daylight"))
  }

  return <ThemeContext.Provider value={{ theme, toggleTheme, isLoading }}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const context = useContext(ThemeContext)
  if (context === undefined) {
    throw new Error("useTheme must be used within a ThemeProvider")
  }
  return context
}