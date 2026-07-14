# COBOL → Java Migration Plan

Version: 1.0
Status: Proposed
Scope: Full migration of the COBOL Legacy Benchmark Suite (CLBS) Investment Portfolio
Management System to a Java stack (Spring Boot, Spring Batch, Spring Data JPA).

> This document is a **plan only**. It does not modify any COBOL source. Every task
> references concrete repository paths so the plan is traceable against the existing
> system. Program dependencies and DB2/VSAM access are taken from
> [`documentation/technical/system-architecture.md`](./system-architecture.md) §5.1.

---

## 0. Target Architecture and Conventions

### 0.1 Target stack

| Concern | COBOL / z/OS today | Java target |
| --- | --- | --- |
| Language / runtime | Enterprise COBOL for z/OS | Java 21 (LTS) |
| Application framework | CICS + JCL | Spring Boot 3.x |
| Batch | JCL job steps + custom checkpoint/restart | Spring Batch |
| Persistence | DB2 for z/OS + VSAM KSDS | Spring Data JPA / Hibernate over an RDBMS |
| Build | z/OS bind + link-edit | Maven or Gradle multi-module |
| Online (3270/BMS) | CICS transactions + BMS maps | Spring MVC REST + optional web UI |
| Transactions | DB2 plan `PORTPLAN` (ISOLATION CS) | Spring `@Transactional` |
| Error/audit | `ERRPROC`/`AUDPROC` + `ERRLOG`/`POSHIST` tables | SLF4J/Logback + JPA audit entities |

### 0.2 RDBMS selection

The current schema is DB2 for z/OS (see `src/database/db2/*.sql`). Recommended target,
in priority order:

1. **PostgreSQL 16** — primary recommendation. Rich `NUMERIC(p,s)` semantics that map
   cleanly to COBOL packed-decimal, native range partitioning (needed for `POSHIST`,
   see `src/database/db2/POSHIST.sql` which partitions by `TRANS_DATE`), stored-procedure
   support for `ERRLOG_CLEANUP`, and strong OSS tooling.
2. **IBM Db2 LUW** — lowest-risk if the organization wants to preserve DB2 SQL dialect
   and DDL almost verbatim; simplifies Phase 1 translation but adds licensing.
3. **H2 (PostgreSQL compatibility mode)** — for the golden-master harness and CI only,
   not production.

All entity/DDL examples below assume PostgreSQL; the mapping conventions are RDBMS-neutral.

### 0.3 Data type mapping conventions (COBOL PIC / COMP-3 → Java / SQL)

These conventions apply to every entity and DTO produced in Phases 1–7. Sources:
`src/copybook/common/*.cpy`, `src/copybook/batch/*.cpy`, `src/database/db2/*.sql`.

| COBOL declaration | Meaning | SQL type | Java type | Notes |
| --- | --- | --- | --- | --- |
| `PIC X(n)` | Fixed-length char/alphanumeric | `CHAR(n)` / `VARCHAR(n)` | `String` | Right-pad on write, `trim()` on read. Preserve length for fixed-width record round-trips. |
| `PIC 9(n)` | Unsigned zoned decimal | `NUMERIC(n)` / `INTEGER` | `int` / `long` / `BigDecimal` | Choose `int`/`long` only for pure counters; use `BigDecimal` where value is monetary/quantity. |
| `PIC 9(8)` used as date | `YYYYMMDD` | `DATE` | `java.time.LocalDate` | e.g. `PORT-CREATE-DATE`, `POS-DATE`, `TRN-DATE`. Convert `19000101`/spaces → null per data rules. |
| `PIC X(6)` used as time | `HHMMSS` | `TIME` | `java.time.LocalTime` | e.g. `TRN-TIME`, `HIST-TIME`. |
| `PIC X(26)` timestamp | z/OS timestamp string | `TIMESTAMP` | `java.time.LocalDateTime` / `Instant` | e.g. `POS-LAST-MAINT-DATE`, `TRN-PROCESS-DATE`. |
| `PIC S9(m)V9(d) COMP-3` | Signed packed decimal | `NUMERIC(m+d, d)` | **`java.math.BigDecimal`** | Never `double`/`float`. Scale = `d`, precision = `m+d`. See table below. |
| `PIC 9(n) COMP` | Binary halfword/fullword | `SMALLINT`/`INTEGER` | `short`/`int` | Counters (e.g. `CK-RECORDS-READ`), not money. |
| `PIC S9(4) COMP` | Signed binary | `SMALLINT`/`INTEGER` | `short`/`int` | Return codes (`BCT-RETURN-CODE`, `PSR-DEP-RC`). |
| `88` condition names | Enumerated values | `CHAR(1)`/`CHAR(2)` + CHECK | Java `enum` | e.g. `PORT-STATUS` → `A/C/S`; `TRN-TYPE` → `BU/SL/TR/FE`. Model as JPA-mapped enum. |
| `FILLER PIC X(n)` | Reserved slack bytes | (drop column) | (omit) | Retain only in the fixed-width codec (Phase 0/1) for exact record round-trips. |

**Packed-decimal precision/scale (`COMP-3`) worked examples** — from the copybooks:

