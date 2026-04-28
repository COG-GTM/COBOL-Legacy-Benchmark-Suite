import { useParams, Link } from 'react-router-dom';
import { mockPortfolios, mockPositions } from '../mocks/mockData';
import { CLIENT_TYPE_LABELS, PORTFOLIO_STATUS_LABELS, POSITION_STATUS_LABELS } from '../types';
import { formatCurrency, formatDate, formatNumber } from '../utils/validation';
import { ErrorDisplay } from '../components/ErrorDisplay';

export function PortfolioDetailPage() {
  const { id } = useParams<{ id: string }>();
  const portfolio = mockPortfolios.find(p => p.portfolioId === id);

  if (!portfolio) {
    return (
      <div className="p-8">
        <ErrorDisplay code="VS23" details={`Portfolio ${id} not found`} />
        <Link to="/portfolios" className="text-blue-600 hover:underline text-sm mt-4 inline-block">
          Back to Portfolio List
        </Link>
      </div>
    );
  }

  const positions = mockPositions.filter(p => p.portfolioId === portfolio.portfolioId);

  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Portfolio: {portfolio.portfolioId}</h1>
        <div className="flex gap-3">
          <Link
            to={`/portfolios/${portfolio.portfolioId}/edit`}
            className="bg-green-600 text-white px-4 py-2 rounded-md text-sm hover:bg-green-700 transition-colors"
          >
            Edit
          </Link>
          <Link to="/portfolios" className="bg-gray-200 text-gray-700 px-4 py-2 rounded-md text-sm hover:bg-gray-300 transition-colors">
            Back
          </Link>
        </div>
      </div>

      {/* Portfolio Info */}
      <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-semibold mb-4">Portfolio Details</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
          <Field label="Portfolio ID" value={portfolio.portfolioId} />
          <Field label="Account Number" value={portfolio.accountNumber} />
          <Field label="Client Name" value={portfolio.clientName} />
          <Field label="Client Type" value={CLIENT_TYPE_LABELS[portfolio.clientType]} />
          <Field label="Status" value={PORTFOLIO_STATUS_LABELS[portfolio.status]} />
          <Field label="Created" value={formatDate(portfolio.createDate)} />
          <Field label="Total Value" value={formatCurrency(portfolio.totalValue)} />
          <Field label="Cash Balance" value={formatCurrency(portfolio.cashBalance)} />
          <Field label="Last Maintained" value={`${formatDate(portfolio.lastMaintDate)} by ${portfolio.lastUser}`} />
        </div>
      </div>

      {/* Positions */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <h2 className="text-lg font-semibold mb-4">Positions ({positions.length})</h2>
        {positions.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b-2 border-gray-300">
                  <th className="text-left py-2 px-3 font-semibold">Investment ID</th>
                  <th className="text-left py-2 px-3 font-semibold">Fund Name</th>
                  <th className="text-right py-2 px-3 font-semibold">Quantity</th>
                  <th className="text-right py-2 px-3 font-semibold">Cost Basis</th>
                  <th className="text-right py-2 px-3 font-semibold">Market Value</th>
                  <th className="text-left py-2 px-3 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody>
                {positions.map((pos, i) => (
                  <tr key={i} className="border-b border-gray-100">
                    <td className="py-2 px-3 font-mono">{pos.investmentId}</td>
                    <td className="py-2 px-3">{pos.fundName}</td>
                    <td className="py-2 px-3 text-right font-mono">{formatNumber(pos.quantity, 4)}</td>
                    <td className="py-2 px-3 text-right font-mono">{formatCurrency(pos.costBasis)}</td>
                    <td className="py-2 px-3 text-right font-mono">{formatCurrency(pos.marketValue)}</td>
                    <td className="py-2 px-3">{POSITION_STATUS_LABELS[pos.status]}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-gray-400 text-sm">No positions found for this portfolio.</p>
        )}
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-gray-500">{label}</p>
      <p className="text-sm font-medium text-gray-900">{value}</p>
    </div>
  );
}
