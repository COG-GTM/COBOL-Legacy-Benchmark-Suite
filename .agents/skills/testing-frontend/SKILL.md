# Testing the COBOL Modernization Frontend

## Dev Server

```bash
cd frontend && npx vite --host 0.0.0.0 --port 5173
```

The server may start on port 5174 if 5173 is occupied.

## Login

The app has a mock auth system — any non-empty User ID and password will work.
Example: User ID = `ADMIN`, Password = `password`

## Test Accounts

Mock data in `frontend/src/data/mockData.ts` has accounts `100000001` through `100000011`.

- Each account has 2 positions in the positions array
- Each account has 2-6 transactions in the transactions array
- Account `100000001` has 6 transactions (most data for testing filters)
- Account `100000005` has a closed position (MUNBPF, status 'C') — good for testing status badges
- Account `100000004` has a Fee transaction (TXN000008) — good for testing Fee badge
- Account `100000008` has an Error status transaction (TXN000016) — good for testing Error badge

## Key Pages

- `/positions` — Position Inquiry (search by 9-digit account number)
- `/transactions` — Transaction History (search by account, filter by type/status/date)
- `/transactions?account=100000001` — Pre-populated transaction search

## Build Verification

```bash
cd frontend && npm run build
```

Must pass with zero TypeScript errors before creating a PR.
