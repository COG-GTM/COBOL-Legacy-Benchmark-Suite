import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * Maps to MENMAP from INQSET.bms lines 7-19
 * Main Menu: 1. Portfolio Position Inquiry, 2. Transaction History, 3. Exit
 * Extended with Portfolio Management and Reports
 */

const menuItems = [
  {
    to: '/positions',
    title: '1. Portfolio Position Inquiry',
    description: 'Look up current positions by account number (POSMAP)',
    color: 'bg-blue-50 border-blue-200 hover:bg-blue-100',
  },
  {
    to: '/history',
    title: '2. Transaction History',
    description: 'View transaction history by account (HISMAP)',
    color: 'bg-green-50 border-green-200 hover:bg-green-100',
  },
  {
    to: '/portfolios',
    title: '3. Portfolio Management',
    description: 'Create, read, update, delete portfolios (PORTMSTR)',
    requiresManager: true,
    color: 'bg-purple-50 border-purple-200 hover:bg-purple-100',
  },
  {
    to: '/transactions/new',
    title: '4. Transaction Entry',
    description: 'Enter new buy/sell/transfer transactions (PORTTRAN)',
    requiresManager: true,
    color: 'bg-orange-50 border-orange-200 hover:bg-orange-100',
  },
  {
    to: '/reports',
    title: '5. Reports',
    description: 'Valuation, audit, and system statistics reports',
    color: 'bg-gray-50 border-gray-200 hover:bg-gray-100',
  },
];

export function DashboardPage() {
  const { userId, role } = useAuth();

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">Portfolio Management System</h1>
        <p className="text-sm text-gray-500 mt-1">Select Option:</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 max-w-5xl">
        {menuItems.map(item => {
          const disabled = item.requiresManager && role === 'read-only';
          return (
            <Link
              key={item.to}
              to={disabled ? '#' : item.to}
              className={`block border rounded-lg p-6 transition-colors ${
                disabled ? 'opacity-50 cursor-not-allowed bg-gray-100' : item.color
              }`}
              onClick={e => { if (disabled) e.preventDefault(); }}
            >
              <h2 className="text-lg font-semibold text-gray-900">{item.title}</h2>
              <p className="text-sm text-gray-600 mt-2">{item.description}</p>
              {disabled && (
                <p className="text-xs text-red-500 mt-2">Requires Portfolio Manager role</p>
              )}
            </Link>
          );
        })}
      </div>

      <p className="text-xs text-gray-400 mt-8">
        Logged in as: {userId} | Role: {role}
      </p>
    </div>
  );
}
