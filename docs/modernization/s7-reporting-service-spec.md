# S7 Reporting Service Specification

## 1. Purpose & scope

S7 is the read-only reporting component for the investment-portfolio system. The
three supplied batch programs are intended to produce:

- a daily position report from the position master and transaction history;
- a system audit report from the audit and error logs; and
- a system-statistics/performance report from DB2 and batch statistics.

The implementation evidence is limited. `RPTPOS00` fully implements file
opening, report headers, position iteration, position formatting, cleanup, and
fatal-open-error handling, but its transaction summarization, totals,
exceptions, and metrics paragraphs are called but absent. `RPTAUD00` declares
the report structures and file lifecycle but its audit/error readers,
summarizers, and summary writers are absent. `RPTSTA00` declares input and
output structures and the file lifecycle, but its accumulators, metric
calculations, and report writers are absent. The Java target is therefore a
Spring Batch reporting service that preserves implemented behavior and makes
unimplemented behavior explicit rather than inventing it.

The decomposition plan places S7 behind read-only replicas and describes the
target as “Spring Batch reports (CSV/PDF), or SQL views + Grafana.” This
specification chooses Spring Batch for the batch-compatible report generation
path; the output format and any Grafana projection remain open decisions.

## 2. Business rules

Rules below are extracted from the supplied programs paragraph-by-paragraph.
“Not implemented” means the paragraph is called or described but has no
executable statements in the supplied source. “INFERRED FROM DOCS” marks
documentation-only behavior.

1. `RPTPOS00` identifies itself as the daily position report generator and
   describes position summary, transaction activity, exception reporting, and
   performance metrics as intended report content. [RPTPOS00.cbl:1-13]
2. `RPTPOS00` reads `POSITION-MASTER` sequentially using `POS-KEY` as the
   indexed-file record key. [RPTPOS00.cbl:17-21]
3. `RPTPOS00` reads `TRANSACTION-HISTORY` sequentially using `TRAN-KEY` as the
   indexed-file record key. [RPTPOS00.cbl:23-27]
4. `RPTPOS00` writes fixed-format sequential records of 132 characters to
   `REPORT-FILE`. [RPTPOS00.cbl:29-31,38-41]
5. `RPTPOS00` opens the position master as input, then the transaction history
   as input, then the report file as output. Each non-`00` file status displays
   a specific error message, sets the return code to 12 through the common
   error paragraph, and immediately executes `GOBACK`; subsequent open steps
   are therefore not reached after a failure. [RPTPOS00.cbl:88-108,157-160]
6. A position report begins with an all-asterisk 132-character line, the
   centered literal `DAILY POSITION REPORT`, and a `REPORT DATE:` line.
   [RPTPOS00.cbl:52-63,110-114]
7. The report date is accepted from the COBOL `DATE` intrinsic source into a
   ten-character field; the source does not document whether the returned
   representation is Gregorian `YYYYMMDD` or another runtime representation.
   [RPTPOS00.cbl:60-63,110-114]
8. `RPTPOS00` performs initialization, report processing, cleanup, and then
   returns with `GOBACK`, in that order. [RPTPOS00.cbl:77-82]
9. Position processing reads one record before entering the loop, marks
   `END-OF-POSITIONS` at end of file, formats each non-EOF record, and repeats
   the read until EOF. [RPTPOS00.cbl:121-131]
10. Position detail copies portfolio ID, description, quantity, and current
    value into the output layout. [RPTPOS00.cbl:65-75,133-137]
11. The position change percentage is calculated exactly as
    `(POS-CURRENT-VALUE - POS-PREVIOUS-VALUE) / POS-PREVIOUS-VALUE * 100`.
    The receiving report field displays two decimal places. No `ROUNDED`
    phrase or explicit rounding mode is present; the source therefore does not
    define a portable rounding/truncation policy for conversion to the edited
    output field, and Java must not assume one without a parity decision.
    Division by zero is not guarded in the supplied paragraph. [RPTPOS00.cbl:138-141]
12. `RPTPOS00` invokes transaction reading and activity summarization after
    position reading. Both called paragraphs are absent from the supplied
    program, so no transaction selection, aggregation, ordering, or output
    behavior is implemented. [RPTPOS00.cbl:143-145]
13. `RPTPOS00` invokes totals, exception, and metrics writers after processing.
    Paragraphs `2310-WRITE-TOTALS`, `2320-WRITE-EXCEPTIONS`, and
    `2330-WRITE-METRICS` are absent, so those report sections have no
    executable behavior. [RPTPOS00.cbl:147-150]
14. On normal cleanup, `RPTPOS00` closes the position master, transaction
    history, and report file. [RPTPOS00.cbl:152-155]
15. `RPTAUD00` identifies itself as an audit report generator for security audit
    trails, process audits, error summaries, and control verification.
    [RPTAUD00.cbl:1-13]
16. `RPTAUD00` reads indexed `AUDIT-FILE` and `ERROR-FILE` sequentially, and
    writes fixed-format 132-character records to `REPORT-FILE`.
    [RPTAUD00.cbl:17-31,38-41]
