import { Routes, Route } from "react-router-dom";
import AppShell from "../components/layout/AppShell";
import Dashboard from "../pages/Dashboard";
import PortfolioInquiry from "../pages/PortfolioInquiry";
import TransactionHistory from "../pages/TransactionHistory";
import Reports from "../pages/Reports";
import BatchJobs from "../pages/BatchJobs";
import SystemMonitor from "../pages/SystemMonitor";
import NotFound from "../pages/NotFound";

export default function AppRouter() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/portfolio-inquiry" element={<PortfolioInquiry />} />
        <Route path="/transaction-history" element={<TransactionHistory />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/batch-jobs" element={<BatchJobs />} />
        <Route path="/system-monitor" element={<SystemMonitor />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
