# S10 Test Harness and Parity Suite Specification

## 1. Purpose & scope

S10 supplies repeatable portfolio and transaction fixtures, executes the
validation scenarios that can be represented by the legacy test-control files,
and emits a fixed-width validation report. In business terms, it is the
controlled test-data and evidence layer for proving that the modernized
portfolio, transaction, position, and reporting components preserve legacy
outcomes.

The analyzed implementation has three entry points:

- `TSTGEN00` reads test configuration and a random seed, then dispatches
  portfolio, transaction, error, or volume generation.
- `TSTVAL00` reads test cases plus expected and actual result streams and is
  intended to produce per-test and summary validation output.
- `PORTTEST` independently generates 100 portfolio records for the portfolio
  test file.

This specification covers only the listed COBOL, JCL, and test-data-specification
sources. The referenced `PORTFLIO`, `TRNREC`, `RTNCODE`, and `ERRHAND` copybook
bodies were not among the permitted sources, so their complete layouts and
runtime behavior cannot be asserted here.

## 2. Business rules

Rules below are extracted from executable paragraphs where present. A rule
derived from `documentation/operations/test-data-specs.md` or the attached
modernization plan is explicitly marked **INFERRED FROM DOCS**.

1. `TSTGEN00` starts by opening the configuration input, portfolio output,
   transaction output, and random-seed input, then initializes randomness and
   counters (`src/programs/test/TSTGEN00.cbl:109-118`).
2. A non-`00` configuration-file status reports `ERROR OPENING CONFIG FILE`
   through the common error handler (`src/programs/test/TSTGEN00.cbl:120-126`).
3. A non-`00` portfolio-output status reports `ERROR OPENING PORTFOLIO OUTPUT`
   through the common error handler (`src/programs/test/TSTGEN00.cbl:128-133`).
4. A non-`00` transaction-output status reports `ERROR OPENING TRANSACTION
   OUTPUT` through the common error handler (`src/programs/test/TSTGEN00.cbl:135-140`).
5. A non-`00` random-seed status reports `ERROR OPENING RANDOM SEED` through the
   common error handler (`src/programs/test/TSTGEN00.cbl:142-147`).
6. The first random-seed record is copied into the working random seed; no
   end-of-file or read-status branch is implemented (`src/programs/test/TSTGEN00.cbl:149-151`).
7. `TSTGEN00` initializes both counters before processing configuration records
   (`src/programs/test/TSTGEN00.cbl:153-154`).
8. Configuration records are read sequentially until end of file; end of file
   sets the `END-OF-CONFIG` Level-88 condition and stops processing
   (`src/programs/test/TSTGEN00.cbl:156-164`).
9. Configuration type `PORTFOLIO` dispatches to portfolio generation
   (`src/programs/test/TSTGEN00.cbl:166-170`).
10. Configuration type `TRANSACTN` dispatches to transaction generation
    (`src/programs/test/TSTGEN00.cbl:166-172`).
11. Configuration type `ERROR` dispatches to error-data generation
    (`src/programs/test/TSTGEN00.cbl:166-174`).
12. Configuration type `VOLUME` dispatches to volume-data generation
    (`src/programs/test/TSTGEN00.cbl:166-175`).
13. Any other configuration type reports `INVALID TEST TYPE` through the common
    error handler (`src/programs/test/TSTGEN00.cbl:176-180`).
14. Portfolio generation intends to iterate `WS-RECORDS-WRITTEN` from 1 by 1
    until it is greater than `CFG-VOLUME`, generating and writing one record per
    iteration (`src/programs/test/TSTGEN00.cbl:182-187`). The called
    `2210-GEN-PORT-DATA` and `2220-WRITE-PORT-RECORD` paragraphs are absent, so
    field-generation and write-status behavior are not implemented.
15. Transaction generation intends to iterate from 1 by 1 until the counter is
    greater than `CFG-VOLUME`, generating and writing one record per iteration
    (`src/programs/test/TSTGEN00.cbl:189-194`). The called
    `2310-GEN-TRAN-DATA` and `2320-WRITE-TRAN-RECORD` paragraphs are absent.
16. Error generation intends to call data-error generation followed by
    process-error generation (`src/programs/test/TSTGEN00.cbl:196-198`), but
    both called paragraphs are absent.
