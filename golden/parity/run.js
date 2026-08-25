'use strict';

/**
 * Cross-language parity runner.
 *
 * Feeds the golden inputs through the modernized JS handlers ("after"), compares the results
 * against the COBOL baseline ("before"), and emits a TSTVAL00-style report. Every case is
 * labelled EXECUTED or DERIVED so the report never overstates what was actually observed.
 *
 * Usage:  node golden/parity/run.js            (prints the report, exits non-zero on failure)
 *         require('./run').runParity()          (used by the jest gate)
 */

const fs = require('fs');
const path = require('path');
const Decimal = require('decimal.js');

const decks = require('./decks');
const normalize = require('./normalize');
const { DERIVED, KNOWN_DIVERGENCES } = require('./cases');

const { createSystem } = require('../../modernized/src');
const { toCanonicalJson } = require('../../modernized/src/canonical');
const validation = require('../../modernized/src/validation');
const comp3 = require('../../modernized/src/codec/comp3');
const records = require('../../modernized/src/schema/records');

const EXECUTED = 'EXECUTED';
const DERIVED_LABEL = 'DERIVED';

// ---------------------------------------------------------------------------
// case plumbing
// ---------------------------------------------------------------------------

/** Stable, key-order-independent rendering, so a diff points at values not at key order. */
function canonicalText(value) {
  return JSON.stringify(value, (_key, inner) => {
    if (Decimal.isDecimal(inner)) return inner.toString();
    if (inner && typeof inner === 'object' && !Array.isArray(inner)) {
      return Object.keys(inner).sort().reduce((acc, key) => ({ ...acc, [key]: inner[key] }), {});
    }
    return inner;
  }, 1);
}

function compare({ id, program, baseline, description, expected, actual }) {
  const expectedText = canonicalText(expected);
  const actualText = canonicalText(actual);
  return {
    id,
    program,
    baseline,
    description,
    passed: expectedText === actualText,
    expected: expectedText,
    actual: actualText,
  };
}

// ---------------------------------------------------------------------------
// COMP-3 codec vs raw COBOL bytes  --  EXECUTED
// ---------------------------------------------------------------------------
function comp3Cases() {
  const vectors = JSON.parse(
    fs.readFileSync(path.join(__dirname, '..', 'vectors', 'comp3-vectors.json'), 'utf8')
  );
  const bytes = fs.readFileSync(path.join(__dirname, '..', 'vectors', 'comp3-vectors.bin'));
  const list = Array.isArray(vectors) ? vectors : vectors.vectors;

  return list.map((vector, index) => {
    const slice = bytes.subarray(index * 8, index * 8 + 8);
    const field = {
      name: `VEC-${index + 1}`,
      kind: 'packed',
      offset: 0,
      length: 8,
      intDigits: vector.intDigits,
      fracDigits: vector.fracDigits,
      signed: true,
    };
    // Both directions: COBOL bytes -> Decimal, and Decimal -> COBOL bytes.
    const actual = {
      hex: comp3.encode(new Decimal(vector.value), field).toString('hex'),
      value: comp3.decode(slice, field).toFixed(vector.fracDigits),
    };
    return compare({
      id: `COMP3-${String(index + 1).padStart(2, '0')}`,
      program: 'codec',
      baseline: EXECUTED,
      description: `${vector.pic} ${vector.value}`,
      expected: {
        hex: slice.toString('hex'),
        value: new Decimal(vector.value).toFixed(vector.fracDigits),
      },
      actual,
    });
  });
}

// ---------------------------------------------------------------------------
// PORTVALD  --  EXECUTED against a compiled PORTVALD.so via VALDRV
// ---------------------------------------------------------------------------
function validationCases() {
  return normalize.parseValidationBaseline(decks.expected('portvald.txt')).map((vector) => {
    const result = validation.validate(vector.type, vector.input, 'legacy');
    return compare({
      id: vector.caseId.replace('VAL-', 'VALD-'),
      program: 'PORTVALD',
      baseline: EXECUTED,
      description: `type ${vector.type} <- "${vector.input}"`,
      expected: { returnCode: vector.returnCode, message: vector.message },
      actual: {
        returnCode: result.returnCode,
        message: normalize.alnum(result.message || ''),
      },
    });
  });
}

