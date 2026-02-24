# Architecture Analysis: Investment Portfolio Management System

## Overview

The Investment Portfolio Management System is an Enterprise COBOL application running on IBM z/OS mainframes. It manages investment portfolios, processes transactions, maintains position records, and provides online inquiry capabilities through CICS terminals. The system uses DB2 for relational data storage, VSAM for indexed file access, JCL for batch job control, and BMS screen maps for terminal-based user interfaces.

---

## 1. System Architecture Layers

The system is organized into five processing layers:

| Layer | Description | Programs |
|-------|-------------|----------|
| **Batch Processing** | Scheduled jobs for data loading, reporting, and maintenance | BCHCTL00, PRCSEQ00, HISTLD00, RPTPOS00, RPTAUD00, RPTSTA00, RTNANA00, RTNCDE00, RCVPRC00, CKPRST |
| **Online Transaction Processing (CICS)** | Real-time inquiry and transaction processing via 3270 terminals | INQONLN, INQPORT, INQHIST, DB2ONLN, DB2RECV |
| **Portfolio Management** | CRUD operations on portfolio and transaction data | PORTMSTR, PORTVALD, PORTTRAN, PORTADD, PORTDEL, PORTREAD, PORTUPDT, PORTTEST |
| **Common/Support Services** | Shared infrastructure for DB2, error handling, and auditing | DB2CONN, DB2CMT, DB2ERR, DB2STAT, ERRPROC, AUDPROC |
| **Utility & Testing** | File maintenance, monitoring, validation, and test data generation | UTLMNT00, UTLMON00, UTLVAL00, TSTGEN00, TSTVAL00 |

---

## 2. Program Catalog

### 2.1 Batch Processing Programs

#### BCHCTL00 — Batch Control Processor
- **Purpose**: Manages batch job initialization, prerequisite checking, status updates, and termination.
- **Functions**: INIT (initialize job), CHEK (check prerequisites), UPDT (update status), TERM (terminate).
- **Copybooks**: BCHCTL, BCHCON, ERRHAND
- **Calls**: ERRPROC
- **Files**: BATCH-CONTROL-FILE (VSAM KSDS, indexed by BCT-KEY)

#### PRCSEQ00 — Process Sequence Manager
- **Purpose**: Manages process sequencing, dependency checking, and status tracking for batch workflows.
- **Functions**: INIT, NEXT (get next process), STAT (check status), TERM.
- **Copybooks**: PRCSEQ, BCHCTL, BCHCON, ERRHAND
- **Calls**: ERRPROC
- **Key Data**: Process table with up to 100 entries; implements dependency checking logic.

#### HISTLD00 — Position History DB2 Load
- **Purpose**: Loads transaction history from VSAM files into the DB2 POSHIST table.
- **Copybooks**: HISTREC, BCHCTL, DBTBLS, SQLCA, DBPROC, ERRHAND, BCHCON
- **Calls**: DB2CONN (connect), DB2CMT (commit), DB2ERR (error handling), DB2STAT (statistics), ERRPROC, AUDPROC
- **DB2 Tables**: POSHIST (INSERT)
- **Files**: TRANSACTION-HISTORY (VSAM input)
- **Key Feature**: Commit checkpoints every 1,000 records for recoverability.

#### RPTPOS00 — Daily Position Report Generator
- **Purpose**: Generates formatted position reports from Position Master and Transaction History files.
- **Copybooks**: POSREC, TRNREC, RTNCODE, ERRHAND
- **Files**: POSITION-MASTER (input), TRANSACTION-HISTORY (input), REPORT-FILE (output)

#### RPTAUD00 — Audit Report Generator
- **Purpose**: Generates audit and error reports from audit and error log files.
- **Copybooks**: AUDITLOG, ERRHAND, RTNCODE
- **Files**: AUDIT-FILE (input), ERROR-FILE (input), REPORT-FILE (output)

#### RPTSTA00 — System Statistics Report Generator
- **Purpose**: Generates performance and system statistics reports.
- **Copybooks**: DB2STAT, BCHCTL, RTNCODE, ERRHAND
- **Files**: DB2-STATS (input), BATCH-STATS (input), REPORT-FILE (output)
- **Key Feature**: Calculates performance metrics from collected statistics.

