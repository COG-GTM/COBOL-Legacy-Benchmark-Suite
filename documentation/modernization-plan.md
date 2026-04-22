# Frontend Modernization Plan

> **Scope**: Frontend-only React/TypeScript SPA backed by mock data. No backend services are built in this plan — API integration is prepared but deferred to a separate backend effort.

The COBOL Legacy Benchmark Suite (CLBS) Investment Portfolio Management System currently runs on z/OS with CICS BMS screens, VSAM files, and DB2. This plan turns those green-screen workflows into a modern browser UI in four phases, each with ordered tasks, concrete deliverables, and a clear "done" gate.

---

## Tech Stack

| Layer        | Choice                          | Notes                                        |
| ------------ | ------------------------------- | -------------------------------------------- |
| Framework    | React 18 + TypeScript 5         | Vite for build tooling                       |
| Routing      | React Router v6                 | Mirrors CICS screen navigation               |
| State        | Zustand                         | Lightweight, replaces COMMAREA-style state    |
| Data         | TanStack Query + MSW            | Mock Service Worker for mock API layer        |
| UI           | shadcn/ui + Tailwind CSS        | Accessible component primitives               |
| Tables       | TanStack Table                  | Sorting, filtering, pagination for grids      |
| Charts       | Recharts                        | Portfolio valuation and trend visualisations   |
| Forms        | React Hook Form + Zod           | Validation mirrors COBOL field-level rules    |
| Testing      | Vitest + Playwright             | Unit + E2E                                    |

---

## Phase 1 — Project Setup & Core Shell

**Goal**: Scaffold the app, establish routing, build the application shell, wire up a mock data layer, and add an auth placeholder.

### Tasks

| #   | Task                                  | Deliverable                                                                                           |
| --- | ------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| 1.1 | Scaffold Vite + React + TS project   | `npm create vite` with ESLint, Prettier, Tailwind, and path aliases configured. Project compiles.     |
| 1.2 | Install core dependencies             | All Tech Stack packages added. `package.json` locked.                                                 |
| 1.3 | Build application shell layout        | Persistent sidebar nav, top header bar, and `<Outlet />` content area. Responsive down to 768 px.    |
| 1.4 | Configure routing                     | All route stubs wired (see Route Map below). Each renders a placeholder page component.               |
| 1.5 | Create mock data layer (MSW)          | MSW handlers serving JSON fixtures for positions, transactions, portfolios, and system status.         |
| 1.6 | Add auth shell                        | `AuthContext` with hardcoded mock user. `<ProtectedRoute>` wrapper. Login page UI (no real auth).     |
| 1.7 | Set up Vitest + first smoke tests     | Vitest config, one test per route stub asserting it renders without crashing.                          |

### Route Map (created here, referenced in later phases)

| Route                          | Page Component       | COBOL Origin           | Phase |
| ------------------------------ | -------------------- | ---------------------- | ----- |
| `/`                            | Dashboard            | INQONLN main menu      | 2     |
| `/positions`                   | PositionInquiry      | INQPORT                | 2     |
| `/positions/:accountNo`        | PositionDetail       | INQPORT detail screen  | 2     |
| `/transactions`                | TransactionHistory   | INQHIST                | 2     |
| `/portfolios`                  | PortfolioList        | POSMSTRE browse        | 3     |
| `/portfolios/:id`              | PortfolioDetail      | POSMSTRE detail        | 3     |
| `/portfolios/new`              | PortfolioCreate      | POSMSTRE add           | 3     |
| `/reports`                     | ReportsDashboard     | RPTPOS00 / RPTAUD00    | 3     |
| `/reports/positions`           | PositionReport       | RPTPOS00               | 3     |
| `/reports/audit`               | AuditReport          | RPTAUD00               | 3     |
| `/reports/statistics`          | StatisticsReport     | RPTSTA00               | 3     |
| `/admin/batch-jobs`            | BatchJobStatus       | BCHCTL00 / PRCSEQ00    | 4     |
| `/admin/system`                | SystemMonitor        | UTLMON00               | 4     |
| `/login`                       | Login                | (new)                  | 1     |

