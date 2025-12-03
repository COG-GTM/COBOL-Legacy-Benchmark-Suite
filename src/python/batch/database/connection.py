"""
Database Connection Management

Provides database connection utilities for the batch processing system.
Replaces DB2 connection handling from DBPROC.cpy.
"""

import os
from contextlib import contextmanager
from typing import Generator, Optional

from sqlalchemy import create_engine, event
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from .models import Base


def get_database_url() -> str:
    """
    Get database URL from environment variables.
    
    Environment variables:
    - DATABASE_URL: Full connection string (takes precedence)
    - DB_HOST: Database host (default: localhost)
    - DB_PORT: Database port (default: 5432)
    - DB_NAME: Database name (default: portfolio)
    - DB_USER: Database user (default: postgres)
    - DB_PASSWORD: Database password (default: postgres)
    """
    if url := os.environ.get("DATABASE_URL"):
        return url
    
    host = os.environ.get("DB_HOST", "localhost")
    port = os.environ.get("DB_PORT", "5432")
    name = os.environ.get("DB_NAME", "portfolio")
    user = os.environ.get("DB_USER", "postgres")
    password = os.environ.get("DB_PASSWORD", "postgres")
    
    return f"postgresql://{user}:{password}@{host}:{port}/{name}"


class DatabaseConnection:
    """
    Database connection manager.
    
    Provides connection pooling and session management similar to
    DB2 connection handling in the COBOL programs.
    """
    
    def __init__(
        self,
        database_url: Optional[str] = None,
        pool_size: int = 5,
        max_overflow: int = 10,
        echo: bool = False,
    ):
        """
        Initialize database connection.
        
        Args:
            database_url: Database connection URL
            pool_size: Connection pool size
            max_overflow: Maximum overflow connections
            echo: Enable SQL logging
        """
        self.database_url = database_url or get_database_url()
        self.engine = create_engine(
            self.database_url,
            pool_size=pool_size,
            max_overflow=max_overflow,
            echo=echo,
            pool_pre_ping=True,
        )
        self.SessionLocal = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=self.engine,
        )
        self._setup_event_listeners()
    
    def _setup_event_listeners(self) -> None:
        """Set up SQLAlchemy event listeners for connection management."""
        @event.listens_for(self.engine, "connect")
        def set_search_path(dbapi_conn, connection_record):
            cursor = dbapi_conn.cursor()
            cursor.execute("SET search_path TO public")
            cursor.close()
    
    def create_tables(self) -> None:
        """Create all database tables."""
        Base.metadata.create_all(bind=self.engine)
    
    def drop_tables(self) -> None:
        """Drop all database tables."""
        Base.metadata.drop_all(bind=self.engine)
    
    @contextmanager
    def get_session(self) -> Generator[Session, None, None]:
        """
        Get a database session with automatic cleanup.
        
        Corresponds to CONNECT-TO-DB2 and DISCONNECT-FROM-DB2 in DBPROC.cpy.
        
        Usage:
            with db.get_session() as session:
                # perform database operations
                session.commit()
        """
        session = self.SessionLocal()
        try:
            yield session
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()
    
    @contextmanager
    def transaction(self) -> Generator[Session, None, None]:
        """
        Get a database session with automatic commit/rollback.
        
        Usage:
            with db.transaction() as session:
                # perform database operations
                # commit happens automatically on success
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
    
    def execute_with_retry(
        self,
        operation,
        max_retries: int = 3,
        retry_delay: float = 0.1,
    ):
        """
        Execute a database operation with retry logic.
        
        Corresponds to DB2-RETRY-COUNT and DB2-MAX-RETRIES in DBPROC.cpy.
        
        Args:
            operation: Callable that takes a session and performs the operation
            max_retries: Maximum number of retry attempts
            retry_delay: Delay between retries in seconds
            
        Returns:
            Result of the operation
        """
        import time
        
        last_exception = None
        for attempt in range(max_retries):
            try:
                with self.transaction() as session:
                    return operation(session)
            except Exception as e:
                last_exception = e
                if attempt < max_retries - 1:
                    time.sleep(retry_delay * (attempt + 1))
        
        raise last_exception
    
    def health_check(self) -> bool:
        """
        Check database connectivity.
        
        Returns:
            True if database is accessible, False otherwise
        """
        try:
            with self.get_session() as session:
                session.execute("SELECT 1")
            return True
        except Exception:
            return False
    
    def close(self) -> None:
        """Close all database connections."""
        self.engine.dispose()


_default_connection: Optional[DatabaseConnection] = None


def get_default_connection() -> DatabaseConnection:
    """Get or create the default database connection."""
    global _default_connection
    if _default_connection is None:
        _default_connection = DatabaseConnection()
    return _default_connection


def close_default_connection() -> None:
    """Close the default database connection."""
    global _default_connection
    if _default_connection is not None:
        _default_connection.close()
        _default_connection = None
