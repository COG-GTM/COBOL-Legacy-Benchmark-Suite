import { decimalStringToScaled } from '../codec/comp3.js';
import { fileFromSeed, text, unchangedState } from './common.js';

const VALID_TYPES = new Set(['BU', 'SL', 'TR', 'FE']);

function transactionError(transaction, message) {
  return {
    kind: 'transaction-error',
    portfolioId: text(transaction.portfolioId).trimEnd(),
    sequenceNo: transaction.sequenceNo,
    message,
  };
}

export function handler({ seed, input }) {
  const file = fileFromSeed(seed);
  const counters = { read: 0, processed: 0, errors: 0 };
  const events = [];

  for (const transaction of input) {
    counters.read += 1;
    const portfolioId = text(transaction.portfolioId).trimEnd();
    let message = '';

    if (!portfolioId) {
      message = 'Portfolio ID is required';
    } else if (!file.browse().some((record) => text(record.portId).trimEnd() === portfolioId)) {
      message = `Invalid Portfolio ID: ${portfolioId}`;
    } else if (!VALID_TYPES.has(text(transaction.type).trimEnd())) {
      message = `Invalid Transaction Type: ${text(transaction.type).trimEnd()}`;
    } else if (decimalStringToScaled(transaction.quantity, 4) <= 0n) {
      message = 'Quantity must be greater than zero';
    } else if (
      text(transaction.type).trimEnd() !== 'TR' &&
      decimalStringToScaled(transaction.price, 4) <= 0n
    ) {
      message = 'Price must be greater than zero';
    } else if (
      text(transaction.type).trimEnd() !== 'TR' &&
      decimalStringToScaled(transaction.amount, 2) <= 0n
    ) {
      message = 'Amount must be greater than zero';
    }

    if (message) {
      counters.errors += 1;
      events.push(transactionError(transaction, message));
      if (counters.errors > 100) break;
    } else {
      counters.processed += 1;
    }
  }

  return { counters, events, finalState: unchangedState(file), audit: null };
}
