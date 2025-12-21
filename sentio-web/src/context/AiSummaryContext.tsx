import React, { createContext, useContext, useState, useEffect, useRef, useCallback, type ReactNode } from 'react';
import { AiSummaryService, type AISummary } from '../services/aiSummaryService';
import { useDeviceContext } from './DeviceContext';

interface AiSummaryContextType {
    summary: AISummary | null;
    loading: boolean;
    error: string | null;
    noDevices: boolean;
    refetch: () => Promise<void>;
}

const AiSummaryContext = createContext<AiSummaryContextType | undefined>(undefined);

export const useAiSummaryContext = () => {
    const context = useContext(AiSummaryContext);
    if (!context) {
        throw new Error('useAiSummaryContext must be used within an AiSummaryProvider');
    }
    return context;
};

interface AiSummaryProviderProps {
    children: ReactNode;
    refreshInterval?: number;
}

export const AiSummaryProvider: React.FC<AiSummaryProviderProps> = ({ children, refreshInterval = 300000 }) => {
    const { hasDevices, loading: devicesLoading } = useDeviceContext();
    const [summary, setSummary] = useState<AISummary | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [noDevices, setNoDevices] = useState(false);

    const intervalRef = useRef<number | null>(null);

    const fetchSummary = useCallback(async () => {
        // Skip API call if user has no devices
        if (!hasDevices) {
            setSummary(null);
            setNoDevices(true);
            setLoading(false);
            return;
        }

        try {
            setError(null);
            setNoDevices(false);
            const data = await AiSummaryService.getCurrentSummary();
            setSummary(data);
        } catch (err) {
            console.error("Failed to fetch AI summary:", err);
            setError(err instanceof Error ? err.message : 'Failed to fetch AI summary');
        } finally {
            setLoading(false);
        }
    }, [hasDevices]); // Removed summary - was causing infinite loop!

    useEffect(() => {
        // Wait for device check to complete
        if (devicesLoading) return;

        fetchSummary();

        // Only set up polling if user has devices
        if (hasDevices) {
            intervalRef.current = setInterval(fetchSummary, refreshInterval);
        }

        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
                intervalRef.current = null;
            }
        };
    }, [refreshInterval, hasDevices, devicesLoading, fetchSummary]);

    return (
        <AiSummaryContext.Provider value={{
            summary,
            loading: loading || devicesLoading,
            error,
            noDevices,
            refetch: fetchSummary
        }}>
            {children}
        </AiSummaryContext.Provider>
    );
};
