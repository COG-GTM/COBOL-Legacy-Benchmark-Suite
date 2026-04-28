import { mockValuationReport } from '../mocks/mockData';
import { formatCurrency, formatNumber } from '../utils/validation';

/**
 * Maps to RPTPOS00 from RPTPOS00.cbl lines 133-141
 * Shows portfolio ID, description, quantity, current value, % change
 */
export function ValuationReportPage() {
  const totalValue = mockValuationReport.reduce((sum, r) => sum + r.currentValue, 0);

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Valuation Report</h1>
      <p className="text-sm text-gray-500 mb-6">Position Valuation Summary (RPTPOS00)</p>

      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b-2 border-gray-300">
              <th className="text-left py-2 px-3 font-semibold">Portfolio ID</th>
              <th className="text-left py-2 px-3 font-semibold">Description</th>
              <th className="text-right py-2 px-3 font-semibold">Quantity</th>
              <th className="text-right py-2 px-3 font-semibold">Current Value</th>
              <th className="text-right py-2 px-3 font-semibold">Previous Value</th>
              <th className="text-right py-2 px-3 font-semibold">% Change</th>
            </tr>
          </thead>
          <tbody>
            {mockValuationReport.map(row => (
              <tr key={row.portfolioId} className="border-b border-gray-100 hover:bg-gray-50">
                <td className="py-2 px-3 font-mono">{row.portfolioId}</td>
                <td className="py-2 px-3">{row.description}</td>
                <td className="py-2 px-3 text-right font-mono">{formatNumber(row.quantity, 0)}</td>
                <td className="py-2 px-3 text-right font-mono">{formatCurrency(row.currentValue)}</td>
                <td className="py-2 px-3 text-right font-mono">{formatCurrency(row.previousValue)}</td>
                <td className={`py-2 px-3 text-right font-mono ${row.changePercent >= 0 ? 'text-green-700' : 'text-red-700'}`}>
                  {row.changePercent >= 0 ? '+' : ''}{formatNumber(row.changePercent)}%
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="border-t-2 border-gray-300 font-semibold">
              <td colSpan={3} className="py-2 px-3">Total</td>
              <td className="py-2 px-3 text-right font-mono">{formatCurrency(totalValue)}</td>
              <td colSpan={2}></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  );
}
