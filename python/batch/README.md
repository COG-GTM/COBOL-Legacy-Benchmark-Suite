# `python/batch/`

Migration of the COBOL batch programs from
[`src/programs/batch/`](../../src/programs/batch/).

Currently implemented:

* `history_loader.py` — port of [`HISTLD00.cbl`](../../src/programs/batch/HISTLD00.cbl).

## `HistoryLoader`

`HistoryLoader` is a one-to-one port of the COBOL paragraph structure. The
class accepts:

* `config` — a `HistoryLoaderConfig` from [`python/config.py`](../config.py).
* `history_records` — optional iterable of `TransactionHistoryRecord` for
  in-memory / library use (skips opening the VSAM file).
* `db` — optional pre-built `DatabaseConnection` (used by tests with shared
  in-memory engines).
* `error_handler` — optional pre-built `ErrorHandler`.
* `batch_control` — optional initial `BatchControlRecord`.

### Field mapping (`2200-LOAD-TO-DB2`)

| Source `TH-*` | Destination `PH-*` | Notes |
|---|---|---|
| `TH-ACCOUNT-NO` | `PH-ACCOUNT-NO` | `PIC X(8)` |
| `TH-PORTFOLIO-ID` | `PH-PORTFOLIO-ID` | `PIC X(10)` |
| `TH-TRANS-DATE` | `PH-TRANS-DATE` | `YYYY-MM-DD` |
| `TH-TRANS-TIME` | `PH-TRANS-TIME` | `HH:MM:SS` |
| `TH-TRANS-TYPE` | `PH-TRANS-TYPE` | `PIC X(2)` |
| `TH-SECURITY-ID` | `PH-SECURITY-ID` | `PIC X(12)` |
| `TH-QUANTITY` | `PH-QUANTITY` | Decimal (3 dp) |
| `TH-PRICE` | `PH-PRICE` | Decimal (3 dp) |
| `TH-AMOUNT` | `PH-AMOUNT` | Decimal (2 dp) |
| `TH-FEES` | `PH-FEES` | Decimal (2 dp) |
| `TH-TOTAL-AMOUNT` | `PH-TOTAL-AMOUNT` | Decimal (2 dp) |
| `TH-COST-BASIS` | `PH-COST-BASIS` | Decimal (2 dp) |
| `TH-GAIN-LOSS` | `PH-GAIN-LOSS` | Decimal (2 dp) |

`PH-PROCESS-DATE`, `PH-PROCESS-TIME`, `PH-PROGRAM-ID`, `PH-USER-ID`, and
`PH-AUDIT-TIMESTAMP` are populated automatically by the loader.

### SQLCODE handling

| SQLCODE | Meaning | Loader behavior |
|---|---|---|
| 0 | Success | Increment `records_written` |
| -803 | Duplicate primary key | Increment `duplicates_skipped`, rollback row, continue |
| Other | DB error | Increment `error_count`, log via `ErrorHandler`, rollback |

The `IntegrityError` raised by SQLAlchemy is treated as the SQLCODE -803
equivalent, matching the COBOL `IF SQLCODE = -803 THEN CONTINUE` branch.

### Commit / checkpoint behavior

The loader increments an internal counter on every successful record. When
the counter reaches `WS-COMMIT-THRESHOLD` (default 1000) it commits the
SQLAlchemy connection, resets the counter, and rewrites the
`BATCH-CONTROL-RECORD` with the latest `BCT-RECORDS-READ` /
`BCT-RECORDS-WRITTEN` counters. A final `3100-FINAL-COMMIT` always runs in
`finalize()`.

### Error / abort behavior

`HistoryLoader.error_routine()` writes one `ERROR-LOG` record (via
`ErrorHandler`) and issues a rollback. The main loop also breaks early
once `error_count` exceeds `max_errors` (default 100), matching the COBOL
`UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100` clause.

## CLI usage

`python -m python.batch.history_loader --help` shows all flags. The CLI
exit code is the COBOL-style `RETURN-CODE` (0 = success, 8 = error, etc.).
