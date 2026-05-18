# Portfolio Management System — Frontend

Modern web frontend for the COBOL-Legacy-Benchmark-Suite Investment Portfolio Management System. Built with React + TypeScript + Vite + TailwindCSS.

## Getting Started

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

## Project Structure

```
frontend/
├── src/
│   ├── components/shared/   # Reusable UI components
│   │   ├── DataTable.tsx     # Sortable, filterable, paginated table
│   │   ├── ErrorBanner.tsx   # Error/warning display (matches ERRMAP)
│   │   ├── LoadingSpinner.tsx
│   │   ├── NavigationBar.tsx # Top navigation bar
│   │   ├── PageLayout.tsx    # Standard page wrapper
│   │   └── ProtectedRoute.tsx
│   ├── context/
│   │   └── AuthContext.tsx   # Authentication state (replaces SECMGR)
│   ├── hooks/
│   │   ├── usePagination.ts  # Client-side pagination
│   │   └── useValidation.ts  # Form validation state management
│   ├── mock-data/            # Static JSON fixtures
│   │   ├── batch-status.json
│   │   ├── history.json
│   │   ├── portfolios.json
│   │   ├── positions.json
│   │   └── reports.json
│   ├── pages/                # Route-level page components
│   │   ├── Login.tsx
│   │   ├── Dashboard.tsx
│   │   ├── PortfolioInquiry.tsx
│   │   ├── TransactionHistory.tsx
│   │   ├── PortfolioManagement.tsx
│   │   ├── PortfolioForm.tsx
│   │   ├── TransactionEntry.tsx
│   │   ├── Reports.tsx
│   │   ├── PositionReport.tsx
│   │   ├── AuditReport.tsx
│   │   ├── StatisticsReport.tsx
│   │   └── BatchStatus.tsx
│   ├── services/
│   │   └── api.ts            # Mock API service layer
│   ├── types/
│   │   └── index.ts          # TypeScript interfaces from COBOL copybooks
│   ├── utils/
│   │   ├── cn.ts             # Tailwind class merging utility
│   │   ├── format.ts         # Currency, shares, date formatting
│   │   └── validation.ts     # Business rule validation
│   ├── App.tsx               # Router configuration
│   ├── main.tsx              # Entry point
│   └── index.css             # Tailwind + theme variables
├── index.html
├── package.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
└── vite.config.ts
```

## COBOL-to-Frontend Mapping

| COBOL Component | Frontend Equivalent |
|---|---|
| MENMAP (main menu) | Dashboard + NavigationBar |
| POSMAP (position inquiry) | PortfolioInquiry page |
| HISMAP (history inquiry) | TransactionHistory page |
| ERRMAP (error display) | ErrorBanner component |
| PORTMSTR (portfolio CRUD) | PortfolioManagement + PortfolioForm |
| PORTTRAN/PORTVALD (transactions) | TransactionEntry page |
| RPTPOS00/RPTAUD00/RPTSTA00 | Reports pages |
| BCHCTL00/UTLMON00 (batch monitor) | BatchStatus page |
| SECMGR (security manager) | AuthContext + Login page |

## TypeScript Types

All types in `src/types/index.ts` mirror COBOL record layouts:

- **PositionRecord** — from `POSITION-RECORD` (POSMSTRE VSAM)
- **HistoryRecord** — from `HISTORY-RECORD` (TRANHIST VSAM)
- **BatchControlRecord** — from `BATCH-CONTROL-RECORD` (BCHCTL VSAM)
- **Portfolio** — from `PORTFOLIO-RECORD` (PORTMSTR.cbl)
- **ErrorCode** — from data-dictionary.md error code table

## API Contract

The service layer in `src/services/api.ts` defines the API contract. Currently backed by mock data. To integrate with a real backend, replace the mock implementations while keeping the same interfaces:

### Position Service
- `getByAccount(accountNo: string): Promise<PositionRecord[]>`
- `getAll(): Promise<PositionRecord[]>`

### History Service
- `getByAccount(accountNo, options?): Promise<PaginatedResult<HistoryRecord>>`

### Portfolio Service
- `getAll(): Promise<Portfolio[]>`
- `getById(portfolioId): Promise<Portfolio | undefined>`
- `create(portfolio): Promise<Portfolio>`
- `update(portfolio): Promise<Portfolio>`
- `remove(portfolioId): Promise<void>`

### Batch Service
- `getAll(): Promise<BatchControlRecord[]>`
- `getByDate(processDate): Promise<BatchControlRecord[]>`

### Report Service
- `getPositionReport(): Promise<PositionReportEntry[]>`
- `getAuditReport(): Promise<AuditReportEntry[]>`
- `getStatisticsReport(): Promise<StatisticsReport>`

## Validation Rules

Client-side validation in `src/utils/validation.ts` mirrors legacy business rules:

- Account Number: 9-digit numeric (100000000–999999999)
- Fund ID: 1-6 alphanumeric characters
- Portfolio ID: `PORT` + 5 numeric digits
- Portfolio Name: required, non-blank
- Portfolio Status: A (Active), I (Inactive), C (Closed)
- Share Quantity: non-zero for Buy/Sell
- Price: > 0 for Buy/Sell
- Amount: non-zero for Fee
- Transaction Date: not in the future
- Transaction Type: BY (Buy), SL (Sell), FE (Fee)

## Authentication

Mock authentication accepts any credentials. Use "admin" as username for Admin role, any other username for Update role. Three access levels:
- **Read** — inquiry access only
- **Update** — read + transaction processing
- **Admin** — full access including management
