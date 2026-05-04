# CKPRST — Checkpoint/Restart Program

## Program Description

**CKPRST** is a batch checkpoint/restart utility program in the Investment Portfolio Management System. It provides a reusable framework for taking, committing, and restoring checkpoints during long-running batch jobs, enabling reliable recovery from failures without reprocessing all data.

The program is invoked via `CALL` from other batch programs (e.g., `HISTLD00`) and uses an `EVALUATE`-based dispatcher to route control to one of four operations based on an entry-point flag passed through the `CHECKPOINT-CONTROL` linkage structure. It manages checkpoint state in an indexed VSAM file (`CKPTFILE`), keyed by program ID and run date.

### Key Components

| Component | Description |
|---|---|
| **Copybook `CKPRST.cpy`** | Defines `CHECKPOINT-CONTROL` (linkage) and `CHECKPOINT-RECORD` (VSAM file record) |
| **Copybook `RETHND.cpy`** | Defines `RETURN-STATUS` for standardized return/error handling |
| **VSAM File `CKPTFILE`** | Indexed checkpoint file; key = `CKR-PROGRAM-ID` + `CKR-RUN-DATE` |

### Data Structures

- **CHECKPOINT-CONTROL** (Linkage): Header (program ID, run date/time, status flags), counters (records read/processed/errors, restart count), position tracking (last key, phase), file resource states (up to 5 files), and control info (commit frequency, max errors/restarts, restart mode).
- **CHECKPOINT-RECORD** (VSAM): Keyed record with `CKR-PROGRAM-ID` (8 bytes) + `CKR-RUN-DATE` (8 bytes) as composite key and 400-byte data area.
- **RETURN-STATUS** (Linkage): Return code (0=success, 4=warning, 8=error, 12=severe, 16=critical), reason code, module/function IDs, error location, error info, and retry actions.

---

## Logic Flow Diagram

```mermaid
flowchart TD
    A([PROCEDURE DIVISION Entry<br/>USING CHECKPOINT-CONTROL, RETURN-STATUS]) --> B{EVALUATE TRUE<br/>Entry-Point Flag}

    B -->|ENTRY-POINT-INIT| C[PERFORM PROC-INIT]
    B -->|ENTRY-POINT-TAKE| D[PERFORM PROC-TAKE-CHECKPOINT]
    B -->|ENTRY-POINT-COMMIT| E[PERFORM PROC-COMMIT-CHECKPOINT]
    B -->|ENTRY-POINT-RESTART| F[PERFORM PROC-RESTART]

    C --> C1["Initialize checkpoint processing<br/>• Set CK-STATUS to 'I' (Initial)<br/>• Populate CK-PROGRAM-ID, CK-RUN-DATE, CK-RUN-TIME<br/>• Reset CK-COUNTERS to zeros<br/>• Set CK-PHASE to '00' (Init)<br/>• Open CHECKPOINT-FILE (CKPTFILE)<br/>• Check WS-FILE-STATUS"]
    C1 --> G([GOBACK<br/>Return to caller])

    D --> D1["Take a checkpoint snapshot<br/>• Capture current CK-POSITION (last key, timestamp, phase)<br/>• Capture CK-FILE-STATUS for up to 5 tracked files<br/>• Update CK-RECORDS-READ, CK-RECORDS-PROC counters<br/>• Set CK-STATUS to 'A' (Active)<br/>• Build CKR-KEY from CK-PROGRAM-ID + CK-RUN-DATE<br/>• WRITE/REWRITE CHECKPOINT-RECORD to CKPTFILE<br/>• Check WS-FILE-STATUS"]
    D1 --> G

    E --> E1["Commit checkpoint to stable storage<br/>• Verify CK-STATUS is 'A' (Active)<br/>• Increment CK-COMMIT-FREQ counter tracking<br/>• REWRITE CHECKPOINT-RECORD in CKPTFILE<br/>• Set CK-STATUS to 'C' (Complete) for phase<br/>• Check WS-FILE-STATUS"]
    E1 --> G

    F --> F1["Handle restart from last checkpoint<br/>• Open CHECKPOINT-FILE (CKPTFILE)<br/>• Build CKR-KEY from CK-PROGRAM-ID + CK-RUN-DATE<br/>• READ CHECKPOINT-RECORD from CKPTFILE<br/>• Check WS-FILE-STATUS"]
    F1 --> F2{Checkpoint<br/>record found?}
    F2 -->|Yes| F3["Restore checkpoint state<br/>• Restore CK-POSITION (last key, phase)<br/>• Restore CK-COUNTERS<br/>• Restore CK-FILE-STATUS entries<br/>• Increment CK-RESTART-COUNT<br/>• Set CK-STATUS to 'R' (Restarted)<br/>• Set CK-MODE-RESTART in CK-RESTART-MODE"]
    F2 -->|No| F4["No checkpoint to restore<br/>• Set RETURN-CODE (RC-WARNING or RC-ERROR)<br/>• Populate error info in RETURN-STATUS"]
    F3 --> G
    F4 --> G

    style A fill:#4a90d9,stroke:#333,color:#fff
    style B fill:#f5a623,stroke:#333,color:#fff
    style G fill:#4a90d9,stroke:#333,color:#fff
    style C fill:#7ed321,stroke:#333,color:#fff
    style D fill:#7ed321,stroke:#333,color:#fff
    style E fill:#7ed321,stroke:#333,color:#fff
    style F fill:#7ed321,stroke:#333,color:#fff
    style C1 fill:#e8f5e9,stroke:#388e3c
    style D1 fill:#e8f5e9,stroke:#388e3c
    style E1 fill:#e8f5e9,stroke:#388e3c
    style F1 fill:#e8f5e9,stroke:#388e3c
    style F2 fill:#fff3e0,stroke:#f57c00
    style F3 fill:#e8f5e9,stroke:#388e3c
    style F4 fill:#ffebee,stroke:#c62828
```

