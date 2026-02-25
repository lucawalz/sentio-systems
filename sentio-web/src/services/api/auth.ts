// src/services/api/auth.ts
// Handles all authentication-related API requests.

export const authService = {
    validateResetToken: async (token: string) => {
        const response = await fetch(`/api/auth/validate-reset-token?token=${encodeURIComponent(token)}`)
        return response.json()
    },

    resetPassword: async (data: Record<string, string>) => {
        const response = await fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        })
        return response.json()
    },

    forgotPassword: async (email: string) => {
        return fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email }),
        })
    },

    verifyEmail: async (token: string) => {
        const response = await fetch(`/api/auth/verify-email?token=${encodeURIComponent(token)}`)
        return response.json()
    },

    resendVerification: async (email: string) => {
        return fetch('/api/auth/resend-verification', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email }),
        })
    }
}
