"""Lightweight VSAM file abstraction for indexed and sequential access.

The COBOL program uses two VSAM KSDS files:

* ``TRANSACTION-HISTORY`` — read sequentially.
* ``BATCH-CONTROL-FILE`` — opened I-O, read by key, then rewritten in place.

For portability we back the abstraction with a SQLite database (one table
per logical "file") so that the same Python pipeline can run on developer
laptops, CI runners, or cloud environments without a real VSAM dataset.

The class returns COBOL-style two-character file status codes from each
operation so callers can branch on '00', '10', '22', '23', etc., exactly
as the COBOL source does.
"""

from __future__ import annotations

import json
import os
import sqlite3
from contextlib import closing
from dataclasses import asdict, is_dataclass
from enum import Enum
from pathlib import Path
from typing import Any, Iterable, Iterator, Optional, Tuple, Union


class VsamStatus(str, Enum):
    """COBOL VSAM file status codes used by the loader."""

    SUCCESS = "00"
    EOF = "10"
    DUPKEY = "22"
    NOTFOUND = "23"
    INVALID_OP = "90"
    OTHER = "99"


class OpenMode(str, Enum):
    """Mirrors ``OPEN INPUT`` / ``OPEN I-O`` / ``OPEN EXTEND``."""

    INPUT = "INPUT"
    IO = "I-O"
    OUTPUT = "OUTPUT"
    EXTEND = "EXTEND"


