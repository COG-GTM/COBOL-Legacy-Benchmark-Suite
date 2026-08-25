'use strict';

const { canonicalize } = require('../canonical');
const { fileResult, validationResult } = require('./result');

function validateAndAdd(input, { store, runDate }) {
  const record = canonicalize(input);
  if (!String(record.portId || '').trim()
      || !String(record.clientName || '').trim()
      || record.status !== 'A') {
    return validationResult('Invalid record data');
  }
  record.createDate = runDate;
  record.lastMaint = runDate;
  const written = store.write(record);
  return fileResult(written.status, { record: written.record }, true);
}

module.exports = { validateAndAdd };
