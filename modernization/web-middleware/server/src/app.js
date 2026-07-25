const express = require('express');
const cors = require('cors');
const { positions, history, menuOptions } = require('./mockData');

const PAGE_SIZE = 10; // HISMAP shows ROW1..ROW10

/** INQCOM.cpy command area returned with every response. */
function commarea(fn, account, responseCode = 0, errorMsg = '') {
  return {
    inqcomFunction: fn,
    inqcomAccountNo: (account || '').padEnd(10).slice(0, 10),
    inqcomResponseCode: responseCode,
    inqcomErrorMsg: errorMsg.padEnd(80).slice(0, 80),
  };
}

function createApp() {
  const app = express();
  app.use(cors());
  app.use(express.json());

  // MENMAP / INQONLN P200-DISPLAY-MENU
  app.get('/api/menu', (_req, res) => {
    res.json({ commarea: commarea('MENU', ''), options: menuOptions });
  });

  // POSMAP / INQONLN P300 -> INQPORT
  app.get('/api/position', (req, res) => {
    const account = String(req.query.account || '').trim();
    const position = positions[account];
    if (!position) {
      // Mirrors P900-NOT-FOUND in INQPORT.cbl
      return res.status(404).json({
        commarea: commarea('INQP', account, 12, 'Position not found for account'),
        position: null,
      });
    }
    res.json({ commarea: commarea('INQP', account), position });
  });

  // HISMAP / INQONLN P400 -> INQHIST
  app.get('/api/history', (req, res) => {
    const account = String(req.query.account || '').trim();
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const rows = history[account];
    if (!rows) {
      return res.status(404).json({
        commarea: commarea('INQH', account, 12, 'History not found for account'),
        rows: [],
        page,
        pageSize: PAGE_SIZE,
        totalRows: 0,
        totalPages: 0,
      });
    }
    const totalPages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
    const start = (page - 1) * PAGE_SIZE;
    res.json({
      commarea: commarea('INQH', account),
      rows: rows.slice(start, start + PAGE_SIZE),
      page,
      pageSize: PAGE_SIZE,
      totalRows: rows.length,
      totalPages,
    });
  });

  // ERRMAP support: unknown routes surface as an error payload
  app.use((req, res) => {
    res.status(404).json({
      commarea: commarea('MENU', '', 16, `Invalid function or resource: ${req.path}`),
    });
  });

  return app;
}

module.exports = { createApp, PAGE_SIZE };
