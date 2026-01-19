import { get, post, del } from './api';

// Animal detection interfaces matching your backend DTOs
export interface AnimalDetectionDTO {
    id: number;
    species: string;
    animalType: string;
    confidence: number;
    alternateSpecies?: AlternateSpeciesDTO[];
    originalSpecies?: string;
    originalConfidence?: number;
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
    processedAt: string;
    aiClassifiedAt?: string;
    aiProcessed: boolean;
}

export interface AlternateSpeciesDTO {
    species: string;
    confidence: number;
}

export interface AnimalDetectionSummary {
    totalDetections: number;
    uniqueSpecies: number;
    averageConfidence: number;
    firstDetection: string;
    lastDetection: string;
    speciesBreakdown: Record<string, number>;
    animalTypeBreakdown: Record<string, number>;
    deviceBreakdown: Record<string, number>;
    mostActiveHour: string;
    detectionsInLastHour: number;
}

const ANIMALS_ENDPOINT = import.meta.env.VITE_API_ANIMALS_ENDPOINT || '/api/animals';

export const animalService = {
    // Get latest animal detections
    getLatestDetections: (limit: number = 10): Promise<AnimalDetectionDTO[]> =>
        get<AnimalDetectionDTO[]>(`${ANIMALS_ENDPOINT}/latest?limit=${limit}`),

    // Get recent animal detections
    getRecentDetections: (hours: number = 24): Promise<AnimalDetectionDTO[]> =>
        get<AnimalDetectionDTO[]>(`${ANIMALS_ENDPOINT}/recent?hours=${hours}`),

    // Get detections by date
    getDetectionsByDate: (date: string): Promise<AnimalDetectionDTO[]> =>
        get<AnimalDetectionDTO[]>(`${ANIMALS_ENDPOINT}/by-date?date=${date}`),

    // Get detections by species
    getDetectionsBySpecies: (species: string, page: number = 0, size: number = 10): Promise<AnimalDetectionDTO[]> =>
        get<AnimalDetectionDTO[]>(`${ANIMALS_ENDPOINT}/by-species?species=${species}&page=${page}&size=${size}`),

    // Get detections by animal type
    getDetectionsByAnimalType: (animalType: string, page: number = 0, size: number = 10): Promise<AnimalDetectionDTO[]> =>
        get<AnimalDetectionDTO[]>(`${ANIMALS_ENDPOINT}/by-type?animalType=${animalType}&page=${page}&size=${size}`),

    // Get detection summary
    getDetectionSummary: (hours: number = 24): Promise<AnimalDetectionSummary> =>
        get<AnimalDetectionSummary>(`${ANIMALS_ENDPOINT}/summary?hours=${hours}`),

    // Get detections by device
    getDetectionsByDevice: (deviceId: string, page: number = 0, size: number = 10): Promise<AnimalDetectionDTO[]> =>
        get<AnimalDetectionDTO[]>(`${ANIMALS_ENDPOINT}/by-device?deviceId=${deviceId}&page=${page}&size=${size}`),

    // Get species count
    getSpeciesCount: (hours: number = 24): Promise<Record<string, number>> =>
        get<Record<string, number>>(`${ANIMALS_ENDPOINT}/species-count?hours=${hours}`),

    // Get animal type count
    getAnimalTypeCount: (hours: number = 24): Promise<Record<string, number>> =>
        get<Record<string, number>>(`${ANIMALS_ENDPOINT}/type-count?hours=${hours}`),

    // Record a new detection
    recordDetection: (detection: Partial<AnimalDetectionDTO>): Promise<AnimalDetectionDTO> =>
        post<AnimalDetectionDTO>(`${ANIMALS_ENDPOINT}/detect`, detection),

    // Delete a detection
    deleteDetection: (id: number): Promise<void> =>
        del(`${ANIMALS_ENDPOINT}/${id}`),
};

// Helper function to get full image URL
export const getImageUrl = (imageUrl: string): string => {
    const imageBaseUrl = import.meta.env.VITE_IMAGE_BASE_URL || 'http://localhost:8080';
    if (!imageUrl) return '';
    if (imageUrl.startsWith('http')) {
        return imageUrl;
    }
    // Ensure we don't double slashes if imageUrl starts with /
    const cleanPath = imageUrl.startsWith('/') ? imageUrl.substring(1) : imageUrl;
    return `${imageBaseUrl}/${cleanPath}`;
};