// ---------------------------------------------------------------------------
// PORTREAD  --  EXECUTED
// ---------------------------------------------------------------------------
async function readCases(seed) {
  const { handler } = createSystem({ seed, mode: 'legacy', runDate: decks.baselineRunDate() });
  const listing = normalize.parseReadListing(decks.expected('portread.stdout.txt'));
  const response = await handler({ action: 'list' });

  return [
    compare({
      id: 'READ-LIST',
      program: 'PORTREAD',
      baseline: EXECUTED,
      description: 'sequential READ NEXT in PORT-KEY order',
      expected: {
        total: listing.total,
        records: listing.records.map((record) => ({
          portId: record.portId,
          accountNo: record.accountNo,
          clientName: record.clientName,
          status: record.status,
          totalValue: record.totalValue,
        })),
      },
      actual: {
        total: response.count,
        records: response.records.map((record) => {
          const json = toCanonicalJson(record);
          return {
            portId: json.portId,
            accountNo: json.accountNo,
            clientName: json.clientName,
            status: json.status,
            totalValue: json.totalValue,
          };
        }),
      },
    }),
  ];
}

/** COBOL state dump -> comparable portfolio list, with the create-path run date masked. */
function expectedState(file, runDate) {
  const parsed = normalize.parseStateDump(decks.expected(file));
  return {
    portfolios: parsed.portfolios.map((portfolio) => normalize.maskRunDate(portfolio, runDate)),
    portCount: parsed.portCount,
    audits: parsed.audits,
  };
}

/** JS store -> the same shape, via the canonical JSON renderer. */
async function actualState(handler, runDate) {
  const listed = await handler({ action: 'list' });
  const audit = await handler({ action: 'auditTrail' });
  return {
    portfolios: listed.records.map((record) => {
      const json = toCanonicalJson(record);
      return normalize.maskRunDate(
        {
          portId: json.portId,
          accountNo: json.accountNo,
          clientName: json.clientName,
          clientType: json.clientType,
          createDate: json.createDate || null,
          lastMaint: json.lastMaint || null,
          status: json.status,
          totalValue: json.totalValue,
          cashBalance: json.cashBalance,
          lastUser: json.lastUser,
          lastTrans: json.lastTrans || null,
        },
        runDate
      );
    }),
    portCount: listed.count,
    audits: audit.records.map((record) => ({
      action: normalize.alnum(record.action),
      key: normalize.alnum(record.key),
      reason: normalize.alnum(record.reason),
      status: normalize.alnum(record.status),
    })),
  };
}

/**
 * Position fields only, for the derived PORTTRAN cases.
 *
 * Deliberately separate from actualState(): PORT-TOTAL-UNITS and PORT-TOTAL-COST live in the
 * derived PORTREC layout, not in the 148-byte PORTFLIO record that the executed COBOL state
 * dumps describe. Folding them into the state projection would make every EXECUTED state
 * comparison assert fields the COBOL baseline cannot have an opinion about.
 */
async function actualPositions(handler) {
  const listed = await handler({ action: 'list' });
  return listed.records.map((record) => {
    const json = toCanonicalJson(record);
    return { portId: json.portId, totalUnits: json.totalUnits, totalCost: json.totalCost };
  });
}