17. Volume generation intends to call large-portfolio and large-transaction
    generation (`src/programs/test/TSTGEN00.cbl:200-202`), but both called
    paragraphs are absent.
18. Cleanup closes all four `TSTGEN00` files (`src/programs/test/TSTGEN00.cbl:204-208`).
19. `TSTGEN00` increments the error counter and displays the error message for
    every handled error (`src/programs/test/TSTGEN00.cbl:210-212`).
20. When the error counter exceeds 100, `TSTGEN00` sets return code 12 and
    executes `GOBACK` (`src/programs/test/TSTGEN00.cbl:213-216`). No explicit
    return-code assignment exists for fewer than 101 errors.
21. `TSTVAL00` opens test cases, expected results, and actual results for input,
    and the test report for output (`src/programs/test/TSTVAL00.cbl:139-166`).
22. A non-`00` test-case, expected-results, actual-results, or report-file
    status reports the corresponding error message through the common handler
    (`src/programs/test/TSTVAL00.cbl:139-166`).
23. Validation writes a 132-character separator and a centered
    `TEST VALIDATION REPORT` header before processing (`src/programs/test/TSTVAL00.cbl:168-170`).
24. Metrics are initialized and the start time is accepted from `TIME`
    (`src/programs/test/TSTVAL00.cbl:172-174`).
25. Test cases are read sequentially until end of file; end of file sets
    `END-OF-TESTS` (`src/programs/test/TSTVAL00.cbl:176-185`).
26. Test type `FUNCTIONAL` dispatches to functional execution, `INTEGRATE` to
    integration execution, `PERFORM` to performance execution, and `ERROR` to
    error execution (`src/programs/test/TSTVAL00.cbl:187-196`).
27. Any other test type reports `INVALID TEST TYPE` through the common handler
    (`src/programs/test/TSTVAL00.cbl:187-201`).
28. After dispatch, validation intends to validate results, update metrics, and
    write test detail (`src/programs/test/TSTVAL00.cbl:201-204`). Paragraphs
    `2200` through `2800` are absent, so comparison semantics, pass/fail
    transitions, counters, and detail formatting are not implemented.
29. Summary elapsed time is computed exactly as
    `WS-END-TIME - WS-START-TIME` after accepting the end time from `TIME`
    (`src/programs/test/TSTVAL00.cbl:206-209`). No midnight rollover handling
    is present.
30. Summary success rate is computed exactly as
    `(WS-TESTS-PASSED / WS-TOTAL-TESTS) * 100` and moved to `PIC ZZ9.99`
    (`src/programs/test/TSTVAL00.cbl:209-214`). No `ROUNDED` phrase is coded;
    any excess fractional precision is therefore subject to the receiving
    field's normal COBOL truncation. Division by zero is not guarded.
31. `TSTVAL00` closes all four files after summary writing
    (`src/programs/test/TSTVAL00.cbl:216-220`).
32. Any `TSTVAL00` handled error displays the message, sets return code 12,
    and immediately executes `GOBACK` (`src/programs/test/TSTVAL00.cbl:222-225`).
33. `PORTTEST` initializes the current date as `YYYYMMDD` before opening its
    output file (`src/programs/portfolio/PORTTEST.cbl:54-56`).
34. A non-`00` `TESTFILE` open status displays the status, performs termination,
    and returns (`src/programs/portfolio/PORTTEST.cbl:57-63`).
35. `PORTTEST` generates records until `WS-RECORD-COUNT` is at least
    `WS-MAX-RECORDS`, whose coded value is 100 (`src/programs/portfolio/PORTTEST.cbl:45-51`,
    `src/programs/portfolio/PORTTEST.cbl:30-34`).
36. Each generated portfolio record is initialized, then key, client,
    portfolio, and financial information are generated in that order
    (`src/programs/portfolio/PORTTEST.cbl:65-72`).
37. A successful write increments the record counter; a failed write displays
    the file status and does not increment the counter
    (`src/programs/portfolio/PORTTEST.cbl:73-80`).
38. A generated portfolio ID is the fixed-size string concatenation
    `'PORT'` followed by the current record count, delimited by size
    (`src/programs/portfolio/PORTTEST.cbl:82-86`).
39. The portfolio account number is computed as
    `WS-RECORD-COUNT + 1000000000` (`src/programs/portfolio/PORTTEST.cbl:87-89`).
