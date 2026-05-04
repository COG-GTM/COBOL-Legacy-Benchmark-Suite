# DB2ERR — DB2 SQL Error Handler

## Program Overview

**Program ID:** `DB2ERR`
**Location:** `src/programs/common/DB2ERR.cbl`

DB2ERR is a shared utility program that provides centralized DB2 SQL error handling for the Investment Portfolio Management System. It is invoked via `CALL` from other programs through a linkage section interface (`LS-ERROR-REQUEST`) and supports three functions:

| Function | Code   | Description |
|----------|--------|-------------|
| **LOG**  | `LOG ` | Logs a DB2 error to the `ERRLOG` table with severity classification and retry guidance. |
| **DIAG** | `DIAG` | Diagnoses a SQLCODE and returns a human-readable error description with a return code. |
| **RETR** | `RETR` | Retrieves the most recent error record for a given program from the `ERRLOG` table. |

### Key Characteristics

- **Copybooks used:** `DBTBLS` (DB2 table layouts), `SQLCA` (SQL Communication Area), `DBPROC` (DB2 standard procedures), `ERRHAND` (error handling definitions).
- **DB2 operations:** `INSERT INTO ERRLOG` (logging), `SELECT FROM ERRLOG` (retrieval).
- **CALL statements:** Calls external program `ERRPROC` for fatal error escalation.
- **SQLCODE categories handled:** Deadlock (`-911`), Timeout (`-913`), Connection Error (`-30081`), Duplicate Key (`-803`), Not Found (`+100`).
- **No file I/O:** All persistence is through DB2 SQL operations.

## Logic Flow Diagram

```mermaid
flowchart TD
    A([CALL 'DB2ERR' USING LS-ERROR-REQUEST]) --> B["0000-MAIN<br/>EVALUATE LS-FUNCTION"]

    B -->|"FUNC-LOG<br/>'LOG '"| C["1000-LOG-ERROR"]
    B -->|"FUNC-DIAG<br/>'DIAG'"| D["2000-DIAGNOSE-ERROR"]
    B -->|"FUNC-RETR<br/>'RETR'"| E["3000-RETRIEVE-ERROR"]
    B -->|OTHER| F["Set ERR-TEXT =<br/>'Invalid function code'"]
    F --> G["9000-ERROR-ROUTINE"]

    %% -------------------------------------------------------
    %% 1000-LOG-ERROR branch
    %% -------------------------------------------------------
    C --> C1["INITIALIZE WS-ERRLOG-REC"]
    C1 --> C2["ACCEPT WS-CURRENT-TIMESTAMP<br/>FROM TIME STAMP"]
    C2 --> C3["Move timestamp, program-id,<br/>error type 'D' to ERRLOG fields"]
    C3 --> C4["1100-SET-SEVERITY"]

    C4 --> S1{"EVALUATE<br/>LS-SQLCODE"}
    S1 -->|"-911 Deadlock<br/>-913 Timeout"| S2["Severity = 2<br/>RETRY = 'Y'"]
    S1 -->|"-30081<br/>Connection Error"| S3["Severity = 4<br/>RETRY = 'N'"]
    S1 -->|"-803<br/>Duplicate Key"| S4["Severity = 1<br/>RETRY = 'N'"]
    S1 -->|"+100<br/>Not Found"| S5["Severity = 1<br/>RETRY = 'N'"]
    S1 -->|OTHER| S6{"LS-SQLCODE < 0?"}
    S6 -->|Yes| S7["Severity = 3<br/>RETRY = 'N'"]
    S6 -->|No| S8["Severity = 1<br/>RETRY = 'N'"]

    S2 --> C5
    S3 --> C5
    S4 --> C5
    S5 --> C5
    S7 --> C5
    S8 --> C5

    C5["STRING SQLCODE + SQLSTATE<br/>into EL-ERROR-CODE"]
    C5 --> C6["Move error text, dates,<br/>user-id, additional info"]
    C6 --> C7["1200-INSERT-ERROR"]

    C7 --> I1["EXEC SQL INSERT INTO ERRLOG<br/>VALUES :WS-ERRLOG-REC"]
    I1 --> I2{"SQLCODE = 0?"}
    I2 -->|Yes| I3["LS-RETURN-CODE = 0"]
    I2 -->|No| I4["Set ERR-TEXT =<br/>'Error logging to ERRLOG'"]
    I4 --> G

    I3 --> Z([GOBACK])

    %% -------------------------------------------------------
    %% 2000-DIAGNOSE-ERROR branch
    %% -------------------------------------------------------
    D --> D1{"EVALUATE<br/>LS-SQLCODE"}
    D1 -->|"-911 Deadlock"| D2["LS-ERROR-TEXT =<br/>'Deadlock detected -<br/>retry transaction'<br/>RC = 4"]
    D1 -->|"-913 Timeout"| D3["LS-ERROR-TEXT =<br/>'Timeout occurred -<br/>retry transaction'<br/>RC = 4"]
    D1 -->|"-30081<br/>Connection Error"| D4["LS-ERROR-TEXT =<br/>'DB2 connection error -<br/>check availability'<br/>RC = 12"]
    D1 -->|"-803<br/>Duplicate Key"| D5["LS-ERROR-TEXT =<br/>'Duplicate key violation'<br/>RC = 8"]
    D1 -->|OTHER| D6{"LS-SQLCODE < 0?"}
    D6 -->|Yes| D7["LS-ERROR-TEXT =<br/>'Unhandled DB2 error'<br/>RC = 12"]
    D6 -->|No| D8["LS-ERROR-TEXT =<br/>'DB2 warning condition'<br/>RC = 4"]

    D2 --> Z
    D3 --> Z
    D4 --> Z
    D5 --> Z
    D7 --> Z
    D8 --> Z

    %% -------------------------------------------------------
    %% 3000-RETRIEVE-ERROR branch
    %% -------------------------------------------------------
    E --> E1["EXEC SQL SELECT<br/>ERROR_MESSAGE, ERROR_SEVERITY,<br/>ADDITIONAL_INFO<br/>FROM ERRLOG<br/>WHERE PROGRAM_ID = :LS-PROGRAM-ID<br/>AND ERROR_TIMESTAMP = MAX"]
    E1 --> E2{"SQLCODE = 0?"}
    E2 -->|Yes| E3["Move ERROR_MESSAGE<br/>to LS-ERROR-TEXT<br/>Move ERROR_SEVERITY<br/>to LS-RETURN-CODE"]
    E2 -->|No| E4["LS-ERROR-TEXT =<br/>'No error history found'<br/>RC = 4"]

    E3 --> Z
    E4 --> Z

    %% -------------------------------------------------------
    %% 9000-ERROR-ROUTINE (shared error escalation)
    %% -------------------------------------------------------
    G --> G1["ERR-PROGRAM = 'DB2ERR'<br/>LS-RETURN-CODE = 12"]
    G1 --> G2["CALL 'ERRPROC'<br/>USING ERR-MESSAGE"]
    G2 --> Z

    %% -------------------------------------------------------
    %% Styles
    %% -------------------------------------------------------
    classDef decision fill:#fff3cd,stroke:#856404,color:#856404
    classDef db2op fill:#d1ecf1,stroke:#0c5460,color:#0c5460
    classDef error fill:#f8d7da,stroke:#721c24,color:#721c24
    classDef endpoint fill:#d4edda,stroke:#155724,color:#155724

    class S1,S6,D1,D6,I2,E2 decision
    class I1,E1 db2op
    class G,G1,G2,F,I4 error
    class A,Z endpoint
```
