# S1 — portfolio-service: Phase 0 Analysis Spec

Component: **S1 portfolio-service** (COBOL → Java / Spring Boot modernization, Phase 0).

Sources analyzed (every file read in full):

| Kind | File | Lines |
|---|---|---|
| Program (called subprogram) | `src/programs/portfolio/PORTMSTR.cbl` | 288 |
| Program (batch) | `src/programs/portfolio/PORTADD.cbl` | 148 |
| Program (batch) | `src/programs/portfolio/PORTDEL.cbl` | 194 |
| Program (batch) | `src/programs/portfolio/PORTREAD.cbl` | 111 |
| Program (batch) | `src/programs/portfolio/PORTUPDT.cbl` | 160 |
| Program (called subprogram) | `src/programs/portfolio/PORTVALD.cbl` | 120 |
| Copybook | `src/copybook/common/PORTFLIO.cpy` | 34 |
| Copybook | `src/copybook/common/PORTVAL.cpy` | 46 |
| JCL | `src/jcl/portfolio/PORTADD.jcl`, `PORTDEF.jcl`, `PORTDEL.jcl`, `PORTREAD.jcl`, `PORTUPDT.jcl` | 16/33/17/15/16 |
| DDL | `src/database/db2/db2-definitions.sql` (`PORTFOLIO_MASTER`, lines 10–24, 67–68, 82–86) | 105 |
| Context only | `documentation/technical/system-architecture.md`, `documentation/technical/data-dictionary.md`, `documentation/technical/development-backlog.md`, `documentation/operations/test-data-specs.md`, `src/database/vsam/vsam-definitions.txt` | — |

Conventions used below: `file:line` references point at the COBOL source. Rules marked **INFERRED FROM DOCS** are not implemented in any analyzed program and come only from documentation. Nothing in this spec is invented; where behavior is absent or broken in the code it is stated as such.

---

## 1. Purpose & scope

The S1 component is the **portfolio master record lifecycle**: creating, reading, updating and deleting the master record that identifies a client portfolio (portfolio id + account number, client name/type, status, create/last-maintenance dates, total value and cash balance, last-user/last-transaction audit stamps).

In the legacy system this is implemented as five batch utilities plus one callable subprogram, all operating directly on the VSAM KSDS `PORTFOLIO.MASTER.FILE` (DDNAME `PORTFILE`):

| Program | Business function | Driver |
|---|---|---|
| `PORTADD` | Bulk-load new portfolios from a sequential input file; stamps create/last-maint dates; rejects records failing minimal validation; reports counts | JCL `PORTADD.jcl` |
| `PORTUPDT` | Apply single-field updates (status, client name, total value) from a sequential update-request file | JCL `PORTUPDT.jcl` |
| `PORTDEL` | Delete portfolios listed in a sequential deletion-request file (with reason code) and append an audit record per deletion | JCL `PORTDEL.jcl` |
| `PORTREAD` | Sequentially dump every portfolio record to SYSOUT and count them (inquiry/verification utility) | JCL `PORTREAD.jcl` |
| `PORTMSTR` | Callable CRUD subprogram (command `C/R/U/D`) with inline validation of id/name/status | none — no caller exists in the repo |
| `PORTVALD` | Callable field-validation subprogram (id / account / investment type / amount) | none — no caller exists in the repo |
| `PORTDEF.jcl` | IDCAMS job that (re)defines the VSAM cluster | operational |

The target is a **Spring Boot REST + JPA `portfolio-service`** owning table `PORTFOLIO_MASTER` (per the decomposition plan), exposing CRUD, a bulk-load/bulk-update/bulk-delete batch capability replacing the three sequential-file jobs, and the validation rules currently spread across `PORTMSTR`, `PORTVALD`, `PORTADD`.

Out of scope for S1 (belongs to other components): transaction posting and position math in `PORTTRAN` (S2/S3), test-data generation in `PORTTEST` (S10), `ERRPROC`/`AUDPROC` implementations (S8).

---

## 2. Business rules

Numbering is global (BR-01 … BR-58). Each rule cites the paragraph and lines that implement it. "RC" = COBOL `RETURN-CODE` / linkage return code.

### 2.1 Record layout & key rules (`PORTFLIO.cpy`)

| # | Rule | Ref |
|---|---|---|
| BR-01 | A portfolio is uniquely identified by the composite key `PORT-KEY` = `PORT-ID` (8 chars) + `PORT-ACCOUNT-NO` (10 chars) = 18 bytes; the VSAM cluster is defined with `KEYS(18 0)`. | `PORTFLIO.cpy:12-14`, `PORTDEF.jcl:30` |
| BR-02 | Client type is one of `I` (Individual), `C` (Corporate), `T` (Trust). No program validates this field; it is carried through as supplied. | `PORTFLIO.cpy:17-20` |
| BR-03 | Portfolio status domain per the shared record is `A` (Active), `C` (Closed), `S` (Suspended). (Other domains exist in `PORTMSTR` — see BR-33 and D-04.) | `PORTFLIO.cpy:24-27` |
| BR-04 | Dates `PORT-CREATE-DATE`, `PORT-LAST-MAINT`, `PORT-LAST-TRANS` are unsigned 8-digit numerics in `YYYYMMDD` form (established by `ACCEPT … FROM DATE YYYYMMDD` in `PORTADD`). | `PORTFLIO.cpy:22-23,33`, `PORTADD.cbl:89,122-123` |
| BR-05 | `PORT-TOTAL-VALUE` and `PORT-CASH-BALANCE` are signed packed decimals with 13 integer + 2 fraction digits (range ±9,999,999,999,999.99). No program in S1 computes them; they are stored as supplied (`PORTADD`) or overwritten verbatim (`PORTUPDT` action `V`). | `PORTFLIO.cpy:29-30` |

### 2.2 `PORTADD` — bulk create (`PORTADD.cbl`)

| # | Rule | Ref |
|---|---|---|
| BR-06 | Job processes the input file `INPTFILE` sequentially until end-of-file; each input record has the full `PORTFLIO` layout (input and master share the same copybook). | `PORTADD.cbl:29-40,79-80,104-111` |
| BR-07 | Files are opened `I-O PORTFILE`, `INPUT INPTFILE`. If either open fails (status ≠ `00`) the program displays `Error opening files: PORT=xx INPT=xx`, sets RC = 8 and performs termination. **Defect:** control then falls through to the main loop with the files closed (END-OF-FILE is never set) — the program does not stop cleanly. | `PORTADD.cbl:91-101,77-82` |
| BR-08 | An input record is **rejected** (error count +1, message `Invalid record data: <PORT-ID>`, record skipped) when any of: `PORT-ID = SPACES`, `PORT-CLIENT-NAME = SPACES`, `PORT-STATUS ≠ 'A'`. Only status `A` is accepted on creation. | `PORTADD.cbl:114-120` |
| BR-09 | No other fields are validated on create: account number, client type, dates, monetary fields, last-user/last-trans are written exactly as supplied. `PORTVALD` is **not** called. | `PORTADD.cbl:113-137` |
| BR-10 | `PORT-CREATE-DATE` and `PORT-LAST-MAINT` are both overwritten with the job run date (`YYYYMMDD`, obtained once at initialization). Any create date supplied in the input is discarded. | `PORTADD.cbl:89,122-123` |
| BR-11 | The record is written with `WRITE`: status `00` → added count +1; status `22` (duplicate key) → duplicate count +1 and `Duplicate record: <PORT-ID>`; any other status → error count +1 and `Write error for: <PORT-ID>`. Processing continues in all cases. | `PORTADD.cbl:125-136` |
| BR-12 | At termination the program displays `Records added`, `Duplicate records`, `Errors occurred` counts (each `9(7)`) and sets `RETURN-CODE` to `WS-RETURN-CODE`. | `PORTADD.cbl:139-148` |
| BR-13 | RC semantics: **RC 0 even if every record was rejected or duplicated**; RC 8 only on file-open failure. Rejections are visible only via DISPLAY output. | `PORTADD.cbl:72,99,147` |
| BR-14 | Counters are `PIC 9(7)`; more than 9,999,999 occurrences of any outcome would wrap silently (unsigned truncation). | `PORTADD.cbl:69-71` |

