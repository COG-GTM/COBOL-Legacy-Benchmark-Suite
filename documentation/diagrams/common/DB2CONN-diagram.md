# DB2CONN — DB2 Connection Manager

## Program Description

**DB2CONN** is a shared utility COBOL program that provides centralized DB2 connection management for the Investment Portfolio Management System. It is invoked via `CALL` from other programs through the `LINKAGE SECTION` interface and supports three operations:

- **CONN** — Establish a DB2 connection with retry logic (up to 3 attempts with a delay between retries).
- **DISC** — Gracefully disconnect from DB2 after committing pending work.
- **STAT** — Check whether the DB2 connection is still active by querying `SYSIBM.SYSDUMMY1`.

The program uses copybooks `SQLCA` (DB2 SQL Communication Area), `DBPROC` (standard DB2 procedures and retry parameters), and `ERRHAND` (standard error message structure and return codes). Return codes follow the standard convention: 0 = success, 4 = warning, 8 = error, 12 = severe.

### Key Characteristics

| Aspect | Detail |
|---|---|
| **Program ID** | `DB2CONN` |
| **Type** | Called subprogram (no `STOP RUN`) |
| **Interface** | `LINKAGE SECTION` / `PROCEDURE DIVISION USING` |
| **Copybooks** | `SQLCA`, `DBPROC`, `ERRHAND` |
| **CALL Statements** | `CALL 'DELAY'` (retry wait), `CALL 'ERRPROC'` (error logging) |
| **DB2 Operations** | `CONNECT TO`, `COMMIT WORK`, `CONNECT RESET`, `SELECT CURRENT SERVER` |
| **File I/O** | None |
| **CICS** | None (batch utility) |

---

## Logic Flow Diagram