40. The type subscript is assigned from `FUNCTION RANDOM(WS-RECORD-COUNT)`, and
    the selected one-character slice of `WS-CLIENT-TYPES` is copied to the
    client-type field (`src/programs/portfolio/PORTTEST.cbl:87-89`,
    `src/programs/portfolio/PORTTEST.cbl:91-97`).
41. Both portfolio creation date and last-maintenance date are set to the
    accepted current date (`src/programs/portfolio/PORTTEST.cbl:99-102`).
42. The status subscript is computed as `FUNCTION RANDOM * 3 + 1`, then the
    selected one-character slice of `WS-STATUS-TYPES` is copied to portfolio
    status (`src/programs/portfolio/PORTTEST.cbl:103-105`).
43. Portfolio total value is computed as `FUNCTION RANDOM * 1000000`
    (`src/programs/portfolio/PORTTEST.cbl:107-110`).
44. Cash balance is computed exactly as `PORT-TOTAL-VALUE * .10`
    (`src/programs/portfolio/PORTTEST.cbl:111-113`). No `ROUNDED` phrase is
    coded; receiving-field scale governs truncation.
45. Termination closes the test file and displays the generated-record count
    (`src/programs/portfolio/PORTTEST.cbl:115-119`).
46. **INFERRED FROM DOCS:** Portfolio IDs must be `PORT` followed by five
    digits, unique, and non-empty (`documentation/operations/test-data-specs.md:85-90`).
47. **INFERRED FROM DOCS:** Portfolio names must contain 1–50 characters,
    use A–Z, 0–9, spaces, or hyphens, and not be all spaces
    (`documentation/operations/test-data-specs.md:92-97`).
48. **INFERRED FROM DOCS:** Portfolio status values are `A` (Active), `I`
    (Inactive), and `C` (Closed) (`documentation/operations/test-data-specs.md:99-105`).
49. **INFERRED FROM DOCS:** Transaction types are `B` (Buy) and `S` (Sell)
    (`documentation/operations/test-data-specs.md:107-112`).
50. **INFERRED FROM DOCS:** Portfolio total value is constrained to
    0.00–9,999,999,999,999.99 and transaction amount to
    0.01–99,999,999,999.99; both must be properly signed
    (`documentation/operations/test-data-specs.md:114-125`).
51. **INFERRED FROM DOCS:** Creation dates must be valid, use `YYYY-MM-DD`, and
    not be future dates; transaction timestamps must be valid, use
    `YYYY-MM-DD-HH.MM.SS.MICSEC`, and not be future timestamps
    (`documentation/operations/test-data-specs.md:127-138`).
52. **INFERRED FROM DOCS:** The parity suite must cover valid and duplicate
    portfolio creation, minimum and maximum values, portfolio updates, valid
    buys and sells, insufficient value, invalid security IDs, VSAM not-found,
    duplicate-key, EOF, file-status, malformed IDs/types/amounts, and missing
    required fields (`documentation/operations/test-data-specs.md:45-81`).

### Return-code semantics

The executable S10 sources explicitly set only return code 12 on critical
handled-error paths. The broader return-code table is **INFERRED FROM DOCS**:
0000 means successful completion, 0004 warning completion, 0008 errors
completed, 0012 critical error/abend, and 0016 environment error
(`documentation/technical/data-dictionary.md:279-287`). The generator and
validator do not implement all five values in the analyzed source.

## 3. Data contracts

### 3.1 Records defined directly by S10 sources

