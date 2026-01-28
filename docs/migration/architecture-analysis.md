# COBOL Legacy Benchmark Suite - Architecture Analysis

## Executive Summary

This document provides a comprehensive analysis of the Investment Portfolio Management System implemented in Enterprise COBOL for z/OS mainframes. The system consists of five processing layers: batch processing, online transaction processing (CICS), reporting, utilities, and testing components. This analysis maps all COBOL programs, their dependencies, and data flows to support the migration to Python.

## System Overview

The COBOL Legacy Benchmark Suite (CLBS) is a production-grade Investment Portfolio Management System designed to:
- Manage portfolios and transaction histories
- Process financial transactions and update positions
- Generate reports on positions, audits, and system statistics
- Support online inquiries for portfolio positions and transaction histories

## Program Inventory

### 1. Batch Processing Programs

Located in `src/programs/batch/`

| Program | Description | Dependencies | Data Access |
|---------|-------------|--------------|-------------|
| BCHCTL00 | Batch Control Processor - manages process dependencies and checkpoint/restart | ERRPROC, BCHCTL copybook, BCHCON copybook | Batch Control File (VSAM) |
| PRCSEQ00 | Process Sequence Manager - controls batch job sequences | ERRPROC, BCHCTL copybook, PRCSEQ copybook | Process Sequence File, Batch Control File |
| HISTLD00 | Position History DB2 Load - transfers VSAM data to DB2 | DB2CONN, ERRPROC, DBTBLS copybook | Transaction History (VSAM), POSHIST (DB2) |
| RPTPOS00 | Daily Position Report Generator | ERRPROC | Position Master (VSAM), Transaction History (VSAM) |
| RPTAUD00 | Audit Report Generator | ERRPROC | Audit File, Error File |
| RPTSTA00 | System Statistics Report Generator | ERRPROC | DB2 Stats, Batch Stats |

### 2. Online Programs (CICS)

Located in `src/programs/online/`

| Program | Description | Dependencies | Data Access |
|---------|-------------|--------------|-------------|
| INQONLN | Main Online Controller - orchestrates CICS screen flow | INQPORT, INQHIST, SECMGR, ERRHNDL | INQCOM copybook |
| INQPORT | Portfolio Position Inquiry Handler | ERRHNDL | POSFILE (VSAM), POSREC copybook |
| INQHIST | Transaction History Inquiry Handler | DB2ONLN, CURSMGR, DB2RECV | POSHIST (DB2) |
| SECMGR | Security Manager - user validation and authorization | ERRHNDL | AUTHFILE (DB2), AUDITLOG (DB2) |
| CURSMGR | Cursor Management for Online Programs | None | DB2 cursors |
| DB2ONLN | Online DB2 Connection Manager | ERRHNDL | DB2 connection pool |
| DB2RECV | DB2 Recovery Manager | DB2ONLN, ERRHNDL | DB2 connections |
| ERRHNDL | Centralized Error Handler | None | ERRLOG (DB2) |

### 3. Utility Programs

Located in `src/programs/utility/`

| Program | Description | Dependencies | Data Access |
|---------|-------------|--------------|-------------|
| UTLMNT00 | File Maintenance Utility - archive, cleanup, reorg | ERRPROC | Control File, Archive File, VSAM files |
| UTLMON00 | System Monitoring Utility - resource tracking | ERRPROC | Monitor Config, Monitor Log, Alert File, DB2 Stats |
| UTLVAL00 | Data Validation Utility - integrity checks | ERRPROC | Position Master, Transaction History |

### 4. Test Programs

Located in `src/programs/test/`

| Program | Description | Dependencies | Data Access |
|---------|-------------|--------------|-------------|
| TSTGEN00 | Test Data Generator | ERRPROC | Portfolio Output, Transaction Output |
| TSTVAL00 | Test Validation Suite | ERRPROC | Test Cases, Expected Results, Actual Results |

