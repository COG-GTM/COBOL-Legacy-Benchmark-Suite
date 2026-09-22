# S8 — platform-common Library: Modernization Specification

Component: **S8 platform-common lib** (per `cobol-java-modernization-plan.md` §2). Not a service; a shared Java library consumed by S1–S7, S9, S10.

Sources analyzed (all read in full):

| Kind | File | Lines |
|---|---|---|
| Program | `src/programs/common/ERRPROC.cbl` | 106 |
| Program | `src/programs/common/AUDPROC.cbl` | 95 |
| Program | `src/programs/common/DB2ERR.cbl` | 199 |
| Program | `src/programs/common/DB2CONN.cbl` | 153 |
| Program | `src/programs/common/DB2CMT.cbl` | 169 |
| Program | `src/programs/common/DB2STAT.cbl` | 227 |
| Program | `src/programs/batch/RTNCDE00.cbl` | 140 |
| Program | `src/programs/batch/RTNANA00.cbl` | 209 |
| Copybook | `src/copybook/common/ERRHAND.cpy`, `AUDITLOG.cpy`, `RTNCODE.cpy`, `COMMON.cpy` | 55 / 35 / 31 / 63 |
| Copybook | `src/copybook/db2/SQLCA.cpy`, `DBPROC.cpy`, `DBTBLS.cpy` | 14 / 62 / 49 |
| DDL | `src/database/db2/ERRLOG.sql`, `RTNCODES.sql`, `PORTPLAN.sql` | 64 / 17 / 9 |
| Templates | `src/templates/error/error-handling.cbl`, `program/file-handling.cbl`, `program/standard-program.cbl`, `database/db2-handling.cbl` | 187 / 189 / 90 / 246 |
| Context | `documentation/technical/system-architecture.md`, `data-dictionary.md` | skimmed |

Conventions used below: `FILE:line` refers to the COBOL source line. Rules marked **INFERRED FROM DOCS** are not present in code. Nothing in this document invents behavior; where code is inconsistent or non-functional, it is stated as such.

---

## 1. Purpose & scope

S8 is the cross-cutting "plumbing" every batch and portfolio program in the Investment Portfolio Management System relies on. In business terms it answers four questions for the operations and compliance teams:

1. **What went wrong, where, and how bad was it?** — `ERRPROC` (write a standardized error record to a sequential error log and echo it to the job log), `DB2ERR` (classify a DB2 SQLCODE into a severity/retryability, persist it to the `ERRLOG` table, and read back the most recent error for a program).
2. **Who did what to which portfolio/account, before and after?** — `AUDPROC` writes fixed-format audit-trail records (system id, user, terminal, action, status, key, before/after images) to the `AUDFILE` sequential file.
3. **Did the job succeed, warn, or fail, and how did the fleet behave over time?** — `RTNCDE00` maintains current/highest return code with S/W/E/F status classification and persists it to the `RTNCODES` table; `RTNANA00` produces a per-program S/W/E/F count report from that table.
4. **How do programs talk to DB2 safely?** — `DB2CONN` (connect with retry, disconnect, status probe), `DB2CMT` (frequency-based commit, rollback, savepoint/restore, counters), `DB2STAT` (per-run row/commit counters and elapsed/CPU time in a session temp table), plus the `DBPROC`/`SQLCA` copybook procedures used inline by callers.

Shared vocabulary is fixed by copybooks: return-code ladder 0/4/8/12/16 (`COMMON.cpy`, `ERRHAND.cpy`), error categories VS/VL/PR/SY, VSAM status codes, DB2 SQLSTATE constants, portfolio status codes, transaction types, currency codes.

Per the plan, in Java this becomes one library (`platform-common`): `ReturnCode` enum, exception hierarchy + error-log persistence, audit event publisher, Micrometer metrics (replacing `DB2STAT`), and Spring transaction management (retiring `DB2CONN`/`DB2CMT`). `RTNANA00` becomes a read-model/report over `RTNCODES`.

Out of scope for S8 (owned elsewhere): `ERRHNDL`/`DB2ONLN`/`DB2RECV` online error handling (S4), `RPTAUD00` audit reporting (S7), batch control/checkpointing (S6).

---

## 2. Business rules

Numbered `BR-nn`. Extracted paragraph by paragraph.

### 2.1 ERRPROC — standard error processor (`src/programs/common/ERRPROC.cbl`)

- **BR-01** Every error-processing request opens the error log `ERRLOG` (DDNAME) in **EXTEND** (append) mode; an open failure is only displayed (`'Error opening log file: ' status`) and processing continues — it is not fatal and does not alter the return code. `ERRPROC.cbl:55-63`.
- **BR-02** The error timestamp is captured at initialization via `ACCEPT ... FROM TIME STAMP` into a 26-byte field and then moved into the 18-byte `ERR-TIMESTAMP` (10-byte date + 8-byte time), i.e. the timestamp is **truncated to its first 18 characters** by a group MOVE. `ERRPROC.cbl:35,57,69`; `ERRHAND.cpy:31-33`.
- **BR-03** An error record is composed field-for-field from the request: program id, category (2 chars), error code (4 chars), severity (binary halfword), 80-char text, 256-char details. `ERRPROC.cbl:69-75`.
- **BR-04** The 370-byte `ERR-MESSAGE` is written as a single 400-byte fixed-length record (`LOG-DATA PIC X(400)`, right-padded with spaces). Write failure is displayed (`'Error writing to log: ' status`) but not propagated. `ERRPROC.cbl:27-28,83-91`.
- **BR-05** Every error is also echoed to SYSOUT as an 8-line boxed block (timestamp, program, category, code, severity, message, details). `ERRPROC.cbl:93-103`.
- **BR-06** **The caller's return code is set equal to the request severity** (`MOVE LS-SEVERITY TO LS-RETURN-CODE`); ERRPROC never escalates or downgrades. `ERRPROC.cbl:80`.
- **BR-07** The log file is closed on every call (open/close per error — no batching). `ERRPROC.cbl:105-107`.
- **BR-08** The request layout ERRPROC actually parses is `LS-ERROR-REQUEST` (program-id, category, code, severity, text, details, return-code — **no timestamp**) `ERRPROC.cbl:38-45`. Most callers pass `ERR-MESSAGE` (which **starts with an 18-byte timestamp**) instead — see Discrepancy D-01. The behavior that results on the mainframe is field-misaligned garbage; the Java library must define one request contract.

### 2.2 AUDPROC — audit trail processor (`src/programs/common/AUDPROC.cbl`)

- **BR-09** Audit file `AUDFILE` is opened in EXTEND mode; if the open fails (status ≠ `'00'`) the message `'Error opening audit file: '` is displayed, **return code 8** is set, the file is closed and the program returns immediately (no record written). `AUDPROC.cbl:59-68`.
- **BR-10** An audit record is built by: timestamp (26-byte `TIME STAMP`), header = caller's system-info group (system id, user id, program, terminal), type, action, status, key info (portfolio id + account no), before image, after image, message. `AUDPROC.cbl:72-82`. Note `LS-SYSTEM-INFO` (32 bytes) is moved to `AUD-HEADER` (58 bytes) **after** the timestamp is set, so the group MOVE overwrites `AUD-TIMESTAMP` with system-id/user-id/program/terminal and space-fills the rest — the timestamp written to the file is effectively lost. `AUDPROC.cbl:74-75`; `AUDITLOG.cpy:8-13`.
- **BR-11** Write failure → display `'Error writing audit record: '` and **return code 8**; success → return code 0. `AUDPROC.cbl:84-91`.
- **BR-12** Allowed audit types are `TRAN`, `USER`, `SYST`; actions `CREATE`, `UPDATE`, `DELETE`, `INQUIRE`, `LOGIN`, `LOGOUT`, `STARTUP`, `SHUTDOWN`; statuses `SUCC`, `FAIL`, `WARN` (Level-88 values). AUDPROC **does not validate** them — any 4/8/4-byte value is written. `AUDITLOG.cpy:14-30`.
- **BR-13** Audit record is 380 bytes fixed (26+8+8+8+8 +4+8+4 +8+10 +100+100+100). `AUDITLOG.cpy:7-36`.

### 2.3 DB2ERR — DB2 SQL error handler (`src/programs/common/DB2ERR.cbl`)

