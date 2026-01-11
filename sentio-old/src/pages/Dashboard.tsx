import { useEffect } from "react"
import { gsap } from "gsap"
import { ThemeProvider } from "../components/theme-context"
import { EnhancedWeatherStation } from "../components/dashboard/weather-station"
import { EnhancedTemperatureChart } from "../components/dashboard/temperature-chart"
import { MicroCards } from "../components/dashboard/micro-cards"
import { FiveDayForecast } from "../components/dashboard/five-day-forecast"
import { AnimalLivestream } from "../components/dashboard/animal-livestream"
import { RecentAnimalSights } from "../components/dashboard/recent-animal-sights"
import { AnimalInformation } from "../components/dashboard/animal-information"
import { AnimalSightingsChart } from "../components/dashboard/animal-sightings-chart"
import { DashboardFooter } from "../components/layout/dashboard-footer"
import { DashboardHeader } from "../components/layout/dashboard-header"
import { EnhancedAISummary } from "../components/dashboard/ai-summary.tsx";
import { WeatherAlerts } from "../components/dashboard/weather-alerts";
import { WeatherRadar } from "../components/dashboard/weather-radar";

function DashboardContent() {
    useEffect(() => {
        // Enhanced stagger animation for dashboard cards
        const cards = gsap.utils.toArray(".dashboard-card")

        gsap.fromTo(
            cards,
            { y: 50, opacity: 0, scale: 0.95, rotateX: 15 },
            {
                y: 0,
                opacity: 1,
                scale: 1,
                rotateX: 0,
                duration: 1.2,
                ease: "power3.out",
                stagger: {
                    amount: 0.8,
                    grid: "auto",
                    from: "start",
                },
                delay: 0.3,
            },
        )

        // Ambient glow animation
        gsap.to(".glow-effect", {
            opacity: 0.6,
            scale: 1.1,
            duration: 3,
            ease: "power2.inOut",
            yoyo: true,
            repeat: -1,
            stagger: 0.5,
        })
    }, [])

    return (
        <div className="min-h-screen bg-background transition-colors duration-300 relative">
            {/* Enhanced ambient background with nature-inspired colors */}
            <div className="fixed inset-0 pointer-events-none overflow-hidden">
                <div className="glow-effect absolute top-1/4 left-1/4 w-96 h-96 rounded-full bg-primary/5 blur-3xl" />
                <div className="glow-effect absolute bottom-1/4 right-1/4 w-80 h-80 rounded-full bg-success/5 blur-3xl" />
                <div className="glow-effect absolute top-1/2 right-1/3 w-64 h-64 rounded-full bg-warning/5 blur-3xl" />
            </div>

            <DashboardHeader />

            <main className="relative max-w-7xl mx-auto p-4 md:p-6 space-y-6 md:space-y-8">
                {/* Top Row - Weather Station and Side Cards */}
                <div className="flex flex-col xl:flex-row gap-6">
                    <div className="flex-1">
                        <EnhancedWeatherStation />
                    </div>
                    <div className="flex-1 space-y-6">
                        <EnhancedTemperatureChart />
                    </div>
                </div>

                <div className="flex gap-6 items-stretch">
                    {/* AI Summary on the left - made narrower */}
                    <div className="w-[420px] xl:w-[480px]">
                        <EnhancedAISummary />
                    </div>

                    {/* Micro Cards in 3x3 grid on the right - now fills remaining space and matches height */}
                    <div className="flex-1">
                        <MicroCards />
                    </div>
                </div>

                {/* Five Day Forecast */}
                <div className="flex-1">
                    <FiveDayForecast />
                </div>

                {/* Weather Alerts and Radar Row */}
                <div className="flex flex-col lg:flex-row gap-6">
                    <div className="flex-1">
                        <WeatherAlerts />
                    </div>
                    <div className="flex-1">
                        <WeatherRadar />
                    </div>
                </div>

                {/* Middle Row - Animal Livestream and Recent Sights */}
                <div className="flex flex-col lg:flex-row gap-6">
                    <div className="flex-1 lg:flex-[2]">
                        <AnimalLivestream />
                    </div>
                    <div className="flex-1 lg:w-96 xl:w-[400px]">
                        <RecentAnimalSights />
                    </div>
                </div>

                {/* Animal Information Section */}
                <div className="flex-1">
                    <AnimalInformation />
                </div>

                {/* Animal Sightings Chart */}
                <div className="flex-1">
                    <AnimalSightingsChart />
                </div>
            </main>

            <DashboardFooter />
        </div>
    )
}

export default function Dashboard() {
    return (
        <ThemeProvider>
            <DashboardContent />
        </ThemeProvider>
    )
}