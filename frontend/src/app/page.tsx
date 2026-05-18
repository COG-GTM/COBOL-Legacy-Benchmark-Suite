import Dashboard from "@/components/Dashboard";
import ErrorBoundary from "@/components/ErrorBoundary";

export default function DashboardPage() {
  return (
    <ErrorBoundary>
      <Dashboard />
    </ErrorBoundary>
  );
}
