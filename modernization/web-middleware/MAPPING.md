# Legacy → Web Mapping

Source of truth for screen definitions: `src/maps/INQSET.bms` (mapset `INQSET`).

## Screens

| Web page | Component | BMS map | COBOL program | Notes |
| --- | --- | --- | --- | --- |
| `/` | `client/src/pages/MenuPage.tsx` | `MENMAP` | `INQONLN` `P200-DISPLAY-MENU` | The three menu options and the `OPTION` input become buttons dispatching `INQP`/`INQH`/`EXIT` |
| `/position` | `client/src/pages/PositionPage.tsx` | `POSMAP` | `INQONLN` `P300-PORTFOLIO-INQUIRY` → `INQPORT` | `ACCTIN` → account input; `FUNDOUT`/`NAMEOUT`/`UNITOUT`/`COSTOUT`/`VALOUT` → data fields |
| `/history` | `client/src/pages/HistoryPage.tsx` | `HISMAP` | `INQONLN` `P400-HISTORY-INQUIRY` → `INQHIST` | `HISAIN` → account input; fixed `ROW1..ROW10` fields → data table; `PF7`/`PF8` → pagination buttons |
| (any page) | `client/src/components/ErrorPanel.tsx` | `ERRMAP` | `INQONLN` `P900-ERROR-ROUTINE`, `INQPORT` `P900-NOT-FOUND`/`P999-ERROR-ROUTINE` | `ERRCOUT`/`ERRDOUT` → error code + details; replaces the row-23 red `ERRMSG`/`POSMSG`/`HISMSG` line |
| `/exit` | `client/src/pages/ExitPage.tsx` | — | `INQONLN` `WHEN 'EXIT'` (`SESSION-TERMINATED`) | Terminal state of the pseudo-conversation |

## Endpoints

| Endpoint | Handler | BMS map | COBOL program / paragraph |
| --- | --- | --- | --- |
| `GET /api/menu` | `server/src/app.js` | `MENMAP` | `INQONLN` `P200-DISPLAY-MENU` |
| `GET /api/position` | `server/src/app.js` | `POSMAP` | `INQPORT` `P200-GET-POSITION` / `P300-FORMAT-DISPLAY`, not-found from `P900-NOT-FOUND` |
| `GET /api/history` | `server/src/app.js` | `HISMAP` | `INQHIST` (linked from `INQONLN` `P400-HISTORY-INQUIRY`) |

## Data contracts

| JSON shape | Copybook |
| --- | --- |
| `commarea` | `src/copybook/online/INQCOM.cpy` (`INQCOM-FUNCTION`, `INQCOM-ACCOUNT-NO`, `INQCOM-RESPONSE-CODE`, `INQCOM-ERROR-MSG`) |
| `position` | `src/copybook/common/POSREC.cpy` (`POS-KEY`, `POS-DATA`, `POS-AUDIT`) |
| `rows[]` | `src/copybook/common/HISTREC.cpy` (`HIST-KEY`, `HIST-DATA`) |

COBOL field names are carried across in lowerCamelCase (`POS-MARKET-VALUE` → `posMarketValue`),
`COMP-3` numerics become JSON numbers, and fixed-length character fields keep their legacy
lengths (e.g. `inqcomErrorMsg` is space-padded to 80).

`POSREC.cpy` has no fund-name field; `posFundName` is a display-only addition sourced from the
fund master concept in `documentation/technical/data-dictionary.md` so `POSMAP`'s `NAMEOUT`
field can be reproduced. Likewise `HISTREC.cpy` carries before/after images rather than
units/price/amount, so `histUnits`/`histPrice`/`histAmount` are derived display fields matching
the `HISMAP` column headers and the `TRANHIST` layout in the data dictionary.

## Known naming discrepancy (source-of-truth choice)

`src/programs/online/INQONLN.cbl` issues:

- `EXEC CICS RECEIVE MAP('INQMAP') MAPSET('INQSET')`
- `EXEC CICS SEND MAP('INQMNU') MAPSET('INQSET')`

but mapset `INQSET` in `src/maps/INQSET.bms` only defines `MENMAP`, `POSMAP`, `HISMAP` and
`ERRMAP`. `INQMAP` and `INQMNU` do not exist — under CICS these calls would fail with
`MAPFAIL`/`INVMPSZ`. This middleware treats **`INQSET.bms` as the source of truth** and maps
`INQMNU` → `MENMAP` (menu send) and `INQMAP` → `MENMAP` (initial receive), because the
dispatch in `INQONLN` reads the `INQCOM-FUNCTION` code that the menu screen produces.
The COBOL is intentionally left unmodified; this note exists so future readers do not mistake
the web naming for a translation error.
