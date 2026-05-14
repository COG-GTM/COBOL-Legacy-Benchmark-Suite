import { Link } from 'react-router-dom';
import { BarChart3, Shield, Activity } from 'lucide-react';

const reports = [
  {
    to: '/reports/position',
    label: 'Position Report',
    description: 'Daily portfolio valuations with gain/loss analysis',
    icon: BarChart3,
    source: 'RPTPOS00',
  },
  {
    to: '/reports/audit',
    label: 'Audit Report',
    description: 'Security audit trail, process execution, and exception logs',
    icon: Shield,
    source: 'RPTAUD00',
  },
  {
    to: '/reports/statistics',
    label: 'Statistics Report',
    description: 'System performance metrics, resource utilization, and trend analysis',
    icon: Activity,
    source: 'RPTSTA00',
  },
];

export default function ReportsIndex() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-800 mb-6">Reports</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {reports.map(({ to, label, description, icon: Icon, source }) => (
          <Link
            key={to}
            to={to}
            className="bg-white rounded-lg shadow-sm border p-6 hover:shadow-md hover:border-blue-300 transition-all"
          >
            <Icon className="w-10 h-10 text-blue-600 mb-4" />
            <h2 className="text-lg font-semibold text-gray-800 mb-1">{label}</h2>
            <p className="text-sm text-gray-500 mb-3">{description}</p>
            <p className="text-xs text-gray-400">Source: {source}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
