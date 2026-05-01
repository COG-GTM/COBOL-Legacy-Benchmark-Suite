import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Portfolios from './pages/Portfolios';
import PortfolioDetail from './pages/PortfolioDetail';
import PortfolioInquiry from './pages/PortfolioInquiry';
import TransactionHistory from './pages/TransactionHistory';
import BatchOperations from './pages/BatchOperations';
import Reports from './pages/Reports';
import SystemMonitor from './pages/SystemMonitor';

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="portfolios" element={<Portfolios />} />
        <Route path="portfolios/:id" element={<PortfolioDetail />} />
        <Route path="inquiry" element={<PortfolioInquiry />} />
        <Route path="history" element={<TransactionHistory />} />
        <Route path="batch" element={<BatchOperations />} />
        <Route path="reports" element={<Reports />} />
        <Route path="monitor" element={<SystemMonitor />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
