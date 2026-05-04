# COBOL Legacy Benchmark Suite — Dependency Mapping

> Auto-generated dependency analysis of the Investment Portfolio Management System.
> Each program is mapped with its copybook includes, program calls, DB2/CICS usage,
> file I/O, and JCL orchestration references.

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Component Summary](#component-summary)
3. [Batch Processing Programs](#batch-processing-programs)
4. [Online (CICS) Programs](#online-cics-programs)
5. [Portfolio Management Programs](#portfolio-management-programs)
6. [Common/Shared Programs](#commonshared-programs)
7. [Utility Programs](#utility-programs)
8. [Test Programs](#test-programs)
9. [Copybook Inventory](#copybook-inventory)
10. [Cross-Reference Matrix](#cross-reference-matrix)
11. [Call Graph Summary](#call-graph-summary)

---

## System Overview

The codebase contains **36 COBOL programs** across 6 modules, **16 copybooks**, **5 DB2 SQL definitions**, **1 VSAM definition**, **15 JCL jobs**, **1 BMS map set**, and **1 CICS CSD definition**.

| Layer        | Programs | Copybooks | JCL Jobs | Key Technologies           |
|-------------|----------|-----------|----------|----------------------------|
| Batch       | 11       | 4         | 4        | DB2 SQL, Sequential Files  |
| Online      | 7        | 3         | —        | CICS, DB2, BMS Maps        |
| Portfolio   | 8        | —         | 6        | VSAM, Sequential Files     |
| Common      | 5        | 3         | —        | DB2 SQL                    |
| Utility     | 3        | —         | 3        | Sequential Files           |
| Test        | 2        | —         | 2        | Sequential Files           |

---

## Component Summary

### Programs by Module

| Module     | Program     | LOC | Description                          |
|-----------|-------------|-----|--------------------------------------|
| batch     | BCHCTL00    | 126 | Batch control/job orchestration      |
| batch     | CKPRST      | 56  | Checkpoint/restart framework         |
| batch     | HISTLD00    | 232 | History loading (DB2)                |
| batch     | POSUPDT     | 0   | Position update (stub/empty)         |
| batch     | PRCSEQ00    | 344 | Process sequencing                   |
| batch     | RCVPRC00    | 301 | Recovery processing                  |
| batch     | RPTAUD00    | 146 | Audit report generation              |
| batch     | RPTPOS00    | 159 | Position report generation           |
| batch     | RPTSTA00    | 184 | Statistics report generation         |
| batch     | RTNANA00    | 209 | Return analysis (DB2)                |
| batch     | RTNCDE00    | 140 | Return code processing (DB2)        |
| online    | CURSMGR     | 90  | DB2 cursor management (CICS)        |
| online    | DB2ONLN     | 119 | DB2 online connection (CICS)        |
| online    | DB2RECV     | 144 | DB2 recovery (CICS)                 |
| online    | ERRHNDL     | 117 | Online error handler (CICS/DB2)     |
| online    | INQHIST     | 192 | Transaction history inquiry (CICS)  |
| online    | INQONLN     | 170 | Online inquiry controller (CICS)    |
| online    | INQPORT     | 109 | Portfolio position inquiry (CICS)   |
| online    | SECMGR      | 134 | Security manager (CICS/DB2)         |
| portfolio | PORTADD     | 147 | Add portfolio record (VSAM)         |
| portfolio | PORTDEL     | 193 | Delete portfolio record (VSAM)      |
| portfolio | PORTMSTR    | 287 | Portfolio master CRUD (VSAM)        |
| portfolio | PORTREAD    | 110 | Read portfolio record (VSAM)        |
| portfolio | PORTTEST    | 118 | Portfolio test harness               |
| portfolio | PORTTRAN    | 316 | Transaction processing (VSAM)       |
| portfolio | PORTUPDT    | 159 | Update portfolio record (VSAM)      |
| portfolio | PORTVALD    | 119 | Portfolio validation                 |
| common    | AUDPROC     | 95  | Audit processing subroutine         |
| common    | DB2CMT      | 169 | DB2 commit handler                  |
| common    | DB2CONN     | 153 | DB2 connection manager              |
| common    | DB2ERR      | 199 | DB2 error handler                   |
| common    | DB2STAT     | 227 | DB2 statistics collector             |
| common    | ERRPROC     | 106 | Error processing subroutine         |
| utility   | UTLMNT00    | 181 | File maintenance utility             |
| utility   | UTLMON00    | 220 | System monitoring utility            |
| utility   | UTLVAL00    | 189 | Data validation utility              |
| test      | TSTGEN00    | 215 | Test data generator                  |
| test      | TSTVAL00    | 224 | Test validation framework            |

---

## Batch Processing Programs

### BCHCTL00 — Batch Control
- **Source**: `src/programs/batch/BCHCTL00.cbl` (126 lines)
- **Copybooks**: `BCHCTL`, `BCHCON`, `ERRHAND`
- **Calls**: `ERRPROC`
- **DB2**: No
- **CICS**: No
- **Files**: `BATCH-CONTROL-FILE` (FD)

### CKPRST — Checkpoint/Restart
- **Source**: `src/programs/batch/CKPRST.cbl` (56 lines)
- **Copybooks**: `CKPRST`, `RETHND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `CHECKPOINT-FILE` (FD)

### HISTLD00 — History Loading
- **Source**: `src/programs/batch/HISTLD00.cbl` (232 lines)
- **Copybooks**: `HISTREC`, `BCHCTL`, `DBTBLS`, `SQLCA`, `DBPROC`, `ERRHAND`, `BCHCON`
- **Calls**: `ERRPROC`
- **DB2**: Yes — `EXEC SQL` (INSERT, cursors, DECLARE SECTION)
- **CICS**: No
- **Files**: `TRANSACTION-HISTORY` (FD), `BATCH-CONTROL-FILE` (FD)
- **JCL**: Referenced implicitly by batch job chain

### POSUPDT — Position Update (STUB)
- **Source**: `src/programs/batch/POSUPDT.cbl` (0 lines — empty file)
- **Note**: Placeholder program; no implementation yet.

### PRCSEQ00 — Process Sequencing
- **Source**: `src/programs/batch/PRCSEQ00.cbl` (344 lines)
- **Copybooks**: `PRCSEQ`, `BCHCTL`, `BCHCON`, `ERRHAND`
- **Calls**: `ERRPROC`
- **DB2**: No
- **CICS**: No
- **Files**: `PROCESS-SEQ-FILE` (FD), `BATCH-CONTROL-FILE` (FD)

### RCVPRC00 — Recovery Processing
- **Source**: `src/programs/batch/RCVPRC00.cbl` (301 lines)
- **Copybooks**: `BCHCTL`, `PRCSEQ`, `BCHCON`, `ERRHAND`
- **Calls**: `ERRPROC` (×2)
- **DB2**: No
- **CICS**: No
- **Files**: `BATCH-CONTROL-FILE` (FD), `PROCESS-SEQ-FILE` (FD)

### RPTAUD00 — Audit Report
- **Source**: `src/programs/batch/RPTAUD00.cbl` (146 lines)
- **Copybooks**: `AUDITLOG`, `ERRHAND`, `RTNCODE`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `AUDIT-FILE` → AUDITLOG, `ERROR-FILE` → ERRLOG, `REPORT-FILE` → RPTFILE
- **JCL**: `src/jcl/batch/RPTAUD.jcl` → `EXEC PGM=RPTAUD00`

### RPTPOS00 — Position Report
- **Source**: `src/programs/batch/RPTPOS00.cbl` (159 lines)
- **Copybooks**: `POSREC`, `TRNREC`, `RTNCODE`, `ERRHAND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `POSITION-MASTER` → POSMSTRE, `TRANSACTION-HISTORY` → TRANHIST, `REPORT-FILE` → RPTFILE
- **JCL**: `src/jcl/batch/RPTPOS.jcl` → `EXEC PGM=RPTPOS00`

### RPTSTA00 — Statistics Report
- **Source**: `src/programs/batch/RPTSTA00.cbl` (184 lines)
- **Copybooks**: `DB2STAT`, `BCHCTL`, `RTNCODE`, `ERRHAND`
- **Calls**: None
- **DB2**: No (reads DB2STAT copybook data, no direct EXEC SQL)
- **CICS**: No
- **Files**: `DB2-STATS` → DB2STATS, `BATCH-STATS` → BCHSTATS, `REPORT-FILE` → RPTFILE
- **JCL**: `src/jcl/batch/RPTSTA.jcl` → `EXEC PGM=RPTSTA00`

### RTNANA00 — Return Analysis
- **Source**: `src/programs/batch/RTNANA00.cbl` (209 lines)
- **Copybooks**: None (uses `EXEC SQL INCLUDE SQLCA`)
- **Calls**: None
- **DB2**: Yes — `EXEC SQL` (DECLARE CURSOR, OPEN, CLOSE, SELECT)
- **CICS**: No
- **Files**: `REPORT-FILE` (FD)
- **JCL**: `src/jcl/RTNANA.jcl` → `EXEC PGM=RTNANA00`

### RTNCDE00 — Return Code Processing
- **Source**: `src/programs/batch/RTNCDE00.cbl` (140 lines)
- **Copybooks**: `RTNCODE`
- **Calls**: None
- **DB2**: Yes — `EXEC SQL` (INCLUDE SQLCA, SQL operations)
- **CICS**: No
- **Files**: None (DB2-only)

---

## Online (CICS) Programs

### INQONLN — Online Inquiry Controller
- **Source**: `src/programs/online/INQONLN.cbl` (170 lines)
- **Copybooks**: `INQCOM`, `ERRHND`
- **CICS Links**: `INQPORT`, `INQHIST`, `ERRHNDL`, `SECMGR` (×3)
- **DB2**: No (delegates to sub-programs)
- **CICS**: Yes — HANDLE CONDITION, RETURN, RECEIVE MAP, SEND MAP, LINK, ASSIGN, ABEND
- **BMS Maps**: `INQMAP` (receive), `INQMNU` (send)
- **Role**: Main CICS transaction entry point; dispatches to sub-programs

### INQPORT — Portfolio Position Inquiry
- **Source**: `src/programs/online/INQPORT.cbl` (109 lines)
- **Copybooks**: `INQCOM`, `POSREC`
- **CICS Links**: None outbound
- **DB2**: Yes — `EXEC SQL INCLUDE SQLPOS`
- **CICS**: Yes — RETURN, HANDLE CONDITION, READ FILE, SEND MAP
- **VSAM File**: `POSFILE` (CICS READ)
- **BMS Maps**: `POSMAP` (send)

### INQHIST — Transaction History Inquiry
- **Source**: `src/programs/online/INQHIST.cbl` (192 lines)
- **Copybooks**: `INQCOM`
- **CICS Links**: `DB2ONLN`, `DB2RECV`, `CURSMGR` (×3)
- **DB2**: Yes — `EXEC SQL INCLUDE SQLCA`
- **CICS**: Yes — RETURN, HANDLE CONDITION, LINK, SEND MAP
- **BMS Maps**: `HISMAP` (send)

### DB2ONLN — DB2 Online Connection
- **Source**: `src/programs/online/DB2ONLN.cbl` (119 lines)
- **Copybooks**: `ERRHND`
- **CICS Links**: None outbound
- **DB2**: Yes — CONNECT, DISCONNECT, SELECT CURRENT SERVER
- **CICS**: Yes — RETURN

### DB2RECV — DB2 Recovery
- **Source**: `src/programs/online/DB2RECV.cbl` (144 lines)
- **Copybooks**: `ERRHND`, `DB2REQ`
- **CICS Links**: `DB2ONLN`, `ERRHNDL`
- **DB2**: Yes — INCLUDE SQLCA, ROLLBACK
- **CICS**: Yes — RETURN, LINK, DELAY

### ERRHNDL — Online Error Handler
- **Source**: `src/programs/online/ERRHNDL.cbl` (117 lines)
- **Copybooks**: `ERRHND`
- **CICS Links**: None outbound
- **DB2**: Yes — INCLUDE SQLCA, DECLARE SECTION, INSERT (error logging)
- **CICS**: Yes — RETURN

### CURSMGR — Cursor Manager
- **Source**: `src/programs/online/CURSMGR.cbl` (90 lines)
- **Copybooks**: None
- **CICS Links**: None outbound
- **DB2**: Yes — INCLUDE SQLCA, DECLARE CURSOR, OPEN
- **CICS**: Yes — RETURN

### SECMGR — Security Manager
- **Source**: `src/programs/online/SECMGR.cbl` (134 lines)
- **Copybooks**: `ERRHND`
- **CICS Links**: None outbound
- **DB2**: Yes — INCLUDE SQLCA, SELECT queries
- **CICS**: Yes — RETURN, ASSIGN

---

## Portfolio Management Programs

### PORTMSTR — Portfolio Master CRUD
- **Source**: `src/programs/portfolio/PORTMSTR.cbl` (287 lines)
- **Copybooks**: None directly (uses FD/inline)
- **Calls**: `ERRPROC`, `AUDPROC`
- **DB2**: No
- **CICS**: No
- **Files**: `PORTFOLIO-FILE` (FD — VSAM)
- **JCL**: Orchestrated via `src/jcl/portfolio/` jobs

### PORTADD — Add Portfolio Record
- **Source**: `src/programs/portfolio/PORTADD.cbl` (147 lines)
- **Copybooks**: `PORTFLIO` (×2)
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `PORTFOLIO-FILE` (FD — VSAM), `INPUT-FILE` (FD)
- **JCL**: `src/jcl/portfolio/PORTADD.jcl` → `EXEC PGM=PORTADD`

### PORTUPDT — Update Portfolio Record
- **Source**: `src/programs/portfolio/PORTUPDT.cbl` (159 lines)
- **Copybooks**: `PORTFLIO`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `PORTFOLIO-FILE` (FD — VSAM), `UPDATE-FILE` (FD)
- **JCL**: `src/jcl/portfolio/PORTUPDT.jcl` → `EXEC PGM=PORTUPDT`

### PORTDEL — Delete Portfolio Record
- **Source**: `src/programs/portfolio/PORTDEL.cbl` (193 lines)
- **Copybooks**: `PORTFLIO`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `PORTFOLIO-FILE` (FD — VSAM), `DELETE-FILE` (FD), `AUDIT-FILE` (FD)
- **JCL**: `src/jcl/portfolio/PORTDEL.jcl` → `EXEC PGM=PORTDEL`

### PORTREAD — Read Portfolio Record
- **Source**: `src/programs/portfolio/PORTREAD.cbl` (110 lines)
- **Copybooks**: `PORTFLIO`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `PORTFOLIO-FILE` (FD — VSAM)
- **JCL**: `src/jcl/portfolio/PORTREAD.jcl` → `EXEC PGM=PORTREAD`

### PORTTRAN — Transaction Processing
- **Source**: `src/programs/portfolio/PORTTRAN.cbl` (316 lines)
- **Copybooks**: `TRNREC`, `PORTREC`, `ERRHAND`, `AUDITLOG`
- **Calls**: `AUDPROC`, `ERRPROC`
- **DB2**: No
- **CICS**: No
- **Files**: `TRANSACTION-FILE` (FD), `PORTFOLIO-FILE` (FD — VSAM)

### PORTVALD — Portfolio Validation
- **Source**: `src/programs/portfolio/PORTVALD.cbl` (119 lines)
- **Copybooks**: `PORTVAL`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: None

### PORTTEST — Portfolio Test Harness
- **Source**: `src/programs/portfolio/PORTTEST.cbl` (118 lines)
- **Copybooks**: `PORTFLIO`, `ERRHAND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `TEST-FILE` (FD)
- **JCL**: `src/jcl/portfolio/PORTTEST.jcl` → `EXEC PGM=PORTTEST`

---

## Common/Shared Programs

### ERRPROC — Error Processing
- **Source**: `src/programs/common/ERRPROC.cbl` (106 lines)
- **Copybooks**: `ERRHAND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Called by**: `PORTMSTR`, `PORTTRAN`, `HISTLD00`, `PRCSEQ00`, `RCVPRC00`, `BCHCTL00`, `DB2CMT`, `DB2ERR`, `DB2STAT`, `DB2CONN`

### AUDPROC — Audit Processing
- **Source**: `src/programs/common/AUDPROC.cbl` (95 lines)
- **Copybooks**: `AUDITLOG`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Called by**: `PORTMSTR`, `PORTTRAN`

### DB2CMT — DB2 Commit Handler
- **Source**: `src/programs/common/DB2CMT.cbl` (169 lines)
- **Copybooks**: `SQLCA`, `DBPROC`, `ERRHAND`
- **Calls**: `ERRPROC`, `DB2ERR`
- **DB2**: Yes — DECLARE SECTION, COMMIT, ROLLBACK, SQL operations
- **CICS**: No

### DB2CONN — DB2 Connection Manager
- **Source**: `src/programs/common/DB2CONN.cbl` (153 lines)
- **Copybooks**: `SQLCA`, `DBPROC`, `ERRHAND`
- **Calls**: `DELAY` (system), `ERRPROC`
- **DB2**: Yes — DECLARE SECTION, CONNECT, DISCONNECT, SQL operations
- **CICS**: No

### DB2ERR — DB2 Error Handler
- **Source**: `src/programs/common/DB2ERR.cbl` (199 lines)
- **Copybooks**: `DBTBLS` (with REPLACING), `SQLCA`, `DBPROC`, `ERRHAND`
- **Calls**: `ERRPROC`
- **DB2**: Yes — DECLARE SECTION, SQL operations
- **CICS**: No
- **Called by**: `DB2CMT`

### DB2STAT — DB2 Statistics Collector
- **Source**: `src/programs/common/DB2STAT.cbl` (227 lines)
- **Copybooks**: `SQLCA`, `DBPROC`, `ERRHAND`
- **Calls**: `ERRPROC`
- **DB2**: Yes — DECLARE SECTION, multiple SQL queries
- **CICS**: No

---

## Utility Programs

### UTLMNT00 — File Maintenance
- **Source**: `src/programs/utility/UTLMNT00.cbl` (181 lines)
- **Copybooks**: `RTNCODE`, `ERRHAND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `CONTROL-FILE` → CTLFILE, `ARCHIVE-FILE` → ARCHFILE, `REPORT-FILE` → RPTFILE
- **JCL**: `src/jcl/utility/UTLMNT.jcl` → `EXEC PGM=UTLMNT00`

### UTLMON00 — System Monitoring
- **Source**: `src/programs/utility/UTLMON00.cbl` (220 lines)
- **Copybooks**: `DB2STAT`, `RTNCODE`, `ERRHAND`
- **Calls**: `ILBOABN0` (system abnormal termination)
- **DB2**: No (reads DB2STAT copybook data)
- **CICS**: No
- **Files**: `MONITOR-CONFIG` → MONCFG, `MONITOR-LOG` → MONLOG, `ALERT-FILE` → ALERTS, `DB2-STATS` → DB2STATS
- **JCL**: `src/jcl/utility/UTLMON.jcl` → `EXEC PGM=UTLMON00`

### UTLVAL00 — Data Validation
- **Source**: `src/programs/utility/UTLVAL00.cbl` (189 lines)
- **Copybooks**: `POSREC`, `TRNREC`, `RTNCODE`, `ERRHAND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: (validates position and transaction records)
- **JCL**: `src/jcl/utility/UTLVAL.jcl` → `EXEC PGM=UTLVAL00`

---

## Test Programs

### TSTGEN00 — Test Data Generator
- **Source**: `src/programs/test/TSTGEN00.cbl` (215 lines)
- **Copybooks**: `PORTFLIO` (with REPLACING), `TRNREC` (with REPLACING), `RTNCODE`, `ERRHAND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `TEST-CONFIG` → TSTCFG, `PORTFOLIO-OUT` → PORTOUT, `TRANSACTION-OUT` → TRANOUT, `RANDOM-SEED` → RANDSED
- **JCL**: `src/jcl/test/TSTGEN.jcl` → `EXEC PGM=TSTGEN00`

### TSTVAL00 — Test Validation Framework
- **Source**: `src/programs/test/TSTVAL00.cbl` (224 lines)
- **Copybooks**: `RTNCODE`, `ERRHAND`
- **Calls**: None
- **DB2**: No
- **CICS**: No
- **Files**: `TEST-CASES` → TESTCASE, `EXPECTED-RESULTS` → EXPECTED, `ACTUAL-RESULTS` → ACTUAL, `TEST-REPORT` → TESTRPT
- **JCL**: `src/jcl/test/TSTVAL.jcl` → `EXEC PGM=TSTVAL00`

---

## Copybook Inventory

### Batch Copybooks (`src/copybook/batch/`)

| Copybook | Used By |
|----------|---------|
| `BCHCON` | HISTLD00, PRCSEQ00, RCVPRC00, BCHCTL00 |
| `BCHCTL` | HISTLD00, PRCSEQ00, RCVPRC00, BCHCTL00, RPTSTA00 |
| `CKPRST` | CKPRST (batch program) |
| `PRCSEQ` | PRCSEQ00, RCVPRC00 |

### Common Copybooks (`src/copybook/common/`)

| Copybook   | Used By |
|-----------|---------|
| `AUDITLOG` | PORTTRAN, RPTAUD00, AUDPROC |
| `COMMON`   | (available for general use) |
| `ERRHAND`  | PORTTEST, PORTTRAN, HISTLD00, PRCSEQ00, RCVPRC00, BCHCTL00, RPTAUD00, RPTPOS00, RPTSTA00, DB2CMT, DB2ERR, DB2STAT, DB2CONN, ERRPROC, UTLMNT00, UTLMON00, UTLVAL00, TSTGEN00, TSTVAL00 |
| `HISTREC`  | HISTLD00 |
| `PORTFLIO` | PORTADD, PORTTEST, PORTUPDT, PORTDEL, PORTREAD, TSTGEN00 |
| `PORTVAL`  | PORTVALD |
| `POSREC`   | RPTPOS00, INQPORT, UTLVAL00 |
| `RETHND`   | CKPRST |
| `RTNCODE`  | TSTVAL00, TSTGEN00, RPTPOS00, RPTSTA00, RPTAUD00, RTNCDE00, UTLMNT00, UTLMON00, UTLVAL00 |
| `TRNREC`   | PORTTRAN, RPTPOS00, TSTGEN00, UTLVAL00 |

### DB2 Copybooks (`src/copybook/db2/`)

| Copybook | Used By |
|----------|---------|
| `DBPROC` | HISTLD00, DB2CMT, DB2ERR, DB2STAT, DB2CONN |
| `DBTBLS` | HISTLD00, DB2ERR |
| `SQLCA`  | HISTLD00, DB2CMT, DB2ERR, DB2STAT, DB2CONN |

### Online Copybooks (`src/copybook/online/`)

| Copybook | Used By |
|----------|---------|
| `DB2REQ` | DB2RECV |
| `ERRHND` | DB2RECV, DB2ONLN, ERRHNDL, INQONLN, SECMGR |
| `INQCOM` | INQPORT, INQONLN, INQHIST |

---

## Cross-Reference Matrix

### Program → Copybook Dependencies

```
Program      | ERRHAND | RTNCODE | SQLCA | DBPROC | DBTBLS | BCHCTL | BCHCON | PORTFLIO | TRNREC | AUDITLOG | INQCOM | ERRHND | Other
-------------|---------|---------|-------|--------|--------|--------|--------|----------|--------|----------|--------|--------|------
BCHCTL00     |    X    |         |       |        |        |   X    |   X    |          |        |          |        |        |
CKPRST       |         |         |       |        |        |        |        |          |        |          |        |        | CKPRST, RETHND
HISTLD00     |    X    |         |   X   |   X    |   X    |   X    |   X    |          |        |          |        |        | HISTREC
PRCSEQ00     |    X    |         |       |        |        |   X    |   X    |          |        |          |        |        | PRCSEQ
RCVPRC00     |    X    |         |       |        |        |   X    |   X    |          |        |          |        |        | PRCSEQ
RPTAUD00     |    X    |    X    |       |        |        |        |        |          |        |    X     |        |        |
RPTPOS00     |    X    |    X    |       |        |        |        |        |          |   X    |          |        |        | POSREC
RPTSTA00     |    X    |    X    |       |        |        |   X    |        |          |        |          |        |        | DB2STAT
RTNANA00     |         |         |       |        |        |        |        |          |        |          |        |        | (SQL INCLUDE SQLCA)
RTNCDE00     |         |    X    |       |        |        |        |        |          |        |          |        |        | (SQL INCLUDE SQLCA)
INQONLN      |         |         |       |        |        |        |        |          |        |          |   X    |   X    |
INQPORT      |         |         |       |        |        |        |        |          |        |          |   X    |        | POSREC
INQHIST      |         |         |       |        |        |        |        |          |        |          |   X    |        |
DB2ONLN      |         |         |       |        |        |        |        |          |        |          |        |   X    |
DB2RECV      |         |         |       |        |        |        |        |          |        |          |        |   X    | DB2REQ
ERRHNDL      |         |         |       |        |        |        |        |          |        |          |        |   X    |
CURSMGR      |         |         |       |        |        |        |        |          |        |          |        |        |
SECMGR       |         |         |       |        |        |        |        |          |        |          |        |   X    |
PORTADD      |         |         |       |        |        |        |        |    X     |        |          |        |        |
PORTUPDT     |         |         |       |        |        |        |        |    X     |        |          |        |        |
PORTDEL      |         |         |       |        |        |        |        |    X     |        |          |        |        |
PORTREAD     |         |         |       |        |        |        |        |    X     |        |          |        |        |
PORTMSTR     |         |         |       |        |        |        |        |          |        |          |        |        |
PORTTRAN     |    X    |         |       |        |        |        |        |          |   X    |    X     |        |        | PORTREC
PORTVALD     |         |         |       |        |        |        |        |          |        |          |        |        | PORTVAL
PORTTEST     |    X    |         |       |        |        |        |        |    X     |        |          |        |        |
AUDPROC      |         |         |       |        |        |        |        |          |        |    X     |        |        |
DB2CMT       |    X    |         |   X   |   X    |        |        |        |          |        |          |        |        |
DB2CONN      |    X    |         |   X   |   X    |        |        |        |          |        |          |        |        |
DB2ERR       |    X    |         |   X   |   X    |   X    |        |        |          |        |          |        |        |
DB2STAT      |    X    |         |   X   |   X    |        |        |        |          |        |          |        |        |
ERRPROC      |    X    |         |       |        |        |        |        |          |        |          |        |        |
UTLMNT00     |    X    |    X    |       |        |        |        |        |          |        |          |        |        |
UTLMON00     |    X    |    X    |       |        |        |        |        |          |        |          |        |        | DB2STAT
UTLVAL00     |    X    |    X    |       |        |        |        |        |          |   X    |          |        |        | POSREC
TSTGEN00     |    X    |    X    |       |        |        |        |        |    X     |   X    |          |        |        |
TSTVAL00     |    X    |    X    |       |        |        |        |        |          |        |          |        |        |
```

---

## Call Graph Summary

### CALL (Batch/Common) — Static Program Calls

```
PORTMSTR ──→ ERRPROC
         ──→ AUDPROC

PORTTRAN ──→ AUDPROC
         ──→ ERRPROC

HISTLD00 ──→ ERRPROC
PRCSEQ00 ──→ ERRPROC
RCVPRC00 ──→ ERRPROC (×2)
BCHCTL00 ──→ ERRPROC

DB2CMT   ──→ ERRPROC
         ──→ DB2ERR

DB2ERR   ──→ ERRPROC
DB2STAT  ──→ ERRPROC
DB2CONN  ──→ DELAY (system)
         ──→ ERRPROC

UTLMON00 ──→ ILBOABN0 (system — abnormal termination)
```

### EXEC CICS LINK — Online Program Links

```
INQONLN ──→ INQPORT
        ──→ INQHIST
        ──→ ERRHNDL
        ──→ SECMGR (×3)

INQHIST ──→ DB2ONLN
        ──→ DB2RECV
        ──→ CURSMGR (×3)

DB2RECV ──→ DB2ONLN
        ──→ ERRHNDL
```

### Full Online Call Tree (from INQONLN entry point)

```
INQONLN (Main CICS Transaction)
├── SECMGR          (authentication/authorization via DB2)
├── INQPORT          (portfolio position inquiry via VSAM + DB2)
├── INQHIST          (transaction history inquiry)
│   ├── DB2ONLN      (DB2 connection management)
│   ├── DB2RECV      (DB2 recovery)
│   │   ├── DB2ONLN  (reconnection)
│   │   └── ERRHNDL  (error logging)
│   └── CURSMGR      (DB2 cursor management)
└── ERRHNDL          (error handling/logging to DB2)
```

### Database Resources

| Resource        | Type | Used By Programs                     |
|----------------|------|--------------------------------------|
| SQLCA          | DB2  | HISTLD00, RTNANA00, RTNCDE00, DB2CMT, DB2CONN, DB2ERR, DB2STAT, CURSMGR, DB2ONLN, DB2RECV, ERRHNDL, SECMGR, INQHIST |
| POSMVP (DB2)   | DB2  | DB2ONLN (CONNECT TO)                 |
| POSFILE (VSAM)  | CICS | INQPORT (CICS READ)                 |
| PORTFOLIO-FILE | VSAM | PORTADD, PORTUPDT, PORTDEL, PORTREAD, PORTMSTR, PORTTRAN |

### BMS Maps

| Map Set | Map    | Used By  | Direction |
|---------|--------|----------|-----------|
| INQSET  | INQMAP | INQONLN  | RECEIVE   |
| INQSET  | INQMNU | INQONLN  | SEND      |
| INQSET  | POSMAP | INQPORT  | SEND      |
| INQSET  | HISMAP | INQHIST  | SEND      |

### CICS Resource Definitions

- **CSD File**: `src/cics/PORTDFN.csd` — defines CICS transaction/program mappings

---

## Component Groups for Modernization

The system naturally decomposes into **6 independent modernization components**:

| # | Component | Programs | Key Dependencies | Complexity |
|---|-----------|----------|------------------|------------|
| 1 | **Batch Processing** | BCHCTL00, CKPRST, HISTLD00, POSUPDT, PRCSEQ00, RCVPRC00 | DB2 (HISTLD00), ERRPROC, batch copybooks | High |
| 2 | **Online/CICS Inquiry** | INQONLN, INQPORT, INQHIST, DB2ONLN, DB2RECV, ERRHNDL, CURSMGR, SECMGR | CICS, DB2, BMS Maps, VSAM | Very High |
| 3 | **Portfolio Management** | PORTADD, PORTUPDT, PORTDEL, PORTREAD, PORTMSTR, PORTTRAN, PORTVALD, PORTTEST | VSAM, ERRPROC, AUDPROC | Medium-High |
| 4 | **Reporting** | RPTAUD00, RPTPOS00, RPTSTA00, RTNANA00, RTNCDE00 | DB2 (RTNANA00, RTNCDE00), sequential files | Medium |
| 5 | **Utility & Monitoring** | UTLMNT00, UTLMON00, UTLVAL00 | Sequential files, system calls | Low-Medium |
| 6 | **Testing Framework** | TSTGEN00, TSTVAL00 | Sequential files, copybooks | Low |

**Shared Layer** (required by all components): `ERRPROC`, `AUDPROC`, `DB2CMT`, `DB2CONN`, `DB2ERR`, `DB2STAT`
