"""
Session management translated from COBOL programs:
- DB2CMT.cbl (commit/rollback logic)
- CURSMGR.cbl (cursor management)

Replaces DB2 transaction commit/rollback with SQLAlchemy session management.
"""

import logging
from collections.abc import Generator
from contextlib import contextmanager

from sqlalchemy.orm import Session, sessionmaker

from src.db.engine import get_engine

logger = logging.getLogger(__name__)

_session_factory: sessionmaker | None = None


def get_session_factory() -> sessionmaker:
    """Get or create session factory."""
    global _session_factory
    if _session_factory is None:
        engine = get_engine()
        _session_factory = sessionmaker(bind=engine, expire_on_commit=False)
    return _session_factory


def reset_session_factory() -> None:
    """Reset session factory (useful for testing)."""
    global _session_factory
    _session_factory = None


@contextmanager
def get_db_session() -> Generator[Session, None, None]:
    """
    Context manager for database sessions.
    Translates DB2CMT.cbl commit/rollback logic:
    - On success: EXEC SQL COMMIT WORK END-EXEC
    - On error: EXEC SQL ROLLBACK WORK END-EXEC
    """
    factory = get_session_factory()
    session = factory()
    try:
        yield session
        session.commit()
        logger.debug("Transaction committed")
    except Exception:
        session.rollback()
        logger.error("Transaction rolled back due to error")
        raise
    finally:
        session.close()


def get_session_dependency() -> Generator[Session, None, None]:
    """FastAPI dependency for database sessions."""
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
