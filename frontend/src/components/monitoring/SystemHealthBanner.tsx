import type { SystemHealth } from '@/types';
import { cn } from '@/lib/utils';
import { Activity, Clock, AlertTriangle } from 'lucide-react';

interface SystemHealthBannerProps {
  health: SystemHealth;
}

const statusConfig = {
  healthy: { bg: 'bg-green-50 border-green-200', dot: 'bg-green-500', label: 'Healthy', text: 'text-green-800' },
  degraded: { bg: 'bg-yellow-50 border-yellow-200', dot: 'bg-yellow-500', label: 'Degraded', text: 'text-yellow-800' },
  critical: { bg: 'bg-red-50 border-red-200', dot: 'bg-red-500', label: 'Critical', text: 'text-red-800' },
};

const subsystems = [
  { key: 'cicsRegionStatus' as const, label: 'CICS Region' },
  { key: 'db2Status' as const, label: 'DB2' },
  { key: 'vsamStatus' as const, label: 'VSAM' },
  { key: 'mqStatus' as const, label: 'MQ' },
];

export default function SystemHealthBanner({ health }: SystemHealthBannerProps) {
  const config = statusConfig[health.overallStatus];

  return (
    <div className={cn('rounded-lg border p-4 md:p-6', config.bg)}>
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3">
            <span className={cn('inline-block h-4 w-4 rounded-full', config.dot)} aria-hidden="true" />
            <div>
              <h2 className={cn('text-lg font-semibold', config.text)}>
                System Status: {config.label}
              </h2>
              <div className="flex items-center gap-4 mt-1 text-sm text-gray-600">
                <span className="inline-flex items-center gap-1">
                  <Activity className="h-3.5 w-3.5" />
                  Uptime: {health.uptime}
                </span>
                <span className="inline-flex items-center gap-1">
                  <Clock className="h-3.5 w-3.5" />
                  Last Incident: {health.lastIncident}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {subsystems.map(({ key, label }) => {
            const isActive = health[key] === 'active';
            return (
              <span
                key={key}
                className={cn(
                  'inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium',
                  isActive
                    ? 'bg-green-100 text-green-700'
                    : 'bg-red-100 text-red-700'
                )}
              >
                <span
                  className={cn('h-2 w-2 rounded-full', isActive ? 'bg-green-500' : 'bg-red-500')}
                  aria-hidden="true"
                />
                {label}: {isActive ? 'Active' : 'Inactive'}
                {!isActive && <AlertTriangle className="h-3 w-3" />}
              </span>
            );
          })}
        </div>
      </div>
    </div>
  );
}
