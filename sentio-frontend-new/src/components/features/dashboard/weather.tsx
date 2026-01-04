import { Header } from '@/components/layout/dashboard/header'
import { Main } from '@/components/layout/dashboard/main'
import { Cloud, Thermometer, CloudRain, Cpu, Activity } from 'lucide-react'
import { useEffect, useState } from 'react'
import { weatherApi, forecastApi, alertsApi, devicesApi, historicalApi } from '@/lib/api'
import type { RaspiWeather, WeatherForecast, WeatherAlert, Device, HistoricalComparison as HistoricalComparisonType } from '@/types/api'
import { CurrentConditions } from './components/current-conditions'
import { AlertsBanner } from './components/alerts-banner'
import { WeatherRadar } from './components/weather-radar'
import { SensorChart } from './components/sensor-chart'
import { AIWeatherAnalysis } from './components/ai-weather-analysis'
import { HistoricalComparison } from './components/historical-comparison'
import { DeviceComparison } from './components/device-comparison'
import { SmartInsightCard } from './components/smart-insight-card'
import { useDevices } from '@/context/device-context'

export default function WeatherPage() {
    const { setFocusLocation, selectedDevice, devices } = useDevices()
    const [loading, setLoading] = useState(true)
    const [sensorData, setSensorData] = useState<RaspiWeather | null>(null)
    const [sensorReadings, setSensorReadings] = useState<RaspiWeather[]>([])
    const [currentForecast, setCurrentForecast] = useState<WeatherForecast | null>(null)
    const [alerts, setAlerts] = useState<WeatherAlert[]>([])
    const [comparison, setComparison] = useState<HistoricalComparisonType | null>(null)

    // Refetch data when selected device changes
    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true)

                // Determine which device to show data for:
                // - If a device is explicitly selected: use that device
                // - If unified view (null): use primary device, or first device as fallback
                let effectiveDeviceId: string | undefined = selectedDevice?.id

                if (!effectiveDeviceId && devices.length > 0) {
                    // Find primary device, or fall back to first device
                    const primaryDevice = devices.find(d => d.isPrimary) || devices[0]
                    effectiveDeviceId = primaryDevice?.id
                }

                const [
                    latestRes,
                    allRecentRes, // All devices for SensorChart and DeviceComparison
                    forecastRes,
                    alertsRes,
                    comparisonRes,
                ] = await Promise.allSettled([
                    // Use effective device for main display (primary in unified view)
                    effectiveDeviceId ? devicesApi.getSensorLatest(effectiveDeviceId) : weatherApi.latest(),
                    // Always fetch all readings for SensorChart and DeviceComparison
                    weatherApi.recent(),
                    effectiveDeviceId ? devicesApi.getForecasts(effectiveDeviceId) : forecastApi.currentLocation(),
                    effectiveDeviceId ? devicesApi.getAlerts(effectiveDeviceId) : alertsApi.currentLocation(),
                    historicalApi.comparison(effectiveDeviceId || undefined),
                ])

                if (latestRes.status === 'fulfilled') setSensorData(latestRes.value.data)
                // Use all readings for chart/comparison, not just selected device
                if (allRecentRes.status === 'fulfilled') {
                    const data = allRecentRes.value.data
                    setSensorReadings(Array.isArray(data) ? data : [])
                }
                if (forecastRes.status === 'fulfilled') {
                    const data = forecastRes.value.data
                    const forecastArray = Array.isArray(data) ? data : []
                    if (forecastArray.length > 0) setCurrentForecast(forecastArray[0])
                }
                if (alertsRes.status === 'fulfilled') {
                    const data = alertsRes.value.data
                    setAlerts(Array.isArray(data) ? data : [])
                }
                if (comparisonRes.status === 'fulfilled') setComparison(comparisonRes.value.data)
            } catch (err) {
                console.error('Failed to load weather data:', err)
            } finally {
                setLoading(false)
            }
        }
        fetchData()
    }, [selectedDevice, devices]) // Refetch when device changes or devices list updates

    // Handle alert click - zoom to alert location on radar
    const handleAlertClick = (alert: WeatherAlert) => {
        if (alert.latitude && alert.longitude) {
            setFocusLocation({
                latitude: alert.latitude,
                longitude: alert.longitude,
                label: alert.city || alert.localizedEvent || 'Alert Location'
            })
        }
    }

    const currentTemp = sensorData?.temperature ?? currentForecast?.temperature ?? null
    const forecastTemp = currentForecast?.temperature ?? null

    // Calculate sensor delta (difference between sensor and forecast)
    const sensorDelta = (sensorData?.temperature != null && forecastTemp != null)
        ? sensorData.temperature - forecastTemp
        : null

    // Calculate high/low from forecast
    const todayHigh = currentForecast?.temperatureMax ?? currentForecast?.temperature
    const todayLow = currentForecast?.temperatureMin ?? (todayHigh ? todayHigh - 5 : null)

    // Rain probability
    const rainProbability = currentForecast?.precipitationProbability ?? null

    // Helper to check if device is online (seen within last 5 minutes)
    const isDeviceOnline = (d: Device) => {
        if (!d.lastSeen) return false;
        // Backend sends LocalDateTime which implies UTC but lacks offset
        // We append 'Z' to ensure browser treats it as UTC
        const lastSeenTime = d.lastSeen.endsWith('Z') ? d.lastSeen : `${d.lastSeen}Z`;
        const lastSeen = new Date(lastSeenTime).getTime();
        const now = Date.now();
        return (now - lastSeen) < 5 * 60 * 1000; // 5 minutes
    };

    // Active devices count
    const activeDevices = devices.filter(isDeviceOnline).length
    const totalDevices = devices.length

    return (
        <>
            <Header>
                <div className="flex items-center gap-2">
                    <Cloud className="h-5 w-5" />
                    <h1 className="text-lg font-semibold">Weather</h1>
                </div>
            </Header>

            <Main>
                {/* Smart Insight Cards */}
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-6">
                    <SmartInsightCard
                        icon={<Thermometer className="h-4 w-4" />}
                        value={`${todayHigh?.toFixed(0) ?? '--'}° / ${todayLow?.toFixed(0) ?? '--'}°`}
                        label="Today's High/Low"
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground">
                                <div className="flex justify-between">
                                    <span>Morning</span>
                                    <span>{todayLow?.toFixed(0) ?? '--'}°</span>
                                </div>
                                <div className="flex justify-between">
                                    <span>Afternoon</span>
                                    <span>{todayHigh?.toFixed(0) ?? '--'}°</span>
                                </div>
                                <div className="flex justify-between">
                                    <span>Evening</span>
                                    <span>{((todayHigh ?? 0) - 3).toFixed(0)}°</span>
                                </div>
                            </div>
                        }
                    />
                    <SmartInsightCard
                        icon={<CloudRain className="h-4 w-4" />}
                        value={`${rainProbability ?? '--'}%`}
                        label="Rain Chance"
                        variant={rainProbability != null && rainProbability > 50 ? "warning" : "default"}
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground">
                                {rainProbability != null ? (
                                    rainProbability > 70 ? "Bring an umbrella!" :
                                        rainProbability > 40 ? "Possible light showers" :
                                            "Looking dry today"
                                ) : "No forecast data"}
                            </div>
                        }
                    />
                    <SmartInsightCard
                        icon={<Cpu className="h-4 w-4" />}
                        value={sensorDelta != null ? `${sensorDelta > 0 ? '+' : ''}${sensorDelta.toFixed(1)}°` : '--'}
                        label="Sensor vs Forecast"
                        trend={sensorDelta != null ? (sensorDelta > 0.5 ? "up" : sensorDelta < -0.5 ? "down" : "neutral") : undefined}
                        trendValue={sensorDelta != null ? (Math.abs(sensorDelta) < 1 ? "accurate" : "deviation") : undefined}
                        variant={sensorDelta != null && Math.abs(sensorDelta) > 3 ? "warning" : "default"}
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground space-y-1">
                                <div className="flex justify-between">
                                    <span>Sensor</span>
                                    <span>{sensorData?.temperature?.toFixed(1) ?? '--'}°</span>
                                </div>
                                <div className="flex justify-between">
                                    <span>Forecast</span>
                                    <span>{forecastTemp?.toFixed(1) ?? '--'}°</span>
                                </div>
                            </div>
                        }
                    />
                    <SmartInsightCard
                        icon={<Activity className="h-4 w-4" />}
                        value={`${activeDevices}/${totalDevices}`}
                        label="Active Sensors"
                        variant={activeDevices === 0 && totalDevices > 0 ? "danger" : activeDevices < totalDevices ? "warning" : "success"}
                        loading={loading}
                        expandedContent={
                            <div className="text-xs text-muted-foreground">
                                {devices.length === 0 ? (
                                    "No devices registered"
                                ) : (
                                    <div className="space-y-1">
                                        {devices.slice(0, 3).map(d => (
                                            <div key={d.id} className="flex justify-between items-center">
                                                <span className="truncate">{d.name}</span>
                                                <span className={isDeviceOnline(d) ? "text-green-600" : "text-red-600"}>
                                                    {isDeviceOnline(d) ? "●" : "○"}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        }
                    />
                </div>

                {/* Bento Grid: Weather Widget + Flippable Forecast (left) | Radar (right) */}
                <div className="grid grid-cols-1 lg:grid-cols-7 gap-4 mb-6">
                    {/* Left column: Weather Widget + Alerts */}
                    <div className="col-span-1 lg:col-span-3 flex flex-col gap-4">
                        <CurrentConditions
                            sensorData={sensorData}
                            forecastData={currentForecast}
                            loading={loading}
                            lastUpdated={sensorData?.timestamp}
                        />
                        <AlertsBanner
                            alerts={alerts}
                            loading={loading}
                            onAlertClick={handleAlertClick}
                        />
                    </div>

                    {/* Right column: Radar */}
                    <div className="col-span-1 lg:col-span-4">
                        <WeatherRadar loading={loading} />
                    </div>
                </div>

                {/* AI Analysis + Sensor Chart - equal columns */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-6">
                    <AIWeatherAnalysis loading={loading} />
                    <SensorChart
                        devices={devices}
                        readings={sensorReadings}
                        loading={loading}
                    />
                </div>

                {/* Device Comparison + Historical Comparison - equal columns */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                    <DeviceComparison
                        devices={devices}
                        readings={sensorReadings}
                        loading={loading}
                    />
                    <HistoricalComparison
                        comparison={comparison}
                        currentTemp={currentTemp}
                        loading={loading}
                    />
                </div>
            </Main>
        </>
    )
}
