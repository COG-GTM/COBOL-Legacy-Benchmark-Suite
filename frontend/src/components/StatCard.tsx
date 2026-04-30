import type { ReactNode } from 'react';

interface Props {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: ReactNode;
  trend?: ReactNode;
}

export default function StatCard({ title, value, subtitle, icon, trend }: Props) {
  return (
    <div className="card-hover">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm text-slate-400">{title}</p>
          <p className="text-2xl font-bold mt-1">{value}</p>
          {subtitle && <p className="text-xs text-slate-500 mt-1">{subtitle}</p>}
          {trend && <div className="mt-2">{trend}</div>}
        </div>
        {icon && <div className="text-blue-400 opacity-60">{icon}</div>}
      </div>
    </div>
  );
}
