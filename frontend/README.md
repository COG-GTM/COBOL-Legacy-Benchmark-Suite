# Inquiry Subsystem Frontend

A frontend-only React application that replicates the main-menu navigation of the
legacy CICS online inquiry subsystem from the COBOL Legacy Benchmark Suite. All
data is mocked/placeholder — there is no backend or data fetching.

## Legacy mapping

The navigation mirrors these legacy sources:

- `src/maps/INQSET.bms` — the BMS map defining the main menu (`MENMAP`) options:
  "1. Portfolio Position Inquiry", "2. Transaction History", "3. Exit".
- `src/programs/online/INQONLN.cbl` — the controller that routes on the 4-char
  function code (`EVALUATE WS-COMMAREA-FUNCTION`).
- `src/copybook/online/INQCOM.cpy` — the COMMAREA copybook that defines the
  function codes and session fields.

Function code → route mapping (`src/legacy/functionCodes.ts`):

| Function code | Destination                  | Route        |
| ------------- | ---------------------------- | ------------ |
| `MENU`        | Main menu                    | `/menu`      |
| `INQP`        | Portfolio Position Inquiry   | `/portfolio` |
| `INQH`        | Transaction History          | `/history`   |
| `EXIT`        | End session                  | `/exit`      |
| (other)       | Error routine (`WHEN OTHER`) | `/error`     |

## Tech stack

- Vite + React + TypeScript
- React Router v6
- ESLint + Prettier

## Getting started

```bash
npm install
npm run dev      # start the dev server (default: http://localhost:5173)
npm run build    # type-check and build for production
npm run lint     # run ESLint
npm run format   # format with Prettier
```

## Structure

```
src/
  components/   reusable UI (MenuOption, BackToMenu)
  context/      COMMAREA-modeled session context (sessionContext, SessionProvider)
  layout/       app shell / layout (AppShell)
  legacy/       ports of the COBOL data model (functionCodes, commarea)
  pages/        screens (MainMenu, Portfolio, History, SessionEnded, ErrorPage)
  routes/       router configuration
```
