/**
 * Loads the golden case definitions in golden/cases/*.json and resolves the
 * named record aliases each case references into concrete record objects.
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

import { GENERATORS } from './generate.mjs';

export const REPO_ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
export const GOLDEN_DIR = path.join(REPO_ROOT, 'golden');

/** Program -> the sequential input file the program reads, and its layout. */
export const PROGRAMS = {
  PORTADD: { seedLayout: 'PORT_RECORD', inputLayout: 'PORT_RECORD', inputDd: 'INPTFILE' },
  PORTREAD: { seedLayout: 'PORT_RECORD', inputLayout: null, inputDd: null },
  PORTUPDT: { seedLayout: 'PORT_RECORD', inputLayout: 'UPDATE_RECORD', inputDd: 'UPDTFILE' },
  PORTDEL: { seedLayout: 'PORT_RECORD', inputLayout: 'DELETE_RECORD', inputDd: 'DELEFILE' },
  PORTTRAN: { seedLayout: 'PORT_RECORD', inputLayout: 'TRANSACTION_RECORD', inputDd: 'TRANFILE' },
  PORTVALD: {
    seedLayout: null,
    inputLayout: 'VALIDATION_REQUEST_RECORD',
    inputDd: 'VALDFILE',
  },
};

export const CASE_FILES = [
  'portadd.json',
  'portread.json',
  'portupdt.json',
  'portdel.json',
  'porttran.json',
  'portvald.json',
  'volume.json',
];

export function loadConfig() {
  return JSON.parse(readFileSync(path.join(GOLDEN_DIR, 'config', 'golden-config.json'), 'utf8'));
}

function resolveAliases(suite, aliases, kind, caseId) {
  return (aliases ?? []).map((alias) => {
    const record = suite.records[alias];
    if (!record) {
      throw new Error(`${suite.program} ${caseId}: unknown ${kind} record alias "${alias}"`);
    }
    return { alias, values: record };
  });
}

/** @returns {Array<{program:string, id:string, type:string, description:string, seed:Array, input:Array, derived:boolean, note?:string}>} */
export function loadCases() {
  const config = loadConfig();
  const out = [];
  for (const file of CASE_FILES) {
    const suite = JSON.parse(readFileSync(path.join(GOLDEN_DIR, 'cases', file), 'utf8'));
    const program = PROGRAMS[suite.program];
    if (!program) throw new Error(`unknown program ${suite.program} in ${file}`);
    for (const testCase of suite.cases) {
      let seed;
      let input;
      if (testCase.generator) {
        const generate = GENERATORS[testCase.generator.kind];
        if (!generate) {
          throw new Error(`${testCase.id}: unknown generator "${testCase.generator.kind}"`);
        }
        ({ seed, input } = generate(config));
      } else {
        seed = resolveAliases(suite, testCase.seed, 'seed', testCase.id);
        input = resolveAliases(suite, testCase.input, 'input', testCase.id);
      }
      out.push({
        program: suite.program,
        id: testCase.id,
        type: testCase.type,
        description: testCase.description,
        seedLayout: program.seedLayout,
        inputLayout: program.inputLayout,
        inputDd: program.inputDd,
        generated: Boolean(testCase.generator),
        seed,
        input,
        suiteNote: suite.note,
      });
    }
  }
  return out;
}

export function caseDir(program, caseId) {
  return path.join(GOLDEN_DIR, 'inputs', program, caseId);
}