### 2.3 `PORTUPDT` — bulk single-field update (`PORTUPDT.cbl`)

| # | Rule | Ref |
|---|---|---|
| BR-15 | Update-request record (`UPDTFILE`, 69 bytes): `UPDT-ID X(8)` + `UPDT-ACCT-NO X(10)` (= master key), `UPDT-ACTION X(1)`, `UPDT-NEW-VALUE X(50)`. | `PORTUPDT.cbl:39-48` |
| BR-16 | Action codes: `S` = replace status, `V` = replace total value, `N` = replace client name. | `PORTUPDT.cbl:44-47` |
| BR-17 | For each request the master record is read by key. If the read does not return `00`, error count +1, `Record not found: <key>` is displayed and the request is skipped (any non-00 status, not only 23, is reported as "not found"). | `PORTUPDT.cbl:118-129` |
| BR-18 | Action `S`: `MOVE UPDT-NEW-VALUE TO PORT-STATUS` — only the **first character** of the 50-byte value is stored; the new status is **not validated** against any domain. | `PORTUPDT.cbl:133-134` |
| BR-19 | Action `N`: `MOVE UPDT-NEW-VALUE TO PORT-CLIENT-NAME` — value is **truncated to 30 characters** (left-justified, alphanumeric move). Blank name is not rejected. | `PORTUPDT.cbl:135-136` |
| BR-20 | Action `V`: the 50-byte alphanumeric value is moved to `WS-NUMERIC-WORK PIC S9(13)V99` and then to `PORT-TOTAL-VALUE`. COBOL alphanumeric→numeric MOVE treats the sender as an **unsigned integer**; the value is right-aligned on the integer part (fraction forced to `.00`), high-order characters beyond 13 digits are truncated, sign cannot be expressed, and non-digit characters (including trailing spaces) yield undefined/abend behavior. **The exact numeric semantics of `V` are therefore unreliable in the legacy code** — see OQ-3. | `PORTUPDT.cbl:80,137-139` |
| BR-21 | Any other action code makes no field change, but the record is **still rewritten** and counted as a successful update. | `PORTUPDT.cbl:132-145` |
| BR-22 | `PORT-LAST-MAINT`, `PORT-LAST-USER`, `PORT-LAST-TRANS` are **not** updated by `PORTUPDT`; no audit record is produced. | `PORTUPDT.cbl:131-150` |
| BR-23 | `REWRITE` status `00` → update count +1; otherwise error count +1 and `Update failed for: <key>`. | `PORTUPDT.cbl:142-149` |
| BR-24 | Termination displays `Updates processed` / `Errors occurred` and sets RC. RC 0 regardless of per-record errors; RC 8 only on open failure (same fall-through defect as BR-07). | `PORTUPDT.cbl:96-106,152-160` |

### 2.4 `PORTDEL` — bulk delete with audit (`PORTDEL.cbl`)

| # | Rule | Ref |
|---|---|---|
| BR-25 | Deletion-request record (`DELEFILE`, 80 bytes): `DEL-ID X(8)` + `DEL-ACCT-NO X(10)` (= master key), `DEL-REASON-CODE X(2)`, filler 60. | `PORTDEL.cbl:44-53` |
| BR-26 | Reason-code domain: `01` Closed, `02` Transferred, `03` Requested. The code is **not validated**; any value is accepted and copied to the audit record. | `PORTDEL.cbl:49-52,174` |
| BR-27 | Files: `I-O PORTFILE`, `INPUT DELEFILE`, `OUTPUT AUDFILE` (JCL `DISP=MOD` → audit file is appended across runs). Open failure of any → display, RC 8, terminate (same fall-through defect as BR-07). | `PORTDEL.cbl:114-127`, `PORTDEL.jcl:14` |
| BR-28 | For each request: read master by key. `00` → proceed to delete; `23` → not-found count +1 and `Record not found: <key>`; other → error count +1 and `Read error for: <key>`. | `PORTDEL.cbl:139-154` |
| BR-29 | Deletion is **unconditional** on the portfolio's status/balances: an `A`ctive portfolio with a non-zero total value or cash balance is deleted without check. Physical delete (VSAM `DELETE`), not a soft close. | `PORTDEL.cbl:156-166` |
| BR-30 | `DELETE` status `00` → delete count +1 and an audit record is written; else error count +1 and `Delete failed for: <key>`. | `PORTDEL.cbl:157-165` |
| BR-31 | Audit record (`AUDFILE`, 80 bytes): `AUD-TIMESTAMP X(26)` from `ACCEPT … FROM TIME STAMP` (non-standard verb; see D-12), `AUD-ACTION = 'DELETE'`, `AUD-KEY` = 18-byte key, `AUD-REASON` = request reason code, `AUD-STATUS` = the portfolio status **as it was before deletion**. Audit write failure only displays `Audit write failed for: <key>` — the deletion is not rolled back. | `PORTDEL.cbl:55-62,168-182` |
| BR-32 | Termination displays `Records deleted` / `Records not found` / `Errors occurred` and sets RC. RC 0 regardless of per-record outcomes; 8 only on open failure. | `PORTDEL.cbl:184-194` |

### 2.5 `PORTMSTR` — callable CRUD subprogram (`PORTMSTR.cbl`)

`PORTMSTR` uses its **own inline record layout**, not `PORTFLIO` (see D-01): `PORT-ID X(10)` key, `PORT-NAME X(50)`, `PORT-CREATE-DATE X(10)`, `PORT-STATUS X(1)`, `PORT-TOTAL-VALUE S9(13)V99 COMP-3`, filler 24.

