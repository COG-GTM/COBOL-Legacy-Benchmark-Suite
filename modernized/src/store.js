'use strict';

const fs = require('fs');
const path = require('path');
const Decimal = require('decimal.js');
const { canonicalize, keyOf } = require('./canonical');

function clone(value) {
  if (Decimal.isDecimal(value)) return new Decimal(value);
  if (Array.isArray(value)) return value.map(clone);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, clone(item)]));
  }
  return value;
}

function encodeJson(value) {
  if (Decimal.isDecimal(value)) return { __decimal: value.toString() };
  if (Array.isArray(value)) return value.map(encodeJson);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, encodeJson(item)]));
  }
  return value;
}

function decodeJson(value) {
  if (value && typeof value === 'object' && typeof value.__decimal === 'string') {
    return new Decimal(value.__decimal);
  }
  if (Array.isArray(value)) return value.map(decodeJson);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, decodeJson(item)]));
  }
  return value;
}

class AuditStore {
  constructor(parent) {
    this.parent = parent;
    this.records = [];
  }

  append(record) {
    this.records.push(clone(record));
    this.parent.persist();
    return { status: '00' };
  }
}

class DocumentStore {
  constructor({ seed = [], filePath } = {}) {
    this.filePath = filePath;
    this.records = new Map();
    this.audit = new AuditStore(this);
    if (filePath && fs.existsSync(filePath)) {
      const saved = JSON.parse(fs.readFileSync(filePath, 'utf8'));
      for (const item of saved.records || []) this.records.set(item.key, decodeJson(item.record));
      this.audit.records = (saved.audit || []).map(decodeJson);
    } else {
      for (const record of seed) {
        const canonical = canonicalize(record);
        this.records.set(keyOf(canonical), clone(canonical));
      }
    }
  }

  persist() {
    if (!this.filePath) return;
    fs.mkdirSync(path.dirname(this.filePath), { recursive: true });
    fs.writeFileSync(this.filePath, JSON.stringify({
      records: [...this.records.entries()].map(([key, record]) => ({ key, record: encodeJson(record) })),
      audit: this.audit.records.map(encodeJson),
    }));
  }

  read(key) {
    const value = keyOf(key);
    if (!this.records.has(value)) return { status: '23' };
    return { status: '00', record: clone(this.records.get(value)) };
  }

  write(record) {
    const canonical = canonicalize(record);
    const key = keyOf(canonical);
    if (Buffer.byteLength(key, 'ascii') !== 18) return { status: '35' };
    if (this.records.has(key)) return { status: '22' };
    this.records.set(key, clone(canonical));
    this.persist();
    return { status: '00', record: clone(canonical) };
  }

  rewrite(record) {
    const canonical = canonicalize(record);
    const key = keyOf(canonical);
    if (!this.records.has(key)) return { status: '23' };
    this.records.set(key, clone(canonical));
    this.persist();
    return { status: '00', record: clone(canonical) };
  }

  delete(key) {
    const value = keyOf(key);
    if (!this.records.has(value)) return { status: '23' };
    const record = this.records.get(value);
    this.records.delete(value);
    this.persist();
    return { status: '00', record: clone(record) };
  }

  readNext(cursor = 0) {
    const keys = [...this.records.keys()].sort();
    if (cursor >= keys.length) return { status: '10', cursor };
    const key = keys[cursor];
    return { status: '00', record: clone(this.records.get(key)), cursor: cursor + 1 };
  }

  findById(portId) {
    const id = String(portId || '');
    // PORTTRAN declares an 8-byte RECORD KEY IS PORT-ID; other programs use the 18-byte PORT-KEY.
    const matches = [...this.records.entries()].filter(([key]) => key.slice(0, 8) === id);
    if (matches.length === 0) return { status: '23' };
    if (matches.length > 1) return { status: '22' };
    return { status: '00', key: matches[0][0], record: clone(matches[0][1]) };
  }

  clear() {
    this.records.clear();
    this.audit.records.length = 0;
    this.persist();
  }
}

module.exports = { DocumentStore, AuditStore, keyOf };
