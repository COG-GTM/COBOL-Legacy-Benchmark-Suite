# S4 — inquiry-api (BFF) Modernization Spec

Phase 0 analysis of the CICS online inquiry layer of the Portfolio Management System.
Target per decomposition plan: **S4 inquiry-api** — Spring REST facade (no data owned; reads S1 portfolio-service / S3 position-service), paginated queries replacing `CURSMGR`, optional thin UI mirroring the `INQSET` screens.

Sources analyzed (every line read):

| File | Lines | Role |
|---|---|---|
| `src/programs/online/INQONLN.cbl` | 170 | Transaction `PINQ` entry point / menu dispatcher |
| `src/programs/online/INQPORT.cbl` | 109 | Position inquiry (VSAM `POSFILE`) |
| `src/programs/online/INQHIST.cbl` | 192 | Transaction-history inquiry (DB2 `POSHIST` via `CURSMGR`) |
| `src/programs/online/CURSMGR.cbl` | 90 | DB2 cursor lifecycle service (**truncated file**, see §5) |
| `src/programs/online/DB2ONLN.cbl` | 119 | DB2 connect/disconnect/status service |
| `src/programs/online/DB2RECV.cbl` | 144 | DB2 recovery (reconnect retry, rollback, cursor error) |
| `src/programs/online/ERRHNDL.cbl` | 117 | Centralized error logger/formatter |
| `src/maps/INQSET.bms` | 100 | BMS mapset: `MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP` |
| `src/cics/PORTDFN.csd` | 99 | CICS resource definitions (`PINQ`, programs, `POSFILE`, DB2ENTRY) |
| `src/copybook/online/INQCOM.cpy` | 11 | Inquiry COMMAREA |
| `src/copybook/online/DB2REQ.cpy` | 12 | DB2 request area |
| `src/copybook/online/ERRHND.cpy` | 20 | Online error-handling area |

Context skimmed: `documentation/technical/system-architecture.md` (SAD §1.2.6, §2.3, §2.5, §5.1.4, §5.2.2), `documentation/technical/data-dictionary.md` (§3.1, §3.2, §4.1), `src/copybook/common/POSREC.cpy` (copied by `INQPORT`), `src/database/db2/POSHIST.sql`, `src/database/db2/ERRLOG.sql`, `src/programs/online/SECMGR.cbl` (S5, referenced only as an outbound interface).

Conventions: `file:line` refs point at the COBOL sources above. Rules marked **INFERRED FROM DOCS** are not present in code. Nothing in this spec invents behavior; where code is absent, broken, or truncated, that is stated.

---

## 1. Purpose & scope

The online inquiry layer gives a 3270 terminal user, under CICS transaction **`PINQ`**, read-only access to two views of an investment account:

1. **Portfolio position inquiry** — for an account number, show the current holding (fund, units, cost basis, market value) read from the Position Master VSAM file.
2. **Transaction history inquiry** — for an account number, show the most recent transactions (date, type, units, price, amount), newest first, up to 10 per screen, read from DB2 table `POSHIST`.

Supporting behavior that belongs to this component in the legacy code:

- A **menu** screen with options 1 (position), 2 (history), 3 (exit).
- **Security gating** — the user must be validated, authorized for resource `INQONLN` with access `READ`, and an audit record written (delegated to `SECMGR`, which is S5).
- **DB2 plumbing** — connection acquisition with a 100-connection cap, cursor declare/open/fetch/close with optional 20-row array fetch, and a 3-attempt reconnect retry with 2-second delay.
- **Error handling** — every online error is logged to DB2 table `ERRLOG`, formatted as `Error in <program> - <message> (<trace-id>)`, and mapped to an action (continue / return / abend).

In the target architecture this becomes **inquiry-api**: a stateless REST BFF that exposes menu-less endpoints for position and history lookups, delegates position data to S3 position-service and (if needed) S1 portfolio-service, delegates authentication/authorization to S5, and uses S8 platform-common for error handling. The CICS/DB2 plumbing programs (`CURSMGR`, `DB2ONLN`, `DB2RECV`) have **no business meaning** and are retired in favor of Spring pagination, connection pooling, and resilience patterns; their rules are recorded below only so parity decisions are explicit.

Out of scope for S4: `SECMGR` internals (S5), position/history *maintenance* (S2/S3), batch.

---

## 2. Business rules

Numbered `BR-nn`. Grouped by program, extracted paragraph by paragraph. Return codes are the exact values placed in the COMMAREA/response fields.

### 2.1 INQONLN — transaction entry / dispatcher

| # | Rule | Ref |
|---|---|---|
| BR-01 | Transaction `PINQ` starts program `INQONLN` (profile `DFHCICST`, group `PORTGRP`); it is the only online transaction. | `PORTDFN.csd:6-11` |
| BR-02 | The session is **conversational**: `P100-PROCESS-REQUEST` loops until `SESSION-TERMINATED` (`WS-END-OF-SESSION = 'Y'`), then `EXEC CICS RETURN` ends the task. No pseudo-conversational RETURN TRANSID. | `INQONLN.cbl:19-21, 46-51` |
| BR-03 | CICS conditions `ERROR`, `PGMIDERR`, `NOTFND` are routed to `P900-ERROR-ROUTINE`. | `INQONLN.cbl:40-44` |
| BR-04 | Every iteration first clears the COMMAREA to `LOW-VALUES`, then `RECEIVE MAP('INQMAP') MAPSET('INQSET')` **into the COMMAREA** (`WS-COMMAREA`) — i.e. the received screen bytes are interpreted directly as the `INQCOM` layout. `RESP` is captured but never inspected. | `INQONLN.cbl:54-60` |
| BR-05 | Function dispatch on the 4-char function code: `'MENU'` → send menu; `'INQP'` → LINK `INQPORT`; `'INQH'` → LINK `INQHIST`; `'EXIT'` → terminate session; anything else → error routine. (Code references `WS-COMMAREA-FUNCTION`, which is **not defined** by `INQCOM.cpy` — the field is `INQCOM-FUNCTION`; see D-01.) | `INQONLN.cbl:62-77`, `INQCOM.cpy:5-9` |
| BR-06 | Menu display = `SEND MAP('INQMNU') MAPSET('INQSET') ERASE`. (Map `INQMNU` does not exist in `INQSET.bms`; the menu map is `MENMAP` — D-02.) | `INQONLN.cbl:93-97`, `INQSET.bms:7` |
| BR-07 | Position inquiry and history inquiry are invoked by `EXEC CICS LINK` passing the full `INQCOM` COMMAREA (98 bytes); the sub-program's changes to `DFHCOMMAREA` are the response. | `INQONLN.cbl:102-106, 111-115` |
| BR-08 | **Security check runs after dispatch**, on every loop iteration (`P050` is performed after the `EVALUATE`, not before). If `SEC-RESPONSE-CODE ≠ 0`, `SEC-ERROR-INFO` is copied to the error message, `P900` is performed, and the task ends immediately with `EXEC CICS RETURN` (skipping the `SESSION-TERMINATED` loop exit). | `INQONLN.cbl:79-88` |
| BR-09 | Security sequence (three LINKs to `SECMGR`, S5): (a) `'V'` validate with `SEC-USER-ID` obtained from `EXEC CICS ASSIGN USERID`; (b) only if (a) returned 0: `'A'` authorize resource `'INQONLN'`, access `'READ'`; (c) only if (b) returned 0: `'L'` write audit record. Failure of any step short-circuits the rest; the last `SEC-RESPONSE-CODE` is the outcome. | `INQONLN.cbl:139-171` |
| BR-10 | `SECMGR` return codes consumed by S4 (from `SECMGR.cbl`, S5): `0` = ok; `8` = user validation failed / access denied; `12` = unable to obtain credentials / authorization check failed / audit logging failed. Any non-zero code terminates the inquiry session (BR-08). | `SECMGR.cbl:62-74, 89-100, 130-136` |
| BR-11 | Error routine: sets `ERR-PROGRAM='INQONLN'`, `ERR-PARAGRAPH='P900-ERROR-ROUTINE'`, `ERR-CICS-RESP=EIBRESP`, `ERR-CICS-RESP2=EIBRESP2`, **severity always `'W'` (warning)**, LINKs `ERRHNDL`; if `ERRHNDL` returns action `'A'` → `EXEC CICS ABEND ABCODE('IERR')`; else the formatted `ERR-MESSAGE` becomes the display error message (`WS-ERROR-MESSAGE` — undefined field, D-03). | `INQONLN.cbl:119-137` |
| BR-12 | Because severity is always `'W'`, `INQONLN` errors abend **only** when `ERRHNDL` itself fails to write `ERRLOG` (BR-46, BR-49). | `INQONLN.cbl:124`, `ERRHNDL.cbl:85-88, 106-107` |
| BR-13 | The menu screen offers exactly three options: `1. Portfolio Position Inquiry`, `2. Transaction History`, `3. Exit`; input `OPTION` is a single numeric character with initial cursor; `ERRMSG` (78 chars, red, bright) on row 23. **No code maps option `1/2/3` to function codes `INQP/INQH/EXIT`** (D-04). | `INQSET.bms:7-19` |

