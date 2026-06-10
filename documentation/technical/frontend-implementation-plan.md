# Frontend Implementation Plan

## Overview

This plan describes the phased build-out of a React single-page application (SPA)
that modernizes the main menu navigation of the legacy COBOL/CICS **Portfolio
Management System** (CLBS). The legacy online layer presents a 3270 terminal
menu (`MENMAP`) that branches to a Portfolio Position Inquiry screen (`POSMAP`),
a Transaction History screen (`HISMAP`), and a system Error screen (`ERRMAP`),
all defined in the BMS mapset [`src/maps/INQSET.bms`](../../src/maps/INQSET.bms).

This is a **frontend-only** phase: there is no backend, database, or
authentication. All data is **mocked** with static JSON. The BMS map
`src/maps/INQSET.bms` is treated as the **source of truth** for screen layout,
field sizes, and navigation, and the COBOL copybooks/programs are the source of
truth for data shapes so that a real backend can be plugged in later without
reshaping the UI.

The goal is to replicate the *navigation and screen behavior* of the legacy
menu, not to reimplement business logic. Field lengths, numeric formats, and the
3-option menu structure are preserved to keep traceability back to the mainframe
screens.

## Module Dependency Analysis (Migration Sequencing)

Before laying out the frontend phases, it is worth understanding how the legacy
modules depend on each other, because that dependency graph is what drives the
*order* in which screens can be safely modernized — first as a mocked frontend,
and later when a backend is introduced.

### Legacy online call graph

The CICS online layer is a controller-with-handlers pattern. `INQONLN` is the
main transaction controller; it receives the menu selection and `LINK`s to the
appropriate handler, which in turn depends on shared infrastructure modules:

```
INQONLN (main controller / menu dispatcher)   src/programs/online/INQONLN.cbl
  ├─ EVALUATE INQCOM-FUNCTION  (MENU / INQP / INQH / EXIT)
  ├─ LINK INQPORT   ── Portfolio Position Inquiry   src/programs/online/INQPORT.cbl
  │     └─ CICS READ FILE('POSFILE')   (VSAM position master, copybook POSREC)
  ├─ LINK INQHIST   ── Transaction History          src/programs/online/INQHIST.cbl
  │     ├─ LINK DB2ONLN  (DB2 connect)
  │     ├─ LINK CURSMGR  (cursor / array fetch over POSHIST)
  │     └─ LINK DB2RECV  (DB2 recovery/retry)
  ├─ LINK SECMGR    ── security check (per request)  src/programs/online/SECMGR.cbl
  └─ LINK ERRHNDL   ── centralized error handler     src/programs/online/ERRHNDL.cbl
```

Shared data contracts that every module agrees on:

- `INQCOM` ([`src/copybook/online/INQCOM.cpy`](../../src/copybook/online/INQCOM.cpy)) —
  the COMMAREA passed between `INQONLN` and every handler. It carries the
  selected function (`MENU`/`INQP`/`INQH`/`EXIT`), the account number, a response
  code, and an error message. This is effectively the **routing + shared state
  contract** of the whole online layer.
- `ERRHND` ([`src/copybook/online/ERRHND.cpy`](../../src/copybook/online/ERRHND.cpy)) —
  the error-handling structure (program, paragraph, severity, message, action)
  used by `ERRHNDL`.
- `POSREC` ([`src/copybook/common/POSREC.cpy`](../../src/copybook/common/POSREC.cpy)) —
  position record read by `INQPORT`.
- `WS-HISTORY-ENTRY` (defined inline in
  [`src/programs/online/INQHIST.cbl`](../../src/programs/online/INQHIST.cbl)) —
  the per-row transaction history shape rendered into the 10 `ROW` fields of
  `HISMAP`.

### What depends on what, and what that implies for ordering

