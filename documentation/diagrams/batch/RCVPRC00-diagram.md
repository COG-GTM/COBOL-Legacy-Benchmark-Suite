# RCVPRC00 — Process Recovery Handler

## Program Description

**RCVPRC00** is a batch COBOL program that provides process recovery capabilities for the Investment Portfolio Management System. It is invoked via `CALL` with a linkage area (`LS-RECOVERY-REQUEST`) that specifies one of three functions:

| Function | Description |
|----------|-------------|
| `INIT`   | Opens VSAM files, validates the recovery request, and sets the recovery mode. |
| `RECV`   | Executes recovery based on the selected mode — single process, all processes for a date, or all processes system-wide. |
| `TERM`   | Logs a final status message and closes all files. |

### Key Characteristics

- **File I/O (VSAM):** Two indexed files — `BATCH-CONTROL-FILE` (I-O, keyed on `BCT-KEY`) and `PROCESS-SEQ-FILE` (INPUT, keyed on `PSR-KEY`). Operations include `READ` (keyed and sequential), `START`, `REWRITE`, `OPEN`, and `CLOSE`.
- **CALL Statements:** Calls external program `ERRPROC` for error logging and final status reporting.
- **Copybooks:** `BCHCTL`, `PRCSEQ` (file records), `BCHCON` (batch constants), `ERRHAND` (error handling fields).
- **Recovery Modes:** Process (`P`), Sequence (`S`), All (`A`).
- **Recovery Actions:** Restart (`R`), Bypass (`B`), Terminate (`T`) — determined by whether the process is restartable and how many restart attempts have occurred.
- **DB2 Operations:** None. This program operates exclusively on VSAM files.

---

## Logic Flow Diagram

