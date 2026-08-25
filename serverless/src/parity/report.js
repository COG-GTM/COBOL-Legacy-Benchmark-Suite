/**
 * TEST-REPORT renderer, laid out like the report TSTVAL00.cbl writes:
 * a 132 column banner, one fixed-width detail line per test case
 * (id X(10), type X(10), description X(50), status X(4)) and a summary line
 * with total / passed / failed / success rate.
 *
 * Failure diffs are appended after the summary; TSTVAL00 has no equivalent
 * because inside COBOL the comparison operands are already the report, but a
 * parity failure is useless without the diverging field.
 */

const LINE_WIDTH = 132;

function pad(text, width) {
  return String(text).slice(0, width).padEnd(width, ' ');
}

function rate(passed, total) {
  if (total === 0) return '  0.00';
  return ((passed / total) * 100).toFixed(2).padStart(6, ' ');
}

/**
 * @param {Array<{caseId:string, program:string, type:string, description:string, status:'PASS'|'FAIL'|'SKIP', derived:boolean, diffs:Array<{path:string, expected:unknown, actual:unknown}>, note?:string}>} results
 */
export function renderReport(results) {
  const lines = [];
  lines.push('*'.repeat(LINE_WIDTH));
  lines.push(`${' '.repeat(30)}${pad('COBOL / JS SERVERLESS PARITY REPORT', 72)}${' '.repeat(30)}`);
  lines.push('*'.repeat(LINE_WIDTH));

  for (const result of results) {
    lines.push(
      [
        pad(result.caseId, 10),
        pad(result.type, 10),
        pad(result.description, 50),
        pad(result.status, 4),
        pad(result.derived ? 'DERIVED EXPECTED' : 'COBOL EXPECTED', 20),
      ].join('  '),
    );
  }

  const total = results.length;
  const passed = results.filter((result) => result.status === 'PASS').length;
  const failed = results.filter((result) => result.status === 'FAIL').length;

  lines.push('*'.repeat(LINE_WIDTH));
  lines.push(
    `${pad('TOTAL TESTS:', 15)}${String(total).padStart(6)}` +
      `${pad('  PASSED:', 15)}${String(passed).padStart(6)}` +
      `${pad('  FAILED:', 15)}${String(failed).padStart(6)}` +
      `${pad('  SUCCESS:', 15)}${rate(passed, total)}%`,
  );

  const failures = results.filter((result) => result.status === 'FAIL');
  if (failures.length > 0) {
    lines.push('');
    lines.push('DIVERGENCES');
    lines.push('-'.repeat(LINE_WIDTH));
    for (const failure of failures) {
      lines.push(`${failure.program} ${failure.caseId} - ${failure.description}`);
      if (failure.note) lines.push(`  ${failure.note}`);
      for (const diff of failure.diffs) {
        lines.push(
          `  ${diff.path}: expected(COBOL)=${JSON.stringify(diff.expected)} ` +
            `actual(JS)=${JSON.stringify(diff.actual)}`,
        );
      }
      lines.push('');
    }
  }

  return `${lines.join('\n')}\n`;
}
