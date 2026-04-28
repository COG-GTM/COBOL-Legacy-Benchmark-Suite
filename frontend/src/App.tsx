import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './components/Toast';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';
import { ErrorBoundary } from './components/ErrorBoundary';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { PositionInquiryPage } from './pages/PositionInquiryPage';
import { TransactionHistoryPage } from './pages/TransactionHistoryPage';
import { PortfolioManagementPage } from './pages/PortfolioManagementPage';
import { PortfolioCreatePage } from './pages/PortfolioCreatePage';
import { PortfolioDetailPage } from './pages/PortfolioDetailPage';
import { PortfolioEditPage } from './pages/PortfolioEditPage';
import { TransactionEntryPage } from './pages/TransactionEntryPage';
import { ReportsPage } from './pages/ReportsPage';
import { ValuationReportPage } from './pages/ValuationReportPage';
import { AuditReportPage } from './pages/AuditReportPage';
import { SystemStatsPage } from './pages/SystemStatsPage';

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<ProtectedRoute />}>
                <Route element={<Layout />}>
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/positions" element={<PositionInquiryPage />} />
                  <Route path="/history" element={<TransactionHistoryPage />} />
                  <Route path="/portfolios" element={<PortfolioManagementPage />} />
                  <Route path="/portfolios/new" element={<PortfolioCreatePage />} />
                  <Route path="/portfolios/:id" element={<PortfolioDetailPage />} />
                  <Route path="/portfolios/:id/edit" element={<PortfolioEditPage />} />
                  <Route path="/transactions/new" element={<TransactionEntryPage />} />
                  <Route path="/reports" element={<ReportsPage />} />
                  <Route path="/reports/valuation" element={<ValuationReportPage />} />
                  <Route path="/reports/audit" element={<AuditReportPage />} />
                  <Route path="/reports/system" element={<SystemStatsPage />} />
                </Route>
              </Route>
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
