import { fileFromSeed, unchangedState } from './common.js';

export function handler({ seed }) {
  const file = fileFromSeed(seed);
  const events = file.browse().map((record, index) => ({
    kind: 'record',
    sequence: index + 1,
    portId: record.portId,
    accountNo: record.accountNo,
    clientName: record.clientName,
    status: record.status,
    totalValue: record.totalValue,
  }));

  return {
    counters: { recordsRead: events.length },
    events,
    finalState: unchangedState(file),
    audit: null,
  };
}
