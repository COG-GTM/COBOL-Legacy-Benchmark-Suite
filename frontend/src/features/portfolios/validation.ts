import {
  CLIENT_TYPES,
  PORTFOLIO_FIELD_LENGTHS,
  PORTFOLIO_STATUSES,
  type ClientType,
  type PortfolioInput,
  type PortfolioStatus,
} from '../../types/portfolio';
import { validateDecimal } from '../../utils/decimal';

export type PortfolioErrors = Partial<Record<keyof PortfolioInput, string>>;

const ALPHANUM_RE = /^[A-Za-z0-9]+$/;

/**
 * Validates user-editable portfolio fields against the PORTFLIO.cpy field
 * definitions. Mirrors the validation the COBOL PORTADD / PORTUPDT programs
 * would perform before writing the VSAM record.
 *
 * @param values     the form values
 * @param isCreate   whether PORT-ID should be validated/required (it is the
 *                   immutable key, only set at create time)
 */
export function validatePortfolio(
  values: PortfolioInput,
  isCreate: boolean,
): PortfolioErrors {
  const errors: PortfolioErrors = {};

  if (isCreate) {
    const portId = values.portId.trim();
    if (!portId) {
      errors.portId = 'Portfolio ID is required.';
    } else if (portId.length > PORTFOLIO_FIELD_LENGTHS.portId) {
      errors.portId = `Up to ${PORTFOLIO_FIELD_LENGTHS.portId} characters.`;
    } else if (!ALPHANUM_RE.test(portId)) {
      errors.portId = 'Letters and numbers only.';
    }
  }

  const accountNo = values.accountNo.trim();
  if (!accountNo) {
    errors.accountNo = 'Account number is required.';
  } else if (accountNo.length > PORTFOLIO_FIELD_LENGTHS.accountNo) {
    errors.accountNo = `Up to ${PORTFOLIO_FIELD_LENGTHS.accountNo} characters.`;
  } else if (!ALPHANUM_RE.test(accountNo)) {
    errors.accountNo = 'Letters and numbers only.';
  }

  const clientName = values.clientName.trim();
  if (!clientName) {
    errors.clientName = 'Client name is required.';
  } else if (clientName.length > PORTFOLIO_FIELD_LENGTHS.clientName) {
    errors.clientName = `Up to ${PORTFOLIO_FIELD_LENGTHS.clientName} characters.`;
  }

  if (!CLIENT_TYPES.includes(values.clientType as ClientType)) {
    errors.clientType = 'Select a client type.';
  }

  if (!PORTFOLIO_STATUSES.includes(values.status as PortfolioStatus)) {
    errors.status = 'Select a status.';
  }

  const totalValueError = validateMoney(values.totalValue, 'Total value');
  if (totalValueError) {
    errors.totalValue = totalValueError;
  }

  const cashBalanceError = validateMoney(values.cashBalance, 'Cash balance');
  if (cashBalanceError) {
    errors.cashBalance = cashBalanceError;
  }

  return errors;
}

function validateMoney(value: string, label: string): string | null {
  if (!value.trim()) {
    return `${label} is required.`;
  }
  // S9(13)V99 COMP-3
  return validateDecimal(value, { maxIntDigits: 13, maxFracDigits: 2 });
}

export function hasErrors(errors: PortfolioErrors): boolean {
  return Object.keys(errors).length > 0;
}
