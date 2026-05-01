import { Link } from 'react-router-dom';

const summaryCards = [
  { label: 'Total Portfolios', value: '—', link: '/portfolios' },
  { label: 'Active Portfolios', value: '—', link: '/portfolios?status=A' },
  { label: 'Total Positions', value: '—', link: '/inquiry' },
  { label: 'Recent Transactions', value: '—', link: '/history' },
];

export default function Dashboard() {
  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">Dashboard</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        {summaryCards.map((card) => (
          <Link
            key={card.label}
            to={card.link}
            className="bg-white rounded-lg shadow p-6 hover:shadow-md transition-shadow"
          >
            <div className="text-sm text-gray-500">{card.label}</div>
            <div className="text-3xl font-bold mt-2">{card.value}</div>
          </Link>
        ))}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
          <div className="space-y-2">
            <Link to="/inquiry" className="block px-4 py-2 bg-blue-50 rounded hover:bg-blue-100 text-blue-700">
              Portfolio Inquiry
            </Link>
            <Link to="/history" className="block px-4 py-2 bg-blue-50 rounded hover:bg-blue-100 text-blue-700">
              Transaction History
            </Link>
            <Link to="/batch" className="block px-4 py-2 bg-blue-50 rounded hover:bg-blue-100 text-blue-700">
              Start Batch Run
            </Link>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-6">
          <h3 className="text-lg font-semibold mb-4">Recent Activity</h3>
          <p className="text-gray-500 text-sm">No recent activity to display.</p>
        </div>
      </div>
    </div>
  );
}
