# COBOL to Java Migration

Migration of the COBOL Investment Portfolio Management System (`src/`) to a Java 17 / Spring Boot
3.3 application (`java/`). Build and run instructions are in [`java/README.md`](../../java/README.md).

## 1. Source inventory

| Area | Path | Count |
| --- | --- | --- |
| Portfolio programs | `src/programs/portfolio` | 8 |
| Batch programs | `src/programs/batch` | 11 |
| Online (CICS) programs | `src/programs/online` | 8 |
| Common subprograms | `src/programs/common` | 6 |
| Utilities | `src/programs/utility` | 3 |
| Test programs | `src/programs/test` | 2 |
| Copybooks | `src/copybook/{common,batch,online,db2}` | 20 |
| JCL | `src/jcl/**` | 17 |
| DB2 DDL / VSAM definitions | `src/database/**` | 6 |
| BMS map / CICS tables | `src/maps`, `src/cics` | 2 |

## 2. Construct mapping

| COBOL construct | Java |
| --- | --- |
| Copybook record layout | POJO in `com.clbs.domain` (or JPA entity in `com.clbs.db2`) |
| `PIC S9(n)V9(m) COMP-3` | `BigDecimal` normalised to the copybook scale on every setter |
| `PIC X(n)` | `String` (JPA `@Column(length = n)` from the DDL/copybook) |
| `88`-levels | constants (`ReturnCode`, `BatchConstants`, `FileStatus`, `ErrorCodes`) |
| Paragraph / `PERFORM` | method, named after the paragraph in its javadoc |
| VSAM KSDS (`READ`/`WRITE`/`REWRITE`/`DELETE`/`STARTBR`) | `store.IndexedFile` with COBOL file statuses |
| QSAM sequential file | `store.SequentialFile` |
| DD-name dataset | `store.DatasetCatalog` |
| DB2 table + `EXEC SQL` | Spring Data JPA repository over H2 |
| CICS `LINK`/`XCTL` + DFHCOMMAREA | Spring bean call + request/response record |
| CICS transaction entry point | REST endpoint in `com.clbs.api` |
| `DISPLAY` / report lines | `common.ProgramResult` display lines and counters |

## 3. Program mapping

### Portfolio

| COBOL | Java |
| --- | --- |
| PORTVALD | `portfolio.PortfolioValidator` |
| PORTMSTR | `portfolio.PortfolioMasterService` |
| PORTADD / PORTUPDT / PORTDEL / PORTREAD | `portfolio.Portfolio{Add,Update,Delete,Read}Program` |
| PORTTRAN | `portfolio.PortfolioTransactionProgram` |
| PORTTEST | superseded by the JUnit suite |

### Batch

| COBOL | Java |
| --- | --- |
| BCHCTL00 | `batch.BatchControlProcessor` |
| PRCSEQ00 | `batch.ProcessSequenceManager` |
| RCVPRC00 | `batch.RecoveryProcessor` |
| HISTLD00 | `batch.HistoryLoadProgram` (writes the `poshist` table) |
| CKPRST | `batch.CheckpointRestart` |
| RPTPOS00 / RPTAUD00 / RPTSTA00 | `batch.{Position,Audit,Statistics}ReportProgram` |
| RTNCDE00 | `common.ReturnCodeHandler` + `common.ReturnCodeArea` |
| RTNANA00 | `batch.ReturnCodeAnalysisProgram` |
| POSUPDT | not migrated — the COBOL program contains no procedural code |

### Online

| COBOL | Java |
| --- | --- |
| INQONLN | `online.OnlineInquiryService` + `api.InquiryController` |
| INQPORT / INQHIST | `online.PortfolioInquiryService` / `online.HistoryInquiryService` |
| SECMGR | `online.SecurityManager` over the `authorization` table |
| ERRHNDL | `online.OnlineErrorHandler` over the `errlog` table |
| DB2ONLN / DB2RECV / CURSMGR | replaced by infrastructure: HikariCP connection pooling, Spring transaction management and JPA query paging respectively; the COBOL programs manage a hand-written connection pool, `EXEC SQL` recovery and cursor lifecycle, none of which survive the move to JPA |
| DB2CONN / DB2CMT / DB2STAT / DB2ERR | same — Spring/JPA transaction and error handling |
| AUDPROC / ERRPROC | `common.AuditProcessor` / `common.ErrorProcessor` |

### Utilities and test support