17. `RPTAUD00` opens the audit file, error file, and report file in that order.
    Each non-`00` status displays a specific error message, sets return code 12,
    and executes `GOBACK` through `9999-ERROR-HANDLER`. [RPTAUD00.cbl:93-113,144-147]
18. An audit report begins with an all-asterisk line, the centered literal
    `SYSTEM AUDIT REPORT`, and a `REPORT DATE:` line. [RPTAUD00.cbl:51-62,115-119]
19. `RPTAUD00` accepts the report date from COBOL `DATE`; its output
    representation is not specified in the source. [RPTAUD00.cbl:59-62,115-119]
20. `RPTAUD00` invokes audit-trail reading and summarization, error-log reading
    and summarization, and three summary writers. The six called paragraphs
    (`2110-READ-AUDIT-RECORDS`, `2120-SUMMARIZE-AUDIT`,
    `2210-READ-ERROR-RECORDS`, `2220-SUMMARIZE-ERRORS`,
    `2310-WRITE-AUDIT-SUMMARY`, `2320-WRITE-ERROR-SUMMARY`, and
    `2330-WRITE-CONTROL-SUMMARY`) have no supplied implementations; no
    filtering, aggregation, control checks, or output semantics can be claimed.
    [RPTAUD00.cbl:121-137]
21. On normal cleanup, `RPTAUD00` closes the audit file, error file, and report
    file. [RPTAUD00.cbl:139-142]
22. `RPTSTA00` identifies itself as a system-statistics report generator for
    processing statistics, performance metrics, resource utilization, and trend
    analysis. [RPTSTA00.cbl:1-13]
23. `RPTSTA00` reads indexed `DB2-STATS` and `BATCH-STATS` sequentially, and
    writes fixed-format 132-character records to `REPORT-FILE`.
    [RPTSTA00.cbl:17-31,38-41]
24. `RPTSTA00` opens DB2 statistics, batch statistics, and the report file in
    that order. Each non-`00` status displays a specific error message, sets
    return code 12, and executes `GOBACK` through `9999-ERROR-HANDLER`.
    [RPTSTA00.cbl:107-127,182-185]
25. A statistics report begins with an all-asterisk line, the centered literal
    `SYSTEM STATISTICS AND PERFORMANCE REPORT`, and a `REPORT DATE:` line.
    [RPTSTA00.cbl:52-63,129-133]
26. `RPTSTA00` initializes all declared performance metric accumulators after
    opening files and writing headers. [RPTSTA00.cbl:102-105,135-136]
27. DB2 statistics are read until EOF, with one accumulation paragraph invoked
    for each record; batch statistics are handled the same way. The called
    accumulation paragraphs are absent, so no counters are incremented and no
    record-level aggregation is implemented. [RPTSTA00.cbl:144-166]
28. `RPTSTA00` invokes DB2 and batch metric calculation paragraphs, then DB2,
    batch, and trend report writers. All five called paragraphs are absent, so
    no average-response, success-rate, utilization, or trend formula is
    implemented. [RPTSTA00.cbl:168-175]
29. The declared DB2 detail layout labels DB2 calls and average response, and
    the batch detail layout labels batch jobs and success rate; these are output
    field declarations only, not implemented calculations. [RPTSTA00.cbl:77-93]
30. On normal cleanup, `RPTSTA00` closes DB2 statistics, batch statistics, and
    report files. [RPTSTA00.cbl:177-180]
31. All three programs display an error message and set `RETURN-CODE` to 12 for
    the file-open failures they explicitly check. No other return-code path is
    implemented in these sources. [RPTPOS00.cbl:157-160; RPTAUD00.cbl:144-147;
    RPTSTA00.cbl:182-185]
32. The JCL allocates each report output as a new cataloged fixed-block
    dataset, with `LRECL=132`; an allocation failure is deleted rather than
    cataloged. [RPTPOS.jcl:10-13; RPTAUD.jcl:10-13; RPTSTA.jcl:10-13]
33. The JCL invokes exactly one program step per job, using the production load
    library and the corresponding input DD names. [RPTPOS.jcl:6-9;
    RPTAUD.jcl:6-9; RPTSTA.jcl:6-9]
34. **INFERRED FROM DOCS:** The architecture document describes RPTPOS00 as
    generating daily positions, portfolio valuations, transaction summaries,
    and reconciliation reports; RPTAUD00 as producing security-audit and
    exception reports; and RPTSTA00 as monitoring performance, resource
    utilization, trends, and operational metrics. These descriptions exceed
    the executable paragraphs supplied here. [system-architecture.md:172-191]
35. **INFERRED FROM DOCS:** The architecture dependency matrix says the
    reporting programs depend on `DB2CONN` and `ERRPROC`, and have read-only
    DB2 access. The supplied programs contain no `CALL`, `EXEC SQL`, or explicit
    DB2 operation; their actual inputs are the VSAM-style files allocated by
    JCL. [system-architecture.md:499-506]
36. **INFERRED FROM DOCS:** The data dictionary schedules report generation
    under process ID `RPTGEN00` after `HISTLD00`, with no restart requirement.
    The supplied JCL instead names three independent jobs/program steps and
    provides no conditional dependency statements. [data-dictionary.md:270-305]
