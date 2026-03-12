"""
Database engine factory translated from DB2CONN.cbl (connection management)
and DB2ONLN.cbl (online session management).

Replaces COBOL DB2 connection handling:
  - 1000-CONNECT-DB2 paragraph -> create_engine()
  - 2000-DISCONNECT-DB2 paragraph -> dispose()
  - Connection retry logic (MAX_DB2_RETRIES=3)
  - Connection pool (MAX_DB2_CONNECTIONS=100)
"""

import logging
import os
import time

from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine

from src.common.constants import MAX_DB2_CONNECTIONS, MAX_DB2_RETRIES, RETRY_INTERVAL_SECONDS

logger = logging.getLogger(__name__)

_engine: Engine | None = None


def get_database_url() -> str:
    """
    Get database URL from environment.

    Default SQLite for dev, PostgreSQL for production.
    Maps to COBOL DB2CONN.cbl 0100-GET-DB-PARAMS.
    """
    return os.environ.get(
        "DATABASE_URL",
        "sqlite:///./investment_portfolio.db",
    )


def create_db_engine(
    database_url: str | None = None,
    pool_size: int = 10,
    max_overflow: int = MAX_DB2_CONNECTIONS - 10,
    echo: bool = False,
) -> Engine:
    """
    Create SQLAlchemy engine with connection pooling.

    Translates DB2CONN.cbl 1000-CONNECT-DB2 and DB2ONLN.cbl connection pool setup.
    Includes retry logic from DB2CONN.cbl 1200-RETRY-CONNECTION.

    Args:
        database_url: Database connection string. Defaults to env var or SQLite.
        pool_size: Base connection pool size (DB2ONLN default pool).
        max_overflow: Maximum overflow connections above pool_size.
        echo: Echo SQL statements for debugging.

    Returns:
        Configured SQLAlchemy Engine.
    """
    url = database_url or get_database_url()
    is_sqlite = url.startswith("sqlite")

    connect_args: dict[str, bool] = {}
    if is_sqlite:
        connect_args["check_same_thread"] = False

    engine_kwargs: dict[str, object] = {
        "echo": echo,
    }

    if not is_sqlite:
        engine_kwargs["pool_size"] = pool_size
        engine_kwargs["max_overflow"] = max_overflow
        engine_kwargs["pool_pre_ping"] = True
        engine_kwargs["pool_recycle"] = 3600
    engine_kwargs["connect_args"] = connect_args

    # Retry logic from DB2CONN.cbl 1200-RETRY-CONNECTION
    last_error: Exception | None = None
    for attempt in range(1, MAX_DB2_RETRIES + 1):
        try:
            engine = create_engine(url, **engine_kwargs)  # type: ignore[arg-type]
            # Test connection (maps to DB2CONN.cbl 1100-VERIFY-CONNECTION)
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info("Database connection established (attempt %d)", attempt)
            return engine
        except Exception as exc:
            last_error = exc
            logger.warning(
                "Database connection attempt %d/%d failed: %s",
                attempt,
                MAX_DB2_RETRIES,
                exc,
            )
            if attempt < MAX_DB2_RETRIES:
                time.sleep(RETRY_INTERVAL_SECONDS)

    raise RuntimeError(
        f"Failed to connect to database after {MAX_DB2_RETRIES} attempts"
    ) from last_error


def get_engine() -> Engine:
    """
    Get or create the global database engine singleton.

    Translates the DB2 connection reuse pattern from DB2ONLN.cbl.
    """
    global _engine
    if _engine is None:
        _engine = create_db_engine()
    return _engine


def dispose_engine() -> None:
    """
    Dispose the global engine and release all connections.

    Translates DB2CONN.cbl 2000-DISCONNECT-DB2.
    """
    global _engine
    if _engine is not None:
        _engine.dispose()
        _engine = None
        logger.info("Database engine disposed")


@event.listens_for(Engine, "connect")
def _set_sqlite_pragma(dbapi_connection: object, connection_record: object) -> None:
    """Enable WAL mode and foreign keys for SQLite connections."""
    import sqlite3

    if isinstance(dbapi_connection, sqlite3.Connection):
        cursor = dbapi_connection.cursor()
        cursor.execute("PRAGMA journal_mode=WAL")
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()
