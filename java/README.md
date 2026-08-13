# IPMS Java Migration

Java migration of the Investment Portfolio Management System (Enterprise COBOL for
z/OS) in this repository. This project is the migration foundation plus the first
vertical slice (the shared common-services layer) — it is **not** a full translation.

## Approach

The migration proceeds layer by layer, bottom-up, following the dependency order of
the COBOL system. Each layer is ported with tests before the next layer starts:

1. **Foundation** (done) — project skeleton, fixed-width record conventions, domain
   model from `src/copybook/common/`, persistence model from `src/database/`.
2. **Common services** (done, first vertical slice) — `ERRPROC` (error logging),
   `DB2CONN` (connection management), `DB2CMT` (commit/rollback/savepoint control).
3. **Portfolio** — `src/programs/portfolio/` core business logic (create/update/
   inquiry/validation), reusing the domain and persistence layers.
4. **Batch** — `src/programs/batch/` sequential processing (transaction processing,
   position updates, history loads) with checkpoint/restart semantics.
5. **Online** — `src/programs/online/` CICS transactions (see redesign note below).
6. **JCL orchestration** — job streams under `src/jcl/` (see redesign note below).

## Layout

- `com.ipms.common.fixedwidth` — `FixedWidthReader`/`FixedWidthWriter` for COBOL
  fixed-width records: `PIC X(n)` strings are space-padded; numeric `PIC` fields use
  `java.math.BigDecimal` with the implied decimal scale preserved (e.g.
  `PIC S9(11)V9(4)` -> `BigDecimal` with scale 4). Record lengths are validated on
  both parse and serialize.
- `com.ipms.domain` — Java records for every copybook in `src/copybook/common/`
  (TRNREC, POSREC, PORTFLIO, HISTREC, AUDITLOG, PORTVAL, COMMON, RTNCODE, RETHND,
  ERRHAND), preserving field names, order, and widths; level-88 condition values are
  enums or constants.
- `com.ipms.persistence` — JPA entities and Spring Data repositories for the DB2
  tables (`PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY`,
  `POSHIST`, `ERRLOG`, `RTNCODES`) and for the VSAM KSDS files (`PORTMSTR`,
  `POSFILE`), which are modeled as relational tables keyed on their VSAM primary
  keys. `PORTPLAN.sql` binds the DB2 package/plan and has no Java equivalent —
  packaging/deployment replaces it.
- `com.ipms.common.error` / `com.ipms.common.db` — the ported common-services slice.
- `com.ipms.batch`, `com.ipms.online`, `com.ipms.portfolio`, `com.ipms.utility` —
  placeholders for the corresponding `src/programs/` layers.

## Architectural redesign boundaries

Two layers must be redesigned rather than translated line-for-line:

- **`online/` (CICS/BMS)** — pseudo-conversational CICS transactions (e.g. the
  `PINQ` portfolio inquiry) with BMS maps and COMMAREA state passing have no direct
  Java equivalent. They will be redesigned as a web layer (REST controllers and/or
  server-rendered UI), with COMMAREA state replaced by request/session state.
- **`jcl/` (job streams)** — JCL jobs, step sequencing, condition-code checks, and
  GDG datasets will be redesigned as Spring Batch jobs (steps, flows, restartable
  job repositories) rather than translated.

## Build and test

Requires Java 21 and Maven:

```
cd java
mvn verify
```

Tests cover the fixed-width conventions, domain record round-trips, and the
common-services slice (error logging against the `ERRLOG` table, connection
retry/status handling, and commit/rollback/savepoint control) using an in-memory
H2 database standing in for DB2.
