"""Database connection management.

Supports PostgreSQL (production) and SQLite (development/testing).
Configuration is driven by environment variables.

Environment variables:
    DATABASE_URL   - Full SQLAlchemy connection string (takes precedence).
    DB_DRIVER      - 'postgresql' or 'sqlite' (default: 'sqlite').
    DB_HOST        - Database host (default: 'localhost').
    DB_PORT        - Database port (default: '5432').
    DB_NAME        - Database name (default: 'portfolio').
    DB_USER        - Database user.
    DB_PASSWORD    - Database password.
    DB_POOL_SIZE   - Connection pool size (default: '5').
    DB_MAX_OVERFLOW - Pool max overflow (default: '10').
"""

import os
from collections.abc import Generator
from contextlib import contextmanager

from sqlalchemy import create_engine
from sqlalchemy.engine import URL, Engine
from sqlalchemy.orm import Session, sessionmaker

from db.schema import Base

_engine: Engine | None = None


def _build_url() -> str:
    """Build a SQLAlchemy database URL from environment variables."""
    url = os.environ.get("DATABASE_URL")
    if url:
        return url

    driver = os.environ.get("DB_DRIVER", "sqlite")

    if driver == "sqlite":
        db_name = os.environ.get("DB_NAME", "portfolio.db")
        return f"sqlite:///{db_name}"

    host = os.environ.get("DB_HOST", "localhost")
    port = os.environ.get("DB_PORT", "5432")
    name = os.environ.get("DB_NAME", "portfolio")
    user = os.environ.get("DB_USER", "")
    password = os.environ.get("DB_PASSWORD", "")
    return str(URL.create(
        "postgresql+psycopg2",
        username=user,
        password=password,
        host=host,
        port=int(port),
        database=name,
    ))


def get_engine() -> Engine:
    """Return a cached SQLAlchemy engine (singleton).

    The engine and its connection pool are created once and reused across calls.
    For SQLite the pool is disabled (StaticPool equivalent via NullPool).
    For PostgreSQL, pool_size and max_overflow are configurable.
    """
    global _engine  # noqa: PLW0603
    if _engine is not None:
        return _engine

    url = _build_url()

    if url.startswith("sqlite"):
        _engine = create_engine(url, echo=False, connect_args={"check_same_thread": False})
    else:
        pool_size = int(os.environ.get("DB_POOL_SIZE", "5"))
        max_overflow = int(os.environ.get("DB_MAX_OVERFLOW", "10"))
        _engine = create_engine(
            url,
            echo=False,
            pool_size=pool_size,
            max_overflow=max_overflow,
            pool_pre_ping=True,
        )

    return _engine


def reset_engine() -> None:
    """Dispose of the cached engine and reset. Useful for testing."""
    global _engine  # noqa: PLW0603
    if _engine is not None:
        _engine.dispose()
        _engine = None


def get_session_factory(engine: Engine | None = None) -> sessionmaker[Session]:
    """Return a Session factory bound to the given (or default) engine."""
    if engine is None:
        engine = get_engine()
    return sessionmaker(bind=engine, expire_on_commit=False)


@contextmanager
def get_session(engine: Engine | None = None) -> Generator[Session, None, None]:
    """Context manager that yields a transactional database session.

    Commits on success, rolls back on exception, and always closes.
    """
    factory = get_session_factory(engine)
    session = factory()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


def init_db(engine: Engine | None = None) -> None:
    """Create all tables defined in the ORM schema.

    Useful for development/testing. In production, use Alembic migrations.
    """
    if engine is None:
        engine = get_engine()
    Base.metadata.create_all(engine)
