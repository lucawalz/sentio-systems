import { get } from './api';

// Bird detection interfaces matching your backend DTOs
export interface BirdDetectionDTO {
    id: number;
    species: string;
    confidence: number;
    x: number;
    y: number;
    width: number;
    height: number;
    classId: number;
    imageUrl: string;
    timestamp: string;
    deviceId: string;
    location: string;
    triggerReason: string;
    cameraResolution: string;
    processedAt: string;
    speciesAiClassified: string;
    predictions: BirdPredictionDTO[];
}

export interface BirdPredictionDTO {
    id: number;
    species: string;
    confidence: number;
}

export interface BirdDetectionSummary {
    totalDetections: number;
    uniqueSpecies: number;
    averageConfidence: number;
    firstDetection: string;
    lastDetection: string;
    speciesBreakdown: Record<string, number>;
    deviceBreakdown: Record<string, number>;
    mostActiveHour: string;
    detectionsInLastHour: number;
}

const BIRDS_ENDPOINT = import.meta.env.VITE_API_BIRDS_ENDPOINT || '/api/birds';

export const birdService = {
    // Get latest bird detections
    getLatestDetections: (limit: number = 10): Promise<BirdDetectionDTO[]> =>
        get<BirdDetectionDTO[]>(`${BIRDS_ENDPOINT}/latest?limit=${limit}`),

    // Get recent bird detections
    getRecentDetections: (hours: number = 24): Promise<BirdDetectionDTO[]> =>
        get<BirdDetectionDTO[]>(`${BIRDS_ENDPOINT}/recent?hours=${hours}`),

    // Get detections by date
    getDetectionsByDate: (date: string): Promise<BirdDetectionDTO[]> =>
        get<BirdDetectionDTO[]>(`${BIRDS_ENDPOINT}/by-date?date=${date}`),

    // Get detections by species
    getDetectionsBySpecies: (species: string, page: number = 0, size: number = 10): Promise<BirdDetectionDTO[]> =>
        get<BirdDetectionDTO[]>(`${BIRDS_ENDPOINT}/by-species?species=${species}&page=${page}&size=${size}`),

    // Get detection summary
    getDetectionSummary: (): Promise<BirdDetectionSummary> =>
        get<BirdDetectionSummary>(`${BIRDS_ENDPOINT}/summary`),

    // Get detections by device
    getDetectionsByDevice: (deviceId: string, page: number = 0, size: number = 10): Promise<BirdDetectionDTO[]> =>
        get<BirdDetectionDTO[]>(`${BIRDS_ENDPOINT}/by-device?deviceId=${deviceId}&page=${page}&size=${size}`),

    // Get species count
    getSpeciesCount: (): Promise<Record<string, number>> =>
        get<Record<string, number>>(`${BIRDS_ENDPOINT}/species-count`),
};

// Helper function to get full image URL
export const getImageUrl = (imageUrl: string): string => {
    const imageBaseUrl = import.meta.env.VITE_IMAGE_BASE_URL || 'http://localhost:8080/images';
    if (imageUrl.startsWith('http')) {
        return imageUrl;
    }
    return `${imageBaseUrl}/${imageUrl}`;
};