| Field (copybook) | COBOL PIC | `BigDecimal` scale | SQL type |
| --- | --- | --- | --- |
| `PORT-TOTAL-VALUE`, `PORT-CASH-BALANCE` (`PORTFLIO.cpy`) | `S9(13)V99 COMP-3` | 2 | `NUMERIC(15,2)` |
| `POS-QUANTITY` (`POSREC.cpy`) | `S9(11)V9(4) COMP-3` | 4 | `NUMERIC(15,4)` |
| `POS-COST-BASIS`, `POS-MARKET-VALUE` (`POSREC.cpy`) | `S9(13)V9(2) COMP-3` | 2 | `NUMERIC(15,2)` |
| `TRN-QUANTITY`, `TRN-PRICE` (`TRNREC.cpy`) | `S9(11)V9(4) COMP-3` | 4 | `NUMERIC(15,4)` |
| `TRN-AMOUNT` (`TRNREC.cpy`) | `S9(13)V9(2) COMP-3` | 2 | `NUMERIC(15,2)` |

Note the DB2 DDL already widens these: `INVESTMENT_POSITIONS.QUANTITY` is `DECIMAL(18,4)`,
`COST_BASIS`/`MARKET_VALUE` `DECIMAL(18,2)` (see `src/database/db2/db2-definitions.sql`).
Phase 1 must reconcile VSAM copybook precision vs DB2 DDL precision and pick the **wider**
of the two for the target schema, documenting each decision.

**Rounding:** All `BigDecimal` arithmetic uses `RoundingMode.HALF_UP` at the field scale,
matching COBOL `ROUNDED`. Every derived monetary value (cost basis, gain/loss, market
value, totals) must set an explicit scale before persistence.

### 0.4 Fixed-width record mapping convention

VSAM KSDS records are fixed-length (see `src/database/vsam/vsam-definitions.txt`):
`PORTMSTR`=400, `TRANHIST`=300, `POSHIST`=350 bytes, with copybook `FILLER` reserving
trailing slack. The golden-master harness and any file-based interop must use a
**byte-exact fixed-width codec** that:

- Maps each copybook `05/10` field to an offset+length span.
- Encodes `COMP-3` as packed nibbles (2 digits/byte + sign nibble) so byte images match.
- Preserves `FILLER` bytes and record length exactly for round-trip equality.
- Is generated from, and validated against, the copybooks in `src/copybook/common/`.

---

## Phase 0 — Foundation

**Goal:** Stand up the Java project, choose the RDBMS, and build the **golden-master test
harness** that will gate every later phase. No business logic is migrated here.

### 0.1 Entry criteria
- Repository access to all COBOL sources and `documentation/technical/`.
- Agreement on target stack (§0.1) and RDBMS (§0.2).

### 0.2 Tasks

| # | Task | References | Type |
| --- | --- | --- | --- |
| 0.1 | Create Maven/Gradle multi-module project (`app-domain`, `app-batch`, `app-online`, `app-data`, `test-harness`). | — | infra |
| 0.2 | Provision RDBMS (PostgreSQL) locally + CI (Testcontainers / H2 PG-mode). | `src/database/db2/*.sql` | infra |
| 0.3 | Implement the fixed-width + `COMP-3` codec (§0.4) driven by copybook layouts. | `src/copybook/common/{PORTFLIO,POSREC,TRNREC,HISTREC}.cpy` | infra |
| 0.4 | Build the **golden-master harness**: (a) capture COBOL program **inputs** from JCL DD statements and test data; (b) run the COBOL program (GnuCOBOL where possible per repo blueprint, else recorded z/OS output) to record **expected outputs**; (c) run the Java equivalent; (d) byte/field compare. | `src/jcl/**`, `documentation/operations/test-data-specs.md`, `src/programs/test/{TSTGEN00,TSTVAL00}.cbl`, `src/jcl/test/{TSTGEN,TSTVAL}.jcl` | test |
| 0.5 | Catalog every JCL job's DD-name → dataset → program mapping as harness fixtures. | `src/jcl/**` (`batch/`, `portfolio/`, `utility/`, `test/`, `RTNANA.jcl`) | test |
| 0.6 | Reuse the COBOL test generator semantics (`TSTGEN00`) to synthesize deterministic input corpora for portfolios, positions, transactions. | `src/programs/test/TSTGEN00.cbl`, `src/jcl/test/TSTGEN.jcl` | test |
| 0.7 | Encode the COBOL validation oracle (`TSTVAL00`) as Java assertions for result comparison. | `src/programs/test/TSTVAL00.cbl`, `src/jcl/test/TSTVAL.jcl` | test |
| 0.8 | CI pipeline: build + run golden-master suite on every PR; publish comparison diffs. | — | infra |
| 0.9 | Return-code semantics fixture: replicate JCL `RC ≤ 4` gating so Java job exit codes are comparable. | `documentation/technical/system-architecture.md` §4.1 | test |

### 0.3 Golden-master harness design

```
COBOL input (JCL DD + VSAM/seq datasets, test-data-specs.md)
        │
        ├──► COBOL program run ──► expected output (reports, VSAM images, DB2 rows)
        │                                   │
        └──► Java program run ─────► actual output
                                            │
                              field-level & byte-level comparator
                                            │
                                   PASS / FAIL (gates the phase)
```

The comparator supports three fidelity modes: **byte-exact** (fixed-width files),
**field-normalized** (dates/timestamps/whitespace tolerant), and **row-set** (DB2/JPA
result sets compared as ordered/unordered sets).

### 0.4 Exit / acceptance criteria
- Harness can capture inputs, run both sides, and diff outputs for at least one
  read-only program end-to-end (proof: a `RPTPOS00` comparison, see Phase 4).
- CI green; codec round-trips every copybook layout byte-for-byte.
- **No COBOL sources modified.**

### 0.5 COBOL sources replaced
None (foundation only). Harness *consumes* `src/programs/test/*` and `src/jcl/**`.

---

## Phase 1 — Data Layer

