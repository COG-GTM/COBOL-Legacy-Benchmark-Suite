import { createBrowserRouter } from 'react-router-dom';
import AppShell from '@/components/layout/AppShell';
import Dashboard from '@/pages/Dashboard';
import PortfolioInquiry from '@/pages/PortfolioInquiry';
import TransactionHistory from '@/pages/TransactionHistory';
import Reports from '@/pages/Reports';
import BatchJobs from '@/pages/BatchJobs';
import SystemMonitor from '@/pages/SystemMonitor';
import NotFound from '@/pages/NotFound';

export const router = createBrowserRouter([
  {
    element: <AppShell />,
    children: [
      { path: '/', element: <Dashboard /> },
      { path: '/portfolio-inquiry', element: <PortfolioInquiry /> },
      { path: '/transaction-history', element: <TransactionHistory /> },
      { path: '/reports', element: <Reports /> },
      { path: '/batch-jobs', element: <BatchJobs /> },
      { path: '/system-monitor', element: <SystemMonitor /> },
      { path: '*', element: <NotFound /> },
    ],
  },
]);
