import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { ApiError } from '../api/types';
import { TraceId } from '../components/TraceId';
import { messages } from '../i18n/messages';
import { useDocumentTitle } from '../state/useDocumentTitle';

const M = messages.systemError;

/** S4 System Error (ERRMAP): Error Code, Details, trace ID and a focused Continue (ENTER) button. */
export function SystemErrorView({ error, onContinue }: { error: ApiError; onContinue: () => void }) {
  const buttonRef = useRef<HTMLButtonElement>(null);
  useEffect(() => buttonRef.current?.focus(), []);

  return (
    <section className="card system-error" aria-labelledby="system-error-title" role="alertdialog" aria-describedby="system-error-details">
      <h1 id="system-error-title" className="page-title">
        <span className="message__icon message__icon--fatal" aria-hidden="true">×</span> {M.title}
      </h1>
      <dl className="detail-grid detail-grid--stacked">
        <div className="detail-grid__item">
          <dt>{M.code}</dt>
          <dd className="mono" data-testid="error-code">{error.code}</dd>
        </div>
        <div className="detail-grid__item">
          <dt>{M.details}</dt>
          <dd id="system-error-details">{error.message}</dd>
        </div>
      </dl>
      {error.traceId && <TraceId traceId={error.traceId} />}
      <p className="hint">{M.prompt}</p>
      <button ref={buttonRef} type="button" className="btn btn--primary" onClick={onContinue}>
        {M.continue}
      </button>
    </section>
  );
}

const GENERIC_ERROR: ApiError = {
  code: 'SYSTEM_ERROR',
  message: messages.errors.SYSTEM_ERROR,
  severity: 'FATAL',
  action: 'ABEND',
  traceId: '',
  timestamp: '',
};

export function SystemErrorScreen() {
  useDocumentTitle(M.title);
  const navigate = useNavigate();
  const location = useLocation();
  const error = (location.state as { error?: ApiError } | null)?.error ?? GENERIC_ERROR;
  return <SystemErrorView error={error} onContinue={() => navigate('/', { replace: true })} />;
}
