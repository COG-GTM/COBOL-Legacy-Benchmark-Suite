import { Routes, Route } from "react-router-dom";
import AppShell from "../components/layout/AppShell";
import Dashboard from "../pages/Dashboard";
import PortfolioInquiry from "../pages/PortfolioInquiry";
import TransactionHistory from "../pages/TransactionHistory";
import Reports from "../pages/Reports";
import BatchJobsPage from "../components/batch/BatchJobsPage";
import PipelineStatusTab from "../components/batch/PipelineStatusTab";
import PipelineDefinitionTab from "../components/batch/PipelineDefinitionTab";
import RunHistoryTab from "../components/batch/RunHistoryTab";
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
        <Route path="/batch-jobs" element={<BatchJobsPage />}>
          <Route path="status" element={<PipelineStatusTab />} />
          <Route path="definition" element={<PipelineDefinitionTab />} />
          <Route path="history" element={<RunHistoryTab />} />
        </Route>
        <Route path="/system-monitor" element={<SystemMonitor />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