### 5. Common/Support Programs

Located in `src/programs/common/`

| Program | Description | Dependencies | Data Access |
|---------|-------------|--------------|-------------|
| DB2CONN | DB2 Connection Manager | ERRPROC | DB2 connections |
| DB2CMT | DB2 Commit Controller | DB2ERR, ERRPROC | DB2 transactions |
| DB2ERR | DB2 SQL Error Handler | ERRPROC | ERRLOG (DB2) |
| DB2STAT | DB2 Statistics Collector | None | DB2 stats |
| ERRPROC | Error Processing Routine | None | Error logging |
| AUDPROC | Audit Processing Routine | None | Audit logging |

### 6. Portfolio Programs

Located in `src/programs/portfolio/`

| Program | Description | Dependencies | Data Access |
|---------|-------------|--------------|-------------|
| PORTMSTR | Portfolio Master Handler | Various | Portfolio Master (VSAM) |
| PORTADD | Portfolio Add Handler | PORTMSTR | Portfolio Master (VSAM) |
| PORTUPDT | Portfolio Update Handler | PORTMSTR | Portfolio Master (VSAM) |
| PORTDEL | Portfolio Delete Handler | PORTMSTR | Portfolio Master (VSAM) |
| PORTREAD | Portfolio Read Handler | PORTMSTR | Portfolio Master (VSAM) |
| PORTTRAN | Portfolio Transaction Handler | PORTMSTR | Portfolio Master (VSAM) |
| PORTVALD | Portfolio Validation Handler | PORTMSTR | Portfolio Master (VSAM) |
| PORTTEST | Portfolio Test Handler | PORTMSTR | Portfolio Master (VSAM) |

## Copybook Inventory

### Batch Copybooks (`src/copybook/batch/`)

| Copybook | Description | Used By |
|----------|-------------|---------|
| BCHCON | Batch Control Constants | BCHCTL00, PRCSEQ00, HISTLD00 |
| BCHCTL | Batch Control Record Structure | BCHCTL00, PRCSEQ00, HISTLD00, RPTSTA00 |
| CKPRST | Checkpoint/Restart Definitions | Batch programs |
| PRCSEQ | Process Sequence Record Structure | PRCSEQ00 |

### Common Copybooks (`src/copybook/common/`)

| Copybook | Description | Used By |
|----------|-------------|---------|
| TRNREC | Transaction Record Structure | HISTLD00, RPTPOS00, UTLVAL00, TSTGEN00 |
| POSREC | Position Record Structure | INQPORT, RPTPOS00, UTLVAL00 |
| HISTREC | History Record Structure | HISTLD00 |
| ERRHAND | Error Handling Definitions | All programs |
| RTNCODE | Return Code Definitions | All programs |
| PORTFLIO | Portfolio Record Structure | Portfolio programs, TSTGEN00 |
| PORTVAL | Portfolio Validation Rules | Portfolio programs |
| AUDITLOG | Audit Log Record Structure | RPTAUD00 |
| COMMON | Common Definitions | Various programs |
| RETHND | Return Handling | Various programs |

### Online Copybooks (`src/copybook/online/`)

| Copybook | Description | Used By |
|----------|-------------|---------|
| INQCOM | Inquiry Communication Area | INQONLN, INQPORT, INQHIST |
| DB2REQ | DB2 Request Area | DB2ONLN, DB2RECV, INQHIST |
| ERRHND | Online Error Handling | INQONLN, SECMGR, DB2ONLN, DB2RECV, ERRHNDL |

### DB2 Copybooks (`src/copybook/db2/`)

| Copybook | Description | Used By |
|----------|-------------|---------|
| DBTBLS | DB2 Table Structures | HISTLD00, DB2ERR |
| DBPROC | DB2 Processing Definitions | DB2CONN, DB2CMT, DB2ERR |
| SQLCA | SQL Communication Area | All DB2 programs |

## Data Flow Diagrams

