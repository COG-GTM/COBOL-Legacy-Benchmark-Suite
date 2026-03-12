"""
Database session management translated from DB2CMT.cbl (commit controller)
and DB2ONLN.cbl (online session management).

Replaces COBOL DB2 session handling:
  - DB2CMT.cbl EVALUATE TRUE dispatch: COMMIT, ROLLBACK, SAVEPOINT
  - DB2ONLN.cbl session lifecycle
  - CURSMGR.cbl cursor management (absorbed into SQLAlchemy session)
"""

import logging
from collections.abc import Generator
from contextlib import contextmanager

from sqlalchemy.orm import Session, sessionmaker

from src.db.engine import get_engine

logger = logging.getLogger(__name__)

_session_factory: sessionmaker[Session] | None = None


def get_session_factory() -> sessionmaker[Session]:
    """
    Get or create the session factory.

    Translates DB2ONLN.cbl session pool initialization.
    """
    global _session_factory
    if _session_factory is None:
        engine = get_engine()
        _session_factory = sessionmaker(bind=engine, expire_on_commit=False)
    return _session_factory


def get_session() -> Generator[Session, None, None]:
    """
    Yield a database session with automatic commit/rollback.

    FastAPI dependency injection compatible.
    Translates DB2CMT.cbl commit/rollback logic:
      - 1000-COMMIT-WORK   -> session.commit()
      - 2000-ROLLBACK-WORK -> session.rollback()
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


@contextmanager
def session_scope() -> Generator[Session, None, None]:
    """
    Context manager for database sessions.

    For use outside of FastAPI dependency injection (batch processing, scripts).
    Translates DB2CMT.cbl commit/rollback with savepoint support.
    """
    factory = get_session_factory()
    session = factory()
    try:
        yield session
        session.commit()
        logger.debug("Session committed successfully")
    except Exception:
        session.rollback()
        logger.warning("Session rolled back due to exception")
        raise
    finally:
        session.close()


def reset_session_factory() -> None:
    """Reset the session factory (for testing or engine changes)."""
    global _session_factory
    _session_factory = None
