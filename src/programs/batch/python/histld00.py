"""HISTLD00 — Position History DB2 Load (Python port of HISTLD00.cbl).

Overview
--------
This module is a functional Python port of ``src/programs/batch/HISTLD00.cbl``.
It performs a high-volume ETL of transaction history records into the DB2
``POSHIST`` table, mirroring the COBOL program's record flow, error handling,
checkpoint/restart behavior, and return code semantics.

Pipeline role
-------------
The COBOL pipeline (driven by ``BCHCTL00``) runs ``HISTLD00`` after
``POSUPD00`` succeeds with ``RC <= 4``. Downstream reporting programs
(``RPTPOS00``, ``RPTAUD00``, ``RPTSTA00``) read the rows this job loads.
This Python implementation preserves the same RC contract so it can drop into
that pipeline:

    POSUPD00 -> [HISTLD00 / histld00.py] -> RPTPOS00 / RPTAUD00 / RPTSTA00

What it does
------------
1. Opens the transaction history input file (fixed-width record layout).
2. Connects to the target database (DB2 by default, with adapters for
   PostgreSQL and SQLite for development / testing).
3. Reads each input record, maps the ``TH-*`` source fields to the
   ``POSHIST`` (``PH-*``) columns and issues an INSERT.
4. Skips duplicate-key violations silently (DB2 SQLCODE -803 / SQLSTATE
   23505), counts other errors, and aborts after 100 errors.
5. Commits and updates the checkpoint every 1000 successfully processed
   records, with a configurable minimum interval between checkpoints.
6. Exits with a return code equal to the error count, capped at 255 to fit
   POSIX exit conventions; ``BCHCTL00`` interprets ``RC <= 4`` as
   "continue to reports".

Configuration
-------------
All configuration is done via environment variables (or CLI flags):

    HISTLD00_INPUT_FILE     Path to the TRANHIST input file (required).
    HISTLD00_CHECKPOINT     Path to the checkpoint JSON file
                            (default: ``./histld00.checkpoint.json``).
    HISTLD00_DB_DRIVER      One of ``db2`` | ``postgres`` | ``sqlite``
                            (default: ``db2``).
    HISTLD00_DB_DSN         Driver-specific connection string. For DB2 a
                            full DSN like
                            ``DATABASE=POSMVP;HOSTNAME=...;PORT=50000;...``
                            For PostgreSQL a libpq URI / DSN. For SQLite
                            a filesystem path.
    HISTLD00_DB_USER        Optional, used by some drivers.
    HISTLD00_DB_PASSWORD    Optional, used by some drivers.
    HISTLD00_USER_ID        Value to write into POSHIST.USER_ID
                            (default: current OS user, truncated to 8).
    HISTLD00_COMMIT_EVERY   Records per commit (default: 1000, matches COBOL
                            ``WS-COMMIT-THRESHOLD``).
    HISTLD00_MAX_ERRORS     Abort threshold (default: 100, matches COBOL
                            ``WS-ERROR-COUNT > 100``).
    HISTLD00_MIN_CKPT_SECS  Minimum seconds between checkpoint writes
                            (default: 120, matches the 2-minute minimum).

Running
-------
::

    python -m src.programs.batch.python.histld00 \\
        --input /path/to/tranhist.dat \\
        --checkpoint /var/run/histld00.ckpt \\
        --db-driver sqlite --db-dsn /tmp/posmvp.db

The program exits with the same return code semantics as the COBOL job:

    0      success
    1-4    warnings (non-zero error count, BCHCTL00 still proceeds)
    5-255  errors (BCHCTL00 halts the pipeline)

Restart behavior
----------------
On startup the program reads the checkpoint file (if present) and skips
ahead to the recorded ``records_read`` position before resuming. After each
commit window (``HISTLD00_COMMIT_EVERY`` records) the checkpoint is
rewritten atomically. A failed run can therefore be relaunched and will
pick up where it left off, matching the COBOL ``2310-UPDATE-CHECKPOINT``
contract.

Functional equivalence
----------------------
The Python version preserves:
  * the same field-to-column mapping (see ``POSHISTRow`` / ``_to_db_row``);
  * the same duplicate-key suppression (SQLCODE -803 / SQLSTATE 23505);
  * the same commit cadence (every 1000 records);
  * the same checkpoint side-effects (records_read, records_written);
  * the same termination on ``error_count > 100``;
  * the same RC contract consumed by ``BCHCTL00``.
"""

from __future__ import annotations

