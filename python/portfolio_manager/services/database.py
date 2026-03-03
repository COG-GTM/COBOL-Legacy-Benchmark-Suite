"""Database connection and session management.

Replaces:
  - DB2ONLN (src/programs/online/DB2ONLN.cbl) — connection pool (max 100)
  - DB2CONN (src/programs/common/DB2CONN.cbl) — batch DB2 connection
  - DBPROC.cpy connect/disconnect routines

Uses SQLAlchemy connection pooling with PostgreSQL (psycopg2 driver).
"""

from __future__ import annotations

import logging
import os
from contextlib import contextmanager
from typing import Generator

from sqlalchemy import create_engine, event, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from portfolio_manager.models.database import Base

logger = logging.getLogger(__name__)

# Default connection string — override via DATABASE_URL env var
DEFAULT_DATABASE_URL = "postgresql+psycopg2://postgres:postgres@localhost:5432/portfolio"


def get_database_url() -> str:
    """Return the database URL from environment or default."""
    return os.environ.get("DATABASE_URL", DEFAULT_DATABASE_URL)


def create_db_engine(
    database_url: str | None = None,
    pool_size: int = 20,
    max_overflow: int = 80,
    pool_timeout: int = 30,
    pool_recycle: int = 1800,
    echo: bool = False,
) -> Engine:
    """Create a SQLAlchemy engine with connection pooling.

    Replaces DB2ONLN connection pool (max 100 connections in original).
    Default pool_size=20 + max_overflow=80 = 100 max connections.

    Args:
        database_url: PostgreSQL connection string.
        pool_size: Number of permanent connections in the pool.
        max_overflow: Additional connections allowed beyond pool_size.
        pool_timeout: Seconds to wait for a connection from the pool.
        pool_recycle: Seconds after which a connection is recycled.
        echo: If True, log all SQL statements.

    Returns:
        Configured SQLAlchemy Engine.
    """
    url = database_url or get_database_url()
    engine = create_engine(
        url,
        pool_size=pool_size,
        max_overflow=max_overflow,
        pool_timeout=pool_timeout,
        pool_recycle=pool_recycle,
        pool_pre_ping=True,  # verify connections before use
        echo=echo,
    )

    # Log pool checkout events (replaces DB2ONLN connection tracking)
    @event.listens_for(engine, "checkout")
    def on_checkout(dbapi_conn, connection_record, connection_proxy):  # type: ignore[no-untyped-def]
        logger.debug("DB connection checked out from pool")

    @event.listens_for(engine, "checkin")
    def on_checkin(dbapi_conn, connection_record):  # type: ignore[no-untyped-def]
        logger.debug("DB connection returned to pool")

    return engine


# Module-level engine and session factory (lazy initialization)
_engine: Engine | None = None
_session_factory: sessionmaker[Session] | None = None


def get_engine() -> Engine:
    """Get or create the module-level engine."""
    global _engine
    if _engine is None:
        _engine = create_db_engine()
    return _engine


def get_session_factory() -> sessionmaker[Session]:
    """Get or create the module-level session factory."""
    global _session_factory
    if _session_factory is None:
        _session_factory = sessionmaker(bind=get_engine(), expire_on_commit=False)
    return _session_factory


@contextmanager
def get_session() -> Generator[Session, None, None]:
    """Provide a transactional database session.

    Replaces the DB2 COMMIT/ROLLBACK pattern from DBPROC.cpy.

    Usage::

        with get_session() as session:
            session.add(record)
            # auto-commits on success, rolls back on exception
    """
    factory = get_session_factory()
    session = factory()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


def init_database(engine: Engine | None = None) -> None:
    """Create all tables defined in the ORM models.

    Call once at application startup or in migration scripts.
    """
    eng = engine or get_engine()
    Base.metadata.create_all(eng)
    logger.info("Database tables created/verified")


def check_connection(engine: Engine | None = None) -> bool:
    """Check database connectivity.

    Replaces DB2ONLN status check (DB2-REQUEST-TYPE = 'S').
    """
    eng = engine or get_engine()
    try:
        with eng.connect() as conn:
            conn.execute(text("SELECT 1"))
        return True
    except Exception as exc:
        logger.error("Database connection check failed: %s", exc)
        return False


def reset_engine() -> None:
    """Dispose the current engine and reset module-level state.

    Replaces DB2ONLN disconnect (DB2-REQUEST-TYPE = 'D').
    """
    global _engine, _session_factory
    if _engine is not None:
        _engine.dispose()
        _engine = None
    _session_factory = None
    logger.info("Database engine disposed and reset")
