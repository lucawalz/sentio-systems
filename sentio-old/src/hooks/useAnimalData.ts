import { useAnimalContext } from '../context/AnimalContext';

export const useAnimalData = (_refreshInterval: number = 30000) => {
    // The refreshInterval is now controlled by the Provider, so we ignore the argument
    // but keep it for backward compatibility if needed, or better, remove it from usage sites later.
    const {
        latestDetections,
        recentDetections,
        detectionSummary,
        speciesCount,
        loading,
        error,
        refetch
    } = useAnimalContext();

    return {
        latestDetections,
        recentDetections,
        detectionSummary,
        speciesCount,
        loading,
        error,
        refetch
    };
};