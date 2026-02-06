# COBOL Legacy Benchmark Suite: Demo Use Case for Migration Tasks

## Executive Summary

The **COBOL Legacy Benchmark Suite (CLBS)** represents a production-grade Investment Portfolio Management System that showcases the full complexity of real-world COBOL modernization challenges. This demo use case document illustrates why CLBS is an ideal benchmark for evaluating LLM translation tools and demonstrates the compelling migration tasks that COBOL power users will recognize from their own enterprise environments.

---

## Why This Benchmark Matters for COBOL Power Users

### The Reality of Legacy COBOL Systems

Most publicly available COBOL examples are trivial "Hello World" programs or simple file processing routines. Real enterprise COBOL systems are vastly more complex, featuring:

- **Multi-layer architectures** spanning batch, online, and reporting subsystems
- **Deep integration** with mainframe infrastructure (VSAM, DB2, CICS)
- **Complex inter-program dependencies** through copybooks and shared data structures
- **Production-grade error handling** with checkpoint/restart capabilities
- **Security frameworks** integrated with RACF and DB2 authorization

The CLBS addresses this gap by providing a comprehensive system that mirrors what COBOL power users encounter in production environments.

---

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        COBOL Legacy Benchmark Suite                          │
│                   Investment Portfolio Management System                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐          │
│  │  Batch Layer     │  │  Online Layer    │  │  Reporting Layer │          │
│  │                  │  │                  │  │                  │          │
│  │  TRNVAL00        │  │  INQONLN (CICS)  │  │  RPTPOS00        │          │
│  │  POSUPDT         │  │  INQPORT         │  │  RPTAUD00        │          │
│  │  HISTLD00        │  │  INQHIST         │  │  RPTSTA00        │          │
│  │  BCHCTL00        │  │  SECMGR          │  │                  │          │
│  └────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘          │
│           │                     │                     │                     │
│  ┌────────┴─────────────────────┴─────────────────────┴─────────┐          │
│  │                     Data Access Layer                         │          │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐   │          │
│  │  │ VSAM Files  │  │ DB2 Tables  │  │ Copybook Structures │   │          │
│  │  │ (KSDS/ESDS) │  │ (POSHIST)   │  │ (POSREC, TRNREC)    │   │          │
│  │  └─────────────┘  └─────────────┘  └─────────────────────┘   │          │
│  └───────────────────────────────────────────────────────────────┘          │
│                                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                                 │
│  │  Utility Layer   │  │  Test Layer      │                                 │
│  │                  │  │                  │                                 │
│  │  UTLMNT00        │  │  TSTGEN00        │                                 │
│  │  UTLMON00        │  │  TSTVAL00        │                                 │
│  │  UTLVAL00        │  │                  │                                 │
│  └──────────────────┘  └──────────────────┘                                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Compelling Migration Challenges

### 1. Complex Business Logic Translation

**Challenge**: Financial calculations with packed decimal precision

The system implements real financial calculations using COBOL's native packed decimal (COMP-3) arithmetic, which must be precisely translated to maintain calculation accuracy.

**Example from `POSREC.cpy`:**
```cobol
05  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
05  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
05  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
```

**Migration Complexity**:
- Packed decimal to BigDecimal/Decimal conversion
- Maintaining precision across 15+ digit calculations
- Handling signed values with implied decimal positions
- Currency rounding rules compliance

---

### 2. VSAM File Handling Patterns

**Challenge**: Indexed and sequential file access patterns

The system uses both KSDS (Key-Sequenced Data Set) and sequential VSAM access patterns that must be translated to modern database or file systems.

**Example from `HISTLD00.cbl`:**
```cobol
SELECT TRANSACTION-HISTORY
    ASSIGN TO TRANHIST
    ORGANIZATION IS INDEXED
    ACCESS MODE IS SEQUENTIAL
    RECORD KEY IS TH-KEY
    FILE STATUS IS WS-TH-STATUS.
```

**Migration Complexity**:
- Translating VSAM KSDS to relational database tables
- Preserving key structure and access patterns
- Handling FILE STATUS codes (00, 10, 23, etc.)
- Converting sequential processing to cursor-based iteration

