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

const searchCache = new Map<string, WikipediaSearchResult | null>();

export const wikipediaService = {
    searchAnimalSpecies: async (speciesName: string): Promise<WikipediaSearchResult | null> => {
        // Check cache first
        if (searchCache.has(speciesName)) {
            return searchCache.get(speciesName) || null;
        }

        try {
            const cleanedName = speciesName
                .replace(/\s+bird$/i, '')
                .replace(/\s+animal$/i, '')
                .replace(/\s+species$/i, '')
                .trim();


            const searchStrategies = [
                cleanedName,
                `${cleanedName} animal`,
                `${cleanedName} bird`,
                `${cleanedName} mammal`,
                cleanedName.split(' ')[0],
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

                        const isLikelyAnimalPage = (
                            result.extract?.toLowerCase().includes('animal') ||
                            result.extract?.toLowerCase().includes('bird') ||
                            result.extract?.toLowerCase().includes('mammal') ||
                            result.extract?.toLowerCase().includes('species') ||
                            result.title?.toLowerCase().includes('animal') ||
                            result.title?.toLowerCase().includes(cleanedName.toLowerCase())
                        );

                        if (isLikelyAnimalPage && (result.thumbnail || result.originalimage)) {
                            searchCache.set(speciesName, result);
                            return result;
                        }
                    }
                } catch (error) {
                    continue;
                }
            }

            searchCache.set(speciesName, null);
            return null;

        } catch (error) {
            console.error('Error fetching Wikipedia data:', error);
            searchCache.set(speciesName, null);
            return null;
        }
    },

    getAnimalImageUrl: async (speciesName: string): Promise<string | null> => {
        try {
            const result = await wikipediaService.searchAnimalSpecies(speciesName);

            if (result?.thumbnail?.source) {
                return result.thumbnail.source;
            }

            if (result?.originalimage?.source) {
                return result.originalimage.source;
            }

            return null;
        } catch (error) {
            console.error('Error fetching animal image:', error);
            return null;
        }
    },

    // Clear cache (useful for debugging or if you want fresh data)
    clearCache: () => {
        searchCache.clear();
    }
};