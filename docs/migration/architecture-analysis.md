# COBOL Legacy Benchmark Suite - Architecture Analysis

## Executive Summary

This document provides a comprehensive analysis of the Investment Portfolio Management System currently implemented in Enterprise COBOL for z/OS mainframes. The system is part of the COBOL Legacy Benchmark Suite (CLBS) and serves as a production-grade benchmark for evaluating LLM translation tools in COBOL modernization efforts.

## System Overview

The Investment Portfolio Management System is organized into five main processing layers:

1. **Batch Processing Layer** - Sequential transaction processing pipeline
2. **Online Transaction Processing (CICS)** - Real-time inquiry and user interaction
3. **Reporting System** - Analytical and audit report generation
4. **Utility Layer** - System maintenance and monitoring tools
5. **Test Layer** - Data generation and validation for benchmarking

## Program Inventory and Analysis

### Batch Processing Programs

Located in `src/programs/batch/`

| Program | Purpose | Dependencies | Data Access |
|---------|---------|--------------|-------------|
| BCHCTL00 | Batch Control Processor - Manages batch job execution, prerequisites, and status updates | ERRPROC, BCHCTL copybook, BCHCON copybook | VSAM Batch Control File |
| PRCSEQ00 | Process Sequence Manager - Builds and manages execution sequences, handles dependencies | ERRPROC, PRCSEQ copybook, BCHCTL copybook | VSAM Process Sequence File, Batch Control File |
| HISTLD00 | History Load Program - Transfers VSAM transaction data to DB2 POSHIST table | DB2CONN, ERRPROC, DBTBLS copybook | VSAM Transaction History, DB2 POSHIST |
| POSUPDT | Position Update Program - Updates portfolio positions based on transactions | (stub implementation) | VSAM Position Master |
| RPTPOS00 | Daily Position Report Generator - Creates portfolio valuation and summary reports | POSREC, TRNREC copybooks | VSAM Position Master, Transaction History |
| RPTAUD00 | Audit Report Generator - Produces security and process audit trails | AUDITLOG, ERRHAND copybooks | VSAM Audit File, Error Log |
| RPTSTA00 | Statistics Report Generator - Monitors system performance metrics | RTNCODE, ERRHAND copybooks | Various system files |
| CKPRST | Checkpoint/Restart Handler - Manages batch job recovery | CKPRST copybook | Checkpoint files |
| RCVPRC00 | Recovery Process - Handles batch job recovery procedures | ERRHAND copybook | Recovery files |
| RTNANA00 | Return Code Analyzer - Analyzes and reports on job return codes | RTNCODE copybook | Return code files |
| RTNCDE00 | Return Code Handler - Processes and manages return codes | RTNCODE copybook | Return code files |

### Online Programs (CICS)

Located in `src/programs/online/`

| Program | Purpose | Dependencies | Data Access |
|---------|---------|--------------|-------------|
| INQONLN | Main Online Controller - Orchestrates CICS screen flow and program dispatch | INQCOM, ERRHND copybooks, SECMGR, ERRHNDL | BMS Maps (INQSET) |
| INQPORT | Portfolio Position Inquiry - Retrieves current portfolio positions from VSAM | INQCOM, POSREC copybooks | VSAM POSFILE |
| INQHIST | Transaction History Inquiry - Retrieves history from DB2 POSHIST | INQCOM, DB2ONLN, CURSMGR, DB2RECV | DB2 POSHIST |
| SECMGR | Security Manager - Handles user validation, authorization, and audit logging | ERRHND copybook | DB2 AUTHFILE, AUDITLOG |
| CURSMGR | Cursor Manager - Manages DB2 cursor declarations and lifecycle | SQLCA | DB2 cursors |
| ERRHNDL | Centralized Error Handler - Logs errors to DB2 and determines recovery actions | ERRHND copybook | DB2 ERRLOG |
| DB2ONLN | Online DB2 Connection Manager - Manages DB2 connection pool | ERRHND copybook | DB2 connections |
| DB2RECV | DB2 Recovery Manager - Handles connection failures and transaction rollbacks | DB2REQ, ERRHND copybooks | DB2 connections |

### Common/Support Programs

Located in `src/programs/common/`

