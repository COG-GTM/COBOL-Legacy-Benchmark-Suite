# CLBS Portfolio Management — Web Frontend

Modernized web frontend for the COBOL Legacy Benchmark Suite Portfolio
Management System. This is the first slice of the frontend modernization epic
([JIRA MBA-1424](https://cog-gtm.atlassian.net/browse/MBA-1424)) and implements
**US-2: Portfolio Management (CRUD)**
([MBA-1426](https://cog-gtm.atlassian.net/browse/MBA-1426)).

The legacy green-screen flows (BMS maps in `../src/maps/INQSET.bms`) and the
COBOL programs (`PORTADD`, `PORTREAD`, `PORTUPDT`, `PORTDEL`, `PORTMSTR`) are
mirrored by a React + TypeScript app backed by mock JSON fixtures. The data
model is a direct translation of the `PORTFLIO.cpy` copybook.

## Tech stack

- React 18 + TypeScript
- Vite (dev server / build)
- React Router (client-side routing)
- Vitest + Testing Library (unit + integration tests)
- ESLint (lint)

## Getting started

```bash
cd frontend
npm install
npm run dev      # start the dev server at http://localhost:5173
```

## Scripts

| Command             | Description                              |
| ------------------- | ---------------------------------------- |
| `npm run dev`       | Start the Vite dev server                |
| `npm run build`     | Type-check and build for production       |
| `npm run preview`   | Preview the production build              |
| `npm run lint`      | Run ESLint                               |
| `npm run typecheck` | Type-check without emitting              |
| `npm test`          | Run the test suite once                  |
| `npm run test:watch`| Run tests in watch mode                  |

## Architecture

```
src/
  components/        Reusable UI (Layout, StatusBadge, ConfirmDialog)
  data/              Mock fixtures mirroring COBOL copybooks
  features/
    portfolios/      Portfolio list / detail / form pages + validation
  services/          Swappable service layer (API integration points)
  types/             Domain types translated from PORTFLIO.cpy
  utils/             COMP-3 decimal + COBOL date helpers
```

### API integration points

The UI talks to a `PortfolioService` interface
(`src/services/portfolioService.ts`). The current implementation
(`MockPortfolioService`) is backed by an in-memory fixture. When the backend is
ready, swap in a REST-backed implementation — the endpoints map 1:1 to the
COBOL programs:

| Operation | REST endpoint                  | COBOL program         |
| --------- | ------------------------------ | --------------------- |
| `list`    | `GET /api/portfolios`          | `PORTMSTR`            |
| `get`     | `GET /api/portfolios/:id`      | `PORTREAD`            |
| `create`  | `POST /api/portfolios`         | `PORTADD`             |
| `update`  | `PUT /api/portfolios/:id`      | `PORTUPDT`            |
| `remove`  | `DELETE /api/portfolios/:id`   | `PORTDEL`             |

### Monetary precision

`PORT-TOTAL-VALUE` / `PORT-CASH-BALANCE` are `S9(13)V99 COMP-3`. To preserve
packed-decimal precision (which exceeds the JS safe-integer range once scaled to
cents), monetary values are kept as decimal **strings** throughout the app and
formatted with string operations (see `src/utils/decimal.ts`).
