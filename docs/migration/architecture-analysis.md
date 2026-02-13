# Architecture Analysis: COBOL Legacy Benchmark Suite

Version: 1.0
Date: 2026-02-13

## 1. System Overview

The COBOL Legacy Benchmark Suite (CLBS) is an Investment Portfolio Management System implemented in Enterprise COBOL for z/OS mainframes. It consists of five processing layers: batch processing, online transaction processing (CICS), reporting, utilities, and testing components.

The system manages investment portfolios through sequential batch processing of transactions, real-time inquiries via CICS terminals, automated reporting, and system maintenance utilities.

## 2. Program Inventory

### 2.1 Batch Programs (`src/programs/batch/`)

| Program    | Purpose                                      | Language       |
|------------|----------------------------------------------|----------------|
| BCHCTL00   | Batch control processor; manages job sequencing, checkpoint/restart, and process dependencies | Enterprise COBOL |
| PRCSEQ00   | Process sequencer; reads process control file and determines execution order | Enterprise COBOL |
| TRNVAL00   | Transaction validation; validates incoming financial transactions before processing | Enterprise COBOL |
| POSUPD00 (POSUPDT) | Position update; modifies portfolio position records based on validated transactions | Enterprise COBOL |
| HISTLD00   | History loader; transfers VSAM transaction data to DB2 POSHIST table for reporting | Enterprise COBOL |
| RPTPOS00   | Position report generator; creates daily portfolio valuation and summary reports | Enterprise COBOL |
| RPTAUD00   | Audit report generator; produces security and process audit trail reports | Enterprise COBOL |
| RPTSTA00   | Statistics report generator; monitors system performance and generates trend analysis | Enterprise COBOL |
| RTNANA00   | Return code analyzer; analyzes return codes from batch processing steps | Enterprise COBOL |
| RCVPRC00   | Recovery processor; handles batch recovery operations | Enterprise COBOL |
| RTNCDE00   | Return code handler; manages return code logging to DB2 RTNCODES table | Enterprise COBOL |
| CKPRST     | Checkpoint/restart handler; manages program-level checkpointing during execution | Enterprise COBOL |

### 2.2 Online Programs (`src/programs/online/`)

| Program  | Purpose                                                        |
|----------|----------------------------------------------------------------|
| INQONLN  | Main online controller; manages CICS screen flow, user sessions, and program dispatch |
| INQPORT  | Portfolio position inquiry; retrieves and displays portfolio position data from VSAM |
| INQHIST  | Transaction history inquiry; retrieves historical data from DB2 POSHIST table |
| SECMGR   | Security manager; validates user access, manages authorization, and logs security events |
| CURSMGR  | Cursor manager; handles BMS cursor positioning, screen navigation, and PF key processing |
| ERRHNDL  | Error handler; processes online errors, formats messages, and manages recovery |
| DB2ONLN  | Online DB2 controller; manages connection pooling and optimizes DB2 access for CICS |
| DB2RECV  | DB2 recovery manager; handles connection failures, rollbacks, and session cleanup |

### 2.3 Common/Support Programs (`src/programs/common/`)

| Program  | Purpose                                                 |
|----------|---------------------------------------------------------|
| DB2CONN  | DB2 connection manager; establishes and manages DB2 connections |
| DB2CMT   | DB2 commit handler; manages transaction commit points |
| DB2ERR   | DB2 error handler; processes SQL errors, deadlock resolution, and rollback management |
| DB2STAT  | DB2 statistics collector; gathers and reports DB2 performance metrics |
| ERRPROC  | Batch error processor; standard error handling, message formatting, and return code management |
| AUDPROC  | Audit processor; manages audit trail logging |

### 2.4 Portfolio Programs (`src/programs/portfolio/`)

| Program   | Purpose                                            |
|-----------|----------------------------------------------------|
| PORTMSTR  | Portfolio master file handler; manages VSAM portfolio master records |
| PORTADD   | Portfolio add; creates new portfolio records |
| PORTUPDT  | Portfolio update; modifies existing portfolio records |
| PORTREAD  | Portfolio read; retrieves portfolio records |
| PORTDEL   | Portfolio delete; removes/closes portfolio records |
| PORTVALD  | Portfolio validation; validates portfolio data integrity |
| PORTTRAN  | Portfolio transaction; processes portfolio-level transactions |
| PORTTEST  | Portfolio test; test harness for portfolio operations |

### 2.5 Utility Programs (`src/programs/utility/`)

| Program   | Purpose                                           |
|-----------|---------------------------------------------------|
| UTLMNT00  | File maintenance; performs archiving, cleanup, VSAM reorganization, and space management |
| UTLMON00  | System monitor; tracks resource utilization, collects performance metrics, generates alerts |
| UTLVAL00  | Data validation; performs cross-reference checks, format validation, and balance reconciliation |

