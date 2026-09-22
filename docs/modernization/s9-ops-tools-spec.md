# S9 Ops Tools Modernization Specification

## 1. Purpose & scope

S9 contains three batch utilities for operational administration of the investment-portfolio platform:

- `UTLMNT00` consumes fixed-width control records and dispatches archive, cleanup, VSAM reorganization, and analysis operations for a named file.
- `UTLMON00` loads monitoring configuration, collects resource/performance metrics, compares them with configured thresholds, writes status logs, and emits alerts.
- `UTLVAL00` consumes validation requests and checks position-master and transaction-history data for integrity, cross-reference, format, and balance issues.

The supplied implementations are orchestration shells. Many operation paragraphs are invoked but are not present in the files, so this specification distinguishes behavior implemented in COBOL from behavior stated only in documentation. The Java target is the S9 `ops-tools` component from the modernization plan: Spring Boot Actuator for health/metrics exposure plus a restricted Spring Shell/admin CLI for maintenance and validation operations.

## 2. Business rules

Rules are numbered in source order. A rule marked **INFERRED FROM DOCS** is not implemented by the supplied COBOL.

1. `UTLMNT00` identifies itself as the file-maintenance utility and describes archive processing, cleanup, VSAM reorganization, and space management (`src/programs/utility/UTLMNT00.cbl:2-13`).
2. `UTLMNT00` reads `CTLFILE` sequentially as fixed records and tracks file status in two bytes (`UTLMNT00.cbl:21-24,37-43,59-62`).
3. Each maintenance control record contains an 8-character function, a 44-character file name, and 100 characters of parameters (`UTLMNT00.cbl:40-43`).
4. `UTLMNT00` writes variable records up to 32,760 characters to `ARCHFILE` and fixed 132-character report records to `RPTFILE` (`UTLMNT00.cbl:45-53`).
5. The valid maintenance functions are exactly `ARCHIVE`, `CLEANUP`, `REORG`, and `ANALYZE` (`UTLMNT00.cbl:70-74`).
6. The maintenance end-of-control flag starts as `N`; reading past the final control record sets it to `Y`, terminating the processing loop (`UTLMNT00.cbl:64-68,122-130`).
7. `UTLMNT00` initializes the read, written, and error counters to zero before processing (`UTLMNT00.cbl:76-79,119-120`).
8. A control function dispatches to archive processing, cleanup processing, reorganization processing, or analysis processing according to an exact string comparison (`UTLMNT00.cbl:132-146`).
9. An unrecognized maintenance function records `INVALID FUNCTION SPECIFIED` through the common error message and error handler (`UTLMNT00.cbl:142-146`).
10. Archive processing copies the control file name into the VSAM name and invokes open, archive-records, and close operations in that order (`UTLMNT00.cbl:148-152`).
11. Cleanup processing copies the control file name and invokes space analysis, deletion of old data, and catalog update in that order (`UTLMNT00.cbl:154-158`).
12. Reorganization processing copies the control file name and invokes export, delete/define, and import in that order (`UTLMNT00.cbl:160-164`).
13. Analysis processing copies the control file name and invokes statistics collection followed by report generation (`UTLMNT00.cbl:166-169`).
14. The detailed archive, cleanup, reorganization, analysis, VSAM, catalog, and report paragraphs invoked by `UTLMNT00` are not present in the supplied file; their side effects, selection criteria, and record counters are therefore unspecified by code (`UTLMNT00.cbl:148-169`).
15. A maintenance open failure places a fixed diagnostic in the common error message and invokes the error handler for the control, archive, or report file (`UTLMNT00.cbl:97-117`).
16. Each invocation of the maintenance error handler increments `WS-ERROR-COUNT`, displays the common error message on the console, and only when the count becomes greater than 100 sets return code 12 and executes `GOBACK` (`UTLMNT00.cbl:176-182`). No earlier return code is assigned by this program.
17. The maintenance program closes the control, archive, and report files during normal cleanup (`UTLMNT00.cbl:171-174`).
18. The architecture documentation describes `UTLMNT00` as handling archiving, cleanup, VSAM reorganization, and space management (**INFERRED FROM DOCS**, `documentation/technical/system-architecture.md:133-140`).
19. `UTLMON00` reads fixed monitor configuration records from `MONCFG`, writes fixed monitor-log records to `MONLOG`, writes fixed alert records to `ALERTS`, and dynamically reads indexed `DB2STATS` by `STAT-KEY` (`src/programs/utility/UTLMON00.cbl:21-39,45-72`).
20. A monitor configuration contains resource type, threshold type, numeric threshold value with two fractional digits, alert level, and a 50-character alert action (`UTLMON00.cbl:48-53`).
21. A monitor log contains a 26-character timestamp, resource type, metric name, metric value with two fractional digits, and a 10-character status (`UTLMON00.cbl:58-63`).
22. An alert contains a 26-character timestamp, alert level, resource, and an 80-character message (`UTLMON00.cbl:68-72`).
23. Monitor resource categories are `CPU`, `MEMORY`, `DASD`, and `DB2`; threshold categories are `UTIL`, `RESPONSE`, `QUEUE`, and `ERROR`; alert levels are `INFO`, `WARNING`, and `CRITICAL` (`UTLMON00.cbl:84-99`).
24. The monitor end-of-config flag starts as `N` and becomes `Y` at end-of-file; the threshold flag is a separate `N`/`Y` switch used to decide whether an alert is written (`UTLMON00.cbl:101-105,171-179,206-210`).
25. Current monitor metrics are represented as CPU, memory, DASD, and DB2 utilization with two decimal places, DB2 response with two decimal places, and integer DB2 queue/error counts (`UTLMON00.cbl:107-114`). No arithmetic, scale conversion, rounding, or truncation is implemented in the supplied file.
26. Initialization opens all four files and captures a timestamp from the system time source; any non-`00` open status invokes the monitor error handler (`UTLMON00.cbl:134-169`).
27. The monitor configuration reader processes every input record through the missing `1310-STORE-CONFIG` paragraph; the storage semantics are not implemented in the supplied file (`UTLMON00.cbl:171-179`).
28. Each monitor cycle collects CPU, memory, DASD, and DB2 metrics in that order, checks utilization/response/queue/error thresholds in that order, logs resource and performance status, and generates alerts if `THRESHOLD-MET` is true (`UTLMON00.cbl:181-210`).
29. The monitor cycle calls `ILBOABN0` with `WS-MINUTE` and then refreshes the timestamp before the next cycle (`UTLMON00.cbl:181-187`).
30. The monitor main loop repeats until the hour component equals 23; the exact wait/abort semantics of `ILBOABN0` and the metric collectors are not defined in the supplied file (`UTLMON00.cbl:127-132,181-187`).
31. The monitor error handler displays the common error message, sets return code 12, and immediately executes `GOBACK` (`UTLMON00.cbl:218-221`).
32. The monitor closes configuration, log, alert, and DB2 statistics files during normal cleanup (`UTLMON00.cbl:212-216`).
33. The architecture documentation states that `UTLMON00` tracks utilization, collects performance metrics, monitors thresholds, and generates alerts (**INFERRED FROM DOCS**, `documentation/technical/system-architecture.md:142-147`).
34. The documentation names CPU, memory, DASD, DB2 statistics, response time, transaction rate, DB2 performance, batch duration, job status, error rate, recovery events, and security incidents as monitoring points (**INFERRED FROM DOCS**; only the first four resource collectors and four threshold categories are called by code, `system-architecture.md:748-768`).
35. `UTLVAL00` reads fixed validation-control records, dynamically reads indexed position master records keyed by `POS-KEY`, dynamically reads indexed transaction history keyed by `TRAN-KEY`, and writes fixed 132-character error reports (`src/programs/utility/UTLVAL00.cbl:21-40,47-57`).
36. A validation-control record contains a 10-character validation type and 70 characters of parameters (`UTLVAL00.cbl:50-52`).
37. Validation types are exactly `INTEGRITY`, `XREF`, `FORMAT`, and `BALANCE` (`UTLVAL00.cbl:69-73`).
38. The validation end-of-input flag starts as `N` and becomes `Y` at end-of-file; `ERROR-FOUND` starts as `N` and is set to `Y` by the error handler (`UTLVAL00.cbl:75-79,138-146,186-190`).
39. Validation totals are initialized to zero: records read, valid, error, total amount, and control total (`UTLVAL00.cbl:81-86,135-136`). No code increments the read/valid totals or calculates either amount total in the supplied file.
40. Each validation record dispatches to integrity, cross-reference, format, or balance checks by exact validation type; all detailed check paragraphs are invoked but absent (`UTLVAL00.cbl:148-178`).
41. An unrecognized validation type records `INVALID VALIDATION TYPE` and invokes the error handler (`UTLVAL00.cbl:158-162`).
42. Integrity validation is defined by the call sequence as position-integrity followed by transaction-integrity; cross-reference validation is position-xref followed by transaction-xref (`UTLVAL00.cbl:164-170`).
43. Format validation is defined by the call sequence as position-format followed by transaction-format (`UTLVAL00.cbl:172-174`).
44. Balance validation is defined by the call sequence as position accumulation followed by balance verification (`UTLVAL00.cbl:176-178`).
45. The validation error handler increments `WS-RECORDS-ERROR` by exactly 1, sets `ERROR-FOUND`, copies the common error message into the 98-character description field, and writes one error record; it does not assign a return code (`UTLVAL00.cbl:186-190`).
46. The documentation says validation covers integrity, cross-reference, format, and balance reconciliation (**INFERRED FROM DOCS**, `system-architecture.md:149-153`).
47. The documentation-derived transaction rules are: account number must be numeric and exist in customer master; fund ID must exist in fund master; transaction date must not be in the future; share quantity must be nonzero for buy/sell; fee amount must be nonzero; and buy/sell price must be greater than zero (**INFERRED FROM DOCS**, `data-dictionary.md:235-245`).
48. The documentation-derived position rules are: share balance must not become negative; cost basis must be updated for buy/sell; average cost must be recalculated for buys; and position status must be active for transactions (**INFERRED FROM DOCS**, `data-dictionary.md:246-251`). The supplied `UTLVAL00` does not implement these checks.
49. Documentation error codes E001–E004 are rejecting errors and W001–W002 are warning/log outcomes (**INFERRED FROM DOCS**, `data-dictionary.md:253-262`); no mapping from these codes to `UTLVAL00` output fields is implemented.
50. The documented batch return codes are 0000 success, 0004 warning complete, 0008 errors complete, 0012 critical abend, and 0016 environment error (**INFERRED FROM DOCS**, `data-dictionary.md:279-287`). The supplied utilities explicitly set only numeric return code 12 in the `UTLMNT00` threshold-exit path and in `UTLMON00` error handling.
51. The JCL runs `UTLMNT00` in job `UTLMNT00` with `CTLFILE`, newly cataloged `ARCHFILE`, and newly cataloged `RPTFILE` DDs (`src/jcl/utility/UTLMNT.jcl:1-18`).
52. The JCL runs `UTLMON00` in job `UTLMON00` with `MONCFG`, newly cataloged `MONLOG`, newly cataloged `ALERTS`, and shared `DB2STATS` DDs (`UTLMON.jcl:1-20`).
53. The JCL runs `UTLVAL00` in job `UTLVAL00` with `VALCTL`, `POSMSTRE`, `TRANHIST`, and newly cataloged `ERRRPT` DDs (`UTLVAL.jcl:1-17`).
54. `src/jcl/utility/README.md` is empty, so it supplies no additional operational rules, parameters, return-code handling, or restart instructions.