| # | Rule | Ref |
|---|---|---|
| BR-33 | Interface: `LS-COMMAND X(1)` (`C` create, `R` read, `U` update, `D` delete), `LS-PORTFOLIO X(100)` (record in/out), `LS-RETURN-CODE S9(4) COMP`. Any other command → error text `Invalid command`, RC 8. | `PORTMSTR.cbl:72-101` |
| BR-34 | Initialization opens `PORTFILE` I-O; open failure → `Error opening Portfolio file`, RC 8, GOBACK. Run date is captured (`ACCEPT … FROM DATE YYYYMMDD`) but **never used**. | `PORTMSTR.cbl:103-113` |
| BR-35 | Validation (`2100-VALIDATE-PORTFOLIO`, applied on Create and Update): (a) `PORT-ID(1:4)` must equal `'PORT'` **and** `PORT-ID(5:5)` must be numeric → i.e. `PORT` + 5 digits, positions 10 unchecked; else `Invalid Portfolio ID format`. (b) `PORT-NAME ≠ SPACES` else `Portfolio Name is required`. (c) `PORT-STATUS ∈ {A, I, C}` else `Invalid Portfolio Status`. First failure wins (EXIT PARAGRAPH). All failures → RC 8. | `PORTMSTR.cbl:138-161` |
| BR-36 | Create: validate, then `WRITE`. Status `22` → `Portfolio ID already exists`; any other non-00 → `Error writing Portfolio record`. Both RC 8. | `PORTMSTR.cbl:115-136` |
| BR-37 | Read: record key taken from caller's `LS-PORTFOLIO`; on `00` the full record is returned in `LS-PORTFOLIO`; `23` → `Portfolio not found`; other → `Error reading Portfolio`; RC 8 on either. | `PORTMSTR.cbl:163-181` |
| BR-38 | Update: validate (BR-35), `REWRITE` without prior READ (legal in dynamic access). `23` → `Portfolio not found for update`; other non-00 → `Error updating Portfolio`. On success an audit call is performed (`2100-LOG-PORTFOLIO-UPDATE`: system `PORTFOLIO`, type `TRAN`, action `UPDATE`, status `SUCC`, before/after images, message `Portfolio updated successfully`, `CALL 'AUDPROC'`). Create and Delete do **not** audit. | `PORTMSTR.cbl:183-207,268-288` |
| BR-39 | Delete: `DELETE` by key from caller's record. `23` → `Portfolio not found for deletion`; other non-00 → `Error deleting Portfolio`; RC 8. No status/balance precondition. | `PORTMSTR.cbl:209-226` |
| BR-40 | Return code contract: only two values, `0` success / `8` error. The descriptive error text (`WS-ERROR-TEXT`) is working-storage only and is **never returned to the caller nor logged** — callers cannot distinguish error causes. `9000-ERROR` closes the file and GOBACKs immediately (single-error-exit). | `PORTMSTR.cbl:47-49,80,228-238` |
| BR-41 | VSAM error classification (`2100-HANDLE-VSAM-ERROR`, **never performed**): status `22` → severity WARNING (+4) text `Duplicate record key`; `23` → WARNING text `Record not found`; other → ERROR (+8) text `Unexpected VSAM error`; category `VS`; details = record key; `CALL 'ERRPROC'`. Dead code with undeclared data items (see D-07). | `PORTMSTR.cbl:243-263` |

### 2.6 `PORTVALD` — callable field validator (`PORTVALD.cbl`, `PORTVAL.cpy`)

| # | Rule | Ref |
|---|---|---|
| BR-42 | Interface: `LS-VALIDATE-TYPE X(1)` (`I` id, `A` account, `T` investment type, `M` amount), `LS-INPUT-VALUE X(50)`, `LS-RETURN-CODE S9(4) COMP`, `LS-ERROR-MSG X(50)`. Unknown type → RC 1 (`VAL-INVALID-ID`) and `Invalid validation type`. | `PORTVALD.cbl:20-50` |
| BR-43 | Return-code catalogue: 0 success, 1 invalid id, 2 invalid account, 3 invalid type, 4 invalid amount; messages `Invalid Portfolio ID format`, `Invalid Account Number format`, `Invalid Investment Type`, `Amount outside valid range`. On success `LS-ERROR-MSG = SPACES`. | `PORTVAL.cpy:11-29`, `PORTVALD.cbl:69-70` |
| BR-44 | Portfolio ID (type `I`): `LS-INPUT-VALUE(1:4)` must equal `'PORT'` and `LS-INPUT-VALUE(5:4)` must be numeric → **`PORT` + 4 digits** (conflicts with `PORTMSTR` BR-35 = 5 digits and docs = 5 digits; see D-05). **Defect:** the 4 digits are moved into `VAL-NUMERIC-CHECK PIC X(10)`, which pads with 6 spaces; `IS NUMERIC` on a space-padded field is false, so **every ID fails validation** as written. | `PORTVALD.cbl:52-71`, `PORTVAL.cpy:37,43` |
| BR-45 | Account number (type `A`): the **entire 50-byte** `LS-INPUT-VALUE` must be numeric and not all zeros. The comment says "10 numeric digits", but the code requires 50 digits (a 10-digit value with trailing spaces fails). Intended rule: 10 numeric digits, non-zero. | `PORTVALD.cbl:73-86` |
| BR-46 | Investment type (type `T`): value must be one of `STK`, `BND`, `MMF`, `ETF` (compared against the full 50-byte field, so the value must be left-justified and space-padded). No S1 record carries an investment type — this rule belongs to positions/transactions (S2/S3). | `PORTVALD.cbl:88-103` |
| BR-47 | Amount (type `M`): value moved alphanumeric→`S9(13)V99` (same unreliable move semantics as BR-20) and checked against `VAL-MIN-AMOUNT = -9,999,999,999,999.99` / `VAL-MAX-AMOUNT = +9,999,999,999,999.99`. Because those bounds equal the full range of the PIC, **the range check can never fail** for any value that survives the move. | `PORTVALD.cbl:105-120`, `PORTVAL.cpy:35-36,44` |
| BR-48 | `PORTVALD` is never CALLed by any program in the repository; its rules are currently **not enforced** anywhere. | repo-wide search |

### 2.7 `PORTREAD` — sequential dump (`PORTREAD.cbl`)

| # | Rule | Ref |
|---|---|---|
| BR-49 | Opens `PORTFILE` INPUT (dynamic access), reads `NEXT RECORD` in key order until EOF; open failure → display, RC 8 (same fall-through defect as BR-07). | `PORTREAD.cbl:74-93` |
| BR-50 | For each record displays sequence number, `PORT-ID`, `PORT-ACCOUNT-NO`, `PORT-CLIENT-NAME`, `PORT-STATUS`, `PORT-TOTAL-VALUE` (raw COMP-3 display — no editing) and a blank line; at end displays `Total Records Read`. Read-only; no filtering. | `PORTREAD.cbl:95-110` |

### 2.8 JCL / operational rules

| # | Rule | Ref |
|---|---|---|
| BR-51 | `PORTDEF` **deletes the existing cluster** (`DELETE … CLUSTER`, `SET MAXCC = 0` so absence is not an error) then defines `PORTFOLIO.MASTER.FILE` as KSDS, fixed `RECORDSIZE(200 200)`, `KEYS(18 0)`, `CYLINDERS(5 1)`, `FREESPACE(10 10)`, `SHAREOPTIONS(2 3)`. Re-running it wipes all portfolios. | `PORTDEF.jcl:18-33` |
| BR-52 | `PORTADD`/`PORTUPDT`/`PORTDEL`/`PORTREAD` jobs each run a single step, `PORTFILE DISP=SHR` (concurrent readers allowed; `SHAREOPTIONS(2 3)` permits one writer + readers). Input request files `INPTFILE`/`UPDTFILE`/`DELEFILE` are `DISP=OLD` (exclusive), `AUDFILE` is `DISP=MOD` (append). | `PORTADD.jcl:12-13`, `PORTUPDT.jcl:12-13`, `PORTDEL.jcl:12-14`, `PORTREAD.jcl:12` |
| BR-53 | No JCL exists for `PORTMSTR` or `PORTVALD` (they are subprograms); no job condition-code checks (`COND`/`IF`) are used, consistent with the RC-always-0 behavior in BR-13/24/32. | `src/jcl/portfolio/*` |

