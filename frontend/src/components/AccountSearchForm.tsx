import { forwardRef, type FormEvent } from 'react';
import { messages } from '../i18n/messages';

interface Props {
  idPrefix: string;
  value: string;
  onChange: (value: string) => void;
  onSubmit: (value: string) => void;
  invalid: boolean;
  messageId: string;
  disabled?: boolean;
}

/**
 * Shared account search bar for S2 and S3 (ACCTIN / HISAIN, X(10), initial cursor).
 * No maxLength attribute: an over-long value is rejected with the BR-01 message instead of being
 * silently truncated (FR-007 requires an 11-digit value to be rejected).
 */
export const AccountSearchForm = forwardRef<HTMLInputElement, Props>(function AccountSearchForm(
  { idPrefix, value, onChange, onSubmit, invalid, messageId, disabled },
  ref,
) {
  const inputId = `${idPrefix}-account`;
  const hintId = `${idPrefix}-account-hint`;

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit(value);
  }

  return (
    <form className="search-form" onSubmit={handleSubmit} noValidate role="search" aria-label={messages.account.label.replace(':', '')}>
      <label htmlFor={inputId} className="search-form__label">
        {messages.account.label}
      </label>
      <div className="search-form__row">
        <input
          ref={ref}
          id={inputId}
          name="accountNo"
          type="text"
          inputMode="numeric"
          autoComplete="off"
          spellCheck={false}
          autoFocus
          className="input input--account"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          aria-invalid={invalid || undefined}
          aria-describedby={`${hintId} ${messageId}`}
        />
        <button type="submit" className="btn btn--primary" disabled={disabled}>
          {messages.account.search}
        </button>
      </div>
      <p id={hintId} className="hint">
        {messages.account.hint}
      </p>
    </form>
  );
});
