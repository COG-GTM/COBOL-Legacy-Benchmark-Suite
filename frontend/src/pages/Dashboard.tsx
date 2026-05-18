import { useNavigate } from 'react-router-dom';
import {
  Search,
  History,
  Briefcase,
  ArrowRightLeft,
  FileBarChart,
  Activity,
} from 'lucide-react';
import { PageLayout } from '@/components/shared';

const menuItems = [
  {
    to: '/portfolio-inquiry',
    label: 'Portfolio Position Inquiry',
    description: 'Look up portfolio positions by account number',
    icon: Search,
    color: 'bg-blue-500',
  },
  {
    to: '/transaction-history',
    label: 'Transaction History',
    description: 'View transaction history with date range filtering',
    icon: History,
    color: 'bg-green-500',
  },
  {
    to: '/portfolio-management',
    label: 'Portfolio Management',
    description: 'Create, update, and manage portfolios',
    icon: Briefcase,
    color: 'bg-purple-500',
  },
  {
    to: '/transaction-processing',
    label: 'Transaction Processing',
    description: 'Process buy, sell, and fee transactions',
    icon: ArrowRightLeft,
    color: 'bg-orange-500',
  },
  {
    to: '/reports',
    label: 'Reports',
    description: 'Position, audit, and statistics reports',
    icon: FileBarChart,
    color: 'bg-teal-500',
  },
  {
    to: '/batch-status',
    label: 'Batch Job Status',
    description: 'Monitor batch processing status and errors',
    icon: Activity,
    color: 'bg-red-500',
  },
];

export function Dashboard() {
  const navigate = useNavigate();

  return (
    <PageLayout title="Dashboard" subtitle="Select an option to continue">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {menuItems.map((item) => (
          <button
            key={item.to}
            onClick={() => navigate(item.to)}
            className="flex items-start gap-4 p-6 bg-white rounded-xl border border-border shadow-sm hover:shadow-md hover:border-primary-light/50 transition-all text-left group"
          >
            <div className={`${item.color} rounded-lg p-3 text-white group-hover:scale-110 transition-transform`}>
              <item.icon className="h-6 w-6" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">{item.label}</h3>
              <p className="text-sm text-secondary mt-1">{item.description}</p>
            </div>
          </button>
        ))}
      </div>
    </PageLayout>
  );
}
