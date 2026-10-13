# Portfolio Inquiry Frontend: Functional Requirements

Phase 1 of moving the CICS online inquiry layer to a modern web stack. This document records what the 3270 frontend does today, where the code and the docs disagree, and the requirements for the React replacement in [`frontend/`](../../frontend).

Every legacy behaviour below cites the source it came from. Where the code is incomplete or contradicts the docs, that is called out under **Gap** and the requirement states what the new frontend does instead.

## 1. Scope

In scope: the CICS transaction `PINQ` and everything a terminal user sees through it.

| Legacy artifact | Role | Source |
| --- | --- | --- |
| `PINQ` transaction | Entry point, runs `INQONLN` | `src/cics/PORTDFN.csd` |
| `INQSET` mapset | Four 24x80 screens: `MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP` | `src/maps/INQSET.bms` |
| `INQONLN` | Menu controller and dispatcher | `src/programs/online/INQONLN.cbl` |
| `INQPORT` | Position inquiry (VSAM `POSFILE` read) | `src/programs/online/INQPORT.cbl` |
| `INQHIST` | Transaction history (DB2 `POSHIST` via cursor) | `src/programs/online/INQHIST.cbl` |
| `SECMGR` | Validate user, authorize resource, write audit row | `src/programs/online/SECMGR.cbl` |
| `ERRHNDL` | Log error to `ERRLOG`, format message with trace ID, decide action | `src/programs/online/ERRHNDL.cbl` |
| `CURSMGR`, `DB2ONLN`, `DB2RECV` | Cursor, connection and retry plumbing | `src/programs/online/*.cbl` |
| `INQCOM`, `ERRHND`, `DB2REQ` | COMMAREA and error layouts | `src/copybook/online/*.cpy` |
| `POSREC`, `TRNREC`, `PORTFLIO` | Position, transaction and portfolio record layouts | `src/copybook/common/*.cpy` |

Out of scope: batch jobs, reports, portfolio maintenance (`PORTADD`/`PORTUPDT`/`PORTDEL`), and the backend API itself. The legacy online layer is read-only (`POSFILE` is `UPDATE(NO)`, history access is `SELECT`), so the new frontend is read-only too.

## 2. Legacy behaviour inventory

### 2.1 Main menu (`MENMAP`)

- Title "Portfolio Management System" and three options: `1. Portfolio Position Inquiry`, `2. Transaction History`, `3. Exit`.
- One numeric input `OPTION` (1 char, cursor starts here). Error line `ERRMSG` on row 23, red.
- `INQONLN` dispatches on `INQCOM-FUNCTION`: `MENU`, `INQP` (link `INQPORT`), `INQH` (link `INQHIST`), `EXIT` (end session). Anything else goes to the error routine.

### 2.2 Position inquiry (`POSMAP`, `INQPORT`)

- Input: `ACCTIN`, 10 characters (`INQCOM-ACCOUNT-NO PIC X(10)`).
- Output fields: Fund ID (6), Fund Name (30), Units (15), Cost Basis (15), Market Value (15).
- `INQPORT` reads `POSFILE` by account. Not found sets "Position not found for account". Any other CICS error sets "Error accessing position data" plus the response code.
- PF key legend: `PF3=Exit  PF7=Previous  PF8=Next`. Message line `POSMSG` on row 23.
- The record layout (`POSREC`) also carries portfolio ID, position date, currency and status `A`=Active, `C`=Closed, `P`=Pending.

### 2.3 Transaction history (`HISMAP`, `INQHIST`)

- Input: `HISAIN`, 10 characters.
- Columns: Date (10), Type (4), Units (10), Price (10), Amount (12). Ten rows per screen (`ROW1`..`ROW10`, `WS-HISTORY-ENTRY OCCURS 10`).
- Query: `POSHIST` for the account, `ORDER BY TRANS_DATE DESC`.
- PF key legend: `PF3=Exit  PF7=Previous  PF8=Next`.
- Transaction types (`TRNREC`): `BU` Buy, `SL` Sell, `TR` Transfer, `FE` Fee. Statuses: `P` Pending, `D` Done, `F` Failed, `R` Reversed.

