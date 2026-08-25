'use strict';

/**
 * The parity case matrix.
 *
 * Each case declares an id, the program it exercises, whether its expectation is EXECUTED
 * (captured from a real GnuCOBOL run) or DERIVED (hand-reasoned from the source because the
 * program cannot compile), and a `run` function returning `{ expected, actual }` already
 * normalized to comparable shapes.
 *
 * The DERIVED constants below are transcribed from golden/expected/DERIVED.md, which shows
 * the reasoning. They are the only expectations in this harness that were not observed.
 */

const DERIVED = {
  // PORTTRAN, reachable legacy path: 2200-UPDATE-POSITIONS is never PERFORMed, so
  // transactions are validated and counted but no portfolio is ever updated.
  porttranLegacy: {
    counters: { read: 9, processed: 5, errors: 4 },
    messages: [
      { text: 'Invalid Transaction Type', operand: 'ZZ' },
      { text: 'Invalid Portfolio ID', operand: 'PORT9997' },
      { text: 'Quantity must be greater than zero', operand: '' },
      { text: 'Portfolio ID is required', operand: '' },
    ],
    positionsChanged: false,
    audits: [],
  },

  // PORTTRAN, intended position math: what 2210/2220/2230/2240 would do if reachable.
  // PORT-TOTAL-UNITS/PORT-TOTAL-COST start at zero (see DERIVED.md section 1.2).
  porttranModernized: {
    counters: { read: 9, processed: 5, applied: 3, errors: 6 },
    positions: [
      { portId: 'PORT0001', totalUnits: '60.0000', totalCost: '8825.00' },
      { portId: 'PORT0002', totalUnits: '0.0000', totalCost: '-125.50' },
      { portId: 'PORT0003', totalUnits: '0.0000', totalCost: '0.00' },
    ],
    messages: [
      { text: 'Insufficient units for sale', operand: '' },
      { text: 'Transfer processing not implemented', operand: '' },
      { text: 'Invalid Transaction Type', operand: 'ZZ' },
      { text: 'Invalid Portfolio ID', operand: 'PORT9997' },
      { text: 'Quantity must be greater than zero', operand: '' },
      { text: 'Portfolio ID is required', operand: '' },
    ],
    // 2300-UPDATE-AUDIT-TRAIL runs after the EVALUATE unconditionally, so the two rejected
    // cases are audited too -- and audited as SUCC. Legacy quirk, preserved.
    audits: [
      { action: 'CREATE', status: 'SUCC' },
      { action: 'DELETE', status: 'SUCC' },
      { action: 'DELETE', status: 'SUCC' },
      { action: 'UPDATE', status: 'SUCC' },
      { action: 'UPDATE', status: 'SUCC' },
    ],
  },

  // PORTMSTR contributes exactly one independently checkable behaviour: the WHEN OTHER arm.
  portmstrInvalidCommand: {
    result: 'invalidCommand',
    http: 400,
    message: 'Invalid command',
  },
};

/** Divergences between legacy and modernized validation that are expected and allowed. */
const KNOWN_DIVERGENCES = [
  {
    id: 'DIV-VALD-I',
    program: 'PORTVALD',
    reason:
      'Legacy 1000-VALIDATE-ID moves 4 chars into PIC X(10), so IS NUMERIC fails on the '
      + 'space padding and every ID is rejected. Modernized mode applies PORT + 4 digits.',
  },
  {
    id: 'DIV-VALD-A',
    program: 'PORTVALD',
    reason:
      'Legacy 2000-VALIDATE-ACCOUNT tests all 50 bytes of LS-INPUT-VALUE for NUMERIC, so a '
      + '10-digit account plus 40 spaces is always rejected. Modernized mode applies 10 digits.',
  },
  {
    id: 'DIV-VALD-M',
    program: 'PORTVALD',
    reason:
      'Legacy 4000-VALIDATE-AMOUNT bounds are the field\'s own representable range, so the '
      + 'test never fails and even non-numeric text passes. Modernized mode requires a numeric '
      + 'value within VAL-MIN/MAX.',
  },
  {
    id: 'DIV-TRAN-POSITIONS',
    program: 'PORTTRAN',
    reason:
      '2200-UPDATE-POSITIONS is unreachable in the legacy source, so no position is ever '
      + 'updated. Modernized mode implements the intended buy/sell/transfer/fee math.',
  },
  {
    id: 'DIV-UPDT-UNKNOWN-ACTION',
    program: 'PORTUPDT',
    reason:
      '2200-APPLY-UPDATE has no WHEN OTHER, so an unrecognised action rewrites the record '
      + 'unchanged and is counted as a successful update. Legacy mode reproduces this; '
      + 'modernized mode rejects the action.',
  },
];

module.exports = { DERIVED, KNOWN_DIVERGENCES };
