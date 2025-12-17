"""
Database service for connection management.
Replaces DB2CONN, DB2CMT, and DB2ONLN COBOL programs.
"""

from collections.abc import Generator
from contextlib import contextmanager

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.config import get_settings
from app.models.database import Base


class DatabaseService:
    """
    Database service for managing connections and sessions.
    Replaces DB2CONN and DB2ONLN functionality.
    """

    def __init__(self, database_url: str | None = None):
        settings = get_settings()
        self.database_url = database_url or settings.database_url
        self.engine = create_engine(
            self.database_url,
            pool_size=settings.database_pool_size,
            max_overflow=settings.database_max_overflow,
            pool_pre_ping=True,
        )
        self.SessionLocal = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=self.engine,
        )

    def create_tables(self) -> None:
        """Create all database tables."""
        Base.metadata.create_all(bind=self.engine)

    def drop_tables(self) -> None:
        """Drop all database tables."""
        Base.metadata.drop_all(bind=self.engine)

    @contextmanager
    def get_session(self) -> Generator[Session, None, None]:
        """
        Get a database session with automatic commit/rollback.
        Replaces DB2CMT commit/rollback functionality.
        """
        session = self.SessionLocal()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def get_session_no_commit(self) -> Session:
        """Get a session without automatic commit (for batch processing)."""
        return self.SessionLocal()


_db_service: DatabaseService | None = None


def get_database_service() -> DatabaseService:
    """Get the singleton database service instance."""
    global _db_service
    if _db_service is None:
        _db_service = DatabaseService()
    return _db_service


def get_db() -> Generator[Session, None, None]:
    """
    Dependency for FastAPI endpoints to get database session.
    """
    db_service = get_database_service()
    session = db_service.SessionLocal()
    try:
        yield session
    finally:
        session.close()
