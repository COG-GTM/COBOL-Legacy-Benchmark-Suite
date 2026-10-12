import { messages } from '../i18n/messages';

/** BR-01 / FR-007: required, trimmed, 1-10 digits, not all zeros (Q-01 default). */
export const ACCOUNT_NO_RE = /^[0-9]{1,10}$/;

export function normaliseAccountNo(raw: string): string {
  return raw.trim();
}

export function validateAccountNo(raw: string): string | null {
  const value = normaliseAccountNo(raw);
  if (!ACCOUNT_NO_RE.test(value) || /^0+$/.test(value)) return messages.errors.INVALID_ACCOUNT_FORMAT;
  return null;
}

export function isValidAccountNo(raw: string): boolean {
  return validateAccountNo(raw) === null;
}

/** Parses the ?page= query value; anything that is not a positive integer becomes page 1. */
export function parsePageParam(raw: string | null): number {
  if (!raw || !/^\d+$/.test(raw)) return 1;
  const page = Number(raw);
  return page >= 1 ? page : 1;
}
