import { Navigate, Route, Routes as RouterRoutes } from 'react-router-dom';
import { MainMenu } from './pages/MainMenu';
import { PortfolioInquiry } from './pages/PortfolioInquiry';
import { TransactionHistory } from './pages/TransactionHistory';
import { SessionEnded } from './pages/SessionEnded';
import { Routes } from './routes/functionCodes';

/**
 * Client-side routing table.
 *
 * Routes mirror the legacy `INQONLN` dispatch:
 *   `/`          MENU  -> MainMenu
 *   `/portfolio` INQP  -> PortfolioInquiry
 *   `/history`   INQH  -> TransactionHistory
 *   `/exit`      EXIT  -> SessionEnded
 */
export default function App() {
  return (
    <RouterRoutes>
      <Route path={Routes.MENU} element={<MainMenu />} />
      <Route path={Routes.PORTFOLIO} element={<PortfolioInquiry />} />
      <Route path={Routes.HISTORY} element={<TransactionHistory />} />
      <Route path={Routes.EXIT} element={<SessionEnded />} />
      <Route path="*" element={<Navigate to={Routes.MENU} replace />} />
    </RouterRoutes>
  );
}
