/**
 * Authentication Service
 * 
 * Handles all authentication API calls to the backend.
 * Uses credentials: 'include' to send/receive httpOnly cookies.
 */

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8083';

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
    email: string;
    firstName: string;
    lastName: string;
}

export interface UserInfo {
    username: string;
    email?: string;
}

class AuthServiceError extends Error {
    statusCode?: number;

    constructor(message: string, statusCode?: number) {
        super(message);
        this.name = 'AuthServiceError';
        this.statusCode = statusCode;
    }
}

export const authService = {
    /**
     * Authenticate user with username and password.
     * On success, cookies are automatically set by the browser.
     */
    /**
     * Authenticate user with username and password.
     * On success, cookies are automatically set by the browser.
     */
    async login(data: LoginRequest): Promise<void> {
        const response = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            let message = 'Login failed';
            if (response.status === 401) {
                message = 'Invalid credentials';
            }
            throw new AuthServiceError(message, response.status);
        }
    },

    /**
     * Register a new user account.
     */
    async register(data: RegisterRequest): Promise<void> {
        const response = await fetch(`${API_BASE}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            let errorMessage = 'Registration failed';
            try {
                const errorData = await response.json();
                if (errorData.message) {
                    errorMessage = errorData.message;
                }
            } catch (e) {
                // Ignore parsing error, use default message
            }

            throw new AuthServiceError(errorMessage, response.status);
        }
    },

    /**
     * Logout the current user.
     * Clears auth cookies on the backend.
     */
    async logout(): Promise<void> {
        try {
            await fetch(`${API_BASE}/api/auth/logout`, {
                method: 'POST',
                credentials: 'include',
            });
        } catch {
            // Even if the request fails, we consider the user logged out client-side
            console.warn('Logout request failed, but proceeding with local logout');
        }
    },

    /**
     * Check if the user is currently authenticated.
     * Returns user info if authenticated, null otherwise.
     * Automatically attempts to refresh token if access token is expired.
     */
    async getCurrentUser(): Promise<UserInfo | null> {
        try {
            let response = await fetch(`${API_BASE}/api/auth/me`, {
                credentials: 'include',
            });

            if (response.status === 401) {
                // Access token expired, try to refresh
                const refreshSuccess = await this.refreshToken();
                if (refreshSuccess) {
                    // Retry the request
                    response = await fetch(`${API_BASE}/api/auth/me`, {
                        credentials: 'include',
                    });
                }
            }

            if (!response.ok) {
                return null;
            }

            return response.json();
        } catch {
            return null;
        }
    },

    /**
     * Attempt to refresh the access token using the httpOnly refresh token cookie.
     */
    async refreshToken(): Promise<boolean> {
        try {
            const response = await fetch(`${API_BASE}/api/auth/refresh`, {
                method: 'POST',
                credentials: 'include',
            });
            return response.ok;
        } catch {
            return false;
        }
    }
};

export { AuthServiceError };
