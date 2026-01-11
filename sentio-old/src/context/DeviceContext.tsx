import React, { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';
import { deviceService, type Device } from '../services/deviceService';
import { useWebSocketSubscription } from './WebSocketContext';

interface DeviceContextType {
    devices: Device[];
    hasDevices: boolean;
    loading: boolean;
    error: string | null;
    refetch: () => Promise<void>;
    registerDevice: (deviceId: string, name: string) => Promise<Device>;
    unregisterDevice: (deviceId: string) => Promise<void>;
}

const DeviceContext = createContext<DeviceContextType | undefined>(undefined);

export const useDeviceContext = () => {
    const context = useContext(DeviceContext);
    if (!context) {
        throw new Error('useDeviceContext must be used within a DeviceProvider');
    }
    return context;
};

interface DeviceProviderProps {
    children: ReactNode;
}

export const DeviceProvider: React.FC<DeviceProviderProps> = ({ children }) => {
    const [devices, setDevices] = useState<Device[]>([]);
    const [hasDevices, setHasDevices] = useState<boolean>(false);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchDevices = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);

            // First check if user has any devices (lightweight call)
            const hasAny = await deviceService.hasAnyDevices();
            setHasDevices(hasAny);

            if (hasAny) {
                // Only fetch full device list if user has devices
                const data = await deviceService.getMyDevices();
                setDevices(data);
            } else {
                setDevices([]);
            }
        } catch (err) {
            console.error("Failed to fetch devices:", err);
            setError(err instanceof Error ? err.message : 'Failed to fetch devices');
            setHasDevices(false);
        } finally {
            setLoading(false);
        }
    }, []);

    // Initial fetch on mount
    useEffect(() => {
        fetchDevices();
    }, [fetchDevices]);

    // Listen for WebSocket device events to auto-refetch
    const handleDeviceEvent = useCallback(() => {
        console.log('[DeviceContext] Received device event via WebSocket, refetching...');
        fetchDevices();
    }, [fetchDevices]);

    // Subscribe to device registered events
    useWebSocketSubscription('DEVICE_REGISTERED', handleDeviceEvent);

    // Subscribe to device unregistered events
    useWebSocketSubscription('DEVICE_UNREGISTERED', handleDeviceEvent);

    const handleRegisterDevice = async (deviceId: string, name: string): Promise<Device> => {
        const device = await deviceService.registerDevice(deviceId, name);
        // WebSocket will trigger refetch automatically, but we also refetch immediately for responsiveness
        await fetchDevices();
        return device;
    };

    const handleUnregisterDevice = async (deviceId: string): Promise<void> => {
        await deviceService.unregisterDevice(deviceId);
        // WebSocket will trigger refetch automatically, but we also refetch immediately for responsiveness
        await fetchDevices();
    };

    return (
        <DeviceContext.Provider value={{
            devices,
            hasDevices,
            loading,
            error,
            refetch: fetchDevices,
            registerDevice: handleRegisterDevice,
            unregisterDevice: handleUnregisterDevice
        }}>
            {children}
        </DeviceContext.Provider>
    );
};