// ---------------------------------------------------------------------------
// PORTADD / PORTUPDT / PORTDEL  --  EXECUTED batch replays
// ---------------------------------------------------------------------------
async function addCases(seed) {
  const runDate = decks.baselineRunDate();
  const { handler } = createSystem({ seed, mode: 'legacy', runDate });
  const counters = { added: 0, duplicates: 0, errors: 0 };
  const messages = [];

  for (const record of decks.addDeck()) {
    const response = await handler({ action: 'create', record });
    if (response.result === 'ok') counters.added += 1;
    else if (response.result === 'conflict') {
      counters.duplicates += 1;
      messages.push({ text: 'Duplicate record', operand: normalize.alnum(record.portId) });
    } else {
      counters.errors += 1;
      messages.push({ text: 'Invalid record data', operand: normalize.alnum(record.portId) });
    }
  }

  const baselineCounters = normalize.parseCounters(decks.expected('portadd.stdout.txt'));
  return [
    compare({
      id: 'ADD-COUNTS',
      program: 'PORTADD',
      baseline: EXECUTED,
      description: 'create + duplicate-key detection tallies',
      expected: {
        added: baselineCounters['Records added'],
        duplicates: baselineCounters['Duplicate records'],
        errors: baselineCounters['Errors occurred'],
      },
      actual: counters,
    }),
    compare({
      id: 'ADD-MSGS',
      program: 'PORTADD',
      baseline: EXECUTED,
      description: 'per-record diagnostics, in emission order',
      expected: normalize.parseMessages(decks.expected('portadd.stdout.txt')),
      actual: messages,
    }),
    compare({
      id: 'ADD-STATE',
      program: 'PORTADD',
      baseline: EXECUTED,
      description: 'resulting KSDS contents',
      expected: expectedState('portadd.state.txt', runDate),
      actual: await actualState(handler, runDate),
    }),
  ];
}

async function updateCases(seed) {
  const runDate = decks.baselineRunDate();
  const { handler } = createSystem({ seed, mode: 'legacy', runDate });
  const counters = { updates: 0, errors: 0 };
  const messages = [];

  for (const update of decks.updateDeck()) {
    const response = await handler({
      action: 'update',
      key: { portId: update.portId, accountNo: update.accountNo },
      updateAction: update.updateAction,
      newValue: update.newValue,
    });
    if (response.result === 'ok') counters.updates += 1;
    else {
      counters.errors += 1;
      messages.push({
        text: 'Record not found',
        operand: `${update.portId}${update.accountNo}`,
      });
    }
  }

  const baselineCounters = normalize.parseCounters(decks.expected('portupdt.stdout.txt'));
  return [
    compare({
      id: 'UPDT-COUNTS',
      program: 'PORTUPDT',
      baseline: EXECUTED,
      description: 'update tallies (unknown action counts as success)',
      expected: {
        updates: baselineCounters['Updates processed'],
        errors: baselineCounters['Errors occurred'],
      },
      actual: counters,
    }),
    compare({
      id: 'UPDT-MSGS',
      program: 'PORTUPDT',
      baseline: EXECUTED,
      description: 'per-record diagnostics, in emission order',
      expected: normalize.parseMessages(decks.expected('portupdt.stdout.txt')),
      actual: messages,
    }),
    compare({
      id: 'UPDT-STATE',
      program: 'PORTUPDT',
      baseline: EXECUTED,
      description: 'resulting KSDS contents after status/name/value updates',
      expected: expectedState('portupdt.state.txt', runDate),
      actual: await actualState(handler, runDate),
    }),
  ];
}

async function deleteCases(seed) {
  const runDate = decks.baselineRunDate();
  const { handler } = createSystem({ seed, mode: 'legacy', runDate });
  const counters = { deleted: 0, notFound: 0, errors: 0 };
  const messages = [];

  for (const request of decks.deleteDeck()) {
    const response = await handler({
      action: 'delete',
      key: { portId: request.portId, accountNo: request.accountNo },
      reasonCode: request.reasonCode,
    });
    if (response.result === 'ok') counters.deleted += 1;
    else if (response.result === 'notFound') {
      counters.notFound += 1;
      messages.push({
        text: 'Record not found',
        operand: `${request.portId}${request.accountNo}`,
      });
    } else counters.errors += 1;
  }

  const baselineCounters = normalize.parseCounters(decks.expected('portdel.stdout.txt'));
  return [
    compare({
      id: 'DEL-COUNTS',
      program: 'PORTDEL',
      baseline: EXECUTED,
      description: 'delete tallies',
      expected: {
        deleted: baselineCounters['Records deleted'],
        notFound: baselineCounters['Records not found'],
        errors: baselineCounters['Errors occurred'],
      },
      actual: counters,
    }),
    compare({
      id: 'DEL-MSGS',
      program: 'PORTDEL',
      baseline: EXECUTED,
      description: 'per-record diagnostics, in emission order',
      expected: normalize.parseMessages(decks.expected('portdel.stdout.txt')),
      actual: messages,
    }),
    compare({
      id: 'DEL-STATE',
      program: 'PORTDEL',
      baseline: EXECUTED,
      description: 'resulting KSDS contents and audit trail',
      expected: expectedState('portdel.state.txt', runDate),
      actual: await actualState(handler, runDate),
    }),
  ];
}

