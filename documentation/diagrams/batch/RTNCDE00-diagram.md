# RTNCDE00 — Standard Return Code Handler

## Program Description

**RTNCDE00** is a reusable COBOL subprogram that provides centralized return code management across the Investment Portfolio Management System. It is invoked via `CALL` from other programs using a shared `LINKAGE SECTION` communication area defined in the `RTNCODE` copybook.

The program supports five operations, dispatched by an `EVALUATE` on the request type flag (`RC-REQUEST-TYPE`):

| Request Type | Flag | Paragraph | Description |
|---|---|---|---|
| Initialize | `I` | `P100-INIT-RETURN-CODES` | Resets all return code fields to defaults |
| Set Code | `S` | `P200-SET-RETURN-CODE` | Sets a new return code and derives status |
| Get Code | `G` | `P300-GET-RETURN-CODE` | Retrieves current and highest return codes |
| Log Code | `L` | `P400-LOG-RETURN-CODE` | Inserts a return code record into DB2 `RTNCODES` table |
| Analyze | `A` | `P500-ANALYZE-CODES` | Queries DB2 for aggregate return code statistics |

### Key Characteristics

- **No File I/O**: The program does not perform any VSAM or sequential file operations.
- **DB2 Operations**: Two SQL statements — an `INSERT` (P400) and a `SELECT` aggregate query (P500) — interact with the `RTNCODES` table.
- **No CALL Statements**: RTNCDE00 does not call any other subprograms; it is a leaf-level utility.
- **Linkage Section Interface**: All data is exchanged through `RC-REQUEST-AREA` (copybook `RTNCODE`), making it a stateless, reentrant service.
- **SQLCA**: Included for DB2 return code checking (`SQLCODE`).

## Logic Flow Diagram

```mermaid
flowchart TD
    A([CALLER invokes RTNCDE00<br>via CALL using RC-REQUEST-AREA]) --> B{EVALUATE<br>RC-REQUEST-TYPE}

    %% ── P100: Initialize ────────────────────────────────
    B -- "'I' RC-INITIALIZE" --> P100[P100-INIT-RETURN-CODES]
    P100 --> P100a[INITIALIZE RC-CODES-AREA]
    P100a --> P100b[MOVE SPACES TO RC-PROGRAM-ID]
    P100b --> P100c[MOVE 0 TO RC-CURRENT-CODE]
    P100c --> P100d[MOVE 0 TO RC-HIGHEST-CODE]
    P100d --> P100e[SET RC-STATUS-SUCCESS = TRUE]
    P100e --> P100f[MOVE 0 TO RC-RESPONSE-CODE]
    P100f --> P100x([P100-EXIT])
    P100x --> Z([GOBACK to caller])

    %% ── P200: Set Return Code ───────────────────────────
    B -- "'S' RC-SET-CODE" --> P200[P200-SET-RETURN-CODE]
    P200 --> P200a{RC-NEW-CODE ><br>RC-HIGHEST-CODE?}
    P200a -- Yes --> P200b[MOVE RC-NEW-CODE<br>TO RC-HIGHEST-CODE]
    P200b --> P200c[MOVE RC-NEW-CODE<br>TO RC-CURRENT-CODE]
    P200a -- No --> P200c
    P200c --> P200d{EVALUATE<br>RC-NEW-CODE}
    P200d -- "0" --> P200e[SET RC-STATUS-SUCCESS]
    P200d -- "1 THRU 4" --> P200f[SET RC-STATUS-WARNING]
    P200d -- "5 THRU 8" --> P200g[SET RC-STATUS-ERROR]
    P200d -- "OTHER" --> P200h[SET RC-STATUS-SEVERE]
    P200e --> P200i[MOVE 0 TO RC-RESPONSE-CODE]
    P200f --> P200i
    P200g --> P200i
    P200h --> P200i
    P200i --> P200x([P200-EXIT])
    P200x --> Z

    %% ── P300: Get Return Code ───────────────────────────
    B -- "'G' RC-GET-CODE" --> P300[P300-GET-RETURN-CODE]
    P300 --> P300a[MOVE RC-CURRENT-CODE<br>TO RC-RETURN-VALUE]
    P300a --> P300b[MOVE RC-HIGHEST-CODE<br>TO RC-HIGHEST-RETURN]
    P300b --> P300c[MOVE RC-STATUS<br>TO RC-RETURN-STATUS]
    P300c --> P300d[MOVE 0 TO RC-RESPONSE-CODE]
    P300d --> P300x([P300-EXIT])
    P300x --> Z

    %% ── P400: Log Return Code (DB2 INSERT) ──────────────
    B -- "'L' RC-LOG-CODE" --> P400[P400-LOG-RETURN-CODE]
    P400 --> P400a[MOVE FUNCTION CURRENT-DATE<br>TO WS-CURRENT-TIME]
    P400a --> P400b[(DB2 INSERT INTO RTNCODES<br>TIMESTAMP, PROGRAM_ID,<br>RETURN_CODE, HIGHEST_CODE,<br>STATUS_CODE, MESSAGE_TEXT)]
    P400b --> P400c{SQLCODE = 0?}
    P400c -- Yes --> P400d[MOVE 0 TO<br>RC-RESPONSE-CODE]
    P400c -- No --> P400e[MOVE 8 TO<br>RC-RESPONSE-CODE]
    P400d --> P400x([P400-EXIT])
    P400e --> P400x
    P400x --> Z

    %% ── P500: Analyze Codes (DB2 SELECT) ────────────────
    B -- "'A' RC-ANALYZE" --> P500[P500-ANALYZE-CODES]
    P500 --> P500a[("DB2 SELECT COUNT(*),<br>MAX(RETURN_CODE),<br>MIN(RETURN_CODE)<br>FROM RTNCODES<br>WHERE PROGRAM_ID & TIME RANGE")]
    P500a --> P500b{SQLCODE = 0?}
    P500b -- Yes --> P500c[MOVE 0 TO<br>RC-RESPONSE-CODE]
    P500b -- No --> P500d[MOVE 8 TO<br>RC-RESPONSE-CODE]
    P500c --> P500x([P500-EXIT])
    P500d --> P500x
    P500x --> Z

    %% ── Styles ──────────────────────────────────────────
    style A fill:#4a90d9,color:#fff
    style Z fill:#4a90d9,color:#fff
    style P100 fill:#2ecc71,color:#fff
    style P200 fill:#e67e22,color:#fff
    style P300 fill:#9b59b6,color:#fff
    style P400 fill:#e74c3c,color:#fff
    style P500 fill:#1abc9c,color:#fff
    style P400b fill:#c0392b,color:#fff
    style P500a fill:#16a085,color:#fff
```

