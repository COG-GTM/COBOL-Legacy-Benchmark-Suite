import { useEffect, useState } from 'react';
import { messages } from '../i18n/messages';

/** Loading indicator that appears only after `delayMs` (NFR-03: shown after 200 ms). */
export function Loading({ delayMs = 200, label = messages.app.loading }: { delayMs?: number; label?: string }) {
  const [visible, setVisible] = useState(delayMs <= 0);
  useEffect(() => {
    if (delayMs <= 0) return;
    const t = window.setTimeout(() => setVisible(true), delayMs);
    return () => window.clearTimeout(t);
  }, [delayMs]);

  return (
    <div role="status" aria-live="polite" className="loading">
      {visible && (
        <>
          <span className="spinner" aria-hidden="true" />
          <span>{label}</span>
        </>
      )}
    </div>
  );
}
