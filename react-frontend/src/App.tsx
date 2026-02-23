/**
 * App Component - Replaces INQONLN (Main Online Controller)
 *
 * INQONLN managed the overall flow:
 * - P100-PROCESS-REQUEST: Main loop processing user input
 * - EVALUATE WS-COMMAREA-FUNCTION: Route to appropriate screen
 * - P050-SECURITY-CHECK: Validate user before processing
 *
 * This App component uses React Router to replicate the CICS
 * screen navigation that was previously managed through
 * CICS SEND MAP / RECEIVE MAP calls.
 */

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ErrorProvider } from './context/ErrorContext';
import { NavigationProvider } from './context/NavigationContext';
import LoginScreen from './components/LoginScreen';
import MainMenu from './components/MainMenu';
import PortfolioInquiry from './components/PortfolioInquiry';
import TransactionHistory from './components/TransactionHistory';
import ErrorDisplay from './components/ErrorDisplay';
import './App.css';

/**
 * ProtectedRoute - Replaces SECMGR authorization check
 * Mirrors INQONLN P050-SECURITY-CHECK:
 * IF SEC-RESPONSE-CODE NOT = 0 → redirect to login
 */
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { auth } = useAuth();

  if (!auth.isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

function AppRoutes() {
  return (
    <Routes>
      {/* Login - replaces CICS terminal sign-on */}
      <Route path="/" element={<LoginScreen />} />

      {/* Main Menu - replaces MENMAP via INQONLN P200-DISPLAY-MENU */}
      <Route
        path="/menu"
        element={
          <ProtectedRoute>
            <MainMenu />
          </ProtectedRoute>
        }
      />

      {/* Portfolio Inquiry - replaces POSMAP via INQPORT */}
      <Route
        path="/portfolio"
        element={
          <ProtectedRoute>
            <PortfolioInquiry />
          </ProtectedRoute>
        }
      />

      {/* Transaction History - replaces HISMAP via INQHIST */}
      <Route
        path="/history"
        element={
          <ProtectedRoute>
            <TransactionHistory />
          </ProtectedRoute>
        }
      />

      {/* Error Display - replaces ERRMAP via ERRHNDL */}
      <Route
        path="/error"
        element={
          <ProtectedRoute>
            <ErrorDisplay />
          </ProtectedRoute>
        }
      />

      {/* Catch-all redirect */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ErrorProvider>
          <NavigationProvider>
            <div className="app-container">
              <AppRoutes />
            </div>
          </NavigationProvider>
        </ErrorProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
