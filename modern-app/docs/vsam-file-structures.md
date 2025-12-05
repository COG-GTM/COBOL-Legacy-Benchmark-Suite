# VSAM File Structures Documentation

## Overview

This document provides detailed documentation of the VSAM (Virtual Storage Access Method) file structures used in the legacy COBOL Investment Portfolio Management System. Understanding these structures is essential for the data migration to the modern PostgreSQL database.

## VSAM File Types Used

The system uses three types of VSAM organizations:

| Type | Description | Use Case |
|------|-------------|----------|
| KSDS | Key-Sequenced Data Set | Primary data files with indexed access |
| ESDS | Entry-Sequenced Data Set | Sequential log files |
| RRDS | Relative Record Data Set | Fixed-position access (not used in this system) |

## File Inventory

### 1. Portfolio Master File (PORTMSTR)

**File Characteristics:**
- Organization: KSDS (Key-Sequenced Data Set)
- Record Format: Fixed
- Record Length: 400 bytes
- Key Length: 12 bytes
- Key Position: 1
- CI Size: 4096 bytes
- Share Options: (2,3) - Read sharing allowed during batch update

**Key Structure:**
| Position | Length | Field | Description |
|----------|--------|-------|-------------|
| 1-8 | 8 | Portfolio ID | Unique portfolio identifier |
| 9-10 | 2 | Account Type | Account classification |
| 11-12 | 2 | Branch ID | Branch identifier |

**Record Layout (from PORTFLIO.cpy):**

```cobol
01  PORT-RECORD.
    05  PORT-KEY.
        10  PORT-ID             PIC X(8).
        10  PORT-ACCOUNT-NO     PIC X(10).
    05  PORT-CLIENT-INFO.
        10  PORT-CLIENT-NAME    PIC X(30).
        10  PORT-CLIENT-TYPE    PIC X(1).
            88  PORT-INDIVIDUAL    VALUE 'I'.
            88  PORT-CORPORATE     VALUE 'C'.
            88  PORT-TRUST         VALUE 'T'.
    05  PORT-PORTFOLIO-INFO.
        10  PORT-CREATE-DATE    PIC 9(8).
        10  PORT-LAST-MAINT     PIC 9(8).
        10  PORT-STATUS         PIC X(1).
            88  PORT-ACTIVE       VALUE 'A'.
            88  PORT-CLOSED       VALUE 'C'.
            88  PORT-SUSPENDED    VALUE 'S'.
    05  PORT-FINANCIAL-INFO.
        10  PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3.
        10  PORT-CASH-BALANCE   PIC S9(13)V99 COMP-3.
    05  PORT-AUDIT-INFO.
        10  PORT-LAST-USER      PIC X(8).
        10  PORT-LAST-TRANS     PIC 9(8).
    05  PORT-FILLER            PIC X(50).
```

**Field Details:**

| Field | Type | Length | Bytes | Description |
|-------|------|--------|-------|-------------|
| PORT-ID | Alphanumeric | 8 | 8 | Portfolio identifier |
| PORT-ACCOUNT-NO | Alphanumeric | 10 | 10 | Account number |
| PORT-CLIENT-NAME | Alphanumeric | 30 | 30 | Client name |
| PORT-CLIENT-TYPE | Alphanumeric | 1 | 1 | I=Individual, C=Corporate, T=Trust |
| PORT-CREATE-DATE | Numeric | 8 | 8 | Creation date (YYYYMMDD) |
| PORT-LAST-MAINT | Numeric | 8 | 8 | Last maintenance date |
| PORT-STATUS | Alphanumeric | 1 | 1 | A=Active, C=Closed, S=Suspended |
| PORT-TOTAL-VALUE | Packed Decimal | 15,2 | 8 | Total portfolio value |
| PORT-CASH-BALANCE | Packed Decimal | 15,2 | 8 | Cash balance |
| PORT-LAST-USER | Alphanumeric | 8 | 8 | Last user to modify |
| PORT-LAST-TRANS | Numeric | 8 | 8 | Last transaction date |
| PORT-FILLER | Alphanumeric | 50 | 50 | Reserved for future use |

