import QuickActions from './QuickActions';
import SummaryCards from './SummaryCards';
import RecentActivity from './RecentActivity';

export default function DashboardPage() {
  const today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <div className="space-y-6">
      {/* Welcome / Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Portfolio Management System</h1>
        <p className="text-sm text-gray-500 mt-1">
          {today} &mdash; Logged in as <span className="font-medium text-gray-700">USER001</span>
        </p>
      </div>

      <QuickActions />
      <SummaryCards />
      <RecentActivity />
    </div>
  );
}