import argparse
import dataclasses
import datetime as dt
import getpass
import json
import logging
import os
import sys
import time
from decimal import Decimal
from pathlib import Path
from typing import Any, BinaryIO, Iterator, Optional

logger = logging.getLogger("histld00")

PROGRAM_ID = "HISTLD00"
DEFAULT_COMMIT_THRESHOLD = 1000
DEFAULT_MAX_ERROR_COUNT = 100
DEFAULT_MIN_CHECKPOINT_INTERVAL_SECONDS = 120

# DB2 SQLCODE / SQLSTATE for duplicate-key violations.
DUPLICATE_KEY_SQLCODE = -803
DUPLICATE_KEY_SQLSTATE = "23505"

# Return code thresholds, matching ERRHAND.cpy / BCHCON.cpy.
RC_SUCCESS = 0
RC_WARNING = 4
RC_ERROR = 8
RC_SEVERE = 12

# ---------------------------------------------------------------------------
# Data model — POSHIST table
# ---------------------------------------------------------------------------
#
# DDL (see src/database/db2/POSHIST.sql):
#
#   CREATE TABLE POSHIST (
#       ACCOUNT_NO       CHAR(8)        NOT NULL,
#       PORTFOLIO_ID     CHAR(10)       NOT NULL,
#       TRANS_DATE       DATE           NOT NULL,
#       TRANS_TIME       TIME           NOT NULL,
#       TRANS_TYPE       CHAR(2)        NOT NULL,
#       SECURITY_ID      CHAR(12)       NOT NULL,
#       QUANTITY         DECIMAL(15,3)  NOT NULL,
#       PRICE            DECIMAL(15,3)  NOT NULL,
#       AMOUNT           DECIMAL(15,2)  NOT NULL,
#       FEES             DECIMAL(15,2)  NOT NULL WITH DEFAULT 0,
#       TOTAL_AMOUNT     DECIMAL(15,2)  NOT NULL,
#       COST_BASIS       DECIMAL(15,2)  NOT NULL,
#       GAIN_LOSS        DECIMAL(15,2)  NOT NULL,
#       PROCESS_DATE     DATE           NOT NULL,
#       PROCESS_TIME     TIME           NOT NULL,
#       PROGRAM_ID       CHAR(8)        NOT NULL,
#       USER_ID          CHAR(8)        NOT NULL,
#       AUDIT_TIMESTAMP  TIMESTAMP      NOT NULL WITH DEFAULT,
#       PRIMARY KEY (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
#   );


@dataclasses.dataclass(frozen=True)
class FieldSpec:
    """One field in the fixed-width input record layout."""

    name: str
    offset: int  # 0-indexed byte offset in the record
    width: int   # field width in bytes
    kind: str    # "char" or "decimal"
    scale: int = 0  # implied decimal places for ``kind == "decimal"``


# Input record layout. Widths match the ``PH-*`` field widths in
# ``src/copybook/db2/DBTBLS.cpy`` so that COBOL ``MOVE TH-* TO PH-*`` is a
# straight copy. Numeric fields are encoded as a 1-char sign plus 15 digits,
# with the implied decimal point indicated by ``scale``.
INPUT_RECORD_SCHEMA: tuple[FieldSpec, ...] = (
    FieldSpec("th_account_no",    0,    8, "char"),
    FieldSpec("th_portfolio_id",  8,   10, "char"),
    FieldSpec("th_trans_date",    18,  10, "char"),
    FieldSpec("th_trans_time",    28,   8, "char"),
    FieldSpec("th_trans_type",    36,   2, "char"),
    FieldSpec("th_security_id",   38,  12, "char"),
    FieldSpec("th_quantity",      50,  16, "decimal", scale=3),
    FieldSpec("th_price",         66,  16, "decimal", scale=3),
    FieldSpec("th_amount",        82,  16, "decimal", scale=2),
    FieldSpec("th_fees",          98,  16, "decimal", scale=2),
    FieldSpec("th_total_amount", 114,  16, "decimal", scale=2),
    FieldSpec("th_cost_basis",   130,  16, "decimal", scale=2),
    FieldSpec("th_gain_loss",    146,  16, "decimal", scale=2),
)

INPUT_RECORD_LENGTH = sum(f.width for f in INPUT_RECORD_SCHEMA)


@dataclasses.dataclass
class TransactionHistoryRecord:
    """In-memory representation of one TRANHIST input record (TH-* fields)."""

    th_account_no: str
    th_portfolio_id: str
    th_trans_date: str   # "YYYY-MM-DD"
    th_trans_time: str   # "HH:MM:SS"
    th_trans_type: str
    th_security_id: str
    th_quantity: Decimal
    th_price: Decimal
    th_amount: Decimal
    th_fees: Decimal
    th_total_amount: Decimal
    th_cost_basis: Decimal
    th_gain_loss: Decimal


