# Business-Rules Catalog

Every rule is tagged with where it is *enforced* (program + paragraph) and/or
*documented* (`documentation/technical/*.md`, copybook). Rules that exist only in
documentation, only in code, or that disagree between the two are flagged in the
**Status** column:

| Status | Meaning |
|---|---|
| `CODE` | enforced in source, not described in documentation |
| `DOC` | described in documentation, no enforcing code in the repo |
| `BOTH` | documented and enforced consistently |
| `CONFLICT` | documented and coded, but they disagree |
| `DEAD` | coded but unreachable (paragraph never performed / program never called) |

Rule IDs: `BR-P` portfolio, `BR-T` transaction, `BR-V` reusable validation,
`BR-A` audit/error, `BR-B` batch control, `BR-D` DB2 access, `BR-O` online,
`BR-R` reporting/validation utilities, `BR-S` structural/data.

## 1. Portfolio maintenance (`BR-P`)

| ID | Rule | Enforced in | Documented in | Status |
|---|---|---|---|---|
| BR-P01 | Portfolio ID must be `'PORT'` followed by digits (PORTVALD: 4 digits, positions 5–8; PORTMSTR: 5 digits, positions 5–9) | PORTVALD `1000-VALIDATE-ID`; PORTMSTR `2100-VALIDATE-PORTFOLIO` | `PORTVAL.cpy` (`VAL-ID-PREFIX`) | `CONFLICT` (4 vs 5 digits; PORTADD does not check format at all) |
| BR-P02 | Portfolio / client name is required (not spaces) | PORTADD `2100-VALIDATE-AND-ADD` (`PORT-CLIENT-NAME`); PORTMSTR `2100` (`PORT-NAME`) | — | `CODE` |
| BR-P03 | Portfolio status must be a known value. PORTADD requires `'A'` on insert; PORTMSTR accepts `A`/`I`/`C`; `PORTFLIO.cpy` defines `A`/`C`/`S`; `COMMON.cpy` defines `A`/`C`/`P`/`S`/`F`/`R`; data dictionary defines `A`/`C` | PORTADD `2100`; PORTMSTR `2100` | data-dictionary §2.2, `PORTFLIO.cpy`, `COMMON.cpy` | `CONFLICT` (four status vocabularies) |
| BR-P04 | Portfolio ID must not be spaces on add | PORTADD `2100` | — | `CODE` |
| BR-P05 | Adding a portfolio with an existing key is rejected (VSAM status 22 → "duplicate") | PORTADD `2100` (`WS-PORT-STATUS = '22'`) | `ERRHAND.cpy` `ERR-VSAM-DUPLICATE` | `BOTH` |
| BR-P06 | `PORT-CREATE-DATE` and `PORT-LAST-MAINT` are set to the current date on add; `PORT-LAST-MAINT` and `PORT-LAST-USER` updated by PORTMSTR on update | PORTADD `2100`; PORTMSTR `4000-UPDATE-PORTFOLIO` | — | `CODE` |
| BR-P07 | Update file action `S` replaces status, `N` replaces client name, `V` replaces total value (must be numeric); any other action is an error | PORTUPDT `2100-PROCESS-UPDATE`, `2200-APPLY-UPDATE` | — | `CODE` (no status/name validation on update – BR-P03 is bypassed) |
| BR-P08 | Delete requires the key to exist; a reason code `01` Closed / `02` Transferred / `03` Requested is carried to the audit line | PORTDEL `2100-PROCESS-DELETE`, `2200-DELETE-RECORD`, `2300-WRITE-AUDIT` | — | `CODE` (reason code is not validated, only copied) |
| BR-P09 | Every physical delete is audited (action, key, reason, prior status) | PORTDEL `2300-WRITE-AUDIT` | system-architecture "Audit Trail" | `BOTH` |
| BR-P10 | Portfolio master operations are `C` create / `R` read / `U` update / `D` delete; every successful C/U/D is audited through AUDPROC | PORTMSTR `0000-MAIN`, `2100-LOG-PORTFOLIO-UPDATE` | — | `CODE` (program has no caller) |