### 2.2 INQPORT — position inquiry

| # | Rule | Ref |
|---|---|---|
| BR-14 | Initialization: position record cleared to `LOW-VALUES`; inbound `DFHCOMMAREA` copied to working COMMAREA; conditions `ERROR` → `P999`, `NOTFND` → `P900-NOT-FOUND`. | `INQPORT.cbl:57-66` |
| BR-15 | Lookup key: the COMMAREA account number is moved to `POSITION-ACCOUNT OF WS-POSITION-RECORD` and used as `RIDFLD` for `EXEC CICS READ FILE('POSFILE')`. Neither `WS-COMMAREA-ACCOUNT-NO` nor `POSITION-ACCOUNT` exists (COMMAREA field is `INQCOM-ACCOUNT-NO` X(10); `POSREC` key is `POS-KEY` = portfolio-id X(8) + date X(8) + investment-id X(10), 26 bytes). **The actual key semantics are undefined in code** (D-05). | `INQPORT.cbl:68-76`, `INQCOM.cpy:10`, `POSREC.cpy:7-10` |
| BR-16 | Existence flag: `RESP = DFHRESP(NORMAL)` → `POSITION-EXISTS ('Y')`; any other RESP (including NOTFND, since `RESP` suppresses the HANDLE CONDITION) → `NO-POSITION ('N')`. One record only — no browse, no paging. | `INQPORT.cbl:78-82` |
| BR-17 | Found → `SEND MAP('POSMAP') MAPSET('INQSET') FROM(WS-POSITION-RECORD) ERASE`: the raw 138-byte `POSREC` (packed decimals included) is sent as the map's symbolic data; no numeric-to-display formatting is coded. | `INQPORT.cbl:86-92` |
| BR-18 | Not found → `INQCOM-ERROR-MSG = 'Position not found for account'` (30 chars, padded to 80), COMMAREA returned to caller; `INQCOM-RESPONSE-CODE` is **left unchanged** (0 / low-values). | `INQPORT.cbl:96-101` |
| BR-19 | CICS `ERROR` condition → `INQCOM-ERROR-MSG = 'Error accessing position data'`, `INQCOM-RESPONSE-CODE = WS-RESPONSE-CODE` (the CICS RESP value), COMMAREA returned. `ERRHNDL` is **not** called (contradicts SAD §5.1.4). | `INQPORT.cbl:103-110` |
| BR-20 | `INQPORT` performs **no DB2 access** despite header comment and `INCLUDE SQLPOS` (include member does not exist in repo — D-06). Only VSAM `POSFILE` is read. | `INQPORT.cbl:7, 20-21` |
| BR-21 | Position screen fields (display contract): Account X(10) input; Fund ID X(6); Fund Name X(30); Units X(15); Cost Basis X(15); Market Value X(15); PF3=Exit PF7=Previous PF8=Next legend; `POSMSG` 78-char red message. PF-key handling is **not implemented** in any program (no `EIBAID` test). | `INQSET.bms:23-49` |
| BR-22 | `POSFILE` is defined read/browse/add-enabled, update/delete-disabled, DSNAME `PORTFOLIO.POSITION.VSAM`, `RECORDSIZE(200)`, 10 strings. Inquiry uses only READ. | `PORTDFN.csd:69-79` |

### 2.3 INQHIST — transaction history inquiry

