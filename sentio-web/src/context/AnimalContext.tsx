import React, { createContext, useContext, useState, useEffect, useRef, type ReactNode } from 'react';
import { animalService, type AnimalDetectionDTO, type AnimalDetectionSummary } from '../services/animalService';

interface AnimalContextType {
    latestDetections: AnimalDetectionDTO[];
    recentDetections: AnimalDetectionDTO[];
    detectionSummary: AnimalDetectionSummary | null;
    speciesCount: Record<string, number>;
    loading: boolean;
    error: string | null;
    refetch: () => Promise<void>;
}

const AnimalContext = createContext<AnimalContextType | undefined>(undefined);

export const useAnimalContext = () => {
    const context = useContext(AnimalContext);
    if (!context) {
        throw new Error('useAnimalContext must be used within an AnimalProvider');
    }
    return context;
};

interface AnimalProviderProps {
    children: ReactNode;
    refreshInterval?: number;
}

export const AnimalProvider: React.FC<AnimalProviderProps> = ({ children, refreshInterval = 30000 }) => {
    const [latestDetections, setLatestDetections] = useState<AnimalDetectionDTO[]>([]);
    const [recentDetections, setRecentDetections] = useState<AnimalDetectionDTO[]>([]);
    const [detectionSummary, setDetectionSummary] = useState<AnimalDetectionSummary | null>(null);
    const [speciesCount, setSpeciesCount] = useState<Record<string, number>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const intervalRef = useRef<number | null>(null);

    const fetchAnimalData = async () => {
        try {
            // Don't set loading to true on background refreshes to avoid UI flickering
            if (latestDetections.length === 0) {
                setLoading(true);
            }
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
            console.error("Failed to fetch animal data:", err);
            setError(err instanceof Error ? err.message : 'Failed to fetch animal data');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAnimalData();

        intervalRef.current = setInterval(fetchAnimalData, refreshInterval);

        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
            }
        };
    }, [refreshInterval]);

    return (
        <AnimalContext.Provider value={{
            latestDetections,
            recentDetections,
            detectionSummary,
            speciesCount,
            loading,
            error,
            refetch: fetchAnimalData
        }}>
            {children}
        </AnimalContext.Provider>
    );
};