### Arithmetic and numeric semantics

The selected COBOL contains no business calculation, division, multiplication, `COMPUTE`, `ROUNDED`, or `ON SIZE ERROR` statement. The only arithmetic is `ADD 1 TO WS-ERROR-COUNT` and `ADD 1 TO WS-RECORDS-ERROR` (`UTLMNT00.cbl:177`, `UTLVAL00.cbl:187`). Those counters have integer PIC definitions, so there is no fractional truncation or rounding behavior to preserve. Numeric fields with implied decimals are data contracts only; the absent collector/check paragraphs do not establish conversion semantics.

## 3. Data contracts

### 3.1 Records defined in the selected COBOL

| Record/field | PIC | Semantics | Proposed Java type | Mapping |
|---|---|---|---|---|
| `CONTROL-RECORD.CTL-FUNCTION` | `X(8)` | Maintenance operation selector | `String` | Control-file record from `CTLFILE`; no DB2 column |
| `CONTROL-RECORD.CTL-FILE-NAME` | `X(44)` | Target VSAM/file name | `String` | VSAM/catalog target named by control input |
| `CONTROL-RECORD.CTL-PARAMETERS` | `X(100)` | Operation parameters; not parsed in supplied code | `String` | Control-file record; semantics unresolved |
| `ARCHIVE-RECORD` | `X(32760)` | Variable archive payload | `String` or `byte[]` if binary preservation is required | `ARCHFILE`; no DB2 column |
| `REPORT-RECORD` | `X(132)` | Fixed maintenance report line | `String` | `RPTFILE`; no DB2 column |
| `CONFIG-RECORD.CFG-RESOURCE-TYPE` | `X(10)` | Monitored resource category | `String` (candidate enum: CPU/MEMORY/DASD/DB2) | `MONCFG`; no DB2 column |
| `CONFIG-RECORD.CFG-THRESHOLD-TYPE` | `X(10)` | Threshold category | `String` (candidate enum: UTIL/RESPONSE/QUEUE/ERROR) | `MONCFG`; no DB2 column |
| `CONFIG-RECORD.CFG-THRESHOLD-VALUE` | `9(9)V99` | Configured threshold | `BigDecimal` scale 2 | `MONCFG`; no DB2 column identified |
| `CONFIG-RECORD.CFG-ALERT-LEVEL` | `X(10)` | Alert severity | `String` (candidate enum: INFO/WARNING/CRITICAL) | `MONCFG`; no DB2 column identified |
| `CONFIG-RECORD.CFG-ALERT-ACTION` | `X(50)` | Action to perform on threshold breach | `String` | `MONCFG`; no DB2 column identified |
| `LOG-RECORD.LOG-TIMESTAMP` | `X(26)` | Metric observation timestamp | `String` initially; parse only after format is confirmed | `MONLOG`; no DB2 column identified |
| `LOG-RECORD.LOG-RESOURCE-TYPE` | `X(10)` | Resource measured | `String`/candidate enum | `MONLOG`; no DB2 column identified |
| `LOG-RECORD.LOG-METRIC-NAME` | `X(20)` | Metric name | `String` | `MONLOG`; no DB2 column identified |
| `LOG-RECORD.LOG-METRIC-VALUE` | `9(9)V99` | Metric value | `BigDecimal` scale 2 | `MONLOG`; no DB2 column identified |
| `LOG-RECORD.LOG-STATUS` | `X(10)` | Metric status | `String` | `MONLOG`; no DB2 column identified |
| `ALERT-RECORD.ALERT-TIMESTAMP` | `X(26)` | Alert timestamp | `String` initially; parse only after format is confirmed | `ALERTS`; no DB2 column identified |
| `ALERT-RECORD.ALERT-LEVEL` | `X(10)` | Alert severity | `String`/candidate enum | `ALERTS`; no DB2 column identified |
| `ALERT-RECORD.ALERT-RESOURCE` | `X(10)` | Alert resource | `String`/candidate enum | `ALERTS`; no DB2 column identified |
| `ALERT-RECORD.ALERT-MESSAGE` | `X(80)` | Alert text | `String` | `ALERTS`; no DB2 column identified |
| `VALIDATION-RECORD.VAL-TYPE` | `X(10)` | Validation operation selector | `String` (candidate enum: INTEGRITY/XREF/FORMAT/BALANCE) | `VALCTL`; no DB2 column |
| `VALIDATION-RECORD.VAL-PARAMETERS` | `X(70)` | Validation parameters; not parsed in supplied code | `String` | `VALCTL`; semantics unresolved |
| `ERROR-RECORD` | `X(132)` | Fixed validation error line | `String` | `ERRRPT`; no DB2 column |

