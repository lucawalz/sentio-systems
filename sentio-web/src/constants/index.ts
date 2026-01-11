/**
 * Application constants
 * 
 * Static values that don't change based on environment.
 */

export const ROUTES = {
    HOME: '/',
    DASHBOARD: '/dashboard',
    LOGIN: '/login',
    REGISTER: '/register',
    SETTINGS: '/settings',
} as const;

export const BREAKPOINTS = {
    sm: 640,
    md: 768,
    lg: 1024,
    xl: 1280,
    '2xl': 1536,
} as const;

export const ANIMATION = {
    fast: 150,
    normal: 300,
    slow: 500,
} as const;
