# Modernized Relational Schema Baseline

Flyway-style baseline migration (`migration/V1__baseline_schema.sql`) for the
modernized Investment Portfolio Management System. It consolidates the legacy
dual-store persistence (VSAM current data + DB2 historical data) into a single
relational schema (ANSI SQL, tested with PostgreSQL 14+).

## Legacy-to-modern mapping

| Legacy source | Legacy object | Modern table/view |
|---|---|---|
| `src/database/db2/db2-definitions.sql` | `PORTFOLIO_MASTER` | `portfolio_master` |
| `src/database/vsam/vsam-definitions.txt` | `PORTMSTR` KSDS | `portfolio_master` (+ `portfolio_master_vsam_key` unique index) |
| `src/database/db2/db2-definitions.sql` | `INVESTMENT_POSITIONS` | `investment_positions` |
| `src/database/db2/db2-definitions.sql` | `TRANSACTION_HISTORY` | `transaction_history` |
| `src/database/vsam/vsam-definitions.txt` | `TRANHIST` KSDS | `transaction_history` (KSDS key encoded in `transaction_id`) |
| `src/database/db2/POSHIST.sql` | `POSHIST` | `position_history` |
| `src/database/vsam/vsam-definitions.txt` | `POSHIST` KSDS | `position_history` |
| `src/database/db2/ERRLOG.sql` | `ERRLOG` | `error_log` |
| `src/database/db2/RTNCODES.sql` | `RTNCODES` | `return_codes` |
| `src/database/db2/db2-definitions.sql` | `ACTIVE_PORTFOLIOS` view | `active_portfolios` |
| `src/database/db2/db2-definitions.sql` | `CURRENT_POSITIONS` view | `current_positions` |

## Design decisions

- **Dual-store consolidation**: the VSAM KSDS files and DB2 tables described
  the same entities; the relational schema is the single source of truth.
  VSAM key structures are preserved as primary keys or unique indexes so
  record-level semantics survive the migration.
- **Status/type codes as CHECK constraints**: the code lists documented in
  the legacy DDL comments (portfolio status `A/C/S`, transaction status
  `P/F/R`, transaction types `BU/SL/TR/FE`, error type `S/A/D`, severity
  `1-4`) are enforced with CHECK constraints instead of comments.
- **`RTNCODES.TIMESTAMP` renamed to `logged_at`**: `TIMESTAMP` is a reserved
  word in most modern dialects.
- **DB2 physical artifacts dropped**: `CREATE DATABASE/TABLESPACE/STOGROUP`,
  bufferpools, `BIND PLAN` (`PORTPLAN.sql`), grants to `POSAPP`/`POSRPT`,
  and range partitioning are z/OS operational concerns. Partitioning of
  `position_history` can be reintroduced with native partitioning if volumes
  require it; access control belongs to the target platform's role model.
- **`ERRLOG_CLEANUP` procedure dropped**: retention is better handled by a
  scheduled job in the modern stack (e.g. Spring `@Scheduled` or cron)
  issuing `DELETE FROM error_log WHERE process_date < CURRENT_DATE - :days`.

## Validation

The migration applies cleanly on PostgreSQL 14+ (`psql -f
migration/V1__baseline_schema.sql`).