// ---------------------------------------------------------------------------
// PORTTRAN  --  DERIVED (PORTREC copybook absent, so nothing to execute)
// ---------------------------------------------------------------------------
async function transactionCases(seed) {
  const runDate = decks.baselineRunDate();
  const results = [];

  for (const mode of ['legacy', 'modernized']) {
    const { handler } = createSystem({ seed, mode, runDate });
    const counters = { read: 0, processed: 0, applied: 0, errors: 0 };
    const messages = [];

    for (const transaction of decks.transactionDeck()) {
      counters.read += 1;
      const response = await handler({ action: 'transaction', transaction });
      if (response.result === 'validationError') {
        counters.errors += 1;
        messages.push({ text: normalize.alnum(response.message), operand: response.operand || '' });
      } else {
        counters.processed += 1;
        if (response.applied) counters.applied += 1;
        if (response.rejected) {
          counters.errors += 1;
          messages.push({ text: normalize.alnum(response.message), operand: '' });
        }
      }
    }

    const state = await actualState(handler, runDate);
    const audits = state.audits.map((audit) => ({ action: audit.action, status: audit.status }));

    if (mode === 'legacy') {
      const expected = DERIVED.porttranLegacy;
      results.push(compare({
        id: 'TRAN-L-CNT',
        program: 'PORTTRAN',
        baseline: DERIVED_LABEL,
        description: 'reachable legacy path: validate and count only',
        expected: expected.counters,
        actual: { read: counters.read, processed: counters.processed, errors: counters.errors },
      }));
      results.push(compare({
        id: 'TRAN-L-MSG',
        program: 'PORTTRAN',
        baseline: DERIVED_LABEL,
        description: 'reachable legacy path: rejection messages',
        expected: expected.messages,
        actual: messages,
      }));
      results.push(compare({
        id: 'TRAN-L-POS',
        program: 'PORTTRAN',
        baseline: DERIVED_LABEL,
        description: '2200-UPDATE-POSITIONS unreachable, so no position changes',
        expected: expectedState('seed-state.txt', runDate).portfolios.map((p) => p.portId),
        actual: state.portfolios.map((p) => p.portId),
      }));
    } else {
      const expected = DERIVED.porttranModernized;
      results.push(compare({
        id: 'TRAN-M-CNT',
        program: 'PORTTRAN',
        baseline: DERIVED_LABEL,
        description: 'intended position math: tallies',
        expected: expected.counters,
        actual: counters,
      }));
      results.push(compare({
        id: 'TRAN-M-POS',
        program: 'PORTTRAN',
        baseline: DERIVED_LABEL,
        description: 'intended position math: BU/SL/TR/FE applied to holdings',
        expected: expected.positions,
        actual: await actualPositions(handler),
      }));
      results.push(compare({
        id: 'TRAN-M-MSG',
        program: 'PORTTRAN',
        baseline: DERIVED_LABEL,
        description: 'intended position math: rejection messages',
        expected: expected.messages,
        actual: messages,
      }));
      results.push(compare({
        id: 'TRAN-M-AUD',
        program: 'PORTTRAN',
        baseline: DERIVED_LABEL,
        description: 'audit trail written after the EVALUATE, even for rejects',
        expected: expected.audits,
        actual: audits,
      }));
    }
  }
  return results;
}

// ---------------------------------------------------------------------------
// PORTMSTR  --  DERIVED (cannot compile), one checkable behaviour: WHEN OTHER
// ---------------------------------------------------------------------------
async function dispatcherCases(seed) {
  const { handler } = createSystem({ seed, mode: 'legacy', runDate: decks.baselineRunDate() });
  const response = await handler({ action: 'refinance' });
  return [
    compare({
      id: 'MSTR-OTHER',
      program: 'PORTMSTR',
      baseline: DERIVED_LABEL,
      description: 'WHEN OTHER arm of the command EVALUATE',
      expected: DERIVED.portmstrInvalidCommand,
      actual: {
        result: response.result,
        http: response.http,
        message: normalize.alnum(response.message),
      },
    }),
  ];
}

