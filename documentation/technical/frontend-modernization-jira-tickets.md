# Frontend Modernization — Jira Ticket Definitions

Phased plan to build a **frontend-only** React web application that modernizes the legacy
CICS online inquiry UI of the COBOL Legacy Benchmark Suite (CLBS).

Each entry below is written so an engineer can copy it directly into Jira (Title, Type,
Description, Acceptance Criteria, Legacy References).

**Scope guardrail for every ticket:** all data stays **mocked/stubbed**. There is no backend
integration in this plan. A future backend effort should only need to replace the mock service
module introduced in Phase 5 (and consumed from Phase 3 onward).

## Legacy source map (shared context)

| Concern | Legacy artifact | Lines |
| --- | --- | --- |
| CICS controller / function routing (`MENU`, `INQP`, `INQH`, `EXIT`) | `src/programs/online/INQONLN.cbl` | 62-77 |
| Menu display (`P200-DISPLAY-MENU`, sends map `INQMNU`) | `src/programs/online/INQONLN.cbl` | 92-98 |
| Main menu screen (`MENMAP`), three options | `src/maps/INQSET.bms` | 12-17 |
| Portfolio position screen (`POSMAP`) | `src/maps/INQSET.bms` | 23-49 |
| Portfolio inquiry program (`INQPORT`) | `src/programs/online/INQPORT.cbl` | 29-34, 96-110 |
| Transaction history screen (`HISMAP`) | `src/maps/INQSET.bms` | 53-86 (rows/legend 60-84) |
| Transaction history program (`INQHIST`) | `src/programs/online/INQHIST.cbl` | 20-26 |
| Error screen (`ERRMAP`): Error Code + Details | `src/maps/INQSET.bms` | 89-97 |
| PF-key conventions: `PF3=Exit  PF7=Previous  PF8=Next` | `src/maps/INQSET.bms` | 47-48, 83-84 |
| Online COMMAREA copybook (`INQCOM`) | `src/copybook/online/INQCOM.cpy` | 1-12 |

**Correction to the original analysis:** the `INQCOM` copybook *does* exist at
`src/copybook/online/INQCOM.cpy` (it was reported missing during initial indexing). Its fields are:

```
05 INQCOM-FUNCTION      PIC X(4)      88s: MENU / INQP / INQH / EXIT
05 INQCOM-ACCOUNT-NO    PIC X(10)
05 INQCOM-RESPONSE-CODE PIC S9(8) COMP
05 INQCOM-ERROR-MSG     PIC X(80)
```

So the *navigation/session* model can be typed from `INQCOM` directly. The *position* and
*transaction history* record shapes are still inferred from the BMS field definitions and from
`INQHIST` working storage (`WS-HISTORY-ENTRY`: `PIC X(10)` date, `PIC X(4)` type, three
`S9(9)V99 COMP-3` numerics); `INQPORT` reads `POSREC`/`SQLPOS`, which should be consulted before
freezing the portfolio types.

---

## CLBS-FE-1 — Epic

**Title / Summary:** Modernize CLBS online inquiry UI to a React frontend

**Type:** Epic

**Description:**

The CLBS online inquiry function is delivered today as a 3270/CICS application: a single
controller program `src/programs/online/INQONLN.cbl` receives a map, routes on a four-character
function code via `EVALUATE WS-COMMAREA-FUNCTION` (`MENU` / `INQP` / `INQH` / `EXIT`, lines 62-77),
and sends the menu map from `P200-DISPLAY-MENU` (lines 92-98). Screen layouts live in
`src/maps/INQSET.bms` (`MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP`), and navigation is driven by PF keys
(`PF3=Exit  PF7=Previous  PF8=Next`, BMS lines 47-48).

This epic delivers a **frontend-only** React + TypeScript web application that reproduces the
legacy main menu and all of its destinations with equivalent information architecture and
navigation semantics, using mock data only. No mainframe, DB2, VSAM or API integration is in
scope. The deliverable is a running, navigable UI that can later be pointed at a real service
layer by swapping a single mock service module.

