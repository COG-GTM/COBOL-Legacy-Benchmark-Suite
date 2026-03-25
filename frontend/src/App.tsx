import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import { ToastProvider } from '@/context/ToastContext';
import { ErrorBoundary } from '@/components/shared/ErrorBoundary';
import { ToastContainer } from '@/components/shared/ToastContainer';
import { AppLayout } from '@/components/layout/AppLayout';
import { LoginPage } from '@/pages/LoginPage';
import { DashboardPage } from '@/pages/DashboardPage';
import { PortfolioPage } from '@/pages/PortfolioPage';
import { TransactionsPage } from '@/pages/TransactionsPage';
import { ErrorPage } from '@/pages/ErrorPage';

export default function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<AppLayout />}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/portfolio" element={<PortfolioPage />} />
                <Route path="/transactions" element={<TransactionsPage />} />
                <Route path="/error" element={<ErrorPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/error" replace />} />
            </Routes>
          </BrowserRouter>
          <ToastContainer />
        </ToastProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}
