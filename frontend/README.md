# Investment Portfolio Management System — Frontend

Modern React SPA replacing the legacy COBOL/CICS/BMS terminal interface for the Investment Portfolio Management System.

## Quick Start

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Login with any username/password (mock auth).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | React 19 + TypeScript |
| Build | Vite 8 |
| Styling | Tailwind CSS v4 |
| Routing | React Router v7 |
| Charts | Recharts |
| Icons | Lucide React |

## Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start dev server (port 5173) |
| `npm run build` | Type-check + production build |
| `npm run lint` | ESLint check |
| `npm run preview` | Preview production build |

## Page Map: Legacy Screens → Modern UI

| Legacy Screen (BMS) | Modern Route | Description |
|---------------------|-------------|-------------|
| Login (SECMGR) | `/login` | Username/password authentication |
| MENMAP (Main Menu) | `/` | Dashboard with summary stats |
| POSMAP (Position Inquiry) | `/positions` | Account position lookup |
| HISMAP (Transaction History) | `/transactions` | Transaction history with filtering |
| PORTMSTR (Portfolio Mgmt) | `/portfolios` | Portfolio CRUD operations |
| PORTTRAN/PORTVALD | `/transactions/new` | Transaction entry form |
| RPTPOS00 | `/reports/positions` | Position valuation report |
| RPTAUD00 | `/reports/audit` | Audit trail report |
| RPTSTA00 | `/reports/statistics` | System statistics report |
| ERRMAP (Error Display) | `/errors` | Error log + global toast/banner |
| Batch Monitor | `/batch` | Batch job status |

## Legacy Key Mapping

| CICS Function Key | Modern UI Equivalent |
|-------------------|---------------------|
| PF3 (Exit) | Back button / navigation |
| PF7 (Previous) | Previous page button |
| PF8 (Next) | Next page button |
| Enter | Form submit |

## Project Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── layout/        # AppLayout, Header, Sidebar
│   │   └── ui/            # Reusable: DataTable, Card, StatusBadge, ErrorToast, ErrorBanner
│   ├── context/           # AuthContext, ErrorContext, PortfolioContext
│   ├── data/
│   │   ├── types.ts       # TypeScript interfaces (from COBOL copybooks)
│   │   └── mockData.ts    # Stub data (no backend yet)
│   ├── pages/
│   │   ├── login/         # Authentication
│   │   ├── dashboard/     # Main dashboard
│   │   ├── positions/     # Position inquiry
│   │   ├── portfolios/    # Portfolio CRUD (list, detail, new, edit)
│   │   ├── transactions/  # Transaction history + entry form
│   │   ├── reports/       # Position, Audit, Statistics reports
│   │   ├── batch/         # Batch job monitor
│   │   └── errors/        # Error log
│   ├── App.tsx            # Route definitions
│   └── main.tsx           # Entry point
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## Data Layer

All data is **mock/stub** — no real backend yet. Mock data lives in `frontend/src/data/mockData.ts` and models the COBOL VSAM/DB2 record structures:

- **Portfolios**: 12 records (PORT0001–PORT0012)
- **Positions**: 22 records across 11 accounts
- **Transactions**: 52 records spanning Jul–Aug 2024
- **Batch Jobs**: 8 records showing a typical nightly cycle
- **Audit Entries**: 16 records from batch processing
- **Error Entries**: 7 records demonstrating validation/system errors

## COBOL Type Mappings

| COBOL Record | TypeScript Interface | Source Copybook |
|-------------|---------------------|-----------------|
| PORT-RECORD | `Portfolio` | PORTFLIO.cpy |
| POSITION-RECORD | `Position` | POSREC.cpy |
| TRANSACTION-RECORD | `Transaction` | TRNREC.cpy |
| BATCH-CONTROL | `BatchJob` | — |
| AUDIT-RECORD | `AuditEntry` | AUDITLOG.cpy |
| ERR-MESSAGE | `ErrorEntry` / `AppError` | ERRHAND.cpy |