| Program | Purpose | Dependencies | Data Access |
|---------|---------|--------------|-------------|
| DB2CONN | DB2 Connection Manager - Establishes and manages DB2 connections | SQLCA, DBPROC, ERRHAND copybooks | DB2 connections |
| DB2CMT | DB2 Commit Handler - Manages transaction commits | SQLCA copybook | DB2 transactions |
| DB2ERR | DB2 SQL Error Handler - Logs and diagnoses DB2 errors | DBTBLS, SQLCA, DBPROC copybooks | DB2 ERRLOG |
| DB2STAT | DB2 Statistics Collector - Gathers DB2 performance metrics | SQLCA copybook | DB2 statistics |
| ERRPROC | Error Processing - Common error handling routines | ERRHAND copybook | Error logs |
| AUDPROC | Audit Processing - Common audit trail routines | AUDITLOG copybook | Audit files |

### Utility Programs

Located in `src/programs/utility/`

| Program | Purpose | Dependencies | Data Access |
|---------|---------|--------------|-------------|
| UTLMNT00 | File Maintenance Utility - Archive, cleanup, VSAM reorganization, space management | RTNCODE, ERRHAND copybooks | VSAM files, Archive files |
| UTLMON00 | System Monitoring Utility - Resource utilization, performance metrics, threshold monitoring | RTNCODE, ERRHAND, DB2STAT copybooks | Config files, Monitor logs, DB2 stats |
| UTLVAL00 | Data Validation Utility - Data integrity, cross-reference validation, balance reconciliation | RTNCODE, ERRHAND, POSREC, TRNREC copybooks | Position Master, Transaction History |

### Test Programs

Located in `src/programs/test/`

| Program | Purpose | Dependencies | Data Access |
|---------|---------|--------------|-------------|
| TSTGEN00 | Test Data Generator - Creates synthetic portfolio and transaction data | RTNCODE, ERRHAND, PORTFLIO, TRNREC copybooks | Test config, Portfolio output, Transaction output |
| TSTVAL00 | Test Validation Suite - Compares actual vs expected results | RTNCODE, ERRHAND copybooks | Test cases, Expected/Actual results |

### Portfolio Programs

Located in `src/programs/portfolio/`

| Program | Purpose | Dependencies | Data Access |
|---------|---------|--------------|-------------|
| PORTMSTR | Portfolio Master Handler - Core portfolio record management | PORTFLIO copybook | VSAM Portfolio Master |
| PORTADD | Portfolio Add - Creates new portfolio records | PORTFLIO, PORTVAL copybooks | VSAM Portfolio Master |
| PORTUPDT | Portfolio Update - Modifies existing portfolio records | PORTFLIO, PORTVAL copybooks | VSAM Portfolio Master |
| PORTDEL | Portfolio Delete - Removes portfolio records | PORTFLIO copybook | VSAM Portfolio Master |
| PORTREAD | Portfolio Read - Retrieves portfolio records | PORTFLIO copybook | VSAM Portfolio Master |
| PORTVALD | Portfolio Validation - Validates portfolio data | PORTVAL copybook | VSAM Portfolio Master |
| PORTTRAN | Portfolio Transaction - Processes portfolio transactions | PORTFLIO, TRNREC copybooks | VSAM Portfolio Master, Transaction files |
| PORTTEST | Portfolio Test - Tests portfolio operations | PORTFLIO copybook | Test files |

## Copybook Catalog

### Batch Copybooks (`src/copybook/batch/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| BCHCTL | Batch Control Record | BCT-KEY, BCT-JOB-NAME, BCT-PROCESS-DATE, BCT-SEQUENCE-NO, BCT-STATUS, BCT-RETURN-CODE |
| BCHCON | Batch Constants | BCT-RC-SUCCESS, BCT-RC-WARNING, BCT-RC-ERROR, BCT-STAT-READY, BCT-STAT-ACTIVE, BCT-STAT-DONE |
| PRCSEQ | Process Sequence Record | PSR-KEY, PSR-PROCESS-ID, PSR-TYPE, PSR-DEP-COUNT, PSR-DEP-ID, PSR-DEP-HARD, PSR-DEP-RC |
| CKPRST | Checkpoint/Restart | Checkpoint data structures |

### Online Copybooks (`src/copybook/online/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| INQCOM | Inquiry Communication Area | INQCOM-FUNCTION (MENU/INQP/INQH/EXIT), INQCOM-ACCOUNT-NO, INQCOM-RESPONSE-CODE, INQCOM-ERROR-MSG |
| DB2REQ | DB2 Request Area | DB2-REQUEST-TYPE (C/D/S), DB2-RESPONSE-CODE, DB2-CONNECTION-TOKEN, DB2-SQLCODE, DB2-ERROR-MSG |
| ERRHND | Error Handling | ERR-PROGRAM, ERR-PARAGRAPH, ERR-SQLCODE, ERR-CICS-RESP, ERR-SEVERITY (F/W/I), ERR-MESSAGE, ERR-ACTION (R/C/A), ERR-TRACE-ID, ERR-TIMESTAMP |