| COBOL | Java |
| --- | --- |
| UTLMNT00 | `utility.FileMaintenanceUtility` |
| UTLMON00 | `utility.SystemMonitorUtility` |
| UTLVAL00 | `utility.DataValidationUtility` |
| TSTGEN00 | `testdata.TestDataGenerator` |
| TSTVAL00 | `testdata.TestValidationProgram` |

JCL job steps are not migrated as scripts; each step's program is reachable through the REST
adapters listed in `java/README.md`.

## 4. Numeric semantics

Every COMP-3 field keeps the scale declared in its copybook (`PORTFLIO.cpy` values `S9(13)V99` →
scale 2, units `S9(11)V9(4)` → scale 4) and is stored as `BigDecimal`. Setters normalise the scale
with `HALF_UP`, so a value posted as `1234.5` is read back as `1234.50`. Divisions that COBOL writes
as `COMPUTE ... ROUNDED` are done at scale 6 and rounded to the reported scale, and every division
is guarded against the zero denominators the COBOL source does not check (`RPTPOS00` change
percentage, `RPTSTA00` and `TSTVAL00` success rates).

## 5. Data stores

* `IndexedFile` reproduces KSDS behaviour, including the file statuses `00`, `10`, `22`, `23`, `30`
  from `ERRHAND.cpy`, browse (`START`/`READNEXT`) and duplicate/not-found handling.
* `DatasetCatalog` owns one dataset per DD name (portfolio, position, transaction, transaction
  history, batch control, process sequence, audit).
* DB2 tables from `src/database/db2` and `DBTBLS.cpy` become JPA entities: `poshist`,
  `errlog`, `rtncode_log`, `authorization`. Column lengths follow the DDL.

## 6. Known COBOL inconsistencies preserved

These are defects or gaps in the COBOL source; the Java code follows the source rather than
inventing behaviour, and each is flagged in the javadoc of the class concerned.

* **PORTTRAN transfers** — `2230-PROCESS-TRANSFER` displays *"Transfer processing not implemented"*.
  `PortfolioTransactionProgram.apply` returns the same error.
* **PORTTRAN main loop** — `2000-PROCESS-TRANSACTIONS` validates records but never performs
  `2200-UPDATE-POSITIONS`, so `run(...)` validates and counts only; applying a transaction is the
  separate `apply(...)` call.
* **Portfolio status values** — `PORTFLIO.cpy` defines `A`/`C`/`S`; `PORTMSTR` validates `A`/`I`/`C`.
  The master service follows `PORTMSTR`.
* **Transaction type codes** — the programs use two-character codes (`BU`, `SL`, `TR`, `FE`) while
  `documentation/operations/test-data-specs.md` uses one character. The programs win.
* **INQPORT** references an account field that `POSREC.cpy` does not define; the inquiry matches the
  supplied value against the position's portfolio ID. This is an adaptation, not parity.
* **Undefined paragraphs** — `RPTPOS00` (2210/2220/2310/2320/2330), `CKPRST` (all four operations),
  `UTLMNT00`, `UTLVAL00`, `TSTGEN00` and `TSTVAL00` leaf paragraphs are `PERFORM`ed but never
  defined. The Java code keeps those steps empty, or derives them from the documented layouts and
  says so, rather than inventing report content or validation rules.
* **POSUPDT** contains only comments.
* **HISTLD00** checkpoints into `BCT-RECORDS-READ`/`BCT-RECORDS-WRITTEN`, which `BCHCTL.cpy` does not
  define; the counters are carried on `BatchControlRecord`.

## 7. Validation

* `mvn -B test` — 54 tests: portfolio validation and CRUD, buy/sell/fee/transfer behaviour,
  BigDecimal scale, batch control and prerequisites, return-code classification and logging,
  position report arithmetic, security manager, test-data generation and validation, plus a
  `@SpringBootTest` that boots the application and exercises the REST adapters end to end.
* `mvn -B package` then `java -jar target/clbs-java-1.0.0.jar` — the application starts on port 8080
  with the sample dataset; see the smoke output in the pull request.

**No COBOL golden outputs were produced.** Compiling the suite with GNU COBOL 3.1.2 in fixed format
fails: missing copybook fields, undefined data items, incomplete programs, and CICS/DB2 preprocessor
dependencies that are not satisfiable outside z/OS. The COBOL source was not modified to force a
compile, so parity was established by reading the source, not by comparing runtime output.
