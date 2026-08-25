'use strict';

/**
 * Build gate: any divergence between the COBOL baseline and the JS port fails the test suite.
 *
 * There is deliberately no tolerance and no allow-list here. Legacy-vs-modernized differences
 * that are intentional are expressed by running the corresponding case in the mode that
 * matches the baseline, and enumerated in golden/parity/cases.js for the report -- never by
 * excusing a failing comparison.
 */

const { runParity } = require('./run');

describe('COBOL -> JS parity on the golden dataset', () => {
  let outcome;

  beforeAll(async () => {
    outcome = await runParity();
  });

  it('runs every case in the matrix', () => {
    expect(outcome.cases.length).toBeGreaterThan(0);
  });

  it('has no divergence between the COBOL baseline and the JS port', () => {
    const failures = outcome.failed.map(
      (item) => `${item.id} (${item.program}, ${item.baseline}): ${item.description}\n`
        + `  expected: ${item.expected}\n  actual:   ${item.actual}`
    );
    expect(failures).toEqual([]);
  });

  it('proves most cases against a real GnuCOBOL run, not a hand-derived expectation', () => {
    const executed = outcome.cases.filter((item) => item.baseline === 'EXECUTED');
    expect(executed.length).toBeGreaterThan(outcome.cases.length / 2);
  });
});

describe('per-case detail', () => {
  it('reports each case individually so a failure names itself', async () => {
    const { cases } = await runParity();
    for (const item of cases) {
      expect({ id: item.id, passed: item.passed }).toEqual({ id: item.id, passed: true });
    }
  });
});
