import { useState, useEffect, useCallback } from 'react';
import { radarService, type RadarMetadata } from '../services/radarService';

export const useRadarData = (refreshInterval: number = 300000) => {
    const [metadata, setMetadata] = useState<RadarMetadata | null>(null);
    const [radarData, setRadarData] = useState<any | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const fetchData = useCallback(async () => {
        try {
            setError(null);

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
    }, []);

    useEffect(() => {
        fetchData();

        // Refresh radar data periodically (default: 5 minutes)
        const interval = setInterval(fetchData, refreshInterval);
        return () => clearInterval(interval);
    }, [fetchData, refreshInterval]);

    return {
        metadata,
        radarData,
        loading,
        error,
        refetch: fetchData,
        hasData: metadata !== null
    };
};

// Helper to check if timestamp is older than X minutes
function isOlderThanMinutes(timestamp: string, minutes: number): boolean {
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    return diffMs > minutes * 60 * 1000;
}
