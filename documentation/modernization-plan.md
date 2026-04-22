# Frontend Modernization — Implementation Plan

Frontend-only modernisation of the COBOL Investment Portfolio Management System. All screens currently rendered via BMS/3270 will be rebuilt as a React SPA backed by mock data. A real backend (API + database) is out of scope for this plan and will be addressed in a separate effort; the mock-data layer is designed so it can be swapped for live API calls later with minimal changes.

## Tech Stack

| Layer | Choice | Notes |
|-------|--------|-------|
| Framework | React 18 + TypeScript | Functional components, hooks only |
| Routing | React Router v6 | Matches BMS screen flow |
| State | Zustand | Lightweight, replaces COMMAREA/working-storage concepts |
| Styling | Tailwind CSS | Utility-first; design tokens for financial UI |
| Tables / Data | TanStack Table v8 | Sorting, pagination, filtering |
| Forms | React Hook Form + Zod | Mirrors COBOL field-level validation rules |
| Charts | Recharts | Portfolio value, allocation, trends |
| Mock Data | MSW (Mock Service Worker) | Intercepts fetch; same request shapes the real API will use |
| Testing | Vitest + React Testing Library + Playwright | Unit, component, E2E |
| Build | Vite | Fast dev/build cycle |

---

## Phase 1 — Project Setup & Core Shell

> **Goal:** Runnable app skeleton with routing, layout, mock data plumbing, and auth shell.

### Tasks

| # | Task | Deliverable |
|---|------|-------------|
| 1.1 | Scaffold Vite + React + TypeScript project | `npm run dev` serves blank page at `localhost:5173` |
| 1.2 | Install and configure Tailwind CSS, ESLint, Prettier | Lint/format scripts pass on empty project |
| 1.3 | Build app shell layout (header, sidebar nav, content area) | Persistent chrome visible on every route |
| 1.4 | Set up React Router with all route stubs | Every path in the route map below renders a placeholder |
| 1.5 | Create mock data layer with MSW | `GET /api/portfolios`, `GET /api/transactions`, etc. return seeded JSON derived from COBOL test data (see appendix) |
| 1.6 | Add Zustand stores for auth and global UI state | `useAuthStore` exposes `user`, `login()`, `logout()` |
| 1.7 | Build auth shell (login page, protected route wrapper) | Unauthenticated users redirected to `/login`; login with any mock credentials succeeds |
| 1.8 | Add Vitest + RTL config and first smoke tests | `npm test` green with shell-layout and route-guard tests |

### Routes (created in 1.4)

| Path | Component | COBOL Origin |
|------|-----------|--------------|
| `/login` | `LoginPage` | CICS sign-on |
| `/` | `DashboardPage` | MENMAP (main menu) |
| `/positions` | `PositionListPage` | POSMAP (portfolio position inquiry) |
| `/positions/:accountId` | `PositionDetailPage` | POSMAP detail view |
| `/transactions` | `TransactionListPage` | HISMAP (transaction history) |
| `/transactions/:accountId` | `TransactionDetailPage` | HISMAP filtered by account |
| `/portfolios` | `PortfolioListPage` | New — no direct COBOL equivalent |
| `/portfolios/:id` | `PortfolioDetailPage` | New — aggregated portfolio view |
| `/portfolios/:id/edit` | `PortfolioEditPage` | New — CRUD for portfolios |
| `/reports` | `ReportsPage` | RPTPOS00, RPTAUD00, RPTSTA00 |
| `/reports/positions` | `PositionReportPage` | RPTPOS00 |
| `/reports/audit` | `AuditReportPage` | RPTAUD00 |
| `/reports/statistics` | `StatisticsReportPage` | RPTSTA00 |
| `/admin/batch-jobs` | `BatchJobsPage` | BCHCTL/PRCSEQ status |
| `/admin/system` | `SystemMonitorPage` | UTLMON00 |
| `/error` | `ErrorPage` | ERRMAP |

### Done when

- [ ] `npm run dev` renders the shell with working sidebar navigation
- [ ] All routes resolve (placeholder content is fine)
- [ ] Mock API returns seeded data for portfolios, positions, and transactions
- [ ] Login flow gates protected routes
- [ ] `npm test` passes
- [ ] `npm run lint` clean

---

## Phase 2 — Inquiry Screens (MVP)

> **Goal:** Core read-only screens that replicate the CICS online inquiry experience (INQONLN, POSMAP, HISMAP, ERRMAP).

### Tasks

| # | Task | Deliverable |
|---|------|-------------|
| 2.1 | Build `DashboardPage` — summary cards (total AUM, position count, recent activity) | Dashboard renders live counts from mock data |
| 2.2 | Build `PositionListPage` — searchable, sortable TanStack Table | Table shows all positions; account search filters inline |
| 2.3 | Build `PositionDetailPage` — single account view with fund breakdown | Detail card: fund ID, CUSIP, share balance, cost basis, market value (mirrors POSMAP fields) |
| 2.4 | Build `TransactionListPage` — paginated history with date-range filter | Columns: date, type (BUY/SELL/FEE), units, price, amount (mirrors HISMAP columns) |
| 2.5 | Build `TransactionDetailPage` — account-scoped history | Same table filtered to one account, with before/after balance |
| 2.6 | Implement error-notification system (toast + `/error` page) | Errors surface via toast; `/error` shows code + description (mirrors ERRMAP) |
| 2.7 | Add keyboard shortcuts (PF-key equivalents) | `Ctrl+B` = back, `Ctrl+N` = next page, `Escape` = close modal — mirrors PF3/PF7/PF8 |
| 2.8 | Write Playwright E2E tests for inquiry flows | Login -> dashboard -> position search -> detail -> transaction history |

### Done when