| Record/field | PIC | Semantics | Proposed Java type | DB2/VSAM mapping |
|---|---|---|---|---|
| `CONFIG-RECORD.CFG-TEST-TYPE` | `X(10)` | Generator dispatch value: `PORTFOLIO`, `TRANSACTN`, `ERROR`, or `VOLUME`. | `String` | `TSTCFG` sequential input; no database target. |
| `CONFIG-RECORD.CFG-VOLUME` | `9(6)` | Number of records intended for portfolio/transaction generation. | `Integer` | `TSTCFG`; no database target. |
| `CONFIG-RECORD.CFG-PARAMETERS` | `X(64)` | Additional configuration payload; no consumer is implemented. | `String` | `TSTCFG`; no database target. |
| `SEED-RECORD` | `9(9)` | Random seed input. | `Long` | `RANDSEED` sequential input; no database target. |
| `TEST-CASE-RECORD.TEST-ID` | `X(10)` | Test identifier. | `String` | `TESTCASE` sequential input. |
| `TEST-CASE-RECORD.TEST-TYPE` | `X(10)` | Validator dispatch value. | `String` or enum for the four coded values | `TESTCASE` sequential input. |
| `TEST-CASE-RECORD.TEST-DESCRIPTION` | `X(50)` | Human-readable test description. | `String` | `TESTCASE` sequential input. |
| `TEST-CASE-RECORD.TEST-PARAMETERS` | `X(100)` | Test-specific parameter payload. | `String` | `TESTCASE` sequential input. |
| `EXPECTED-RECORD` | `X(200)` | Expected result line; comparison logic is absent. | `String` | `EXPECTED` sequential input. |
| `ACTUAL-RECORD` | `X(200)` | Actual result line; comparison logic is absent. | `String` | `ACTUAL` sequential input. |
| `REPORT-RECORD` | `X(132)` | Fixed-width validation report line. | `String` | `TESTRPT` sequential output. |
| `WS-TEST-PASSED` | `X`, Level-88 `TEST-PASSED='Y'` | Validator pass flag. | `TestOutcome` enum | Working storage only. |
| `WS-END-OF-TESTS` | `X`, Level-88 `END-OF-TESTS='Y'` | End-of-input flag. | `boolean` | Working storage only. |
| `WS-END-OF-CONFIG` | `X`, Level-88 `END-OF-CONFIG='Y'` | End-of-input flag. | `boolean` | Working storage only. |
| `WS-TOTAL-TESTS`, `WS-TESTS-PASSED`, `WS-TESTS-FAILED` | `9(5)` | Validator counters. | `Integer` | Working storage only. |
| `WS-START-TIME`, `WS-END-TIME`, `WS-ELAPSED-TIME` | `9(8)` | Time values accepted from `TIME`; not dates. | `Integer` or `LocalTime` after a defined adapter | Working storage only. |
| `WS-SUCCESS-RATE` | `ZZ9.99` | Formatted percentage. | `BigDecimal` scale 2 | Report-only working storage. |
| `WS-CFG-STATUS`, `WS-PORT-STATUS`, `WS-TRAN-STATUS`, `WS-RAND-STATUS` | `XX` | Sequential-file status values. | `String` | Working storage only. |
| `WS-ERROR-COUNT` | `9(9)` | Count of handled generator errors. | `Long` | Working storage only. |
| `WS-RANDOM-SEED`, `WS-RANDOM-NUM` | `9(9)` | Seed and random integer work values. | `Long` | Working storage only. |
| `WS-RANDOM-DECIMAL` | `9(9)V99` | Random decimal work value. | `BigDecimal` scale 2 | Working storage only. |
| `WS-PORT-ID` | `X(10)` | Generator working portfolio identifier. | `String` | Working storage only. |
| `WS-PORT-NAME` | `X(30)` | Generator working name. | `String` | Working storage only. |
| `WS-PORT-TYPE` | `X(2)` | Generator working portfolio type. | `String` | Working storage only. |
| `WS-PORT-STATUS` | `X(1)` | Generator working status. | `String` or `PortfolioStatus` enum | Working storage only. |
| `WS-PORT-BALANCE` | `9(15)V99` | Generator working balance. | `BigDecimal` scale 2 | Working storage only. |
| `WS-TRAN-ID` | `X(12)` | Generator working transaction ID. | `String` | Working storage only. |
| `WS-TRAN-TYPE` | `X(2)` | Generator working transaction type. | `String` | Working storage only. |
| `WS-TRAN-AMOUNT` | `9(15)V99` | Generator working amount. | `BigDecimal` scale 2 | Working storage only. |
| `WS-TRAN-DATE` | `X(8)` | Generator working date text. | `String` unless validated as `YYYYMMDD`, then `LocalDate` | Working storage only. |
| `WS-TRAN-STATUS` | `X(1)` | Generator working status. | `String` | Working storage only. |
| `WS-CURRENT-DATE` | `9(8)` | `PORTTEST` date accepted as `YYYYMMDD`. | `LocalDate` | Working storage only. |
| `WS-FILE-STATUS` | `X(2)` | `PORTTEST` output-file status. | `String` | Working storage only. |
| `WS-RECORD-COUNT`, `WS-MAX-RECORDS` | `9(5)` | Generated-record count and coded limit 100. | `Integer` | Working storage only. |
| `WS-CLIENT-TYPES` | `X(3)` | Coded client-type source string `ICT`. | `String` | Working storage only. |
| `WS-STATUS-TYPES` | `X(3)` | Coded status source string `ACS`. | `String` | Working storage only. |
| `WS-NAME-PREFIX` | `X(4)` | Coded name prefix `TEST`. | `String` | Working storage only. |
| `WS-TYPE-SUB`, `WS-STATUS-SUB` | `9(1)` | One-character selection subscripts. | `Integer` | Working storage only. |

