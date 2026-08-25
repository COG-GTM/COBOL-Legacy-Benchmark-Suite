import { moveAlphanumericToScaled } from '../codec/cobol-move.js';
import { fileFromSeed } from './common.js';

const MESSAGES = {
  id: 'Invalid Portfolio ID format',
  account: 'Invalid Account Number format',
  type: 'Invalid Investment Type',
  amount: 'Amount outside valid range',
  validation: 'Invalid validation type',
};

const MIN_AMOUNT = -999999999999999n;
const MAX_AMOUNT = 999999999999999n;

function validation(inputValue, validateType) {
  const linkageValue = String(inputValue ?? '').padEnd(50, ' ');
  if (validateType === 'I') {
    if (linkageValue.slice(0, 4) !== 'PORT') {
      return { returnCode: 1, message: MESSAGES.id };
    }
    // COBOL moves four chars into X(10); the six moved-in spaces make
    // NUMERIC false even when the four source chars are digits.
    const numericCheck = linkageValue.slice(4, 8).padEnd(10, ' ');
    if (!/^\d{10}$/.test(numericCheck)) {
      return { returnCode: 1, message: MESSAGES.id };
    }
    return { returnCode: 0, message: '' };
  }

  if (validateType === 'A') {
    if (!/^\d{50}$/.test(linkageValue) || /^0{50}$/.test(linkageValue)) {
      return { returnCode: 2, message: MESSAGES.account };
    }
    return { returnCode: 0, message: '' };
  }

  if (validateType === 'T') {
    return ['STK', 'BND', 'MMF', 'ETF'].includes(linkageValue.trim())
      ? { returnCode: 0, message: '' }
      : { returnCode: 3, message: MESSAGES.type };
  }

  if (validateType === 'M') {
    const scaled = moveAlphanumericToScaled(linkageValue);
    return scaled < MIN_AMOUNT || scaled > MAX_AMOUNT
      ? { returnCode: 4, message: MESSAGES.amount }
      : { returnCode: 0, message: '' };
  }

  return { returnCode: 1, message: MESSAGES.validation };
}

export function handler({ input }) {
  const events = input.map((request) => {
    const result = validation(request.inputValue, request.validateType);
    return {
      kind: 'validation',
      caseId: request.caseId,
      validateType: request.validateType,
      returnCode: result.returnCode,
      message: result.message,
    };
  });

  return {
    counters: { validations: input.length },
    events,
    finalState: null,
    audit: null,
  };
}
