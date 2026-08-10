import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { PortfolioListPage } from './features/portfolios/PortfolioListPage';
import { PortfolioDetailPage } from './features/portfolios/PortfolioDetailPage';
import { PortfolioFormPage } from './features/portfolios/PortfolioFormPage';
import { PositionInquiryPage } from './features/positions/PositionInquiryPage';
import { TransactionFormPage } from './features/transactions/TransactionFormPage';
import { TransactionListPage } from './features/transactions/TransactionListPage';

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
        <Route path="transactions" element={<TransactionListPage />} />
        <Route path="transactions/new" element={<TransactionFormPage />} />
        <Route path="*" element={<Navigate to="/portfolios" replace />} />
      </Route>
    </Routes>
  );
}
