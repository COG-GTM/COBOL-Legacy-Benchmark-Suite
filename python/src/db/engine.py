"""SQLAlchemy engine factory for the Investment Portfolio Management System.

Translated from:
  - src/programs/common/DB2CONN.cbl  (connection manager with retry logic)
  - src/programs/online/DB2ONLN.cbl  (online connection pool, max 100 connections)

The COBOL DB2CONN program managed connections with:
  - Connect/disconnect/status-check functions
  - Retry logic (max 3 retries with 100ms wait)
  - Connection state tracking

The COBOL DB2ONLN program managed an online connection pool with:
  - Pool statistics (total, active, available connections)
  - Maximum 100 connections
  - Connection token generation

This module replaces both with SQLAlchemy's built-in connection pooling.
"""

from __future__ import annotations

import logging
import os
from typing import Any

from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine

logger = logging.getLogger(__name__)

# Module-level engine singleton
_engine: Engine | None = None

# Default connection settings (mirroring COBOL DB2ONLN WS-MAX-CONNECTIONS = 100)
_DEFAULT_POOL_SIZE = 10
_DEFAULT_MAX_OVERFLOW = 90  # pool_size + max_overflow = 100 total
_DEFAULT_POOL_TIMEOUT = 30  # seconds
_DEFAULT_POOL_RECYCLE = 1800  # 30 minutes
_DEFAULT_SQLITE_URL = "sqlite:///portfolio.db"


def _get_database_url() -> str:
    """Resolve the database URL from environment or default.

    Reads DATABASE_URL environment variable.  Falls back to SQLite for
    development.  PostgreSQL is the expected production backend.
    """
    return os.environ.get("DATABASE_URL", _DEFAULT_SQLITE_URL)


def _build_engine_kwargs(url: str, **overrides: Any) -> dict[str, Any]:
    """Build keyword arguments for ``create_engine`` based on the dialect.

    SQLite uses ``StaticPool`` semantics (no real connection pool) while
    PostgreSQL uses QueuePool with settings derived from COBOL DB2ONLN's
    pool configuration.
    """
    kwargs: dict[str, Any] = {
        "echo": os.environ.get("DATABASE_ECHO", "").lower() in ("1", "true", "yes"),
    }

    if url.startswith("sqlite"):
        # SQLite: use check_same_thread=False for multi-threaded dev servers
        kwargs["connect_args"] = {"check_same_thread": False}
        kwargs["pool_pre_ping"] = True
    else:
        # PostgreSQL / other production databases — mirror COBOL pool limits
        kwargs["pool_size"] = overrides.pop("pool_size", _DEFAULT_POOL_SIZE)
        kwargs["max_overflow"] = overrides.pop("max_overflow", _DEFAULT_MAX_OVERFLOW)
        kwargs["pool_timeout"] = overrides.pop("pool_timeout", _DEFAULT_POOL_TIMEOUT)
        kwargs["pool_recycle"] = overrides.pop("pool_recycle", _DEFAULT_POOL_RECYCLE)
        kwargs["pool_pre_ping"] = True  # mirrors DB2CONN's status-check function

    kwargs.update(overrides)
    return kwargs


def get_engine(url: str | None = None, **kwargs: Any) -> Engine:
    """Return the module-level engine, creating it on first call.

    Mirrors COBOL ``DB2CONN`` 1000-CONNECT: creates a connection (engine)
    with retry-ready pool settings.  Subsequent calls return the cached
    engine unless ``url`` differs.

    Parameters
    ----------
    url:
        SQLAlchemy connection string.  Defaults to ``DATABASE_URL`` env var
        or an SQLite file.
    **kwargs:
        Forwarded to :func:`sqlalchemy.create_engine` (e.g. ``pool_size``).
    """
    global _engine

    resolved_url = url or _get_database_url()

    if _engine is not None:
        existing_url = str(_engine.url)
        if existing_url != resolved_url:
            raise ValueError(
                f"Engine already exists with URL {existing_url!r}; "
                f"requested URL {resolved_url!r} differs. "
                f"Call dispose_engine() first to create a new engine."
            )
        return _engine

    engine_kwargs = _build_engine_kwargs(resolved_url, **kwargs)
    _engine = create_engine(resolved_url, **engine_kwargs)

    # Register event listeners for connection lifecycle logging
    # (replaces COBOL DB2ONLN's WS-POOL-STATS tracking)
    @event.listens_for(_engine, "connect")
    def _on_connect(dbapi_conn: Any, connection_record: Any) -> None:
        logger.debug("DB connection established (pool connect)")

    @event.listens_for(_engine, "checkout")
    def _on_checkout(dbapi_conn: Any, connection_record: Any, connection_proxy: Any) -> None:
        logger.debug("DB connection checked out from pool")

    @event.listens_for(_engine, "checkin")
    def _on_checkin(dbapi_conn: Any, connection_record: Any) -> None:
        logger.debug("DB connection returned to pool")

    logger.info("Database engine created: %s", resolved_url.split("@")[-1] if "@" in resolved_url else resolved_url)
    return _engine


def dispose_engine() -> None:
    """Dispose the engine and release all pooled connections.

    Mirrors COBOL ``DB2CONN`` 2000-DISCONNECT: commits outstanding work
    and resets the connection.
    """
    global _engine
    if _engine is not None:
        _engine.dispose()
        logger.info("Database engine disposed")
        _engine = None


def check_connection(engine: Engine | None = None) -> bool:
    """Verify the database connection is alive.

    Mirrors COBOL ``DB2CONN`` 3000-CHECK-STATUS which ran
    ``SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1``.
    """
    eng = engine or get_engine()
    try:
        with eng.connect() as conn:
            conn.execute(text("SELECT 1"))
        return True
    except Exception:
        logger.exception("Database connection check failed")
        return False