```mermaid
flowchart TD
    START([PROCEDURE DIVISION ENTRY<br>USING LS-RECOVERY-REQUEST]) --> MAIN[0000-MAIN<br>Evaluate LS-FUNCTION]

    MAIN -->|FUNC-INIT| INIT[1000-INITIALIZE-RECOVERY]
    MAIN -->|FUNC-RECV| RECV[2000-PROCESS-RECOVERY]
    MAIN -->|FUNC-TERM| TERM[3000-TERMINATE-RECOVERY]
    MAIN -->|OTHER| ERR_INVALID[Set ERR-TEXT =<br>'Invalid function code']
    ERR_INVALID --> ERR[9000-ERROR-ROUTINE]

    INIT --> GOBACK
    RECV --> GOBACK
    TERM --> GOBACK
    ERR --> GOBACK[MOVE LS-RETURN-CODE<br>TO RETURN-CODE<br>GOBACK]

    %% ============================================================
    %% 1000 — INITIALIZE RECOVERY
    %% ============================================================
    INIT --> OPEN[1100-OPEN-FILES]
    OPEN --> OPEN_BCT[OPEN I-O<br>BATCH-CONTROL-FILE]
    OPEN_BCT --> CHK_BCT{WS-BCT-STATUS<br>= '00'?}
    CHK_BCT -->|No| ERR_OPEN_BCT[ERR: 'Error opening<br>control file'] --> ERR
    CHK_BCT -->|Yes| OPEN_PSR[OPEN INPUT<br>PROCESS-SEQ-FILE]
    OPEN_PSR --> CHK_PSR{WS-PSR-STATUS<br>= '00'?}
    CHK_PSR -->|No| ERR_OPEN_PSR[ERR: 'Error opening<br>sequence file'] --> ERR
    CHK_PSR -->|Yes| VALIDATE[1200-VALIDATE-REQUEST]

    VALIDATE --> CHK_DATE{LS-PROCESS-DATE<br>= SPACES?}
    CHK_DATE -->|Yes| ERR_DATE[ERR: 'Process date<br>required'] --> ERR
    CHK_DATE -->|No| CHK_TYPE{LS-RECOVERY-TYPE<br>= 'P', 'S', or 'A'?}
    CHK_TYPE -->|No| ERR_TYPE[ERR: 'Invalid recovery<br>type'] --> ERR
    CHK_TYPE -->|Yes| SETMODE[1300-SET-RECOVERY-MODE]

    SETMODE --> SET_WS[MOVE LS-RECOVERY-TYPE<br>TO WS-RECOVERY-MODE]
    SET_WS --> CHK_PID{Mode = 'P' AND<br>LS-PROCESS-ID<br>= SPACES?}
    CHK_PID -->|Yes| ERR_PID[ERR: 'Process ID required<br>for process recovery'] --> ERR
    CHK_PID -->|No| INIT_DONE([Initialization Complete])

    %% ============================================================
    %% 2000 — PROCESS RECOVERY
    %% ============================================================
    RECV --> EVAL_MODE{Evaluate<br>WS-RECOVERY-MODE}
    EVAL_MODE -->|'P'| RCV_PROC[2100-RECOVER-PROCESS]
    EVAL_MODE -->|'S'| RCV_SEQ[2200-RECOVER-SEQUENCE]
    EVAL_MODE -->|'A'| RCV_ALL[2300-RECOVER-ALL]

    %% --- 2100 RECOVER-PROCESS ---
    RCV_PROC --> SET_KEY[MOVE LS-PROCESS-ID<br>TO BCT-JOB-NAME<br>MOVE LS-PROCESS-DATE<br>TO BCT-PROCESS-DATE]
    SET_KEY --> READ_BCT[READ BATCH-CONTROL-FILE<br>by key]
    READ_BCT --> CHK_READ{INVALID KEY?}
    CHK_READ -->|Yes| ERR_RD[ERR: 'Process record<br>not found'] --> ERR
    CHK_READ -->|No| DET_ACT[2110-DETERMINE-ACTION]

    DET_ACT --> READ_PSR[MOVE LS-PROCESS-ID<br>TO PSR-PROCESS-ID<br>READ PROCESS-SEQ-FILE]
    READ_PSR --> CHK_PSR_RD{INVALID KEY?}
    CHK_PSR_RD -->|Yes| ERR_PSR_RD[ERR: 'Process definition<br>not found'] --> ERR
    CHK_PSR_RD -->|No| CHK_RESTART{PSR-RESTARTABLE?}
    CHK_RESTART -->|Yes| ACT_R[Set WS-ACTION-RESTART]
    CHK_RESTART -->|No| CHK_MAX{BCT-RESTART-COUNT<br>> BCT-MAX-RESTARTS?}
    CHK_MAX -->|Yes| ACT_T[Set WS-ACTION-TERMINATE]
    CHK_MAX -->|No| ACT_B[Set WS-ACTION-BYPASS]

    ACT_R --> EXEC_RCV[2120-EXECUTE-RECOVERY]
    ACT_T --> EXEC_RCV
    ACT_B --> EXEC_RCV

    EXEC_RCV --> EVAL_ACT{Evaluate<br>WS-RECOVERY-ACTION}
    EVAL_ACT -->|RESTART| RESTART[2121-RESTART-PROCESS]
    EVAL_ACT -->|BYPASS| BYPASS[2122-BYPASS-PROCESS]
    EVAL_ACT -->|TERMINATE| TERMINATE_P[2123-TERMINATE-PROCESS]

    RESTART --> RST_UPD[Set BCT-STATUS = READY<br>ADD 1 TO BCT-RESTART-COUNT<br>Set BCT-ATTEMPT-TS = NOW]
    RST_UPD --> RST_RW[REWRITE BATCH-CONTROL-RECORD]
    RST_RW --> CHK_RST_RW{INVALID KEY?}
    CHK_RST_RW -->|Yes| ERR_RST[ERR: 'Error updating<br>control record'] --> ERR
    CHK_RST_RW -->|No| RCV_DONE([Recovery Action Complete])

    BYPASS --> BYP_UPD[Set BCT-STATUS = DONE<br>BCT-RETURN-CODE = WARNING<br>BCT-ERROR-DESC = 'Process<br>bypassed by recovery']
    BYP_UPD --> BYP_RW[REWRITE BATCH-CONTROL-RECORD]
    BYP_RW --> CHK_BYP_RW{INVALID KEY?}
    CHK_BYP_RW -->|Yes| ERR_BYP[ERR: 'Error updating<br>control record'] --> ERR
    CHK_BYP_RW -->|No| RCV_DONE

    TERMINATE_P --> TRM_UPD[Set BCT-STATUS = ERROR<br>BCT-RETURN-CODE = ERROR<br>BCT-ERROR-DESC = 'Process<br>terminated by recovery']
    TRM_UPD --> TRM_RW[REWRITE BATCH-CONTROL-RECORD]
    TRM_RW --> CHK_TRM_RW{INVALID KEY?}
    CHK_TRM_RW -->|Yes| ERR_TRM[ERR: 'Error updating<br>control record'] --> ERR
    CHK_TRM_RW -->|No| RCV_DONE

    %% --- 2200 RECOVER-SEQUENCE ---
    RCV_SEQ --> SEQ_POS[MOVE LS-PROCESS-DATE<br>TO BCT-PROCESS-DATE<br>MOVE LOW-VALUES<br>TO BCT-JOB-NAME]
    SEQ_POS --> SEQ_START[START BATCH-CONTROL-FILE<br>KEY > BCT-KEY]
    SEQ_START --> CHK_SEQ_ST{INVALID KEY?}
    CHK_SEQ_ST -->|Yes| ERR_SEQ_ST[ERR: 'No processes<br>found for date'] --> ERR
    CHK_SEQ_ST -->|No| SEQ_LOOP{WS-BCT-STATUS<br>= '10'?}
    SEQ_LOOP -->|Yes| SEQ_DONE([Sequence Recovery<br>Complete])
    SEQ_LOOP -->|No| SEQ_READ[READ BATCH-CONTROL-FILE<br>NEXT RECORD]
    SEQ_READ -->|AT END| SEQ_END[MOVE '10' TO<br>WS-BCT-STATUS] --> SEQ_LOOP
    SEQ_READ -->|NOT AT END| CHK_SEQ_DATE{BCT-PROCESS-DATE<br>= LS-PROCESS-DATE?}
    CHK_SEQ_DATE -->|Yes| SEQ_RECOVER[PERFORM<br>2100-RECOVER-PROCESS] --> SEQ_LOOP
    CHK_SEQ_DATE -->|No| SEQ_LOOP

    %% --- 2300 RECOVER-ALL ---
    RCV_ALL --> ALL_POS[MOVE LOW-VALUES<br>TO BCT-KEY]
    ALL_POS --> ALL_START[START BATCH-CONTROL-FILE<br>KEY > BCT-KEY]
    ALL_START --> CHK_ALL_ST{INVALID KEY?}
    CHK_ALL_ST -->|Yes| ERR_ALL_ST[ERR: 'No processes<br>found'] --> ERR
    CHK_ALL_ST -->|No| ALL_LOOP{WS-BCT-STATUS<br>= '10'?}
    ALL_LOOP -->|Yes| ALL_DONE([All Recovery<br>Complete])
    ALL_LOOP -->|No| ALL_READ[READ BATCH-CONTROL-FILE<br>NEXT RECORD]
    ALL_READ -->|AT END| ALL_END[MOVE '10' TO<br>WS-BCT-STATUS] --> ALL_LOOP
    ALL_READ -->|NOT AT END| ALL_SET[MOVE BCT-JOB-NAME<br>TO LS-PROCESS-ID] --> ALL_RECOVER[PERFORM<br>2100-RECOVER-PROCESS] --> ALL_LOOP

    %% ============================================================
    %% 3000 — TERMINATE RECOVERY
    %% ============================================================
    TERM --> FINAL[3100-UPDATE-FINAL-STATUS]
    FINAL --> CHK_RC{LS-RETURN-CODE<br>= ZERO?}
    CHK_RC -->|Yes| MSG_OK[ERR-TEXT = 'Recovery<br>completed successfully']
    CHK_RC -->|No| MSG_ERR[ERR-TEXT = 'Recovery<br>completed with errors']
    MSG_OK --> CALL_ERRP[CALL 'ERRPROC'<br>USING ERR-MESSAGE]
    MSG_ERR --> CALL_ERRP
    CALL_ERRP --> CLOSE[3200-CLOSE-FILES]

    CLOSE --> CLOSE_FILES[CLOSE BATCH-CONTROL-FILE<br>CLOSE PROCESS-SEQ-FILE]
    CLOSE_FILES --> CHK_CLOSE{File status<br>errors?}
    CHK_CLOSE -->|Yes| ERR_CLOSE[ERR: 'Error closing<br>files'] --> ERR
    CHK_CLOSE -->|No| TERM_DONE([Termination Complete])

    %% ============================================================
    %% 9000 — ERROR ROUTINE
    %% ============================================================
    ERR --> ERR_SET[MOVE 'RCVPRC00' TO ERR-PROGRAM<br>MOVE BCT-RC-ERROR<br>TO LS-RETURN-CODE]
    ERR_SET --> ERR_CALL[CALL 'ERRPROC'<br>USING ERR-MESSAGE]

    %% ============================================================
    %% Styling
    %% ============================================================
    classDef errorNode fill:#f8d7da,stroke:#dc3545,color:#721c24
    classDef fileIO fill:#d1ecf1,stroke:#0c5460,color:#0c5460
    classDef decision fill:#fff3cd,stroke:#856404,color:#856404
    classDef action fill:#d4edda,stroke:#155724,color:#155724
    classDef terminal fill:#e2e3e5,stroke:#383d41,color:#383d41

    class ERR,ERR_SET,ERR_CALL,ERR_INVALID,ERR_OPEN_BCT,ERR_OPEN_PSR,ERR_DATE,ERR_TYPE,ERR_PID,ERR_RD,ERR_PSR_RD,ERR_RST,ERR_BYP,ERR_TRM,ERR_SEQ_ST,ERR_ALL_ST,ERR_CLOSE errorNode
    class OPEN_BCT,OPEN_PSR,READ_BCT,READ_PSR,SEQ_START,SEQ_READ,ALL_START,ALL_READ,RST_RW,BYP_RW,TRM_RW,CLOSE_FILES fileIO
    class CHK_BCT,CHK_PSR,CHK_DATE,CHK_TYPE,CHK_PID,CHK_READ,CHK_PSR_RD,CHK_RESTART,CHK_MAX,CHK_RST_RW,CHK_BYP_RW,CHK_TRM_RW,CHK_SEQ_ST,CHK_SEQ_DATE,CHK_ALL_ST,CHK_RC,CHK_CLOSE,EVAL_MODE,EVAL_ACT,SEQ_LOOP,ALL_LOOP decision
    class RST_UPD,BYP_UPD,TRM_UPD,SET_KEY,SET_WS,SEQ_POS,ALL_POS,ALL_SET action
    class START,GOBACK,INIT_DONE,RCV_DONE,SEQ_DONE,ALL_DONE,TERM_DONE terminal
```