### Done when

- [ ] `npm run dev` serves the shell with working sidebar navigation to every route stub.
- [ ] MSW intercepts network requests in dev mode; no real HTTP calls leave the browser.
- [ ] Auth shell redirects unauthenticated users to `/login`.
- [ ] `npm test` passes all smoke tests.
- [ ] Lighthouse accessibility score >= 90 on the shell layout.

---

## Phase 2 — Inquiry Screens (MVP)

**Goal**: Build the core read-only screens that replicate CICS online inquiry (INQONLN, INQPORT, INQHIST) and add an operational dashboard.

### Tasks

| #   | Task                                    | Deliverable                                                                                          |
| --- | --------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| 2.1 | Build Dashboard page                    | Card grid showing portfolio count, total market value, today's transactions, and error count.         |
| 2.2 | Build PositionInquiry page              | Searchable, sortable table of positions (account no, fund ID, share balance, cost basis, status).     |
| 2.3 | Build PositionDetail page               | Detail view for a single account+fund: holdings summary, cost basis breakdown, last transaction info. |
| 2.4 | Build TransactionHistory page           | Filterable table (date range, type, account) with columns from TRANHIST: timestamp, type, qty, price, amount, result code. |
| 2.5 | Add error notification system           | Toast + notification drawer for validation errors. Maps to COBOL error codes (E001-E004, W001-W002). |
| 2.6 | Wire pages to mock data via TanStack Query | All pages fetch from MSW handlers. Loading/error/empty states handled.                             |
| 2.7 | Write unit + integration tests          | Vitest tests for each page component. At least one Playwright E2E test for the position inquiry flow.|

**Depends on**: Phase 1 complete.

### Done when

- [ ] Dashboard renders real-looking data from mock fixtures.
- [ ] User can search positions by account number, click into detail, and navigate back.
- [ ] Transaction history filters by date range and transaction type (BUY/SELL/FEE).
- [ ] Error toasts appear for invalid searches (e.g., non-numeric account number).
- [ ] All unit tests and the E2E test pass.

---

## Phase 3 — Portfolio Management & Reports

**Goal**: Add write-path UI for portfolio CRUD (mirroring PORTMSTR operations) and build the reporting dashboard with charts and exports.

### Tasks

| #   | Task                                     | Deliverable                                                                                         |
| --- | ---------------------------------------- | --------------------------------------------------------------------------------------------------- |
| 3.1 | Build PortfolioList page                 | Table of portfolios with status badges (Active/Closed). Supports pagination.                        |
| 3.2 | Build PortfolioDetail page               | Read view of a single portfolio: positions list, summary stats, last maintenance timestamp.          |
| 3.3 | Build PortfolioCreate / Edit forms       | React Hook Form + Zod. Validates portfolio ID (8 chars), account number (9 digits), investment type. Mirrors COBOL field rules from data dictionary. |
| 3.4 | Implement optimistic mutations           | Zustand store + TanStack Query mutations for add/update/delete. MSW handlers return success/error.   |
| 3.5 | Build ReportsDashboard page              | Card links to each report type. Shows last-generated timestamp per report.                          |
| 3.6 | Build PositionReport page                | Table + Recharts bar chart of portfolio valuations. Data sourced from mock RPTPOS00 output.          |
| 3.7 | Build AuditReport page                   | Filterable log table: timestamp, program ID, error code, account, description. Maps to ERRLOG table. |
| 3.8 | Build StatisticsReport page              | Recharts line/area charts for processing stats, error rates, resource utilisation over time.         |
| 3.9 | Add CSV / PDF export                     | "Export" button on each report page. CSV via client-side generation; PDF via `@react-pdf/renderer`.  |
| 3.10| Write tests for forms and reports        | Vitest tests for form validation (valid + invalid). Playwright E2E for create-portfolio flow.        |

