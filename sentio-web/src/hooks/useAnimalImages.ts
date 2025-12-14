
import { useState, useEffect, useRef, useCallback } from 'react';
import { wikipediaService } from '../services/wikipediaService';

interface AnimalImageCache {
    [species: string]: {
        url: string | null;
        lastAccessed: number;
    };
}

// Configuration
const MAX_CACHE_SIZE = 50;
const CACHE_CLEANUP_THRESHOLD = 40;

const INVALID_SPECIES = [
    'unknown',
    'unknown bird',
    'unknown animal',
    'bird',
    'mammal',
    'animal',
    'unidentified',
    'unidentified bird',
    'unidentified animal',
    'no detection',
    'none',
    'n/a',
    'null',
    'undefined'
];

const isValidSpecies = (species: string): boolean => {
    if (!species || typeof species !== 'string') {
        return false;
    }

    const normalizedSpecies = species.toLowerCase().trim();

    if (INVALID_SPECIES.includes(normalizedSpecies)) {
        return false;
    }

    if (normalizedSpecies.length < 3) {
        return false;
    }

    if (normalizedSpecies.startsWith('unknown') || normalizedSpecies.includes('unidentified')) {
        return false;
    }

    return true;
};

export const useAnimalImages = (speciesList: string[]) => {
    const [imageCache, setImageCache] = useState<AnimalImageCache>({});
    const [loading, setLoading] = useState<Set<string>>(new Set());
    const cleanupTimeoutRef = useRef<number | null>(null);

    // Clean up old cache entries when cache gets too large
    const cleanupCache = useCallback((currentCache: AnimalImageCache) => {
        const cacheEntries = Object.entries(currentCache);

        if (cacheEntries.length <= MAX_CACHE_SIZE) {
            return currentCache;
        }

        const sortedEntries = cacheEntries.sort((a, b) =>
            b[1].lastAccessed - a[1].lastAccessed
        );

        const keepEntries = sortedEntries.slice(0, CACHE_CLEANUP_THRESHOLD);

        const cleanedCache: AnimalImageCache = {};
        keepEntries.forEach(([species, data]) => {
            cleanedCache[species] = data;
        });

        console.log(`Cache cleaned: ${cacheEntries.length} -> ${keepEntries.length} entries`);
        return cleanedCache;
    }, []);

    const scheduleCleanup = useCallback(() => {
        if (cleanupTimeoutRef.current) {
            clearTimeout(cleanupTimeoutRef.current);
        }

        cleanupTimeoutRef.current = setTimeout(() => {
            setImageCache(currentCache => cleanupCache(currentCache));
        }, 1000);
    }, [cleanupCache]);

    const fetchAnimalImage = useCallback(async (species: string) => {
        if (!isValidSpecies(species)) {
            console.log(`Skipping image fetch for invalid species: "${species}"`);
            setImageCache(prev => ({
                ...prev,
                [species]: {
                    url: null,
                    lastAccessed: Date.now()
                }
            }));
            return;
        }

        setImageCache(prev => {
            if (prev[species] !== undefined) {
                return prev;
            }
            return prev;
        });

        setLoading(prev => {
            if (prev.has(species)) {
                return prev;
            }
            return new Set(prev).add(species);
        });

        try {
            const imageUrl = await wikipediaService.getAnimalImageUrl(species);
            const cacheEntry = {
                url: imageUrl,
                lastAccessed: Date.now()
            };

            setImageCache(prev => {
                const newCache = { ...prev, [species]: cacheEntry };

                if (Object.keys(newCache).length > MAX_CACHE_SIZE) {
                    setTimeout(() => scheduleCleanup(), 0);
                }

                return newCache;
            });
        } catch (error) {
            console.error(`Error fetching image for ${species}:`, error);
            setImageCache(prev => ({
                ...prev,
                [species]: {
                    url: null,
                    lastAccessed: Date.now()
                }
            }));
        } finally {
            setLoading(prev => {
                const newSet = new Set(prev);
                newSet.delete(species);
                return newSet;
            });
        }
    }, [scheduleCleanup]); // Only depend on scheduleCleanup

    const processedSpeciesRef = useRef<Set<string>>(new Set());

    useEffect(() => {
        const newSpeciesToProcess = speciesList.filter(species =>
            species && !processedSpeciesRef.current.has(species)
        );

        if (newSpeciesToProcess.length === 0) {
            return;
        }

        newSpeciesToProcess.forEach(species => {
            processedSpeciesRef.current.add(species);
            fetchAnimalImage(species);
        });
    }, [speciesList, fetchAnimalImage]);

    useEffect(() => {
        return () => {
            if (cleanupTimeoutRef.current) {
                clearTimeout(cleanupTimeoutRef.current);
            }
        };
    }, []);

    const getAnimalImage = useCallback((species: string): string | null => {
        const cacheEntry = imageCache[species];
        return cacheEntry ? cacheEntry.url : null;
    }, [imageCache]);

    const markImageAccessed = useCallback((species: string) => {
        setImageCache(prev => {
            if (prev[species]) {
                return {
                    ...prev,
                    [species]: {
                        ...prev[species],
                        lastAccessed: Date.now()
                    }
                };
            }
            return prev;
        });
    }, []); // No dependencies

    const isLoadingImage = useCallback((species: string): boolean => {
        return loading.has(species);
    }, [loading]);

    const isValidSpeciesForImage = useCallback((species: string): boolean => {
        return isValidSpecies(species);
    }, []);

    const getCacheStats = useCallback(() => {
        return (currentCache: AnimalImageCache) => {
            const entries = Object.entries(currentCache);
            return {
                totalEntries: entries.length,
                successfulEntries: entries.filter(([, data]) => data.url !== null).length,
                failedEntries: entries.filter(([, data]) => data.url === null).length,
                invalidEntries: entries.filter(([species]) => !isValidSpecies(species)).length,
                oldestEntry: entries.reduce((oldest, [species, data]) =>
                    !oldest || data.lastAccessed < oldest.lastAccessed
                        ? { species, lastAccessed: data.lastAccessed }
                        : oldest
                    , null as { species: string; lastAccessed: number } | null)
            };
        };
    }, []);

    return {
        getAnimalImage,
        markImageAccessed,
        isLoadingImage,
        isValidSpeciesForImage,
        imageCache: Object.fromEntries(
            Object.entries(imageCache).map(([species, data]) => [species, data.url])
        ),
        getCacheStats: getCacheStats()(imageCache),
    };
};