#### RTNANA00 — Return Code Analysis Utility
- **Purpose**: Analyzes return codes across the system for trend analysis and error pattern reporting.
- **DB2 Tables**: RTNCODES (SELECT with cursor operations)
- **Key Feature**: Generates trend analysis and error pattern reports using DB2 embedded SQL.

#### RTNCDE00 — Standard Return Code Handler
- **Purpose**: Manages standardized return codes; logs and analyzes return code patterns.
- **Functions**: INIT, SET-CODE, GET-CODE, LOG-CODE, ANALYZE.
- **DB2 Tables**: RTNCODES (INSERT, SELECT)

#### RCVPRC00 — Process Recovery Handler
- **Purpose**: Handles process recovery and restart logic after failures.
- **Functions**: INIT, RECV (recovery), TERM.
- **Recovery Modes**: Process (P), Sequence (S), All (A).
- **Recovery Actions**: Restart (R), Bypass (B), Terminate (T).
- **Copybooks**: BCHCTL, PRCSEQ, BCHCON, ERRHAND

#### CKPRST — Checkpoint Handler
- **Purpose**: Manages checkpoint/restart operations for long-running batch jobs.
- **Functions**: INIT, TAKE-CHECKPOINT, COMMIT-CHECKPOINT, RESTART.
- **Copybooks**: CKPRST, RETHND

### 2.2 Online Transaction Processing Programs (CICS)

#### INQONLN — Portfolio Online Inquiry Main Handler
- **Purpose**: Main CICS entry point for online inquiry; displays menu and routes requests.
- **Functions**: MENU (display menu), INQP (portfolio inquiry), INQH (history inquiry), EXIT.
- **Copybooks**: INQCOM, ERRHND
- **Calls**: INQPORT, INQHIST, SECMGR, ERRHNDL
- **CICS Commands**: RECEIVE MAP, SEND MAP, LINK PROGRAM
- **BMS Maps**: MENMAP (main menu from INQSET mapset)

#### INQPORT — Portfolio Position Inquiry Handler
- **Purpose**: Retrieves and displays current portfolio positions from VSAM files.
- **Copybooks**: INQCOM, POSREC, SQLPOS
- **Files**: POSFILE (VSAM KSDS, CICS READ)
- **BMS Maps**: POSMAP (position display from INQSET mapset)
- **Key Feature**: Reads position data by account number and formats for 3270 display.

#### INQHIST — Transaction History Inquiry Handler
- **Purpose**: Retrieves and displays transaction history from DB2.
- **Copybooks**: INQCOM
- **Calls**: DB2ONLN (connect), CURSMGR (cursor management), DB2RECV (recovery)
- **DB2 Tables**: POSHIST (SELECT with cursor)
- **BMS Maps**: HISMAP (history display from INQSET mapset)
- **Key Feature**: Supports scrolling through history rows; fetches up to 10 rows at a time.

#### SECMGR — Security Manager
- **Purpose**: Validates CICS user credentials, manages DB2 authorization, and maintains security audit trail.
- **Functions**: Validate (V), Authorize (A), Audit/Log (L).
- **Copybooks**: ERRHND
- **DB2 Tables**: AUTHFILE (SELECT for authorization check), AUDITLOG (INSERT for audit logging)
- **CICS Commands**: ASSIGN USERID, ASSIGN TERMID

#### CURSMGR — Cursor Manager
- **Purpose**: Manages DB2 cursor lifecycle (declare, open, fetch, close) with array fetch optimization.
- **Functions**: Declare (D), Open (O), Fetch (F), Close (C).
- **Key Feature**: Supports array fetching (up to 20 rows) for performance optimization.

#### ERRHNDL — Centralized Error Handler
- **Purpose**: Processes all online errors; logs errors to DB2, formats messages, and determines recovery action.
- **Copybooks**: ERRHND
- **DB2 Tables**: ERRLOG (INSERT)
- **Key Feature**: Determines action based on severity: Fatal → Abend, Warning/Info → Continue.

#### DB2ONLN — Online DB2 Connection Manager
- **Purpose**: Manages DB2 connection pool for online programs.
- **Functions**: Connect (C), Disconnect (D), Status (S).
- **Copybooks**: ERRHND
- **Key Feature**: Connection pool with max 100 connections; generates connection tokens.