37. **INFERRED FROM DOCS:** The decomposition plan says S7 consumes read-only
    replicas and may produce CSV/PDF or SQL views/Grafana. The supplied JCL
    proves only fixed-width 132-byte report datasets; it does not prove CSV,
    PDF, SQL views, or Grafana behavior. [cobol-java-modernization-plan.md:18-31]

### Calculation and numeric fidelity

Only one calculation is present: the position change percentage in rule 11.
The source has no `ROUNDED`, `ON SIZE ERROR`, or explicit `TRUNCATION` phrase.
The edited receiving field has two decimal display positions, while the
underlying source field definitions come from `POSREC` and are not consistent
with the documentation's position layout. Java should retain full intermediate
precision, use an explicitly selected `RoundingMode` only after a parity
decision, and reject or separately define a zero-denominator case; the COBOL
source itself does not define those policies.

No status transition is performed by any supplied report paragraph. The
`POSREC`, `TRNREC`, `AUDITLOG`, and `BCHCTL` status values below are data
contract values, not transitions implemented by S7.

## 3. Data contracts

### 3.1 Report records and working storage

| Record/field | PIC | Semantics | Proposed Java type | DB2/VSAM mapping |
|---|---|---|---|---|
| `REPORT-RECORD` | `X(132)` | Fixed-width output line | `String` length 132 | `RPTFILE` output dataset; Java report line |
| `WS-REPORT-DATE` | `X(10)` | Date inserted in report header | `String` pending runtime-format confirmation, or `LocalDate` after confirmation | Derived; no source column |
| `WS-POS-PORTFOLIO` | `X(10)` | Position portfolio identifier | `String` | Derived from `POS-PORTFOLIO-ID`; `POSMSTRE` |
| `WS-POS-DESCRIPTION` | `X(30)` | Position description | `String` | No corresponding field in supplied `POSREC`; source/copybook discrepancy |
| `WS-POS-QUANTITY` | `ZZZ,ZZZ,ZZ9.99` | Formatted position quantity | `BigDecimal` with source scale 4, rendered to report scale 2 | Derived from `POS-QUANTITY`; `POSMSTRE` |
| `WS-POS-VALUE` | `$$$$,$$$,$$9.99` | Formatted current value | `BigDecimal` scale 2 | Derived from `POS-MARKET-VALUE`; `POSMSTRE` |
| `WS-POS-CHANGE-PCT` | `+ZZ9.99` | Formatted change percentage | `BigDecimal` scale 2 after parity decision | Derived from current/previous values; no direct column |
| `WS-AUD-TIMESTAMP` | `X(26)` | Audit event timestamp in output | `String` or `Instant` with explicit conversion | `AUDITLOG.AUD-TIMESTAMP`; `AUDITLOG` |
| `WS-AUD-PROGRAM` | `X(8)` | Program that emitted audit event | `String` | `AUDITLOG.AUD-PROGRAM`; `AUDITLOG` |
| `WS-AUD-TYPE` | `X(10)` | Audit type output field | `String` | Source copybook has `AUD-TYPE X(4)`; discrepancy |
| `WS-AUD-MESSAGE` | `X(80)` | Audit message | `String` | Derived from `AUD-MESSAGE X(100)`; `AUDITLOG` |
| `WS-ERR-TIMESTAMP` | `X(26)` | Error timestamp in output | `String` or `Instant` | `ERRLOG.EL-ERROR-TIMESTAMP`; `ERRLOG` |
| `WS-ERR-PROGRAM` | `X(8)` | Program associated with error | `String` | `ERRLOG.EL-PROGRAM-ID`; `ERRLOG` |
| `WS-ERR-CODE` | `X(4)` | Error code in output | `String` | `ERRLOG.EL-ERROR-CODE X(8)`; discrepancy |
| `WS-ERR-MESSAGE` | `X(80)` | Error description in output | `String` | `ERRLOG.EL-ERROR-MESSAGE X(200)`; discrepancy |
| `WS-DB2-CALLS` / `WS-DB2-CALLS-OUT` | `9(9)` / `ZZZ,ZZZ,ZZ9` | DB2 call count and formatted output | `long` | `DB2STATS` input; no supplied copybook/table |
| `WS-DB2-ELAPSED` | `9(9)V99` | DB2 elapsed metric accumulator | `BigDecimal` scale 2 | `DB2STATS`; no supplied mapping |
| `WS-DB2-CPU` | `9(9)V99` | DB2 CPU metric accumulator | `BigDecimal` scale 2 | `DB2STATS`; no supplied mapping |
| `WS-DB2-WAIT` | `9(9)V99` | DB2 wait metric accumulator | `BigDecimal` scale 2 | `DB2STATS`; no supplied mapping |
| `WS-DB2-AVG-RESP` | `ZZ,ZZ9.999` | Formatted average response | `BigDecimal` scale 3 | Derived; formula absent |
| `WS-BATCH-JOBS` / `WS-BATCH-TOTAL` | `9(9)` / `ZZZ,ZZ9` | Batch job count and formatted output | `long` | `BCHSTATS`; no supplied mapping |
| `WS-BATCH-SUCCESS` | `9(9)` | Successful batch count | `long` | `BCHSTATS`; no supplied mapping |
| `WS-BATCH-FAILED` | `9(9)` | Failed batch count | `long` | `BCHSTATS`; no supplied mapping |
| `WS-BATCH-ELAPSED` | `9(9)V99` | Batch elapsed metric accumulator | `BigDecimal` scale 2 | `BCHSTATS`; no supplied mapping |
| `WS-SUCCESS-RATE` | `ZZ9.99` | Formatted success rate | `BigDecimal` scale 2 | Derived; formula absent |

