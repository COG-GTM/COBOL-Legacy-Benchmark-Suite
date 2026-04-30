import { Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Portfolios from './pages/Portfolios';
import Transactions from './pages/Transactions';
import Reports from './pages/Reports';
import Admin from './pages/Admin';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Dashboard />} />
        <Route path="portfolios" element={<Portfolios />} />
        <Route path="transactions" element={<Transactions />} />
        <Route path="reports" element={<Reports />} />
        <Route path="admin" element={<Admin />} />
      </Route>
    </Routes>
  );
}
