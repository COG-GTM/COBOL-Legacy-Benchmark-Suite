'use strict';

const Decimal = require('decimal.js');
const { fileResult, validationResult } = require('./result');

function validateTransaction(transaction, { store }) {
  const id = String(transaction.portfolioId || '');
  if (!id.trim()) return validationResult('Portfolio ID is required');
  const portfolio = store.findById(id);
  if (portfolio.status !== '00') return validationResult(`Invalid Portfolio ID: ${id}`);
  const type = String(transaction.type || '');
  if (!['BU', 'SL', 'TR', 'FE'].includes(type)) {
    return validationResult(`Invalid Transaction Type: ${type}`);
  }
  let quantity;
  let price;
  let amount;
  try {
    quantity = new Decimal(transaction.quantity || 0);
    price = new Decimal(transaction.price || 0);
    amount = new Decimal(transaction.amount || 0);
  } catch {
    return validationResult('Amount must be greater than zero');
  }
  if (!quantity.gt(0)) return validationResult('Quantity must be greater than zero');
  if (!price.gt(0) && type !== 'TR') return validationResult('Price must be greater than zero');
  if (!amount.gt(0) && type !== 'TR') return validationResult('Amount must be greater than zero');
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
  return {
    ...record,
    totalUnits: new Decimal(record.totalUnits || 0).plus(quantity),
    totalCost: new Decimal(record.totalCost || 0).plus(amount),
  };
}

function processSell(record, quantity, amount) {
  const units = new Decimal(record.totalUnits || 0);
  if (units.lt(quantity)) return validationResult('Insufficient units for sale');
  return {
    ...record,
    totalUnits: units.minus(quantity),
    totalCost: new Decimal(record.totalCost || 0).minus(amount),
  };
}

function processTransfer() {
  return validationResult('Transfer processing not implemented');
}

function processFee(record, amount) {
  return {
    ...record,
    totalCost: new Decimal(record.totalCost || 0).minus(amount),
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
  let updated;
  switch (checked.type) {
    case 'BU': updated = processBuy(checked.portfolio, checked.quantity, checked.amount); break;
    case 'SL': {
      const result = processSell(checked.portfolio, checked.quantity, checked.amount);
      if (result.result === 'validationError') {
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
  return fileResult('00', { processed: 1, record: saved.record });
}

function processTransaction(transaction, context) {
  const checked = validateTransaction(transaction, context);
  if (checked.status !== '00') return checked;
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
