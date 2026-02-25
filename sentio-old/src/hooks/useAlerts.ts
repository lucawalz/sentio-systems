import { useState, useEffect, useCallback } from 'react';
import { alertService, type WeatherAlert } from '../services/alertService';
import { useDeviceContext } from '../context/DeviceContext';
import { useWebSocketSubscription } from '../context/WebSocketContext';

export const useAlerts = (refreshInterval: number = 300000) => {
    const { hasDevices, loading: devicesLoading } = useDeviceContext();
    const [alerts, setAlerts] = useState<WeatherAlert[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [noDevices, setNoDevices] = useState(false);

    const fetchAlerts = useCallback(async () => {
        // Skip API call if user has no devices
        if (!hasDevices) {
            setAlerts([]);
            setNoDevices(true);
            setLoading(false);
            return;
        }

        try {
            setError(null);
            setNoDevices(false);
            const data = await alertService.getCurrentLocationAlerts('en');
            setAlerts(data);
        } catch (err) {
            console.error('Failed to fetch alerts:', err);
            setError(err instanceof Error ? err.message : 'Failed to fetch alerts');
        } finally {
            setLoading(false);
        }
    }, [hasDevices]);

    // Listen for WebSocket alerts updates
    const handleAlertsUpdate = useCallback(() => {
        console.log('[useAlerts] Received ALERTS_UPDATED event, refetching...');
        fetchAlerts();
    }, [fetchAlerts]);

    useWebSocketSubscription('ALERTS_UPDATED', handleAlertsUpdate);

    useEffect(() => {
        // Wait for device check to complete
        if (devicesLoading) return;

        fetchAlerts();

        // Only set up polling if user has devices (as fallback)
        if (hasDevices) {
            const interval = setInterval(fetchAlerts, refreshInterval);
            return () => clearInterval(interval);
        }
    }, [fetchAlerts, refreshInterval, hasDevices, devicesLoading]);

    return {
        alerts,
        loading: loading || devicesLoading,
        error,
        refetch: fetchAlerts,
        hasAlerts: alerts.length > 0,
        noDevices
    };
};
