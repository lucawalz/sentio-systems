
import { get } from './api'

// Define the interface based on your backend AISummary model
export interface AISummary {
    id: number;
    analysisText: string | null;
    dataConfidence: number | null;
    sourceDataSummary: string | null;
    metadata: string | null;
    timestamp: string;
    lastUpdated: string;
    weatherCondition: string | null;
    peakActivityTime: string | null;
    expectedSpecies: number | null;
    accuracyPercentage: number | null;
    recentBirdCount: number | null;
    dominantSpecies: string | null;
    temperatureRange: string | null;
    humidityRange: string | null;
    pressureRange: string | null;
    windConditionRange: string | null;
}

export class AiSummaryService {
    private static readonly BASE_ENDPOINT = '/api/ai-summary'

    /**
     * Gets the current (most recent) AI summary
     */
    static async getCurrentSummary(): Promise<AISummary | null> {
        try {
            return await get<AISummary>(`${this.BASE_ENDPOINT}/current`)
        } catch (error) {
            console.error('Error fetching current AI summary:', error)
            return null
        }
    }

    /**
     * Gets recent AI summaries from the last 24 hours
     */
    static async getRecentSummaries(): Promise<AISummary[]> {
        try {
            return await get<AISummary[]>(`${this.BASE_ENDPOINT}/recent`)
        } catch (error) {
            console.error('Error fetching recent AI summaries:', error)
            return []
        }
    }
}