---

## Paragraph Reference

| Paragraph | Purpose |
|-----------|---------|
| `0000-MAIN` | Entry point. Dispatches to INIT, RECV, or TERM based on `LS-FUNCTION`. Sets `RETURN-CODE` and returns via `GOBACK`. |
| `1000-INITIALIZE-RECOVERY` | Orchestrates initialization: open files, validate request, set recovery mode. |
| `1100-OPEN-FILES` | Opens `BATCH-CONTROL-FILE` (I-O) and `PROCESS-SEQ-FILE` (INPUT). Checks file statuses. |
| `1200-VALIDATE-REQUEST` | Validates `LS-PROCESS-DATE` is not spaces and `LS-RECOVERY-TYPE` is `P`, `S`, or `A`. |
| `1300-SET-RECOVERY-MODE` | Sets `WS-RECOVERY-MODE`. Validates `LS-PROCESS-ID` is present when mode is `P`. |
| `2000-PROCESS-RECOVERY` | Dispatches to recovery handler based on mode: Process, Sequence, or All. |
| `2100-RECOVER-PROCESS` | Reads the batch control record by key, determines recovery action, and executes it. |
| `2110-DETERMINE-ACTION` | Reads process definition from `PROCESS-SEQ-FILE`. Sets action to Restart (if restartable), Terminate (if max restarts exceeded), or Bypass. |
| `2120-EXECUTE-RECOVERY` | Dispatches to Restart, Bypass, or Terminate based on determined action. |
| `2121-RESTART-PROCESS` | Sets status to READY, increments restart count, timestamps, and rewrites the control record. |
| `2122-BYPASS-PROCESS` | Sets status to DONE with WARNING return code and rewrites the control record. |
| `2123-TERMINATE-PROCESS` | Sets status to ERROR with ERROR return code and rewrites the control record. |
| `2200-RECOVER-SEQUENCE` | Positions at the given date and sequentially recovers all matching processes. |
| `2300-RECOVER-ALL` | Positions at the start of the file and recovers every process record. |
| `3000-TERMINATE-RECOVERY` | Orchestrates termination: logs final status, closes files. |
| `3100-UPDATE-FINAL-STATUS` | Logs success or error message via `CALL 'ERRPROC'`. |
| `3200-CLOSE-FILES` | Closes both VSAM files and checks for close errors. |
| `9000-ERROR-ROUTINE` | Sets program name and error return code, calls `ERRPROC` for error logging. |

## External Program Calls

| Program | Usage |
|---------|-------|
| `ERRPROC` | Called in `9000-ERROR-ROUTINE` and `3100-UPDATE-FINAL-STATUS` for error/status logging. Receives `ERR-MESSAGE`. |

## File I/O Summary

| File | DD Name | Access | Operations |
|------|---------|--------|------------|
| `BATCH-CONTROL-FILE` | `BCHCTL` | I-O (Indexed, Dynamic) | `OPEN`, `READ` (keyed), `START`, `READ NEXT`, `REWRITE`, `CLOSE` |
| `PROCESS-SEQ-FILE` | `PRCSEQ` | INPUT (Indexed, Dynamic) | `OPEN`, `READ` (keyed), `CLOSE` |

## Copybooks Used

| Copybook | Section | Purpose |
|----------|---------|---------|
| `BCHCTL` | FILE SECTION | Record layout for Batch Control File |
| `PRCSEQ` | FILE SECTION | Record layout for Process Sequence File |
| `BCHCON` | WORKING-STORAGE | Batch processing constants (status codes, return codes) |
| `ERRHAND` | WORKING-STORAGE | Error handling fields (`ERR-TEXT`, `ERR-PROGRAM`, `ERR-MESSAGE`) |
