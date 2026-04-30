import { useMemo } from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { generatePerformanceData } from '../data/mockData';
import { formatCurrency } from '../lib/format';

interface PerformanceChartProps {
  timeRange: '1M' | '3M' | '6M' | '1Y';
}

export default function PerformanceChart({ timeRange }: PerformanceChartProps) {
  const allData = useMemo(() => generatePerformanceData(), []);

  const data = useMemo(() => {
    const daysMap = { '1M': 30, '3M': 90, '6M': 180, '1Y': 365 };
    return allData.slice(-daysMap[timeRange]);
  }, [allData, timeRange]);

  const tickFormatter = (dateStr: string) => {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  };

  return (
    <ResponsiveContainer width="100%" height={320}>
      <AreaChart data={data} margin={{ top: 5, right: 5, left: 5, bottom: 5 }}>
        <defs>
          <linearGradient id="portfolioGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#22d3ee" stopOpacity={0.25} />
            <stop offset="100%" stopColor="#22d3ee" stopOpacity={0} />
          </linearGradient>
          <linearGradient id="benchmarkGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#60a5fa" stopOpacity={0.1} />
            <stop offset="100%" stopColor="#60a5fa" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid stroke="rgba(255,255,255,0.05)" vertical={false} />
        <XAxis
          dataKey="date"
          tickFormatter={tickFormatter}
          tick={{ fill: '#94a3b8', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          interval="preserveStartEnd"
          minTickGap={60}
        />
        <YAxis
          tickFormatter={(v: number) => formatCurrency(v, true)}
          tick={{ fill: '#94a3b8', fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          width={70}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1e293b',
            border: '1px solid #334155',
            borderRadius: '8px',
            color: '#ffffff',
            fontSize: '12px',
          }}
          formatter={(value: number, name: string) => [
            formatCurrency(value),
            name === 'value' ? 'Portfolio' : 'Benchmark',
          ]}
          labelFormatter={(label: string) =>
            new Date(label).toLocaleDateString('en-US', {
              month: 'long',
              day: 'numeric',
              year: 'numeric',
            })
          }
        />
        <Area
          type="monotone"
          dataKey="benchmark"
          stroke="#60a5fa"
          strokeWidth={2.5}
          fill="url(#benchmarkGradient)"
          dot={false}
          activeDot={false}
        />
        <Area
          type="monotone"
          dataKey="value"
          stroke="#22d3ee"
          strokeWidth={2.5}
          fill="url(#portfolioGradient)"
          dot={false}
          activeDot={{ r: 4, fill: '#22d3ee', stroke: '#0f172a', strokeWidth: 2 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
}
