import { createContext, useContext, useState, useCallback } from 'react';
import type { ReactNode } from 'react';

interface User {
  username: string;
  displayName: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => boolean;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(() => {
    const stored = sessionStorage.getItem('clbs_user');
    return stored ? (JSON.parse(stored) as User) : null;
  });

  const login = useCallback((username: string, password: string): boolean => {
    if (username === 'admin' && password === 'admin') {
      const newUser: User = {
        username: 'admin',
        displayName: 'System Administrator',
        role: 'admin',
      };
      setUser(newUser);
      sessionStorage.setItem('clbs_user', JSON.stringify(newUser));
      return true;
    }
    if (username && password === 'password') {
      const newUser: User = {
        username,
        displayName: username.charAt(0).toUpperCase() + username.slice(1),
        role: 'user',
      };
      setUser(newUser);
      sessionStorage.setItem('clbs_user', JSON.stringify(newUser));
      return true;
    }
    return false;
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    sessionStorage.removeItem('clbs_user');
  }, []);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated: !!user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
