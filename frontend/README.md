# CLBS Portfolio Management — Web Frontend

Modernized web frontend for the COBOL Legacy Benchmark Suite Portfolio
Management System. This slice implements **US-5: Transaction History Inquiry**
([MBA-1429](https://cog-gtm.atlassian.net/browse/MBA-1429)), replacing the
`HISMAP` green screen (`../src/maps/INQSET.bms`) and the `INQHIST` CICS program
with a React + TypeScript app backed by mock fixtures. The data model is a
direct translation of the `HISTREC.cpy` copybook.

## Tech stack

- React 18 + TypeScript
- Vite (dev server / build)
- React Router (client-side routing)
- Vitest + Testing Library (unit + integration tests)
- ESLint (lint)

## Getting started

```bash
cd frontend
npm install
npm run dev      # start the dev server at http://localhost:5173
```

Search for account `ACCT100001` to see a paginated history; `ACCT100002`,
`ACCT100003` and `ACCT100006` carry smaller result sets.

## Scripts

| Command              | Description                         |
| -------------------- | ----------------------------------- |
| `npm run dev`        | Start the Vite dev server           |
| `npm run build`      | Type-check and build for production |
| `npm run preview`    | Preview the production build        |
| `npm run lint`       | Run ESLint                          |
| `npm run typecheck`  | Type-check without emitting         |
| `npm test`           | Run the test suite once             |
| `npm run test:watch` | Run tests in watch mode             |

## Architecture

```
src/
  components/        Reusable UI (Layout, Pagination)
  data/              Mock fixtures mirroring COBOL copybooks
  features/
    history/         History inquiry + record detail pages, CSV export
  services/          Swappable service layer (API integration points)
  types/             Domain types translated from HISTREC.cpy / TRNREC.cpy
  utils/             COMP-3 decimal, COBOL date, and CSV helpers
```

### Legacy mapping

| Legacy behaviour                    | Web equivalent                             |
| ----------------------------------- | ------------------------------------------ |
| `HISMAP` account field (`HISAIN`)   | Account number search input                 |
| `HISMAP` ten data rows              | History table, 10 rows per page             |
| `PF7` / `PF8` scrolling             | Previous / Next pagination controls         |
| `PF3` exit                          | Router navigation (back to the inquiry)     |
| `INQHIST` POSHIST cursor (date DESC) | `HistoryService.listByAccount`, newest first |

Date-range and record-type filtering, row drill-down, and CSV export have no
3270 equivalent — they are the modernization gains called for by the ticket.

### API integration point

The UI talks to a `HistoryService` interface
(`src/services/historyService.ts`). The current implementation
(`MockHistoryService`) is backed by an in-memory fixture; when the backend is
ready, swap in a REST-backed implementation:

| Operation       | REST endpoint                          | COBOL program |
| --------------- | -------------------------------------- | ------------- |
| `listByAccount` | `GET /api/accounts/:accountNo/history` | `INQHIST`     |
| `get`           | `GET /api/history/:recordKey`          | `INQHIST`     |

`recordKey` is the 26-character `HIST-KEY` (portfolio id + date + time +
sequence number).

### Monetary precision

`TRN-AMOUNT` is `S9(13)V9(2) COMP-3` and units/prices are `S9(11)V9(4) COMP-3`.
To preserve packed-decimal precision (which exceeds the JS safe-integer range
once scaled), these values are kept as decimal **strings** throughout the app
and formatted with string operations (see `src/utils/decimal.ts`).