### Batch Processing Flow

```
                    +----------------+
                    |   BCHCTL00     |
                    | Batch Control  |
                    +-------+--------+
                            |
                            v
                    +----------------+
                    |   PRCSEQ00     |
                    | Sequence Mgr   |
                    +-------+--------+
                            |
            +---------------+---------------+
            |               |               |
            v               v               v
    +-------------+  +-------------+  +-------------+
    |  TRNVAL00   |  |  POSUPD00   |  |  HISTLD00   |
    | Transaction |  |  Position   |  |  History    |
    | Validation  |  |  Update     |  |  Load       |
    +------+------+  +------+------+  +------+------+
           |                |                |
           v                v                v
    +-------------+  +-------------+  +-------------+
    | Transaction |  |  Position   |  |  POSHIST    |
    |   (VSAM)    |  |   (VSAM)    |  |   (DB2)     |
    +-------------+  +-------------+  +-------------+
```

### Online Processing Flow

```
    +------------------+
    |    CICS User     |
    +--------+---------+
             |
             v
    +------------------+
    |     INQONLN      |
    | Main Controller  |
    +--------+---------+
             |
    +--------+---------+--------+
    |                  |        |
    v                  v        v
+--------+      +--------+  +--------+
| INQPORT|      | INQHIST|  | SECMGR |
| Position|     | History|  |Security|
+----+---+      +----+---+  +----+---+
     |               |           |
     v               v           v
+--------+      +--------+  +--------+
| POSFILE|      | POSHIST|  |AUTHFILE|
| (VSAM) |      | (DB2)  |  | (DB2)  |
+--------+      +--------+  +--------+
```

### Reporting Flow

```
    +----------------+     +----------------+     +----------------+
    |   RPTPOS00     |     |   RPTAUD00     |     |   RPTSTA00     |
    | Position Rpt   |     |  Audit Rpt     |     | Statistics Rpt |
    +-------+--------+     +-------+--------+     +-------+--------+
            |                      |                      |
    +-------+-------+      +-------+-------+      +-------+-------+
    |               |      |               |      |               |
    v               v      v               v      v               v
+--------+    +--------+ +--------+  +--------+ +--------+  +--------+
|Position|    |  Trans |  | Audit |  | Error  | |  DB2   |  | Batch  |
| Master |    |History |  |  Log  |  |  Log   | | Stats  |  | Stats  |
| (VSAM) |    | (VSAM) |  | (DB2) |  | (DB2)  | | (VSAM) |  | (VSAM) |
+--------+    +--------+ +--------+  +--------+ +--------+  +--------+
```

## Database Schema Summary

### DB2 Tables

| Table | Description | Primary Key |
|-------|-------------|-------------|
| POSHIST | Position History - transaction history | ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME |
| ERRLOG | Error Logging - application errors | ERROR_TIMESTAMP, PROGRAM_ID |
| AUTHFILE | Authorization File - user access control | USER_ID, RESOURCE, ACCESS_TYPE |
| AUDITLOG | Audit Log - security audit trail | TIMESTAMP, USER_ID |
| PORTFOLIO_MASTER | Portfolio Master | PORTFOLIO_ID |
| INVESTMENT_POSITIONS | Investment Positions | PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE |
| TRANSACTION_HISTORY | Transaction History | TRANSACTION_ID |

### VSAM Files

| File | Organization | Key | Description |
|------|--------------|-----|-------------|
| PORTMSTR | KSDS | Portfolio ID + Account Type + Branch ID | Portfolio Master |
| TRANHIST | KSDS | Trans Date + Time + Portfolio ID + Seq | Transaction History |
| POSHIST | KSDS | Portfolio ID + Position Date + Investment ID | Position History |

## Program Dependencies Matrix