### Common Copybooks (`src/copybook/common/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| TRNREC | Transaction Record | TRN-KEY (DATE+TIME+PORTFOLIO+SEQ), TRN-INVESTMENT-ID, TRN-TYPE (BU/SL/TR/FE), TRN-QUANTITY, TRN-PRICE, TRN-AMOUNT, TRN-CURRENCY, TRN-STATUS (P/D/F/R), TRN-AUDIT |
| POSREC | Position Record | POS-KEY (PORTFOLIO+DATE+INVESTMENT), POS-QUANTITY, POS-COST-BASIS, POS-MARKET-VALUE, POS-CURRENCY, POS-STATUS (A/C/P), POS-AUDIT |
| HISTREC | History Record | HIST-KEY (PORTFOLIO+DATE+TIME+SEQ), HIST-RECORD-TYPE (PT/PS/TR), HIST-ACTION-CODE (A/C/D), HIST-BEFORE-IMAGE, HIST-AFTER-IMAGE, HIST-REASON-CODE, HIST-AUDIT |
| ERRHAND | Error Handling | ERR-PROGRAM, ERR-TEXT, WS-ERROR-MESSAGE |
| RTNCODE | Return Codes | Standard return code definitions |
| PORTFLIO | Portfolio Record | Portfolio data structure |
| PORTVAL | Portfolio Validation | Validation rules and constants |
| AUDITLOG | Audit Log Record | Audit trail data structure |
| COMMON | Common Definitions | Shared constants and structures |
| RETHND | Return Handler | Return handling structures |

### DB2 Copybooks (`src/copybook/db2/`)

| Copybook | Purpose | Key Fields |
|----------|---------|------------|
| SQLCA | SQL Communication Area | SQLCODE, SQLERRMC, SQLSTATE |
| DBPROC | DB2 Procedures | DB2-RETRY-WAIT, connection parameters |
| DBTBLS | DB2 Table Definitions | POSHIST-RECORD, ERRLOG-RECORD structures |

## Data Flow Analysis

### Batch Processing Flow

```
Input Files (VSAM)          Batch Programs              Output (DB2/Reports)
==================          ==============              ====================
                                   |
Transaction History  -----> TRNVAL00 (Validation) ----> Validated Transactions
                                   |
                                   v
Position Master     -----> POSUPD00 (Update) --------> Updated Positions
                                   |
                                   v
Transaction History  -----> HISTLD00 (Load) ---------> DB2 POSHIST Table
                                   |
                                   v
Position Master     -----> RPTPOS00 (Report) --------> Daily Position Report
Audit/Error Logs    -----> RPTAUD00 (Report) --------> Audit Report
System Stats        -----> RPTSTA00 (Report) --------> Statistics Report
```

### Online Processing Flow

```
CICS Terminal               Online Programs              Data Access
=============               ===============              ===========
                                   |
User Request ---------> INQONLN (Controller)
                              |
                              +---> SECMGR (Security) ----> DB2 AUTHFILE/AUDITLOG
                              |
                              +---> INQPORT (Position) ---> VSAM POSFILE
                              |
                              +---> INQHIST (History) ----> DB2 POSHIST
                                         |
                                         +---> DB2ONLN (Connection)
                                         +---> CURSMGR (Cursor)
                                         +---> DB2RECV (Recovery)
                              |
                              +---> ERRHNDL (Errors) -----> DB2 ERRLOG
```

### Batch Control Flow

```
BCHCTL00 (Control)
      |
      +---> PRCSEQ00 (Sequencing)
                |
                +---> Check Dependencies
                +---> Update Status
                +---> Manage Checkpoints
```

## Database Schema Analysis

### DB2 Tables

**POSHIST (Position History)**
- Primary Key: ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME
- Contains: Transaction details, amounts, fees, cost basis, gain/loss
- Partitioned by TRANS_DATE (quarterly)
- Indexes: Security ID + Date, Process Date + Program ID

**ERRLOG (Error Log)**
- Primary Key: ERROR_TIMESTAMP, PROGRAM_ID
- Contains: Error details, severity, messages, additional info
- Index: Process Date + Error Severity

**AUTHFILE (Authorization)**
- Contains: USER_ID, RESOURCE, ACCESS_TYPE
- Used by SECMGR for access control

