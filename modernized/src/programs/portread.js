'use strict';

const { fileResult } = require('./result');

function readPortfolio(key, { store }) {
  const result = store.read(key);
  return fileResult(result.status, { record: result.record });
}

function listPortfolios({ store }) {
  const records = [];
  let cursor = 0;
  while (true) {
    const next = store.readNext(cursor);
    if (next.status === '10') return fileResult('00', { records, count: records.length });
    if (next.status !== '00') return fileResult(next.status, { records, count: records.length });
    records.push(next.record);
    cursor = next.cursor;
  }
}

module.exports = { readPortfolio, listPortfolios };