#### DB2RECV — DB2 Recovery Manager
- **Purpose**: Handles DB2 connection failures with retry logic, transaction rollback, and cursor recovery.
- **Functions**: Connection recovery (C), Transaction recovery (T), Cursor recovery (R).
- **Copybooks**: ERRHND, DB2REQ
- **Calls**: DB2ONLN (reconnect), ERRHNDL (error logging)
- **Key Feature**: Retry up to 3 times with 2-second intervals between attempts.

### 2.3 Portfolio Management Programs

#### PORTMSTR — Portfolio Master File Maintenance
- **Purpose**: CRUD operations for Portfolio records on VSAM KSDS file.
- **Functions**: Create (C), Read (R), Update (U), Delete (D).
- **Files**: PORTFOLIO-FILE (VSAM KSDS, indexed by PORT-ID)
- **Calls**: ERRPROC, AUDPROC
- **Key Feature**: Validates portfolio ID format (must start with 'PORT' + numeric digits), name, and status.

#### PORTVALD — Portfolio Validation Subroutine
- **Purpose**: Validates portfolio data elements (ID, account, type, amount).
- **Functions**: Validate ID (I), Account (A), Type (T), Amount (M).
- **Copybooks**: PORTVAL
- **Key Validations**:
  - Portfolio ID: Must start with 'PORT' + 4 numeric digits
  - Account: Must be 10 numeric digits, non-zero
  - Investment Type: Must be STK, BND, MMF, or ETF
  - Amount: Must be within range (-9999999999999.99 to +9999999999999.99)

#### PORTTRAN — Portfolio Transaction Processing
- **Purpose**: Processes portfolio transactions (buy, sell, transfer, fee) against portfolio files.
- **Copybooks**: ERRHAND, AUDITLOG, TRNREC
- **Files**: TRANSACTION-FILE (sequential input), PORTFOLIO-FILE (VSAM KSDS I-O)
- **Calls**: ERRPROC, AUDPROC
- **Key Feature**: Validates transactions, updates portfolio balances, maintains audit trail. Stops after 100 errors.

#### PORTADD, PORTDEL, PORTREAD, PORTUPDT — Portfolio CRUD Helpers
- **Purpose**: Individual programs for adding, deleting, reading, and updating portfolio records.

#### PORTTEST — Portfolio Test Program
- **Purpose**: Test harness for portfolio management functionality.

### 2.4 Common/Support Programs

#### DB2CONN — DB2 Connection Manager (Batch)
- **Purpose**: Manages DB2 connections for batch programs with retry logic.
- **Functions**: CONN (connect), DISC (disconnect), STAT (check status).
- **Copybooks**: SQLCA, DBPROC, ERRHAND
- **Calls**: ERRPROC
- **Key Feature**: Retries up to 3 times; handles specific SQL codes (-30081 max connections, -99999 network error).

#### DB2CMT — DB2 Commit Controller
- **Purpose**: Controls commit/rollback operations with savepoint support.
- **Functions**: INIT, CMIT (commit), RBAK (rollback), SAVE (savepoint), REST (restore), STAT (statistics).
- **Copybooks**: SQLCA, DBPROC, ERRHAND
- **Calls**: DB2ERR, ERRPROC
- **Key Feature**: Frequency-based commits; tracks commit/rollback/savepoint statistics.

#### DB2ERR — DB2 SQL Error Handler
- **Purpose**: Logs, diagnoses, and retrieves DB2 errors with severity classification.
- **Functions**: LOG, DIAG (diagnose), RETR (retrieve last error).
- **Copybooks**: DBTBLS, SQLCA, DBPROC, ERRHAND
- **DB2 Tables**: ERRLOG (INSERT, SELECT)
- **Key Feature**: Classifies errors by SQL code (deadlock → retry, timeout → retry, connection error → severe, duplicate key → no retry).

#### DB2STAT — DB2 Statistics Collector
- **Purpose**: Collects and reports DB2 performance statistics per program.
- **Functions**: INIT, UPDT (update), TERM (terminate), DISP (display).
- **Copybooks**: SQLCA, DBPROC, ERRHAND
- **DB2 Tables**: SESSION.DBSTATS (temporary table for statistics)
- **Calls**: ERRPROC
- **Key Feature**: Tracks rows read/inserted/updated/deleted, commits, rollbacks, CPU time, and elapsed time.

