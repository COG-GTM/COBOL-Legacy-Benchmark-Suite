import { Loader2 } from 'lucide-react';
import { cn } from '@/utils/cn';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  message?: string;
}

const sizeMap = { sm: 'h-4 w-4', md: 'h-8 w-8', lg: 'h-12 w-12' };

export function LoadingSpinner({ size = 'md', className, message }: LoadingSpinnerProps) {
  return (
    <div className={cn('flex flex-col items-center justify-center gap-2 p-8', className)}>
      <Loader2 className={cn('animate-spin text-primary-light', sizeMap[size])} />
      {message && <p className="text-sm text-secondary">{message}</p>}
    </div>
  );
}