The source also defines status/flag and counter fields. `PIC X` flags should be represented as booleans internally and serialized as their legacy values at file boundaries; `PIC 9(n)` counters should be `long`; `PIC XX` file statuses should be `String` (two-character legacy status). Specifically: `WS-END-OF-CTL`, `WS-FUNCTION-FLAG`, `WS-END-OF-CONFIG`, `WS-THRESHOLD-MET`, `WS-END-OF-VAL`, and `WS-ERROR-FOUND` are flags; `WS-RECORDS-READ`, `WS-RECORDS-WRITTEN`, `WS-ERROR-COUNT`, `WS-RECORDS-VALID`, and `WS-RECORDS-ERROR` are `long`; `WS-TOTAL-AMOUNT` and `WS-CONTROL-TOTAL` are `BigDecimal` scale 2; and `WS-CTL-STATUS`, `WS-ARCH-STATUS`, `WS-REPORT-STATUS`, `WS-CFG-STATUS`, `WS-LOG-STATUS`, `WS-ALERT-STATUS`, `WS-DB2-STATUS`, `WS-VAL-STATUS`, `WS-POS-STATUS`, `WS-TRAN-STATUS`, and `WS-RPT-STATUS` are two-character `String` values (`UTLMNT00.cbl:59-84`, `UTLMON00.cbl:78-125`, `UTLVAL00.cbl:63-93`).

