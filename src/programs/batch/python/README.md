# Python ports of batch programs

This directory holds Python implementations of the COBOL batch programs in
`../`. Each module is a functional equivalent of its COBOL counterpart and
is intended to support the modernization benchmark by providing a parallel
implementation downstream tooling can compare against.

## `histld00.py` — Position History DB2 Load

`histld00.py` is the Python port of [`HISTLD00.cbl`](../HISTLD00.cbl), the
high-volume ETL job that loads transaction history records into the DB2
`POSHIST` table.

### Pipeline role

Driven by `BCHCTL00`, the COBOL pipeline runs `HISTLD00` after `POSUPD00`
succeeds with `RC ≤ 4`. Downstream reporting jobs (`RPTPOS00`, `RPTAUD00`,
`RPTSTA00`) read the rows it loads:

```
POSUPD00  ─►  HISTLD00 / histld00.py  ─►  RPTPOS00, RPTAUD00, RPTSTA00
```

The Python port preserves the same return-code contract (`RC = error
count`, capped at 255 for POSIX) so it can drop into the existing pipeline.

### What it does

1. Opens a fixed-width input file (the `TRANHIST` data set) whose layout is
   defined by [`INPUT_RECORD_SCHEMA`](histld00.py).
2. Connects to the target database. DB2 (`ibm_db`) is the default;
   PostgreSQL (`psycopg2`) and SQLite (`sqlite3`) are also supported for
   development/testing.
3. Maps the `TH-*` source fields to the `POSHIST` (`PH-*`) columns and runs
   an `INSERT` per record (mirrors paragraph `2200-LOAD-TO-DB2`).
4. Suppresses duplicate-key violations (DB2 `SQLCODE -803` /
   `SQLSTATE 23505`), counts everything else as a hard error, and aborts
   after 100 errors (mirrors `WS-ERROR-COUNT > 100`).
5. Commits every 1000 successful inserts (mirrors `WS-COMMIT-THRESHOLD`)
   and updates a JSON checkpoint file at the same cadence, with a
   configurable minimum interval between writes (default 2 minutes).

### Configuration

All knobs are available via environment variables and matching CLI flags:

| Env var                  | CLI flag                | Description                                                    |
|--------------------------|-------------------------|----------------------------------------------------------------|
| `HISTLD00_INPUT_FILE`    | `--input`               | Path to the `TRANHIST` fixed-width input file. **Required.**   |
| `HISTLD00_CHECKPOINT`    | `--checkpoint`          | Path to the checkpoint JSON file.                              |
| `HISTLD00_DB_DRIVER`     | `--db-driver`           | `db2` (default), `postgres`, or `sqlite`.                      |
| `HISTLD00_DB_DSN`        | `--db-dsn`              | Driver-specific connection string. **Required.**               |
| `HISTLD00_DB_USER`       | `--db-user`             | Database user (driver-dependent).                              |
| `HISTLD00_DB_PASSWORD`   | `--db-password`         | Database password (driver-dependent).                          |
| `HISTLD00_USER_ID`       | `--user-id`             | Value to write into `POSHIST.USER_ID`.                         |
| `HISTLD00_COMMIT_EVERY`  | `--commit-every`        | Commit every N successful inserts (default 1000).              |
| `HISTLD00_MAX_ERRORS`    | `--max-errors`          | Abort after this many errors (default 100).                    |
| `HISTLD00_MIN_CKPT_SECS` | `--min-checkpoint-secs` | Minimum seconds between checkpoint writes (default 120).       |
| `HISTLD00_LOG_LEVEL`     | `--log-level`           | Python `logging` level (default `INFO`).                       |

### Running

```bash
# DB2 (production drop-in)
HISTLD00_DB_DRIVER=db2 \
HISTLD00_DB_DSN='DATABASE=POSMVP;HOSTNAME=db2host;PORT=50000;PROTOCOL=TCPIP' \
HISTLD00_DB_USER=posapp \
HISTLD00_DB_PASSWORD=...$secret$... \
python -m src.programs.batch.python.histld00 \
    --input /var/spool/tranhist.dat \
    --checkpoint /var/run/histld00.ckpt

# Local SQLite (development / smoke testing)
python -m src.programs.batch.python.histld00 \
    --input ./samples/tranhist.dat \
    --db-driver sqlite \
    --db-dsn ./posmvp.db \
    --checkpoint ./histld00.ckpt
```

### Return codes

| RC      | Meaning                                                                |
|---------|------------------------------------------------------------------------|
| `0`     | Success — every record loaded (or skipped as a duplicate).             |
| `1–4`   | Warnings — `BCHCTL00` still proceeds to the reports.                   |
| `5–255` | Errors — `BCHCTL00` halts the pipeline.                                |

### Checkpoint / restart behavior

The job persists `records_read` / `records_written` / `error_count` to the
checkpoint file every commit window. On restart, the file pointer seeks
forward by `records_read × INPUT_RECORD_LENGTH` bytes so the previously
processed records are skipped. The COBOL contract is preserved:

* checkpoints are written after each commit, but no more often than the
  configured minimum interval (default 2 minutes), and
* a final checkpoint is always force-written from `3100-FINAL-COMMIT`.

### Functional equivalence

The Python port preserves:

* the `TH-* → PH-*` field-to-column mapping (see `_to_db_row`);
* duplicate-key suppression (`SQLCODE -803` / `SQLSTATE 23505`);
* the 1000-record commit cadence;
* the 100-error abort threshold;
* the `RC = error_count` semantics consumed by `BCHCTL00`.

A SQLite shadow DDL is provided in
[`SQLITE_POSHIST_DDL`](histld00.py) for local development; production
deployments rely on [`POSHIST.sql`](../../../database/db2/POSHIST.sql).

### Tests

A small in-process smoke test that exercises the main flow against an
in-memory SQLite database lives at
[`test_histld00.py`](test_histld00.py). Run it with:

```bash
python -m unittest src.programs.batch.python.test_histld00
```
