# DB2STAT — DB2 Statistics Collector

## Program Description

**DB2STAT** is a shared COBOL utility program that collects and reports DB2 execution statistics for other programs in the Investment Portfolio Management System. It is invoked via `CALL` with a linkage-section request structure (`LS-STAT-REQUEST`) that specifies one of four function codes:

| Function | Code   | Purpose |
|----------|--------|---------|
| INIT     | `INIT` | Initialize statistics tracking — creates a DB2 Global Temporary Table (`SESSION.DBSTATS`) and inserts an initial row for the calling program. |
| UPDT     | `UPDT` | Update running statistics (rows read/inserted/updated/deleted, commits, rollbacks) from the caller's counters. |
| TERM     | `TERM` | Finalize statistics — captures end timestamp, calculates elapsed and CPU time, updates the table, and displays results. |
| DISP     | `DISP` | Display current statistics by querying the temporary table and writing formatted output. |

### Key Characteristics

- **Type**: Called sub-program (not standalone) — receives parameters via `LINKAGE SECTION`.
- **DB2 Operations**: `DECLARE GLOBAL TEMPORARY TABLE`, `INSERT`, `UPDATE`, `SELECT INTO`.
- **Copybooks**: `SQLCA` (DB2 communication area), `DBPROC` (DB2 standard procedures), `ERRHAND` (error handling definitions).
- **External CALL**: Invokes `ERRPROC` for error logging when any DB2 operation fails.
- **No File I/O**: All persistence is through the DB2 temporary table `SESSION.DBSTATS`.
- **Error Handling**: Every DB2 operation checks `SQLCODE`; failures route to `9000-ERROR-ROUTINE` which sets return code 12 and calls `ERRPROC`.

---

## Logic Flow Diagram

```mermaid
flowchart TD
    START(["CALL 'DB2STAT' USING LS-STAT-REQUEST"])

    START --> MAIN["0000-MAIN<br/>EVALUATE LS-FUNCTION"]

    MAIN -->|FUNC-INIT| INIT["1000-INITIALIZE"]
    MAIN -->|FUNC-UPDT| UPDT["2000-UPDATE-STATS"]
    MAIN -->|FUNC-TERM| TERM["3000-TERMINATE"]
    MAIN -->|FUNC-DISP| DISP["4000-DISPLAY-STATS"]
    MAIN -->|OTHER| ERRINV["Move 'Invalid function code'<br/>to ERR-TEXT"]
    ERRINV --> ERR["9000-ERROR-ROUTINE"]

    %% ---- INIT branch ----
    INIT --> INIT1["INITIALIZE WS-STATS-RECORD<br/>MOVE LS-PROGRAM-ID → WS-PROGRAM-ID"]
    INIT1 --> INIT2["ACCEPT WS-CURRENT-TIMESTAMP<br/>FROM TIME STAMP"]
    INIT2 --> INIT3["Move timestamp →<br/>WS-START-TIME &amp; WS-START-TIMESTAMP"]
    INIT3 --> CREATE["1100-CREATE-STATS-TABLE<br/>EXEC SQL DECLARE GLOBAL<br/>TEMPORARY TABLE SESSION.DBSTATS"]
    CREATE --> CCHK{"SQLCODE = 0<br/>or -601?"}
    CCHK -->|Yes| INSERT["1200-INSERT-INITIAL<br/>EXEC SQL INSERT INTO<br/>SESSION.DBSTATS<br/>(initial zeroed row)"]
    CCHK -->|No| CERR["Move 'Error creating<br/>stats table' to ERR-TEXT"] --> ERR
    INSERT --> ICHK{"SQLCODE = 0?"}
    ICHK -->|Yes| IRET["MOVE 0 → LS-RETURN-CODE"] --> GOBACK
    ICHK -->|No| IERR["Move 'Error initializing<br/>stats' to ERR-TEXT"] --> ERR

    %% ---- UPDATE branch ----
    UPDT --> UMOVE["Move linkage counters →<br/>WS-ROWS-READ, WS-ROWS-INSERTED,<br/>WS-ROWS-UPDATED, WS-ROWS-DELETED,<br/>WS-COMMITS, WS-ROLLBACKS"]
    UMOVE --> USQL["EXEC SQL UPDATE SESSION.DBSTATS<br/>SET row counts &amp; counters<br/>WHERE PROGRAM_ID = :WS-PROGRAM-ID"]
    USQL --> UCHK{"SQLCODE = 0?"}
    UCHK -->|Yes| URET["MOVE 0 → LS-RETURN-CODE"] --> GOBACK
    UCHK -->|No| UERR["Move 'Error updating<br/>stats' to ERR-TEXT"] --> ERR

    %% ---- TERMINATE branch ----
    TERM --> TTIME["ACCEPT WS-CURRENT-TIMESTAMP<br/>Move → WS-END-TIME"]
    TTIME --> TCALC["3100-CALC-TIMES<br/>COMPUTE WS-ELAPSED-TIME =<br/>END - START timestamps<br/>WS-CPU-TIME = ELAPSED × 0.65"]
    TCALC --> TSQL["EXEC SQL UPDATE SESSION.DBSTATS<br/>SET END_TIME, CPU_TIME,<br/>ELAPSED_TIME<br/>WHERE PROGRAM_ID = :WS-PROGRAM-ID"]
    TSQL --> TCHK{"SQLCODE = 0?"}
    TCHK -->|Yes| TDISP["MOVE 0 → LS-RETURN-CODE<br/>PERFORM 4000-DISPLAY-STATS"]
    TCHK -->|No| TERR["Move 'Error finalizing<br/>stats' to ERR-TEXT"] --> ERR
    TDISP --> DSQL

    %% ---- DISPLAY branch ----
    DISP --> DSQL["4000-DISPLAY-STATS<br/>EXEC SQL SELECT stats<br/>INTO :WS-STATS-RECORD<br/>FROM SESSION.DBSTATS<br/>WHERE PROGRAM_ID = :WS-PROGRAM-ID"]
    DSQL --> DCHK{"SQLCODE = 0?"}
    DCHK -->|Yes| DOUT["DISPLAY formatted statistics:<br/>Rows Read / Inserted / Updated / Deleted<br/>Commits / Rollbacks<br/>CPU Time / Elapsed Time"]
    DCHK -->|No| DERR["Move 'Error retrieving<br/>stats' to ERR-TEXT"] --> ERR
    DOUT --> DRET["MOVE 0 → LS-RETURN-CODE"] --> GOBACK

    %% ---- ERROR routine ----
    ERR["9000-ERROR-ROUTINE<br/>MOVE 'DB2STAT' → ERR-PROGRAM<br/>MOVE 12 → LS-RETURN-CODE<br/>CALL 'ERRPROC' USING ERR-MESSAGE"]
    ERR --> GOBACK

    GOBACK(["GOBACK<br/>(Return to caller)"])

    %% ---- Styling ----
    classDef startEnd fill:#4a6fa5,color:#fff,stroke:#2c4a73,stroke-width:2px
    classDef process fill:#e8f0fe,color:#1a1a2e,stroke:#4a6fa5,stroke-width:1px
    classDef decision fill:#fff3cd,color:#1a1a2e,stroke:#c9a825,stroke-width:2px
    classDef error fill:#f8d7da,color:#721c24,stroke:#f5c6cb,stroke-width:2px
    classDef db2 fill:#d4edda,color:#155724,stroke:#c3e6cb,stroke-width:1px
    classDef display fill:#e2d9f3,color:#3b2069,stroke:#b8a9c9,stroke-width:1px

    class START,GOBACK startEnd
    class MAIN,INIT,INIT1,INIT2,INIT3,UMOVE,TTIME,TCALC,IRET,URET,TDISP,DRET process
    class CCHK,ICHK,UCHK,TCHK,DCHK decision
    class ERRINV,CERR,IERR,UERR,TERR,DERR,ERR error
    class CREATE,INSERT,USQL,TSQL,DSQL db2
    class DOUT display
```

