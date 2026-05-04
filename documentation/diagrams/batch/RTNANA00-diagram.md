# RTNANA00 - Return Code Analysis Utility

## Program Description

**RTNANA00** is a batch COBOL program that analyzes return codes across the system and produces a formatted analysis report. It queries the `RTNCODES` DB2 table to aggregate return code statistics by program, categorizing each entry as Success (`S`), Warning (`W`), Error (`E`), or Severe/Fatal (`F`). The program generates a sequential report file containing per-program breakdowns and summary totals.

### Key Characteristics

| Attribute         | Details                                                                 |
|-------------------|-------------------------------------------------------------------------|
| **Program ID**    | RTNANA00                                                                |
| **Type**          | Batch                                                                   |
| **File I/O**      | Sequential output to `REPORT-FILE` (DD name `RPTFILE`, fixed 133-byte records) |
| **DB2 Table**     | `RTNCODES` (columns: `PROGRAM_ID`, `STATUS_CODE`)                      |
| **DB2 Cursor**    | `PRGCUR` - aggregates counts by `PROGRAM_ID` with `GROUP BY` / `ORDER BY` |
| **CALL Statements** | None                                                                  |
| **SQLCA**         | Included for DB2 return code handling                                   |

### Sections and Paragraphs

| Paragraph               | Purpose                                                        |
|--------------------------|----------------------------------------------------------------|
| `P100-INIT-PROGRAM`      | Captures current date/time, opens report file, initializes counters |
| `P200-PROCESS-ANALYSIS`  | Declares and opens DB2 cursor, drives header and detail writing, closes cursor |
| `P210-WRITE-HEADERS`     | Writes report header lines (title, date/time, column headers)  |
| `P220-PROCESS-DETAIL`    | Fetches each row from cursor, writes detail line, accumulates totals |
| `P300-GENERATE-REPORT`   | Writes summary totals line at end of report                    |
| `P900-CLOSE-FILES`       | Closes the report file                                         |

## Logic Flow Diagram

```mermaid
flowchart TD
    Start([PROCEDURE DIVISION<br/>Start]) --> P100[P100-INIT-PROGRAM]

    subgraph P100_SUB [P100-INIT-PROGRAM]
        P100 --> P100_DATE[MOVE FUNCTION CURRENT-DATE<br/>to WS-CURRENT-DATE-DATA]
        P100_DATE --> P100_OPEN[OPEN OUTPUT REPORT-FILE]
        P100_OPEN --> P100_CHK{WS-REPORT-STATUS<br/>= '00'?}
        P100_CHK -- No --> P100_ERR[DISPLAY error message<br/>MOVE 12 TO RETURN-CODE<br/>GOBACK]
        P100_CHK -- Yes --> P100_INIT[INITIALIZE WS-ANALYSIS-AREA]
        P100_INIT --> P100_EXIT([P100-EXIT])
    end

    P100_EXIT --> P200[P200-PROCESS-ANALYSIS]

    subgraph P200_SUB [P200-PROCESS-ANALYSIS]
        P200 --> P200_DECL["DECLARE CURSOR PRGCUR<br/>SELECT PROGRAM_ID,<br/>COUNT(*), COUNT(S),<br/>COUNT(W), COUNT(E), COUNT(F)<br/>FROM RTNCODES<br/>GROUP BY PROGRAM_ID<br/>ORDER BY PROGRAM_ID"]
        P200_DECL --> P200_OPEN[EXEC SQL OPEN PRGCUR]
        P200_OPEN --> P210[PERFORM P210-WRITE-HEADERS]
        P210 --> P220_LOOP[PERFORM P220-PROCESS-DETAIL<br/>UNTIL SQLCODE = 100]
        P220_LOOP --> P200_CLOSE[EXEC SQL CLOSE PRGCUR]
        P200_CLOSE --> P200_EXIT([P200-EXIT])
    end

    subgraph P210_SUB [P210-WRITE-HEADERS]
        P210_START([P210-WRITE-HEADERS]) --> P210_H1[WRITE header separator line]
        P210_H1 --> P210_H2[WRITE report title line]
        P210_H2 --> P210_DT[Format date and time<br/>into WS-RPT-DATE / WS-RPT-TIME]
        P210_DT --> P210_H3[WRITE date/time line]
        P210_H3 --> P210_H4[WRITE header separator]
        P210_H4 --> P210_H5[WRITE column headers:<br/>Program, Total, Success,<br/>Warning, Error, Severe]
        P210_H5 --> P210_H6[WRITE header separator]
        P210_H6 --> P210_EXIT([P210-EXIT])
    end

    subgraph P220_SUB [P220-PROCESS-DETAIL]
        P220_START([P220-PROCESS-DETAIL]) --> P220_FETCH["EXEC SQL FETCH PRGCUR<br/>INTO :WS-DTL-PROGRAM,<br/>:WS-DTL-TOTAL,<br/>:WS-DTL-SUCCESS,<br/>:WS-DTL-WARNING,<br/>:WS-DTL-ERROR,<br/>:WS-DTL-SEVERE"]
        P220_FETCH --> P220_CHK{SQLCODE = 0?}
        P220_CHK -- Yes --> P220_WRITE[WRITE detail line<br/>to REPORT-FILE]
        P220_WRITE --> P220_ACC[Accumulate totals:<br/>ADD to WS-PROGRAM-COUNT,<br/>WS-SUCCESS-COUNT,<br/>WS-WARNING-COUNT,<br/>WS-ERROR-COUNT,<br/>WS-SEVERE-COUNT]
        P220_ACC --> P220_EXIT([P220-EXIT])
        P220_CHK -- "No (SQLCODE = 100<br/>= end of data)" --> P220_EXIT
    end

    P200_EXIT --> P300[P300-GENERATE-REPORT]

    subgraph P300_SUB [P300-GENERATE-REPORT]
        P300 --> P300_SEP1[WRITE header separator]
        P300_SEP1 --> P300_TOT["MOVE 'TOTALS' to WS-DTL-PROGRAM<br/>MOVE accumulated counts<br/>to detail line fields"]
        P300_TOT --> P300_WR[WRITE totals detail line]
        P300_WR --> P300_SEP2[WRITE header separator]
        P300_SEP2 --> P300_EXIT([P300-EXIT])
    end

    P300_EXIT --> P900[P900-CLOSE-FILES]

    subgraph P900_SUB [P900-CLOSE-FILES]
        P900 --> P900_CL[CLOSE REPORT-FILE]
        P900_CL --> P900_EXIT([P900-EXIT])
    end

    P900_EXIT --> GOBACK([GOBACK])

    P100_ERR --> ABORT([Program Aborts<br/>Return Code 12])
```
