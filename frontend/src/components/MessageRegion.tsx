import type { ReactNode } from 'react';
import type { ErrorSeverity } from '../api/types';
import { messages } from '../i18n/messages';
import { TraceId } from './TraceId';

export interface Message {
  text: string;
  severity: ErrorSeverity;
  traceId?: string;
}

const PREFIX: Record<ErrorSeverity, string> = {
  INFO: messages.errors.infoPrefix,
  WARNING: messages.errors.warningPrefix,
  ERROR: messages.errors.errorPrefix,
  FATAL: messages.errors.errorPrefix,
};

const ICON: Record<ErrorSeverity, string> = { INFO: 'i', WARNING: '!', ERROR: '×', FATAL: '×' };

/**
 * Replaces the row-23 message line (ERRMSG / POSMSG / HISMSG). The container is always mounted
 * with role="alert" so screen readers announce each change (FR-006, FR-028). Severity is conveyed by
 * an icon and a text prefix, not by colour alone (NFR-01).
 */
export function MessageRegion({ id, message, action }: { id: string; message: Message | null; action?: ReactNode }) {
  return (
    <div id={id} role="alert" className="message-region" data-testid={id}>
      {message && (
        <div className={`message message--${message.severity.toLowerCase()}`}>
          <span className="message__icon" aria-hidden="true">
            {ICON[message.severity]}
          </span>
          <div className="message__body">
            <p className="message__text">
              <span className="visually-hidden">{PREFIX[message.severity]}: </span>
              {message.text}
            </p>
            {message.traceId && <TraceId traceId={message.traceId} />}
          </div>
          {action && <div className="message__action">{action}</div>}
        </div>
      )}
    </div>
  );
}