---

## Paragraph Reference

| Paragraph | Lines | Purpose |
|-----------|-------|---------|
| `0000-MAIN` | 58–74 | Entry point — dispatches to the appropriate function based on `LS-FUNCTION`. |
| `1000-INITIALIZE` | 76–86 | Initializes stats record, captures start timestamp, creates temp table, inserts initial row. |
| `1100-CREATE-STATS-TABLE` | 88–109 | Executes `DECLARE GLOBAL TEMPORARY TABLE SESSION.DBSTATS`. Tolerates SQLCODE -601 (table already exists). |
| `1200-INSERT-INITIAL` | 111–128 | Inserts zeroed-out initial statistics row for the calling program. |
| `2000-UPDATE-STATS` | 130–155 | Copies linkage counters to working storage and updates the DB2 row. |
| `3000-TERMINATE` | 157–178 | Captures end timestamp, calculates times, finalizes DB2 row, and displays results. |
| `3100-CALC-TIMES` | 180–187 | Computes elapsed time (end − start) and estimates CPU time as 65% of elapsed. |
| `4000-DISPLAY-STATS` | 189–222 | Queries the temp table and writes formatted statistics to SYSOUT via `DISPLAY`. |
| `9000-ERROR-ROUTINE` | 224–228 | Sets return code 12 and calls external `ERRPROC` for error logging. |

## DB2 Operations Summary

| Operation | SQL Statement | Table | Paragraph |
|-----------|--------------|-------|-----------|
| DDL | `DECLARE GLOBAL TEMPORARY TABLE` | `SESSION.DBSTATS` | `1100-CREATE-STATS-TABLE` |
| INSERT | `INSERT INTO SESSION.DBSTATS` | `SESSION.DBSTATS` | `1200-INSERT-INITIAL` |
| UPDATE | `UPDATE SESSION.DBSTATS SET row counts` | `SESSION.DBSTATS` | `2000-UPDATE-STATS` |
| UPDATE | `UPDATE SESSION.DBSTATS SET times` | `SESSION.DBSTATS` | `3000-TERMINATE` |
| SELECT | `SELECT ... INTO :WS-STATS-RECORD` | `SESSION.DBSTATS` | `4000-DISPLAY-STATS` |

## External Dependencies

| Dependency | Type | Source |
|------------|------|--------|
| `SQLCA` | Copybook | DB2 SQL Communication Area |
| `DBPROC` | Copybook | `src/copybook/db2/DBPROC.cpy` — standard DB2 procedures |
| `ERRHAND` | Copybook | `src/copybook/common/ERRHAND.cpy` — error handling definitions |
| `ERRPROC` | Called program | External error processing routine |