@dataclasses.dataclass
class POSHISTRow:
    """In-memory representation of a row destined for the POSHIST table."""

    account_no: str
    portfolio_id: str
    trans_date: dt.date
    trans_time: dt.time
    trans_type: str
    security_id: str
    quantity: Decimal
    price: Decimal
    amount: Decimal
    fees: Decimal
    total_amount: Decimal
    cost_basis: Decimal
    gain_loss: Decimal
    process_date: dt.date
    process_time: dt.time
    program_id: str
    user_id: str
    audit_timestamp: dt.datetime


# ---------------------------------------------------------------------------
# Input record parsing
# ---------------------------------------------------------------------------


def _parse_decimal(raw: bytes, scale: int) -> Decimal:
    """Parse a fixed-width signed numeric field of width 16.

    Layout: 1 sign byte (``'+'`` / ``'-'`` / ``' '``) followed by 15 digit
    bytes. The implied decimal point is positioned ``scale`` digits from the
    right. Leading zeros and trailing whitespace are tolerated.
    """
    text = raw.decode("ascii", errors="replace").strip()
    if not text:
        return Decimal(0)
    sign = "-" if text[0] == "-" else ""
    digits = text.lstrip("+- ").lstrip("0") or "0"
    if scale == 0:
        return Decimal(f"{sign}{digits}")
    if len(digits) <= scale:
        digits = digits.zfill(scale + 1)
    int_part = digits[:-scale]
    frac_part = digits[-scale:]
    return Decimal(f"{sign}{int_part}.{frac_part}")


def parse_record(buf: bytes) -> TransactionHistoryRecord:
    """Parse one ``INPUT_RECORD_LENGTH``-byte record into a dataclass."""
    if len(buf) < INPUT_RECORD_LENGTH:
        raise ValueError(
            f"Short record: expected {INPUT_RECORD_LENGTH} bytes, "
            f"got {len(buf)}"
        )
    fields: dict[str, Any] = {}
    for spec in INPUT_RECORD_SCHEMA:
        chunk = buf[spec.offset : spec.offset + spec.width]
        if spec.kind == "char":
            fields[spec.name] = chunk.decode("ascii", errors="replace").rstrip()
        else:  # decimal
            fields[spec.name] = _parse_decimal(chunk, spec.scale)
    return TransactionHistoryRecord(**fields)


def encode_record(rec: TransactionHistoryRecord) -> bytes:
    """Serialize a record back to the fixed-width on-disk format.

    Useful for tests and tooling that produce sample input files.
    """
    parts: list[bytes] = []
    for spec in INPUT_RECORD_SCHEMA:
        value = getattr(rec, spec.name)
        if spec.kind == "char":
            text = str(value).ljust(spec.width)[: spec.width]
            parts.append(text.encode("ascii"))
        else:
            scaled = (Decimal(value) * (Decimal(10) ** spec.scale)).quantize(
                Decimal(1)
            )
            sign = "-" if scaled < 0 else "+"
            digits = str(abs(int(scaled))).zfill(spec.width - 1)
            parts.append(f"{sign}{digits}".encode("ascii"))
    return b"".join(parts)


def iter_records(stream: BinaryIO) -> Iterator[TransactionHistoryRecord]:
    """Yield ``TransactionHistoryRecord`` instances from a fixed-width stream."""
    while True:
        buf = stream.read(INPUT_RECORD_LENGTH)
        if not buf:
            return
        if len(buf) < INPUT_RECORD_LENGTH:
            logger.warning(
                "Trailing %d-byte fragment ignored at end of input file",
                len(buf),
            )
            return
        yield parse_record(buf)


# ---------------------------------------------------------------------------
# TH -> PH mapping (mirrors COBOL paragraph 2200-LOAD-TO-DB2)
# ---------------------------------------------------------------------------