## 2. Transaction processing (`BR-T`)

| ID | Rule | Enforced in | Documented in | Status |
|---|---|---|---|---|
| BR-T01 | Transaction must reference a non-blank portfolio ID that exists on `PORTFILE` | PORTTRAN `2110-CHECK-PORTFOLIO` | data-dictionary §5.1 ("Account must be numeric and exist") | `BOTH` (keyed by portfolio, not account) |
| BR-T02 | Transaction type must be one of `BU`, `SL`, `TR`, `FE` | PORTTRAN `2120-CHECK-TRANSACTION-TYPE`; `TRNREC.cpy`, `COMMON.cpy` 88-levels | data-dictionary §5.1 (`E003 Invalid Transaction Type`) | `BOTH` |
| BR-T03 | Quantity must be > 0 for **all** types | PORTTRAN `2130-CHECK-AMOUNTS` | data-dictionary §5.1 "Buy/sell quantity cannot be zero" | `CONFLICT` (code applies it to fees and transfers too, and rejects negatives) |
| BR-T04 | Price and amount must be > 0 unless type is `TR` | PORTTRAN `2130-CHECK-AMOUNTS` | data-dictionary §5.1 "Buy/sell price must be positive", "Fee amount cannot be zero" | `BOTH` |
| BR-T05 | Buy: portfolio units += quantity, total cost += amount | PORTTRAN `2210-PROCESS-BUY` | data-dictionary §5.2 "Cost basis updates on every buy/sell" | `DEAD` (`2200-UPDATE-POSITIONS` is never performed; fields live in missing `PORTREC`) |
| BR-T06 | Sell: reject if units < quantity ("Insufficient units for sale"); otherwise units −= quantity, total cost −= amount | PORTTRAN `2220-PROCESS-SELL` | data-dictionary §5.2 "Share balance cannot become negative", `E004 Insufficient Position Balance` | `DEAD` |
| BR-T07 | Fee: total cost −= amount; units unchanged | PORTTRAN `2240-PROCESS-FEE` | — | `DEAD` |
| BR-T08 | Transfer: explicitly rejected – "Transfer processing not implemented" | PORTTRAN `2230-PROCESS-TRANSFER` | — | `DEAD` (and a functional gap) |
| BR-T09 | Average cost is recalculated on buys | — | data-dictionary §5.2 | `DOC` |
| BR-T10 | Transaction date cannot be in the future | — | data-dictionary §5.1 | `DOC` |
| BR-T11 | Fund must exist; position must be active for the transaction to apply | — | data-dictionary §5.1, §5.2 | `DOC` (no fund master or position lookup in any program) |
| BR-T12 | Processing stops when more than 100 transaction errors have occurred | PORTTRAN `0000-MAIN` (`WS-ERROR-COUNT > 100`) | `CKPRST.cpy` `CK-MAX-ERRORS 100` | `BOTH` |
| BR-T13 | Every applied transaction writes an audit trail record (`TRAN`/`UPDATE`) | PORTTRAN `2300-UPDATE-AUDIT-TRAIL` → AUDPROC | system-architecture "Audit Trail" | `DEAD` (reached only from `2200`) |
| BR-T14 | Transaction status lifecycle `P` Pending → `D` Done / `F` Failed / `R` Reversed | `TRNREC.cpy` 88-levels | — | `CODE` (defined, never set by any program) |

## 3. Reusable field validation (`BR-V`, program `PORTVALD`)

| ID | Rule | Enforced in | Status |
|---|---|---|---|
| BR-V01 | Account number must be 10 numeric digits and not all zeros | PORTVALD `2000-VALIDATE-ACCOUNT` | `CONFLICT` with data-dictionary `ACCOUNT-NO PIC 9(09)` (9 digits) |
| BR-V02 | Investment type must be `STK`, `BND`, `MMF` or `ETF` | PORTVALD `3000-VALIDATE-TYPE` | `CODE` (no field in any record carries this type) |
| BR-V03 | Amount must lie within ±9 999 999 999 999.99 | PORTVALD `4000-VALIDATE-AMOUNT`; `PORTVAL.cpy` `VAL-MIN/MAX-AMOUNT` | `CODE` |
| BR-V04 | Validation outcomes are 0 ok / 1 bad id / 2 bad account / 3 bad type / 4 bad amount with fixed messages | `PORTVAL.cpy` `VAL-RETURN-CODES`, `VAL-ERROR-MESSAGES` | `CODE` |