#### ERRPROC — Standard Error Processing Subroutine
- **Purpose**: Central error processing; logs errors to sequential file and displays formatted error messages.
- **Copybooks**: ERRHAND
- **Files**: ERROR-LOG (sequential, EXTEND mode)

#### AUDPROC — Audit Trail Processing Subroutine
- **Purpose**: Writes audit records to sequential audit file.
- **Copybooks**: AUDITLOG
- **Files**: AUDIT-FILE (sequential, EXTEND mode)

### 2.5 Utility Programs

#### UTLMNT00 — File Maintenance Utility
- **Purpose**: Performs maintenance on system files: archiving, cleanup, VSAM reorganization, space analysis.
- **Functions**: ARCHIVE, CLEANUP, REORG, ANALYZE.
- **Copybooks**: RTNCODE, ERRHAND
- **Files**: CONTROL-FILE (input), ARCHIVE-FILE (output), REPORT-FILE (output)

#### UTLMON00 — System Monitoring Utility
- **Purpose**: Monitors system health: CPU, memory, DASD, DB2 metrics; generates alerts on threshold violations.
- **Resource Types**: CPU, MEMORY, DASD, DB2.
- **Threshold Types**: Utilization, Response time, Queue depth, Error rate.
- **Alert Levels**: INFO, WARNING, CRITICAL.
- **Copybooks**: DB2STAT, RTNCODE, ERRHAND
- **Files**: MONITOR-CONFIG (input), MONITOR-LOG (output), ALERT-FILE (output), DB2-STATS (VSAM input)

#### UTLVAL00 — Data Validation Utility
- **Purpose**: Comprehensive data validation: integrity checks, cross-reference validation, format verification, balance reconciliation.
- **Validation Types**: INTEGRITY, XREF, FORMAT, BALANCE.
- **Copybooks**: POSREC, TRNREC, RTNCODE, ERRHAND
- **Files**: VALIDATION-CONTROL (input), POSITION-MASTER (VSAM input), TRANSACTION-HISTORY (VSAM input), ERROR-REPORT (output)

### 2.6 Test Programs

#### TSTGEN00 — Test Data Generator
- **Purpose**: Generates test data for system testing: portfolio data, transaction scenarios, error conditions, performance volumes.
- **Test Types**: PORTFOLIO, TRANSACTN, ERROR, VOLUME.
- **Copybooks**: PORTFLIO, TRNREC, RTNCODE, ERRHAND
- **Files**: TEST-CONFIG (input), PORTFOLIO-OUT (output), TRANSACTION-OUT (output), RANDOM-SEED (input)

#### TSTVAL00 — Test Validation Suite
- **Purpose**: Validates test results: functional tests, integration tests, performance benchmarks, error condition tests.
- **Test Types**: FUNCTIONAL, INTEGRATE, PERFORM, ERROR.
- **Copybooks**: RTNCODE, ERRHAND
- **Files**: TEST-CASES (input), EXPECTED-RESULTS (input), ACTUAL-RESULTS (input), TEST-REPORT (output)

---

## 3. Copybook Catalog