def _to_db_row(
    th: TransactionHistoryRecord,
    *,
    program_id: str,
    user_id: str,
    now: dt.datetime,
) -> POSHISTRow:
    """Map a ``TH-*`` input record to the corresponding ``POSHIST`` row."""
    return POSHISTRow(
        account_no=th.th_account_no,
        portfolio_id=th.th_portfolio_id,
        trans_date=dt.date.fromisoformat(th.th_trans_date),
        trans_time=dt.time.fromisoformat(th.th_trans_time),
        trans_type=th.th_trans_type,
        security_id=th.th_security_id,
        quantity=th.th_quantity,
        price=th.th_price,
        amount=th.th_amount,
        fees=th.th_fees,
        total_amount=th.th_total_amount,
        cost_basis=th.th_cost_basis,
        gain_loss=th.th_gain_loss,
        process_date=now.date(),
        process_time=now.time().replace(microsecond=0),
        program_id=program_id,
        user_id=user_id,
        audit_timestamp=now,
    )


# ---------------------------------------------------------------------------
# Database layer
# ---------------------------------------------------------------------------

INSERT_COLUMNS = (
    "ACCOUNT_NO",
    "PORTFOLIO_ID",
    "TRANS_DATE",
    "TRANS_TIME",
    "TRANS_TYPE",
    "SECURITY_ID",
    "QUANTITY",
    "PRICE",
    "AMOUNT",
    "FEES",
    "TOTAL_AMOUNT",
    "COST_BASIS",
    "GAIN_LOSS",
    "PROCESS_DATE",
    "PROCESS_TIME",
    "PROGRAM_ID",
    "USER_ID",
    "AUDIT_TIMESTAMP",
)


def _row_values(row: POSHISTRow) -> tuple[Any, ...]:
    return (
        row.account_no,
        row.portfolio_id,
        row.trans_date,
        row.trans_time,
        row.trans_type,
        row.security_id,
        row.quantity,
        row.price,
        row.amount,
        row.fees,
        row.total_amount,
        row.cost_basis,
        row.gain_loss,
        row.process_date,
        row.process_time,
        row.program_id,
        row.user_id,
        row.audit_timestamp,
    )


class HistoryLoader:
    """Thin DB-API 2.0 wrapper that performs the ``POSHIST`` INSERTs.

    The class is intentionally driver-agnostic: it accepts any PEP 249
    connection along with the ``DatabaseError`` / ``IntegrityError`` classes
    its driver exposes. The duplicate-key check first looks at the standard
    ``IntegrityError`` mechanism (raised by ibm_db_dbi, psycopg2 and sqlite3
    on unique-constraint violations), and falls back to inspecting any
    SQLCODE / SQLSTATE the driver attaches to the exception.
    """

    def __init__(
        self,
        conn: Any,
        *,
        paramstyle: str = "qmark",
        integrity_error: type[BaseException] | None = None,
        database_error: type[BaseException] | None = None,
    ) -> None:
        self._conn = conn
        self._cursor = conn.cursor()
        self._integrity_error = integrity_error or _resolve_exc(
            conn, "IntegrityError"
        )
        self._database_error = database_error or _resolve_exc(
            conn, "DatabaseError"
        )
        placeholders = (
            "?" if paramstyle in ("qmark", "numeric", "named") else "%s"
        )
        cols = ", ".join(INSERT_COLUMNS)
        marks = ", ".join([placeholders] * len(INSERT_COLUMNS))
        self._sql = f"INSERT INTO POSHIST ({cols}) VALUES ({marks})"

    def insert(self, row: POSHISTRow) -> str:
        """Insert one row.

        Returns ``"ok"`` on success, ``"duplicate"`` if a unique-constraint
        violation was suppressed, or ``"error"`` if the driver raised any
        other database error (in which case the exception is re-raised after
        the caller has had a chance to log).
        """
        try:
            self._cursor.execute(self._sql, _row_values(row))
        except self._integrity_error as exc:  # type: ignore[misc]
            if _is_duplicate_key(exc):
                return "duplicate"
            raise
        except self._database_error as exc:  # type: ignore[misc]
            if _is_duplicate_key(exc):
                return "duplicate"
            raise
        return "ok"

    def commit(self) -> None:
        self._conn.commit()

    def rollback(self) -> None:
        try:
            self._conn.rollback()
        except Exception:  # pragma: no cover - defensive
            logger.exception("rollback failed")

    def close(self) -> None:
        try:
            self._cursor.close()
        finally:
            try:
                self._conn.close()
            except Exception:  # pragma: no cover - defensive
                logger.exception("connection close failed")


def _resolve_exc(conn: Any, name: str) -> type[BaseException]:
    """Best-effort lookup of a PEP 249 exception class on ``conn``'s module."""
    module_name = type(conn).__module__
    module = sys.modules.get(module_name)
    if module is not None and hasattr(module, name):
        return getattr(module, name)
    base = sys.modules.get(module_name.split(".")[0])
    if base is not None and hasattr(base, name):
        return getattr(base, name)
    return Exception  # type: ignore[return-value]