`WS-ERR-TYPE` (`X(10)`), `WS-ERR-KEY` (`X(20)`), and `WS-ERR-DESC` (`X(98)`) form the validation error-line fields (`UTLVAL00.cbl:88-93`) and map to `String`; the two filler fields are fixed two-space separators and should not become domain fields.

### 3.2 Referenced copybooks and documented domain records

The selected programs reference `RTNCODE`, `ERRHAND`, `DB2STAT`, `POSREC`, and `TRNREC`, but their definitions are outside the source list requested for this analysis. Their contracts must therefore remain explicit integration boundaries rather than invented field layouts:

- `RTNCODE`: return-code contract. Map to the shared `platform-common` `ReturnCode` enum. The documentation defines 0000/0004/0008/0012/0016; the copybook field name/PIC is not visible in the selected sources.
- `ERRHAND`: common error-message contract. Map to a shared `OpsError`/`ErrorReport` value object; the `WS-ERROR-MESSAGE` field name is used by all three utilities, but its PIC is not visible in the selected sources.
- `DB2STAT`: indexed DB2-statistics record keyed by `STAT-KEY`; map to the monitoring metrics repository. Its fields are not visible in the selected sources.
- `POSREC` and `TRNREC`: position and transaction records used by `UTLVAL00`; the file keys `POS-KEY` and `TRAN-KEY` are visible, but copybook field definitions are not. The documentation provides the following **INFERRED FROM DOCS** layouts and mappings:

