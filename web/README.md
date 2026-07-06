# Web Modernization Layer

A modern **React** frontend backed by a new **REST/JSON API** that bridges the
legacy COBOL/CICS/BMS **Investment Portfolio Management System**.

The original application has no HTTP interface — it is driven by 3270 BMS
screens over CICS transactions. This layer implements the **"new backend against
the same data"** approach: a standalone API mirrors the read semantics of the
COBOL online inquiries and serves them as JSON to a browser UI. **No live
z/OS / CICS / DB2 runtime is required** — the API ships with a seeded in-memory
data layer that matches the DB2 schema, with a documented path to real DB2.

No existing COBOL source is modified.

```
┌───────────────┐     HTTP/JSON      ┌──────────────────┐     Repository      ┌──────────────────────┐
│ React frontend │ ───────────────▶ │  Express REST API │ ─── interface ────▶ │ Data layer            │
│ (Vite + TS)    │                   │  (Node + TS)      │                     │ • in-memory seed (now)│
│ web/frontend   │ ◀─────────────── │  web/api          │ ◀───────────────── │ • DB2 (documented)    │
└───────────────┘                    └──────────────────┘                     └──────────────────────┘
```

## Directory layout

| Path             | What                                                          |
| ---------------- | ------------------------------------------------------------ |
| `web/api/`       | REST API (Node.js + Express + TypeScript)                    |
| `web/frontend/`  | React app (Vite + React + TypeScript + React Router)         |

## How it maps to the COBOL system

The API and UI mirror the original online inquiry programs and BMS maps.

| REST endpoint                              | COBOL program | BMS map (`src/maps/INQSET.bms`) | Semantics preserved |
| ------------------------------------------ | ------------- | ------------------------------- | ------------------- |
| `GET /api/portfolios/:accountNo/position`  | `INQPORT.cbl` | `POSMAP`                        | Single position lookup by account key (INQPORT reads POSFILE by `POSITION-ACCOUNT`). Returns Fund ID, Fund Name, Units, Cost Basis, Market Value. |
| `GET /api/portfolios/:accountNo/history`   | `INQHIST.cbl` | `HISMAP`                        | Transaction list by account, ordered by date descending (INQHIST's `HISTORY_CURSOR` `SELECT ... ORDER BY TRANS_DATE DESC`). Returns Date, Type, Units, Price, Amount. |
| `GET /api/portfolios/:portfolioId`         | —             | (master record)                 | Returns `PORTFOLIO_MASTER` fields. |

The UI screens map 1:1 to the BMS maps:

| React route  | BMS map  | Notes                                                          |
| ------------ | -------- | ------------------------------------------------------------- |
| `/`          | `MENMAP` | Main menu linking to the two inquiries.                       |
| `/position`  | `POSMAP` | Account input + position detail (Fund ID/Name, Units, Cost Basis, Market Value). |
| `/history`   | `HISMAP` | Account input + table of transactions (Date, Type, Units, Price, Amount). |

The COBOL "not found" / error paths (e.g. `INQPORT` `P900-NOT-FOUND`
*"Position not found for account"*) map to HTTP `404` responses with an error
envelope, which the UI surfaces as inline error states.

### Data shapes

JSON field names are derived from the DB2 schema
(`src/database/db2/db2-definitions.sql`) and the COBOL copybooks
(`src/copybook/common/PORTFLIO.cpy`, `POSREC.cpy`, `TRNREC.cpy`), mapped from
`UPPER_SNAKE` / `PIC` fields to camelCase. See `web/api/src/types.ts`.

Key note on identifiers: the DB2 tables are keyed by `PORTFOLIO_ID` (`CHAR(8)`),
while the online inquiries are keyed by **account number**
(`INQCOM-ACCOUNT-NO` / `PORT-ACCOUNT-NO`, `PIC X(10)`). Each portfolio in the
seed data therefore carries **both** a `portfolioId` and an `accountNo`, so the
account-keyed inquiry endpoints and the portfolio-id master endpoint resolve to
the same underlying record.

## Running locally

Requires Node.js 18+ (developed on Node 20). Run the two projects in separate
terminals.

### 1. API (`web/api`)

```bash
cd web/api
npm install
cp .env.example .env      # optional; defaults are fine
npm run dev               # ts-node-dev, http://localhost:4000
```

Quick check:

```bash
curl http://localhost:4000/api/health
curl http://localhost:4000/api/portfolios/1000000001/position
curl http://localhost:4000/api/portfolios/1000000001/history
curl http://localhost:4000/api/portfolios/PORT0001
```

Production build: `npm run build && npm start`.

### 2. Frontend (`web/frontend`)

```bash
cd web/frontend
npm install
npm run dev               # http://localhost:5173
```

The Vite dev server proxies `/api/*` to `http://localhost:4000` (configurable
via `VITE_API_TARGET`; see `vite.config.ts`). Open http://localhost:5173 and try
sample accounts `1000000001`, `1000000002`, `1000000003`.

Production build: `npm run build` (output in `dist/`). Set `VITE_API_BASE_URL`
to point the built app at a deployed API.

## Sample data

The in-memory store (`web/api/src/data/seed.ts`) contains three portfolios with
positions and transaction history, shaped to the DB2 schema:

| Account       | Portfolio ID | Name                          | Status |
| ------------- | ------------ | ----------------------------- | ------ |
| `1000000001`  | `PORT0001`   | Anderson Retirement Portfolio | Active |
| `1000000002`  | `PORT0002`   | Beacon Capital Corporate Fund | Active |
| `1000000003`  | `PORT0003`   | Carter Family Trust           | Suspended |

## Swapping in real DB2

The API depends only on the `PortfolioRepository` interface
(`web/api/src/repository/PortfolioRepository.ts`). Selection is driven by the
`DATA_SOURCE` env var:

- `DATA_SOURCE=memory` (default) → `InMemoryPortfolioRepository` (seed data).
- `DATA_SOURCE=db2` → `Db2PortfolioRepository` (a documented stub).

To connect to a live DB2:

1. `cd web/api && npm install ibm_db` (IBM's official DB2 driver).
2. Implement the queries in
   `web/api/src/repository/Db2PortfolioRepository.ts` — the required SQL (which
   mirrors the COBOL programs) is written in comments next to each method,
   e.g. `SELECT ... FROM TRANSACTION_HISTORY WHERE PORTFOLIO_ID = ? ORDER BY
   TRANSACTION_DATE DESC` for history.
3. Set the connection settings in `.env` (`DB2_DATABASE`, `DB2_HOSTNAME`,
   `DB2_PORT`, `DB2_UID`, `DB2_PWD`) and `DATA_SOURCE=db2`.

Note: the account-number lookups assume `PORTFOLIO_MASTER` is extended with an
`ACCOUNT_NO` column (or a portfolio/account cross-reference table exists), since
the base DB2 schema is keyed by `PORTFOLIO_ID` while the online inquiries are
keyed by account number.
