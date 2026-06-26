import { Navigate, Route, Routes } from 'react-router-dom';
import type { ReactElement } from 'react';
import { SessionProvider } from './session/SessionContext';
import { useSession } from './session/sessionContextValue';
import { StatusBar } from './components/StatusBar';
import { LoginScreen } from './screens/LoginScreen';
import { MenuScreen } from './screens/MenuScreen';
import { PortfolioScreen } from './screens/PortfolioScreen';
import { HistoryScreen } from './screens/HistoryScreen';
import { ErrorScreen } from './screens/ErrorScreen';
import { ExitScreen } from './screens/ExitScreen';

/** Route guard: unauthenticated users are sent to the sign-on screen. */
function RequireAuth({ children }: { children: ReactElement }) {
  const { isAuthenticated } = useSession();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/menu" replace />} />
      <Route path="/login" element={<LoginScreen />} />
      <Route path="/exit" element={<ExitScreen />} />
      <Route path="/menu" element={<RequireAuth><MenuScreen /></RequireAuth>} />
      <Route
        path="/portfolio"
        element={<RequireAuth><PortfolioScreen /></RequireAuth>}
      />
      <Route
        path="/history"
        element={<RequireAuth><HistoryScreen /></RequireAuth>}
      />
      <Route path="/error" element={<RequireAuth><ErrorScreen /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/menu" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <SessionProvider>
      <div className="app-shell">
        <StatusBar />
        <AppRoutes />
      </div>
    </SessionProvider>
  );
}
