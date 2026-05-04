# Portfolio Management Layer — Technical Documentation

> **COBOL Legacy Benchmark Suite — Investment Portfolio Management System**

This document provides comprehensive technical documentation for the **Portfolio Management Layer**, which consists of 8 COBOL programs responsible for maintaining, querying, processing, testing, and validating portfolio records stored in a VSAM KSDS (Key-Sequenced Data Set).

---

## Table of Contents

1. [Layer Overview](#1-layer-overview)
2. [VSAM File Layout — Portfolio Master File](#2-vsam-file-layout--portfolio-master-file)
3. [Copybook Dependencies](#3-copybook-dependencies)
4. [Program Documentation](#4-program-documentation)
   - 4.1 [PORTMSTR — Portfolio Master File Maintenance](#41-portmstr--portfolio-master-file-maintenance)
   - 4.2 [PORTADD — Portfolio Addition Program](#42-portadd--portfolio-addition-program)
   - 4.3 [PORTUPDT — Portfolio Update Program](#43-portupdt--portfolio-update-program)
   - 4.4 [PORTDEL — Portfolio Deletion Program](#44-portdel--portfolio-deletion-program)
   - 4.5 [PORTREAD — Portfolio Record Reading](#45-portread--portfolio-record-reading)
   - 4.6 [PORTTRAN — Portfolio Transaction Processing](#46-porttran--portfolio-transaction-processing)
   - 4.7 [PORTTEST — Portfolio Test Data Generator](#47-porttest--portfolio-test-data-generator)
   - 4.8 [PORTVALD — Portfolio Validation Subroutine](#48-portvald--portfolio-validation-subroutine)
5. [Validation Rules Matrix](#5-validation-rules-matrix)
6. [Transaction Processing Flow](#6-transaction-processing-flow)
7. [Error Handling Approach](#7-error-handling-approach)
8. [JCL Reference](#8-jcl-reference)

---

## 1. Layer Overview

The Portfolio Management Layer provides full lifecycle management of investment portfolio records. It is structured as a set of cooperating batch programs and callable subroutines:

| Program    | Type          | Purpose                                      |
|------------|---------------|----------------------------------------------|
| PORTMSTR   | Subroutine    | CRUD dispatcher for portfolio records         |
| PORTADD    | Batch         | Bulk addition of portfolios from input file   |
| PORTUPDT   | Batch         | Bulk update of portfolio fields               |
| PORTDEL    | Batch         | Bulk deletion with audit trail                |
| PORTREAD   | Batch         | Sequential read and display of all records    |
| PORTTRAN   | Batch         | Financial transaction processing (Buy/Sell/Transfer/Fee) |
| PORTTEST   | Batch         | Test data generation                          |
| PORTVALD   | Subroutine    | Reusable field-level validation               |

All programs target **IBM z/OS** (`SOURCE-COMPUTER. IBM-ZOS`).

---

## 2. VSAM File Layout — Portfolio Master File

The portfolio master file is a **VSAM KSDS** (Key-Sequenced Data Set) defined via IDCAMS in `src/jcl/portfolio/PORTDEF.jcl`.

### VSAM Cluster Definition

```
DEFINE CLUSTER (NAME(PORTFOLIO.MASTER.FILE)
       INDEXED
       RECORDSIZE(200 200)
       KEYS(18 0)
       CYLINDERS(5 1)
       FREESPACE(10 10)
       SHAREOPTIONS(2 3))
```

| Attribute       | Value                      | Description                                     |
|-----------------|----------------------------|-------------------------------------------------|
| Cluster Name    | `PORTFOLIO.MASTER.FILE`    | High-level qualifier for the VSAM file          |
| Organization    | `INDEXED` (KSDS)           | Key-sequenced for random and sequential access   |
| Record Size     | 200 bytes (fixed)          | Average and maximum record length                |
| Primary Key     | 18 bytes at offset 0       | Composite: PORT-ID (8) + PORT-ACCOUNT-NO (10)   |
| Space           | 5 cylinders primary, 1 secondary | Allocation in cylinders                   |
| Free Space      | 10% CI / 10% CA            | Reserved for insertions                          |
| Share Options   | (2 3)                      | Cross-region: multiple readers, one writer; cross-system: full sharing |

### Record Layout Diagram

```
Offset  Length  Field               PIC Clause           Description
──────  ──────  ──────────────────  ───────────────────  ─────────────────────────────
  0       8     PORT-ID             X(8)                 Portfolio identifier (PORTnnnn)
  8      10     PORT-ACCOUNT-NO     X(10)                Account number (10 numeric digits)
 18      30     PORT-CLIENT-NAME    X(30)                Client display name
 48       1     PORT-CLIENT-TYPE    X(1)                 I=Individual, C=Corporate, T=Trust
 49       8     PORT-CREATE-DATE    9(8)                 Creation date (YYYYMMDD)
 57       8     PORT-LAST-MAINT     9(8)                 Last maintenance date (YYYYMMDD)
 65       1     PORT-STATUS         X(1)                 A=Active, C=Closed, S=Suspended
 66       8     PORT-TOTAL-VALUE    S9(13)V99 COMP-3     Portfolio total market value
 74       8     PORT-CASH-BALANCE   S9(13)V99 COMP-3     Available cash balance
 82       8     PORT-LAST-USER      X(8)                 Last user to modify the record
 90       8     PORT-LAST-TRANS     9(8)                 Last transaction date
 98      50     PORT-FILLER         X(50)                Reserved for future use
──────────────────────────────────────────────────────────────────────────────────────
Total: ~148 bytes of defined content + filler (record padded to 200 bytes)
```

> **Note:** The PORTFLIO copybook defines the 01-level `PORT-RECORD` used across most programs. PORTMSTR defines its own inline 100-byte record layout for the LINKAGE SECTION interface, while batch programs use the full 200-byte copybook layout.

### Composite Key Structure

```
┌──────────────────── PORT-KEY (18 bytes) ────────────────────┐
│  PORT-ID (8 bytes)  │  PORT-ACCOUNT-NO (10 bytes)           │
│  e.g. "PORT0001"    │  e.g. "1000000001"                    │
└─────────────────────┴───────────────────────────────────────┘
```

---

## 3. Copybook Dependencies

Each copybook resides in `src/copybook/common/` and provides shared data definitions:

### PORTFLIO.cpy — Portfolio Master Record Layout

Defines the standard `PORT-RECORD` (01-level) used by PORTADD, PORTUPDT, PORTDEL, PORTREAD, and PORTTEST.

**Key groups:**
- `PORT-KEY` — Composite primary key (PORT-ID + PORT-ACCOUNT-NO)
- `PORT-CLIENT-INFO` — Client name and type with level-88 conditions
- `PORT-PORTFOLIO-INFO` — Dates and status with level-88 conditions
- `PORT-FINANCIAL-INFO` — Total value and cash balance (COMP-3)
- `PORT-AUDIT-INFO` — Last user and transaction date

### PORTREC (referenced as `COPY PORTREC` in PORTTRAN)

Referenced by PORTTRAN for the portfolio file's FD section. Provides the same portfolio record structure used under the PORTFOLIO-FILE FD, likely an alias or project-local copy of the PORTFLIO layout with additional fields such as `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST` used in transaction processing.

### TRNREC.cpy — Transaction Record Structure

Defines `TRANSACTION-RECORD` for the transaction input file:

| Field               | PIC Clause         | Description                              |
|---------------------|--------------------|------------------------------------------|
| TRN-DATE            | X(8)               | Transaction date (YYYYMMDD)              |
| TRN-TIME            | X(6)               | Transaction time (HHMMSS)                |
| TRN-PORTFOLIO-ID    | X(8)               | Portfolio identifier                     |
| TRN-SEQUENCE-NO     | X(6)               | Sequence number for ordering             |
| TRN-INVESTMENT-ID   | X(10)              | Security/investment identifier           |
| TRN-TYPE            | X(2)               | BU=Buy, SL=Sell, TR=Transfer, FE=Fee    |
| TRN-QUANTITY        | S9(11)V9(4) COMP-3 | Number of units                          |
| TRN-PRICE           | S9(11)V9(4) COMP-3 | Price per unit                           |
| TRN-AMOUNT          | S9(13)V9(2) COMP-3 | Total transaction amount                 |
| TRN-CURRENCY        | X(3)               | ISO currency code                        |
| TRN-STATUS          | X(1)               | P=Pending, D=Done, F=Failed, R=Reversed |
| TRN-PROCESS-DATE    | X(26)              | Processing timestamp                     |
| TRN-PROCESS-USER    | X(8)               | Processing user ID                       |
| TRN-FILLER          | X(50)              | Reserved                                 |

### PORTVAL.cpy — Portfolio Validation Rules

Used exclusively by PORTVALD. Contains:

- **Return codes:** `VAL-SUCCESS` (0), `VAL-INVALID-ID` (1), `VAL-INVALID-ACCT` (2), `VAL-INVALID-TYPE` (3), `VAL-INVALID-AMT` (4)
- **Error messages:** Pre-defined text for each validation failure
- **Constants:** `VAL-ID-PREFIX` = `'PORT'`, `VAL-MIN-AMOUNT` = −9,999,999,999,999.99, `VAL-MAX-AMOUNT` = +9,999,999,999,999.99
- **Work areas:** `VAL-NUMERIC-CHECK`, `VAL-TEMP-NUM`, `VAL-ERROR-CODE`, `VAL-ERROR-MSG`

### ERRHAND.cpy — Standard Error Handling Definitions

Used by PORTTRAN and PORTTEST. Defines:

- **Error categories:** `VS` (VSAM), `VL` (Validation), `PR` (Processing), `SY` (System)
- **Severity codes:** 0=Success, 4=Warning, 8=Error, 12=Severe, 16=Terminal
- **Error message structure** (`ERR-MESSAGE`): timestamp, program, category, code, severity, text (80 bytes), details (256 bytes)
- **VSAM status mappings:** `00`=Success, `22`=Duplicate key, `23`=Not found, `10`=EOF

### AUDITLOG.cpy — Audit Trail Record Definitions

Used by PORTTRAN for audit trail logging. Defines the `AUDIT-RECORD` structure:

| Field             | PIC Clause | Description                              |
|-------------------|------------|------------------------------------------|
| AUD-TIMESTAMP     | X(26)      | ISO timestamp of the event               |
| AUD-SYSTEM-ID     | X(8)       | Originating system identifier            |
| AUD-USER-ID       | X(8)       | User who initiated the action            |
| AUD-PROGRAM       | X(8)       | Program name                             |
| AUD-TERMINAL      | X(8)       | Terminal identifier                      |
| AUD-TYPE          | X(4)       | TRAN / USER / SYST                      |
| AUD-ACTION        | X(8)       | CREATE / UPDATE / DELETE / INQUIRE / etc.|
| AUD-STATUS        | X(4)       | SUCC / FAIL / WARN                      |
| AUD-PORTFOLIO-ID  | X(8)       | Portfolio identifier                     |
| AUD-ACCOUNT-NO    | X(10)      | Account number                           |
| AUD-BEFORE-IMAGE  | X(100)     | Record state before the operation        |
| AUD-AFTER-IMAGE   | X(100)     | Record state after the operation         |
| AUD-MESSAGE       | X(100)     | Descriptive message                      |

---

## 4. Program Documentation

---

### 4.1 PORTMSTR — Portfolio Master File Maintenance

**Source:** `src/programs/portfolio/PORTMSTR.cbl`

#### Purpose & Business Function

PORTMSTR is the central CRUD dispatcher for portfolio records. It is designed as a **callable subroutine** (invoked via `CALL 'PORTMSTR' USING LS-COMMAND-AREA`) that receives a single-character command and performs the corresponding operation on the VSAM portfolio file.

#### VSAM File Definition

| Property        | Value                  |
|-----------------|------------------------|
| DD Name         | `PORTFILE`             |
| Organization    | INDEXED (KSDS)         |
| Access Mode     | DYNAMIC                |
| Record Key      | `PORT-ID`              |
| File Status     | `WS-PORT-STATUS`       |
| Record Size     | 100 bytes (inline FD)  |

> **Note:** PORTMSTR defines its own inline record layout (100 bytes) rather than using the PORTFLIO copybook. The inline layout includes: PORT-ID (X(10)), PORT-NAME (X(50)), PORT-CREATE-DATE (X(10)), PORT-STATUS (X(1)), PORT-TOTAL-VALUE (S9(13)V99 COMP-3), and FILLER (X(24)).

#### Copybook Dependencies

- None directly via `COPY` statements. However, the program references ERRHAND/AUDITLOG fields in its error-handling and audit-logging paragraphs (e.g., `ERR-CAT-VSAM`, `LS-AUDIT-REQUEST`), indicating linkage to those structures at the system level.

#### LINKAGE SECTION Interface

```
01  LS-COMMAND-AREA.
    05  LS-COMMAND          PIC X(1).     C=Create, R=Read, U=Update, D=Delete
    05  LS-PORTFOLIO        PIC X(100).   Portfolio record data
    05  LS-RETURN-CODE      PIC S9(4) COMP.
```

#### Processing Flow

| Paragraph               | Description                                              |
|--------------------------|----------------------------------------------------------|
| `0000-MAIN`              | Entry point; initializes, dispatches by command, terminates |
| `1000-INITIALIZE`        | Opens PORTFOLIO-FILE in I-O mode; accepts current date   |
| `2000-CREATE-PORTFOLIO`  | Validates data via `2100-VALIDATE-PORTFOLIO`, WRITEs record; checks for duplicate key (status '22') |
| `2100-VALIDATE-PORTFOLIO`| Checks PORT-ID starts with 'PORT' and has numeric suffix; PORT-NAME not blank; PORT-STATUS is 'A', 'I', or 'C' |
| `3000-READ-PORTFOLIO`    | READs by key; returns record via LINKAGE or errors on not-found ('23') |
| `4000-UPDATE-PORTFOLIO`  | Validates, REWRITEs record, then calls `2100-LOG-PORTFOLIO-UPDATE` for audit logging |
| `5000-DELETE-PORTFOLIO`  | DELETEs record by key; errors on not-found              |
| `6000-TERMINATE`         | CLOSEs file; propagates return code to linkage          |
| `9000-ERROR`             | Sets error return code and terminates                   |
| `2100-HANDLE-VSAM-ERROR` | Maps VSAM status codes to severity levels; calls ERRPROC |
| `2100-LOG-PORTFOLIO-UPDATE` | Builds AUDITLOG record with before/after images; calls AUDPROC |

#### Error Handling

- VSAM file status checked after every I/O operation
- Error text set to descriptive message before calling `9000-ERROR`
- `9000-ERROR` sets return code to `+8` and terminates with GOBACK
- Example VSAM error handler maps statuses ('22', '23') to ERR severity levels and calls `ERRPROC`

---

### 4.2 PORTADD — Portfolio Addition Program

**Source:** `src/programs/portfolio/PORTADD.cbl`

#### Purpose & Business Function

PORTADD is a **batch program** that reads new portfolio records from a sequential input file and adds them to the VSAM portfolio master file. It validates each record before insertion and tracks counts of successful additions, duplicates, and errors.

#### VSAM File Definition

| Property        | Value                  |
|-----------------|------------------------|
| DD Name         | `PORTFILE`             |
| Organization    | INDEXED (KSDS)         |
| Access Mode     | RANDOM                 |
| Record Key      | `PORT-KEY`             |
| File Status     | `WS-FILE-STATUS`       |

#### Additional Files

| DD Name    | Organization | Access     | Description              |
|------------|-------------|------------|--------------------------|
| `INPTFILE` | SEQUENTIAL  | INPUT      | New portfolio records    |

#### Copybook Dependencies

- **PORTFLIO** — Used for both the portfolio file FD and the input file FD record layouts

#### Processing Flow

| Paragraph           | Description                                              |
|----------------------|----------------------------------------------------------|
| `0000-MAIN`          | Initialize → Process until EOF → Terminate               |
| `1000-INITIALIZE`    | Opens files; validates file statuses; accepts date       |
| `2000-PROCESS`       | Reads next input record; dispatches to validation        |
| `2100-VALIDATE-AND-ADD` | Validates PORT-ID not blank, PORT-CLIENT-NAME not blank, PORT-STATUS = 'A'; sets creation/maintenance dates; WRITEs record |
| `3000-TERMINATE`     | Closes files; displays summary counts                    |

#### Validation Rules

- `PORT-ID` must not be SPACES
- `PORT-CLIENT-NAME` must not be SPACES
- `PORT-STATUS` must equal `'A'` (Active)
- Creation and last-maintenance dates set to current system date

#### Error Handling

- File status checked after OPEN; terminates on failure
- WRITE results evaluated: '00' = success, '22' = duplicate (counted), other = error (counted)
- Summary counts displayed at termination

---

### 4.3 PORTUPDT — Portfolio Update Program

**Source:** `src/programs/portfolio/PORTUPDT.cbl`

#### Purpose & Business Function

PORTUPDT is a **batch program** that reads update requests from a sequential file and applies field-level changes to existing portfolio records. Each update request specifies which field to modify (status, name, or value).

#### VSAM File Definition

| Property        | Value                  |
|-----------------|------------------------|
| DD Name         | `PORTFILE`             |
| Organization    | INDEXED (KSDS)         |
| Access Mode     | RANDOM                 |
| Record Key      | `PORT-KEY`             |
| File Status     | `WS-FILE-STATUS`       |

#### Additional Files

| DD Name    | Organization | Access     | Description              |
|------------|-------------|------------|--------------------------|
| `UPDTFILE` | SEQUENTIAL  | INPUT      | Update request records   |

#### Copybook Dependencies

- **PORTFLIO** — Used for the portfolio file FD

#### Update Record Layout

```
01  UPDATE-RECORD.
    05  UPDT-KEY.
        10  UPDT-ID        PIC X(8).      Portfolio ID
        10  UPDT-ACCT-NO   PIC X(10).     Account number
    05  UPDT-ACTION        PIC X(1).      S=Status, V=Value, N=Name
    05  UPDT-NEW-VALUE     PIC X(50).     New field value
```

#### Processing Flow

| Paragraph            | Description                                              |
|-----------------------|----------------------------------------------------------|
| `0000-MAIN`           | Initialize → Process until EOF → Terminate               |
| `1000-INITIALIZE`     | Opens files; validates statuses                          |
| `2000-PROCESS`        | Reads next update record                                 |
| `2100-PROCESS-UPDATE` | Moves update key to PORT-KEY; READs portfolio record     |
| `2200-APPLY-UPDATE`   | Evaluates UPDT-ACTION: 'S' → updates PORT-STATUS, 'N' → updates PORT-CLIENT-NAME, 'V' → converts and updates PORT-TOTAL-VALUE; REWRITEs record |
| `3000-TERMINATE`      | Closes files; displays update and error counts           |

#### Error Handling

- File status checked after OPEN
- READ failure: increments error count, displays key
- REWRITE failure: increments error count, displays key
- Summary counts displayed at termination

---

### 4.4 PORTDEL — Portfolio Deletion Program

**Source:** `src/programs/portfolio/PORTDEL.cbl`

#### Purpose & Business Function

PORTDEL is a **batch program** that processes portfolio deletion requests from a sequential file. It physically deletes records from the VSAM file and writes an audit trail for each deletion with a reason code.

#### VSAM File Definition

| Property        | Value                  |
|-----------------|------------------------|
| DD Name         | `PORTFILE`             |
| Organization    | INDEXED (KSDS)         |
| Access Mode     | RANDOM                 |
| Record Key      | `PORT-KEY`             |
| File Status     | `WS-FILE-STATUS`       |

#### Additional Files

| DD Name    | Organization | Access     | Description               |
|------------|-------------|------------|---------------------------|
| `DELEFILE` | SEQUENTIAL  | INPUT      | Deletion request records  |
| `AUDFILE`  | SEQUENTIAL  | OUTPUT     | Audit trail records       |

#### Copybook Dependencies

- **PORTFLIO** — Used for the portfolio file FD

#### Delete Request Record Layout

```
01  DELETE-RECORD.
    05  DEL-KEY.
        10  DEL-ID          PIC X(8).      Portfolio ID
        10  DEL-ACCT-NO     PIC X(10).     Account number
    05  DEL-REASON-CODE     PIC X(2).      01=Closed, 02=Transferred, 03=Requested
    05  DEL-FILLER          PIC X(60).
```

#### Deletion Reason Codes

| Code | Level-88 Name     | Meaning                 |
|------|-------------------|-------------------------|
| `01` | `DEL-CLOSED`      | Account closed          |
| `02` | `DEL-TRANSFERRED` | Portfolio transferred   |
| `03` | `DEL-REQUESTED`   | Client-requested deletion |

#### Audit Record Layout (Inline)

```
01  AUDIT-RECORD.
    05  AUD-TIMESTAMP      PIC X(26).
    05  AUD-ACTION         PIC X(6).       Always 'DELETE'
    05  AUD-KEY             PIC X(18).      PORT-KEY value
    05  AUD-REASON          PIC X(2).       Reason code
    05  AUD-STATUS          PIC X(1).       Portfolio status at time of deletion
    05  AUD-FILLER          PIC X(27).
```

#### Processing Flow

| Paragraph           | Description                                              |
|----------------------|----------------------------------------------------------|
| `0000-MAIN`          | Initialize → Process until EOF → Terminate               |
| `1000-INITIALIZE`    | Opens PORTFOLIO-FILE (I-O), DELETE-FILE (INPUT), AUDIT-FILE (OUTPUT) |
| `2000-PROCESS`       | Reads next delete request                                |
| `2100-PROCESS-DELETE` | Moves DEL-KEY to PORT-KEY; READs record; dispatches based on status |
| `2200-DELETE-RECORD` | DELETEs record; on success writes audit; on failure increments errors |
| `2300-WRITE-AUDIT`   | Accepts timestamp; writes audit record with action, key, reason, and status |
| `3000-TERMINATE`     | Closes all files; displays deleted, not-found, and error counts |

#### Error Handling

- Triple file status check after OPEN
- READ status evaluated: '00' → delete, '23' → not-found count, other → error count
- DELETE failure increments error count
- Audit write failure logged but does not stop processing

---

### 4.5 PORTREAD — Portfolio Record Reading

**Source:** `src/programs/portfolio/PORTREAD.cbl`

#### Purpose & Business Function

PORTREAD is a **batch program** that sequentially reads and displays all records in the VSAM portfolio master file. It is primarily used for reporting and verification purposes.

#### VSAM File Definition

| Property        | Value                  |
|-----------------|------------------------|
| DD Name         | `PORTFILE`             |
| Organization    | INDEXED (KSDS)         |
| Access Mode     | DYNAMIC                |
| Record Key      | `PORT-KEY`             |
| File Status     | `WS-FILE-STATUS`       |

> **Note:** The file is opened for INPUT only; DYNAMIC access mode enables sequential reading via `READ NEXT`.

#### Copybook Dependencies

- **PORTFLIO** — Used for the portfolio file FD

#### Processing Flow

| Paragraph           | Description                                                          |
|----------------------|----------------------------------------------------------------------|
| `0000-MAIN`          | Initialize → Process until EOF → Terminate                           |
| `1000-INITIALIZE`    | Opens PORTFOLIO-FILE for INPUT; validates file status                |
| `2000-PROCESS`       | Reads next record sequentially (`READ PORTFOLIO-FILE NEXT RECORD`)   |
| `2100-DISPLAY-RECORD`| Displays: PORT-ID, PORT-ACCOUNT-NO, PORT-CLIENT-NAME, PORT-STATUS, PORT-TOTAL-VALUE |
| `3000-TERMINATE`     | Closes file; displays total record count                             |

#### Output Fields Displayed

- Portfolio ID
- Account Number
- Client Name
- Status
- Total Value

#### Error Handling

- File status checked after OPEN; terminates on failure
- Return code propagated to system via `RETURN-CODE`

---

### 4.6 PORTTRAN — Portfolio Transaction Processing

**Source:** `src/programs/portfolio/PORTTRAN.cbl`

#### Purpose & Business Function

PORTTRAN is the **core batch transaction processor** for the portfolio system. It reads financial transactions from a sequential file, validates each transaction, updates portfolio positions, and writes a comprehensive audit trail.

#### VSAM File Definitions

| File             | DD Name    | Organization | Access Mode | Record Key | File Status       |
|------------------|------------|-------------|-------------|------------|-------------------|
| Transaction File | `TRANFILE` | SEQUENTIAL  | SEQUENTIAL  | N/A        | `WS-TRAN-STATUS`  |
| Portfolio File   | `PORTFILE` | INDEXED     | RANDOM      | `PORT-ID`  | `WS-PORT-STATUS`  |

#### Copybook Dependencies

- **TRNREC** — Transaction record layout (TRANSACTION-FILE FD)
- **PORTREC** — Portfolio record layout (PORTFOLIO-FILE FD)
- **ERRHAND** — Error handling definitions (WORKING-STORAGE)
- **AUDITLOG** — Audit trail record structure (WORKING-STORAGE)

#### Transaction Types

| Code | Level-88 Name    | Operation                                   | Position Update Logic                          |
|------|------------------|---------------------------------------------|------------------------------------------------|
| `BU` | `TRN-TYPE-BUY`  | Purchase securities                         | ADD TRN-QUANTITY to PORT-TOTAL-UNITS; ADD TRN-AMOUNT to PORT-TOTAL-COST |
| `SL` | `TRN-TYPE-SELL`  | Sell securities                             | SUBTRACT TRN-QUANTITY from PORT-TOTAL-UNITS; SUBTRACT TRN-AMOUNT from PORT-TOTAL-COST |
| `TR` | `TRN-TYPE-TRANS` | Transfer between portfolios                 | Not yet implemented (logs error)               |
| `FE` | `TRN-TYPE-FEE`   | Apply fee/charge                            | SUBTRACT TRN-AMOUNT from PORT-TOTAL-COST       |

#### Processing Flow

| Paragraph                  | Description                                              |
|-----------------------------|----------------------------------------------------------|
| `0000-MAIN`                 | Initialize → Process until EOF or >100 errors → Terminate |
| `1000-INITIALIZE`           | Opens both files; initializes counters and EOF flag      |
| `2000-PROCESS-TRANSACTIONS` | Reads next transaction; increments read count            |
| `2100-VALIDATE-TRANSACTION` | Three-phase validation: portfolio, type, amounts         |
| `2110-CHECK-PORTFOLIO`      | Validates portfolio ID not blank; verifies record exists via READ |
| `2120-CHECK-TRANSACTION-TYPE` | Validates TRN-TYPE is one of: BU, SL, TR, FE           |
| `2130-CHECK-AMOUNTS`        | Quantity > 0; Price > 0 (except TR); Amount > 0 (except TR) |
| `2200-UPDATE-POSITIONS`     | Dispatches to type-specific handler; then writes audit   |
| `2210-PROCESS-BUY`          | Reads portfolio; adds quantity and amount; rewrites      |
| `2220-PROCESS-SELL`         | Reads portfolio; checks sufficient units; subtracts; rewrites |
| `2230-PROCESS-TRANSFER`     | Stub — logs "not implemented" error                      |
| `2240-PROCESS-FEE`          | Reads portfolio; subtracts amount from cost; rewrites    |
| `2300-UPDATE-AUDIT-TRAIL`   | Builds audit record with timestamp, user, action, key, before-image, and message |
| `2310-WRITE-AUDIT-RECORD`   | Calls `AUDPROC` external program                         |
| `3000-TERMINATE`            | Closes files; displays summary counts                    |
| `9000-ERROR-ROUTINE`        | Increments error count; sets category; calls `ERRPROC`   |

#### Safety Mechanisms

- **Error threshold:** Processing halts if `WS-ERROR-COUNT > 100`
- **Sell validation:** Checks `PORT-TOTAL-UNITS >= TRN-QUANTITY` before proceeding
- **Audit trail:** Every successful transaction is logged with before-image of the portfolio record

---

### 4.7 PORTTEST — Portfolio Test Data Generator

**Source:** `src/programs/portfolio/PORTTEST.cbl`

#### Purpose & Business Function

PORTTEST generates synthetic portfolio records for testing purposes. It creates up to 100 test records with randomized field values and writes them to a sequential output file that can subsequently be loaded via PORTADD.

#### File Definition

| DD Name    | Organization | Access     | Description              |
|------------|-------------|------------|--------------------------|
| `TESTFILE` | SEQUENTIAL  | OUTPUT     | Generated test records   |

#### Copybook Dependencies

- **PORTFLIO** — Used for the test file FD record layout
- **ERRHAND** — Error handling definitions

#### Test Data Generation Logic

| Paragraph                 | Description                                              |
|----------------------------|----------------------------------------------------------|
| `0000-MAIN`                | Initialize → Generate records until count >= 100 → Terminate |
| `1000-INITIALIZE`          | Accepts current date; opens test file                    |
| `2000-GENERATE-RECORDS`    | Initializes PORT-RECORD; calls sub-generators; writes    |
| `2100-GENERATE-KEY`        | PORT-ID = `'PORT' + record_count`; PORT-ACCOUNT-NO = `record_count + 1000000000` |
| `2200-GENERATE-CLIENT-INFO`| PORT-CLIENT-NAME = `'TEST' + record_count`; PORT-CLIENT-TYPE cycles through I/C/T |
| `2300-GENERATE-PORTFOLIO-INFO` | Dates set to current date; PORT-STATUS cycles through A/C/S |
| `2400-GENERATE-FINANCIAL-INFO` | PORT-TOTAL-VALUE = random × 1,000,000; PORT-CASH-BALANCE = 10% of total value |
| `3000-TERMINATE`           | Closes file; displays count                              |

#### Generated Data Characteristics

| Field             | Generation Rule                                  |
|-------------------|--------------------------------------------------|
| PORT-ID           | `PORTnnnnn` (sequential from 0)                 |
| PORT-ACCOUNT-NO   | `1000000000 + n`                                 |
| PORT-CLIENT-NAME  | `TESTnnnnn`                                      |
| PORT-CLIENT-TYPE  | Cycles through `I` (Individual), `C` (Corporate), `T` (Trust) |
| PORT-STATUS       | Random selection from `A` (Active), `C` (Closed), `S` (Suspended) |
| PORT-TOTAL-VALUE  | Random value 0–1,000,000                         |
| PORT-CASH-BALANCE | 10% of PORT-TOTAL-VALUE                          |

---

### 4.8 PORTVALD — Portfolio Validation Subroutine

**Source:** `src/programs/portfolio/PORTVALD.cbl`

#### Purpose & Business Function

PORTVALD is a **reusable callable subroutine** that performs field-level validation on portfolio data elements. It is invoked via `CALL 'PORTVALD' USING LS-VALIDATION-REQUEST` and returns a validation result code with an error message.

#### Copybook Dependencies

- **PORTVAL** — Validation rules, constants, error messages, and work areas

#### LINKAGE SECTION Interface

```
01  LS-VALIDATION-REQUEST.
    05  LS-VALIDATE-TYPE    PIC X(1).     I=ID, A=Account, T=Type, M=Amount
    05  LS-INPUT-VALUE      PIC X(50).    Value to validate
    05  LS-RETURN-CODE      PIC S9(4) COMP.  Result code
    05  LS-ERROR-MSG        PIC X(50).    Error message (spaces if valid)
```

#### Processing Flow

| Paragraph            | Description                                              |
|-----------------------|----------------------------------------------------------|
| `0000-MAIN`           | Initializes work areas; dispatches by validation type    |
| `1000-VALIDATE-ID`    | Checks ID starts with 'PORT' and positions 5–8 are numeric |
| `2000-VALIDATE-ACCOUNT` | Checks input is all numeric and not all zeros           |
| `3000-VALIDATE-TYPE`  | Checks input is one of: STK, BND, MMF, ETF              |
| `4000-VALIDATE-AMOUNT`| Converts to numeric; checks within min/max range         |

#### Validation Details

See [Section 5 — Validation Rules Matrix](#5-validation-rules-matrix) for the complete rule set.

---

## 5. Validation Rules Matrix

### PORTVALD Validation Rules

| Type Code | Validation Name    | Input Field    | Rule                                                | Return Code | Error Message                  |
|-----------|--------------------|----------------|-----------------------------------------------------|-------------|-------------------------------|
| `I`       | Portfolio ID       | LS-INPUT-VALUE | Positions 1–4 must be `'PORT'`; positions 5–8 must be numeric | `+1` (VAL-INVALID-ID)   | `'Invalid Portfolio ID format'` |
| `A`       | Account Number     | LS-INPUT-VALUE | Must be fully numeric; must not be all zeros         | `+2` (VAL-INVALID-ACCT) | `'Invalid Account Number format'` |
| `T`       | Investment Type    | LS-INPUT-VALUE | Must be one of: `'STK'`, `'BND'`, `'MMF'`, `'ETF'` | `+3` (VAL-INVALID-TYPE) | `'Invalid Investment Type'`     |
| `M`       | Amount Range       | LS-INPUT-VALUE | Must be between −9,999,999,999,999.99 and +9,999,999,999,999.99 | `+4` (VAL-INVALID-AMT)  | `'Amount outside valid range'`  |
| (other)   | Invalid type       | —              | Validation type not recognized                       | `+1` (VAL-INVALID-ID)   | `'Invalid validation type'`     |

### PORTMSTR Inline Validation (2100-VALIDATE-PORTFOLIO)

| Field        | Rule                                               |
|--------------|-----------------------------------------------------|
| PORT-ID      | First 4 characters = `'PORT'`; next 5 characters numeric |
| PORT-NAME    | Must not be SPACES                                  |
| PORT-STATUS  | Must be `'A'` (Active), `'I'` (Inactive), or `'C'` (Closed) |

### PORTADD Inline Validation (2100-VALIDATE-AND-ADD)

| Field            | Rule                         |
|------------------|------------------------------|
| PORT-ID          | Must not be SPACES           |
| PORT-CLIENT-NAME | Must not be SPACES           |
| PORT-STATUS      | Must equal `'A'`             |

### PORTTRAN Transaction Validation

| Check             | Rule                                                         |
|-------------------|--------------------------------------------------------------|
| Portfolio ID      | Must not be SPACES; must exist in VSAM file                  |
| Transaction Type  | Must be `'BU'`, `'SL'`, `'TR'`, or `'FE'`                   |
| Quantity          | Must be greater than zero                                    |
| Price             | Must be greater than zero (except for Transfer `'TR'`)       |
| Amount            | Must be greater than zero (except for Transfer `'TR'`)       |
| Sell sufficiency  | `PORT-TOTAL-UNITS >= TRN-QUANTITY` (checked during sell processing) |

### Investment Type Codes

| Code  | Description            |
|-------|------------------------|
| `STK` | Stock                  |
| `BND` | Bond                   |
| `MMF` | Money Market Fund      |
| `ETF` | Exchange-Traded Fund   |

### Portfolio Status Codes

| Code | Description | Programs That Accept       |
|------|-------------|----------------------------|
| `A`  | Active      | PORTMSTR, PORTADD, PORTVALD |
| `C`  | Closed      | PORTMSTR                    |
| `I`  | Inactive    | PORTMSTR                    |
| `S`  | Suspended   | PORTFLIO copybook (level-88) |

### Client Type Codes

| Code | Description   |
|------|---------------|
| `I`  | Individual    |
| `C`  | Corporate     |
| `T`  | Trust         |

---

## 6. Transaction Processing Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    PORTTRAN — Main Flow                          │
└─────────────────────────────────────────────────────────────────┘

    ┌──────────────┐
    │ 1000-INIT    │  Open TRANFILE (INPUT), PORTFILE (I-O)
    └──────┬───────┘
           │
           ▼
    ┌──────────────────────────────────────────────┐
    │ 2000-PROCESS-TRANSACTIONS (loop until EOF     │
    │                           or errors > 100)    │
    └──────┬───────────────────────────────────────┘
           │
           ▼
    ┌──────────────┐
    │ READ next    │  Increment WS-READ-COUNT
    │ transaction  │
    └──────┬───────┘
           │
           ▼
    ┌──────────────────────────────────────────────┐
    │ 2100-VALIDATE-TRANSACTION                     │
    │                                               │
    │  ┌─────────────────┐                          │
    │  │ 2110-CHECK-     │  Portfolio ID not blank   │
    │  │ PORTFOLIO       │  Record exists in VSAM    │
    │  └────────┬────────┘                          │
    │           │ (if valid)                         │
    │  ┌────────▼────────┐                          │
    │  │ 2120-CHECK-     │  Type in {BU,SL,TR,FE}   │
    │  │ TRANSACTION-TYPE│                           │
    │  └────────┬────────┘                          │
    │           │ (if valid)                         │
    │  ┌────────▼────────┐                          │
    │  │ 2130-CHECK-     │  Qty > 0                  │
    │  │ AMOUNTS         │  Price > 0 (not TR)       │
    │  └────────┬────────┘  Amount > 0 (not TR)      │
    └───────────┼──────────────────────────────────┘
                │
       ┌────────▼────────┐
       │ All valid?       │
       ├─── YES ──────────┤─── NO ───┐
       │                  │           │
       ▼                  │           ▼
┌──────────────┐          │  ┌─────────────────┐
│ 2200-UPDATE- │          │  │ 9000-ERROR-     │
│ POSITIONS    │          │  │ ROUTINE         │
└──────┬───────┘          │  └─────────────────┘
       │                  │
       ▼                  │
  ┌─────────┐             │
  │ TRN-TYPE│             │
  └────┬────┘             │
       │                  │
  ┌────┴────┬────────┬────────┐
  │         │        │        │
  ▼         ▼        ▼        ▼
┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐
│ BU   │ │ SL   │ │ TR   │ │ FE   │
│ Buy  │ │ Sell │ │Trans.│ │ Fee  │
│      │ │      │ │(stub)│ │      │
│ ADD  │ │ SUB  │ │ N/A  │ │ SUB  │
│ qty  │ │ qty  │ │      │ │ amt  │
│ ADD  │ │ SUB  │ │      │ │ from │
│ amt  │ │ amt  │ │      │ │ cost │
└──┬───┘ └──┬───┘ └──┬───┘ └──┬───┘
   │        │        │        │
   └────────┴────────┴────────┘
                │
                ▼
    ┌──────────────────────┐
    │ 2300-UPDATE-AUDIT-   │  Build audit record:
    │ TRAIL                │  - Timestamp, User, Program
    │                      │  - Action: CREATE/DELETE/UPDATE
    │                      │  - Before-image of portfolio
    │                      │  - Transaction details message
    └──────────┬───────────┘
               │
               ▼
    ┌──────────────────────┐
    │ 2310-WRITE-AUDIT-    │  CALL 'AUDPROC'
    │ RECORD               │
    └──────────────────────┘
```

### Audit Trail Action Mapping

| Transaction Type | Audit Action  |
|------------------|---------------|
| BU (Buy)         | `CREATE`      |
| SL (Sell)        | `DELETE`      |
| TR (Transfer)    | `UPDATE`      |
| FE (Fee)         | `UPDATE`      |

---

## 7. Error Handling Approach

### Strategy by Program

| Program  | Approach                                                                 |
|----------|--------------------------------------------------------------------------|
| PORTMSTR | Sets `WS-ERROR-TEXT`, calls `9000-ERROR` which sets RC=8 and GOBACKs. Also has structured VSAM error handler calling `ERRPROC`. |
| PORTADD  | Inline validation with DISPLAY; counts errors and duplicates; continues processing |
| PORTUPDT | Counts errors per record; DISPLAY on failure; continues processing        |
| PORTDEL  | Counts deletes, not-found, and errors separately; writes audit trail; continues processing |
| PORTREAD | Checks file status on OPEN; terminates on failure; sets RETURN-CODE      |
| PORTTRAN | Calls `ERRPROC` via `9000-ERROR-ROUTINE`; halts if error count exceeds 100 |
| PORTTEST | DISPLAY on file errors; terminates on OPEN failure                        |
| PORTVALD | Returns error code and message via LINKAGE SECTION; no DISPLAY or abort  |

### VSAM File Status Codes Used

| Status | Meaning         | Programs                    |
|--------|-----------------|-----------------------------|
| `00`   | Success         | All                         |
| `10`   | End of file     | PORTADD, PORTUPDT, PORTDEL, PORTREAD, PORTTRAN |
| `22`   | Duplicate key   | PORTMSTR, PORTADD           |
| `23`   | Record not found| PORTMSTR, PORTUPDT, PORTDEL, PORTREAD |

### Error Severity Levels (ERRHAND)

| Code | Severity | Description         |
|------|----------|---------------------|
| `+0` | Success  | Normal completion   |
| `+4` | Warning  | Non-fatal condition |
| `+8` | Error    | Processing failure  |
| `+12`| Severe   | Critical failure    |
| `+16`| Terminal | Unrecoverable       |

### Error Categories (ERRHAND)

| Code | Category   | Description                |
|------|------------|----------------------------|
| `VS` | VSAM       | File I/O errors            |
| `VL` | Validation | Data validation failures   |
| `PR` | Processing | Business logic errors      |
| `SY` | System     | System-level failures      |

---

## 8. JCL Reference

All JCL files reside in `src/jcl/portfolio/`.

### PORTDEF.jcl — VSAM File Definition

Runs `IDCAMS` to delete and redefine the `PORTFOLIO.MASTER.FILE` VSAM cluster.

```
//PORTDEF   JOB (ACCT),'DEFINE PORTFOLIO',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//DEFVSAM   EXEC PGM=IDCAMS
```

**Key parameters:** INDEXED, RECORDSIZE(200 200), KEYS(18 0), CYLINDERS(5 1), FREESPACE(10 10), SHAREOPTIONS(2 3)

---

### PORTADD.jcl — Portfolio Addition

```
//PORTADD   JOB (ACCT),'ADD PORTFOLIO',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//STEP1     EXEC PGM=PORTADD
```

| DD Name   | Dataset                     | Disposition |
|-----------|-----------------------------|-------------|
| STEPLIB   | YOUR.LOADLIB                | SHR         |
| PORTFILE  | PORTFOLIO.MASTER.FILE       | SHR         |
| INPTFILE  | PORTFOLIO.INPUT.FILE        | OLD         |

---

### PORTUPDT.jcl — Portfolio Update

```
//PORTUPDT  JOB (ACCT),'UPDATE PORTFOLIO',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//STEP1     EXEC PGM=PORTUPDT
```

| DD Name   | Dataset                     | Disposition |
|-----------|-----------------------------|-------------|
| STEPLIB   | YOUR.LOADLIB                | SHR         |
| PORTFILE  | PORTFOLIO.MASTER.FILE       | SHR         |
| UPDTFILE  | PORTFOLIO.UPDATE.FILE       | OLD         |

---

### PORTDEL.jcl — Portfolio Deletion

```
//PORTDEL   JOB (ACCT),'DELETE PORTFOLIO',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//STEP1     EXEC PGM=PORTDEL
```

| DD Name   | Dataset                     | Disposition |
|-----------|-----------------------------|-------------|
| STEPLIB   | YOUR.LOADLIB                | SHR         |
| PORTFILE  | PORTFOLIO.MASTER.FILE       | SHR         |
| DELEFILE  | PORTFOLIO.DELETE.FILE       | OLD         |
| AUDFILE   | PORTFOLIO.AUDIT.FILE        | MOD         |

---

### PORTREAD.jcl — Portfolio Reading

```
//PORTREAD  JOB (ACCT),'READ PORTFOLIO',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//STEP1     EXEC PGM=PORTREAD
```

| DD Name   | Dataset                     | Disposition |
|-----------|-----------------------------|-------------|
| STEPLIB   | YOUR.LOADLIB                | SHR         |
| PORTFILE  | PORTFOLIO.MASTER.FILE       | SHR         |

---

### PORTTEST.jcl — Test Data Generation

```
//PORTTEST  JOB (ACCT),'GEN TEST DATA',CLASS=A,MSGCLASS=X,NOTIFY=&SYSUID
//STEP1     EXEC PGM=PORTTEST
```

| DD Name   | Dataset                     | Disposition          |
|-----------|-----------------------------|----------------------|
| STEPLIB   | YOUR.LOADLIB                | SHR                  |
| TESTFILE  | PORTFOLIO.TEST.FILE         | (NEW,CATLG,DELETE)   |

**DCB:** RECFM=FB, LRECL=200, BLKSIZE=0

---

### JCL Not Found

No dedicated JCL files were found for:
- **PORTMSTR** — Invoked as a subroutine via `CALL`, not as a standalone job
- **PORTTRAN** — No JCL in `src/jcl/portfolio/`; may be executed via batch layer JCL
- **PORTVALD** — Invoked as a subroutine via `CALL`, not as a standalone job
