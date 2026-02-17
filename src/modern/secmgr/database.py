"""
Database session management for the Security Manager service.

Provides SQLAlchemy engine and session factory configuration,
replacing the direct DB2 EXEC SQL calls in the COBOL program.
"""

from __future__ import annotations

import os
from typing import Generator

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from .models import Base

_engine: Engine | None = None
_session_factory: sessionmaker[Session] | None = None


def get_engine() -> Engine:
    """Return the singleton SQLAlchemy engine.

    The database URL is read from the DATABASE_URL environment variable.
    Defaults to a local SQLite database for development.
    """
    global _engine
    if _engine is None:
        database_url = os.environ.get(
            "DATABASE_URL",
            "sqlite:///secmgr.db",
        )
        _engine = create_engine(
            database_url,
            echo=os.environ.get("SQL_ECHO", "").lower() == "true",
            pool_pre_ping=True,
        )
    return _engine


def get_session_factory() -> sessionmaker[Session]:
    """Return the singleton session factory."""
    global _session_factory
    if _session_factory is None:
        _session_factory = sessionmaker(bind=get_engine())
    return _session_factory


def get_db() -> Generator[Session, None, None]:
    """Yield a database session for dependency injection (e.g., FastAPI Depends).

    Ensures the session is properly closed after use.
    """
    factory = get_session_factory()
    session = factory()
    try:
        yield session
    finally:
        session.close()


def init_db() -> None:
    """Create all tables defined in the ORM models.

    Called once at application startup. In production, use Alembic migrations instead.
    """
    Base.metadata.create_all(bind=get_engine())


def reset_engine() -> None:
    """Reset the engine and session factory. Used primarily in tests."""
    global _engine, _session_factory
    if _engine is not None:
        _engine.dispose()
    _engine = None
    _session_factory = None
