
import { useEffect, useRef, useState } from "react"
import { gsap } from "gsap"
import { XAxis, YAxis, ResponsiveContainer, Area, AreaChart, Tooltip } from "recharts"
import { ChevronDown, Thermometer, Droplets, Gauge, Sun, Zap, Database, WifiOff, RefreshCw, BarChart3 } from "lucide-react"
import { useWeatherData } from "../../hooks/useWeatherData"
import { format, isAfter, subHours } from "date-fns"

type SensorType = 'temperature' | 'humidity' | 'pressure' | 'lux' | 'uvi'

interface SensorConfig {
    key: SensorType
    label: string
    icon: React.ComponentType<{ className?: string; style?: React.CSSProperties }>
    color: string
    unit: string
    gradientId: string
    domain?: [number, number] | ((dataMin: number, dataMax: number) => [number, number])
}

const sensorConfigs: SensorConfig[] = [
    {
        key: 'temperature',
        label: 'Temperature',
        icon: Thermometer,
        color: '#B0D6FF',
        unit: '°C',
        gradientId: 'tempGradient',
        domain: (dataMin: number, dataMax: number) => {
            const range = dataMax - dataMin
            const padding = Math.max(range * 0.3, 5) // At least 5 degrees padding
            return [dataMin - padding, dataMax + padding]
        }
    },
    {
        key: 'humidity',
        label: 'Humidity',
        icon: Droplets,
        color: '#A8D5BA',
        unit: '%',
        gradientId: 'humidityGradient',
        domain: (dataMin: number, dataMax: number) => {
            const range = dataMax - dataMin
            const padding = Math.max(range * 0.3, 10) // At least 10% padding
            return [Math.max(0, dataMin - padding), Math.min(100, dataMax + padding)]
        }
    },
    {
        key: 'pressure',
        label: 'Pressure',
        icon: Gauge,
        color: '#FFD8A8',
        unit: 'hPa',
        gradientId: 'pressureGradient',
        domain: (dataMin: number, dataMax: number) => {
            const range = dataMax - dataMin
            const padding = Math.max(range * 0.3, 20) // At least 20 hPa padding
            return [dataMin - padding, dataMax + padding]
        }
    },
    {
        key: 'lux',
        label: 'Light Level',
        icon: Sun,
        color: '#FFF176',
        unit: 'lux',
        gradientId: 'luxGradient'
        // No domain restriction for lux as it can vary widely
    },
    {
        key: 'uvi',
        label: 'UV Index',
        icon: Zap,
        color: '#CE93D8',
        unit: '',
        gradientId: 'uviGradient',
        domain: [0, 12] // UV index typically ranges from 0-12
    }
]

// Custom tooltip component props
interface CustomTooltipProps {
    active?: boolean
    payload?: Array<{
        value: number
        payload: {
            time: string
            value: number
            originalValue?: number
        }
    }>
    label?: string
}

