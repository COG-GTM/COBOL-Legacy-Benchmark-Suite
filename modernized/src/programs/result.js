'use strict';

const { FILE_STATUS, FILE_STATUS_DEFAULT } = require('../schema/records');

function fileResult(status, extra = {}, create = false) {
  const mapped = FILE_STATUS[status] || FILE_STATUS_DEFAULT;
  return {
    status,
    result: mapped.result,
    http: create && status === '00' ? 201 : mapped.http,
    ...extra,
  };
}

function validationResult(message, returnCode = 8) {
  return {
    status: '00',
    result: 'validationError',
    http: 400,
    returnCode,
    message,
  };
}

module.exports = { fileResult, validationResult };
