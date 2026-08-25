/**
 * The canonical form both sides of the parity comparison are reduced to.
 *
 * A case result is:
 *   {
 *     program:    'PORTADD',
 *     caseId:     'ADD-002',
 *     counters:   { added: 0, duplicates: 1, errors: 0 },   // integers
 *     events:     [ { kind: 'duplicate-record', portId: 'PORT0001' } ],
 *     finalState: [ <canonical PORT-RECORD>, ... ],          // key order
 *     audit:      [ <canonical AUDIT-RECORD>, ... ]          // PORTDEL only
 *   }
 *
 * Money fields inside finalState are exact decimal strings produced by the
 * COMP-3 codec, so comparison is by decimal value and never by raw bytes.
 * Clock driven fields are replaced by tokens (see normalizeRunValues) because
 * COBOL stamps them from the system clock at capture time while the JS side
 * stamps them at test time.
 */

import { portKey } from '../store/indexed-file.js';

export const RUN_DATE_TOKEN = '@RUNDATE';
export const RUN_TIMESTAMP_TOKEN = '@RUNSTAMP';

/** Date fields that the COBOL programs stamp from the system clock. */
const CLOCK_DATE_FIELDS = ['createDate', 'lastMaint'];

/**
 * Replaces clock driven values with stable tokens.
 * @param {object} record canonical record
 * @param {{runDate:string}} run the YYYYMMDD the side under normalization ran on
 */
export function normalizeRunValues(record, { runDate }) {
  const out = { ...record };
  for (const field of CLOCK_DATE_FIELDS) {
    if (out[field] === runDate) out[field] = RUN_DATE_TOKEN;
  }
  if (typeof out.timestamp === 'string' && out.timestamp.trim() !== '') {
    out.timestamp = RUN_TIMESTAMP_TOKEN;
  }
  return out;
}

export function normalizeState(records, run) {
  return [...records]
    .sort((left, right) => (portKey(left) < portKey(right) ? -1 : 1))
    .map((record) => normalizeRunValues(record, run));
}

export function canonicalResult({ program, caseId, counters, events, finalState, audit }, run) {
  return {
    program,
    caseId,
    counters: { ...counters },
    events: events.map((event) => ({ ...event })),
    finalState: finalState ? normalizeState(finalState, run) : null,
    audit: audit ? audit.map((record) => normalizeRunValues(record, run)) : null,
  };
}

/**
 * Deep structural diff between the golden (expected) and actual canonical
 * results, reported as a flat list of paths so a failure names the exact field
 * that diverged rather than dumping two objects.
 * @returns {Array<{path:string, expected:unknown, actual:unknown}>}
 */
export function diffCanonical(expected, actual, path = '') {
  if (Object.is(expected, actual)) return [];

  const bothArrays = Array.isArray(expected) && Array.isArray(actual);
  const bothObjects =
    !bothArrays &&
    expected !== null &&
    actual !== null &&
    typeof expected === 'object' &&
    typeof actual === 'object';

  if (bothArrays) {
    if (expected.length !== actual.length) {
      return [{ path: `${path}.length`, expected: expected.length, actual: actual.length }];
    }
    return expected.flatMap((item, index) =>
      diffCanonical(item, actual[index], `${path}[${index}]`),
    );
  }

  if (bothObjects) {
    const keys = [...new Set([...Object.keys(expected), ...Object.keys(actual)])].sort();
    return keys.flatMap((key) =>
      diffCanonical(expected[key], actual[key], path === '' ? key : `${path}.${key}`),
    );
  }

  return [{ path: path === '' ? '(root)' : path, expected, actual }];
}
