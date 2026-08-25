'use strict';

/**
 * Record layouts translated from src/copybook/common/*.cpy.
 *
 * Offsets and byte lengths are measured against GnuCOBOL (LENGTH OF), not inferred.
 * See modernized/CONTRACTS.md section 1. The copybooks are the source of truth; the
 * simplified layout in documentation/operations/test-data-specs.md is NOT.
 *
 * Field kinds:
 *   'alnum'   PIC X(n)              -> string, space padded right
 *   'digits'  PIC 9(n)              -> string of ASCII digits, zero padded left
 *   'packed'  PIC S9(a)V9(b) COMP-3 -> Decimal, ceil((a+b+1)/2) bytes
 */

const ALNUM = 'alnum';
const DIGITS = 'digits';
const PACKED = 'packed';

/** Bytes occupied by a COMP-3 field with `intDigits` + `fracDigits` digits. */
function packedBytes(intDigits, fracDigits) {
  return Math.ceil((intDigits + fracDigits + 1) / 2);
}

const packed = (name, offset, intDigits, fracDigits) => ({
  name,
  kind: PACKED,
  offset,
  length: packedBytes(intDigits, fracDigits),
  intDigits,
  fracDigits,
  signed: true,
});

const alnum = (name, offset, length) => ({ name, kind: ALNUM, offset, length });
const digits = (name, offset, length) => ({ name, kind: DIGITS, offset, length });

/** PORT-RECORD, from PORTFLIO.cpy. 148 bytes. */
const PORT_RECORD = {
  name: 'PORT-RECORD',
  copybook: 'PORTFLIO',
  length: 148,
  keyOffset: 0,
  keyLength: 18, // PORT-KEY = PORT-ID + PORT-ACCOUNT-NO, matches KEYS(18 0) in PORTDEF.jcl
  fields: [
    alnum('PORT-ID', 0, 8),
    alnum('PORT-ACCOUNT-NO', 8, 10),
    alnum('PORT-CLIENT-NAME', 18, 30),
    alnum('PORT-CLIENT-TYPE', 48, 1),
    digits('PORT-CREATE-DATE', 49, 8),
    digits('PORT-LAST-MAINT', 57, 8),
    alnum('PORT-STATUS', 65, 1),
    packed('PORT-TOTAL-VALUE', 66, 13, 2),
    packed('PORT-CASH-BALANCE', 74, 13, 2),
    alnum('PORT-LAST-USER', 82, 8),
    digits('PORT-LAST-TRANS', 90, 8),
    alnum('PORT-FILLER', 98, 50),
  ],
};

/**
 * PORTREC — DERIVED. No such copybook exists in this repository, but PORTTRAN.cbl:40
 * does `COPY PORTREC`. Layout reasoned from PORTTRAN's field usage, keeping the 148-byte
 * record length and 18-byte key wire-compatible with PORT-RECORD by carving the two
 * position fields out of PORT-FILLER. See CONTRACTS.md section 1.3.
 *
 * Anything computed from this layout must be reported as DERIVED, never as executed.
 */
const PORTREC_RECORD = {
  name: 'PORTREC',
  copybook: 'PORTREC (derived — absent from repo)',
  derived: true,
  length: 148,
  keyOffset: 0,
  keyLength: 18,
  // PORTTRAN's SELECT declares `RECORD KEY IS PORT-ID` (8 bytes), unlike every other
  // program. The transaction path resolves on PORT-ID alone and must raise on ambiguity.
  transactionKeyLength: 8,
  fields: [
    ...PORT_RECORD.fields.slice(0, -1), // offsets 0..97, identical to PORT-RECORD
    packed('PORT-TOTAL-UNITS', 98, 11, 4),
    packed('PORT-TOTAL-COST', 106, 13, 2),
    alnum('PORT-FILLER', 114, 34),
  ],
};

/** TRANSACTION-RECORD, from TRNREC.cpy. 152 bytes. */
const TRANSACTION_RECORD = {
  name: 'TRANSACTION-RECORD',
  copybook: 'TRNREC',
  length: 152,
  keyOffset: 0,
  keyLength: 28, // TRN-KEY
  fields: [
    alnum('TRN-DATE', 0, 8),
    alnum('TRN-TIME', 8, 6),
    alnum('TRN-PORTFOLIO-ID', 14, 8),
    alnum('TRN-SEQUENCE-NO', 22, 6),
    alnum('TRN-INVESTMENT-ID', 28, 10),
    alnum('TRN-TYPE', 38, 2),
    packed('TRN-QUANTITY', 40, 11, 4),
    packed('TRN-PRICE', 48, 11, 4),
    packed('TRN-AMOUNT', 56, 13, 2),
    alnum('TRN-CURRENCY', 64, 3),
    alnum('TRN-STATUS', 67, 1),
    alnum('TRN-PROCESS-DATE', 68, 26),
    alnum('TRN-PROCESS-USER', 94, 8),
    alnum('TRN-FILLER', 102, 50),
  ],
};

/** UPDATE-RECORD, from the inline FD in PORTUPDT.cbl. 69 bytes. */
const UPDATE_RECORD = {
  name: 'UPDATE-RECORD',
  copybook: 'PORTUPDT (inline FD)',
  length: 69,
  fields: [
    alnum('UPDT-ID', 0, 8),
    alnum('UPDT-ACCT-NO', 8, 10),
    alnum('UPDT-ACTION', 18, 1), // S = status, V = total value, N = client name
    alnum('UPDT-NEW-VALUE', 19, 50),
  ],
};