def _is_duplicate_key(exc: BaseException) -> bool:
    """Return True if ``exc`` represents a duplicate-key (SQLCODE -803) error."""
    sqlcode = getattr(exc, "sqlcode", None)
    if sqlcode is not None:
        try:
            if int(sqlcode) == DUPLICATE_KEY_SQLCODE:
                return True
        except (TypeError, ValueError):  # pragma: no cover - defensive
            pass
    sqlstate = getattr(exc, "sqlstate", None) or getattr(exc, "pgcode", None)
    if sqlstate and str(sqlstate) == DUPLICATE_KEY_SQLSTATE:
        return True
    text = str(exc).lower()
    if "sqlcode=-803" in text or "sqlcode: -803" in text:
        return True
    if "sqlstate=23505" in text:
        return True
    if "unique constraint failed" in text or "duplicate key" in text:
        return True
    return False


# ---------------------------------------------------------------------------
# Database connection helpers
# ---------------------------------------------------------------------------


def connect_database(
    driver: str,
    *,
    dsn: str,
    user: Optional[str] = None,
    password: Optional[str] = None,
) -> tuple[Any, str]:
    """Open a DB-API 2.0 connection. Returns ``(conn, paramstyle)``."""
    driver = driver.lower()
    if driver == "db2":
        import ibm_db  # type: ignore[import-not-found]
        import ibm_db_dbi  # type: ignore[import-not-found]

        full_dsn = dsn
        if user and "UID=" not in full_dsn.upper():
            full_dsn = f"{full_dsn};UID={user}"
        if password and "PWD=" not in full_dsn.upper():
            full_dsn = f"{full_dsn};PWD={password}"
        raw = ibm_db.connect(full_dsn, "", "")
        conn = ibm_db_dbi.Connection(raw)
        return conn, "qmark"
    if driver in ("postgres", "postgresql"):
        import psycopg2  # type: ignore[import-not-found]

        kwargs: dict[str, Any] = {"dsn": dsn}
        if user:
            kwargs["user"] = user
        if password:
            kwargs["password"] = password
        conn = psycopg2.connect(**kwargs)
        return conn, "format"
    if driver == "sqlite":
        import sqlite3

        _register_sqlite_adapters()
        conn = sqlite3.connect(dsn)
        return conn, "qmark"
    raise ValueError(f"Unsupported db driver: {driver!r}")


def _register_sqlite_adapters() -> None:
    """Register adapters for sqlite3 once.

    sqlite3 in Python 3.12+ no longer ships default adapters for
    ``datetime.date`` / ``datetime.time`` / ``datetime.datetime`` and never
    had one for :class:`decimal.Decimal`. Registering them up front means
    the loader can pass native Python types straight to ``cursor.execute``
    regardless of the active driver.
    """
    import sqlite3

    sqlite3.register_adapter(Decimal, lambda d: str(d))
    sqlite3.register_adapter(dt.date, lambda d: d.isoformat())
    sqlite3.register_adapter(dt.time, lambda t: t.isoformat())
    sqlite3.register_adapter(
        dt.datetime, lambda x: x.isoformat(sep=" ", timespec="microseconds")
    )


# Register sqlite adapters at module import so callers that build their own
# connection (e.g. tests, downstream tooling) get the same coercion
# behavior as ``connect_database("sqlite", ...)``.
_register_sqlite_adapters()


def _utc_now() -> dt.datetime:
    """UTC "now" as a naive ``datetime`` (matches DB2 TIMESTAMP semantics)."""
    return dt.datetime.now(dt.timezone.utc).replace(tzinfo=None)


# DDL for the SQLite shadow of POSHIST. Used by tests / local development
# only — real deployments use src/database/db2/POSHIST.sql.
SQLITE_POSHIST_DDL = """
CREATE TABLE IF NOT EXISTS POSHIST (
    ACCOUNT_NO       TEXT    NOT NULL,
    PORTFOLIO_ID     TEXT    NOT NULL,
    TRANS_DATE       TEXT    NOT NULL,
    TRANS_TIME       TEXT    NOT NULL,
    TRANS_TYPE       TEXT    NOT NULL,
    SECURITY_ID      TEXT    NOT NULL,
    QUANTITY         NUMERIC NOT NULL,
    PRICE            NUMERIC NOT NULL,
    AMOUNT           NUMERIC NOT NULL,
    FEES             NUMERIC NOT NULL DEFAULT 0,
    TOTAL_AMOUNT     NUMERIC NOT NULL,
    COST_BASIS       NUMERIC NOT NULL,
    GAIN_LOSS        NUMERIC NOT NULL,
    PROCESS_DATE     TEXT    NOT NULL,
    PROCESS_TIME     TEXT    NOT NULL,
    PROGRAM_ID       TEXT    NOT NULL,
    USER_ID          TEXT    NOT NULL,
    AUDIT_TIMESTAMP  TEXT    NOT NULL,
    PRIMARY KEY (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
)
"""