### 2.4 Errors (`ERRMAP`, `ERRHNDL`)

- Screen shows "System Error", an Error Code (8) and Details (65), and "Press ENTER to continue".
- `ERRHNDL` writes the error to `ERRLOG`, generates a trace ID if none was supplied, and formats `Error in <program> - <message> (<trace id>)`.
- Severity drives the action: Fatal -> abend (`IERR`), Warning/Info -> continue, anything else -> return.
- `DB2RECV` retries a failed DB2 connection with a delay before giving up.

### 2.5 Security (`SECMGR`)

- Three calls per request: `V` validate the CICS user ID, `A` check `AUTHFILE` for `READ` on `INQONLN`, `L` insert an `AUDITLOG` row (timestamp, user, terminal, transaction, program, access type).
- Messages: "User validation failed", "Unable to obtain user credentials", "Access denied", "Authorization check failed", "Audit logging failed".

### 2.6 Gaps between code and docs

The architecture doc and backlog describe the online layer as complete. The code does not support that:

| # | Gap | Evidence | Requirement impact |
| --- | --- | --- | --- |
| G1 | `INQONLN` sends/receives maps `INQMAP` and `INQMNU`, which do not exist in `INQSET` (it defines `MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP`). It also references `WS-COMMAREA-FUNCTION` and `WS-ERROR-MESSAGE`, which are not declared. | `INQONLN.cbl` P100/P200 | Screen flow is taken from the BMS maps and the dispatcher's intent, not from runnable code. |
| G2 | PF keys are shown on screen but no program reads `EIBAID`, so PF3/PF7/PF8 do nothing. | `INQSET.bms`, all online programs | Paging and exit are designed fresh (FR-5, FR-7). |
| G3 | `SECMGR` is called after the inquiry has already been dispatched and displayed. | `INQONLN.cbl` P100 order | Authorization must complete before any account data is shown (FR-9). |
| G4 | `CURSMGR.cbl` ends after the open paragraph; fetch and close are missing. | `CURSMGR.cbl` | History paging is specified independently of the cursor code. |
| G5 | `INQHIST` selects `TRANS_UNITS`, `TRANS_PRICE`, `TRANS_AMOUNT`; `POSHIST.sql` has `QUANTITY`, `PRICE`, `AMOUNT`. | `INQHIST.cbl` P200, `POSHIST.sql` | API contract uses the table's column meanings (quantity, price, amount). |
| G6 | Account number length disagrees: 10 chars in `INQCOM`/BMS, 9 digits in the data dictionary, `CHAR(8)` in `POSHIST`. | `INQCOM.cpy`, `data-dictionary.md`, `POSHIST.sql` | Frontend accepts the 10-character screen/COMMAREA format, numeric only (FR-1). Backend must resolve the storage mismatch. |
| G7 | `INQPORT` reads one `POSFILE` record per account and maps it straight into `POSMAP`, so only one fund can show. `POSREC` is keyed by portfolio + date + investment, so an account can hold many positions. | `INQPORT.cbl`, `POSREC.cpy` | Show all positions for the account (FR-4). This is what PF7/PF8 on `POSMAP` imply. |
| G8 | `ERRHNDL` has no `DEFINE PROGRAM` in the CSD. | `PORTDFN.csd` | No frontend impact; noted for the backend team. |
| G9 | Transaction status and the `FE` type exist in `TRNREC` but `HISMAP` has no status column; the `POSHIST` comment lists only BU/SL/TR. | `TRNREC.cpy`, `INQSET.bms`, `POSHIST.sql` | Show type and status for every row (FR-6). |
| G10 | `documentation/user/README.md`, `operations/README.md` and `technical/README.md` are empty, and no test data has account numbers or positions. | repo | Sample data for the frontend is synthetic and labelled as such. |

## 3. Functional requirements

