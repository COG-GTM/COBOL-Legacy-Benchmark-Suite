# Portfolio Management System — Frontend

Modernized web UI for the COBOL Legacy Benchmark Suite Investment Portfolio Management System.

## Stack

- **React 19** + **TypeScript 6** + **Vite 8**
- **Ant Design 6** — UI component library
- **React Router 7** — client-side routing with lazy-loaded pages
- **react-hook-form 7** — form state and validation

## Getting Started

```bash
npm install
npm run dev
```

Open http://localhost:5173 and log in with any username/password (mock auth).

## Available Scripts

| Script          | Description                              |
| --------------- | ---------------------------------------- |
| `npm run dev`   | Start Vite dev server with HMR           |
| `npm run build` | Type-check with `tsc` then bundle        |
| `npm run lint`  | Run ESLint                               |
| `npm run preview` | Preview production build locally       |

## Project Structure

```
src/
├── components/   # Shared components (ErrorBoundary, ProtectedRoute, Toast)
├── contexts/     # AuthContext (localStorage-based mock auth)
├── layouts/      # AppLayout with collapsible sidebar
├── mocks/        # Mock JSON data files
├── pages/        # Route pages grouped by feature
│   ├── auth/         # LoginPage
│   ├── dashboard/    # DashboardPage
│   ├── inquiry/      # PositionInquiryPage, TransactionHistoryPage
│   ├── portfolio/    # List, Create, Detail, Edit pages
│   ├── reports/      # Valuation, Audit, Statistics pages
│   └── transactions/ # TransactionEntryPage
├── types/        # TypeScript interfaces (mirroring COBOL copybooks)
├── utils/        # Validation (PORTVALD rules) and formatting utilities
├── router.tsx    # Route definitions
├── App.tsx       # Root component
└── main.tsx      # Entry point
```

## Notes

- **Frontend only** — no backend server. All data comes from mock JSON files or in-memory state.
- API contract types in `src/types/api.ts` are defined for future backend integration.
- Validation rules in `src/utils/validation.ts` mirror `PORTVALD.cbl`.