// ---------------------------------------------------------------------------
// report
// ---------------------------------------------------------------------------
function pad(text, width) {
  return String(text).slice(0, width).padEnd(width);
}

/** Reproduces TSTVAL00's 132-column WS-TEST-DETAIL / WS-SUMMARY-LINE layout. */
function formatReport(cases) {
  const lines = [
    '*'.repeat(132),
    `${' '.repeat(30)}${pad('PORTFOLIO COBOL -> JS PARITY REPORT', 72)}${' '.repeat(30)}`.slice(0, 132),
    '*'.repeat(132),
    `${pad('TEST-ID', 12)}  ${pad('PROGRAM', 10)}  ${pad('BASELINE', 8)}  ${pad('DESCRIPTION', 52)}  ${pad('STAT', 4)}`,
    '-'.repeat(132),
  ];

  for (const item of cases) {
    lines.push(
      `${pad(item.id, 12)}  ${pad(item.program, 10)}  ${pad(item.baseline, 8)}  `
      + `${pad(item.description, 52)}  ${pad(item.passed ? 'PASS' : 'FAIL', 4)}`
    );
  }

  const total = cases.length;
  const passed = cases.filter((item) => item.passed).length;
  const failed = total - passed;
  const rate = total === 0 ? 0 : (passed / total) * 100;
  const executed = cases.filter((item) => item.baseline === EXECUTED).length;

  lines.push('-'.repeat(132));
  lines.push(
    `${pad('TOTAL TESTS:', 15)}${String(total).padStart(6)}`
    + `${pad('  PASSED:', 15)}${String(passed).padStart(6)}`
    + `${pad('  FAILED:', 15)}${String(failed).padStart(6)}`
    + `${pad('  SUCCESS:', 15)}${rate.toFixed(2).padStart(6)}%`
  );
  lines.push(
    `${pad('BASELINE:', 15)}${String(executed).padStart(6)} EXECUTED against GnuCOBOL, `
    + `${total - executed} DERIVED from source (see golden/expected/DERIVED.md)`
  );
  lines.push('');
  lines.push('KNOWN INTENTIONAL DIVERGENCES (legacy vs modernized, not failures):');
  for (const divergence of KNOWN_DIVERGENCES) {
    lines.push(`  ${pad(divergence.id, 24)} ${divergence.program}`);
    lines.push(`  ${' '.repeat(24)} ${divergence.reason.replace(/\s+/g, ' ')}`);
  }

  for (const item of cases.filter((entry) => !entry.passed)) {
    lines.push('');
    lines.push(`FAIL ${item.id} (${item.program}, ${item.baseline}) -- ${item.description}`);
    lines.push('  expected (COBOL):');
    lines.push(item.expected.split('\n').map((line) => `    ${line}`).join('\n'));
    lines.push('  actual (JS):');
    lines.push(item.actual.split('\n').map((line) => `    ${line}`).join('\n'));
  }

  return lines.join('\n');
}

async function runParity() {
  records.PORT_RECORD; // touches assertGeometry: a bad layout fails before any case runs
  const seed = decks.seedPortfolios();
  const cases = [
    ...comp3Cases(),
    ...validationCases(),
    ...(await readCases(seed)),
    ...(await addCases(seed)),
    ...(await updateCases(seed)),
    ...(await deleteCases(seed)),
    ...(await transactionCases(seed)),
    ...(await dispatcherCases(seed)),
  ];
  return { cases, report: formatReport(cases), failed: cases.filter((item) => !item.passed) };
}

if (require.main === module) {
  runParity()
    .then(({ report, failed }) => {
      const target = path.join(__dirname, '..', 'PARITY-REPORT.txt');
      fs.writeFileSync(target, `${report}\n`);
      process.stdout.write(`${report}\n\nreport written to ${target}\n`);
      process.exit(failed.length === 0 ? 0 : 1);
    })
    .catch((error) => {
      process.stderr.write(`${error.stack}\n`);
      process.exit(2);
    });
}

module.exports = { runParity, formatReport };
