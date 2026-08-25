import { moveAlphanumericToDecimal } from '../codec/cobol-move.js';
import { STATUS } from '../store/indexed-file.js';
import { fileFromSeed, requestKey, text, unchangedState } from './common.js';

export function handler({ seed, input }) {
  const file = fileFromSeed(seed);
  const counters = { updates: 0, errors: 0 };
  const events = [];

  for (const request of input) {
    const key = requestKey(request);
    const read = file.read(key);
    if (read.status !== STATUS.OK) {
      counters.errors += 1;
      events.push({ kind: 'not-found', key });
      continue;
    }

    const record = read.record;
    if (request.action === 'S') {
      record.status = text(request.newValue).charAt(0);
    } else if (request.action === 'N') {
      record.clientName = text(request.newValue).slice(0, 30).trimEnd();
    } else if (request.action === 'V') {
      record.totalValue = moveAlphanumericToDecimal(request.newValue);
    }

    const rewritten = file.rewrite(record);
    if (rewritten.status === STATUS.OK) {
      counters.updates += 1;
    } else {
      counters.errors += 1;
      events.push({ kind: 'update-failed', key });
    }
  }

  return { counters, events, finalState: unchangedState(file), audit: null };
}