### 3.2 Portfolio record contract

The `PORTFLIO` copybook body is unavailable in the permitted source set.
`PORTTEST` therefore cannot be assigned a complete executable record contract.
The following fields are **INFERRED FROM DOCS** from the test-data
specification, and conflict with the source's `PORTTEST` names and widths where
noted:

| Field | PIC | Semantics | Proposed Java type | Mapping |
|---|---|---|---|---|
| `PORT-ID` | `X(10)` | Portfolio key. | `String` | Portfolio master VSAM KSDS in docs; S1 Java `PORTFOLIO_MASTER`. |
| `PORT-NAME` | `X(50)` | Portfolio display name. | `String` | Same as above. |
| `PORT-CREATE-DATE` | `X(10)` | ISO creation date. | `LocalDate` | Same as above. |
| `PORT-STATUS` | `X(01)` | `A`, `I`, or `C`. | `PortfolioStatus` enum | Same as above. |
| `PORT-TOTAL-VALUE` | `S9(13)V99 COMP-3` | Signed portfolio total. | `BigDecimal` scale 2 | Same as above. |
| filler | `X(24)` | Reserved bytes. | Not represented in domain DTO; preserve only in a codec | Same as above. |

The source additionally references `PORT-ACCOUNT-NO`, `PORT-CLIENT-NAME`,
`PORT-CLIENT-TYPE`, `PORT-CREATE-DATE`, `PORT-LAST-MAINT`, `PORT-STATUS`,
`PORT-TOTAL-VALUE`, and `PORT-CASH-BALANCE`
(`src/programs/portfolio/PORTTEST.cbl:82-113`). Their PIC clauses are in the
missing `PORTFLIO` copybook and cannot be verified from the allowed sources.

### 3.3 Transaction record contract

The `TRNREC` copybook body is unavailable. The operations specification gives a
different sequential transaction layout, marked **INFERRED FROM DOCS**:

| Field | PIC | Semantics | Proposed Java type | Mapping |
|---|---|---|---|---|
| `TXN-TIMESTAMP` | `X(26)` | Timestamp in `YYYY-MM-DD-HH.MM.SS.MICSEC`. | `String` plus strict parser, or `LocalDateTime` with microsecond precision | Transaction sequential file; S2 `TRANSACTION_HISTORY`. |
| `TXN-PORT-ID` | `X(10)` | Portfolio identifier. | `String` | Same as above. |
| `TXN-TYPE` | `X(01)` | `B` buy or `S` sell. | `TransactionType` enum | Same as above. |
| `TXN-AMOUNT` | `S9(11)V99 COMP-3` | Signed transaction amount. | `BigDecimal` scale 2 | Same as above. |
| `TXN-SECURITY-ID` | `X(10)` | Security identifier. | `String` | Same as above. |
| filler | `X(20)` | Reserved bytes. | Codec-only padding | Same as above. |

### 3.4 Referenced copybooks and external contracts

`TSTGEN00` references `RTNCODE`, `ERRHAND`, `PORTFLIO`, and `TRNREC`; `TSTVAL00`
references `RTNCODE` and `ERRHAND`; `PORTTEST` references `PORTFLIO`
(`src/programs/test/TSTGEN00.cbl:51-67`,
`src/programs/test/TSTVAL00.cbl:66-68`, and
`src/programs/portfolio/PORTTEST.cbl:24-28`). Their fields, Level-88 values,
and DB2/VSAM mappings are not reproduced because those copybook files were
outside the allowed source list. The Java implementation must obtain those
layouts from the canonical copybook-to-domain mapping in the S1/S2/S3
specifications before implementing binary or fixed-width codecs.

## 4. Interfaces

### 4.1 Inbound interfaces

