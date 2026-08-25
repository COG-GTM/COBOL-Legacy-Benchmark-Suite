'use strict';

/**
 * Normalization layer for the COBOL <-> JS parity harness.
 *
 * COBOL emits fixed-width, space-padded, sign-prefixed, zero-padded text and packed-decimal
 * bytes. JS emits objects. Neither is comparable to the other raw, so both sides are reduced
 * to the same canonical shapes here, per modernized/CONTRACTS.md section 3:
 *
 *   - X(n)      -> trailing spaces stripped; all-spaces becomes ''
 *   - 9(n) date -> '00000000' becomes null, otherwise the digit string
 *   - COMP-3    -> compared as a Decimal rendered at the field's own scale, never as raw
 *                  bytes and never as a JS float
 *
 * Nothing here parses the *JS* side: the JS side is asked for canonical objects directly.
 * This module only decodes the COBOL side and defines the comparison rules.
 */

const Decimal = require('decimal.js');

const RUN_DATE_SENTINEL = '@RUNDATE';

/** X(n) -> string. COBOL right-pads with spaces; that padding carries no meaning. */
function alnum(raw) {
  return String(raw === undefined || raw === null ? '' : raw).replace(/\s+$/, '');
}

/** 9(8) date -> 'YYYYMMDD' or null. All zeros is COBOL's "never set". */
function date(raw) {
  const text = alnum(raw);
  return /^0*$/.test(text) ? null : text;
}

/**
 * Any COBOL numeric rendering -> canonical decimal string at `scale`.
 * Handles the leading-sign edited forms GOLDDUMP and PORTREAD emit
 * ('        1250000.00', '+0000001250000.00', '-0000000780000.00') and plain digit strings.
 */
function decimal(raw, scale) {
  const text = alnum(raw).replace(/\s+/g, '');
  if (text === '') return null;
  const value = new Decimal(text);
  return value.toFixed(scale);
}

/** Decimal-aware equality, so 1.50 and 1.5 and Decimal('1.500') all agree. */
function decimalEquals(left, right, scale) {
  if (left === null || right === null) return left === right;
  return new Decimal(left).toFixed(scale) === new Decimal(right).toFixed(scale);
}

// ---------------------------------------------------------------------------
// golden/expected/portvald.txt  --  VAL|<case>|<type>|<input>|<rc>|<message>
// ---------------------------------------------------------------------------
function parseValidationBaseline(text) {
  return text
    .split('\n')
    .filter((line) => line.startsWith('VAL|'))
    .map((line) => {
      const [, caseId, type, input, returnCode, message] = line.split('|');
      return {
        caseId: alnum(caseId),
        type: alnum(type),
        input: alnum(input),
        returnCode: Number(alnum(returnCode)),
        message: alnum(message),
      };
    });
}

// ---------------------------------------------------------------------------
// GOLDDUMP output  --  PORT|... / PORTCOUNT|n / AUD|... / AUDCOUNT|n
// AUD-TIMESTAMP is deliberately absent from the dump: it is wall-clock and unreproducible.
// ---------------------------------------------------------------------------
function parseStateDump(text) {
  const portfolios = [];
  const audits = [];
  let portCount = null;
  let auditCount = 0;

  for (const line of text.split('\n')) {
    const parts = line.split('|');
    switch (parts[0]) {
      case 'PORT':
        portfolios.push({
          portId: alnum(parts[1]),
          accountNo: alnum(parts[2]),
          clientName: alnum(parts[3]),
          clientType: alnum(parts[4]),
          createDate: date(parts[5]),
          lastMaint: date(parts[6]),
          status: alnum(parts[7]),
          totalValue: decimal(parts[8], 2),
          cashBalance: decimal(parts[9], 2),
          lastUser: alnum(parts[10]),
          lastTrans: date(parts[11]),
        });
        break;
      case 'PORTCOUNT':
        portCount = Number(alnum(parts[1]));
        break;
      case 'AUD':
        audits.push({
          action: alnum(parts[1]),
          key: alnum(parts[2]),
          reason: alnum(parts[3]),
          status: alnum(parts[4]),
        });
        break;
      case 'AUDCOUNT':
        auditCount = Number(alnum(parts[1]));
        break;
      default:
        break;
    }
  }
  return { portfolios, portCount, audits, auditCount };
}

// ---------------------------------------------------------------------------
// PORTREAD stdout  --  a labelled block per record, then a total.
// ---------------------------------------------------------------------------
function parseReadListing(text) {
  const records = [];
  let current = null;
  let total = null;

  for (const line of text.split('\n')) {
    const record = /^Portfolio Record: (\d+)/.exec(line);
    if (record) {
      current = { sequence: Number(record[1]) };
      records.push(current);
      continue;
    }
    const total_ = /^Total Records Read:\s*(\d+)/.exec(line);
    if (total_) {
      total = Number(total_[1]);
      continue;
    }
    const field = /^ {2}(ID|Account|Client|Status|Total Value): ?(.*)$/.exec(line);
    if (field && current) {
      const [, label, raw] = field;
      if (label === 'ID') current.portId = alnum(raw);
      if (label === 'Account') current.accountNo = alnum(raw);
      if (label === 'Client') current.clientName = alnum(raw);
      if (label === 'Status') current.status = alnum(raw);
      if (label === 'Total Value') current.totalValue = decimal(raw, 2);
    }
  }
  return { records, total };
}

/**
 * Batch summary counters. COBOL prints 'Label: 0000001' with a zero-padded PIC 9(7); the
 * padding and the exact column alignment are formatting, not behaviour, so only the label
 * and the integer are compared.
 */
function parseCounters(text) {
  const counters = {};
  for (const line of text.split('\n')) {
    const match = /^([A-Za-z][A-Za-z ]*[A-Za-z]):\s*(\d+)\s*$/.exec(line);
    if (match) counters[match[1].trim()] = Number(match[2]);
  }
  return counters;
}

/**
 * Per-record diagnostic messages, in emission order. The trailing operand of each DISPLAY is
 * a fixed-width field, so it is trimmed; the message text itself is compared verbatim.
 */
function parseMessages(text) {
  return text
    .split('\n')
    .map((line) => line.replace(/\s+$/, ''))
    .filter((line) => line !== '' && !/^[A-Za-z][A-Za-z ]*[A-Za-z]:\s*\d+\s*$/.test(line))
    .map((line) => {
      const match = /^([^:]+):\s?(.*)$/.exec(line);
      return match
        ? { text: match[1].trim(), operand: match[2].replace(/\s+$/, '') }
        : { text: line.trim(), operand: '' };
    });
}

/**
 * PORTADD stamps PORT-CREATE-DATE and PORT-LAST-MAINT from ACCEPT ... FROM DATE, i.e. the
 * run date. That is the only genuinely nondeterministic field in the executed baseline, so
 * both sides substitute a sentinel for it rather than pretending it is stable.
 */
function maskRunDate(portfolio, runDate) {
  const mask = (value) => (value !== null && value === runDate ? RUN_DATE_SENTINEL : value);
  return { ...portfolio, createDate: mask(portfolio.createDate), lastMaint: mask(portfolio.lastMaint) };
}

module.exports = {
  RUN_DATE_SENTINEL,
  alnum,
  date,
  decimal,
  decimalEquals,
  parseValidationBaseline,
  parseStateDump,
  parseReadListing,
  parseCounters,
  parseMessages,
  maskRunDate,
};
