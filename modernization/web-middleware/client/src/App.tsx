import { Navigate, Route, Routes } from 'react-router-dom';
import ThemeProvider from './theme/ThemeProvider';
import Layout from './components/Layout';
import MenuPage from './pages/MenuPage';
import PositionPage from './pages/PositionPage';
import HistoryPage from './pages/HistoryPage';
import ExitPage from './pages/ExitPage';

export default function App() {
  return (
    <ThemeProvider>
      <Layout>
        <Routes>
          <Route path="/" element={<MenuPage />} />
          <Route path="/position" element={<PositionPage />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/exit" element={<ExitPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Layout>
    </ThemeProvider>
  );
}
