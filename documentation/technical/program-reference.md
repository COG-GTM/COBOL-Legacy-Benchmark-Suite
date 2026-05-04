# COBOL Legacy Benchmark Suite — Program Reference Guide

> Comprehensive documentation for all 38 COBOL programs, 20 copybooks, and 15 JCL jobs in the Investment Portfolio Management System.

---

## Table of Contents

- [Batch Processing Programs](#batch-processing-programs)
  - [BCHCTL00 — Batch Control Processor](#bchctl00--batch-control-processor)
  - [CKPRST — Checkpoint/Restart Framework](#ckprst--checkpointrestart-framework)
  - [HISTLD00 — Position History DB2 Load](#histld00--position-history-db2-load)
  - [POSUPDT — Position Update](#posupdt--position-update)
  - [PRCSEQ00 — Process Sequence Manager](#prcseq00--process-sequence-manager)
  - [RCVPRC00 — Recovery Process Handler](#rcvprc00--recovery-process-handler)
  - [RPTAUD00 — Audit Report Generator](#rptaud00--audit-report-generator)
  - [RPTPOS00 — Position Report Generator](#rptpos00--position-report-generator)
  - [RPTSTA00 — Statistics Report Generator](#rptsta00--statistics-report-generator)
  - [RTNANA00 — Return Code Analysis](#rtnana00--return-code-analysis)
  - [RTNCDE00 — Return Code Processing](#rtncde00--return-code-processing)
- [Common/Shared Programs](#commonshared-programs)
  - [ERRPROC — Error Processing](#errproc--error-processing)
  - [AUDPROC — Audit Trail Processing](#audproc--audit-trail-processing)
  - [DB2CONN — DB2 Connection Manager](#db2conn--db2-connection-manager)
  - [DB2CMT — DB2 Commit Controller](#db2cmt--db2-commit-controller)
  - [DB2ERR — DB2 Error Handler](#db2err--db2-error-handler)
  - [DB2STAT — DB2 Statistics Collector](#db2stat--db2-statistics-collector)
- [Online/CICS Programs](#onlinecics-programs)
  - [INQONLN — Online Inquiry Main Handler](#inqonln--online-inquiry-main-handler)
  - [INQPORT — Portfolio Position Inquiry](#inqport--portfolio-position-inquiry)
  - [INQHIST — Transaction History Inquiry](#inqhist--transaction-history-inquiry)
  - [SECMGR — Security Manager](#secmgr--security-manager)
  - [DB2ONLN — DB2 Online Connection](#db2onln--db2-online-connection)
  - [DB2RECV — DB2 Online Recovery](#db2recv--db2-online-recovery)
  - [CURSMGR — Cursor Manager](#cursmgr--cursor-manager)
  - [ERRHNDL — CICS Error Handler](#errhndl--cics-error-handler)
- [Portfolio Management Programs](#portfolio-management-programs)
  - [PORTMSTR — Portfolio Master Maintenance](#portmstr--portfolio-master-maintenance)
  - [PORTADD — Portfolio Addition](#portadd--portfolio-addition)
  - [PORTREAD — Portfolio Record Read](#portread--portfolio-record-read)
  - [PORTUPDT — Portfolio Update](#portupdt--portfolio-update)
  - [PORTDEL — Portfolio Deletion](#portdel--portfolio-deletion)
  - [PORTTRAN — Portfolio Transaction Processing](#porttran--portfolio-transaction-processing)
  - [PORTVALD — Portfolio Validation](#portvald--portfolio-validation)
  - [PORTTEST — Portfolio Test Data Generator](#porttest--portfolio-test-data-generator)
- [Utility Programs](#utility-programs)
  - [UTLMNT00 — File Maintenance](#utlmnt00--file-maintenance)
  - [UTLMON00 — System Monitoring](#utlmon00--system-monitoring)
  - [UTLVAL00 — Data Validation](#utlval00--data-validation)
- [Test Programs](#test-programs)
  - [TSTGEN00 — Test Data Generation](#tstgen00--test-data-generation)
  - [TSTVAL00 — Test Validation](#tstval00--test-validation)
- [Copybook Reference](#copybook-reference)

---

## Batch Processing Programs

### BCHCTL00 — Batch Control Processor

**Location:** `src/programs/batch/BCHCTL00.cbl`  
**Type:** Batch  
**Purpose:** Central batch control program that manages job initialization, prerequisite checking, status updates, and termination for all batch processes.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| BCHCTL | `src/copybook/batch/` | Batch control record layout — job parameters, status flags |
| BCHCON | `src/copybook/batch/` | Batch constants — return codes, limits, thresholds |
| ERRHAND | `src/copybook/common/` | Error handling data structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL | Standard error processing and logging |

#### Key Sections and Logic

- **0000-MAIN**: Entry point — dispatches to functions via `EVALUATE` on `LS-FUNCTION` (88-level conditions: FUNC-INIT, FUNC-CHEK, FUNC-UPDT, FUNC-TERM)
- **1000-PROCESS-INITIALIZE**: Opens indexed BATCH-CONTROL-FILE, reads control record, validates process, updates start status
- **2000-CHECK-PREREQUISITES**: Reads control record, checks job dependencies; returns `BCT-RC-SUCCESS` or `BCT-RC-WARNING`
- **3000-UPDATE-STATUS**: Reads/updates/rewrites batch control record with new status
- **4000-PROCESS-TERMINATE**: Updates completion status, closes files
- **9000-ERROR-ROUTINE**: Sets program name to 'BCHCTL00', calls ERRPROC

#### File I/O

| File | DD Name | Organization | Access | Key | Purpose |
|------|---------|-------------|--------|-----|---------|
| BATCH-CONTROL-FILE | BCHCTL | Indexed (VSAM KSDS) | Dynamic | BCT-KEY | Job status and control records |

#### Key Working-Storage Variables

- `WS-PREREQ-MET` (88: PREREQS-SATISFIED / PREREQS-PENDING) — prerequisite check result
- `WS-PROCESS-MODE` (88: MODE-INITIALIZE / MODE-CHECK-PREREQ / MODE-UPDATE-STATUS / MODE-FINALIZE) — current processing mode

#### Linkage Section Interface

```
LS-CONTROL-REQUEST:
  LS-FUNCTION    PIC X(4)    — INIT/CHEK/UPDT/TERM
  LS-JOB-NAME    PIC X(8)    — Calling job name
  LS-PROCESS-DATE PIC X(8)   — Processing date
  LS-SEQUENCE-NO  PIC 9(4)   — Step sequence number
  LS-RETURN-CODE  PIC S9(4)  — Return code (output)
```

---

### CKPRST — Checkpoint/Restart Framework

**Location:** `src/programs/batch/CKPRST.cbl`  
**Type:** Batch  
**Purpose:** Implements a checkpoint/restart framework enabling long-running batch jobs to resume processing from the last committed checkpoint after a failure.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| CKPRST | `src/copybook/batch/` | Checkpoint data structures — commit points, position tracking |
| RETHND | `src/copybook/common/` | Return/recovery handling data structures |

#### Program Dependencies

None — standalone framework program.

#### Key Sections and Logic

- Records checkpoint positions at configurable intervals
- On restart, reads last committed checkpoint and repositions files to resume processing
- Uses the CKPRST copybook for checkpoint record layout and RETHND for recovery coordination

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| CHECKPOINT-FILE | *(system)* | Indexed | Stores checkpoint position data |

---

### HISTLD00 — Position History DB2 Load

**Location:** `src/programs/batch/HISTLD00.cbl`  
**Type:** Batch  
**Purpose:** Reads transaction history records from a VSAM indexed file and loads them into the DB2 `POSHIST` table. Implements commit-interval processing with checkpoint support.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| HISTREC | `src/copybook/common/` | Transaction history record layout (FD) |
| BCHCTL | `src/copybook/batch/` | Batch control record layout (FD) |
| DBTBLS | `src/copybook/db2/` | DB2 host variable declarations |
| SQLCA | `src/copybook/db2/` | SQL Communication Area |
| DBPROC | `src/copybook/db2/` | DB2 processing parameters |
| ERRHAND | `src/copybook/common/` | Error handling structures |
| BCHCON | `src/copybook/batch/` | Batch constants |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL | Error processing and logging |

#### Key Sections and Logic

- **0000-MAIN**: Initialize → Process loop (until EOF or >100 errors) → Terminate
- **1000-INITIALIZE**: Opens files (1100), connects to DB2 (1200), initializes checkpoints (1300)
- **2000-PROCESS**: Read history record (2100) → Load to DB2 via INSERT (2200) → Check commit threshold (2300)
- **3000-TERMINATE**: Final commit (3100) → Close files (3200) → Disconnect DB2 (3300) → Display stats (3400)
- **2200-LOAD-TO-DB2**: Maps 13 fields from TH-* (history) to PH-* (DB2) host variables, then executes `EXEC SQL INSERT INTO POSHIST`
- **2300-CHECK-COMMIT**: Commits every 1000 records (`WS-COMMIT-THRESHOLD`)

#### File I/O

| File | DD Name | Organization | Access | Purpose |
|------|---------|-------------|--------|---------|
| TRANSACTION-HISTORY | TRANHIST | Indexed | Sequential | Source transaction records |
| BATCH-CONTROL-FILE | BCHCTL | Indexed | Dynamic | Job status tracking |

#### DB2 Usage

- **INSERT INTO POSHIST** — Loads history records with 13 columns (account, portfolio, dates, amounts, cost basis, gain/loss)
- **COMMIT** — Interval-based commits every 1000 records
- Uses SQLCA for return code checking

#### Key Counters

- `WS-RECORDS-READ` — Total records read from history file
- `WS-RECORDS-WRITTEN` — Successfully loaded to DB2
- `WS-ERROR-COUNT` — Error threshold (>100 stops processing)
- `WS-COMMIT-COUNT` — Tracks records since last commit

---

### POSUPDT — Position Update

**Location:** `src/programs/batch/POSUPDT.cbl`  
**Type:** Batch  
**Purpose:** Updates position master records based on processed transactions. Minimal implementation — serves as a template for position recalculation logic.

#### Dependencies

No copybook dependencies or program calls. Standalone template program.

---

### PRCSEQ00 — Process Sequence Manager

**Location:** `src/programs/batch/PRCSEQ00.cbl`  
**Type:** Batch  
**Purpose:** Manages the sequencing of batch process steps, ensuring correct execution order and dependency tracking across multi-step batch runs.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PRCSEQ | `src/copybook/batch/` | Process sequence record layout — step ordering, status |
| BCHCTL | `src/copybook/batch/` | Batch control record layout |
| BCHCON | `src/copybook/batch/` | Batch constants |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL | Error processing and logging |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| PROCESS-SEQ-FILE | *(system)* | Indexed | Step sequence tracking |
| BATCH-CONTROL-FILE | BCHCTL | Indexed | Job control |

---

### RCVPRC00 — Recovery Process Handler

**Location:** `src/programs/batch/RCVPRC00.cbl`  
**Type:** Batch  
**Purpose:** Handles batch process recovery after failures. Reads batch control and process sequence records to determine recovery points and re-initiate processing.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| BCHCTL | `src/copybook/batch/` | Batch control record layout |
| PRCSEQ | `src/copybook/batch/` | Process sequence records |
| BCHCON | `src/copybook/batch/` | Batch constants |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL (×2) | Error processing — called in two separate error paths |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| BATCH-CONTROL-FILE | BCHCTL | Indexed | Recovery state tracking |
| PROCESS-SEQ-FILE | *(system)* | Indexed | Step recovery positions |

---

### RPTAUD00 — Audit Report Generator

**Location:** `src/programs/batch/RPTAUD00.cbl`  
**Type:** Batch  
**Purpose:** Reads audit log and error log files to generate a formatted audit trail report to a sequential output file.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| AUDITLOG | `src/copybook/common/` | Audit log record layout |
| ERRHAND | `src/copybook/common/` | Error handling structures |
| RTNCODE | `src/copybook/common/` | Return code definitions |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| AUDIT-FILE | AUDITLOG | Sequential | Input — audit trail records |
| ERROR-FILE | ERRLOG | Sequential | Input — error log records |
| REPORT-FILE | RPTFILE | Sequential | Output — formatted audit report |

---

### RPTPOS00 — Position Report Generator

**Location:** `src/programs/batch/RPTPOS00.cbl`  
**Type:** Batch  
**Purpose:** Generates a position report by reading the VSAM position master file and transaction history, producing a formatted report showing holdings, values, and transaction activity.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| POSREC | `src/copybook/common/` | Position master record layout |
| TRNREC | `src/copybook/common/` | Transaction record layout |
| RTNCODE | `src/copybook/common/` | Return code definitions |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| POSITION-MASTER | POSMSTRE | VSAM | Input — current positions |
| TRANSACTION-HISTORY | TRANHIST | Sequential/Indexed | Input — transaction records |
| REPORT-FILE | RPTFILE | Sequential | Output — position report |

---

### RPTSTA00 — Statistics Report Generator

**Location:** `src/programs/batch/RPTSTA00.cbl`  
**Type:** Batch  
**Purpose:** Generates a system statistics report combining DB2 performance metrics and batch processing statistics into a formatted report.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| DB2STAT | *(copybook reference)* | DB2 statistics data structures |
| BCHCTL | `src/copybook/batch/` | Batch control record layout |
| RTNCODE | `src/copybook/common/` | Return code definitions |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| DB2-STATS | DB2STATS | Sequential | Input — DB2 performance metrics |
| BATCH-STATS | BCHSTATS | Sequential | Input — batch processing statistics |
| REPORT-FILE | RPTFILE | Sequential | Output — statistics report |

---

### RTNANA00 — Return Code Analysis

**Location:** `src/programs/batch/RTNANA00.cbl`  
**Type:** Batch  
**Purpose:** Analyzes return codes from batch processing by querying DB2 tables via a cursor and generating an analysis report.

#### DB2 Usage

- **EXEC SQL INCLUDE SQLCA** — SQL Communication Area
- **DECLARE PRGCUR CURSOR** — Cursor for return code analysis query
- **OPEN / FETCH / CLOSE** cursor operations

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| REPORT-FILE | *(system)* | Sequential | Output — return code analysis |

---

### RTNCDE00 — Return Code Processing

**Location:** `src/programs/batch/RTNCDE00.cbl`  
**Type:** Batch  
**Purpose:** Processes and standardizes return codes from batch operations, storing results in DB2 for tracking and analysis.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| RTNCODE | `src/copybook/common/` | Return code definitions and condition names |

#### DB2 Usage

- **EXEC SQL INCLUDE SQLCA** — SQL Communication Area
- SQL operations for storing/retrieving return code data

---

## Common/Shared Programs

### ERRPROC — Error Processing

**Location:** `src/programs/common/ERRPROC.cbl`  
**Type:** Common (most heavily depended-upon program — called by 10 programs)  
**Purpose:** Central error processing subroutine. Receives error details via LINKAGE SECTION, timestamps them, writes to a sequential error log file, and displays error details to the console.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| ERRHAND | `src/copybook/common/` | Error handling data structures — ERR-MESSAGE, ERR-PROGRAM, etc. |

#### Called By

BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00, PORTMSTR, PORTTRAN, DB2CMT, DB2CONN, DB2ERR, DB2STAT

#### Key Sections and Logic

- **0000-MAIN**: Initialize → Process Error → Terminate → GOBACK
- **1000-INITIALIZE**: Gets timestamp (`ACCEPT FROM TIME STAMP`), opens error log in EXTEND mode
- **2000-PROCESS-ERROR**: Maps linkage fields (program ID, category, code, severity, text, details) to ERR-MESSAGE fields, writes log, displays to console
- **2100-WRITE-LOG**: Writes 400-byte formatted record to sequential error log
- **2200-DISPLAY-ERROR**: Outputs formatted error banner with all fields to SYSOUT
- **3000-TERMINATE**: Closes error log file

#### File I/O

| File | DD Name | Organization | Recording Mode | Purpose |
|------|---------|-------------|----------------|---------|
| ERROR-LOG | ERRLOG | Sequential | Fixed (F) | Append-only error log |

#### Linkage Section Interface

```
LS-ERROR-REQUEST:
  LS-PROGRAM-ID    PIC X(8)    — Calling program name
  LS-CATEGORY      PIC X(2)    — Error category code
  LS-ERROR-CODE    PIC X(4)    — Specific error code
  LS-SEVERITY      PIC S9(4)   — Severity level (returned as LS-RETURN-CODE)
  LS-ERROR-TEXT    PIC X(80)    — Error message text
  LS-ERROR-DETAILS PIC X(256)  — Extended error details
  LS-RETURN-CODE   PIC S9(4)   — Return code (output = severity)
```

---

### AUDPROC — Audit Trail Processing

**Location:** `src/programs/common/AUDPROC.cbl`  
**Type:** Common  
**Purpose:** Audit trail processing subroutine. Writes audit records to a sequential audit file using the AUDITLOG copybook layout.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| AUDITLOG | `src/copybook/common/` | Audit log record layout — timestamps, user IDs, actions |

#### Called By

PORTMSTR, PORTTRAN

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| AUDIT-FILE | *(system)* | Sequential | Audit trail records |

---

### DB2CONN — DB2 Connection Manager

**Location:** `src/programs/common/DB2CONN.cbl`  
**Type:** Common  
**Purpose:** Manages DB2 database connections with retry logic. Handles connection establishment, validation, and reconnection after failures.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| SQLCA | `src/copybook/db2/` | SQL Communication Area |
| DBPROC | `src/copybook/db2/` | DB2 processing parameters |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| DELAY | CALL | Wait between connection retry attempts |
| ERRPROC | CALL | Error logging on connection failures |

#### DB2 Usage

- **CONNECT** — Establishes DB2 connection
- **Retry logic** — Configurable retry count with delay between attempts

---

### DB2CMT — DB2 Commit Controller

**Location:** `src/programs/common/DB2CMT.cbl`  
**Type:** Common  
**Purpose:** Controls DB2 commit processing. Handles COMMIT and ROLLBACK operations with error detection and recovery coordination.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| SQLCA | `src/copybook/db2/` | SQL Communication Area |
| DBPROC | `src/copybook/db2/` | DB2 processing parameters |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL | Error logging |
| DB2ERR | CALL | Detailed SQL error analysis |

#### DB2 Usage

- **COMMIT** — Commits current transaction
- **ROLLBACK** — Rolls back on errors
- Checks SQLCA return codes for commit/rollback success

---

### DB2ERR — DB2 Error Handler

**Location:** `src/programs/common/DB2ERR.cbl`  
**Type:** Common  
**Purpose:** Specialized DB2 SQL error handler. Analyzes SQLCODE values, formats diagnostic information from SQLCA, and logs detailed SQL error reports.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| DBTBLS | `src/copybook/db2/` | DB2 table definitions (with REPLACING) |
| SQLCA | `src/copybook/db2/` | SQL Communication Area — SQLCODE, SQLERRM |
| DBPROC | `src/copybook/db2/` | DB2 processing parameters |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL | Logs formatted SQL error details |

#### DB2 Usage

- Reads and interprets SQLCA diagnostic fields
- Formats SQL error messages with SQLCODE, SQLERRM, SQLERRD values

---

### DB2STAT — DB2 Statistics Collector

**Location:** `src/programs/common/DB2STAT.cbl`  
**Type:** Common  
**Purpose:** Collects and records DB2 performance statistics including connection metrics, SQL execution counts, and timing data.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| SQLCA | `src/copybook/db2/` | SQL Communication Area |
| DBPROC | `src/copybook/db2/` | DB2 processing parameters |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL | Error logging for statistics collection failures |

#### DB2 Usage

- Queries DB2 catalog/performance tables
- Collects execution statistics (SQL counts, timings, buffer pool metrics)

---

## Online/CICS Programs

### INQONLN — Online Inquiry Main Handler

**Location:** `src/programs/online/INQONLN.cbl`  
**Type:** Online (CICS)  
**Purpose:** Main CICS transaction handler for portfolio inquiries. Receives BMS map input, dispatches to sub-programs (INQPORT for positions, INQHIST for history), manages security validation, and handles terminal session lifecycle.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| INQCOM | `src/copybook/online/` | Inquiry COMMAREA — shared data area for CICS LINK calls |
| ERRHND | `src/copybook/online/` | Online error handling structures (CICS-specific) |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| INQPORT | CICS LINK | Portfolio position inquiry |
| INQHIST | CICS LINK | Transaction history inquiry |
| ERRHNDL | CICS LINK | CICS error handling |
| SECMGR | CICS LINK | Security validation (×3 calls) |

#### CICS Resources

| Resource Type | Name | Purpose |
|--------------|------|---------|
| MAP | INQMAP / INQSET | Main inquiry input map |
| MAP | INQMNU / INQSET | Menu display map |
| TRANSACTION | *(CSD-defined)* | Inquiry transaction |

#### Key Sections and Logic

- **P100-PROCESS-REQUEST**: Main loop — receives INQMAP, evaluates `WS-COMMAREA-FUNCTION`:
  - `'MENU'` → P200-DISPLAY-MENU (sends INQMNU map)
  - `'INQP'` → P300-PORTFOLIO-INQUIRY (CICS LINK to INQPORT)
  - `'INQH'` → P400-HISTORY-INQUIRY (CICS LINK to INQHIST)
  - `'EXIT'` → Sets SESSION-TERMINATED
- **P050-SECURITY-CHECK**: Gets USERID via `EXEC CICS ASSIGN`, calls SECMGR for authorization
- **P900-ERROR-ROUTINE**: Sets ERR-PROGRAM='INQONLN', captures EIBRESP/EIBRESP2, calls ERRHNDL; abends with code 'IERR' if severe

#### Key Working-Storage Variables

- `WS-END-OF-SESSION` (88: SESSION-ACTIVE / SESSION-TERMINATED) — session lifecycle
- `WS-RESPONSE-CODE` — CICS RESP value
- `WS-SECURITY-REQUEST` — SECMGR interface (request type, user ID, resource, access type, response)

---

### INQPORT — Portfolio Position Inquiry

**Location:** `src/programs/online/INQPORT.cbl`  
**Type:** Online (CICS)  
**Purpose:** Retrieves and displays portfolio position records from VSAM file. Called by INQONLN via CICS LINK, receives COMMAREA with inquiry criteria.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| INQCOM | `src/copybook/online/` | Inquiry COMMAREA |
| POSREC | `src/copybook/common/` | Position master record layout |

#### CICS Resources

| Resource Type | Name | Purpose |
|--------------|------|---------|
| FILE | POSFILE | VSAM position master file |
| MAP | POSMAP | Position display map |

#### Key Sections and Logic

- Reads VSAM POSFILE using `EXEC CICS READ FILE('POSFILE')`
- Handles NOTFND condition
- Sends results via `EXEC CICS SEND MAP('POSMAP')`

---

### INQHIST — Transaction History Inquiry

**Location:** `src/programs/online/INQHIST.cbl`  
**Type:** Online (CICS)  
**Purpose:** Queries transaction history via DB2. Called by INQONLN, links to DB2ONLN for connection, DB2RECV for recovery, and CURSMGR for cursor operations.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| INQCOM | `src/copybook/online/` | Inquiry COMMAREA |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| DB2ONLN | CICS LINK | DB2 online connection |
| DB2RECV | CICS LINK | DB2 recovery |
| CURSMGR | CICS LINK (×4) | Cursor open/fetch/close operations |

#### CICS Resources

| Resource Type | Name | Purpose |
|--------------|------|---------|
| MAP | HISMAP | History display map |

#### DB2 Usage

- **EXEC SQL INCLUDE SQLCA** — SQL Communication Area
- Uses CURSMGR for cursor-based result set retrieval

---

### SECMGR — Security Manager

**Location:** `src/programs/online/SECMGR.cbl`  
**Type:** Online (CICS)  
**Purpose:** Manages DB2 authorization and security validation for online inquiry transactions. Validates user access rights and resource permissions.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| ERRHND | `src/copybook/online/` | Online error handling structures |

#### CICS Resources

- `EXEC CICS ASSIGN` — Retrieves user and terminal identity information

#### DB2 Usage

- Queries security/authorization tables to validate user permissions
- Uses SQLCA for return code checking

---

### DB2ONLN — DB2 Online Connection

**Location:** `src/programs/online/DB2ONLN.cbl`  
**Type:** Online (CICS)  
**Purpose:** Manages DB2 connections specifically for online CICS transactions. Handles CONNECT, DISCONNECT, and connection validation.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| ERRHND | `src/copybook/online/` | Online error handling structures |

#### DB2 Usage

- **CONNECT TO POSMVP** — Connects to the POSMVP DB2 subsystem
- **DISCONNECT** — Releases DB2 connection
- **SELECT CURRENT SERVER** — Validates active connection

---

### DB2RECV — DB2 Online Recovery

**Location:** `src/programs/online/DB2RECV.cbl`  
**Type:** Online (CICS)  
**Purpose:** Handles DB2 recovery operations in the online environment. Performs ROLLBACK, re-establishes connections via DB2ONLN, and coordinates with ERRHNDL for error reporting.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| ERRHND | `src/copybook/online/` | Online error handling structures |
| DB2REQ | `src/copybook/online/` | DB2 request/response area |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| DB2ONLN | CICS LINK | Re-establish DB2 connection after recovery |
| ERRHNDL | CICS LINK | Error reporting |

#### DB2 Usage

- **ROLLBACK** — Rolls back current transaction on error
- **EXEC CICS DELAY** — Retry delay between recovery attempts

---

### CURSMGR — Cursor Manager

**Location:** `src/programs/online/CURSMGR.cbl`  
**Type:** Online (CICS)  
**Purpose:** Manages DB2 cursor operations (DECLARE, OPEN, FETCH, CLOSE) for the online inquiry system. Provides a reusable cursor management service.

#### DB2 Usage

- **DECLARE :CURS-NAME CURSOR** — Dynamic cursor declaration
- **OPEN :CURS-NAME** — Opens declared cursor
- Uses SQLCA for cursor operation status

---

### ERRHNDL — CICS Error Handler

**Location:** `src/programs/online/ERRHNDL.cbl`  
**Type:** Online (CICS)  
**Purpose:** CICS-specific error handler. Receives error information via COMMAREA, logs errors to DB2, and determines whether the error requires an abend or can be handled gracefully.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| ERRHND | `src/copybook/online/` (×2) | Online error handling structures |

#### DB2 Usage

- **INSERT** — Logs error records to DB2 error table
- Uses SQLCA for operation status

---

## Portfolio Management Programs

### PORTMSTR — Portfolio Master Maintenance

**Location:** `src/programs/portfolio/PORTMSTR.cbl`  
**Type:** Portfolio  
**Purpose:** Central CRUD operations program for portfolio master records. Handles Create, Read, Update, and Delete operations on the VSAM portfolio file with validation, error handling, and audit logging.

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ERRPROC | CALL | VSAM error logging with categorized error handling |
| AUDPROC | CALL | Audit trail logging for portfolio modifications |

#### Key Sections and Logic

- **0000-MAIN**: Initialize → EVALUATE on LS-COMMAND (88-levels: CREATE-PORT, READ-PORT, UPDATE-PORT, DELETE-PORT) → Terminate
- **1000-INITIALIZE**: Opens PORTFOLIO-FILE in I-O mode, accepts current date
- **2000-CREATE-PORTFOLIO**: Validates → WRITE; checks for DUP-KEY (status '22')
- **2100-VALIDATE-PORTFOLIO**: Checks PORT-ID format (must start with 'PORT' + 5 numeric), name not blank, status in ('A','I','C')
- **3000-READ-PORTFOLIO**: READ by key; handles NOT-FOUND (status '23')
- **4000-UPDATE-PORTFOLIO**: Validates → REWRITE; calls 2100-LOG-PORTFOLIO-UPDATE for audit
- **5000-DELETE-PORTFOLIO**: DELETE by key; handles NOT-FOUND
- **2100-HANDLE-VSAM-ERROR**: Categorized VSAM error handling — maps file status to severity/message, calls ERRPROC
- **2100-LOG-PORTFOLIO-UPDATE**: Initializes audit request, populates system ID, user, program, terminal; calls AUDPROC

#### File I/O

| File | DD Name | Organization | Access | Record Size | Key |
|------|---------|-------------|--------|-------------|-----|
| PORTFOLIO-FILE | PORTFILE | Indexed (VSAM KSDS) | Dynamic | 100 bytes | PORT-ID |

#### Record Layout

```
PORTFOLIO-RECORD (100 bytes):
  PORT-ID          PIC X(10)       — Portfolio identifier (format: PORT#####)
  PORT-NAME        PIC X(50)       — Portfolio name
  PORT-CREATE-DATE PIC X(10)       — Creation date
  PORT-STATUS      PIC X(01)       — A=Active, I=Inactive, C=Closed
  PORT-TOTAL-VALUE PIC S9(13)V99   — Total portfolio value (COMP-3)
  FILLER           PIC X(24)
```

#### Linkage Section Interface

```
LS-COMMAND-AREA:
  LS-COMMAND       PIC X(01)   — C=Create, R=Read, U=Update, D=Delete
  LS-PORTFOLIO     PIC X(100)  — Portfolio record data
  LS-RETURN-CODE   PIC S9(4)   — Return code (0=success, 8=error)
```

---

### PORTADD — Portfolio Addition

**Location:** `src/programs/portfolio/PORTADD.cbl`  
**Type:** Portfolio  
**Purpose:** Reads portfolio records from an input sequential file and adds them to the VSAM portfolio master file.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PORTFLIO | `src/copybook/common/` | Portfolio record layout |

#### File I/O

| File | Organization | Purpose |
|------|-------------|---------|
| PORTFOLIO-FILE | VSAM Indexed | Target — portfolio master |
| INPUT-FILE | Sequential | Source — new portfolio records |

---

### PORTREAD — Portfolio Record Read

**Location:** `src/programs/portfolio/PORTREAD.cbl`  
**Type:** Portfolio  
**Purpose:** Reads and displays portfolio records from the VSAM master file.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PORTFLIO | `src/copybook/common/` | Portfolio record layout |

#### File I/O

| File | Organization | Purpose |
|------|-------------|---------|
| PORTFOLIO-FILE | VSAM Indexed | Portfolio master records |

---

### PORTUPDT — Portfolio Update

**Location:** `src/programs/portfolio/PORTUPDT.cbl`  
**Type:** Portfolio  
**Purpose:** Reads update records from a sequential file and applies them to existing portfolio master records via REWRITE.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PORTFLIO | `src/copybook/common/` | Portfolio record layout |

#### File I/O

| File | Organization | Purpose |
|------|-------------|---------|
| PORTFOLIO-FILE | VSAM Indexed | Target — portfolio master |
| UPDATE-FILE | Sequential | Source — update records |

---

### PORTDEL — Portfolio Deletion

**Location:** `src/programs/portfolio/PORTDEL.cbl`  
**Type:** Portfolio  
**Purpose:** Processes portfolio deletion requests. Reads deletion criteria from a sequential file, deletes matching records from the VSAM master, and writes audit records.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PORTFLIO | `src/copybook/common/` | Portfolio record layout |

#### File I/O

| File | Organization | Purpose |
|------|-------------|---------|
| PORTFOLIO-FILE | VSAM Indexed | Target — portfolio master |
| DELETE-FILE | Sequential | Source — deletion requests |
| AUDIT-FILE | Sequential | Output — deletion audit trail |

---

### PORTTRAN — Portfolio Transaction Processing

**Location:** `src/programs/portfolio/PORTTRAN.cbl`  
**Type:** Portfolio  
**Purpose:** Processes financial transactions (buys, sells, transfers) against portfolio records. Validates transactions, updates positions, and maintains audit trails.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| TRNREC | `src/copybook/common/` | Transaction record layout |
| PORTREC | *(inline reference)* | Portfolio record layout |
| ERRHAND | `src/copybook/common/` | Error handling structures |
| AUDITLOG | `src/copybook/common/` | Audit log record layout |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| AUDPROC | CALL | Writes audit records for each transaction |
| ERRPROC | CALL | Error logging for transaction failures |

#### File I/O

| File | Organization | Purpose |
|------|-------------|---------|
| TRANSACTION-FILE | Sequential | Input — transaction records to process |
| PORTFOLIO-FILE | VSAM Indexed | Target — portfolio positions to update |

---

### PORTVALD — Portfolio Validation

**Location:** `src/programs/portfolio/PORTVALD.cbl`  
**Type:** Portfolio  
**Purpose:** Validates portfolio data fields against business rules defined in the PORTVAL copybook. Called as a validation subroutine.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PORTVAL | `src/copybook/common/` | Portfolio validation rules and field constraints |

---

### PORTTEST — Portfolio Test Data Generator

**Location:** `src/programs/portfolio/PORTTEST.cbl`  
**Type:** Portfolio/Test  
**Purpose:** Generates test portfolio records for testing and benchmarking purposes. Creates synthetic data and writes to a test output file.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PORTFLIO | `src/copybook/common/` | Portfolio record layout |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### File I/O

| File | Organization | Purpose |
|------|-------------|---------|
| TEST-FILE | Sequential | Output — generated test portfolio records |

---

## Utility Programs

### UTLMNT00 — File Maintenance

**Location:** `src/programs/utility/UTLMNT00.cbl`  
**Type:** Utility  
**Purpose:** Performs file maintenance operations including archival of aged records, file reorganization, and cleanup of expired data.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| RTNCODE | `src/copybook/common/` | Return code definitions |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| CONTROL-FILE | CTLFILE | Sequential | Maintenance control parameters |
| ARCHIVE-FILE | ARCHFILE | Sequential | Target — archived records |
| REPORT-FILE | RPTFILE | Sequential | Output — maintenance report |

---

### UTLMON00 — System Monitoring

**Location:** `src/programs/utility/UTLMON00.cbl`  
**Type:** Utility  
**Purpose:** Monitors system health by checking DB2 statistics, batch processing metrics, and resource utilization. Generates alerts when thresholds are exceeded.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| DB2STAT | *(reference)* | DB2 statistics data structures |
| RTNCODE | `src/copybook/common/` | Return code definitions |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### Program Dependencies

| Called Program | Call Type | Purpose |
|---------------|-----------|---------|
| ILBOABN0 | CALL | System delay/wait (IBM LE routine) |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| MONITOR-CONFIG | MONCFG | Sequential | Input — monitoring thresholds |
| MONITOR-LOG | MONLOG | Sequential | Output — monitoring log |
| ALERT-FILE | ALERTS | Sequential | Output — threshold alerts |
| DB2-STATS | DB2STATS | Sequential | Input — DB2 performance data |

---

### UTLVAL00 — Data Validation

**Location:** `src/programs/utility/UTLVAL00.cbl`  
**Type:** Utility  
**Purpose:** Validates data integrity across the system by cross-checking position master records against transaction history and applying business rules.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| POSREC | `src/copybook/common/` | Position master record layout |
| TRNREC | `src/copybook/common/` | Transaction record layout |
| RTNCODE | `src/copybook/common/` | Return code definitions |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| VALIDATION-CONTROL | VALCTL | Sequential | Input — validation rules/control |
| POSITION-MASTER | POSMSTRE | VSAM | Input — position records to validate |
| TRANSACTION-HISTORY | TRANHIST | Indexed | Input — transaction records to cross-check |
| ERROR-REPORT | ERRRPT | Sequential | Output — validation discrepancies |

---

## Test Programs

### TSTGEN00 — Test Data Generation

**Location:** `src/programs/test/TSTGEN00.cbl`  
**Type:** Test  
**Purpose:** Generates synthetic test data for portfolios and transactions based on configuration parameters. Uses COPY REPLACING to adapt copybook prefixes.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| PORTFLIO | `src/copybook/common/` | Portfolio layout (REPLACING ==:PREFIX:== BY ==PORT==) |
| TRNREC | `src/copybook/common/` | Transaction layout (REPLACING ==:PREFIX:== BY ==TRAN==) |
| RTNCODE | `src/copybook/common/` | Return code definitions |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| TEST-CONFIG | TSTCFG | Sequential | Input — generation parameters |
| PORTFOLIO-OUT | PORTOUT | Sequential | Output — generated portfolio records |
| TRANSACTION-OUT | TRANOUT | Sequential | Output — generated transaction records |
| RANDOM-SEED | RANDSEED | Sequential | Input — random seed values |

---

### TSTVAL00 — Test Validation

**Location:** `src/programs/test/TSTVAL00.cbl`  
**Type:** Test  
**Purpose:** Validates test results by comparing actual output against expected results. Generates a test report with pass/fail outcomes.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| RTNCODE | `src/copybook/common/` | Return code definitions |
| ERRHAND | `src/copybook/common/` | Error handling structures |

#### File I/O

| File | DD Name | Organization | Purpose |
|------|---------|-------------|---------|
| TEST-CASES | TESTCASE | Sequential | Input — test case definitions |
| EXPECTED-RESULTS | EXPECTED | Sequential | Input — expected outputs |
| ACTUAL-RESULTS | ACTUAL | Sequential | Input — actual program outputs |
| TEST-REPORT | TESTRPT | Sequential | Output — pass/fail report |

---

## Copybook Reference

### Batch Copybooks (`src/copybook/batch/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| **BCHCTL** | Batch control record layout | BCT-KEY, BCT-JOB-NAME, BCT-STATUS, BCT-STAT-ACTIVE |
| **BCHCON** | Batch constants and limits | BCT-RC-SUCCESS, BCT-RC-WARNING, BCT-RC-ERROR |
| **CKPRST** | Checkpoint/restart structures | Checkpoint position, commit point data |
| **PRCSEQ** | Process sequence records | Step ordering, step status, dependencies |

### Common Copybooks (`src/copybook/common/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| **AUDITLOG** | Audit log record layout | Timestamps, user IDs, action types, resource details |
| **COMMON** | System-wide shared definitions | Common constants, date/time formats |
| **ERRHAND** | Error handling (batch) | ERR-MESSAGE, ERR-PROGRAM, ERR-CATEGORY, ERR-CODE, ERR-SEVERITY, ERR-TEXT, ERR-DETAILS, ERR-TIMESTAMP |
| **HISTREC** | Transaction history record | TH-KEY, TH-ACCOUNT-NO, TH-PORTFOLIO-ID, TH-TRANS-DATE/TIME/TYPE, TH-SECURITY-ID, TH-QUANTITY/PRICE/AMOUNT, TH-FEES, TH-COST-BASIS, TH-GAIN-LOSS |
| **PORTFLIO** | Portfolio record layout | Portfolio ID, name, status, holdings, account info |
| **PORTVAL** | Portfolio validation rules | Field constraints, business rules |
| **POSREC** | Position master record | Instrument ID, quantity, cost basis, market value |
| **RETHND** | Return/recovery handling | Recovery data structures |
| **RTNCODE** | Return code definitions | Standard return codes, 88-level condition names |
| **TRNREC** | Transaction record layout | Trade type, amount, dates, security ID |

### DB2 Copybooks (`src/copybook/db2/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| **DBPROC** | DB2 processing parameters | Connection info, SQL options, retry settings |
| **DBTBLS** | DB2 host variable declarations | POSHIST-RECORD (PH-ACCOUNT-NO, PH-PORTFOLIO-ID, PH-TRANS-DATE, etc.) |
| **SQLCA** | SQL Communication Area | SQLCODE, SQLERRM, SQLERRD, SQLWARN |

### Online Copybooks (`src/copybook/online/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| **DB2REQ** | DB2 request/response (online) | Request type, status, recovery data |
| **ERRHND** | Error handling (CICS) | ERR-PROGRAM, ERR-PARAGRAPH, ERR-CICS-RESP/RESP2, ERR-WARNING, ERR-ABEND, ERR-MESSAGE |
| **INQCOM** | Inquiry COMMAREA | WS-COMMAREA-FUNCTION, portfolio/history inquiry fields, response data |