**Depends on**: Phase 2 complete.

### Done when

- [ ] User can create a new portfolio, see it in the list, open it, edit it, and delete it — all against mock data.
- [ ] Form validation rejects invalid portfolio IDs, account numbers, and amounts (matching COBOL rules).
- [ ] All three report pages render charts and tables with mock data.
- [ ] CSV export downloads a valid file for at least the Position Report.
- [ ] All tests pass.

---

## Phase 4 — System Admin & Integration Prep

**Goal**: Build admin screens (batch job status, system monitor), finalise the API contract so a real backend can slot in, and run an accessibility audit.

### Tasks

| #   | Task                                     | Deliverable                                                                                         |
| --- | ---------------------------------------- | --------------------------------------------------------------------------------------------------- |
| 4.1 | Build BatchJobStatus page                | Table of batch jobs: process ID, status (Waiting/In-Process/Complete/Error), start/end time, record count, return code. Mirrors BCHCTL record layout. |
| 4.2 | Build SystemMonitor page                 | Live-updating cards for CPU, memory, DASD, DB2 stats (mock values via MSW + polling). Threshold alerts highlighted. |
| 4.3 | Add job dependency visualisation         | Mermaid or React Flow diagram showing TRNVAL00 → POSUPD00 → HISTLD00 → RPTGEN00 chain with live status colours. |
| 4.4 | Finalise OpenAPI contract                | `openapi.yaml` spec covering every MSW endpoint. Types auto-generated into `src/types/api.ts`.       |
| 4.5 | Add feature flag for mock vs. real API   | Environment variable `VITE_USE_MOCK_API`. When false, MSW is not loaded and TanStack Query hits real URLs. |
| 4.6 | Accessibility audit + fixes              | Run axe-core + Lighthouse. Fix all critical/serious issues. Document remaining minor issues.         |
| 4.7 | Write final E2E test suite               | Playwright tests covering: login → dashboard → position inquiry → create portfolio → view report → batch status. |

**Depends on**: Phase 3 complete.

### Done when

- [ ] Batch job status page shows mock job data with correct status badges and return code colouring.
- [ ] System monitor page auto-refreshes and highlights values exceeding thresholds.
- [ ] `openapi.yaml` is committed and types compile without errors.
- [ ] App runs cleanly with `VITE_USE_MOCK_API=false` (requests go to real URLs, just 404 — no runtime errors).
- [ ] Lighthouse accessibility score >= 95 on all pages.
- [ ] Full E2E suite passes.

---

## Appendix: COBOL → UI Mapping Reference

<details>
<summary>Expand COBOL screen / program mapping</summary>

| COBOL Component | Type         | Modern UI Equivalent              | Key Data                                      |
| --------------- | ------------ | --------------------------------- | --------------------------------------------- |
| INQONLN         | CICS program | App shell + Dashboard             | Main menu navigation, session state            |
| INQPORT         | CICS program | PositionInquiry / PositionDetail  | POSMSTRE fields: account, fund, shares, cost   |
| INQHIST         | CICS program | TransactionHistory                | TRANHIST fields: timestamp, type, qty, amount  |
| PORTMSTR        | Batch        | PortfolioList / Create / Detail   | VSAM CRUD: portfolio ID, account, status       |
| RPTPOS00        | Batch        | PositionReport                    | Portfolio valuations, transaction summaries     |
| RPTAUD00        | Batch        | AuditReport                       | ERRLOG table: timestamp, program, error code   |
| RPTSTA00        | Batch        | StatisticsReport                  | Processing metrics, resource utilisation        |
| BCHCTL00        | Batch        | BatchJobStatus                    | BCHCTL record: process ID, status, return code |
| UTLMON00        | Utility      | SystemMonitor                     | CPU, memory, DASD, DB2 stats, thresholds       |
| INQSET.bms      | BMS map      | React component layouts           | Screen field positions → form/table columns    |
| COMMAREA         | Interface    | Zustand store / React context     | Session state passed between screens           |

</details>