### 2.9 Rules present only in documentation — INFERRED FROM DOCS

| # | Rule | Source |
|---|---|---|
| BR-54 | **INFERRED FROM DOCS** — Portfolio name: 1–50 chars, allowed characters A–Z, 0–9, space, hyphen; must not be all spaces. (Code only checks ≠ SPACES.) | `test-data-specs.md §3.2` |
| BR-55 | **INFERRED FROM DOCS** — Create date must be a valid calendar date and not in the future. (No program validates dates.) | `test-data-specs.md §3.6` |
| BR-56 | **INFERRED FROM DOCS** — `PORT-TOTAL-VALUE` minimum 0.00 (non-negative). (Code range is ±9.99e12.) | `test-data-specs.md §3.5` |
| BR-57 | **INFERRED FROM DOCS** — Status change "Active → Inactive" is an expected update path; docs list status domain `A/I/C`. (Code: `PORTUPDT` accepts any single char; `PORTFLIO` defines `A/C/S`; `PORTMSTR` and DB2 notes differ — see D-04.) | `test-data-specs.md §2.1, §3.3`, `db2-definitions.sql:100` |
| BR-58 | **INFERRED FROM DOCS** — DB2 `ACTIVE_PORTFOLIOS` view semantics: a portfolio is "active" when `STATUS = 'A'` and (`CLOSE_DATE IS NULL` or `CLOSE_DATE > CURRENT DATE`). No COBOL program in S1 accesses DB2 at all. | `db2-definitions.sql:82-86` |

**Business rule count: 58** (50 from code, 8 doc-inferred).

---

## 3. Data contracts

### 3.1 `PORTFLIO.cpy` — Portfolio master record (VSAM `PORTFILE`, 148 bytes actual)

Computed length: key 18 + client 31 + portfolio-info 17 + financial 16 (two 8-byte COMP-3) + audit 16 + filler 50 = **148 bytes** (cluster is defined as 200 — see D-02).

| Field | PIC | Semantics | Java type | Target storage (`PORTFOLIO_MASTER` unless noted) |
|---|---|---|---|---|
| `PORT-ID` | `X(8)` | Portfolio identifier, key part 1. Validated as `PORT`+digits by `PORTMSTR`/`PORTVALD` (BR-35/44) | `String` (8) | `PORTFOLIO_ID CHAR(8)` (PK) |
| `PORT-ACCOUNT-NO` | `X(10)` | Client account number, key part 2 | `String` (10) | **no column** — closest is `CLIENT_ID CHAR(10)`; F1 must decide (OQ-1) |
| `PORT-CLIENT-NAME` | `X(30)` | Client / portfolio display name | `String` (≤30) | `PORTFOLIO_NAME VARCHAR(50)` |
| `PORT-CLIENT-TYPE` | `X(1)` 88s `I/C/T` | Individual / Corporate / Trust | `enum ClientType { INDIVIDUAL('I'), CORPORATE('C'), TRUST('T') }` | **no column** — new column required or map onto `ACCOUNT_TYPE CHAR(2)` (OQ-1) |
| `PORT-CREATE-DATE` | `9(8)` YYYYMMDD | Creation date, set by `PORTADD` to run date | `LocalDate` | `OPEN_DATE DATE` |
| `PORT-LAST-MAINT` | `9(8)` YYYYMMDD | Last maintenance date (set on create only in code) | `LocalDate` (DB2 side is `TIMESTAMP` → `LocalDateTime`) | `LAST_MAINT_DATE TIMESTAMP` |
| `PORT-STATUS` | `X(1)` 88s `A/C/S` | Lifecycle status | `enum PortfolioStatus { ACTIVE('A'), CLOSED('C'), SUSPENDED('S') }` — plus `INACTIVE('I')` only if OQ-2 confirms | `STATUS CHAR(1)` |
| `PORT-TOTAL-VALUE` | `S9(13)V99 COMP-3` | Total portfolio value | `BigDecimal` precision 15, **scale 2**, `RoundingMode.DOWN` (COBOL truncation) | **no column** — value is derived in target from S3 positions or needs a new column (OQ-4) |
| `PORT-CASH-BALANCE` | `S9(13)V99 COMP-3` | Cash balance | `BigDecimal` scale 2, `RoundingMode.DOWN` | **no column** (OQ-4) |
| `PORT-LAST-USER` | `X(8)` | Last maintaining user id | `String` (8) | `LAST_MAINT_USER VARCHAR(8)` |
| `PORT-LAST-TRANS` | `9(8)` | Last transaction date (YYYYMMDD) — naming suggests id, PIC says date | `LocalDate` | **no column** (S2 `TRANSACTION_HISTORY` reference instead) |
| `PORT-FILLER` | `X(50)` | unused | — | dropped |

### 3.2 `PORTMSTR` inline record (VSAM `PORTFILE` per `PORTMSTR.cbl:31-39`, declared 100 / actual 103 bytes)

| Field | PIC | Semantics | Java type | Maps to |
|---|---|---|---|---|
| `PORT-ID` | `X(10)` | Key (10-char: `PORT`+5 digits + 1 unchecked) | `String` | `PORTFOLIO_ID CHAR(8)` — **length mismatch** (D-01) |
| `PORT-NAME` | `X(50)` | Portfolio name, required | `String` (≤50) | `PORTFOLIO_NAME VARCHAR(50)` |
| `PORT-CREATE-DATE` | `X(10)` | Date as text (docs sample `YYYY-MM-DD`) | `LocalDate` (ISO parse) | `OPEN_DATE DATE` |
| `PORT-STATUS` | `X(1)`, valid `A/I/C` | Status | `PortfolioStatus` | `STATUS CHAR(1)` |
| `PORT-TOTAL-VALUE` | `S9(13)V99 COMP-3` | Total value | `BigDecimal` scale 2 | see OQ-4 |
| `FILLER` | `X(24)` | unused | — | dropped |

### 3.3 `PORTUPDT` update-request record (`UPDTFILE`, 69 bytes)

| Field | PIC | Semantics | Java type |
|---|---|---|---|
| `UPDT-ID` | `X(8)` | Portfolio id | `String` |
| `UPDT-ACCT-NO` | `X(10)` | Account no | `String` |
| `UPDT-ACTION` | `X(1)` 88s `S/V/N` | Field selector | `enum UpdateAction { STATUS('S'), TOTAL_VALUE('V'), CLIENT_NAME('N') }` |
| `UPDT-NEW-VALUE` | `X(50)` | New value (interpretation depends on action, BR-18–20) | `String`; converted to `PortfolioStatus` / `String(30)` / `BigDecimal(scale 2)` per action |

### 3.4 `PORTDEL` deletion-request record (`DELEFILE`, 80 bytes)

