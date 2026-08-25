'use strict';

const Decimal = require('decimal.js');
const { fileResult, validationResult } = require('./result');

function transactionValidation(message, operand = '') {
  return { ...validationResult(message), operand };
}

function positionRejected(message) {
  return {
    status: '00',
    result: 'positionRejected',
    http: 400,
    rejected: true,
    applied: false,
    message,
    operand: '',
  };
}

function withPositionFields(record) {
  // PORTFLIO bytes 98..113 are PORT-FILLER spaces, not valid packed values; zero is derived.
  return {
    ...record,
    totalUnits: new Decimal(record.totalUnits || 0),
    totalCost: new Decimal(record.totalCost || 0),
  };
}

function initializePositions(store) {
  for (const [key, record] of store.records.entries()) {
    store.records.set(key, withPositionFields(record));
  }
}

function validateTransaction(transaction, { store }) {
  const input = transaction || {};
  const id = String(input.portId !== undefined ? input.portId : input.portfolioId || '');
  if (!id.trim()) return transactionValidation('Portfolio ID is required');
  const portfolio = store.findById(id);
  if (portfolio.status !== '00') return transactionValidation('Invalid Portfolio ID', id);
  const type = String(input.type || '');
  if (!['BU', 'SL', 'TR', 'FE'].includes(type)) {
    return transactionValidation('Invalid Transaction Type', type);
  }
  let quantity;
  let price;
  let amount;
  try {
    quantity = new Decimal(input.quantity || 0);
    price = new Decimal(input.price || 0);
    amount = new Decimal(input.amount || 0);
  } catch {
    return transactionValidation('Amount must be greater than zero');
  }
  if (!quantity.gt(0)) return transactionValidation('Quantity must be greater than zero');
  if (!price.gt(0) && type !== 'TR') return transactionValidation('Price must be greater than zero');
  if (!amount.gt(0) && type !== 'TR') return transactionValidation('Amount must be greater than zero');
  return {
    status: '00',
    portfolio: portfolio.record,
    key: portfolio.key,
    id,
    type,
    quantity,
    amount,
  };
}

function processBuy(record, quantity, amount) {
  const positions = withPositionFields(record);
  return {
    ...positions,
    totalUnits: positions.totalUnits.plus(quantity),
    totalCost: positions.totalCost.plus(amount),
  };
}

function processSell(record, quantity, amount) {
  const positions = withPositionFields(record);
  const units = positions.totalUnits;
  if (units.lt(quantity)) return positionRejected('Insufficient units for sale');
  return {
    ...positions,
    totalUnits: units.minus(quantity),
    totalCost: positions.totalCost.minus(amount),
  };
}

function processTransfer() {
  return positionRejected('Transfer processing not implemented');
}

function processFee(record, amount) {
  const positions = withPositionFields(record);
  return {
    ...positions,
    totalCost: positions.totalCost.minus(amount),
  };
}

function updateAudit(context, checked) {
  context.auditStore.append({
    timestamp: context.runDate,
    action: checked.type === 'BU' ? 'CREATE' : checked.type === 'SL' ? 'DELETE' : 'UPDATE',
    key: checked.key,
    reason: checked.type,
    status: 'SUCC',
  });
}

function updatePositions(transaction, context, checked) {
  if (context.mode === 'legacy') {
    return fileResult('00', { processed: 1, record: checked.portfolio });
  }
  initializePositions(context.store);
  let updated;
  switch (checked.type) {
    case 'BU': updated = processBuy(checked.portfolio, checked.quantity, checked.amount); break;
    case 'SL': {
      const result = processSell(checked.portfolio, checked.quantity, checked.amount);
      if (result.result === 'positionRejected') {
        updateAudit(context, checked);
        return result;
      }
      updated = result;
      break;
    }
    case 'FE': updated = processFee(checked.portfolio, checked.amount); break;
    case 'TR': {
      const result = processTransfer();
      updateAudit(context, checked);
      return result;
    }
    default: return validationResult(`Invalid Transaction Type: ${checked.type}`);
  }
  const saved = context.store.rewrite(updated);
  if (saved.status !== '00') return fileResult(saved.status);
  updateAudit(context, checked);
  return fileResult('00', { processed: 1, record: saved.record, applied: true });
}

function processTransaction(transaction, context) {
  const checked = validateTransaction(transaction, context);
  if (checked.result === 'validationError') return checked;
  return updatePositions(transaction, context, checked);
}

module.exports = {
  validateTransaction,
  updatePositions,
  processTransaction,
  processBuy,
  processSell,
  processTransfer,
  processFee,
};