| Order | Module / contract | Depends on | Why it must come first |
|:-----:|:------------------|:-----------|:-----------------------|
| 1 | `INQCOM` COMMAREA + `ERRHND` | nothing (pure data) | Every screen passes the COMMAREA and surfaces errors. The shared **types** and the **error surface** must exist before any screen is built. |
| 2 | `INQONLN` (menu dispatcher) | `INQCOM` | Nothing can be reached without the menu/router. It is the single entry point that fans out to the handlers. |
| 3 | `INQPORT` (position inquiry) | `INQCOM`, `POSREC`, VSAM `POSFILE` | Self-contained single-record `READ`. No cross-module fan-out → simplest handler to modernize first. |
| 4 | `INQHIST` (history) | `INQCOM`, `DB2ONLN`, `CURSMGR`, `DB2RECV` | Heaviest dependency chain (DB2 connect → cursor/array fetch → recovery) and adds **pagination** state. Modernize after the simpler position screen. |
| 5 | `EXIT` + `ERRHNDL` | `INQONLN`, `ERRHND` | Session-end and the global error screen are cross-cutting; they wrap the flows that already exist, so they are finished last. |

The frontend phases below follow exactly this dependency order:

- **Shared contract first** (Phase 0): scaffold + the `INQCOM`/`ERRHND`-equivalent
  TypeScript types, the global layout (header + error area), and the router —
  the modern analog of "the COMMAREA and the dispatcher must exist first."
- **Dispatcher next** (Phase 1): the dashboard *is* `INQONLN`'s `EVALUATE` —
  it routes `MENU`→`/`, `INQP`→`/portfolios`, `INQH`→`/transactions`, `EXIT`→exit.
- **Simplest leaf handler** (Phase 2): `/portfolios` mirrors `INQPORT`, a single
  self-contained lookup.
- **Most-dependent leaf handler** (Phase 3): `/transactions` mirrors `INQHIST`,
  which carries the most state (pagination over many rows).
- **Cross-cutting concerns last** (Phase 4): the exit/session-end flow and the
  global error boundary (`ERRMAP`/`ERRHNDL`), which wrap everything else.

This same ordering carries forward to the eventual backend integration: the
COMMAREA/`INQCOM` contract becomes the API request/response shape, `INQPORT`'s
single VSAM read becomes the first endpoint to wire up, and `INQHIST`'s
DB2 cursor (with `DB2ONLN`/`CURSMGR`/`DB2RECV`) becomes the paginated
history endpoint — so building the mock data in these shapes now avoids reshaping
the UI later.

## Phase 0: Project Foundation

- Scaffold with **Vite + React + TypeScript**.
- Install dependencies: **React Router v6**, a component library
  (suggest **Tailwind CSS** or **MUI**), **ESLint**, **Prettier**.
- Folder structure:
  ```
  src/
    components/    # Shared UI components (Layout, ErrorBoundary, NavCard)
    pages/         # Route-level page components
    mocks/         # Static JSON mock data
    types/         # TypeScript interfaces mirroring copybook structures
    routes/        # Route definitions
  ```
- Configure React Router with route stubs for `/`, `/portfolios`, `/transactions`.
- Add a global **layout** component (header with app title
  "Portfolio Management System", nav bar, and an error message area — mirrors the
  `MENMAP` header at row 1 and the `ERRMSG` field at row 23 of
  [`src/maps/INQSET.bms`](../../src/maps/INQSET.bms)).
- **Acceptance:** app runs locally, all routes render placeholder content.

## Phase 1: Main Menu / Dashboard

- Implement the `/` (dashboard) page with the **3 navigation options** as
  clickable cards/links, matching `MENMAP` options at rows 5–7:
  1. "Portfolio Position Inquiry" → navigates to `/portfolios` (legacy `INQP`)
  2. "Transaction History" → navigates to `/transactions` (legacy `INQH`)
  3. "Exit" → triggers a logout/session-end flow (legacy `EXIT`; placeholder for
     now, shows confirmation)
- Add an **error message banner** component at the bottom of the page (mirrors the
  `ERRMSG` field at row 23 of `MENMAP`).
- Style to clearly present the options; no authentication required yet.
- **Acceptance:** user can click each option and navigate to the correct route;
  "Exit" shows a confirmation dialog.

## Phase 2: Portfolio Position Inquiry

- Implement the `/portfolios` page (mirrors `POSMAP` and the legacy `INQPORT`
  handler).
- **Account number input** field (mirrors `ACCTIN` — 10 chars, numeric).
- On submit, display mock position data (mirrors the `POSMAP` output fields):
  - Fund ID (6 chars — `FUNDOUT`)
  - Fund Name (30 chars — `NAMEOUT`)
  - Units (numeric with 2 decimal places — `UNITOUT`)
  - Cost Basis (currency — `COSTOUT`)
  - Market Value (currency — `VALOUT`)