/** DELETE-RECORD, from the inline FD in PORTDEL.cbl. 80 bytes. */
const DELETE_RECORD = {
  name: 'DELETE-RECORD',
  copybook: 'PORTDEL (inline FD)',
  length: 80,
  fields: [
    alnum('DEL-ID', 0, 8),
    alnum('DEL-ACCT-NO', 8, 10),
    alnum('DEL-REASON-CODE', 18, 2), // 01 closed, 02 transferred, 03 requested
    alnum('DEL-FILLER', 20, 60),
  ],
};

/**
 * AUDIT-RECORD as declared inline in PORTDEL.cbl (WORKING-STORAGE, written to AUDIT-FILE).
 * 80 bytes. Note this is a *different, narrower* AUDIT-RECORD than AUDITLOG.cpy's, which
 * PORTTRAN uses — the two layouts coexist in the legacy code under the same 01 name.
 */
const AUDIT_RECORD_PORTDEL = {
  name: 'AUDIT-RECORD (PORTDEL)',
  copybook: 'PORTDEL (inline FD)',
  length: 80,
  fields: [
    alnum('AUD-TIMESTAMP', 0, 26),
    alnum('AUD-ACTION', 26, 6),
    alnum('AUD-KEY', 32, 18),
    alnum('AUD-REASON', 50, 2),
    alnum('AUD-STATUS', 52, 1),
    alnum('AUD-FILLER', 53, 27),
  ],
};

/** AUDIT-RECORD, from AUDITLOG.cpy. Used by PORTTRAN via CALL 'AUDPROC'. 392 bytes. */
const AUDIT_LOG_RECORD = {
  name: 'AUDIT-RECORD (AUDITLOG)',
  copybook: 'AUDITLOG',
  length: 392,
  fields: [
    alnum('AUD-TIMESTAMP', 0, 26),
    alnum('AUD-SYSTEM-ID', 26, 8),
    alnum('AUD-USER-ID', 34, 8),
    alnum('AUD-PROGRAM', 42, 8),
    alnum('AUD-TERMINAL', 50, 8),
    alnum('AUD-TYPE', 58, 4),
    alnum('AUD-ACTION', 62, 8),
    alnum('AUD-STATUS', 70, 4),
    alnum('AUD-PORTFOLIO-ID', 74, 8),
    alnum('AUD-ACCOUNT-NO', 82, 10),
    alnum('AUD-BEFORE-IMAGE', 92, 100),
    alnum('AUD-AFTER-IMAGE', 192, 100),
    alnum('AUD-MESSAGE', 292, 100),
  ],
};

/** Level-88 condition names, preserved from the copybooks. */
const CONDITIONS = {
  PORT_CLIENT_TYPE: { I: 'individual', C: 'corporate', T: 'trust' },
  PORT_STATUS: { A: 'active', C: 'closed', S: 'suspended' },
  TRN_TYPE: { BU: 'buy', SL: 'sell', TR: 'transfer', FE: 'fee' },
  TRN_STATUS: { P: 'pending', D: 'done', F: 'failed', R: 'reversed' },
  UPDT_ACTION: { S: 'status', V: 'totalValue', N: 'clientName' },
  DEL_REASON: { '01': 'closed', '02': 'transferred', '03': 'requested' },
};

/** FILE STATUS -> result/HTTP mapping. CONTRACTS.md section 5. */
const FILE_STATUS = {
  '00': { result: 'ok', http: 200 },
  '10': { result: 'endOfFile', http: null },
  '22': { result: 'conflict', http: 409 },
  '23': { result: 'notFound', http: 404 },
};
const FILE_STATUS_DEFAULT = { result: 'ioError', http: 500 };

/** Self-check: declared record length must equal the sum of its field widths. */
function assertGeometry(layout) {
  const end = layout.fields.reduce((max, f) => Math.max(max, f.offset + f.length), 0);
  if (end !== layout.length) {
    throw new Error(
      `${layout.name}: fields span ${end} bytes but length is declared as ${layout.length}`
    );
  }
  let expected = 0;
  for (const f of layout.fields) {
    if (f.offset !== expected) {
      throw new Error(
        `${layout.name}.${f.name}: expected offset ${expected}, declared ${f.offset}`
      );
    }
    expected += f.length;
  }
  return layout;
}

module.exports = {
  ALNUM,
  DIGITS,
  PACKED,
  packedBytes,
  PORT_RECORD: assertGeometry(PORT_RECORD),
  PORTREC_RECORD: assertGeometry(PORTREC_RECORD),
  TRANSACTION_RECORD: assertGeometry(TRANSACTION_RECORD),
  UPDATE_RECORD: assertGeometry(UPDATE_RECORD),
  DELETE_RECORD: assertGeometry(DELETE_RECORD),
  AUDIT_RECORD_PORTDEL: assertGeometry(AUDIT_RECORD_PORTDEL),
  AUDIT_LOG_RECORD: assertGeometry(AUDIT_LOG_RECORD),
  CONDITIONS,
  FILE_STATUS,
  FILE_STATUS_DEFAULT,
  assertGeometry,
};
