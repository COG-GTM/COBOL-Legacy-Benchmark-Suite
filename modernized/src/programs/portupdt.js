'use strict';

const Decimal = require('decimal.js');
const { fileResult, validationResult } = require('./result');

function applyUpdate(record, action, newValue, mode = 'legacy') {
  const updated = { ...record };
  switch (action) {
    case 'S': updated.status = String(newValue || '').charAt(0); break;
    case 'N': updated.clientName = String(newValue || '').slice(0, 30); break;
    case 'V':
      try {
        updated.totalValue = new Decimal(String(newValue === undefined ? '' : newValue).trim());
      } catch {
        return null;
      }
      break;
    default:
      if (mode === 'modernized') return validationResult('Invalid update action');
      break;
  }
  return updated;
}

function processUpdate(input, { store, mode }) {
  const existing = store.read(input.key);
  if (existing.status !== '00') return fileResult(existing.status);
  const updated = applyUpdate(existing.record, input.updateAction, input.newValue, mode);
  if (updated && updated.result === 'validationError') return updated;
  if (!updated) return validationResult('Invalid numeric value');
  const rewritten = store.rewrite(updated);
  return fileResult(rewritten.status, { record: rewritten.record });
}

module.exports = { processUpdate, applyUpdate };
