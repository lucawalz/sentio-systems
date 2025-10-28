
import { useState, useEffect, useRef, useCallback } from 'react';
import { wikipediaService } from '../services/wikipediaService';

interface BirdImageCache {
    [species: string]: {
        url: string | null;
        lastAccessed: number;
    };
}

// Configuration
const MAX_CACHE_SIZE = 50; // Maximum number of images to cache
const CACHE_CLEANUP_THRESHOLD = 40; // When to trigger cleanup (keep most recent 40)

// Species that should not have images fetched
const INVALID_SPECIES = [
    'unknown',
    'unknown bird',
    'bird',
    'unidentified',
    'unidentified bird',
    'no detection',
    'none',
    'n/a',
    'null',
    'undefined'
];

// Helper function to check if a species is valid for image fetching
const isValidSpecies = (species: string): boolean => {
    if (!species || typeof species !== 'string') {
        return false;
    }

    const normalizedSpecies = species.toLowerCase().trim();

    // Check if it's in the invalid list
    if (INVALID_SPECIES.includes(normalizedSpecies)) {
        return false;
    }

    // Check if it's too short or generic
    if (normalizedSpecies.length < 3) {
        return false;
    }

    // Check if it starts with "unknown" or contains "unidentified"
    if (normalizedSpecies.startsWith('unknown') || normalizedSpecies.includes('unidentified')) {
        return false;
    }

    return true;
};

export const useBirdImages = (speciesList: string[]) => {
    const [imageCache, setImageCache] = useState<BirdImageCache>({});
    const [loading, setLoading] = useState<Set<string>>(new Set());
    const cleanupTimeoutRef = useRef<number | null>(null);

    // Clean up old cache entries when cache gets too large
    const cleanupCache = useCallback((currentCache: BirdImageCache) => {
        const cacheEntries = Object.entries(currentCache);

        if (cacheEntries.length <= MAX_CACHE_SIZE) {
            return currentCache;
        }

        // Sort by last accessed time (most recent first)
        const sortedEntries = cacheEntries.sort((a, b) =>
            b[1].lastAccessed - a[1].lastAccessed
        );

        // Keep only the most recently accessed entries
        const keepEntries = sortedEntries.slice(0, CACHE_CLEANUP_THRESHOLD);

        const cleanedCache: BirdImageCache = {};
        keepEntries.forEach(([species, data]) => {
            cleanedCache[species] = data;
        });

        console.log(`Cache cleaned: ${cacheEntries.length} -> ${keepEntries.length} entries`);
        return cleanedCache;
    }, []);

    // Debounced cache cleanup to avoid frequent operations
    const scheduleCleanup = useCallback(() => {
        if (cleanupTimeoutRef.current) {
            clearTimeout(cleanupTimeoutRef.current);
        }

        cleanupTimeoutRef.current = setTimeout(() => {
            setImageCache(currentCache => cleanupCache(currentCache));
        }, 1000); // Wait 1 second before cleaning up
    }, [cleanupCache]);

    // FIXED: Remove dependencies that cause circular updates
    const fetchBirdImage = useCallback(async (species: string) => {
        // Check if species is valid before doing anything
        if (!isValidSpecies(species)) {
            console.log(`Skipping image fetch for invalid species: "${species}"`);
            // Cache the result as null so we don't keep trying
            setImageCache(prev => ({
                ...prev,
                [species]: {
                    url: null,
                    lastAccessed: Date.now()
                }
            }));
            return;
        }

        // Use functional updates to avoid dependencies on current state
        setImageCache(prev => {
            // Check if already cached or loading
            if (prev[species] !== undefined) {
                return prev;
            }
            return prev;
        });

        setLoading(prev => {
            // Check if already loading
            if (prev.has(species)) {
                return prev;
            }
            return new Set(prev).add(species);
        });

        try {
            const imageUrl = await wikipediaService.getBirdImageUrl(species);
            const cacheEntry = {
                url: imageUrl,
                lastAccessed: Date.now()
            };

            setImageCache(prev => {
                const newCache = { ...prev, [species]: cacheEntry };

                // Schedule cleanup if cache is getting large
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

    // FIXED: Use a ref to track processed species to prevent infinite loops
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
            fetchBirdImage(species);
        });
    }, [speciesList, fetchBirdImage]);

    // Cleanup timeout on unmount
    useEffect(() => {
        return () => {
            if (cleanupTimeoutRef.current) {
                clearTimeout(cleanupTimeoutRef.current);
            }
        };
    }, []);

    const getBirdImage = useCallback((species: string): string | null => {
        const cacheEntry = imageCache[species];
        return cacheEntry ? cacheEntry.url : null;
    }, [imageCache]);

    // FIXED: Remove imageCache dependency to prevent circular updates
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

    // Helper function to check if a species is valid (useful for components)
    const isValidSpeciesForImage = useCallback((species: string): boolean => {
        return isValidSpecies(species);
    }, []);

    // FIXED: Remove imageCache dependency
    const getCacheStats = useCallback(() => {
        return (currentCache: BirdImageCache) => {
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
        getBirdImage,
        markImageAccessed,
        isLoadingImage,
        isValidSpeciesForImage,
        imageCache: Object.fromEntries(
            Object.entries(imageCache).map(([species, data]) => [species, data.url])
        ),
        getCacheStats: getCacheStats()(imageCache), // Call the function with current cache
    };
};