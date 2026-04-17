# Frontend Modernization Plan

> **Scope**: React + TypeScript frontend against mock data. No backend services in this plan — API integration is prepared but deferred.

This plan replaces the COBOL-based CICS 3270 terminal screens (BMS maps in `src/maps/INQSET.bms`) and batch report outputs with a modern browser UI. Every COBOL program's user-facing surface is mapped to a React route; all data is served from a local mock layer until a real backend is built. The four phases below are ordered by dependency — each phase builds on the previous one's deliverables.

---

## Tech Stack

| Layer         | Choice                          | Why                                              |
| ------------- | ------------------------------- | ------------------------------------------------ |
| Framework     | React 18 + TypeScript           | Component model, type safety, ecosystem          |
| Build         | Vite                            | Fast HMR, native TS/TSX support                  |
| Routing       | React Router v6                 | Nested layouts, loader pattern                   |
| State         | Zustand                         | Lightweight, no boilerplate                      |
| Data fetching | TanStack Query + mock adapters  | Cache/retry built-in; swap adapters for real API |
| UI components | shadcn/ui (Radix + Tailwind)    | Accessible primitives, easy to theme             |
| Charts        | Recharts                        | Composable, React-native charting                |
| Tables        | TanStack Table                  | Headless, sortable, filterable, paginated        |
| Forms         | React Hook Form + Zod           | Validation mirrors COBOL field rules             |
| Testing       | Vitest + React Testing Library  | Fast unit/component tests                        |
| E2E           | Playwright                      | Cross-browser integration tests                  |

---

## Phase 1 — Project Setup & Core Shell

**Goal**: A running app with navigation, layout, auth shell, and a mock data layer that every later screen consumes.

| #   | Task                              | Deliverable                                                                                           |
| --- | --------------------------------- | ----------------------------------------------------------------------------------------------------- |
| 1.1 | Scaffold Vite + React + TS       | `npm create vite` with TS template; ESLint, Prettier, Tailwind configured; `npm run dev` works        |
| 1.2 | Configure path aliases & env      | `@/` alias in `tsconfig`; `.env` files for `VITE_API_BASE_URL`; env-specific build targets            |
| 1.3 | Build shell layout                | `<AppShell>` with sidebar nav, top bar (user menu, notifications bell), and `<Outlet>` content area   |
| 1.4 | Set up React Router               | Route tree with lazy-loaded pages; `ProtectedRoute` wrapper; 404 catch-all                           |
| 1.5 | Create mock data layer            | `/src/mocks/` with typed JSON fixtures mirroring VSAM/DB2 record layouts (see Reference appendix)     |
| 1.6 | Build data service adapters       | `PortfolioService`, `TransactionService`, `ReportService` — interfaces that return mock data now, swap to fetch later |
| 1.7 | Auth shell (no real provider yet) | `AuthContext` with `login()` / `logout()` stubs; `useAuth` hook; persisted token in `localStorage`    |
| 1.8 | Set up Vitest + RTL               | Config, first smoke test for `<AppShell>` renders without crash                                       |

**Done when**:
- [ ] `npm run dev` serves the app at `localhost:5173`
- [ ] Sidebar shows nav links for Dashboard, Positions, Transactions, Reports, System
- [ ] Clicking a nav link changes the URL and renders a placeholder page
- [ ] Mock services return typed data (`Position[]`, `Transaction[]`, etc.)
- [ ] Auth shell redirects unauthenticated users to `/login`
- [ ] `npm test` passes with at least one component test

### Routes created in this phase

| Route               | Page component       | COBOL origin              |
| ------------------- | -------------------- | ------------------------- |
| `/login`            | `LoginPage`          | SECMGR (auth gate)        |
| `/`                 | `DashboardPage`      | INQONLN main menu         |
| `/positions`        | `PositionsPage`      | INQPORT (placeholder)     |
| `/transactions`     | `TransactionsPage`   | INQHIST (placeholder)     |
| `/reports`          | `ReportsPage`        | RPTPOS00 etc (placeholder)|
| `/system`           | `SystemPage`         | UTLMON00 (placeholder)    |
| `*`                 | `NotFoundPage`       | —                         |

---

## Phase 2 — Inquiry Screens (MVP)

**Goal**: The core read-only screens that replace the CICS online inquiry programs (INQONLN, INQPORT, INQHIST).

**Depends on**: Phase 1 (shell, routing, mock layer, auth).

