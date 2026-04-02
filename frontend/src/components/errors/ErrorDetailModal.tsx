import { useEffect, useRef, type CSSProperties } from 'react';

export interface ErrorDetailModalProps {
  /** Whether the modal is open. */
  open: boolean;
  /** Error code – maps to ERRCOUT (8 chars) from ERRMAP. */
  errorCode: string;
  /** Error details – maps to ERRDOUT (65 chars) from ERRMAP. */
  errorDetails: string;
  /** Called when the user dismisses the modal. */
  onClose: () => void;
}

const overlayStyle: CSSProperties = {
  position: 'fixed',
  inset: 0,
  backgroundColor: 'rgba(0,0,0,0.5)',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  zIndex: 10000,
};

const dialogStyle: CSSProperties = {
  backgroundColor: '#fff',
  borderRadius: 8,
  padding: 24,
  maxWidth: 520,
  width: '90%',
  boxShadow: '0 4px 16px rgba(0,0,0,0.25)',
};

const headingStyle: CSSProperties = {
  margin: '0 0 16px',
  color: '#d32f2f',
};

const labelStyle: CSSProperties = {
  fontWeight: 600,
  display: 'block',
  marginBottom: 4,
  color: '#333',
};

const valueStyle: CSSProperties = {
  color: '#d32f2f',
  marginBottom: 16,
  wordBreak: 'break-word',
};

const buttonStyle: CSSProperties = {
  padding: '8px 24px',
  backgroundColor: '#1976d2',
  color: '#fff',
  border: 'none',
  borderRadius: 4,
  cursor: 'pointer',
  fontSize: '1rem',
};

/**
 * Modal dialog for critical / system errors requiring user acknowledgment.
 *
 * Replaces the legacy ERRMAP screen from src/maps/INQSET.bms (lines 89-99),
 * with fields ERRCOUT (error code) and ERRDOUT (error details).
 * The legacy "Press ENTER to continue" prompt is replaced by a "Continue" button.
 */
export default function ErrorDetailModal({
  open,
  errorCode,
  errorDetails,
  onClose,
}: ErrorDetailModalProps) {
  const btnRef = useRef<HTMLButtonElement>(null);

  // Trap focus to the Continue button when the modal opens
  useEffect(() => {
    if (open) {
      btnRef.current?.focus();
    }
  }, [open]);

  // Support Escape key to close
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      style={overlayStyle}
      role="dialog"
      aria-modal="true"
      aria-label="System Error"
      data-testid="error-modal-overlay"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div style={dialogStyle} data-testid="error-modal">
        <h2 style={headingStyle}>System Error</h2>

        <span style={labelStyle}>Error Code:</span>
        <p style={valueStyle} data-testid="error-modal-code">
          {errorCode}
        </p>

        <span style={labelStyle}>Details:</span>
        <p style={valueStyle} data-testid="error-modal-details">
          {errorDetails}
        </p>

        <button
          ref={btnRef}
          style={buttonStyle}
          onClick={onClose}
          data-testid="error-modal-continue"
        >
          Continue
        </button>
      </div>
    </div>
  );
}