| Documented record | Fields and proposed Java types | Legacy mapping |
|---|---|---|
| Position master | `POS-ACCOUNT-NO` `9(09)` → `Long`; `POS-FUND-ID` `X(06)` → `String`; `POS-CUSIP` `X(09)` → `String`; `POS-SHARE-BAL` `S9(11)V999` → `BigDecimal` scale 3; `POS-AVG-COST` `9(5)V9999` → `BigDecimal` scale 4; `POS-COST-BASIS` `S9(11)V99` → `BigDecimal` scale 2; `POS-LAST-DATE` `9(08)` → `LocalDate` after YYYYMMDD parsing; `POS-LAST-TRANS` `X(12)` → `String`; `POS-STATUS` `X(01)` → enum ACTIVE/CLOSED | VSAM `POSMSTRE`; DB2 `INVESTMENT_POSITIONS` where the table definition is available in the modernization plan, otherwise canonical position service schema |
| Transaction/history | `HIST-TIMESTAMP` `X(26)` → `String`/timestamp after format confirmation; `HIST-ACCOUNT-NO` `9(09)` → `Long`; `HIST-FUND-ID` `X(06)` → `String`; `HIST-TRANS-ID` `X(12)` → `String`; `HIST-TRANS-TYPE` `X(02)` → enum/candidate String; `HIST-SHARE-QTY` `S9(11)V999` → `BigDecimal` scale 3; `HIST-PRICE` `9(5)V9999` → `BigDecimal` scale 4; `HIST-AMOUNT` `S9(11)V99` → `BigDecimal` scale 2; `HIST-RESULT-CODE` `X(04)` → `String`; `HIST-BEFORE-BAL` and `HIST-AFTER-BAL` `S9(11)V999` → `BigDecimal` scale 3 | VSAM `TRANHIST`; DB2 `TRANSACTION_HISTORY`/`POSHIST` according to the canonical data-model decision |