| Legacy interface | Evidence | Java target and interaction |
|---|---|---|
| `TSTGEN00` job step | `TSTGEN.jcl:1-20` | S10 generator batch step; library call to fixture-generation services, not a REST endpoint. |
| `TSTVAL00` job step | `TSTVAL.jcl:1-17` | S10 validator batch step; library call to comparison/report services. |
| `PORTTEST` job step | `PORTTEST.jcl:1-18` | S10 portfolio fixture batch step; library call to portfolio-fixture generator. |
| `TSTCFG`/`TEST.CONFIG.FILE` | `TSTGEN00.cbl:20-24`, `TSTGEN.jcl:8` | `TstgenConfigReader` reading a fixed-width sequential stream. |
| `RANDSEED`/`TEST.RANDOM.SEED` | `TSTGEN00.cbl:34-36`, `TSTGEN.jcl:17` | Seed provider reading one fixed-width numeric record. |
| `TESTCASE`/`TEST.CASE.FILE` | `TSTVAL00.cbl:20-24`, `TSTVAL.jcl:8` | `TestCaseReader` reading fixed-width test cases. |
| `EXPECTED` and `ACTUAL` | `TSTVAL00.cbl:26-34`, `TSTVAL.jcl:9-10` | Golden-file reader and actual-result reader. |
| `TESTFILE`/`PORTFOLIO.TEST.FILE` | `PORTTEST.cbl:16-20`, `PORTTEST.jcl:12-15` | Portfolio fixture output codec. |

### 4.2 Outbound interfaces

| Legacy interface | Evidence | Java mapping |
|---|---|---|
| `PORTOUT` and `TRANOUT` | `TSTGEN00.cbl:26-32`, `TSTGEN.jcl:9-16` | Fixed-width fixture files consumed by S1/S2 parity tests; file output. |
| `TESTRPT` | `TSTVAL00.cbl:36-38`, `TSTVAL.jcl:11-14` | Fixed-width report artifact plus structured test summary; file output. |
| `SYSOUT`/console messages | `TSTGEN00.cbl:210-216`, `TSTVAL00.cbl:222-225`, `PORTTEST.cbl:57-60`, `PORTTEST.cbl:77-79`, `PORTTEST.cbl:115-119` | Structured logger and batch exit status. |
| `RETURN-CODE` | `TSTGEN00.cbl:213-216`, `TSTVAL00.cbl:222-225` | `ReturnCode`/process exit status from the S8 platform-common library. |
| Portfolio and transaction domain services | Attached modernization plan §2, S10 row | S10 invokes S1/S2/S3 test APIs or test fixtures; use REST only for black-box contract tests, and library calls for deterministic codec/golden-file tests. |
| Testcontainers PostgreSQL | Attached modernization plan §2, S10 row | Integration-test dependency for the modernized persistence path; database interaction, not a legacy interface. |

No `CALL`, `EXEC CICS LINK`, `EXEC CICS XCTL`, or SQL statement appears in
the three listed COBOL programs. The JCL supplies only `EXEC PGM` steps and DD
allocations. The architecture document's generic statement that test programs
read DB2 is a documentation mismatch for these source files.

### 4.3 JCL contracts

`TSTGEN` allocates `STEPLIB`, `TSTCFG`, `PORTOUT`, `TRANOUT`, `RANDSEED`,
`SYSOUT`, `SYSUDUMP`, and `SYSPRINT` (`src/jcl/test/TSTGEN.jcl:6-20`).
`TSTVAL` allocates `STEPLIB`, `TESTCASE`, `EXPECTED`, `ACTUAL`, `TESTRPT`,
`SYSOUT`, `SYSUDUMP`, and `SYSPRINT` (`src/jcl/test/TSTVAL.jcl:6-17`).
`PORTTEST` allocates `STEPLIB`, `TESTFILE`, `SYSOUT`, `SYSPRINT`, and
`SYSUDUMP` (`src/jcl/portfolio/PORTTEST.jcl:10-18`). The Java replacement
should preserve the logical DD-to-stream mapping while using application
configuration and temporary test artifacts rather than z/OS datasets.

## 5. Discrepancies

1. `TSTGEN00` advertises portfolio, transaction, error, and volume generation,
   but all implementation paragraphs that actually populate or write those
   records are absent (`src/programs/test/TSTGEN00.cbl:182-202`).
