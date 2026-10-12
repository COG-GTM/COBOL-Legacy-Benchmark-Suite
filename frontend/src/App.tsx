import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { Loading } from './components/Loading';
import { HistoryScreen } from './screens/HistoryScreen';
import { AccessDeniedScreen, NotFoundScreen, SignedOutScreen, SignInScreen } from './screens/InfoScreens';
import { MenuScreen } from './screens/MenuScreen';
import { PositionsScreen } from './screens/PositionsScreen';
import { SystemErrorScreen, SystemErrorView } from './screens/SystemErrorScreen';
import { useSession } from './state/SessionContext';

/** FR-001 / FR-002: authenticate and authorize (INQONLN:READ) before any data screen renders. */
function RequireInquiryAccess() {
  const { status, canInquire, sessionError } = useSession();
  if (status === 'loading') return <Loading delayMs={0} />;
  if (status === 'unauthenticated') return <Navigate to="/signin" replace />;
  if (status === 'signedOut') return <Navigate to="/signed-out" replace />;
  if (status === 'denied') return <Navigate to="/access-denied" replace />;
  if (status === 'error')
    return (
      <SystemErrorView
        error={
          sessionError ?? {
            code: 'SYSTEM_ERROR',
            message: 'A system error occurred',
            severity: 'FATAL',
            action: 'ABEND',
            traceId: '',
            timestamp: '',
          }
        }
        onContinue={() => window.location.reload()}
      />
    );
  if (!canInquire) return <Navigate to="/access-denied" replace />;
  return <Outlet />;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route element={<RequireInquiryAccess />}>
          <Route index element={<MenuScreen />} />
          <Route path="positions" element={<PositionsScreen />} />
          <Route path="positions/:accountNo" element={<PositionsScreen />} />
          <Route path="history" element={<HistoryScreen />} />
          <Route path="history/:accountNo" element={<HistoryScreen />} />
        </Route>
        <Route path="error" element={<SystemErrorScreen />} />
        <Route path="access-denied" element={<AccessDeniedScreen />} />
        <Route path="signin" element={<SignInScreen />} />
        <Route path="signed-out" element={<SignedOutScreen />} />
        <Route path="*" element={<NotFoundScreen />} />
      </Route>
    </Routes>
  );
}