| Copybook | Directory | Purpose | Key Fields |
|----------|-----------|---------|------------|
| **TRNREC** | common | Transaction record structure | TRN-KEY (date+time+portfolio+seq), TRN-TYPE (BU/SL/TR/FE), TRN-QUANTITY, TRN-PRICE, TRN-AMOUNT, TRN-STATUS (P/D/F/R) |
| **POSREC** | common | Position record structure | POS-KEY (portfolio+date+investment), POS-QUANTITY, POS-COST-BASIS, POS-MARKET-VALUE, POS-STATUS (A/C/P) |
| **HISTREC** | common | History record structure | HIST-KEY (portfolio+date+time+seq), HIST-RECORD-TYPE (PT/PS/TR), HIST-ACTION-CODE (A/C/D), HIST-BEFORE/AFTER-IMAGE |
| **PORTFLIO** | common | Portfolio master record | PORT-KEY (id+account), PORT-CLIENT-INFO, PORT-STATUS (A/C/S), PORT-TOTAL-VALUE, PORT-CASH-BALANCE |
| **AUDITLOG** | common | Audit trail record | AUD-HEADER (timestamp+system+user+program+terminal), AUD-TYPE (TRAN/USER/SYST), AUD-ACTION, AUD-STATUS |
| **ERRHAND** | common | Error handling definitions | ERR-CATEGORIES (VS/VL/PR/SY), ERR-RETURN-CODES (0/4/8/12/16), ERR-MESSAGE structure, VSAM status handling |
| **COMMON** | common | Common definitions/constants | RETURN-CODES, STATUS-CODES, TRANSACTION-TYPES, DATETIME fields, CURRENCY-CODES (USD/EUR/GBP/JPY/CAD) |
| **RTNCODE** | common | Return code management | RC-REQUEST-TYPE (I/S/G/L/A), RC-CODES-AREA, RC-ANALYSIS-DATA |
| **RETHND** | common | Return code handling | RETURN-STATUS, RETURN-DETAILS (error location/info), RETURN-ACTIONS (continue/abort/retry), STD-ERROR-CODES (E001-E010) |
| **PORTVAL** | common | Portfolio validation rules | VAL-RETURN-CODES, VAL-ERROR-MESSAGES, VAL-CONSTANTS (min/max amounts, ID prefix) |
| **INQCOM** | online | Online inquiry communication area | INQCOM-FUNCTION (MENU/INQP/INQH/EXIT), INQCOM-ACCOUNT-NO, INQCOM-RESPONSE-CODE, INQCOM-ERROR-MSG |
| **DB2REQ** | online | DB2 request area | DB2-REQUEST-TYPE (C/D/S), DB2-RESPONSE-CODE, DB2-CONNECTION-TOKEN, DB2-ERROR-INFO |
| **ERRHND** | online | Online error handling | ERR-PROGRAM, ERR-PARAGRAPH, ERR-SQLCODE, ERR-CICS-RESP, ERR-SEVERITY (F/W/I), ERR-ACTION (R/C/A), ERR-TRACE |
| **DBTBLS** | db2 | DB2 table definitions (host variables) | POSHIST-RECORD (18 fields), ERRLOG-RECORD (10 fields) |
| **DBPROC** | db2 | DB2 standard procedures | DB2-ERROR-HANDLING, CONNECT-TO-DB2, DISCONNECT-FROM-DB2, DB2-ERROR-ROUTINE, CHECK-SQL-STATUS |
| **SQLCA** | db2 | SQL Communication Area | SQL status codes: SUCCESS(00000), NOT-FOUND(02000), DUP-KEY(23505), DEADLOCK(40001), TIMEOUT(40003) |

---

## 4. Database Objects

### 4.1 DB2 Tables

| Table | Purpose | Primary Key | Key Columns |
|-------|---------|-------------|-------------|
| **POSHIST** | Position/Transaction history | ACCOUNT_NO + PORTFOLIO_ID + TRANS_DATE + TRANS_TIME | SECURITY_ID, QUANTITY, PRICE, AMOUNT, FEES, COST_BASIS, GAIN_LOSS |
| **ERRLOG** | Application error log | ERROR_TIMESTAMP + PROGRAM_ID | ERROR_TYPE (S/A/D), ERROR_SEVERITY (1-4), ERROR_CODE, ERROR_MESSAGE |
| **PORTFOLIO_MASTER** | Portfolio master records | PORTFOLIO_ID | CLIENT_ID, PORTFOLIO_NAME, CURRENCY_CODE, RISK_LEVEL, STATUS |
| **INVESTMENT_POSITIONS** | Current investment positions | PORTFOLIO_ID + INVESTMENT_ID + POSITION_DATE | QUANTITY, COST_BASIS, MARKET_VALUE |
| **TRANSACTION_HISTORY** | Full transaction log | TRANSACTION_ID | PORTFOLIO_ID, INVESTMENT_ID, TRANSACTION_TYPE, QUANTITY, PRICE, AMOUNT |
| **RTNCODES** | Return code logging | TIMESTAMP + PROGRAM_ID | RETURN_CODE, HIGHEST_CODE, STATUS_CODE, MESSAGE_TEXT |
| **AUTHFILE** | User authorization | (referenced in SECMGR) | USER_ID, RESOURCE, ACCESS_TYPE |
| **AUDITLOG** | Security audit trail | (referenced in SECMGR) | TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE |

