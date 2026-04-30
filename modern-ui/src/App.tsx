import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Portfolios from './pages/Portfolios';
import Transactions from './pages/Transactions';
import Positions from './pages/Positions';
import Reports from './pages/Reports';
import BatchJobs from './pages/BatchJobs';
import AuditLog from './pages/AuditLog';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/portfolios" element={<Portfolios />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/positions" element={<Positions />} />
          <Route path="/reports" element={<Reports />} />
          <Route path="/batch-jobs" element={<BatchJobs />} />
          <Route path="/audit-log" element={<AuditLog />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
