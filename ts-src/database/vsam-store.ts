/**
 * VSAM Key-Sequenced Data Set (KSDS) Store.
 * Migrated from: src/database/vsam/vsam-definitions.txt
 *
 * In-memory key-value store that emulates VSAM KSDS behaviour:
 * keyed random access, sequential browsing, read/write/delete by key.
 */

/** Callback to extract the string key from a record of type T. */
export type KeyExtractor<T> = (record: T) => string;

/**
 * Generic VSAM KSDS replacement.
 *
 * Records are kept sorted by key so that sequential browsing returns
 * them in key order – matching VSAM KSDS semantics.
 */
export class VsamStore<T> {
  private data: Map<string, T> = new Map();
  private browseKeys: string[] = [];
  private browseIndex = 0;
  private browsing = false;

  constructor(private readonly keyExtractor: KeyExtractor<T>) {}

  /** READ by key – equivalent to COBOL READ FILE KEY IS. */
  read(key: string): T | undefined {
    return this.data.get(key);
  }

  /** WRITE – equivalent to COBOL WRITE (fails on duplicate). */
  write(record: T): void {
    const key = this.keyExtractor(record);
    if (this.data.has(key)) {
      throw new VsamError('22', `Duplicate key: ${key}`);
    }
    this.data.set(key, record);
  }

  /** REWRITE – equivalent to COBOL REWRITE (fails if not found). */
  rewrite(record: T): void {
    const key = this.keyExtractor(record);
    if (!this.data.has(key)) {
      throw new VsamError('23', `Record not found: ${key}`);
    }
    this.data.set(key, record);
  }

  /** DELETE by key – equivalent to COBOL DELETE. */
  delete(key: string): void {
    if (!this.data.has(key)) {
      throw new VsamError('23', `Record not found: ${key}`);
    }
    this.data.delete(key);
  }

  /** Start a sequential browse (from beginning or from a given key). */
  startBrowse(fromKey?: string): void {
    this.browseKeys = Array.from(this.data.keys()).sort();
    this.browsing = true;

    if (fromKey) {
      this.browseIndex = this.browseKeys.findIndex((k) => k >= fromKey);
      if (this.browseIndex === -1) {
        this.browseIndex = this.browseKeys.length;
      }
    } else {
      this.browseIndex = 0;
    }
  }

  /** Read next record during a browse – returns undefined at EOF. */
  readNext(): T | undefined {
    if (!this.browsing) {
      throw new VsamError('46', 'Browse not started');
    }
    if (this.browseIndex >= this.browseKeys.length) {
      this.browsing = false;
      return undefined; // EOF – status '10'
    }
    const key = this.browseKeys[this.browseIndex++];
    return this.data.get(key);
  }

  /** End the browse session. */
  endBrowse(): void {
    this.browsing = false;
    this.browseKeys = [];
    this.browseIndex = 0;
  }

  /** Return all records as an array (sorted by key). */
  getAll(): T[] {
    const keys = Array.from(this.data.keys()).sort();
    return keys.map((k) => this.data.get(k)!);
  }

  /** Number of records in the store. */
  get size(): number {
    return this.data.size;
  }

  /** Clear all records. */
  clear(): void {
    this.data.clear();
  }

  /** Check whether a key exists. */
  has(key: string): boolean {
    return this.data.has(key);
  }
}

/** Error class that carries a VSAM-style two-character status code. */
export class VsamError extends Error {
  constructor(
    public readonly statusCode: string,
    message: string,
  ) {
    super(message);
    this.name = 'VsamError';
  }
}