**Goal:** Convert the DB2 and VSAM definitions into a relational schema + JPA entities,
and lock down the type-mapping conventions (§0.3–0.4).

### 1.1 Entry criteria
- Phase 0 harness and codec operational; RDBMS provisioned.

### 1.2 Tasks

| # | Task | Source(s) → Target | Access |
| --- | --- | --- | --- |
| 1.1 | Translate core DB2 DDL to target DDL + Flyway/Liquibase migrations. | `src/database/db2/db2-definitions.sql` → `PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY` tables, indexes, views (`ACTIVE_PORTFOLIOS`, `CURRENT_POSITIONS`) | write (schema) |
| 1.2 | Translate `POSHIST` incl. range partitioning by `TRANS_DATE` → native partitioned table. | `src/database/db2/POSHIST.sql` | write (schema) |
| 1.3 | Translate `ERRLOG` table + `ERRLOG_CLEANUP` stored procedure (retention delete). | `src/database/db2/ERRLOG.sql` | write (schema) |
| 1.4 | Translate `RTNCODES` return-code logging table + indexes. | `src/database/db2/RTNCODES.sql` | write (schema) |
| 1.5 | Model DB2 `PORTPLAN` bind options as JPA/tx defaults (ISOLATION CS → `READ_COMMITTED`, RELEASE(COMMIT) → tx-scoped connections). | `src/database/db2/PORTPLAN.sql` | config |
| 1.6 | Map VSAM KSDS clusters to entities/tables, reconciling key structure and record length. | `src/database/vsam/vsam-definitions.txt` (`PORTMSTR` 400/key12, `TRANHIST` 300/key20, `POSHIST` 350/key18) | write (schema) |
| 1.7 | Generate JPA entities from copybooks with the §0.3 type map and `@Enumerated`/converter for `88`-level codes. | `src/copybook/common/{PORTFLIO,POSREC,TRNREC,HISTREC}.cpy` | — |
| 1.8 | Reconcile VSAM copybook precision vs DB2 DDL precision; choose wider scale; document each field. | `POSREC.cpy`/`TRNREC.cpy` vs `db2-definitions.sql` | — |
| 1.9 | Map DB2 interface copybooks to repository/DTO boundary. | `src/copybook/db2/{DBTBLS,DBPROC,SQLCA}.cpy` | — |
| 1.10 | Author the fixed-width ↔ entity mapping for each copybook (for file interop + harness). | `src/copybook/common/*.cpy` | — |

### 1.3 Entity/table mapping summary

| COBOL record (copybook) | VSAM file | DB2 table (nearest) | JPA entity | Key |
| --- | --- | --- | --- | --- |
| `PORT-RECORD` (`PORTFLIO.cpy`) | `PORTMSTR` (400) | `PORTFOLIO_MASTER` | `Portfolio` | `PORT-ID` + `PORT-ACCOUNT-NO` |
| `POSITION-RECORD` (`POSREC.cpy`) | `POSHIST` (350) | `INVESTMENT_POSITIONS` | `Position` | `PORTFOLIO-ID`+`DATE`+`INVESTMENT-ID` |
| `TRANSACTION-RECORD` (`TRNREC.cpy`) | `TRANHIST` (300) | `TRANSACTION_HISTORY` | `Transaction` | `DATE`+`TIME`+`PORTFOLIO-ID`+`SEQ` |
| `HISTORY-RECORD` (`HISTREC.cpy`) | (history/audit) | `POSHIST` / audit | `HistoryEntry` | `PORTFOLIO-ID`+`DATE`+`TIME`+`SEQ` |
| `BATCH-CONTROL-RECORD` (`BCHCTL.cpy`) | BCHCTL KSDS | (Spring Batch meta) | `JobControl` | `JOB-NAME`+`DATE`+`SEQ` |
| `CHECKPOINT-*` (`CKPRST.cpy`) | Checkpoint KSDS | (Spring Batch meta) | (ExecutionContext) | `PROGRAM-ID`+`RUN-DATE` |

> **Naming note:** The DB2 `PORTFOLIO_MASTER` key is `PORTFOLIO_ID` while the VSAM
> copybook key is `PORT-ID` + `PORT-ACCOUNT-NO`; `POSHIST.sql` uses `ACCOUNT_NO`+
> `PORTFOLIO_ID`. Phase 1 must define a canonical identity and record the crosswalk.

### 1.4 Exit / acceptance criteria
- Schema migrations apply cleanly; entities load/persist sample data.
- Codec round-trips all four common copybooks byte-exact (golden-master).
- Precision/scale crosswalk document reviewed and signed off.

### 1.5 COBOL sources replaced
`src/database/db2/{db2-definitions,POSHIST,ERRLOG,RTNCODES,PORTPLAN}.sql`,
`src/database/vsam/vsam-definitions.txt`, and the data-definition copybooks
`src/copybook/common/*.cpy`, `src/copybook/db2/*.cpy` (consumed, not deleted).

---

## Phase 2 — Cross-cutting Services

**Goal:** Replace the shared DB2 plumbing and error/audit subroutines with Spring
infrastructure so later phases have connection management, transactions, logging, and
audit available.

### 2.1 Entry criteria
- Phase 1 schema + entities merged and green.

### 2.2 Tasks

