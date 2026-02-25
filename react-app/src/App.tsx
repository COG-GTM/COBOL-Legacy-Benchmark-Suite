/**
 * App — main router setup.
 * Replaces the EVALUATE WS-COMMAREA-FUNCTION routing logic
 * in INQONLN.cbl (lines 62-77).
 *
 * Routes:
 *   /login     → LoginPage
 *   /          → MenuPage (redirect)
 *   /menu      → MenuPage
 *   /portfolio → PortfolioPage
 *   /history   → HistoryPage
 *   /error     → ErrorPage
 */

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, ProtectedRoute } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import MenuPage from "./pages/MenuPage";
import PortfolioPage from "./pages/PortfolioPage";
import HistoryPage from "./pages/HistoryPage";
import ErrorPage from "./pages/ErrorPage";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Navigate to="/menu" replace />
              </ProtectedRoute>
            }
          />
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

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/menu" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