---

### 3. DB2 Integration with Embedded SQL

**Challenge**: Embedded SQL with host variables and SQLCA handling

The system demonstrates production-grade DB2 integration including connection management, transaction control, and error handling.

**Example from `HISTLD00.cbl`:**
```cobol
EXEC SQL
    INSERT INTO POSHIST
    VALUES (:POSHIST-RECORD)
END-EXEC

IF SQLCODE = 0
    ADD 1 TO WS-RECORDS-WRITTEN
ELSE
    IF SQLCODE = -803
        CONTINUE
    ELSE
        ADD 1 TO WS-ERROR-COUNT
        PERFORM DB2-ERROR-ROUTINE
    END-IF
END-IF
```

**Migration Complexity**:
- Converting embedded SQL to JDBC/ORM patterns
- Translating SQLCODE handling to exception-based error handling
- Preserving transaction boundaries (COMMIT WORK)
- Handling duplicate key scenarios (-803)

---

### 4. CICS Transaction Processing

**Challenge**: Online transaction processing with screen mapping

The online inquiry system demonstrates full CICS integration including BMS screen handling, COMMAREA passing, and transaction security.

**Example from `INQONLN.cbl`:**
```cobol
EXEC CICS HANDLE CONDITION
          ERROR(P900-ERROR-ROUTINE)
          PGMIDERR(P900-ERROR-ROUTINE)
          NOTFND(P900-ERROR-ROUTINE)
END-EXEC.

EXEC CICS RECEIVE MAP('INQMAP')
          MAPSET('INQSET')
          INTO(WS-COMMAREA)
          RESP(WS-RESPONSE-CODE)
END-EXEC.

EXEC CICS LINK PROGRAM('INQPORT')
          COMMAREA(WS-COMMAREA)
          LENGTH(LENGTH OF WS-COMMAREA)
          RESP(WS-RESPONSE-CODE)
END-EXEC.
```

**Migration Complexity**:
- Converting BMS maps to modern UI frameworks (React, Angular)
- Translating CICS LINK/XCTL to REST API calls or service invocations
- Preserving COMMAREA data flow as DTOs or session state
- Converting HANDLE CONDITION to try-catch exception handling

---

### 5. BMS Screen Definitions

**Challenge**: Terminal-based UI to modern web interfaces

The BMS mapset defines 3270 terminal screens that must be translated to modern web interfaces while preserving field validation and navigation.

**Example from `INQSET.bms`:**
```
POSMAP   DFHMDI SIZE=(24,80)
         DFHMDF POS=(1,1),LENGTH=40,ATTRB=(PROT,BRT),
               INITIAL='Portfolio Position Inquiry'
ACCTIN   DFHMDF POS=(3,12),LENGTH=10,ATTRB=(UNPROT,IC)
FUNDOUT  DFHMDF POS=(5,12),LENGTH=6,ATTRB=(PROT),COLOR=TURQUOISE
UNITOUT  DFHMDF POS=(7,12),LENGTH=15,ATTRB=(PROT),COLOR=TURQUOISE
```

**Migration Complexity**:
- Converting 24x80 fixed layout to responsive design
- Translating field attributes (PROT, UNPROT, BRT) to HTML/CSS
- Preserving PF key navigation as button/keyboard handlers
- Maintaining field-level validation rules

---

### 6. Checkpoint/Restart Framework

**Challenge**: Batch recovery patterns for long-running processes

The batch control system implements checkpoint/restart capabilities essential for processing millions of records.

**Example from `HISTLD00.cbl`:**
```cobol
2300-CHECK-COMMIT.
    ADD 1 TO WS-COMMIT-COUNT
    
    IF WS-COMMIT-COUNT >= WS-COMMIT-THRESHOLD
        EXEC SQL
            COMMIT WORK
        END-EXEC
        
        MOVE 0 TO WS-COMMIT-COUNT
        
        PERFORM 2310-UPDATE-CHECKPOINT
    END-IF.

2310-UPDATE-CHECKPOINT.
    MOVE WS-RECORDS-READ TO BCT-RECORDS-READ
    MOVE WS-RECORDS-WRITTEN TO BCT-RECORDS-WRITTEN
    
    REWRITE BATCH-CONTROL-RECORD
```

