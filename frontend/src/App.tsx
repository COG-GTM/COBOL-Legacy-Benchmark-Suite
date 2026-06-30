import { Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { PortfoliosPage } from './features/portfolios/PortfoliosPage';
import { TransactionsPage } from './features/transactions/TransactionsPage';
import { HistoryPage } from './features/history/HistoryPage';
import { ReportsPage } from './features/reports/ReportsPage';
import { NotFoundPage } from './features/NotFoundPage';

export function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="portfolios" element={<PortfoliosPage />} />
        <Route path="transactions" element={<TransactionsPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
