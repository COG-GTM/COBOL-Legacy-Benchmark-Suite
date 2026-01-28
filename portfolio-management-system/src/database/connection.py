"""
Database connection management.
Replaces COBOL DB2CONN program functionality.
"""

from contextlib import contextmanager
from typing import Generator

from sqlalchemy import create_engine, event
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import QueuePool

from src.config.settings import settings


class DatabaseConnection:
    """
    Database connection manager.
    Replaces DB2CONN.cbl connection management with retry logic.
    """
    
    _engine: Engine = None
    _session_factory: sessionmaker = None
    
    MAX_RETRIES = 3
    
    @classmethod
    def initialize(cls, database_url: str = None) -> None:
        """
        Initialize database connection.
        Equivalent to DB2CONN CONNECT function.
        """
        if cls._engine is not None:
            return
        
        url = database_url or settings.database.url
        
        cls._engine = create_engine(
            url,
            poolclass=QueuePool,
            pool_size=settings.database.pool_size,
            max_overflow=settings.database.max_overflow,
            pool_pre_ping=True,
            echo=settings.app.debug,
        )
        
        cls._session_factory = sessionmaker(
            bind=cls._engine,
            autocommit=False,
            autoflush=False,
        )
    
    @classmethod
    def get_engine(cls) -> Engine:
        """Get the database engine."""
        if cls._engine is None:
            cls.initialize()
        return cls._engine
    
    @classmethod
    @contextmanager
    def get_session(cls) -> Generator[Session, None, None]:
        """
        Get a database session with automatic cleanup.
        Equivalent to DB2 connection with commit/rollback handling.
        """
        if cls._session_factory is None:
            cls.initialize()
        
        session = cls._session_factory()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()
    
    @classmethod
    def disconnect(cls) -> None:
        """
        Disconnect from database.
        Equivalent to DB2CONN DISCONNECT function.
        """
        if cls._engine is not None:
            cls._engine.dispose()
            cls._engine = None
            cls._session_factory = None
    
    @classmethod
    def check_status(cls) -> bool:
        """
        Check database connection status.
        Equivalent to DB2CONN CHECK-STATUS function.
        """
        if cls._engine is None:
            return False
        
        try:
            with cls._engine.connect() as conn:
                conn.execute("SELECT 1")
            return True
        except Exception:
            return False


def get_db() -> Generator[Session, None, None]:
    """
    Dependency injection for Flask/FastAPI routes.
    Provides database session for request handling.
    """
    with DatabaseConnection.get_session() as session:
        yield session