- **BR-14** Function dispatch on `LS-FUNCTION`: `'LOG '` → log error, `'DIAG'` → diagnose, `'RETR'` → retrieve last error; any other value → text `'Invalid function code'`, return code 12, and a CALL to `ERRPROC`. `DB2ERR.cbl:40-43,57-70,196-200`.
- **BR-15 (LOG)** An `ERRLOG` row is built with: `ERROR_TIMESTAMP` = current `TIME STAMP`; `PROGRAM_ID` = caller's; `ERROR_TYPE` = **always `'D'` (Data)** — `'S'`/`'A'` are never produced by this program; `USER_ID` = spaces; `ADDITIONAL_INFO` = caller's 100-char additional info; `ERROR_MESSAGE` = caller's 80-char text. `DB2ERR.cbl:73-95`.
- **BR-16 (LOG)** `ERROR_CODE` is intended to be `'SQLCODE: ' + edited-sqlcode + ' STATE: ' + sqlstate` but the target `EL-ERROR-CODE` is `PIC X(8)`, so the stored value is **always the literal `'SQLCODE:'`** (STRING stops at receiving-field end; no ON OVERFLOW). The SQLCODE/SQLSTATE are therefore **not persisted** in `ERROR_CODE`. `DB2ERR.cbl:83-87`; `DBTBLS.cpy:45`.
- **BR-17 (LOG)** `PROCESS_DATE` receives `FUNCTION CURRENT-DATE(1:10)` and `PROCESS_TIME` receives `CURRENT-DATE(12:8)`. `CURRENT-DATE` is `YYYYMMDDhhmmsscc±hhmm`, so `PROCESS_DATE` = `YYYYMMDDhh` (not a valid DB2 DATE string) and `PROCESS_TIME` = `mmsscc±h` (not a valid TIME). On DB2 this INSERT would fail with a conversion error; the code path then goes to `9000-ERROR-ROUTINE` (BR-21). `DB2ERR.cbl:90-93`.
- **BR-18 (LOG) Severity & retry classification** — exact mapping, evaluated on the caller-supplied SQLCODE (`DB2ERR.cbl:100-124`, constants `:31-36`):

  | SQLCODE | `ERROR_SEVERITY` | Retry flag |
  |---|---|---|
  | −911 (deadlock) | 2 (Warning) | `Y` |
  | −913 (timeout) | 2 (Warning) | `Y` |
  | −30081 (connection) | 4 (Severe) | `N` |
  | −803 (duplicate key) | 1 (Info) | `N` |
  | +100 (not found) | 1 (Info) | `N` |
  | any other < 0 | 3 (Error) | `N` |
  | any other ≥ 0 (incl. 0) | 1 (Info) | `N` |

- **BR-19 (LOG)** `INSERT INTO ERRLOG VALUES (:WS-ERRLOG-REC)` (positional, whole-row host structure). SQLCODE 0 → return code 0; otherwise text `'Error logging to ERRLOG'`, return code 12, call ERRPROC. `DB2ERR.cbl:126-138`.
- **BR-20 (DIAG)** Diagnosis returns a message and return code without any I/O (`DB2ERR.cbl:140-169`):

  | SQLCODE | `LS-ERROR-TEXT` | RC |
  |---|---|---|
  | −911 | `Deadlock detected - retry transaction` | 4 |
  | −913 | `Timeout occurred - retry transaction` | 4 |
  | −30081 | `DB2 connection error - check availability` | 12 |
  | −803 | `Duplicate key violation` | 8 |
  | other < 0 | `Unhandled DB2 error` | 12 |
  | other ≥ 0 (incl. +100 and 0) | `DB2 warning condition` | 4 |

  Note DIAG does **not** set the retry flag; only LOG does (BR-18). +100 is "Info" in LOG but "warning, RC 4" in DIAG.
- **BR-21 (RETR)** Retrieve the most recent `ERRLOG` row for the program (`ERROR_TIMESTAMP = MAX(ERROR_TIMESTAMP)` for that `PROGRAM_ID`), returning `ERROR_MESSAGE` as text and **`ERROR_SEVERITY` (1–4) as the return code**. Not found / any SQL error → text `'No error history found'`, RC 4. `DB2ERR.cbl:171-194`.
- **BR-22** Internal failure path: `ERR-PROGRAM='DB2ERR'`, RC 12, `CALL 'ERRPROC' USING ERR-MESSAGE`. The SQLCODE of the *failed insert* is not recorded anywhere (ERR-CODE/ERR-SEVERITY left as-is). `DB2ERR.cbl:196-200`.

### 2.4 DB2CONN — connection manager (`src/programs/common/DB2CONN.cbl`)

- **BR-23** Function dispatch: `CONN`, `DISC`, `STAT`; other → `'Invalid function code'`, RC 12, call ERRPROC. `DB2CONN.cbl:35-38,48-61,150-154`.
- **BR-24 (CONN)** `CONNECT TO :db-name` is attempted **up to 3 times** (`WS-MAX-RETRIES = 3`); loop ends on success or when retry count reaches 3. Success → connected, RC 0. Each failure increments the retry count and maps the message (BR-25). `DB2CONN.cbl:30-31,63-88`.
- **BR-25 (CONN)** Failure message by SQLCODE: −30081 → `Maximum connections exceeded`; −99999 → `Network error connecting to DB2`; other → `General DB2 connection error`. RC is set to **12** on every failed attempt (so a later success resets it to 0 — BR-24). `DB2CONN.cbl:91-107`.
- **BR-26 (CONN)** Between failed attempts (while retries < 3 and not connected) the program `CALL 'DELAY' USING DB2-RETRY-WAIT` with `DB2-RETRY-WAIT = 100` (units undefined in code; `DELAY` is not in the repo — see D-08). `DB2CONN.cbl:84-87`; `DBPROC.cpy:21`.
- **BR-27 (CONN)** The plan name is copied into a host variable but **never used** in any SQL. `DB2CONN.cbl:19,67`.
- **BR-28 (DISC)** Disconnect only acts if the in-memory state is "connected" (working-storage flag — see D-09 on statefulness): `COMMIT WORK` then `CONNECT RESET`. Success → RC 0; failure → SQLCODE copied out, `Error disconnecting from DB2`, RC **8**. The COMMIT's own SQLCODE is not checked. `DB2CONN.cbl:109-129`.
- **BR-29 (STAT)** Status probe = `SELECT CURRENT SERVER INTO :WS-DB-NAME FROM SYSIBM.SYSDUMMY1`. Success → connected, RC 0; failure → disconnected, SQLCODE copied out, `DB2 connection not active`, RC **4**. `DB2CONN.cbl:131-148`.

### 2.5 DB2CMT — commit controller (`src/programs/common/DB2CMT.cbl`)

- **BR-30** Function dispatch: `INIT`, `CMIT`, `RBAK`, `SAVE`, `REST`, `STAT`; other → `'Invalid function code'`, RC 12, call ERRPROC. `DB2CMT.cbl:34-40,54-73,162-166`.
- **BR-31 (INIT)** Zero the commit/rollback/savepoint counters; RC 0. `DB2CMT.cbl:75-78`.
- **BR-32 (CMIT) Commit frequency rule**: a commit is issued **only if** `records-processed >= commit-frequency` **or** the force flag is `'Y'`. Otherwise nothing happens and **`LS-RETURN-CODE` is left unchanged** (not set to 0). `DB2CMT.cbl:80-85`.
- **BR-33 (CMIT)** `COMMIT WORK`: success → commit counter +1, RC 0; failure → SQLCODE out, `Commit failed`, RC 8, then `CALL 'DB2ERR' USING LS-ERROR-INFO`. The caller's `records-processed` counter is **not reset** by DB2CMT (caller's responsibility). `DB2CMT.cbl:87-101`.
- **BR-34 (RBAK)** `ROLLBACK WORK`: success → rollback counter +1, RC 0; failure → `Rollback failed`, RC 8, call DB2ERR. `DB2CMT.cbl:103-117`.
- **BR-35 (SAVE)** `SAVEPOINT :name ON ROLLBACK RETAIN CURSORS` (name ≤ 18 chars): success → savepoint counter +1, RC 0; failure → `Savepoint creation failed`, RC 8, call DB2ERR. `DB2CMT.cbl:119-135`.
- **BR-36 (REST)** `ROLLBACK TO SAVEPOINT :name`: success → **rollback** counter +1 (savepoint restores count as rollbacks), RC 0; failure → `Savepoint restore failed`, RC 8, call DB2ERR. `DB2CMT.cbl:137-153`.
- **BR-37 (STAT)** Display the three counters to SYSOUT; RC unchanged. `DB2CMT.cbl:155-160`.
- **BR-38** The DB2ERR call passes `LS-ERROR-INFO` (SQLCODE + 80-char message) although DB2ERR expects a request beginning with a 4-char function code — see D-02; the effective behavior is undefined.

### 2.6 DB2STAT — statistics collector (`src/programs/common/DB2STAT.cbl`)

