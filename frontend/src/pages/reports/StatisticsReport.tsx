import { useMemo } from 'react';
import { ArrowLeft, TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import reportData from '../../mocks/reports/statisticsReport.json';
import type { StatisticsEntry } from '../../types';

const trendIcons = {
  up: TrendingUp,
  down: TrendingDown,
  stable: Minus,
};

const trendColors = {
  up: 'text-green-600',
  down: 'text-red-600',
  stable: 'text-gray-500',
};

export default function StatisticsReport() {
  const navigate = useNavigate();
  const data = reportData as StatisticsEntry[];

  const dates = useMemo(() => [...new Set(data.map((d) => d.date))].sort().reverse(), [data]);

  const latestDate = dates[0];
  const latestData = useMemo(() => data.filter((d) => d.date === latestDate), [data, latestDate]);

  const formatValue = (entry: StatisticsEntry) => {
    if (entry.unit === 'USD') return `$${entry.value.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    if (entry.unit === 'percent') return `${entry.value}%`;
    if (entry.unit === 'seconds') return `${entry.value}s`;
    return entry.value.toLocaleString();
  };

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <button onClick={() => navigate('/reports')} className="p-2 hover:bg-gray-100 rounded-md">
          <ArrowLeft className="w-5 h-5 text-gray-600" />
        </button>
        <h1 className="text-2xl font-bold text-gray-800">Statistics Report</h1>
      </div>

      <div className="mb-6">
        <h2 className="text-lg font-semibold text-gray-700 mb-4">
          Current Metrics — {latestDate}
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {latestData.map((entry, i) => {
            const TrendIcon = trendIcons[entry.trend];
            return (
              <div key={i} className="bg-white rounded-lg shadow-sm border p-5">
                <p className="text-sm text-gray-500 mb-1">{entry.metric}</p>
                <div className="flex items-end justify-between">
                  <p className="text-2xl font-bold text-gray-800">{formatValue(entry)}</p>
                  <TrendIcon className={`w-5 h-5 ${trendColors[entry.trend]}`} />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-4 border-b">
          <h2 className="text-lg font-semibold text-gray-800">Historical Data</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-gray-50">
              <tr>
                <th className="text-left px-4 py-2 text-gray-600">Date</th>
                <th className="text-left px-4 py-2 text-gray-600">Metric</th>
                <th className="text-right px-4 py-2 text-gray-600">Value</th>
                <th className="text-left px-4 py-2 text-gray-600">Unit</th>
                <th className="text-center px-4 py-2 text-gray-600">Trend</th>
              </tr>
            </thead>
            <tbody>
              {data.map((entry, i) => {
                const TrendIcon = trendIcons[entry.trend];
                return (
                  <tr key={i} className="border-t">
                    <td className="px-4 py-2 text-gray-700">{entry.date}</td>
                    <td className="px-4 py-2 text-gray-700">{entry.metric}</td>
                    <td className="px-4 py-2 text-right text-gray-700">{formatValue(entry)}</td>
                    <td className="px-4 py-2 text-gray-500">{entry.unit}</td>
                    <td className="px-4 py-2 text-center">
                      <TrendIcon className={`w-4 h-4 inline ${trendColors[entry.trend]}`} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