- [ ] A user can log in, view the dashboard, search positions by account, drill into details, and browse transaction history
- [ ] Error states (invalid account, no data) display meaningful messages
- [ ] Keyboard navigation works for core flows
- [ ] E2E test suite passes

---

## Phase 3 — Portfolio Management & Reports

> **Goal:** Write-path screens (portfolio CRUD) and reporting dashboards.

### Tasks

| # | Task | Deliverable |
|---|------|-------------|
| 3.1 | Build `PortfolioListPage` — card grid of all portfolios | Each card shows portfolio ID, account, total value, status badge |
| 3.2 | Build `PortfolioDetailPage` — holdings table + allocation chart | Recharts pie for allocation; TanStack Table for holdings |
| 3.3 | Build `PortfolioEditPage` — create/edit form | React Hook Form + Zod validation matching COBOL field rules (9-digit account, 6-char fund ID, etc.) |
| 3.4 | Implement optimistic mutations in Zustand store | Create/update/delete reflect immediately; mock API confirms |
| 3.5 | Build `ReportsPage` — report index with cards linking to sub-reports | Three cards: Position, Audit, Statistics |
| 3.6 | Build `PositionReportPage` — portfolio valuation summary with export | Mirrors RPTPOS00 output; CSV/PDF download via client-side generation |
| 3.7 | Build `AuditReportPage` — security & process audit trail | Mirrors RPTAUD00; filterable by date range and event type |
| 3.8 | Build `StatisticsReportPage` — system performance charts | Recharts line/bar for processing stats, error rates, throughput (mirrors RPTSTA00) |
| 3.9 | Add data export (CSV, PDF) utility | Shared `useExport` hook reusable across all report pages |

### Done when

- [ ] Full portfolio CRUD works against mock data
- [ ] All three report pages render with realistic data and are exportable
- [ ] Charts render correctly and are responsive
- [ ] Form validation rejects invalid data with field-level error messages

---

## Phase 4 — System Admin & Integration Prep

> **Goal:** Admin screens, API contract finalisation, accessibility audit, production readiness.

### Tasks

| # | Task | Deliverable |
|---|------|-------------|
| 4.1 | Build `BatchJobsPage` — job status dashboard | Table of batch jobs with status (Waiting/Processing/Complete/Error), timestamps, record counts — maps to BCHCTL fields |
| 4.2 | Build `SystemMonitorPage` — resource utilisation gauges | CPU, memory, DB2 connections displayed as gauges/sparklines — maps to UTLMON00 metrics |
| 4.3 | Define OpenAPI spec for all mock endpoints | `openapi.yaml` with request/response schemas matching Zod types and COBOL record layouts |
| 4.4 | Replace MSW handlers with fetch wrappers pointing at OpenAPI contract | Single `apiClient.ts` module; toggle between mock and live via env var |
| 4.5 | Accessibility audit and remediation | WCAG 2.1 AA: aria labels, focus management, colour contrast, screen-reader tested |
| 4.6 | Performance pass — lazy routes, code splitting, image optimisation | Lighthouse performance score >= 90 |
| 4.7 | Final E2E suite — full regression across all pages | Playwright tests covering every route and major interaction |
| 4.8 | Documentation — update README, add developer onboarding guide | README covers setup, architecture, mock-to-live switch, and contribution |

### Done when

- [ ] Admin pages display realistic batch-job and system-monitor data
- [ ] `openapi.yaml` is complete and MSW handlers validate against it
- [ ] `apiClient.ts` can switch between mock and live backend with one env var change
- [ ] Lighthouse accessibility score >= 90
- [ ] Lighthouse performance score >= 90
- [ ] Full E2E suite green
- [ ] Documentation reviewed and merged

---

## Appendix — COBOL-to-UI Field Mapping

<details>
<summary>Expand reference mapping</summary>

### Position Inquiry (POSMAP -> PositionDetailPage)

| BMS Field | UI Field | Source |
|-----------|----------|--------|
| ACCTIN | Account Number input | POS-ACCOUNT-NO (9 digits) |
| FUNDOUT | Fund ID | POS-FUND-ID (6 chars) |
| NAMEOUT | Fund Name | Lookup from fund master |
| UNITOUT | Share Balance | POS-SHARE-BAL (S9(11)V999) |
| COSTOUT | Cost Basis | POS-COST-BASIS (S9(11)V99) |
| VALOUT | Market Value | Calculated: shares x current price |

### Transaction History (HISMAP -> TransactionListPage)

| BMS Column | UI Column | Source |
|------------|-----------|--------|
| Date | Date | HIST-TIMESTAMP |
| Type | Type | HIST-TRANS-TYPE (BY/SL/FE) |
| Units | Units | HIST-SHARE-QTY (S9(11)V999) |
| Price | Price | HIST-PRICE (9(5)V9999) |
| Amount | Amount | HIST-AMOUNT (S9(11)V99) |

### Error Display (ERRMAP -> ErrorPage)

| BMS Field | UI Field | Source |
|-----------|----------|--------|
| ERRCOUT | Error Code | HIST-RESULT-CODE / system error code |
| ERRDOUT | Error Detail | Error description text |

### Batch Control (BCHCTL -> BatchJobsPage)

| COBOL Field | UI Column | Source |
|-------------|-----------|--------|
| BCH-PROCESS-ID | Job Name | 8-char process identifier |
| BCH-STATUS | Status | W/P/C/E mapped to badge labels |
| BCH-START-TIME | Started | Timestamp |
| BCH-END-TIME | Ended | Timestamp |
| BCH-RECORD-COUNT | Records | 9-digit count |
| BCH-ERROR-COUNT | Errors | 9-digit count |
| BCH-RETURN-CODE | RC | 4-digit return code |

</details>
