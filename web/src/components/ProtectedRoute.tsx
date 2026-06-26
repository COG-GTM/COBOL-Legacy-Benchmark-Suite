import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { UserRole } from '../types/auth';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
  children: ReactNode;
  /** When set, the user must hold this role or access is denied. */
  requiredRole?: UserRole;
}

/**
 * Gates a route behind authentication and (optionally) a required role.
 * Mirrors the legacy SECMGR access-control flow: validate the user, then
 * authorize the requested resource (P100/P200 in SECMGR.cbl).
 */
export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
}