export function EnhancedTemperatureChart() {
    const cardRef = useRef<HTMLDivElement>(null)
    const dropdownRef = useRef<HTMLDivElement>(null)
    const [timeRange, setTimeRange] = useState<'24h' | 'all'>('24h')
    const [selectedSensor, setSelectedSensor] = useState<SensorType>('temperature')
    const [isDropdownOpen, setIsDropdownOpen] = useState(false)
    const { recentWeather, allWeather, loading, error, isEmpty, refetch } = useWeatherData(30000)

    const currentSensorConfig = sensorConfigs.find(config => config.key === selectedSensor) || sensorConfigs[0]

    useEffect(() => {
        // Check if cardRef.current exists before animating
        if (cardRef.current) {
            gsap.fromTo(
                cardRef.current,
                { y: 30, opacity: 0, scale: 0.95 },
                { y: 0, opacity: 1, scale: 1, duration: 1.2, ease: "power3.out", delay: 0.1 },
            )
        }
    }, [])

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsDropdownOpen(false)
            }
        }

        if (isDropdownOpen) {
            document.addEventListener('mousedown', handleClickOutside)
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside)
        }
    }, [isDropdownOpen])

    // Filter and transform the weather data based on selected time range
    const getChartData = () => {
        // Use the correct data source based on time range
        let sourceData = timeRange === 'all' ? allWeather : recentWeather;
        let filteredData = sourceData;

        // Debug: Check what data we have
        console.log('Total source data:', sourceData.length)
        console.log('Selected sensor:', selectedSensor)
        console.log('Time range:', timeRange)

        if (timeRange === '24h') {
            const twentyFourHoursAgo = subHours(new Date(), 24)
            filteredData = sourceData.filter(data =>
                isAfter(new Date(data.timestamp), twentyFourHoursAgo)
            )
            console.log('Filtered data (24h):', filteredData.length)
        }

        // Apply data limits: minimum 1, maximum 100 datapoints
        const limitedData = filteredData.length > 100
            ? filteredData.slice(-100) // Take the most recent 100 datapoints
            : filteredData

        console.log('Limited data:', limitedData.length)

        const chartData = limitedData.map(data => {
            const sensorValue = data[selectedSensor]
            console.log(`Sensor ${selectedSensor} value:`, sensorValue)

            return {
                time: timeRange === '24h'
                    ? format(new Date(data.timestamp), 'HH:mm')
                    : format(new Date(data.timestamp), 'MMM dd'),
                value: selectedSensor === 'uvi'
                    ? Math.round(sensorValue * 10) / 10
                    : Math.round(sensorValue),
                originalValue: sensorValue // Store the original unrounded value
            }
        })

        console.log('Final chart data:', chartData.length)
        return chartData
    }

    const chartData = getChartData()

    // Calculate Y-axis domain based on sensor type and data
    const getYAxisDomain = () => {
        if (!chartData.length) return undefined

        const values = chartData.map(d => d.value)
        const dataMin = Math.min(...values)
        const dataMax = Math.max(...values)

        if (currentSensorConfig.domain) {
            if (typeof currentSensorConfig.domain === 'function') {
                return currentSensorConfig.domain(dataMin, dataMax)
            }
            return currentSensorConfig.domain
        }

        return undefined // Let Recharts auto-scale for sensors without domain
    }

    const handleSensorSelect = (sensorKey: SensorType) => {
        setSelectedSensor(sensorKey)
        setIsDropdownOpen(false)
    }

    const handleTimeRangeToggle = () => {
        setTimeRange(timeRange === '24h' ? 'all' : '24h')
    }

    const formatSensorValue = (value: number | null) => {
        if (value === null) return '--';
        return value.toFixed(2);
    }

    // Custom tooltip component that has access to selectedSensor
    const TooltipWithSensor = ({ active, payload, label }: CustomTooltipProps) => {
        if (active && payload && payload.length) {
            const originalValue = payload[0].payload.originalValue ?? payload[0].value;
            return (
                <div className="bg-background/95 border border-border/60 rounded-lg p-3 shadow-xl backdrop-blur-sm">
                    <p className="text-sm text-muted-foreground mb-1">{label}</p>
                    <p className="font-semibold flex items-center gap-2">
                        <currentSensorConfig.icon className="w-4 h-4" style={{ color: currentSensorConfig.color }} />
                        <span style={{ color: currentSensorConfig.color }}>
                {formatSensorValue(originalValue)}{currentSensorConfig.unit}
              </span>
                    </p>
                </div>
            )
        }
        return null
    }

    if (loading) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[481.25px] relative overflow-hidden shadow-xl">
                <div className="flex items-center justify-center h-full flex-col space-y-4">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <div className="text-sm text-muted-foreground">Loading sensor data...</div>
                </div>
            </div>
        )
    }

    if (isEmpty) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[481.25px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <Database className="w-16 h-16 text-muted-foreground mb-4" />
                    <div className="text-xl font-semibold mb-2 text-foreground">No Sensor Data Available</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">
                        No weather data has been collected yet. Please wait for the sensors to start sending data via MQTT.
                    </div>
                    <button
                        onClick={refetch}
                        className="px-6 py-3 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors font-medium flex items-center gap-2"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Check Again
                    </button>
                </div>
            </div>
        )
    }

    if (error && !isEmpty) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[481.25px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <WifiOff className="w-16 h-16 text-destructive mb-4" />
                    <div className="text-xl font-semibold mb-2 text-destructive">Connection Error</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">{error}</div>
                    <button
                        onClick={refetch}
                        className="px-6 py-3 bg-destructive text-destructive-foreground rounded-lg hover:bg-destructive/90 transition-colors font-medium flex items-center gap-2"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Retry
                    </button>
                </div>
            </div>
        )
    }

    if (chartData.length === 0) {
        return (
            <div className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[481.25px] relative overflow-hidden shadow-xl">
                <div className="flex flex-col items-center justify-center h-full text-center">
                    <currentSensorConfig.icon className="w-16 h-16 text-muted-foreground mb-4" />
                    <div className="text-xl font-semibold mb-2 text-foreground">Insufficient Data</div>
                    <div className="text-sm text-muted-foreground mb-6 max-w-md">
                        Not enough {currentSensorConfig.label.toLowerCase()} data available for the selected time range.
                    </div>
                    <button
                        onClick={handleTimeRangeToggle}
                        className="px-6 py-3 bg-accent text-accent-foreground rounded-lg hover:bg-accent/80 transition-colors font-medium flex items-center gap-2"
                    >
                        <RefreshCw className="w-4 h-4" />
                        Try {timeRange === '24h' ? 'All Time' : '24 Hours'}
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div
            ref={cardRef}
            className="dashboard-card bg-card/80 backdrop-blur-sm border border-border rounded-3xl p-6 h-[481.25px] relative overflow-hidden hover:shadow-lg transition-shadow duration-300"
        >
            {/* Background gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-chart-2/5 pointer-events-none rounded-3xl" />

            <div className="relative z-10 h-full flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between mb-8">
                    <div className="flex items-center space-x-3">
                        <BarChart3 className="w-6 h-6 text-primary" />
                        <h3 className="text-2xl font-bold text-foreground">Sensor Data</h3>
                    </div>
                    <div className="flex items-center space-x-2 text-xs text-muted-foreground">
                        <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                        <span>Real-time</span>
                    </div>
                </div>

                {/* Controls */}
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center space-x-3">
                        {/* Current Sensor Display */}
                        <div className="flex items-center space-x-2 px-4 py-2 bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50">
                            <currentSensorConfig.icon
                                className="w-5 h-5"
                                style={{ color: currentSensorConfig.color }}
                            />
                            <span className="text-sm font-medium text-foreground">
                  {currentSensorConfig.label}
                </span>
                        </div>

                        {/* Sensor Selector Dropdown */}
                        <div className="relative" ref={dropdownRef}>
                            <button
                                onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                                className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 px-4 py-2"
                            >
                                <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                                <div className="relative z-10 flex items-center space-x-2">
                                    <span className="text-sm font-medium">Change Sensor</span>
                                    <ChevronDown className={`w-4 h-4 transition-transform ${isDropdownOpen ? 'rotate-180' : ''}`} />
                                </div>
                            </button>

                            {isDropdownOpen && (
                                <div className="absolute top-full left-0 mt-2 w-52 bg-card/95 backdrop-blur-sm border border-border/60 rounded-2xl shadow-xl z-50 overflow-hidden">
                                    {sensorConfigs.map((config) => (
                                        <button
                                            key={config.key}
                                            onClick={() => handleSensorSelect(config.key)}
                                            className={`w-full group relative bg-transparent hover:bg-primary/5 transition-all duration-300 ${
                                                selectedSensor === config.key ? 'bg-primary/10' : ''
                                            }`}
                                        >
                                            <div className="flex items-center space-x-3 px-4 py-3 text-sm text-left">
                                                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-primary/20 to-primary/40 flex items-center justify-center shadow-sm">
                                                    <config.icon className="w-4 h-4 text-white" />
                                                </div>
                                                <span className="font-medium">{config.label}</span>
                                            </div>
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Time Range Toggle */}
                    <button
                        onClick={handleTimeRangeToggle}
                        className="group relative bg-card/60 backdrop-blur-sm rounded-2xl border border-border/50 hover:border-primary/30 transition-all duration-300 overflow-hidden hover:scale-105 px-4 py-2"
                    >
                        <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-primary/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                        <div className="relative z-10">
                <span className="text-sm font-medium">
                  {timeRange === '24h' ? '24 Hours' : 'All Time'}
                </span>
                        </div>
                    </button>
                </div>

                {/* Chart Container */}
                <div className="flex-1 min-h-0 bg-card/30 backdrop-blur-sm rounded-2xl border border-border/30 p-4">
                    <ResponsiveContainer width="100%" height="100%">
                        <AreaChart data={chartData} margin={{ top: 10, right: 10, left: 10, bottom: 10 }}>
                            <defs>
                                <linearGradient id={currentSensorConfig.gradientId} x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor={currentSensorConfig.color} stopOpacity={0.3} />
                                    <stop offset="95%" stopColor={currentSensorConfig.color} stopOpacity={0.05} />
                                </linearGradient>
                            </defs>
                            <XAxis
                                dataKey="time"
                                axisLine={false}
                                tickLine={false}
                                tick={{ fontSize: 12, fill: 'hsl(var(--muted-foreground))' }}
                                interval="preserveStartEnd"
                            />
                            <YAxis
                                axisLine={false}
                                tickLine={false}
                                tick={{ fontSize: 12, fill: 'hsl(var(--muted-foreground))' }}
                                domain={getYAxisDomain()}
                                tickFormatter={(value) => `${value}${currentSensorConfig.unit}`}
                            />
                            <Tooltip content={<TooltipWithSensor />} />
                            <Area
                                type="monotone"
                                dataKey="value"
                                stroke={currentSensorConfig.color}
                                strokeWidth={2}
                                fill={`url(#${currentSensorConfig.gradientId})`}
                                dot={false}
                                activeDot={{
                                    r: 4,
                                    fill: currentSensorConfig.color,
                                    stroke: 'hsl(var(--background))',
                                    strokeWidth: 2
                                }}
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>

                {/* Footer Info */}
                <div className="mt-6 text-center">
                    <div className="text-xs text-muted-foreground opacity-60">
                        Live sensor readings • MQTT updates every 5 minutes
                    </div>
                </div>
            </div>
        </div>
    )
}