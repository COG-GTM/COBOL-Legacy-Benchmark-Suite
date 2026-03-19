import { createBrowserRouter } from 'react-router-dom';
import AppLayout from '@/components/layout/AppLayout';
import SystemMonitorPage from '@/components/monitoring/SystemMonitorPage';
import PlaceholderPage from '@/components/common/PlaceholderPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <PlaceholderPage title="Dashboard" /> },
      { path: 'system-monitor', element: <SystemMonitorPage /> },
      { path: 'portfolio-inquiry', element: <PlaceholderPage title="Portfolio Inquiry" /> },
      { path: 'transaction-history', element: <PlaceholderPage title="Transaction History" /> },
      { path: 'reports', element: <PlaceholderPage title="Reports" /> },
      { path: 'batch-jobs', element: <PlaceholderPage title="Batch Jobs" /> },
    ],
  },
]);
