# Portfolio Management System — React Frontend

A modern React/TypeScript web frontend that replicates the functionality of the COBOL Portfolio Management System from the COBOL Legacy Benchmark Suite. This is a **frontend-only** implementation using mock/stub data that mirrors the COBOL copybook record structures.

## Quick Start

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173 and log in with any non-empty User ID.

## Tech Stack

- **React 19** + **TypeScript 6** + **Vite 8**
- **React Router 7** — client-side routing
- **Tailwind CSS 4** — utility-first styling via Vite plugin
- **ESLint** — linting with React hooks + refresh rules

## Architecture Overview

```
frontend/src/
├── App.tsx                   # React Router configuration
├── main.tsx                  # Application entry point
├── index.css                 # Tailwind CSS import
├── context/
│   ├── authContextDef.ts     # Auth context + types (shared)
│   └── AuthContext.tsx        # AuthProvider component
├── hooks/
│   ├── useAuth.ts            # Auth hook
│   └── useToast.ts           # Toast notification hook
├── components/
│   ├── Layout.tsx             # App shell (sidebar nav + content area)
│   ├── ProtectedRoute.tsx     # Route guard for authenticated pages
│   ├── ErrorBoundary.tsx      # React error boundary
│   ├── ErrorDisplay.tsx       # Error code + details display (ERRMAP)
│   ├── InlineError.tsx        # Field-level validation errors
│   ├── Toast.tsx              # Dismissible toast notifications
│   ├── toastContextDef.ts     # Toast context definition
│   ├── PositionDetail.tsx     # Position data display card
│   ├── TransactionTable.tsx   # Paginated, sortable table
│   ├── PortfolioForm.tsx      # Shared create/edit form
│   └── DeleteConfirmation.tsx # Delete with reason code dialog
├── pages/
│   ├── LoginPage.tsx          # Login form (SECMGR)
│   ├── DashboardPage.tsx      # Main menu (MENMAP)
│   ├── PositionInquiryPage.tsx # Position inquiry (POSMAP/INQPORT)
│   ├── TransactionHistoryPage.tsx # Transaction history (HISMAP/INQHIST)
│   ├── PortfolioManagementPage.tsx # Portfolio list/search
│   ├── PortfolioCreatePage.tsx    # Create portfolio (PORTMSTR)
│   ├── PortfolioDetailPage.tsx    # View portfolio detail
│   ├── PortfolioEditPage.tsx      # Edit portfolio (PORTUPDT)
│   ├── TransactionEntryPage.tsx   # Transaction entry (PORTTRAN)
│   ├── ReportsPage.tsx            # Reports hub
│   ├── ValuationReportPage.tsx    # Valuation report (RPTPOS00)
│   ├── AuditReportPage.tsx        # Audit log (AUDITLOG)
│   └── SystemStatsPage.tsx        # System statistics (RPTSTA00)
├── types/
│   ├── index.ts               # Re-exports all types
│   ├── portfolio.ts           # Portfolio types (PORTFLIO.cpy)
│   ├── position.ts            # Position types (POSREC.cpy)
│   ├── transaction.ts         # Transaction types (TRNREC.cpy)
│   ├── audit.ts               # Audit types (AUDITLOG.cpy)
│   └── error.ts               # Error types (ERRHAND.cpy, INQCOM.cpy)
├── mocks/
│   └── mockData.ts            # Centralized mock data store
└── utils/
    └── validation.ts          # Validation functions (PORTVALD.cbl)
```

## Screen-to-Page Mapping

| COBOL Screen/Program | BMS Map | Frontend Page | Route |
|---|---|---|---|
| Main Menu | MENMAP (INQSET.bms:7-19) | `DashboardPage.tsx` | `/` |
| Security Check | SECMGR (INQONLN.cbl:139-169) | `LoginPage.tsx` | `/login` |
| Position Inquiry | POSMAP (INQSET.bms:23-49) | `PositionInquiryPage.tsx` | `/positions` |
| Transaction History | HISMAP (INQSET.bms:53-85) | `TransactionHistoryPage.tsx` | `/history` |
| Error Display | ERRMAP (INQSET.bms:89-101) | `ErrorDisplay.tsx` | (component) |
| Portfolio Master CRUD | PORTMSTR.cbl | `PortfolioManagementPage.tsx` | `/portfolios` |
| Portfolio Create | PORTMSTR.cbl:86-98 | `PortfolioCreatePage.tsx` | `/portfolios/new` |
| Portfolio Update | PORTUPDT.cbl:44-47 | `PortfolioEditPage.tsx` | `/portfolios/:id/edit` |
| Portfolio Delete | PORTDEL.cbl:168-182 | `DeleteConfirmation.tsx` | (dialog) |
| Transaction Entry | PORTTRAN.cbl:102-118 | `TransactionEntryPage.tsx` | `/transactions/new` |
| Valuation Report | RPTPOS00.cbl:133-141 | `ValuationReportPage.tsx` | `/reports/valuation` |
| Audit Report | AUDITLOG.cpy:14-17 | `AuditReportPage.tsx` | `/reports/audit` |
| System Statistics | RPTSTA00.cbl:169-170 | `SystemStatsPage.tsx` | `/reports/system` |

## COBOL Copybook → TypeScript Type Mapping

| Copybook | TypeScript Interface | Key Fields |
|---|---|---|
| `PORTFLIO.cpy` | `Portfolio` | portfolioId (8), accountNumber (10), clientName (30), clientType (I/C/T), status (A/C/S) |
| `POSREC.cpy` | `Position` | portfolioId (8), investmentId (10), quantity, costBasis, marketValue, status (A/C/P) |
| `TRNREC.cpy` | `Transaction` | date, portfolioId, type (BU/SL/TR/FE), quantity, price, amount, status (P/D/F/R) |
| `AUDITLOG.cpy` | `AuditRecord` | timestamp, type (TRAN/USER/SYST), action, status (SUCC/FAIL/WARN) |
| `ERRHAND.cpy` | `AppError` | code, category (VS/VL/PR/SY), severity, text |
| `INQCOM.cpy` | `InquiryCommArea` | function (MENU/INQP/INQH/EXIT), accountNumber (10) |

## Validation Rules (from PORTVALD.cbl)

| Rule | Function | COBOL Source |
|---|---|---|
| Portfolio ID format | `validatePortfolioId()` | Must start with 'PORT' + 4 numeric digits |
| Account Number | `validateAccountNumber()` | Must be 10 numeric digits, non-zero |
| Investment Type | `validateInvestmentType()` | Must be STK, BND, MMF, or ETF |
| Amount Range | `validateAmount()` | Within S9(13)V99 bounds |
| Client Name | `validateClientName()` | Cannot be blank |

## Mock Data

The mock data in `src/mocks/mockData.ts` includes:
- **8 portfolios** across individual, corporate, and trust types
- **22 positions** across 7 portfolios with realistic holdings
- **30+ transaction history entries** across 6 accounts
- **15 audit log entries** covering TRAN, USER, and SYST event types
- **Valuation report data** with % change calculations
- **System statistics** with DB2 and batch processing metrics

All mock data uses consistent account numbers, portfolio IDs, and investment IDs across views.

## Available Scripts

| Command | Description |
|---|---|
| `npm run dev` | Start dev server (http://localhost:5173) |
| `npm run build` | TypeScript check + production build |
| `npm run lint` | Run ESLint |
| `npm run preview` | Preview production build |
