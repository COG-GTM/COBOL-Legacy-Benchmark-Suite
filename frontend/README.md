# Portfolio Inquiry frontend (React)

React 18 + TypeScript + Vite replacement for the CICS online inquiry (`PINQ` / mapset `INQSET`:
`MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP`, driven by `src/programs/online/*`). Behaviour follows the
phase-1 functional requirements (`documentation/modernization/frontend-functional-requirements.md`,
PR #302).

| Route | Legacy screen |
|---|---|
| `/` | S1 Main Menu (MENMAP) |
| `/positions`, `/positions/:accountNo?page=n` | S2 Position Inquiry (POSMAP / INQPORT) |
| `/history`, `/history/:accountNo?page=n` | S3 Transaction History (HISMAP / INQHIST) |
| `/error` | S4 System Error (ERRMAP / ERRHNDL) |
| `/signin`, `/access-denied`, `/signed-out` | 401, 403 / missing `INQONLN:READ`, after Exit |

## Install and run

Requires Node 20+.

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173, mock API on by default
```

`npm run build` writes a static bundle to `dist/`; `npm run preview` serves it on port 4173.

## Test

```bash
npm run lint         # ESLint (typescript-eslint, react-hooks, jsx-a11y)
npm run typecheck    # tsc, strict
npm run test         # Vitest + React Testing Library (unit/component, FR acceptance criteria)
npx playwright install chromium   # first time only
npm run test:e2e     # Playwright smoke test of the main flows + axe checks, desktop and 360px
```

## Mock API

With `VITE_API_MODE=mock` (the default in `.env`) an [MSW](https://mswjs.io) service worker answers
the REST contract in section 7 of the FR doc, seeded with the section 7.4 data
(`src/mocks/data/*.json`). Handlers are in `src/mocks/handlers.ts`; the same handlers back the
Vitest suite.

Trigger accounts for manual testing:

| Account / URL | Behaviour |
|---|---|
| `1000000001` | 2 positions (table), 12 transactions (2 pages) |
| `1000000002` | 1 position (summary card), 1 transaction |
| `1000000003` | 1 closed position, 1 transaction |
| `1000000004` | position 404 `POSITION_NOT_FOUND`; empty history |
| `1000000005` | 12 positions (position paging) |
| `0000000000` | 400 `INVALID_ACCOUNT_FORMAT` (also rejected client-side) |
| `9999999995` | 403 `ACCESS_DENIED` |
| `9999999996` | 503 `AUTH_CHECK_FAILED` (returns to menu) |
| `9999999997` | history 503 `DB_UNAVAILABLE` (Retry) |
| `9999999998` | 500 `POSITION_READ_ERROR` / `HISTORY_READ_ERROR` |
| `9999999999` | 500 `SYSTEM_ERROR`, `action: ABEND` (System Error screen) |
| `?mockUser=denied` | session without `INQONLN:READ` |
| `?mockUser=anon` | session 401 |

`VITE_MOCK_DELAY_MS` adds latency so loading states are visible.

## Switching to a real backend

The UI only talks to the `PortfolioApi` interface in `src/api/client.ts`, which issues plain
`fetch` calls against the section 7 contract. No component imports the mocks, so switching is
configuration only:

```bash
cp .env.example .env.local
# .env.local
VITE_API_MODE=real
VITE_API_BASE_URL=/api/v1
VITE_API_PROXY_TARGET=http://localhost:8080   # dev server proxies /api to the backend
VITE_AUTH_LOGIN_URL=https://sso.example.com/login     # optional, used by "Sign in"
VITE_AUTH_LOGOUT_URL=https://sso.example.com/logout   # optional, used by Exit
```

What the backend must provide:

- `GET /api/v1/session`, `GET /api/v1/accounts/{accountNo}/positions?page&pageSize`,
  `GET /api/v1/accounts/{accountNo}/transactions?page&pageSize` with the shapes in
  `src/api/types.ts`.
- Decimals as JSON strings (they are formatted without float conversion).
- Errors as the `ApiError` body with an `X-Trace-Id` header; `action` drives routing
  (`CONTINUE` inline, `RETURN` back to menu, `ABEND` System Error). 401 goes to sign-in, 403 to
  Access denied.
- Cookie/session auth: requests use `credentials: 'include'` and send `X-Request-Id`.

For a production build served from a different origin, set `VITE_API_BASE_URL` to the absolute API
URL and enable CORS with credentials on the API.

## Layout

```
src/
  api/        types, typed fetch client, error catalogue (section 7.3)
  components/ layout, message region, pager, account form, trace ID, ...
  format/     string-safe decimal, date and code-label formatting
  i18n/       every user-facing string (NFR-05)
  mocks/      MSW handlers + seed data
  screens/    route components (S1-S4 + auth screens) and their tests
  state/      session context (replaces COMMAREA), validation, error routing, shortcuts
e2e/          Playwright smoke test
```

## Keyboard

PF keys are mapped to normal buttons and links; shortcuts are extras.

| Legacy | Web |
|---|---|
| ENTER | Submit the form / focused button |
| PF3 | "Back to menu", `Esc` or `Alt+3` |
| PF7 / PF8 | "Previous" / "Next", `PageUp` / `PageDown` |
