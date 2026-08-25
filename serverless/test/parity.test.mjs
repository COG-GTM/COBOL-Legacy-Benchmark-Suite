/**
 * Golden-dataset parity suite.
 *
 * Every golden case is run through the JS serverless handler and diffed
 * against the COBOL-captured (or, for PORTTRAN, derived) expected result. A
 * single diverging field fails the build, which is the whole point of the
 * harness. The run also writes the TSTVAL00-style report to
 * golden/reports/parity-report.txt.
 */

import assert from 'node:assert/strict';
import { mkdirSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import test from 'node:test';

import { renderReport } from '../src/parity/report.js';
import { runCase } from '../src/parity/run.js';
import { GOLDEN_DIR, loadCases, loadConfig } from '../../tools/golden/lib/cases.mjs';

const config = loadConfig();
const RUN_DATE = config.captureRunDate;
const cases = loadCases();
const results = [];

test('golden inputs and expected results are present', () => {
  assert.ok(cases.length > 0, 'no golden cases were loaded');
  assert.ok(RUN_DATE, 'golden/config/golden-config.json has no captureRunDate');
});

for (const testCase of cases) {
  test(`${testCase.program} ${testCase.id} - ${testCase.description}`, () => {
    const result = runCase(GOLDEN_DIR, testCase, { runDate: RUN_DATE });
    results.push(result);

    assert.notEqual(
      result.status,
      'SKIP',
      `${testCase.program} ${testCase.id} has no golden expected result`,
    );

    if (result.diffs.length > 0) {
      const detail = result.diffs
        .map(
          (diff) =>
            `  ${diff.path}: expected(COBOL)=${JSON.stringify(diff.expected)} actual(JS)=${JSON.stringify(diff.actual)}`,
        )
        .join('\n');
      assert.fail(`${testCase.program} ${testCase.id} diverged from the golden:\n${detail}`);
    }
  });
}

test('write parity report', () => {
  const reportDir = path.join(GOLDEN_DIR, 'reports');
  mkdirSync(reportDir, { recursive: true });
  writeFileSync(path.join(reportDir, 'parity-report.txt'), renderReport(results));

  const failed = results.filter((result) => result.status === 'FAIL');
  assert.equal(failed.length, 0, `${failed.length} case(s) diverged from the COBOL goldens`);
});