- **BR-39** Function dispatch: `INIT`, `UPDT`, `TERM`, `DISP`; other → `'Invalid function code'`, RC 12, call ERRPROC. `DB2STAT.cbl:42-46,58-74,224-228`.
- **BR-40 (INIT)** Zero the stats record, set program id and start time (`TIME STAMP`), then create the session temp table `SESSION.DBSTATS` (`DECLARE GLOBAL TEMPORARY TABLE ... ON COMMIT PRESERVE ROWS`). SQLCODE ≠ 0 **and ≠ −601** (already exists) → `'Error creating stats table'`, RC 12, ERRPROC. `DB2STAT.cbl:76-109`.
- **BR-41 (INIT)** Insert the initial row: program id, `CURRENT TIMESTAMP`, all six counters 0; END_TIME/CPU_TIME/ELAPSED_TIME null. Failure → `'Error initializing stats'`, RC 12. `DB2STAT.cbl:111-128`.
- **BR-42 (UPDT)** Overwrite (not increment) the six counters (rows read/inserted/updated/deleted, commits, rollbacks) for the program's row. Failure → `'Error updating stats'`, RC 12. Counters are absolute values supplied by the caller. `DB2STAT.cbl:130-155`.
- **BR-43 (TERM)** Set end time = `TIME STAMP`, compute times (BR-44), update `END_TIME, CPU_TIME, ELAPSED_TIME`, then display (BR-45). Failure → `'Error finalizing stats'`, RC 12. `DB2STAT.cbl:157-178`.
- **BR-44 (TERM) Time arithmetic** — `ELAPSED_TIME = NUMVAL(end-timestamp(1:15)) − NUMVAL(start-timestamp(1:15))`, stored in `PIC S9(9)V99 COMP-3` (truncated, no ROUNDED). Then `CPU_TIME = ELAPSED_TIME × 0.65` (`MULTIPLY 0.65 BY WS-CPU-TIME`, truncated to 2 decimals, no ROUNDED). The first 15 chars of a `TIME STAMP` are `YYYY-MM-DD-HH.M` which is **not a valid numeric string**; `NUMVAL` on it is undefined/returns 0 in most runtimes, so elapsed is effectively meaningless and CPU time is a **fixed 65 % of elapsed, not a measurement**. `DB2STAT.cbl:180-187`.
- **BR-45 (DISP)** Select the 8 metric columns for the program `INTO :WS-STATS-RECORD` (an 11-field group — D-10) and display them; CPU/elapsed are formatted `ZZ,ZZ9.99` + `' seconds'`. Failure → `'Error retrieving stats'`, RC 12. `DB2STAT.cbl:189-222`.
- **BR-46** `SESSION.DBSTATS` schema (the only place it is defined): `PROGRAM_ID CHAR(8) NN, START_TIME TIMESTAMP NN, END_TIME TIMESTAMP, ROWS_READ/ROWS_INSERTED/ROWS_UPDATED/ROWS_DELETED/COMMITS/ROLLBACKS INTEGER NN, CPU_TIME DECIMAL(11,2), ELAPSED_TIME DECIMAL(11,2)`. Rows are per-session and vanish at thread end. `DB2STAT.cbl:88-103`.

### 2.7 RTNCDE00 — standard return code handler (`src/programs/batch/RTNCDE00.cbl`)

- **BR-47** Function dispatch on `RC-REQUEST-TYPE`: `I` init, `S` set, `G` get, `L` log, `A` analyze. **Unknown request types are silently ignored** (no WHEN OTHER; response code untouched). `RTNCDE00.cbl:33-49`; `RTNCODE.cpy:5-10`.
- **BR-48 (I)** Reset: codes area zeroed, program id = spaces, current = 0, highest = 0, status = `S`, response = 0. `RTNCDE00.cbl:53-61`.
- **BR-49 (S) Highest-code retention**: `highest = max(highest, new)`; `current = new`. `RTNCDE00.cbl:64-68`.
- **BR-50 (S) Status classification** of the new code: `0 → S` (success), `1..4 → W` (warning), `5..8 → E` (error), **anything else (≥ 9 or negative) → F** (severe). Note this differs from the 0/4/8/12/16 ladder — e.g. RC 2 is a warning here, RC 12 and 16 are both `F`. Response = 0. `RTNCDE00.cbl:70-81`.
- **BR-51 (G)** Copy current, highest, status into the return-data group; response 0. `RTNCDE00.cbl:85-91`.
- **BR-52 (L)** Insert one `RTNCODES` row: `TIMESTAMP` = `FUNCTION CURRENT-DATE` moved to a 16-digit numeric group (`YYYYMMDDhhmmsscc` — **not a DB2 TIMESTAMP literal**, D-11), `PROGRAM_ID`, `RETURN_CODE` = current, `HIGHEST_CODE`, `STATUS_CODE`, `MESSAGE_TEXT`. SQLCODE 0 → response 0; else response **8**. No ERRPROC call. `RTNCDE00.cbl:93-119`.
- **BR-53 (A)** Analysis = `COUNT(*), MAX(RETURN_CODE), MIN(RETURN_CODE)` from `RTNCODES` for the program within `[start, end]` timestamps (inclusive both ends). SQLCODE 0 → response 0; else 8 (an empty window yields COUNT 0 with null MAX/MIN and no null indicators → SQLCODE −305 → response 8). `RTNCDE00.cbl:121-139`.
- **BR-54** RTNCDE00 has **no callers** anywhere in `src/` (`RTNCODE.cpy` is copied by RPT*/UTL*/TST* programs for its constants only). Its rules are therefore latent, not exercised. (grep of `CALL 'RTNCDE00'` — zero hits.)

### 2.8 RTNANA00 — return code analysis report (`src/programs/batch/RTNANA00.cbl`)

- **BR-55** Standalone batch job (JCL `src/jcl/RTNANA.jcl`, `PGM=RTNANA00`, `RPTFILE DD SYSOUT=*` 133-byte FBA). Report file open failure → display, **`RETURN-CODE 12`**, GOBACK. `RTNANA00.cbl:111-119`.
- **BR-56** Cursor `PRGCUR`: for each `PROGRAM_ID` in `RTNCODES` (all history, no date filter), `COUNT(*)`, and counts where `STATUS_CODE` = `'S'`, `'W'`, `'E'`, `'F'`; ordered by program id. Rows with any other status code count toward TOTAL only. `RTNANA00.cbl:126-137`.
- **BR-57** Report layout (133 cols): dash rule; centered title `Return Code Analysis Report`; `Report Date: YYYYMMDD` + `Report Time: hh:mm:ss`; dash rule; column header `Program | Total | Success | Warning | Error | Severe`; dash rule; one detail line per program (`ZZZ,ZZ9` edited counts); dash rule; `TOTALS` line summing the five counts across programs; dash rule. `RTNANA00.cbl:54-94,152-166,192-205`.
- **BR-58** Fetch loop runs until SQLCODE = 100; **any negative SQLCODE loops forever** (only `= 0` writes, only `= 100` exits). Report date is `YYYYMMDD` unformatted (10-byte field, 8 used). `RTNANA00.cbl:144-146,156,180-188`.
- **BR-59** Counts are fetched directly `INTO` numeric-**edited** fields (`PIC ZZZ,ZZ9`), which are not valid DB2 host variables — D-12. Totals are accumulated by `ADD`ing those edited fields. `RTNANA00.cbl:85-93,171-187`.
- **BR-60** Normal completion does not set `RETURN-CODE` (defaults 0) and RTNANA00 does not log its own outcome to `RTNCODES`/`ERRLOG`. `RTNANA00.cbl:96-109`.

### 2.9 Copybook-level rules (constants shared by all callers)

