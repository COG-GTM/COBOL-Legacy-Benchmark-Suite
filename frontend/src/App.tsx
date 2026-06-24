import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { PortfolioListPage } from './features/portfolios/PortfolioListPage';
import { PortfolioDetailPage } from './features/portfolios/PortfolioDetailPage';
import { PortfolioFormPage } from './features/portfolios/PortfolioFormPage';

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
        <Route path="*" element={<Navigate to="/portfolios" replace />} />
      </Route>
    </Routes>
  );
}
