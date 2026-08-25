'use strict';

const { createSystem } = require('./index');

const system = createSystem({ runDate: '00000000' });

module.exports = {
  handler: system.handler,
  store: system.store,
  auditStore: system.auditStore,
  reset: () => system.store.clear(),
};
