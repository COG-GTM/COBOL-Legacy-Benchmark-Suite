# Portfolio Management System — Frontend

A modern React + TypeScript web UI that reproduces the legacy CICS main menu
navigation of the COBOL Legacy Benchmark Suite (CLBS).

The behavior mirrors the legacy front controller
[`src/programs/online/INQONLN.cbl`](../src/programs/online/INQONLN.cbl), which
routes on a 4-character function code, and the `MENMAP` menu screen defined in
[`src/maps/INQSET.bms`](../src/maps/INQSET.bms).

## Legacy mapping

The menu has exactly three options, faithful to the legacy `MENMAP` labels/order
and the `INQCOM-FUNCTION` codes in
[`src/copybook/online/INQCOM.cpy`](../src/copybook/online/INQCOM.cpy):

| Option | Label                      | Legacy code | Legacy handler | Route        |
| ------ | -------------------------- | ----------- | -------------- | ------------ |
| 1      | Portfolio Position Inquiry | `INQP`      | `INQPORT`      | `/portfolio` |
| 2      | Transaction History        | `INQH`      | `INQHIST`      | `/history`   |
| 3      | Exit                       | `EXIT`      | terminates     | `/exit`      |

The menu screen itself corresponds to the `MENU` function code. These codes live
in one place — [`src/routes/functionCodes.ts`](./src/routes/functionCodes.ts) —
so later phases can attach real portfolio/history data and authentication while
staying aligned with the legacy dispatch.

> This is a frontend-only build. Options 1 and 2 render placeholder screens
> ("backend pending"); Option 3 ends the session on the client (mirroring
> `SET SESSION-TERMINATED TO TRUE`). No backend calls are made.

## Tech stack

- [Vite](https://vite.dev/) + React 19 + TypeScript
- [React Router](https://reactrouter.com/) for client-side routing
- ESLint + Prettier for linting/formatting
- Vitest + React Testing Library for tests

## Getting started

Requires Node.js 20+.

```bash
cd frontend
npm install       # install dependencies
npm run dev       # start the dev server (http://localhost:5173)
```

## Available scripts

| Command                | Description                         |
| ---------------------- | ----------------------------------- |
| `npm run dev`          | Start the Vite dev server           |
| `npm run build`        | Type-check and build for production |
| `npm run preview`      | Preview the production build        |
| `npm run lint`         | Run ESLint                          |
| `npm run format`       | Format all files with Prettier      |
| `npm run format:check` | Check formatting without writing    |
| `npm test`             | Run the unit tests once (Vitest)    |
| `npm run test:watch`   | Run the unit tests in watch mode    |

## Project structure

```
frontend/
├── src/
│   ├── components/   # Layout shell, shared UI (BackToMenuLink)
│   ├── pages/        # MainMenu, PortfolioInquiry, TransactionHistory, SessionEnded
│   ├── routes/       # functionCodes.ts — legacy code ↔ route mapping
│   ├── test/         # Vitest setup
│   ├── App.tsx       # route table
│   └── main.tsx      # app entry (BrowserRouter)
└── ...
```