`PORTVALD` is not called by any program in the repo; these rules are available but
unused.

## 4. Error handling and audit (`BR-A`)

| ID | Rule | Enforced in | Documented in | Status |
|---|---|---|---|---|
| BR-A01 | Return-code scale: 0 success, 4 warning, 8 error, 12 severe, 16 critical/terminal | `COMMON.cpy`, `ERRHAND.cpy`, `RETHND.cpy`, `BCHCON.cpy`; every `MOVE … TO RETURN-CODE` | system-architecture, data-dictionary §5.3 | `BOTH` |
| BR-A02 | RTNCDE00 status classification: 0 success, 1–4 warning, 5–8 error, > 8 severe; highest code is retained across calls | RTNCDE00 `P200-SET-RETURN-CODE` | — | `CODE` |
| BR-A03 | Every error is logged with timestamp, program, category (`VS`/`VL`/`PR`/`SY`), 4-char code, severity, 80-char text, 256-char details | ERRPROC; `ERRHAND.cpy` | system-architecture "Error Handling" | `BOTH` |
| BR-A04 | Severity 12 ("severe") means abort processing, 16 means environment failure | — (ERRPROC only logs, displays and echoes the severity back as its return code; callers such as HISTLD00 `9000-ERROR-ROUTINE` roll back and continue the loop until BR-B08 trips) | system-architecture "Return code 12: abort" | `DOC` |
| BR-A05 | Standard error identifiers `E001 Invalid data`, `E002 Not found`, `E003 Duplicate`, `E004 File error`, `E005 DB2 error`, `E006 Timeout`, `E007 Auth`, `E008 Resource`, `E009 Sequence`, `E010 Checkpoint` | `RETHND.cpy` `STD-ERROR-CODES` | data-dictionary §5.4 defines `E001 Invalid Account`, `E002 Invalid Fund`, `E003 Invalid Trans Type`, `E004 Insufficient Balance`, `W001 Late Trans`, `W002 Price Variance` | `CONFLICT` |
| BR-A06 | Error action flags: `C` continue, `A` abort, `R` retry, max 3 retries | `RETHND.cpy`; DB2ERR `1100-SET-SEVERITY`; DB2RECV; `DBPROC.cpy` `DB2-MAX-RETRIES 3` | — | `CODE` |
| BR-A07 | Audit records carry type (`TRAN`/`USER`/`SYST`), action (`CREATE`…`SHUTDOWN`), status (`SUCC`/`FAIL`/`WARN`), before/after images, and are appended (OPEN EXTEND) never rewritten | AUDPROC; `AUDITLOG.cpy` | system-architecture "Audit Trail" | `BOTH` |
| BR-A08 | Audit write failure is a hard error (RC 12) in AUDPROC but only a DISPLAY in PORTDEL | AUDPROC `2000-WRITE-AUDIT`; PORTDEL `2300-WRITE-AUDIT` | — | `CONFLICT` |
| BR-A09 | Online errors carry severity `F` fatal / `W` warning / `I` info and an action `R` return / `C` continue / `A` abend (`'IERR'`); fatal errors are also written to DB2 `ERRLOG` | ERRHNDL; `ERRHND.cpy` | — | `CODE` |
| BR-A10 | VSAM status `00` success, `10` EOF, `22` duplicate, `23` not found are the recognised outcomes | `ERRHAND.cpy` `ERR-VSAM-STATUSES`; PORTADD/PORTUPDT/PORTDEL 88-levels | — | `CODE` |

## 5. Batch control, checkpoint and restart (`BR-B`)