| # | COBOL source | Replace with | DB2 access | Type |
| --- | --- | --- | --- | --- |
| 2.1 | `src/programs/common/DB2CONN.cbl` | Spring `DataSource` + HikariCP connection pool; drop manual connect/disconnect. | Read/Write | write-path (infra) |
| 2.2 | `src/programs/common/DB2CMT.cbl` | `@Transactional` + `PlatformTransactionManager` (commit/rollback, commit points → commit-interval). | Read/Write | write-path (infra) |
| 2.3 | `src/programs/common/DB2ERR.cbl` | `SQLExceptionTranslator` + `@ControlAdvice`-style DB error mapping; deadlock retry via `@Retryable`. | — | infra |
| 2.4 | `src/programs/common/DB2STAT.cbl` | Micrometer metrics + `SQLCA`-equivalent status wrapper for legacy status-code parity. | Read | infra |
| 2.5 | `src/programs/common/ERRPROC.cbl` | SLF4J/Logback error module writing structured events to `ERRLOG` entity (severity/type parity with `ERRLOG.sql` codes). | Write (`ERRLOG`) | infra |
| 2.6 | `src/programs/common/AUDPROC.cbl` | JPA audit module (before/after images) writing history/audit rows; align to `HISTREC.cpy` before/after image semantics. | Write | infra |
| 2.7 | `src/copybook/db2/SQLCA.cpy` semantics | Status/return-code adapter so callers still observe legacy `SQLCODE`-style outcomes during parallel-run. | — | infra |

> Dependencies (§5.1): nearly every batch/online program depends on `DB2CONN` and
> `ERRPROC`. This phase is therefore a hard prerequisite for Phases 3–7.

### 2.3 Exit / acceptance criteria
- Transactions commit/rollback with commit-interval matching `CK-COMMIT-FREQ` (1000,
  see `CKPRST.cpy`).
- Error events land in `ERRLOG` with matching `ERROR_TYPE`/`ERROR_SEVERITY` codes.
- Audit before/after images match COBOL `AUDPROC` output on golden-master fixtures.

### 2.4 COBOL sources replaced
`src/programs/common/{DB2CONN,DB2CMT,DB2ERR,DB2STAT,ERRPROC,AUDPROC}.cbl`.

---

## Phase 3 — Online Inquiry (read-only)

**Goal:** Translate the CICS inquiry programs into REST endpoints and map the BMS screens
to API responses (and optionally a thin web UI). **Read-only** — no writes to portfolio
state (`SECMGR` writes only security/session logs).

### 3.1 Entry criteria
- Phase 2 cross-cutting services available.

### 3.2 Tasks (dependencies/resources from §5.1.4)

| # | COBOL source | Depends on | Resources | Target | R/W |
| --- | --- | --- | --- | --- | --- |
| 3.1 | `src/programs/online/INQONLN.cbl` (main controller/menu) | `SECMGR`, `CURSMGR` | CICS, BMS maps | REST controller / menu-routing endpoint | read-only |
| 3.2 | `src/programs/online/INQPORT.cbl` (portfolio inquiry) | `DB2ONLN`, `ERRHNDL` | DB2, VSAM | `GET /portfolios/{id}/positions` | read-only |
| 3.3 | `src/programs/online/INQHIST.cbl` (history inquiry) | `DB2ONLN`, `ERRHNDL` | DB2, history files | `GET /portfolios/{id}/transactions?from&to` (date-range) | read-only |
| 3.4 | `src/programs/online/CURSMGR.cbl` (cursor/paging/PF keys) | None | BMS maps | Pagination + navigation params (PF7/PF8 → `page`/`size`) | read-only |
| 3.5 | `src/programs/online/SECMGR.cbl` (security/authorization) | `DB2ONLN` | Security tables | Spring Security (authN/authZ) + session/security event logging | write (sec log only) |
| 3.6 | `src/programs/online/ERRHNDL.cbl` (online error handler) | None | Error log | `@RestControllerAdvice` exception → error payload | read-only |
| 3.7 | `src/programs/online/DB2ONLN.cbl` (online DB2 controller) | `DB2RECV` | DB2 connection | Read-only repositories over Phase 2 `DataSource` | read-only |
| 3.8 | `src/programs/online/DB2RECV.cbl` (DB2 recovery) | `ERRHNDL` | DB2 connection | Retry/rollback via Phase 2 `DB2ERR` replacement | read-only |
| 3.9 | `src/maps/INQSET.bms` (MENMAP/POSMAP/HISMAP/ERRMAP) | — | 3270 screens | JSON response schemas + optional web UI screens | read-only |

### 3.3 BMS map → API mapping

| BMS map (`INQSET.bms`) | Purpose | API response shape |
| --- | --- | --- |
| `MENMAP` | Main menu (options 1/2/3) | Menu/routing metadata |
| `POSMAP` | Position inquiry (Account, Fund, Units, Cost Basis, Market Value) | `PositionResponse { account, fundId, fundName, units, costBasis, marketValue }` |
| `HISMAP` | Transaction history (10 rows: Date/Type/Units/Price/Amount) | `Page<TransactionResponse>` |
| `ERRMAP` | Error screen (code + details) | RFC-7807 `ProblemDetail { code, detail }` |

PF-key navigation (`PF3=Exit`, `PF7=Previous`, `PF8=Next`) maps to REST pagination and
client navigation.

### 3.4 Exit / acceptance criteria
- Each endpoint's response matches COBOL inquiry output on golden-master fixtures
  (field-normalized comparison of `POSMAP`/`HISMAP` field values).
- No mutation of portfolio/position/transaction data (verified by DB snapshot diff = ∅
  except security/session logs).

### 3.5 COBOL sources replaced
`src/programs/online/{INQONLN,INQPORT,INQHIST,CURSMGR,SECMGR,ERRHNDL,DB2ONLN,DB2RECV}.cbl`;
`src/maps/INQSET.bms`. CICS resource defs `src/cics/PORTDFN.csd` retired.

