interface MessageProps {
  variant: 'error' | 'info';
  children: React.ReactNode;
}

/**
 * Inline status message. The "error" variant mirrors the red BMS message lines
 * (POSMSG / HISMSG / ERRMAP) used by the COBOL error and "not found" flows.
 */
export function Message({ variant, children }: MessageProps) {
  return (
    <p className={`message message--${variant}`} role={variant === 'error' ? 'alert' : 'status'}>
      {children}
    </p>
  );
}
