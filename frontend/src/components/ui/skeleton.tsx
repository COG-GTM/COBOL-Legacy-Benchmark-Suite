import { cn } from '@/lib/utils';
import type { HTMLAttributes } from 'react';

function Skeleton({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div className={cn('animate-pulse rounded-md bg-[#334155]', className)} {...props} />
  );
}

export { Skeleton };
