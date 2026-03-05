"""
Database engine factory translated from COBOL programs:
- DB2CONN.cbl (connection management with retry logic)
- DB2ONLN.cbl (online session management)

Replaces DB2 connection pool with SQLAlchemy engine.
"""

import logging
import os
import time

from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine

from src.common.constants import MAX_CONNECTION_RETRIES

logger = logging.getLogger(__name__)

_engine: Engine | None = None


def get_database_url() -> str:
    """Get database URL from environment, defaulting to SQLite for dev."""
    return os.environ.get("DATABASE_URL", "sqlite:///portfolio.db")


def create_db_engine(database_url: str | None = None, echo: bool = False) -> Engine:
    """
    Create SQLAlchemy engine with retry logic.
    Translates DB2CONN.cbl 1000-CONNECT with retry loop.
    """
    url = database_url or get_database_url()
    retry_count = 0

    while retry_count < MAX_CONNECTION_RETRIES:
        try:
            engine = create_engine(
                url,
                echo=echo,
                pool_pre_ping=True,
            )
            # Test connection (equivalent to DB2 CONNECT TO :WS-DB-NAME)
            with engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            logger.info("Database connection established: %s", url.split("@")[-1] if "@" in url else url)
            return engine
        except Exception as e:
            retry_count += 1
            if retry_count >= MAX_CONNECTION_RETRIES:
                logger.error("Failed to connect after %d retries: %s", MAX_CONNECTION_RETRIES, e)
                raise
            logger.warning("Connection attempt %d failed, retrying: %s", retry_count, e)
            time.sleep(1)

    raise RuntimeError("Unreachable: connection retry loop exhausted")


def get_engine(database_url: str | None = None, echo: bool = False) -> Engine:
    """Get or create singleton engine. Translates DB2CONN 3000-CHECK-STATUS."""
    global _engine
    if _engine is None:
        _engine = create_db_engine(database_url, echo)
    return _engine


def dispose_engine() -> None:
    """Dispose engine. Translates DB2CONN 2000-DISCONNECT."""
    global _engine
    if _engine is not None:
        _engine.dispose()
        _engine = None
        logger.info("Database connection disposed")


def init_db(engine: Engine | None = None) -> None:
    """Create all tables. Used for development/testing."""
    from src.db.tables import Base

    eng = engine or get_engine()
    Base.metadata.create_all(eng)
    logger.info("Database tables created")


@event.listens_for(Engine, "connect")
def set_sqlite_pragma(dbapi_connection, connection_record):
    """Enable WAL mode and foreign keys for SQLite."""
    import sqlite3

    if isinstance(dbapi_connection, sqlite3.Connection):
        cursor = dbapi_connection.cursor()
        cursor.execute("PRAGMA journal_mode=WAL")
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()