**IDCAMS Definition:**

```jcl
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

### 2. Transaction History File (TRANHIST)

**File Characteristics:**
- Organization: KSDS (Key-Sequenced Data Set)
- Record Format: Fixed
- Record Length: 300 bytes
- Key Length: 28 bytes
- Key Position: 1
- CI Size: 4096 bytes
- Share Options: (2,3)

**Key Structure:**
| Position | Length | Field | Description |
|----------|--------|-------|-------------|
| 1-8 | 8 | Transaction Date | YYYYMMDD format |
| 9-14 | 6 | Transaction Time | HHMMSS format |
| 15-22 | 8 | Portfolio ID | Portfolio identifier |
| 23-28 | 6 | Sequence Number | Transaction sequence |

**Record Layout (from TRNREC.cpy):**

```cobol
01  TRANSACTION-RECORD.
    05  TRN-KEY.
        10  TRN-DATE           PIC X(08).
        10  TRN-TIME           PIC X(06).
        10  TRN-PORTFOLIO-ID   PIC X(08).
        10  TRN-SEQUENCE-NO    PIC X(06).
    05  TRN-DATA.
        10  TRN-INVESTMENT-ID  PIC X(10).
        10  TRN-TYPE           PIC X(02).
            88  TRN-TYPE-BUY     VALUE 'BU'.
            88  TRN-TYPE-SELL    VALUE 'SL'.
            88  TRN-TYPE-TRANS   VALUE 'TR'.
            88  TRN-TYPE-FEE     VALUE 'FE'.
        10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
        10  TRN-PRICE         PIC S9(11)V9(4) COMP-3.
        10  TRN-AMOUNT        PIC S9(13)V9(2) COMP-3.
        10  TRN-CURRENCY      PIC X(03).
        10  TRN-STATUS        PIC X(01).
            88  TRN-STATUS-PEND   VALUE 'P'.
            88  TRN-STATUS-DONE   VALUE 'D'.
            88  TRN-STATUS-FAIL   VALUE 'F'.
            88  TRN-STATUS-REV    VALUE 'R'.
    05  TRN-AUDIT.
        10  TRN-PROCESS-DATE  PIC X(26).
        10  TRN-PROCESS-USER  PIC X(08).
    05  TRN-FILLER           PIC X(50).
