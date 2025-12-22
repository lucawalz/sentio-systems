import React, { createContext, useContext, useState, useEffect, useRef, useCallback, type ReactNode } from 'react';
import { animalService, type AnimalDetectionDTO, type AnimalDetectionSummary } from '../services/animalService';
import { useDeviceContext } from './DeviceContext';
import { useWebSocketSubscription } from './WebSocketContext';

interface AnimalContextType {
    latestDetections: AnimalDetectionDTO[];
    recentDetections: AnimalDetectionDTO[];
    detectionSummary: AnimalDetectionSummary | null;
    speciesCount: Record<string, number>;
    loading: boolean;
    error: string | null;
    noDevices: boolean;
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
    const { hasDevices, loading: devicesLoading } = useDeviceContext();
    const [latestDetections, setLatestDetections] = useState<AnimalDetectionDTO[]>([]);
    const [recentDetections, setRecentDetections] = useState<AnimalDetectionDTO[]>([]);
    const [detectionSummary, setDetectionSummary] = useState<AnimalDetectionSummary | null>(null);
    const [speciesCount, setSpeciesCount] = useState<Record<string, number>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [noDevices, setNoDevices] = useState(false);

    const intervalRef = useRef<number | null>(null);

    const fetchAnimalData = useCallback(async () => {
        // Skip API call if user has no devices
        if (!hasDevices) {
            setLatestDetections([]);
            setRecentDetections([]);
            setDetectionSummary(null);
            setSpeciesCount({});
            setNoDevices(true);
            setLoading(false);
            return;
        }

        try {
            setError(null);
            setNoDevices(false);

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
    }, [hasDevices]); // Removed latestDetections.length - was causing infinite loop!

    // Listen for WebSocket updates (could add ANIMAL_DETECTED event later)
    const handleWeatherUpdate = useCallback(() => {
        // Animal detections often correlate with weather station data updates
        console.log('[AnimalContext] Received WEATHER_UPDATED event, refetching...');
        fetchAnimalData();
    }, [fetchAnimalData]);

    useWebSocketSubscription('WEATHER_UPDATED', handleWeatherUpdate);

    useEffect(() => {
        // Wait for device check to complete
        if (devicesLoading) return;

        fetchAnimalData();

        // Only set up polling if user has devices
        if (hasDevices) {
            intervalRef.current = setInterval(fetchAnimalData, refreshInterval);
        }

        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
                intervalRef.current = null;
            }
        };
    }, [refreshInterval, hasDevices, devicesLoading, fetchAnimalData]);

    return (
        <AnimalContext.Provider value={{
            latestDetections,
            recentDetections,
            detectionSummary,
            speciesCount,
            loading: loading || devicesLoading,
            error,
            noDevices,
            refetch: fetchAnimalData
        }}>
            {children}
        </AnimalContext.Provider>
    );
};
