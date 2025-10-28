
import { useState, useEffect, useRef } from 'react';
import { birdService, type BirdDetectionDTO, type BirdDetectionSummary } from '../services/birdService';

export const useBirdData = (refreshInterval: number = 300000) => {
    const [latestDetections, setLatestDetections] = useState<BirdDetectionDTO[]>([]);
    const [recentDetections, setRecentDetections] = useState<BirdDetectionDTO[]>([]);
    const [detectionSummary, setDetectionSummary] = useState<BirdDetectionSummary | null>(null);
    const [speciesCount, setSpeciesCount] = useState<Record<string, number>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // Use ref to store the current interval ID (browser environment)
    const intervalRef = useRef<number | null>(null);

    const fetchBirdData = async () => {
        try {
            setLoading(true);
            setError(null);

            const [latest, recent, summary, species] = await Promise.all([
                birdService.getLatestDetections(10),
                birdService.getRecentDetections(24),
                birdService.getDetectionSummary(),
                birdService.getSpeciesCount(),
            ]);

            setLatestDetections(latest);
            setRecentDetections(recent);
            setDetectionSummary(summary);
            setSpeciesCount(species);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch bird data');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // Clear existing interval first
        if (intervalRef.current !== null) {
            clearInterval(intervalRef.current);
        }

        // Fetch data immediately
        fetchBirdData();

        // Set up new interval
        intervalRef.current = setInterval(fetchBirdData, refreshInterval);

        // Cleanup function
        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
                intervalRef.current = null;
            }
        };
    }, [refreshInterval]);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
            }
        };
    }, []);

    return {
        latestDetections,
        recentDetections,
        detectionSummary,
        speciesCount,
        loading,
        error,
        refetch: fetchBirdData,
    };
};