| Field | PIC | Semantics | Java type |
|---|---|---|---|
| `DEL-ID` | `X(8)` | Portfolio id | `String` |
| `DEL-ACCT-NO` | `X(10)` | Account no | `String` |
| `DEL-REASON-CODE` | `X(2)` 88s `01/02/03` | Closed / Transferred / Requested | `enum DeleteReason { CLOSED("01"), TRANSFERRED("02"), REQUESTED("03") }` |
| `DEL-FILLER` | `X(60)` | unused | — |

### 3.5 `PORTDEL` audit record (`AUDFILE`, 80 bytes) → S8 audit event

| Field | PIC | Semantics | Java type / target |
|---|---|---|---|
| `AUD-TIMESTAMP` | `X(26)` | DB2-style timestamp text | `Instant`/`LocalDateTime` → `AuditEvent.occurredAt` |
| `AUD-ACTION` | `X(6)` | constant `DELETE` | `AuditAction.DELETE` |
| `AUD-KEY` | `X(18)` | portfolio key | `portfolioId`, `accountNo` |
| `AUD-REASON` | `X(2)` | reason code | `DeleteReason` |
| `AUD-STATUS` | `X(1)` | status before deletion | `PortfolioStatus` |
| `AUD-FILLER` | `X(27)` | unused | — |

### 3.6 `PORTVAL.cpy` — validation constants

| Field | PIC / value | Java |
|---|---|---|
| `VAL-SUCCESS/INVALID-ID/INVALID-ACCT/INVALID-TYPE/INVALID-AMT` | `S9(4)` = 0/1/2/3/4 | `enum ValidationCode { SUCCESS(0), INVALID_ID(1), INVALID_ACCOUNT(2), INVALID_TYPE(3), INVALID_AMOUNT(4) }` with message text from `VAL-ERROR-MESSAGES` |
| `VAL-MIN-AMOUNT` / `VAL-MAX-AMOUNT` | `S9(13)V99` = ∓9,999,999,999,999.99 | `BigDecimal` constants, scale 2 |
| `VAL-ID-PREFIX` | `X(4)` = `PORT` | `String` constant |
| `VAL-NUMERIC-CHECK X(10)`, `VAL-TEMP-NUM S9(13)V99`, `VAL-ERROR-CODE S9(4)`, `VAL-ERROR-MSG X(50)` | work areas | not carried over |

### 3.7 `PORTMSTR`/`PORTVALD` linkage areas

| Area | Fields | Java equivalent |
|---|---|---|
| `LS-COMMAND-AREA` (`PORTMSTR.cbl:73-80`) | `LS-COMMAND X(1)` (`C/R/U/D`), `LS-PORTFOLIO X(100)`, `LS-RETURN-CODE S9(4) COMP` | REST verbs POST/GET/PUT/DELETE on `/portfolios`; RC → HTTP status |
| `LS-VALIDATION-REQUEST` (`PORTVALD.cbl:21-29`) | `LS-VALIDATE-TYPE X(1)` (`I/A/T/M`), `LS-INPUT-VALUE X(50)`, `LS-RETURN-CODE`, `LS-ERROR-MSG X(50)` | `PortfolioValidator` methods + Bean Validation constraint violations |

### 3.8 `PORTFOLIO_MASTER` (DB2, `db2-definitions.sql:10-24`) — target table

| Column | Type | Source in COBOL | Java |
|---|---|---|---|
| `PORTFOLIO_ID` | `CHAR(8)` PK | `PORT-ID` (8) | `String` |
| `ACCOUNT_TYPE` | `CHAR(2)` | none (vsam-definitions.txt lists it as key part 2) | `String` / enum — OQ-1 |
| `BRANCH_ID` | `CHAR(2)` | none | `String` — OQ-1 |
| `CLIENT_ID` | `CHAR(10)` | possibly `PORT-ACCOUNT-NO` | `String` — OQ-1 |
| `PORTFOLIO_NAME` | `VARCHAR(50)` | `PORT-CLIENT-NAME` (30) / `PORT-NAME` (50) | `String` |
| `CURRENCY_CODE` | `CHAR(3)` | none | `String` (ISO 4217) — OQ-1 |
| `RISK_LEVEL` | `CHAR(1)` | none | enum TBD — OQ-1 |
| `STATUS` | `CHAR(1)` (`A/C/S` per note line 100) | `PORT-STATUS` | `PortfolioStatus` |
| `OPEN_DATE` | `DATE` | `PORT-CREATE-DATE` | `LocalDate` |
| `CLOSE_DATE` | `DATE` nullable | none | `LocalDate` |
| `LAST_MAINT_DATE` | `TIMESTAMP` | `PORT-LAST-MAINT` (date only) | `LocalDateTime` |
| `LAST_MAINT_USER` | `VARCHAR(8)` | `PORT-LAST-USER` | `String` |
| Index `IDX_PORT_MASTER_CLIENT (CLIENT_ID, STATUS)`; view `ACTIVE_PORTFOLIOS` | — | — | JPA index + derived query |

---

## 4. Interfaces

### 4.1 Inbound

| Legacy interface | Program | Target (per decomposition plan) | Becomes |
|---|---|---|---|
| `CALL 'PORTMSTR' USING LS-COMMAND-AREA` — no caller exists | `PORTMSTR` | portfolio-service `PortfolioController` | REST: `POST/GET/PUT/DELETE /api/v1/portfolios/{id}` |
| `CALL 'PORTVALD' USING LS-VALIDATION-REQUEST` — no caller exists | `PORTVALD` | portfolio-service `PortfolioValidator` (also exportable to S2 for investment-type check) | Library call (Bean Validation); optionally `POST /api/v1/portfolios:validate` |
| JCL `PORTADD` + `INPTFILE` (`PORTFOLIO.INPUT.FILE`, `PORTFLIO` layout) | `PORTADD` | portfolio-service Spring Batch job `portfolioBulkLoadJob` | Batch: `FlatFileItemReader` (fixed-length) → validate → JPA write; or `POST /api/v1/portfolios:bulk` |
| JCL `PORTUPDT` + `UPDTFILE` (`PORTFOLIO.UPDATE.FILE`, 69-byte layout) | `PORTUPDT` | Spring Batch job `portfolioBulkUpdateJob` | Batch → `PATCH /api/v1/portfolios/{id}` semantics |
| JCL `PORTDEL` + `DELEFILE` (`PORTFOLIO.DELETE.FILE`, 80-byte layout) | `PORTDEL` | Spring Batch job `portfolioBulkDeleteJob` | Batch → `DELETE /api/v1/portfolios/{id}?reason=` |
| JCL `PORTREAD` | `PORTREAD` | `GET /api/v1/portfolios` (paged) / Actuator-style dump task | REST read |
| JCL `PORTDEF` (IDCAMS) | — | Flyway migration `V1__portfolio_master.sql` (F1) | DB schema migration; destructive re-define is **not** carried over |

### 4.2 Outbound

