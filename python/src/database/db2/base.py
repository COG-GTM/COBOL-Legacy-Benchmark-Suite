"""DeclarativeBase for all DB2-migrated SQLAlchemy models."""

from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """Base class for all DB2-migrated ORM models targeting PostgreSQL."""

    pass