```

**Field Details:**

| Field | Type | Length | Bytes | Description |
|-------|------|--------|-------|-------------|
| TRN-DATE | Alphanumeric | 8 | 8 | Transaction date (YYYYMMDD) |
| TRN-TIME | Alphanumeric | 6 | 6 | Transaction time (HHMMSS) |
| TRN-PORTFOLIO-ID | Alphanumeric | 8 | 8 | Portfolio identifier |
| TRN-SEQUENCE-NO | Alphanumeric | 6 | 6 | Sequence number |
| TRN-INVESTMENT-ID | Alphanumeric | 10 | 10 | Investment identifier |
| TRN-TYPE | Alphanumeric | 2 | 2 | BU=Buy, SL=Sell, TR=Transfer, FE=Fee |
| TRN-QUANTITY | Packed Decimal | 15,4 | 8 | Transaction quantity |
| TRN-PRICE | Packed Decimal | 15,4 | 8 | Transaction price |
| TRN-AMOUNT | Packed Decimal | 15,2 | 8 | Transaction amount |
| TRN-CURRENCY | Alphanumeric | 3 | 3 | Currency code (USD, EUR, etc.) |
| TRN-STATUS | Alphanumeric | 1 | 1 | P=Pending, D=Done, F=Failed, R=Reversed |
| TRN-PROCESS-DATE | Alphanumeric | 26 | 26 | Processing timestamp |
| TRN-PROCESS-USER | Alphanumeric | 8 | 8 | Processing user ID |
| TRN-FILLER | Alphanumeric | 50 | 50 | Reserved |

---

### 3. Position History File (POSHIST)

**File Characteristics:**
- Organization: KSDS (Key-Sequenced Data Set)
- Record Format: Fixed
- Record Length: 350 bytes
- Key Length: 26 bytes
- Key Position: 1
- CI Size: 4096 bytes
- Share Options: (2,3)

**Key Structure:**
| Position | Length | Field | Description |
|----------|--------|-------|-------------|
| 1-8 | 8 | Portfolio ID | Portfolio identifier |
| 9-16 | 8 | Position Date | YYYYMMDD format |
| 17-26 | 10 | Investment ID | Investment identifier |

**Record Layout (from POSREC.cpy):**

```cobol
01  POSITION-RECORD.
    05  POS-KEY.
        10  POS-PORTFOLIO-ID   PIC X(08).
        10  POS-DATE           PIC X(08).
        10  POS-INVESTMENT-ID  PIC X(10).
    05  POS-DATA.
        10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
        10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
        10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
        10  POS-CURRENCY       PIC X(03).
        10  POS-STATUS         PIC X(01).
            88  POS-STATUS-ACTIVE  VALUE 'A'.
            88  POS-STATUS-CLOSED  VALUE 'C'.
            88  POS-STATUS-PEND    VALUE 'P'.
    05  POS-AUDIT.
        10  POS-LAST-MAINT-DATE   PIC X(26).
        10  POS-LAST-MAINT-USER   PIC X(08).
    05  POS-FILLER               PIC X(50).
```

**Field Details:**

| Field | Type | Length | Bytes | Description |
|-------|------|--------|-------|-------------|
| POS-PORTFOLIO-ID | Alphanumeric | 8 | 8 | Portfolio identifier |
| POS-DATE | Alphanumeric | 8 | 8 | Position date (YYYYMMDD) |
| POS-INVESTMENT-ID | Alphanumeric | 10 | 10 | Investment identifier |
| POS-QUANTITY | Packed Decimal | 15,4 | 8 | Holding quantity |
| POS-COST-BASIS | Packed Decimal | 15,2 | 8 | Total cost basis |
| POS-MARKET-VALUE | Packed Decimal | 15,2 | 8 | Current market value |
| POS-CURRENCY | Alphanumeric | 3 | 3 | Currency code |
| POS-STATUS | Alphanumeric | 1 | 1 | A=Active, C=Closed, P=Pending |
| POS-LAST-MAINT-DATE | Alphanumeric | 26 | 26 | Last maintenance timestamp |
| POS-LAST-MAINT-USER | Alphanumeric | 8 | 8 | Last maintenance user |
| POS-FILLER | Alphanumeric | 50 | 50 | Reserved |

---

### 4. Batch Control File (BCHCTL)

**File Characteristics:**
- Organization: KSDS (Key-Sequenced Data Set)
- Record Format: Fixed
- Record Length: 200 bytes
- Key Length: 20 bytes
- Key Position: 1

**Key Structure:**
| Position | Length | Field | Description |
|----------|--------|-------|-------------|
| 1-8 | 8 | Job Name | Batch job name |
| 9-16 | 8 | Process Date | YYYYMMDD format |
| 17-20 | 4 | Sequence Number | Job sequence |

**Record Layout (from BCHCTL.cpy):**

```cobol
01  BATCH-CONTROL-RECORD.
    05  BCT-KEY.
        10  BCT-JOB-NAME      PIC X(8).
        10  BCT-PROCESS-DATE  PIC X(8).
        10  BCT-SEQUENCE-NO   PIC 9(4).
    05  BCT-DATA.
        10  BCT-STATUS        PIC X(1).
            88  BCT-STATUS-READY    VALUE 'R'.
            88  BCT-STATUS-ACTIVE   VALUE 'A'.
            88  BCT-STATUS-WAITING  VALUE 'W'.
            88  BCT-STATUS-DONE     VALUE 'D'.
            88  BCT-STATUS-ERROR    VALUE 'E'.
        10  BCT-PROCESS-CONTROL.
            15  BCT-STEP-NAME    PIC X(8).
            15  BCT-PROGRAM-NAME PIC X(8).
            15  BCT-START-TIME   PIC X(8).
            15  BCT-END-TIME     PIC X(8).
        10  BCT-RETURN-INFO.
            15  BCT-RETURN-CODE  PIC S9(4) COMP.
            15  BCT-ERROR-DESC   PIC X(80).
    05  BCT-STATISTICS.
        10  BCT-RESTART-COUNT  PIC 9(2) COMP.
        10  BCT-ATTEMPT-TS     PIC X(26).
        10  BCT-COMPLETE-TS    PIC X(26).
    05  BCT-FILLER            PIC X(50).
