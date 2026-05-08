# HISTLD00 — Python Migration

This directory contains the Python migration of the COBOL HISTLD00 batch
program (Position History DB2 Loader) found in
[`src/programs/batch/HISTLD00.cbl`](../src/programs/batch/HISTLD00.cbl).

The migration preserves the original program's structure paragraph-for-
paragraph, mirrors the data shapes from the seven COBOL copybooks, and
implements identical behavior for commit checkpointing, duplicate
detection, and error handling.

## Layout

```
python/
├── config.py                    # Runtime configuration (env vars + CLI)
├── requirements.txt             # Python dependencies
├── README.md                    # this file
├── models/                      # Dataclasses mirroring COBOL copybooks
├── common/                      # DB connection, error log, VSAM-like file
├── batch/                       # HistoryLoader (HISTLD00 port) + CLI
├── sql/                         # POSHIST schema DDL
└── tests/                       # Unit, integration, parity, performance
```

## Running the loader

```bash
# Install dependencies
pip install -r python/requirements.txt

# Run with defaults (SQLite + local files)
python -m python.batch.history_loader \
    --db-url sqlite:///poshist.db \
    --tranhist tranhist.db \
    --bchctl bchctl.db \
    --errlog errlog.txt \
    --commit-threshold 1000

# Or via environment variables (mirrors COBOL DD names)
POSHIST_DB_URL=postgresql://user:pw@host:5432/posmvp \
TRANHIST_PATH=/data/tranhist.db \
BCHCTL_PATH=/data/bchctl.db \
ERRLOG_PATH=/var/log/histld00/errlog.txt \
python -m python.batch.history_loader
```

## COBOL ↔ Python mapping

| COBOL element | Python equivalent |
|---|---|
| `HISTLD00.cbl` `0000-MAIN` | `HistoryLoader.run()` |
| `1000-INITIALIZE` | `HistoryLoader.initialize()` |
| `1100-OPEN-FILES` | `HistoryLoader._open_files()` |
| `1200-CONNECT-DB2` | `HistoryLoader._connect_db()` |
| `1300-INIT-CHECKPOINTS` | `HistoryLoader._init_batch_control()` |
| `2000-PROCESS` loop | `HistoryLoader.process_records()` |
| `2100-READ-HISTORY` | `HistoryLoader._history_source()` |
| `2200-LOAD-TO-DB2` | `HistoryLoader.load_to_db()` |
| `2300-CHECK-COMMIT` | `HistoryLoader.check_commit()` |
| `2310-UPDATE-CHECKPOINT` | `HistoryLoader._update_checkpoint()` |
| `3000-TERMINATE` | `HistoryLoader.finalize()` |
| `9000-ERROR-ROUTINE` | `HistoryLoader.error_routine()` |
| `HISTREC.cpy` | `models.history_record.HistoryRecord` + `TransactionHistoryRecord` |
| `BCHCTL.cpy` | `models.batch_control.BatchControlRecord` |
| `BCHCON.cpy` | `models.batch_constants` (enums + module constants) |
| `DBTBLS.cpy` POSHIST-RECORD | `models.poshist_record.PosHistRecord` (+ `PosHist` SQLAlchemy ORM) |
| `DBPROC.cpy` | `common.db_connection.DatabaseConnection` |
| `ERRPROC.cbl` + `ERRHAND.cpy` | `common.error_handler.ErrorHandler` + `models.error_message.ErrorMessage` |
| VSAM KSDS files | `common.vsam_file.VsamFile` (SQLite-backed) |

## Configuration

`HistoryLoaderConfig` (see [`config.py`](config.py)) accepts the following
environment variables:

| Variable | Default | Mirrors |
|---|---|---|
| `POSHIST_DB_URL` | `sqlite:///poshist.db` | `CONNECT TO POSMVP` |
| `TRANHIST_PATH` | `tranhist.db` | DD `TRANHIST` |
| `BCHCTL_PATH` | `bchctl.db` | DD `BCHCTL` |
| `ERRLOG_PATH` | `errlog.txt` | DD `ERRLOG` |
| `COMMIT_THRESHOLD` | `1000` | `WS-COMMIT-THRESHOLD` |
| `MAX_ERRORS` | `100` | `WS-ERROR-COUNT > 100` halt condition |

## COMP-3 / Decimal precision

All financial fields use `decimal.Decimal` and are quantized to match the
COBOL PIC clauses:

| COBOL | Decimal precision |
|---|---|
| `PIC S9(12)V9(3) COMP-3` (quantity, price) | 3 decimal places |
| `PIC S9(13)V9(2) COMP-3` (amount, fees, total, cost-basis, gain-loss) | 2 decimal places |

## Testing

See [`tests/README.md`](tests/README.md). In short:

```bash
cd /home/ubuntu/repos/COBOL-Legacy-Benchmark-Suite
pytest python/tests
```
