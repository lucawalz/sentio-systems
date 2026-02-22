import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { devicesApi } from '@/lib/api'
import type { Device } from '@/types/api'

// Location to focus/zoom on (from alert click, etc.)
interface FocusLocation {
    latitude: number
    longitude: number
    label?: string
}

interface DeviceContextType {
    devices: Device[]
    selectedDevice: Device | null  // null = unified view (Sentio Systems)
    focusLocation: FocusLocation | null // Temporary zoom target (e.g., from alert)
    isLoading: boolean
    setSelectedDevice: (device: Device | null) => void
    setFocusLocation: (location: FocusLocation | null) => void
    setPrimaryDevice: (deviceId: string) => Promise<void>
    refreshDevices: () => Promise<void>
}

const DeviceContext = createContext<DeviceContextType | null>(null)

export function useDevices() {
    const context = useContext(DeviceContext)
    if (!context) {
        throw new Error('useDevices must be used within a DeviceProvider')
    }
    return context
}

interface DeviceProviderProps {
    children: ReactNode
}

export function DeviceProvider({ children }: DeviceProviderProps) {
    const [devices, setDevices] = useState<Device[]>([])
    const [selectedDevice, setSelectedDevice] = useState<Device | null>(null)
    const [focusLocation, setFocusLocation] = useState<FocusLocation | null>(null)
    const [isLoading, setIsLoading] = useState(true)

    const refreshDevices = useCallback(async () => {
        try {
            const response = await devicesApi.list()
            const deviceList = response.data as Device[]
            setDevices(deviceList)
        } catch (err) {
            console.error('Failed to fetch devices:', err)
        } finally {
            setIsLoading(false)
        }
    }, [])

    const setPrimaryDeviceHandler = useCallback(async (deviceId: string) => {
        try {
            await devicesApi.setPrimary(deviceId)
            await refreshDevices()
        } catch (err) {
            console.error('Failed to set primary device:', err)
            throw err
        }
    }, [refreshDevices])

    useEffect(() => {
        refreshDevices()
    }, [refreshDevices])

    const value: DeviceContextType = {
        devices,
        selectedDevice,
        focusLocation,
        isLoading,
        setSelectedDevice,
        setFocusLocation,
        setPrimaryDevice: setPrimaryDeviceHandler,
        refreshDevices,
    }

    return <DeviceContext.Provider value={value}>{children}</DeviceContext.Provider>
}
