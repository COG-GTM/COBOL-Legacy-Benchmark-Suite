import type { BatchPipelineData } from '@/types';
import { cn } from '@/lib/utils';
import { CheckCircle, XCircle, Clock, Loader2, PauseCircle, Calendar } from 'lucide-react';

interface BatchPipelineStatusProps {
  data: BatchPipelineData;
}

const statusConfig = {
  complete: { icon: CheckCircle, color: 'text-green-500', bg: 'bg-green-50 border-green-200', line: 'bg-green-500' },
  running: { icon: Loader2, color: 'text-blue-500', bg: 'bg-blue-50 border-blue-200', line: 'bg-blue-500' },
  pending: { icon: Clock, color: 'text-gray-400', bg: 'bg-gray-50 border-gray-200', line: 'bg-gray-300' },
  error: { icon: XCircle, color: 'text-red-500', bg: 'bg-red-50 border-red-200', line: 'bg-red-500' },
  suspended: { icon: PauseCircle, color: 'text-yellow-500', bg: 'bg-yellow-50 border-yellow-200', line: 'bg-yellow-500' },
};

function formatTime(time: string | null): string {
  if (!time) return '—';
  return time.split(' ')[1] ?? time;
}

function formatRecords(count: number | null): string {
  if (count === null) return '—';
  return count.toLocaleString();
}

export default function BatchPipelineStatus({ data }: BatchPipelineStatusProps) {
  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6">
        <h3 className="text-sm font-medium text-gray-600">Batch Pipeline Status</h3>
        <div className="flex items-center gap-4 mt-2 sm:mt-0 text-xs text-gray-500">
          <span className="inline-flex items-center gap-1">
            <Calendar className="h-3.5 w-3.5" />
            Last Run: {data.lastRun}
          </span>
          <span className="inline-flex items-center gap-1">
            <Clock className="h-3.5 w-3.5" />
            Next: {data.nextScheduled}
          </span>
        </div>
      </div>

      {/* Desktop: Horizontal pipeline */}
      <div className="hidden md:block overflow-x-auto">
        <div className="flex items-start min-w-max">
          {data.steps.map((step, i) => {
            const config = statusConfig[step.status];
            const Icon = config.icon;
            const isLast = i === data.steps.length - 1;

            return (
              <div key={step.name} className="flex items-start">
                <div className="flex flex-col items-center w-32">
                  <div
                    className={cn('rounded-full border-2 p-2 mb-2', config.bg)}
                    aria-label={`${step.name}: ${step.status}`}
                  >
                    <Icon className={cn('h-5 w-5', config.color, step.status === 'running' && 'animate-spin')} />
                  </div>
                  <p className="text-xs font-medium text-gray-700 text-center leading-tight mb-1">{step.name}</p>
                  <p className="text-xs text-gray-400">{formatTime(step.startTime)}</p>
                  {step.endTime && (
                    <p className="text-xs text-gray-400">→ {formatTime(step.endTime)}</p>
                  )}
                  {step.recordsProcessed !== null && (
                    <p className="text-xs text-gray-500 mt-0.5">{formatRecords(step.recordsProcessed)} rec</p>
                  )}
                </div>
                {!isLast && (
                  <div className="flex items-center pt-4 px-1">
                    <div className={cn('h-0.5 w-8', config.line)} />
                    <div className={cn(
                      'h-0 w-0 border-t-4 border-b-4 border-l-6 border-t-transparent border-b-transparent',
                      step.status === 'complete' ? 'border-l-green-500' : 'border-l-gray-300'
                    )} />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Mobile: Vertical timeline */}
      <div className="md:hidden space-y-4">
        {data.steps.map((step) => {
          const config = statusConfig[step.status];
          const Icon = config.icon;

          return (
            <div key={step.name} className="flex items-start gap-3">
              <div className={cn('rounded-full border-2 p-1.5 shrink-0', config.bg)}>
                <Icon className={cn('h-4 w-4', config.color, step.status === 'running' && 'animate-spin')} />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-700">{step.name}</p>
                <div className="flex flex-wrap gap-x-3 text-xs text-gray-400 mt-0.5">
                  <span>{formatTime(step.startTime)} → {formatTime(step.endTime)}</span>
                  {step.recordsProcessed !== null && (
                    <span>{formatRecords(step.recordsProcessed)} records</span>
                  )}
                </div>
              </div>
              <span className={cn('text-xs capitalize', config.color)}>{step.status}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
