export const ACCOUNT_NO_LENGTH = 10;

export type ParsedAccountInput =
  | { ok: true; digits: string }
  | { ok: false; digits: string; message: string };

/** Validates lookup input against the 10-character INQCOM-ACCOUNT-NO / ACCTIN field. */
export function parseAccountInput(raw: string): ParsedAccountInput {
  const trimmed = raw.trim();
  if (!trimmed) return { ok: false, digits: '', message: 'Enter a 10-digit account number, like 4471-0028-93.' };
  if (/[^\d\s-]/.test(trimmed)) return { ok: false, digits: '', message: 'Account numbers contain digits only.' };
  const digits = trimmed.replace(/\D/g, '');
  return { ok: true, digits };
}

/** Resolves full numbers, or a 4+ digit suffix that matches exactly one known account. */
export function resolveAccount(digits: string, known: string[]): string | null {
  if (digits.length === ACCOUNT_NO_LENGTH) return digits;
  if (digits.length >= 4) {
    const matches = known.filter((no) => no.endsWith(digits));
    if (matches.length === 1) return matches[0];
  }
  return null;
}

export function formatAccountNo(no: string): string {
  return no.length === ACCOUNT_NO_LENGTH ? `${no.slice(0, 4)}-${no.slice(4, 8)}-${no.slice(8)}` : no;
}