class VsamFile:
    """Indexed-file abstraction backed by SQLite.

    Each ``VsamFile`` instance corresponds to a single logical KSDS. Records
    are serialized as JSON and stored in a table keyed by the user-supplied
    composite key. Sequential reads honor key order, mirroring the COBOL
    ``ACCESS MODE IS SEQUENTIAL`` semantics.
    """

    _SCHEMA = (
        "CREATE TABLE IF NOT EXISTS records ("
        "  key TEXT PRIMARY KEY,"
        "  payload TEXT NOT NULL"
        ")"
    )

    def __init__(
        self,
        path: Union[str, os.PathLike],
        *,
        table: str = "records",
    ) -> None:
        self._path = Path(path)
        self._table = table
        self._mode: Optional[OpenMode] = None
        self._conn: Optional[sqlite3.Connection] = None
        self._cursor: Optional[sqlite3.Cursor] = None
        # current position for sequential reads
        self._seq_iter: Optional[Iterator[Tuple[str, str]]] = None

    # ------------------------------------------------------------------
    # OPEN / CLOSE
    # ------------------------------------------------------------------
    def open(self, mode: OpenMode = OpenMode.INPUT) -> str:
        """Open the file in the requested mode. Returns a status code."""
        self._mode = mode
        self._path.parent.mkdir(parents=True, exist_ok=True)
        self._conn = sqlite3.connect(self._path)
        self._conn.execute(self._SCHEMA.replace("records", self._table))
        if mode == OpenMode.OUTPUT:
            # Truncate the file (matches ``OPEN OUTPUT``).
            self._conn.execute(f"DELETE FROM {self._table}")
            self._conn.commit()
        if mode in (OpenMode.INPUT, OpenMode.IO):
            self._reset_sequential_cursor()
        return VsamStatus.SUCCESS.value

    def close(self) -> str:
        if self._conn is not None:
            self._conn.commit()
            self._conn.close()
            self._conn = None
        self._mode = None
        self._seq_iter = None
        return VsamStatus.SUCCESS.value

    def __enter__(self) -> "VsamFile":
        if self._mode is None:
            self.open(OpenMode.INPUT)
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        self.close()

    # ------------------------------------------------------------------
    # READ / WRITE / REWRITE / START
    # ------------------------------------------------------------------
    def read(self, key: Optional[str] = None) -> Tuple[str, Optional[Any]]:
        """Read the next sequential record, or a specific key.

        Returns a ``(status, record)`` tuple. When ``key`` is provided the
        read behaves like a random read and uses status '23' for not-found.
        For sequential reads, the EOF status is '10'.
        """
        self._require_open(read=True)
        assert self._conn is not None
        if key is not None:
            row = self._conn.execute(
                f"SELECT payload FROM {self._table} WHERE key = ?",
                (key,),
            ).fetchone()
            if row is None:
                return VsamStatus.NOTFOUND.value, None
            return VsamStatus.SUCCESS.value, json.loads(row[0])

        if self._seq_iter is None:
            self._reset_sequential_cursor()
        try:
            assert self._seq_iter is not None
            _, payload = next(self._seq_iter)
        except StopIteration:
            return VsamStatus.EOF.value, None
        return VsamStatus.SUCCESS.value, json.loads(payload)

    def write(self, key: str, record: Any) -> str:
        """Write a brand-new record. Returns '22' on duplicate keys."""
        self._require_open(write=True)
        assert self._conn is not None
        try:
            self._conn.execute(
                f"INSERT INTO {self._table} (key, payload) VALUES (?, ?)",
                (key, _serialize(record)),
            )
            self._conn.commit()
            return VsamStatus.SUCCESS.value
        except sqlite3.IntegrityError:
            return VsamStatus.DUPKEY.value

    def rewrite(self, key: str, record: Any) -> str:
        """Overwrite an existing record in place (REWRITE)."""
        self._require_open(write=True)
        assert self._conn is not None
        cur = self._conn.execute(
            f"UPDATE {self._table} SET payload = ? WHERE key = ?",
            (_serialize(record), key),
        )
        self._conn.commit()
        if cur.rowcount == 0:
            return VsamStatus.NOTFOUND.value
        return VsamStatus.SUCCESS.value

    def start(self, key: Optional[str] = None) -> str:
        """Position the sequential cursor at ``key`` (or the first record)."""
        self._require_open(read=True)
        assert self._conn is not None
        if key is None:
            self._reset_sequential_cursor()
            return VsamStatus.SUCCESS.value
        row = self._conn.execute(
            f"SELECT key FROM {self._table} WHERE key >= ? ORDER BY key LIMIT 1",
            (key,),
        ).fetchone()
        if row is None:
            return VsamStatus.NOTFOUND.value
        self._reset_sequential_cursor(start_key=row[0])
        return VsamStatus.SUCCESS.value

    # ------------------------------------------------------------------
    # Iteration helpers (Pythonic convenience over READ AT END loop)
    # ------------------------------------------------------------------
    def __iter__(self) -> Iterator[Any]:
        self._require_open(read=True)
        self._reset_sequential_cursor()
        while True:
            status, record = self.read()
            if status == VsamStatus.EOF.value:
                return
            yield record

    def bulk_write(self, items: Iterable[Tuple[str, Any]]) -> int:
        """Write many records in a single transaction. Returns count written."""
        self._require_open(write=True)
        assert self._conn is not None
        count = 0
        with closing(self._conn.cursor()) as cur:
            for key, record in items:
                cur.execute(
                    f"INSERT OR REPLACE INTO {self._table} (key, payload) "
                    "VALUES (?, ?)",
                    (key, _serialize(record)),
                )
                count += 1
        self._conn.commit()
        return count

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------
    def _require_open(self, *, read: bool = False, write: bool = False) -> None:
        if self._conn is None or self._mode is None:
            raise RuntimeError(f"VSAM file {self._path} is not open")
        if read and self._mode == OpenMode.OUTPUT:
            raise RuntimeError("Cannot read from a file opened OUTPUT")
        if write and self._mode == OpenMode.INPUT:
            raise RuntimeError("Cannot write to a file opened INPUT")

    def _reset_sequential_cursor(self, start_key: Optional[str] = None) -> None:
        assert self._conn is not None
        if start_key is None:
            rows = self._conn.execute(
                f"SELECT key, payload FROM {self._table} ORDER BY key"
            ).fetchall()
        else:
            rows = self._conn.execute(
                f"SELECT key, payload FROM {self._table} "
                "WHERE key >= ? ORDER BY key",
                (start_key,),
            ).fetchall()
        self._seq_iter = iter(rows)


def _serialize(record: Any) -> str:
    """Serialize a record to JSON, supporting dataclasses and Decimals."""
    if is_dataclass(record):
        record = asdict(record)
    return json.dumps(record, default=_json_default, sort_keys=True)


def _json_default(value: Any) -> Any:
    from decimal import Decimal

    if isinstance(value, Decimal):
        return str(value)
    raise TypeError(f"Object of type {type(value).__name__} is not JSON-serializable")
