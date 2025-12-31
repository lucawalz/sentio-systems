import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { authApi } from '@/lib/api'
import type { User } from '@/types/api'

interface AuthContextType {
    user: User | null
    isLoading: boolean
    isAuthenticated: boolean
    login: (username: string, password: string) => Promise<void>
    logout: () => Promise<void>
    checkAuth: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function useAuth() {
    const context = useContext(AuthContext)
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider')
    }
    return context
}

interface AuthProviderProps {
    children: ReactNode
}

export function AuthProvider({ children }: AuthProviderProps) {
    const [user, setUser] = useState<User | null>(null)
    const [isLoading, setIsLoading] = useState(true)

    const checkAuth = useCallback(async () => {
        try {
            const response = await authApi.me()
            setUser(response.data)
        } catch {
            setUser(null)
        } finally {
            setIsLoading(false)
        }
    }, [])

    const login = useCallback(async (username: string, password: string) => {
        await authApi.login(username, password)
        // After successful login, fetch user info
        await checkAuth()
    }, [checkAuth])

    const logout = useCallback(async () => {
        try {
            await authApi.logout()
        } finally {
            setUser(null)
        }
    }, [])

    // Check auth on mount
    useEffect(() => {
        checkAuth()
    }, [checkAuth])

    const value: AuthContextType = {
        user,
        isLoading,
        isAuthenticated: !!user,
        login,
        logout,
        checkAuth,
    }

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