Delivery is phased; each phase is a separate story linked to this epic:

1. CLBS-FE-2 — Phase 1: Scaffold foundational empty React app
2. CLBS-FE-3 — Phase 2: Build main menu shell and routing
3. CLBS-FE-4 — Phase 3: Implement Portfolio Position Inquiry screen (mocked)
4. CLBS-FE-5 — Phase 4: Implement Transaction History Inquiry screen (mocked)
5. CLBS-FE-6 — Phase 5: Add shared error handling and PF-key UX polish

**Acceptance Criteria:**

- All five phase stories are completed and linked to this epic.
- A React + TypeScript app lives in a new top-level `frontend/` directory and builds cleanly.
- A user can start at the main menu, reach Portfolio Position Inquiry and Transaction History,
  page through history results, trigger the error display, and return to the menu — mirroring the
  legacy `INQONLN` routing and `INQSET.bms` screen inventory.
- Every screen's displayed fields correspond one-to-one with the fields of its legacy BMS map.
- 100% of displayed data comes from a mock service module; no network calls to a backend exist in
  the codebase.
- A short README documents the legacy-to-React mapping and the swap-in point for a real API.

**Legacy References:** `src/programs/online/INQONLN.cbl` (62-77, 92-98);
`src/maps/INQSET.bms` (7-18, 23-49, 53-86, 89-97); `src/programs/online/INQPORT.cbl`;
`src/programs/online/INQHIST.cbl`; `src/copybook/online/INQCOM.cpy`.

---

## CLBS-FE-2 — Story (Phase 1)

**Title / Summary:** Phase 1: Scaffold foundational empty React app

**Type:** Story — *linked to epic CLBS-FE-1*

**Description:**

Create the foundation for the modernized inquiry UI. Scaffold a **React + TypeScript** application
with **Vite** in a new top-level directory `frontend/` (the repository currently contains only
COBOL/JCL/BMS sources under `src/` and docs under `documentation/`, so the frontend is additive and
isolated).

Included in this phase:

- Vite React-TS scaffold under `frontend/` with `package.json` scripts `dev`, `build`, `preview`.
- `react-router-dom` installed and a router mounted with a **single placeholder route** (`/`).
- A base layout component (e.g. `src/components/Layout.tsx`) providing the page frame that later
  phases populate with header/footer and a PF-key action area.
- Global styles / theme tokens (a terminal-inspired palette is acceptable; the legacy maps use
  bright headers, turquoise output fields and red error lines — see `INQSET.bms`).
- ESLint + Prettier configured and wired to a `lint` script.
- `frontend/README.md` describing the goal, how to run the app, and the fact that the app is
  frontend-only with mocked data.

Explicitly **not** in this phase: menu, portfolio, history or error screens; any mock data.

**Acceptance Criteria:**

- `npm install && npm run dev` in `frontend/` serves the app locally without errors.
- Navigating to `/` renders a placeholder home page inside the base layout.
- `npm run build` and `npm run preview` succeed.
- `npm run lint` passes with no errors; Prettier formatting is applied consistently.
- No feature screens, mock data, or backend calls exist yet.
- `frontend/README.md` exists and documents scripts plus the frontend-only/mock-data constraint.

**Legacy References:** background only — `src/programs/online/INQONLN.cbl`,
`src/maps/INQSET.bms`.

---

## CLBS-FE-3 — Story (Phase 2)

**Title / Summary:** Phase 2: Build main menu shell and routing

**Type:** Story — *linked to epic CLBS-FE-1*

**Description:**

Implement the modern equivalent of the legacy main menu (`MENMAP`) and the routing that the CICS
controller performs today.

The legacy menu (`src/maps/INQSET.bms` lines 12-17) presents a title
(`Portfolio Management System`), a `Select Option:` prompt and exactly three options:

1. `1. Portfolio Position Inquiry`
2. `2. Transaction History`
3. `3. Exit`