```mermaid
flowchart TD
    START(["`**ENTRY: PROCEDURE DIVISION USING LS-DB2-REQUEST**`"]) --> MAIN["`**0000-MAIN**
    Evaluate LS-FUNCTION`"]

    MAIN -->|"LS-FUNCTION = 'CONN'"| CONN["`**1000-CONNECT**`"]
    MAIN -->|"LS-FUNCTION = 'DISC'"| DISC["`**2000-DISCONNECT**`"]
    MAIN -->|"LS-FUNCTION = 'STAT'"| STAT["`**3000-CHECK-STATUS**`"]
    MAIN -->|"OTHER"| ERR_INVALID["`Set ERR-TEXT =
    'Invalid function code'`"]
    ERR_INVALID --> ERR9000["`**9000-ERROR-ROUTINE**
    Set ERR-PROGRAM = 'DB2CONN'
    Set LS-RETURN-CODE = 12
    CALL 'ERRPROC' USING ERR-MESSAGE`"]
    ERR9000 --> GOBACK

    %% ===== 1000-CONNECT =====
    CONN --> CONN_INIT["`Set WS-DISCONNECTED = TRUE
    Set WS-RETRY-COUNT = 0
    Move LS-DB-NAME → WS-DB-NAME
    Move LS-PLAN-NAME → WS-PLAN-NAME`"]
    CONN_INIT --> LOOP_CHECK{"`WS-CONNECTED?
    OR RETRY-COUNT >= MAX-RETRIES (3)?`"}

    LOOP_CHECK -->|"Exit loop"| GOBACK

    LOOP_CHECK -->|"Continue loop"| SQL_CONNECT["`**EXEC SQL**
    CONNECT TO :WS-DB-NAME`"]

    SQL_CONNECT --> CHECK_CONN_SQL{"`SQLCODE = 0?`"}

    CHECK_CONN_SQL -->|"Yes"| CONN_OK["`Set WS-CONNECTED = TRUE
    Set LS-RETURN-CODE = 0`"]
    CONN_OK --> LOOP_CHECK

    CHECK_CONN_SQL -->|"No"| CONN_FAIL["`Add 1 to WS-RETRY-COUNT`"]
    CONN_FAIL --> HANDLE_ERR["`**1100-HANDLE-CONN-ERROR**
    Move SQLCODE → LS-SQLCODE`"]

    HANDLE_ERR --> EVAL_SQL{"`Evaluate SQLCODE`"}
    EVAL_SQL -->|"-30081"| MSG1["`LS-ERROR-MSG =
    'Maximum connections exceeded'`"]
    EVAL_SQL -->|"-99999"| MSG2["`LS-ERROR-MSG =
    'Network error connecting to DB2'`"]
    EVAL_SQL -->|"OTHER"| MSG3["`LS-ERROR-MSG =
    'General DB2 connection error'`"]

    MSG1 --> SET_RC12["`Set LS-RETURN-CODE = 12`"]
    MSG2 --> SET_RC12
    MSG3 --> SET_RC12

    SET_RC12 --> RETRY_CHECK{"`RETRY-COUNT < MAX-RETRIES
    AND NOT WS-CONNECTED?`"}
    RETRY_CHECK -->|"Yes"| DELAY["`CALL 'DELAY'
    USING DB2-RETRY-WAIT`"]
    DELAY --> LOOP_CHECK
    RETRY_CHECK -->|"No"| LOOP_CHECK

    %% ===== 2000-DISCONNECT =====
    DISC --> CHECK_CONNECTED{"`WS-CONNECTED?`"}
    CHECK_CONNECTED -->|"No"| GOBACK

    CHECK_CONNECTED -->|"Yes"| COMMIT["`**EXEC SQL**
    COMMIT WORK`"]
    COMMIT --> RESET["`**EXEC SQL**
    CONNECT RESET`"]
    RESET --> CHECK_DISC_SQL{"`SQLCODE = 0?`"}

    CHECK_DISC_SQL -->|"Yes"| DISC_OK["`Set WS-DISCONNECTED = TRUE
    Set LS-RETURN-CODE = 0`"]
    DISC_OK --> GOBACK

    CHECK_DISC_SQL -->|"No"| DISC_FAIL["`Move SQLCODE → LS-SQLCODE
    LS-ERROR-MSG =
    'Error disconnecting from DB2'
    Set LS-RETURN-CODE = 8`"]
    DISC_FAIL --> GOBACK

    %% ===== 3000-CHECK-STATUS =====
    STAT --> SQL_STATUS["`**EXEC SQL**
    SELECT CURRENT SERVER
    INTO :WS-DB-NAME
    FROM SYSIBM.SYSDUMMY1`"]
    SQL_STATUS --> CHECK_STAT_SQL{"`SQLCODE = 0?`"}

    CHECK_STAT_SQL -->|"Yes"| STAT_OK["`Set WS-CONNECTED = TRUE
    Set LS-RETURN-CODE = 0`"]
    STAT_OK --> GOBACK

    CHECK_STAT_SQL -->|"No"| STAT_FAIL["`Set WS-DISCONNECTED = TRUE
    Move SQLCODE → LS-SQLCODE
    LS-ERROR-MSG =
    'DB2 connection not active'
    Set LS-RETURN-CODE = 4`"]
    STAT_FAIL --> GOBACK

    %% ===== GOBACK =====
    GOBACK(["`**GOBACK**
    Return to caller`"])

    %% ===== Styling =====
    classDef entryExit fill:#1a1a2e,color:#e0e0ff,stroke:#7b68ee,stroke-width:2px
    classDef paragraph fill:#16213e,color:#e0e0ff,stroke:#4a90d9,stroke-width:2px
    classDef decision fill:#0f3460,color:#e0e0ff,stroke:#e94560,stroke-width:2px
    classDef sqlOp fill:#1b2838,color:#c3f0ca,stroke:#2ecc71,stroke-width:2px
    classDef callOp fill:#2d132c,color:#f8c8dc,stroke:#e91e63,stroke-width:2px
    classDef errorNode fill:#3b0a0a,color:#ff9999,stroke:#ff4444,stroke-width:2px
    classDef successNode fill:#0a3b0a,color:#99ff99,stroke:#44ff44,stroke-width:2px

    class START,GOBACK entryExit
    class MAIN,CONN,DISC,STAT paragraph
    class LOOP_CHECK,CHECK_CONN_SQL,EVAL_SQL,RETRY_CHECK,CHECK_CONNECTED,CHECK_DISC_SQL,CHECK_STAT_SQL decision
    class SQL_CONNECT,COMMIT,RESET,SQL_STATUS sqlOp
    class DELAY,ERR9000 callOp
    class CONN_FAIL,DISC_FAIL,STAT_FAIL,ERR_INVALID,MSG1,MSG2,MSG3,SET_RC12,HANDLE_ERR errorNode
    class CONN_OK,DISC_OK,STAT_OK,CONN_INIT successNode
```
