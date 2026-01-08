"""Database connection management - replaces DB2CONN, DB2CMT, DB2ERR, DB2STAT.

This module provides SQLAlchemy-based database connection management,
replacing the COBOL DB2 support layer programs.
"""

import os
from contextlib import contextmanager
from typing import Generator

from sqlalchemy import create_engine, event
from sqlalchemy.orm import Session, sessionmaker

from app.utils.logging import get_logger

logger = get_logger(__name__)

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./portfolio.db")

engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False} if "sqlite" in DATABASE_URL else {},
    echo=os.getenv("SQL_ECHO", "false").lower() == "true",
    pool_pre_ping=True,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db() -> Generator[Session, None, None]:
    """Get database session - FastAPI dependency.

    Yields:
        Database session

    This replaces the COBOL DB2CONN program's connection management.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


@contextmanager
def get_db_context() -> Generator[Session, None, None]:
    """Get database session as context manager.

    Yields:
        Database session

    Usage:
        with get_db_context() as db:
            db.query(...)
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db() -> None:
    """Initialize database - create all tables.

    This replaces the DB2 DDL scripts for table creation.
    """
    from app.database.models import Base

    Base.metadata.create_all(bind=engine)
    logger.info("Database initialized successfully")


def commit_transaction(db: Session) -> bool:
    """Commit transaction - replaces DB2CMT program.

    Args:
        db: Database session

    Returns:
        True if commit successful, False otherwise
    """
    try:
        db.commit()
        logger.debug("Transaction committed successfully")
        return True
    except Exception as e:
        logger.error(f"Transaction commit failed: {e}")
        db.rollback()
        return False


def rollback_transaction(db: Session) -> None:
    """Rollback transaction - replaces DB2CMT rollback functionality.

    Args:
        db: Database session
    """
    try:
        db.rollback()
        logger.debug("Transaction rolled back")
    except Exception as e:
        logger.error(f"Transaction rollback failed: {e}")


class DatabaseStats:
    """Database statistics tracking - replaces DB2STAT program."""

    def __init__(self):
        self.queries_executed = 0
        self.commits = 0
        self.rollbacks = 0
        self.errors = 0

    def increment_queries(self) -> None:
        """Increment query count."""
        self.queries_executed += 1

    def increment_commits(self) -> None:
        """Increment commit count."""
        self.commits += 1

    def increment_rollbacks(self) -> None:
        """Increment rollback count."""
        self.rollbacks += 1

    def increment_errors(self) -> None:
        """Increment error count."""
        self.errors += 1

    def get_stats(self) -> dict:
        """Get current statistics."""
        return {
            "queries_executed": self.queries_executed,
            "commits": self.commits,
            "rollbacks": self.rollbacks,
            "errors": self.errors,
        }

    def reset(self) -> None:
        """Reset statistics."""
        self.queries_executed = 0
        self.commits = 0
        self.rollbacks = 0
        self.errors = 0


db_stats = DatabaseStats()


@event.listens_for(engine, "before_cursor_execute")
def receive_before_cursor_execute(
    conn, cursor, statement, parameters, context, executemany
):
    """Track query execution for statistics."""
    db_stats.increment_queries()


class DatabaseError(Exception):
    """Database error - replaces DB2ERR error handling.

    This exception class provides structured error information
    similar to the COBOL DB2ERR program.
    """

    def __init__(
        self,
        sqlcode: int = 0,
        message: str = "",
        program: str = "",
        details: str = "",
    ):
        self.sqlcode = sqlcode
        self.message = message
        self.program = program
        self.details = details
        super().__init__(self.message)

    def to_dict(self) -> dict:
        """Convert to dictionary for logging/API responses."""
        return {
            "sqlcode": self.sqlcode,
            "message": self.message,
            "program": self.program,
            "details": self.details,
        }

    @classmethod
    def from_exception(cls, e: Exception, program: str = "") -> "DatabaseError":
        """Create DatabaseError from generic exception."""
        return cls(
            sqlcode=-1,
            message=str(e),
            program=program,
            details=type(e).__name__,
        )


def handle_db_error(e: Exception, program: str = "") -> DatabaseError:
    """Handle database error - replaces DB2ERR program.

    Args:
        e: Exception that occurred
        program: Program name where error occurred

    Returns:
        DatabaseError with structured error information
    """
    db_stats.increment_errors()
    error = DatabaseError.from_exception(e, program)
    logger.error(
        f"Database error in {program}: {error.message}",
        extra={"sqlcode": error.sqlcode, "details": error.details},
    )
    return error
