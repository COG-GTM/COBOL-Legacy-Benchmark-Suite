import { messages } from '../i18n/messages';

/** BR-08: code plus label (e.g. "BU – Buy"); unknown codes are shown raw. */
export function transactionTypeLabel(code: string): string {
  const label = messages.history.typeLabel[code];
  return label ? `${code} – ${label}` : code;
}

/** BR-11 status badge text; unknown codes are shown raw. */
export function positionStatusLabel(code: string): string {
  return messages.positions.statusLabel[code] ?? code;
}
