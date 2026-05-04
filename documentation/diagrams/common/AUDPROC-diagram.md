# AUDPROC - Audit Trail Processing Subroutine

## Program Description

**AUDPROC** is a COBOL subroutine that provides centralized audit trail logging for the Investment Portfolio Management System. It is invoked via `CALL` from other programs (batch, online, and utility) through the `LINKAGE SECTION`, receiving an audit request record that contains system information, audit type, action, status, key identifiers, before/after data images, and a free-text message.

The program writes a single audit record to a sequential audit trail file (`AUDFILE`) in append mode. It performs file status validation after both the `OPEN` and `WRITE` operations, returning a status code (`LS-RETURN-CODE`) to the caller: `0` for success or `8` for any file I/O error.

### Key Characteristics

| Attribute | Detail |
|---|---|
| **Program Type** | Called subroutine (`CALL` / `GOBACK`) |
| **File I/O** | Sequential file — `OPEN EXTEND`, `WRITE`, `CLOSE` |
| **DB2 Operations** | None |
| **CICS Usage** | None |
| **CALL Statements** | None (leaf program) |
| **Copybook** | `AUDITLOG` — defines the `AUDIT-RECORD` layout |
| **Linkage** | `LS-AUDIT-REQUEST` — caller-provided audit data |
| **Return Codes** | `0` = Success, `8` = File I/O error |

### Audit Record Fields (via AUDITLOG copybook)

| Field | Size | Description | Level-88 Values |
|---|---|---|---|
| `AUD-TIMESTAMP` | X(26) | Timestamp from system clock | — |
| `AUD-SYSTEM-ID` | X(8) | Originating system identifier | — |
| `AUD-USER-ID` | X(8) | User performing the action | — |
| `AUD-PROGRAM` | X(8) | Calling program name | — |
| `AUD-TERMINAL` | X(8) | Terminal identifier | — |
| `AUD-TYPE` | X(4) | Audit event type | `TRAN`, `USER`, `SYST` |
| `AUD-ACTION` | X(8) | Action performed | `CREATE`, `UPDATE`, `DELETE`, `INQUIRE`, `LOGIN`, `LOGOUT`, `STARTUP`, `SHUTDOWN` |
| `AUD-STATUS` | X(4) | Outcome status | `SUCC`, `FAIL`, `WARN` |
| `AUD-PORTFOLIO-ID` | X(8) | Portfolio identifier | — |
| `AUD-ACCOUNT-NO` | X(10) | Account number | — |
| `AUD-BEFORE-IMAGE` | X(100) | Data state before change | — |
| `AUD-AFTER-IMAGE` | X(100) | Data state after change | — |
| `AUD-MESSAGE` | X(100) | Free-text audit message | — |

---

## Logic Flow Diagram

```mermaid
flowchart TD
    A([Caller invokes AUDPROC<br/>via CALL USING LS-AUDIT-REQUEST]) --> B["<b>0000-MAIN</b><br/>Entry Point"]

    B --> C["PERFORM <b>1000-INITIALIZE</b>"]

    subgraph INIT["1000-INITIALIZE"]
        direction TB
        C1["ACCEPT WS-FORMATTED-TIME<br/>FROM TIME STAMP"]
        C2["OPEN EXTEND AUDIT-FILE"]
        C3{"WS-FILE-STATUS<br/>= '00'?"}
        C4["DISPLAY error message"]
        C5["MOVE 8 TO LS-RETURN-CODE"]
        C6["PERFORM <b>3000-TERMINATE</b><br/>(close file)"]
        C7(["GOBACK to caller<br/>(return code = 8)"])

        C1 --> C2 --> C3
        C3 -- No --> C4 --> C5 --> C6 --> C7
    end

    C --> C1
    C3 -- Yes --> D

    D["PERFORM <b>2000-PROCESS-AUDIT</b>"]

    subgraph PROC["2000-PROCESS-AUDIT"]
        direction TB
        D1["INITIALIZE AUDIT-RECORD"]
        D2["MOVE fields from Linkage Section<br/>to AUDIT-RECORD:<br/>WS-FORMATTED-TIME → AUD-TIMESTAMP<br/>LS-SYSTEM-INFO → AUD-HEADER<br/>LS-TYPE → AUD-TYPE<br/>LS-ACTION → AUD-ACTION<br/>LS-STATUS → AUD-STATUS<br/>LS-KEY-INFO → AUD-KEY-INFO<br/>LS-BEFORE-IMAGE → AUD-BEFORE-IMAGE<br/>LS-AFTER-IMAGE → AUD-AFTER-IMAGE<br/>LS-MESSAGE → AUD-MESSAGE"]
        D3["WRITE AUDIT-RECORD"]
        D4{"WS-FILE-STATUS<br/>= '00'?"}
        D5["MOVE 0 TO LS-RETURN-CODE"]
        D6["DISPLAY error message"]
        D7["MOVE 8 TO LS-RETURN-CODE"]

        D1 --> D2 --> D3 --> D4
        D4 -- Yes --> D5
        D4 -- No --> D6 --> D7
    end

    D --> D1
    D5 --> E
    D7 --> E

    E["PERFORM <b>3000-TERMINATE</b>"]

    subgraph TERM["3000-TERMINATE"]
        direction TB
        E1["CLOSE AUDIT-FILE"]
    end

    E --> E1
    E1 --> F(["GOBACK to caller<br/>(return code = 0 or 8)"])

    style A fill:#e1f5fe,stroke:#0288d1,color:#000
    style F fill:#e1f5fe,stroke:#0288d1,color:#000
    style C7 fill:#ffebee,stroke:#c62828,color:#000
    style B fill:#fff3e0,stroke:#ef6c00,color:#000
    style C3 fill:#fff9c4,stroke:#f9a825,color:#000
    style D4 fill:#fff9c4,stroke:#f9a825,color:#000
```
