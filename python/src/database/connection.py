"""Connection pooling and configuration for PostgreSQL.

Pool settings are sized to match the COBOL DB2CONN 100-connection limit:
  pool_size=20 (base connections) + max_overflow=80 = 100 total max.
"""

import os

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine


def get_database_url() -> str:
    """Build the database URL from environment variables.

    Expected env vars:
        DB_HOST     - PostgreSQL host (default: localhost)
        DB_PORT     - PostgreSQL port (default: 5432)
        DB_NAME     - database name  (default: portfolio)
        DB_USER     - database user  (default: postgres)
        DB_PASSWORD - database password
    """
    host = os.environ.get("DB_HOST", "localhost")
    port = os.environ.get("DB_PORT", "5432")
    name = os.environ.get("DB_NAME", "portfolio")
    user = os.environ.get("DB_USER", "postgres")
    password = os.environ.get("DB_PASSWORD", "")
    return f"postgresql+psycopg2://{user}:{password}@{host}:{port}/{name}"


def create_db_engine(url: str | None = None) -> Engine:
    """Create a SQLAlchemy engine with connection-pool settings.

    Parameters
    ----------
    url:
        Database URL.  When *None*, ``get_database_url()`` is used.
    """
    if url is None:
        url = get_database_url()

    return create_engine(
        url,
        pool_size=20,
        max_overflow=80,
        pool_pre_ping=True,
        pool_recycle=3600,
        echo=False,
    )
