'use strict';

const { keyOf } = require('../canonical');
const { fileResult } = require('./result');

function writeAudit({ auditStore, runDate }, key, reasonCode, status) {
  return auditStore.append({
    timestamp: runDate,
    action: 'DELETE',
    key,
    reason: reasonCode,
    status,
  });
}

function deleteRecord(input, context) {
  const current = context.store.read(input.key);
  if (current.status !== '00') return fileResult(current.status);
  const deleted = context.store.delete(input.key);
  if (deleted.status === '00') {
    writeAudit(context, keyOf(input.key), input.reasonCode, current.record.status);
  }
  return fileResult(deleted.status, { record: deleted.record });
}

function processDelete(input, context) {
  return deleteRecord(input, context);
}

module.exports = { processDelete, deleteRecord, writeAudit };
