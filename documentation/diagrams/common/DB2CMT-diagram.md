# DB2CMT — DB2 Commit Controller: Logic Flow Diagram

## Program Overview

**DB2CMT** is a reusable COBOL sub-program that provides centralized DB2 transaction commit control for the Investment Portfolio Management System. It is called by other programs via the `CALL` statement and receives instructions through the `LS-COMMIT-REQUEST` linkage area.

The program supports six functions:

| Function Code | Name         | Description                                      |
|---------------|--------------|--------------------------------------------------|
| `INIT`        | Initialize   | Resets commit/rollback/savepoint counters        |
| `CMIT`        | Commit       | Conditionally issues a DB2 `COMMIT WORK`         |
| `RBAK`        | Rollback     | Issues a DB2 `ROLLBACK WORK`                     |
| `SAVE`        | Savepoint    | Creates a named DB2 savepoint                    |
| `REST`        | Restore      | Rolls back to a named DB2 savepoint              |
| `STAT`        | Statistics   | Displays commit/rollback/savepoint counts        |

### Key Characteristics

- **No file I/O** — all persistence is handled through DB2 SQL operations.
- **Copybooks used**: `SQLCA` (SQL Communication Area), `DBPROC` (DB2 standard procedures), `ERRHAND` (standard error handling definitions).
- **External CALL statements**: `ERRPROC` (error processing routine) and `DB2ERR` (DB2-specific error logging).
- **Linkage-driven**: The caller controls behavior by setting `LS-FUNCTION`, `LS-SAVEPOINT-NAME`, `LS-RECORDS-PROC`, `LS-COMMIT-FREQ`, and `LS-FORCE-FLAG`.

## Mermaid Flowchart

