import { Link } from 'react-router-dom';

export function ReportsPage() {
  const reports = [
    { to: '/reports/valuation', title: 'Valuation Report', description: 'Portfolio position valuation with % change (RPTPOS00)' },
    { to: '/reports/audit', title: 'Audit Report', description: 'Filterable audit log by event type TRAN/USER/SYST (AUDITLOG)' },
    { to: '/reports/system', title: 'System Statistics', description: 'DB2 metrics and batch processing statistics (RPTSTA00)' },
  ];

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Reports</h1>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-w-4xl">
        {reports.map(r => (
          <Link
            key={r.to}
            to={r.to}
            className="block border border-gray-200 rounded-lg p-6 bg-white hover:bg-gray-50 transition-colors"
          >
            <h2 className="text-lg font-semibold text-gray-900">{r.title}</h2>
            <p className="text-sm text-gray-600 mt-2">{r.description}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
