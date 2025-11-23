import React, { createContext, useContext, useEffect, useState } from "react"

// Simple user type for this demo. Expand with email/id/token fields
// when integrating a real auth backend.
type User = {
  name?: string
}

// Shape of the auth context value that components will consume.
// - `user`: the current logged-in user (or null)
// - `loggedIn`: convenience boolean (true when `user` !== null)
// - `login`: function to set the user (can be wired to a real auth flow)
// - `logout`: function to clear the user
type AuthContextValue = {
  user: User | null
  loggedIn: boolean
  login: (user?: User) => void
  logout: () => void
}

// Key used to persist a small amount of auth state to localStorage.
// This is intentionally minimal (only `user`) to make the demo easy to reason about.
// For production, prefer httpOnly cookies for tokens and server-side session checks.
const STORAGE_KEY = "sentio_auth"

// Create the React context. The initial value is `undefined` so that
// `useAuth()` can throw a helpful error if used outside the provider.
const AuthContext = createContext<AuthContextValue | undefined>(undefined)

// AuthProvider wraps the app and exposes `user`, `loggedIn`, `login`, and `logout`.
// It also persists the `user` to localStorage so the UI remains logged-in across reloads.
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Internal state holding the current user object (or null).
  const [user, setUser] = useState<User | null>(null)

  // On mount, try to restore persisted user from localStorage.
  // This is synchronous and lightweight; it only affects the client-side UI.
  useEffect(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        const parsed = JSON.parse(raw)
        // parsed should be an object like { user: { name: '...' } }
        setUser(parsed.user ?? null)
      }
    } catch {
      // ignore JSON parse errors and continue with null user
    }
  }, [])

  // Persist the `user` to localStorage whenever it changes.
  // In a real app you'd store only non-sensitive metadata client-side,
  // and keep tokens in httpOnly cookies or secure storage handled by the server.
  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ user }))
    } catch {
      // ignore storage errors (e.g., private mode restrictions)
    }
  }, [user])

  // `login` is intentionally simple: it sets the user object. Replace this
  // with an async API call that authenticates and then calls `setUser` with
  // the returned user info (and possibly a token handled by the server).
  const login = (u?: User) => {
    setUser(u ?? { name: "User" })
  }

  // Clear local user (and persistent storage via the effect above).
  const logout = () => setUser(null)

  // Provide the context value to consumers.
  return (
    <AuthContext.Provider value={{ user, loggedIn: !!user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// Custom hook that components can call to access auth state and helpers.
// It throws if used outside of the `AuthProvider` which helps catch wiring bugs early.
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider")
  }
  return ctx
}

// Default export kept for convenience, but prefer named `AuthProvider` import.
export default AuthProvider
