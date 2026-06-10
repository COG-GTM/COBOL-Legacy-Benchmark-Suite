import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from '@/context/AuthContext';
import { PortfolioProvider } from '@/context/PortfolioContext';
import { ErrorProvider } from '@/context/ErrorContext';
import { AppLayout } from '@/components/layout/AppLayout';
import { LoginPage } from '@/pages/login/LoginPage';
import { DashboardPage } from '@/pages/dashboard/DashboardPage';
import { PortfolioListPage } from '@/pages/portfolios/PortfolioListPage';
import { PortfolioNewPage } from '@/pages/portfolios/PortfolioNewPage';
import { PortfolioDetailPage } from '@/pages/portfolios/PortfolioDetailPage';
import { PortfolioEditPage } from '@/pages/portfolios/PortfolioEditPage';
import { TransactionListPage } from '@/pages/transactions/TransactionListPage';
import { TransactionNewPage } from '@/pages/transactions/TransactionNewPage';
import { PositionReportPage } from '@/pages/reports/PositionReportPage';
import { AuditReportPage } from '@/pages/reports/AuditReportPage';
import { StatisticsReportPage } from '@/pages/reports/StatisticsReportPage';
import { BatchMonitorPage } from '@/pages/batch/BatchMonitorPage';
import { ErrorLogPage } from '@/pages/errors/ErrorLogPage';
import { PositionInquiryPage } from '@/pages/positions/PositionInquiryPage';

function App() {
  return (
    <ErrorProvider>
      <AuthProvider>
        <PortfolioProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<AppLayout />}>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/positions" element={<PositionInquiryPage />} />
                <Route path="/portfolios" element={<PortfolioListPage />} />
                <Route path="/portfolios/new" element={<PortfolioNewPage />} />
                <Route path="/portfolios/:id" element={<PortfolioDetailPage />} />
                <Route path="/portfolios/:id/edit" element={<PortfolioEditPage />} />
                <Route path="/transactions" element={<TransactionListPage />} />
                <Route path="/transactions/new" element={<TransactionNewPage />} />
                <Route path="/reports/positions" element={<PositionReportPage />} />
                <Route path="/reports/audit" element={<AuditReportPage />} />
                <Route path="/reports/statistics" element={<StatisticsReportPage />} />
                <Route path="/batch" element={<BatchMonitorPage />} />
                <Route path="/errors" element={<ErrorLogPage />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </PortfolioProvider>
      </AuthProvider>
    </ErrorProvider>
  );
}

export default App;
