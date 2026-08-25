'use strict';

const Decimal = require('decimal.js');

const MESSAGES = {
  id: 'Invalid Portfolio ID format',
  account: 'Invalid Account Number format',
  type: 'Invalid Investment Type',
  amount: 'Amount outside valid range',
  unknown: 'Invalid validation type',
};

const RETURN_CODES = { success: 0, id: 1, account: 2, type: 3, amount: 4 };
const divergences = [
  { type: 'I', reason: 'Legacy moves four digits into a ten-byte numeric check padded with spaces.' },
  { type: 'A', reason: 'Legacy tests all 50 bytes, including trailing spaces, for numeric content.' },
  { type: 'M', reason: 'Legacy alphanumeric-to-numeric MOVE and field-sized bounds make the range check always pass.' },
];

function result(code, message) {
  return { code, message: message || '' };
}

function validateLegacy(type, input) {
  const value = String(input === undefined || input === null ? '' : input);
  switch (String(type || '')) {
    case 'I': return result(RETURN_CODES.id, MESSAGES.id);
    case 'A': return result(RETURN_CODES.account, MESSAGES.account);
    case 'T': return ['STK', 'BND', 'MMF', 'ETF'].includes(value.trim())
      ? result(RETURN_CODES.success) : result(RETURN_CODES.type, MESSAGES.type);
    case 'M': return result(RETURN_CODES.success);
    default: return result(RETURN_CODES.id, MESSAGES.unknown);
  }
}

function validateModernized(type, input) {
  const value = String(input === undefined || input === null ? '' : input).trim();
  switch (String(type || '')) {
    case 'I':
      return /^PORT[0-9]{4}$/.test(value)
        ? result(RETURN_CODES.success) : result(RETURN_CODES.id, MESSAGES.id);
    case 'A':
      return /^[0-9]{10}$/.test(value) && !/^0+$/.test(value)
        ? result(RETURN_CODES.success) : result(RETURN_CODES.account, MESSAGES.account);
    case 'T':
      return ['STK', 'BND', 'MMF', 'ETF'].includes(value)
        ? result(RETURN_CODES.success) : result(RETURN_CODES.type, MESSAGES.type);
    case 'M': {
      try {
        const amount = new Decimal(value);
        const min = new Decimal('-9999999999999.99');
        const max = new Decimal('9999999999999.99');
        return amount.isFinite() && amount.gte(min) && amount.lte(max)
          ? result(RETURN_CODES.success) : result(RETURN_CODES.amount, MESSAGES.amount);
      } catch {
        return result(RETURN_CODES.amount, MESSAGES.amount);
      }
    }
    default: return result(RETURN_CODES.id, MESSAGES.unknown);
  }
}

function validate(type, input, mode = 'legacy') {
  return mode === 'modernized'
    ? validateModernized(type, input) : validateLegacy(type, input);
}

module.exports = {
  MESSAGES,
  RETURN_CODES,
  divergences,
  knownDivergences: divergences,
  validate,
  validateLegacy,
  validateModernized,
};
