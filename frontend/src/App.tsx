import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ToastProvider } from './context/ToastContext';
import { PortfolioProvider } from './context/PortfolioContext';
import ErrorBoundary from './components/ErrorBoundary';
import ToastContainer from './components/ToastContainer';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';

import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import PortfolioList from './pages/PortfolioList';
import PortfolioDetail from './pages/PortfolioDetail';
import PortfolioCreate from './pages/PortfolioCreate';
import PortfolioEdit from './pages/PortfolioEdit';
import PositionInquiry from './pages/PositionInquiry';
import TransactionHistory from './pages/TransactionHistory';
import TransactionEntry from './pages/TransactionEntry';
import ReportsIndex from './pages/reports/ReportsIndex';
import PositionReport from './pages/reports/PositionReport';
import AuditReport from './pages/reports/AuditReport';
import StatisticsReport from './pages/reports/StatisticsReport';
import ErrorPage from './pages/ErrorPage';

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <AuthProvider>
          <ToastProvider>
            <PortfolioProvider>
              <ToastContainer />
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route element={<ProtectedRoute />}>
                  <Route element={<Layout />}>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/portfolios" element={<PortfolioList />} />
                    <Route path="/portfolios/new" element={<PortfolioCreate />} />
                    <Route path="/portfolios/:id" element={<PortfolioDetail />} />
                    <Route path="/portfolios/:id/edit" element={<PortfolioEdit />} />
                    <Route path="/positions" element={<PositionInquiry />} />
                    <Route path="/history" element={<TransactionHistory />} />
                    <Route path="/transactions/new" element={<TransactionEntry />} />
                    <Route path="/reports" element={<ReportsIndex />} />
                    <Route path="/reports/position" element={<PositionReport />} />
                    <Route path="/reports/audit" element={<AuditReport />} />
                    <Route path="/reports/statistics" element={<StatisticsReport />} />
                    <Route path="/error" element={<ErrorPage />} />
                  </Route>
                </Route>
              </Routes>
            </PortfolioProvider>
          </ToastProvider>
        </AuthProvider>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
