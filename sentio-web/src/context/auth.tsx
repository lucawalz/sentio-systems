import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import { authService } from "../services/authService";
import type { RegisterRequest, UserInfo } from "../services/authService";

// Shape of the auth context value that components will consume.
type AuthContextValue = {
  user: UserInfo | null;
  loggedIn: boolean;
  isLoading: boolean;
  error: string | null;
  login: (username?: string, password?: string) => Promise<void>;
  register: (data: RegisterRequest) => Promise<boolean>;
  logout: () => Promise<void>;
  clearError: () => void;
};

// Create the React context. The initial value is `undefined` so that
// `useAuth()` can throw a helpful error if used outside the provider.
const AuthContext = createContext<AuthContextValue | undefined>(undefined);

// AuthProvider wraps the app and exposes auth state and methods.
// It checks for existing session on mount via the /me endpoint.
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [isLoading, setIsLoading] = useState(true); // Start true to check existing session
  const [error, setError] = useState<string | null>(null);

  // On mount, check if user is already authenticated via cookie
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const currentUser = await authService.getCurrentUser();
        setUser(currentUser);
      } catch {
        // Not authenticated, that's fine
        setUser(null);
      } finally {
        setIsLoading(false);
      }
    };
    checkAuth();
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  const login = useCallback(async (username?: string, password?: string) => {
    // If args provided, perform native login. Otherwise do nothing (or could redirect if we kept that).
    if (username && password) {
      setIsLoading(true);
      setError(null);
      try {
        await authService.login({ username, password });
        // After successful login, fetch user info to update state
        const currentUser = await authService.getCurrentUser();
        setUser(currentUser);
      } catch (err: any) {
        throw err; // Re-throw so component can handle error
      } finally {
        setIsLoading(false);
      }
    }
  }, []);

  const register = useCallback(async (data: RegisterRequest): Promise<boolean> => {
    setIsLoading(true);
    setError(null);

    try {
      await authService.register(data);
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Registration failed';
      setError(message);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    setIsLoading(true);
    try {
      await authService.logout();
    } finally {
      setUser(null);
      setIsLoading(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{
      user,
      loggedIn: !!user,
      isLoading,
      error,
      login,
      register,
      logout,
      clearError
    }}>
      {children}
    </AuthContext.Provider>
  );
};

// Custom hook that components can call to access auth state and helpers.
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}

export default AuthProvider;