# ---------------------------------------------------------------------------
# Checkpoint manager
# ---------------------------------------------------------------------------


@dataclasses.dataclass
class CheckpointState:
    """Persistent state mirrored from BCT-RECORDS-READ / BCT-RECORDS-WRITTEN."""

    job_name: str = PROGRAM_ID
    records_read: int = 0
    records_written: int = 0
    error_count: int = 0
    last_update: Optional[str] = None  # ISO timestamp of last write


class CheckpointManager:
    """Persists ``CheckpointState`` to a JSON file with atomic rewrites.

    Mirrors paragraphs 1300-INIT-CHECKPOINTS and 2310-UPDATE-CHECKPOINT in
    the COBOL: an existing checkpoint indicates a restart, in which case the
    main loop seeks past records already loaded.
    """

    def __init__(self, path: Path, *, min_interval_seconds: int) -> None:
        self.path = path
        self.min_interval_seconds = min_interval_seconds
        self._last_write_monotonic: Optional[float] = None

    def load(self) -> CheckpointState:
        if not self.path.exists():
            return CheckpointState()
        try:
            data = json.loads(self.path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            logger.warning(
                "Could not read checkpoint %s (%s); starting from scratch",
                self.path,
                exc,
            )
            return CheckpointState()
        return CheckpointState(
            job_name=data.get("job_name", PROGRAM_ID),
            records_read=int(data.get("records_read", 0)),
            records_written=int(data.get("records_written", 0)),
            error_count=int(data.get("error_count", 0)),
            last_update=data.get("last_update"),
        )

    def write(self, state: CheckpointState, *, force: bool = False) -> bool:
        """Atomically persist ``state``.

        Honors ``min_interval_seconds`` unless ``force=True``. Returns True
        if a write actually happened.
        """
        now = time.monotonic()
        if (
            not force
            and self._last_write_monotonic is not None
            and (now - self._last_write_monotonic) < self.min_interval_seconds
        ):
            return False
        state = dataclasses.replace(
            state, last_update=_utc_now().isoformat(timespec="seconds")
        )
        tmp = self.path.with_suffix(self.path.suffix + ".tmp")
        tmp.parent.mkdir(parents=True, exist_ok=True)
        tmp.write_text(json.dumps(dataclasses.asdict(state)), encoding="utf-8")
        os.replace(tmp, self.path)
        self._last_write_monotonic = now
        return True


# ---------------------------------------------------------------------------
# Job orchestrator
# ---------------------------------------------------------------------------


@dataclasses.dataclass
class JobConfig:
    input_file: Path
    checkpoint_file: Path
    db_driver: str
    db_dsn: str
    db_user: Optional[str] = None
    db_password: Optional[str] = None
    user_id: str = ""
    commit_threshold: int = DEFAULT_COMMIT_THRESHOLD
    max_error_count: int = DEFAULT_MAX_ERROR_COUNT
    min_checkpoint_interval_seconds: int = DEFAULT_MIN_CHECKPOINT_INTERVAL_SECONDS


@dataclasses.dataclass
class JobStats:
    """Mirrors WS-COUNTERS in the COBOL working storage."""

    records_read: int = 0
    records_written: int = 0
    duplicate_count: int = 0
    error_count: int = 0


class HISTLD00Job:
    """Top-level orchestrator. Runs the same flow as PROCEDURE DIVISION."""

    def __init__(
        self,
        config: JobConfig,
        *,
        loader: Optional[HistoryLoader] = None,
        checkpoint: Optional[CheckpointManager] = None,
    ) -> None:
        self.config = config
        self._loader = loader
        self._checkpoint = checkpoint or CheckpointManager(
            config.checkpoint_file,
            min_interval_seconds=config.min_checkpoint_interval_seconds,
        )
        self.stats = JobStats()

    # 1000-INITIALIZE
    def _initialize(self) -> tuple[HistoryLoader, CheckpointState]:
        state = self._checkpoint.load()
        if state.records_read:
            logger.info(
                "Restarting from checkpoint at record %d (%d already written)",
                state.records_read,
                state.records_written,
            )
        if self._loader is None:
            conn, paramstyle = connect_database(
                self.config.db_driver,
                dsn=self.config.db_dsn,
                user=self.config.db_user,
                password=self.config.db_password,
            )
            loader = HistoryLoader(conn, paramstyle=paramstyle)
        else:
            loader = self._loader
        # Seed running stats from the checkpoint so the next checkpoint
        # writes monotonic counters.
        self.stats.records_read = state.records_read
        self.stats.records_written = state.records_written
        self.stats.error_count = state.error_count
        return loader, state

    # 2000-PROCESS / 2200-LOAD-TO-DB2 / 2300-CHECK-COMMIT
    def _process(self, loader: HistoryLoader, state: CheckpointState) -> None:
        commit_count = 0
        skip_remaining = state.records_read
        with self.config.input_file.open("rb") as stream:
            # Restart support: skip records already processed.
            if skip_remaining:
                stream.seek(skip_remaining * INPUT_RECORD_LENGTH)
            for record in iter_records(stream):
                if self.stats.error_count > self.config.max_error_count:
                    logger.error(
                        "Aborting: error count %d exceeded max %d",
                        self.stats.error_count,
                        self.config.max_error_count,
                    )
                    break
                self.stats.records_read += 1
                outcome = self._load_one(loader, record)
                if outcome == "ok":
                    self.stats.records_written += 1
                    commit_count += 1
                elif outcome == "duplicate":
                    self.stats.duplicate_count += 1
                    commit_count += 1
                else:  # "error"
                    self.stats.error_count += 1

                if commit_count >= self.config.commit_threshold:
                    loader.commit()
                    commit_count = 0
                    self._checkpoint.write(self._snapshot_state())

    def _load_one(self, loader: HistoryLoader, record: TransactionHistoryRecord) -> str:
        now = _utc_now()
        try:
            row = _to_db_row(
                record,
                program_id=PROGRAM_ID,
                user_id=self.config.user_id[:8],
                now=now,
            )
        except (ValueError, TypeError) as exc:
            logger.error(
                "Skipping malformed record %d: %s", self.stats.records_read + 1, exc
            )
            return "error"
        try:
            return loader.insert(row)
        except Exception as exc:  # noqa: BLE001 - mirror COBOL DB2-ERROR-ROUTINE
            logger.error(
                "DB2-ERROR-ROUTINE: insert failed for record %d (%s/%s/%s/%s): %s",
                self.stats.records_read,
                row.account_no,
                row.portfolio_id,
                row.trans_date,
                row.trans_time,
                exc,
            )
            loader.rollback()
            return "error"

    # 3000-TERMINATE
    def _terminate(self, loader: HistoryLoader) -> None:
        try:
            loader.commit()
        except Exception:  # pragma: no cover - defensive
            logger.exception("final commit failed")
        # Always force a final checkpoint write regardless of min interval —
        # this matches 3100-FINAL-COMMIT which unconditionally calls
        # 2310-UPDATE-CHECKPOINT.
        self._checkpoint.write(self._snapshot_state(), force=True)
        loader.close()
        self._display_stats()

    def _snapshot_state(self) -> CheckpointState:
        return CheckpointState(
            job_name=PROGRAM_ID,
            records_read=self.stats.records_read,
            records_written=self.stats.records_written,
            error_count=self.stats.error_count,
        )

    def _display_stats(self) -> None:
        # Mirrors 3400-DISPLAY-STATS.
        logger.info("HISTLD00 Processing Statistics:")
        logger.info("  Records Read:    %d", self.stats.records_read)
        logger.info("  Records Written: %d", self.stats.records_written)
        logger.info("  Duplicates:      %d", self.stats.duplicate_count)
        logger.info("  Errors:          %d", self.stats.error_count)

    def run(self) -> int:
        loader, state = self._initialize()
        try:
            self._process(loader, state)
        finally:
            self._terminate(loader)
        # COBOL: MOVE WS-ERROR-COUNT TO RETURN-CODE. Cap at 255 for POSIX.
        return min(self.stats.error_count, 255)


# ---------------------------------------------------------------------------
# CLI / entry point
# ---------------------------------------------------------------------------


def _build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="histld00",
        description=(
            "Position History DB2 Load — Python port of HISTLD00.cbl. "
            "See the module docstring for the full pipeline contract."
        ),
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=os.environ.get("HISTLD00_INPUT_FILE"),
        help="Path to the TRANHIST fixed-width input file (env: HISTLD00_INPUT_FILE).",
    )
    parser.add_argument(
        "--checkpoint",
        type=Path,
        default=Path(
            os.environ.get("HISTLD00_CHECKPOINT", "histld00.checkpoint.json")
        ),
        help="Path to the checkpoint JSON file (env: HISTLD00_CHECKPOINT).",
    )
    parser.add_argument(
        "--db-driver",
        default=os.environ.get("HISTLD00_DB_DRIVER", "db2"),
        choices=("db2", "postgres", "postgresql", "sqlite"),
        help="Database driver to use (env: HISTLD00_DB_DRIVER).",
    )
    parser.add_argument(
        "--db-dsn",
        default=os.environ.get("HISTLD00_DB_DSN"),
        help="Driver-specific connection string (env: HISTLD00_DB_DSN).",
    )
    parser.add_argument(
        "--db-user",
        default=os.environ.get("HISTLD00_DB_USER"),
        help="Database user (env: HISTLD00_DB_USER).",
    )
    parser.add_argument(
        "--db-password",
        default=os.environ.get("HISTLD00_DB_PASSWORD"),
        help="Database password (env: HISTLD00_DB_PASSWORD).",
    )
    parser.add_argument(
        "--user-id",
        default=os.environ.get(
            "HISTLD00_USER_ID", getpass.getuser() if hasattr(getpass, "getuser") else ""
        ),
        help="USER_ID column value (env: HISTLD00_USER_ID).",
    )
    parser.add_argument(
        "--commit-every",
        type=int,
        default=int(
            os.environ.get("HISTLD00_COMMIT_EVERY", DEFAULT_COMMIT_THRESHOLD)
        ),
        help=(
            "Commit every N successful inserts "
            f"(env: HISTLD00_COMMIT_EVERY, default {DEFAULT_COMMIT_THRESHOLD})."
        ),
    )
    parser.add_argument(
        "--max-errors",
        type=int,
        default=int(
            os.environ.get("HISTLD00_MAX_ERRORS", DEFAULT_MAX_ERROR_COUNT)
        ),
        help=(
            "Abort after this many errors "
            f"(env: HISTLD00_MAX_ERRORS, default {DEFAULT_MAX_ERROR_COUNT})."
        ),
    )
    parser.add_argument(
        "--min-checkpoint-secs",
        type=int,
        default=int(
            os.environ.get(
                "HISTLD00_MIN_CKPT_SECS",
                DEFAULT_MIN_CHECKPOINT_INTERVAL_SECONDS,
            )
        ),
        help=(
            "Minimum seconds between checkpoint writes "
            f"(env: HISTLD00_MIN_CKPT_SECS, default "
            f"{DEFAULT_MIN_CHECKPOINT_INTERVAL_SECONDS})."
        ),
    )
    parser.add_argument(
        "--log-level",
        default=os.environ.get("HISTLD00_LOG_LEVEL", "INFO"),
        help="Python logging level (default: INFO).",
    )
    return parser


