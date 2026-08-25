'use strict';

const Decimal = require('decimal.js');

const NAMES = {
  portId: 'PORT-ID',
  accountNo: 'PORT-ACCOUNT-NO',
  clientName: 'PORT-CLIENT-NAME',
  clientType: 'PORT-CLIENT-TYPE',
  createDate: 'PORT-CREATE-DATE',
  lastMaint: 'PORT-LAST-MAINT',
  status: 'PORT-STATUS',
  totalValue: 'PORT-TOTAL-VALUE',
  cashBalance: 'PORT-CASH-BALANCE',
  lastUser: 'PORT-LAST-USER',
  lastTrans: 'PORT-LAST-TRANS',
  totalUnits: 'PORT-TOTAL-UNITS',
  totalCost: 'PORT-TOTAL-COST',
};

const DECIMAL_SCALES = { totalValue: 2, cashBalance: 2, totalUnits: 4, totalCost: 2 };

function pick(record, canonical, legacy) {
  return record[canonical] !== undefined ? record[canonical] : record[legacy];
}

function asDecimal(value) {
  if (value === undefined || value === null || value === '') return new Decimal(0);
  return Decimal.isDecimal(value) ? new Decimal(value) : new Decimal(value);
}

function canonicalize(record = {}) {
  const output = {};
  for (const [canonical, legacy] of Object.entries(NAMES)) {
    const value = pick(record, canonical, legacy);
    if (value !== undefined) output[canonical] = value;
  }
  for (const field of Object.keys(DECIMAL_SCALES)) {
    if (output[field] !== undefined) output[field] = asDecimal(output[field]);
  }
  return output;
}

function keyOf(recordOrKey) {
  if (typeof recordOrKey === 'string' || Buffer.isBuffer(recordOrKey)) {
    return Buffer.isBuffer(recordOrKey) ? recordOrKey.toString('ascii') : recordOrKey;
  }
  const record = canonicalize(recordOrKey);
  return `${record.portId || ''}${record.accountNo || ''}`;
}

function toCanonicalJson(record) {
  const canonical = canonicalize(record);
  const output = {};
  for (const field of Object.keys(NAMES)) {
    if (canonical[field] === undefined) continue;
    output[field] = DECIMAL_SCALES[field] === undefined
      ? canonical[field] : asDecimal(canonical[field]).toFixed(DECIMAL_SCALES[field]);
  }
  return output;
}

module.exports = { NAMES, DECIMAL_SCALES, canonicalize, keyOf, toCanonicalJson };