| Legacy interface | Program / ref | Target | Becomes |
|---|---|---|---|
| VSAM `PORTFILE` (`PORTFOLIO.MASTER.FILE`) READ/WRITE/REWRITE/DELETE/READ NEXT | all six programs | `PORTFOLIO_MASTER` via `PortfolioRepository` (JPA) | DB read/write (Spring `@Transactional`) |
| Sequential `AUDFILE` (`PORTFOLIO.AUDIT.FILE`, DISP=MOD) WRITE | `PORTDEL.cbl:177` | S8 platform-common audit publisher (`AUDITLOG`/`AUDPROC` equivalent) | Event: `PortfolioDeleted` audit event |
| `CALL 'AUDPROC' USING LS-AUDIT-REQUEST` (update audit) | `PORTMSTR.cbl:287` | S8 audit publisher | Library call → `PortfolioUpdated` audit event |
| `CALL 'ERRPROC' USING LS-ERROR-REQUEST` (dead code) | `PORTMSTR.cbl:262` | S8 exception hierarchy / `ReturnCode` | Library call (exception mapping) |
| `DISPLAY` messages & counters to SYSOUT | all batch programs | SLF4J logging + Micrometer counters (`portfolio.bulk.added/duplicate/error/…`) | Logging/metrics |
| `RETURN-CODE` 0/8 | all batch programs | Spring Batch `ExitStatus`; REST HTTP status (400 validation, 404 not found, 409 duplicate, 500 other) | Return code mapping |
| SQL statements | **none** — no `EXEC SQL` in any S1 program; `PORTFOLIO_MASTER` is never touched by COBOL | — | Target table is adopted by design, not by translation |
| CICS LINK/XCTL | **none** — S1 programs are batch/callable only | — | — |
| Consumers of S1 data in other components: `PORTTRAN` (S2) reads/updates `PORTFILE` by `PORT-ID`; `INQPORT` (S4) reads portfolio data via DB2 | — | S2 → REST call or `PortfolioStatusChanged` event subscription; S4 → REST read of S1 | REST / event |

---

## 5. Discrepancies (code vs. code, code vs. docs)

| # | Discrepancy | Evidence |
|---|---|---|
| D-01 | **Two incompatible master record layouts.** `PORTMSTR` declares an inline 103-byte record with a 10-byte `PORT-ID` key, 50-byte name, text date and no account/client-type/cash/audit fields; `PORTADD/UPDT/DEL/READ` use `PORTFLIO.cpy` (148 bytes, 18-byte composite key). Both `ASSIGN TO PORTFILE`. `PORTMSTR`-written records would be unreadable by the others. `test-data-specs.md §1.1` documents the `PORTMSTR` layout, not the copybook. | `PORTMSTR.cbl:22-39` vs `PORTFLIO.cpy`, `PORTDEF.jcl:29-30`, `test-data-specs.md:7-15` |
| D-02 | **Record-length mismatches.** `PORTMSTR` FD says `RECORD CONTAINS 100` but fields total 103 bytes (COMP-3 `S9(13)V99` = 8 bytes). `PORTFLIO` totals 148 bytes but `PORTDEF.jcl` defines `RECORDSIZE(200 200)`. `vsam-definitions.txt` documents the same file (`PORTMSTR`) as 400 bytes with a 12-byte key (`Portfolio ID 8 + Account Type 2 + Branch ID 2`). Three different physical definitions. | `PORTMSTR.cbl:32`, `PORTDEF.jcl:29-30`, `vsam-definitions.txt:10-25` |
| D-03 | **DB2 `PORTFOLIO_MASTER` shares only 4–5 concepts with the VSAM record.** Table has `ACCOUNT_TYPE`, `BRANCH_ID`, `CLIENT_ID`, `CURRENCY_CODE`, `RISK_LEVEL`, `CLOSE_DATE` with no COBOL source; VSAM has `PORT-ACCOUNT-NO`, `PORT-CLIENT-TYPE`, `PORT-TOTAL-VALUE`, `PORT-CASH-BALANCE`, `PORT-LAST-TRANS` with no column. No S1 program (or any program in the repo) issues SQL against `PORTFOLIO_MASTER`. | `db2-definitions.sql:10-24`, `PORTFLIO.cpy` |
| D-04 | **Four status domains.** `PORTFLIO` + DB2 note: `A/C/S`; `PORTMSTR` validation: `A/I/C`; `test-data-specs.md §3.3`: `A/I/C`; `PORTADD` accepts only `A`; `PORTUPDT` accepts anything. | `PORTFLIO.cpy:24-27`, `db2-definitions.sql:100`, `PORTMSTR.cbl:58-59`, `PORTADD.cbl:116`, `PORTUPDT.cbl:134` |
| D-05 | **Portfolio ID format disagrees.** `PORTMSTR`: `PORT`+5 digits (positions 5–9 of a 10-char id). `PORTVALD` comment and code: `PORT`+4 digits (positions 5–8). Docs: `PORT`+5 digits, example `PORT00001` (9 chars — fits neither the 8-byte `PORTFLIO` id nor cleanly the 10-byte `PORTMSTR` id). `PORTFLIO.PORT-ID` is only 8 bytes → at most `PORT`+4. | `PORTMSTR.cbl:142-143`, `PORTVALD.cbl:54,56-62`, `test-data-specs.md:87-90`, `PORTFLIO.cpy:13` |
| D-06 | **`PORTVALD` ID check is unsatisfiable** (4 digits moved into `X(10)` → space-padded → `IS NUMERIC` false). Account check requires 50 digits instead of the commented 10. Amount range check can never fail. | `PORTVALD.cbl:62-63,77,111-112`, `PORTVAL.cpy:35-36,43` |
| D-07 | **`PORTMSTR` would not compile.** Paragraphs `2100-HANDLE-VSAM-ERROR` and `2100-LOG-PORTFOLIO-UPDATE` reference undeclared items (`LS-PROGRAM-ID`, `LS-CATEGORY`, `LS-ERROR-CODE`, `LS-SEVERITY`, `LS-ERROR-TEXT`, `LS-ERROR-DETAILS`, `LS-ERROR-REQUEST`, `ERR-CAT-VSAM`, `ERR-VSAM-*`, `ERR-WARNING`, `ERR-ERROR`, `ERR-OTHER`, `WS-FILE-STATUS`, `PORT-KEY`, `LS-AUDIT-REQUEST`, `LS-SYSTEM-ID`, `LS-USER-ID`, `USERID`, `TERMINAL-ID`, `LS-*` audit fields, `PORT-ACCOUNT-NO`, `WS-BEFORE-IMAGE`, `PORT-RECORD`); no `COPY ERRHAND` / `COPY AUDITLOG`. `2100-HANDLE-VSAM-ERROR` is also never performed. The comments label both as "Example … call" — they are template stubs. | `PORTMSTR.cbl:240-288` |
| D-08 | **`PORTADD` would not compile as written.** Both FDs `COPY PORTFLIO`, producing two `01 PORT-RECORD` groups with identical subordinate names; unqualified references (`READ INPUT-FILE INTO PORT-RECORD`, `PORT-ID`, `WRITE PORT-RECORD`) are ambiguous. | `PORTADD.cbl:36-40,105,114-125` |
| D-09 | **Open-failure fall-through** in `PORTADD`, `PORTUPDT`, `PORTDEL`, `PORTREAD`: after `PERFORM 3000-TERMINATE` on open error, control returns and the main `PERFORM … UNTIL END-OF-FILE` loop runs against closed files. | `PORTADD.cbl:94-101`, `PORTUPDT.cbl:99-106`, `PORTDEL.cbl:118-127`, `PORTREAD.cbl:78-82` |
| D-10 | **Return codes never reflect data errors.** All four batch jobs exit RC 0 after any number of rejected/duplicate/not-found records; JCL has no `COND` checks. `data-dictionary.md §8.2` return-code semantics (per S6/S8) are not applied here. | BR-13/24/32; `src/jcl/portfolio/*.jcl` |
| D-11 | **Docs claim features not implemented.** `development-backlog.md` marks for PORTMSTR: account-number validation, investment-type validation, amount range checks, "VSAM status code handling", "Audit trail logging", "Transaction logging / User activity tracking / System event recording" as done. In code: `PORTMSTR` validates only id/name/status; account/type/amount rules live only in the never-called, defective `PORTVALD`; audit exists only for update (stub) and delete (`PORTDEL` flat file). | `development-backlog.md:127-154` |
| D-12 | **Non-standard verb** `ACCEPT WS-TIMESTAMP FROM TIME STAMP` (`TIME STAMP` is not an Enterprise COBOL `ACCEPT … FROM` option; standard forms are `DATE`, `DAY`, `DAY-OF-WEEK`, `TIME`). Timestamp content is therefore undefined. | `PORTDEL.cbl:169` |
| D-13 | **`PORTUPDT` does not maintain audit fields** (`PORT-LAST-MAINT`, `PORT-LAST-USER`) despite DB2 note "All tables include audit fields (LAST_MAINT_DATE, LAST_MAINT_USER)" and the backlog "User activity tracking". | `PORTUPDT.cbl:131-150`, `db2-definitions.sql:97` |
| D-14 | **`PORTMSTR` and `PORTVALD` have no callers** anywhere in `src/` (only a comment in `RETHND.cpy:60` names `PORTMSTR` as an example MODULE-ID). The "CRUD subprogram" and "validation subroutine" are effectively dead code; the live behavior is the four batch programs. | repo-wide search |
| D-15 | **`system-architecture.md` does not describe the portfolio programs** beyond listing the `programs/portfolio/` and `jcl/portfolio/` folders; `data-dictionary.md` has no Portfolio Master layout at all (§2 covers TRANFILE, POSMSTRE, TRANHIST, BCHCTL, PRCCTL only). The only documented layout (`test-data-specs.md §1.1`) matches `PORTMSTR`'s inline record, not the copybook. | `system-architecture.md:393-415`, `data-dictionary.md:39-183` |
| D-16 | `PORTMSTR` captures the run date (`WS-CURRENT-DATE`) and never uses it; create date comes from the caller unvalidated, while `PORTADD` forces it to the run date. Inconsistent create-date ownership. | `PORTMSTR.cbl:112,119`, `PORTADD.cbl:122` |
| D-17 | `PORTREAD` displays a `COMP-3` field directly (`DISPLAY PORT-TOTAL-VALUE`), producing packed bytes rather than a readable number. | `PORTREAD.cbl:101` |
| D-18 | `PORTFLIO.PORT-LAST-TRANS` is `9(8)` (a date shape) although the name and `AUDITLOG`/data-dictionary conventions (`TRANS-ID X(12)`) suggest a transaction identifier. | `PORTFLIO.cpy:33`, `data-dictionary.md:26` |