**Migration Complexity**:
- Translating checkpoint files to database-based state management
- Implementing idempotent processing for restart scenarios
- Converting commit thresholds to batch/chunk processing frameworks
- Preserving audit trail of processing progress

---

### 7. Security Framework Integration

**Challenge**: RACF/DB2 authorization to modern security patterns

The security manager demonstrates enterprise security patterns including user validation, resource authorization, and audit logging.

**Example from `SECMGR.cbl`:**
```cobol
P200-CHECK-AUTH.
    EXEC SQL
         SELECT COUNT(*)
         INTO :WS-DB2-AREA
         FROM AUTHFILE
         WHERE USER_ID = :SEC-USER-ID
           AND RESOURCE = :SEC-RESOURCE-NAME
           AND ACCESS_TYPE = :SEC-ACCESS-TYPE
    END-EXEC.

P300-LOG-ACCESS.
    EXEC SQL
         INSERT INTO AUDITLOG
         (TIMESTAMP, USER_ID, TERMINAL_ID, 
          TRANS_ID, PROGRAM, ACCESS_TYPE)
         VALUES
         (:WS-TIMESTAMP, :WS-USER-ID, :WS-TERMINAL-ID,
          :WS-TRANSACTION-ID, :WS-PROGRAM-NAME, 
          :WS-ACCESS-TYPE)
    END-EXEC.
```

**Migration Complexity**:
- Converting DB2-based authorization to OAuth2/JWT patterns
- Translating CICS ASSIGN to Spring Security context
- Preserving audit logging with modern logging frameworks
- Implementing role-based access control (RBAC)

---

### 8. Inter-Program Communication

**Challenge**: CALL/LINK patterns and copybook sharing

The system demonstrates complex inter-program dependencies through COBOL CALL statements and shared copybook structures.

**Program Dependency Matrix:**

| Program  | Calls/Links To | Copybooks Used | Data Shared |
|----------|----------------|----------------|-------------|
| INQONLN  | INQPORT, INQHIST, SECMGR, ERRHNDL | INQCOM, ERRHND | COMMAREA |
| HISTLD00 | ERRPROC, DB2CONN | HISTREC, BCHCTL, DBTBLS, SQLCA | Control records |
| RPTPOS00 | Error handler | POSREC, TRNREC, RTNCODE | Position/Transaction data |
| SECMGR   | DB2 access | ERRHND, SQLCA | Security request area |

**Migration Complexity**:
- Converting CALL BY REFERENCE to method parameters
- Translating copybooks to shared DTOs/POJOs
- Preserving data layout compatibility
- Handling LINKAGE SECTION to method signatures

---

### 9. JCL Procedures and Job Scheduling

**Challenge**: Batch job orchestration to modern schedulers

The JCL procedures define job execution parameters, file allocations, and step dependencies.

**Example from `RPTPOS.jcl`:**
```jcl
//RPTPOS00 JOB (ACCT#),'DAILY POSITION RPT',
//             CLASS=A,MSGCLASS=X,MSGLEVEL=(1,1)
//STEP01   EXEC PGM=RPTPOS00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//POSMSTRE DD   DSN=PROD.POSITION.MASTER,DISP=SHR
//TRANHIST DD   DSN=PROD.TRANSACTION.HISTORY,DISP=SHR
//RPTFILE  DD   DSN=PROD.DAILY.POSITION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE)
```

**Migration Complexity**:
- Converting DD statements to file path configurations
- Translating DISP parameters to file handling modes
- Implementing job dependencies in modern schedulers (Airflow, Control-M)
- Preserving SPACE allocation as storage quotas

---

### 10. CICS Resource Definitions

**Challenge**: Transaction and program definitions to service configurations

The CSD file defines CICS resources including transactions, programs, files, and DB2 connections.