- Add navigation: **"Back to Menu"** link (replaces `PF3=Exit`).
- Create mock data file `src/mocks/portfolios.json` with 3–5 sample portfolio
  positions.
- Define a TypeScript interface in `src/types/portfolio.ts` based on copybook
  fields from [`src/copybook/online/INQCOM.cpy`](../../src/copybook/online/INQCOM.cpy)
  and the `POSMAP` fields (cross-reference
  [`src/copybook/common/POSREC.cpy`](../../src/copybook/common/POSREC.cpy) for the
  underlying position record layout).
- **Acceptance:** entering a valid mock account number displays position details;
  an invalid one shows an error in the message area.

## Phase 3: Transaction History

- Implement the `/transactions` page (mirrors `HISMAP` and the legacy `INQHIST`
  handler).
- **Account number input** field (mirrors `HISAIN` — 10 chars).
- Display results in a **table** with columns: Date, Type, Units, Price, Amount
  (mirrors the `HISMAP` column headers at row 5).
- **Paginate at 10 rows per page** (mirrors the 10 `ROW1`–`ROW10` fields in
  `HISMAP`).
- **Previous/Next** pagination controls (replaces `PF7`/`PF8`).
- Transaction types to display: **BU** (Buy), **SL** (Sell), **TR** (Transfer),
  **FE** (Fee).
- Create mock data file `src/mocks/transactions.json` with **25+** sample
  transactions.
- Define a TypeScript interface in `src/types/transaction.ts` based on the
  `WS-HISTORY-ENTRY` fields from
  [`src/programs/online/INQHIST.cbl`](../../src/programs/online/INQHIST.cbl)
  (`WS-TRANS-DATE` `X(10)`, `WS-TRANS-TYPE` `X(4)`, `WS-TRANS-UNITS`,
  `WS-TRANS-PRICE`, `WS-TRANS-AMOUNT` each `S9(9)V99`).
- **"Back to Menu"** link.
- **Acceptance:** entering a valid account shows paginated transaction history;
  pagination works correctly.

## Phase 4: Exit Flow & Polish

- Implement the **exit/logout confirmation modal**.
- When the user clicks "Exit" on the dashboard, show an "Are you sure?" dialog.
- On confirm, redirect to a simple **"Session Ended"** page (placeholder for a
  future login screen).
- Add a global **error boundary** component (mirrors `ERRMAP` — shows error code
  + details, per `ERRCOUT`/`ERRDOUT`).
- Final polish: responsive layout, consistent styling, keyboard navigation (Tab
  through options like the original 3270 terminal).
- **Acceptance:** full navigation flow works end-to-end with mock data; no
  console errors.

## Legacy-to-Modern Mapping Reference

| Legacy Screen (BMS) | Legacy Handler | Modern Route | Component |
|:---------------------|:---------------|:-------------|:----------|
| MENMAP               | INQONLN        | `/`          | DashboardPage |
| POSMAP               | INQPORT        | `/portfolios`| PortfolioInquiryPage |
| HISMAP               | INQHIST        | `/transactions` | TransactionHistoryPage |
| ERRMAP               | ERRHNDL        | Global       | ErrorBoundary |

## Mock Data Contract (for future backend integration)

Mock data shapes should align with these legacy structures so the backend can be
plugged in later without reshaping the UI:

- **Portfolio position:** portfolio ID (`PORT` + 5 digits), fund ID, fund name,
  units, cost basis, market value.
- **Transaction:** date (`X(10)`), type (`X(4)`: BU/SL/TR/FE), units
  (`9(9)V99`), price (`9(9)V99`), amount (`9(9)V99`).
- **Error response:** error code (`X(8)`), error message (`X(80)`).

These shapes trace directly back to the COBOL contracts:
`INQCOM` ([`src/copybook/online/INQCOM.cpy`](../../src/copybook/online/INQCOM.cpy))
for the account number / response code / error message envelope,
`WS-HISTORY-ENTRY` in [`INQHIST.cbl`](../../src/programs/online/INQHIST.cbl) for
transactions, and `POSREC`
([`src/copybook/common/POSREC.cpy`](../../src/copybook/common/POSREC.cpy)) /
`PORTFLIO` ([`src/copybook/common/PORTFLIO.cpy`](../../src/copybook/common/PORTFLIO.cpy))
for the portfolio/position records.
