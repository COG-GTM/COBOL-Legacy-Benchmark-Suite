# RPTAUD00 — Audit Report Generator

## Program Description

RPTAUD00 is a batch COBOL program that generates a comprehensive system audit report. It reads from two indexed input files — an audit trail log (`AUDITLOG`) and an error log (`ERRLOG`) — and produces a formatted sequential report (`RPTFILE`). The report includes:

- **Security audit trails** — login/logout events, user actions, system events
- **Process audit reporting** — transaction create/update/delete/inquire records
- **Error summary reporting** — aggregated error records by program, category, and severity
- **Control verification** — summary totals and control counts

The program uses three copybooks: `AUDITLOG` (audit record layout with level-88 type/action/status flags), `ERRHAND` (error categories, return codes, VSAM status handling), and `RTNCODE` (return code management). There are no CALL statements or DB2 operations; all data is sourced from VSAM indexed files.

### Files

| Logical Name | DD Name    | Organization | Access     | Direction |
|-------------|------------|--------------|------------|-----------|
| AUDIT-FILE  | `AUDITLOG` | Indexed      | Sequential | Input     |
| ERROR-FILE  | `ERRLOG`   | Indexed      | Sequential | Input     |
| REPORT-FILE | `RPTFILE`  | Sequential   | Sequential | Output    |

## Logic Flow Diagram

```mermaid
flowchart TD
    START([Program Entry — RPTAUD00]) --> MAIN["0000-MAIN"]

    MAIN --> INIT["1000-INITIALIZE"]
    INIT --> OPEN["1100-OPEN-FILES"]

    OPEN --> OPEN_AUD["OPEN INPUT AUDIT-FILE"]
    OPEN_AUD --> CHK_AUD{WS-AUDIT-STATUS<br/>= '00'?}
    CHK_AUD -- Yes --> OPEN_ERR["OPEN INPUT ERROR-FILE"]
    CHK_AUD -- No --> ERR_AUD["Move 'ERROR OPENING AUDIT FILE'<br/>to WS-ERROR-MESSAGE"]
    ERR_AUD --> ERR_HANDLER

    OPEN_ERR --> CHK_ERR{WS-ERROR-STATUS<br/>= '00'?}
    CHK_ERR -- Yes --> OPEN_RPT["OPEN OUTPUT REPORT-FILE"]
    CHK_ERR -- No --> ERR_ERR["Move 'ERROR OPENING ERROR FILE'<br/>to WS-ERROR-MESSAGE"]
    ERR_ERR --> ERR_HANDLER

    OPEN_RPT --> CHK_RPT{WS-REPORT-STATUS<br/>= '00'?}
    CHK_RPT -- Yes --> HEADERS["1200-WRITE-HEADERS"]
    CHK_RPT -- No --> ERR_RPT["Move 'ERROR OPENING REPORT FILE'<br/>to WS-ERROR-MESSAGE"]
    ERR_RPT --> ERR_HANDLER

    HEADERS --> GET_DATE["ACCEPT WS-REPORT-DATE<br/>FROM DATE"]
    GET_DATE --> WRITE_H1["WRITE REPORT-RECORD<br/>FROM WS-HEADER1<br/>(asterisk border)"]
    WRITE_H1 --> WRITE_H2["WRITE REPORT-RECORD<br/>FROM WS-HEADER2<br/>(SYSTEM AUDIT REPORT title)"]
    WRITE_H2 --> WRITE_H3["WRITE REPORT-RECORD<br/>FROM WS-HEADER3<br/>(report date)"]

    WRITE_H3 --> PROCESS["2000-PROCESS-REPORT"]

    PROCESS --> AUDIT_TRAIL["2100-PROCESS-AUDIT-TRAIL"]
    AUDIT_TRAIL --> READ_AUD["2110-READ-AUDIT-RECORDS<br/>(read all AUDIT-FILE records)"]
    READ_AUD --> SUM_AUD["2120-SUMMARIZE-AUDIT<br/>(aggregate audit statistics)"]

    SUM_AUD --> ERROR_LOG["2200-PROCESS-ERROR-LOG"]
    ERROR_LOG --> READ_ERR["2210-READ-ERROR-RECORDS<br/>(read all ERROR-FILE records)"]
    READ_ERR --> SUM_ERR["2220-SUMMARIZE-ERRORS<br/>(aggregate error statistics)"]

    SUM_ERR --> SUMMARY["2300-WRITE-SUMMARY"]
    SUMMARY --> W_AUD_SUM["2310-WRITE-AUDIT-SUMMARY<br/>(write audit totals to report)"]
    W_AUD_SUM --> W_ERR_SUM["2320-WRITE-ERROR-SUMMARY<br/>(write error totals to report)"]
    W_ERR_SUM --> W_CTL_SUM["2330-WRITE-CONTROL-SUMMARY<br/>(write control verification)"]

    W_CTL_SUM --> CLEANUP["3000-CLEANUP"]
    CLEANUP --> CLOSE["CLOSE AUDIT-FILE,<br/>ERROR-FILE,<br/>REPORT-FILE"]
    CLOSE --> GOBACK_OK([GOBACK — Normal Exit])

    ERR_HANDLER["9999-ERROR-HANDLER"]
    ERR_HANDLER --> DISPLAY["DISPLAY WS-ERROR-MESSAGE"]
    DISPLAY --> SET_RC["MOVE 12 TO RETURN-CODE"]
    SET_RC --> GOBACK_ERR([GOBACK — Abnormal Exit<br/>RC=12])

    style START fill:#2d6a4f,color:#fff
    style GOBACK_OK fill:#2d6a4f,color:#fff
    style GOBACK_ERR fill:#ae2012,color:#fff
    style ERR_HANDLER fill:#ae2012,color:#fff
    style CHK_AUD fill:#e9c46a,color:#000
    style CHK_ERR fill:#e9c46a,color:#000
    style CHK_RPT fill:#e9c46a,color:#000
```