No selected source contains a SQL statement, DB2 table column, or explicit calculation that establishes a more precise S9 persistence mapping. The Java implementation must not silently invent one.

## 4. Interfaces

| Direction/interface | Legacy behavior | Java target and interaction |
|---|---|---|
| `UTLMNT.jcl` → `UTLMNT00` | Batch invocation with `CTLFILE`, `ARCHFILE`, `RPTFILE` DDNAMEs | `ops-tools` Spring Batch job or Spring Shell command; file ingest/output adapter, not REST |
| `UTLMON.jcl` → `UTLMON00` | Batch invocation with `MONCFG`, `MONLOG`, `ALERTS`, `DB2STATS` DDNAMEs | Actuator/Micrometer collection plus scheduled ops job; configuration and output adapters, with DB2 statistics read through a repository |
| `UTLVAL.jcl` → `UTLVAL00` | Batch invocation with `VALCTL`, `POSMSTRE`, `TRANHIST`, `ERRRPT` DDNAMEs | Admin CLI/Spring Batch validation step; DB reads for positions/history and report writer |
| `CALL 'ILBOABN0' USING WS-MINUTE` | Monitor loop invokes an external wait/abort-related runtime call | Java scheduler delay/cancellation policy or library call; no REST/event mapping is justified by the source |
| `COPY ERRHAND` | Common error message consumed by handlers | `platform-common` error value object/library call |
| `COPY RTNCODE` | Shared return-code area | `platform-common` `ReturnCode` enum/library call |
| `COPY DB2STAT` | Indexed DB2-statistics record (`STAT-KEY`) | Monitoring repository/DB read; exact table is not identified in the selected sources |
| `COPY POSREC`, `COPY TRNREC` | Position and transaction records used by validation | Position service and transaction service repositories/DB reads; VSAM adapters are transitional |
| `CTLFILE`, `VALCTL`, `MONCFG` | Sequential fixed-format control/configuration inputs | File adapter for compatibility; REST is not required for batch parity |
| `POSMSTRE`, `TRANHIST`, `DB2STATS` | Indexed dynamic reads | Repository calls: position/transaction/monitoring DB reads; no outbound event is specified |
| `ARCHFILE`, `RPTFILE`, `MONLOG`, `ALERTS`, `ERRRPT` | Sequential output files | Report/archive writers; optionally publish operational alert events only after a product decision |
| `documentation/technical/system-architecture.md` utility dependency table | Documents `ERRPROC` for maintenance and `DB2CONN, ERRPROC` for validation/monitoring | `platform-common` error handling and Spring transaction/repository infrastructure, not standalone services |

