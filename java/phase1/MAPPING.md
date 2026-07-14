# Phase 1 Data Layer Mapping

This document describes the migration of the COBOL Investment Portfolio Management System DB2/VSAM data layer to a relational model with JPA entities.

## Source Files

- `src/database/db2/db2-definitions.sql` - DB2 `PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY`
- `src/database/db2/POSHIST.sql` - DB2 `POSHIST`
- `src/database/db2/ERRLOG.sql` - DB2 `ERRLOG`
- `src/database/db2/RTNCODES.sql` - DB2 `RTNCODES`
- `src/database/vsam/vsam-definitions.txt` - VSAM KSDS files `PORTMSTR`, `TRANHIST`, `POSHIST`
- `src/copybook/common/` - COBOL record layouts:
  - `PORTFLIO` - portfolio master record
  - `POSREC` - position record
  - `TRNREC` - transaction record
  - `HISTREC` - history/audit record
  - `AUDITLOG` - audit log record
  - `RTNCODE` - return code area

## Table-to-COBOL File Mapping

| Relational Table | Source Definition | COBOL Copybook / File | Notes |
|------------------|-------------------|-----------------------|-------|
| `portfolio_master` | `db2-definitions.sql` | `PORTFOLIO_MASTER` | Normalized master keyed on `portfolio_id`. |
| `investment_positions` | `db2-definitions.sql` | `INVESTMENT_POSITIONS` | Composite key `(portfolio_id, investment_id, position_date)`. |
| `transaction_history` | `db2-definitions.sql` | `TRANSACTION_HISTORY` | Key is `transaction_id` (20 chars). |
| `poshist` | `POSHIST.sql` / `DBTBLS.cpy` | `POSHIST` | Position/transaction history loaded by `HISTLD00`. |
| `errlog` | `ERRLOG.sql` / `DBTBLS.cpy` | `ERRLOG` | Error log. |
| `rtncodes` | `RTNCODES.sql` | `RTNCODE` | Return code log (subset of `RTNCODE` work area). |
| `vsam_portmstr` | `vsam-definitions.txt` | `PORTMSTR` / `PORTFLIO` | VSAM portfolio master keyed on `portfolio_id, account_no`. |
| `vsam_poshist` | `vsam-definitions.txt` | `POSHIST` / `POSREC` | VSAM position history keyed on `portfolio_id, position_date, investment_id`. |
| `vsam_tranhist` | `vsam-definitions.txt` | `TRANHIST` / `TRNREC` | VSAM transaction history keyed on `transaction_id` (derived from `date+time+sequence_number`). |
| `history_record` | `HISTREC.cpy` | `HISTREC` | Generic history/audit record with before/after images. |
| `audit_log` | `AUDITLOG.cpy` | `AUDITLOG` | Audit log record (file and `SECMGR` DB2 table). |

## PIC / COMP-3 to `BigDecimal` Mapping

COBOL packed-decimal fields (`PIC S9(p)V9(s) COMP-3`) are mapped to `java.math.BigDecimal` using the exact precision and scale derived from the COBOL picture:

| COBOL Picture | Precision | Scale | Java Type |
|---------------|-----------|-------|-----------|
| `S9(13)V99` | 15 | 2 | `BigDecimal` |
| `S9(11)V9(4)` | 15 | 4 | `BigDecimal` |
| `S9(13)V9(2)` | 15 | 2 | `BigDecimal` |
| `S9(12)V9(3)` | 15 | 3 | `BigDecimal` |
| `S9(11)V99` | 13 | 2 | `BigDecimal` |
| `S9(4) COMP` | - | - | `Integer` |
| `S9(8) COMP` | - | - | `Integer` |

All `BigDecimal` columns use `DECIMAL(p,s)` in the DDL and the JPA `@Column(precision = p, scale = s)` annotation. This preserves the signed value and fractional precision used by the COBOL programs.

## Date/Time Representation

| COBOL Representation | Java Type | SQL Type | Notes |
|----------------------|-----------|----------|-------|
| `PIC 9(8)` / `PIC X(8)` YYYYMMDD | `LocalDate` | `DATE` | `position_date`, `transaction_date`, `create_date`, etc. |
| `PIC X(6)` HHMMSS | `LocalTime` | `TIME` | `transaction_time`, `trans_time`, `process_time`, `history_time`. |
| `PIC X(26)` ISO timestamp | `LocalDateTime` | `TIMESTAMP` | `last_maint_date`, `process_date`, `audit_timestamp`, `log_timestamp`. |

The ISO `X(26)` timestamps are stored as `TIMESTAMP` (microsecond precision) and mapped with `LocalDateTime` in JPA. The `YYYYMMDD` and `HHMMSS` fields are mapped to `DATE`/`TIME` columns because the semantics are date/time values, even though the COBOL copybooks store them as strings.

## Primary/Secondary Index Choices

### DB2 tables

- `portfolio_master` - PK `portfolio_id`; secondary `idx_port_master_client(client_id, status)`.
- `investment_positions` - PK `(portfolio_id, investment_id, position_date)`; secondary `idx_positions_date(position_date, portfolio_id)`.
- `transaction_history` - PK `transaction_id`; secondaries `idx_trans_hist_port(portfolio_id, transaction_date)` and `idx_trans_hist_date(transaction_date, portfolio_id)`.
- `poshist` - PK `(account_no, portfolio_id, trans_date, trans_time)`; secondaries `poshist_ix1(security_id, trans_date)` and `poshist_ix2(process_date, program_id)`.
- `errlog` - PK `(error_timestamp, program_id)`; secondary `errlog_ix1(process_date, error_severity)`.
- `rtncodes` - PK `(log_timestamp, program_id)`; secondaries `rtncodes_prg_idx(program_id, log_timestamp)` and `rtncodes_sts_idx(status_code, log_timestamp)`.

### VSAM-mapped tables

- `vsam_portmstr` - PK `(portfolio_id, account_no)`; secondaries `idx_vsam_portmstr_client(client_name)` and `idx_vsam_portmstr_status(status)`.
- `vsam_poshist` - PK `(portfolio_id, position_date, investment_id)`; secondaries `idx_vsam_poshist_invest(investment_id, position_date)` and `idx_vsam_poshist_status(status)`.
- `vsam_tranhist` - PK `transaction_id`; secondaries `idx_vsam_tranhist_portfolio(portfolio_id, transaction_date)` and `idx_vsam_tranhist_invest(investment_id, transaction_date)`.

### Notes on Source Inconsistencies

The repository contains multiple, partially inconsistent definitions for the same logical entities (e.g. `POSHIST` in `POSHIST.sql` differs from the VSAM `POSHIST` in `vsam-definitions.txt`). The migration keeps the definitions as separate tables so that each original source is preserved and can be tested independently. The integration test `Phase1DataLayerIntegrationTest` inserts one representative row per table and verifies BigDecimal precision and composite keys.

## Build & Test

```bash
cd java/phase1
./mvnw test
```

The H2 in-memory database is used by default. The Flyway migration `V1__init_schema.sql` creates the schema, and JPA entities validate against it at runtime.