### 3.2 Included copybooks

The following are the copybooks directly named by the supplied programs. Types
are based on the PIC clauses; `COMP`/`COMP-3` affects storage encoding, not the
proposed domain type.

#### `POSREC` — position input

| Field | PIC | Semantics | Java type | Mapping |
|---|---|---|---|---|
| `POS-PORTFOLIO-ID` | `X(08)` | Portfolio identifier | `String` | `POSMSTRE`; no documented DB2 column |
| `POS-DATE` | `X(08)` | Position date, comment says YYYYMMDD | `LocalDate` after format validation | `POSMSTRE`; no documented DB2 column |
| `POS-INVESTMENT-ID` | `X(10)` | Investment identifier | `String` | `POSMSTRE`; no documented DB2 column |
| `POS-QUANTITY` | `S9(11)V9(4)` | Holding quantity | `BigDecimal` scale 4 | `POSMSTRE`; no documented DB2 column |
| `POS-COST-BASIS` | `S9(13)V9(2)` | Total cost basis | `BigDecimal` scale 2 | `POSMSTRE`; `POSHIST.COST_BASIS` is a related documented target |
| `POS-MARKET-VALUE` | `S9(13)V9(2)` | Current market value | `BigDecimal` scale 2 | `POSMSTRE`; no documented DB2 column |
| `POS-CURRENCY` | `X(03)` | Currency code | `String` | `POSMSTRE`; no documented DB2 column |
| `POS-STATUS` | `X(01)`; 88 `A/C/P` | Active, closed, pending | `PositionStatus` enum | `POSMSTRE`; no documented DB2 column |
| `POS-LAST-MAINT-DATE` | `X(26)` | Last maintenance timestamp | `String` or `Instant` | `POSMSTRE`; no documented DB2 column |
| `POS-LAST-MAINT-USER` | `X(08)` | Last maintenance user | `String` | `POSMSTRE`; no documented DB2 column |
| `POS-FILLER` | `X(50)` | Reserved bytes | `String` or omitted from domain model | `POSMSTRE` record padding |

#### `TRNREC` — transaction-history input

| Field | PIC | Semantics | Java type | Mapping |
|---|---|---|---|---|
| `TRN-DATE` | `X(08)` | Transaction date, comment says YYYYMMDD | `LocalDate` after format validation | `TRANHIST`; no documented DB2 column |
| `TRN-TIME` | `X(06)` | Transaction time, comment says HHMMSS | `LocalTime` after format validation | `TRANHIST`; no documented DB2 column |
| `TRN-PORTFOLIO-ID` | `X(08)` | Portfolio identifier | `String` | `TRANHIST`; no documented DB2 column |
| `TRN-SEQUENCE-NO` | `X(06)` | Sequence within transactions | `String` | `TRANHIST`; no documented DB2 column |
| `TRN-INVESTMENT-ID` | `X(10)` | Investment identifier | `String` | `TRANHIST`; no documented DB2 column |
| `TRN-TYPE` | `X(02)`; 88 `BU/SL/TR/FE` | Buy, sell, transfer, fee | `TransactionType` enum | `TRANHIST`; no documented DB2 column |
| `TRN-QUANTITY` | `S9(11)V9(4)` | Transaction quantity | `BigDecimal` scale 4 | `TRANHIST`; no documented DB2 column |
| `TRN-PRICE` | `S9(11)V9(4)` | Transaction price | `BigDecimal` scale 4 | `TRANHIST`; no documented DB2 column |
| `TRN-AMOUNT` | `S9(13)V9(2)` | Transaction amount | `BigDecimal` scale 2 | `TRANHIST`; no documented DB2 column |
| `TRN-CURRENCY` | `X(03)` | Currency code | `String` | `TRANHIST`; no documented DB2 column |
| `TRN-STATUS` | `X(01)`; 88 `P/D/F/R` | Pending, done, failed, reversed | `TransactionStatus` enum | `TRANHIST`; no documented DB2 column |
| `TRN-PROCESS-DATE` | `X(26)` | Processing timestamp despite name | `String` or `Instant` | `TRANHIST`; no documented DB2 column |
| `TRN-PROCESS-USER` | `X(08)` | Processing user | `String` | `TRANHIST`; no documented DB2 column |
| `TRN-FILLER` | `X(50)` | Reserved bytes | `String` or omitted | `TRANHIST` record padding |

#### `AUDITLOG` — audit input

