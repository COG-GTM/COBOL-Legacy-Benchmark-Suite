"""
Database session management for the COBOL to Python migration.

This module provides database connection and session management utilities
that replace the VSAM file handling and DB2 connection management from
the original COBOL system.

Key Features:
- SQLAlchemy session management with context managers
- Support for SQLite (development) and PostgreSQL (production)
- Connection pooling for efficient resource usage
- Transaction management with commit/rollback support
"""

import os
from contextlib import contextmanager
from typing import Optional, Generator

from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker, Session
from sqlalchemy.pool import StaticPool

from migration.python.database.orm_models import Base


class DatabaseManager:
    """
    Database manager providing connection and session management.
    
    This class replaces the file handling logic from COBOL programs like
    BCHCTL00 (batch control) and the DB2 connection management from
    online programs like DB2ONLN.
    
    Usage:
        # Initialize with SQLite for development
        db = DatabaseManager('sqlite:///portfolio.db')
        db.create_tables()
        
        # Use sessions for database operations
        with db.session_scope() as session:
            session.add(new_record)
            # Auto-commits on success, rolls back on exception
    """
    
    def __init__(self, database_url: Optional[str] = None, echo: bool = False):
        """
        Initialize the database manager.
        
        Args:
            database_url: SQLAlchemy database URL. Defaults to SQLite in-memory.
                         Examples:
                         - 'sqlite:///portfolio.db' (SQLite file)
                         - 'sqlite:///:memory:' (SQLite in-memory)
                         - 'postgresql://user:pass@host/db' (PostgreSQL)
            echo: If True, log all SQL statements (useful for debugging)
        """
        if database_url is None:
            database_url = os.environ.get(
                'DATABASE_URL', 
                'sqlite:///portfolio.db'
            )
        
        self.database_url = database_url
        self.echo = echo
        
        # Configure engine based on database type
        if database_url.startswith('sqlite'):
            # SQLite-specific configuration
            if ':memory:' in database_url:
                # In-memory SQLite needs special pooling
                self.engine = create_engine(
                    database_url,
                    echo=echo,
                    connect_args={'check_same_thread': False},
                    poolclass=StaticPool
                )
            else:
                self.engine = create_engine(
                    database_url,
                    echo=echo,
                    connect_args={'check_same_thread': False}
                )
            
            # Enable foreign key support for SQLite
            @event.listens_for(self.engine, 'connect')
            def set_sqlite_pragma(dbapi_connection, connection_record):
                cursor = dbapi_connection.cursor()
                cursor.execute('PRAGMA foreign_keys=ON')
                cursor.close()
        else:
            # PostgreSQL or other databases
            self.engine = create_engine(
                database_url,
                echo=echo,
                pool_size=5,
                max_overflow=10,
                pool_pre_ping=True  # Verify connections before use
            )
        
        # Create session factory
        self.SessionLocal = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=self.engine
        )
    
    def create_tables(self):
        """
        Create all database tables.
        
        This is equivalent to running IDCAMS DEFINE CLUSTER for VSAM files
        or CREATE TABLE statements for DB2.
        """
        Base.metadata.create_all(bind=self.engine)
    
    def drop_tables(self):
        """
        Drop all database tables.
        
        Use with caution - this deletes all data.
        """
        Base.metadata.drop_all(bind=self.engine)
    
    def get_session(self) -> Session:
        """
        Get a new database session.
        
        The caller is responsible for closing the session.
        Prefer using session_scope() for automatic management.
        
        Returns:
            SQLAlchemy Session object
        """
        return self.SessionLocal()
    
    @contextmanager
    def session_scope(self) -> Generator[Session, None, None]:
        """
        Provide a transactional scope around a series of operations.
        
        This context manager handles commit/rollback automatically,
        similar to the checkpoint/restart logic in BCHCTL00.
        
        Usage:
            with db.session_scope() as session:
                session.add(record)
                # Commits automatically on success
                # Rolls back automatically on exception
        
        Yields:
            SQLAlchemy Session object
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
    
    def execute_sql(self, sql: str, params: Optional[dict] = None):
        """
        Execute raw SQL statement.
        
        Args:
            sql: SQL statement to execute
            params: Optional parameters for the SQL statement
            
        Returns:
            Result of the SQL execution
        """
        with self.engine.connect() as connection:
            if params:
                result = connection.execute(sql, params)
            else:
                result = connection.execute(sql)
            connection.commit()
            return result


# Global database manager instance
_db_manager: Optional[DatabaseManager] = None


def init_database(database_url: Optional[str] = None, 
                  echo: bool = False,
                  create_tables: bool = True) -> DatabaseManager:
    """
    Initialize the global database manager.
    
    This function should be called once at application startup,
    similar to the file OPEN statements in COBOL batch programs.
    
    Args:
        database_url: SQLAlchemy database URL
        echo: If True, log all SQL statements
        create_tables: If True, create tables if they don't exist
        
    Returns:
        DatabaseManager instance
    """
    global _db_manager
    _db_manager = DatabaseManager(database_url, echo)
    if create_tables:
        _db_manager.create_tables()
    return _db_manager


def get_session() -> Session:
    """
    Get a database session from the global manager.
    
    Raises:
        RuntimeError: If database has not been initialized
        
    Returns:
        SQLAlchemy Session object
    """
    if _db_manager is None:
        raise RuntimeError(
            "Database not initialized. Call init_database() first."
        )
    return _db_manager.get_session()


def get_db_manager() -> DatabaseManager:
    """
    Get the global database manager.
    
    Raises:
        RuntimeError: If database has not been initialized
        
    Returns:
        DatabaseManager instance
    """
    if _db_manager is None:
        raise RuntimeError(
            "Database not initialized. Call init_database() first."
        )
    return _db_manager
