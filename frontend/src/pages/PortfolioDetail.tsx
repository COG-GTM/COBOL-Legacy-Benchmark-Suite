import { useParams, Link, useNavigate } from 'react-router-dom';
import { ArrowLeft, Pencil } from 'lucide-react';
import { usePortfolio } from '../context/PortfolioContext';
import { useAuth } from '../context/AuthContext';

const statusLabels: Record<string, string> = { A: 'Active', I: 'Inactive', C: 'Closed' };
const clientTypeLabels: Record<string, string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export default function PortfolioDetail() {
  const { id } = useParams<{ id: string }>();
  const { portfolios, positions } = usePortfolio();
  const { user } = useAuth();
  const navigate = useNavigate();

  const portfolio = portfolios.find((p) => p.id === id);

  if (!portfolio) {
    return (
      <div className="text-center py-12">
        <h2 className="text-xl font-semibold text-gray-700 mb-2">Portfolio Not Found</h2>
        <p className="text-gray-500 mb-4">No portfolio with ID "{id}" exists.</p>
        <Link to="/portfolios" className="text-blue-600 hover:underline">
          Back to Portfolios
        </Link>
      </div>
    );
  }

  const portfolioPositions = positions.filter((p) => p.portfolioId === portfolio.id);

  const formatDate = (d: string) =>
    d ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : 'N/A';

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/portfolios')}
            className="p-2 hover:bg-gray-100 rounded-md"
          >
            <ArrowLeft className="w-5 h-5 text-gray-600" />
          </button>
          <h1 className="text-2xl font-bold text-gray-800">Portfolio {portfolio.id}</h1>
        </div>
        {user?.role === 'read-write' && (
          <Link
            to={`/portfolios/${portfolio.id}/edit`}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
          >
            <Pencil className="w-4 h-4" /> Edit
          </Link>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Portfolio Information</h2>
          <dl className="grid grid-cols-2 gap-4">
            {([
              ['Portfolio ID', portfolio.id],
              ['Account Number', portfolio.accountNo],
              ['Client Name', portfolio.clientName],
              ['Client Type', clientTypeLabels[portfolio.clientType]],
              ['Status', statusLabels[portfolio.status]],
              ['Created', formatDate(portfolio.createDate)],
              ['Last Maintained', formatDate(portfolio.lastMaintDate)],
              ['Last User', portfolio.lastUser],
            ] as [string, string][]).map(([label, value]) => (
              <div key={label}>
                <dt className="text-sm text-gray-500">{label}</dt>
                <dd className="text-sm font-medium text-gray-800">{value}</dd>
              </div>
            ))}
          </dl>
        </div>

        <div className="bg-white rounded-lg shadow-sm border p-6">
          <h2 className="text-lg font-semibold text-gray-800 mb-4">Financial Summary</h2>
          <dl className="grid grid-cols-1 gap-4">
            <div>
              <dt className="text-sm text-gray-500">Total Value</dt>
              <dd className="text-2xl font-bold text-gray-800">
                ${portfolio.totalValue.toLocaleString('en-US', { minimumFractionDigits: 2 })}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">Cash Balance</dt>
              <dd className="text-xl font-semibold text-gray-700">
                ${portfolio.cashBalance.toLocaleString('en-US', { minimumFractionDigits: 2 })}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-gray-500">Invested Value</dt>
              <dd className="text-xl font-semibold text-gray-700">
                ${(portfolio.totalValue - portfolio.cashBalance).toLocaleString('en-US', {
                  minimumFractionDigits: 2,
                })}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      {portfolioPositions.length > 0 && (
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="p-4 border-b">
            <h2 className="text-lg font-semibold text-gray-800">Positions</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50">
                <tr>
                  <th className="text-left px-4 py-2 text-gray-600">Fund ID</th>
                  <th className="text-left px-4 py-2 text-gray-600">Fund Name</th>
                  <th className="text-right px-4 py-2 text-gray-600">Units</th>
                  <th className="text-right px-4 py-2 text-gray-600">Cost Basis</th>
                  <th className="text-right px-4 py-2 text-gray-600">Market Value</th>
                  <th className="text-right px-4 py-2 text-gray-600">Gain/Loss</th>
                </tr>
              </thead>
              <tbody>
                {portfolioPositions.map((pos, i) => {
                  const gl = pos.marketValue - pos.costBasis;
                  return (
                    <tr key={i} className="border-t">
                      <td className="px-4 py-2 font-mono text-gray-700">{pos.investmentId}</td>
                      <td className="px-4 py-2 text-gray-700">{pos.fundName}</td>
                      <td className="px-4 py-2 text-right text-gray-700">
                        {pos.quantity.toLocaleString('en-US', { minimumFractionDigits: 4 })}
                      </td>
                      <td className="px-4 py-2 text-right text-gray-700">
                        ${pos.costBasis.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="px-4 py-2 text-right text-gray-700">
                        ${pos.marketValue.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                      </td>
                      <td
                        className={`px-4 py-2 text-right font-medium ${gl >= 0 ? 'text-green-600' : 'text-red-600'}`}
                      >
                        ${gl.toLocaleString('en-US', { minimumFractionDigits: 2 })}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
