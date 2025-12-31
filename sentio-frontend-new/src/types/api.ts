// API Types for Sentio Backend

// ============ Auth Types ============
export interface User {
    username: string
    email: string
    roles: string[]
}

export interface LoginRequest {
    username: string
    password: string
}

export interface RegisterRequest {
    username: string
    password: string
    email: string
    firstName: string
    lastName: string
}

// ============ Device Types ============
export interface Device {
    id: string
    name: string
    ownerId: string
    activeServices: string[]
    ipAddress: string | null
    lastSeen: string | null
    createdAt: string
}

// ============ Weather Types ============
export interface RaspiWeather {
    id: number
    temperature: number
    humidity: number
    pressure: number
    timestamp: string
    deviceId: string
}

export interface WeatherStats {
    totalReadings: number
    latest: RaspiWeather | null
    avgTemperature: number
    avgHumidity: number
    avgPressure: number
}

export interface WeatherForecast {
    id: number
    forecastDate: string
    forecastDateTime: string
    // Temperature fields
    temperature: number | null
    apparentTemperature: number | null
    temperatureMax: number
    temperatureMin: number
    // Humidity & Pressure
    humidity: number | null
    pressure: number | null
    dewPoint: number | null
    // Wind data
    windSpeed: number | null
    windDirection: number | null
    windGusts: number | null
    // Precipitation
    precipitation: number | null
    precipitationProbability: number
    precipitationSum: number
    rain: number | null
    showers: number | null
    snowfall: number | null
    snowDepth: number | null
    // Conditions
    cloudCover: number | null
    visibility: number | null
    weatherCode: number
    description: string | null
    weatherMain: string | null
    icon: string | null
    // Location
    city: string
    country: string | null
    latitude: number
    longitude: number
    createdAt: string
    updatedAt: string
}

export interface HistoricalWeather {
    id: number
    weatherDate: string
    temperatureMax: number
    temperatureMin: number
    temperatureMean: number
    precipitationSum: number
    windSpeedMax: number
    city: string
    latitude: number
    longitude: number
    createdAt: string
    updatedAt: string
}

export interface HistoricalComparison {
    threeDaysAgo: HistoricalWeather | null
    oneWeekAgo: HistoricalWeather | null
    oneMonthAgo: HistoricalWeather | null
    threeMonthsAgo: HistoricalWeather | null
    oneYearAgo: HistoricalWeather | null
}

// ============ Weather Alert Types ============
export interface WeatherAlert {
    id: number
    alertId: string
    severity: 'minor' | 'moderate' | 'severe' | 'extreme'
    certainty: string
    urgency: string
    event: string
    headline: string
    description: string
    instruction: string
    effectiveFrom: string
    expiresAt: string
    city: string
    warnCellId: number
}

export interface RadarMetadata {
    timestamp: string
    source: string
    latitude: number
    longitude: number
    distance: number
    precipitationMin: number
    precipitationMax: number
    precipitationAvg: number
    coveragePercent: number
    significantRainCells: number
    totalCells: number
    directApiUrl: string
    createdAt: string
    hasActivePrecipitation: boolean
}

export interface RadarEndpointConfig {
    radarEndpoint: string
    format: string
    distance: number
    documentation: string
    note: string
}

// ============ Animal Detection Types ============
export interface AnimalDetection {
    id: number
    species: string
    animalType: 'bird' | 'mammal' | 'other'
    confidence: number
    imagePath: string | null
    deviceId: string
    timestamp: string
    createdAt: string
}

export interface AnimalSummary {
    totalDetections: number
    uniqueSpecies: number
    uniqueAnimalTypes: number
    averageConfidence: number
    firstDetection: string | null
    lastDetection: string | null
    speciesBreakdown: Record<string, number>
    animalTypeBreakdown: Record<string, number>
    deviceBreakdown: Record<string, number>
    mostActiveHour: string | null
    detectionsInLastHour: number
    birdDetections: number
    mammalDetections: number
    otherAnimalDetections: number
}

export interface SystemStats {
    totalDetections: number
    todayDetections: number
    uniqueSpecies: number
    activeDevices: number
}

// ============ Workflow / AI Types ============
export type WorkflowType = 'SUMMARY' | 'WEATHER_SUMMARY' | 'SIGHTINGS_SUMMARY' | 'AGENT_RESPONSE'

export interface WorkflowResult {
    id: number
    workflowType: WorkflowType
    userId: string | null
    analysisText: string | null
    dataConfidence: number | null
    sourceDataSummary: string | null
    metadata: string | null
    timestamp: string
    lastUpdated: string | null
    weatherCondition: string | null
    peakActivityTime: string | null
    expectedSpecies: number | null
    accuracyPercentage: number | null
    recentBirdCount: number | null
    dominantSpecies: string | null
    temperatureRange: string | null
    humidityRange: string | null
    pressureRange: string | null
    windConditionRange: string | null
}

export interface AgentQuery {
    query: string
}

export interface AgentResponse {
    response: string
    success: boolean
}

// ============ Location Types ============
export interface LocationData {
    id: number
    ipAddress: string
    city: string
    region: string
    country: string
    countryCode: string
    latitude: number
    longitude: number
    timezone: string
    createdAt: string
}

// ============ API Response Types ============
export interface ApiError {
    error: string
    message: string
    action?: string
}

export interface UpdateInfo {
    lastUpdated: string | null
    createdAt: string | null
    hasRecentData: boolean
    nextUpdateEstimate?: string
}
