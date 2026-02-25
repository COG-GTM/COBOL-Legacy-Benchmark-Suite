import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import MenuPage from "./pages/MenuPage";
import PortfolioPage from "./pages/PortfolioPage";
import HistoryPage from "./pages/HistoryPage";
import ErrorPage from "./pages/ErrorPage";
import type { ReactNode } from "react";

/**
 * Protected route wrapper — replaces SECMGR authorization checks.
 * Unauthenticated users are redirected to /login (maps to SEC-AUTHORIZE).
 */
function ProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

/**
 * Main App — replaces INQONLN.cbl main controller/router.
 * Routes map to EVALUATE WS-COMMAREA-FUNCTION branches:
 *   /menu      → WHEN 'MENU' (P200-DISPLAY-MENU)
 *   /portfolio → WHEN 'INQP' (P300-PORTFOLIO-INQUIRY → INQPORT)
 *   /history   → WHEN 'INQH' (P400-HISTORY-INQUIRY → INQHIST)
 *   /error     → WHEN OTHER  (P900-ERROR-ROUTINE → ERRMAP)
 */
function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/menu"
        element={
          <ProtectedRoute>
            <MenuPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/portfolio"
        element={
          <ProtectedRoute>
            <PortfolioPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/history"
        element={
          <ProtectedRoute>
            <HistoryPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/error"
        element={
          <ProtectedRoute>
            <ErrorPage />
          </ProtectedRoute>
        }
      />
      <Route path="/" element={<Navigate to="/menu" replace />} />
      <Route path="*" element={<Navigate to="/menu" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