- **BR-61 Return-code ladder** `0 success / 4 warning / 8 error / 12 severe / 16 terminal|critical` — defined twice with different names: `ERR-SUCCESS..ERR-TERMINAL` (`ERRHAND.cpy:20-25`, `S9(4) COMP`) and `RC-SUCCESS..RC-CRITICAL` (`COMMON.cpy:8-13`, `S9(4) DISPLAY`). The template `error-handling.cbl:18-23` repeats `RC-*` as COMP. Docs (`data-dictionary.md §8.2`) name 12 "Critical error, abend" and 16 "Environment error".
- **BR-62 Final RC from counts (template pattern)**: severe-count > 0 → 12; else error-count > 0 → 8; else warning-count > 0 → 4; else 0. Abend path: `CALL 'CEE3ABD' USING RC-CRITICAL, 3` (abend code 16, clean-up 3). `templates/error/error-handling.cbl:105-122,181-187`. Template only — not linked by any program.
- **BR-63 Error categories** `VS` VSAM, `VL` validation, `PR` processing, `SY` system. `ERRHAND.cpy:11-15`.
- **BR-64 VSAM status semantics** `00` success, `22` duplicate key → `Duplicate record key`, `23` not found → `Record not found`, `10` EOF, other → `Unexpected VSAM error`. `ERRHAND.cpy:44-56`; identical Level-88s in `templates/program/file-handling.cbl:72-81`.
- **BR-65 SQLSTATE constants** `00000` success, `02000` not found, `23505` dup key, `40001` deadlock, `40003` timeout, `08001` connection error, `58004` DB error. Declared but **unused** by any program in scope (all logic keys on SQLCODE). `SQLCA.cpy:8-15`.
- **BR-66 DBPROC inline procedures** (copied into WORKING-STORAGE of DB2ERR/DB2CONN/DB2CMT/DB2STAT/HISTLD00 — see D-13): `CONNECT-TO-DB2` = `CONNECT TO POSMVP` (hard-coded), failure → `Connection failed` → `DB2-ERROR-ROUTINE`; `DISCONNECT-FROM-DB2` = COMMIT + CONNECT RESET; `DB2-ERROR-ROUTINE` = format `SQLCODE: nnnnnn STATE: sssss ERROR: text`, **ROLLBACK WORK**, `ERR-PROGRAM='DB2ERROR'`, `CALL 'ERRPROC' USING ERR-MESSAGE`; `CHECK-SQL-STATUS` = any SQLCODE ≠ 0 (including +100) is an error. Retry parameters: max 3, wait 100. `DBPROC.cpy:10-63`.
- **BR-67 Template DB2 status check** (`templates/database/db2-handling.cbl:222-237`): SQLCODE ≠ 0 → display `DB2 ERROR - SQLCODE: -nnn, SQLERRM: ...`; if < 0 → ROLLBACK, `RETURN-CODE 8`, COMMIT+CONNECT RESET, GOBACK. Positive SQLCODEs (warnings, +100) are displayed but processing continues. Template only.
- **BR-68 Portfolio status codes** `A` active, `C` closed, `P` pending, `S` suspended, `F` failed, `R` reversed; **transaction types** `BU` buy, `SL` sell, `TR` transfer, `FE` fee; **currencies** USD/EUR/GBP/JPY/CAD. `COMMON.cpy:16-29,58-63`. (Data dictionary uses `BY`/`SL`/`FE` — D-14.)
- **BR-69 ERRLOG retention**: stored procedure `ERRLOG_CLEANUP(RETENTION_DAYS)` deletes rows with `PROCESS_DATE < CURRENT DATE − n DAYS`. Grants: `POSAPP` SELECT+INSERT, `POSRPT` SELECT (no UPDATE/DELETE for apps). `ERRLOG.sql:54-65`.
- **BR-70 ERRLOG keys**: PK/clustered unique index `(ERROR_TIMESTAMP, PROGRAM_ID)`; secondary `(PROCESS_DATE ASC, ERROR_SEVERITY DESC)`. Two errors from the same program in the same microsecond collide (−803). `ERRLOG.sql:29-43`.
- **BR-71 RTNCODES keys**: PK `(TIMESTAMP, PROGRAM_ID)`; indexes `(PROGRAM_ID, TIMESTAMP)` and `(STATUS_CODE, TIMESTAMP)`. `RTNCODES.sql:2-18`.
- **BR-72 Plan/bind semantics** `PORTPLAN`: `ISOLATION(CS)` (cursor stability = read committed), `ACQUIRE(USE) RELEASE(COMMIT)`, `VALIDATE(RUN)`, package list `*.PORTPKG.*`. Java equivalent: default `READ_COMMITTED` isolation, locks released at commit. `PORTPLAN.sql:2-10`.

**Total business rules: 72.**

---

## 3. Data contracts

Java type conventions: `PIC X(n)` → `String` (trimmed; max length n); `S9(4) COMP` → `short`/`Short` (or `int` where a return code); `S9(8)/S9(9) COMP` → `int`/`long`; `S9(9)V99 COMP-3` → `BigDecimal` scale 2, `RoundingMode.DOWN` (COBOL truncation); `X(26)` DB2 timestamp text → `LocalDateTime` (micro precision); `X(10)` DB2 DATE text → `LocalDate`; `X(8)` DB2 TIME text → `LocalTime`; Level-88 groups → `enum`.

### 3.1 `ERRHAND.cpy` — `ERR-MESSAGE` (error record) and constants

