# Utility & Test Programs — Technical Reference

Version: 1.0
Last Updated: 2024-04-09

---

## Table of Contents

1. [Overview](#1-overview)
2. [UTLMNT00 — File Maintenance Utility](#2-utlmnt00--file-maintenance-utility)
3. [UTLMON00 — System Monitoring Utility](#3-utlmon00--system-monitoring-utility)
4. [UTLVAL00 — Data Validation Utility](#4-utlval00--data-validation-utility)
5. [TSTGEN00 — Test Data Generator](#5-tstgen00--test-data-generator)
6. [TSTVAL00 — Test Validation Suite](#6-tstval00--test-validation-suite)
7. [System Monitoring Architecture Diagram](#7-system-monitoring-architecture-diagram)
8. [Test Data Generation Flow Diagram](#8-test-data-generation-flow-diagram)
9. [Validation Types Matrix](#9-validation-types-matrix)
10. [DB2 & VSAM Data Definitions](#10-db2--vsam-data-definitions)
11. [Appendix — Copybook Quick Reference](#11-appendix--copybook-quick-reference)

---

## 1. Overview

The COBOL Legacy Benchmark Suite includes three **utility programs** and two **test programs** that support the Investment Portfolio Management System. Utility programs provide file maintenance, system monitoring, and data validation capabilities. Test programs generate synthetic test data and validate system behavior against expected results.

| Program   | Type    | Purpose                        | Source Path                              |
|-----------|---------|--------------------------------|------------------------------------------|
| UTLMNT00  | Utility | File maintenance & archival    | `src/programs/utility/UTLMNT00.cbl`      |
| UTLMON00  | Utility | System health monitoring       | `src/programs/utility/UTLMON00.cbl`      |
| UTLVAL00  | Utility | Data integrity validation      | `src/programs/utility/UTLVAL00.cbl`      |
| TSTGEN00  | Test    | Test data generation           | `src/programs/test/TSTGEN00.cbl`         |
| TSTVAL00  | Test    | Test result validation suite   | `src/programs/test/TSTVAL00.cbl`         |

---

## 2. UTLMNT00 — File Maintenance Utility

### 2.1 Purpose & Business Function

UTLMNT00 performs scheduled and on-demand maintenance operations on the system's VSAM files. It reads a sequential control file containing maintenance directives and dispatches each directive to the appropriate processing routine. The program supports four maintenance functions: archiving aged records, cleaning up expired data, reorganizing VSAM clusters for optimal performance, and analyzing file space utilization.

### 2.2 Copybook Dependencies

| Copybook | Location                           | Purpose                                    |
|----------|------------------------------------|--------------------------------------------|
| RTNCODE  | `src/copybook/common/RTNCODE.cpy` | Return code management area                |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions        |

### 2.3 File I/O

| DD Name   | File Type   | Direction | Organization | Description                                             |
|-----------|-------------|-----------|-------------|---------------------------------------------------------|
| CTLFILE   | Sequential  | Input     | Sequential  | Control file with maintenance directives                |
| ARCHFILE  | Sequential  | Output    | Sequential  | Archive output (variable-length records, up to 32,760 bytes) |
| RPTFILE   | Sequential  | Output    | Sequential  | Maintenance report (132-byte fixed-length records)      |

**Control Record Layout (152 bytes):**

| Field            | PIC         | Bytes | Description                              |
|------------------|-------------|-------|------------------------------------------|
| CTL-FUNCTION     | X(8)        | 8     | Maintenance function code                |
| CTL-FILE-NAME    | X(44)       | 44    | Target VSAM file dataset name            |
| CTL-PARAMETERS   | X(100)      | 100   | Function-specific parameters             |

### 2.4 Processing Modes / Functions

| Function   | Value      | Paragraph         | Sub-steps                                        | Description                                                         |
|------------|------------|--------------------|--------------------------------------------------|---------------------------------------------------------------------|
| ARCHIVE    | `ARCHIVE`  | 2200-ARCHIVE-PROCESS | Open VSAM → Archive records → Close VSAM         | Copies aged records from a VSAM file to the sequential archive file |
| CLEANUP    | `CLEANUP`  | 2300-CLEANUP-PROCESS | Analyze space → Delete old records → Update catalog | Removes expired records and reclaims space                         |
| REORG      | `REORG`    | 2400-REORG-PROCESS   | Export data → Delete/Define cluster → Import data | Full VSAM reorganization via export/import cycle                   |
| ANALYZE    | `ANALYZE`  | 2500-ANALYZE-PROCESS | Collect statistics → Generate report              | Gathers space and performance statistics for a VSAM file           |

**Processing Flow:**

```
0000-MAIN
 ├── 1000-INITIALIZE
 │    ├── 1100-OPEN-FILES (CTLFILE, ARCHFILE, RPTFILE)
 │    └── 1200-INIT-PROCESSING (reset counters)
 ├── 2000-PROCESS (loop until end of CTLFILE)
 │    └── 2100-PROCESS-FUNCTION (EVALUATE CTL-FUNCTION)
 │         ├── ARCHIVE → 2200-ARCHIVE-PROCESS
 │         ├── CLEANUP → 2300-CLEANUP-PROCESS
 │         ├── REORG   → 2400-REORG-PROCESS
 │         └── ANALYZE → 2500-ANALYZE-PROCESS
 ├── 3000-CLEANUP (close all files)
 └── 9999-ERROR-HANDLER (RC 12 after 100 errors)
```

### 2.5 Error Handling

- Errors increment `WS-ERROR-COUNT`.
- If the error count exceeds **100**, the program terminates with **RETURN-CODE 12**.
- All error messages are displayed on the operator console (`DISPLAY ... UPON CONS`).

### 2.6 JCL Reference

**Job:** `src/jcl/utility/UTLMNT.jcl`

```
//UTLMNT00 JOB (ACCT#),'FILE MAINTENANCE',CLASS=A,MSGCLASS=X
//STEP01   EXEC PGM=UTLMNT00
//STEPLIB  DD DSN=PROD.LOAD.LIBRARY,DISP=SHR
//CTLFILE  DD DSN=PROD.CONTROL.FILE,DISP=SHR
//ARCHFILE DD DSN=PROD.ARCHIVE.FILE,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(100,50),RLSE),DCB=(RECFM=VB,LRECL=32756,BLKSIZE=0)
//RPTFILE  DD DSN=PROD.MAINTENANCE.REPORT,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(10,5),RLSE),DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
```

---

## 3. UTLMON00 — System Monitoring Utility

### 3.1 Purpose & Business Function

UTLMON00 is a long-running monitoring utility that continuously tracks system health and performance across four resource domains: CPU, Memory, DASD (Direct Access Storage), and DB2 subsystem. It collects metrics at regular intervals, compares them against configurable thresholds, logs status information, and generates alerts when thresholds are breached. The program runs until hour 23 (11 PM), at which point it terminates gracefully.

### 3.2 Copybook Dependencies

| Copybook | Location                           | Purpose                                         |
|----------|------------------------------------|-------------------------------------------------|
| RTNCODE  | `src/copybook/common/RTNCODE.cpy` | Return code management area                     |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions             |
| DB2STAT  | (File Section COPY)               | DB2 statistics indexed file record layout       |

### 3.3 File I/O

| DD Name   | File Type   | Direction | Organization | Description                                     |
|-----------|-------------|-----------|-------------|-------------------------------------------------|
| MONCFG    | Sequential  | Input     | Sequential  | Monitoring configuration / threshold definitions|
| MONLOG    | Sequential  | Output    | Sequential  | Monitoring log (132-byte fixed records)         |
| ALERTS    | Sequential  | Output    | Sequential  | Alert output (132-byte fixed records)           |
| DB2STATS  | Indexed     | Input     | KSDS        | DB2 performance statistics file                 |

**Configuration Record Layout (91 bytes):**

| Field               | PIC          | Description                                      |
|---------------------|--------------|--------------------------------------------------|
| CFG-RESOURCE-TYPE   | X(10)        | Resource type (`CPU`, `MEMORY`, `DASD`, `DB2`)   |
| CFG-THRESHOLD-TYPE  | X(10)        | Threshold category (see below)                   |
| CFG-THRESHOLD-VALUE | 9(9)V99      | Threshold value                                  |
| CFG-ALERT-LEVEL     | X(10)        | Alert severity (`INFO`, `WARNING`, `CRITICAL`)   |
| CFG-ALERT-ACTION    | X(50)        | Action to take when threshold is breached        |

**Monitor Log Record Layout (77 bytes):**

| Field               | PIC          | Description                                      |
|---------------------|--------------|--------------------------------------------------|
| LOG-TIMESTAMP       | X(26)        | Timestamp of the metric collection               |
| LOG-RESOURCE-TYPE   | X(10)        | Resource type                                    |
| LOG-METRIC-NAME     | X(20)        | Name of the metric                               |
| LOG-METRIC-VALUE    | 9(9)V99      | Collected metric value                           |
| LOG-STATUS          | X(10)        | Status at time of collection                     |

**Alert Record Layout (126 bytes):**

| Field               | PIC          | Description                                      |
|---------------------|--------------|--------------------------------------------------|
| ALERT-TIMESTAMP     | X(26)        | Timestamp of the alert                           |
| ALERT-LEVEL         | X(10)        | Alert severity level                             |
| ALERT-RESOURCE      | X(10)        | Resource that triggered the alert                |
| ALERT-MESSAGE       | X(80)        | Descriptive alert message                        |

### 3.4 Resource Types (Monitoring Domains)

| Resource | Value     | Metrics Collected                                          |
|----------|-----------|------------------------------------------------------------|
| CPU      | `CPU`     | CPU utilization percentage (`WS-CPU-UTIL`)                 |
| MEMORY   | `MEMORY`  | Memory utilization percentage (`WS-MEMORY-UTIL`)           |
| DASD     | `DASD`    | DASD utilization percentage (`WS-DASD-UTIL`)               |
| DB2      | `DB2`     | DB2 utilization, response time, queue depth, error count   |

### 3.5 Threshold Types

| Threshold Type | Value      | Applicable Resources | Description                                   |
|----------------|------------|----------------------|-----------------------------------------------|
| UTILIZATION    | `UTIL`     | CPU, MEMORY, DASD, DB2 | Percentage utilization of the resource       |
| RESPONSE       | `RESPONSE` | DB2                  | Response time in hundredths of a second       |
| QUEUE          | `QUEUE`    | DB2                  | Queue depth (number of waiting requests)      |
| ERROR          | `ERROR`    | DB2                  | Error count within the monitoring interval    |

### 3.6 Alert Levels

| Level    | Value      | Typical Use                                                   |
|----------|------------|---------------------------------------------------------------|
| INFO     | `INFO`     | Informational notices — no action required                    |
| WARNING  | `WARNING`  | Approaching threshold — investigate proactively               |
| CRITICAL | `CRITICAL` | Threshold breached — immediate operator intervention required |

### 3.7 Current Metrics Working Storage

| Field           | PIC        | Description                                    |
|-----------------|------------|------------------------------------------------|
| WS-CPU-UTIL     | 9(3)V99    | Current CPU utilization (0–100.99%)            |
| WS-MEMORY-UTIL  | 9(3)V99    | Current memory utilization (0–100.99%)         |
| WS-DASD-UTIL    | 9(3)V99    | Current DASD utilization (0–100.99%)           |
| WS-DB2-UTIL     | 9(3)V99    | Current DB2 utilization (0–100.99%)            |
| WS-DB2-RESP     | 9(5)V99    | DB2 response time                              |
| WS-DB2-QUEUE    | 9(5)       | DB2 queue depth                                |
| WS-DB2-ERRORS   | 9(5)       | DB2 error count                                |

### 3.8 Processing Flow

```
0000-MAIN
 ├── 1000-INITIALIZE
 │    ├── 1100-OPEN-FILES (MONCFG, MONLOG, ALERTS, DB2STATS)
 │    ├── 1200-INIT-PROCESSING (accept current timestamp)
 │    └── 1300-READ-CONFIG (load all threshold definitions)
 ├── 2000-PROCESS (loop until WS-HOUR = 23)
 │    ├── 2100-COLLECT-METRICS
 │    │    ├── 2110-GET-CPU-METRICS
 │    │    ├── 2120-GET-MEMORY-METRICS
 │    │    ├── 2130-GET-DASD-METRICS
 │    │    └── 2140-GET-DB2-METRICS
 │    ├── 2200-CHECK-THRESHOLDS
 │    │    ├── 2210-CHECK-UTILIZATION
 │    │    ├── 2220-CHECK-RESPONSE
 │    │    ├── 2230-CHECK-QUEUES
 │    │    └── 2240-CHECK-ERRORS
 │    ├── 2300-LOG-STATUS
 │    │    ├── 2310-LOG-RESOURCES
 │    │    └── 2320-LOG-PERFORMANCE
 │    ├── 2400-GENERATE-ALERTS (if THRESHOLD-MET)
 │    │    ├── 2410-FORMAT-ALERT
 │    │    └── 2420-WRITE-ALERT
 │    └── CALL 'ILBOABN0' (wait interval), then refresh timestamp
 ├── 3000-CLEANUP (close all files)
 └── 9999-ERROR-HANDLER (RC 12, immediate GOBACK)
```

### 3.9 JCL Reference

**Job:** `src/jcl/utility/UTLMON.jcl`

```
//UTLMON00 JOB (ACCT#),'SYSTEM MONITOR',CLASS=A,MSGCLASS=X
//STEP01   EXEC PGM=UTLMON00
//STEPLIB  DD DSN=PROD.LOAD.LIBRARY,DISP=SHR
//MONCFG   DD DSN=PROD.MONITOR.CONFIG,DISP=SHR
//MONLOG   DD DSN=PROD.MONITOR.LOG,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(50,20),RLSE),DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//ALERTS   DD DSN=PROD.MONITOR.ALERTS,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(10,5),RLSE),DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
//DB2STATS DD DSN=PROD.DB2.STATISTICS,DISP=SHR
```

---

## 4. UTLVAL00 — Data Validation Utility

### 4.1 Purpose & Business Function

UTLVAL00 performs comprehensive data validation across the system's master files. It reads validation directives from a control file and executes four categories of validation: integrity checks (ensuring data consistency within individual files), cross-reference validation (verifying referential integrity across files), format verification (checking data format compliance), and balance reconciliation (verifying that calculated totals match control totals).

### 4.2 Copybook Dependencies

| Copybook | Location                           | Purpose                                         |
|----------|------------------------------------|-------------------------------------------------|
| RTNCODE  | `src/copybook/common/RTNCODE.cpy` | Return code management area                     |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions             |
| POSREC   | `src/copybook/common/POSREC.cpy`  | Position Master VSAM record layout              |
| TRNREC   | `src/copybook/common/TRNREC.cpy`  | Transaction History VSAM record layout          |

### 4.3 File I/O

| DD Name   | File Type   | Direction | Organization | Description                                            |
|-----------|-------------|-----------|-------------|--------------------------------------------------------|
| VALCTL    | Sequential  | Input     | Sequential  | Validation control file with directives                |
| POSMSTRE  | Indexed     | Input     | KSDS        | Position Master VSAM file                              |
| TRANHIST  | Indexed     | Input     | KSDS        | Transaction History VSAM file                          |
| ERRRPT    | Sequential  | Output    | Sequential  | Error report (132-byte fixed-length records)           |

**Validation Control Record Layout (80 bytes):**

| Field           | PIC     | Description                                         |
|-----------------|---------|-----------------------------------------------------|
| VAL-TYPE        | X(10)   | Validation type code                                |
| VAL-PARAMETERS  | X(70)   | Type-specific parameters                            |

### 4.4 Validation Types

| Type       | Value       | Paragraph              | Sub-checks                                             | Description                                                              |
|------------|-------------|------------------------|--------------------------------------------------------|--------------------------------------------------------------------------|
| INTEGRITY  | `INTEGRITY` | 2200-CHECK-INTEGRITY   | Position integrity + Transaction integrity             | Validates internal data consistency within each file                     |
| XREF       | `XREF`      | 2300-CHECK-XREF        | Position cross-ref + Transaction cross-ref             | Verifies referential integrity across Position Master and Transaction History |
| FORMAT     | `FORMAT`    | 2400-CHECK-FORMAT      | Position format + Transaction format                   | Checks that field values conform to expected formats and ranges          |
| BALANCE    | `BALANCE`   | 2500-CHECK-BALANCE     | Accumulate positions + Verify balances                 | Reconciles calculated totals against control totals                      |

### 4.5 Validation Totals (Working Storage)

| Field              | PIC            | Description                                        |
|--------------------|----------------|----------------------------------------------------|
| WS-RECORDS-READ    | 9(9)           | Total records processed                            |
| WS-RECORDS-VALID   | 9(9)           | Records passing validation                         |
| WS-RECORDS-ERROR   | 9(9)           | Records failing validation                         |
| WS-TOTAL-AMOUNT    | S9(15)V99      | Running total for balance checks                   |
| WS-CONTROL-TOTAL   | S9(15)V99      | Control total for reconciliation                   |

**Error Report Line Layout (132 bytes):**

| Field       | PIC     | Description                                      |
|-------------|---------|--------------------------------------------------|
| WS-ERR-TYPE | X(10)   | Validation type that detected the error          |
| WS-ERR-KEY  | X(20)   | Key of the record in error                       |
| WS-ERR-DESC | X(98)   | Description of the validation failure            |

### 4.6 Processing Flow

```
0000-MAIN
 ├── 1000-INITIALIZE
 │    ├── 1100-OPEN-FILES (VALCTL, POSMSTRE, TRANHIST, ERRRPT)
 │    └── 1200-INIT-PROCESSING (reset validation totals)
 ├── 2000-PROCESS (loop until end of VALCTL)
 │    └── 2100-PROCESS-VALIDATION (EVALUATE VAL-TYPE)
 │         ├── INTEGRITY → 2200-CHECK-INTEGRITY
 │         │    ├── 2210-CHECK-POSITION-INTEGRITY
 │         │    └── 2220-CHECK-TRANSACTION-INTEGRITY
 │         ├── XREF → 2300-CHECK-XREF
 │         │    ├── 2310-CHECK-POSITION-XREF
 │         │    └── 2320-CHECK-TRANSACTION-XREF
 │         ├── FORMAT → 2400-CHECK-FORMAT
 │         │    ├── 2410-CHECK-POSITION-FORMAT
 │         │    └── 2420-CHECK-TRANSACTION-FORMAT
 │         └── BALANCE → 2500-CHECK-BALANCE
 │              ├── 2510-ACCUMULATE-POSITIONS
 │              └── 2520-VERIFY-BALANCES
 ├── 3000-CLEANUP (close all files)
 └── 9999-ERROR-HANDLER (increment WS-RECORDS-ERROR, write error line)
```

### 4.7 Error Handling

- Each validation error sets the `ERROR-FOUND` flag and increments `WS-RECORDS-ERROR`.
- Errors are written to the Error Report file with the validation type, record key, and error description.
- The error handler does **not** terminate the program — processing continues to capture all errors.

### 4.8 JCL Reference

**Job:** `src/jcl/utility/UTLVAL.jcl`

```
//UTLVAL00 JOB (ACCT#),'DATA VALIDATION',CLASS=A,MSGCLASS=X
//STEP01   EXEC PGM=UTLVAL00
//STEPLIB  DD DSN=PROD.LOAD.LIBRARY,DISP=SHR
//VALCTL   DD DSN=PROD.VALIDATION.CONTROL,DISP=SHR
//POSMSTRE DD DSN=PROD.POSITION.MASTER,DISP=SHR
//TRANHIST DD DSN=PROD.TRANSACTION.HISTORY,DISP=SHR
//ERRRPT   DD DSN=PROD.VALIDATION.REPORT,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(10,5),RLSE),DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
```

---

## 5. TSTGEN00 — Test Data Generator

### 5.1 Purpose & Business Function

TSTGEN00 generates synthetic test data for system-level testing of the Investment Portfolio Management System. It reads a configuration file specifying the type and volume of data to generate, seeds a pseudo-random number generator for reproducibility, and writes portfolio and transaction records to sequential output files. The generated data is used as input for subsequent batch processing and validation test runs.

### 5.2 Copybook Dependencies

| Copybook | Location                           | Purpose                                                |
|----------|------------------------------------|---------------------------------------------------------|
| RTNCODE  | `src/copybook/common/RTNCODE.cpy` | Return code management area                            |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions                    |
| PORTFLIO | `src/copybook/common/PORTFLIO.cpy` | Portfolio record layout (via COPY REPLACING in FD)     |
| TRNREC   | `src/copybook/common/TRNREC.cpy`  | Transaction record layout (via COPY REPLACING in FD)   |

### 5.3 File I/O

| DD Name   | File Type   | Direction | Organization | Description                                      |
|-----------|-------------|-----------|-------------|--------------------------------------------------|
| TSTCFG    | Sequential  | Input     | Sequential  | Test configuration file                          |
| PORTOUT   | Sequential  | Output    | Sequential  | Generated portfolio test data                    |
| TRANOUT   | Sequential  | Output    | Sequential  | Generated transaction test data                  |
| RANDSEED  | Sequential  | Input     | Sequential  | Random seed file (9-digit numeric seed)          |

**Test Configuration Record Layout (80 bytes):**

| Field           | PIC     | Description                                                         |
|-----------------|---------|---------------------------------------------------------------------|
| CFG-TEST-TYPE   | X(10)   | Test data type code                                                 |
| CFG-VOLUME      | 9(6)    | Number of records to generate (up to 999,999)                       |
| CFG-PARAMETERS  | X(64)   | Type-specific generation parameters                                 |

### 5.4 Test Data Generation Strategy

| Scenario    | Value        | Paragraph               | Strategy                                                                              |
|-------------|--------------|--------------------------|--------------------------------------------------------------------------------------|
| PORTFOLIO   | `PORTFOLIO`  | 2200-GEN-PORTFOLIO       | Generates `CFG-VOLUME` portfolio records with random IDs, names, types, statuses, and balances |
| TRANSACTION | `TRANSACTN`  | 2300-GEN-TRANSACTION     | Generates `CFG-VOLUME` transaction records with random types (BU/SL/TR/FE), amounts, and dates |
| ERROR       | `ERROR`      | 2400-GEN-ERROR-DATA      | Generates intentionally malformed data for error-path testing (data errors + process errors) |
| VOLUME      | `VOLUME`     | 2500-GEN-VOLUME-DATA     | Generates high-volume datasets for performance/stress testing (large portfolio + large transaction sets) |

**Portfolio Data Working Storage:**

| Field           | PIC        | Description                              |
|-----------------|------------|------------------------------------------|
| WS-PORT-ID      | X(10)      | Generated portfolio identifier           |
| WS-PORT-NAME    | X(30)      | Generated portfolio name                 |
| WS-PORT-TYPE    | X(2)       | Portfolio type code                      |
| WS-PORT-STATUS  | X(1)       | Portfolio status                         |
| WS-PORT-BALANCE | 9(15)V99   | Portfolio balance                        |

**Transaction Data Working Storage:**

| Field           | PIC        | Description                              |
|-----------------|------------|------------------------------------------|
| WS-TRAN-ID      | X(12)      | Generated transaction identifier         |
| WS-TRAN-TYPE    | X(2)       | Transaction type (BU/SL/TR/FE)           |
| WS-TRAN-AMOUNT  | 9(15)V99   | Transaction amount                       |
| WS-TRAN-DATE    | X(8)       | Transaction date (YYYYMMDD)              |
| WS-TRAN-STATUS  | X(1)       | Transaction status                       |

**Random Number Generation:**

- The seed is read from the `RANDSEED` input file (`SEED-RECORD`, PIC 9(9)).
- Working storage provides `WS-RANDOM-SEED`, `WS-RANDOM-NUM`, and `WS-RANDOM-DECIMAL` for deterministic pseudo-random value generation.
- Using a file-based seed ensures test reproducibility across multiple runs.

### 5.5 Processing Flow

```
0000-MAIN
 ├── 1000-INITIALIZE
 │    ├── 1100-OPEN-FILES (TSTCFG, PORTOUT, TRANOUT, RANDSEED)
 │    ├── 1200-INIT-RANDOM (read seed file)
 │    └── 1300-INIT-COUNTERS
 ├── 2000-PROCESS (loop until end of TSTCFG)
 │    └── 2100-GENERATE-TEST-DATA (EVALUATE CFG-TEST-TYPE)
 │         ├── PORTFOLIO  → 2200-GEN-PORTFOLIO
 │         │    └── Loop CFG-VOLUME times:
 │         │         2210-GEN-PORT-DATA → 2220-WRITE-PORT-RECORD
 │         ├── TRANSACTN  → 2300-GEN-TRANSACTION
 │         │    └── Loop CFG-VOLUME times:
 │         │         2310-GEN-TRAN-DATA → 2320-WRITE-TRAN-RECORD
 │         ├── ERROR      → 2400-GEN-ERROR-DATA
 │         │    ├── 2410-GEN-DATA-ERRORS
 │         │    └── 2420-GEN-PROCESS-ERRORS
 │         └── VOLUME     → 2500-GEN-VOLUME-DATA
 │              ├── 2510-GEN-LARGE-PORTFOLIO
 │              └── 2520-GEN-LARGE-TRANSACTION
 ├── 3000-CLEANUP (close all files)
 └── 9999-ERROR-HANDLER (RC 12 after 100 errors)
```

### 5.6 JCL Reference

**Job:** `src/jcl/test/TSTGEN.jcl`

```
//TSTGEN00 JOB (ACCT#),'TEST DATA GEN',CLASS=A,MSGCLASS=X
//STEP01   EXEC PGM=TSTGEN00
//STEPLIB  DD DSN=TEST.LOAD.LIBRARY,DISP=SHR
//TSTCFG   DD DSN=TEST.CONFIG.FILE,DISP=SHR
//PORTOUT  DD DSN=TEST.PORTFOLIO.DATA,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(100,50),RLSE),DCB=(RECFM=FB,LRECL=100,BLKSIZE=0)
//TRANOUT  DD DSN=TEST.TRANSACTION.DATA,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(100,50),RLSE),DCB=(RECFM=FB,LRECL=100,BLKSIZE=0)
//RANDSEED DD DSN=TEST.RANDOM.SEED,DISP=SHR
```

> **Note:** Test programs use `TEST.LOAD.LIBRARY` (not `PROD.LOAD.LIBRARY`) to isolate test execution from production.

---

## 6. TSTVAL00 — Test Validation Suite

### 6.1 Purpose & Business Function

TSTVAL00 is the test validation framework for the Investment Portfolio Management System. It reads test case definitions along with corresponding expected and actual result files, executes each test, compares results, and produces a comprehensive test report. The suite supports four categories of tests: functional, integration, performance, and error-handling. It tracks pass/fail metrics and computes an overall success rate.

### 6.2 Copybook Dependencies

| Copybook | Location                           | Purpose                                   |
|----------|------------------------------------|--------------------------------------------|
| RTNCODE  | `src/copybook/common/RTNCODE.cpy` | Return code management area               |
| ERRHAND  | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions       |

### 6.3 File I/O

| DD Name   | File Type   | Direction | Organization | Description                                       |
|-----------|-------------|-----------|-------------|---------------------------------------------------|
| TESTCASE  | Sequential  | Input     | Sequential  | Test case definitions                             |
| EXPECTED  | Sequential  | Input     | Sequential  | Expected result records (200-byte fixed)          |
| ACTUAL    | Sequential  | Input     | Sequential  | Actual result records (200-byte fixed)            |
| TESTRPT   | Sequential  | Output    | Sequential  | Test validation report (132-byte fixed)           |

**Test Case Record Layout (170 bytes):**

| Field            | PIC      | Description                                    |
|------------------|----------|------------------------------------------------|
| TEST-ID          | X(10)    | Unique test case identifier                    |
| TEST-TYPE        | X(10)    | Test category code                             |
| TEST-DESCRIPTION | X(50)    | Human-readable test description                |
| TEST-PARAMETERS  | X(100)   | Test-specific execution parameters             |

### 6.4 Test Case Execution Framework

| Category     | Value        | Paragraph                       | Description                                                              |
|--------------|--------------|----------------------------------|--------------------------------------------------------------------------|
| FUNCTIONAL   | `FUNCTIONAL` | 2200-RUN-FUNCTIONAL-TEST        | Unit-level tests for individual business logic operations                |
| INTEGRATION  | `INTEGRATE`  | 2300-RUN-INTEGRATION-TEST       | End-to-end tests spanning multiple programs and data stores              |
| PERFORMANCE  | `PERFORM`    | 2400-RUN-PERFORMANCE-TEST       | Timing and throughput benchmarks under load                              |
| ERROR        | `ERROR`      | 2500-RUN-ERROR-TEST             | Tests for proper error detection, handling, and recovery                 |

**After executing each test, three additional paragraphs are always performed:**

1. **2600-VALIDATE-RESULTS** — Compares actual results against expected results.
2. **2700-UPDATE-METRICS** — Increments pass/fail counters.
3. **2800-WRITE-TEST-DETAIL** — Writes a detail line to the test report.

### 6.5 Test Metrics

| Field            | PIC     | Description                                 |
|------------------|---------|---------------------------------------------|
| WS-TOTAL-TESTS   | 9(5)   | Total number of test cases executed         |
| WS-TESTS-PASSED  | 9(5)   | Number of tests that passed                 |
| WS-TESTS-FAILED  | 9(5)   | Number of tests that failed                 |
| WS-START-TIME    | 9(8)   | Execution start time (from ACCEPT TIME)     |
| WS-END-TIME      | 9(8)   | Execution end time (from ACCEPT TIME)       |
| WS-ELAPSED-TIME  | 9(8)   | Computed elapsed time (end − start)         |

### 6.6 Report Output

**Report Header:** A line of asterisks followed by the title `TEST VALIDATION REPORT`.

**Detail Line Layout (132 bytes):**

| Field              | PIC    | Description                          |
|--------------------|--------|--------------------------------------|
| WS-TEST-ID-OUT     | X(10)  | Test case identifier                 |
| WS-TEST-TYPE-OUT   | X(10)  | Test category                        |
| WS-TEST-DESC-OUT   | X(50)  | Test description                     |
| WS-TEST-STATUS-OUT | X(4)   | Result (`PASS` / `FAIL`)            |

**Summary Line Layout:**

```
TOTAL TESTS: ZZ,ZZ9  PASSED: ZZ,ZZ9  FAILED: ZZ,ZZ9  SUCCESS: ZZ9.99%
```

### 6.7 Processing Flow

```
0000-MAIN
 ├── 1000-INITIALIZE
 │    ├── 1100-OPEN-FILES (TESTCASE, EXPECTED, ACTUAL, TESTRPT)
 │    ├── 1200-WRITE-HEADERS
 │    └── 1300-INIT-METRICS (reset counters, accept start time)
 ├── 2000-PROCESS (loop until end of TESTCASE)
 │    ├── 2100-EXECUTE-TEST (EVALUATE TEST-TYPE)
 │    │    ├── FUNCTIONAL  → 2200-RUN-FUNCTIONAL-TEST
 │    │    ├── INTEGRATE   → 2300-RUN-INTEGRATION-TEST
 │    │    ├── PERFORM     → 2400-RUN-PERFORMANCE-TEST
 │    │    └── ERROR       → 2500-RUN-ERROR-TEST
 │    ├── 2600-VALIDATE-RESULTS
 │    ├── 2700-UPDATE-METRICS
 │    └── 2800-WRITE-TEST-DETAIL
 │    (after loop:)
 │    └── 2900-WRITE-SUMMARY (compute success rate, write summary line)
 ├── 3000-CLEANUP (close all files)
 └── 9999-ERROR-HANDLER (RC 12, immediate GOBACK)
```

### 6.8 JCL Reference

**Job:** `src/jcl/test/TSTVAL.jcl`

```
//TSTVAL00 JOB (ACCT#),'TEST VALIDATION',CLASS=A,MSGCLASS=X
//STEP01   EXEC PGM=TSTVAL00
//STEPLIB  DD DSN=TEST.LOAD.LIBRARY,DISP=SHR
//TESTCASE DD DSN=TEST.CASE.FILE,DISP=SHR
//EXPECTED DD DSN=TEST.EXPECTED.RESULTS,DISP=SHR
//ACTUAL   DD DSN=TEST.ACTUAL.RESULTS,DISP=SHR
//TESTRPT  DD DSN=TEST.VALIDATION.REPORT,DISP=(NEW,CATLG,DELETE),
//            SPACE=(CYL,(10,5),RLSE),DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
```

---

## 7. System Monitoring Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    UTLMON00 — System Monitoring Utility                  │
│                                                                         │
│  ┌──────────────┐    ┌──────────────────────────────────────────────┐   │
│  │  MONITOR     │    │            2000-PROCESS (loop)               │   │
│  │  CONFIG      │───▶│                                              │   │
│  │  (MONCFG)    │    │  ┌────────────────────────────────────────┐  │   │
│  │              │    │  │  2100-COLLECT-METRICS                  │  │   │
│  │ Thresholds:  │    │  │  ┌────────┐ ┌────────┐ ┌────────┐    │  │   │
│  │ - UTIL       │    │  │  │  CPU   │ │ MEMORY │ │  DASD  │    │  │   │
│  │ - RESPONSE   │    │  │  │ Metrics│ │ Metrics│ │ Metrics│    │  │   │
│  │ - QUEUE      │    │  │  └────────┘ └────────┘ └────────┘    │  │   │
│  │ - ERROR      │    │  │  ┌──────────────────────────────┐     │  │   │
│  └──────────────┘    │  │  │   DB2 Metrics (from KSDS)    │     │  │   │
│                      │  │  │   - Utilization              │     │  │   │
│  ┌──────────────┐    │  │  │   - Response Time            │     │  │   │
│  │  DB2 STATS   │───▶│  │  │   - Queue Depth             │     │  │   │
│  │  (DB2STATS)  │    │  │  │   - Error Count             │     │  │   │
│  │  KSDS File   │    │  │  └──────────────────────────────┘     │  │   │
│  └──────────────┘    │  └────────────────────────────────────────┘  │   │
│                      │                                              │   │
│                      │  ┌────────────────────────────────────────┐  │   │
│                      │  │  2200-CHECK-THRESHOLDS                │  │   │
│                      │  │  Compare metrics vs. config values     │  │   │
│                      │  │  Set THRESHOLD-MET flag if breached    │  │   │
│                      │  └────────────────────────────────────────┘  │   │
│                      │          │                      │            │   │
│                      │          ▼                      ▼            │   │
│                      │  ┌──────────────┐   ┌───────────────────┐   │   │
│                      │  │ 2300-LOG     │   │ 2400-GENERATE     │   │   │
│                      │  │ STATUS       │   │ ALERTS            │   │   │
│                      │  └──────┬───────┘   └───────┬───────────┘   │   │
│                      │         │                   │               │   │
│                      └─────────┼───────────────────┼───────────────┘   │
│                                ▼                   ▼                    │
│                      ┌──────────────┐   ┌───────────────────┐          │
│                      │ MONITOR LOG  │   │    ALERT FILE     │          │
│                      │  (MONLOG)    │   │    (ALERTS)       │          │
│                      │              │   │                   │          │
│                      │ Timestamped  │   │ Level: INFO /     │          │
│                      │ metric log   │   │ WARNING / CRITICAL│          │
│                      └──────────────┘   └───────────────────┘          │
│                                                                         │
│  ▸ Runs continuously until hour 23 (11 PM)                             │
│  ▸ Uses ILBOABN0 for interval waits between cycles                     │
│  ▸ Timestamps via ACCEPT FROM TIME                                     │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 8. Test Data Generation Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                  TSTGEN00 — Test Data Generation Flow                 │
│                                                                      │
│  ┌──────────────┐     ┌──────────────┐                              │
│  │  RANDOM SEED │     │  TEST CONFIG │                              │
│  │  (RANDSEED)  │     │  (TSTCFG)    │                              │
│  │  9-digit     │     │              │                              │
│  │  numeric     │     │  Records:    │                              │
│  └──────┬───────┘     │  TYPE|VOLUME │                              │
│         │             │  |PARAMS     │                              │
│         │             └──────┬───────┘                              │
│         │                    │                                      │
│         ▼                    ▼                                      │
│  ┌─────────────────────────────────────────────────────────┐       │
│  │              1000-INITIALIZE                             │       │
│  │  Read seed → Initialize PRNG → Open output files        │       │
│  └─────────────────────────────────────────────────────────┘       │
│                           │                                        │
│                           ▼                                        │
│  ┌─────────────────────────────────────────────────────────┐       │
│  │  2100-GENERATE-TEST-DATA (EVALUATE CFG-TEST-TYPE)       │       │
│  │                                                         │       │
│  │  ┌─────────────┐  ┌─────────────┐  ┌────────────────┐  │       │
│  │  │  PORTFOLIO   │  │ TRANSACTN   │  │    ERROR       │  │       │
│  │  │             │  │             │  │                │  │       │
│  │  │ Generates:  │  │ Generates:  │  │ Generates:     │  │       │
│  │  │ - Port ID   │  │ - Tran ID   │  │ - Malformed    │  │       │
│  │  │ - Name      │  │ - Type      │  │   data fields  │  │       │
│  │  │ - Type (I/  │  │   (BU/SL/   │  │ - Invalid      │  │       │
│  │  │   C/T)      │  │   TR/FE)    │  │   formats      │  │       │
│  │  │ - Status    │  │ - Amount    │  │ - Out-of-range │  │       │
│  │  │ - Balance   │  │ - Date      │  │   values       │  │       │
│  │  │             │  │ - Status    │  │ - Process      │  │       │
│  │  │ Loop:       │  │             │  │   errors       │  │       │
│  │  │ CFG-VOLUME  │  │ Loop:       │  │                │  │       │
│  │  │ iterations  │  │ CFG-VOLUME  │  └────────────────┘  │       │
│  │  └──────┬──────┘  │ iterations  │  ┌────────────────┐  │       │
│  │         │         └──────┬──────┘  │    VOLUME      │  │       │
│  │         │                │         │                │  │       │
│  │         │                │         │ Generates:     │  │       │
│  │         │                │         │ - Large port-  │  │       │
│  │         │                │         │   folio sets   │  │       │
│  │         │                │         │ - Large tran-  │  │       │
│  │         │                │         │   saction sets │  │       │
│  │         │                │         │ (stress test)  │  │       │
│  │         │                │         └───────┬────────┘  │       │
│  └─────────┼────────────────┼─────────────────┼───────────┘       │
│            │                │                 │                    │
│            ▼                ▼                 ▼                    │
│  ┌──────────────┐  ┌──────────────┐  (both output files)          │
│  │ PORTFOLIO    │  │ TRANSACTION  │                               │
│  │ OUTPUT       │  │ OUTPUT       │                               │
│  │ (PORTOUT)    │  │ (TRANOUT)    │                               │
│  │              │  │              │                               │
│  │ Uses PORTFLIO│  │ Uses TRNREC  │                               │
│  │ copybook     │  │ copybook     │                               │
│  └──────────────┘  └──────────────┘                               │
│                                                                    │
│  ▸ Seed-based PRNG ensures reproducible test data                 │
│  ▸ Error scenario generates intentional data/process errors       │
│  ▸ Volume scenario generates high-volume datasets for perf tests  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 9. Validation Types Matrix

### 9.1 UTLVAL00 — Data Validation Matrix

| Validation Type | Target Files                   | Position Checks                                      | Transaction Checks                                   |
|-----------------|--------------------------------|------------------------------------------------------|------------------------------------------------------|
| **INTEGRITY**   | POSMSTRE, TRANHIST             | Key uniqueness, required fields populated, status codes valid | Key uniqueness, required fields populated, valid transaction types |
| **XREF**        | POSMSTRE ↔ TRANHIST            | Every position has a corresponding portfolio         | Every transaction references an existing position    |
| **FORMAT**      | POSMSTRE, TRANHIST             | Date formats (YYYYMMDD), numeric field ranges, currency codes (3-char ISO) | Date/time formats, amount precision, type codes (BU/SL/TR/FE) |
| **BALANCE**     | POSMSTRE, TRANHIST             | Sum of position market values matches control total  | Sum of transaction amounts reconciles with position changes |

### 9.2 TSTVAL00 — Test Category Matrix

| Test Category    | Code        | Scope                           | Validation Method                                    | Typical Test Cases                                      |
|------------------|-------------|----------------------------------|------------------------------------------------------|---------------------------------------------------------|
| **FUNCTIONAL**   | `FUNCTIONAL`| Single program/module            | Compare actual output to expected for a given input  | Buy/Sell processing, portfolio valuation, fee calculation |
| **INTEGRATION**  | `INTEGRATE` | Multi-program workflow           | End-to-end comparison of final state                 | Transaction → Position Update → History Load pipeline   |
| **PERFORMANCE**  | `PERFORM`   | Throughput & timing              | Elapsed time measurement against benchmarks          | Batch processing rate under volume load                 |
| **ERROR**        | `ERROR`     | Error paths & recovery           | Verify error detection and graceful handling          | Invalid data input, missing files, duplicate keys       |

---

## 10. DB2 & VSAM Data Definitions

### 10.1 DB2 Tables

All DB2 objects reside in the `POSMVP` database. DDL source files are in `src/database/db2/`.

#### 10.1.1 PORTFOLIO_MASTER

**Source:** `src/database/db2/db2-definitions.sql`

| Column           | Type          | Nullable | Description                              |
|------------------|---------------|----------|------------------------------------------|
| PORTFOLIO_ID     | CHAR(8)       | NOT NULL | Primary key                              |
| ACCOUNT_TYPE     | CHAR(2)       | NOT NULL | Account classification                   |
| BRANCH_ID        | CHAR(2)       | NOT NULL | Branch identifier                        |
| CLIENT_ID        | CHAR(10)      | NOT NULL | Client identifier                        |
| PORTFOLIO_NAME   | VARCHAR(50)   | NOT NULL | Descriptive name                         |
| CURRENCY_CODE    | CHAR(3)       | NOT NULL | ISO currency code                        |
| RISK_LEVEL       | CHAR(1)       | NOT NULL | Risk classification                      |
| STATUS           | CHAR(1)       | NOT NULL | A=Active, C=Closed, S=Suspended          |
| OPEN_DATE        | DATE          | NOT NULL | Date opened                              |
| CLOSE_DATE       | DATE          | Nullable | Date closed                              |
| LAST_MAINT_DATE  | TIMESTAMP     | NOT NULL | Last maintenance timestamp               |
| LAST_MAINT_USER  | VARCHAR(8)    | NOT NULL | Last maintenance user ID                 |

**Indexes:** `IDX_PORT_MASTER_CLIENT` on (CLIENT_ID, STATUS).

#### 10.1.2 INVESTMENT_POSITIONS

**Source:** `src/database/db2/db2-definitions.sql`

| Column           | Type           | Nullable | Description                             |
|------------------|----------------|----------|-----------------------------------------|
| PORTFOLIO_ID     | CHAR(8)        | NOT NULL | FK → PORTFOLIO_MASTER                   |
| INVESTMENT_ID    | CHAR(10)       | NOT NULL | Investment identifier                   |
| POSITION_DATE    | DATE           | NOT NULL | Position snapshot date                  |
| QUANTITY         | DECIMAL(18,4)  | NOT NULL | Holdings quantity                       |
| COST_BASIS       | DECIMAL(18,2)  | NOT NULL | Total cost basis                        |
| MARKET_VALUE     | DECIMAL(18,2)  | NOT NULL | Current market value                    |
| CURRENCY_CODE    | CHAR(3)        | NOT NULL | ISO currency code                       |
| LAST_MAINT_DATE  | TIMESTAMP      | NOT NULL | Last maintenance timestamp              |
| LAST_MAINT_USER  | VARCHAR(8)     | NOT NULL | Last maintenance user ID                |

**Primary Key:** (PORTFOLIO_ID, INVESTMENT_ID, POSITION_DATE).
**Index:** `IDX_POSITIONS_DATE` on (POSITION_DATE, PORTFOLIO_ID).

#### 10.1.3 TRANSACTION_HISTORY

**Source:** `src/database/db2/db2-definitions.sql`

| Column            | Type           | Nullable | Description                            |
|-------------------|----------------|----------|----------------------------------------|
| TRANSACTION_ID    | CHAR(20)       | NOT NULL | Primary key (YYYYMMDDHHMMSS + 6-digit seq) |
| PORTFOLIO_ID      | CHAR(8)        | NOT NULL | FK → PORTFOLIO_MASTER                  |
| TRANSACTION_DATE  | DATE           | NOT NULL | Trade date                             |
| TRANSACTION_TIME  | TIME           | NOT NULL | Trade time                             |
| INVESTMENT_ID     | CHAR(10)       | NOT NULL | Investment identifier                  |
| TRANSACTION_TYPE  | CHAR(2)        | NOT NULL | BU=Buy, SL=Sell, TR=Transfer, FE=Fee  |
| QUANTITY          | DECIMAL(18,4)  | NOT NULL | Transaction quantity                   |
| PRICE             | DECIMAL(18,4)  | NOT NULL | Transaction price                      |
| AMOUNT            | DECIMAL(18,2)  | NOT NULL | Transaction amount                     |
| CURRENCY_CODE     | CHAR(3)        | NOT NULL | ISO currency code                      |
| STATUS            | CHAR(1)        | NOT NULL | P=Processed, F=Failed, R=Reversed      |
| PROCESS_DATE      | TIMESTAMP      | NOT NULL | Processing timestamp                   |
| PROCESS_USER      | VARCHAR(8)     | NOT NULL | Processing user ID                     |

**Indexes:**
- `IDX_TRANS_HIST_PORT` on (PORTFOLIO_ID, TRANSACTION_DATE)
- `IDX_TRANS_HIST_DATE` on (TRANSACTION_DATE, PORTFOLIO_ID)

#### 10.1.4 POSHIST (Position History)

**Source:** `src/database/db2/POSHIST.sql`

Tablespace is range-partitioned by `TRANS_DATE` in quarterly intervals.

| Column           | Type           | Nullable | Description                              |
|------------------|----------------|----------|------------------------------------------|
| ACCOUNT_NO       | CHAR(8)        | NOT NULL | Account number                           |
| PORTFOLIO_ID     | CHAR(10)       | NOT NULL | Portfolio identifier                     |
| TRANS_DATE       | DATE           | NOT NULL | Transaction date                         |
| TRANS_TIME       | TIME           | NOT NULL | Transaction time                         |
| TRANS_TYPE       | CHAR(2)        | NOT NULL | BU=Buy, SL=Sell, TR=Transfer             |
| SECURITY_ID      | CHAR(12)       | NOT NULL | Security identifier                      |
| QUANTITY         | DECIMAL(15,3)  | NOT NULL | Transaction quantity                     |
| PRICE            | DECIMAL(15,3)  | NOT NULL | Transaction price                        |
| AMOUNT           | DECIMAL(15,2)  | NOT NULL | Transaction amount                       |
| FEES             | DECIMAL(15,2)  | NOT NULL | Fees (default 0)                         |
| TOTAL_AMOUNT     | DECIMAL(15,2)  | NOT NULL | Total including fees                     |
| COST_BASIS       | DECIMAL(15,2)  | NOT NULL | Cost basis amount                        |
| GAIN_LOSS        | DECIMAL(15,2)  | NOT NULL | Realized gain/loss                       |
| PROCESS_DATE     | DATE           | NOT NULL | Processing date                          |
| PROCESS_TIME     | TIME           | NOT NULL | Processing time                          |
| PROGRAM_ID       | CHAR(8)        | NOT NULL | Program that created the record          |
| USER_ID          | CHAR(8)        | NOT NULL | User ID                                  |
| AUDIT_TIMESTAMP  | TIMESTAMP      | NOT NULL | Audit timestamp (default CURRENT)        |

**Indexes:**
- `POSHIST_PK` on (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME) — clustered
- `POSHIST_IX1` on (SECURITY_ID, TRANS_DATE)
- `POSHIST_IX2` on (PROCESS_DATE, PROGRAM_ID)

#### 10.1.5 ERRLOG (Error Log)

**Source:** `src/database/db2/ERRLOG.sql`

| Column           | Type           | Nullable | Description                              |
|------------------|----------------|----------|------------------------------------------|
| ERROR_TIMESTAMP  | TIMESTAMP      | NOT NULL | When the error occurred                  |
| PROGRAM_ID       | CHAR(8)        | NOT NULL | Program that raised the error            |
| ERROR_TYPE       | CHAR(1)        | NOT NULL | S=System, A=Application, D=Data          |
| ERROR_SEVERITY   | INTEGER        | NOT NULL | 1=Info, 2=Warning, 3=Error, 4=Severe     |
| ERROR_CODE       | CHAR(8)        | NOT NULL | Error code                               |
| ERROR_MESSAGE    | VARCHAR(200)   | NOT NULL | Descriptive message                      |
| PROCESS_DATE     | DATE           | NOT NULL | Processing date                          |
| PROCESS_TIME     | TIME           | NOT NULL | Processing time                          |
| USER_ID          | CHAR(8)        | NOT NULL | User ID                                  |
| ADDITIONAL_INFO  | VARCHAR(500)   | Nullable | Extended diagnostic information          |

**Indexes:**
- `ERRLOG_PK` on (ERROR_TIMESTAMP, PROGRAM_ID) — clustered, unique
- `ERRLOG_IX1` on (PROCESS_DATE, ERROR_SEVERITY DESC)

**Stored Procedure:** `ERRLOG_CLEANUP(RETENTION_DAYS INTEGER)` — purges records older than the specified number of days.

**Grants:** SELECT + INSERT to `POSAPP`; SELECT to `POSRPT`.

#### 10.1.6 RTNCODES (Return Code Log)

**Source:** `src/database/db2/RTNCODES.sql`

| Column        | Type         | Nullable | Description                              |
|---------------|--------------|----------|------------------------------------------|
| TIMESTAMP     | TIMESTAMP    | NOT NULL | When the return code was logged          |
| PROGRAM_ID    | CHAR(8)      | NOT NULL | Program identifier                       |
| RETURN_CODE   | INTEGER      | NOT NULL | Return code value                        |
| HIGHEST_CODE  | INTEGER      | NOT NULL | Highest return code in the run           |
| STATUS_CODE   | CHAR(1)      | NOT NULL | Status classification                    |
| MESSAGE_TEXT  | VARCHAR(80)  | Nullable | Associated message                       |

**Indexes:**
- `RTNCODES_PRG_IDX` on (PROGRAM_ID, TIMESTAMP)
- `RTNCODES_STS_IDX` on (STATUS_CODE, TIMESTAMP)

#### 10.1.7 DB2 Plan

**Source:** `src/database/db2/PORTPLAN.sql`

```sql
BIND PLAN PORTPLAN
     PKLIST(*.PORTPKG.*)
     ACTION(REPLACE) RETAIN
     VALIDATE(RUN) ISOLATION(CS)
     ACQUIRE(USE) RELEASE(COMMIT)
     EXPLAIN(YES);
```

#### 10.1.8 DB2 Views

| View                | Source Table         | Filter                                                            |
|---------------------|----------------------|-------------------------------------------------------------------|
| ACTIVE_PORTFOLIOS   | PORTFOLIO_MASTER     | STATUS = 'A' AND (CLOSE_DATE IS NULL OR CLOSE_DATE > CURRENT DATE) |
| CURRENT_POSITIONS   | INVESTMENT_POSITIONS + PORTFOLIO_MASTER | POSITION_DATE = CURRENT DATE − 1 DAY   |

### 10.2 VSAM File Definitions

**Source:** `src/database/vsam/vsam-definitions.txt`

#### 10.2.1 PORTMSTR (Portfolio Master)

| Attribute       | Value       | Notes                                               |
|-----------------|-------------|------------------------------------------------------|
| Organization    | KSDS        | Key-Sequenced Data Set                               |
| Record Format   | Fixed       |                                                      |
| Record Length   | 400 bytes   |                                                      |
| Key Length      | 12 bytes    | Portfolio ID (8) + Account Type (2) + Branch ID (2)  |
| Key Position    | 1           |                                                      |
| CI Size         | 4096        |                                                      |
| Freespace       | CI-20, CA-20| Higher freespace for frequent updates                |
| Share Options   | (2,3)       | Read sharing during batch updates                    |
| Recovery        | Yes         | Recoverable for backup/restore                       |
| Buffer Space    | 65536       |                                                      |

#### 10.2.2 TRANHIST (Transaction History)

| Attribute       | Value       | Notes                                                          |
|-----------------|-------------|----------------------------------------------------------------|
| Organization    | KSDS        |                                                                |
| Record Format   | Fixed       |                                                                |
| Record Length   | 300 bytes   |                                                                |
| Key Length      | 20 bytes    | Date (8) + Time (6) + Portfolio ID (8) + Sequence (6) = 28 *  |
| Key Position    | 1           |                                                                |
| CI Size         | 4096        |                                                                |
| Freespace       | CI-10, CA-10| Lower freespace — mainly sequential writes                     |
| Share Options   | (2,3)       |                                                                |
| Recovery        | Yes         |                                                                |
| Buffer Space    | 65536       |                                                                |

> \* The VSAM definition specifies a 20-byte key; the TRNREC copybook defines a 28-byte composite key (TRN-KEY). The VSAM key covers the first 20 bytes of the record.

#### 10.2.3 POSHIST (Position History)

| Attribute       | Value       | Notes                                               |
|-----------------|-------------|------------------------------------------------------|
| Organization    | KSDS        |                                                      |
| Record Format   | Fixed       |                                                      |
| Record Length   | 350 bytes   |                                                      |
| Key Length      | 18 bytes    | Portfolio ID (8) + Position Date (8) + Investment ID first 2 bytes |
| Key Position    | 1           |                                                      |
| CI Size         | 4096        |                                                      |
| Freespace       | CI-10, CA-10|                                                      |
| Share Options   | (2,3)       |                                                      |
| Recovery        | Yes         |                                                      |
| Buffer Space    | 65536       |                                                      |

#### 10.2.4 IDCAMS DEFINE CLUSTER Example

```
DEFINE CLUSTER                                        -
       (NAME(PORTFOLIO.MASTER.FILE)                   -
        VOLUMES(VSAM01)                              -
        CYLINDERS(100 20)                            -
        KEYS(12 0)                                   -
        RECORDSIZE(400 400)                          -
        FREESPACE(20 20)                             -
        INDEXED                                      -
        SHAREOPTIONS(2 3))                           -
       DATA                                          -
       (NAME(PORTFOLIO.MASTER.FILE.DATA))            -
       INDEX                                         -
       (NAME(PORTFOLIO.MASTER.FILE.INDEX))
```

---

## 11. Appendix — Copybook Quick Reference

### 11.1 RTNCODE (Return Code Management)

**Source:** `src/copybook/common/RTNCODE.cpy`

Provides a standardized return code management area used by all utility and test programs.

| Field             | PIC / Type      | Description                                        |
|-------------------|-----------------|----------------------------------------------------|
| RC-REQUEST-TYPE   | X               | I=Initialize, S=Set, G=Get, L=Log, A=Analyze      |
| RC-PROGRAM-ID     | X(8)            | Calling program identifier                         |
| RC-CURRENT-CODE   | S9(4) COMP      | Current return code                                |
| RC-HIGHEST-CODE   | S9(4) COMP      | Highest return code encountered                    |
| RC-NEW-CODE       | S9(4) COMP      | New code to set                                    |
| RC-STATUS         | X               | S=Success, W=Warning, E=Error, F=Severe            |
| RC-MESSAGE        | X(80)           | Return code message                                |

### 11.2 ERRHAND (Error Handling)

**Source:** `src/copybook/common/ERRHAND.cpy`

Provides error categories, standard return codes, error message structures, and VSAM status handling.

**Error Categories:**

| Category  | Code | Description                     |
|-----------|------|---------------------------------|
| VSAM      | `VS` | VSAM file operation errors      |
| VALID     | `VL` | Validation errors               |
| PROC      | `PR` | Processing errors               |
| SYSTEM    | `SY` | System-level errors             |

**Standard Return Codes:**

| Severity  | Value | Description                     |
|-----------|-------|---------------------------------|
| SUCCESS   | 0     | Normal completion               |
| WARNING   | 4     | Warning — processing continued  |
| ERROR     | 8     | Error — partial processing      |
| SEVERE    | 12    | Severe error — processing halted|
| TERMINAL  | 16    | Terminal error — immediate abort|

**VSAM Status Codes:**

| Status | Message              |
|--------|----------------------|
| `00`   | Success              |
| `22`   | Duplicate record key |
| `23`   | Record not found     |
| `10`   | End of file          |

### 11.3 POSREC (Position Record)

**Source:** `src/copybook/common/POSREC.cpy`

Defines the Position Master VSAM record layout. Used by UTLVAL00 for validation.

| Field               | PIC             | Description                    |
|---------------------|-----------------|--------------------------------|
| POS-KEY             | —               | Composite key (26 bytes)       |
| — POS-PORTFOLIO-ID  | X(8)            | Portfolio identifier           |
| — POS-DATE          | X(8)            | Position date (YYYYMMDD)       |
| — POS-INVESTMENT-ID | X(10)           | Investment identifier          |
| POS-QUANTITY        | S9(11)V9(4) COMP-3 | Holding quantity            |
| POS-COST-BASIS      | S9(13)V9(2) COMP-3 | Total cost basis            |
| POS-MARKET-VALUE    | S9(13)V9(2) COMP-3 | Current market value        |
| POS-CURRENCY        | X(3)            | ISO currency code              |
| POS-STATUS          | X(1)            | A=Active, C=Closed, P=Pending  |

### 11.4 TRNREC (Transaction Record)

**Source:** `src/copybook/common/TRNREC.cpy`

Defines the Transaction History VSAM record layout. Used by UTLVAL00 and TSTGEN00.

| Field               | PIC             | Description                    |
|---------------------|-----------------|--------------------------------|
| TRN-KEY             | —               | Composite key (28 bytes)       |
| — TRN-DATE          | X(8)            | Transaction date (YYYYMMDD)    |
| — TRN-TIME          | X(6)            | Transaction time (HHMMSS)      |
| — TRN-PORTFOLIO-ID  | X(8)            | Portfolio identifier           |
| — TRN-SEQUENCE-NO   | X(6)            | Sequence number                |
| TRN-INVESTMENT-ID   | X(10)           | Investment identifier          |
| TRN-TYPE            | X(2)            | BU=Buy, SL=Sell, TR=Transfer, FE=Fee |
| TRN-QUANTITY        | S9(11)V9(4) COMP-3 | Transaction quantity        |
| TRN-PRICE           | S9(11)V9(4) COMP-3 | Transaction price           |
| TRN-AMOUNT          | S9(13)V9(2) COMP-3 | Transaction amount          |
| TRN-CURRENCY        | X(3)            | ISO currency code              |
| TRN-STATUS          | X(1)            | P=Pending, D=Done, F=Failed, R=Reversed |

### 11.5 PORTFLIO (Portfolio Record)

**Source:** `src/copybook/common/PORTFLIO.cpy`

Defines the Portfolio Master record layout. Used by TSTGEN00 for test data generation via `COPY REPLACING`.

| Field               | PIC             | Description                      |
|---------------------|-----------------|----------------------------------|
| PORT-KEY            | —               | Composite key (18 bytes)         |
| — PORT-ID           | X(8)            | Portfolio identifier             |
| — PORT-ACCOUNT-NO   | X(10)           | Account number                   |
| PORT-CLIENT-NAME    | X(30)           | Client name                      |
| PORT-CLIENT-TYPE    | X(1)            | I=Individual, C=Corporate, T=Trust |
| PORT-CREATE-DATE    | 9(8)            | Creation date                    |
| PORT-LAST-MAINT     | 9(8)            | Last maintenance date            |
| PORT-STATUS         | X(1)            | A=Active, C=Closed, S=Suspended  |
| PORT-TOTAL-VALUE    | S9(13)V99 COMP-3 | Total portfolio value           |
| PORT-CASH-BALANCE   | S9(13)V99 COMP-3 | Cash balance                    |
| PORT-LAST-USER      | X(8)            | Last maintenance user            |
| PORT-LAST-TRANS     | 9(8)            | Last transaction date            |

---

*This document is auto-generated from source analysis of the COBOL Legacy Benchmark Suite. For the overall system architecture, see [`system-architecture.md`](system-architecture.md). For field-level data definitions, see [`data-dictionary.md`](data-dictionary.md).*