### 2.6 Test Programs (`src/programs/test/`)

| Program   | Purpose                                           |
|-----------|---------------------------------------------------|
| TSTGEN00  | Test data generator; creates synthetic portfolios, transactions, and error scenarios |
| TSTVAL00  | Test validation suite; executes test cases, validates results, measures performance |

## 3. Copybook Catalog (`src/copybook/`)

### 3.1 Common Copybooks (`src/copybook/common/`)

| Copybook  | Purpose                              | Key Fields                                    |
|-----------|--------------------------------------|-----------------------------------------------|
| TRNREC    | Transaction record structure         | TRN-KEY (date+time+portfolio+seq), TRN-TYPE (BU/SL/TR/FE), TRN-QUANTITY, TRN-PRICE, TRN-AMOUNT, TRN-STATUS (P/D/F/R) |
| POSREC    | Position record structure            | POS-KEY (portfolio+date+investment), POS-QUANTITY, POS-COST-BASIS, POS-MARKET-VALUE, POS-STATUS (A/C/P) |
| HISTREC   | History record structure             | HIST-KEY (portfolio+date+time+seq), HIST-RECORD-TYPE (PT/PS/TR), HIST-ACTION-CODE (A/C/D), HIST-BEFORE-IMAGE, HIST-AFTER-IMAGE |
| PORTFLIO  | Portfolio master record layout       | PORT-KEY (id+account), PORT-CLIENT-INFO, PORT-STATUS (A/C/S), PORT-TOTAL-VALUE, PORT-CASH-BALANCE |
| ERRHAND   | Standard error handling definitions  | ERR-CATEGORIES (VS/VL/PR/SY), ERR-RETURN-CODES (0/4/8/12/16), ERR-MESSAGE structure, ERR-VSAM-STATUSES |
| COMMON    | Common field definitions             | Shared data elements across all programs |
| RTNCODE   | Return code definitions              | Standard return code values and descriptions |
| AUDITLOG  | Audit log record layout              | Audit trail record structure |
| PORTVAL   | Portfolio validation rules           | Validation constraints for portfolio data |
| RETHND    | Return handling definitions          | Return code processing structures |

### 3.2 Batch Copybooks (`src/copybook/batch/`)

| Copybook | Purpose                          | Key Fields                                      |
|----------|----------------------------------|-------------------------------------------------|
| BCHCTL   | Batch control record definition  | BCT-KEY (job+date+seq), BCT-STATUS (R/A/W/D/E), BCT-DEPENDENCIES (up to 10 prereqs), BCT-RETURN-INFO |
| CKPRST   | Checkpoint/restart record        | Program-level checkpointing fields |
| PRCSEQ   | Process sequence definition      | Process ordering and dependency rules |
| BCHCON   | Batch constants                  | Batch processing constant values |

### 3.3 Online Copybooks (`src/copybook/online/`)

| Copybook | Purpose                           | Key Fields                                    |
|----------|-----------------------------------|-----------------------------------------------|
| INQCOM   | Inquiry communication area        | INQCOM-FUNCTION (MENU/INQP/INQH/EXIT), INQCOM-ACCOUNT-NO, INQCOM-RESPONSE-CODE, INQCOM-ERROR-MSG |
| DB2REQ   | DB2 request area                  | DB2-REQUEST-TYPE (C/D/S), DB2-CONNECTION-TOKEN, DB2-SQLCODE, DB2-ERROR-MSG |
| ERRHND   | Online error handling             | ERR-PROGRAM, ERR-SQLCODE, ERR-CICS-RESP, ERR-SEVERITY (F/W/I), ERR-ACTION (R/C/A), ERR-TRACE |

### 3.4 DB2 Copybooks (`src/copybook/db2/`)

| Copybook | Purpose                     | Key Fields                                         |
|----------|-----------------------------|---------------------------------------------------|
| SQLCA    | SQL communication area      | SQL-STATUS-CODES (success, not-found, dup-key, deadlock, timeout, connection-error) |
| DBPROC   | DB2 standard procedures     | DB2-ERROR-HANDLING (retry logic, max 3 retries), CONNECT/DISCONNECT procedures, error routine |

## 4. Database Components

### 4.1 DB2 Tables (`src/database/db2/`)

