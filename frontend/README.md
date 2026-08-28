# Portfolio Operations Console

Frontend-only modernization of the COBOL Investment Portfolio Management System for MBA-1433.
The app uses React 18, TypeScript, Vite, and a typed in-memory mock API. No backend or
external services are required.

## Run locally

```bash
cd frontend
npm install
npm run dev
```

Quality checks:

```bash
npm run lint
npm run build
npx vitest run
```

Use `admin` for an administrator session, or any other non-empty username for a read-only
session. Data is realistic but ephemeral and resets when the page is reloaded.
