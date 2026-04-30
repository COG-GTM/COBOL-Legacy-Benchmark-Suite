import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import PortfolioInquiry from './pages/PortfolioInquiry';
import TransactionHistory from './pages/TransactionHistory';
import BatchMonitor from './pages/BatchMonitor';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="portfolio" element={<PortfolioInquiry />} />
        <Route path="transactions" element={<TransactionHistory />} />
        <Route path="batch" element={<BatchMonitor />} />
      </Route>
    </Routes>
  );
}