## Data Structures

### RTNCODE Copybook (`src/copybook/common/RTNCODE.cpy`)

| Field | PIC | Purpose |
|---|---|---|
| `RC-REQUEST-TYPE` | `X` | Operation selector (`I`/`S`/`G`/`L`/`A`) |
| `RC-PROGRAM-ID` | `X(8)` | Calling program identifier |
| `RC-CURRENT-CODE` | `S9(4) COMP` | Most recently set return code |
| `RC-HIGHEST-CODE` | `S9(4) COMP` | High-water mark across all set operations |
| `RC-NEW-CODE` | `S9(4) COMP` | Input code for Set operation |
| `RC-STATUS` | `X` | Derived status: `S`=Success, `W`=Warning, `E`=Error, `F`=Severe |
| `RC-MESSAGE` | `X(80)` | Free-text message for logging |
| `RC-RESPONSE-CODE` | `S9(8) COMP` | 0 = OK, 8 = DB2 failure |
| `RC-START-TIME` / `RC-END-TIME` | `X(26)` | Time range for Analyze query |
| `RC-TOTAL-CODES` | `S9(8) COMP` | Count result from Analyze |
| `RC-MAX-CODE` / `RC-MIN-CODE` | `S9(4) COMP` | Aggregate results from Analyze |
| `RC-RETURN-VALUE` | `S9(4) COMP` | Output: current code (Get) |
| `RC-HIGHEST-RETURN` | `S9(4) COMP` | Output: highest code (Get) |
| `RC-RETURN-STATUS` | `X` | Output: status flag (Get) |

### DB2 Table — `RTNCODES` (`src/database/db2/RTNCODES.sql`)

| Column | Type | Notes |
|---|---|---|
| `TIMESTAMP` | `TIMESTAMP` | PK (with PROGRAM_ID) |
| `PROGRAM_ID` | `CHAR(8)` | PK |
| `RETURN_CODE` | `INTEGER` | Current code at log time |
| `HIGHEST_CODE` | `INTEGER` | High-water mark at log time |
| `STATUS_CODE` | `CHAR(1)` | Derived status flag |
| `MESSAGE_TEXT` | `VARCHAR(80)` | Optional message |