| Field | PIC | Semantics | Java | Persisted to |
|---|---|---|---|---|
| ERR-DATE | X(10) | date part of timestamp (first 10 chars of `TIME STAMP`, `YYYY-MM-DD`) | `LocalDate` | ERRLOG seq file (DDNAME `ERRLOG`) offset 0 |
| ERR-TIME | X(8) | time part (`-HH.MM.S` after truncation — see BR-02) | `LocalTime` | seq file |
| ERR-PROGRAM | X(8) | reporting program id | `String` | seq file; maps to `ERRLOG.PROGRAM_ID` conceptually |
| ERR-CATEGORY | X(2) | VS/VL/PR/SY | `enum ErrorCategory {VSAM, VALIDATION, PROCESSING, SYSTEM}` | seq file (no DB2 column) |
| ERR-CODE | X(4) | application error code (docs: E001…W002) | `String` | seq file (docs' `ERRLOG.ERROR_CODE CHAR(4)`, not in DDL — D-05) |
| ERR-SEVERITY | S9(4) COMP | 0/4/8/12/16 | `enum ReturnCode` | seq file |
| ERR-TEXT | X(80) | message | `String` | seq file |
| ERR-DETAILS | X(256) | free-form details (e.g. PORT-KEY) | `String` | seq file |
| ERR-CAT-* | X(2) VALUE | constants | enum values | — |
| ERR-SUCCESS/WARNING/ERROR/SEVERE/TERMINAL | S9(4) COMP VALUE 0/4/8/12/16 | constants | `enum ReturnCode {SUCCESS(0), WARNING(4), ERROR(8), SEVERE(12), TERMINAL(16)}` | — |
| ERR-VSAM-SUCCESS/DUPKEY/NOTFND/EOF | X(2) VALUE 00/22/23/10 | VSAM status | `enum VsamStatus` | — |
| ERR-VSAM-22/23/OTHER | X(80) VALUE | messages | message bundle | — |

`LS-ERROR-REQUEST` (ERRPROC's actual parameter, `ERRPROC.cbl:38-45`): PROGRAM-ID X(8), CATEGORY X(2), ERROR-CODE X(4), SEVERITY S9(4) COMP, ERROR-TEXT X(80), ERROR-DETAILS X(256), RETURN-CODE S9(4) COMP → Java `ErrorReport` record (+ `ReturnCode` result).

### 3.2 `AUDITLOG.cpy` — `AUDIT-RECORD` (380 bytes, DDNAME `AUDFILE`)

| Field | PIC | Semantics | Java | Maps to |
|---|---|---|---|---|
| AUD-TIMESTAMP | X(26) | event time (`TIME STAMP`) | `LocalDateTime` | AUDFILE / `audit_event.occurred_at` |
| AUD-SYSTEM-ID | X(8) | originating system | `String` | AUDFILE |
| AUD-USER-ID | X(8) | user | `String` | AUDFILE |
| AUD-PROGRAM | X(8) | program | `String` | AUDFILE |
| AUD-TERMINAL | X(8) | CICS terminal / job | `String` | AUDFILE |
| AUD-TYPE | X(4) | TRAN/USER/SYST | `enum AuditType {TRANSACTION, USER_ACTION, SYSTEM_EVENT}` | AUDFILE |
| AUD-ACTION | X(8) | CREATE/UPDATE/DELETE/INQUIRE/LOGIN/LOGOUT/STARTUP/SHUTDOWN | `enum AuditAction` | AUDFILE |
| AUD-STATUS | X(4) | SUCC/FAIL/WARN | `enum AuditStatus {SUCCESS, FAILURE, WARNING}` | AUDFILE |
| AUD-PORTFOLIO-ID | X(8) | key | `String` | AUDFILE |
| AUD-ACCOUNT-NO | X(10) | key | `String` | AUDFILE |
| AUD-BEFORE-IMAGE | X(100) | before image (opaque) | `String` (or JSON) | AUDFILE |
| AUD-AFTER-IMAGE | X(100) | after image | `String` | AUDFILE |
| AUD-MESSAGE | X(100) | narrative | `String` | AUDFILE |

`LS-AUDIT-REQUEST` (`AUDPROC.cbl:34-49`) = same fields minus timestamp, plus `LS-RETURN-CODE S9(4) COMP`. Consumed by `RPTAUD00` (S7) which reads the same layout via DDNAME `AUDITLOG`.

### 3.3 `RTNCODE.cpy` — `RETURN-CODE-AREA`

| Field | PIC | Semantics | Java | Maps to |
|---|---|---|---|---|
| RC-REQUEST-TYPE | X | I/S/G/L/A | `enum ReturnCodeOp {INIT, SET, GET, LOG, ANALYZE}` | — |
| RC-PROGRAM-ID | X(8) | program | `String` | `RTNCODES.PROGRAM_ID` |
| RC-CURRENT-CODE | S9(4) COMP | last code set | `int` | `RTNCODES.RETURN_CODE` |
| RC-HIGHEST-CODE | S9(4) COMP | max seen since init | `int` | `RTNCODES.HIGHEST_CODE` |
| RC-NEW-CODE | S9(4) COMP | input for SET | `int` | — |
| RC-STATUS | X | S/W/E/F | `enum RunStatus {SUCCESS('S'), WARNING('W'), ERROR('E'), SEVERE('F')}` | `RTNCODES.STATUS_CODE` |
| RC-MESSAGE | X(80) | message | `String` | `RTNCODES.MESSAGE_TEXT VARCHAR(80)` |
| RC-RESPONSE-CODE | S9(8) COMP | 0 ok / 8 SQL failure | `int` | — |
| RC-START-TIME / RC-END-TIME | X(26) | analysis window | `LocalDateTime` | predicate on `RTNCODES.TIMESTAMP` |
| RC-TOTAL-CODES | S9(8) COMP | COUNT(*) | `long` | derived |
| RC-MAX-CODE / RC-MIN-CODE | S9(4) COMP | MAX/MIN(RETURN_CODE) | `Integer` (nullable) | derived |
| RC-RETURN-VALUE / RC-HIGHEST-RETURN / RC-RETURN-STATUS | S9(4) COMP / S9(4) COMP / X | GET outputs | `ReturnCodeSnapshot(int current, int highest, RunStatus status)` | — |

### 3.4 `COMMON.cpy`

| Group / field | PIC | Java | Notes |
|---|---|---|---|
| RC-SUCCESS…RC-CRITICAL | S9(4) VALUE 0/4/8/12/16 | `ReturnCode` enum (same as ERRHAND, `TERMINAL`≡`CRITICAL`) | duplicate definition |
| STATUS-ACTIVE/CLOSED/PENDING/SUSPENDED/FAILED/REVERSED | X(1) A/C/P/S/F/R | `enum PortfolioStatus` | owned by S1 domain; S8 hosts the enum |
| TRN-TYPE-BUY/SELL/TRANSFER/FEE | X(2) BU/SL/TR/FE | `enum TransactionType` | S2 domain; D-14 |
| CURR-YEAR/MONTH/DAY, CURR-HOUR/MINUTE/SECOND/MSEC | X(4)/X(2)… | `LocalDateTime` | `CURRENT-DATE` layout |
| ERROR-CODE / ERROR-MODULE / ERROR-ROUTINE / ERROR-MESSAGE | X(4)/X(8)/X(8)/X(80) | fields of `ErrorReport` | alternate error structure, unused by S8 programs |
| AUDIT-TIMESTAMP/USER/TERMINAL/PROGRAM | X(26)/X(8)/X(8)/X(8) | `AuditHeader` | subset of AUDITLOG header |
| CURR-USD/EUR/GBP/JPY/CAD | X(3) | `java.util.Currency` or `enum` | allowed ISO codes |

### 3.5 `SQLCA.cpy` / `DBPROC.cpy`

| Field | PIC | Java | Notes |
|---|---|---|---|
| SQLCA (SQLCODE, SQLSTATE, SQLERRMC…) | IBM-supplied via INCLUDE | `java.sql.SQLException` (`getErrorCode()`, `getSQLState()`) | — |
| SQL-SUCCESS…SQL-DB-ERROR | X(5) VALUE | `enum SqlStateClass` or constants in `Db2ErrorClassifier` | unused in COBOL |
| DB2-SQLCODE-TXT / DB2-STATE / DB2-ERROR-TEXT | X(6)/X(5)/X(70) | formatted message in `DataAccessException` translator | — |
| DB2-SAVE-STATUS | X(5) | — | never used |
| DB2-RETRY-COUNT / DB2-MAX-RETRIES / DB2-RETRY-WAIT | S9(4) COMP 0/3/100 | Spring Retry config: `maxAttempts=3`, `backoff=100ms` (unit assumed — OQ-3) | — |

### 3.6 `DBTBLS.cpy` — `ERRLOG-RECORD` ↔ `ERRLOG` table (`ERRLOG.sql`)

| Field | PIC | DB2 column | Java | Notes |
|---|---|---|---|---|
| EL-ERROR-TIMESTAMP | X(26) | ERROR_TIMESTAMP TIMESTAMP NN (PK) | `LocalDateTime` | — |
| EL-PROGRAM-ID | X(8) | PROGRAM_ID CHAR(8) NN (PK) | `String` | — |
| EL-ERROR-TYPE | X(1) 88 S/A/D | ERROR_TYPE CHAR(1) NN | `enum ErrorType {SYSTEM, APPLICATION, DATA}` | DB2ERR always writes `D` |
| EL-ERROR-SEVERITY | S9(4) COMP 88 1/2/3/4 | ERROR_SEVERITY INTEGER NN | `enum ErrorSeverity {INFO(1), WARN(2), ERROR(3), SEVERE(4)}` | halfword vs INTEGER (host-var size mismatch, works via conversion on z/OS) |
| EL-ERROR-CODE | X(8) | ERROR_CODE CHAR(8) NN | `String` | always `SQLCODE:` in practice (BR-16) |
| EL-ERROR-MESSAGE | X(200) | ERROR_MESSAGE VARCHAR(200) NN | `String` | — |
| EL-PROCESS-DATE | X(10) | PROCESS_DATE DATE NN | `LocalDate` | fed malformed (BR-17) |
| EL-PROCESS-TIME | X(8) | PROCESS_TIME TIME NN | `LocalTime` | fed malformed (BR-17) |
| EL-USER-ID | X(8) | USER_ID CHAR(8) NN | `String` | always spaces |
| EL-ADDITIONAL-INFO | X(500) | ADDITIONAL_INFO VARCHAR(500) | `String` | caller supplies ≤100 |

`POSHIST-RECORD` (`DBTBLS.cpy:10-28`) is also in this copybook but belongs to S3; DB2ERR pulls it in (renamed `WS-POSHIST-REC`) and never uses it. Fields for reference: quantities/prices `S9(12)V9(3) COMP-3` → `BigDecimal` scale 3; amounts `S9(13)V9(2) COMP-3` → `BigDecimal` scale 2.

### 3.7 `RTNCODES` table (`RTNCODES.sql`)

| Column | Type | Java | Fed by |
|---|---|---|---|
| TIMESTAMP | TIMESTAMP NN (PK) | `LocalDateTime` | RTNCDE00 `WS-CURRENT-TIME` (16-digit numeric — D-11) |
| PROGRAM_ID | CHAR(8) NN (PK) | `String` | RC-PROGRAM-ID |
| RETURN_CODE | INTEGER NN | `int` | RC-CURRENT-CODE |
| HIGHEST_CODE | INTEGER NN | `int` | RC-HIGHEST-CODE |
| STATUS_CODE | CHAR(1) NN | `RunStatus` | RC-STATUS |
| MESSAGE_TEXT | VARCHAR(80) | `String` | RC-MESSAGE |

### 3.8 `SESSION.DBSTATS` (declared in `DB2STAT.cbl:90-101`) / `WS-STATS-RECORD`

| Field | PIC | Column | Java (Micrometer replacement) |
|---|---|---|---|
| WS-PROGRAM-ID | X(8) | PROGRAM_ID CHAR(8) | tag `job` |
| WS-START-TIME / WS-END-TIME | X(26) | START_TIME / END_TIME TIMESTAMP | `Timer.Sample` / Spring Batch `JobExecution.startTime/endTime` |
| WS-ROWS-READ/INSERTED/UPDATED/DELETED | S9(9) COMP | INTEGER | `Counter`s `db.rows{op=read|insert|update|delete}` / `StepExecution.readCount/writeCount` |
| WS-COMMITS / WS-ROLLBACKS | S9(9) COMP | INTEGER | `StepExecution.commitCount/rollbackCount` |
| WS-CPU-TIME / WS-ELAPSED-TIME | S9(9)V99 COMP-3 | DECIMAL(11,2) | `Duration` (elapsed); CPU from `process.cpu.time` gauge — **not** 0.65×elapsed |

### 3.9 Sequential files

| DDNAME | Program | Mode | LRECL | Layout | JCL evidence |
|---|---|---|---|---|---|
| ERRLOG | ERRPROC | EXTEND | 400 | ERR-MESSAGE (370) + pad | `jcl/batch/RPTAUD.jcl:9` (`PROD.ERROR.LOG`, read by RPTAUD00) |
| AUDFILE | AUDPROC | EXTEND | 380 | AUDIT-RECORD | `jcl/portfolio/PORTDEL.jcl:14` (`PORTFOLIO.AUDIT.FILE, DISP=MOD`) |
| RPTFILE | RTNANA00 | OUTPUT | 133 | report lines | `jcl/RTNANA.jcl` (`SYSOUT=*`) |

---

## 4. Interfaces

### 4.1 Inbound (who calls S8)

| Caller | Call | Parameter actually passed | Expected by callee | Java mapping |
|---|---|---|---|---|
| `BCHCTL00:114`, `HISTLD00:228`, `PRCSEQ00:122`, `RCVPRC00:113,290`, `PORTTRAN:316`, `DB2ERR:199`, `DB2CONN:153`, `DB2CMT:165`, `DB2STAT:227`, `DBPROC.cpy:56` | `CALL 'ERRPROC'` | `ERR-MESSAGE` (with timestamp prefix) | `LS-ERROR-REQUEST` (no timestamp) — D-01 | library call: `ErrorReporter.report(ErrorReport)` → returns `ReturnCode`; SLF4J + `ErrorLogRepository` |
| `PORTMSTR:262` | `CALL 'ERRPROC'` | `LS-ERROR-REQUEST` (correct layout) | — | same |
| `PORTMSTR:287` | `CALL 'AUDPROC'` | `LS-AUDIT-REQUEST` (correct) | — | library call `AuditPublisher.publish(AuditEvent)` → Spring `ApplicationEvent` and/or Kafka topic `audit-events` |
| `PORTTRAN:294` | `CALL 'AUDPROC'` | `AUDIT-RECORD` (file layout, has timestamp, no RC) — D-03 | `LS-AUDIT-REQUEST` | same |
| `DB2CMT:169` | `CALL 'DB2ERR'` | `LS-ERROR-INFO` (SQLCODE+msg) — D-02 | `LS-ERROR-REQUEST` (function-code first) | `Db2ErrorClassifier.classify(SQLException)`; `ErrorLogRepository.save` |
| none | `CALL 'RTNCDE00'` | — | RTNCODE area | `ReturnCodeTracker` (Spring Batch `ExitStatus`/`JobExecutionListener`) |
| none in `src/programs` | `CALL 'DB2CONN'`, `'DB2STAT'` | — | — | retired → Spring `DataSource`/`@Transactional` and Micrometer (docs claim HISTLD00 etc. depend on DB2CONN; code does not — D-07) |
| JCL `RTNANA.jcl` | `EXEC PGM=RTNANA00` | RPTFILE | — | Spring Batch job `returnCodeAnalysisJob` in S8 (or S7) producing CSV/text; or SQL view + Grafana |

### 4.2 Outbound (what S8 calls / touches)

| From | Target | Kind | Java target |
|---|---|---|---|
| ERRPROC | DDNAME `ERRLOG` (seq, 400) | file write (append) | replaced by `ERRLOG` table write + structured log line; S7 `RPTAUD00` reads it → S7 reads `error_log` table (DB read) |
| ERRPROC | SYSOUT | DISPLAY | SLF4J `ERROR`/`WARN` by severity |
| AUDPROC | DDNAME `AUDFILE` (seq, 380) | file write | `AuditEvent` **event** (Spring event + outbox → broker); S7 audit report consumes topic or `audit_event` table |
| DB2ERR | `ERRLOG` table | SQL INSERT / SELECT (max-timestamp) | JPA `ErrorLogRepository` (DB write/read), library call |
| DB2ERR, DB2CONN, DB2CMT, DB2STAT | `ERRPROC` | CALL | internal library call |
| DB2CONN | `CONNECT TO :db` / `CONNECT RESET` / `COMMIT` / `SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1` | SQL | HikariCP pool + `DataSourceHealthIndicator` (validation query `SELECT 1`) — **retired** |
| DB2CONN | `'DELAY'` | CALL (external, not in repo) | Spring Retry `FixedBackOffPolicy` |
| DB2CMT | `COMMIT WORK`, `ROLLBACK WORK`, `SAVEPOINT … ON ROLLBACK RETAIN CURSORS`, `ROLLBACK TO SAVEPOINT` | SQL | `PlatformTransactionManager` / `@Transactional`, `TransactionStatus.createSavepoint()`; commit-interval → Spring Batch chunk size — **retired** |
| DB2CMT | `DB2ERR` | CALL | internal |
| DB2STAT | `SESSION.DBSTATS` DGTT | SQL DDL/INSERT/UPDATE/SELECT | Micrometer registry + Spring Batch `BATCH_STEP_EXECUTION` — **retired** |
| RTNCDE00 | `RTNCODES` table | SQL INSERT / aggregate SELECT | JPA `ReturnCodeLogRepository` (DB write/read) |
| RTNANA00 | `RTNCODES` table (cursor `PRGCUR`), DDNAME `RPTFILE` | SQL read, file write | JPA aggregate query (`GROUP BY program_id`) → report writer |
| DBPROC (inline) | `CONNECT TO POSMVP`, ROLLBACK, ERRPROC | SQL/CALL | `spring.datasource.*` config; `@Transactional(rollbackFor=…)` |

### 4.3 Cross-component contracts (per plan §2)

- S1 portfolio-service, S2 transaction-service, S3 position-service, S6 batch-orchestrator, S7 reporting, S9 ops-tools, S10 test-harness all **depend on S8 as a Maven/Gradle dependency** (`libs/platform-common`). No REST endpoint is exposed by S8.
- S7 (`RPTAUD00`) currently reads the `ERRLOG` seq file and `AUDITLOG` file → in Java S7 reads `error_log`/`audit_event` tables (or consumes `audit-events`). S8 owns these tables and their entities.
- S6 orchestrator consumes `ReturnCode`/`RunStatus` to gate steps (`PRCCTL REQUIRED-RC`), replacing RTNCDE00 SET/GET.
- S4 inquiry-api uses its own `ERRHNDL`; it should reuse S8's `ReturnCode`, `ErrorSeverity`, `Db2ErrorClassifier` but not the batch file writers.

---

## 5. Discrepancies (code vs. code, code vs. docs)

| # | Discrepancy | Evidence |
|---|---|---|
| D-01 | **ERRPROC parameter layout mismatch.** ERRPROC parses `LS-ERROR-REQUEST` (program id at offset 0). 10 of 11 call sites pass `ERR-MESSAGE`, whose first 18 bytes are a timestamp, so program id/category/code/severity/text are read from shifted offsets and `LS-RETURN-CODE` is written into the tail of `ERR-DETAILS`. Only `PORTMSTR:262` passes the right structure. | `ERRPROC.cbl:38-47` vs `ERRHAND.cpy:30-39`; call sites listed in §4.1 |
| D-02 | **DB2CMT→DB2ERR layout mismatch.** DB2CMT passes `LS-ERROR-INFO` (SQLCODE S9(9) COMP, msg X(80)); DB2ERR expects function code X(4) first, then program id. The binary SQLCODE is interpreted as a function code → falls to `WHEN OTHER` → "Invalid function code" → ERRPROC. Commit/rollback failures are therefore never logged to `ERRLOG`. | `DB2CMT.cbl:48-50,169` vs `DB2ERR.cbl:39-53` |
| D-03 | **PORTTRAN→AUDPROC layout mismatch.** PORTTRAN passes `AUDIT-RECORD` (timestamp first); AUDPROC expects `LS-AUDIT-REQUEST` (system id first, RC last). Fields shift by 26 bytes and the RC is written past the record; PORTTRAN then checks the special register `RETURN-CODE`, which AUDPROC never sets. | `PORTTRAN.cbl:294-296` vs `AUDPROC.cbl:34-49` |
| D-04 | **AUDPROC overwrites its own timestamp** (group move of 32-byte `LS-SYSTEM-INFO` into 58-byte `AUD-HEADER` after setting `AUD-TIMESTAMP`). | `AUDPROC.cbl:74-75` |
| D-05 | **ERRLOG table definition differs between DDL and data dictionary.** DDL: `ERROR_TYPE, ERROR_SEVERITY, ERROR_CODE CHAR(8), ERROR_MESSAGE VARCHAR(200), PROCESS_DATE, PROCESS_TIME, USER_ID, ADDITIONAL_INFO`. Docs: `ERROR_CODE CHAR(4), ACCOUNT_NO DECIMAL(9), FUND_ID CHAR(6), TRANS_ID CHAR(12), ERROR_DESC VARCHAR(100)`. Docs' business-key columns (account/fund/trans id) and error-code catalogue (E001–E004, W001–W002) are **not implemented** anywhere in code. | `ERRLOG.sql:15-26` vs `data-dictionary.md §3.2, §6` |
| D-06 | **DB2ERR never persists the SQLCODE/SQLSTATE** (8-byte `ERROR_CODE` truncates to `SQLCODE:`), and feeds malformed DATE/TIME strings (BR-16/17). Docs describe DB2ERR as doing "Connection error recovery, Deadlock resolution, Transaction rollback management" — code does none of these (no retry loop, no ROLLBACK; it only *flags* retryability). | `DB2ERR.cbl:83-93` vs `system-architecture.md §6.1.1` |
| D-07 | **Docs' dependency table lists DB2CONN as a dependency of POSUPDT/HISTLD00/RPT*/UTL*/TSTVAL00**; no program in `src/programs` CALLs DB2CONN, DB2STAT or RTNCDE00. HISTLD00 uses the inline `DBPROC` procedures (`CONNECT TO POSMVP`) instead. Docs' §2.2 flow (TRNVAL00→POSUPD00→HISTLD00→DB2CONN→DB2CMT→DB2STAT→RPTGEN00) names programs that don't exist (`TRNVAL00`, `POSUPD00`, `RPTGEN00`) and a call chain not present in code. | `system-architecture.md §5.1, §2.2`; grep of CALLs |
| D-08 | **`DELAY` subprogram is not in the repo**; `DB2-RETRY-WAIT = 100` has no unit. | `DB2CONN.cbl:86`; `DBPROC.cpy:21` |
| D-09 | **DB2CONN's connection state and DB2CMT/DB2STAT counters live in WORKING-STORAGE** without `IS INITIAL`; correctness depends on the subprogram staying resident between CALLs in the same run unit (true on z/OS batch, undefined under CICS/other). `DISC` is a no-op if `CONN` was not done in the same run unit. | `DB2CONN.cbl:26-31,110`; `DB2CMT.cbl:25-28` |
| D-10 | **DB2STAT SELECT INTO group mismatch**: 8 columns selected into an 11-field group `WS-STATS-RECORD` (precompiler error -> would not bind). Also `WS-CPU-TIME`/`WS-ELAPSED-TIME` COMP-3 host vars for `DECIMAL(11,2)` are fine, but the `NUMVAL` on a non-numeric timestamp slice makes the values meaningless (BR-44). CPU time is a hard-coded 65 % of elapsed. | `DB2STAT.cbl:181-186,190-198` |
| D-11 | **RTNCDE00 inserts a 16-digit numeric group (`YYYYMMDDhhmmsscc`) into a TIMESTAMP column**; not a valid timestamp string → SQLCODE −180 at run time → response 8 on every LOG call. Same program declares `ENVIRONMENT DIVISION.` with no sections and INCLUDEs SQLCA under a level-01 group (non-standard). | `RTNCDE00.cbl:15-26,94-111` |
| D-12 | **RTNANA00 fetches into numeric-edited fields** (`PIC ZZZ,ZZ9`), which are not valid host variables (precompiler error). Infinite loop on any negative SQLCODE during FETCH. `WS-HEADER2` totals 133 (30+73+30) but title text is left-aligned in a 73-byte field, not centered. | `RTNANA00.cbl:85-93,144-146,171-178` |
| D-13 | **`DBPROC.cpy` mixes PROCEDURE DIVISION paragraphs into a copybook that is COPYed in WORKING-STORAGE** (`DB2ERR:25`, `DB2CONN:23`, `DB2CMT:22`, `DB2STAT:33`, `HISTLD00:45`) — a compile error as written. Its `CONNECT TO POSMVP` hard-codes the subsystem; docs call the data-base "POSMVP" only in DDL tablespace names. | `DBPROC.cpy:26-63` |
| D-14 | **Transaction type codes disagree**: `COMMON.cpy` uses `BU/SL/TR/FE`; data dictionary §5.1 uses `BY/SL/FE`. | `COMMON.cpy:25-29` vs `data-dictionary.md §5.1` |
| D-15 | **Two return-code constant sets with different names/usage** (`ERR-*` COMP in ERRHAND; `RC-*` DISPLAY in COMMON; `RC-*` COMP in template) and docs naming (12 = "Critical error, abend", 16 = "Environment error") vs code (`ERR-SEVERE`=12, `ERR-TERMINAL`/`RC-CRITICAL`=16). No program in scope COPYs `COMMON.cpy`. | `ERRHAND.cpy:20-25`, `COMMON.cpy:8-13`, `data-dictionary.md §8.2` |
| D-16 | **Two severity scales**: ERRLOG `ERROR_SEVERITY` 1–4 (Info/Warn/Error/Severe) vs return codes 0/4/8/12/16 vs RTNCDE00 status buckets (0 / 1–4 / 5–8 / else). DB2ERR RETR returns a 1–4 severity *as* a return code (BR-21), mixing scales. | `DBTBLS.cpy:40-44`, `RTNCDE00.cbl:70-79`, `DB2ERR.cbl:193` |
| D-17 | **ERRPROC's sequential log DDNAME is `ERRLOG`, the same name as the DB2 table** — two different "error logs" (file for ERRPROC, table for DB2ERR) that S7's `RPTAUD00` reads only the file of. Docs' "Error Log" is drawn as a single store. | `ERRPROC.cbl:18`, `ERRLOG.sql:15`, `RPTAUD00.cbl:23`, `system-architecture.md §6.1.2` |
| D-18 | **`ERR-TIMESTAMP` (18 bytes) truncates the 26-byte `TIME STAMP`** to `YYYY-MM-DD-HH.MM.S` (loses seconds' units and micros). | `ERRPROC.cbl:35,69`; `ERRHAND.cpy:31-33` |
| D-19 | **Templates are not linked or invoked by any program**; `error-handling.cbl` calls `CEE3ABD` (LE abend) which no production program does. `db2-handling.cbl` connects to `sample`, not `POSMVP`, and declares a `PORTFOLIO` table whose columns match neither `PORTFOLIO_MASTER` in the data dictionary nor any DDL in `src/database`. | `templates/**` |
| D-20 | **RTNCDE00 is documented as "Maintains return code audit trail / integrates with error handling framework"** but never calls ERRPROC/AUDPROC and has no callers (BR-54). RTNANA00 is documented as "Generates trend analysis / Identifies error patterns" but only produces per-program S/W/E/F counts with no time dimension. | `RTNCDE00.cbl:3-9`, `RTNANA00.cbl:3-9` |
| D-21 | `DB2ERR` REPLACING clause renames `POSHIST-RECORD` to `WS-POSHIST-REC` inside a level-01 `WS-ERRLOG-REC` group, nesting two 01-levels — invalid COBOL; the intent (host structure for ERRLOG only) is clear. `WS-DUP-KEY`/`WS-NOT-FOUND` etc. are `S9(8)` DISPLAY compared to binary SQLCODE (fine) but `WS-CONNECTION-ERROR -30081` also appears hard-coded in DB2CONN. | `DB2ERR.cbl:18-22,31-36`; `DB2CONN.cbl:95` |
| D-22 | The plan (`§2 S8`) says S8 owns "`AUDITLOG`" as data; there is **no AUDITLOG DB2 table** — only the `AUDFILE`/`AUDITLOG` sequential file (380-byte `AUDIT-RECORD`). `db2-definitions.sql` defines neither `ERRLOG`, `RTNCODES` nor an audit table. | `AUDPROC.cbl:17-18`, `RPTAUD00.cbl:17`, `src/database/db2/db2-definitions.sql` |

Missing / stub programs relevant to S8: `DELAY` (called, absent), `RTNCDE00` (present but dead code), `TRNVAL00`/`POSUPD00`/`RPTGEN00` (documented callers of DB2 plumbing, absent), `POSUPDT.cbl` (documented as depending on DB2CONN/ERRPROC; file is empty per plan §1). Templates `ERRHANDL`, `FILEHNDL`, `PROGNAME`, `DB2HNDL` are skeletons, not programs.

---

## 6. Proposed Java design (library `libs/platform-common`)

Gradle module `platform-common` + auto-configuration `platform-common-spring-boot-starter`. No controllers (not a service). Target Postgres per plan (F1 owns Flyway; S8 supplies the two table definitions below).

```
com.cog.portfolio.platform
├── returncode/
│   ├── ReturnCode            enum SUCCESS(0) WARNING(4) ERROR(8) SEVERE(12) TERMINAL(16)   [BR-61]
│   │                         + static of(int) with RTNCDE00 bucketing → RunStatus         [BR-50]
│   ├── RunStatus             enum S/W/E/F
│   ├── ReturnCodeTracker     @Component (job-scoped): init/set/get (highest-retention)     [BR-48..51]
│   ├── ReturnCodeLog         @Entity → table return_code_log (RTNCODES)                    [BR-52, §3.7]
│   ├── ReturnCodeLogRepository  JpaRepository + analyze(program, from, to) → Count/Max/Min [BR-53]
│   └── ReturnCodeJobListener JobExecutionListener: maps ExitStatus↔ReturnCode, logs row   (replaces RTNCDE00 'L')
├── error/
│   ├── ErrorCategory         enum VSAM/VALIDATION/PROCESSING/SYSTEM                        [BR-63]
│   ├── ErrorSeverity         enum INFO(1) WARN(2) ERROR(3) SEVERE(4)                       [§3.6]
│   ├── ErrorType             enum SYSTEM/APPLICATION/DATA
│   ├── ErrorReport           record(programId, category, code, ReturnCode severity, text, details)
│   ├── PlatformException     RuntimeException base: carries ErrorReport + ReturnCode
│   │   ├── ValidationException (VL, WARNING/ERROR), ProcessingException (PR), SystemException (SY),
│   │   └── DataAccessPlatformException (wraps SQLException, carries Db2Diagnosis)
│   ├── ErrorReporter         @Service: report(ErrorReport) → ReturnCode  (=severity, BR-06);
│   │                         writes ErrorLog row + SLF4J structured log (replaces ERRLOG seq file + DISPLAY)
│   ├── ErrorLog              @Entity → table error_log (ERRLOG DDL, §3.6) incl. real sqlcode/sqlstate columns (fix D-06; OQ-1)
│   ├── ErrorLogRepository    JpaRepository: findLatestByProgramId (BR-21), deleteOlderThan (BR-69)
│   ├── Db2ErrorClassifier    pure function SQLException → Db2Diagnosis(severity, retryable, message, ReturnCode)   [BR-18, BR-20]
│   └── PlatformExceptionTranslator  Spring PersistenceExceptionTranslator using the classifier
├── audit/
│   ├── AuditType / AuditAction / AuditStatus  enums                                        [BR-12]
│   ├── AuditEvent            record(occurredAt, systemId, userId, program, terminal, type, action, status,
│   │                                portfolioId, accountNo, beforeImage, afterImage, message)  [§3.2]
│   ├── AuditPublisher        interface publish(AuditEvent) → returns ReturnCode(0|8)            [BR-11]
│   ├── SpringEventAuditPublisher  ApplicationEventPublisher impl (default)
│   ├── OutboxAuditPublisher  writes audit_event table + relays to broker topic audit-events (for S7)
│   └── AuditEventEntity      @Entity audit_event (new; replaces AUDFILE — OQ-2)
├── tx/
│   ├── PlatformTransactionConfig  @Configuration: JpaTransactionManager, isolation READ_COMMITTED (PORTPLAN CS, BR-72)
│   ├── RetryConfig           Spring Retry template: maxAttempts 3, fixed backoff 100 ms, retryOn = classifier.retryable
│   │                         (−911/−913 → retry; connection errors → fail)                    [BR-18, BR-24..26]
│   └── ChunkCommitPolicy     note: DB2CMT frequency rule (BR-32) == Spring Batch commit-interval; force-flag == step end
├── metrics/
│   ├── JobMetrics            Micrometer: counters rows.read/inserted/updated/deleted, commits, rollbacks; Timer job.elapsed
│   │                         tagged job=<programId>   (replaces DB2STAT / SESSION.DBSTATS)
│   └── JobMetricsStepListener  StepExecutionListener populating from StepExecution counts
├── common/
│   ├── PortfolioStatus, TransactionType, CurrencyCode enums                                 [BR-68]
│   ├── VsamStatus enum + messages                                                           [BR-64]
│   └── CobolTimestamps       parse/format DB2 `YYYY-MM-DD-HH.MM.SS.ffffff` ↔ LocalDateTime
├── report/
│   └── ReturnCodeAnalysisJob  Spring Batch job (tasklet): GROUP BY program_id S/W/E/F counts + TOTALS → text/CSV writer
│                              (replaces RTNANA00; fixed-width 133-col layout kept for parity tests)   [BR-56, BR-57]
└── autoconfigure/
    └── PlatformCommonAutoConfiguration (+ META-INF/spring/...AutoConfiguration.imports)
```

Persistence (handed to F1 for Flyway): `error_log` (from `ERRLOG.sql`, plus `sqlcode INTEGER`, `sqlstate CHAR(5)` if OQ-1 approved; PK `(error_timestamp, program_id)` should become a surrogate id to avoid BR-70 collisions — OQ-5), `return_code_log` (from `RTNCODES.sql`), `audit_event` (from `AUDITLOG.cpy`).

Configuration (`platform.common.*`): `error-log.retention-days`, `audit.publisher=spring|outbox`, `retry.max-attempts=3`, `retry.backoff-ms=100`, `metrics.enabled`.

Events: `AuditEvent` (published for every create/update/delete/inquire/login/logout/startup/shutdown), `ErrorLogged` (optional Spring event for alerting). No REST.

Retired without replacement logic: `DB2CONN` (pool + health indicator), `DB2CMT` (transaction manager/chunking), `DB2STAT` (Micrometer), `DBPROC` inline procedures, `DELAY`.

Parity-test guidance for F2/S10: the truth-table rows of BR-18, BR-20, BR-50, BR-62 and the RTNANA00 report layout (BR-57) are the testable behaviors; BR-16/17/44 and D-01..D-03 are defects to be **not** reproduced.

---

## 7. Open questions

- **OQ-1** DB2ERR intends to store SQLCODE/SQLSTATE in `ERROR_CODE` but truncates to `SQLCODE:` (BR-16). Should the Java `error_log` add explicit `sqlcode`/`sqlstate` columns (schema change) or keep an 8-char `error_code` for parity?
- **OQ-2** Audit trail is a sequential file (`AUDFILE`) today. Target: DB table, event stream (Kafka/RabbitMQ), or both (outbox)? Which retention/immutability (compliance) requirements apply to audit and error logs? The DDL only sets ERRLOG retention via a manual `ERRLOG_CLEANUP(days)` procedure — what value of `RETENTION_DAYS` is used in production?
- **OQ-3** `DB2-RETRY-WAIT = 100` — unit (ms/cs/s)? `DELAY` is external; confirm intended backoff (fixed vs exponential) and whether deadlock/timeout retry (flagged `Y` by DB2ERR but acted on by no caller) should be automatic in Java.
- **OQ-4** Which severity scale is canonical for the business: return codes 0/4/8/12/16, ERRLOG 1–4, or RTNCDE00 buckets (0 / 1–4 / 5–8 / other)? RC 2 is a "warning" for RTNCDE00 but is not a valid code in the ladder; RC 16 and RC 12 both map to `F`. Decide a single mapping.
- **OQ-5** `ERRLOG`/`RTNCODES` primary keys `(timestamp, program_id)` collide for same-microsecond errors. Accept a surrogate key (recommended) or keep for parity?
- **OQ-6** Because ERRPROC/AUDPROC/DB2ERR were called with mismatched parameter layouts (D-01..D-03), production logs (if any exist) contain shifted fields. Is there any historical `ERRLOG`/`AUDFILE` data that must be migrated/re-parsed, or do we start clean?
- **OQ-7** The documented error catalogue (E001–E004, W001–W002) and business-key columns (account/fund/trans id) in `data-dictionary.md §3.2/§6` are not implemented. Should `platform-common` define this catalogue as the canonical `ErrorCode` enum for S1–S3 to use (INFERRED FROM DOCS)?
- **OQ-8** `RTNCDE00` is dead code and `RTNANA00` is non-compilable as written. Is per-program return-code history (and the RTNANA report) a required business capability, or can it be dropped in favour of Spring Batch's job repository + dashboards?
- **OQ-9** DB2STAT's "CPU time" is 65 % of (an invalid) elapsed computation. Does anyone consume `CPU_TIME` (e.g. `RPTSTA00` statistics report, chargeback)? If yes, define what it should mean in Java.
- **OQ-10** Transaction type codes: `BU/SL/TR/FE` (code) vs `BY/SL/FE` (docs). Which set is authoritative for the shared enum, and is `TR` (transfer) a real transaction type?
- **OQ-11** Docs name the 16 code "Environment error" and 12 "Critical error, abend"; code names them `TERMINAL`/`CRITICAL` and `SEVERE`. Confirm business labels, and whether any code should trigger an abend-equivalent (fail-fast, non-restartable) in the orchestrator.
