# BCHCTL00 — Batch Control Processor

## Program Description

**BCHCTL00** is a called batch control program that manages job-level sequencing, prerequisite checking, and status tracking for the Investment Portfolio Management System's batch processing layer. It operates on a VSAM indexed control file (`BATCH-CONTROL-FILE`) and is invoked by other batch programs via the `CALL` interface with a function code that determines the operation to perform.

### Key Characteristics

| Attribute | Value |
|---|---|
| **Program ID** | BCHCTL00 |
| **Type** | Called subprogram (LINKAGE SECTION interface) |
| **Platform** | IBM z/OS |
| **File I/O** | VSAM indexed file (dynamic access) |
| **DB2 Operations** | None |
| **CALL Statements** | `CALL 'ERRPROC'` (error handler) |
| **Copybooks** | `BCHCTL` (control record), `BCHCON` (constants), `ERRHAND` (error handling) |

### Supported Functions

| Code | Function | Description |
|---|---|---|
| `INIT` | Initialize | Opens files, reads/validates control record, sets start status |
| `CHEK` | Check Prerequisites | Reads control record, verifies job dependencies are satisfied |
| `UPDT` | Update Status | Reads control record, updates process status, writes back |
| `TERM` | Terminate | Updates completion info, closes files |

### Return Codes

| Code | Meaning |
|---|---|
| 0 | Success |
| 4 | Warning (e.g., prerequisites pending) |
| 8 | Error |
| 12 | Severe |
| 16 | Critical |

## Logic Flow Diagram

```mermaid
flowchart TD
    START([Caller invokes BCHCTL00<br/>via CALL ... USING LS-CONTROL-REQUEST]) --> MAIN["<b>0000-MAIN</b><br/>EVALUATE LS-FUNCTION"]

    MAIN -->|"FUNC-INIT<br/>(LS-FUNCTION = 'INIT')"| SET_INIT["SET MODE-INITIALIZE TO TRUE"]
    MAIN -->|"FUNC-CHEK<br/>(LS-FUNCTION = 'CHEK')"| SET_CHEK["SET MODE-CHECK-PREREQ TO TRUE"]
    MAIN -->|"FUNC-UPDT<br/>(LS-FUNCTION = 'UPDT')"| SET_UPDT["SET MODE-UPDATE-STATUS TO TRUE"]
    MAIN -->|"FUNC-TERM<br/>(LS-FUNCTION = 'TERM')"| SET_TERM["SET MODE-FINALIZE TO TRUE"]
    MAIN -->|"WHEN OTHER<br/>(Invalid function code)"| ERR_TEXT["MOVE 'Invalid function code'<br/>TO ERR-TEXT"]

    %% ============================================================
    %% INIT path
    %% ============================================================
    SET_INIT --> P1000["<b>1000-PROCESS-INITIALIZE</b>"]
    P1000 --> P1100["<b>1100-OPEN-FILES</b><br/>Open BATCH-CONTROL-FILE<br/>(VSAM indexed)"]
    P1100 --> P1200["<b>1200-READ-CONTROL-RECORD</b><br/>Read BCT record by key:<br/>JOB-NAME + PROCESS-DATE + SEQUENCE-NO"]
    P1200 --> P1300["<b>1300-VALIDATE-PROCESS</b><br/>Validate control record<br/>against business rules"]
    P1300 --> P1400["<b>1400-UPDATE-START-STATUS</b><br/>Set BCT-STATUS to ACTIVE,<br/>record start timestamp"]
    P1400 --> RETURN

    %% ============================================================
    %% CHEK path
    %% ============================================================
    SET_CHEK --> P2000["<b>2000-CHECK-PREREQUISITES</b>"]
    P2000 --> P2100["<b>2100-READ-CONTROL-RECORD</b><br/>Read BCT record by key"]
    P2100 --> P2200["<b>2200-CHECK-DEPENDENCIES</b><br/>Iterate BCT-PREREQ-JOBS<br/>(up to BCT-PREREQ-COUNT entries).<br/>Verify each prerequisite job<br/>status and return code."]
    P2200 --> PREREQ_IF{"PREREQS-SATISFIED?<br/>(WS-PREREQ-MET = 'Y')"}
    PREREQ_IF -->|"Yes"| RC_SUCCESS["LS-RETURN-CODE =<br/>BCT-RC-SUCCESS (0)"]
    PREREQ_IF -->|"No"| RC_WARNING["LS-RETURN-CODE =<br/>BCT-RC-WARNING (4)"]
    RC_SUCCESS --> RETURN
    RC_WARNING --> RETURN

    %% ============================================================
    %% UPDT path
    %% ============================================================
    SET_UPDT --> P3000["<b>3000-UPDATE-STATUS</b>"]
    P3000 --> P3100["<b>3100-READ-CONTROL-RECORD</b><br/>Read BCT record by key"]
    P3100 --> P3200["<b>3200-UPDATE-PROCESS-STATUS</b><br/>Update BCT-STATUS and<br/>BCT-RETURN-CODE fields"]
    P3200 --> P3300["<b>3300-WRITE-CONTROL-RECORD</b><br/>REWRITE record to<br/>BATCH-CONTROL-FILE"]
    P3300 --> RETURN

    %% ============================================================
    %% TERM path
    %% ============================================================
    SET_TERM --> P4000["<b>4000-PROCESS-TERMINATE</b>"]
    P4000 --> P4100["<b>4100-UPDATE-COMPLETION</b><br/>Set BCT-STATUS to DONE,<br/>record completion timestamp"]
    P4100 --> P4200["<b>4200-CLOSE-FILES</b><br/>Close BATCH-CONTROL-FILE"]
    P4200 --> RETURN

    %% ============================================================
    %% ERROR path
    %% ============================================================
    ERR_TEXT --> P9000["<b>9000-ERROR-ROUTINE</b><br/>MOVE 'BCHCTL00' TO ERR-PROGRAM<br/>MOVE BCT-RC-ERROR (8)<br/>TO LS-RETURN-CODE"]
    P9000 --> CALL_ERR["CALL 'ERRPROC'<br/>USING ERR-MESSAGE"]
    CALL_ERR --> RETURN

    %% ============================================================
    %% Common exit
    %% ============================================================
    RETURN["MOVE LS-RETURN-CODE<br/>TO RETURN-CODE<br/>GOBACK"]
    RETURN --> END_PGM([Return to Caller])

    %% ============================================================
    %% Styling
    %% ============================================================
    classDef startEnd fill:#e1f5fe,stroke:#0288d1,stroke-width:2px,color:#01579b
    classDef process fill:#f3e5f5,stroke:#7b1fa2,stroke-width:1px,color:#4a148c
    classDef decision fill:#fff3e0,stroke:#ef6c00,stroke-width:2px,color:#e65100
    classDef fileIO fill:#e8f5e9,stroke:#2e7d32,stroke-width:1px,color:#1b5e20
    classDef errorNode fill:#ffebee,stroke:#c62828,stroke-width:1px,color:#b71c1c
    classDef returnCode fill:#fce4ec,stroke:#ad1457,stroke-width:1px,color:#880e4f

    class START,END_PGM startEnd
    class MAIN,SET_INIT,SET_CHEK,SET_UPDT,SET_TERM,P1000,P2000,P3000,P4000 process
    class PREREQ_IF decision
    class P1100,P1200,P2100,P3100,P3300,P4200 fileIO
    class P1300,P1400,P2200,P3200,P4100,RETURN returnCode
    class ERR_TEXT,P9000,CALL_ERR errorNode
    class RC_SUCCESS,RC_WARNING returnCode
```
