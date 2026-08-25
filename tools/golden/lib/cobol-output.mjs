/**
 * Parsers for the raw COBOL artifacts captured in golden/cobol-run.
 *
 * Every DISPLAY literal below is quoted from the program source (see the
 * DISPLAY statements in src/programs/portfolio/*.cbl), so the parser is a
 * transcription of the programs' own output contract rather than a guess.
 * Counters and event lists become the canonical result that the JS handlers
 * are compared against.
 */

/**
 * '+0000005555555.55' -> '5555555.55', '-9999999999999.99' -> '-9999999999999.99'.
 * DISPLAY of a signed COMP-3 field emits a sign plus fixed width zero padded
 * digits; the canonical form is the exact decimal value.
 */
export function canonicalDisplayedDecimal(text, scale = 2) {
  const match = /^([+-]?)(\d+)(?:\.(\d*))?$/.exec(text.trim());
  if (!match) throw new Error(`not a displayed numeric: ${JSON.stringify(text)}`);
  const [, sign, whole, frac = ''] = match;
  const wholeCanonical = whole.replace(/^0+(?=\d)/, '');
  const fracCanonical = frac.padEnd(scale, '0').slice(0, scale);
  const magnitude = scale > 0 ? `${wholeCanonical}.${fracCanonical}` : wholeCanonical;
  const isZero = /^0*$/.test(`${whole}${frac}`);
  return sign === '-' && !isZero ? `-${magnitude}` : magnitude;
}

function counterValue(line, prefix) {
  return Number.parseInt(line.slice(prefix.length).trim(), 10);
}

/**
 * @param {string} stdout
 * @param {Array<{prefix:string, kind:string, field:string}>} eventSpecs
 * @param {Record<string, string>} counterSpecs canonical name -> DISPLAY prefix
 */
function parseLineOriented(stdout, eventSpecs, counterSpecs) {
  const events = [];
  const counters = {};
  const unmatched = [];

  for (const line of stdout.split('\n')) {
    if (line.trim() === '') continue;

    const counterEntry = Object.entries(counterSpecs).find(([, prefix]) =>
      line.startsWith(prefix),
    );
    if (counterEntry) {
      counters[counterEntry[0]] = counterValue(line, counterEntry[1]);
      continue;
    }

    const eventSpec = eventSpecs.find((spec) => line.startsWith(spec.prefix));
    if (eventSpec) {
      events.push({ kind: eventSpec.kind, [eventSpec.field]: line.slice(eventSpec.prefix.length).trimEnd() });
      continue;
    }

    unmatched.push(line);
  }

  return { events, counters, unmatched };
}

const ADD_EVENTS = [
  { prefix: 'Invalid record data: ', kind: 'invalid-record', field: 'portId' },
  { prefix: 'Duplicate record: ', kind: 'duplicate-record', field: 'portId' },
  { prefix: 'Write error for: ', kind: 'write-error', field: 'portId' },
];

const UPDT_EVENTS = [
  { prefix: 'Record not found: ', kind: 'not-found', field: 'key' },
  { prefix: 'Update failed for: ', kind: 'update-failed', field: 'key' },
];

const DEL_EVENTS = [
  { prefix: 'Record not found: ', kind: 'not-found', field: 'key' },
  { prefix: 'Read error for: ', kind: 'read-error', field: 'key' },
  { prefix: 'Delete failed for: ', kind: 'delete-failed', field: 'key' },
  { prefix: 'Audit write failed for: ', kind: 'audit-write-failed', field: 'key' },
];

export function parsePortAdd(stdout) {
  return parseLineOriented(stdout, ADD_EVENTS, {
    added: 'Records added:    ',
    duplicates: 'Duplicate records:',
    errors: 'Errors occurred:  ',
  });
}

export function parsePortUpdt(stdout) {
  return parseLineOriented(stdout, UPDT_EVENTS, {
    updates: 'Updates processed: ',
    errors: 'Errors occurred:  ',
  });
}

export function parsePortDel(stdout) {
  return parseLineOriented(stdout, DEL_EVENTS, {
    deleted: 'Records deleted:  ',
    notFound: 'Records not found:',
    errors: 'Errors occurred:  ',
  });
}

/** PORTREAD emits a six line block per record followed by a blank DISPLAY. */
export function parsePortRead(stdout) {
  const events = [];
  const counters = {};
  const unmatched = [];
  let current = null;

  const finish = () => {
    if (current) events.push(current);
    current = null;
  };

  for (const line of stdout.split('\n')) {
    if (line.trim() === '') continue;

    if (line.startsWith('Portfolio Record: ')) {
      finish();
      current = { kind: 'record', sequence: counterValue(line, 'Portfolio Record: ') };
    } else if (line.startsWith('  ID: ')) {
      current.portId = line.slice('  ID: '.length).trimEnd();
    } else if (line.startsWith('  Account: ')) {
      current.accountNo = line.slice('  Account: '.length).trimEnd();
    } else if (line.startsWith('  Client: ')) {
      current.clientName = line.slice('  Client: '.length).trimEnd();
    } else if (line.startsWith('  Status: ')) {
      current.status = line.slice('  Status: '.length).trimEnd();
    } else if (line.startsWith('  Total Value: ')) {
      current.totalValue = canonicalDisplayedDecimal(line.slice('  Total Value: '.length), 2);
    } else if (line.startsWith('Total Records Read: ')) {
      finish();
      counters.recordsRead = counterValue(line, 'Total Records Read: ');
    } else {
      unmatched.push(line);
    }
  }
  finish();

  return { events, counters, unmatched };
}

/** GLDVALD emits: VALD CASE=<X(10)> TYPE=<X(1)> RC=<S9(4) leading sep> MSG=<X(50)> */
const VALD_LINE =
  /^VALD CASE=(.{10}) TYPE=(.) RC=([+-]\d{4}) MSG=(.*)$/;

export function parsePortVald(stdout) {
  const events = [];
  const counters = {};
  const unmatched = [];

  for (const line of stdout.split('\n')) {
    if (line.trim() === '') continue;

    const match = VALD_LINE.exec(line);
    if (match) {
      const [, caseId, validateType, returnCode, message] = match;
      events.push({
        kind: 'validation',
        caseId: caseId.trimEnd(),
        validateType,
        returnCode: Number.parseInt(returnCode, 10),
        message: message.trimEnd(),
      });
      continue;
    }
    if (line.startsWith('Validations performed: ')) {
      counters.validations = counterValue(line, 'Validations performed: ');
      continue;
    }
    unmatched.push(line);
  }

  return { events, counters, unmatched };
}

export const PARSERS = {
  PORTADD: parsePortAdd,
  PORTREAD: parsePortRead,
  PORTUPDT: parsePortUpdt,
  PORTDEL: parsePortDel,
  PORTVALD: parsePortVald,
};