| Table/Object         | Source File          | Purpose                                     |
|----------------------|----------------------|---------------------------------------------|
| PORTFOLIO_MASTER     | db2-definitions.sql  | Master portfolio records with client info, risk level, status |
| INVESTMENT_POSITIONS | db2-definitions.sql  | Portfolio positions with quantity, cost basis, market value |
| TRANSACTION_HISTORY  | db2-definitions.sql  | Complete transaction history with type, price, amount |
| POSHIST              | POSHIST.sql          | Position history with partitioned storage by quarter; detailed transaction audit trail |
| ERRLOG               | ERRLOG.sql           | Error logging with severity levels (1-4), types (S/A/D), cleanup procedure |
| RTNCODES             | RTNCODES.sql         | Return code logging for program analysis |
| PORTPLAN             | PORTPLAN.sql         | DB2 application plan binding for portfolio system |

### 4.2 VSAM Files (`src/database/vsam/`)

| File       | Organization | Record Length | Key                              | Purpose                        |
|------------|-------------|---------------|----------------------------------|--------------------------------|
| PORTMSTR   | KSDS        | 400           | Portfolio ID + Account Type + Branch ID (12 bytes) | Portfolio master records |
| TRANHIST   | KSDS        | 300           | Date + Time + Portfolio + Seq (20 bytes) | Transaction history |
| POSHIST    | KSDS        | 350           | Portfolio + Date + Investment (18 bytes) | Position history |

### 4.3 Views

| View               | Source Table(s)                          | Purpose                          |
|--------------------|------------------------------------------|----------------------------------|
| ACTIVE_PORTFOLIOS  | PORTFOLIO_MASTER                         | Active portfolios with open status |
| CURRENT_POSITIONS  | INVESTMENT_POSITIONS + PORTFOLIO_MASTER  | Current day positions with portfolio names |

## 5. BMS Screen Maps (`src/maps/INQSET.bms`)

| Map    | Purpose                    | Key Fields                                          |
|--------|----------------------------|-----------------------------------------------------|
| MENMAP | Main menu                  | Option selection (1=Portfolio, 2=History, 3=Exit)   |
| POSMAP | Portfolio position inquiry | Account input, Fund ID, Fund Name, Units, Cost Basis, Market Value |
| HISMAP | Transaction history view   | Account input, 10 scrollable history rows (Date, Type, Units, Price, Amount) |
| ERRMAP | Error display              | Error Code, Error Details                           |

## 6. JCL Job Definitions

### 6.1 Batch Jobs (`src/jcl/batch/`)

| JCL File   | Program(s) Executed | Purpose                    |
|------------|---------------------|----------------------------|
| RPTPOS.jcl | RPTPOS00           | Position report generation |
| RPTAUD.jcl | RPTAUD00           | Audit report generation    |
| RPTSTA.jcl | RPTSTA00           | Statistics report generation |

### 6.2 Portfolio Jobs (`src/jcl/portfolio/`)

| JCL File      | Program(s) Executed | Purpose                  |
|---------------|---------------------|--------------------------|
| PORTDEF.jcl   | PORTMSTR           | Portfolio file definition |
| PORTADD.jcl   | PORTADD            | Add new portfolios       |
| PORTUPDT.jcl  | PORTUPDT           | Update portfolios        |
| PORTREAD.jcl  | PORTREAD           | Read portfolio data      |
| PORTDEL.jcl   | PORTDEL            | Delete portfolios        |
| PORTTEST.jcl  | PORTTEST           | Portfolio test execution  |

### 6.3 Utility Jobs (`src/jcl/utility/`)

| JCL File    | Program(s) Executed | Purpose                  |
|-------------|---------------------|--------------------------|
| UTLMNT.jcl  | UTLMNT00           | File maintenance         |
| UTLMON.jcl  | UTLMON00           | System monitoring        |
| UTLVAL.jcl  | UTLVAL00           | Data validation          |

### 6.4 Test Jobs (`src/jcl/test/`)

| JCL File   | Program(s) Executed | Purpose               |
|------------|---------------------|------------------------|
| TSTGEN.jcl | TSTGEN00           | Test data generation   |
| TSTVAL.jcl | TSTVAL00           | Test validation        |

## 7. Program Dependencies

### 7.1 Batch Program Dependencies

```
BCHCTL00
├── ERRPROC (error handling)
├── CKPRST (checkpoint/restart)
├── PRCSEQ00 (process sequencing)
└── Controls: TRNVAL00, POSUPD00, HISTLD00, RPT* programs

TRNVAL00
├── ERRPROC (error handling)
├── Reads: TRANFILE (sequential input)
└── Writes: Validated transactions

POSUPD00 (POSUPDT)
├── DB2CONN (database connection)
├── ERRPROC (error handling)
├── Reads: Validated transactions
├── Updates: Position Master (VSAM)
└── Writes: Transaction History (VSAM)

HISTLD00
├── DB2CONN (database connection)
├── DB2CMT (commit management)
├── DB2STAT (statistics)
├── ERRPROC (error handling)
├── Reads: Transaction History (VSAM)
└── Writes: POSHIST (DB2)

RPTPOS00
├── DB2CONN (database connection)
├── ERRPROC (error handling)
├── Reads: Position Master (VSAM), Transaction History
└── Writes: Position Reports

RPTAUD00
├── DB2CONN (database connection)
├── ERRPROC (error handling)
├── Reads: Audit Log, Transaction History
└── Writes: Audit Reports

RPTSTA00
├── DB2CONN (database connection)
├── ERRPROC (error handling)
├── Reads: System Statistics (DB2)
└── Writes: Statistics Reports
```

