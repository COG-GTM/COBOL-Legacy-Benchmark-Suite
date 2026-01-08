"""
Database connection management - Replaces COBOL DB2 connection handling.
Provides connection pooling, transaction management, and recovery.
"""

import os
import logging
from contextlib import contextmanager
from typing import Generator, Optional

from sqlalchemy import create_engine, event
from sqlalchemy.orm import sessionmaker, Session, scoped_session
from sqlalchemy.pool import QueuePool
from sqlalchemy.exc import SQLAlchemyError, OperationalError

# Import all models to ensure they're registered with Base
from ..models.position import Base as PositionBase, Position
from ..models.transaction import Base as TransactionBase, Transaction
from ..models.history import Base as HistoryBase, History
from ..models.batch_control import Base as BatchControlBase, BatchControl

logger = logging.getLogger(__name__)


class DatabaseConnection:
    """
    Database connection manager - Replaces COBOL DB2ONLN and DB2RECV programs.
    
    Provides:
    - Connection pooling (replaces CICS connection management)
    - Transaction management (replaces DB2 COMMIT/ROLLBACK)
    - Error recovery (replaces DB2RECV error handling)
    - Session management
    """
    
    _instance: Optional['DatabaseConnection'] = None
    _engine = None
    _session_factory = None
    
    def __new__(cls, *args, **kwargs):
        """Singleton pattern for database connection"""
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self, db_url: str = None):
        """
        Initialize database connection.
        
        Args:
            db_url: Database URL. Defaults to SQLite if not provided.
        """
        if self._engine is not None:
            return
            
        self.db_url = db_url or os.getenv('DATABASE_URL', 'sqlite:///portfolio.db')
        self._initialize_engine()
    
    def _initialize_engine(self):
        """Initialize SQLAlchemy engine with connection pooling"""
        # Configure engine based on database type
        if self.db_url.startswith('sqlite'):
            # SQLite doesn't support connection pooling the same way
            self._engine = create_engine(
                self.db_url,
                echo=os.getenv('SQL_ECHO', 'false').lower() == 'true',
                connect_args={'check_same_thread': False}
            )
        else:
            # PostgreSQL or other databases with connection pooling
            self._engine = create_engine(
                self.db_url,
                poolclass=QueuePool,
                pool_size=5,
                max_overflow=10,
                pool_timeout=30,
                pool_recycle=1800,
                echo=os.getenv('SQL_ECHO', 'false').lower() == 'true'
            )
        
        # Create session factory
        self._session_factory = sessionmaker(
            bind=self._engine,
            autocommit=False,
            autoflush=False
        )
        
        # Set up event listeners for connection management
        @event.listens_for(self._engine, 'connect')
        def on_connect(dbapi_conn, connection_record):
            logger.debug("Database connection established")
        
        @event.listens_for(self._engine, 'checkout')
        def on_checkout(dbapi_conn, connection_record, connection_proxy):
            logger.debug("Connection checked out from pool")
        
        logger.info(f"Database engine initialized: {self.db_url}")
    
    @property
    def engine(self):
        """Get SQLAlchemy engine"""
        return self._engine
    
    def create_all_tables(self):
        """Create all database tables"""
        # We need to use a single Base for all models
        # For now, create tables from each Base
        PositionBase.metadata.create_all(self._engine)
        TransactionBase.metadata.create_all(self._engine)
        HistoryBase.metadata.create_all(self._engine)
        BatchControlBase.metadata.create_all(self._engine)
        logger.info("All database tables created")
    
    def drop_all_tables(self):
        """Drop all database tables"""
        PositionBase.metadata.drop_all(self._engine)
        TransactionBase.metadata.drop_all(self._engine)
        HistoryBase.metadata.drop_all(self._engine)
        BatchControlBase.metadata.drop_all(self._engine)
        logger.info("All database tables dropped")
    
    def get_session(self) -> Session:
        """Get a new database session"""
        return self._session_factory()
    
    def get_scoped_session(self) -> scoped_session:
        """Get a thread-local scoped session"""
        return scoped_session(self._session_factory)
    
    @contextmanager
    def session_scope(self) -> Generator[Session, None, None]:
        """
        Provide a transactional scope around a series of operations.
        Replaces COBOL DB2 COMMIT/ROLLBACK handling.
        
        Usage:
            with db.session_scope() as session:
                session.add(record)
                # Auto-commits on success, rolls back on exception
        """
        session = self.get_session()
        try:
            yield session
            session.commit()
            logger.debug("Transaction committed successfully")
        except SQLAlchemyError as e:
            session.rollback()
            logger.error(f"Transaction rolled back due to error: {e}")
            raise
        finally:
            session.close()
    
    def recover_connection(self) -> bool:
        """
        Attempt to recover database connection.
        Replaces COBOL DB2RECV recovery logic.
        
        Returns:
            True if recovery successful, False otherwise
        """
        try:
            # Test connection
            with self._engine.connect() as conn:
                conn.execute("SELECT 1")
            logger.info("Database connection recovered successfully")
            return True
        except OperationalError as e:
            logger.error(f"Database connection recovery failed: {e}")
            # Attempt to recreate engine
            try:
                self._engine.dispose()
                self._initialize_engine()
                return True
            except Exception as e2:
                logger.error(f"Engine recreation failed: {e2}")
                return False
    
    def close(self):
        """Close all database connections"""
        if self._engine:
            self._engine.dispose()
            logger.info("Database connections closed")


# Module-level convenience functions

_db_connection: Optional[DatabaseConnection] = None


def get_database(db_url: str = None) -> DatabaseConnection:
    """Get or create database connection instance"""
    global _db_connection
    if _db_connection is None:
        _db_connection = DatabaseConnection(db_url)
    return _db_connection


def get_session() -> Session:
    """Get a new database session"""
    return get_database().get_session()


def create_tables():
    """Create all database tables"""
    get_database().create_all_tables()


@contextmanager
def session_scope() -> Generator[Session, None, None]:
    """Provide a transactional scope"""
    with get_database().session_scope() as session:
        yield session