---

## Phase 4 — Reporting (read-only)

**Goal:** Reimplement the batch report/analysis programs as read-only reporting jobs
(Spring Batch read-only steps or query services).

### 4.1 Entry criteria
- Phases 1–2 complete (schema + DB2 access). Phase 3 not required but recommended for
  shared read models.

### 4.2 Tasks (deps/access from §5.1.1)

| # | COBOL source | Depends on | Input | Output | DB2 | JCL |
| --- | --- | --- | --- | --- | --- | --- |
| 4.1 | `src/programs/batch/RPTPOS00.cbl` (daily position report + valuations) | `DB2CONN`, `ERRPROC` | Position Master, Transaction History | Position report (FB LRECL 132) | Read | `src/jcl/batch/RPTPOS.jcl` |
| 4.2 | `src/programs/batch/RPTAUD00.cbl` (audit/compliance report) | `DB2CONN`, `ERRPROC` | Audit Log | Audit reports | Read | `src/jcl/batch/RPTAUD.jcl` |
| 4.3 | `src/programs/batch/RPTSTA00.cbl` (statistics/performance report) | `DB2CONN`, `ERRPROC` | System Stats | Statistics reports | Read | `src/jcl/batch/RPTSTA.jcl` |
| 4.4 | `src/programs/batch/RTNANA00.cbl` (return-code analysis) | `ERRPROC` | `RTNCODES` table | Return-code analysis report (FBA LRECL 133) | Read | `src/jcl/RTNANA.jcl` |
| 4.5 | `src/programs/batch/RTNCDE00.cbl` (return-code logging/lookup) | `ERRPROC` | `RTNCODES` table | Logged return codes | Read/Write (`RTNCODES`) | — |

> `RTNCDE00` writes `RTNCODES` rows (return-code capture); it is a supporting logger for
> the analysis job `RTNANA00`. Treat 4.5 as "instrumentation write" only — it does not
> touch portfolio/position/transaction state.

### 4.3 Exit / acceptance criteria
- Generated report content matches COBOL report output byte/field level on golden-master
  fixtures (report line layouts preserved: LRECL 132/133).
- Reporting jobs perform zero writes to business tables (write allowed only to
  `RTNCODES` for 4.5).

### 4.4 COBOL sources replaced
`src/programs/batch/{RPTPOS00,RPTAUD00,RPTSTA00,RTNANA00,RTNCDE00}.cbl`;
`src/jcl/batch/{RPTPOS,RPTAUD,RPTSTA}.jcl`, `src/jcl/RTNANA.jcl`.

---

## Phase 5 — Portfolio Domain Services

**Goal:** Translate the portfolio management programs into domain services and JPA
repositories. This introduces the first **write-path** domain logic, but scoped to
portfolio/position CRUD rather than the high-volume batch pipeline.

### 5.1 Entry criteria
- Phases 1–2 complete. Phase 4 recommended (shared read models validated).

### 5.2 Tasks

| # | COBOL source | Target | Data access | R/W |
| --- | --- | --- | --- | --- |
| 5.1 | `src/programs/portfolio/PORTREAD.cbl` | `PortfolioService.read()` / repository finders | `PORTMSTR` / `PORTFOLIO_MASTER` | read-only |
| 5.2 | `src/programs/portfolio/PORTVALD.cbl` (validation; compiled as module) | `PortfolioValidator` (Bean Validation + domain rules) | — | read-only |
| 5.3 | `src/programs/portfolio/PORTADD.cbl` | `PortfolioService.add()` | `PORTMSTR` / `PORTFOLIO_MASTER` | write-path |
| 5.4 | `src/programs/portfolio/PORTUPDT.cbl` | `PortfolioService.update()` | `PORTMSTR` / `PORTFOLIO_MASTER` | write-path |
| 5.5 | `src/programs/portfolio/PORTDEL.cbl` | `PortfolioService.delete()` (soft-delete via status `C`) | `PORTMSTR` / `PORTFOLIO_MASTER` | write-path |
| 5.6 | `src/programs/portfolio/PORTTRAN.cbl` | `PortfolioTransferService` (transaction/transfer logic) | `PORTMSTR`, history | write-path |
| 5.7 | `src/programs/portfolio/PORTMSTR.cbl` | `PortfolioMaster` aggregate/repository (master maintenance) | `PORTMSTR` / `PORTFOLIO_MASTER` | write-path |
| 5.8 | `src/programs/portfolio/PORTTEST.cbl` (driver/harness for the above) | Migrated into Phase 0 harness + service integration tests | — | test |

JCL drivers to convert to service invocations / job endpoints:
`src/jcl/portfolio/{PORTADD,PORTUPDT,PORTDEL,PORTREAD,PORTTEST,PORTDEF}.jcl`.

### 5.3 Exit / acceptance criteria
- CRUD + transfer operations produce identical resulting records (byte-exact via codec)
  vs COBOL for the same inputs; validation rejects the same inputs `PORTVALD` rejects.
- Audit before/after images (Phase 2 `AUDPROC` replacement) match on every mutation.
- All writes are transactional and roll back atomically on validation failure.

### 5.4 COBOL sources replaced
`src/programs/portfolio/{PORTADD,PORTDEL,PORTREAD,PORTUPDT,PORTTRAN,PORTVALD,PORTMSTR,PORTTEST}.cbl`;
`src/jcl/portfolio/*.jcl`.

---

## Phase 6 — Batch Write-path Core

**Goal:** Migrate the high-volume transactional pipeline and the control/checkpoint
framework into Spring Batch. This is the highest-risk phase and is deliberately last
among the migration-of-logic phases.