2. `TSTVAL00` dispatches to four test types and then calls validation, metric,
   and detail paragraphs, but paragraphs `2200`–`2800` are absent
   (`src/programs/test/TSTVAL00.cbl:187-204`).
3. The system architecture says the codebase contains all necessary copybooks
   and data definitions, but the S10 source references copybooks whose bodies
   are outside this analysis and are required to determine actual record
   layouts (`documentation/technical/system-architecture.md:8-18`).
4. The architecture lists `TSTGEN00` and `TSTVAL00` as test components and
   describes their intended capabilities, while the source implements only
   orchestration and file setup for those capabilities
   (`documentation/technical/system-architecture.md:155-168`).
5. The architecture's test dependency table says `TSTVAL00` has DB2 read
   access, but the listed `TSTVAL00` source contains no SQL, DB2 call, or DB2
   file (`documentation/technical/system-architecture.md:516-522`;
   `src/programs/test/TSTVAL00.cbl:1-225`).
6. The operations specification describes a `PORTFOLIO-RECORD` with
   `PORT-NAME X(50)`, ISO date text, signed packed total value, and filler,
   while `PORTTEST` generates additional fields (`PORT-ACCOUNT-NO`,
   `PORT-CLIENT-NAME`, `PORT-CLIENT-TYPE`, `PORT-LAST-MAINT`,
   `PORT-CASH-BALANCE`) whose definitions are hidden in `PORTFLIO`
   (`documentation/operations/test-data-specs.md:5-15`;
   `src/programs/portfolio/PORTTEST.cbl:24-39`).
7. The documentation's sample portfolio status includes `I` and the validation
   criteria define `A`, `I`, and `C`, but `PORTTEST` uses `WS-STATUS-TYPES`
   value `ACS`, so it can produce `S`, which is undocumented
   (`src/programs/portfolio/PORTTEST.cbl:36-39,103-105`;
   `documentation/operations/test-data-specs.md:99-105`).
8. The documentation's portfolio sample uses ISO dates and a 50-character
   name, while the source's working fields are `X(8)` date text and `X(30)`
   name text (`documentation/operations/test-data-specs.md:8-15`;
   `src/programs/test/TSTGEN00.cbl:94-100`).
9. The documentation's transaction record uses a 26-character timestamp,
   one-character type, and `TXN-PORT-ID`, while `TSTGEN00` working storage uses
   a 12-character ID, two-character type, and `TRAN`-prefixed names
   (`documentation/operations/test-data-specs.md:27-35`;
   `src/programs/test/TSTGEN00.cbl:101-106`).
10. The operations specification documents VSAM KSDS/ESDS records and business
    validation rules, but none of the listed S10 programs opens or accesses a
    VSAM organization; all listed files are sequential
    (`documentation/operations/test-data-specs.md:5-35`;
    `src/programs/test/TSTGEN00.cbl:20-36`;
    `src/programs/test/TSTVAL00.cbl:20-38`;
    `src/programs/portfolio/PORTTEST.cbl:16-20`).
11. `TSTGEN.jcl` allocates `LRECL=100` for both output datasets, but the
    portfolio and transaction layouts in the documentation are not both
    100-byte records, and the source does not define the copied layouts
    (`src/jcl/test/TSTGEN.jcl:9-16`;
    `documentation/operations/test-data-specs.md:5-35`).
12. `PORTTEST.jcl` allocates `LRECL=200`, while the documented portfolio
    example is 100 bytes by its field totals and the source's copied record
    length is unavailable (`src/jcl/portfolio/PORTTEST.jcl:12-15`;
    `documentation/operations/test-data-specs.md:5-15`).
13. The documentation defines return codes 0000, 0004, 0008, 0012, and 0016,
    but the analyzed programs explicitly set only 12 on selected error paths
    (`documentation/technical/data-dictionary.md:279-287`;
    `src/programs/test/TSTGEN00.cbl:210-216`;
    `src/programs/test/TSTVAL00.cbl:222-225`).
14. `TSTVAL00` computes success rate without a zero-test guard; the
    documentation does not define an empty-suite result
    (`src/programs/test/TSTVAL00.cbl:206-214`).
