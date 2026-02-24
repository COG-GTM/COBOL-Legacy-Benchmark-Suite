"""
Shared SQLAlchemy Base for all ORM models.

This module provides a single declarative_base() instance that should be
imported by all model files to ensure unified schema management.
"""

from sqlalchemy.orm import declarative_base

Base = declarative_base()