The three JCL jobs are the only concrete job interfaces supplied. No SQL statement, CICS `LINK`/`XCTL`, or explicit outbound `CALL` other than `ILBOABN0` appears in the selected COBOL.

## 5. Discrepancies

1. `src/jcl/utility/README.md` is empty, despite the repository containing utility JCL and the architecture documentation describing utility behavior.
2. The architecture dependency table says `UTLVAL00` has DB2 access and `UTLMON00` has DB2 read/write access (`system-architecture.md:508-515`), but the selected code has no `EXEC SQL`; it reads `DB2-STATS` as an indexed file and reads VSAM position/history files.
3. The architecture dependency table says `UTLMNT00` depends on `ERRPROC`, while the selected code only shows `COPY ERRHAND` and no `CALL 'ERRPROC'` (`UTLMNT00.cbl:55-57`).
4. The architecture documentation calls the maintenance operation “space management,” but the selected source only invokes missing paragraphs; no space calculation or deletion rule is implemented (`UTLMNT00.cbl:154-158`).
5. `UTLMNT00` declares `WS-RECORDS-READ`, `WS-RECORDS-WRITTEN`, `WS-VSAM-FUNCTION`, and `WS-VSAM-STATUS`, but the selected procedure never updates or consumes them (`UTLMNT00.cbl:76-84`).
6. `UTLMNT00` declares `WS-FUNCTION-FLAG` and `VALID-FUNCTION`, but dispatch does not set or test that condition (`UTLMNT00.cbl:64-68,132-146`).
7. `UTLMON00` declares `DB2-STATS` as indexed with `STAT-KEY`, but no `STAT-KEY` record operations or DB2 metric implementation is present (`UTLMON00.cbl:35-39,189-193`).
8. `UTLMON00` invokes `1310-STORE-CONFIG`, metric collectors, threshold checks, log writers, alert formatters, and alert writers that are not defined in the file (`UTLMON00.cbl:171-210`).
9. `UTLMON00` documents/declares threshold and alert categories but contains no comparison operators or threshold arithmetic, so configured values cannot affect behavior in the supplied program (`UTLMON00.cbl:84-114,195-210`).
10. `UTLMON00` calls `ILBOABN0` with `WS-MINUTE`; the source does not define whether this is a delay, an abort, or a platform-specific wait (`UTLMON00.cbl:181-187`).
11. The architecture documentation lists monitoring points such as transaction rates, batch duration, recovery events, and security incidents, but the selected code only names four resource collectors and four threshold categories (`system-architecture.md:748-768`; `UTLMON00.cbl:189-199`).
12. `UTLVAL00` invokes every detailed validation paragraph but defines none of them, so its advertised integrity, xref, format, and balance behavior is not executable in the supplied source (`UTLVAL00.cbl:164-178`).
13. `UTLVAL00` declares total/valid/error counters and amount/control totals, but only increments the error counter in the visible code (`UTLVAL00.cbl:81-86,186-190`).
14. `UTLVAL00` has no visible assignment to `RETURN-CODE`; this conflicts with the repository-wide documented return-code contract and differs from `UTLMON00`, which explicitly sets 12 on error (`UTLVAL00.cbl:186-190`; `data-dictionary.md:279-287`).
15. Documentation describes transaction/position validation rules and E/W error codes, but no selected utility paragraph implements or emits them (`data-dictionary.md:235-262`).
16. The documentation describes `POSHIST`, `ERRLOG`, and other DB2 structures, but the selected utility code does not issue SQL or identify DB2 columns for its records (`data-dictionary.md:186-218`; selected sources).
17. The JCL comments and program IDs are consistent (`UTLMNT00`, `UTLMON00`, `UTLVAL00`), but the JCL DD names are operational dataset aliases rather than semantic record names; no README explains their required formats.

## 6. Proposed Java design

Use a single deployable `ops-tools` Spring Boot application with separate adapters for compatibility and operational APIs:

