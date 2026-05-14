# Portfolio Management System — Frontend

A modern React + TypeScript web application that modernizes the legacy COBOL/CICS Investment Portfolio Management System.

## Tech Stack

- **React 19** with TypeScript
- **Vite** for build tooling
- **Tailwind CSS v4** for styling
- **React Router v7** for client-side routing
- **Lucide React** for icons

## Getting Started

### Prerequisites

- Node.js 18+ and npm

### Install & Run

```bash
cd frontend
npm install
npm run dev
```

The app will be available at `http://localhost:5173`.

### Build for Production

```bash
npm run build
npm run preview
```

## Mock Credentials

| Username  | Password     | Role       |
|-----------|-------------|------------|
| admin     | admin123    | read-write |
| analyst   | analyst123  | read-only  |
| trader    | trader123   | read-write |

## Pages & Routes

| Route                   | Page                  | COBOL Source         |
|-------------------------|-----------------------|----------------------|
| `/login`                | Login                 | SECMGR               |
| `/`                     | Dashboard             | MENMAP (INQSET.bms)  |
| `/portfolios`           | Portfolio List        | PORTMSTR             |
| `/portfolios/new`       | Create Portfolio      | PORTMSTR             |
| `/portfolios/:id`       | Portfolio Detail      | PORTMSTR             |
| `/portfolios/:id/edit`  | Edit Portfolio        | PORTMSTR             |
| `/positions`            | Position Inquiry      | INQPORT / POSMAP     |
| `/history`              | Transaction History   | INQHIST / HISMAP     |
| `/transactions/new`     | Transaction Entry     | PORTTRAN             |
| `/reports`              | Reports Index         | —                    |
| `/reports/position`     | Position Report       | RPTPOS00             |
| `/reports/audit`        | Audit Report          | RPTAUD00             |
| `/reports/statistics`   | Statistics Report     | RPTSTA00             |
| `/error`                | Error Page            | ERRMAP (INQSET.bms)  |

## Project Structure

```
frontend/
├── src/
│   ├── components/       # Shared UI components (Layout, ErrorBoundary, Toast, etc.)
│   ├── context/          # React Context providers (Auth, Toast, Portfolio data)
│   ├── hooks/            # Custom hooks (usePortfolioForm)
│   ├── mocks/            # Static JSON mock data files
│   │   └── reports/      # Report-specific mock data
│   ├── pages/            # Page components matching routes
│   │   └── reports/      # Report page components
│   └── types/            # TypeScript interfaces/types
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## Mock Data

All backend interactions use static JSON mock data in `src/mocks/`. Data structures are derived from COBOL copybooks:

- `portfolios.json` — `PORTFLIO.cpy` (PORT-RECORD)
- `positions.json` — `POSREC.cpy` (POSITION-RECORD)
- `transactions.json` — `TRNREC.cpy` (TRANSACTION-RECORD)
- `users.json` — Mock user credentials with roles
- `reports/` — Mock report data for position, audit, and statistics reports

## Features

- Role-based access (read-only vs read-write)
- Full CRUD for portfolio management
- Position inquiry by account number
- Transaction history with date-range filtering
- Transaction entry with COBOL-equivalent validation rules
- Three report types (Position, Audit, Statistics)
- Error boundary and toast notifications
- Responsive layout with collapsible sidebar
- Protected routes with auth redirect