**AUDITLOG (Audit Trail)**
- Contains: TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE
- Used for security audit trail

**Additional Tables (from db2-definitions.sql)**
- PORTFOLIO_MASTER: Portfolio metadata and status
- INVESTMENT_POSITIONS: Current position holdings
- TRANSACTION_HISTORY: Transaction records

### VSAM Files

**PORTMSTR (Portfolio Master)**
- Organization: KSDS
- Record Length: 400 bytes
- Key: Portfolio ID (8) + Account Type (2) + Branch ID (2) = 12 bytes

**TRANHIST (Transaction History)**
- Organization: KSDS
- Record Length: 300 bytes
- Key: Date (8) + Time (6) + Portfolio ID (8) + Sequence (6) = 28 bytes

**POSHIST (Position History - VSAM)**
- Organization: KSDS
- Record Length: 350 bytes
- Key: Portfolio ID (8) + Position Date (8) + Investment ID (10) = 26 bytes

## BMS Screen Maps

Located in `src/maps/INQSET.bms`

| Map | Purpose | Key Fields |
|-----|---------|------------|
| MENMAP | Main Menu | Option selection (1-3), Error message display |
| POSMAP | Portfolio Position | Account input, Fund ID/Name, Units, Cost Basis, Market Value |
| HISMAP | Transaction History | Account input, 10 rows of history (Date, Type, Units, Price, Amount) |
| ERRMAP | Error Display | Error code, Error details |

## JCL Job Analysis

Located in `src/jcl/`

### Batch Jobs (`src/jcl/batch/`)
- RPTPOS.jcl - Daily Position Report execution
- RPTAUD.jcl - Audit Report execution
- RPTSTA.jcl - Statistics Report execution

### Portfolio Jobs (`src/jcl/portfolio/`)
- PORTDEF.jcl - Portfolio definition
- PORTADD.jcl - Portfolio addition
- PORTUPDT.jcl - Portfolio update
- PORTDEL.jcl - Portfolio deletion
- PORTREAD.jcl - Portfolio read
- PORTTEST.jcl - Portfolio testing

### Test Jobs (`src/jcl/test/`)
- TSTGEN.jcl - Test data generation
- TSTVAL.jcl - Test validation

### Utility Jobs (`src/jcl/utility/`)
- UTLMNT.jcl - File maintenance
- UTLMON.jcl - System monitoring
- UTLVAL.jcl - Data validation

## Program Dependency Graph

```
                              BCHCTL00
                                 |
                    +------------+------------+
                    |                         |
                PRCSEQ00                  ERRPROC
                    |
        +-----------+-----------+
        |           |           |
    HISTLD00    POSUPD00    RPTPOS00
        |                       |
    +---+---+               +---+---+
    |       |               |       |
DB2CONN  ERRPROC        POSREC   TRNREC


                              INQONLN
                                 |
            +--------+----------+----------+--------+
            |        |          |          |        |
         SECMGR   INQPORT   INQHIST    ERRHNDL   (BMS)
            |        |          |          |
            |     POSREC    +---+---+   ERRLOG
            |               |       |
         AUTHFILE       DB2ONLN  CURSMGR
         AUDITLOG           |       |
                        DB2RECV  SQLCA
```

## Key Observations for Migration

1. **Layered Architecture**: The system follows a clear separation between batch, online, reporting, and utility functions, which maps well to a modern microservices or modular monolith architecture.

2. **Data Access Patterns**: 
   - VSAM files are used for high-performance operational data
   - DB2 is used for historical data, audit trails, and reporting
   - This dual-storage pattern can be consolidated into a single RDBMS

3. **Error Handling**: Centralized error handling through ERRHNDL and ERRPROC programs with DB2 logging provides a good pattern to replicate in Python.

4. **Security Model**: SECMGR implements authentication, authorization, and audit logging - this maps to modern authentication/authorization frameworks.

5. **Batch Control**: BCHCTL00 and PRCSEQ00 provide job sequencing and dependency management - this maps to workflow orchestration tools like Airflow.

6. **Connection Pooling**: DB2ONLN implements connection pooling for online programs - this is handled automatically by modern ORMs and connection pools.

7. **Recovery Mechanisms**: DB2RECV provides retry logic and transaction recovery - this pattern should be preserved in the Python implementation.

8. **Copybook Reuse**: Copybooks provide data structure consistency across programs - Python dataclasses/Pydantic models will serve the same purpose.
