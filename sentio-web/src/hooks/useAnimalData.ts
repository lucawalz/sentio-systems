import { useState, useEffect, useRef } from 'react';
import { animalService, type AnimalDetectionDTO, type AnimalDetectionSummary } from '../services/animalService';

export const useAnimalData = (refreshInterval: number = 300000) => {
    const [latestDetections, setLatestDetections] = useState<AnimalDetectionDTO[]>([]);
    const [recentDetections, setRecentDetections] = useState<AnimalDetectionDTO[]>([]);
    const [detectionSummary, setDetectionSummary] = useState<AnimalDetectionSummary | null>(null);
    const [speciesCount, setSpeciesCount] = useState<Record<string, number>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const intervalRef = useRef<number | null>(null);

    const fetchAnimalData = async () => {
        try {
            setLoading(true);
            setError(null);

            const [latest, recent, summary, species] = await Promise.all([
                animalService.getLatestDetections(10),
                animalService.getRecentDetections(24),
                animalService.getDetectionSummary(),
                animalService.getSpeciesCount(),
            ]);

            setLatestDetections(latest);
            setRecentDetections(recent);
            setDetectionSummary(summary);
            setSpeciesCount(species);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch animal data');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {

        if (intervalRef.current !== null) {
            clearInterval(intervalRef.current);
        }

        fetchAnimalData();

        intervalRef.current = setInterval(fetchAnimalData, refreshInterval);

        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
                intervalRef.current = null;
            }
        };
    }, [refreshInterval]);

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
        refetch: fetchAnimalData,
    };
};