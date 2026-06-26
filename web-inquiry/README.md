# PINQ — Portfolio Online Inquiry (Web UI)

A frontend-only web application that replicates the navigation and interaction
model of the CLBS COBOL online inquiry subsystem (the `PINQ` / `INQONLN` CICS
layer in this repository). There is **no backend** — all data is mocked and held
in-memory behind a swappable service layer.

## What it mirrors

| Legacy artifact | Web equivalent |
| --- | --- |
| `INQSET.bms` (MENMAP/POSMAP/HISMAP/ERRMAP) | The four screens / routes |
| `INQCOM.cpy` COMMAREA | `SessionContext` (`function`, `accountNo`, `responseCode`, `errorMsg`) |
| `INQONLN.cbl` front controller | React Router + menu routing |
| `INQPORT.cbl` | Portfolio Position screen + `getPosition` (incl. "Position not found for account") |
| `INQHIST.cbl` | Transaction History screen + `getHistory` (10 rows/page, date desc) |
| `SECMGR` USERID check | Stubbed sign-on screen + user indicator |
| PF3 / PF7 / PF8 | Exit / Previous / Next (buttons **and** real F3/F7/F8 keys) |

## Screens / routes

1. `/login` — stubbed SECMGR sign-on (any user id accepted).
2. `/menu` — Main Menu (MENMAP): Portfolio Position Inquiry, Transaction History, Exit.
3. `/portfolio` — Portfolio Position Inquiry (POSMAP): Fund ID, Fund Name, Units, Cost Basis, Market Value.
4. `/history` — Transaction History (HISMAP): Date, Type, Units, Price, Amount with paging.
5. `/error` — System Error (ERRMAP): Error Code + Details, acknowledge to return to menu.

## Mock data (see `src/data/mockData.ts`)

| Account | Demonstrates |
| --- | --- |
| `0000001001` | Position found + 23 transactions (multi-page paging) |
| `0000001002` | Position found + 4 transactions (single page) |
| `0000002001` | Position found + exactly 10 transactions |
| `0000003001` | Closed position + **no** transaction history (empty page) |
| `0000009999` | **Position not found** path |
| `0000000000` | **System error** path (simulated data-access failure) |

## Swapping in a real backend

All data access goes through `src/services/inquiryService.ts`
(`getPosition`, `getHistory`, `authenticate`). Reimplement those functions with
real API calls (e.g. `fetch`) and no UI code needs to change.

## Tech stack

React 19 + TypeScript, Vite, React Router. Read-only inquiry only — no
write/transaction operations.

## Develop

```bash
npm install
npm run dev        # start the dev server
npm run build      # typecheck + production build
npm run lint       # eslint
npm run typecheck  # tsc --noEmit
```

Requires Node 20+.