| #   | Task                                | Deliverable                                                                                              |
| --- | ----------------------------------- | -------------------------------------------------------------------------------------------------------- |
| 2.1 | Dashboard overview cards            | Summary cards: total portfolios, AUM, today's transactions, error count — data from mock services        |
| 2.2 | Position inquiry table              | `<PositionTable>` with sort, filter, pagination; columns mirror `POSREC` fields (account, fund, shares, cost basis, status) |
| 2.3 | Position detail drawer / page       | Click a row to see full position record + recent transaction list for that account+fund pair             |
| 2.4 | Transaction history table           | `<TransactionTable>` filterable by account, fund, date range, type (BUY/SELL/FEE); mirrors `HISTREC`    |
| 2.5 | Transaction detail view             | Expand a row to see before/after balances, result code, timestamps                                       |
| 2.6 | Global search                       | Search bar in top nav — searches positions by account number or fund ID (mirrors INQPORT account lookup) |
| 2.7 | Error / notification toasts         | Toast system for validation errors and warnings (maps to COBOL error codes E001–E004, W001–W002)        |
| 2.8 | Write unit + integration tests      | Tests for table filtering, detail view rendering, search results                                         |

**Done when**:
- [ ] Dashboard shows live-looking summary cards fed from mock data
- [ ] Positions table loads, sorts by any column, filters by status (Active/Closed)
- [ ] Clicking a position row shows its detail with related transactions
- [ ] Transaction history filters by date range and transaction type
- [ ] Search returns matching positions by account number
- [ ] Error toasts display when mock service simulates an error scenario
- [ ] All new components have at least one test; `npm test` passes

### Routes created in this phase

| Route                          | Page component          | COBOL origin         |
| ------------------------------ | ----------------------- | -------------------- |
| `/positions/:accountNo/:fundId` | `PositionDetailPage`   | INQPORT detail view  |
| `/transactions/:transId`        | `TransactionDetailPage`| INQHIST detail view  |

---

## Phase 3 — Portfolio Management & Reports

**Goal**: CRUD screens for portfolio management and the reporting dashboard that replaces RPTPOS00, RPTAUD00, and RPTSTA00.

**Depends on**: Phase 2 (inquiry screens, data service pattern established).

| #   | Task                                | Deliverable                                                                                              |
| --- | ----------------------------------- | -------------------------------------------------------------------------------------------------------- |
| 3.1 | Portfolio CRUD forms                | Create / Edit / Close portfolio forms using React Hook Form + Zod; validation rules mirror COBOL field checks (account 9-digit numeric, fund ID 6-char alphanumeric, etc.) |
| 3.2 | Transaction submission form         | New transaction form (Buy/Sell/Fee) with validation matching TRNVAL00 rules (no future dates, non-zero qty, positive price) |
| 3.3 | Reports dashboard                   | `/reports` page with tab navigation: Position Report, Audit Report, System Statistics                    |
| 3.4 | Position report view                | Table + summary matching RPTPOS00 output: portfolio valuations, transaction summaries, reconciliation    |
| 3.5 | Audit report view                   | Filterable log matching RPTAUD00: security events, process execution, exceptions                         |
| 3.6 | System statistics charts            | Recharts visualizations matching RPTSTA00: processing stats, performance metrics, resource utilization   |
| 3.7 | CSV / PDF export                    | Export buttons on report views; CSV via client-side generation, PDF via `@react-pdf/renderer` or similar |
| 3.8 | Tests for forms + reports           | Validation edge cases, report rendering, export triggers                                                 |

**Done when**:
- [ ] Portfolio create form validates all COBOL-equivalent field rules and submits to mock service
- [ ] Transaction form enforces TRNVAL00 validation (rejects future dates, zero quantities, etc.)
- [ ] Reports page shows three report tabs with realistic mock data
- [ ] Position report displays portfolio valuations with totals
- [ ] Audit report filters by date range, program ID, error code
- [ ] At least one chart renders system statistics (e.g., transaction volume over time)
- [ ] CSV export downloads a file with correct headers and data
- [ ] `npm test` passes; form validation tests cover edge cases

### Routes created in this phase

| Route                  | Page component             | COBOL origin          |
| ---------------------- | -------------------------- | --------------------- |
| `/portfolios/new`      | `PortfolioCreatePage`      | PORTMSTR add record   |
| `/portfolios/:id/edit` | `PortfolioEditPage`        | PORTMSTR update record|
| `/transactions/new`    | `TransactionCreatePage`    | PORTTRAN submission   |
| `/reports/positions`   | `PositionReportPage`       | RPTPOS00              |
| `/reports/audit`       | `AuditReportPage`          | RPTAUD00              |
| `/reports/statistics`  | `StatisticsReportPage`     | RPTSTA00              |

