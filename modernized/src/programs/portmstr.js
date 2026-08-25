'use strict';

const portadd = require('./portadd');
const portread = require('./portread');
const portupdt = require('./portupdt');
const portdel = require('./portdel');
const porttran = require('./porttran');

function dispatch(action, event, context) {
  switch (action) {
    case 'create': return portadd.validateAndAdd(event.record, context);
    case 'read': return portread.readPortfolio(event.key, context);
    case 'list': return portread.listPortfolios(context);
    case 'update': return portupdt.processUpdate(event, context);
    case 'delete': return portdel.processDelete(event, context);
    case 'transaction': return porttran.processTransaction(event.transaction, context);
    default: return {
      status: '00',
      result: 'invalidCommand',
      http: 400,
      message: 'Invalid command',
    };
  }
}

module.exports = { dispatch };
