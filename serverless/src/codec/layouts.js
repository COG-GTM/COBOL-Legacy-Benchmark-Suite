/**
 * Fixed-width record layouts transcribed from the authoritative copybooks in
 * src/copybook/common (NOT from documentation/operations/test-data-specs.md,
 * whose layout is a simplified older variant).
 *
 * Field kinds:
 *   'X'      alphanumeric, space padded    -> string (trailing spaces trimmed)
 *   '9'      zoned display numeric         -> string of digits (padding kept)
 *   'comp3'  packed decimal                -> exact decimal string
 */

import { comp3ByteLength } from './comp3.js';

const X = (name, size) => ({ name, kind: 'X', size });
const N9 = (name, size) => ({ name, kind: '9', size });
const P3 = (name, digits, scale) => ({
  name,
  kind: 'comp3',
  digits,
  scale,
  size: comp3ByteLength(digits),
});

/** PORTFLIO.cpy - portfolio master record (148 bytes). */
export const PORT_RECORD = {
  name: 'PORT-RECORD',
  fields: [
    X('portId', 8), // PORT-ID
    X('accountNo', 10), // PORT-ACCOUNT-NO
    X('clientName', 30), // PORT-CLIENT-NAME
    X('clientType', 1), // PORT-CLIENT-TYPE  I/C/T
    N9('createDate', 8), // PORT-CREATE-DATE  9(8)
    N9('lastMaint', 8), // PORT-LAST-MAINT   9(8)
    X('status', 1), // PORT-STATUS       A/C/S
    P3('totalValue', 15, 2), // PORT-TOTAL-VALUE  S9(13)V99 COMP-3
    P3('cashBalance', 15, 2), // PORT-CASH-BALANCE S9(13)V99 COMP-3
    X('lastUser', 8), // PORT-LAST-USER
    N9('lastTrans', 8), // PORT-LAST-TRANS   9(8)
    X('filler', 50), // PORT-FILLER
  ],
};

/** TRNREC.cpy - transaction record (152 bytes). */
export const TRANSACTION_RECORD = {
  name: 'TRANSACTION-RECORD',
  fields: [
    X('trnDate', 8), // TRN-DATE     YYYYMMDD
    X('trnTime', 6), // TRN-TIME     HHMMSS
    X('portfolioId', 8), // TRN-PORTFOLIO-ID
    X('sequenceNo', 6), // TRN-SEQUENCE-NO
    X('investmentId', 10), // TRN-INVESTMENT-ID
    X('type', 2), // TRN-TYPE     BU/SL/TR/FE
    P3('quantity', 15, 4), // TRN-QUANTITY S9(11)V9(4) COMP-3
    P3('price', 15, 4), // TRN-PRICE    S9(11)V9(4) COMP-3
    P3('amount', 15, 2), // TRN-AMOUNT   S9(13)V9(2) COMP-3
    X('currency', 3), // TRN-CURRENCY
    X('status', 1), // TRN-STATUS   P/D/F/R
    X('processDate', 26), // TRN-PROCESS-DATE
    X('processUser', 8), // TRN-PROCESS-USER
    X('filler', 50), // TRN-FILLER
  ],
};

/** PORTUPDT.cbl FD UPDATE-FILE - update request record (69 bytes). */
export const UPDATE_RECORD = {
  name: 'UPDATE-RECORD',
  fields: [
    X('portId', 8), // UPDT-ID
    X('accountNo', 10), // UPDT-ACCT-NO
    X('action', 1), // UPDT-ACTION  S/V/N
    X('newValue', 50), // UPDT-NEW-VALUE
  ],
};

/** PORTDEL.cbl FD DELETE-FILE - deletion request record (80 bytes). */
export const DELETE_RECORD = {
  name: 'DELETE-RECORD',
  fields: [
    X('portId', 8), // DEL-ID
    X('accountNo', 10), // DEL-ACCT-NO
    X('reasonCode', 2), // DEL-REASON-CODE 01/02/03
    X('filler', 60), // DEL-FILLER
  ],
};

/** PORTDEL.cbl FD AUDIT-FILE - deletion audit record (80 bytes). */
export const DELETE_AUDIT_RECORD = {
  name: 'AUDIT-RECORD',
  fields: [
    X('timestamp', 26), // AUD-TIMESTAMP
    X('action', 6), // AUD-ACTION
    X('key', 18), // AUD-KEY (PORT-KEY)
    X('reason', 2), // AUD-REASON
    X('status', 1), // AUD-STATUS
    X('filler', 27), // AUD-FILLER
  ],
};

/** Driver record for the PORTVALD validation harness (GLDVALD.cbl). */
export const VALIDATION_REQUEST_RECORD = {
  name: 'VALIDATION-REQUEST',
  fields: [
    X('caseId', 10),
    X('validateType', 1), // I / A / T / M
    X('inputValue', 50),
  ],
};

export const LAYOUTS = {
  PORT_RECORD,
  TRANSACTION_RECORD,
  UPDATE_RECORD,
  DELETE_RECORD,
  DELETE_AUDIT_RECORD,
  VALIDATION_REQUEST_RECORD,
};

export function recordLength(layout) {
  return layout.fields.reduce((total, field) => total + field.size, 0);
}
