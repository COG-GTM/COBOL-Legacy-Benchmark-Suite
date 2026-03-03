"""Database engine and session management.

Replaces COBOL DB2CONN.cbl connection manager and DB2ONLN.cbl
connection pool with SQLAlchemy engine and session factory.
Supports up to 100 connections matching the COBOL WS-MAX-CONNECTIONS.
"""

import logging
from contextlib import contextmanager
from typing import Generator

from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

logger = logging.getLogger(__name__)

# Default PostgreSQL connection string
DEFAULT_DATABASE_URL = "postgresql://postgres:postgres@localhost:5432/portfolio_mgmt"

# Connection pool size matching COBOL DB2ONLN WS-MAX-CONNECTIONS = 100
MAX_POOL_SIZE = 100

_engine: Engine | None = None
_session_factory: sessionmaker[Session] | None = None


def get_engine(database_url: str = DEFAULT_DATABASE_URL) -> Engine:
    """Get or create the SQLAlchemy engine.

    Replaces COBOL DB2CONN 1000-CONNECT with retry logic and
    DB2ONLN connection pool management.
    """
    global _engine
    if _engine is None:
        _engine = create_engine(
            database_url,
            pool_size=20,
            max_overflow=MAX_POOL_SIZE - 20,
            pool_pre_ping=True,
            pool_recycle=3600,
            echo=False,
        )
        logger.info("Database engine created: pool_size=20, max_overflow=%d", MAX_POOL_SIZE - 20)
    return _engine


def get_session_factory(database_url: str = DEFAULT_DATABASE_URL) -> sessionmaker[Session]:
    """Get or create the session factory."""
    global _session_factory
    if _session_factory is None:
        engine = get_engine(database_url)
        _session_factory = sessionmaker(bind=engine, autocommit=False, autoflush=False)
    return _session_factory


@contextmanager
def get_db_session(database_url: str = DEFAULT_DATABASE_URL) -> Generator[Session, None, None]:
    """Context manager for database sessions.

    Replaces the COBOL pattern of:
    - CONNECT-TO-DB2 (1200-CONNECT-DB2)
    - COMMIT WORK (3100-FINAL-COMMIT)
    - DISCONNECT-FROM-DB2 (3300-DISCONNECT-DB2)
    """
    factory = get_session_factory(database_url)
    session = factory()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


def check_connection(database_url: str = DEFAULT_DATABASE_URL) -> bool:
    """Check database connectivity.

    Replaces COBOL DB2CONN 3000-CHECK-STATUS which runs
    SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1.
    """
    try:
        engine = get_engine(database_url)
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
        return True
    except Exception as exc:
        logger.error("Database connection check failed: %s", exc)
        return False


def dispose_engine() -> None:
    """Dispose the engine and release all connections.

    Replaces COBOL DB2CONN 2000-DISCONNECT.
    """
    global _engine, _session_factory
    if _engine is not None:
        _engine.dispose()
        _engine = None
        _session_factory = None
        logger.info("Database engine disposed")
