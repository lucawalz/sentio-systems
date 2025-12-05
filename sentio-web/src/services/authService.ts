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
    async login(credentials: LoginRequest): Promise<UserInfo> {
        const response = await fetch(`${API_BASE}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // Critical: sends/receives cookies
            body: JSON.stringify(credentials),
        });

        if (!response.ok) {
            if (response.status === 401) {
                throw new AuthServiceError('Invalid username or password', 401);
            }
            throw new AuthServiceError('Login failed', response.status);
        }

        return response.json();
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
            if (response.status === 409) {
                throw new AuthServiceError('Username or email already exists', 409);
            }
            throw new AuthServiceError('Registration failed', response.status);
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
     */
    async getCurrentUser(): Promise<UserInfo | null> {
        try {
            const response = await fetch(`${API_BASE}/api/auth/me`, {
                credentials: 'include',
            });

            if (!response.ok) {
                return null;
            }

            return response.json();
        } catch {
            return null;
        }
    },
};

export { AuthServiceError };