| ID | Rule | Enforced in | Documented in | Status |
|---|---|---|---|---|
| BR-B01 | Daily batch runs in the fixed order `TRNVAL00` → `POSUPD00` → `HISTLD00` → `RPTGEN00`, preceded by `INITDAY`/`CKPCLR`/`DATEVAL` and followed by `BCKLOD00`/`ENDDAY` | `PRCSEQ.cpy` `STANDARD-SEQUENCES` | system-architecture "Batch Processing Sequence"; operations README | `DOC` (only `HISTLD00` exists) |
| BR-B02 | A process may start only when every hard dependency (`PSR-DEP-HARD`) is `D` Done; an unfinished hard dependency yields RC 4, a finished dependency whose RC exceeds `PSR-DEP-RC` yields RC 8 | PRCSEQ00 `2210-CHECK-DEP-STATUS` | system-architecture "Job dependencies" | `BOTH` |
| BR-B03 | Batch control status lifecycle `R` Ready → `A` Active → `D` Done / `E` Error, with `W` Waiting for blocked processes | `BCHCON.cpy`, `BCHCTL.cpy` 88s; PRCSEQ00 `2300-UPDATE-PROCESS-STATUS`; RCVPRC00 | system-architecture | `BOTH` |
| BR-B04 | Maximum 10 prerequisites per job; wait interval 300 s; maximum wait 3600 s | `BCHCON.cpy` | — | `CODE` (constants only) |
| BR-B05 | Recovery decision: if the process is restartable → restart; else if `BCT-RESTART-COUNT > BCT-MAX-RESTARTS (3)` → terminate; else bypass | RCVPRC00 `2110-DETERMINE-ACTION` | system-architecture "Restart Procedures" | `BOTH` |
| BR-B06 | Recovery mode is `P` single process (requires process ID), `S` sequence, or `A` all | RCVPRC00 `1200-VALIDATE-REQUEST`, `1300-SET-RECOVERY-MODE` | — | `CODE` |
| BR-B07 | Checkpoint / commit every 1000 records (HISTLD00, `CK-COMMIT-FREQ`); position update every 500 (documented only); minimum 2-minute interval (documented only) | HISTLD00 `WS-COMMIT-THRESHOLD 1000`; `CKPRST.cpy` | system-architecture "Checkpoint Frequency" | `CONFLICT` (500/2-min rules have no code) |
| BR-B08 | A job stops after more than 100 errors; `RETURN-CODE` = error count | HISTLD00 `0000-MAIN`; `CKPRST.cpy` `CK-MAX-ERRORS` | — | `CODE` (error-count-as-RC breaks BR-A01) |
| BR-B09 | HISTLD00 marks its `BCHCTL` record Active at start, writes the record count to it at every commit (`2310-UPDATE-CHECKPOINT`), and ignores duplicate `POSHIST` rows (SQLCODE −803) so a rerun is idempotent; there is **no** skip-to-checkpoint logic on restart – the whole input is re-read | HISTLD00 `1300-INIT-CHECKPOINTS`, `2200-LOAD-TO-DB2`, `2310-UPDATE-CHECKPOINT` | system-architecture "Restart from last checkpoint" | `CONFLICT` |
| BR-B10 | Maximum 3 restarts per program run | `CKPRST.cpy` `CK-MAX-RESTARTS`; `BCHCON.cpy` `BCT-MAX-RESTARTS` | operations README | `BOTH` |
| BR-B11 | Process schedule honours active weekdays, month-end flag and holiday-run flag | `PRCSEQ.cpy` `PSR-SCHEDULE` | — | `CODE` (fields defined, not evaluated by PRCSEQ00) |

## 6. DB2 access (`BR-D`)