---

## Phase 4 — System Admin & Integration Prep

**Goal**: System administration screens, API contract finalization, and production readiness.

**Depends on**: Phase 3 (all CRUD and report screens complete).

| #   | Task                                 | Deliverable                                                                                             |
| --- | ------------------------------------ | ------------------------------------------------------------------------------------------------------- |
| 4.1 | Batch job status dashboard           | `/system/jobs` page showing mock batch job statuses (maps to BCHCTL records: waiting/processing/complete/error) |
| 4.2 | System monitor dashboard             | `/system/monitor` with resource utilization gauges, alert log, threshold indicators (maps to UTLMON00)  |
| 4.3 | File maintenance status view         | `/system/maintenance` showing archive status, cleanup schedules, VSAM reorg status (maps to UTLMNT00)  |
| 4.4 | Define OpenAPI / API contract        | `/docs/api-contract.yaml` specifying every endpoint the frontend expects; types generated from contract |
| 4.5 | Swap mock adapters → fetch adapters  | Implement real `fetch`-based service adapters behind the same interfaces; toggle via env var             |
| 4.6 | Accessibility audit + fixes          | Run axe-core; fix all critical/serious violations; ensure keyboard navigation works on every screen      |
| 4.7 | E2E test suite (Playwright)          | Golden-path E2E: login → dashboard → position inquiry → transaction history → create portfolio → export report |
| 4.8 | Performance baseline                 | Lighthouse CI run; bundle analysis; lazy-load any routes > 50 KB                                        |

**Done when**:
- [ ] Batch job dashboard shows status cards with progress indicators for each COBOL batch program
- [ ] System monitor displays at least 3 gauges (CPU, memory, DASD analog) with mock threshold alerts
- [ ] API contract YAML covers all service endpoints with request/response schemas
- [ ] Setting `VITE_USE_MOCK=false` + `VITE_API_BASE_URL=http://...` switches to real API calls (404s expected without a backend)
- [ ] Zero critical or serious axe-core accessibility violations
- [ ] Playwright E2E suite passes end-to-end on the mock data layer
- [ ] Lighthouse performance score >= 90 on the dashboard page
- [ ] `npm run build` produces a production bundle under 500 KB gzipped

### Routes created in this phase

| Route                  | Page component              | COBOL origin          |
| ---------------------- | --------------------------- | --------------------- |
| `/system/jobs`         | `BatchJobStatusPage`        | BCHCTL00 status view  |
| `/system/monitor`      | `SystemMonitorPage`         | UTLMON00              |
| `/system/maintenance`  | `MaintenanceStatusPage`     | UTLMNT00              |

---

## Reference — COBOL-to-UI Mapping

<details>
<summary>Expand: COBOL program → frontend surface mapping</summary>

| COBOL Program | Type    | Frontend Replacement              | Phase |
| ------------- | ------- | --------------------------------- | ----- |
| INQONLN       | Online  | App shell + dashboard             | 1, 2  |
| INQPORT       | Online  | Position inquiry table + detail   | 2     |
| INQHIST       | Online  | Transaction history table + detail| 2     |
| SECMGR        | Online  | Auth shell + login page           | 1     |
| CURSMGR        | Online  | React Router navigation           | 1     |
| ERRHNDL       | Online  | Toast notification system         | 2     |
| PORTMSTR      | Batch   | Portfolio CRUD forms              | 3     |
| TRNVAL00      | Batch   | Transaction form validation       | 3     |
| POSUPD00      | Batch   | Position update (via API, Phase 4)| 3, 4  |
| RPTPOS00      | Batch   | Position report view + export     | 3     |
| RPTAUD00      | Batch   | Audit report view + export        | 3     |
| RPTSTA00      | Batch   | System statistics charts          | 3     |
| BCHCTL00      | Utility | Batch job status dashboard        | 4     |
| UTLMON00      | Utility | System monitor dashboard          | 4     |
| UTLMNT00      | Utility | Maintenance status view           | 4     |
| HISTLD00      | Batch   | (Backend-only — no direct UI)     | —     |
| DB2CONN/CMT   | Infra   | (Backend-only — no direct UI)     | —     |
| TSTGEN00      | Test    | (Dev tooling — no production UI)  | —     |
| TSTVAL00      | Test    | (Dev tooling — no production UI)  | —     |

</details>
