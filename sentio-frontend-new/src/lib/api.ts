import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios'

// API base URL - empty for nginx proxy (relative URLs)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

// Create axios instance with credentials for httpOnly cookies
const api: AxiosInstance = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true, // Send cookies with requests
    headers: {
        'Content-Type': 'application/json',
    },
})

// Track if we're currently refreshing to prevent infinite loops
let isRefreshing = false
let failedQueue: Array<{
    resolve: (value?: unknown) => void
    reject: (error?: unknown) => void
}> = []

const processQueue = (error: Error | null = null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error)
        } else {
            prom.resolve()
        }
    })
    failedQueue = []
}

// Request interceptor - add any custom headers
api.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // Cookies are sent automatically due to withCredentials
        return config
    },
    (error) => Promise.reject(error)
)

// Response interceptor - handle 401 and token refresh
api.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
        const originalRequest = error.config

        // If 401 and not already retrying
        if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
            // Don't retry auth endpoints
            if (originalRequest.url?.includes('/api/auth/')) {
                return Promise.reject(error)
            }

            if (isRefreshing) {
                // Queue the request while refreshing
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject })
                }).then(() => api(originalRequest))
            }

            originalRequest._retry = true
            isRefreshing = true

            try {
                // Try to refresh the token
                await api.post('/api/auth/refresh')
                processQueue()
                return api(originalRequest)
            } catch (refreshError) {
                processQueue(refreshError as Error)
                // Redirect to login on refresh failure
                window.location.href = '/login'
                return Promise.reject(refreshError)
            } finally {
                isRefreshing = false
            }
        }

        return Promise.reject(error)
    }
)

// Extend axios config to support _retry flag
declare module 'axios' {
    export interface InternalAxiosRequestConfig {
        _retry?: boolean
    }
}

export default api

// Export typed API methods for convenience
export const authApi = {
    login: (username: string, password: string) =>
        api.post('/api/auth/login', { username, password }),
    logout: () => api.post('/api/auth/logout'),
    me: () => api.get('/api/auth/me'),
    refresh: () => api.post('/api/auth/refresh'),
    register: (data: { username: string; password: string; email: string; firstName: string; lastName: string }) =>
        api.post('/api/auth/register', data),
}

export const devicesApi = {
    list: () => api.get('/api/devices'),
    register: (deviceId: string, name: string) =>
        api.post('/api/devices/register', null, { params: { deviceId, name } }),
    unregister: (deviceId: string) => api.delete(`/api/devices/${deviceId}`),
    hasAny: () => api.get('/api/devices/has-any'),
}

export const weatherApi = {
    latest: () => api.get('/api/weather/latest'),
    recent: () => api.get('/api/weather/recent'),
    stats: () => api.get('/api/weather/stats'),
}

export const forecastApi = {
    currentLocation: () => api.get('/api/forecast/current-location'),
    upcoming: () => api.get('/api/forecast/upcoming'),
    dateRange: (startDate: string, endDate: string) =>
        api.get('/api/forecast/date-range', { params: { startDate, endDate } }),
    forDate: (date: string) => api.get(`/api/forecast/date/${date}`),
    lastUpdate: () => api.get('/api/forecast/last-update'),
}

export const historicalApi = {
    currentLocation: () => api.get('/api/historical/current-location'),
    comparison: () => api.get('/api/historical/comparison'),
    forDate: (date: string) => api.get(`/api/historical/date/${date}`),
    lastUpdate: () => api.get('/api/historical/last-update'),
}

export const animalsApi = {
    latest: (limit = 10) => api.get('/api/animals/latest', { params: { limit } }),
    recent: (hours = 24) => api.get('/api/animals/recent', { params: { hours } }),
    byDate: (date: string) => api.get('/api/animals/by-date', { params: { date } }),
    bySpecies: (species: string, page = 0, size = 20) =>
        api.get('/api/animals/by-species', { params: { species, page, size } }),
    byType: (animalType: string, page = 0, size = 20) =>
        api.get('/api/animals/by-type', { params: { animalType, page, size } }),
    summary: (hours = 24) => api.get('/api/animals/summary', { params: { hours } }),
    speciesCount: (hours = 24) => api.get('/api/animals/species-count', { params: { hours } }),
    typeCount: (hours = 24) => api.get('/api/animals/type-count', { params: { hours } }),
    hourlyActivity: (date: string) => api.get('/api/animals/hourly-activity', { params: { date } }),
    species: () => api.get('/api/animals/species'),
    types: () => api.get('/api/animals/types'),
    stats: () => api.get('/api/animals/stats'),
    getById: (id: number) => api.get(`/api/animals/${id}`),
}

export const alertsApi = {
    currentLocation: (lang = 'en') => api.get('/api/alerts/current-location', { params: { lang } }),
    active: (lang = 'en') => api.get('/api/alerts/active', { params: { lang } }),
    recent: (lang = 'en') => api.get('/api/alerts/recent', { params: { lang } }),
    bySeverity: (severity: string, lang = 'en') =>
        api.get(`/api/alerts/severity/${severity}`, { params: { lang } }),
    cities: () => api.get('/api/alerts/cities'),
    radarEndpoint: (distance?: number, format = 'compressed') =>
        api.get('/api/alerts/radar/endpoint', { params: { distance, format } }),
    radarLatest: () => api.get('/api/alerts/radar/latest'),
    fetchRadar: (distance?: number) => api.post('/api/alerts/radar/fetch', null, { params: { distance } }),
}

export const locationApi = {
    current: () => api.get('/api/location/current'),
    byIp: (ip?: string) => api.get('/api/location/by-ip', { params: { ip } }),
}

export const workflowApi = {
    current: () => api.get('/api/workflow/current'),
    recent: () => api.get('/api/workflow/recent'),
    currentSummary: () => api.get('/api/workflow/summaries/current'),
    myWeather: () => api.get('/api/workflow/me/weather'),
    mySightings: () => api.get('/api/workflow/me/sightings'),
    myRecent: () => api.get('/api/workflow/me/recent'),
    generateWeather: () => api.post('/api/workflow/generate/weather'),
    generateSightings: () => api.post('/api/workflow/generate/sightings'),
    askAgent: (query: string) => api.post('/api/workflow/agent/ask', { query }),
}
