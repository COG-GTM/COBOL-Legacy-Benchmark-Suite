import { Navigate, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { HistoryInquiryPage } from './features/history/HistoryInquiryPage';
import { HistoryDetailPage } from './features/history/HistoryDetailPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/history" replace />} />
        <Route path="history" element={<HistoryInquiryPage />} />
        <Route path="history/:recordKey" element={<HistoryDetailPage />} />
        <Route path="*" element={<Navigate to="/history" replace />} />
      </Route>
    </Routes>
  );
}