---

## Caller Integration

Other batch programs invoke CKPRST through the standard calling convention documented in `CKPRST.cpy`:

```
CALL 'CKPINIT' USING CHECKPOINT-CONTROL RETURN-STATUS   (Initialize)
CALL 'CKPTAKE' USING CHECKPOINT-CONTROL RETURN-STATUS   (Take checkpoint)
CALL 'CKPCMIT' USING CHECKPOINT-CONTROL RETURN-STATUS   (Commit checkpoint)
CALL 'CKPRSTR' USING CHECKPOINT-CONTROL RETURN-STATUS   (Restart from checkpoint)
```

For example, `HISTLD00` (History Loader) calls checkpoint routines during its processing loop to enable restart after failures.

## File I/O Summary

| Operation | File | Access | Key |
|---|---|---|---|
| OPEN | CKPTFILE (VSAM Indexed) | DYNAMIC | `CKR-PROGRAM-ID + CKR-RUN-DATE` |
| READ | CKPTFILE | By key | Composite key lookup |
| WRITE | CKPTFILE | Sequential/Key | New checkpoint record |
| REWRITE | CKPTFILE | By key | Update existing checkpoint |

## Status Flags (Level 88)

| Flag | Value | Meaning |
|---|---|---|
| `CK-INITIAL` | `'I'` | Checkpoint initialized |
| `CK-ACTIVE` | `'A'` | Checkpoint in progress |
| `CK-COMPLETE` | `'C'` | Checkpoint committed |
| `CK-FAILED` | `'F'` | Checkpoint failed |
| `CK-RESTARTED` | `'R'` | Restarted from checkpoint |

## Phase Codes

| Phase | Value | Description |
|---|---|---|
| `CK-PHASE-INIT` | `'00'` | Initialization |
| `CK-PHASE-READ` | `'10'` | Reading input |
| `CK-PHASE-PROC` | `'20'` | Processing records |
| `CK-PHASE-UPDT` | `'30'` | Updating output |
| `CK-PHASE-TERM` | `'40'` | Termination |
