export interface WikipediaImage {
    url: string;
    title: string;
}

export interface WikipediaSearchResult {
    title: string;
    extract: string;
    thumbnail?: {
        source: string;
        width: number;
        height: number;
    };
    originalimage?: {
        source: string;
        width: number;
        height: number;
    };
}

const WIKIPEDIA_API_BASE = 'https://en.wikipedia.org/api/rest_v1';

// Cache for search results to avoid repeated API calls
const searchCache = new Map<string, WikipediaSearchResult | null>();

export const wikipediaService = {
    // Search for a bird species and get basic info with image
    searchBirdSpecies: async (speciesName: string): Promise<WikipediaSearchResult | null> => {
        // Check cache first
        if (searchCache.has(speciesName)) {
            return searchCache.get(speciesName) || null;
        }

        try {
            // Clean the species name (remove common suffixes that might interfere)
            const cleanedName = speciesName
                .replace(/\s+bird$/i, '') // Remove "bird" if already present
                .replace(/\s+species$/i, '') // Remove "species" if present
                .trim();

            // Try multiple search strategies in order of preference
            const searchStrategies = [
                cleanedName, // Try exact species name first
                `${cleanedName} bird`, // Then with "bird" appended
                cleanedName.split(' ')[0], // Try just the genus name as fallback
            ];

            for (const searchTerm of searchStrategies) {
                try {
                    const response = await fetch(
                        `${WIKIPEDIA_API_BASE}/page/summary/${encodeURIComponent(searchTerm)}`,
                        {
                            headers: {
                                'Accept': 'application/json',
                            }
                        }
                    );

                    if (response.ok) {
                        const result = await response.json();

                        // Check if this looks like a bird-related page
                        const isLikelyBirdPage = (
                            result.extract?.toLowerCase().includes('bird') ||
                            result.extract?.toLowerCase().includes('species') ||
                            result.title?.toLowerCase().includes('bird') ||
                            result.title?.toLowerCase().includes(cleanedName.toLowerCase())
                        );

                        if (isLikelyBirdPage && (result.thumbnail || result.originalimage)) {
                            searchCache.set(speciesName, result);
                            return result;
                        }
                    }
                } catch (error) {
                    // Continue to next strategy
                    continue;
                }
            }

            // If no results found, cache null to avoid repeated requests
            searchCache.set(speciesName, null);
            return null;

        } catch (error) {
            console.error('Error fetching Wikipedia data:', error);
            searchCache.set(speciesName, null);
            return null;
        }
    },

    // Get bird image URL with fallbacks
    getBirdImageUrl: async (speciesName: string): Promise<string | null> => {
        try {
            const result = await wikipediaService.searchBirdSpecies(speciesName);

            if (result?.thumbnail?.source) {
                return result.thumbnail.source;
            }

            if (result?.originalimage?.source) {
                return result.originalimage.source;
            }

            return null;
        } catch (error) {
            console.error('Error fetching bird image:', error);
            return null;
        }
    },

    // Clear cache (useful for debugging or if you want fresh data)
    clearCache: () => {
        searchCache.clear();
    }
};