**Example from `PORTDFN.csd`:**
```
DEFINE TRANSACTION(PINQ)
       PROGRAM(INQONLN)
       PROFILE(DFHCICST)
       DESCRIPTION(Portfolio Inquiry Transaction)

DEFINE DB2ENTRY(PORTDB2)
       AUTHTYPE(USERID)
       PLAN(PORTPLAN)
       PRIORITY(HIGH)
       PROTECTNUM(5)

DEFINE FILE(POSFILE)
       DSNAME(PORTFOLIO.POSITION.VSAM)
       ADD(YES) BROWSE(YES) DELETE(NO) READ(YES) UPDATE(NO)
```

**Migration Complexity**:
- Converting transaction definitions to REST endpoint configurations
- Translating DB2ENTRY to connection pool settings
- Mapping file definitions to data source configurations
- Preserving access control (ADD, BROWSE, DELETE, READ, UPDATE)

---

## Test Data Generation for Benchmarking

The TSTGEN00 program generates comprehensive test data for validating translations:

```cobol
01  WS-TEST-TYPES.
    05  WS-PORTFOLIO         PIC X(10) VALUE 'PORTFOLIO'.
    05  WS-TRANSACTION       PIC X(10) VALUE 'TRANSACTN'.
    05  WS-ERROR-TEST        PIC X(10) VALUE 'ERROR'.
    05  WS-VOLUME-TEST       PIC X(10) VALUE 'VOLUME'.
```

**Test Scenarios Supported**:
- **Portfolio data**: Valid portfolio records with various statuses
- **Transaction data**: Buy, sell, transfer, and fee transactions
- **Error conditions**: Invalid data for error handling validation
- **Volume testing**: Large datasets for performance benchmarking

---

## Migration Validation Framework

The TSTVAL00 program provides a validation framework for comparing original COBOL behavior with translated code:

**Validation Approach**:
1. Generate identical test data for both systems
2. Execute equivalent operations
3. Compare output files and database states
4. Validate error handling behavior
5. Measure performance characteristics

---

## Summary: Why CLBS is the Ideal Benchmark

| Feature | CLBS Coverage | Real-World Relevance |
|---------|---------------|---------------------|
| Batch Processing | Full pipeline with checkpoint/restart | Daily/monthly processing cycles |
| Online Transactions | CICS with BMS screens | Customer-facing applications |
| Database Integration | DB2 with embedded SQL | Enterprise data management |
| File Handling | VSAM KSDS/ESDS patterns | Legacy data storage |
| Security | RACF/DB2 authorization | Compliance requirements |
| Error Handling | Production-grade recovery | System reliability |
| Inter-program Communication | CALL/LINK with copybooks | Modular architecture |
| Job Control | JCL with dependencies | Batch orchestration |
| Reporting | Multiple report types | Business intelligence |
| Testing | Data generation and validation | Quality assurance |

---

## Getting Started with Migration Evaluation

### Step 1: Understand the System
Review the [System Architecture Document](../technical/system-architecture.md) and [Data Dictionary](../technical/data-dictionary.md).

### Step 2: Select Migration Scope
Choose specific programs or layers to evaluate:
- **Batch-only**: HISTLD00, RPTPOS00
- **Online-only**: INQONLN, INQPORT, INQHIST
- **Full system**: All components with dependencies

### Step 3: Generate Test Data
Use TSTGEN00 to create baseline test data.

### Step 4: Execute Translation
Apply your LLM translation tool to the selected scope.

### Step 5: Validate Results
Use TSTVAL00 patterns to compare behavior.

---

## Conclusion

The COBOL Legacy Benchmark Suite provides COBOL power users with a realistic, production-grade system for evaluating LLM translation tools. Unlike trivial examples, CLBS presents the full complexity of enterprise COBOL applications, making it an invaluable resource for:

- **Tool developers**: Benchmarking translation accuracy and completeness
- **Modernization teams**: Evaluating migration approaches
- **Researchers**: Studying legacy code transformation challenges
- **Organizations**: Planning COBOL modernization initiatives

The system's comprehensive coverage of COBOL and mainframe features ensures that any translation tool capable of handling CLBS will be well-prepared for real-world migration projects.

---

*Document Version: 1.0*  
*Last Updated: 2024*  
*Repository: [COBOL-Legacy-Benchmark-Suite](https://github.com/COG-GTM/COBOL-Legacy-Benchmark-Suite)*
