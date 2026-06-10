import type { ErrorCategory, ErrorSeverity } from '@/data/types';

export const CATEGORY_LABELS: Record<ErrorCategory, string> = {
  VS: 'VSAM',
  VL: 'Validation',
  PR: 'Processing',
  SY: 'System',
};

export const SEVERITY_COLORS: Record<ErrorSeverity, { bg: string; text: string; border: string; ring: string }> = {
  warning: { bg: 'bg-amber-50', text: 'text-amber-800', border: 'border-amber-300', ring: 'ring-amber-600/20' },
  error: { bg: 'bg-red-50', text: 'text-red-800', border: 'border-red-300', ring: 'ring-red-600/20' },
  severe: { bg: 'bg-orange-50', text: 'text-orange-800', border: 'border-orange-300', ring: 'ring-orange-600/20' },
  critical: { bg: 'bg-purple-50', text: 'text-purple-800', border: 'border-purple-300', ring: 'ring-purple-600/20' },
};
