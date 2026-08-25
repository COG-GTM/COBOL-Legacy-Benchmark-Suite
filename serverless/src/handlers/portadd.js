import { STATUS } from '../store/indexed-file.js';
import { fileFromSeed, text, unchangedState } from './common.js';

export function handler({ seed, input, runDate }) {
  const file = fileFromSeed(seed);
  const counters = { added: 0, duplicates: 0, errors: 0 };
  const events = [];

  for (const source of input) {
    const record = { ...source };
    const portId = text(record.portId).trimEnd();
    if (!portId || !text(record.clientName).trimEnd() || record.status !== 'A') {
      counters.errors += 1;
      events.push({ kind: 'invalid-record', portId });
      continue;
    }

    record.createDate = runDate;
    record.lastMaint = runDate;
    const result = file.write(record);
    if (result.status === STATUS.OK) {
      counters.added += 1;
    } else if (result.status === STATUS.DUPLICATE_KEY) {
      counters.duplicates += 1;
      events.push({ kind: 'duplicate-record', portId });
    } else {
      counters.errors += 1;
      events.push({ kind: 'write-error', portId });
    }
  }

  return { counters, events, finalState: unchangedState(file), audit: null };
}
