import { createContext } from 'react';

export type UserRole = 'read-only' | 'portfolio-manager';

export interface AuthContextType {
  isAuthenticated: boolean;
  userId: string;
  role: UserRole;
  loginTime: number | null;
  login: (userId: string, role?: UserRole) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);
