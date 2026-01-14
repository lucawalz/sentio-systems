import { useAiSummaryContext } from '../context/AiSummaryContext';

export const useAiSummary = (_refreshInterval: number = 300000) => {
    // interval managed by provider
    const {
        summary,
        loading,
        error,
        refetch
    } = useAiSummaryContext();

    return {
        summary,
        loading,
        error,
        refetch
    };
};
