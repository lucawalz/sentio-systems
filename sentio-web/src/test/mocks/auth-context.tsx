import { vi } from 'vitest';
import { ReactNode } from 'react';

export const mockLogin = vi.fn();
export const mockLogout = vi.fn();
export const mockRegister = vi.fn();

export const mockAuthContext = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  login: mockLogin,
  logout: mockLogout,
  register: mockRegister,
};

export const AuthContext = {
  Provider: ({ children }: { children: ReactNode }) => children,
};

export const useAuth = () => mockAuthContext;
