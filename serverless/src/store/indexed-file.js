/**
 * KSDS (key sequenced) file emulation.
 *
 * The COBOL programs address PORTFILE as an indexed file keyed on PORT-KEY
 * (PORT-ID X(8) + PORT-ACCOUNT-NO X(10), 18 bytes) and branch on the two byte
 * file status. The JS handlers must reproduce those status codes exactly, so
 * this store exposes the file verbs the programs use rather than a generic
 * map, and returns COBOL status codes instead of throwing.
 */

export const STATUS = {
  OK: '00',
  DUPLICATE_KEY: '22',
  NOT_FOUND: '23',
  END_OF_FILE: '10',
  FILE_NOT_FOUND: '35',
};

/** PORT-KEY as stored on the record: 8 byte id + 10 byte account, space padded. */
export function portKey(record) {
  return `${String(record.portId ?? '').padEnd(8, ' ')}${String(record.accountNo ?? '').padEnd(10, ' ')}`;
}

export class IndexedFile {
  /** @param {Array<object>} records initial (seed) records */
  constructor(records = []) {
    this.records = new Map();
    for (const record of records) {
      this.records.set(portKey(record), { ...record });
    }
  }

  /** READ ... KEY IS -> { status, record } */
  read(key) {
    const record = this.records.get(key);
    if (!record) return { status: STATUS.NOT_FOUND, record: null };
    return { status: STATUS.OK, record: { ...record } };
  }

  /** WRITE -> status 22 when the key already exists */
  write(record) {
    const key = portKey(record);
    if (this.records.has(key)) return { status: STATUS.DUPLICATE_KEY };
    this.records.set(key, { ...record });
    return { status: STATUS.OK };
  }

  /** REWRITE -> status 23 when the key is gone */
  rewrite(record) {
    const key = portKey(record);
    if (!this.records.has(key)) return { status: STATUS.NOT_FOUND };
    this.records.set(key, { ...record });
    return { status: STATUS.OK };
  }

  /** DELETE -> status 23 when the key is gone */
  delete(key) {
    if (!this.records.has(key)) return { status: STATUS.NOT_FOUND };
    this.records.delete(key);
    return { status: STATUS.OK };
  }

  /**
   * Sequential browse in key order, which for a KSDS is byte order of the
   * 18 byte key - not insertion order.
   */
  browse() {
    return [...this.records.keys()].sort().map((key) => ({ ...this.records.get(key) }));
  }

  get count() {
    return this.records.size;
  }
}
