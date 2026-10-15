# Portfolio Management System — web frontend

React + TypeScript + Vite front end for the CLBS portfolio system, replacing the CICS
3270 screens in `src/maps/INQSET.bms`. Components follow [shadcn/ui](https://ui.shadcn.com)
(Radix primitives + Tailwind CSS, copied into `src/components/ui`).

All data is mock JSON in `src/data` — there are no backend calls yet.

## Run

```bash
cd frontend
npm ci
npm run dev        # http://localhost:5173
```

| Script              | What it does                     |
| ------------------- | -------------------------------- |
| `npm run dev`       | Vite dev server                  |
| `npm run build`     | Type-check and production build  |
| `npm run typecheck` | `tsc -b`                         |
| `npm run lint`      | ESLint                           |
| `npm test`          | Vitest + React Testing Library   |

## Layout

| Path                         | Contents                                                        |
| ---------------------------- | --------------------------------------------------------------- |
| `src/components/layout`      | App shell: sidebar / drawer, header, Exit dialog, message line  |
| `src/components/ui`          | shadcn/ui components                                            |
| `src/nav/navigation.ts`      | Menu items, MENMAP options, placeholder page metadata           |
| `src/pages`                  | Dashboard and placeholder pages                                 |
| `src/services`               | Pure functions that derive view data from the mock records      |
| `src/types`                  | Types mirroring the copybooks (`PORTFLIO.cpy`, `TRNREC.cpy`)    |
| `src/data`                   | Mock JSON                                                       |

## Legacy mapping (MENMAP)

| MENMAP (INQSET.bms)            | Web                                                  |
| ------------------------------ | ---------------------------------------------------- |
| Title `Portfolio Management System` | Header title                                    |
| `1. Portfolio Position Inquiry` | **Positions** (`/positions`)                        |
| `2. Transaction History`       | **Transactions** (`/transactions`)                   |
| `3. Exit`                      | **Exit** in the sidebar → confirm → `/signed-out`    |
| `OPTION` input                 | Dashboard *Main menu* card: type 1–3, press Enter    |
| `ERRMSG` (row 23)              | Message line at the bottom of every page             |

Navigation is a fixed sidebar on desktop (≥ 1024 px), an icon rail on tablet (768–1023 px)
and a drawer behind the header menu button below 768 px.
