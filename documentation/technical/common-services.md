# Common/Shared Services Layer Documentation

Version: 1.0
Last Updated: 2024-03-20

## Table of Contents

- [1. Overview](#1-overview)
- [2. DB2 Service Layer Architecture](#2-db2-service-layer-architecture)
- [3. Program Reference](#3-program-reference)
  - [3.1 ERRPROC — Standard Error Processing Subroutine](#31-errproc--standard-error-processing-subroutine)
  - [3.2 AUDPROC — Audit Trail Processing Subroutine](#32-audproc--audit-trail-processing-subroutine)
  - [3.3 DB2CONN — DB2 Connection Manager](#33-db2conn--db2-connection-manager)
  - [3.4 DB2CMT — DB2 Commit Controller](#34-db2cmt--db2-commit-controller)
  - [3.5 DB2ERR — DB2 SQL Error Handler](#35-db2err--db2-sql-error-handler)
  - [3.6 DB2STAT — DB2 Statistics Collector](#36-db2stat--db2-statistics-collector)
- [4. Error Severity Classification](#4-error-severity-classification)
- [5. Copybook Cross-Reference](#5-copybook-cross-reference)
- [6. Copybook Reference](#6-copybook-reference)
  - [6.1 DB2 Copybooks](#61-db2-copybooks)
  - [6.2 Common Copybooks](#62-common-copybooks)
  - [6.3 Batch Copybooks](#63-batch-copybooks)
  - [6.4 Online Copybooks](#64-online-copybooks)

---

## 1. Overview

The Common/Shared Services Layer provides reusable infrastructure programs that underpin the entire Investment Portfolio Management System. These six programs standardize error handling, audit logging, and DB2 database management across all batch, online, and reporting components.

| Program   | Source File                          | Purpose                              |
|-----------|--------------------------------------|--------------------------------------|
| ERRPROC   | `src/programs/common/ERRPROC.cbl`    | Standard error processing            |
| AUDPROC   | `src/programs/common/AUDPROC.cbl`    | Audit trail processing               |
| DB2CONN   | `src/programs/common/DB2CONN.cbl`    | DB2 connection management            |
| DB2CMT    | `src/programs/common/DB2CMT.cbl`     | DB2 commit/rollback control          |
| DB2ERR    | `src/programs/common/DB2ERR.cbl`     | DB2 SQL error handling & diagnostics |
| DB2STAT   | `src/programs/common/DB2STAT.cbl`    | DB2 execution statistics collection  |

---

## 2. DB2 Service Layer Architecture

The four DB2 programs form a cohesive service layer that manages the full lifecycle of database interactions. All batch and online programs use these services rather than issuing raw SQL directly.

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Calling Programs                               │
│         (TRNVAL00, POSUPD00, HISTLD00, INQONLN, etc.)              │
└────────┬──────────┬──────────────┬───────────────┬─────────────────┘
         │          │              │               │
         │ CALL     │ CALL         │ CALL          │ CALL
         ▼          ▼              ▼               ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│  DB2CONN    │ │  DB2CMT     │ │  DB2ERR     │ │  DB2STAT    │
│             │ │             │ │             │ │             │
│ • CONNECT   │ │ • COMMIT    │ │ • LOG error │ │ • INIT      │
│ • DISCONNECT│ │ • ROLLBACK  │ │ • DIAGNOSE  │ │ • UPDATE    │
│ • STATUS    │ │ • SAVEPOINT │ │ • RETRIEVE  │ │ • TERMINATE │
│             │ │ • RESTORE   │ │             │ │ • DISPLAY   │
│ Retry logic │ │ • STATISTICS│ │ Severity    │ │             │
│ with DELAY  │ │             │ │ classification│ │ Temp table │
└──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
       │               │               │               │
       │  SQL:         │  SQL:         │  SQL:         │  SQL:
       │  CONNECT TO   │  COMMIT WORK  │  INSERT INTO  │  DECLARE GLOBAL
       │  CONNECT RESET│  ROLLBACK     │    ERRLOG     │    TEMPORARY TABLE
       │  SELECT       │  SAVEPOINT    │  SELECT FROM  │  INSERT/UPDATE/
       │               │  ROLLBACK TO  │    ERRLOG     │    SELECT on
       │               │    SAVEPOINT  │               │    SESSION.DBSTATS
       ▼               ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          DB2 Subsystem                              │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────────┐   │
│  │ SYSIBM.      │  │ ERRLOG       │  │ SESSION.DBSTATS         │   │
│  │ SYSDUMMY1    │  │ (persistent) │  │ (global temporary)      │   │
│  └──────────────┘  └──────────────┘  └─────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
                            │
                            │  On error, DB2 programs call:
                            ▼
                     ┌─────────────┐
                     │  ERRPROC    │
                     │  (Error log │
                     │   to file)  │
                     └─────────────┘
```

### Interaction Summary

1. **DB2CONN** establishes and manages the database connection. It retries failed connections up to 3 times with a configurable delay (`DB2-RETRY-WAIT` from DBPROC copybook). On disconnect, it issues a final `COMMIT WORK` before `CONNECT RESET`.

2. **DB2CMT** controls transaction boundaries. It supports frequency-based commits (commit every N records), forced commits, savepoint creation with `ON ROLLBACK RETAIN CURSORS`, and rollback-to-savepoint for partial recovery.

3. **DB2ERR** classifies SQL errors by severity, logs them to the persistent `ERRLOG` table, diagnoses error conditions, and retrieves historical errors. It sets a retry flag for deadlocks and timeouts.

4. **DB2STAT** tracks execution metrics using a declared global temporary table (`SESSION.DBSTATS`). It records row counts, commit/rollback counts, CPU time, and elapsed time for each program execution.

5. **ERRPROC** is the system-wide error sink. All DB2 programs (and other system components) delegate to ERRPROC for logging errors to a sequential file and displaying them to the console.

---

## 3. Program Reference

### 3.1 ERRPROC — Standard Error Processing Subroutine

**Source**: `src/programs/common/ERRPROC.cbl`

#### Purpose & Business Function

ERRPROC is the central error logging facility for the entire system. Any program encountering an error calls ERRPROC to record the event in a sequential error log file and display formatted error details on the console. It provides a standardized, auditable error trail across batch and online processing.

#### Copybook Dependencies

| Copybook | Path                              | Usage                                      |
|----------|-----------------------------------|--------------------------------------------|
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Error message structure, categories, return codes, VSAM status handling |

#### DB2 Interactions

None. ERRPROC is a file-based error logger that does not interact with DB2.

#### Linkage Section Interface

```
01  LS-ERROR-REQUEST.
    05  LS-PROGRAM-ID      PIC X(8).       Program issuing the error
    05  LS-CATEGORY        PIC X(2).       Error category (VS, VL, PR, SY)
    05  LS-ERROR-CODE      PIC X(4).       Application error code
    05  LS-SEVERITY        PIC S9(4) COMP. Severity level
    05  LS-ERROR-TEXT      PIC X(80).      Error description
    05  LS-ERROR-DETAILS   PIC X(256).     Extended error details
    05  LS-RETURN-CODE     PIC S9(4) COMP. Return code (output: set to severity)
```

**Function Codes**: None (single-function subroutine).

**Return Codes**: The return code is set to the severity value passed in by the caller.

#### Processing Flow & Paragraph Structure

```
0000-MAIN
  ├── 1000-INITIALIZE       Initialize work areas, accept timestamp,
  │                          OPEN EXTEND error log file
  ├── 2000-PROCESS-ERROR     Build error record from linkage data
  │   ├── 2100-WRITE-LOG     Write formatted record to sequential log
  │   └── 2200-DISPLAY-ERROR Display error details to SYSOUT
  └── 3000-TERMINATE         CLOSE error log file
```

#### Error Categories (from ERRHAND copybook)

| Code | Category   | Description            |
|------|------------|------------------------|
| VS   | VSAM       | VSAM file errors       |
| VL   | Validation | Data validation errors |
| PR   | Processing | Processing errors      |
| SY   | System     | System-level errors    |

---

### 3.2 AUDPROC — Audit Trail Processing Subroutine

**Source**: `src/programs/common/AUDPROC.cbl`

#### Purpose & Business Function

AUDPROC creates an immutable audit trail of system activity. It records transactions, user actions, and system events to a sequential audit file. Each audit entry captures before/after images of data changes, supporting regulatory compliance, forensic analysis, and operational monitoring.

#### Copybook Dependencies

| Copybook | Path                                | Usage                                  |
|----------|-------------------------------------|----------------------------------------|
| AUDITLOG | `src/copybook/common/AUDITLOG.cpy`  | Audit record structure (`AUDIT-RECORD`) — included in FD section |

#### DB2 Interactions

None. AUDPROC writes to a sequential file (AUDFILE), not to DB2.

#### Linkage Section Interface

```
01  LS-AUDIT-REQUEST.
    05  LS-SYSTEM-INFO.
        10  LS-SYSTEM-ID    PIC X(8).      System identifier
        10  LS-USER-ID      PIC X(8).      User performing action
        10  LS-PROGRAM      PIC X(8).      Program name
        10  LS-TERMINAL     PIC X(8).      Terminal ID
    05  LS-TYPE            PIC X(4).       Audit type (TRAN/USER/SYST)
    05  LS-ACTION          PIC X(8).       Action code
    05  LS-STATUS          PIC X(4).       Status (SUCC/FAIL/WARN)
    05  LS-KEY-INFO.
        10  LS-PORT-ID     PIC X(8).       Portfolio ID
        10  LS-ACCT-NO     PIC X(10).      Account number
    05  LS-BEFORE-IMAGE    PIC X(100).     Record image before change
    05  LS-AFTER-IMAGE     PIC X(100).     Record image after change
    05  LS-MESSAGE         PIC X(100).     Descriptive message
    05  LS-RETURN-CODE     PIC S9(4) COMP. Return code (output)
```

**Function Codes**: None (single-function subroutine).

**Return Codes**:

| Code | Meaning                    |
|------|----------------------------|
| 0    | Audit record written OK    |
| 8    | Error opening or writing   |

#### Audit Types (from AUDITLOG copybook)

| Code | Type              | Actions                                       |
|------|-------------------|-----------------------------------------------|
| TRAN | Transaction       | CREATE, UPDATE, DELETE, INQUIRE                |
| USER | User Action       | LOGIN, LOGOUT                                  |
| SYST | System Event      | STARTUP, SHUTDOWN                              |

#### Processing Flow & Paragraph Structure

```
0000-MAIN
  ├── 1000-INITIALIZE       Accept timestamp, OPEN EXTEND audit file
  │                          (on error: set RC=8, terminate, GOBACK)
  ├── 2000-PROCESS-AUDIT     Initialize AUDIT-RECORD, populate from
  │                          linkage data, WRITE record
  │                          (on success: RC=0; on error: RC=8)
  └── 3000-TERMINATE         CLOSE audit file
```

---

### 3.3 DB2CONN — DB2 Connection Manager

**Source**: `src/programs/common/DB2CONN.cbl`

#### Purpose & Business Function

DB2CONN manages the lifecycle of DB2 database connections. It provides connect, disconnect, and status-check operations with built-in retry logic for transient connection failures. This centralizes connection management so that calling programs need not handle raw CONNECT/DISCONNECT SQL.

#### Copybook Dependencies

| Copybook | Path                              | Usage                                        |
|----------|-----------------------------------|----------------------------------------------|
| SQLCA    | `src/copybook/db2/SQLCA.cpy`      | SQL Communication Area for SQLCODE/SQLSTATE  |
| DBPROC   | `src/copybook/db2/DBPROC.cpy`     | DB2 standard procedures, retry wait interval |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Error message structure for ERRPROC calls    |

#### DB2 Interactions

| Operation       | SQL Statement                               | Purpose                           |
|-----------------|---------------------------------------------|-----------------------------------|
| Connect         | `CONNECT TO :WS-DB-NAME`                    | Establish DB2 connection          |
| Disconnect      | `COMMIT WORK` then `CONNECT RESET`          | Clean disconnect with final commit|
| Status Check    | `SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1` | Verify connection is active    |

#### Linkage Section Interface

```
01  LS-DB2-REQUEST.
    05  LS-FUNCTION         PIC X(4).      Function code
        88  FUNC-CONN         VALUE 'CONN'.   Connect
        88  FUNC-DISC         VALUE 'DISC'.   Disconnect
        88  FUNC-STAT         VALUE 'STAT'.   Check status
    05  LS-DB-NAME          PIC X(8).      Database name
    05  LS-PLAN-NAME        PIC X(8).      DB2 plan name
    05  LS-RETURN-CODE      PIC S9(4) COMP. Return code (output)
    05  LS-ERROR-INFO.
        10  LS-SQLCODE      PIC S9(9) COMP. SQLCODE (output)
        10  LS-ERROR-MSG    PIC X(80).      Error message (output)
```

**Return Codes**:

| Code | Meaning                              |
|------|--------------------------------------|
| 0    | Operation successful                 |
| 4    | Connection not active (status check) |
| 8    | Disconnect error                     |
| 12   | Connection failed / invalid function |

#### Retry Logic

DB2CONN implements automatic retry for connection failures:

- **Maximum retries**: 3 (`WS-MAX-RETRIES`)
- **Delay between retries**: Calls external `DELAY` subroutine with `DB2-RETRY-WAIT` parameter (default 100 ms, defined in DBPROC copybook)
- **Retry loop**: Continues until connected or retry count exhausted
- **Connection error classification**:
  - SQLCODE `-30081`: Maximum connections exceeded
  - SQLCODE `-99999`: Network error
  - Other: General DB2 connection error

#### Processing Flow & Paragraph Structure

```
0000-MAIN
  ├── EVALUATE LS-FUNCTION
  │   ├── 'CONN' → 1000-CONNECT
  │   │   └── Loop: attempt CONNECT TO, on failure:
  │   │       ├── 1100-HANDLE-CONN-ERROR  (classify SQLCODE)
  │   │       └── CALL 'DELAY'            (wait before retry)
  │   ├── 'DISC' → 2000-DISCONNECT
  │   │   └── COMMIT WORK → CONNECT RESET
  │   ├── 'STAT' → 3000-CHECK-STATUS
  │   │   └── SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1
  │   └── OTHER  → 9000-ERROR-ROUTINE
  │       └── CALL 'ERRPROC'
  └── GOBACK
```

---

### 3.4 DB2CMT — DB2 Commit Controller

**Source**: `src/programs/common/DB2CMT.cbl`

#### Purpose & Business Function

DB2CMT provides centralized transaction control for all DB2 operations. It manages commits, rollbacks, savepoints, and savepoint restoration. The frequency-based commit mechanism supports high-volume batch processing by committing at regular intervals, which limits restart windows and resource lock duration.

#### Copybook Dependencies

| Copybook | Path                              | Usage                                         |
|----------|-----------------------------------|-----------------------------------------------|
| SQLCA    | `src/copybook/db2/SQLCA.cpy`      | SQL Communication Area for SQLCODE            |
| DBPROC   | `src/copybook/db2/DBPROC.cpy`     | DB2 standard procedures                       |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Error message structure for ERRPROC calls     |

#### DB2 Interactions

| Operation           | SQL Statement                                          | Purpose                           |
|---------------------|--------------------------------------------------------|-----------------------------------|
| Commit              | `COMMIT WORK`                                          | Commit current transaction        |
| Rollback            | `ROLLBACK WORK`                                        | Rollback entire transaction       |
| Create Savepoint    | `SAVEPOINT :WS-SAVEPOINT-ID ON ROLLBACK RETAIN CURSORS` | Intermediate recovery point     |
| Restore Savepoint   | `ROLLBACK TO SAVEPOINT :WS-SAVEPOINT-ID`               | Partial rollback to savepoint    |

#### Linkage Section Interface

```
01  LS-COMMIT-REQUEST.
    05  LS-FUNCTION         PIC X(4).      Function code
        88  FUNC-INIT         VALUE 'INIT'.   Initialize counters
        88  FUNC-CMIT         VALUE 'CMIT'.   Conditional commit
        88  FUNC-RBACK        VALUE 'RBAK'.   Rollback
        88  FUNC-SAVE         VALUE 'SAVE'.   Create savepoint
        88  FUNC-REST         VALUE 'REST'.   Restore to savepoint
        88  FUNC-STAT         VALUE 'STAT'.   Display statistics
    05  LS-SAVEPOINT-NAME   PIC X(18).     Savepoint identifier
    05  LS-COMMIT-PARMS.
        10  LS-RECORDS-PROC PIC S9(9) COMP.  Records processed so far
        10  LS-COMMIT-FREQ  PIC S9(4) COMP.  Commit every N records
        10  LS-FORCE-FLAG   PIC X(1).         Force commit flag
            88  LS-FORCE-COMMIT VALUE 'Y'.
    05  LS-RETURN-CODE      PIC S9(4) COMP. Return code (output)
    05  LS-ERROR-INFO.
        10  LS-SQLCODE      PIC S9(9) COMP. SQLCODE (output)
        10  LS-ERROR-MSG    PIC X(80).      Error message (output)
```

**Return Codes**:

| Code | Meaning                              |
|------|--------------------------------------|
| 0    | Operation successful                 |
| 8    | Commit/rollback/savepoint failed     |
| 12   | Invalid function code                |

#### Commit Logic

The `CMIT` function uses frequency-based commit control:

```
IF LS-RECORDS-PROC >= LS-COMMIT-FREQ
OR LS-FORCE-COMMIT
    Issue COMMIT WORK
END-IF
```

This pattern enables callers to commit at regular intervals (e.g., every 1000 records) or force an immediate commit at critical points (end-of-job, error recovery).

#### Internal Statistics

DB2CMT tracks the following counters in working storage:

| Counter              | Description               |
|----------------------|---------------------------|
| WS-COMMIT-COUNT      | Total commits issued      |
| WS-ROLLBACK-COUNT    | Total rollbacks issued    |
| WS-SAVEPOINT-COUNT   | Total savepoints created  |

The `STAT` function displays these via `DISPLAY` statements.

#### Processing Flow & Paragraph Structure

```
0000-MAIN
  ├── EVALUATE LS-FUNCTION
  │   ├── 'INIT' → 1000-INITIALIZE       Reset counters
  │   ├── 'CMIT' → 2000-COMMIT           Check frequency/force flag
  │   │   └── 2100-ISSUE-COMMIT           COMMIT WORK, increment counter
  │   ├── 'RBAK' → 3000-ROLLBACK         ROLLBACK WORK
  │   ├── 'SAVE' → 4000-SAVEPOINT        SAVEPOINT with RETAIN CURSORS
  │   ├── 'REST' → 5000-RESTORE          ROLLBACK TO SAVEPOINT
  │   ├── 'STAT' → 6000-STATISTICS       DISPLAY commit/rollback counts
  │   └── OTHER  → 9000-ERROR-ROUTINE    CALL 'ERRPROC'
  └── GOBACK

Error delegation:
  9000-ERROR-ROUTINE → CALL 'ERRPROC' USING ERR-MESSAGE
  9100-LOG-ERROR     → CALL 'DB2ERR' USING LS-ERROR-INFO
```

---

### 3.5 DB2ERR — DB2 SQL Error Handler

**Source**: `src/programs/common/DB2ERR.cbl`

#### Purpose & Business Function

DB2ERR is the specialized SQL error handler. It classifies DB2 errors by severity, logs them to the persistent `ERRLOG` table, provides diagnostic text for known error conditions, and retrieves historical error information. Its severity classification and retry flag mechanism allow calling programs to make informed decisions about recovery vs. abort.

#### Copybook Dependencies

| Copybook | Path                              | Usage                                        |
|----------|-----------------------------------|----------------------------------------------|
| SQLCA    | `src/copybook/db2/SQLCA.cpy`      | SQL Communication Area for SQLCODE/SQLSTATE  |
| DBPROC   | `src/copybook/db2/DBPROC.cpy`     | DB2 standard procedures                      |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Error message structure for ERRPROC calls    |
| DBTBLS   | `src/copybook/db2/DBTBLS.cpy`     | ERRLOG-RECORD table layout (via REPLACING)   |

#### DB2 Interactions

| Operation         | SQL Statement                              | Purpose                              |
|-------------------|--------------------------------------------|--------------------------------------|
| Log Error         | `INSERT INTO ERRLOG VALUES (:WS-ERRLOG-REC)` | Persist error to ERRLOG table     |
| Retrieve Error    | `SELECT ... FROM ERRLOG WHERE PROGRAM_ID = :LS-PROGRAM-ID AND ERROR_TIMESTAMP = (SELECT MAX(...))` | Retrieve most recent error for a program |

#### Linkage Section Interface

```
01  LS-ERROR-REQUEST.
    05  LS-FUNCTION         PIC X(4).      Function code
        88  FUNC-LOG          VALUE 'LOG '.   Log error to ERRLOG
        88  FUNC-DIAG         VALUE 'DIAG'.   Diagnose SQLCODE
        88  FUNC-RETR         VALUE 'RETR'.   Retrieve last error
    05  LS-PROGRAM-ID       PIC X(8).      Calling program name
    05  LS-ERROR-INFO.
        10  LS-SQLCODE      PIC S9(9) COMP. SQLCODE value
        10  LS-SQLSTATE     PIC X(5).       SQLSTATE value
        10  LS-ERROR-TEXT   PIC X(80).      Error message (input/output)
    05  LS-ADDITIONAL-INFO  PIC X(100).     Extra context
    05  LS-RETURN-CODE      PIC S9(4) COMP. Return code (output)
    05  LS-RETRY-FLAG       PIC X(1).       Retry recommendation (output)
        88  LS-SHOULD-RETRY   VALUE 'Y'.
        88  LS-NO-RETRY       VALUE 'N'.
```

**Return Codes**:

| Code | Meaning                               |
|------|---------------------------------------|
| 0    | Error logged successfully             |
| 4    | Warning / deadlock / timeout          |
| 8    | Duplicate key violation               |
| 12   | Connection error / unhandled / invalid function |

#### Error Categories & Severity Classification

DB2ERR classifies SQL errors into categories with defined severity levels and retry behavior:

| SQLCODE  | Condition            | Severity | Retry? | Return Code | Diagnostic Text                        |
|----------|----------------------|----------|--------|-------------|----------------------------------------|
| -911     | Deadlock             | 2 (Warn) | Yes    | 4           | "Deadlock detected - retry transaction"|
| -913     | Timeout              | 2 (Warn) | Yes    | 4           | "Timeout occurred - retry transaction" |
| -30081   | Connection Error     | 4 (Severe)| No    | 12          | "DB2 connection error - check availability"|
| -803     | Duplicate Key        | 1 (Info) | No     | 8           | "Duplicate key violation"              |
| +100     | Not Found            | 1 (Info) | No     | 4*          | (set during LOG; DIAG returns different RC) |
| < 0 (other) | Unhandled Error   | 3 (Error)| No     | 12          | "Unhandled DB2 error"                  |
| >= 0 (other) | Warning          | 1 (Info) | No     | 4           | "DB2 warning condition"                |

*Note: The severity values map to the ERRLOG table's `EL-ERROR-SEVERITY` field (from DBTBLS copybook):*

| Severity Value | Level 88 Name  | Meaning  |
|----------------|----------------|----------|
| 1              | EL-SEV-INFO    | Info     |
| 2              | EL-SEV-WARN    | Warning  |
| 3              | EL-SEV-ERROR   | Error    |
| 4              | EL-SEV-SEVERE  | Severe   |

#### Processing Flow & Paragraph Structure

```
0000-MAIN
  ├── EVALUATE LS-FUNCTION
  │   ├── 'LOG ' → 1000-LOG-ERROR
  │   │   ├── 1100-SET-SEVERITY     Classify SQLCODE → severity + retry flag
  │   │   └── 1200-INSERT-ERROR     INSERT INTO ERRLOG
  │   ├── 'DIAG' → 2000-DIAGNOSE-ERROR
  │   │   └── EVALUATE LS-SQLCODE   Return diagnostic text + return code
  │   ├── 'RETR' → 3000-RETRIEVE-ERROR
  │   │   └── SELECT MAX(ERROR_TIMESTAMP) FROM ERRLOG
  │   └── OTHER  → 9000-ERROR-ROUTINE
  │       └── CALL 'ERRPROC'
  └── GOBACK
```

---

### 3.6 DB2STAT — DB2 Statistics Collector

**Source**: `src/programs/common/DB2STAT.cbl`

#### Purpose & Business Function

DB2STAT collects and reports runtime execution statistics for DB2 programs. It uses a declared global temporary table to track row counts, commit/rollback counts, and timing metrics. This enables performance monitoring and capacity planning without impacting permanent tables.

#### Copybook Dependencies

| Copybook | Path                              | Usage                                        |
|----------|-----------------------------------|----------------------------------------------|
| SQLCA    | `src/copybook/db2/SQLCA.cpy`      | SQL Communication Area for SQLCODE           |
| DBPROC   | `src/copybook/db2/DBPROC.cpy`     | DB2 standard procedures                      |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Error message structure for ERRPROC calls    |

#### DB2 Interactions

| Operation          | SQL Statement                                                      | Purpose                              |
|--------------------|--------------------------------------------------------------------|--------------------------------------|
| Create Temp Table  | `DECLARE GLOBAL TEMPORARY TABLE SESSION.DBSTATS (...) ON COMMIT PRESERVE ROWS` | Create session-scoped stats table |
| Insert Initial     | `INSERT INTO SESSION.DBSTATS (...) VALUES (..., 0, 0, 0, 0, 0, 0)` | Initialize stats row for program    |
| Update Stats       | `UPDATE SESSION.DBSTATS SET ROWS_READ=..., ... WHERE PROGRAM_ID=...` | Update running counters             |
| Finalize           | `UPDATE SESSION.DBSTATS SET END_TIME=..., CPU_TIME=..., ELAPSED_TIME=... WHERE PROGRAM_ID=...` | Record final timing |
| Display            | `SELECT ... FROM SESSION.DBSTATS WHERE PROGRAM_ID=...`             | Retrieve stats for display           |

#### Statistics Temporary Table Schema

```sql
DECLARE GLOBAL TEMPORARY TABLE SESSION.DBSTATS (
    PROGRAM_ID      CHAR(8)       NOT NULL,
    START_TIME      TIMESTAMP     NOT NULL,
    END_TIME        TIMESTAMP,
    ROWS_READ       INTEGER       NOT NULL,
    ROWS_INSERTED   INTEGER       NOT NULL,
    ROWS_UPDATED    INTEGER       NOT NULL,
    ROWS_DELETED    INTEGER       NOT NULL,
    COMMITS         INTEGER       NOT NULL,
    ROLLBACKS       INTEGER       NOT NULL,
    CPU_TIME        DECIMAL(11,2),
    ELAPSED_TIME    DECIMAL(11,2)
) ON COMMIT PRESERVE ROWS
```

The table is session-scoped (exists only for the DB2 session lifetime) and uses `ON COMMIT PRESERVE ROWS` to retain data across commits.

#### Linkage Section Interface

```
01  LS-STAT-REQUEST.
    05  LS-FUNCTION         PIC X(4).      Function code
        88  FUNC-INIT         VALUE 'INIT'.   Initialize (create table + insert)
        88  FUNC-UPDT         VALUE 'UPDT'.   Update counters
        88  FUNC-TERM         VALUE 'TERM'.   Finalize (calc times, update)
        88  FUNC-DISP         VALUE 'DISP'.   Display stats
    05  LS-PROGRAM-ID       PIC X(8).      Program being tracked
    05  LS-STAT-DATA.
        10  LS-ROWS-READ    PIC S9(9) COMP. Rows read count
        10  LS-ROWS-INSRT   PIC S9(9) COMP. Rows inserted count
        10  LS-ROWS-UPDT    PIC S9(9) COMP. Rows updated count
        10  LS-ROWS-DELT    PIC S9(9) COMP. Rows deleted count
        10  LS-COMMITS      PIC S9(9) COMP. Commit count
        10  LS-ROLLBACKS    PIC S9(9) COMP. Rollback count
    05  LS-RETURN-CODE      PIC S9(4) COMP. Return code (output)
```

**Return Codes**:

| Code | Meaning                           |
|------|-----------------------------------|
| 0    | Operation successful              |
| 12   | Error (table creation, insert, update, or invalid function) |

#### Time Calculation

On `TERM`, DB2STAT calculates:

- **Elapsed time** = `NUMVAL(end_timestamp) - NUMVAL(start_timestamp)` (seconds)
- **CPU time** = `elapsed_time × 0.65` (estimated CPU fraction)

#### Processing Flow & Paragraph Structure

```
0000-MAIN
  ├── EVALUATE LS-FUNCTION
  │   ├── 'INIT' → 1000-INITIALIZE
  │   │   ├── 1100-CREATE-STATS-TABLE   DECLARE GLOBAL TEMPORARY TABLE
  │   │   │                              (ignores SQLCODE -601 if exists)
  │   │   └── 1200-INSERT-INITIAL        INSERT initial zero-count row
  │   ├── 'UPDT' → 2000-UPDATE-STATS    UPDATE counters from linkage
  │   ├── 'TERM' → 3000-TERMINATE
  │   │   ├── 3100-CALC-TIMES           Compute elapsed & CPU time
  │   │   └── UPDATE end time + timing  Finalize stats row
  │   │   └── 4000-DISPLAY-STATS        (auto-called on success)
  │   ├── 'DISP' → 4000-DISPLAY-STATS   SELECT + DISPLAY stats
  │   └── OTHER  → 9000-ERROR-ROUTINE   CALL 'ERRPROC'
  └── GOBACK
```

---

## 4. Error Severity Classification

The system uses a consistent severity model across all components. The following table shows the DB2-specific error classification as implemented in DB2ERR:

| Severity | Level          | SQLCODE(s)    | Condition          | Retry | Action Required                       |
|----------|----------------|---------------|--------------------|-------|---------------------------------------|
| 1        | Info           | +100          | Not Found          | No    | Normal condition, handle in caller    |
| 1        | Info           | -803          | Duplicate Key      | No    | Business logic error, handle in caller|
| 1        | Info           | > 0 (other)   | Warning            | No    | Log and continue                      |
| 2        | Warning        | -911          | Deadlock           | Yes   | Retry the transaction                 |
| 2        | Warning        | -913          | Timeout            | Yes   | Retry the transaction                 |
| 3        | Error          | < 0 (other)   | Unhandled Error    | No    | Log, rollback, investigate            |
| 4        | Severe         | -30081        | Connection Error   | No    | Critical — check DB2 availability     |

### System-Wide Return Code Standard (from ERRHAND / COMMON copybooks)

| Code | Level    | Description                                    |
|------|----------|------------------------------------------------|
| 0    | Success  | Operation completed normally                   |
| 4    | Warning  | Non-critical issue, processing continues       |
| 8    | Error    | Significant error, operation may have failed   |
| 12   | Severe   | Critical error, program should terminate       |
| 16   | Terminal | Unrecoverable error, immediate abort required  |

---

## 5. Copybook Cross-Reference

The following table shows which copybooks each common program includes:

| Copybook  | ERRPROC | AUDPROC | DB2CONN | DB2CMT | DB2ERR | DB2STAT |
|-----------|:-------:|:-------:|:-------:|:------:|:------:|:-------:|
| SQLCA     |         |         |    X    |   X    |   X    |    X    |
| DBPROC    |         |         |    X    |   X    |   X    |    X    |
| ERRHAND   |    X    |         |    X    |   X    |   X    |    X    |
| DBTBLS    |         |         |         |        |   X    |         |
| AUDITLOG  |         |    X    |         |        |        |         |

### Cross-Program Call Dependencies

| Caller    | Callee   | Purpose                             |
|-----------|----------|-------------------------------------|
| DB2CONN   | ERRPROC  | Log invalid function codes          |
| DB2CONN   | DELAY    | Wait between connection retries     |
| DB2CMT    | ERRPROC  | Log invalid function codes          |
| DB2CMT    | DB2ERR   | Log SQL errors from commit/rollback |
| DB2ERR    | ERRPROC  | Log invalid function codes and insert failures |
| DB2STAT   | ERRPROC  | Log errors from stats operations    |

---

## 6. Copybook Reference

### 6.1 DB2 Copybooks

#### SQLCA — SQL Communication Area
**Path**: `src/copybook/db2/SQLCA.cpy`

Includes the standard DB2 SQLCA via `EXEC SQL INCLUDE SQLCA END-EXEC` and defines named SQLSTATE constants:

| Constant              | SQLSTATE | Condition         |
|-----------------------|----------|-------------------|
| SQL-SUCCESS           | 00000    | Successful        |
| SQL-NOT-FOUND         | 02000    | Row not found     |
| SQL-DUP-KEY           | 23505    | Duplicate key     |
| SQL-DEADLOCK          | 40001    | Deadlock          |
| SQL-TIMEOUT           | 40003    | Timeout           |
| SQL-CONNECTION-ERROR  | 08001    | Connection error  |
| SQL-DB-ERROR          | 58004    | DB2 system error  |

#### DBPROC — DB2 Standard Procedures
**Path**: `src/copybook/db2/DBPROC.cpy`

Defines reusable DB2 infrastructure:

- **DB2-ERROR-HANDLING**: Error message structure (`DB2-SQLCODE-TXT`, `DB2-STATE`, `DB2-ERROR-TEXT`), retry counter (`DB2-RETRY-COUNT`, `DB2-MAX-RETRIES` = 3), and retry wait interval (`DB2-RETRY-WAIT` = 100).
- **CONNECT-TO-DB2**: Standard connection paragraph connecting to `POSMVP` database.
- **DISCONNECT-FROM-DB2**: Standard disconnect with `COMMIT WORK` + `CONNECT RESET`.
- **DB2-ERROR-ROUTINE**: Formats SQLCODE/SQLSTATE, issues `ROLLBACK WORK`, calls `ERRPROC`.
- **CHECK-SQL-STATUS**: Checks `SQLCODE NOT = 0` and invokes `DB2-ERROR-ROUTINE`.

#### DBTBLS — DB2 Table Definitions
**Path**: `src/copybook/db2/DBTBLS.cpy`

Defines COBOL record layouts for two DB2 tables:

**POSHIST-RECORD (Position History Table)**:

| Field                | PIC             | Description                   |
|----------------------|-----------------|-------------------------------|
| PH-ACCOUNT-NO       | X(8)            | Account number                |
| PH-PORTFOLIO-ID      | X(10)           | Portfolio identifier          |
| PH-TRANS-DATE        | X(10)           | Transaction date              |
| PH-TRANS-TIME        | X(8)            | Transaction time              |
| PH-TRANS-TYPE        | X(2)            | Transaction type              |
| PH-SECURITY-ID       | X(12)           | Security identifier           |
| PH-QUANTITY          | S9(12)V9(3)     | Quantity (COMP-3)             |
| PH-PRICE             | S9(12)V9(3)     | Price (COMP-3)                |
| PH-AMOUNT            | S9(13)V9(2)     | Amount (COMP-3)               |
| PH-FEES              | S9(13)V9(2)     | Fees (COMP-3)                 |
| PH-TOTAL-AMOUNT      | S9(13)V9(2)     | Total amount (COMP-3)         |
| PH-COST-BASIS        | S9(13)V9(2)     | Cost basis (COMP-3)           |
| PH-GAIN-LOSS         | S9(13)V9(2)     | Gain/loss (COMP-3)            |
| PH-PROCESS-DATE      | X(10)           | Processing date               |
| PH-PROCESS-TIME      | X(8)            | Processing time               |
| PH-PROGRAM-ID        | X(8)            | Processing program            |
| PH-USER-ID           | X(8)            | Processing user               |
| PH-AUDIT-TIMESTAMP   | X(26)           | Audit timestamp               |

**ERRLOG-RECORD (Error Log Table)**:

| Field                | PIC             | Description                   |
|----------------------|-----------------|-------------------------------|
| EL-ERROR-TIMESTAMP   | X(26)           | When error occurred           |
| EL-PROGRAM-ID        | X(8)            | Program that errored          |
| EL-ERROR-TYPE        | X(1)            | S=System, A=App, D=Data       |
| EL-ERROR-SEVERITY    | S9(4) COMP      | 1=Info, 2=Warn, 3=Error, 4=Severe |
| EL-ERROR-CODE        | X(8)            | Error code string             |
| EL-ERROR-MESSAGE     | X(200)          | Error description             |
| EL-PROCESS-DATE      | X(10)           | Processing date               |
| EL-PROCESS-TIME      | X(8)            | Processing time               |
| EL-USER-ID           | X(8)            | User ID                       |
| EL-ADDITIONAL-INFO   | X(500)          | Extended error context        |

---

### 6.2 Common Copybooks

#### ERRHAND — Standard Error Handling Definitions
**Path**: `src/copybook/common/ERRHAND.cpy`

Defines:
- **ERR-CATEGORIES**: Error category codes (VS=VSAM, VL=Validation, PR=Processing, SY=System)
- **ERR-RETURN-CODES**: Standard return codes (0=Success, 4=Warning, 8=Error, 12=Severe, 16=Terminal)
- **ERR-MESSAGE**: Structured error message with timestamp, program, category, code, severity, text (80 chars), and details (256 chars)
- **ERR-VSAM-STATUSES**: VSAM file status values (00=Success, 22=DupKey, 23=NotFound, 10=EOF)
- **ERR-VSAM-MSGS**: Human-readable VSAM error messages

#### AUDITLOG — Audit Trail Record Definitions
**Path**: `src/copybook/common/AUDITLOG.cpy`

Defines the `AUDIT-RECORD` layout used by AUDPROC:
- **AUD-HEADER**: Timestamp, system ID, user ID, program, terminal
- **AUD-TYPE**: TRAN (transaction), USER (user action), SYST (system event)
- **AUD-ACTION**: CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN
- **AUD-STATUS**: SUCC (success), FAIL (failure), WARN (warning)
- **AUD-KEY-INFO**: Portfolio ID + Account number
- **AUD-BEFORE-IMAGE / AUD-AFTER-IMAGE**: 100-byte before/after images
- **AUD-MESSAGE**: 100-byte descriptive message

#### COMMON — Common Definitions and Constants
**Path**: `src/copybook/common/COMMON.cpy`

System-wide constants:
- **RETURN-CODES**: RC-SUCCESS(0), RC-WARNING(4), RC-ERROR(8), RC-SEVERE(12), RC-CRITICAL(16)
- **STATUS-CODES**: Active(A), Closed(C), Pending(P), Suspended(S), Failed(F), Reversed(R)
- **TRANSACTION-TYPES**: Buy(BU), Sell(SL), Transfer(TR), Fee(FE)
- **COMMON-DATETIME**: Date/time working fields
- **ERROR-HANDLING**: Generic error fields (code, module, routine, message)
- **AUDIT-FIELDS**: Common audit trail fields
- **CURRENCY-CODES**: USD, EUR, GBP, JPY, CAD

#### PORTFLIO — Portfolio Master Record Layout
**Path**: `src/copybook/common/PORTFLIO.cpy`

Defines `PORT-RECORD`:
- **PORT-KEY**: Portfolio ID (X(8)) + Account number (X(10))
- **PORT-CLIENT-INFO**: Client name (X(30)), type (I=Individual, C=Corporate, T=Trust)
- **PORT-PORTFOLIO-INFO**: Create date, last maintenance date, status (A=Active, C=Closed, S=Suspended)
- **PORT-FINANCIAL-INFO**: Total value and cash balance (S9(13)V99 COMP-3)
- **PORT-AUDIT-INFO**: Last user, last transaction date
- **PORT-FILLER**: 50 bytes reserved

#### PORTVAL — Portfolio Validation Rules
**Path**: `src/copybook/common/PORTVAL.cpy`

Defines validation infrastructure:
- **VAL-RETURN-CODES**: Success(0), Invalid ID(1), Invalid Acct(2), Invalid Type(3), Invalid Amt(4)
- **VAL-ERROR-MESSAGES**: Human-readable validation error texts
- **VAL-CONSTANTS**: Min/max amount bounds, ID prefix ('PORT')
- **VAL-WORK-AREAS**: Temporary fields for validation processing

#### POSREC — Position Record Structure
**Path**: `src/copybook/common/POSREC.cpy`

Defines `POSITION-RECORD`:
- **POS-KEY**: Portfolio ID + Date + Investment ID
- **POS-DATA**: Quantity (S9(11)V9(4)), Cost Basis, Market Value (S9(13)V9(2)), Currency, Status (A/C/P)
- **POS-AUDIT**: Last maintenance date and user
- **POS-FILLER**: 50 bytes reserved

#### HISTREC — History Record Structure
**Path**: `src/copybook/common/HISTREC.cpy`

Defines `HISTORY-RECORD`:
- **HIST-KEY**: Portfolio ID + Date + Time + Sequence number
- **HIST-DATA**: Record type (PT=Portfolio, PS=Position, TR=Transaction), Action (A=Add, C=Change, D=Delete), Before/After images (400 bytes each), Reason code
- **HIST-AUDIT**: Process date and user
- **HIST-FILLER**: 50 bytes reserved

#### TRNREC — Transaction Record Structure
**Path**: `src/copybook/common/TRNREC.cpy`

Defines `TRANSACTION-RECORD`:
- **TRN-KEY**: Date + Time + Portfolio ID + Sequence number
- **TRN-DATA**: Investment ID, Type (BU=Buy, SL=Sell, TR=Transfer, FE=Fee), Quantity, Price, Amount (all COMP-3), Currency, Status (P=Pending, D=Done, F=Failed, R=Reversed)
- **TRN-AUDIT**: Process date and user
- **TRN-FILLER**: 50 bytes reserved

#### RETHND — Return Code Handling Definitions
**Path**: `src/copybook/common/RETHND.cpy`

Comprehensive return code management:
- **RETURN-STATUS**: Return code (with level-88 names), reason code, module ID, function ID
- **RETURN-DETAILS**: Error location (program, paragraph, routine), error info (type: V/P/D/F/S, code, text), system info
- **RETURN-ACTIONS**: Action flag (C=Continue, A=Abort, R=Retry), retry count, max retries (default 3)
- **STD-ERROR-CODES**: E001 (Invalid Data) through E010 (Timeout)

#### RTNCODE — Return Code Management
**Path**: `src/copybook/common/RTNCODE.cpy`

Operational return code tracking:
- **RC-REQUEST-TYPE**: Initialize(I), Set(S), Get(G), Log(L), Analyze(A)
- **RC-CODES-AREA**: Current code, highest code, new code, status (S/W/E/F)
- **RC-ANALYSIS-DATA**: Start/end times, total codes, min/max codes
- **RC-RETURN-DATA**: Return value, highest return, return status

---

### 6.3 Batch Copybooks

#### BCHCON — Batch Control Constants
**Path**: `src/copybook/batch/BCHCON.cpy`

System-wide batch processing constants:
- **Process statuses**: Ready(R), Active(A), Waiting(W), Done(D), Error(E)
- **Return code thresholds**: 0/4/8/12/16
- **Process control values**: Max prerequisites (10), max restarts (3), wait interval (300s), max wait time (3600s)
- **Process types**: Initial(INI), Update(UPD), Report(RPT), Cleanup(CLN)
- **Dependency types**: Required(R), Optional(O), Exclusive(X)
- **Special process names**: STARTDAY, ENDDAY, EMERGENCY

#### BCHCTL — Batch Control File Record
**Path**: `src/copybook/batch/BCHCTL.cpy`

Job-level control and sequencing:
- **BCT-KEY**: Job name + Process date + Sequence number
- **BCT-DATA**: Status, step/program names, start/end times
- **BCT-DEPENDENCIES**: Up to 10 prerequisite jobs with names, sequences, and required return codes
- **BCT-STATISTICS**: Restart count, attempt timestamp, completion timestamp

#### CKPRST — Checkpoint/Restart Control Structure
**Path**: `src/copybook/batch/CKPRST.cpy`

Program-level checkpoint/restart:
- **CK-HEADER**: Program ID, run date/time, status (I=Initial, A=Active, C=Complete, F=Failed, R=Restarted)
- **CK-COUNTERS**: Records read, processed, errored; restart count
- **CK-POSITION**: Last key processed, last timestamp, processing phase (00-40)
- **CK-RESOURCES**: File status for up to 5 files (name, position, status)
- **CK-CONTROL-INFO**: Commit frequency (default 1000), max errors (100), max restarts (3), restart mode (N/R/C)
- **CHECKPOINT-RECORD**: VSAM record layout (key = program ID + run date, 400-byte data)

#### PRCSEQ — Process Sequence Definitions
**Path**: `src/copybook/batch/PRCSEQ.cpy`

Process orchestration:
- **PSR-KEY**: Process ID + Version
- **PSR-DATA**: Description, type (INI/PRC/RPT/TRM), timing (daily/weekly/monthly), dependencies (up to 10, hard/soft), control (program, parm, max RC, restartable flag)
- **PSR-SCHEDULE**: Active days (weekday/weekend/all), month-end flag, holiday run flag
- **PSR-RECOVERY**: Recovery program, parm, error limit
- **STANDARD-SEQUENCES**: Predefined sequences:
  - Start-of-day: INITDAY → CKPCLR → DATEVAL
  - Main process: TRNVAL00 → POSUPD00 → HISTLD00
  - End-of-day: RPTGEN00 → BCKLOD00 → ENDDAY

---

### 6.4 Online Copybooks

#### DB2REQ — DB2 Request Area
**Path**: `src/copybook/online/DB2REQ.cpy`

Online DB2 request interface:
- **DB2-REQUEST-TYPE**: Connect(C), Disconnect(D), Status(S)
- **DB2-RESPONSE-CODE**: Response code (S9(8) COMP)
- **DB2-CONNECTION-TOKEN**: 16-byte connection token
- **DB2-ERROR-INFO**: SQLCODE + 80-byte error message

#### ERRHND — Online Error Handling
**Path**: `src/copybook/online/ERRHND.cpy`

CICS-aware error handling:
- **ERR-PROGRAM / ERR-PARAGRAPH**: Error location
- **ERR-SQLCODE**: DB2 SQLCODE
- **ERR-CICS-RESP / ERR-CICS-RESP2**: CICS response codes
- **ERR-SEVERITY**: Fatal(F), Warning(W), Info(I)
- **ERR-ACTION**: Return(R), Continue(C), Abend(A)
- **ERR-TRACE**: Trace ID + timestamp for CICS debugging

#### INQCOM — Online Inquiry Communication Area
**Path**: `src/copybook/online/INQCOM.cpy`

CICS program communication area (COMMAREA):
- **INQCOM-FUNCTION**: MENU, INQP (portfolio inquiry), INQH (history inquiry), EXIT
- **INQCOM-ACCOUNT-NO**: Account number (X(10))
- **INQCOM-RESPONSE-CODE**: Response code (S9(8) COMP)
- **INQCOM-ERROR-MSG**: 80-byte error message
