// src/services/api/weather.ts
// Handles weather and radar-related API requests.

export const weatherService = {
    fetchRadarData: async () => {
        const response = await fetch('https://api.brightsky.dev/radar?tz=Europe/Berlin')

        if (!response.ok) {
            throw new Error(`Failed to fetch radar data: ${response.status}`)
        }

        return response.json()
    }
}
