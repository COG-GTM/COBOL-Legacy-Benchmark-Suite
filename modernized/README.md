# Position Update Service — Modernized (Java 21 / Spring Boot 3)

A modern, REST-enabled microservice port of the COBOL **batch position-update**
process from the COBOL Legacy Benchmark Suite. It preserves the legacy
calculation semantics (position aggregation, cost-basis maintenance, realized /
unrealized P&L) while replacing VSAM/DB2 storage with Spring Data JPA, the
checkpoint/restart framework with Spring Batch, and CICS online inquiry with a
REST API.

---

## ⚠️ Source-program note (important)

The task referenced `src/programs/batch/CLBP0001.cbl`. **That file does not exist
in this repository**, and the actual position-update program `POSUPDT.cbl`
(process id `POSUPD00`) is an **empty source file** — the COBOL was never filled
in. The data layer and business rules are, however, fully specified by the
copybooks, the DB2 DDL, the documentation, and the one program that *does*
implement portfolio position math (`PORTTRAN.cbl`).

This service therefore reconstructs the position-update logic faithfully from
those authoritative sources:

| Source | What was taken from it |
|---|---|
| `src/copybook/common/POSREC.cpy` | Position master record layout (KSDS `POSMSTR`) |
| `src/copybook/common/TRNREC.cpy` | Transaction input record layout |
| `src/copybook/db2/DBTBLS.cpy` + `src/database/db2/POSHIST.sql` | Position-history table; **cost basis & gain/loss** columns that define P&L output |
| `src/copybook/batch/CKPRST.cpy`, `src/programs/batch/CKPRST.cbl` | Checkpoint/restart mechanics |
| `src/programs/portfolio/PORTTRAN.cbl` | Concrete BUY/SELL/FEE/TRANSFER position math, validation, insufficient-units guard |
| `documentation/technical/{system-architecture,data-dictionary}.md` | POSUPDT responsibilities, 500-record checkpoint frequency, job dependency `TRNVAL00 → POSUPD00 → HISTLD00`, validation rules |

---

## Architecture

```
REST (GET /positions, /positions/{id})        <-  CICS online inquiry (INQONLN)
        │
        ▼
PositionUpdateService  (@Transactional)        <-  POSUPDT unit of work
        │
        ├── PositionCalculator (pure domain)   <-  PORTTRAN position math + P&L
        │
        ├── PositionRepository    (JPA)        <-  VSAM KSDS POSMSTR
        ├── TransactionRepository (JPA)        <-  sequential TRANFILE
        └── PositionHistoryRepository (JPA)    <-  DB2 POSHIST (embedded SQL)

PositionUpdateJob (Spring Batch, chunk=500)    <-  CKPRST checkpoint/restart
```

## Tech stack

- Java 21, Spring Boot 3.3.x (Maven)
- Spring Data JPA + Hibernate, Spring Batch, Spring Web
- Flyway migrations; H2 (dev/default) and PostgreSQL (prod) profiles
- springdoc-openapi (Swagger UI), Lombok

---

## COBOL → Java mapping

### Programs / divisions

| COBOL construct | Java equivalent |
|---|---|
| `POSUPDT` / `POSUPD00` batch program | `PositionUpdateService` + `PositionUpdateJobConfig` |
| `PORTTRAN` `2200-UPDATE-POSITIONS` (`EVALUATE TRN-TYPE`) | `PositionCalculator.apply(...)` (`switch` on `TransactionType`) |
| `2210-PROCESS-BUY` / `2220-PROCESS-SELL` / `2240-PROCESS-FEE` | `processBuy` / `processSell` / `processFee` |
| `2230-PROCESS-TRANSFER` (`'... not implemented'`) | `UnsupportedTransactionException` |
| `9000-ERROR-ROUTINE` (error codes E001–E004) | `GlobalExceptionHandler` (400/409/422) |
| CICS inquiry `INQONLN` / COMMAREA function 'P' | `PositionController` HTTP GET |
| JCL job step executing `POSUPD00` | `POST /positions/jobs/position-update` |

### Data types (copybook PIC → Java / SQL)

| COBOL PIC | Java | SQL |
|---|---|---|
| `PIC X(n)` | `String` (`@Column(length=n)`) | `VARCHAR(n)` |
| `PIC S9(11)V9(4) COMP-3` (quantity, price) | `BigDecimal` scale 4 | `DECIMAL(15,4)` |
| `PIC S9(13)V9(2) COMP-3` (cost, market, amount) | `BigDecimal` scale 2 | `DECIMAL(15,2)` |
| DB2 `DECIMAL(15,3)` (POSHIST qty/price) | `BigDecimal` scale 3 | `DECIMAL(15,3)` |
| DB2 `DATE` / `TIME` / `TIMESTAMP` | `LocalDate` / `LocalTime` / `LocalDateTime` | `DATE` / `TIME` / `TIMESTAMP` |
| `88`-level condition names | `enum` (`TransactionType`, `PositionStatus`) | — |
| `FILLER` | *not mapped* (padding) | — |

