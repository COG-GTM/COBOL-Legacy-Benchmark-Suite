# COBOL → Java Migration Guide

This directory contains the Java migration of the Enterprise COBOL Investment
Portfolio Management System. The original COBOL source remains under `src/`
for reference. The first vertical slice migrated end-to-end is **HISTLD00**
(Position History DB2 Load), which establishes the conventions below for all
subsequent programs.

## Project layout

```
java/
├── pom.xml                          Maven, Java 17, Spring Boot 3 (Batch + Data JPA + H2)
└── src/main/java/com/portfolio/
    ├── model/copybook/              Step 1: copybook record layouts → POJOs
    ├── domain/                      Step 2: DB2 DDL + VSAM KSDS → JPA entities
    ├── repository/                  Spring Data JPA repositories
    ├── common/                      Step 3: DB2CONN/DB2CMT/ERRPROC migrations
    └── batch/                       Step 4: HISTLD00 Spring Batch job
```

## Mapping conventions

### Copybook → class (`model/copybook/`)

Each `01` record layout in `src/copybook/` becomes one Java class with the
same field names (COBOL-CASE → camelCase) and the original PIC clause
preserved in Javadoc. Level-88 condition names and constant copybooks
(BCHCON, COMMON) become `public static final` constants. `OCCURS n TIMES`
tables become `List<NestedType>`.

| COBOL | Java |
|---|---|
| `HISTREC` | `HistoryRecord` |
| `BCHCTL` | `BatchControlRecord` |
| `BCHCON` | `BatchControlConstants` |
| `CKPRST` | `CheckpointControl` |
| `PRCSEQ` | `ProcessSequenceRecord` |
| `TRNREC` | `TransactionRecord` |
| `POSREC` | `PositionRecord` |
| `PORTFLIO` | `PortfolioRecord` |
| `AUDITLOG` | `AuditRecord` |
| `RTNCODE`/`RETHND` | `ReturnCodeArea` |
| `ERRHAND` | `ErrorMessage` |
| `PORTVAL` | `PortfolioValidation` |
| `SQLCA` | `SqlStatusCodes` (constants only — see below) |
| `DBTBLS` (host variables) | JPA entities in `domain/` |

### PIC → Java type

| COBOL PIC | Java type |
|---|---|
| `PIC X(n)` | `String` (length documented / `@Column(length = n)`) |
| `PIC 9(n)` / `PIC S9(n)` display | `int` / `long` (`String` if it encodes a date/time) |
| `PIC S9(4) COMP` | `int` |
| `PIC S9(9) COMP` | `long` (counters) or `int` (codes) |
| `PIC S9(n)V9(m) COMP-3` (packed decimal, financial) | `BigDecimal` — always, never `double` |
| Dates `PIC X(8)`/`PIC X(10)` (YYYYMMDD / ISO) | `LocalDate` in entities, `String` in raw copybook models |
| Times `PIC X(6)`/`PIC X(8)` | `LocalTime` in entities |
| Timestamps `PIC X(26)` | `LocalDateTime` in entities |
| Level-88 values | `static final` constants |

### VSAM KSDS → relational table (`domain/`)

Each VSAM indexed file becomes a table named `VSAM_<cluster>` whose composite
primary key (`@EmbeddedId`) is exactly the COBOL `RECORD KEY`:

| VSAM file | Entity | Key (COBOL RECORD KEY) |
|---|---|---|
| TRANHIST | `TransactionHistoryFileRecord` | trans date + time + portfolio + sequence (`TH-KEY`) |
| BCHCTL | `BatchControl` | job name + process date + sequence (`BCT-KEY`) |

DB2 tables from `src/database/db2/` map 1:1 to entities: `POSHIST` →
`PositionHistory`, `ERRLOG` → `ErrorLog`, `RTNCODES` → `ReturnCodeLog`,
`PORTFOLIO_MASTER` → `PortfolioMaster`, `INVESTMENT_POSITIONS` →
`InvestmentPosition`, `TRANSACTION_HISTORY` → `TransactionHistory`.
`DECIMAL(p,s)` → `BigDecimal` with matching `precision`/`scale`.

### EXEC SQL → JPA / Spring Data

- Singleton `SELECT`/`INSERT`/`UPDATE` → Spring Data repository methods.
- Cursors → paged reads (`RepositoryItemReader`) or streaming queries.
- `SQLCODE` checks → exceptions: non-zero SQLCODE branches become
  `SqlProcessingException` (or Spring's `DataAccessException`); code-specific
  behavior is preserved explicitly (e.g. `-803` duplicate → existence check).
- Two-character `FILE STATUS` checks → `FileProcessingException`, preserving
  the original status value (e.g. `'23'` record not found).

### Common subprograms → shared infrastructure (`common/`)

| COBOL | Java |
|---|---|
| `DB2CONN` (connect/disconnect/status, retry) | `DataSourceConfig` — Spring Boot pooled `DataSource` (HikariCP) |
| `DB2CMT` (commit/rollback/savepoint, frequency) | `TransactionHelper` + Spring transactions; in batch jobs, chunk boundaries |
| `ERRPROC` (format + write ERRLOG, return severity) | `ErrorHandlingService.logError(...)` → `ERRLOG` table, REQUIRES_NEW |

### JCL → Spring Batch

| JCL / COBOL batch concept | Spring Batch |
|---|---|
| Job step running a program | `Job` + `Step` bean (`HistoryLoadJobConfig`) |
| Sequential file read loop | `ItemReader` |
| Record validation / field moves | `ItemProcessor` |
| DB2 INSERT + commit threshold | `ItemWriter` + chunk size (HISTLD00: 1000) |
| Checkpoint REWRITE of BCHCTL | `ChunkListener.afterChunk` → `BatchControlService` |
| `MOVE ... TO RETURN-CODE` | `ExitCodeGenerator` (JVM exit code) |
| Abort condition (`WS-ERROR-COUNT > 100`) | exception from processor fails the job |
| JCL parameters (dates etc.) | `JobParameters` |

### CICS → REST (deferred)

The online layer (`src/programs/online/`, `src/maps/`) is **not** migrated in
this pass. BMS screens and CICS transactions do not translate mechanically —
they require a UI/API redesign (REST controllers + a separate frontend,
pseudo-conversational state → stateless requests, EIBAID keys → HTTP verbs).
Flagged as a separate redesign effort. The online copybooks are already
modeled in `model/copybook/` so the future REST layer can reuse them.

## Reference slice: HISTLD00

`src/programs/batch/HISTLD00.cbl` → `com.portfolio.batch`:

- Reads `VSAM_TRANHIST` in RECORD KEY order (`RepositoryItemReader`).
- Validates and maps TH-* → PH-* fields (`HistoryItemProcessor`).
- Inserts into `POSHIST`, skipping duplicates like SQLCODE `-803`
  (`HistoryItemWriter`).
- Commits every 1000 records via the chunk size (WS-COMMIT-THRESHOLD).
- Updates the batch control record after each commit and at job end
  (`BatchControlService`).
- Counts errors (`HistoryLoadStats`); aborts when the count exceeds 100 and
  returns the error count as the process exit code (`HistoryLoadJobRunner`).

## Building and running

```bash
cd java
mvn test                      # runs the HISTLD00 job tests against embedded H2
mvn spring-boot:run -Dspring-boot.run.profiles=histld00 \
    -Dspring-boot.run.arguments=20240320   # sample run with seeded data
```