Priority: **M** must, **S** should.

### Account lookup and session

| ID | Requirement | Legacy source | Pri |
| --- | --- | --- | --- |
| FR-1 | A persistent account lookup accepts a 10-digit account number, with or without dashes/spaces (shown as `4471-0028-93`). Non-numeric input or wrong length is rejected inline before any request is made. | `ACCTIN`/`HISAIN` X(10), `INQCOM-ACCOUNT-NO` | M |
| FR-1a | If the user types 4 or more digits and exactly one known account ends with them, that account is selected. | new | S |
| FR-2 | The selected account stays selected across all screens. The user never re-keys it to move between positions and history. | replaces separate `ACCTIN` and `HISAIN` | M |
| FR-3 | The account header shows account name, account type, formatted account number, open date and the "prices as of" date. | `PORTFLIO` (`PORT-CLIENT-NAME`, `PORT-CLIENT-TYPE`, `PORT-CREATE-DATE`) | M |

### Navigation (replaces `MENMAP` and PF keys)

| ID | Requirement | Legacy source | Pri |
| --- | --- | --- | --- |
| FR-NAV-1 | Three tabs replace the menu: Overview, Positions (option 1), Transactions (option 2). Keys `1`, `2`, `3` switch tabs when focus is not in a field. | `MENMAP` options, `INQCOM-FUNCTION` | M |
| FR-NAV-2 | The active tab is reflected in the URL hash so it survives reload and can be linked. | new | S |
| FR-NAV-3 | `/` focuses the search box of the current tab (account lookup on Overview). `Esc` leaves a field. | new | S |
| FR-NAV-4 | There is no Exit option. Closing the tab ends the session. | `3. Exit`, `PF3` | M |

### Overview (new summary screen)

| ID | Requirement | Pri |
| --- | --- | --- |
| FR-OV-1 | KPIs: total market value with today's change, cost basis with active position count, unrealized gain/loss with % and realized total, pending transaction count with net amount. Totals use Active positions only. | M |
| FR-OV-2 | Portfolio value chart: market value line and cost basis dashed line, ranges 3M / 6M / 1Y / All, hover tooltip with date, value and gain vs cost. | S |
| FR-OV-3 | Allocation by asset class for Active positions: stacked bar plus list with amount and %. | S |
| FR-OV-4 | Recent activity: latest 6 transactions with type, fund, date, units @ price, amount, status, and a link to the Transactions tab. | M |

### Positions (replaces `POSMAP`)

| ID | Requirement | Legacy source | Pri |
| --- | --- | --- | --- |
| FR-4 | List every position for the account: fund name and code, asset class, units held, NAV, cost basis, market value, gain/loss ($ and %), portfolio weight, status. | `POSMAP` Fund ID/Name/Units/Cost/Value, `POSREC` (G7) | M |
| FR-4a | Status is one of Active / Pending / Closed (`POSREC` `A`/`P`/`C`). Closed positions show realized gain and no cost or value. Active positions with settling buys show "+N settling". | `POS-STATUS` | M |
| FR-4b | Filter by status with counts, search by fund name or code, sort by any column. | new | M |
| FR-4c | Table and Cards layouts; the choice is remembered. Cards is the default on narrow screens. | new | S |
| FR-4d | Table footer totals cost basis, market value and gain/loss for Active positions. | new | S |
| FR-5 | No PF7/PF8: all positions fit on one scrollable screen. | `PF7`/`PF8` on `POSMAP` (G2) | M |

### Transactions (replaces `HISMAP`)

