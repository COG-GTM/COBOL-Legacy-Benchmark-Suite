import { forwardRef } from 'react';
import type { ButtonHTMLAttributes } from 'react';
import { cva } from 'class-variance-authority';
import type { VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-[#22D3EE] text-[#0F172A] hover:bg-[#22D3EE]/90',
        destructive: 'bg-[#F87171] text-white hover:bg-[#F87171]/90',
        outline:
          'border border-[#334155] bg-transparent text-[#CBD5E1] hover:bg-[#1E293B] hover:text-white',
        secondary: 'bg-[#1E293B] text-[#CBD5E1] hover:bg-[#334155]',
        ghost: 'text-[#CBD5E1] hover:bg-[#1E293B] hover:text-white',
        link: 'text-[#22D3EE] underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-10 px-4 py-2',
        sm: 'h-9 rounded-md px-3',
        lg: 'h-11 rounded-md px-8',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
);

interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => {
    return (
      <button className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />
    );
  }
);
Button.displayName = 'Button';

export { Button, buttonVariants };
