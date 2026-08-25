#!/usr/bin/env node
/**
 * Materialises every golden case in golden/cases/*.json as the fixed-width
 * files the COBOL programs read, plus a manifest with a sha256 per file.
 *
 * The same files are the input to both sides of the parity comparison: the
 * COBOL capture (tools/golden/capture-cobol.sh) and the JS handlers, so a case
 * can never drift between the two.
 *
 *   golden/inputs/<PROGRAM>/<CASE-ID>/seed.dat    portfolio master seed records
 *   golden/inputs/<PROGRAM>/<CASE-ID>/input.dat   the program's request file
 */

import { createHash } from 'node:crypto';
import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import path from 'node:path';

import { LAYOUTS, recordLength } from '../../serverless/src/codec/layouts.js';
import { encodeFile } from '../../serverless/src/codec/record.js';
import { GOLDEN_DIR, caseDir, loadCases, loadConfig } from './lib/cases.mjs';

function writeRecords(file, layoutName, records) {
  const layout = LAYOUTS[layoutName];
  const buf = encodeFile(
    layout,
    records.map((record) => record.values),
  );
  writeFileSync(file, buf);
  return {
    file: path.relative(GOLDEN_DIR, file),
    layout: layoutName,
    recordLength: recordLength(layout),
    records: records.length,
    sha256: createHash('sha256').update(buf).digest('hex'),
  };
}

function main() {
  const config = loadConfig();
  const inputsRoot = path.join(GOLDEN_DIR, 'inputs');
  rmSync(inputsRoot, { recursive: true, force: true });

  const manifest = { randomSeed: config.randomSeed, cases: [] };

  for (const testCase of loadCases()) {
    const dir = caseDir(testCase.program, testCase.id);
    mkdirSync(dir, { recursive: true });

    const files = [];
    if (testCase.seedLayout) {
      files.push(writeRecords(path.join(dir, 'seed.dat'), testCase.seedLayout, testCase.seed));
    }
    if (testCase.inputLayout) {
      files.push(writeRecords(path.join(dir, 'input.dat'), testCase.inputLayout, testCase.input));
    }

    manifest.cases.push({
      program: testCase.program,
      id: testCase.id,
      type: testCase.type,
      description: testCase.description,
      generated: testCase.generated,
      files,
    });
  }

  writeFileSync(
    path.join(inputsRoot, 'manifest.json'),
    `${JSON.stringify(manifest, null, 2)}\n`,
  );
  process.stdout.write(`Built ${manifest.cases.length} golden input case(s)\n`);
}

main();