Missing / stub programs relevant to S1: none of the six listed programs is empty, but `PORTMSTR` (non-compiling template stubs, no caller) and `PORTVALD` (no caller, unsatisfiable checks) are effectively **stubs**. No JCL exists to exercise them.

---

## 6. Proposed Java design

Module: `services/portfolio-service` (Spring Boot 3.x, Java 21, JPA/Hibernate, Spring Batch, depends on `libs/domain-model` (F1) and `libs/platform-common` (F2)).

### 6.1 Package layout

```
com.cog.portfolio
├── api                      # REST layer
│   ├── PortfolioController          GET/POST/PUT/PATCH/DELETE /api/v1/portfolios
│   ├── PortfolioBulkController      POST /api/v1/portfolios:bulk-load|bulk-update|bulk-delete (launches batch jobs)
│   ├── dto                          PortfolioRequest, PortfolioResponse, PortfolioPatchRequest,
│   │                                BulkJobStatusResponse, ValidationErrorResponse
│   └── ApiExceptionHandler          maps domain exceptions → HTTP (400/404/409/500) + platform-common ReturnCode
├── domain
│   ├── Portfolio                    aggregate (JPA entity, table PORTFOLIO_MASTER)
│   ├── PortfolioId                  value object (8-char, PORT+digits)
│   ├── PortfolioStatus              enum A/C/S (+I pending OQ-2)
│   ├── ClientType                   enum I/C/T
│   ├── DeleteReason                 enum 01/02/03
│   ├── UpdateAction                 enum S/V/N
│   ├── ValidationCode               enum 0..4 (PORTVAL return codes + messages)
│   └── event                        PortfolioCreated, PortfolioUpdated, PortfolioStatusChanged, PortfolioDeleted
├── service
│   ├── PortfolioService             create/read/update/delete + status transition (BR-33..40 semantics)
│   ├── PortfolioValidator           id/name/status/account/amount rules (BR-08, BR-35, BR-43..47 corrected)
│   └── PortfolioAuditPublisher      adapter to platform-common audit publisher (replaces AUDPROC/AUDFILE)
├── repository
│   └── PortfolioRepository          JpaRepository<Portfolio, String>; findByClientIdAndStatus, findAllActive
├── batch                            Spring Batch replacements for the JCL jobs
│   ├── load    PortfolioLoadJobConfig, PortfolioFixedLengthReader (PORTFLIO layout), PortfolioLoadProcessor, PortfolioLoadWriter
│   ├── update  PortfolioUpdateJobConfig, UpdateRequestReader (69-byte layout), UpdateRequestProcessor
│   ├── delete  PortfolioDeleteJobConfig, DeleteRequestReader (80-byte layout), DeleteRequestProcessor
│   ├── export  PortfolioExportTasklet (PORTREAD equivalent, CSV/JSON to a file or log)
│   └── BulkRunSummaryListener       added/duplicate/notFound/error counters → log + Micrometer + ExitStatus
└── config
    ├── PortfolioServiceProperties   (@ConfigurationProperties "portfolio.*")
    ├── BatchConfig, JpaConfig, SecurityConfig (S5 resource-server)
    └── OpenApiConfig
```

### 6.2 Spring components & behaviors

