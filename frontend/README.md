# CLBS Frontend — Portfolio Management Web UI

Modernized web frontend for the COBOL Legacy Benchmark Suite Portfolio
Management System. This package delivers the **navigation & layout shell**
(MBA-1432 / US-8) that replaces the legacy MENMAP menu and PF-key flow from
`src/maps/INQSET.bms` with a responsive, desktop-first web experience.

## Scope

- Dashboard / home page with key metrics (total AUM, active portfolios, recent
  transactions).
- Primary sidebar navigation: Dashboard, Portfolios, Transactions, History,
  Reports.
- Breadcrumb navigation replacing the PF-key (PF3/PF7/PF8) flow.
- Consistent header and footer across all pages.
- Responsive layout: full sidebar on desktop, collapsible drawer on tablet.
- Optional keyboard shortcuts: press `g` then a section key (`d`/`p`/`t`/`h`/`r`).

Section pages other than the dashboard are intentionally light placeholders;
their full implementations are delivered by sibling tickets in the epic
(MBA-1426 portfolios, MBA-1428 transactions, MBA-1429 history, MBA-1430
reports).

## Tech stack

React 18 · TypeScript · Vite 5 · React Router 6 · Vitest.

The data layer currently reads from in-memory fixtures (`src/data/`) that mirror
the COBOL copybooks (`PORTFLIO.cpy`, `TRNREC.cpy`). The `/api` proxy in
`vite.config.ts` is the integration point for a future backend.

## Requirements

Node.js 18+ (Node 20 recommended).

## Commands

```bash
npm install      # install dependencies
npm run dev      # start the dev server (http://localhost:5173)
npm run build    # type-check and build for production
npm run preview  # preview the production build
npm run lint     # eslint (zero-warning policy)
npm run typecheck
npm test         # run unit tests (vitest)
```
