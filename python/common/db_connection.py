"""Replacement for DBPROC.cpy.

Provides ``CONNECT-TO-DB2`` / ``DISCONNECT-FROM-DB2`` / ``CHECK-SQL-STATUS`` /
``DB2-ERROR-ROUTINE`` equivalents using SQLAlchemy. The connection string is
configurable via the environment so the loader can target SQLite (for tests)
or any RDBMS supported by SQLAlchemy.
"""

from __future__ import annotations

import logging
import os
from contextlib import contextmanager
from typing import Iterator, Optional

from sqlalchemy import create_engine
from sqlalchemy.engine import Connection, Engine
from sqlalchemy.exc import IntegrityError, SQLAlchemyError

from python.models.poshist_table import Base


LOGGER = logging.getLogger(__name__)


class DatabaseError(RuntimeError):
    """Raised when a non-recoverable database error occurs."""


class DatabaseConnection:
    """Encapsulates the lifecycle of a SQLAlchemy database connection.

    The class exposes ``connect``, ``disconnect``, ``commit``, ``rollback``,
    ``check_sql_status`` and ``db2_error_routine`` analogous to the COBOL
    procedures in DBPROC.cpy.
    """

    DEFAULT_DB_URL = "sqlite:///poshist.db"

    def __init__(
        self,
        url: Optional[str] = None,
        *,
        create_schema: bool = False,
        engine: Optional[Engine] = None,
        echo: bool = False,
    ) -> None:
        """Create a new connection wrapper.

        Args:
            url: SQLAlchemy URL. If ``None``, falls back to ``$POSHIST_DB_URL``
                or :attr:`DEFAULT_DB_URL`.
            create_schema: When ``True``, run ``Base.metadata.create_all`` on
                connect (useful for tests / SQLite).
            engine: Pre-built SQLAlchemy engine (used by tests to inject a
                shared in-memory engine).
            echo: Forwarded to ``create_engine`` for SQL logging.
        """
        self._url = url or os.environ.get("POSHIST_DB_URL") or self.DEFAULT_DB_URL
        self._create_schema = create_schema
        self._engine: Optional[Engine] = engine
        self._connection: Optional[Connection] = None
        self._echo = echo

    @property
    def url(self) -> str:
        return self._url

    @property
    def engine(self) -> Engine:
        if self._engine is None:
            raise DatabaseError("Engine has not been initialized; call connect() first")
        return self._engine

    @property
    def connection(self) -> Connection:
        if self._connection is None:
            raise DatabaseError(
                "Database connection is not open; call connect() first"
            )
        return self._connection

    # ------------------------------------------------------------------
    # Lifecycle: CONNECT-TO-DB2 / DISCONNECT-FROM-DB2
    # ------------------------------------------------------------------
    def connect(self) -> Connection:
        """Open a SQLAlchemy connection (CONNECT-TO-DB2)."""
        if self._engine is None:
            self._engine = create_engine(self._url, echo=self._echo, future=True)
        if self._create_schema:
            Base.metadata.create_all(self._engine)
        if self._connection is None or self._connection.closed:
            self._connection = self._engine.connect()
        LOGGER.info("Connected to database %s", self._url)
        return self._connection

    def disconnect(self) -> None:
        """Commit and close the active connection (DISCONNECT-FROM-DB2)."""
        if self._connection is not None and not self._connection.closed:
            try:
                self.commit()
            finally:
                self._connection.close()
                self._connection = None
        LOGGER.info("Disconnected from database %s", self._url)

    # ------------------------------------------------------------------
    # Transaction primitives
    # ------------------------------------------------------------------
    def commit(self) -> None:
        """Issue ``COMMIT WORK``."""
        if self._connection is None:
            return
        self._connection.commit()

    def rollback(self) -> None:
        """Issue ``ROLLBACK WORK``."""
        if self._connection is None:
            return
        self._connection.rollback()

    def check_sql_status(self, exc: Optional[Exception]) -> bool:
        """Return ``True`` for success, ``False`` for an error.

        Mirrors ``CHECK-SQL-STATUS`` from DBPROC.cpy: if the wrapped
        operation raised, log the error and return False to allow the caller
        to invoke the error routine.
        """
        if exc is None:
            return True
        LOGGER.error("SQL error: %s", exc)
        return False

    def db2_error_routine(self, exc: Exception) -> None:
        """Mirror DB2-ERROR-ROUTINE: rollback and re-raise as DatabaseError."""
        try:
            self.rollback()
        except SQLAlchemyError:
            LOGGER.exception("Rollback failed during DB2 error routine")
        raise DatabaseError(str(exc)) from exc

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------
    @staticmethod
    def is_duplicate_key_error(exc: Exception) -> bool:
        """Return True for DB-level duplicate-key errors (SQLCODE -803).

        Maps to ``IntegrityError`` raised by SQLAlchemy when a unique/primary
        key constraint is violated. The COBOL program treats SQLCODE -803 as
        a non-fatal "skip and continue" condition.
        """
        return isinstance(exc, IntegrityError)

    @contextmanager
    def transaction(self) -> Iterator[Connection]:
        """Context manager that yields the connection and commits/rolls back."""
        conn = self.connect()
        try:
            yield conn
            self.commit()
        except Exception:
            self.rollback()
            raise

    # Make the connection itself usable as a context manager
    def __enter__(self) -> "DatabaseConnection":
        self.connect()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        if exc_type is not None:
            try:
                self.rollback()
            except SQLAlchemyError:
                LOGGER.exception("Rollback failed in context manager exit")
        self.disconnect()