| ID | Rule | Enforced in | Status |
|---|---|---|---|
| BR-D01 | Connection is to `POSMVP`; connect failure is retried up to 3 times with a 100-unit delay (`DB2CONN` calls system `DELAY`) | DB2CONN; `DBPROC.cpy` | `CODE` |
| BR-D02 | Commit-manager functions: `INIT`, `CMIT` commit, `RBAK` rollback, `SAVE` savepoint, `REST` restore to savepoint, `STAT` status, with a force-commit flag; `ROLLBACK WORK` is issued on any SQL error before logging | DB2CMT `0000-MAIN`; `DBPROC.cpy` `DB2-ERROR-ROUTINE` | `CODE` |
| BR-D03 | SQL error severity: deadlock (−911) / timeout (−913) → severity 2, retryable; connection error (−30081) → 4, no retry; duplicate (−803) / not found (+100) → 1; any other negative → 3; non-negative → 1 | DB2ERR `1100-SET-SEVERITY` | `CODE` |
| BR-D04 | Every DB2 error is logged with `EL-ERROR-TYPE = 'D'` (data), although `DBTBLS.cpy` also defines `S` system and `A` application | DB2ERR `1000-LOG-ERROR`; `DBTBLS.cpy` | `CODE` (classification not used) |
| BR-D05 | Error log rows older than `RETENTION_DAYS` are purged by `ERRLOG_CLEANUP` | `ERRLOG.sql` stored procedure | `CODE` |
| BR-D06 | Online DB2 connection pool is capped at 100 active connections | DB2ONLN `WS-MAX-CONNECTIONS` | `CODE` |
| BR-D07 | Online connection recovery retries 3 times at 2-second intervals, then reports failure | DB2RECV | `CODE` |
| BR-D08 | Return codes are logged to `RTNCODES` with program, current, highest and status; analysis computes counts by status per program | RTNCDE00 `P400`, `P500`; RTNANA00 | `CODE` |

## 7. Online inquiry and security (`BR-O`)

| ID | Rule | Enforced in | Status |
|---|---|---|---|
| BR-O01 | Every `PINQ` request is first validated (`V`, using `ASSIGN USERID`), then authorised (`A`) against `AUTHFILE` for resource `INQONLN` / access `READ`, then logged (`L`) to DB2 `AUDITLOG`; each step runs only if the previous returned 0 | INQONLN `P050-SECURITY-CHECK`; SECMGR | `CODE` |
| BR-O02 | Authorisation succeeds when `COUNT(*) > 0` for (user, resource, access type) | SECMGR `P200-CHECK-AUTH` | `CODE` |
| BR-O03 | Menu functions: `INQP` position inquiry, `INQH` history inquiry, `EXIT`, anything else is "Invalid function" | INQONLN `P100-PROCESS-REQUEST`; `INQCOM.cpy` | `CODE` |
| BR-O04 | Position inquiry reads `POSFILE` by account number; not-found → "Position not found" on the map | INQPORT | `CODE` |
| BR-O05 | History inquiry lists `POSHIST` rows for an account via a dynamic cursor, most recent first, paged into `HISMAP` | INQHIST, CURSMGR | `CODE` (column names do not match DDL) |
| BR-O06 | Online DB2 failures trigger connection recovery before the request is abandoned | INQHIST → DB2RECV | `CODE` |

## 8. Reporting and data-validation utilities (`BR-R`)

| ID | Rule | Enforced in | Status |
|---|---|---|---|
| BR-R01 | Daily position report lists every `POSMSTRE` record (current vs previous value, net change), summarises `TRANHIST` activity, then writes totals, exceptions and metrics | RPTPOS00 `2100`–`2300` | `CODE` (summary paragraphs are stubs) |
| BR-R02 | Audit report merges `AUDITLOG` and `ERRLOG` records for the run date and totals by type | RPTAUD00 | `CODE` |
| BR-R03 | Statistics report combines DB2 statistics and batch statistics | RPTSTA00 | `CODE` (input layouts missing) |
| BR-R04 | Return-code analysis reports per-program counts of success/warning/error/severe | RTNANA00 | `CODE` |
| BR-R05 | Data validation checks: integrity (position, transaction), cross-reference (position↔transaction), format, and balance (accumulate positions, verify against transactions), driven by `VALCTL` control records | UTLVAL00 `2200`–`2500` | `CODE` (leaf paragraphs are stubs) |
| BR-R06 | Monitoring: 23-hour run loop, DB2 statistics polled against `MONCFG` thresholds, alerts written to `ALERTS` | UTLMON00 | `CODE` (stub) |
| BR-R07 | Maintenance: archive/purge driven by `CTLFILE`, output to variable-length `ARCHFILE` | UTLMNT00 | `CODE` (stub) |
| BR-R08 | Test-data generation is seeded from `RANDSEED` and sized by `CFG-VOLUME`; test validation compares `EXPECTED` vs `ACTUAL` per `TESTCASE` | TSTGEN00, TSTVAL00 | `CODE` |

