"""
Shared SQLAlchemy DeclarativeBase for VSAM-equivalent PostgreSQL tables.

Provides the common base class used by all VSAM file models converted
from the COBOL Legacy Benchmark Suite.
"""

from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """Base class for all VSAM-equivalent SQLAlchemy models."""

    pass
