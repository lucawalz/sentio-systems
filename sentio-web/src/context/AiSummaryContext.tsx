import React, { createContext, useContext, useState, useEffect, useRef, type ReactNode } from 'react';
import { AiSummaryService, type AISummary } from '../services/aiSummaryService';

interface AiSummaryContextType {
    summary: AISummary | null;
    loading: boolean;
    error: string | null;
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
    const [summary, setSummary] = useState<AISummary | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const intervalRef = useRef<number | null>(null);

    const fetchSummary = async () => {
        try {
            if (!summary) {
                setLoading(true);
            }
            setError(null);
            const data = await AiSummaryService.getCurrentSummary();
            setSummary(data);
        } catch (err) {
            console.error("Failed to fetch AI summary:", err);
            setError(err instanceof Error ? err.message : 'Failed to fetch AI summary');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSummary();

        intervalRef.current = setInterval(fetchSummary, refreshInterval);

        return () => {
            if (intervalRef.current !== null) {
                clearInterval(intervalRef.current);
            }
        };
    }, [refreshInterval]);

    return (
        <AiSummaryContext.Provider value={{
            summary,
            loading,
            error,
            refetch: fetchSummary
        }}>
            {children}
        </AiSummaryContext.Provider>
    );
};