The option entered into the `OPTION` field is turned into a COMMAREA function code that
`INQONLN.cbl` routes on (`MENU` / `INQP` / `INQH` / `EXIT`, lines 62-77); `P200-DISPLAY-MENU`
(lines 92-98) re-sends the menu map. Reproduce this as client-side routing:

| Legacy function | Route | Destination |
| --- | --- | --- |
| `MENU` | `/` | Main menu |
| `INQP` | `/portfolio` | Portfolio Position Inquiry (placeholder in this phase) |
| `INQH` | `/history` | Transaction History (placeholder in this phase) |
| *error path* | `/error` | Error display (placeholder in this phase) |
| `EXIT` | — | Returns to `/` (menu) |

Also add a shared header mirroring the legacy screen titles and a footer/action area that renders
the legacy PF-key legend (`PF3=Exit  PF7=Previous  PF8=Next`, BMS lines 47-48) as on-screen
buttons; wiring the actual key handlers is Phase 5.

All data remains mocked/absent — the placeholder pages contain no data yet.

**Acceptance Criteria:**

- The main menu lists exactly the three legacy options with their legacy wording, numbered 1-3.
- Options are activated both by click and by the numeric selection consistent with the legacy
  `OPTION` field behaviour (documented if implemented as a shortcut).
- Routes `/`, `/portfolio`, `/history` and `/error` all resolve and render inside the base layout;
  an unknown route renders the error page (mirroring `WHEN OTHER` → `P900-ERROR-ROUTINE`).
- "Exit" returns the user to the main menu.
- Placeholder page components exist for portfolio and history and are clearly marked as
  placeholders.
- The shared header shows the screen title and the footer shows the PF-key legend on every screen.
- End-to-end navigation menu → portfolio → menu → history → menu works in the browser.
- `npm run lint` and `npm run build` pass.

**Legacy References:** `src/maps/INQSET.bms` (12-17, 47-48);
`src/programs/online/INQONLN.cbl` (62-77, 92-98); `src/copybook/online/INQCOM.cpy` (function codes).

---

## CLBS-FE-4 — Story (Phase 3)

**Title / Summary:** Phase 3: Implement Portfolio Position Inquiry screen (mocked)

**Type:** Story — *linked to epic CLBS-FE-1*

**Description:**

Replace the `/portfolio` placeholder with a working Portfolio Position Inquiry screen backed by
local mock data.

The legacy screen `POSMAP` (`src/maps/INQSET.bms` lines 23-49) has the title
`Portfolio Position Inquiry`, a single unprotected input `ACCTIN` (`Account:`, length 10), and the
following protected output fields:

| Legacy field | Label | BMS length |
| --- | --- | --- |
| `FUNDOUT` | Fund ID | 6 |
| `NAMEOUT` | Fund Name | 30 |
| `UNITOUT` | Units | 15 |
| `COSTOUT` | Cost Basis | 15 |
| `VALOUT` | Market Value | 15 |

`src/programs/online/INQPORT.cbl` takes the account number from the COMMAREA, reads file `POSFILE`,
and either formats the display (`P300-FORMAT-DISPLAY`) or falls into `P900-NOT-FOUND` (lines 96-101),
which returns the message `Position not found for account`. Field labels are declared in
`WS-MAP-FIELDS` (lines 29-34).

Implementation:

- Account input with basic validation (required, max 10 characters, matching the `PIC X(10)` field).
- A results panel rendering the five output fields with sensible formatting (units and money
  values right-aligned with fixed decimals — the underlying COBOL numerics are packed decimal with
  two decimal places).
- Local mock data keyed by account number, with at least a few accounts populated.
- A "not found" state mirroring `P900-NOT-FOUND`, showing the legacy wording.
- A back-to-menu action labelled as the PF3 equivalent.

Mock data must be consumed through a function boundary (e.g. `getPortfolioPosition(account)`) so
Phase 5 can centralize it into the shared service module with no component changes.

**Acceptance Criteria:**

- Entering a known mock account and submitting displays Fund ID, Fund Name, Units, Cost Basis and
  Market Value; no other fields are shown.