### 6.1 Entry criteria
- Phases 1–5 complete and green on golden-master.
- Portfolio domain services (Phase 5) available for the pipeline to call.

### 6.2 Pipeline tasks (JCL flow §4.1: `TRNVAL00 → POSUPD00 → HISTLD00 → RPP`)

| # | COBOL source | Target (Spring Batch) | Input | Output | DB2 | R/W |
| --- | --- | --- | --- | --- | --- | --- |
| 6.1 | **`TRNVAL00`** (transaction validation, aka TRNMAIN) | `transactionValidationStep` (ItemReader→Validator→Writer) | Transaction File | Validated transactions | — | write-path |
| 6.2 | **`POSUPDT`/`POSUPD00`** (position update, cost basis) | `positionUpdateStep` | Validated transactions | Position Master, Transaction History | Read/Write | write-path |
| 6.3 | `src/programs/batch/HISTLD00.cbl` (history load to DB2) | `historyLoadStep` (JPA batch insert) | Transaction History (`TRANHIST`) | `POSHIST`/DB2 tables | Write | write-path |
| 6.4 | `src/programs/batch/BCHCTL00.cbl` (job-level control/sequencing) | Spring Batch `JobExecution` + `JobExplorer` (status/dependencies from `BCHCTL.cpy`) | Control File | Status updates | Write | write-path |
| 6.5 | `src/programs/batch/CKPRST.cbl` (checkpoint/restart) | Spring Batch commit-interval + restart + `ExecutionContext` (map `CK-LAST-KEY`, `CK-RECORDS-*`) | Checkpoint KSDS | — | — | infra |
| 6.6 | `src/programs/batch/PRCSEQ00.cbl` (process sequencing) | Spring Batch job flow / step ordering + `JobParameters` (from `PRCSEQ.cpy` sequences) | Process ctl | — | — | infra |
| 6.7 | `src/programs/batch/RCVPRC00.cbl` (recovery processing) | Spring Batch `SkipPolicy`/`RetryPolicy` + restart recovery | — | — | Read/Write | write-path |

> **Repository-accuracy notes (must be resolved before Phase 6 coding):**
> - `src/programs/batch/POSUPDT.cbl` is **an empty placeholder (0 lines)** in this
>   benchmark. The position-update behavior must be derived from
>   `system-architecture.md` §1.2.2 / §2.2 and copybooks (`POSREC.cpy`, `TRNREC.cpy`),
>   not from source. Flag as a **specification-gap** risk.
> - **`TRNVAL00` has no source file** in the repo. It appears only in
>   `system-architecture.md` (§1.2.2, §2.2, §4.1) and `src/copybook/batch/PRCSEQ.cpy`
>   `STANDARD-SEQUENCES` (`SEQ-MAIN-PROCESS`: `TRNVAL00`/`POSUPD00`/`HISTLD00`).
>   Behavior must be reconstructed from the architecture doc + transaction copybook.
> - `RPTGEN00`/`BCKLOD00`/`ENDDAY` referenced in `PRCSEQ.cpy` are likewise not present
>   as sources; treat as sequence steps to be mapped (reporting handled in Phase 4).

### 6.3 JCL job flow → Spring Batch mapping

```
JCL (§4.1)                         Spring Batch
Start of Day ──1800──► TRNVAL00     job "eod" ─► step transactionValidation
   │ RC ≤ 4                             │ (fail step if errors > threshold)
   ▼                                    ▼
POSUPD00 ──RC ≤ 4──►               step positionUpdate (commit-interval = CK-COMMIT-FREQ 1000)
   ▼                                    ▼
HISTLD00 ──RC ≤ 4──►               step historyLoad
   ▼                                    ▼
RPP (reports) ─► End of Day        (Phase 4 reporting jobs)
```

- `RC ≤ 4` step gating → Spring Batch step `ExitStatus` transitions / `JobExecutionDecider`.
- `BCHCTL` prerequisites (`BCT-PREREQ-JOBS`, `BCT-PREREQ-RC`) → job flow conditions.
- Checkpoint/restart (`CK-COMMIT-FREQ`=1000, `CK-MAX-RESTARTS`=3, `CK-LAST-KEY`) →
  chunk `commit-interval`, restartable job, `ExecutionContext` cursor, retry limit.

### 6.4 Exit / acceptance criteria
- Full pipeline run produces identical Position Master, Transaction History, and DB2
  `POSHIST` contents (byte/row exact) vs COBOL for the same transaction corpus.
- Restart from a forced mid-run failure resumes at the last commit point and yields the
  same final state as an uninterrupted run (validates checkpoint parity).
- Step exit codes reproduce the `RC ≤ 4` gating behavior.

### 6.5 COBOL sources replaced
`src/programs/batch/{HISTLD00,BCHCTL00,CKPRST,PRCSEQ00,RCVPRC00}.cbl`, plus the
reconstructed `TRNVAL00` and `POSUPDT`/`POSUPD00` behavior; `src/copybook/batch/*.cpy`
(consumed); `src/jcl/batch/*` job flow.

---

## Phase 7 — Utilities, Orchestration & Decommission

**Goal:** Replace operational utilities, convert remaining JCL to a scheduler, run
parallel validation at scale, and retire the COBOL system.

### 7.1 Entry criteria
- Phases 0–6 complete; golden-master green across read and write paths.

### 7.2 Tasks (utility deps/access from §5.1.2)

