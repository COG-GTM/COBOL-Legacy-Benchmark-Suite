# OCBC Web Middleware — CICS BMS 3270 Replacement

A locally-runnable full-stack app that replaces the CICS BMS 3270 green-screen UI
(`src/maps/INQSET.bms`) of the Investment Portfolio Management System with a modern,
OCBC-branded web UI.

The original COBOL/CICS programs cannot run without z/OS, so the Express backend serves
**mock data shaped exactly like the COBOL copybooks**. Every page and endpoint keeps a 1:1
mapping to a legacy BMS map and COBOL program — see [MAPPING.md](./MAPPING.md).

No existing COBOL, BMS, JCL or CICS artefact is modified; this is a purely additive layer.

## Stack

- **Backend**: Node.js + Express (`server/`), in-memory mock store, Jest + Supertest
- **Frontend**: React 18 + Vite + TypeScript (`client/`), Vitest + Testing Library
- **Theme**: OCBC design tokens in `src/theme/ocbc.ts`, applied globally as CSS variables

## Run locally (npm)

```bash
cd modernization/web-middleware
npm install
npm run dev
```

- Web UI: <http://localhost:5173>
- API: <http://localhost:4000> (the Vite dev server proxies `/api` to it)

## Run locally (docker-compose)

```bash
cd modernization/web-middleware
docker compose up
```

Same URLs: UI on <http://localhost:5173>, API on <http://localhost:4000>.

## Tests

```bash
npm test                       # backend (Jest/Supertest) + frontend (Vitest/RTL)
npm run test -w web-middleware-server
npm run test -w web-middleware-client
npm run typecheck -w web-middleware-client
```

## API

| Endpoint | Legacy equivalent | Notes |
| --- | --- | --- |
| `GET /api/menu` | `MENMAP` / `INQONLN` `P200-DISPLAY-MENU` | Three options mapped to `INQCOM-FUNCTION` values `INQP`/`INQH`/`EXIT` |
| `GET /api/position?account=` | `POSMAP` / `INQPORT` | POSREC-shaped payload; 404 + `Position not found for account` mirrors `P900-NOT-FOUND` |
| `GET /api/history?account=&page=` | `HISMAP` / `INQHIST` | HISTREC-shaped rows, 10 per page (the `ROW1..ROW10` window) |

Every response carries a `commarea` object modelled on `src/copybook/online/INQCOM.cpy`
(`inqcomFunction`, `inqcomAccountNo` 10 chars, `inqcomResponseCode`, `inqcomErrorMsg` 80 chars).

Sample accounts (derived from `documentation/operations/test-data-specs.md` and
`documentation/technical/data-dictionary.md`): `100000001`, `100000002`, `100000003`.

## Theming

`src/theme/ocbc.ts` is the single source of truth for the palette. The values were derived at
build time from the OCBC brand source <https://api.ocbc.com/store> (dominant brand red
`#d8232a`). Re-derive with:

```bash
npm run theme:sync
```

If the source is unreachable, the script keeps the documented fallback and leaves the TODO in
`src/theme/ocbc.ts` pointing at the source URL.

BMS colour semantics are mapped onto the tokens (`bmsColorMap`):

| BMS attribute | Theme token |
| --- | --- |
| `COLOR=RED` (error fields) | `error` |
| `COLOR=TURQUOISE` (data output) | `accent` |
| `ATTRB=BRT` (titles/headers) | `primary` |