### 7.2 Online Program Dependencies

```
INQONLN (Main Controller)
├── SECMGR (security validation)
├── CURSMGR (cursor/screen management)
├── INQPORT (portfolio inquiry)
│   ├── DB2ONLN (DB2 connection pool)
│   │   ├── DB2RECV (recovery)
│   │   └── ERRHNDL (error handling)
│   └── Reads: VSAM Position Master, DB2
├── INQHIST (history inquiry)
│   ├── DB2ONLN (DB2 connection pool)
│   │   ├── DB2RECV (recovery)
│   │   └── ERRHNDL (error handling)
│   └── Reads: DB2 POSHIST
└── BMS Maps: MENMAP, POSMAP, HISMAP, ERRMAP
```

### 7.3 Common Program Dependencies

```
DB2CONN → DB2CMT, DB2ERR, DB2STAT
DB2ONLN → DB2RECV, ERRHNDL
DB2ERR  → ERRPROC (for logging)
ERRPROC → ERRLOG (DB2 table)
AUDPROC → AUDITLOG records
```

## 8. Data Flow Summary

### 8.1 Batch Processing Pipeline

```
Input Transactions (TRANFILE)
    │
    ▼
TRNVAL00 (Validate)
    │ RC <= 4
    ▼
POSUPD00 (Update Positions)
    │ Updates VSAM Position Master
    │ Writes VSAM Transaction History
    │ RC <= 4
    ▼
HISTLD00 (Load to DB2)
    │ Reads VSAM Transaction History
    │ Writes DB2 POSHIST
    │ RC <= 4
    ▼
Report Generation
    ├── RPTPOS00 (Position Reports)
    ├── RPTAUD00 (Audit Reports)
    └── RPTSTA00 (Statistics Reports)
```

### 8.2 Online Inquiry Flow

```
User Terminal
    │
    ▼
CICS (Transaction PINQ)
    │
    ▼
INQONLN (Main Controller)
    ├── SECMGR → Validate user access
    ├── Option 1: INQPORT → Read VSAM Position Master
    ├── Option 2: INQHIST → Query DB2 POSHIST
    └── Option 3: Exit
```

### 8.3 Batch Scheduling (Time Windows)

| Step     | Time Window | Prerequisite | Condition        |
|----------|-------------|--------------|------------------|
| TRNVAL00 | 1800-1815   | None         | Day must be open |
| POSUPD00 | 1815-1900   | TRNVAL00     | RC <= 0004       |
| HISTLD00 | 1900-1930   | POSUPD00     | RC <= 0004       |
| Reports  | 1930-2000   | HISTLD00     | None             |

## 9. CICS Resource Definitions (`src/cics/PORTDFN.csd`)

- Transaction ID: PINQ (Portfolio Inquiry)
- Programs: INQONLN, INQPORT, INQHIST, SECMGR, CURSMGR, ERRHNDL, DB2ONLN, DB2RECV
- Files: POSFILE (VSAM Position Master)
- Mapset: INQSET (contains MENMAP, POSMAP, HISMAP, ERRMAP)

## 10. Error Handling Architecture

### 10.1 Error Categories

| Category | Code | Description                |
|----------|------|----------------------------|
| VSAM     | VS   | VSAM file operation errors |
| Validation | VL | Data validation errors     |
| Processing | PR | Business logic errors      |
| System   | SY   | System/environment errors  |

### 10.2 Return Codes

| Code | Severity | Description              | Action           |
|------|----------|--------------------------|------------------|
| 0000 | Success  | Successful completion    | Continue         |
| 0004 | Warning  | Warnings, processing OK  | Review warnings  |
| 0008 | Error    | Errors, processing done  | Review errors    |
| 0012 | Severe   | Critical error, abend    | Immediate action |
| 0016 | Terminal | Environment error        | System support   |

### 10.3 Checkpoint Frequency

- Transaction processor (TRNVAL00): Every 1,000 records
- Position update (POSUPD00): Every 500 updates
- History load (HISTLD00): Every 1,000 records
- Minimum checkpoint interval: 2 minutes