> Financial values are **`BigDecimal` only** — never floating point — at the exact
> scales declared by the copybook (see `MoneyScale`). COBOL adds/subtracts of
> equal-scale fields are exact; the single division (average cost) carries extra
> precision and rounds `HALF_UP`.

### Storage / access patterns

| COBOL | Java |
|---|---|
| VSAM KSDS `POSMSTR`, composite key `POS-PORTFOLIO-ID + POS-DATE + POS-INVESTMENT-ID` | `position_master` table; natural key kept as a **unique constraint**, surrogate `id` PK so the REST resource is addressable by one path variable |
| Keyed `READ` | `findByPortfolioIdAndPositionDateAndInvestmentId` / `findById` |
| Sequential browse (`RPTPOS00 2100-READ-POSITIONS`) | `findAll` |
| `REWRITE` / `WRITE` | `save` |
| DB2 `EXEC SQL INSERT INTO POSHIST` | `PositionHistoryRepository.save` |
| DB2 `SELECT ... WHERE`, `SUM(GAIN_LOSS)` | derived queries + `@Query("SELECT SUM(h.gainLoss) ...")` |

### Checkpoint / restart → Spring Batch

| `CKPRST.cpy` | Spring Batch |
|---|---|
| `CK-COMMIT-FREQ` (commit every N) — data dict 8.3 "every 500 updates" | chunk size = **500** (`PositionUpdateJobConfig.CHUNK_SIZE`) |
| `CALL 'CKPTAKE'` / `'CKPCMIT'` | chunk commit; read/write counts persisted to `JobRepository` |
| `CK-LAST-KEY`, `CK-RECORDS-PROC` | `BATCH_STEP_EXECUTION` counters |
| `CALL 'CKPRSTR'` with `CK-MODE-RESTART` | relaunch with same `JobParameters`; resumes after last committed chunk (per-item update is idempotent) |

---

## Business rules preserved

- **BUY** (`PORTTRAN 2210`): `quantity += trn.qty`, `costBasis += trn.amount`;
  market value re-marked to trade price; no realized P&L.
- **SELL** (`PORTTRAN 2220`): rejects if `quantity < trn.qty`
  (`InsufficientPositionException`, rule E004). Cost basis is reduced at
  **weighted-average cost**: `costOfSharesSold = avgCost × qtySold`; realized
  gain/loss = `proceeds − costOfSharesSold` (recorded to `POSHIST.GAIN_LOSS`).
- **FEE** (`PORTTRAN 2240`): `costBasis −= trn.amount`; quantity and market value
  unchanged. *(Faithful to PORTTRAN, which subtracts the fee from total cost.)*
- **TRANSFER**: unimplemented in the COBOL — preserved as an explicit
  `UnsupportedTransactionException` rather than inventing semantics.
- **Validation** (`PORTTRAN 2120/2130`, data dict 5.1): quantity > 0; price and
  amount > 0 (except transfers).
- **Average cost** = `costBasis / quantity` (0 for empty holdings — avoids the
  COBOL S0C7 divide-by-zero).
- **Unrealized P&L** = `marketValue − costBasis`.

---

## Running

```bash
cd modernized
JAVA_HOME=/path/to/jdk-21 mvn spring-boot:run     # H2 in-memory, Flyway-seeded
```

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 console: `http://localhost:8080/h2-console` (JDBC `jdbc:h2:mem:posdb`)

### Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/positions` | List positions (`?portfolioId=` or `?status=A|C|P`) |
| `GET` | `/positions/{id}` | Single position (404 if absent) — includes derived `averageCost`, `unrealizedGainLoss` |
| `POST` | `/positions/jobs/position-update?runDate=YYYYMMDD` | Run/restart the batch job |

### Production profile

`-Dspring.profiles.active=prod` uses PostgreSQL. Credentials come **only** from
environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) — nothing is
hardcoded.

---

## Tests

`mvn test` — 24 tests, all passing:

| Suite | Coverage |
|---|---|
| `PositionCalculatorTest` (11) | BUY/SELL/FEE/TRANSFER, realized & unrealized P&L, average cost, validation, insufficient-units guard, trade aggregation, copybook decimal scales |
| `PositionRepositoryTest` (5) | Flyway seed load, keyed read, finders, CRUD round-trip |
| `PositionUpdateServiceTest` (4) | apply BUY/SELL, new-position creation, DB2 gain/loss aggregate |
| `PositionControllerTest` (3) | `GET /positions`, `GET /positions/{id}` found + 404 |
| `PositionUpdateJobTest` (1) | full chunk-oriented batch run over seeded transactions |
