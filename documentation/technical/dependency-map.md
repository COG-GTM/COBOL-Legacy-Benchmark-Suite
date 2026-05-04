# COBOL Legacy Benchmark Suite — Dependency Map

> Auto-generated dependency mapping for the Investment Portfolio Management System.
> Each program lists its copybooks, program CALLs / CICS LINKs, DB2 tables, VSAM/file I/O, BMS maps, and CICS resources.

---

## Table of Contents

- [System Overview](#system-overview)
- [Dependency Matrix](#dependency-matrix)
- [Batch Layer](#batch-layer)
- [Online (CICS) Layer](#online-cics-layer)
- [Portfolio Management Layer](#portfolio-management-layer)
- [Common / Shared Services](#common--shared-services)
- [Utility Programs](#utility-programs)
- [Test Programs](#test-programs)
- [Copybook Index](#copybook-index)
- [DB2 Table Index](#db2-table-index)
- [VSAM File Index](#vsam-file-index)
- [Cross-Reference: Copybook Usage](#cross-reference-copybook-usage)
- [Cross-Reference: Program Call Graph](#cross-reference-program-call-graph)

---

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    ONLINE (CICS) LAYER                         │
│  INQONLN ──┬── INQPORT (VSAM POSFILE)                         │
│            ├── INQHIST (DB2 POSHIST)                           │
│            ├── SECMGR  (DB2 AUTHFILE, AUDITLOG)                │
│            └── ERRHNDL (DB2 ERRLOG)                            │
│                 ├── DB2ONLN (DB2 Connection Pool)              │
│                 ├── DB2RECV (Recovery Manager)                 │
│                 └── CURSMGR (Cursor Manager)                   │
├─────────────────────────────────────────────────────────────────┤
│                     BATCH LAYER                                │
│  PRCSEQ00 (Sequencer) ──► BCHCTL00 (Controller)               │
│  RCVPRC00 (Recovery)  ──► BCHCTL00                             │
│  HISTLD00 (History Loader) ──► DB2 POSHIST                     │
│  RPTPOS00 / RPTAUD00 / RPTSTA00 (Reports)                     │
│  RTNANA00 / RTNCDE00 (Return Code Analysis/Handler)            │
│  CKPRST   (Checkpoint/Restart)                                 │
│  POSUPDT  (Position Update — stub)                             │
├─────────────────────────────────────────────────────────────────┤
│                  PORTFOLIO MANAGEMENT                          │
│  PORTMSTR (Master CRUD) │ PORTADD (Batch Add)                  │
│  PORTUPDT (Batch Update) │ PORTDEL (Batch Delete)              │
│  PORTREAD (Sequential Read) │ PORTTRAN (Transaction Processing)│
│  PORTTEST (Test Data Gen) │ PORTVALD (Validation Subroutine)   │
├─────────────────────────────────────────────────────────────────┤
│                   COMMON SERVICES                              │
│  ERRPROC (Error Processing)  │ AUDPROC (Audit Trail)           │
│  DB2CONN (Connection Mgr)    │ DB2CMT  (Commit Controller)     │
│  DB2ERR  (SQL Error Handler) │ DB2STAT (Statistics Collector)   │
├─────────────────────────────────────────────────────────────────┤
│                 UTILITY & TEST                                 │
│  UTLMNT00 (File Maintenance) │ UTLMON00 (System Monitoring)    │
│  UTLVAL00 (Data Validation)  │ TSTGEN00 (Test Data Generator)  │
│  TSTVAL00 (Test Validation Suite)                              │
└─────────────────────────────────────────────────────────────────┘
```

---

## Dependency Matrix

| Program   | Layer     | Copybooks                           | Calls/Links               | DB2 Tables                  | VSAM/Files                        | BMS Maps  | CICS |
|-----------|-----------|--------------------------------------|----------------------------|-----------------------------|-----------------------------------|-----------|------|
| BCHCTL00  | Batch     | BCHCTL, BCHCON, ERRHAND              | ERRPROC                    | —                           | BCHCTL (Indexed)                  | —         | No   |
| CKPRST    | Batch     | CKPRST, RETHND                       | —                          | —                           | CKPTFILE (Indexed)                | —         | No   |
| HISTLD00  | Batch     | HISTREC, BCHCTL, DBTBLS, SQLCA, DBPROC, ERRHAND, BCHCON | CONNECT-TO-DB2 (internal)  | POSHIST (INSERT)            | TRANHIST, BCHCTL (Indexed)        | —         | No   |
| POSUPDT   | Batch     | *(empty stub)*                       | —                          | —                           | —                                 | —         | No   |
| PRCSEQ00  | Batch     | PRCSEQ, BCHCTL, BCHCON, ERRHAND     | ERRPROC                    | —                           | PRCSEQ, BCHCTL (Indexed)          | —         | No   |
| RCVPRC00  | Batch     | BCHCTL, PRCSEQ, BCHCON, ERRHAND     | ERRPROC                    | —                           | BCHCTL, PRCSEQ (Indexed)          | —         | No   |
| RPTAUD00  | Batch     | AUDITLOG, ERRHAND, RTNCODE          | —                          | —                           | AUDITLOG, ERRLOG, RPTFILE         | —         | No   |
| RPTPOS00  | Batch     | POSREC, TRNREC, RTNCODE, ERRHAND    | —                          | —                           | POSMSTRE, TRANHIST, RPTFILE       | —         | No   |
| RPTSTA00  | Batch     | DB2STAT (file copy), BCHCTL, RTNCODE, ERRHAND | —              | —                           | DB2STATS, BCHSTATS, RPTFILE       | —         | No   |
| RTNANA00  | Batch     | SQLCA (INCLUDE)                      | —                          | RTNCODES (SELECT)           | RPTFILE                           | —         | No   |
| RTNCDE00  | Batch     | RTNCODE, SQLCA (INCLUDE)             | —                          | RTNCODES (INSERT, SELECT)   | —                                 | —         | No   |
| INQONLN   | Online    | INQCOM, ERRHND                       | INQPORT, INQHIST, ERRHNDL, SECMGR | —               | —                                 | INQMAP, INQMNU (INQSET) | Yes  |
| INQPORT   | Online    | INQCOM, POSREC, SQLPOS (INCLUDE)     | —                          | *(SQLPOS for DB2 position)* | POSFILE (CICS READ)               | POSMAP (INQSET) | Yes  |
| INQHIST   | Online    | INQCOM                               | DB2ONLN, DB2RECV, CURSMGR  | POSHIST (SELECT via cursor) | —                                 | —         | Yes  |
| DB2ONLN   | Online    | ERRHND, SQLCA (INCLUDE)              | —                          | POSMVP (CONNECT)            | —                                 | —         | Yes  |
| DB2RECV   | Online    | ERRHND, DB2REQ, SQLCA (INCLUDE)      | DB2ONLN, ERRHNDL           | *(ROLLBACK)*                | —                                 | —         | Yes  |
| CURSMGR   | Online    | SQLCA (INCLUDE)                      | —                          | *(dynamic cursors)*         | —                                 | —         | Yes  |
| ERRHNDL   | Online    | ERRHND, SQLCA (INCLUDE)              | —                          | ERRLOG (INSERT)             | —                                 | —         | Yes  |
| SECMGR    | Online    | ERRHND, SQLCA (INCLUDE)              | —                          | AUTHFILE (SELECT), AUDITLOG (INSERT) | —                        | —         | Yes  |
| PORTMSTR  | Portfolio | *(inline record def)*                | —                          | —                           | PORTFILE (Indexed, CRUD)          | —         | No   |
| PORTADD   | Portfolio | PORTFLIO                             | —                          | —                           | PORTFILE (Indexed), INPTFILE      | —         | No   |
| PORTUPDT  | Portfolio | PORTFLIO                             | —                          | —                           | PORTFILE (Indexed), UPDTFILE      | —         | No   |
| PORTDEL   | Portfolio | PORTFLIO                             | —                          | —                           | PORTFILE (Indexed), DELEFILE, AUDFILE | —     | No   |
| PORTREAD  | Portfolio | PORTFLIO                             | —                          | —                           | PORTFILE (Indexed)                | —         | No   |
| PORTTRAN  | Portfolio | TRNREC, PORTREC, ERRHAND, AUDITLOG   | —                          | —                           | TRANFILE, PORTFILE (Indexed)      | —         | No   |
| PORTTEST  | Portfolio | PORTFLIO, ERRHAND                    | —                          | —                           | TESTFILE                          | —         | No   |
| PORTVALD  | Portfolio | PORTVAL                              | —                          | —                           | —                                 | —         | No   |
| AUDPROC   | Common    | AUDITLOG                             | —                          | —                           | AUDFILE (Sequential)              | —         | No   |
| ERRPROC   | Common    | ERRHAND                              | —                          | —                           | ERRLOG (Sequential)               | —         | No   |
| DB2CONN   | Common    | SQLCA, DBPROC, ERRHAND               | ERRPROC, DELAY             | *(CONNECT/DISCONNECT)*      | —                                 | —         | No   |
| DB2CMT    | Common    | SQLCA, DBPROC, ERRHAND               | —                          | *(COMMIT/ROLLBACK/SAVEPOINT)* | —                               | —         | No   |
| DB2ERR    | Common    | DBTBLS, SQLCA, DBPROC, ERRHAND       | —                          | ERRLOG (INSERT)             | —                                 | —         | No   |
| DB2STAT   | Common    | SQLCA, DBPROC, ERRHAND               | —                          | SESSION.DBSTATS (temp table) | —                                | —         | No   |
| UTLMNT00  | Utility   | RTNCODE, ERRHAND                     | —                          | —                           | CTLFILE, ARCHFILE, RPTFILE        | —         | No   |
| UTLMON00  | Utility   | DB2STAT (file copy), RTNCODE, ERRHAND | —                         | —                           | MONCFG, MONLOG, ALERTS, DB2STATS  | —         | No   |
| UTLVAL00  | Utility   | POSREC, TRNREC, RTNCODE, ERRHAND    | —                          | —                           | VALCTL, POSMSTRE, TRANHIST, ERRRPT | —        | No   |
| TSTGEN00  | Test      | PORTFLIO, TRNREC, RTNCODE, ERRHAND  | —                          | —                           | TSTCFG, PORTOUT, TRANOUT, RANDSEED | —        | No   |
| TSTVAL00  | Test      | RTNCODE, ERRHAND                     | —                          | —                           | TESTCASE, EXPECTED, ACTUAL, TESTRPT | —       | No   |

---

## Batch Layer

### BCHCTL00 — Batch Control Processor
- **Purpose**: Manages batch job lifecycle — initialization, prerequisite checking, status updates, and termination.
- **Copybooks**: `BCHCTL`, `BCHCON`, `ERRHAND`
- **Calls**: `ERRPROC` (error handling)
- **Files**: `BCHCTL` (Indexed VSAM — batch control records)
- **Linkage**: Accepts `LS-CONTROL-REQUEST` with functions INIT/CHEK/UPDT/TERM.

### CKPRST — Checkpoint/Restart Handler
- **Purpose**: Provides checkpoint/restart framework for resuming failed batch jobs.
- **Copybooks**: `CKPRST`, `RETHND`
- **Files**: `CKPTFILE` (Indexed VSAM — checkpoint records)
- **Linkage**: Accepts `CHECKPOINT-CONTROL` and `RETURN-STATUS`.

### HISTLD00 — Position History DB2 Load
- **Purpose**: Loads transaction history from VSAM to DB2 POSHIST table with commit-point processing.
- **Copybooks**: `HISTREC`, `BCHCTL`, `DBTBLS`, `SQLCA`, `DBPROC`, `ERRHAND`, `BCHCON`
- **DB2**: `POSHIST` (INSERT)
- **Files**: `TRANHIST` (Indexed VSAM — input), `BCHCTL` (Indexed VSAM — control)
- **Key Logic**: Commit every 1000 records; max 100 errors before abort.

### POSUPDT — Position Update (Stub)
- **Purpose**: Placeholder for position update batch processing. Currently empty.

### PRCSEQ00 — Process Sequence Manager
- **Purpose**: Manages the ordered execution sequence of batch processes. Handles dependency checking and dispatching.
- **Copybooks**: `PRCSEQ`, `BCHCTL`, `BCHCON`, `ERRHAND`
- **Calls**: `ERRPROC`
- **Files**: `PRCSEQ` (Indexed VSAM), `BCHCTL` (Indexed VSAM)

### RCVPRC00 — Process Recovery Handler
- **Purpose**: Handles recovery of failed batch processes — supports process-level, sequence-level, and full recovery.
- **Copybooks**: `BCHCTL`, `PRCSEQ`, `BCHCON`, `ERRHAND`
- **Calls**: `ERRPROC`
- **Files**: `BCHCTL` (Indexed VSAM), `PRCSEQ` (Indexed VSAM)

### RPTAUD00 — Audit Report Generator
- **Purpose**: Generates system audit report from audit trail and error log files.
- **Copybooks**: `AUDITLOG`, `ERRHAND`, `RTNCODE`
- **Files**: `AUDITLOG` (Indexed input), `ERRLOG` (Indexed input), `RPTFILE` (Sequential output)

### RPTPOS00 — Daily Position Report Generator
- **Purpose**: Generates daily position report with portfolio summaries, transaction activity, and exceptions.
- **Copybooks**: `POSREC`, `TRNREC`, `RTNCODE`, `ERRHAND`
- **Files**: `POSMSTRE` (Indexed input), `TRANHIST` (Indexed input), `RPTFILE` (Sequential output)

### RPTSTA00 — System Statistics Report Generator
- **Purpose**: Generates system performance and statistics report including DB2 and batch metrics.
- **Copybooks**: `DB2STAT` (as file copy), `BCHCTL`, `RTNCODE`, `ERRHAND`
- **Files**: `DB2STATS` (Indexed input), `BCHSTATS` (Indexed input), `RPTFILE` (Sequential output)

### RTNANA00 — Return Code Analysis Utility
- **Purpose**: Analyzes return codes across the system from DB2, generates trend analysis reports.
- **DB2**: `RTNCODES` (SELECT for analysis — via `EXEC SQL INCLUDE SQLCA`)
- **Files**: `RPTFILE` (Sequential output)

### RTNCDE00 — Standard Return Code Handler
- **Purpose**: Manages standardized return codes, logging, and analysis across the system.
- **Copybooks**: `RTNCODE`, SQLCA (INCLUDE)
- **DB2**: `RTNCODES` (INSERT for logging, SELECT for analysis)
- **Linkage**: Accepts `RC-REQUEST-AREA` with functions INIT/SET/GET/LOG/ANALYZE.

---

## Online (CICS) Layer

### INQONLN — Portfolio Online Inquiry Main Handler
- **Purpose**: Main CICS transaction handler (Transaction ID: `PINQ`). Routes user requests to position inquiry, history inquiry, or menu display.
- **Copybooks**: `INQCOM`, `ERRHND`
- **CICS Links**: `INQPORT`, `INQHIST`, `ERRHNDL`, `SECMGR`
- **BMS Maps**: `INQMAP` / `INQMNU` in mapset `INQSET`
- **CICS Resources**: Uses HANDLE CONDITION, RECEIVE MAP, SEND MAP, ASSIGN USERID.

### INQPORT — Portfolio Position Inquiry Handler
- **Purpose**: Retrieves current portfolio positions from VSAM POSFILE and displays via BMS.
- **Copybooks**: `INQCOM`, `POSREC`, SQLPOS (INCLUDE)
- **CICS File**: `POSFILE` (READ via CICS)
- **BMS Maps**: `POSMAP` in mapset `INQSET`

### INQHIST — Transaction History Inquiry Handler
- **Purpose**: Retrieves transaction history from DB2 POSHIST using array fetching via cursor.
- **Copybooks**: `INQCOM`
- **CICS Links**: `DB2ONLN` (connection), `DB2RECV` (recovery), `CURSMGR` (cursor management)
- **DB2**: `POSHIST` (SELECT via cursor)

### DB2ONLN — Online DB2 Connection Manager
- **Purpose**: Manages DB2 connection pool for CICS online programs.
- **Copybooks**: `ERRHND`, SQLCA (INCLUDE)
- **DB2**: `POSMVP` (CONNECT TO), SYSIBM.SYSDUMMY1 (status check)

### DB2RECV — DB2 Recovery Manager
- **Purpose**: Handles DB2 connection failures with retry logic, transaction rollback, and cursor recovery.
- **Copybooks**: `ERRHND`, `DB2REQ`, SQLCA (INCLUDE)
- **CICS Links**: `DB2ONLN` (reconnection), `ERRHNDL` (error logging)
- **CICS Commands**: DELAY (retry interval)

### CURSMGR — Cursor Manager
- **Purpose**: Manages DB2 cursor declarations, lifecycle, and array fetching for online programs.
- **DB2**: Dynamic cursor operations (DECLARE, OPEN, FETCH, CLOSE)

### ERRHNDL — Centralized Error Handler (Online)
- **Purpose**: Processes all online errors — logs to DB2, formats messages, determines recovery action.
- **Copybooks**: `ERRHND`, SQLCA (INCLUDE)
- **DB2**: `ERRLOG` (INSERT)
- **CICS**: Receives control via COMMAREA (DFHCOMMAREA)

### SECMGR — Security Manager
- **Purpose**: Validates CICS user credentials, checks DB2 authorization, and maintains security audit trail.
- **Copybooks**: `ERRHND`, SQLCA (INCLUDE)
- **DB2**: `AUTHFILE` (SELECT for authorization), `AUDITLOG` (INSERT for audit)
- **CICS Commands**: ASSIGN USERID/TERMID/TRANSID

---

## Portfolio Management Layer

### PORTMSTR — Portfolio Master File Maintenance
- **Purpose**: Core CRUD operations for Portfolio records via VSAM indexed file.
- **Files**: `PORTFILE` (Indexed VSAM — dynamic access for C/R/U/D)
- **Linkage**: Accepts `LS-COMMAND-AREA` with commands C/R/U/D.
- **Note**: Uses inline record definition, not PORTFLIO copybook.

### PORTADD — Portfolio Addition Program
- **Purpose**: Batch creation of new portfolio records from sequential input file.
- **Copybooks**: `PORTFLIO`
- **Files**: `PORTFILE` (Indexed VSAM — I/O), `INPTFILE` (Sequential input)

### PORTUPDT — Portfolio Update Program
- **Purpose**: Batch updates to existing portfolio records (status, name, value changes).
- **Copybooks**: `PORTFLIO`
- **Files**: `PORTFILE` (Indexed VSAM — I/O), `UPDTFILE` (Sequential input)

### PORTDEL — Portfolio Deletion Program
- **Purpose**: Processes portfolio deletion requests with audit trail.
- **Copybooks**: `PORTFLIO`
- **Files**: `PORTFILE` (Indexed VSAM — I/O), `DELEFILE` (Sequential input), `AUDFILE` (Sequential audit output)

### PORTREAD — Portfolio Record Reading Program
- **Purpose**: Demonstrates sequential reading of portfolio file — displays all records.
- **Copybooks**: `PORTFLIO`
- **Files**: `PORTFILE` (Indexed VSAM — sequential read)

### PORTTRAN — Portfolio Transaction Processing
- **Purpose**: Processes buy/sell/transfer/fee transactions against portfolio positions.
- **Copybooks**: `TRNREC`, `PORTREC`, `ERRHAND`, `AUDITLOG`
- **Files**: `TRANFILE` (Sequential input), `PORTFILE` (Indexed VSAM — I/O)

### PORTTEST — Portfolio Test Data Generator
- **Purpose**: Generates randomized test portfolio records for testing.
- **Copybooks**: `PORTFLIO`, `ERRHAND`
- **Files**: `TESTFILE` (Sequential output)

### PORTVALD — Portfolio Validation Subroutine
- **Purpose**: Validates portfolio data elements — ID format, account numbers, types, amounts.
- **Copybooks**: `PORTVAL`
- **Linkage**: Callable subroutine accepting `LS-VALIDATION-REQUEST`.

---

## Common / Shared Services

### ERRPROC — Standard Error Processing Subroutine
- **Purpose**: Central batch error handler — writes to error log file and displays formatted error messages.
- **Copybooks**: `ERRHAND`
- **Files**: `ERRLOG` (Sequential — extends)
- **Called By**: BCHCTL00, PRCSEQ00, RCVPRC00, DB2CONN

### AUDPROC — Audit Trail Processing Subroutine
- **Purpose**: Writes structured audit records to sequential audit file.
- **Copybooks**: `AUDITLOG`
- **Files**: `AUDFILE` (Sequential — extends)

### DB2CONN — DB2 Connection Manager (Batch)
- **Purpose**: Manages DB2 connections for batch programs with retry logic.
- **Copybooks**: `SQLCA`, `DBPROC`, `ERRHAND`
- **Calls**: `ERRPROC`, `DELAY` (wait between retries)
- **DB2**: CONNECT/DISCONNECT/status check via SYSIBM.SYSDUMMY1

### DB2CMT — DB2 Commit Controller
- **Purpose**: Controls commit/rollback/savepoint operations for batch DB2 processing.
- **Copybooks**: `SQLCA`, `DBPROC`, `ERRHAND`
- **DB2**: COMMIT WORK, ROLLBACK WORK, SAVEPOINT operations

### DB2ERR — DB2 SQL Error Handler
- **Purpose**: Logs SQL errors to ERRLOG table, diagnoses error categories (deadlock, timeout, dup key, etc.).
- **Copybooks**: `DBTBLS`, `SQLCA`, `DBPROC`, `ERRHAND`
- **DB2**: `ERRLOG` (INSERT)

### DB2STAT — DB2 Statistics Collector
- **Purpose**: Collects and stores DB2 processing statistics (rows read/inserted/updated, CPU time, etc.).
- **Copybooks**: `SQLCA`, `DBPROC`, `ERRHAND`
- **DB2**: `SESSION.DBSTATS` (temporary table — CREATE, INSERT, UPDATE, SELECT)

---

## Utility Programs

### UTLMNT00 — File Maintenance Utility
- **Purpose**: Performs file maintenance operations — archive, cleanup, VSAM reorganization, space analysis.
- **Copybooks**: `RTNCODE`, `ERRHAND`
- **Files**: `CTLFILE` (Sequential input), `ARCHFILE` (Sequential output), `RPTFILE` (Sequential output)

### UTLMON00 — System Monitoring Utility
- **Purpose**: Monitors system health — CPU, memory, DASD, DB2 utilization. Generates alerts on threshold breaches.
- **Copybooks**: `DB2STAT` (as file copy), `RTNCODE`, `ERRHAND`
- **Files**: `MONCFG`, `MONLOG`, `ALERTS`, `DB2STATS`

### UTLVAL00 — Data Validation Utility
- **Purpose**: Performs integrity checks, cross-reference validation, format verification, and balance reconciliation.
- **Copybooks**: `POSREC`, `TRNREC`, `RTNCODE`, `ERRHAND`
- **Files**: `VALCTL`, `POSMSTRE`, `TRANHIST`, `ERRRPT`

---

## Test Programs

### TSTGEN00 — Test Data Generator
- **Purpose**: Generates test data sets for system testing — portfolios, transactions, error scenarios, volume tests.
- **Copybooks**: `PORTFLIO`, `TRNREC`, `RTNCODE`, `ERRHAND`
- **Files**: `TSTCFG` (config input), `PORTOUT` (portfolio output), `TRANOUT` (transaction output), `RANDSEED` (random seed input)

### TSTVAL00 — Test Validation Suite
- **Purpose**: Executes and validates test cases — functional, integration, performance, and error testing.
- **Copybooks**: `RTNCODE`, `ERRHAND`
- **Files**: `TESTCASE`, `EXPECTED`, `ACTUAL`, `TESTRPT`

---

## Copybook Index

| Copybook  | Category | Used By                                                                |
|-----------|----------|------------------------------------------------------------------------|
| AUDITLOG  | Common   | AUDPROC, RPTAUD00, PORTTRAN                                          |
| BCHCON    | Batch    | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00                               |
| BCHCTL    | Batch    | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00, RPTSTA00                     |
| CKPRST    | Batch    | CKPRST                                                                |
| COMMON    | Common   | *(available but not directly referenced in current programs)*          |
| DB2REQ    | Online   | DB2RECV                                                               |
| DBPROC    | DB2      | HISTLD00, DB2CMT, DB2CONN, DB2ERR, DB2STAT                           |
| DBTBLS    | DB2      | HISTLD00, DB2ERR                                                      |
| ERRHAND   | Common   | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00, RPTPOS00, RPTSTA00, RPTAUD00, ERRPROC, DB2CMT, DB2CONN, DB2ERR, DB2STAT, PORTTRAN, PORTTEST, TSTGEN00, TSTVAL00, UTLMNT00, UTLMON00, UTLVAL00 |
| ERRHND    | Online   | INQONLN, DB2ONLN, DB2RECV, ERRHNDL, SECMGR                          |
| HISTREC   | Common   | HISTLD00                                                              |
| INQCOM    | Online   | INQONLN, INQPORT, INQHIST                                            |
| PORTFLIO  | Common   | PORTADD, PORTDEL, PORTREAD, PORTUPDT, PORTTEST, TSTGEN00             |
| PORTREC   | Common   | PORTTRAN                                                              |
| PORTVAL   | Common   | PORTVALD                                                              |
| POSREC    | Common   | RPTPOS00, INQPORT, UTLVAL00                                          |
| PRCSEQ    | Batch    | PRCSEQ00, RCVPRC00                                                    |
| RETHND    | Common   | CKPRST                                                                |
| RTNCODE   | Common   | RPTAUD00, RPTPOS00, RPTSTA00, RTNCDE00, UTLMNT00, UTLMON00, UTLVAL00, TSTGEN00, TSTVAL00 |
| SQLCA     | DB2      | HISTLD00, DB2CMT, DB2CONN, DB2ERR, DB2STAT                           |
| TRNREC    | Common   | RPTPOS00, PORTTRAN, UTLVAL00, TSTGEN00                               |

---

## DB2 Table Index

| Table             | Type      | Programs Writing         | Programs Reading           |
|-------------------|-----------|--------------------------|----------------------------|
| POSHIST           | Permanent | HISTLD00 (INSERT)        | INQHIST (SELECT), RTNANA00 |
| ERRLOG            | Permanent | DB2ERR (INSERT), ERRHNDL (INSERT) | —                  |
| RTNCODES          | Permanent | RTNCDE00 (INSERT)        | RTNCDE00 (SELECT), RTNANA00 (SELECT) |
| AUTHFILE          | Permanent | —                        | SECMGR (SELECT)            |
| AUDITLOG (DB2)    | Permanent | SECMGR (INSERT)          | —                          |
| SESSION.DBSTATS   | Temporary | DB2STAT (CREATE, INSERT, UPDATE) | DB2STAT (SELECT)  |

---

## VSAM File Index

| DD Name    | Organization | Key           | Programs Using                                      |
|------------|-------------|---------------|-----------------------------------------------------|
| PORTFILE   | KSDS        | PORT-KEY      | PORTMSTR, PORTADD, PORTUPDT, PORTDEL, PORTREAD, PORTTRAN |
| POSMSTRE   | KSDS        | POS-KEY       | RPTPOS00, UTLVAL00, INQPORT (via CICS POSFILE)      |
| TRANHIST   | KSDS        | TH-KEY/TRAN-KEY | HISTLD00, RPTPOS00, UTLVAL00                      |
| BCHCTL     | KSDS        | BCT-KEY       | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00              |
| PRCSEQ     | KSDS        | PSR-KEY       | PRCSEQ00, RCVPRC00                                   |
| CKPTFILE   | KSDS        | CKR-KEY       | CKPRST                                               |
| DB2STATS   | KSDS        | STAT-KEY      | RPTSTA00, UTLMON00                                   |
| POSFILE    | KSDS (CICS) | Account No    | INQPORT (CICS READ)                                  |

---

## Cross-Reference: Copybook Usage

```
ERRHAND ──► 19 programs (most widely shared copybook)
RTNCODE ──► 9 programs
BCHCTL  ──► 5 programs
BCHCON  ──► 4 programs
PORTFLIO──► 6 programs
SQLCA   ──► 5 programs (batch DB2 layer)
DBPROC  ──► 5 programs (batch DB2 layer)
ERRHND  ──► 5 programs (online layer)
INQCOM  ──► 3 programs (online inquiry)
TRNREC  ──► 4 programs
POSREC  ──► 3 programs
```

---

## Cross-Reference: Program Call Graph

```
BATCH CALL CHAIN:
  BCHCTL00 ──CALL──► ERRPROC
  PRCSEQ00 ──CALL──► ERRPROC
  RCVPRC00 ──CALL──► ERRPROC
  DB2CONN  ──CALL──► ERRPROC, DELAY

ONLINE (CICS LINK) CHAIN:
  INQONLN ──LINK──► INQPORT
  INQONLN ──LINK──► INQHIST
  INQONLN ──LINK──► SECMGR
  INQONLN ──LINK──► ERRHNDL
  INQHIST ──LINK──► DB2ONLN
  INQHIST ──LINK──► DB2RECV
  INQHIST ──LINK──► CURSMGR
  DB2RECV ──LINK──► DB2ONLN
  DB2RECV ──LINK──► ERRHNDL
```

---

## BMS Map Definitions

| Mapset  | Map     | Used By  | Description                       |
|---------|---------|----------|-----------------------------------|
| INQSET  | MENMAP  | INQONLN  | Main menu (3 options + error msg) |
| INQSET  | POSMAP  | INQPORT  | Portfolio position display         |
| INQSET  | HISMAP  | INQHIST  | Transaction history (10-row table) |
| INQSET  | ERRMAP  | ERRHNDL  | Error message display              |

## CICS Resource Definitions (PORTDFN.csd)

| Resource Type | Name    | Details                                    |
|---------------|---------|--------------------------------------------|
| TRANSACTION   | PINQ    | Program: INQONLN, Profile: DFHCICST        |
| PROGRAM       | INQONLN | Language: COBOL, Group: PORTGRP            |
| PROGRAM       | INQPORT | Language: COBOL, Group: PORTGRP            |
| PROGRAM       | INQHIST | Language: COBOL, Group: PORTGRP            |
| PROGRAM       | DB2ONLN | Language: COBOL, Group: PORTGRP            |
| PROGRAM       | CURSMGR | Language: COBOL, Group: PORTGRP            |
| PROGRAM       | DB2RECV | Language: COBOL, Group: PORTGRP            |
| PROGRAM       | SECMGR  | Language: COBOL, Group: PORTGRP            |
| MAPSET        | INQSET  | Group: PORTGRP                             |
| FILE          | POSFILE | DSN: PORTFOLIO.POSITION.VSAM, Read/Browse  |
| DB2ENTRY      | PORTDB2 | Plan: PORTPLAN, Priority: HIGH             |
| DB2TRAN       | PINQ    | Entry: PORTDB2                             |