| Field | PIC | Semantics | Java type | Mapping |
|---|---|---|---|---|
| `AUD-TIMESTAMP` | `X(26)` | Audit timestamp | `String` or `Instant` | `AUDITLOG`; `ERRLOG` only documents errors, not audit events |
| `AUD-SYSTEM-ID` | `X(8)` | System identifier | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-USER-ID` | `X(8)` | User identifier | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-PROGRAM` | `X(8)` | Emitting program | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-TERMINAL` | `X(8)` | Terminal identifier | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-TYPE` | `X(4)`; 88 `TRAN/USER/SYST` | Audit category | `AuditType` enum | `AUDITLOG`; no documented DB2 column |
| `AUD-ACTION` | `X(8)`; 88 create/update/delete/inquire/login/logout/startup/shutdown | Audit action | `AuditAction` enum | `AUDITLOG`; no documented DB2 column |
| `AUD-STATUS` | `X(4)`; 88 `SUCC/FAIL/WARN` | Audit result | `AuditStatus` enum | `AUDITLOG`; no documented DB2 column |
| `AUD-PORTFOLIO-ID` | `X(8)` | Portfolio key context | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-ACCOUNT-NO` | `X(10)` | Account key context | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-BEFORE-IMAGE` | `X(100)` | Before-image text | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-AFTER-IMAGE` | `X(100)` | After-image text | `String` | `AUDITLOG`; no documented DB2 column |
| `AUD-MESSAGE` | `X(100)` | Audit message | `String` | `AUDITLOG`; no documented DB2 column |

#### `ERRHAND` — error and return-code support

| Field | PIC | Semantics | Java type | Mapping |
|---|---|---|---|---|
| `ERR-CAT-VSAM` / `ERR-CAT-VALID` / `ERR-CAT-PROC` / `ERR-CAT-SYSTEM` | `X(2)` values `VS/VL/PR/SY` | Error categories | `ErrorCategory` enum | Error/audit support; `ERRLOG` |
| `ERR-SUCCESS` / `ERR-WARNING` / `ERR-ERROR` / `ERR-SEVERE` / `ERR-TERMINAL` | `S9(4) COMP` values 0/4/8/12/16 | Standard return codes | `ReturnCode` enum | Job return code; `ERRLOG` for logged failures |
| `ERR-DATE` | `X(10)` | Error date | `String` or `LocalDate` after format confirmation | `ERRLOG` timestamp |
| `ERR-TIME` | `X(8)` | Error time | `String` or `LocalTime` after format confirmation | `ERRLOG` timestamp |
| `ERR-PROGRAM` | `X(8)` | Error program | `String` | `ERRLOG.EL-PROGRAM-ID` |
| `ERR-CATEGORY` | `X(2)` | Error category | `ErrorCategory` enum | No exact documented `ERRLOG` column |
| `ERR-CODE` | `X(4)` | Error code | `String` | `ERRLOG.EL-ERROR-CODE` (documented as X(8) in copybook) |
| `ERR-SEVERITY` | `S9(4) COMP` | Numeric severity | `int` or `ErrorSeverity` enum | `ERRLOG.EL-ERROR-SEVERITY` |
| `ERR-TEXT` | `X(80)` | Short error text | `String` | `ERRLOG.EL-ERROR-MESSAGE` |
| `ERR-DETAILS` | `X(256)` | Detailed error context | `String` | `ERRLOG.EL-ADDITIONAL-INFO` |
| `ERR-VSAM-SUCCESS/DUPKEY/NOTFND/EOF` | `X(2)` values `00/22/23/10` | VSAM status constants | `enum` or constants | File-status handling |
| `ERR-VSAM-22/ERR-VSAM-23/ERR-OTHER` | `X(80)` | VSAM messages | `String` constants | Error output; no direct table column |

#### `RTNCODE` — return-code management

| Field | PIC | Semantics | Java type | Mapping |
|---|---|---|---|---|
| `RC-REQUEST-TYPE` | `X(1)`; 88 `I/S/G/L/A` | Return-code operation | `ReturnCodeRequest` enum | Platform-common library |
| `RC-PROGRAM-ID` | `X(8)` | Program identifier | `String` | Job/audit metadata |
| `RC-CURRENT-CODE` / `RC-HIGHEST-CODE` / `RC-NEW-CODE` | `S9(4) COMP` | Current, highest, and new return codes | `int` / `ReturnCode` | Job control metadata |
| `RC-STATUS` | `X(1)`; 88 `S/W/E/F` | Return-code severity | `ReturnCodeStatus` enum | Job control metadata |
| `RC-MESSAGE` | `X(80)` | Return-code message | `String` | Job/audit metadata |
| `RC-RESPONSE-CODE` | `S9(8) COMP` | Response code | `int` | Job control metadata |
| `RC-START-TIME` / `RC-END-TIME` | `X(26)` | Analysis timestamps | `String` or `Instant` | Statistics metadata |
| `RC-TOTAL-CODES` | `S9(8) COMP` | Number of codes analyzed | `long` | Statistics metadata |
| `RC-MAX-CODE` / `RC-MIN-CODE` | `S9(4) COMP` | Code extrema | `int` | Statistics metadata |
| `RC-RETURN-VALUE` / `RC-HIGHEST-RETURN` | `S9(4) COMP` | Return values | `int` / `ReturnCode` | Job control metadata |
| `RC-RETURN-STATUS` | `X(1)` | Return status | `ReturnCodeStatus` enum | Job control metadata |

#### `BCHCTL` and missing `DB2STAT`

`RPTSTA00` names `COPY DB2STAT`, but no `DB2STAT` copybook is present in the
repository. Its input record fields therefore cannot be enumerated from the
supplied source set. `BCHCTL` is present and is included because it is the
declared batch-statistics/control shape relevant to the S7 plan:

| Field | PIC | Semantics | Java type | Mapping |
|---|---|---|---|---|
| `BCT-JOB-NAME` | `X(8)` | Batch job name | `String` | `BCHSTATS`/batch-control replica |
| `BCT-PROCESS-DATE` | `X(8)` | Process date | `LocalDate` after YYYYMMDD validation | `BCHSTATS`/batch-control replica |
| `BCT-SEQUENCE-NO` | `9(4)` | Step sequence | `int` | `BCHSTATS`/batch-control replica |
| `BCT-STATUS` | `X(1)`; 88 `R/A/W/D/E` | Ready, active, waiting, done, error | `BatchStatus` enum | `BCHSTATS`/batch-control replica |
| `BCT-STEP-NAME` / `BCT-PROGRAM-NAME` | `X(8)` | Step and program names | `String` | `BCHSTATS`/batch-control replica |
| `BCT-START-TIME` / `BCT-END-TIME` | `X(8)` | Step times | `String` or `LocalTime` after format confirmation | `BCHSTATS`/batch-control replica |
| `BCT-PREREQ-COUNT` | `9(2) COMP` | Prerequisite count | `int` | Batch-control replica |
| `BCT-PREREQ-NAME` | `X(8)` occurs 10 | Prerequisite job name | `List<String>` | Batch-control replica |
| `BCT-PREREQ-SEQ` | `9(4)` occurs 10 | Prerequisite sequence | `List<Integer>` | Batch-control replica |
| `BCT-PREREQ-RC` | `S9(4) COMP` occurs 10 | Required prerequisite RC | `List<Integer>` / `ReturnCode` | Batch-control replica |
| `BCT-RETURN-CODE` | `S9(4) COMP` | Job return code | `ReturnCode` enum | Batch-control replica |
| `BCT-ERROR-DESC` | `X(80)` | Job error description | `String` | Batch-control replica |
| `BCT-RESTART-COUNT` | `9(2) COMP` | Restart attempts | `int` | Batch-control replica |
| `BCT-ATTEMPT-TS` / `BCT-COMPLETE-TS` | `X(26)` | Attempt/completion timestamps | `String` or `Instant` | Batch-control replica |
| `BCT-FILLER` | `X(50)` | Reserved bytes | `String` or omitted | Batch-control record padding |

### 3.3 Documentation-only related contracts

The data dictionary documents a different `POSMSTRE` layout using numeric
account/fund keys, `POS-SHARE-BAL`, `POS-AVG-COST`, `POS-COST-BASIS`,
`POS-LAST-DATE`, `POS-LAST-TRANS`, and active/closed status. It also documents
`TRANHIST` fields and the DB2 tables `POSHIST` and `ERRLOG`. Those fields are
not the same as the directly included `POSREC`, `TRNREC`, `AUDITLOG`, and
`ERRHAND` layouts; they are retained as discrepancy context rather than
silently merged into the executable contract. [data-dictionary.md:41-119,186-218]

## 4. Interfaces

### 4.1 Inbound interfaces

| Interface | Evidence | Java target and interaction |
|---|---|---|
| Position master | `RPTPOS.jcl` DD `POSMSTRE` → `PROD.POSITION.MASTER`; `RPTPOS00` opens `POSITION-MASTER` input | S3 position-service read replica; DB read through a Spring Data/JDBC repository or batch `ItemReader` |
| Transaction history | `RPTPOS.jcl` DD `TRANHIST` → `PROD.TRANSACTION.HISTORY`; `RPTPOS00` opens `TRANSACTION-HISTORY` input | S2 transaction-service read replica; DB read, not a REST call unless the deployment forbids replica access |
| Audit log | `RPTAUD.jcl` DD `AUDITLOG` → `PROD.AUDIT.LOG`; `RPTAUD00` opens `AUDIT-FILE` input | S8 platform-common audit store/read replica; DB read |
| Error log | `RPTAUD.jcl` DD `ERRLOG` → `PROD.ERROR.LOG`; `RPTAUD00` opens `ERROR-FILE` input | S8 platform-common error store/read replica; DB read |
| DB2 statistics | `RPTSTA.jcl` DD `DB2STATS` → `PROD.DB2.STATISTICS`; `RPTSTA00` opens `DB2-STATS` input | S8 Micrometer/statistics projection or read-only metrics store; DB read; exact schema is unavailable because `DB2STAT` is missing |
| Batch statistics | `RPTSTA.jcl` DD `BCHSTATS` → `PROD.BATCH.STATISTICS`; `RPTSTA00` opens `BATCH-STATS` input | S6 batch-orchestrator job repository/read replica; DB read |

### 4.2 Outbound interfaces

| Interface | Evidence | Java target and interaction |
|---|---|---|
| Position report | `RPTPOS.jcl` DD `RPTFILE` → `PROD.DAILY.POSITION.REPORT`, fixed 132-byte records | S7 Spring Batch writer; file/object output. CSV/PDF is a plan option, not source-proven |
| Audit report | `RPTAUD.jcl` DD `RPTFILE` → `PROD.AUDIT.REPORT`, fixed 132-byte records | S7 Spring Batch writer; file/object output |
| Statistics report | `RPTSTA.jcl` DD `RPTFILE` → `PROD.SYSTEM.STATS.REPORT`, fixed 132-byte records | S7 Spring Batch writer; file/object output |
| Error/return handling | `ERRHAND`/`RTNCODE` copies and each `9999-ERROR-HANDLER` | S8 platform-common library; library call/exception mapping and batch exit status |

### 4.3 Calls, CICS, SQL, and jobs

There are no `CALL`, CICS `LINK`/`XCTL`, or `EXEC SQL` statements in the six
supplied source files. The architecture document's `DB2CONN` and `ERRPROC`
dependencies are documentation claims, not executable interfaces in these
programs. [system-architecture.md:499-506] The concrete inbound job
interfaces are the three one-step JCL jobs:

| JCL job | Program | Inputs | Output |
|---|---|---|---|
| `RPTPOS.jcl` | `RPTPOS00` | `POSMSTRE`, `TRANHIST` | `RPTFILE` → `PROD.DAILY.POSITION.REPORT` |
| `RPTAUD.jcl` | `RPTAUD00` | `AUDITLOG`, `ERRLOG` | `RPTFILE` → `PROD.AUDIT.REPORT` |
| `RPTSTA.jcl` | `RPTSTA00` | `DB2STATS`, `BCHSTATS` | `RPTFILE` → `PROD.SYSTEM.STATS.REPORT` |

The plan's suggested event flow says S3 can publish `PositionChanged` to S7
and S6 orchestrates S7, but none of the supplied reporting programs consumes
an event or exposes a service endpoint. [cobol-java-modernization-plan.md:31]

## 5. Discrepancies

1. The plan calls S7 sources `RPTPOS00`, `RPTAUD00`, and `RPTSTA00`, while the
   data dictionary and scheduling documentation use the aggregate process ID
   `RPTGEN00`; JCL uses the three concrete program names. [plan:18-27;
   data-dictionary.md:270-305]
2. The architecture matrix says each report program uses `DB2CONN` and
   `ERRPROC` and has DB2 read access, but the supplied programs contain no
   `CALL`, `EXEC SQL`, or DB2 copybook usage. [system-architecture.md:499-506]
3. The architecture reporting flow shows RPTAUD00 reading DB2 audit data and
   transaction history, while JCL allocates `AUDITLOG` and `ERRLOG`; no DB2
   audit SQL is present. [system-architecture.md:331-345; RPTAUD.jcl:8-10]
4. `RPTPOS00` moves `POS-DESCRIPTION` and `POS-PREVIOUS-VALUE`, but the
   included `POSREC` copybook defines neither field. It defines
   `POS-INVESTMENT-ID`, `POS-COST-BASIS`, and `POS-MARKET-VALUE` instead.
   [RPTPOS00.cbl:134-140; POSREC.cpy:7-23]
5. `RPTPOS00` uses `POS-KEY` and `TRAN-KEY`, but the included copybooks define
   composite `POS-KEY` and `TRN-KEY`; no `TRAN-KEY` is present in `TRNREC`.
   [RPTPOS00.cbl:17-27; POSREC.cpy:7-10; TRNREC.cpy:7-11]
6. `RPTPOS00` declares `COPY POSREC` and `COPY TRNREC`, but its displayed
   quantity has two decimals while the copybook quantity has four decimal
   places. [RPTPOS00.cbl:65-75; POSREC.cpy:12]
7. `RPTAUD00` declares `WS-AUD-TYPE X(10)`, `WS-ERR-CODE X(4)`, and
   `WS-ERR-MESSAGE X(80)`, while `AUDITLOG` uses audit type X(4), and the
   DB2 error copybook uses error code X(8) and message X(200).
   [RPTAUD00.cbl:64-80; AUDITLOG.cpy:14-36; DBTBLS.cpy:33-50]
8. `RPTSTA00` declares `COPY DB2STAT`, but no `DB2STAT` file exists in the
   repository; therefore the DB2 statistics record contract is missing.
   [RPTSTA00.cbl:33-36]
9. The data dictionary's `POSMSTRE` and `TRANHIST` layouts use account/fund
   identifiers and different numeric scales from `POSREC`/`TRNREC`.
   [data-dictionary.md:41-119; POSREC.cpy:6-23; TRNREC.cpy:6-31]
10. The data dictionary describes DB2 `POSHIST` and `ERRLOG` tables, but the
    report programs do not issue SQL and the supplied `AUDITLOG` copybook has
    no documented DB2 audit table mapping. [data-dictionary.md:186-218]
11. The architecture document says reports create reconciliation, exception,
    control, and trend sections, but the corresponding paragraphs are absent
    from the supplied programs. [system-architecture.md:172-191;
    RPTPOS00.cbl:147-150; RPTAUD00.cbl:134-137; RPTSTA00.cbl:172-175]
12. The JCL allocates fixed-block, 132-byte datasets, while the modernization
    plan proposes CSV/PDF reports or SQL views/Grafana; no conversion contract
    is defined. [RPTPOS.jcl:10-13; RPTAUD.jcl:10-13; RPTSTA.jcl:10-13;
    cobol-java-modernization-plan.md:26-31]
13. The data dictionary says `RPTGEN00` has no restart requirement, while the
    plan says S6 orchestrates S7 and the JCL contains no restart/condition
    clauses. [data-dictionary.md:270-305; RPTPOS.jcl:1-16; RPTAUD.jcl:1-16;
    RPTSTA.jcl:1-16]
14. The report programs use a return code of 12 for open failures, whereas the
    data dictionary labels 0012 a critical abend and lists 0000/0004/0008/0016
    as other standard outcomes. No success or warning assignment is present in
    the supplied paragraphs. [RPTPOS00.cbl:157-160; data-dictionary.md:279-286]

## 6. Proposed Java design

### Package layout

```text
com.example.reporting
├── batch
│   ├── PositionReportJobConfig
│   ├── AuditReportJobConfig
│   ├── StatisticsReportJobConfig
│   ├── PositionReportStep
│   ├── AuditReportStep
│   └── StatisticsReportStep
├── domain
│   ├── PositionRecord
│   ├── TransactionRecord
│   ├── AuditRecord
│   ├── ErrorRecord
│   ├── BatchStatisticsRecord
│   └── enums
├── repository
│   ├── PositionReadRepository
│   ├── TransactionHistoryReadRepository
│   ├── AuditReadRepository
│   ├── ErrorReadRepository
│   └── StatisticsReadRepository
├── service
│   ├── PositionReportService
│   ├── AuditReportService
│   ├── StatisticsReportService
│   └── ReportFormattingService
├── writer
│   ├── FixedWidthReportWriter
│   ├── CsvReportWriter
│   └── PdfReportWriter
└── config
    ├── ReportingProperties
    └── ReadReplicaDataSourceConfig