## 9. Structural and data rules (`BR-S`)

| ID | Rule | Source | Status |
|---|---|---|---|
| BR-S01 | Portfolio identity is (portfolio ID 8, account 10) | `PORTFLIO.cpy`, `PORTDEF.jcl` `KEYS(18 0)` | `CONFLICT` with PORTMSTR (10-byte ID) and `vsam-definitions.txt` (12-byte key) |
| BR-S02 | Position identity is (portfolio, date, investment) | `POSREC.cpy`, `db2-definitions.sql` | `CONFLICT` with data-dictionary (account, fund) |
| BR-S03 | Transaction identity is (date, time, portfolio, sequence) | `TRNREC.cpy`, `vsam-definitions.txt` key comment | `CONFLICT` with data-dictionary `TRANS-ID X(12)` and DB2 `TRANSACTION_ID CHAR(20)` |
| BR-S04 | Money is 15,2 signed packed decimal; quantities/prices are 15,4 (copybooks) or 15,3 (data dictionary, `DBTBLS`) | copybooks, DDL | `CONFLICT` |
| BR-S05 | Dates are `YYYYMMDD` strings in VSAM/flat files and DB2 `DATE` in tables; timestamps are 26-char DB2 format | data-dictionary §1, copybooks | `BOTH` |
| BR-S06 | Currency is a 3-char ISO code from {USD, EUR, GBP, JPY, CAD} | `COMMON.cpy` | `CODE` (never validated) |
| BR-S07 | `TRANHIST` is append-only history (ESDS) | data-dictionary §2.3 | `CONFLICT` with `vsam-definitions.txt` (KSDS) and all programs (keyed reads) |
| BR-S08 | `POSHIST` is range-partitioned by quarter of `TRANS_DATE` (2024 only) | `POSHIST.sql` | `CODE` (needs a rolling partition strategy in the target) |

## 10. Rules recommended for the target system

Consolidating the above, the target rule set that a migration should implement
(and where the legacy code is ambiguous, the choice to be confirmed with the
business) is:

1. **Identity**: portfolio = (portfolioId, accountNumber); position = (portfolioId,
   positionDate, investmentId); transaction = (timestamp, portfolioId, sequence).
   Confirm whether accounts are 9 or 10 digits (BR-V01).
2. **Portfolio lifecycle**: create requires `'PORT'`+digits id, non-blank name,
   status Active; status enum to be fixed (BR-P03); every create/update/delete
   audited.
3. **Transaction validation**: type ∈ {BU, SL, TR, FE}; quantity > 0 for BU/SL;
   price > 0 and amount > 0 for BU/SL/FE; transaction date ≤ today (BR-T10, not
   yet coded); portfolio exists and is Active.
4. **Position arithmetic**: buy adds units/cost and recomputes average cost
   (BR-T09); sell requires sufficient units and reduces units/cost; fee reduces
   cost only; transfer semantics must be specified (BR-T08).
5. **Error/return codes**: single 0/4/8/12/16 severity scale; one error-code
   catalogue replacing the two `E001–E004` lists (BR-A05); errors persisted once
   (not both flat file and DB2).
6. **Batch orchestration**: dependency-checked sequence with hard/soft
   dependencies, RC thresholds, 3-restart cap, checkpoint every 1000 records and
   restart from checkpoint (BR-B02…B10).
7. **Online**: authenticate + authorise + audit every inquiry; two read-only
   inquiry operations (position by account, history by account).
