import type { CSSProperties } from 'react';

export interface OfflineStateProps {
  /** Called when the user clicks the Retry button. */
  onRetry: () => void;
}

const containerStyle: CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  padding: 48,
  textAlign: 'center',
  minHeight: 300,
};

const messageStyle: CSSProperties = {
  fontSize: '1.2rem',
  color: '#555',
  marginBottom: 24,
};

const buttonStyle: CSSProperties = {
  padding: '10px 28px',
  backgroundColor: '#1976d2',
  color: '#fff',
  border: 'none',
  borderRadius: 4,
  cursor: 'pointer',
  fontSize: '1rem',
};

/**
 * Graceful-degradation component displayed when the backend is unavailable.
 */
export default function OfflineState({ onRetry }: OfflineStateProps) {
  return (
    <div data-testid="offline-state" style={containerStyle} role="alert">
      <p style={messageStyle}>
        System is temporarily unavailable. Please try again later.
      </p>
      <button
        style={buttonStyle}
        onClick={onRetry}
        data-testid="offline-retry"
      >
        Retry
      </button>
    </div>
  );
}
