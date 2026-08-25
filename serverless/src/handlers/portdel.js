import { STATUS } from '../store/indexed-file.js';
import { fileFromSeed, requestKey, unchangedState } from './common.js';

export function handler({ seed, input, runDate }) {
  const file = fileFromSeed(seed);
  const counters = { deleted: 0, notFound: 0, errors: 0 };
  const events = [];
  const audit = [];

  for (const request of input) {
    const key = requestKey(request);
    const read = file.read(key);
    if (read.status === STATUS.NOT_FOUND) {
      counters.notFound += 1;
      events.push({ kind: 'not-found', key });
      continue;
    }
    if (read.status !== STATUS.OK) {
      counters.errors += 1;
      events.push({ kind: 'read-error', key });
      continue;
    }

    const deleted = file.delete(key);
    if (deleted.status !== STATUS.OK) {
      counters.errors += 1;
      events.push({ kind: 'delete-failed', key });
      continue;
    }

    counters.deleted += 1;
    audit.push({
      timestamp: `${runDate}000000000000000000`,
      action: 'DELETE',
      key,
      reason: request.reasonCode,
      status: read.record.status,
      filler: '\u0000'.repeat(27),
    });
  }

  return { counters, events, finalState: unchangedState(file), audit };
}