| ID | Requirement | Legacy source | Pri |
| --- | --- | --- | --- |
| FR-6 | Columns: date, transaction ID, type, fund, units (signed by direction), price, amount, status. Default sort is date descending. Failed and Reversed amounts are struck through. | `HISMAP` columns, `ORDER BY TRANS_DATE DESC`, `TRNREC` | M |
| FR-6a | Type is one of Buy / Sell / Transfer (in or out) / Fee (`BU`/`SL`/`TR`/`FE`). Status is one of Pending / Done / Failed / Reversed (`P`/`D`/`F`/`R`). | `TRNREC` (G9) | M |
| FR-6b | Filter by type (chips with counts), status, date range, and free text (fund name, fund code, transaction ID). "Clear filters" resets all filters. | new | M |
| FR-6c | A summary strip shows matching count and Done-only totals: bought, sold, net transfers, fees. | new | S |
| FR-7 | Pagination replaces PF7/PF8: 10 rows per page by default (same as `HISMAP`), selectable 10/25/50, numbered pages with previous/next, keys `[` and `]`. | `ROW1..ROW10`, `PF7`/`PF8` | M |
| FR-7a | Any filter or page-size change goes back to page 1. | new | M |

### Errors, empty states and security

| ID | Requirement | Legacy source | Pri |
| --- | --- | --- | --- |
| FR-8 | Account not found shows "No portfolio found for <input>" under the lookup and keeps the current account on screen. | `INQPORT` P900 "Position not found for account" | M |
| FR-8a | A backend failure shows an error panel with error code, details and trace ID (same fields as `ERRMAP`) and a Retry button (replaces "Press ENTER to continue"). | `ERRMAP`, `ERRHNDL` P300 | M |
| FR-8b | Loading state while a request is in flight; filters that match nothing show an empty state with a clear-filters action. | new | M |
| FR-9 | If the user is not authorized for an account, show "Access denied" and no account data. Authorization happens before data is returned, not after. | `SECMGR` `A`, G3 | M |
| FR-9a | Auditing and authentication are backend concerns. The frontend sends the user's identity with every request and never caches account data across users. | `SECMGR` `V`/`L` | M |

## 4. Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR-1 | React + TypeScript, built with Vite. No runtime dependencies beyond React. |
| NFR-2 | All data goes through one typed API interface. A mock implementation backs local development; an HTTP implementation is selected with `VITE_API_BASE_URL`. |
| NFR-3 | Visual design follows the "Portfolio Desk" reference: IBM Plex Sans/Mono, tabular numbers, light and dark themes from `prefers-color-scheme`. |
| NFR-4 | Responsive down to 360px wide: KPIs reflow, the transactions table becomes stacked rows, positions default to cards. |
| NFR-5 | Accessible: tabs use `role="tab"`/`aria-selected`, sortable headers expose `aria-sort`, focus is visible, errors are announced with `role="alert"`. |
| NFR-6 | Money is shown in USD with 2 decimals (0 in KPIs), units with 3 decimals (`SHARE-QTY` 3 decimals), dates as `Mon D, YYYY`. Values are computed from raw numbers, never from formatted strings. |

## 5. API contract (for the backend phase)

The frontend depends only on this interface (`frontend/src/api/types.ts`):

| Call | Returns | Legacy equivalent |
| --- | --- | --- |
| `listAccounts()` | account summaries for the lookup suggestions | none |
| `getAccount(accountNo)` | account header, positions, transactions, value history | `INQPORT` + `INQHIST` |

Errors are typed: `NOT_FOUND`, `ACCESS_DENIED` and `SYSTEM` (with `code` and `traceId`), matching the `INQPORT`, `SECMGR` and `ERRHNDL` outcomes.

## 6. Traceability: legacy element to new UI

| Legacy | New UI |
| --- | --- |
| `MENMAP` option 1 | Positions tab |
| `MENMAP` option 2 | Transactions tab |
| `MENMAP` option 3 / `PF3` | Not needed (close tab) |
| `ACCTIN`, `HISAIN` | Single header lookup |
| `POSMAP` fields | Positions table columns |
| `HISMAP` rows 1-10 | Transactions table, 10 rows/page default |
| `PF7` / `PF8` | Pager buttons and `[` / `]` |
| `POSMSG`, `HISMSG`, `ERRMSG` | Inline lookup message |
| `ERRMAP` | Error panel with code, details, trace ID, Retry |
| `SECMGR` deny | Access denied panel |
