import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';
import { AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="flex min-h-[400px] flex-col items-center justify-center gap-4 p-8">
          <div className="rounded-full bg-[#F87171]/20 p-4">
            <AlertCircle className="h-12 w-12 text-[#F87171]" />
          </div>
          <h2 className="text-xl font-semibold text-white">Something went wrong</h2>
          <p className="max-w-md text-center text-[#94A3B8]">
            {this.state.error?.message ?? 'An unexpected error occurred'}
          </p>
          <Button
            onClick={() => this.setState({ hasError: false, error: null })}
            variant="outline"
          >
            Try Again
          </Button>
          <Button
            variant="ghost"
            onClick={() => (window.location.href = '/')}
          >
            Return to Dashboard
          </Button>
        </div>
      );
    }

    return this.props.children;
  }
}
