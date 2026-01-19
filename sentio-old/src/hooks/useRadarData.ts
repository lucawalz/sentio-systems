import { useState, useEffect, useCallback } from 'react';
import { radarService, type RadarMetadata } from '../services/radarService';
import { useDeviceContext } from '../context/DeviceContext';
import { useWebSocketSubscription } from '../context/WebSocketContext';

export const useRadarData = (refreshInterval: number = 300000) => {
    const { hasDevices, loading: devicesLoading } = useDeviceContext();
    const [metadata, setMetadata] = useState<RadarMetadata | null>(null);
    const [radarData, setRadarData] = useState<any | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [noDevices, setNoDevices] = useState(false);

    const fetchData = useCallback(async () => {
        // Skip API call if user has no devices
        if (!hasDevices) {
            setMetadata(null);
            setRadarData(null);
            setNoDevices(true);
            setLoading(false);
            return;
        }

        try {
            setError(null);
            setNoDevices(false);

            // First, try to get latest stored metadata
            let meta = await radarService.getLatestRadarMetadata();

            // If no metadata exists or it's older than 10 minutes, fetch fresh
            if (!meta || isOlderThanMinutes(meta.timestamp, 10)) {
                meta = await radarService.fetchRadarMetadata(100000);
            }

            setMetadata(meta);

            // If we have a direct API URL, fetch the actual radar data
            if (meta?.directApiUrl) {
                // Use plain format for easier parsing in frontend
                const plainUrl = meta.directApiUrl.replace('format=compressed', 'format=plain');
                const data = await radarService.fetchRadarDataDirect(plainUrl);
                setRadarData(data);
            }
        } catch (err) {
            console.error('Failed to fetch radar data:', err);
            setError(err instanceof Error ? err.message : 'Failed to fetch radar data');
        } finally {
            setLoading(false);
        }
    }, [hasDevices]);

    // Listen for WebSocket weather updates (radar is part of weather data)
    const handleWeatherUpdate = useCallback(() => {
        console.log('[useRadarData] Received WEATHER_UPDATED event, refetching...');
        fetchData();
    }, [fetchData]);

    useWebSocketSubscription('WEATHER_UPDATED', handleWeatherUpdate);

    useEffect(() => {
        // Wait for device check to complete
        if (devicesLoading) return;

        fetchData();

        // Only set up polling if user has devices (as fallback)
        if (hasDevices) {
            const interval = setInterval(fetchData, refreshInterval);
            return () => clearInterval(interval);
        }
    }, [fetchData, refreshInterval, hasDevices, devicesLoading]);

    return {
        metadata,
        radarData,
        loading: loading || devicesLoading,
        error,
        refetch: fetchData,
        hasData: metadata !== null,
        noDevices
    };
};

// Helper to check if timestamp is older than X minutes
function isOlderThanMinutes(timestamp: string, minutes: number): boolean {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    return diffMs > minutes * 60 * 1000;
}