### 4.2 DB2 Views

| View | Purpose |
|------|---------|
| **ACTIVE_PORTFOLIOS** | Active portfolios with no close date or close date in the future |
| **CURRENT_POSITIONS** | Current positions joined with portfolio name and client ID |

### 4.3 VSAM Files

| File | Organization | Record Length | Key Length | Key Structure |
|------|-------------|--------------|-----------|---------------|
| **PORTMSTR** | KSDS | 400 | 12 | Portfolio ID (8) + Account Type (2) + Branch ID (2) |
| **TRANHIST** | KSDS | 300 | 20 | Trans Date (8) + Trans Time (6) + Portfolio ID (8) + Seq No (6) |
| **POSHIST** | KSDS | 350 | 18 | Portfolio ID (8) + Position Date (8) + Investment ID (10) |
| **BATCH-CONTROL-FILE** | KSDS | varies | BCT-KEY | Batch job control records |

---

## 5. Program Dependency Map

### 5.1 Call Graph

```
INQONLN (CICS Main Handler)
├── SECMGR (Security Manager)
│   └── DB2 [AUTHFILE, AUDITLOG]
├── INQPORT (Position Inquiry)
│   └── VSAM [POSFILE]
├── INQHIST (History Inquiry)
│   ├── DB2ONLN (Online DB2 Connection)
│   ├── CURSMGR (Cursor Manager)
│   │   └── DB2 [POSHIST]
│   └── DB2RECV (Recovery Manager)
│       ├── DB2ONLN
│       └── ERRHNDL
└── ERRHNDL (Error Handler)
    └── DB2 [ERRLOG]

HISTLD00 (Batch History Loader)
├── DB2CONN (Batch DB2 Connection)
│   └── ERRPROC
├── DB2CMT (Commit Controller)
│   ├── DB2ERR
│   │   └── ERRPROC
│   └── ERRPROC
├── DB2ERR (SQL Error Handler)
│   └── ERRPROC
├── DB2STAT (Statistics Collector)
│   └── ERRPROC
├── ERRPROC (Error Processing)
└── AUDPROC (Audit Processing)

PORTTRAN (Transaction Processing)
├── ERRPROC
└── AUDPROC

PORTMSTR (Portfolio Master Maintenance)
├── PORTVALD (Validation)
├── ERRPROC
└── AUDPROC

BCHCTL00 (Batch Control)
└── ERRPROC

PRCSEQ00 (Process Sequencing)
└── ERRPROC

RCVPRC00 (Process Recovery)
└── ERRPROC
```

### 5.2 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        BATCH PROCESSING LAYER                          │
│                                                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐         │
│  │ BCHCTL00 │───>│ PRCSEQ00 │───>│ HISTLD00 │───>│ CKPRST   │         │
│  │(Control) │    │(Sequence)│    │(DB2 Load)│    │(Chkpoint)│         │
│  └──────────┘    └──────────┘    └────┬─────┘    └──────────┘         │
│                                       │                                 │
│  ┌──────────┐    ┌──────────┐    ┌────▼─────┐                         │
│  │ RPTPOS00 │    │ RPTAUD00 │    │ RPTSTA00 │                         │
│  │(Pos Rpt) │    │(Aud Rpt) │    │(Stat Rpt)│                         │
│  └────┬─────┘    └────┬─────┘    └────┬─────┘                         │
│       │               │               │                                 │
│       └───────────────┴───────────────┘                                 │
│                       │                                                  │
│               ┌───────▼───────┐                                         │
│               │  REPORT FILES │                                         │
│               └───────────────┘                                         │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     ONLINE PROCESSING LAYER (CICS)                     │
│                                                                         │
│              ┌──────────┐                                               │
│  3270 ──────>│ INQONLN  │                                               │
│  Terminal    │  (Menu)  │                                               │
│              └────┬─────┘                                               │
│         ┌─────────┼─────────┐                                          │
│    ┌────▼───┐ ┌───▼────┐ ┌──▼───┐                                     │
│    │INQPORT │ │INQHIST │ │SECMGR│                                     │
│    │(VSAM)  │ │ (DB2)  │ │(Auth)│                                     │
│    └────┬───┘ └───┬────┘ └──┬───┘                                     │
│         │         │         │                                           │
│    ┌────▼───┐ ┌───▼────┐ ┌──▼───────┐                                 │
│    │POSFILE │ │POSHIST │ │AUTHFILE  │                                  │
│    │(VSAM)  │ │ (DB2)  │ │AUDITLOG  │                                  │
│    └────────┘ └────────┘ └──────────┘                                  │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     PORTFOLIO MANAGEMENT LAYER                          │
│                                                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                          │
│  │ PORTMSTR │<──>│ PORTVALD │    │ PORTTRAN │                          │
│  │  (CRUD)  │    │  (Valid) │    │  (Trans) │                          │
│  └────┬─────┘    └──────────┘    └────┬─────┘                          │
│       │                               │                                 │
│  ┌────▼───────────────────────────────▼────┐                           │
│  │        PORTFOLIO-FILE (VSAM KSDS)        │                           │
│  └──────────────────────────────────────────┘                           │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     COMMON SERVICES LAYER                               │
│                                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ DB2CONN  │  │  DB2CMT  │  │  DB2ERR  │  │ DB2STAT  │               │
│  │(Connect) │  │ (Commit) │  │ (Error)  │  │ (Stats)  │               │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘               │
│                                                                         │
│  ┌──────────┐  ┌──────────┐                                            │
│  │ ERRPROC  │  │ AUDPROC  │                                            │
│  │(Err Log) │  │(Aud Log) │                                            │
│  └──────────┘  └──────────┘                                            │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 6. BMS Screen Maps (INQSET.bms)

