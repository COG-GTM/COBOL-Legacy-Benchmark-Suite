import { useState } from 'react';
import { messages } from '../i18n/messages';

/** FR-030: the trace ID is visible and copyable for support (legacy ERR-TRACE-ID). */
export function TraceId({ traceId }: { traceId: string }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(traceId);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }

  return (
    <p className="trace-id">
      <span>{messages.errors.traceId}</span> <code data-testid="trace-id">{traceId}</code>{' '}
      <button type="button" className="btn btn--small btn--ghost" onClick={copy} aria-label={`${messages.errors.copyTraceId} ${traceId}`}>
        {copied ? messages.errors.copied : messages.errors.copyTraceId}
      </button>
      <span className="visually-hidden" aria-live="polite">
        {copied ? messages.errors.copied : ''}
      </span>
    </p>
  );
}
