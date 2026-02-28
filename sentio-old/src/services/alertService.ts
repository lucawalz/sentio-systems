import { get, post } from './api';

// Weather Alert interfaces matching backend DTOs
export interface WeatherAlert {
    id: number | null;
    brightSkyId: number | null;
    alertId: string;
    status: string;
    effective: string;
    onset: string;
    expires: string | null;
    category: string | null;
    responseType: string | null;
    urgency: string | null;
    severity: string | null;
    certainty: string | null;
    eventCode: number | null;
    eventEn: string | null;
    eventDe: string | null;
    headlineEn: string | null;
    headlineDe: string | null;
    descriptionEn: string | null;
    descriptionDe: string | null;
    instructionEn: string | null;
    instructionDe: string | null;
    warnCellId: number | null;
    name: string | null;
    nameShort: string | null;
    district: string | null;
    state: string | null;
    stateShort: string | null;
    city: string | null;
    country: string | null;
    latitude: number | null;
    longitude: number | null;
    createdAt: string | null;
    updatedAt: string | null;
    isActive: boolean | null;
    // Localized fields based on language preference
    localizedHeadline: string | null;
    localizedDescription: string | null;
    localizedEvent: string | null;
    localizedInstruction: string | null;
}

const ALERTS_ENDPOINT = '/api/alerts';

export const alertService = {
    // Get alerts for current location
    getCurrentLocationAlerts: async (lang: string = 'en'): Promise<WeatherAlert[]> => {
        try {
            const result = await get<WeatherAlert[]>(`${ALERTS_ENDPOINT}/current-location?lang=${lang}`);
            return Array.isArray(result) ? result : [];
        } catch (error) {
            console.error('Failed to fetch alerts for current location:', error);
            return [];
        }
    },

    // Get all active alerts
    getActiveAlerts: async (lang: string = 'en'): Promise<WeatherAlert[]> => {
        try {
            const result = await get<WeatherAlert[]>(`${ALERTS_ENDPOINT}/active?lang=${lang}`);
            return Array.isArray(result) ? result : [];
        } catch (error) {
            console.error('Failed to fetch active alerts:', error);
            return [];
        }
    },

    // Get recent alerts (last 24 hours)
    getRecentAlerts: async (lang: string = 'en'): Promise<WeatherAlert[]> => {
        try {
            const result = await get<WeatherAlert[]>(`${ALERTS_ENDPOINT}/recent?lang=${lang}`);
            return Array.isArray(result) ? result : [];
        } catch (error) {
            console.error('Failed to fetch recent alerts:', error);
            return [];
        }
    },

    // Trigger update of alerts
    updateAlerts: async (): Promise<void> => {
        try {
            await post(`${ALERTS_ENDPOINT}/update`, {});
        } catch (error) {
            console.error('Failed to trigger alert update:', error);
            throw error;
        }
    }
};
