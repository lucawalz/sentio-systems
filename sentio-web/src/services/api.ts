
// Base API configuration
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// Create axios-like configuration for fetch
const apiConfig = {
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
};

// Generic API request function
export const apiRequest = async <T>(
    endpoint: string,
    options: RequestInit = {}
): Promise<T> => {
    const url = `${apiConfig.baseURL}${endpoint}`;

    const config: RequestInit = {
        ...options,
        headers: {
            ...apiConfig.headers,
            ...options.headers,
        },
    };

    try {
        const response = await fetch(url, config);

        if (!response.ok) {
            throw new Error(`API Error: ${response.status} - ${response.statusText}`);
        }

        // Check if response has content
        const contentType = response.headers.get('content-type');
        const contentLength = response.headers.get('content-length');

        // Handle empty responses or non-JSON responses
        if (contentLength === '0' || !contentType?.includes('application/json')) {
            if (response.status === 204) {
                // No content response is expected
                return null as T;
            }
            throw new Error('No data available - database might be empty');
        }

        const text = await response.text();

        // Handle empty response body
        if (!text || text.trim() === '') {
            throw new Error('No data available - database might be empty');
        }

        try {
            return JSON.parse(text);
        } catch (parseError) {
            console.error('JSON Parse Error:', parseError);
            console.error('Response text:', text);
            throw new Error('Invalid response format from server');
        }

    } catch (error) {
        if (error instanceof TypeError && error.message.includes('fetch')) {
            throw new Error('Cannot connect to server - check if backend is running');
        }
        console.error('API Request failed:', error);
        throw error;
    }
};

// GET request helper
export const get = <T>(endpoint: string, options?: RequestInit): Promise<T> =>
    apiRequest<T>(endpoint, { ...options, method: 'GET' });

// POST request helper
export const post = <T>(
    endpoint: string,
    data?: any,
    options?: RequestInit
): Promise<T> =>
    apiRequest<T>(endpoint, {
        ...options,
        method: 'POST',
        body: data ? JSON.stringify(data) : undefined,
    });

// PUT request helper
export const put = <T>(
    endpoint: string,
    data?: any,
    options?: RequestInit
): Promise<T> =>
    apiRequest<T>(endpoint, {
        ...options,
        method: 'PUT',
        body: data ? JSON.stringify(data) : undefined,
    });

// DELETE request helper
export const del = <T>(endpoint: string, options?: RequestInit): Promise<T> =>
    apiRequest<T>(endpoint, { ...options, method: 'DELETE' });

export { API_BASE_URL };