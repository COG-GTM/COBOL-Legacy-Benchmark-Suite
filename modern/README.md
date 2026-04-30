# CLBS Modern — Investment Portfolio Manager

Modernized version of the COBOL Legacy Benchmark Suite (CLBS) Investment Portfolio Management System, rebuilt with **Next.js 14**, **Prisma**, **PostgreSQL 16**, and **TypeScript**.

---

## Architecture Overview

### COBOL Program &rarr; TypeScript Module Mapping

| COBOL Program | TypeScript Module | Description |
|---|---|---|
| `PORTVALD.cbl` + `PORTVAL.cpy` | `src/services/portfolio/validation.ts` | Portfolio data validation (ID, account, type, amount) |
| `PORTTRAN.cbl` + `TRNREC.cpy` | `src/services/transactions/processor.ts` | Transaction processing (Buy, Sell, Transfer, Fee) |
| `PORTTRAN.cbl` (2100-VALIDATE) | `src/services/batch/transactionValidator.ts` | Batch transaction validation |
| `POSUPDT.cbl` + `POSREC.cpy` | `src/services/batch/positionUpdater.ts` | Batch position recalculation |
| `BCHCTL00.cbl` + `BCHCTL.cpy` | `src/app/api/batch/route.ts` | Batch job control and orchestration |
| `PORTMSTR.cbl` + `PORTFLIO.cpy` | `src/app/api/portfolios/route.ts` | Portfolio CRUD operations |
| `INQONLN.cbl` + `INQPORT.cbl` | `src/app/api/portfolios/[id]/route.ts` | Portfolio inquiry (detail view) |
| `HISTLD00.cbl` + `HISTREC.cpy` | `src/app/api/transactions/route.ts` | Transaction history |
| `AUDPROC.cbl` + `AUDITLOG.cpy` | Prisma `AuditLog` model | Audit trail |
| `TSTGEN00.cbl` / `TSTVAL00.cbl` | `src/__tests__/` + `e2e/` | Test data generation and validation |

### Data Model Mapping

| COBOL Copybook | Prisma Model | Database Table |
|---|---|---|
| `PORTFLIO.cpy` | `Portfolio` | `portfolios` |
| `POSREC.cpy` | `Position` | `positions` |
| `TRNREC.cpy` | `Transaction` | `transactions` |
| `BCHCTL.cpy` | `BatchJob` | `batch_jobs` |
| `AUDITLOG.cpy` | `AuditLog` | `audit_logs` |

### Directory Structure

```
modern/
├── prisma/
│   ├── schema.prisma          # Database schema (from COBOL copybooks)
│   └── seed.ts                # Test data seed (from TSTGEN00.cbl)
├── src/
│   ├── app/
│   │   ├── api/
│   │   │   ├── health/        # Health check endpoint
│   │   │   ├── portfolios/    # Portfolio CRUD (PORTMSTR/INQPORT)
│   │   │   ├── transactions/  # Transaction submit (PORTTRAN)
│   │   │   ├── positions/     # Position inquiry (INQONLN)
│   │   │   └── batch/         # Batch processing (BCHCTL00/POSUPDT)
│   │   ├── layout.tsx
│   │   └── page.tsx
│   ├── lib/
│   │   └── prisma.ts          # Prisma client singleton
│   ├── services/
│   │   ├── portfolio/
│   │   │   └── validation.ts  # PORTVALD validation rules
│   │   ├── transactions/
│   │   │   └── processor.ts   # PORTTRAN transaction engine
│   │   └── batch/
│   │       ├── transactionValidator.ts  # Batch validation
│   │       └── positionUpdater.ts       # POSUPDT position recalc
│   └── __tests__/
│       ├── services/           # Unit tests (Vitest)
│       └── integration/        # Integration tests (Vitest + Prisma)
├── e2e/                        # E2E tests (Playwright)
├── Dockerfile                  # Multi-stage production build
├── docker-compose.yml          # App + PostgreSQL 16
└── README.md
```

---

## Running Locally

### Prerequisites

- Node.js 20+
- PostgreSQL 16 (or use Docker)

### Option 1: Docker Compose (recommended)

```bash
cd modern
docker-compose up --build
```

