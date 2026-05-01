# Portfolio Management System

Full-stack Investment Portfolio Management application modernized from the **COBOL Legacy Benchmark Suite**. This system replicates all business logic from the original mainframe COBOL programs, including portfolio CRUD operations, transaction processing, batch job control, position management, and reporting.

## Architecture

| Layer    | Technology | COBOL Equivalent |
|----------|-----------|------------------|
| Frontend | React 18 + TypeScript + Vite + TailwindCSS | BMS Screen Maps (INQSET.bms) |
| Backend  | Express + TypeScript + Socket.IO | CICS Programs (INQONLN, SECMGR, ERRHNDL) |
| Database | PostgreSQL + Prisma ORM | DB2 + VSAM Files |
| Real-time | WebSockets (Socket.IO) | N/A (new capability) |

## COBOL Program Mapping

| COBOL Program | TypeScript Module | Description |
|---------------|-------------------|-------------|
| PORTADD.cbl | `portfolioService.createPortfolio()` | Create portfolio |
| PORTREAD.cbl | `portfolioService.getPortfolio()` | Read portfolio |
| PORTUPDT.cbl | `portfolioService.updatePortfolio()` | Update portfolio |
| PORTDEL.cbl | `portfolioService.deletePortfolio()` | Delete/close portfolio |
| PORTVALD.cbl | `portfolioService.validatePortfolio()` | Validate portfolio data |
| TRNVAL00.cbl | `transactionService.validateTransaction()` | Transaction validation |
| POSUPD00.cbl | `positionService.updatePositionFromTransaction()` | Position updates |
| BCHCTL00.cbl | `batchService.runBatchCycle()` | Batch control |
| HISTLD00.cbl | `batchService.runBatchCycle()` (step 3) | History loader |
| RPTPOS00.cbl | `reportService.getPositionReport()` | Position report |
| RPTAUD00.cbl | `reportService.getAuditReport()` | Audit report |
| RPTSTA00.cbl | `reportService.getStatisticsReport()` | Statistics report |
| UTLMNT00.cbl | `systemService.runMaintenance()` | File maintenance |
| UTLMON00.cbl | `systemService.getSystemHealth()` | System monitoring |
| SECMGR.cbl | `middleware/auth.ts` | Security manager (JWT) |
| ERRHNDL.cbl | `middleware/errorHandler.ts` | Error handler |
| INQONLN.cbl | React Router | Online inquiry controller |
| INQPORT.cbl | `positionService.getPositions()` | Position inquiry |
| INQHIST.cbl | `transactionService.getTransactionHistory()` | History inquiry |

## Quick Start

### Prerequisites
- Node.js 18+
- PostgreSQL 14+

### Setup

```bash
cd portfolio-management-app

# Install dependencies
npm install

# Set up environment
cp packages/backend/.env.example packages/backend/.env

# Generate Prisma client
npm run db:generate

# Run database migrations
npm run db:migrate

# Seed database with sample data
npm run db:seed

# Start development servers
npm run dev
```

### Docker Compose

```bash
cd portfolio-management-app
docker compose up
```

### Demo Accounts

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Admin |
| trader1 | trader123 | User |
| viewer1 | viewer123 | Viewer |

## API Endpoints

### Authentication
- `POST /api/auth/login` — Login
- `POST /api/auth/register` — Register

### Portfolios
- `POST /api/portfolios` — Create portfolio
- `GET /api/portfolios` — List portfolios
- `GET /api/portfolios/:id` — Get portfolio
- `PUT /api/portfolios/:id` — Update portfolio
- `DELETE /api/portfolios/:id` — Close portfolio
- `POST /api/portfolios/:id/validate` — Validate portfolio

### Positions
- `GET /api/portfolios/:id/positions` — Get positions
- `PUT /api/portfolios/:id/positions` — Update positions

### Transactions
- `POST /api/transactions` — Create transaction
- `GET /api/transactions/portfolio/:id` — Transaction history
- `GET /api/transactions/:id` — Get transaction

### Batch Processing
- `POST /api/batch/run` — Run batch cycle
- `GET /api/batch/status` — Get batch status

### Reports
- `GET /api/reports/positions` — Position report
- `GET /api/reports/audit` — Audit report
- `GET /api/reports/statistics` — Statistics report

### System
- `GET /api/system/health` — Health check
- `POST /api/system/validate` — Data validation
- `POST /api/system/maintenance` — Maintenance operations

### WebSocket Events
- `transaction:created` — New transaction
- `position:updated` — Position change
- `batch:progress` — Batch job progress
- `batch:completed` — Batch complete
- `batch:failed` — Batch failed
- `system:alert` — System alert

## Data Type Mapping

| COBOL | TypeScript | Example |
|-------|-----------|---------|
| PIC X(n) | string (max length n) | PIC X(8) → string, maxLength 8 |
| PIC 9(n) | number (integer) | PIC 9(4) → number |
| PIC S9(n)V9(m) COMP-3 | Decimal.js | PIC S9(13)V99 → Decimal |
| 88-level conditions | TypeScript enums | PORT-ACTIVE → PortfolioStatus.Active |