def _config_from_args(args: argparse.Namespace) -> JobConfig:
    if args.input is None:
        raise SystemExit(
            "HISTLD00_INPUT_FILE / --input is required"
        )
    if args.db_dsn is None:
        raise SystemExit(
            "HISTLD00_DB_DSN / --db-dsn is required"
        )
    return JobConfig(
        input_file=Path(args.input),
        checkpoint_file=Path(args.checkpoint),
        db_driver=args.db_driver,
        db_dsn=args.db_dsn,
        db_user=args.db_user,
        db_password=args.db_password,
        user_id=args.user_id or "",
        commit_threshold=args.commit_every,
        max_error_count=args.max_errors,
        min_checkpoint_interval_seconds=args.min_checkpoint_secs,
    )


def main(argv: Optional[list[str]] = None) -> int:
    args = _build_arg_parser().parse_args(argv)
    logging.basicConfig(
        level=getattr(logging, args.log_level.upper(), logging.INFO),
        format="%(asctime)s %(levelname)s %(name)s: %(message)s",
    )
    config = _config_from_args(args)
    logger.info(
        "Starting %s: input=%s driver=%s checkpoint=%s",
        PROGRAM_ID,
        config.input_file,
        config.db_driver,
        config.checkpoint_file,
    )
    rc = HISTLD00Job(config).run()
    logger.info("%s exiting with RC=%d", PROGRAM_ID, rc)
    return rc


if __name__ == "__main__":  # pragma: no cover
    sys.exit(main())
