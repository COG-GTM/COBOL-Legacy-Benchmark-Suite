import { messages } from '../i18n/messages';

/** FR-012: dash with accessible text "Not available". */
export function NotAvailable() {
  return (
    <span>
      <span aria-hidden="true">—</span>
      <span className="visually-hidden">{messages.app.notAvailable}</span>
    </span>
  );
}
