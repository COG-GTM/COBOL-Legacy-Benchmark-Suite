'use strict';

/**
 * Reads the golden input decks that GOLDGEN.cbl wrote.
 *
 * These are raw fixed-length COBOL records with packed-decimal fields and no delimiters, so
 * decoding them with the *modernized* codec is not just plumbing: it is the first parity
 * assertion in the harness. If modernized/src/codec disagreed with GnuCOBOL about COMP-3
 * layout or field offsets, nothing below would decode at all.
 */

const fs = require('fs');
const path = require('path');

const records = require('../../modernized/src/schema/records');
const { decodeRecord } = require('../../modernized/src/codec/fixed');
const { canonicalize } = require('../../modernized/src/canonical');

const INPUT_DIR = path.join(__dirname, '..', 'input');
const EXPECTED_DIR = path.join(__dirname, '..', 'expected');

/** Split a fixed-length record file into records, rejecting a partial trailing record. */
function slice(file, length) {
  const buffer = fs.readFileSync(path.join(INPUT_DIR, file));
  if (buffer.length % length !== 0) {
    throw new RangeError(`${file}: ${buffer.length} bytes is not a multiple of ${length}`);
  }
  const out = [];
  for (let offset = 0; offset < buffer.length; offset += length) {
    out.push(buffer.subarray(offset, offset + length));
  }
  return out;
}

function decodeAll(file, layout) {
  return slice(file, layout.length).map((buffer) => decodeRecord(buffer, layout));
}

/** The three seeded portfolios, canonical, in file order (which is PORT-KEY order). */
function seedPortfolios() {
  return decodeAll('seed-portfolios.dat', records.PORT_RECORD).map(canonicalize);
}

/** PORTADD's input deck: 148-byte PORT-RECORDs offered for creation. */
function addDeck() {
  return decodeAll('add-deck.dat', records.PORT_RECORD).map(canonicalize);
}

/** PORTUPDT's input deck: id + account + single-character action + new value. */
function updateDeck() {
  return decodeAll('update-deck.dat', records.UPDATE_RECORD).map((record) => ({
    portId: record['UPDT-ID'],
    accountNo: record['UPDT-ACCT-NO'],
    updateAction: record['UPDT-ACTION'],
    newValue: record['UPDT-NEW-VALUE'],
  }));
}

/** PORTDEL's input deck: id + account + reason code. */
function deleteDeck() {
  return decodeAll('delete-deck.dat', records.DELETE_RECORD).map((record) => ({
    portId: record['DEL-ID'],
    accountNo: record['DEL-ACCT-NO'],
    reasonCode: record['DEL-REASON-CODE'],
  }));
}

/** PORTTRAN's input deck: 152-byte TRANSACTION-RECORDs with three COMP-3 fields. */
function transactionDeck() {
  return decodeAll('transaction-deck.dat', records.TRANSACTION_RECORD).map((record) => ({
    date: record['TRN-DATE'],
    time: record['TRN-TIME'],
    portId: record['TRN-PORTFOLIO-ID'],
    sequenceNo: record['TRN-SEQUENCE-NO'],
    investmentId: record['TRN-INVESTMENT-ID'],
    type: record['TRN-TYPE'],
    quantity: record['TRN-QUANTITY'],
    price: record['TRN-PRICE'],
    amount: record['TRN-AMOUNT'],
    currency: record['TRN-CURRENCY'],
    status: record['TRN-STATUS'],
  }));
}

function expected(file) {
  return fs.readFileSync(path.join(EXPECTED_DIR, file), 'utf8');
}

/** The date the COBOL baseline was captured; the create path stamps it, so it is masked. */
function baselineRunDate() {
  return expected('run-date.txt').trim();
}

module.exports = {
  INPUT_DIR,
  EXPECTED_DIR,
  seedPortfolios,
  addDeck,
  updateDeck,
  deleteDeck,
  transactionDeck,
  expected,
  baselineRunDate,
};