```text
com.coggtm.portfolio.ops
├── OpsToolsApplication
├── maintenance/
│   ├── MaintenanceController
│   ├── MaintenanceCommandService
│   ├── ControlRecordReader
│   ├── ArchiveWriter
│   └── MaintenanceReportWriter
├── monitoring/
│   ├── MonitoringController
│   ├── MonitoringScheduler
│   ├── MetricsCollector
│   ├── ThresholdEvaluator
│   ├── MonitorLogRepository
│   └── AlertPublisher
├── validation/
│   ├── ValidationController
│   ├── ValidationJob
│   ├── ValidationService
│   ├── PositionRepository
│   ├── TransactionHistoryRepository
│   └── ErrorReportWriter
├── legacy/
│   ├── FixedRecordCodec
│   ├── VsamCompatibilityAdapter
│   └── ReturnCodeMapper
└── config/
    ├── OpsSecurityConfiguration
    ├── SchedulingConfiguration
    └── OpsProperties
```

- `MaintenanceCommandService` exposes archive/cleanup/reorg/analyze commands only to an admin-authorized principal. Since the COBOL operation bodies are missing, each command must report an explicit unsupported-operation result until the corresponding legacy behavior is supplied; it must not claim to have archived or deleted records.
- `MonitoringScheduler` uses Micrometer/Actuator collectors for CPU, memory, disk, and database observations. `ThresholdEvaluator` uses explicit `BigDecimal` scale 2 values and a versioned configuration repository; alert severity remains an enum only after accepted values are confirmed.
- `ValidationJob` is a Spring Batch step or admin CLI command that reads the control request, invokes repositories, writes fixed-width-compatible error lines, and returns the documented `ReturnCode`. It must keep the unresolved copybook codecs behind `legacy`.
- `PositionRepository` and `TransactionHistoryRepository` target the position/transaction services from the decomposition plan through DB reads. VSAM adapters remain a migration compatibility layer, not a new system of record.
- `AlertPublisher` should remain a library boundary initially. An `OperationalThresholdBreached` event is a proposed integration only; the supplied COBOL writes an alert file and does not publish an event.
- `OpsSecurityConfiguration` applies the documentation’s admin security classification to maintenance operations (**INFERRED FROM DOCS**, `system-architecture.md:875-881`), with audit logging delegated to `platform-common`.
- Configuration must include input/output encodings, fixed-width lengths, legacy return-code policy, scheduler interval, threshold comparison semantics, and an explicit feature flag for any operation whose COBOL paragraph is absent.

## 7. Open questions

1. What are the definitions of `RTNCODE`, `ERRHAND`, `DB2STAT`, `POSREC`, and `TRNREC`, including the actual PIC layouts and error-message length?
2. Should S9 preserve the three JCL file interfaces, or may callers use only Spring Shell/REST while compatibility adapters run internally?
3. What do the missing maintenance paragraphs do: which records qualify for archive/delete, how are VSAM files reorganized, and how are catalog updates performed?
4. What are the monitor metric sources, threshold comparison operators, polling interval, and semantics of `ILBOABN0`?
5. Should monitor logs and alerts be files, database rows, Actuator/Micrometer metrics, events, or all of these for the migration period?
6. Which DB2 tables/columns are authoritative for position master, transaction history, monitor statistics, and validation results?
7. What exact validation behavior should the missing `UTLVAL00` paragraphs implement, including key lookup failures, duplicate handling, totals, and balance formulas?
8. Which documented E001–E004/W001–W002 codes apply to S9, and what return code should validation use when errors are found?
9. Should an invalid control/validation type stop the whole job, skip the record, or continue after writing an error report?
10. Is the documented `0012` critical-error behavior intended for every file-open failure, or only for the monitor and maintenance paths that explicitly set return code 12?
11. What security roles and audit requirements govern maintenance commands and validation runs?
12. What is the canonical mapping between VSAM `POSMSTRE`/`TRANHIST` and the modern position/transaction service data stores?
