/**
 * Parity driver.
 *
 * For each golden case it decodes the SAME fixed-width input files the COBOL
 * programs were fed (golden/inputs/**), invokes the JS serverless handler,
 * reduces the handler result to the canonical form, and diffs it against the
 * golden expected result captured from COBOL (golden/expected/**).
 *
 * Decoding the fixture bytes rather than re-reading the case JSON is
 * deliberate: it means the two sides cannot diverge in what they were asked to
 * process, only in how they processed it.
 */

import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';

import { LAYOUTS } from '../codec/layouts.js';
import { decodeFile } from '../codec/record.js';
import { HANDLERS } from '../handlers/index.js';
import { canonicalResult, diffCanonical } from './canonical.js';

export function loadInputs(goldenDir, testCase) {
  const dir = path.join(goldenDir, 'inputs', testCase.program, testCase.id);
  const read = (file, layoutName) => {
    const full = path.join(dir, file);
    if (!layoutName || !existsSync(full)) return [];
    return decodeFile(LAYOUTS[layoutName], readFileSync(full));
  };
  return {
    seed: read('seed.dat', testCase.seedLayout),
    input: read('input.dat', testCase.inputLayout),
  };
}

export function loadExpected(goldenDir, testCase) {
  const file = path.join(goldenDir, 'expected', testCase.program, `${testCase.id}.json`);
  if (!existsSync(file)) return null;
  return JSON.parse(readFileSync(file, 'utf8'));
}

/**
 * Runs one case through its JS handler and diffs it against the golden.
 * @returns {{caseId:string, program:string, type:string, description:string,
 *            status:'PASS'|'FAIL'|'SKIP', derived:boolean, note?:string,
 *            diffs:Array<{path:string, expected:unknown, actual:unknown}>}}
 */
export function runCase(goldenDir, testCase, { runDate }) {
  const expected = loadExpected(goldenDir, testCase);
  const base = {
    caseId: testCase.id,
    program: testCase.program,
    type: testCase.type,
    description: testCase.description,
    derived: expected ? expected.derived === true : false,
    diffs: [],
  };

  if (!expected) {
    return { ...base, status: 'SKIP', note: 'no golden expected result for this case' };
  }

  const handler = HANDLERS[testCase.program];
  if (!handler) {
    return { ...base, status: 'FAIL', note: `no JS handler registered for ${testCase.program}` };
  }

  const { seed, input } = loadInputs(goldenDir, testCase);
  const actualRaw = handler({
    caseId: testCase.id,
    seed,
    input,
    runDate,
  });

  const actual = canonicalResult(
    {
      program: testCase.program,
      caseId: testCase.id,
      counters: actualRaw.counters,
      events: actualRaw.events,
      finalState: actualRaw.finalState ?? null,
      audit: actualRaw.audit ?? null,
    },
    { runDate },
  );

  const diffs = diffCanonical(expected.result, actual);
  return {
    ...base,
    status: diffs.length === 0 ? 'PASS' : 'FAIL',
    diffs,
    note: expected.derived ? 'expected result is derived, not captured from COBOL' : undefined,
  };
}
