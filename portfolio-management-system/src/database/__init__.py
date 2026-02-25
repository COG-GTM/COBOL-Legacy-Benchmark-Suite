"""
Database Layer Module

Contains database connection and session management:
- SQLAlchemy engine configuration
- Session factory
- Connection pooling (replaces DB2ONLN)
- Transaction management (replaces DB2CMT)
"""

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, scoped_session
from sqlalchemy.pool import QueuePool

engine = None
Session = None


def init_db(database_url: str, pool_size: int = 5, max_overflow: int = 10):
    """
    Initialize database connection.
    
    Replaces DB2CONN and DB2ONLN connection pool management.
    
    Args:
        database_url: PostgreSQL connection string
        pool_size: Number of connections to maintain in pool
        max_overflow: Maximum overflow connections allowed
    """
    global engine, Session
    
    engine = create_engine(
        database_url,
        poolclass=QueuePool,
        pool_size=pool_size,
        max_overflow=max_overflow,
        pool_pre_ping=True,
    )
    
    session_factory = sessionmaker(bind=engine)
    Session = scoped_session(session_factory)
    
    return engine


def get_session():
    """Get a database session."""
    if Session is None:
        raise RuntimeError("Database not initialized. Call init_db() first.")
    return Session()


def close_session():
    """Close the current session."""
    if Session is not None:
        Session.remove()