```
                    Called Programs
Calling Program   | ERRPROC | DB2CONN | DB2CMT | DB2ERR | SECMGR | ERRHNDL | DB2ONLN | CURSMGR | DB2RECV |
------------------|---------|---------|--------|--------|--------|---------|---------|---------|---------|
BCHCTL00          |    X    |         |        |        |        |         |         |         |         |
PRCSEQ00          |    X    |         |        |        |        |         |         |         |         |
HISTLD00          |    X    |    X    |   X    |   X    |        |         |         |         |         |
INQONLN           |         |         |        |        |   X    |    X    |         |         |         |
INQPORT           |         |         |        |        |        |    X    |         |         |         |
INQHIST           |         |         |        |        |        |    X    |    X    |    X    |    X    |
SECMGR            |         |         |        |        |        |    X    |         |         |         |
DB2CMT            |    X    |         |        |   X    |        |         |         |         |         |
DB2CONN           |    X    |         |        |        |        |         |         |         |         |
DB2ERR            |    X    |         |        |        |        |         |         |         |         |
DB2RECV           |         |         |        |        |        |    X    |    X    |         |         |
UTLMNT00          |    X    |         |        |        |        |         |         |         |         |
UTLMON00          |    X    |         |        |        |        |         |         |         |         |
UTLVAL00          |    X    |         |        |        |        |         |         |         |         |
TSTGEN00          |    X    |         |        |        |        |         |         |         |         |
TSTVAL00          |    X    |         |        |        |        |         |         |         |         |
RPTPOS00          |    X    |         |        |        |        |         |         |         |         |
RPTAUD00          |    X    |         |        |        |        |         |         |         |         |
RPTSTA00          |    X    |         |        |        |        |         |         |         |         |
```

## BMS Screen Maps

Located in `src/maps/INQSET.bms`

| Map | Description | Fields |
|-----|-------------|--------|
| MENMAP | Main Menu | Option selection, Error message |
| POSMAP | Portfolio Position Display | Account input, Fund ID, Fund Name, Units, Cost Basis, Market Value |
| HISMAP | Transaction History Display | Account input, 10 history rows (Date, Type, Units, Price, Amount) |
| ERRMAP | Error Display | Error code, Error details |

## JCL Job Inventory

Located in `src/jcl/batch/`

The JCL files define batch job execution including:
- Job scheduling and dependencies
- File allocations (VSAM, sequential)
- DB2 plan bindings
- Checkpoint/restart procedures

## Key Business Logic Components

### Transaction Processing
- Transaction validation (TRNVAL00)
- Position updates based on buy/sell transactions (POSUPD00)
- History loading to DB2 for reporting (HISTLD00)

### Security
- User validation via CICS ASSIGN
- Authorization checking against AUTHFILE
- Audit logging to AUDITLOG

### Error Handling
- Centralized error handler (ERRHNDL)
- DB2 error handling with retry logic (DB2ERR, DB2RECV)
- Error logging to ERRLOG table

### Data Validation
- Portfolio validation rules (PORTVALD)
- Transaction validation (TRNVAL00)
- Data integrity checks (UTLVAL00)

## Migration Considerations

### High Priority Components
1. Core data models (TRNREC, POSREC, HISTREC)
2. Database layer (DB2 tables, VSAM files)
3. Business logic (transaction processing, validation)
4. Error handling framework

### Medium Priority Components
1. Batch processing framework
2. Reporting system
3. Online inquiry system

### Lower Priority Components
1. Utility programs
2. Test data generation
3. System monitoring

## Conclusion

The COBOL Legacy Benchmark Suite represents a comprehensive investment portfolio management system with well-defined layers and clear separation of concerns. The migration to Python should preserve the existing business logic while modernizing the technology stack. Key areas requiring careful attention include:

1. Data type mappings (COMP-3, PIC clauses to Python types)
2. Transaction management (DB2 commit/rollback to SQLAlchemy)
3. File handling (VSAM to relational database)
4. Screen handling (BMS to web interface)
5. Job scheduling (JCL to Airflow/Celery)
