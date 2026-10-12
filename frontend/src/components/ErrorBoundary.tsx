import { Component, type ErrorInfo, type ReactNode } from 'react';
import type { ApiError } from '../api/types';
import { messages } from '../i18n/messages';
import { SystemErrorView } from '../screens/SystemErrorScreen';

function clientTraceId(): string {
  let id = '';
  for (let i = 0; i < 16; i++) id += Math.floor(Math.random() * 16).toString(16);
  return id.toUpperCase();
}

interface State {
  error: ApiError | null;
}

/** Replaces ABEND IERR: unexpected client exceptions show S4 instead of crashing the app (FR-027). */
export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { error: null };

  static getDerivedStateFromError(): State {
    return {
      error: {
        code: 'SYSTEM_ERROR',
        message: messages.errors.SYSTEM_ERROR,
        severity: 'FATAL',
        action: 'ABEND',
        traceId: clientTraceId(),
        timestamp: new Date().toISOString(),
      },
    };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ui] unhandled error', this.state.error?.traceId, error, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <main id="main" className="main">
          <SystemErrorView
            error={this.state.error}
            onContinue={() => {
              this.setState({ error: null });
              window.location.assign('/');
            }}
          />
        </main>
      );
    }
    return this.props.children;
  }
}