| Map Name | Purpose | Key Fields |
|----------|---------|------------|
| **MENMAP** | Main menu — option selection | OPTION (1-3), ERRMSG |
| **POSMAP** | Portfolio position display | ACCTIN, FUNDOUT, NAMEOUT, UNITOUT, COSTOUT, VALOUT, POSMSG |
| **HISMAP** | Transaction history display | HISAIN, ROW1-ROW10 (10 data rows), HISMSG |
| **ERRMAP** | Error display | ERRCOUT (error code), ERRDOUT (error details) |

All maps are 24x80 terminal format with PF3=Exit, PF7=Previous, PF8=Next navigation.

---

## 7. JCL Batch Jobs

| JCL File | Purpose | Programs Executed |
|----------|---------|-------------------|
| **RPTPOS.jcl** | Daily position report generation | RPTPOS00 |
| **RPTAUD.jcl** | Audit report generation | RPTAUD00 |
| **RPTSTA.jcl** | System statistics report generation | RPTSTA00 |

---

## 8. Key Data Type Mappings (COBOL to Python Reference)

| COBOL Type | Example | Python Equivalent | Notes |
|-----------|---------|-------------------|-------|
| PIC X(n) | PIC X(8) | str (max length n) | Fixed-length character |
| PIC 9(n) | PIC 9(4) | int | Unsigned integer |
| PIC S9(n) COMP | PIC S9(4) COMP | int | Signed binary integer |
| PIC S9(n)V9(m) COMP-3 | PIC S9(11)V9(4) COMP-3 | Decimal | Packed decimal |
| PIC S9(n)V99 | PIC S9(13)V99 | Decimal | Display numeric with 2 decimal places |
| 88-level | 88 TRN-TYPE-BUY VALUE 'BU' | Enum or constant | Condition name (boolean flag) |

---

## 9. Assumptions and Observations

1. **AUTHFILE and AUDITLOG tables**: Referenced in SECMGR program SQL but no separate DDL files found. Schema inferred from program usage.
2. **Some programs reference paragraph names** (e.g., 2210-OPEN-VSAM) that are not fully implemented in the source — these are stubs for maintenance operations.
3. **The PORTPLAN.sql** file contains a DB2 BIND PLAN statement specific to z/OS — this has no direct equivalent in PostgreSQL.
4. **VSAM file definitions** are documented in `vsam-definitions.txt` rather than as IDCAMS control statements, but IDCAMS DEFINE CLUSTER examples are included for PORTMSTR.
5. **The system uses both batch and online DB2 connection managers** (DB2CONN for batch, DB2ONLN for CICS) — these will be unified in the Python implementation using SQLAlchemy connection pooling.
