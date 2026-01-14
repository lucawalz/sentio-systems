import { useState, useEffect, useCallback } from 'react';
import { deviceService, type Device } from '../services/deviceService';

export const useDevices = () => {
    const [devices, setDevices] = useState<Device[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchDevices = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await deviceService.getMyDevices();
            setDevices(data);
        } catch (err) {
            console.error("Failed to fetch devices:", err);
            setError(err instanceof Error ? err.message : 'Failed to fetch devices');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchDevices();
    }, [fetchDevices]);

    return {
        devices,
        loading,
        error,
        refetch: fetchDevices,
        registerDevice: deviceService.registerDevice,
        unregisterDevice: deviceService.unregisterDevice
    };
};
