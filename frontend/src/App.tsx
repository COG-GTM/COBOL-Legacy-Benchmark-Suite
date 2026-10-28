import { Route, Routes } from 'react-router-dom'

import { AppLayout } from '@/components/layout/AppLayout'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlaceholderPage } from '@/pages/PlaceholderPage'
import { PortfolioDetailPage } from '@/pages/portfolios/PortfolioDetailPage'
import { SignedOutPage } from '@/pages/SignedOutPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="portfolios" element={<PlaceholderPage pageId="portfolios" />} />
        <Route path="portfolios/:portfolioId" element={<PortfolioDetailPage />} />
        <Route path="positions" element={<PlaceholderPage pageId="positions" />} />
        <Route path="transactions" element={<PlaceholderPage pageId="transactions" />} />
        <Route path="reports" element={<PlaceholderPage pageId="reports" />} />
        <Route path="audit-log" element={<PlaceholderPage pageId="auditLog" />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="signed-out" element={<SignedOutPage />} />
    </Routes>
  )
}
