import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Briefcase, History, FileBarChart, Settings } from 'lucide-react';

interface NavCard {
  title: string;
  description: string;
  icon: typeof Briefcase;
  path: string;
  color: string;
  disabled?: boolean;
}

const navCards: NavCard[] = [
  {
    title: 'Portfolio Position Inquiry',
    description: 'Search and view portfolio positions, market values, and account details',
    icon: Briefcase,
    path: '/portfolio',
    color: '#22D3EE',
  },
  {
    title: 'Transaction History',
    description: 'View transaction records including buys, sells, transfers, and fees',
    icon: History,
    path: '/transactions',
    color: '#60A5FA',
  },
  {
    title: 'Reports',
    description: 'Generate portfolio performance and audit reports',
    icon: FileBarChart,
    path: '#',
    color: '#818CF8',
    disabled: true,
  },
  {
    title: 'System Settings',
    description: 'Configure system preferences and user management',
    icon: Settings,
    path: '#',
    color: '#A78BFA',
    disabled: true,
  },
];

export function DashboardPage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold text-white">Dashboard</h2>
        <p className="mt-1 text-[#94A3B8]">
          Welcome to the CLBS Portfolio Management System. Select an option below to get started.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {navCards.map((card) => (
          <Card
            key={card.title}
            className={`group cursor-pointer transition-all ${
              card.disabled
                ? 'cursor-not-allowed opacity-50'
                : 'hover:border-[' + card.color + ']/50 hover:shadow-lg hover:shadow-[' + card.color + ']/5'
            }`}
            onClick={() => {
              if (!card.disabled) navigate(card.path);
            }}
            role="button"
            tabIndex={card.disabled ? -1 : 0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !card.disabled) navigate(card.path);
            }}
            aria-disabled={card.disabled}
          >
            <CardHeader>
              <div
                className="mb-2 flex h-12 w-12 items-center justify-center rounded-lg"
                style={{ backgroundColor: `${card.color}20` }}
              >
                <card.icon className="h-6 w-6" style={{ color: card.color }} />
              </div>
              <CardTitle className="text-lg">{card.title}</CardTitle>
            </CardHeader>
            <CardContent>
              <CardDescription>{card.description}</CardDescription>
              {card.disabled && (
                <span className="mt-2 inline-block text-xs text-[#94A3B8]">Coming soon</span>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">System Overview</CardTitle>
          <CardDescription>COBOL Legacy Benchmark Suite - Modernized Frontend</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-3">
            <div className="rounded-lg bg-[#0F172A] p-4">
              <p className="text-sm text-[#94A3B8]">Active Portfolios</p>
              <p className="mt-1 text-2xl font-bold text-[#4ADE80]">3</p>
            </div>
            <div className="rounded-lg bg-[#0F172A] p-4">
              <p className="text-sm text-[#94A3B8]">Total Market Value</p>
              <p className="mt-1 text-2xl font-bold text-[#22D3EE]">$3.80M</p>
            </div>
            <div className="rounded-lg bg-[#0F172A] p-4">
              <p className="text-sm text-[#94A3B8]">Recent Transactions</p>
              <p className="mt-1 text-2xl font-bold text-[#60A5FA]">20</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
