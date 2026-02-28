import { get, post } from './api';

// Radar Metadata interface matching backend DTO
export interface RadarMetadata {
    timestamp: string;
    source: string | null;
    latitude: number;
    longitude: number;
    distance: number;
    precipitationMin: number;
    precipitationMax: number;
    precipitationAvg: number;
    coveragePercent: number;
    significantRainCells: number;
    totalCells: number;
    directApiUrl: string;
    createdAt: string;
    hasActivePrecipitation: boolean;
}

// Radar endpoint info
export interface RadarEndpointInfo {
    radarEndpoint: string | null;
    format: string;
    distance: number;
    documentation: string;
    note: string;
    error?: string;
}

const ALERTS_ENDPOINT = '/api/alerts';

export const radarService = {
    // Get radar endpoint URL for direct BrightSky access
    getRadarEndpoint: async (distance?: number, format: string = 'plain'): Promise<RadarEndpointInfo | null> => {
        try {
            const params = new URLSearchParams();
            if (distance) params.set('distance', distance.toString());
            params.set('format', format);

            const result = await get<RadarEndpointInfo>(`${ALERTS_ENDPOINT}/radar/endpoint?${params.toString()}`);
            return result;
        } catch (error) {
            console.error('Failed to get radar endpoint:', error);
            return null;
        }
    },

    // Fetch and store radar metadata (triggers backend to fetch from BrightSky)
    fetchRadarMetadata: async (distance?: number): Promise<RadarMetadata | null> => {
        try {
            const params = distance ? `?distance=${distance}` : '';
            const result = await post<RadarMetadata>(`${ALERTS_ENDPOINT}/radar/fetch${params}`, {});
            return result;
        } catch (error) {
            console.error('Failed to fetch radar metadata:', error);
            return null;
        }
    },

    // Get latest stored radar metadata
    getLatestRadarMetadata: async (): Promise<RadarMetadata | null> => {
        try {
            const result = await get<RadarMetadata>(`${ALERTS_ENDPOINT}/radar/latest`);
            return result;
        } catch (error) {
            // 404 is expected if no data exists yet
            if (error instanceof Error && error.message.includes('404')) {
                return null;
            }
            console.error('Failed to get latest radar metadata:', error);
            return null;
        }
    },

    // Fetch radar data directly from BrightSky API (for visualization)
    fetchRadarDataDirect: async (url: string): Promise<any | null> => {
        try {
            const response = await fetch(url);
            if (!response.ok) {
                throw new Error(`Failed to fetch radar data: ${response.status}`);
            }
            return await response.json();
        } catch (error) {
            console.error('Failed to fetch radar data from BrightSky:', error);
            return null;
        }
    }
};
