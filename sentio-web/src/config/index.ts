/**
 * Application configuration
 * 
 * All environment variables and runtime config are centralized here.
 */

export const config = {
    app: {
        name: 'Sentio',
        version: import.meta.env.VITE_APP_VERSION || '0.0.0',
        environment: import.meta.env.MODE,
        isDev: import.meta.env.DEV,
        isProd: import.meta.env.PROD,
    },
    api: {
        baseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
        timeout: 30000,
    },
    features: {
        // Feature flags
        enableAnalytics: import.meta.env.VITE_ENABLE_ANALYTICS === 'true',
    },
} as const;

export type Config = typeof config;
