# Batch Layer — Technical Documentation

> **COBOL Legacy Benchmark Suite: Investment Portfolio Management System**

This document provides a comprehensive technical reference for the 11 batch programs that comprise the Batch Layer of the Investment Portfolio Management System. The batch layer handles high-volume transaction processing, position history loading, checkpoint/restart management, batch orchestration, recovery processing, and reporting.

---

## Table of Contents

1. [Batch Architecture Overview](#batch-architecture-overview)
2. [Batch Execution Sequence Diagram](#batch-execution-sequence-diagram)
3. [Program Reference](#program-reference)
   - [BCHCTL00 — Batch Control Processor](#1-bchctl00--batch-control-processor)
   - [CKPRST — Checkpoint/Restart Handler](#2-ckprst--checkpointrestart-handler)
   - [HISTLD00 — Position History DB2 Loader](#3-histld00--position-history-db2-loader)
   - [POSUPDT — Position Update Stub](#4-posupdt--position-update-stub)
   - [PRCSEQ00 — Process Sequence Manager](#5-prcseq00--process-sequence-manager)
   - [RCVPRC00 — Process Recovery Handler](#6-rcvprc00--process-recovery-handler)
   - [RPTAUD00 — Audit Report Generator](#7-rptaud00--audit-report-generator)
   - [RPTPOS00 — Daily Position Report](#8-rptpos00--daily-position-report)
   - [RPTSTA00 — System Statistics Report](#9-rptsta00--system-statistics-report)
   - [RTNANA00 — Return Code Analysis Utility](#10-rtnana00--return-code-analysis-utility)
   - [RTNCDE00 — Standard Return Code Handler](#11-rtncde00--standard-return-code-handler)
4. [Copybook Reference](#copybook-reference)
5. [JCL Reference](#jcl-reference)
6. [Error Handling Strategy](#error-handling-strategy)
7. [DB2 Integration](#db2-integration)

---

## Batch Architecture Overview

The batch layer is structured around three functional tiers:

| Tier | Programs | Purpose |
|------|----------|---------|
| **Orchestration** | PRCSEQ00, BCHCTL00, RCVPRC00 | Sequence management, batch control, and failure recovery |
| **Data Processing** | HISTLD00, POSUPDT | History loading to DB2 and position updates |
| **Reporting** | RPTAUD00, RPTPOS00, RPTSTA00, RTNANA00 | Audit, position, statistics, and return-code reports |
| **Infrastructure** | CKPRST, RTNCDE00 | Checkpoint/restart services and return code management |

All batch programs follow a consistent pattern:
- **VSAM** indexed files for batch control, process sequencing, checkpointing, and master data.
- **DB2** for position history persistence and return-code logging.
- **Standardized copybooks** for record layouts, error handling, and constants.
- **Return codes**: 0 (success), 4 (warning), 8 (error), 12 (severe), 16 (critical).

---

## Batch Execution Sequence Diagram

The following diagram shows how **PRCSEQ00** orchestrates batch execution through **BCHCTL00**, and how **RCVPRC00** provides recovery when processes fail.

```
                          ┌──────────────────────────────────────┐
                          │          JOB SCHEDULER               │
                          │  (Submits daily batch schedule)      │
                          └──────────────┬───────────────────────┘
                                         │
                                    FUNC-INIT
                                         │
                          ┌──────────────▼───────────────────────┐
                          │          PRCSEQ00                     │
                          │    Process Sequence Manager           │
                          │                                      │
                          │  1. Opens PRCSEQ + BCHCTL files      │
                          │  2. Builds sequence table from        │
                          │     process definitions (PRCSEQ.cpy) │
                          │  3. Creates BCHCTL control records   │
                          │     for each process (status=READY)  │
                          └──────────────┬───────────────────────┘
                                         │
                                    FUNC-NEXT (loop)
                                         │
                  ┌──────────────────────▼──────────────────────────┐
                  │                                                  │
                  │  For each process in sequence:                   │
                  │                                                  │
                  │  ┌─────────────────────────────────────────┐    │
                  │  │  2100-FIND-NEXT-READY                   │    │
                  │  │  Find next process with READY status    │    │
                  │  └─────────────┬───────────────────────────┘    │
                  │                │                                 │
                  │  ┌─────────────▼───────────────────────────┐    │
                  │  │  2200-CHECK-DEPENDENCIES                │    │
                  │  │  Verify prerequisite processes           │    │
                  │  │  completed within acceptable RC          │    │
                  │  └─────────────┬───────────────────────────┘    │
                  │                │                                 │
                  │       ┌────────▼────────┐                       │
                  │       │ Dependencies OK? │                       │
                  │       └───┬──────────┬──┘                       │
                  │        Yes│          │No                         │
                  │           │          └──► Wait / Skip            │
                  │  ┌────────▼────────────────────────────────┐    │
                  │  │  CALL BCHCTL00 (FUNC-INIT)              │    │
                  │  │  Initialize batch control for process    │    │
                  │  │  ─ Opens BCHCTL file                    │    │
                  │  │  ─ Reads control record                 │    │
                  │  │  ─ Validates process                    │    │
                  │  │  ─ Sets status = ACTIVE                 │    │
                  │  └─────────────┬───────────────────────────┘    │
                  │                │                                 │
                  │  ┌─────────────▼───────────────────────────┐    │
                  │  │  Execute Target Program                  │    │
                  │  │  (e.g. HISTLD00, RPTxxx00, etc.)        │    │
                  │  │                                          │    │
                  │  │  Program may CALL:                       │    │
                  │  │  ─ CKPRST for checkpoint/restart         │    │
                  │  │  ─ RTNCDE00 for return code mgmt        │    │
                  │  │  ─ ERRPROC for error handling            │    │
                  │  └─────────────┬───────────────────────────┘    │
                  │                │                                 │
                  │  ┌─────────────▼───────────────────────────┐    │
                  │  │  CALL BCHCTL00 (FUNC-UPDT)              │    │
                  │  │  Update process status + return code     │    │
                  │  └─────────────┬───────────────────────────┘    │
                  │                │                                 │
                  │  ┌─────────────▼───────────────────────────┐    │
                  │  │  CALL BCHCTL00 (FUNC-TERM)              │    │
                  │  │  Finalize: update completion timestamp,  │    │
                  │  │  set status = DONE, close files          │    │
                  │  └─────────────┬───────────────────────────┘    │
                  │                │                                 │
                  │      ┌─────────▼──────────┐                     │
                  │      │ Process failed?     │                     │
                  │      └───┬────────────┬───┘                     │
                  │       No │            │ Yes                      │
                  │          │   ┌────────▼───────────────────┐     │
                  │          │   │  RCVPRC00 (FUNC-RECV)      │     │
                  │          │   │  Recovery Handler           │     │
                  │          │   │                             │     │
                  │          │   │  Determines action:         │     │
                  │          │   │  ─ RESTART if restartable   │     │
                  │          │   │  ─ BYPASS if not (RC=4)     │     │
                  │          │   │  ─ TERMINATE if max retries │     │
                  │          │   └────────┬───────────────────┘     │
                  │          │            │                          │
                  │          └────────────┤                          │
                  │                       │                          │
                  └───────────────────────┤                          │
                                          │                          │
                  ┌───────────────────────▼──────────────────────────┘
                  │
      ┌───────────▼──────────────────────────┐
      │  PRCSEQ00 (FUNC-STAT)                │
      │  Check overall completion:            │
      │  ─ All processes DONE → RC=0          │
      │  ─ Active processes remain → RC=4     │
      │  ─ Errors detected → RC=8             │
      └───────────┬──────────────────────────┘
                   │
      ┌────────────▼─────────────────────────┐
      │  PRCSEQ00 (FUNC-TERM)               │
      │  Close files, return final RC         │
      └──────────────────────────────────────┘
```

### Standard Daily Batch Sequence (from PRCSEQ.cpy)

The `STANDARD-SEQUENCES` definition in `PRCSEQ.cpy` defines the canonical daily batch order:

| Phase | Sequence | Process | Description |
|-------|----------|---------|-------------|
| **Start of Day** | 1 | `INITDAY` | Day initialization |
| | 2 | `CKPCLR` | Clear prior checkpoints |
| | 3 | `DATEVAL` | Date validation |
| **Main Processing** | 4 | `TRNVAL00` | Transaction validation |
| | 5 | `POSUPD00` | Position updates |
| | 6 | `HISTLD00` | History loading to DB2 |
| **End of Day** | 7 | `RPTGEN00` | Report generation |
| | 8 | `BCKLOD00` | Backup loading |
| | 9 | `ENDDAY` | Day finalization |

---

## Program Reference

---

### 1. BCHCTL00 — Batch Control Processor

**Source**: `src/programs/batch/BCHCTL00.cbl`

#### Purpose & Business Function

BCHCTL00 provides centralized batch control services for the entire batch layer. It manages the lifecycle of batch processes by initializing control records, checking prerequisites, updating process status, and finalizing process completion. It is called by the orchestration layer (PRCSEQ00) to manage individual process execution states within a batch sequence.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `BCHCTL` | `src/copybook/batch/BCHCTL.cpy` | Batch control file record layout (FD) |
| `BCHCON` | `src/copybook/batch/BCHCON.cpy` | Batch control constants (status values, return codes, process types) |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `BCHCTL` | BATCH-CONTROL-FILE | VSAM Indexed (KSDS) | Dynamic | `BCT-KEY` (job-name + process-date + sequence-no) | I-O: Read/Update control records |

**File Status**: `WS-BCT-STATUS` (PIC X(2))

#### DB2 Interactions

None. BCHCTL00 operates exclusively on VSAM files.

#### Program Calls

| Target | Type | Purpose |
|--------|------|---------|
| `ERRPROC` | CALL | External error processing routine |

#### Linkage Section Interface

```
01  LS-CONTROL-REQUEST.
    05  LS-FUNCTION          PIC X(4).
        88  FUNC-INIT          VALUE 'INIT'.   ← Initialize process
        88  FUNC-CHEK          VALUE 'CHEK'.   ← Check prerequisites
        88  FUNC-UPDT          VALUE 'UPDT'.   ← Update process status
        88  FUNC-TERM          VALUE 'TERM'.   ← Terminate/finalize process
    05  LS-JOB-NAME          PIC X(8).         ← Process/job identifier
    05  LS-PROCESS-DATE      PIC X(8).         ← Processing date
    05  LS-SEQUENCE-NO       PIC 9(4).         ← Sequence number
    05  LS-RETURN-CODE       PIC S9(4) COMP.   ← Return code (output)
```

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| `0000-MAIN` | Evaluates function code and dispatches to appropriate processing paragraph |
| `1000-PROCESS-INITIALIZE` | Orchestrates INIT function: open files, read control record, validate, update start status |
| `1100-OPEN-FILES` | Opens BATCH-CONTROL-FILE for I-O |
| `1200-READ-CONTROL-RECORD` | Reads the control record by key |
| `1300-VALIDATE-PROCESS` | Validates process parameters |
| `1400-UPDATE-START-STATUS` | Updates control record to ACTIVE status |
| `2000-CHECK-PREREQUISITES` | Reads control record and checks dependency completion |
| `2100-READ-CONTROL-RECORD` | Reads control record for prerequisite check |
| `2200-CHECK-DEPENDENCIES` | Iterates prerequisite jobs to verify completion status |
| `3000-UPDATE-STATUS` | Reads, updates, and rewrites the control record |
| `3100-READ-CONTROL-RECORD` | Reads the current control record |
| `3200-UPDATE-PROCESS-STATUS` | Modifies the process status in-memory |
| `3300-WRITE-CONTROL-RECORD` | Rewrites the updated control record to VSAM |
| `4000-PROCESS-TERMINATE` | Updates completion info and closes files |
| `4100-UPDATE-COMPLETION` | Sets completion timestamp and final status |
| `4200-CLOSE-FILES` | Closes BATCH-CONTROL-FILE |
| `9000-ERROR-ROUTINE` | Sets program name in error message, sets RC=8, calls ERRPROC |

#### Error Handling

- Returns `BCT-RC-ERROR` (8) on any error and calls the external `ERRPROC` routine.
- Invalid function codes are caught by the `WHEN OTHER` clause and routed to `9000-ERROR-ROUTINE`.
- VSAM file status is checked via `WS-BCT-STATUS`.

#### JCL References

No dedicated JCL. BCHCTL00 is invoked via CALL from PRCSEQ00 or other orchestrating programs, not as a standalone job step.

---

### 2. CKPRST — Checkpoint/Restart Handler

**Source**: `src/programs/batch/CKPRST.cbl`

#### Purpose & Business Function

CKPRST provides checkpoint and restart services for batch programs. It allows long-running batch jobs to periodically save their processing state (checkpoints) so that, in case of failure, processing can resume from the last committed checkpoint rather than restarting from the beginning. This is critical for high-volume batch jobs to avoid reprocessing millions of records.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `CKPRST` | `src/copybook/batch/CKPRST.cpy` | Checkpoint control structure and VSAM record layout (used in both FD and Linkage) |
| `RETHND` | `src/copybook/common/RETHND.cpy` | Return code handling definitions |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `CKPTFILE` | CHECKPOINT-FILE | VSAM Indexed (KSDS) | Dynamic | `CKR-KEY` (program-id + run-date) | I-O: Read/Write checkpoint records |

**File Status**: `WS-FILE-STATUS` (PIC X(2))

#### DB2 Interactions

None. CKPRST operates exclusively on VSAM files.

#### Program Calls

None. CKPRST is a called subprogram; it does not call other programs.

#### Linkage Section Interface

CKPRST accepts two parameters via the `PROCEDURE DIVISION USING` clause:

**Parameter 1: `CHECKPOINT-CONTROL`** (from CKPRST.cpy):
```
01  CHECKPOINT-CONTROL.
    05  CK-HEADER.
        10  CK-PROGRAM-ID       PIC X(8).    ← Calling program
        10  CK-RUN-DATE         PIC X(8).    ← Current run date
        10  CK-RUN-TIME         PIC X(6).    ← Current run time
        10  CK-STATUS           PIC X(1).    ← I/A/C/F/R status
    05  CK-COUNTERS.
        10  CK-RECORDS-READ     PIC 9(9).    ← Records read count
        10  CK-RECORDS-PROC     PIC 9(9).    ← Records processed
        10  CK-RECORDS-ERROR    PIC 9(9).    ← Error count
        10  CK-RESTART-COUNT    PIC 9(2).    ← Number of restarts
    05  CK-POSITION.
        10  CK-LAST-KEY         PIC X(50).   ← Last processed key
        10  CK-LAST-TIME        PIC X(26).   ← Last checkpoint timestamp
        10  CK-PHASE            PIC X(2).    ← Processing phase (00-40)
    05  CK-RESOURCES.                        ← File position tracking (x5)
    05  CK-CONTROL-INFO.
        10  CK-COMMIT-FREQ      PIC 9(5).   ← Commit frequency (default 1000)
        10  CK-MAX-ERRORS       PIC 9(3).   ← Max errors (default 100)
        10  CK-MAX-RESTARTS     PIC 9(2).   ← Max restarts (default 3)
        10  CK-RESTART-MODE     PIC X(1).   ← N=Normal, R=Restart, C=Recover
```

**Function codes** are determined by level-88 conditions on fields within the CKPRST copybook:
- `ENTRY-POINT-INIT` — Initialize checkpoint processing
- `ENTRY-POINT-TAKE` — Take a checkpoint (save current state)
- `ENTRY-POINT-COMMIT` — Commit a checkpoint (make it permanent)
- `ENTRY-POINT-RESTART` — Restart from last committed checkpoint

**Parameter 2: `RETURN-STATUS`** (from RETHND.cpy): Standard return status structure.

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| Main (unnamed) | Evaluates entry-point condition and dispatches |
| `PROC-INIT` | Initialize checkpoint processing (stub) |
| `PROC-TAKE-CHECKPOINT` | Take a checkpoint — save current state (stub) |
| `PROC-COMMIT-CHECKPOINT` | Commit checkpoint — make permanent (stub) |
| `PROC-RESTART` | Handle restart processing — restore state (stub) |

> **Note**: The procedure bodies are stubs (empty paragraphs with comments). The checkpoint framework defines the interface and VSAM file structure; implementation details are left for the target platform.

#### Error Handling

Return codes are communicated via the `RETURN-STATUS` parameter (RETHND.cpy), which provides structured error information including error type, location, and retry actions.

#### JCL References

No dedicated JCL. CKPRST is invoked via CALL from batch programs (e.g., HISTLD00).

---

### 3. HISTLD00 — Position History DB2 Loader

**Source**: `src/programs/batch/HISTLD00.cbl`

#### Purpose & Business Function

HISTLD00 loads transaction history records from a VSAM file into the DB2 `POSHIST` (Position History) table. It is a key data-processing program that populates the relational database with historical portfolio transaction data for querying, reporting, and auditing purposes. The program implements periodic DB2 commits and checkpoint updates for recoverability during high-volume loads.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `HISTREC` | `src/copybook/common/HISTREC.cpy` | Transaction history file record layout (FD — note: uses TH-* prefix at runtime) |
| `BCHCTL` | `src/copybook/batch/BCHCTL.cpy` | Batch control file record layout (FD) |
| `DBTBLS` | `src/copybook/db2/DBTBLS.cpy` | DB2 table host variable definitions (POSHIST-RECORD) |
| `SQLCA` | `src/copybook/db2/SQLCA.cpy` | SQL Communication Area (SQLCA + standard SQL status codes) |
| `DBPROC` | `src/copybook/db2/DBPROC.cpy` | Standard DB2 procedures (connect, disconnect, error handling) |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions |
| `BCHCON` | `src/copybook/batch/BCHCON.cpy` | Batch control constants |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `TRANHIST` | TRANSACTION-HISTORY | VSAM Indexed (KSDS) | Sequential | `TH-KEY` | INPUT: Read transaction history records |
| `BCHCTL` | BATCH-CONTROL-FILE | VSAM Indexed (KSDS) | Dynamic | `BCT-KEY` | I-O: Read/Update checkpoint progress |

**File Statuses**: `WS-TH-STATUS`, `WS-BCT-STATUS`

#### DB2 Interactions

| Table | Operation | Details |
|-------|-----------|---------|
| `POSHIST` | INSERT | Inserts each transaction history record as a position history row using `:POSHIST-RECORD` host variable structure |
| — | COMMIT WORK | Periodic commits every 1,000 records (`WS-COMMIT-THRESHOLD`) |
| — | ROLLBACK WORK | Rollback on error in `9000-ERROR-ROUTINE` |

**DB2 Connection**: Connects to database `POSMVP` via `CONNECT-TO-DB2` procedure (from DBPROC.cpy).

**SQLCA Usage**:
- `SQLCODE = 0` → Success, increment records written
- `SQLCODE = -803` → Duplicate key, skip (CONTINUE)
- Other SQLCODE → Error, increment error count, call `DB2-ERROR-ROUTINE`

#### Program Calls

| Target | Type | Purpose |
|--------|------|---------|
| `ERRPROC` | CALL | External error processing (via 9000-ERROR-ROUTINE) |
| `CONNECT-TO-DB2` | PERFORM | DB2 connection (from DBPROC.cpy) |
| `DISCONNECT-FROM-DB2` | PERFORM | DB2 disconnect (from DBPROC.cpy) |
| `DB2-ERROR-ROUTINE` | PERFORM | DB2 error handling (from DBPROC.cpy) |

#### Linkage Section Interface

None. HISTLD00 is a standalone batch program (no USING clause).

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| `0000-MAIN` | Initialize → Process loop → Terminate; sets RETURN-CODE to error count |
| `1000-INITIALIZE` | Opens files, connects to DB2, initializes checkpoints |
| `1100-OPEN-FILES` | Opens TRANSACTION-HISTORY (input) and BATCH-CONTROL-FILE (I-O) |
| `1200-CONNECT-DB2` | Establishes DB2 connection via `CONNECT-TO-DB2` |
| `1300-INIT-CHECKPOINTS` | Reads the HISTLD00 control record, sets status to ACTIVE |
| `2000-PROCESS` | Reads next history record, loads to DB2, checks commit threshold |
| `2100-READ-HISTORY` | Reads next sequential record from TRANSACTION-HISTORY |
| `2200-LOAD-TO-DB2` | Maps 13 fields from TH-* to PH-* and executes INSERT INTO POSHIST |
| `2300-CHECK-COMMIT` | Increments commit counter; when threshold (1000) reached, commits and updates checkpoint |
| `2310-UPDATE-CHECKPOINT` | Updates batch control record with current read/written counts |
| `3000-TERMINATE` | Final commit, close files, disconnect DB2, display statistics |
| `3100-FINAL-COMMIT` | Issues final COMMIT WORK and updates checkpoint |
| `3200-CLOSE-FILES` | Closes both VSAM files |
| `3300-DISCONNECT-DB2` | Disconnects from DB2 via `DISCONNECT-FROM-DB2` |
| `3400-DISPLAY-STATS` | Displays records read, written, and error count |
| `9000-ERROR-ROUTINE` | Sets program name, calls ERRPROC, issues ROLLBACK WORK |

#### Error Handling

- **DB2 errors**: SQLCODE checked after each INSERT; -803 (duplicate) skipped, others counted and routed to `DB2-ERROR-ROUTINE`.
- **VSAM errors**: File status checked after OPEN; non-'00' triggers `9000-ERROR-ROUTINE`.
- **Abort threshold**: Processing stops when `WS-ERROR-COUNT > 100`.
- **Rollback**: All errors trigger `ROLLBACK WORK` before error reporting.
- **Return code**: Set to the final error count (`WS-ERROR-COUNT`).

#### JCL References

No dedicated JCL file in `src/jcl/batch/` for HISTLD00. The program is executed as part of the main batch sequence managed by PRCSEQ00 (sequence position: `HISTLD00` in SEQ-MAIN-PROCESS).

---

### 4. POSUPDT — Position Update Stub

**Source**: `src/programs/batch/POSUPDT.cbl`

#### Purpose & Business Function

POSUPDT is a placeholder/stub program for position update processing. In the full system design, it would apply transaction results to update the Position Master VSAM file, recalculating quantities, market values, and cost basis for each portfolio holding. The source file is currently empty (1 blank line).

#### Copybook Dependencies

None (stub).

#### File I/O

None defined (stub).

#### DB2 Interactions

None (stub).

#### Program Calls

None (stub).

#### Linkage Section Interface

None defined (stub).

#### Processing Flow

No processing logic implemented. The program is referenced in the standard batch sequence (PRCSEQ.cpy: `POSUPD00` in SEQ-MAIN-PROCESS).

#### Error Handling

Not implemented (stub).

#### JCL References

None.

---

### 5. PRCSEQ00 — Process Sequence Manager

**Source**: `src/programs/batch/PRCSEQ00.cbl`

#### Purpose & Business Function

PRCSEQ00 is the **master orchestrator** of the batch layer. It manages the ordered execution of batch processes by:
1. Building a process sequence table from the PRCSEQ VSAM file definitions.
2. Creating batch control records (BCHCTL) for each process in the sequence.
3. Determining the next ready process, checking its dependencies, and updating its status to ACTIVE.
4. Tracking overall sequence completion and reporting final status.

This program is the entry point that drives the entire daily batch cycle.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `PRCSEQ` | `src/copybook/batch/PRCSEQ.cpy` | Process sequence record layout (FD) + standard sequence definitions |
| `BCHCTL` | `src/copybook/batch/BCHCTL.cpy` | Batch control file record layout (FD) |
| `BCHCON` | `src/copybook/batch/BCHCON.cpy` | Batch control constants |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `PRCSEQ` | PROCESS-SEQ-FILE | VSAM Indexed (KSDS) | Dynamic | `PSR-KEY` (process-id + version) | I-O: Read process sequence definitions |
| `BCHCTL` | BATCH-CONTROL-FILE | VSAM Indexed (KSDS) | Dynamic | `BCT-KEY` (job-name + process-date + sequence-no) | I-O: Create/Read/Update control records |

**File Statuses**: `WS-PSR-STATUS`, `WS-BCT-STATUS`

#### DB2 Interactions

None. PRCSEQ00 operates exclusively on VSAM files.

#### Program Calls

| Target | Type | Purpose |
|--------|------|---------|
| `ERRPROC` | CALL | External error processing routine |

**Called indirectly**: PRCSEQ00 orchestrates the execution of downstream programs (BCHCTL00, HISTLD00, RPTxxx00, etc.) through the batch sequence mechanism.

#### Linkage Section Interface

```
01  LS-SEQUENCE-REQUEST.
    05  LS-FUNCTION          PIC X(4).
        88  FUNC-INIT          VALUE 'INIT'.   ← Initialize sequence
        88  FUNC-NEXT          VALUE 'NEXT'.   ← Get next ready process
        88  FUNC-STAT          VALUE 'STAT'.   ← Check sequence status
        88  FUNC-TERM          VALUE 'TERM'.   ← Terminate sequence
    05  LS-PROCESS-DATE      PIC X(8).         ← Processing date
    05  LS-SEQUENCE-TYPE     PIC X(3).         ← Sequence type (INI/UPD/RPT/CLN)
    05  LS-NEXT-PROCESS      PIC X(8).         ← Next process ID (output)
    05  LS-RETURN-CODE       PIC S9(4) COMP.   ← Return code (output)
```

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| `0000-MAIN` | Dispatches to function handler based on FUNC-INIT/NEXT/STAT/TERM |
| `1000-INITIALIZE-SEQUENCE` | Opens files, builds sequence, creates control records |
| `1100-OPEN-FILES` | Opens PROCESS-SEQ-FILE and BATCH-CONTROL-FILE (both I-O) |
| `1200-BUILD-SEQUENCE` | Positions on PRCSEQ by date, reads matching entries into WS-PROCESS-TABLE (up to 100 entries) |
| `1210-ADD-TO-SEQUENCE` | Adds a single process entry to the in-memory sequence table |
| `1300-CREATE-CONTROL-RECORDS` | Creates a BCHCTL record for each process in the sequence with READY status |
| `2000-GET-NEXT-PROCESS` | Finds next ready process, checks dependencies, updates status |
| `2100-FIND-NEXT-READY` | Scans WS-PROCESS-TABLE for first entry with READY status |
| `2200-CHECK-DEPENDENCIES` | Reads process definition from PRCSEQ, iterates dependencies |
| `2210-CHECK-DEP-STATUS` | For each dependency: reads its BCHCTL record, verifies DONE status and acceptable RC. Hard dependencies block; exceeded RC thresholds cause errors |
| `2300-UPDATE-PROCESS-STATUS` | Sets process to ACTIVE, records start timestamp, rewrites BCHCTL |
| `3000-CHECK-STATUS` | Reads control status, updates sequence table, checks overall completion |
| `3100-READ-CONTROL-STATUS` | Reads the batch control record for the current process |
| `3200-UPDATE-SEQUENCE-TABLE` | Syncs BCHCTL status/RC back to in-memory table |
| `3300-CHECK-COMPLETION` | Counts ACTIVE and ERROR processes to determine overall state |
| `4000-TERMINATE-SEQUENCE` | Checks final status and closes files |
| `4100-CHECK-FINAL-STATUS` | Returns RC=0 (all done), RC=4 (active remaining), or RC=8 (errors) |
| `4200-CLOSE-FILES` | Closes both VSAM files with error checking |
| `9000-ERROR-ROUTINE` | Sets program name, RC=8, calls ERRPROC |

#### Error Handling

- File open errors immediately trigger `9000-ERROR-ROUTINE`.
- VSAM INVALID KEY conditions produce descriptive error messages and call ERRPROC.
- Dependencies with non-zero return codes exceeding thresholds set `LS-RETURN-CODE` to BCT-RC-ERROR.
- Final status reflects aggregate results: success only if all processes completed without errors.

#### JCL References

No dedicated JCL. PRCSEQ00 is the top-level orchestrator invoked by the job scheduler.

---

### 6. RCVPRC00 — Process Recovery Handler

**Source**: `src/programs/batch/RCVPRC00.cbl`

#### Purpose & Business Function

RCVPRC00 provides automated recovery services for failed batch processes. When a batch process fails, RCVPRC00 analyzes the failure, determines the appropriate recovery action (restart, bypass, or terminate), and executes it. It supports three recovery scopes:
- **Process** (`P`): Recover a single named process
- **Sequence** (`S`): Recover all processes for a specific date
- **All** (`A`): Recover all processes across all dates

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `BCHCTL` | `src/copybook/batch/BCHCTL.cpy` | Batch control file record layout (FD) |
| `PRCSEQ` | `src/copybook/batch/PRCSEQ.cpy` | Process sequence record layout (FD) |
| `BCHCON` | `src/copybook/batch/BCHCON.cpy` | Batch control constants |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `BCHCTL` | BATCH-CONTROL-FILE | VSAM Indexed (KSDS) | Dynamic | `BCT-KEY` | I-O: Read/Update control records for recovery |
| `PRCSEQ` | PROCESS-SEQ-FILE | VSAM Indexed (KSDS) | Dynamic | `PSR-KEY` | INPUT: Read process definitions for restartability |

**File Statuses**: `WS-BCT-STATUS`, `WS-PSR-STATUS`

#### DB2 Interactions

None. RCVPRC00 operates exclusively on VSAM files.

#### Program Calls

| Target | Type | Purpose |
|--------|------|---------|
| `ERRPROC` | CALL | External error processing routine (error handling + final status logging) |

#### Linkage Section Interface

```
01  LS-RECOVERY-REQUEST.
    05  LS-FUNCTION          PIC X(4).
        88  FUNC-INIT          VALUE 'INIT'.   ← Initialize recovery
        88  FUNC-RECV          VALUE 'RECV'.   ← Execute recovery
        88  FUNC-TERM          VALUE 'TERM'.   ← Terminate recovery
    05  LS-PROCESS-DATE      PIC X(8).         ← Date scope for recovery
    05  LS-PROCESS-ID        PIC X(8).         ← Process ID (for type 'P')
    05  LS-RECOVERY-TYPE     PIC X(1).         ← P=Process, S=Sequence, A=All
    05  LS-RECOVERY-PARM     PIC X(50).        ← Additional recovery parameters
    05  LS-RETURN-CODE       PIC S9(4) COMP.   ← Return code (output)
```

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| `0000-MAIN` | Dispatches to INIT/RECV/TERM handler |
| `1000-INITIALIZE-RECOVERY` | Opens files, validates request, sets recovery mode |
| `1100-OPEN-FILES` | Opens BATCH-CONTROL-FILE (I-O) and PROCESS-SEQ-FILE (INPUT) |
| `1200-VALIDATE-REQUEST` | Validates process date (required) and recovery type (P/S/A) |
| `1300-SET-RECOVERY-MODE` | Sets WS-RECOVERY-MODE; validates process ID for type 'P' |
| `2000-PROCESS-RECOVERY` | Evaluates recovery mode and dispatches to appropriate handler |
| `2100-RECOVER-PROCESS` | Reads BCHCTL record for the process, determines action, executes recovery |
| `2110-DETERMINE-ACTION` | Reads PRCSEQ definition; if restartable → RESTART; else if max retries exceeded → TERMINATE; else → BYPASS |
| `2120-EXECUTE-RECOVERY` | Evaluates action flag and calls restart/bypass/terminate |
| `2121-RESTART-PROCESS` | Sets status=READY, increments restart count, updates timestamp |
| `2122-BYPASS-PROCESS` | Sets status=DONE with RC=4 (warning), records bypass message |
| `2123-TERMINATE-PROCESS` | Sets status=ERROR with RC=8, records termination message |
| `2200-RECOVER-SEQUENCE` | Browses all BCHCTL records for the given date, recovers each |
| `2300-RECOVER-ALL` | Browses all BCHCTL records across all dates, recovers each |
| `3000-TERMINATE-RECOVERY` | Updates final status and closes files |
| `3100-UPDATE-FINAL-STATUS` | Logs success/error message via ERRPROC |
| `3200-CLOSE-FILES` | Closes both VSAM files with error checking |
| `9000-ERROR-ROUTINE` | Sets program name, RC=8, calls ERRPROC |

#### Error Handling

- Request validation errors (missing date, invalid type) route to `9000-ERROR-ROUTINE`.
- VSAM INVALID KEY conditions produce descriptive messages.
- Recovery decisions are based on `PSR-RESTARTABLE` flag and `BCT-RESTART-COUNT` vs. `BCT-MAX-RESTARTS`.
- Final status reported via ERRPROC regardless of success or failure.

#### JCL References

No dedicated JCL. RCVPRC00 is invoked via CALL when PRCSEQ00 detects a failed process.

---

### 7. RPTAUD00 — Audit Report Generator

**Source**: `src/programs/batch/RPTAUD00.cbl`

#### Purpose & Business Function

RPTAUD00 generates a comprehensive system audit report that includes:
- Security audit trails (login, logout, access events)
- Process audit reporting (transaction, system events)
- Error summary reporting
- Control verification

This report is essential for compliance, security review, and operational oversight.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `AUDITLOG` | `src/copybook/common/AUDITLOG.cpy` | Audit trail record layout (FD for AUDIT-FILE) |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Error record layout (FD for ERROR-FILE) |
| `RTNCODE` | `src/copybook/common/RTNCODE.cpy` | Return code management definitions |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `AUDITLOG` | AUDIT-FILE | VSAM Indexed (KSDS) | Sequential | `AUD-KEY` | INPUT: Read audit trail records |
| `ERRLOG` | ERROR-FILE | VSAM Indexed (KSDS) | Sequential | `ERR-KEY` | INPUT: Read error log records |
| `RPTFILE` | REPORT-FILE | Sequential (QSAM) | Sequential | — | OUTPUT: Write formatted report (RECFM=F, LRECL=132) |

**File Statuses**: `WS-AUDIT-STATUS`, `WS-ERROR-STATUS`, `WS-REPORT-STATUS`

#### DB2 Interactions

None. RPTAUD00 reads VSAM files and writes a sequential report.

#### Program Calls

None. RPTAUD00 is a self-contained report program.

#### Linkage Section Interface

None. RPTAUD00 is a standalone batch program (no USING clause).

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| `0000-MAIN` | Initialize → Process → Cleanup |
| `1000-INITIALIZE` | Opens files and writes report headers |
| `1100-OPEN-FILES` | Opens AUDIT-FILE (input), ERROR-FILE (input), REPORT-FILE (output) |
| `1200-WRITE-HEADERS` | Writes header lines (separator, title "SYSTEM AUDIT REPORT", date) |
| `2000-PROCESS-REPORT` | Orchestrates the three report sections |
| `2100-PROCESS-AUDIT-TRAIL` | Reads and summarizes audit records |
| `2110-READ-AUDIT-RECORDS` | Sequentially reads all audit log records |
| `2120-SUMMARIZE-AUDIT` | Generates audit trail summary |
| `2200-PROCESS-ERROR-LOG` | Reads and summarizes error records |
| `2210-READ-ERROR-RECORDS` | Sequentially reads all error log records |
| `2220-SUMMARIZE-ERRORS` | Generates error summary |
| `2300-WRITE-SUMMARY` | Writes audit summary, error summary, and control verification |
| `2310-WRITE-AUDIT-SUMMARY` | Formats and writes audit totals |
| `2320-WRITE-ERROR-SUMMARY` | Formats and writes error totals |
| `2330-WRITE-CONTROL-SUMMARY` | Writes control verification section |
| `3000-CLEANUP` | Closes all three files |
| `9999-ERROR-HANDLER` | Displays error message, sets RC=12, GOBACK |

#### Error Handling

- File open failures display an error message and immediately terminate with RC=12.
- Uses `9999-ERROR-HANDLER` pattern (DISPLAY + GOBACK).

#### JCL References

**`src/jcl/batch/RPTAUD.jcl`**:
```jcl
//RPTAUD00 JOB (ACCT#),'AUDIT REPORT',CLASS=A
//STEP01   EXEC PGM=RPTAUD00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//AUDITLOG DD   DSN=PROD.AUDIT.LOG,DISP=SHR
//ERRLOG   DD   DSN=PROD.ERROR.LOG,DISP=SHR
//RPTFILE  DD   DSN=PROD.AUDIT.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
```

---

### 8. RPTPOS00 — Daily Position Report

**Source**: `src/programs/batch/RPTPOS00.cbl`

#### Purpose & Business Function

RPTPOS00 generates the daily position report for the investment portfolio system. It provides:
- Portfolio position summary (holdings, quantities, market values)
- Transaction activity summary
- Exception reporting (unusual movements)
- Performance metrics (percentage change from previous day)

This is a critical end-of-day report used by portfolio managers and operations staff.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `POSREC` | `src/copybook/common/POSREC.cpy` | Position master record layout (FD for POSITION-MASTER) |
| `TRNREC` | `src/copybook/common/TRNREC.cpy` | Transaction record layout (FD for TRANSACTION-HISTORY) |
| `RTNCODE` | `src/copybook/common/RTNCODE.cpy` | Return code management definitions |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `POSMSTRE` | POSITION-MASTER | VSAM Indexed (KSDS) | Sequential | `POS-KEY` (portfolio-id + date + investment-id) | INPUT: Read position records |
| `TRANHIST` | TRANSACTION-HISTORY | VSAM Indexed (KSDS) | Sequential | `TRAN-KEY` | INPUT: Read transaction records |
| `RPTFILE` | REPORT-FILE | Sequential (QSAM) | Sequential | — | OUTPUT: Write formatted report (RECFM=F, LRECL=132) |

**File Statuses**: `WS-POSITION-STATUS`, `WS-TRAN-STATUS`, `WS-REPORT-STATUS`

#### DB2 Interactions

None. RPTPOS00 reads VSAM files and writes a sequential report.

#### Program Calls

None. RPTPOS00 is a self-contained report program.

#### Linkage Section Interface

None. RPTPOS00 is a standalone batch program (no USING clause).

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| `0000-MAIN` | Initialize → Process → Cleanup |
| `1000-INITIALIZE` | Opens files, writes headers |
| `1100-OPEN-FILES` | Opens POSITION-MASTER (input), TRANSACTION-HISTORY (input), REPORT-FILE (output) |
| `1200-WRITE-HEADERS` | Writes header lines (separator, title "DAILY POSITION REPORT", date) |
| `2000-PROCESS-REPORT` | Reads positions, processes transactions, writes summary |
| `2100-READ-POSITIONS` | Sequentially reads all position records, formatting each |
| `2110-FORMAT-POSITION` | Maps position fields to report detail line; computes daily change % = `(current - previous) / previous * 100` |
| `2200-PROCESS-TRANSACTIONS` | Reads and summarizes transaction activity |
| `2210-READ-TRANSACTIONS` | Reads all transaction records |
| `2220-SUMMARIZE-ACTIVITY` | Generates transaction activity summary |
| `2300-WRITE-SUMMARY` | Writes totals, exceptions, and performance metrics |
| `2310-WRITE-TOTALS` | Writes overall portfolio totals |
| `2320-WRITE-EXCEPTIONS` | Writes exception/anomaly section |
| `2330-WRITE-METRICS` | Writes performance metrics section |
| `3000-CLEANUP` | Closes all three files |
| `9999-ERROR-HANDLER` | Displays error message, sets RC=12, GOBACK |

#### Error Handling

- File open failures display error message and terminate with RC=12.
- Uses `9999-ERROR-HANDLER` pattern.

#### JCL References

**`src/jcl/batch/RPTPOS.jcl`**:
```jcl
//RPTPOS00 JOB (ACCT#),'DAILY POSITION RPT',CLASS=A
//STEP01   EXEC PGM=RPTPOS00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//POSMSTRE DD   DSN=PROD.POSITION.MASTER,DISP=SHR
//TRANHIST DD   DSN=PROD.TRANSACTION.HISTORY,DISP=SHR
//RPTFILE  DD   DSN=PROD.DAILY.POSITION.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
```

---

### 9. RPTSTA00 — System Statistics Report

**Source**: `src/programs/batch/RPTSTA00.cbl`

#### Purpose & Business Function

RPTSTA00 generates a system performance and statistics report that covers:
- DB2 processing statistics (call counts, elapsed time, CPU, wait time)
- Batch job statistics (total jobs, success/failure counts, elapsed time)
- Calculated metrics (average DB2 response time, batch success rate)
- Trend analysis

This report supports capacity planning, performance tuning, and operational monitoring.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `DB2STAT` | (referenced in FD) | DB2 statistics record layout |
| `BCHCTL` | `src/copybook/batch/BCHCTL.cpy` | Batch statistics record layout (FD for BATCH-STATS) |
| `RTNCODE` | `src/copybook/common/RTNCODE.cpy` | Return code management definitions |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Standard error handling definitions |

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `DB2STATS` | DB2-STATS | VSAM Indexed (KSDS) | Sequential | `STAT-KEY` | INPUT: Read DB2 statistics records |
| `BCHSTATS` | BATCH-STATS | VSAM Indexed (KSDS) | Sequential | `BCH-KEY` | INPUT: Read batch execution statistics |
| `RPTFILE` | REPORT-FILE | Sequential (QSAM) | Sequential | — | OUTPUT: Write formatted report (RECFM=F, LRECL=132) |

**File Statuses**: `WS-DB2-STATUS`, `WS-BCH-STATUS`, `WS-REPORT-STATUS`

#### DB2 Interactions

None. RPTSTA00 reads VSAM statistics files and writes a sequential report.

#### Program Calls

None. RPTSTA00 is a self-contained report program.

#### Linkage Section Interface

None. RPTSTA00 is a standalone batch program (no USING clause).

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| `0000-MAIN` | Initialize → Process → Cleanup |
| `1000-INITIALIZE` | Opens files, writes headers, initializes accumulators |
| `1100-OPEN-FILES` | Opens DB2-STATS (input), BATCH-STATS (input), REPORT-FILE (output) |
| `1200-WRITE-HEADERS` | Writes header lines (separator, title "SYSTEM STATISTICS AND PERFORMANCE REPORT", date) |
| `1300-INIT-ACCUMULATORS` | Initializes WS-PERFORMANCE-METRICS to zeros |
| `2000-PROCESS-REPORT` | Processes DB2 stats, batch stats, calculates metrics, writes report |
| `2100-PROCESS-DB2-STATS` | Reads all DB2 statistics records sequentially |
| `2110-ACCUMULATE-DB2-STATS` | Accumulates DB2 metrics (calls, elapsed, CPU, wait) |
| `2200-PROCESS-BATCH-STATS` | Reads all batch statistics records sequentially |
| `2210-ACCUMULATE-BATCH-STATS` | Accumulates batch metrics (jobs, success, failed, elapsed) |
| `2300-CALCULATE-METRICS` | Calculates derived metrics |
| `2310-CALC-DB2-METRICS` | Calculates average DB2 response time |
| `2320-CALC-BATCH-METRICS` | Calculates batch success rate percentage |
| `2400-WRITE-REPORT` | Writes DB2 section, batch section, and trend analysis |
| `2410-WRITE-DB2-SECTION` | Writes DB2 performance section with call counts and avg response |
| `2420-WRITE-BATCH-SECTION` | Writes batch section with job counts and success rate |
| `2430-WRITE-TREND-ANALYSIS` | Writes trend analysis section |
| `3000-CLEANUP` | Closes all three files |
| `9999-ERROR-HANDLER` | Displays error message, sets RC=12, GOBACK |

#### Error Handling

- File open failures display error and terminate with RC=12.
- Uses `9999-ERROR-HANDLER` pattern.

#### JCL References

**`src/jcl/batch/RPTSTA.jcl`**:
```jcl
//RPTSTA00 JOB (ACCT#),'SYSTEM STATS RPT',CLASS=A
//STEP01   EXEC PGM=RPTSTA00
//STEPLIB  DD   DSN=PROD.LOAD.LIBRARY,DISP=SHR
//DB2STATS DD   DSN=PROD.DB2.STATISTICS,DISP=SHR
//BCHSTATS DD   DSN=PROD.BATCH.STATISTICS,DISP=SHR
//RPTFILE  DD   DSN=PROD.SYSTEM.STATS.REPORT,
//             DISP=(NEW,CATLG,DELETE),
//             SPACE=(CYL,(10,5),RLSE),
//             DCB=(RECFM=FB,LRECL=132,BLKSIZE=0)
```

---

### 10. RTNANA00 — Return Code Analysis Utility

**Source**: `src/programs/batch/RTNANA00.cbl`

#### Purpose & Business Function

RTNANA00 analyzes return codes across the entire system by querying the DB2 `RTNCODES` table. It produces a report showing, for each program:
- Total return code entries
- Count by severity (Success, Warning, Error, Severe)
- Grand totals across all programs

This utility helps operations teams identify programs with high error rates and track return-code trends over time.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `SQLCA` | (inline via `EXEC SQL INCLUDE SQLCA`) | SQL Communication Area |

> **Note**: RTNANA00 uses inline SQL INCLUDE rather than COPY for SQLCA.

#### File I/O

| DD Name | File | Organization | Access Mode | Record Key | Usage |
|---------|------|-------------|-------------|------------|-------|
| `RPTFILE` | REPORT-FILE | Sequential (QSAM) | Sequential | — | OUTPUT: Write analysis report (RECFM=F, LRECL=133) |

**File Status**: `WS-REPORT-STATUS`

#### DB2 Interactions

| Table | Operation | Details |
|-------|-----------|---------|
| `RTNCODES` | SELECT (cursor `PRGCUR`) | Aggregates return codes by PROGRAM_ID: COUNT(*), COUNT by STATUS_CODE (S/W/E/F), ordered by PROGRAM_ID |

**Cursor**: `PRGCUR` — declared, opened, fetched in a loop until `SQLCODE = 100`, then closed.

**SQLCA Usage**: SQLCODE checked after OPEN and FETCH; loop terminates on SQLCODE=100 (not found).

#### Program Calls

None. RTNANA00 is a self-contained analysis utility.

#### Linkage Section Interface

None. RTNANA00 is a standalone batch program.

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| Main (unnamed) | Calls P100 → P200 → P300 → P900 sequentially with THRU exits |
| `P100-INIT-PROGRAM` | Gets current date/time, opens REPORT-FILE, initializes analysis area |
| `P200-PROCESS-ANALYSIS` | Declares and opens PRGCUR cursor, writes headers, fetches detail rows |
| `P210-WRITE-HEADERS` | Writes report header lines (separator, title "Return Code Analysis Report", date/time, column headers) |
| `P220-PROCESS-DETAIL` | Fetches one row from PRGCUR into detail line fields; writes to report; accumulates grand totals |
| `P300-GENERATE-REPORT` | Writes grand totals line ("TOTALS") |
| `P900-CLOSE-FILES` | Closes REPORT-FILE |

#### Error Handling

- Report file open failure displays error and terminates with RC=12.
- DB2 cursor errors are implicitly handled via SQLCODE checks in the fetch loop.

#### JCL References

**`src/jcl/RTNANA.jcl`** (note: at `src/jcl/` root, not in `batch/`):
```jcl
//RTNANA00 JOB (ACCT),'RETURN CODE ANALYSIS',CLASS=A
//RTNANA   EXEC PGM=RTNANA00
//STEPLIB  DD DSN=PROD.LOAD.LIBRARY,DISP=SHR
//RPTFILE  DD SYSOUT=*,
//            DCB=(RECFM=FBA,LRECL=133,BLKSIZE=0)
```

> **Note**: RPTFILE output goes to SYSOUT (printer) rather than a catalogued dataset.

---

### 11. RTNCDE00 — Standard Return Code Handler

**Source**: `src/programs/batch/RTNCDE00.cbl`

#### Purpose & Business Function

RTNCDE00 is a shared service program that provides standardized return code management across the batch (and online) system. It supports five operations:
1. **Initialize** return code tracking for a program
2. **Set** a new return code (tracks current and highest)
3. **Get** current return code and status
4. **Log** return codes to the DB2 `RTNCODES` audit table
5. **Analyze** historical return codes for a program within a time range

This program is the foundation of the system's return code framework, enabling consistent error classification and auditing.

#### Copybook Dependencies

| Copybook | Location | Purpose |
|----------|----------|---------|
| `RTNCODE` | `src/copybook/common/RTNCODE.cpy` | Return code management area (Linkage Section) |
| `SQLCA` | (inline via `EXEC SQL INCLUDE SQLCA`) | SQL Communication Area |

#### File I/O

None. RTNCDE00 operates purely on in-memory data and DB2.

#### DB2 Interactions

| Table | Operation | Details |
|-------|-----------|---------|
| `RTNCODES` | INSERT | Logs return code entry with timestamp, program ID, current code, highest code, status, and message |
| `RTNCODES` | SELECT | Analyzes codes: COUNT, MAX, MIN for a program within a time range |

**SQLCA Usage**:
- `SQLCODE = 0` → RC-RESPONSE-CODE = 0 (success)
- `SQLCODE != 0` → RC-RESPONSE-CODE = 8 (error)

#### Program Calls

None. RTNCDE00 is a called subprogram.

#### Linkage Section Interface

```
01  RC-REQUEST-AREA.
    COPY RTNCODE.
    ─────────────────────────────────────────
    05  RC-REQUEST-TYPE       PIC X.
        88  RC-INITIALIZE       VALUE 'I'.   ← Initialize
        88  RC-SET-CODE         VALUE 'S'.   ← Set return code
        88  RC-GET-CODE         VALUE 'G'.   ← Get return code
        88  RC-LOG-CODE         VALUE 'L'.   ← Log to DB2
        88  RC-ANALYZE          VALUE 'A'.   ← Analyze codes
    05  RC-PROGRAM-ID         PIC X(8).      ← Program identifier
    05  RC-CODES-AREA.
        10  RC-CURRENT-CODE   PIC S9(4).     ← Current return code
        10  RC-HIGHEST-CODE   PIC S9(4).     ← Highest RC seen
        10  RC-NEW-CODE       PIC S9(4).     ← New code to set
        10  RC-STATUS         PIC X.         ← S/W/E/F status
    05  RC-MESSAGE            PIC X(80).     ← Message text
    05  RC-RESPONSE-CODE      PIC S9(8).     ← Response (0=ok, 8=error)
    05  RC-ANALYSIS-DATA.
        10  RC-START-TIME     PIC X(26).     ← Analysis start time
        10  RC-END-TIME       PIC X(26).     ← Analysis end time
        10  RC-TOTAL-CODES    PIC S9(8).     ← Total codes found
        10  RC-MAX-CODE       PIC S9(4).     ← Maximum code
        10  RC-MIN-CODE       PIC S9(4).     ← Minimum code
    05  RC-RETURN-DATA.
        10  RC-RETURN-VALUE   PIC S9(4).     ← Retrieved current code
        10  RC-HIGHEST-RETURN PIC S9(4).     ← Retrieved highest code
        10  RC-RETURN-STATUS  PIC X.         ← Retrieved status
```

#### Processing Flow

| Paragraph | Description |
|-----------|-------------|
| Main (unnamed) | Evaluates RC-REQUEST-TYPE and dispatches to P100–P500 with THRU exits |
| `P100-INIT-RETURN-CODES` | Initializes all return code fields to zero/spaces; sets status to SUCCESS |
| `P200-SET-RETURN-CODE` | Sets current code; updates highest if new code exceeds it; classifies status: 0=Success, 1-4=Warning, 5-8=Error, 9+=Severe |
| `P300-GET-RETURN-CODE` | Copies current code, highest code, and status to return data fields |
| `P400-LOG-RETURN-CODE` | Gets current timestamp; inserts return code record into DB2 RTNCODES table |
| `P500-ANALYZE-CODES` | Queries RTNCODES for COUNT, MAX, MIN within the specified program and time range |

#### Error Handling

- DB2 operations set `RC-RESPONSE-CODE` to 0 on success or 8 on failure.
- No CALL to ERRPROC; error status communicated purely via the response code in the linkage area.

#### JCL References

No dedicated JCL. RTNCDE00 is invoked via CALL from other programs.

---

## Copybook Reference

### Batch Copybooks (`src/copybook/batch/`)

| Copybook | File | Description |
|----------|------|-------------|
| **BCHCON** | `BCHCON.cpy` | Batch control constants: status values (R/A/W/D/E), return code thresholds (0/4/8/12/16), process types (INI/UPD/RPT/CLN), dependency types (R/O/X), max restarts (3), wait intervals |
| **BCHCTL** | `BCHCTL.cpy` | Batch control VSAM record: composite key (job-name + date + sequence), status, process control times, up to 10 prerequisite dependencies, return info, restart statistics |
| **CKPRST** | `CKPRST.cpy` | Checkpoint/restart structure: program header, counters (read/processed/errors), position tracking (last key, phase 00-40), file resource tracking (5 files), control parameters (commit freq, max errors/restarts, restart mode) |
| **PRCSEQ** | `PRCSEQ.cpy` | Process sequence definition: process ID + version key, type (INI/PRC/RPT/TRM), timing (daily/weekly/monthly), up to 10 dependencies (hard/soft), program + parms, schedule (active days, month-end, holiday), recovery config, audit trail |

### DB2 Copybooks (`src/copybook/db2/`)

| Copybook | File | Description |
|----------|------|-------------|
| **DBTBLS** | `DBTBLS.cpy` | DB2 host variable definitions: `POSHIST-RECORD` (position history with 13 financial fields) and `ERRLOG-RECORD` (error log with severity levels) |
| **DBPROC** | `DBPROC.cpy` | Standard DB2 procedures: `CONNECT-TO-DB2` (connects to POSMVP), `DISCONNECT-FROM-DB2` (commit + reset), `DB2-ERROR-ROUTINE` (format SQLCODE/STATE, rollback, call ERRPROC), `CHECK-SQL-STATUS` |
| **SQLCA** | `SQLCA.cpy` | SQL Communication Area include + standard SQLSTATE codes (success 00000, not-found 02000, duplicate 23505, deadlock 40001, timeout 40003, connection error 08001) |

### Common Copybooks (`src/copybook/common/`)

| Copybook | File | Description |
|----------|------|-------------|
| **ERRHAND** | `ERRHAND.cpy` | Error categories (VS/VL/PR/SY), return codes (0/4/8/12/16), error message structure (timestamp, program, category, code, severity, text, details), VSAM status handling |
| **RTNCODE** | `RTNCODE.cpy` | Return code management interface for RTNCDE00: request type (I/S/G/L/A), program ID, current/highest/new codes, status (S/W/E/F), analysis data (time range, counts, min/max) |
| **RETHND** | `RETHND.cpy` | Return handling structure: return status (RC 0/4/8/12/16), error location (program, paragraph, routine), error info (type V/P/D/F/S, code, text), system info, action flags (Continue/Abort/Retry), standard error codes E001-E010 |
| **HISTREC** | `HISTREC.cpy` | History record: portfolio + date + time + sequence key, record type (PT/PS/TR), action (A/C/D), before/after images (400 bytes each), audit info |
| **POSREC** | `POSREC.cpy` | Position record: portfolio + date + investment key, quantity, cost basis, market value, currency, status (A/C/P), audit info |
| **TRNREC** | `TRNREC.cpy` | Transaction record: date + time + portfolio + sequence key, investment ID, type (BU/SL/TR/FE), quantity, price, amount, currency, status (P/D/F/R), audit info |
| **AUDITLOG** | `AUDITLOG.cpy` | Audit record: timestamp, system/user/program/terminal IDs, type (TRAN/USER/SYST), action (CREATE/UPDATE/DELETE/etc.), status (SUCC/FAIL/WARN), portfolio/account keys, before/after images, message |

---

## JCL Reference

### Batch JCL Files (`src/jcl/batch/`)

| JCL File | Program | Job Name | Description |
|----------|---------|----------|-------------|
| `RPTAUD.jcl` | RPTAUD00 | RPTAUD00 | System Audit Report — reads PROD.AUDIT.LOG and PROD.ERROR.LOG, writes PROD.AUDIT.REPORT |
| `RPTPOS.jcl` | RPTPOS00 | RPTPOS00 | Daily Position Report — reads PROD.POSITION.MASTER and PROD.TRANSACTION.HISTORY, writes PROD.DAILY.POSITION.REPORT |
| `RPTSTA.jcl` | RPTSTA00 | RPTSTA00 | System Statistics Report — reads PROD.DB2.STATISTICS and PROD.BATCH.STATISTICS, writes PROD.SYSTEM.STATS.REPORT |

### Other JCL (`src/jcl/`)

| JCL File | Program | Description |
|----------|---------|-------------|
| `RTNANA.jcl` | RTNANA00 | Return Code Analysis — output to SYSOUT (RECFM=FBA, LRECL=133) |

### Common JCL Conventions

- **STEPLIB**: All programs load from `PROD.LOAD.LIBRARY`
- **Report files**: Fixed-block format, LRECL=132 (or 133 for RTNANA00), allocated as new catalogued datasets with (CYL,(10,5),RLSE)
- **System DDs**: SYSOUT, SYSUDUMP, SYSPRINT directed to SYSOUT class
- **Note**: BCHCTL00, CKPRST, HISTLD00, POSUPDT, PRCSEQ00, RCVPRC00, and RTNCDE00 do not have dedicated JCL files — they are invoked via CALL from other programs or through the PRCSEQ00 orchestration framework

---

## Error Handling Strategy

The batch layer employs a multi-level error handling strategy:

### Level 1: Return Code Classification (RTNCDE00)
| Code | Status | Meaning |
|------|--------|---------|
| 0 | Success (S) | Normal completion |
| 1–4 | Warning (W) | Non-critical issues; processing continues |
| 5–8 | Error (E) | Significant problems; may require intervention |
| 9+ | Severe (F) | Critical failures; processing should stop |

### Level 2: VSAM File Status Handling (ERRHAND.cpy)
| Status | Meaning |
|--------|---------|
| `00` | Success |
| `10` | End of file |
| `22` | Duplicate record key |
| `23` | Record not found |
| Other | Unexpected error |

### Level 3: DB2 Error Handling (DBPROC.cpy + SQLCA.cpy)
- SQLCODE checked after every SQL operation
- Error routine formats SQLCODE + SQLSTATE for diagnostics
- Automatic ROLLBACK WORK on DB2 errors
- Retry logic available (DB2-MAX-RETRIES = 3)
- Standard SQLSTATE codes for common conditions (deadlock, timeout, etc.)

### Level 4: Recovery Processing (RCVPRC00)
- Automatic recovery decisions based on process definition (restartable flag)
- Maximum restart count enforcement (BCT-MAX-RESTARTS = 3)
- Three recovery actions: Restart (reset to READY), Bypass (set DONE with RC=4), Terminate (set ERROR with RC=8)

---

## DB2 Integration

### Database: POSMVP

The batch layer connects to the `POSMVP` DB2 database for position history and return code management.

### Tables Used

| Table | Programs | Operations |
|-------|----------|------------|
| `POSHIST` | HISTLD00 | INSERT (bulk load from VSAM) |
| `RTNCODES` | RTNCDE00, RTNANA00 | INSERT (log), SELECT (analyze, report) |

### Connection Management

- **Connect**: `EXEC SQL CONNECT TO POSMVP END-EXEC` (via DBPROC.cpy `CONNECT-TO-DB2`)
- **Commit**: Periodic commits in HISTLD00 (every 1,000 records); final commit on disconnect
- **Disconnect**: `EXEC SQL COMMIT WORK END-EXEC` followed by `EXEC SQL CONNECT RESET END-EXEC`
- **Rollback**: Automatic on error via `EXEC SQL ROLLBACK WORK END-EXEC`

### Host Variable Structures

Defined in `DBTBLS.cpy`:
- **POSHIST-RECORD**: 13+ fields covering account, portfolio, transaction details, financial amounts (COMP-3), and audit metadata
- **ERRLOG-RECORD**: Error logging with severity levels, types, and extended message/info fields (up to 500 bytes additional info)