```

---

## Data Type Conversions

### Packed Decimal (COMP-3) Conversion

Packed decimal fields require special handling during migration:

| COBOL Definition | Storage Bytes | PostgreSQL Type |
|------------------|---------------|-----------------|
| PIC S9(13)V99 COMP-3 | 8 | DECIMAL(15,2) |
| PIC S9(11)V9(4) COMP-3 | 8 | DECIMAL(15,4) |
| PIC S9(11)V999 COMP-3 | 8 | DECIMAL(14,3) |

**Conversion Formula:**
```
PostgreSQL Value = COMP-3 Value / 10^decimal_places
```

### Date Conversion

| COBOL Format | Example | PostgreSQL Format |
|--------------|---------|-------------------|
| PIC 9(8) YYYYMMDD | 20241205 | DATE '2024-12-05' |
| PIC X(6) HHMMSS | 143025 | TIME '14:30:25' |
| PIC X(26) Timestamp | 2024-12-05-14.30.25.123456 | TIMESTAMP |

### Character Encoding

| Source | Target | Notes |
|--------|--------|-------|
| EBCDIC | UTF-8 | Standard mainframe to ASCII conversion |

## Access Patterns

### PORTMSTR Access Patterns

| Operation | Key Used | Frequency |
|-----------|----------|-----------|
| Direct Read | Portfolio ID | High |
| Sequential Scan | None | Daily batch |
| Update | Portfolio ID | Medium |

### TRANHIST Access Patterns

| Operation | Key Used | Frequency |
|-----------|----------|-----------|
| Direct Read | Full key | Low |
| Range Read | Date range | High |
| Sequential Write | Append | High |

### POSHIST Access Patterns

| Operation | Key Used | Frequency |
|-----------|----------|-----------|
| Direct Read | Portfolio + Date + Investment | Medium |
| Range Read | Portfolio + Date range | High |
| Update | Full key | Medium |

## Migration Considerations

### Data Volume Estimates

| File | Estimated Records | Record Size | Total Size |
|------|-------------------|-------------|------------|
| PORTMSTR | 100,000 | 400 bytes | 40 MB |
| TRANHIST | 10,000,000 | 300 bytes | 3 GB |
| POSHIST | 5,000,000 | 350 bytes | 1.75 GB |
| BCHCTL | 10,000 | 200 bytes | 2 MB |

### Extraction Methods

1. **IDCAMS REPRO**: Export to sequential file
2. **COBOL Program**: Custom extraction with transformation
3. **File Transfer**: FTP/SFTP to migration server

### Key Mapping to PostgreSQL

| VSAM File | VSAM Key | PostgreSQL Primary Key |
|-----------|----------|------------------------|
| PORTMSTR | PORT-ID + ACCOUNT-TYPE + BRANCH-ID | portfolio_id |
| TRANHIST | TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO | transaction_id |
| POSHIST | POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID | (portfolio_id, position_date, investment_id) |
| BCHCTL | BCT-JOB-NAME + BCT-PROCESS-DATE + BCT-SEQUENCE-NO | (job_name, process_date, sequence_number) |
