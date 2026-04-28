import { Component, type ReactNode, type ErrorInfo } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary caught:', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center bg-gray-50 p-8">
          <div className="max-w-lg w-full bg-white rounded-lg shadow-lg p-8">
            <h1 className="text-2xl font-bold text-red-600 mb-2">System Error</h1>
            <div className="mb-4">
              <p className="text-sm text-gray-500">Error Code:</p>
              <p className="font-mono text-red-700">{this.state.error?.name ?? 'UNKNOWN'}</p>
            </div>
            <div className="mb-6">
              <p className="text-sm text-gray-500">Details:</p>
              <p className="text-gray-700">{this.state.error?.message ?? 'An unexpected error occurred'}</p>
            </div>
            <button
              onClick={() => {
                this.setState({ hasError: false, error: null });
                window.location.href = '/';
              }}
              className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition-colors"
            >
              Press ENTER to continue
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
