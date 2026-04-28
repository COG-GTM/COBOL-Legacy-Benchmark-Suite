# Testing the Modernized React Frontend

## Quick Start

```bash
cd frontend
npm install
npm run dev   # starts on http://localhost:5173
```

## Login

The app uses a mock auth context — any non-empty User ID + Password will authenticate.
Default test credentials: `ADMIN` / `password`

Auth state is stored in `sessionStorage` under key `auth`. Refreshing the page preserves the session.

## Navigation Structure

Sidebar navigation (defined in `src/components/layout/Sidebar.tsx`):
- **Dashboard** — `/`
- **Portfolios** — `/portfolios` (list), `/portfolios/new`, `/portfolios/:id`, `/portfolios/:id/edit`
- **Transactions** — `/transactions`, `/transactions/new`
- **Reports** (expandable, expanded by default):
  - Position Report — `/reports/positions`
  - Audit Report — `/reports/audit`
  - Statistics — `/reports/statistics`
- **Batch Monitor** — `/batch`
- **Error Log** — `/errors`

## Mock Data

All data comes from `src/data/mockData.ts` with types in `src/data/types.ts`.

### Key Data Relationships
- **Portfolios** have IDs like `PORT0001` through `PORT0012`
- **Positions** are linked to portfolios via `accountNo`, NOT `portfolioId`
- Account number pattern: `1000000XX` where `XX` = zero-padded portfolio index
  - PORT0001 → 100000001, PORT0010 → 100000010, PORT0012 → 100000012
- Portfolios with `status: 'C'` (Closed) are excluded from reports
- Positions with `status: 'C'` are filtered out of quantity calculations

### Batch Jobs
- 8 jobs defined: TRNVAL00, POSUPD00, HISTLD00, RPTPOS00, RPTAUD00, RPTSTA00, UTLMNT00, UTLMON00
- Jobs have `recordCount: number` — always defined in mock data, can be 0
- Jobs with `recordCount === 0`: RPTSTA00 (Processing), UTLMNT00 (Waiting), UTLMON00 (Waiting)

### Error Entries
- 12 entries across 4 programs (TRNVAL00, POSUPD00, HISTLD00, RPTPOS00)
- Error codes E001-E004 (Error severity) and W001-W002 (Warning severity)
- All timestamps on 2024-08-15

## Common Pitfalls

1. **Truthiness checks on numeric fields**: `value ? ... : fallback` treats `0` as falsy. Always use `value !== undefined` for numeric fields that can legitimately be zero.

2. **Portfolio-to-position mapping**: There is no direct `portfolioId` on positions. The mapping is computed via account number using the portfolio's index extracted from its ID.

3. **Unicode em-dash**: The codebase uses `\u2014` (em-dash) as placeholder for missing values, not a regular hyphen.

4. **Recharts typing**: The `Pie` component's `label` render prop receives `PieLabelRenderProps` where `name` and `percent` are optional — use nullish coalescing (`??`) to provide defaults.

## Build Verification

```bash
npm run build  # runs tsc -b && vite build
```

Build must pass with zero TypeScript errors before creating a PR.

## Devin Secrets Needed

None — this frontend uses only mock data with no external APIs or authentication services.
