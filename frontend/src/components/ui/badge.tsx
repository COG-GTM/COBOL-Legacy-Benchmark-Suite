import { cva } from 'class-variance-authority';
import type { VariantProps } from 'class-variance-authority';
import type { HTMLAttributes } from 'react';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors',
  {
    variants: {
      variant: {
        default: 'border-transparent bg-[#22D3EE]/20 text-[#22D3EE]',
        success: 'border-transparent bg-[#4ADE80]/20 text-[#4ADE80]',
        destructive: 'border-transparent bg-[#F87171]/20 text-[#F87171]',
        warning: 'border-transparent bg-amber-500/20 text-amber-400',
        outline: 'border-[#334155] text-[#CBD5E1]',
        secondary: 'border-transparent bg-[#60A5FA]/20 text-[#60A5FA]',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

interface BadgeProps extends HTMLAttributes<HTMLDivElement>, VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
