import { createBrowserRouter } from 'react-router-dom';

/**
 * Route definitions for the Portfolio Management System
 * Maps legacy COBOL screens to modern React pages
 */

const router = createBrowserRouter([
  {
    path: '/login',
    lazy: () => import('./pages/auth/LoginPage'),
  },
  {
    path: '/',
    lazy: () => import('./layouts/AppLayout'),
    children: [
      {
        index: true,
        lazy: () => import('./pages/dashboard/DashboardPage'),
      },
      {
        path: 'portfolios',
        lazy: () => import('./pages/portfolios/PortfolioListPage'),
      },
      {
        path: 'portfolios/new',
        lazy: () => import('./pages/portfolios/CreatePortfolioPage'),
      },
      {
        path: 'portfolios/:id',
        lazy: () => import('./pages/portfolios/PortfolioDetailPage'),
      },
      {
        path: 'portfolios/:id/edit',
        lazy: () => import('./pages/portfolios/EditPortfolioPage'),
      },
      {
        path: 'positions',
        lazy: () => import('./pages/positions/PositionInquiryPage'),
      },
      {
        path: 'history',
        lazy: () => import('./pages/history/TransactionHistoryPage'),
      },
      {
        path: 'transactions/new',
        lazy: () => import('./pages/transactions/TransactionEntryPage'),
      },
      {
        path: 'reports/valuation',
        lazy: () => import('./pages/reports/ValuationReportPage'),
      },
      {
        path: 'reports/audit',
        lazy: () => import('./pages/reports/AuditReportPage'),
      },
      {
        path: 'reports/statistics',
        lazy: () => import('./pages/reports/StatisticsReportPage'),
      },
      {
        path: 'system/jobs',
        lazy: () => import('./pages/system/BatchJobStatusPage'),
      },
      {
        path: 'system/monitor',
        lazy: () => import('./pages/system/SystemMonitorPage'),
      },
      {
        path: 'system/maintenance',
        lazy: () => import('./pages/system/MaintenanceStatusPage'),
      },
    ],
  },
]);

export default router;