15. The attached modernization plan identifies `TRNVAL00` as missing and
    `POSUPDT` as empty, while the architecture document presents them as core
    batch components. They are not S10 sources, but their absence blocks
    end-to-end parity tests that S10 is expected to support (attached plan
    §1 and §2; `documentation/technical/system-architecture.md:89-99`).
16. `PORTTEST` uses `YOUR.LOADLIB` and placeholder author metadata, whereas
    the other jobs use `TEST.LOAD.LIBRARY` and account-specific job metadata
    (`src/jcl/portfolio/PORTTEST.jcl:3-11`;
    `src/jcl/test/TSTGEN.jcl:1-7`).

## 6. Proposed Java design

The attached plan places S10 in a test-harness/parity-suite module using
Java Faker or Instancio, golden-file equivalence tests, and Testcontainers
PostgreSQL. A concrete package layout is:

```text
com.cognition.portfolio.s10
├── config          # fixture profiles, seed, paths, strictness
├── contract       # fixed-width records and codec metadata
├── generator       # portfolio, transaction, error, volume generators
├── validator       # test dispatch, expected/actual comparison, metrics
├── report          # 132-column legacy report renderer and JSON summary
├── adapter         # S1/S2/S3 REST clients and in-process test adapters
├── batch           # generator and validator Spring Batch jobs/steps
└── support         # ReturnCode mapping, logging, clock, deterministic random
```

Recommended Spring components:

- `TstgenJobConfiguration` and `TstvalJobConfiguration` define chunk-oriented
  steps for configuration/test-case streams and explicit exit-status mapping.
- `FixtureGenerationService` owns deterministic seeded generation and exposes
  separate portfolio, transaction, error, and volume strategies. Missing
  COBOL paragraphs remain explicit unsupported strategies until a product
  decision supplies behavior.
- `FixedWidthCodec` preserves field widths, padding, sign/scale, and the
  documented record variants; it must not silently choose between the
  conflicting `PORTFLIO` and operations layouts.
- `GoldenResultValidator` compares expected and actual records byte-for-byte
  by default, with an explicit field-aware mode only where a contract defines
  normalization.
- `LegacyReportRenderer` emits the two headers, detail lines, and summary line;
  it must define empty-suite handling before production use because COBOL does
  not.
- `S1PortfolioClient`, `S2TransactionClient`, and `S3PositionClient` provide
  REST contract-test adapters for black-box parity. Pure codec and rule tests
  use in-process library calls.
- `TestcontainersConfiguration` supplies PostgreSQL for integration parity,
  with seeded fixtures isolated per test class and a fixed `Clock`.
- `ReturnCodeExitStatusListener` maps explicit legacy RC values to Spring Batch
  exit statuses and process exit codes without claiming undocumented mappings.

No Java code is specified or added here. The initial implementation should
first resolve the record-layout conflicts and missing COBOL paragraphs, then
add characterization tests for every executable rule in section 2.

## 7. Open questions

1. What are the authoritative `PORTFLIO` and `TRNREC` copybook layouts,
   including PIC clauses, padding, signs, and Level-88 values?
2. Should the Java harness preserve the documented VSAM record contracts or
   the fields actually referenced by `PORTTEST`?
3. Is `S` a valid portfolio status, or is `WS-STATUS-TYPES` in `PORTTEST`
   incorrect and intended to generate `A`, `I`, or `C`?
4. What exact algorithms should fill the missing TSTGEN00 paragraphs for
   portfolio, transaction, error, and volume data?
5. What exact comparison, pass/fail, metrics-update, and detail-write behavior
   should fill TSTVAL00 paragraphs `2200`–`2800`?
6. What should the validator return for zero test cases, especially for the
   success-rate division?
7. Should an individual failed comparison continue processing subsequent test
   cases, or should it terminate the job?
8. Which return-code values should S10 emit for warnings, ordinary validation
   failures, environment failures, and successful completion?
9. Should generated output be byte-identical fixed-width files, semantic
   records, or both?
10. How should random generation be made reproducible when COBOL
    `FUNCTION RANDOM` behavior differs from the selected Java generator?
11. Should the S10 integration suite call S1–S3 over REST, use in-process
    adapters, or support both modes?
12. Which database should Testcontainers use for parity: PostgreSQL as stated
    in the plan, DB2-compatible infrastructure, or a contract-test substitute?
13. How should missing `TRNVAL00` and empty `POSUPDT` be represented in
    end-to-end tests until S2/S3 behavior is decided?