- Entering an unknown account displays the "position not found" message rather than an empty panel
  or a crash.
- Empty/over-length account input is rejected client-side with a clear message.
- A PF3 / back-to-menu control returns the user to `/`.
- Monetary and unit values are formatted consistently (two decimal places).
- All values originate from local mock data; no network requests are made.
- `npm run lint` and `npm run build` pass.

**Legacy References:** `src/programs/online/INQPORT.cbl` (29-34, 96-110);
`src/maps/INQSET.bms` `POSMAP` (23-49); `src/copybook/online/INQCOM.cpy`.

---

## CLBS-FE-5 — Story (Phase 4)

**Title / Summary:** Phase 4: Implement Transaction History Inquiry screen (mocked)

**Type:** Story — *linked to epic CLBS-FE-1*

**Description:**

Replace the `/history` placeholder with a working Transaction History Inquiry screen backed by
local mock data.

The legacy screen `HISMAP` (`src/maps/INQSET.bms` lines 60-84) has the title
`Transaction History Inquiry`, a single unprotected account input `HISAIN` (length 10), column
headers `Date`, `Type`, `Units`, `Price`, `Amount`, and exactly **ten** data rows
(`ROW1`-`ROW10`), plus the PF-key legend (`PF3=Exit  PF7=Previous  PF8=Next`, BMS lines 83-84).

`src/programs/online/INQHIST.cbl` defines the matching working-storage table (lines 20-26):

```
05 WS-HISTORY-ENTRY OCCURS 10 TIMES.
   10 WS-TRANS-DATE    PIC X(10).
   10 WS-TRANS-TYPE    PIC X(4).
   10 WS-TRANS-UNITS   PIC S9(9)V99 COMP-3.
   10 WS-TRANS-PRICE   PIC S9(9)V99 COMP-3.
   10 WS-TRANS-AMOUNT  PIC S9(9)V99 COMP-3.
```

It fetches rows through a DB2 cursor (`HISTORY_CURSOR`) and tracks whether more rows exist via the
`MORE-ROWS` / `NO-MORE-ROWS` condition names — the basis of PF7/PF8 paging.

Implementation:

- Account input with the same validation rules as Phase 3.
- A results table with the five legacy columns, rendering **10 rows per page**.
- Previous / Next paging over a larger mock dataset (enough rows for at least three pages for at
  least one account), with the Previous control disabled on the first page and Next disabled on the
  last page — the equivalent of the legacy `NO-MORE-ROWS` state.
- A page indicator (e.g. "Rows 11-20").
- Controls labelled with their PF-key equivalents (PF7 Previous, PF8 Next, PF3 Exit); key bindings
  themselves land in Phase 5.
- An empty-result state for accounts with no transactions.

Mock data must be reached through a function boundary (e.g.
`getTransactionHistory(account, page)`) so Phase 5 can move it behind the shared service module.

**Acceptance Criteria:**

- A known mock account renders up to 10 transaction rows with Date, Type, Units, Price and Amount.
- Next advances to the following 10 rows; Previous returns to the prior page; neither control can
  page beyond the dataset bounds.
- Paging state resets when a different account is queried.
- An account with no transactions shows an explicit empty-state message rather than an empty table.
- A PF3 / back-to-menu control returns the user to `/`.
- All values originate from local mock data; no network requests are made.
- `npm run lint` and `npm run build` pass.

**Legacy References:** `src/programs/online/INQHIST.cbl` (20-26);
`src/maps/INQSET.bms` `HISMAP` (53-86, PF legend 83-84); `src/copybook/online/INQCOM.cpy`.

---

## CLBS-FE-6 — Story (Phase 5)

**Title / Summary:** Phase 5: Add shared error handling and PF-key UX polish

**Type:** Story — *linked to epic CLBS-FE-1*

**Description:**

Complete the modernization with the cross-cutting concerns the legacy application handles in its
controller and shared maps.