```

### Spring components and boundaries

- Use three Spring Batch jobs, one per supplied JCL program, with chunk-oriented
  readers over read-only replica repositories and deterministic report writers.
- Keep the implemented 132-character fixed-width format as the compatibility
  writer. Add CSV/PDF writers only behind configuration once the product output
  contract is selected.
- `PositionReportService` owns the implemented percentage expression and
  formatting. It must expose denominator-zero and numeric-rounding decisions as
  configuration or validation, not silently choose behavior.
- `AuditReportService` and `StatisticsReportService` should model only the
  declared fields and implemented headers until the missing paragraphs and
  `DB2STAT` record contract are resolved.
- Use repository interfaces for read-only DB views/replicas. Do not call S2/S3
  synchronously from the batch job unless deployment constraints prohibit a
  replica; the COBOL jobs are file-based and have no service-call contract.
- Put return-code mapping, error exceptions, audit publication, and metrics in
  the S8 platform-common library. Map file-open/read failures to the documented
  critical return code 12, preserving the original message and source job.
- S6 batch-orchestrator should launch the jobs and record job status. The
  supplied JCL does not define conditional dependencies, restart checkpoints,
  or event consumption for S7; those are orchestration decisions rather than
  implemented S7 behavior.
- Define `PositionStatus`, `TransactionType`, `TransactionStatus`, `AuditType`,
  `AuditAction`, `AuditStatus`, `BatchStatus`, `ErrorCategory`, and
  `ReturnCode` as enums backed by their COBOL values.

## 7. Open questions

1. What is the authoritative position and transaction schema: the directly
   included `POSREC`/`TRNREC` layouts, or the account/fund-based layouts in the
   data dictionary?
2. Should S7 read S2/S3/S8/S6 database replicas directly, consume a
   `PositionChanged` event, or use a generated reporting data mart?
3. What are the missing `DB2STAT` record fields, source table, units, and
   aggregation semantics?
4. What should `POS-DESCRIPTION` and `POS-PREVIOUS-VALUE` map to, given that
   they are referenced by `RPTPOS00` but absent from `POSREC`?
5. What should happen when `POS-PREVIOUS-VALUE` is zero, and what exact
   rounding mode should apply to the two-decimal change percentage?
6. Should the Java service preserve fixed-width 132-character output, or is
   CSV/PDF/SQL-view output the approved replacement?
7. Are the absent RPTPOS00 totals/exceptions/metrics, RPTAUD00 summaries, and
   RPTSTA00 accumulators/calculations intentionally stubbed, or are their
   implementations stored outside the supplied repository?
8. Does `RPTGEN00` represent one aggregate job or should the three concrete JCL
   jobs remain independently schedulable?
9. Should report generation be restartable in Spring Batch despite the
   documentation's “No” restart value for `RPTGEN00`?
10. Should audit records be sourced from a DB2 table, the `AUDITLOG` VSAM
    dataset, or both, and which audit fields are reportable?
11. Should a file-open/read failure terminate the job with return code 12,
    map to a Java exception, or be represented as a failed Spring Batch exit
    status with a separate platform return code?
12. What report sections are required for compliance: position
    reconciliation, transaction activity, exceptions, control verification,
    resource utilization, and trend analysis?
