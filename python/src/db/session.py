"""Session management for the Investment Portfolio Management System.

Translated from:
  - src/programs/common/DB2CMT.cbl   (commit controller with commit/rollback/
                                      savepoint logic)
  - src/programs/online/CURSMGR.cbl  (cursor lifecycle management)

The COBOL DB2CMT program provided:
  - INIT:  Initialize commit statistics
  - CMIT:  Commit based on record count / frequency / force flag
  - RBAK:  Rollback work
  - SAVE:  Create savepoints with ``SAVEPOINT ... ON ROLLBACK RETAIN CURSORS``
  - REST:  Rollback to a named savepoint
  - STAT:  Display commit/rollback/savepoint counts

The COBOL CURSMGR program managed DB2 cursors with:
  - Declare, Open, Fetch (single or array), Close operations
  - Fetch-count and rows-fetched statistics

This module replaces both with SQLAlchemy session management and a
context-manager that maps COBOL COMMIT/ROLLBACK semantics to Python
transaction boundaries.
"""

from __future__ import annotations

import logging
from collections.abc import Generator
from contextlib import contextmanager

from sqlalchemy.orm import Session, sessionmaker

from python.src.db.engine import get_engine

logger = logging.getLogger(__name__)

# Module-level session factory
_session_factory: sessionmaker[Session] | None = None


def _get_session_factory() -> sessionmaker[Session]:
    """Return (and lazily create) the module-level session factory."""
    global _session_factory
    if _session_factory is None:
        engine = get_engine()
        _session_factory = sessionmaker(bind=engine, expire_on_commit=False)
        logger.debug("Session factory created")
    return _session_factory


def get_session() -> Session:
    """Create and return a new :class:`~sqlalchemy.orm.Session`.

    Callers are responsible for committing/rolling back and closing.  For
    automatic transaction management prefer :func:`transactional`.
    """
    factory = _get_session_factory()
    return factory()


@contextmanager
def transactional() -> Generator[Session, None, None]:
    """Context manager providing a transactional database session.

    Mirrors COBOL ``DB2CMT`` commit/rollback logic:
      - On normal exit the session is **committed** (``2100-ISSUE-COMMIT``).
      - On exception the session is **rolled back** (``3000-ROLLBACK``)
        and the exception is re-raised.
      - The session is always closed on exit.

    Usage::

        with transactional() as session:
            session.add(portfolio)
            # auto-committed on exit, or rolled back on error
    """
    session = get_session()
    try:
        yield session
        session.commit()
        logger.debug("Transaction committed")
    except Exception:
        session.rollback()
        logger.debug("Transaction rolled back")
        raise
    finally:
        session.close()


@contextmanager
def read_only() -> Generator[Session, None, None]:
    """Context manager for read-only database access.

    Mirrors COBOL ``CURSMGR`` cursor lifecycle (open -> fetch -> close)
    translated into a read-only session.  No commit is issued; the session
    is simply closed when the block exits.
    """
    session = get_session()
    try:
        yield session
    finally:
        session.close()


def reset_session_factory() -> None:
    """Reset the module-level session factory.

    Useful in tests or after calling :func:`dispose_engine`.
    """
    global _session_factory
    _session_factory = None
    logger.debug("Session factory reset")