| # | COBOL source | Depends on | Target | DB2 | JCL |
| --- | --- | --- | --- | --- | --- |
| 7.1 | `src/programs/utility/UTLMNT00.cbl` (file maintenance/archive/reorg) | `ERRPROC` | Ops job (archive/cleanup) + `ERRLOG_CLEANUP` schedule | None | `src/jcl/utility/UTLMNT.jcl` |
| 7.2 | `src/programs/utility/UTLVAL00.cbl` (data integrity/cross-ref/balance) | `DB2CONN`, `ERRPROC` | Data-validation job + reconciliation checks | Read | `src/jcl/utility/UTLVAL.jcl` |
| 7.3 | `src/programs/utility/UTLMON00.cbl` (system monitor/metrics/alerts) | `DB2CONN`, `ERRPROC` | Micrometer + Actuator + alerting (thresholds → alerts) | Read/Write | `src/jcl/utility/UTLMON.jcl` |
| 7.4 | Remaining JCL orchestration | — | Scheduler (Spring Batch + Quartz / enterprise scheduler); convert `src/jcl/**` job graph | — | `src/jcl/**` |
| 7.5 | Parallel-run validation | — | Run COBOL and Java in parallel on production-like volumes; compare via golden-master comparator | — | — |
| 7.6 | Decommission | — | Freeze COBOL, cut over, retire z/OS artifacts (`src/cics/PORTDFN.csd`, load libs) | — | — |

### 7.3 Exit / acceptance criteria
- Utilities reproduce COBOL outputs (archive manifests, validation reports, monitor
  metrics) on golden-master fixtures.
- Parallel run shows zero material differences over an agreed observation window.
- Cutover runbook approved; COBOL formally retired.

### 7.4 COBOL sources replaced
`src/programs/utility/{UTLMNT00,UTLMON00,UTLVAL00}.cbl`; `src/jcl/utility/*.jcl` and all
remaining `src/jcl/**`; CICS/z/OS deployment artifacts.

---

## 8. Dependency-Ordering Rationale

The phase order is driven by **dependency direction and risk**, not by module size.

1. **Foundation and data first (Phases 0–1).** Nothing can be compared without the
   golden-master harness (Phase 0), and nothing can persist without the schema/entities
   (Phase 1). These are prerequisites for every other phase.

2. **Cross-cutting services next (Phase 2).** The §5.1 dependency tables show that almost
   every batch and online program depends on `DB2CONN` and `ERRPROC`. Migrating these
   shared leaves once — as Spring `DataSource`, `@Transactional`, logging, and audit —
   means later phases inherit correct connection, transaction, error, and audit behavior
   instead of re-implementing it per program.

3. **Read-only before write-path (Phases 3–4 before 5–6).** Online inquiry (`INQPORT`,
   `INQHIST`, `DB2ONLN`) and reporting (`RPTPOS00`, `RPTAUD00`, `RPTSTA00`, `RTNANA00`)
   are **read-only** (§5.1: DB2 access = *Read*). They:
   - are **leaf consumers** — other programs do not depend on them, so migrating them
     cannot break upstream logic;
   - are **safe to run in parallel** against production data, because a bug produces a
     wrong *read/report*, never corrupted state;
   - **exercise the Phase 1 schema and Phase 2 data-access layer end-to-end** on real
     data, surfacing mapping/precision defects (e.g. `COMP-3` scale, date formats)
     *before* those layers are trusted with writes;
   - give the golden-master harness its first full-pipeline comparisons cheaply.

4. **Portfolio domain services before batch core (Phase 5 before 6).** Portfolio CRUD is
   lower-volume, single-record write logic with clear validation (`PORTVALD`). Getting
   the write semantics, audit images, and transaction boundaries correct here — at low
   volume — de-risks the high-throughput pipeline, which also *calls into* the same
   domain rules.

5. **Transactional write-path core last among logic phases (Phase 6).** `TRNVAL00 →
   POSUPD00 → HISTLD00` plus checkpoint/restart (`CKPRST`, `BCHCTL00`, `PRCSEQ00`,
   `RCVPRC00`) is the **root of the dependency graph** and the highest-blast-radius code:
   a defect here corrupts positions and history for every downstream report and inquiry.
   By the time it is migrated, the schema, data-access layer, audit, read models, and
   domain rules it depends on are already validated by the golden-master suite, so the
   remaining risk is isolated to the pipeline/checkpoint logic itself.

6. **Utilities, orchestration, decommission last (Phase 7).** Utilities and the scheduler
   operate *on top of* a fully migrated system; parallel-run validation and retirement
   are only meaningful once every functional phase passes golden-master.

In short: build the ruler (Phase 0), then the foundation (1–2), then migrate **leaves
inward** — read-only consumers → domain services → the transactional core — so each phase
is validated against COBOL before anything depends on it, and the write-path is only
touched once everything beneath it is proven.

---

## Appendix A — Program → Phase Index

