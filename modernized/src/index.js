'use strict';

const { DocumentStore } = require('./store');
const { dispatch } = require('./programs/portmstr');
const { toCanonicalJson } = require('./canonical');

function createSystem({ seed = [], mode = 'legacy', runDate } = {}) {
  const store = new DocumentStore({ seed });
  const auditStore = store.audit;
  const handler = async (event = {}) => {
    if (event.action === 'auditTrail') return { records: auditStore.records.map((record) => ({ ...record })) };
    return dispatch(event.action, event, { store, auditStore, mode, runDate });
  };
  return { handler, store, auditStore };
}

module.exports = { createSystem, toCanonicalJson };
