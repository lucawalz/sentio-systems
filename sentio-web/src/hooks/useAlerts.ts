import { useState, useEffect, useCallback } from 'react';
import { alertService, type WeatherAlert } from '../services/alertService';

export const useAlerts = (refreshInterval: number = 300000) => {
    const [alerts, setAlerts] = useState<WeatherAlert[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchAlerts = useCallback(async () => {
        try {
            setError(null);
            const data = await alertService.getCurrentLocationAlerts('en');
            setAlerts(data);
        } catch (err) {
            console.error('Failed to fetch alerts:', err);
            setError(err instanceof Error ? err.message : 'Failed to fetch alerts');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchAlerts();

        // Refresh alerts periodically (default: 5 minutes)
        const interval = setInterval(fetchAlerts, refreshInterval);
        return () => clearInterval(interval);
    }, [fetchAlerts, refreshInterval]);

    return {
        alerts,
        loading,
        error,
        refetch: fetchAlerts,
        hasAlerts: alerts.length > 0
    };
};
