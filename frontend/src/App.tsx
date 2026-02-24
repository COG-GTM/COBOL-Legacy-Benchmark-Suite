import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Header from './components/Header';
import LoginPage from './components/LoginPage';
import MainMenu from './components/MainMenu';
import PortfolioView from './components/PortfolioView';
import TransactionHistory from './components/TransactionHistory';
import ErrorBoundary from './components/ErrorBoundary';

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function App() {
  const { isAuthenticated } = useAuth();

  return (
    <ErrorBoundary>
      {isAuthenticated && <Header />}
      <Routes>
        <Route path="/login" element={
          isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />
        } />
        <Route path="/" element={
          <ProtectedRoute><MainMenu /></ProtectedRoute>
        } />
        <Route path="/portfolios/:id" element={
          <ProtectedRoute><PortfolioView /></ProtectedRoute>
        } />
        <Route path="/portfolios/:id/history" element={
          <ProtectedRoute><TransactionHistory /></ProtectedRoute>
        } />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </ErrorBoundary>
  );
}

export default App;
