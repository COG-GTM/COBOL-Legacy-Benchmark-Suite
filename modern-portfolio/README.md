# Modern Portfolio Management System

A full-stack React/TypeScript application modernized from the COBOL Legacy Benchmark Suite (Investment Portfolio Management System).

## Architecture

```
modern-portfolio/
├── frontend/          # React + TypeScript + Vite + TailwindCSS
├── backend/           # Node.js + Express + TypeScript + Prisma
├── database/          # PostgreSQL migrations (Prisma ORM)
├── shared/            # Shared TypeScript types/interfaces
└── docker-compose.yml # Local dev environment (PostgreSQL + Redis)
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, TypeScript, Vite, TailwindCSS, React Router, TanStack Query, React Hook Form, Recharts |
| Backend | Node.js, Express, TypeScript, Prisma ORM, JWT Auth, BullMQ |
| Database | PostgreSQL 15 |
| Queue | Redis 7 (for batch job processing) |
| DevOps | Docker Compose |

## COBOL-to-Modern Mapping

| COBOL Program | Modern Equivalent |
|---------------|-------------------|
| PORTMSTR.cbl (CRUD) | `/api/portfolios` REST endpoints |
| SECMGR.cbl | JWT auth middleware + `/api/auth` |
| INQPORT.cbl | Portfolio Inquiry page |
| INQHIST.cbl | Transaction History page |
| TRNVAL00.cbl | Transaction validation service |
| POSUPD00.cbl | Position update batch job |
| HISTLD00.cbl | History loading batch job |
| BCHCTL00.cbl | Job management API + Admin panel |
| RPTPOS00/RPTAUD00/RPTSTA00 | Reports dashboard with charts |
| AUDPROC.cbl | Audit middleware + audit_logs table |
| ERRPROC.cbl / ERRHNDL.cbl | Centralized error handler |
| TSTGEN00.cbl | Database seed script |
| INQSET.bms (BMS maps) | React pages (Dashboard, Inquiry, History) |

## Quick Start

### With Docker Compose

```bash
cd modern-portfolio
docker compose up -d
```

This starts PostgreSQL, Redis, backend (port 3001), and frontend (port 5173).

### Local Development

1. Start PostgreSQL and Redis (or use Docker):
   ```bash
   docker compose up postgres redis -d
   ```

2. Set up the database:
   ```bash
   cd database
   npm install
   cp ../.env.example ../.env
   npx prisma migrate dev --schema=prisma/schema.prisma
   npx prisma db seed
   ```

3. Start the backend:
   ```bash
   cd backend
   npm install
   npm run dev
   ```

4. Start the frontend:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

5. Open http://localhost:5173

### Test Credentials

| Username | Password | Role |
|----------|----------|------|
| admin | password123 | ADMIN |
| trader | password123 | UPDATE |
| viewer | password123 | READ |

## API Endpoints

### Auth
- `POST /api/auth/login` — Login
- `POST /api/auth/register` — Register
- `POST /api/auth/logout` — Logout
- `GET /api/auth/me` — Current user

### Portfolios
- `GET /api/portfolios` — List (with search, filters, pagination)
- `GET /api/portfolios/:id` — Details with positions
- `POST /api/portfolios` — Create (validates PORT + 5 digits format)
- `PUT /api/portfolios/:id` — Update (validates status transitions)
- `DELETE /api/portfolios/:id` — Delete (admin only)

### Positions
- `GET /api/positions/current` — Current positions view
- `GET /api/positions/portfolio/:id` — Portfolio positions
- `POST /api/positions/portfolio/:id` — Add/update position

### Transactions
- `GET /api/transactions` — List with filters
- `GET /api/transactions/portfolio/:id` — Portfolio transactions
- `GET /api/transactions/:id` — Transaction details
- `POST /api/transactions` — Submit transaction

### Reports
- `GET /api/reports/positions` — Position report
- `GET /api/reports/audit` — Audit report
- `GET /api/reports/statistics` — System statistics

### Batch Jobs
- `POST /api/jobs/process-transactions` — Run TRNVAL00 → POSUPD00 → HISTLD00 pipeline
- `POST /api/jobs/generate-reports` — Generate all reports
- `GET /api/jobs/status` — Job history

## Business Logic Preserved

1. **Portfolio ID validation**: `PORT` + 5 numeric digits (from PORTMSTR.cbl lines 142-147)
2. **Transaction types**: BUY, SELL, TRANSFER, FEE (from TRNREC copybook)
3. **Status transitions**: Portfolio (A→C/S), Position (A/C/P), Transaction (P→D/F/R)
4. **Batch pipeline**: Validate → Update Positions → Load History → Reports
5. **Audit trail**: All mutations logged with before/after JSONB images
6. **Security flow**: Authenticate → Authorize (role-based) → Log access
7. **Error handling**: Categorized (VS/VL/PR/SY) with severity levels (0/4/8/12/16)
