import { IndexedFile } from '../store/indexed-file.js';

export function text(value) {
  return String(value ?? '');
}

export function requestKey(record) {
  return `${text(record.portId).padEnd(8, ' ')}${text(record.accountNo).padEnd(10, ' ')}`;
}

export function fileFromSeed(seed) {
  return new IndexedFile(seed);
}

export function unchangedState(file) {
  return file.browse();
}
