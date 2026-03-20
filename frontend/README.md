# Portfolio Management System — Web Frontend

Modernized web frontend for the COBOL Portfolio Management System (originally CICS 3270 terminal application).

## Tech Stack

- **React 18** with TypeScript
- **Vite** as build tool
- **Tailwind CSS** for styling
- **shadcn/ui** component primitives
- **React Router v6** for client-side routing
- **Lucide React** for icons

## Getting Started

```bash
cd frontend
npm install
npm run dev
```

## Current Status

**Phase 1 — Dashboard/Main Menu implemented with mock data. No backend connectivity.**

The dashboard serves as the modernized replacement for the original CICS 3270 main menu screen (`MENMAP` in `src/maps/INQSET.bms`). All data displayed is static mock data.

## Screen Mapping

| Original 3270 Screen | BMS Map | Modernized Route | Status |
|---|---|---|---|
| Main Menu | `MENMAP` | `/` (Dashboard) | Implemented |
| Portfolio Position Inquiry | `POSMAP` | `/portfolio-inquiry` | Placeholder |
| Transaction History | `HISMAP` | `/transaction-history` | Placeholder |
| Error Display | `ERRMAP` | `ErrorBanner` component | Implemented |

## Project Structure

```
frontend/src/
├── components/
│   ├── layout/          # AppShell, Sidebar, Header, Footer
│   ├── dashboard/       # DashboardPage, QuickActions, SummaryCards, RecentActivity
│   └── common/          # ErrorBanner, LoadingSpinner
├── pages/               # Route-level page components
├── router/              # React Router configuration
├── mock/                # Mock data for dashboard
├── types/               # TypeScript type definitions
├── lib/                 # Utility functions (cn, formatCurrency, etc.)
├── App.tsx              # Root app component with RouterProvider
├── main.tsx             # Entry point
└── index.css            # Tailwind CSS imports and theme variables
```