This starts:
- **PostgreSQL 16** on port 5432
- **Prisma migrations** (auto-runs on startup)
- **Next.js app** on [http://localhost:3000](http://localhost:3000)

### Option 2: Local Development

```bash
cd modern

# Install dependencies
npm install

# Set up environment
cp .env.example .env   # or create .env with your DATABASE_URL
# DATABASE_URL="postgresql://clbs:clbs_dev_password@localhost:5432/clbs?schema=public"

# Generate Prisma client and run migrations
npx prisma generate
npx prisma migrate dev

# Seed test data
npm run db:seed

# Start dev server
npm run dev
```

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | PostgreSQL connection string | (required) |
| `NODE_ENV` | Environment mode | `development` |
| `PORT` | Server port | `3000` |

---

## Running Tests

### Unit Tests (Vitest)

```bash
# Run all unit tests
npm test

# Watch mode
npm run test:watch

# With coverage
npm run test:coverage
```

Unit tests cover:
- **`validation.test.ts`** — All PORTVALD.cbl validation rules (ID format, account numbers, investment types, amount ranges)
- **`processor.test.ts`** — Buy/Sell/Transfer/Fee transaction logic with balance and position verification
- **`transactionValidator.test.ts`** — Batch validation pass/fail scenarios
- **`positionUpdater.test.ts`** — Position recalculation, portfolio total updates

### Integration Tests

Require a running PostgreSQL instance with `DATABASE_URL` set:

```bash
npm run test:integration
```

Integration tests cover:
- Full CRUD lifecycle (create → transact → verify → history)
- Batch pipeline (validate → process → update positions)
- Error scenarios (duplicates, insufficient funds, invalid inputs)

### E2E Tests (Playwright)

Require the app and database to be running:

```bash
# Install Playwright browsers (first time only)
npx playwright install chromium

# Run E2E tests
npm run test:e2e

# Interactive UI mode
npm run test:e2e:ui
```

E2E tests cover the same scenarios as `TSTGEN00.cbl` and `TSTEXEC.cbl`:
- **`portfolio-flow.spec.ts`** — Dashboard navigation, portfolio search, position view, transaction submission
- **`batch-flow.spec.ts`** — Batch trigger, completion verification, report checks

---

## API Documentation

### Health Check

```
GET /api/health
```

Response: `{ "status": "healthy", "database": "connected" }`

### Portfolios

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/portfolios` | List portfolios (query: `?status=ACTIVE&search=PORT`) |
| `POST` | `/api/portfolios` | Create portfolio |
| `GET` | `/api/portfolios/:id` | Get portfolio detail with positions and transactions |
| `PUT` | `/api/portfolios/:id` | Update portfolio (clientName, clientType, status) |
| `DELETE` | `/api/portfolios/:id` | Close portfolio (soft delete) |

**Create Portfolio Request:**
```json
{
  "id": "PORT0001",
  "accountNo": "1234567890",
  "clientName": "John Smith",
  "clientType": "INDIVIDUAL"
}
```

**Validation Rules (from PORTVALD.cbl):**
- `id` — Must match format `PORTnnnn` (4-letter prefix + 4 digits)
- `accountNo` — Must be exactly 10 numeric digits, non-zero
- `clientType` — One of: `INDIVIDUAL`, `CORPORATE`, `TRUST`

### Transactions

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/transactions` | List transactions (query: `?portfolioId=PORT0001&status=DONE`) |
| `POST` | `/api/transactions` | Submit transaction |

**Submit Transaction Request:**
```json
{
  "portfolioId": "PORT0001",
  "investmentId": "AAPL",
  "type": "BUY",
  "quantity": 100,
  "price": 150.00,
  "amount": 15000.00,
  "currency": "USD"
}
```

**Transaction Types (from TRNREC.cpy):**
- `BUY` — Purchase units, adds to position quantity and cost basis
- `SELL` — Sell units, subtracts from position (checks sufficiency)
- `TRANSFER` — Move between portfolios (not yet implemented)
- `FEE` — Deduct fee from cost basis

### Positions

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/positions?portfolioId=PORT0001` | List positions for a portfolio |

### Batch Processing

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/batch` | List recent batch jobs |
| `POST` | `/api/batch` | Trigger batch run |

**Trigger Batch Request:**
```json
{
  "portfolioId": "PORT0001",
  "transactions": [...]
}
```

The batch pipeline:
1. Validates all transactions in the batch
2. Processes pending transactions for the portfolio
3. Recalculates position quantities and cost bases
4. Updates portfolio total value
5. Records batch job status (DONE/ERROR with return codes)

**Return Codes (from BCHCON.cpy):**
- `0` — Success
- `4` — Warning (prerequisites pending)
- `8` — Error (processing failures)
- `12` — Severe error
- `16` — Critical error
