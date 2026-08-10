import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { PortfolioListPage } from './features/portfolios/PortfolioListPage';
import { PortfolioDetailPage } from './features/portfolios/PortfolioDetailPage';
import { PortfolioFormPage } from './features/portfolios/PortfolioFormPage';
import { PositionInquiryPage } from './features/positions/PositionInquiryPage';
import { AuditReportPage } from './features/reports/AuditReportPage';
import { PositionReportPage } from './features/reports/PositionReportPage';
import { ReportsPage } from './features/reports/ReportsPage';
import { ReturnAnalysisPage } from './features/reports/ReturnAnalysisPage';
import { SystemStatisticsPage } from './features/reports/SystemStatisticsPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/portfolios" replace />} />
        <Route path="portfolios" element={<PortfolioListPage />} />
        <Route
          path="portfolios/new"
          element={<PortfolioFormPage mode="create" />}
        />
        <Route path="portfolios/:id" element={<PortfolioDetailPage />} />
        <Route
          path="portfolios/:id/edit"
          element={<PortfolioFormPage mode="edit" />}
        />
        <Route path="positions" element={<PositionInquiryPage />} />
        <Route path="reports" element={<ReportsPage />}>
          <Route index element={<Navigate to="positions" replace />} />
          <Route path="positions" element={<PositionReportPage />} />
          <Route path="audit" element={<AuditReportPage />} />
          <Route path="statistics" element={<SystemStatisticsPage />} />
          <Route path="returns" element={<ReturnAnalysisPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/portfolios" replace />} />
      </Route>
    </Routes>
  );
}
