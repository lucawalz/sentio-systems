import axios from 'axios'

export function getAuthErrorMessage(error: unknown, context: 'login' | 'register' = 'login'): string {
    if (axios.isAxiosError(error)) {
        const status = error.response?.status
        const data = error.response?.data

        switch (status) {
            case 400:
                return data?.message || 'Invalid request. Please check your input.'
            case 401:
                return context === 'login' ? 'Invalid username or password' : 'Unauthorized'
            case 403:
                return 'Your account has been locked. Please contact support.'
            case 404:
                return context === 'login' ? 'Account not found. Please check your username or sign up.' : 'Not found.'
            case 409:
                if (data?.message?.toLowerCase().includes('username')) {
                    return 'This username is already taken. Please choose a different one.'
                }
                if (data?.message?.toLowerCase().includes('email')) {
                    return 'An account with this email already exists.'
                }
                return 'An account with this username or email already exists.'
            case 422:
                return data?.message || 'Validation failed. Please check your input.'
            case 429:
                return 'Too many attempts. Please wait a moment and try again.'
            case 500:
                return 'Server error. Please try again later.'
            case 502:
            case 503:
            case 504:
                return 'Service temporarily unavailable. Please try again later.'
            default:
                return data?.message || `${context === 'login' ? 'Login' : 'Registration'} failed. Please try again.`
        }
    }

    if (error instanceof Error) {
        if (error.message.includes('Network Error')) {
            return 'Unable to connect to the server. Please check your internet connection.'
        }
        return error.message
    }

    return 'An unexpected error occurred. Please try again.'
}
