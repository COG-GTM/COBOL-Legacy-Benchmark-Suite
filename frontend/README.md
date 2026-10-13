# Portfolio Desk (React frontend)

Modern replacement for the CICS `PINQ` portfolio inquiry screens (`MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP` in `src/maps/INQSET.bms`). Requirements and legacy traceability: [`documentation/modernization/frontend-functional-requirements.md`](../documentation/modernization/frontend-functional-requirements.md).

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
npm test           # vitest + Testing Library
npm run lint && npm run typecheck && npm run build
```

## Data

With no configuration the app uses an in-browser mock API (`src/api/mock.ts`) with fictional accounts, since the legacy repo has no account/position test data. Set `VITE_API_BASE_URL` to use the HTTP adapter (`src/api/http.ts`), which expects:

| Endpoint | Returns |
| --- | --- |
| `GET /accounts` | `AccountSummary[]` |
| `GET /accounts/{accountNo}` | `AccountDetail` — 404 not found, 403 access denied, other errors `{ code, message, traceId }` |

Types are in `src/api/types.ts`.

## Mock accounts

| Account | Scenario |
| --- | --- |
| 4471-0028-93 | Okafor Family Trust (active, pending and closed positions) |
| 4471-0055-12 | R. Lindqvist Rollover IRA |
| 4471-0090-47 | Delgado Joint Brokerage |
| 4471-0004-03 | Access denied (SECMGR deny path) |
| 4471-0005-00 | System error with code and trace ID (ERRHNDL path) |
| any other 10 digits | Account not found |