```mermaid
flowchart TD
    START(["`**ENTRY**
    PROCEDURE DIVISION
    USING LS-COMMIT-REQUEST`"]) --> MAIN["0000-MAIN
    EVALUATE TRUE"]

    MAIN --> CHK_INIT{{"LS-FUNCTION = 'INIT'?"}}
    CHK_INIT -- Yes --> INIT["1000-INITIALIZE
    Reset WS-COMMIT-STATS
    Set LS-RETURN-CODE = 0"]
    INIT --> GOBACK

    CHK_INIT -- No --> CHK_CMIT{{"LS-FUNCTION = 'CMIT'?"}}
    CHK_CMIT -- Yes --> COMMIT_CHK{"LS-RECORDS-PROC >= LS-COMMIT-FREQ
    OR LS-FORCE-COMMIT = 'Y'?"}
    COMMIT_CHK -- No --> GOBACK
    COMMIT_CHK -- Yes --> ISSUE_COMMIT["2100-ISSUE-COMMIT
    EXEC SQL COMMIT WORK"]
    ISSUE_COMMIT --> COMMIT_RC{"SQLCODE = 0?"}
    COMMIT_RC -- Yes --> COMMIT_OK["Increment WS-COMMIT-COUNT
    Set LS-RETURN-CODE = 0"]
    COMMIT_OK --> GOBACK
    COMMIT_RC -- No --> COMMIT_FAIL["Set LS-SQLCODE = SQLCODE
    Set LS-ERROR-MSG = 'Commit failed'
    Set LS-RETURN-CODE = 8"]
    COMMIT_FAIL --> LOG_ERR_COMMIT["9100-LOG-ERROR
    CALL 'DB2ERR'
    USING LS-ERROR-INFO"]
    LOG_ERR_COMMIT --> GOBACK

    CHK_CMIT -- No --> CHK_RBACK{{"LS-FUNCTION = 'RBAK'?"}}
    CHK_RBACK -- Yes --> ROLLBACK["3000-ROLLBACK
    EXEC SQL ROLLBACK WORK"]
    ROLLBACK --> RBACK_RC{"SQLCODE = 0?"}
    RBACK_RC -- Yes --> RBACK_OK["Increment WS-ROLLBACK-COUNT
    Set LS-RETURN-CODE = 0"]
    RBACK_OK --> GOBACK
    RBACK_RC -- No --> RBACK_FAIL["Set LS-SQLCODE = SQLCODE
    Set LS-ERROR-MSG = 'Rollback failed'
    Set LS-RETURN-CODE = 8"]
    RBACK_FAIL --> LOG_ERR_RBACK["9100-LOG-ERROR
    CALL 'DB2ERR'
    USING LS-ERROR-INFO"]
    LOG_ERR_RBACK --> GOBACK

    CHK_RBACK -- No --> CHK_SAVE{{"LS-FUNCTION = 'SAVE'?"}}
    CHK_SAVE -- Yes --> SAVEPOINT["4000-SAVEPOINT
    Copy LS-SAVEPOINT-NAME
    to WS-SAVEPOINT-ID"]
    SAVEPOINT --> SAVE_SQL["EXEC SQL SAVEPOINT
    :WS-SAVEPOINT-ID
    ON ROLLBACK RETAIN CURSORS"]
    SAVE_SQL --> SAVE_RC{"SQLCODE = 0?"}
    SAVE_RC -- Yes --> SAVE_OK["Increment WS-SAVEPOINT-COUNT
    Set LS-RETURN-CODE = 0"]
    SAVE_OK --> GOBACK
    SAVE_RC -- No --> SAVE_FAIL["Set LS-SQLCODE = SQLCODE
    Set LS-ERROR-MSG =
    'Savepoint creation failed'
    Set LS-RETURN-CODE = 8"]
    SAVE_FAIL --> LOG_ERR_SAVE["9100-LOG-ERROR
    CALL 'DB2ERR'
    USING LS-ERROR-INFO"]
    LOG_ERR_SAVE --> GOBACK

    CHK_SAVE -- No --> CHK_REST{{"LS-FUNCTION = 'REST'?"}}
    CHK_REST -- Yes --> RESTORE["5000-RESTORE
    Copy LS-SAVEPOINT-NAME
    to WS-SAVEPOINT-ID"]
    RESTORE --> REST_SQL["EXEC SQL ROLLBACK
    TO SAVEPOINT :WS-SAVEPOINT-ID"]
    REST_SQL --> REST_RC{"SQLCODE = 0?"}
    REST_RC -- Yes --> REST_OK["Increment WS-ROLLBACK-COUNT
    Set LS-RETURN-CODE = 0"]
    REST_OK --> GOBACK
    REST_RC -- No --> REST_FAIL["Set LS-SQLCODE = SQLCODE
    Set LS-ERROR-MSG =
    'Savepoint restore failed'
    Set LS-RETURN-CODE = 8"]
    REST_FAIL --> LOG_ERR_REST["9100-LOG-ERROR
    CALL 'DB2ERR'
    USING LS-ERROR-INFO"]
    LOG_ERR_REST --> GOBACK

    CHK_REST -- No --> CHK_STAT{{"LS-FUNCTION = 'STAT'?"}}
    CHK_STAT -- Yes --> STATS["6000-STATISTICS
    DISPLAY commit/rollback/
    savepoint counts"]
    STATS --> GOBACK

    CHK_STAT -- No --> ERR_INVALID["9000-ERROR-ROUTINE
    Set ERR-PROGRAM = 'DB2CMT'
    Set LS-RETURN-CODE = 12"]
    ERR_INVALID --> CALL_ERRPROC["CALL 'ERRPROC'
    USING ERR-MESSAGE"]
    CALL_ERRPROC --> GOBACK

    GOBACK(["`**GOBACK**
    Return to caller`"])

    style START fill:#2d6a4f,stroke:#1b4332,color:#fff
    style GOBACK fill:#2d6a4f,stroke:#1b4332,color:#fff
    style MAIN fill:#264653,stroke:#1d3557,color:#fff
    style INIT fill:#457b9d,stroke:#1d3557,color:#fff
    style ISSUE_COMMIT fill:#e76f51,stroke:#9b2226,color:#fff
    style ROLLBACK fill:#e76f51,stroke:#9b2226,color:#fff
    style SAVE_SQL fill:#e76f51,stroke:#9b2226,color:#fff
    style REST_SQL fill:#e76f51,stroke:#9b2226,color:#fff
    style COMMIT_FAIL fill:#9b2226,stroke:#6a040f,color:#fff
    style RBACK_FAIL fill:#9b2226,stroke:#6a040f,color:#fff
    style SAVE_FAIL fill:#9b2226,stroke:#6a040f,color:#fff
    style REST_FAIL fill:#9b2226,stroke:#6a040f,color:#fff
    style ERR_INVALID fill:#9b2226,stroke:#6a040f,color:#fff
    style LOG_ERR_COMMIT fill:#bc4749,stroke:#9b2226,color:#fff
    style LOG_ERR_RBACK fill:#bc4749,stroke:#9b2226,color:#fff
    style LOG_ERR_SAVE fill:#bc4749,stroke:#9b2226,color:#fff
    style LOG_ERR_REST fill:#bc4749,stroke:#9b2226,color:#fff
    style CALL_ERRPROC fill:#bc4749,stroke:#9b2226,color:#fff
```

## Legend

| Shape / Color | Meaning |
|---|---|
| Green rounded rectangles | Program entry and exit (`GOBACK`) |
| Dark blue rectangle | Main dispatcher (`EVALUATE TRUE`) |
| Blue rectangle | Initialization paragraph |
| Orange/red rectangles | DB2 SQL operations (`COMMIT`, `ROLLBACK`, `SAVEPOINT`) |
| Dark red rectangles | Error states (return code 8 or 12) |
| Red rectangles | External error-handling calls (`DB2ERR`, `ERRPROC`) |
| Hexagons | Function-code decision branches |
| Diamonds | Conditional logic (SQLCODE checks, commit-frequency checks) |

## DB2 Operations Summary

| Paragraph | SQL Statement | Purpose |
|---|---|---|
| `2100-ISSUE-COMMIT` | `COMMIT WORK` | Persist all pending changes |
| `3000-ROLLBACK` | `ROLLBACK WORK` | Undo all changes since last commit |
| `4000-SAVEPOINT` | `SAVEPOINT :id ON ROLLBACK RETAIN CURSORS` | Create a named recovery point |
| `5000-RESTORE` | `ROLLBACK TO SAVEPOINT :id` | Undo changes back to a named savepoint |

## External Calls

| Paragraph | Called Program | Data Passed | Purpose |
|---|---|---|---|
| `9000-ERROR-ROUTINE` | `ERRPROC` | `ERR-MESSAGE` (from `ERRHAND` copybook) | General error processing |
| `9100-LOG-ERROR` | `DB2ERR` | `LS-ERROR-INFO` (SQLCODE + error message) | DB2-specific error logging |
