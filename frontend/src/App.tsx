import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import { NavigationBar, ProtectedRoute } from '@/components/shared';
import { Login } from '@/pages/Login';
import { Dashboard } from '@/pages/Dashboard';
import { PortfolioInquiry } from '@/pages/PortfolioInquiry';
import { TransactionHistory } from '@/pages/TransactionHistory';
import { PortfolioManagement } from '@/pages/PortfolioManagement';
import { PortfolioForm } from '@/pages/PortfolioForm';
import { TransactionEntry } from '@/pages/TransactionEntry';
import { Reports } from '@/pages/Reports';
import { PositionReport } from '@/pages/PositionReport';
import { AuditReport } from '@/pages/AuditReport';
import { StatisticsReport } from '@/pages/StatisticsReport';
import { BatchStatus } from '@/pages/BatchStatus';

function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-surface">
      <NavigationBar />
      {children}
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <AppLayout><Dashboard /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/portfolio-inquiry"
            element={
              <ProtectedRoute>
                <AppLayout><PortfolioInquiry /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/transaction-history"
            element={
              <ProtectedRoute>
                <AppLayout><TransactionHistory /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/portfolio-management"
            element={
              <ProtectedRoute>
                <AppLayout><PortfolioManagement /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/portfolio-management/new"
            element={
              <ProtectedRoute>
                <AppLayout><PortfolioForm /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/portfolio-management/:id"
            element={
              <ProtectedRoute>
                <AppLayout><PortfolioForm /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/transaction-processing"
            element={
              <ProtectedRoute>
                <AppLayout><TransactionEntry /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/reports"
            element={
              <ProtectedRoute>
                <AppLayout><Reports /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/reports/position"
            element={
              <ProtectedRoute>
                <AppLayout><PositionReport /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/reports/audit"
            element={
              <ProtectedRoute>
                <AppLayout><AuditReport /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/reports/statistics"
            element={
              <ProtectedRoute>
                <AppLayout><StatisticsReport /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/batch-status"
            element={
              <ProtectedRoute>
                <AppLayout><BatchStatus /></AppLayout>
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