| Program / artifact | Path | Phase | R/W |
| --- | --- | --- | --- |
| `DB2CONN` | `src/programs/common/DB2CONN.cbl` | 2 | infra (R/W) |
| `DB2CMT` | `src/programs/common/DB2CMT.cbl` | 2 | infra (R/W) |
| `DB2ERR` | `src/programs/common/DB2ERR.cbl` | 2 | infra |
| `DB2STAT` | `src/programs/common/DB2STAT.cbl` | 2 | infra (R) |
| `ERRPROC` | `src/programs/common/ERRPROC.cbl` | 2 | infra (W) |
| `AUDPROC` | `src/programs/common/AUDPROC.cbl` | 2 | infra (W) |
| `INQONLN` | `src/programs/online/INQONLN.cbl` | 3 | read-only |
| `INQPORT` | `src/programs/online/INQPORT.cbl` | 3 | read-only |
| `INQHIST` | `src/programs/online/INQHIST.cbl` | 3 | read-only |
| `CURSMGR` | `src/programs/online/CURSMGR.cbl` | 3 | read-only |
| `SECMGR` | `src/programs/online/SECMGR.cbl` | 3 | write (sec log) |
| `ERRHNDL` | `src/programs/online/ERRHNDL.cbl` | 3 | read-only |
| `DB2ONLN` | `src/programs/online/DB2ONLN.cbl` | 3 | read-only |
| `DB2RECV` | `src/programs/online/DB2RECV.cbl` | 3 | read-only |
| `RPTPOS00` | `src/programs/batch/RPTPOS00.cbl` | 4 | read-only |
| `RPTAUD00` | `src/programs/batch/RPTAUD00.cbl` | 4 | read-only |
| `RPTSTA00` | `src/programs/batch/RPTSTA00.cbl` | 4 | read-only |
| `RTNANA00` | `src/programs/batch/RTNANA00.cbl` | 4 | read-only |
| `RTNCDE00` | `src/programs/batch/RTNCDE00.cbl` | 4 | write (`RTNCODES`) |
| `PORTREAD` | `src/programs/portfolio/PORTREAD.cbl` | 5 | read-only |
| `PORTVALD` | `src/programs/portfolio/PORTVALD.cbl` | 5 | read-only |
| `PORTADD` | `src/programs/portfolio/PORTADD.cbl` | 5 | write-path |
| `PORTUPDT` | `src/programs/portfolio/PORTUPDT.cbl` | 5 | write-path |
| `PORTDEL` | `src/programs/portfolio/PORTDEL.cbl` | 5 | write-path |
| `PORTTRAN` | `src/programs/portfolio/PORTTRAN.cbl` | 5 | write-path |
| `PORTMSTR` | `src/programs/portfolio/PORTMSTR.cbl` | 5 | write-path |
| `PORTTEST` | `src/programs/portfolio/PORTTEST.cbl` | 5 / 0 | test |
| `TRNVAL00` | *(no source; arch §1.2.2, PRCSEQ.cpy)* | 6 | write-path |
| `POSUPDT`/`POSUPD00` | `src/programs/batch/POSUPDT.cbl` *(empty stub)* | 6 | write-path |
| `HISTLD00` | `src/programs/batch/HISTLD00.cbl` | 6 | write-path |
| `BCHCTL00` | `src/programs/batch/BCHCTL00.cbl` | 6 | write-path |
| `CKPRST` | `src/programs/batch/CKPRST.cbl` | 6 | infra |
| `PRCSEQ00` | `src/programs/batch/PRCSEQ00.cbl` | 6 | infra |
| `RCVPRC00` | `src/programs/batch/RCVPRC00.cbl` | 6 | write-path |
| `UTLMNT00` | `src/programs/utility/UTLMNT00.cbl` | 7 | none |
| `UTLVAL00` | `src/programs/utility/UTLVAL00.cbl` | 7 | read-only |
| `UTLMON00` | `src/programs/utility/UTLMON00.cbl` | 7 | write (monitor log) |
| `TSTGEN00` | `src/programs/test/TSTGEN00.cbl` | 0 | test |
| `TSTVAL00` | `src/programs/test/TSTVAL00.cbl` | 0 | test |

## Appendix B — Data Definition → Phase Index

| Artifact | Path | Phase |
| --- | --- | --- |
| Core DB2 tables/views | `src/database/db2/db2-definitions.sql` | 1 |
| Position history (partitioned) | `src/database/db2/POSHIST.sql` | 1 |
| Error log + cleanup proc | `src/database/db2/ERRLOG.sql` | 1 (proc → 2) |
| Return-code table | `src/database/db2/RTNCODES.sql` | 1 |
| DB2 bind plan | `src/database/db2/PORTPLAN.sql` | 1 (tx config → 2) |
| VSAM cluster defs | `src/database/vsam/vsam-definitions.txt` | 1 |
| Common record copybooks | `src/copybook/common/*.cpy` | 1 |
| DB2 interface copybooks | `src/copybook/db2/*.cpy` | 1–2 |
| Batch control copybooks | `src/copybook/batch/*.cpy` | 6 |
| Online copybooks | `src/copybook/online/*.cpy` | 3 |
| BMS maps | `src/maps/INQSET.bms` | 3 |
| CICS defs | `src/cics/PORTDFN.csd` | 3 / 7 (retire) |

## Appendix C — Repository Accuracy / Open Items

- `src/programs/batch/POSUPDT.cbl` is empty (0 lines) — position-update logic is
  specified only in the architecture doc/copybooks. Reconstruct + confirm with a domain SME.
- `TRNVAL00` (transaction validation) has **no source file**; referenced in
  `system-architecture.md` §1.2.2/§2.2/§4.1 and `src/copybook/batch/PRCSEQ.cpy`.
- `RPTGEN00`, `BCKLOD00`, `INITDAY`, `ENDDAY`, `DATEVAL`, `CKPCLR` appear in
  `PRCSEQ.cpy` `STANDARD-SEQUENCES` but have no sources; map as orchestration steps.
- DB2 vs VSAM key/precision mismatches (`PORTFOLIO_MASTER.PORTFOLIO_ID` vs
  `PORT-ID`+`PORT-ACCOUNT-NO`; `DECIMAL(18,x)` DDL vs `S9(11/13)` copybooks) must be
  reconciled in Phase 1 with a documented canonical model.
