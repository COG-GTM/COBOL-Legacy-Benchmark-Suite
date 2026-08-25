#!/usr/bin/env node
/**
 * Turns the raw COBOL artifacts in golden/cobol-run into the canonical golden
 * EXPECTED-RESULTS in golden/expected/<PROGRAM>/<CASE-ID>.json.
 *
 * This is the COBOL half of the comparison layer: fixed-width dumps are
 * decoded field by field, COMP-3 money is decoded to exact decimal strings,
 * space padding is trimmed, and clock driven fields are replaced with tokens.
 * The JS half produces the identical shape, so the parity runner can diff the
 * two structurally.
 */

import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';

import { LAYOUTS } from '../../serverless/src/codec/layouts.js';
import { decodeFile } from '../../serverless/src/codec/record.js';
import { canonicalResult } from '../../serverless/src/parity/canonical.js';
import { GOLDEN_DIR, loadCases, loadConfig } from './lib/cases.mjs';
import { PARSERS } from './lib/cobol-output.mjs';

const RUN_DIR = path.join(GOLDEN_DIR, 'cobol-run');
const EXPECTED_DIR = path.join(GOLDEN_DIR, 'expected');

function readArtifact(program, caseId, file) {
  const full = path.join(RUN_DIR, program, caseId, file);
  return existsSync(full) ? readFileSync(full) : null;
}

function main() {
  const config = loadConfig();
  const metadata = JSON.parse(readFileSync(path.join(RUN_DIR, 'metadata.json'), 'utf8'));
  if (!metadata.captureRunDate) throw new Error('cobol-run/metadata.json has no captureRunDate');
  const run = { runDate: metadata.captureRunDate };

  let written = 0;
  const skipped = [];

  for (const testCase of loadCases()) {
    const { program, id } = testCase;
    const stdout = readArtifact(program, id, 'stdout.txt');
    if (stdout === null) {
      skipped.push(`${program}/${id}`);
      continue;
    }

    const parse = PARSERS[program];
    if (!parse) throw new Error(`no stdout parser for ${program}`);
    const { events, counters, unmatched } = parse(stdout.toString('latin1'));
    if (unmatched.length > 0) {
      throw new Error(
        `${program}/${id}: unparsed COBOL output line(s):\n  ${unmatched.join('\n  ')}`,
      );
    }

    const dump = readArtifact(program, id, 'dump.dat');
    const auditRaw = readArtifact(program, id, 'audit.dat');

    const result = canonicalResult(
      {
        program,
        caseId: id,
        counters,
        events,
        finalState: dump ? decodeFile(LAYOUTS.PORT_RECORD, dump) : null,
        audit: auditRaw ? decodeFile(LAYOUTS.DELETE_AUDIT_RECORD, auditRaw) : null,
      },
      run,
    );

    const golden = {
      source: 'cobol',
      derived: false,
      program,
      caseId: id,
      type: testCase.type,
      description: testCase.description,
      capture: {
        gnucobolVersion: metadata.gnucobolVersion,
        indexedFileHandler: metadata.indexedFileHandler,
        captureRunDate: metadata.captureRunDate,
        exitCode: Number.parseInt(
          readArtifact(program, id, 'exit-code.txt').toString('utf8').trim(),
          10,
        ),
      },
      result,
    };

    const outDir = path.join(EXPECTED_DIR, program);
    mkdirSync(outDir, { recursive: true });
    writeFileSync(path.join(outDir, `${id}.json`), `${JSON.stringify(golden, null, 2)}\n`);
    written += 1;
  }

  process.stdout.write(
    `Normalized ${written} COBOL golden case(s)` +
      (skipped.length > 0 ? `; no COBOL capture for: ${skipped.join(', ')}\n` : '\n'),
  );
  if (config.captureRunDate !== metadata.captureRunDate) {
    process.stderr.write(
      `warning: golden/config captureRunDate (${config.captureRunDate}) differs from ` +
        `cobol-run metadata (${metadata.captureRunDate})\n`,
    );
  }
}

main();