**1. Error display.** The legacy `ERRMAP` (`src/maps/INQSET.bms` lines 89-97) is a dedicated screen
titled `System Error` with two fields — `Error Code:` (`ERRCOUT`, length 8) and `Details:`
(`ERRDOUT`, length 65) — and the prompt `Press ENTER to continue`. `INQONLN.cbl` routes to
`P900-ERROR-ROUTINE` for any unrecognized function code (`WHEN OTHER`, lines 62-77) and for failed
security checks; `INQPORT.cbl` populates `INQCOM-ERROR-MSG` / `INQCOM-RESPONSE-CODE` in
`P900-NOT-FOUND` and `P999-ERROR-ROUTINE` (lines 96-110). Implement a reusable error component
served both as the `/error` route and as a modal, taking an error code and details, with a
"continue" action that returns to the main menu.

**2. PF-key keyboard shortcuts.** Bind the legacy conventions globally
(`PF3=Exit  PF7=Previous  PF8=Next`, BMS lines 47-48 and 83-84):

| Key | Action |
| --- | --- |
| F3 | Exit / back to main menu |
| F7 | Previous page (history) |
| F8 | Next page (history) |

Keys must be no-ops where the action does not apply on the current screen, must not fight browser
defaults where avoidable, and must be mirrored by the on-screen buttons introduced in Phases 2-4.

**3. Centralized mock service.** Move all mock data behind a single service module (e.g.
`frontend/src/services/inquiryService.ts`) exposing the async operations used by the screens —
`getPortfolioPosition(account)` and `getTransactionHistory(account, page)` — returning promises so
that a later backend swap changes only this module.

**4. TypeScript types.** Define shared types for the domain records:

- `PortfolioPosition` — `fundId`, `fundName`, `units`, `costBasis`, `marketValue` (inferred from
  `POSMAP`, BMS lines 23-49, and `INQPORT` `WS-MAP-FIELDS` lines 29-34).
- `TransactionHistoryRow` — `date`, `type`, `units`, `price`, `amount` (from `INQHIST`
  `WS-HISTORY-ENTRY`, lines 20-26, and `HISMAP`, BMS lines 53-86).
- `InquiryFunction` / session types derived from `src/copybook/online/INQCOM.cpy`
  (`MENU` / `INQP` / `INQH` / `EXIT`, `accountNo`, `responseCode`, `errorMessage`).

**Note on provenance:** the `INQCOM` copybook was initially reported as missing but does exist at
`src/copybook/online/INQCOM.cpy`; the navigation/COMMAREA types can therefore be taken from source.
The **portfolio position record** shape is still inferred from the BMS map because the underlying
`POSREC` / `SQLPOS` layouts were not validated against the screen — flag any mismatch found while
implementing, and treat field lengths/precision as provisional until confirmed.

**Acceptance Criteria:**

- Triggering an error condition (e.g. navigating to an unknown route) renders the error display
  with an Error Code and Details, matching `ERRMAP`'s information content.
- The error display is reusable as both a route and a modal, and its continue action returns to the
  main menu.
- F3, F7 and F8 perform exit, previous and next respectively on the screens where those actions
  apply, and do nothing where they do not.
- On-screen buttons and keyboard shortcuts trigger identical behaviour.
- All portfolio and history screens read their data exclusively through the centralized service
  module; searching the app for mock arrays finds them only in that module (or data files it owns).
- `PortfolioPosition`, `TransactionHistoryRow` and the INQCOM-derived types are exported and used
  by the screens; no `any` types remain in the data path.
- The README documents that replacing the service module is the only change needed for backend
  integration, and records the provisional/inferred portfolio field types.
- `npm run lint` and `npm run build` pass.

**Legacy References:** `src/maps/INQSET.bms` `ERRMAP` (89-97), PF legend (47-48, 83-84);
`src/programs/online/INQONLN.cbl` (62-77); `src/programs/online/INQPORT.cbl` (96-110);
`src/programs/online/INQHIST.cbl` (20-26); `src/copybook/online/INQCOM.cpy` (1-12).