| # | Rule | Ref |
|---|---|---|
| BR-23 | Initialization: COMMAREA copied in, `WS-ROW-COUNT = 0`, `NO-MORE-ROWS`, `ERROR` condition → `P999`, then DB2 connect. `WS-ROW-COUNT` and `MORE-ROWS` are never updated afterwards (no scrolling state). | `INQHIST.cbl:81-93` |
| BR-24 | DB2 connect: LINK `DB2ONLN` with request `'C'`. On non-zero response: LINK `DB2RECV` with request `'C'`, `RECV-PROGRAM='INQHIST'`, `RECV-SQLCODE = DB2-SQLCODE`; if recovery succeeds, **re-perform `P150-DB2-CONNECT`** (a recursive PERFORM — undefined in COBOL, D-07); otherwise `INQCOM-ERROR-MSG = RECV-MESSAGE` and `P999`. Connection token copied to `WS-DB2-TOKEN` (undefined field — D-03). | `INQHIST.cbl:95-127` |
| BR-25 | History query (dynamic SQL text handed to `CURSMGR`): `SELECT TRANS_DATE, TRANS_TYPE, TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC`. **The `?` parameter is never bound** — the account number is not passed to `CURSMGR` (no field for it in `CURSOR-REQUEST-AREA`). Columns `TRANS_UNITS/TRANS_PRICE/TRANS_AMOUNT` do not exist in `POSHIST.sql` (D-08). | `INQHIST.cbl:130-135`, `CURSMGR.cbl:28-41`, `POSHIST.sql:25-43` |
| BR-26 | Cursor protocol: `'D'` declare (cursor name `HISTORY_CURSOR`, array fetch `'Y'`) → if rc 0: `'O'` open → if rc 0: one `'F'` fetch → **always** `'C'` close (even when declare/open failed). | `INQHIST.cbl:137-160`, `INQHIST.cbl:45-47` |
| BR-27 | Fetch result: if `CURS-RESPONSE-CODE >= 0` (so SQLCODE `+100` "no rows" also counts as success), the first bytes of the 3000-byte `CURS-DATA-AREA` are moved into `WS-HISTORY-TABLE` (10 × 32 = 320 bytes); the remainder is truncated. Only one fetch per screen: **maximum 10 rows, no next/previous page** despite the PF7/PF8 legend. | `INQHIST.cbl:164-175`, `INQHIST.cbl:20-26` |
| BR-28 | Row layout expected from the fetch (per entry, 32 bytes): `WS-TRANS-DATE X(10)`, `WS-TRANS-TYPE X(4)`, `WS-TRANS-UNITS S9(9)V99 COMP-3`, `WS-TRANS-PRICE S9(9)V99 COMP-3`, `WS-TRANS-AMOUNT S9(9)V99 COMP-3`. Arithmetic: none — values are displayed as fetched; scale 2 for units/price/amount (vs. `DECIMAL(15,3)` for `QUANTITY`/`PRICE` in DDL → would truncate the third decimal if ever mapped; D-08). | `INQHIST.cbl:20-26` |
| BR-29 | Display: `SEND MAP('HISMAP') MAPSET('INQSET') FROM(WS-HISTORY-TABLE) LENGTH(320) ERASE` — raw packed table sent as map data; no row formatting into the 65-char `ROW1..ROW10` fields. | `INQHIST.cbl:177-186`, `INQSET.bms:72-81` |
| BR-30 | Error path: `INQCOM-RESPONSE-CODE = SQLCODE` (from `INQHIST`'s own SQLCA, which no statement in `INQHIST` populates — always 0 unless preprocessor initialises otherwise), COMMAREA returned. No message text set except in the connect-failure path (BR-24). `ERRHNDL` is **not** called. | `INQHIST.cbl:188-193` |
| BR-31 | History screen contract: Account X(10) input; column headers `Date`(10) `Type`(4) `Units`(10) `Price`(10) `Amount`(12); 10 data rows of 65 chars at rows 7–16; PF3/PF7/PF8 legend; `HISMSG` 78-char red message. | `INQSET.bms:53-85` |

### 2.4 CURSMGR — DB2 cursor manager

| # | Rule | Ref |
|---|---|---|
| BR-32 | Request types (Level-88): `'D'` declare, `'O'` open, `'F'` fetch, `'C'` close; unknown type → no action, returns unchanged. | `CURSMGR.cbl:29-33, 44-57` |
| BR-33 | Declare: rc reset to 0; array size = 20 (`WS-MAX-ROWS`) when `CURS-ARRAY-FETCH='Y'`, else 1 (value computed but never used by any later paragraph). `EXEC SQL DECLARE :CURS-NAME CURSOR FOR :CURS-STMT` — host variables are not legal for cursor name/statement in static SQL; a real implementation needs `PREPARE`/dynamic SQL (D-09). Non-zero SQLCODE → `CURS-RESPONSE-CODE = SQLCODE`. | `CURSMGR.cbl:61-78` |
| BR-34 | Open: fetch statistics zeroed; `EXEC SQL OPEN :CURS-NAME`; `CURS-RESPONSE-CODE = 0` on success, else `SQLCODE`. | `CURSMGR.cbl:80-90` |
| BR-35 | **Fetch (`P300-FETCH-DATA`) and Close (`P400-CLOSE-CURSOR`) paragraphs do not exist** — the file ends at line 90 mid-paragraph (`P200-EXIT` without period). A PERFORM of an undefined paragraph is a compile error; the row-fetch and array-fetch behavior is therefore **unimplemented**. `WS-FETCH-COUNT`, `WS-ROWS-FETCHED`, `WS-FETCH-TIME`, `CURS-DATA-LENGTH` are never set. | `CURSMGR.cbl:51-56, 90` |
| BR-36 | **INFERRED FROM DOCS** (header comment only): "array fetching for performance" and "cursor status monitoring" — intended multi-row FETCH into `CURS-DATA-AREA` with stats. No code. | `CURSMGR.cbl:3-9` |

### 2.5 DB2ONLN — DB2 connection manager

| # | Rule | Ref |
|---|---|---|
| BR-37 | Request types: `'C'` connect, `'D'` disconnect, `'S'` status; unknown → no action. | `DB2ONLN.cbl:29-32, 40-50` |
| BR-38 | Connection cap: connect is attempted only while `WS-ACTIVE-CONNECTIONS < 100`; otherwise `DB2-ERROR-MSG='Maximum connections reached'`, `DB2-RESPONSE-CODE=-1`. Counters live in WORKING-STORAGE of a `RESIDENT(NO)` program, so the count is **not durable across LINKs** — the cap is effectively never reached (D-10). | `DB2ONLN.cbl:18-22, 54-62`, `PORTDFN.csd:35-40` |
| BR-39 | Connect: `EXEC SQL CONNECT TO POSMVP`; success → active count +1, `DB2-SQLCODE=0`, `DB2-RESPONSE-CODE=0`, token generated; failure → `DB2-SQLCODE=SQLCODE`, `DB2-ERROR-MSG=SQLERRMC`, `DB2-RESPONSE-CODE=-1`. | `DB2ONLN.cbl:66-81` |
| BR-40 | Token = `FUNCTION CURRENT-DATE` (21 chars) truncated to X(16) = `YYYYMMDDhhmmssff`; the subsequent `STRING token, active-count INTO token` overlaps source and target and cannot add characters beyond the 16-byte field, so the token is effectively the timestamp prefix (not unique per connection). | `DB2ONLN.cbl:83-89` |
| BR-41 | Disconnect: `EXEC SQL DISCONNECT`; success → active count −1, rc 0; failure → `DB2-SQLCODE`, `SQLERRMC`, rc −1. No caller in S4 ever issues `'D'`. | `DB2ONLN.cbl:91-103` |
| BR-42 | Status: `SELECT CURRENT SERVER INTO :DB2-ERROR-MSG`; rc 0/−1 set from SQLCODE, then **overwritten** with `WS-ACTIVE-CONNECTIONS` — the status response code is the active-connection count, and a failed SELECT is indistinguishable from success. No S4 caller uses `'S'`. | `DB2ONLN.cbl:105-120` |

### 2.6 DB2RECV — DB2 recovery manager

| # | Rule | Ref |
|---|---|---|
| BR-43 | Request types: `'C'` connection recovery, `'T'` transaction recovery, `'R'` cursor recovery; unknown → no action. | `DB2RECV.cbl:32-35, 48-58` |
| BR-44 | Connection recovery: up to **3** attempts (`WS-MAX-RETRIES`), each a LINK `DB2ONLN 'C'`; on success `RECV-STATUS='S'`, rc 0, loop exits; on failure `RECV-STATUS='R'`, `RECV-SQLCODE = DB2-SQLCODE`, then `EXEC CICS DELAY INTERVAL(2)` (**2 seconds**) and retry counter +1. After 3 failures `RECV-STATUS='F'`, `RECV-RESPONSE-CODE=-1`. `RECV-MESSAGE` is never set on this path (so `INQHIST` BR-24 displays blanks). | `DB2RECV.cbl:19-21, 62-109` |
| BR-45 | Transaction recovery: `EXEC SQL ROLLBACK`; success → `'S'`, rc 0; failure → `'F'`, `RECV-SQLCODE=SQLCODE`, rc −1. No S4 caller uses `'T'`. | `DB2RECV.cbl:111-123` |
| BR-46 | Cursor recovery: builds an `ERRHND` area (`ERR-PROGRAM=RECV-PROGRAM`, `ERR-PARAGRAPH=RECV-CURSOR`, `ERR-SQLCODE=RECV-SQLCODE`, severity `'W'`), LINKs `ERRHNDL`; if action is `'C'` continue → `RECV-STATUS='R'` (retry), else `'F'`; `RECV-MESSAGE = ERR-MESSAGE`. Given BR-49, the outcome is retry unless error logging failed. No S4 caller uses `'R'`. | `DB2RECV.cbl:125-145` |

### 2.7 ERRHNDL — centralized error handler

| # | Rule | Ref |
|---|---|---|
| BR-47 | Init: COMMAREA copied in; `ERR-TIMESTAMP = FUNCTION CURRENT-DATE` (21 chars into X(26), space padded); if `ERR-TRACE-ID` is spaces, a new trace id is generated from `FUNCTION RANDOM` (numeric moved to X(16) — value format is implementation-defined). | `ERRHNDL.cbl:52-61` |
| BR-48 | Log: `INSERT INTO ERRLOG VALUES (timestamp, program, paragraph, sqlcode, cics-resp, severity, message, trace-id)` — 8 positional values. The `ERRLOG.sql` DDL has 10 columns with different names/types, so this statement would fail at bind (D-11). | `ERRHNDL.cbl:63-83`, `ERRLOG.sql:15-26` |
| BR-49 | If the INSERT fails (`SQLCODE ≠ 0`): `ERR-MESSAGE='Error logging failed'` and severity escalated to `'F'` fatal → action `'A'` abend (BR-51). | `ERRHNDL.cbl:85-88` |
| BR-50 | Message format: `'Error in ' + ERR-PROGRAM (to first space) + ' - ' + ERR-MESSAGE + ' (' + ERR-TRACE-ID + ')'` written back **into `ERR-MESSAGE`** (80 chars; overflow silently truncated; source/target overlap). | `ERRHNDL.cbl:92-101` |
| BR-51 | Severity → action mapping: `'F'` fatal → `'A'` abend; `'W'` warning → `'C'` continue; `'I'` info → `'C'` continue; any other/blank severity → `'R'` return. Result area copied back to `DFHCOMMAREA`. | `ERRHNDL.cbl:104-117`, `ERRHND.cpy:10-18` |
| BR-52 | `ERRHNDL` is LINKed by `INQONLN` and `DB2RECV` but has **no `DEFINE PROGRAM` in `PORTDFN.csd`** — a LINK would raise `PGMIDERR` at runtime unless autoinstalled (D-12). | `PORTDFN.csd:13-61` |

### 2.8 Screen / CICS resource rules

| # | Rule | Ref |
|---|---|---|
| BR-53 | `ERRMAP` ("System Error", `ERRCOUT` X(8) code, `ERRDOUT` X(65) details, "Press ENTER to continue") is defined but **never sent by any program**. | `INQSET.bms:89-99` |
| BR-54 | Mapset attributes: `MODE=INOUT`, `LANG=COBOL`, `STORAGE=AUTO`, `TIOAPFX=YES`, colour/highlight extended attributes; all data fields protected except `OPTION`, `ACCTIN`, `HISAIN`. | `INQSET.bms:2-3, 18, 28, 58` |
| BR-55 | DB2 access for `PINQ` goes through `DB2ENTRY(PORTDB2)`: plan `PORTPLAN`, `AUTHTYPE(USERID)` (DB2 authorization id = CICS user), `PROTECTNUM(5)` protected threads, priority HIGH. | `PORTDFN.csd:82-94` |

**Business rule count: 55** (BR-01 … BR-55; one marked INFERRED FROM DOCS: BR-36).

---

## 3. Data contracts

Scale/rounding: COBOL `MOVE` between numeric fields truncates (no rounding). No arithmetic is performed anywhere in S4 sources, so the only fidelity concern is representation (see BR-28).

### 3.1 `INQCOM.cpy` — Inquiry COMMAREA (`INQCOM-AREA`, 98 bytes)

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `INQCOM-FUNCTION` | X(4) | Requested function; 88s: `MENU`, `INQP`, `INQH`, `EXIT` | `enum InquiryFunction { MENU, PORTFOLIO("INQP"), HISTORY("INQH"), EXIT }` | REST route (no persistence) |
| `INQCOM-ACCOUNT-NO` | X(10) | Account being inquired | `String` (10, right-space-padded) | Path variable `accountNo`; S3 position key / `POSHIST.ACCOUNT_NO` (CHAR(8) in DDL — see D-13) |
| `INQCOM-RESPONSE-CODE` | S9(8) COMP | CICS RESP (INQPORT) or SQLCODE (INQHIST) of the last failure; 0 = ok | `int` → mapped to HTTP status + S8 `ReturnCode` | none |
| `INQCOM-ERROR-MSG` | X(80) | Human-readable error text | `String` (≤80) | Error DTO `message` |

### 3.2 `DB2REQ.cpy` — DB2 request area (`DB2-REQUEST-AREA`, 109 bytes)

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `DB2-REQUEST-TYPE` | X | 88s: `C` connect, `D` disconnect, `S` status | `enum Db2Request { CONNECT, DISCONNECT, STATUS }` | Retired — DataSource/HikariCP |
| `DB2-RESPONSE-CODE` | S9(8) COMP | 0 ok / −1 fail / active-count (status) | `int` | Retired |
| `DB2-CONNECTION-TOKEN` | X(16) | `YYYYMMDDhhmmssff` timestamp prefix | `String` | Retired (connection identity is pool-managed) |
| `DB2-SQLCODE` | S9(9) COMP | Last SQLCODE | `int` | `DataAccessException` (S8) |
| `DB2-ERROR-MSG` | X(80) | `SQLERRMC` or literal | `String` | Exception message |

### 3.3 `ERRHND.cpy` — Online error area (`ERROR-HANDLING`, 210 bytes)

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `ERR-PROGRAM` | X(8) | Originating program | `String` | `ERRLOG.PROGRAM_ID` CHAR(8) |
| `ERR-PARAGRAPH` | X(30) | Originating paragraph (or cursor name) | `String` | no DDL column (ERRLOG has `ADDITIONAL_INFO` VARCHAR(500) — D-11) |
| `ERR-SQLCODE` | S9(9) COMP | DB2 SQLCODE | `Integer` | `ERRLOG.ERROR_CODE` CHAR(8) (type mismatch) |
| `ERR-CICS-RESP` | S9(8) COMP | EIBRESP | `Integer` | no DDL column |
| `ERR-CICS-RESP2` | S9(8) COMP | EIBRESP2 (never logged) | `Integer` | none |
| `ERR-SEVERITY` | X | 88s: `F` fatal, `W` warning, `I` info | `enum ErrorSeverity { FATAL, WARNING, INFO }` | `ERRLOG.ERROR_SEVERITY` INTEGER (type mismatch) |
| `ERR-MESSAGE` | X(80) | Message; overwritten with formatted text | `String` (≤80 legacy; target ≤200) | `ERRLOG.ERROR_MESSAGE` VARCHAR(200) |
| `ERR-ACTION` | X | 88s: `R` return, `C` continue, `A` abend | `enum ErrorAction { RETURN, CONTINUE, ABEND }` | none (drives control flow) |
| `ERR-TRACE-ID` | X(16) | Correlation id | `String` (target: W3C trace id from Micrometer Tracing) | no DDL column |
| `ERR-TIMESTAMP` | X(26) | `CURRENT-DATE` text | `OffsetDateTime` | `ERRLOG.ERROR_TIMESTAMP` TIMESTAMP |

### 3.4 Working-storage contracts that cross program boundaries (LINK COMMAREAs, not copybooks)

**`CURSOR-REQUEST-AREA`** (`CURSMGR.cbl:28-41`, `INQHIST.cbl:43-50`, 3267 bytes)

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `CURS-REQUEST-TYPE` | X | `D`/`O`/`F`/`C` | `enum CursorOp` | Retired — Spring Data `Pageable` |
| `CURS-NAME` | X(18) | Cursor name (`HISTORY_CURSOR`) | — | Retired |
| `CURS-STMT` | X(240) | SQL text | — | Repository query method |
| `CURS-ARRAY-FETCH` | X | `Y` = 20-row array fetch, `N` = single | `int pageSize` (legacy 20 fetch / 10 display) | `Pageable.getPageSize()` |
| `CURS-RESPONSE-CODE` | S9(8) COMP | SQLCODE or 0 | `int` | exception mapping |
| `CURS-DATA-AREA` | X(3000) | Fetched rows, raw | `List<HistoryRow>` | DTO |
| `CURS-DATA-LENGTH` | S9(4) COMP | Bytes used (never set) | — | `Page.getNumberOfElements()` |

**`RECOVERY-REQUEST-AREA`** (`DB2RECV.cbl:31-45`, 122 bytes)

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `RECV-REQUEST-TYPE` | X | `C` connection, `T` transaction, `R` cursor | `enum RecoveryKind` | Retired — Resilience4j retry / Spring `@Transactional` rollback |
| `RECV-RESPONSE-CODE` | S9(8) COMP | 0 / −1 | `int` | — |
| `RECV-SQLCODE` | S9(9) COMP | Failing SQLCODE | `Integer` | — |
| `RECV-PROGRAM` | X(8) | Caller | `String` | log MDC |
| `RECV-CURSOR` | X(18) | Cursor name | `String` | log MDC |
| `RECV-MESSAGE` | X(80) | Error text | `String` | — |
| `RECV-STATUS` | X | 88s `S` success, `F` failed, `R` retry | `enum RecoveryStatus { SUCCESS, FAILED, RETRY }` | — |

**`WS-SECURITY-REQUEST`** (`INQONLN.cbl:27-33`, 109 bytes) — outbound to S5

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `SEC-REQUEST-TYPE` | X | `V` validate, `A` authorize, `L` audit | — | Spring Security filter chain / `@PreAuthorize` / audit event |
| `SEC-USER-ID` | X(8) | CICS user id | `String` | JWT `sub`/`preferred_username` |
| `SEC-RESOURCE-NAME` | X(8) | `INQONLN` | `String` constant `"inquiry-api"` | S5 permission name |
| `SEC-ACCESS-TYPE` | X(8) | `READ` | `String` constant `"READ"` | S5 permission action |
| `SEC-RESPONSE-CODE` | S9(8) COMP | 0 / 8 / 12 | `int` | 401 (12 credentials) / 403 (8) / 500 (12 audit) |
| `SEC-ERROR-INFO` | X(80) | Message | `String` | error DTO |

**`WS-HISTORY-TABLE`** (`INQHIST.cbl:20-26`, 10 × 32 = 320 bytes) — the history-row DTO

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `WS-TRANS-DATE` | X(10) | Transaction date as text (`YYYY-MM-DD` DB2 DATE ISO) | `LocalDate` | `POSHIST.TRANS_DATE` DATE |
| `WS-TRANS-TYPE` | X(4) | Transaction type code | `String` (DDL is CHAR(2); enum deferred to S2/S3 spec) | `POSHIST.TRANS_TYPE` CHAR(2) |
| `WS-TRANS-UNITS` | S9(9)V99 COMP-3 | Units traded | `BigDecimal` scale 2 (DDL `QUANTITY` DECIMAL(15,3) → scale 3 in target; see Q-05) | `POSHIST.QUANTITY` |
| `WS-TRANS-PRICE` | S9(9)V99 COMP-3 | Unit price | `BigDecimal` scale 2 (DDL `PRICE` DECIMAL(15,3)) | `POSHIST.PRICE` |
| `WS-TRANS-AMOUNT` | S9(9)V99 COMP-3 | Gross amount | `BigDecimal` scale 2 | `POSHIST.AMOUNT` DECIMAL(15,2) |

### 3.5 `POSREC.cpy` — Position record as consumed by `INQPORT` (138 bytes; CSD says 200 — D-14)

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `POS-PORTFOLIO-ID` | X(08) | Portfolio identifier (key 1) | `String` | S3 `INVESTMENT_POSITIONS` / `POSMSTRE` key |
| `POS-DATE` | X(08) | Position date YYYYMMDD (key 2) | `LocalDate` (`BASIC_ISO_DATE`) | S3 |
| `POS-INVESTMENT-ID` | X(10) | Investment/fund identifier (key 3) | `String` | S3; displayed in `FUNDOUT` (6 chars — D-15) |
| `POS-QUANTITY` | S9(11)V9(4) COMP-3 | Holding quantity | `BigDecimal` scale 4 | `UNITOUT` |
| `POS-COST-BASIS` | S9(13)V9(2) COMP-3 | Total cost basis | `BigDecimal` scale 2 | `COSTOUT` |
| `POS-MARKET-VALUE` | S9(13)V9(2) COMP-3 | Current market value | `BigDecimal` scale 2 | `VALOUT` |
| `POS-CURRENCY` | X(03) | ISO currency | `String`/`java.util.Currency` | not displayed |
| `POS-STATUS` | X(01) | 88s `A` active, `C` closed, `P` pending | `enum PositionStatus { ACTIVE, CLOSED, PENDING }` | not displayed / not filtered |
| `POS-LAST-MAINT-DATE` | X(26) | Audit timestamp | `OffsetDateTime` | not displayed |
| `POS-LAST-MAINT-USER` | X(08) | Audit user | `String` | not displayed |
| `POS-FILLER` | X(50) | — | — | — |

Note: `POSMAP` has a `NAMEOUT` Fund Name X(30) field with **no source field** in `POSREC` or any S4 program (D-15).

### 3.6 BMS symbolic fields (DTO shapes for the optional UI)

| Map | Field | Len | Attr | Purpose |
|---|---|---|---|---|
| `MENMAP` | `OPTION` | 1 | UNPROT,NUM,IC | menu choice 1/2/3 |
| `MENMAP` | `ERRMSG` | 78 | PROT,BRT,RED | error line |
| `POSMAP` | `ACCTIN` | 10 | UNPROT,IC | account input |
| `POSMAP` | `FUNDOUT` / `NAMEOUT` / `UNITOUT` / `COSTOUT` / `VALOUT` | 6/30/15/15/15 | PROT,TURQ | position fields |
| `POSMAP` | `POSMSG` | 78 | PROT,BRT,RED | error line |
| `HISMAP` | `HISAIN` | 10 | UNPROT,IC | account input |
| `HISMAP` | `ROW1`–`ROW10` | 65 | PROT,TURQ | preformatted history lines |
| `HISMAP` | `HISMSG` | 78 | PROT,BRT,RED | error line |
| `ERRMAP` | `ERRCOUT` / `ERRDOUT` | 8/65 | PROT,RED | error code / details |

---

## 4. Interfaces

### 4.1 Inbound

| Legacy | Detail | Target |
|---|---|---|
| CICS transaction `PINQ` → `INQONLN` | 3270 terminal, conversational | `inquiry-api` REST endpoints (§6); optional UI |
| `RECEIVE MAP INQMAP/INQSET` | screen input → `INQCOM` | HTTP request (path/query params) |
| LINK into `INQPORT`, `INQHIST` (COMMAREA `INQCOM`) | internal | in-process service calls |
| LINK into `CURSMGR`, `DB2ONLN`, `DB2RECV`, `ERRHNDL` | internal plumbing | retired (library calls to Spring/S8) |

### 4.2 Outbound

| Caller → Target | Mechanism | Data | Target component | Becomes |
|---|---|---|---|---|
| `INQONLN` → `SECMGR` (`V`,`A`,`L`) | `EXEC CICS LINK` | `WS-SECURITY-REQUEST` | **S5 auth-service** | OIDC bearer-token validation (`V`), `@PreAuthorize("hasAuthority('inquiry:read')")` (`A`), audit **event** `AccessAudited` published via S8 audit publisher (`L`) |
| `INQONLN` → `INQPORT` / `INQHIST` | `EXEC CICS LINK` | `INQCOM` | S4 (internal) | library call |
| `INQONLN`, `DB2RECV` → `ERRHNDL` | `EXEC CICS LINK` | `ERRHND` | **S8 platform-common** | library call: exception → `ErrorLogger` (writes `ERRLOG`) + `@ControllerAdvice` |
| `INQPORT` → VSAM `POSFILE` (DSN `PORTFOLIO.POSITION.VSAM`, DDNAME/FCT `POSFILE`) | `EXEC CICS READ` by key | `POSREC` | **S3 position-service** | **REST call** `GET /positions/{accountNo}` (S3 owns `INVESTMENT_POSITIONS`/`POSHIST`) |
| `INQHIST` → DB2 `POSHIST` | dynamic `SELECT … FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC` via `CURSMGR` | 5 columns | **S3 position-service** (owner of `POSHIST`) | **REST call** `GET /positions/{accountNo}/history?page=&size=` (paginated; replaces cursor) |
| `INQHIST` → `DB2ONLN` (`C`) | LINK | `DB2REQ` | retired | DataSource/HikariCP inside S3 (S4 owns no DB) |
| `INQHIST` → `DB2RECV` (`C`) | LINK | recovery area | retired | Resilience4j `@Retry(maxAttempts=3, waitDuration=2s)` on the S3 client — preserves BR-44 semantics |
| `INQHIST` → `CURSMGR` (`D`,`O`,`F`,`C`) | LINK | cursor area | retired | Spring Data `Pageable`/`Page` on the S3 side |
| `DB2ONLN` → DB2 | `CONNECT TO POSMVP`, `DISCONNECT`, `SELECT CURRENT SERVER` | — | retired | pool health via Actuator `db` health indicator |
| `DB2RECV` → DB2 | `ROLLBACK` | — | retired | Spring `@Transactional` rollback |
| `DB2RECV` → CICS | `DELAY INTERVAL(2)` | — | retired | retry backoff |
| `ERRHNDL` → DB2 `ERRLOG` | `INSERT` (8 values) | `ERRHND` | **S8 platform-common** | **DB write** inside S8 library (S8 owns `ERRLOG`) |
| `INQONLN` → CICS | `ABEND ABCODE('IERR')` | — | S8 | HTTP 500 + `FatalErrorException` |
| Screens | `SEND MAP MENMAP/POSMAP/HISMAP` (`ERRMAP` unused) | symbolic maps | S4 UI (optional) | JSON DTOs / Thymeleaf or React views |

### 4.3 Files, tables, JCL

- Files: `POSFILE` (read only). No sequential files, no DDNAMEs beyond the FCT entry.
- DB2 tables: `POSHIST` (read), `ERRLOG` (write via `ERRHNDL`). Plan `PORTPLAN` via `DB2ENTRY PORTDB2`. (`AUTHFILE`, `AUDITLOG` are touched only by `SECMGR` → S5.)
- JCL: **none** — no JCL job references any S4 program.

---

## 5. Discrepancies (code vs. code, code vs. documentation)

| # | Discrepancy | Evidence |
|---|---|---|
| D-01 | `INQONLN` evaluates `WS-COMMAREA-FUNCTION`; `INQCOM.cpy` defines `INQCOM-FUNCTION`. Field does not exist → compile error. | `INQONLN.cbl:62`, `INQCOM.cpy:5` |
| D-02 | `INQONLN` uses map names `INQMAP` (receive) and `INQMNU` (send); `INQSET.bms` defines only `MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP`. | `INQONLN.cbl:56, 93`, `INQSET.bms:7,23,53,89` |
| D-03 | Undefined data names: `WS-ERROR-MESSAGE` (`INQONLN.cbl:84,135`), `WS-DB2-TOKEN` (`INQHIST.cbl:125`), `WS-COMMAREA-ACCOUNT-NO` and `POSITION-ACCOUNT` (`INQPORT.cbl:69-70,74`). | as cited |
| D-04 | Menu map collects a 1-digit `OPTION`; dispatcher expects 4-char `MENU/INQP/INQH/EXIT`. No translation exists; the menu cannot drive the dispatcher as written. | `INQSET.bms:18`, `INQONLN.cbl:62-77` |
| D-05 | `INQPORT` reads `POSFILE` with an account number (X(10)) as key; `POSREC` key is portfolio-id+date+investment-id (26 bytes). The lookup contract (which record for an account? which date?) is undefined. | `INQPORT.cbl:69-74`, `POSREC.cpy:7-10` |
| D-06 | `INQPORT` `EXEC SQL INCLUDE SQLPOS` — member `SQLPOS` does not exist anywhere in the repo; `WS-DB2-POSITION` is never used. Header claims "Handles VSAM and DB2 access"; only VSAM is accessed. | `INQPORT.cbl:7, 20-21` |
| D-07 | `P150-DB2-CONNECT` performs itself recursively on successful recovery; recursive PERFORM is undefined behavior in COBOL. | `INQHIST.cbl:113-115` |
| D-08 | `INQHIST` SQL selects `TRANS_UNITS, TRANS_PRICE, TRANS_AMOUNT`; `POSHIST.sql` has `QUANTITY, PRICE, AMOUNT` (DECIMAL(15,3)/(15,3)/(15,2)) and `TRANS_TYPE CHAR(2)` vs host X(4); `ACCOUNT_NO CHAR(8)` vs commarea X(10). `data-dictionary.md §3.1` describes a *third* `POSHIST` layout (`ACCOUNT_NO DECIMAL(9,0)`, `FUND_ID`, `SHARE_BAL`, `COST_BASIS`, `AVG_COST`) with no transaction-level columns at all. | `INQHIST.cbl:130-134`, `POSHIST.sql:25-43`, `data-dictionary.md:186-201` |
| D-09 | `CURSMGR` uses host variables for cursor name and statement in `DECLARE CURSOR`/`OPEN`; not valid embedded SQL (requires `PREPARE` + `DECLARE … FOR stmt-name`). | `CURSMGR.cbl:70-72, 84` |
| D-10 | `DB2ONLN` connection-pool counters are in WORKING-STORAGE of a non-resident LINKed program; the 100-connection cap and token counter are not persistent. SAD describes "connection pooling"; none exists. | `DB2ONLN.cbl:18-22`, `PORTDFN.csd:35-40`, SAD §1.2.6 |
| D-11 | `ERRHNDL` `INSERT INTO ERRLOG VALUES (8 values)` vs `ERRLOG.sql` (10 columns: `ERROR_TIMESTAMP, PROGRAM_ID, ERROR_TYPE, ERROR_SEVERITY INTEGER, ERROR_CODE CHAR(8), ERROR_MESSAGE VARCHAR(200), PROCESS_DATE, PROCESS_TIME, USER_ID, ADDITIONAL_INFO`) vs `data-dictionary.md §3.2` (7 columns incl. `ACCOUNT_NO`, `FUND_ID`, `TRANS_ID`, `ERROR_DESC`). Three incompatible layouts. | `ERRHNDL.cbl:73-83`, `ERRLOG.sql:15-26`, `data-dictionary.md:203-217` |
| D-12 | `ERRHNDL` has no `DEFINE PROGRAM` in `PORTDFN.csd` although LINKed by `INQONLN` and `DB2RECV`; `SECMGR` is defined. `ERRHNDL.cbl` is also written in free-format (column 1) unlike every other program (fixed-format, column 8). | `PORTDFN.csd:13-61`, `ERRHNDL.cbl:1-2` |
| D-13 | COMMAREA layout: code `INQCOM` (`X(4)` function, `X(10)` account, `S9(8) COMP` rc, `X(80)` msg) vs `data-dictionary.md §4.1` `INQUERY-COMMAREA` (`X(1)` function P/H, `9(9)` account, `X(6)` fund id, `X(2)` rc, `X(50)` message). Fund ID is a documented input that is not implemented. | `INQCOM.cpy`, `data-dictionary.md:222-231` |
| D-14 | `POSFILE RECORDSIZE(200)` vs `POSREC` length 138 bytes. | `PORTDFN.csd:77`, `POSREC.cpy` |
| D-15 | `POSMAP` fields `FUNDOUT` (6) and `NAMEOUT` Fund Name (30) have no source: `POS-INVESTMENT-ID` is X(10) and no fund-name field exists in any S4 source. `INQPORT` also defines `WS-MAP-FIELDS` labels that are never used (labels are already `INITIAL=` in the map). | `INQSET.bms:33,36`, `POSREC.cpy:10`, `INQPORT.cbl:29-34` |
| D-16 | `CURSMGR.cbl` is truncated: `P300-FETCH-DATA` and `P400-CLOSE-CURSOR` are PERFORMed but not defined; file ends mid-paragraph. Fetch/close = missing program logic. | `CURSMGR.cbl:51-56, 90` |
| D-17 | SAD §1.2.6 describes `CURSMGR` as *screen* cursor / PF-key / field-selection handling; the code is a *DB2* cursor manager. SAD §5.1.4 lists `CURSMGR` resources as "BMS Maps" (it uses none). | SAD lines 218-223, 530 |
| D-18 | SAD §5.1.4 dependency table vs code: `INQONLN` actual LINKs = `SECMGR, INQPORT, INQHIST, ERRHNDL` (doc: `SECMGR, CURSMGR`); `INQPORT` actual = none (doc: `DB2ONLN, ERRHNDL`); `INQHIST` actual = `DB2ONLN, DB2RECV, CURSMGR` (doc: `DB2ONLN, ERRHNDL`); `DB2ONLN` calls nothing (doc: `DB2RECV`). SAD §5.2.2 shows `INQPORT/INQHIST → SECMGR` and `→ CURSMGR` for both; code has neither `INQPORT→CURSMGR` nor any `→SECMGR` from sub-programs. | SAD lines 525-534, 571-590 |
| D-19 | SAD §1.2.6 claims `INQPORT` "shows portfolio summaries / handles portfolio search" and `INQHIST` "supports date range queries / displays audit information"; none is implemented (single key read; fixed 10-row query, no date predicate). SAD §2.5 has `CUR-->>USR: Show Results` (CURSMGR sends screens) — false. | SAD lines 203-215, 366-378 |
| D-20 | Screens advertise `PF3=Exit PF7=Previous PF8=Next` but no program inspects `EIBAID`; `INQHIST` has scrolling flags (`MORE-ROWS`, `WS-ROW-COUNT`) that are never updated. `ERRMAP` is never sent. | `INQSET.bms:47-48, 83-84, 89-99`, `INQHIST.cbl:30-33` |
| D-21 | Security is checked *after* the requested function has already executed (BR-08), contradicting SAD §2.5 (`INQ->>SEC: Validate Access` before dispatch). | `INQONLN.cbl:62-88`, SAD lines 362-364 |
| D-22 | `INQHIST` array fetch requests 20 rows (`WS-MAX-ROWS`) but the display table holds 10; `CURS-DATA-AREA` (3000 bytes) → `WS-HISTORY-TABLE` (320 bytes) truncates. | `CURSMGR.cbl:24`, `INQHIST.cbl:20-26, 172` |
| D-23 | `DB2ONLN` status request overwrites the success/failure code with the connection count (BR-42); `DB2RECV` connection-recovery never sets `RECV-MESSAGE` so `INQHIST` displays a blank error (BR-24/BR-44). | `DB2ONLN.cbl:117-118`, `DB2RECV.cbl:62-83` |
| D-24 | Decomposition plan names the menu map `INQMNU`; the BMS map is `MENMAP` (plan inherited the code's wrong name). | plan §2 row S4, `INQSET.bms:7` |

**Missing or stub programs / members** (for structured output): `CURSMGR` (truncated — fetch/close paragraphs absent), `SQLPOS` (DB2 include referenced by `INQPORT`, not in repo), `ERRHNDL` (source present but no CSD definition and non-compilable format), BMS maps `INQMAP`/`INQMNU` (referenced, not defined).

---

## 6. Proposed Java design

Stateless Spring Boot 3 / Java 21 service; **no database of its own**. Spring MVC (blocking) is sufficient — the two upstream calls per request are sequential and low-fan-out; WebFlux is not required.

### 6.1 Package layout (`com.cog.portfolio.inquiry`)

```
inquiry-api/
  api/            InquiryController (position, history), MenuController (optional UI), dto/
  application/    PositionInquiryService, HistoryInquiryService
  client/         PositionServiceClient (S3, HTTP interface client), PortfolioServiceClient (S1, optional fund name)
  security/       ResourceAuthorizationConfig ("inquiry:read"), AccessAuditPublisher
  error/          InquiryExceptionHandler (@ControllerAdvice) → S8 ErrorLogger / ProblemDetail
  config/         ClientProperties, RetryConfig, OpenApiConfig
  ui/ (optional)  Thymeleaf templates menu.html, position.html, history.html, error.html
```

### 6.2 REST contract (replaces `PINQ`/`INQCOM`)

| Legacy | Endpoint | Response |
|---|---|---|
| `MENU` | `GET /api/v1/inquiry/menu` (UI only) | static option list 1/2/3 |
| `INQP` | `GET /api/v1/inquiry/accounts/{accountNo}/position` | `PositionDto { accountNo, portfolioId, positionDate, investmentId, fundName?, quantity(scale 4), costBasis(2), marketValue(2), currency, status }` |
| `INQH` | `GET /api/v1/inquiry/accounts/{accountNo}/history?page=0&size=10` | `Page<HistoryRowDto { transDate, transType, units, price, amount }>` — default `size=10` (BR-27), max 20 (BR-33 array size); ordered `transDate DESC` |
| `EXIT` | none (stateless) | — |
| errors | RFC 9457 `ProblemDetail { status, title, detail, traceId, legacyResponseCode }` | `detail` carries legacy text (`Position not found for account`, `Error accessing position data`) |

Status mapping: not-found → 404 (BR-18); S3/S1 failure → 502 with legacy message (BR-19); `SECMGR` 8 → 403, 12 → 401/500 (BR-10); fatal/unloggable → 500 (BR-12).

### 6.3 Spring components

- `InquiryController` — validates `accountNo` (`@Size(max=10)`, non-blank; numeric-ness is an open question Q-01) and delegates.
- `PositionInquiryService` — calls `PositionServiceClient.getCurrentPosition(accountNo)`; optionally enriches `fundName` from `PortfolioServiceClient` (Q-03).
- `HistoryInquiryService` — calls `PositionServiceClient.getHistory(accountNo, pageable)`; enforces size ≤ 20.
- `PositionServiceClient` / `PortfolioServiceClient` — Spring `@HttpExchange` interface clients; `@Retry(name="s3", maxAttempts=3, waitDuration=2s)` + `@CircuitBreaker` (Resilience4j) — direct replacement for `DB2RECV` BR-44.
- Security — `SecurityFilterChain` with OAuth2 resource server (S5/Keycloak); `.requestMatchers("/api/v1/inquiry/**").hasAuthority("inquiry:read")`; **authorization before handler execution** (fixes D-21). `AccessAuditPublisher` publishes `AccessAudited{userId, resource="inquiry-api", accessType="READ", terminal/trace}` via S8's audit publisher (replaces `SECMGR 'L'`).
- `InquiryExceptionHandler` — maps exceptions to `ProblemDetail`; calls S8 `ErrorLogger.log(program="INQONLN"|..., severity, message, traceId)` which persists to `ERRLOG` (S8-owned) and formats `Error in <svc> - <msg> (<traceId>)` (BR-50). Severity policy mirrors BR-51: `FATAL→500 & alert`, `WARNING/INFO→continue`. `traceId` from Micrometer Tracing replaces `FUNCTION RANDOM`.
- Actuator `health` (S3 reachability) replaces `DB2ONLN 'S'`.

### 6.4 Entities / events

- **Entities: none** (S4 owns no tables).
- **Events published**: `AccessAudited` (from `SECMGR 'L'` semantics). **Consumed**: none.
- **DTOs** (see §3.4–3.6): `PositionDto`, `HistoryRowDto`, `ProblemDetail` extension.

### 6.5 Configuration

```yaml
inquiry:
  clients:
    position-service.base-url: http://position-service:8080
    portfolio-service.base-url: http://portfolio-service:8080
  history.default-page-size: 10      # BR-27
  history.max-page-size: 20          # BR-33
resilience4j.retry.instances.s3: { max-attempts: 3, wait-duration: 2s }   # BR-44
spring.security.oauth2.resourceserver.jwt.issuer-uri: ${KEYCLOAK_ISSUER}
```

### 6.6 Optional UI

Thymeleaf pages mirroring `MENMAP`, `POSMAP`, `HISMAP`, `ERRMAP` (10 rows/page, Prev/Next buttons replacing PF7/PF8, Exit replacing PF3). Field widths from §3.6 become max display lengths, not storage constraints.

### 6.7 Architecture review

Per the plan, `inquiry-api` is a new service; F3 (Phase 1) drafts the decomposition ADR and runs ARB triage for all new services. This document is analysis only and introduces no infrastructure.

---

## 7. Open questions

| # | Question | Why it matters |
|---|---|---|
| Q-01 | What is the **account number** format — X(10) alphanumeric (`INQCOM`), 9(9) numeric (data dictionary), or CHAR(8) (`POSHIST.sql`)? Should the API validate numeric-only? | Path-variable validation, S3 contract (D-08, D-13) |
| Q-02 | How does an account map to a **position record**? `POSREC` is keyed by portfolio-id + date + investment-id; the code passes an account number (D-05). Is the intended result "latest position date for the account's portfolio, one row per investment" (i.e. a list), or a single row? | Shape of `GET …/position` (single vs. list) and S3 endpoint |
| Q-03 | **Fund name** (`NAMEOUT`) has no data source. Should S4 enrich from S1 portfolio-service / a fund master, or drop the field? | Extra outbound dependency |
| Q-04 | Should the **Fund ID** input from the documented commarea (`INQ-FUND-ID`, D-13) be supported as an optional filter on position and history endpoints? | API surface |
| Q-05 | Which **`POSHIST` schema** is authoritative (DDL with `QUANTITY/PRICE` DECIMAL(15,3), code with `TRANS_UNITS/PRICE/AMOUNT` scale 2, or data dictionary with `SHARE_BAL/AVG_COST`)? If DDL, history units/price must be scale 3 and the legacy scale-2 display truncation is a behavior change to approve. | F1 canonical model, BigDecimal scale |
| Q-06 | Is **paging** (PF7/PF8) required in the target? Legacy code shows only the first 10 rows and never scrolls (D-20); the screens promise navigation. | Endpoint design, parity test I2 |
| Q-07 | **Date-range filter** on history (SAD claims it, code lacks it) — required? | API surface |
| Q-08 | Should the **security check order** be corrected (authorize before executing the inquiry, D-21)? Assumed yes in §6.3; confirm no one depends on "execute then audit". | Security posture / parity |
| Q-09 | Is **transaction type** (`TRANS_TYPE`) a closed set to expose as an enum (CHAR(2) in DDL vs X(4) in code)? Defer to S2/S3 spec owners. | DTO typing |
| Q-10 | Should the target keep the **3-attempts / 2-second** upstream retry from `DB2RECV` (BR-44) or adopt platform defaults? | Latency budget of the BFF |
| Q-11 | `ERRLOG` write on every inquiry error (BR-48) — keep as a DB write via S8, or downgrade to structured logging with `ERRLOG` reserved for batch? | S8 library contract, DB load |
| Q-12 | Abend semantics (BR-12/BR-49): when error logging itself fails, legacy abends the CICS task. Target: fail the request (500) only, or also raise an operational alert? | S8 `FATAL` policy |
| Q-13 | Is the optional 3270-lookalike UI in scope for Phase 2, or is `inquiry-api` JSON-only? | Effort, package layout `ui/` |
