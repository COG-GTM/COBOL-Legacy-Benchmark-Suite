import type { CSSProperties } from 'react';

export interface InlineErrorProps {
  /** The message to display. */
  message: string;
  /** Severity level – controls colour and ARIA role. */
  severity: 'error' | 'warning' | 'info';
  /** Whether the message is currently visible. */
  visible: boolean;
}

const SEVERITY_STYLES: Record<InlineErrorProps['severity'], CSSProperties> = {
  error: { color: '#d32f2f' },   // red – matches legacy COLOR=RED
  warning: { color: '#ed6c02' }, // orange
  info: { color: '#0288d1' },    // blue
};

/**
 * Inline validation message rendered adjacent to a form field.
 *
 * Replaces the legacy BMS fields ERRMSG (line 19), POSMSG (line 49),
 * and HISMSG (line 85) from src/maps/INQSET.bms.
 */
export default function InlineError({ message, severity, visible }: InlineErrorProps) {
  if (!visible) return null;

  return (
    <span
      role="alert"
      data-testid="inline-error"
      style={{
        fontSize: '0.85rem',
        marginTop: 4,
        display: 'block',
        ...SEVERITY_STYLES[severity],
      }}
    >
      {message}
    </span>
  );
}