- **Entity `Portfolio`** — columns per §3.8; `@Version` optimistic lock replaces VSAM record locking; `lastMaintDate/lastMaintUser` set by a JPA `@PreUpdate`/`@PrePersist` listener (fixes D-13); `openDate` defaulted to `LocalDate.now(clock)` on create (BR-10). Monetary fields, if retained (OQ-4), are `@Column(precision=15, scale=2)` `BigDecimal` with `setScale(2, RoundingMode.DOWN)` applied at the boundary to mirror COBOL truncation.
- **`PortfolioService`** — `@Transactional`; `create` throws `DuplicatePortfolioException` (→409, legacy status 22), `get/update/delete` throw `PortfolioNotFoundException` (→404, status 23); `update` publishes `PortfolioUpdated` (BR-38 audit) and `PortfolioStatusChanged` when status differs; `delete(id, reason)` publishes `PortfolioDeleted{portfolioId, accountNo, reason, statusBefore, occurredAt}` (BR-31). Deletion precondition (active/non-zero balance) is configurable, default off to preserve BR-29 until OQ-5 is decided.
- **`PortfolioValidator`** — implements the corrected intent of BR-35/44/45: id = `PORT` + N digits (N fixed by OQ-2), name non-blank (and doc charset rule BR-54 behind a feature flag), status in enum, account = 10 digits non-zero, amount within ±9,999,999,999,999.99 with scale ≤ 2. Exposed both as Bean Validation `ConstraintValidator`s on DTOs and as a plain service for batch processors. `ValidationCode` retains the legacy 0–4 codes/messages for parity tests (S10).
- **Batch jobs** — one `Job` per legacy JCL (`portfolioLoadJob`, `portfolioUpdateJob`, `portfolioDeleteJob`, `portfolioExportJob`), chunk-oriented with `skip` policy so per-record failures are counted rather than fatal (BR-11/17/28). `BulkRunSummaryListener` writes the three legacy counters to the log and to Micrometer; `ExitStatus` is `COMPLETED` when errors = 0, else `COMPLETED_WITH_ERRORS` (departure from BR-13 RC-0 behavior — see OQ-6). Readers use `FixedLengthTokenizer` with the byte offsets from §3.1/3.3/3.4 so existing mainframe extracts can be replayed (EBCDIC→UTF-8 and COMP-3 decoding via a small `CobolFieldConverter` in `libs/domain-model`).
- **Events** — published via platform-common's publisher (Kafka/Rabbit chosen by F3): `PortfolioCreated`, `PortfolioUpdated`, `PortfolioStatusChanged`, `PortfolioDeleted`. S2 subscribes to `PortfolioStatusChanged`/`PortfolioDeleted` to reject transactions on closed/deleted portfolios; S7 consumes `PortfolioDeleted` for audit reporting.
- **REST contract** — `POST /portfolios` (201/400/409), `GET /portfolios/{id}` (200/404), `GET /portfolios?clientId=&status=&page=` (uses `IDX_PORT_MASTER_CLIENT`), `PUT /portfolios/{id}` (full replace, 200/400/404), `PATCH /portfolios/{id}` (body `{action: STATUS|TOTAL_VALUE|CLIENT_NAME, value}` mirroring `UPDTFILE`, or JSON Merge Patch), `DELETE /portfolios/{id}?reason=01|02|03` (204/404), `POST /portfolios:bulk-*` (202 + job id), `GET /portfolios/jobs/{id}`.
- **Config** — `portfolio.id.digits` (4|5), `portfolio.status.allowed` (A,C,S[,I]), `portfolio.delete.require-closed` (bool), `portfolio.bulk.exit-on-errors` (bool), `portfolio.audit.enabled`; datasource/Flyway from F1; Actuator health/metrics.

### 6.3 Persistence / schema notes for F1

Adopt `PORTFOLIO_MASTER` as the canonical table, add columns for the VSAM-only fields the business decides to keep (`ACCOUNT_NO CHAR(10)`, `CLIENT_TYPE CHAR(1)`, optionally `TOTAL_VALUE`/`CASH_BALANCE DECIMAL(15,2)`, `LAST_TRANS_DATE DATE`), and record the copybook→column mapping from §3.1. The audit flat file (`AUDFILE`) is replaced by S8's `AUDITLOG` store; `PORTDEF.jcl` is replaced by Flyway (no destructive re-define).

---

## 7. Open questions

| # | Question | Why it matters |
|---|---|---|
| OQ-1 | **Which record is canonical — VSAM `PORTFLIO` (id+account key, client type, total value, cash balance) or DB2 `PORTFOLIO_MASTER` (id-only key, account type, branch, client id, currency, risk level, close date)?** How should `PORT-ACCOUNT-NO` map (to `CLIENT_ID`, a new `ACCOUNT_NO` column, or dropped from the key)? Where do `ACCOUNT_TYPE`, `BRANCH_ID`, `CURRENCY_CODE`, `RISK_LEVEL` come from since no COBOL populates them? | Defines the entity, primary key and every API contract (D-01/D-03). |
| OQ-2 | **Portfolio ID format and status domain**: `PORT`+4 digits (8-char id, `PORTFLIO`/`PORTVALD`) or `PORT`+5 digits (10-char id, `PORTMSTR`/docs)? Status set `A/C/S` (copybook, DB2) or `A/I/C` (`PORTMSTR`, docs)? Is `I` (Inactive) distinct from `S` (Suspended)? | Validation rules BR-35/44 and the `PortfolioStatus` enum (D-04/D-05). |
| OQ-3 | **Intended semantics of `PORTUPDT` action `V`** (total value): what is the input format in `UPDTFILE` (implied 2 decimals? explicit decimal point? sign?), given the legacy alphanumeric→numeric MOVE is undefined for real inputs (BR-20)? Should total value be updatable at all, or derived from positions (S3)? | Determines whether `TOTAL_VALUE` exists on the entity and how `PATCH` parses it (OQ-4). |
| OQ-4 | Should `PORT-TOTAL-VALUE` and `PORT-CASH-BALANCE` be persisted on the portfolio (as in VSAM) or computed from S3 `INVESTMENT_POSITIONS`/S2 transactions (as the DB2 model implies)? If persisted, who owns writes (S1 via PATCH, or S3 via event)? | Data ownership boundary between S1 and S3. |
| OQ-5 | **Delete semantics**: keep physical delete with no preconditions (BR-29), or require status `C`losed / zero balances / no open positions, and/or switch to soft-delete (`STATUS='C'` + `CLOSE_DATE`) as the DB2 `ACTIVE_PORTFOLIOS` view suggests? Are reason codes `01/02/03` the complete list and should they be mandatory? | Referential integrity with `INVESTMENT_POSITIONS`/`TRANSACTION_HISTORY` FKs; audit requirements. |
| OQ-6 | **Bulk job exit behavior**: preserve legacy RC 0 regardless of rejected records (BR-13/24/32), or fail/warn the job when any record is rejected? What thresholds, if any? | Orchestration contract with S6 (`REQUIRED-RC` in `PRCCTL`). |
| OQ-7 | Should `PORTADD` continue to (a) accept only status `A` on create and (b) override any supplied create date with the run date, and should `PORTUPDT` be extended to validate new status values and maintain `LAST_MAINT_*` (currently not done — D-13)? | Fidelity vs. correctness choice for parity tests. |
| OQ-8 | Are the `PORTVALD` rules for **account number (10 digits, non-zero)** and **investment type (`STK/BND/MMF/ETF`)** required in S1, or do they belong exclusively to S2/S3? The amount range check is a no-op today — is a tighter business range (e.g., docs' minimum 0.00, BR-56) wanted? | Scope of `PortfolioValidator` and which service owns the investment-type enum. |
| OQ-9 | What audit coverage is required: legacy audits only deletes (flat file) and, nominally, updates (`PORTMSTR` stub); creates and reads are never audited. Should all mutations emit audit events, and must the `AUDFILE` 80-byte format be preserved for downstream consumers? | S8 event schema and S7 reporting. |
| OQ-10 | Who are the real callers of `PORTMSTR`/`PORTVALD` (none exist in the repo)? If an online CICS caller was intended, does S4 need synchronous CRUD endpoints beyond inquiry? | Confirms whether the REST CRUD surface is required in Phase 2 or only the batch jobs. |
