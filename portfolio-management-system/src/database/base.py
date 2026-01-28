"""
SQLAlchemy base model and common database utilities.
"""

from datetime import datetime
from typing import Any

from sqlalchemy import Column, DateTime, String
from sqlalchemy.orm import DeclarativeBase, declared_attr


class Base(DeclarativeBase):
    """Base class for all SQLAlchemy models."""
    
    @declared_attr
    def __tablename__(cls) -> str:
        """Generate table name from class name."""
        return cls.__name__.lower()


class AuditMixin:
    """
    Mixin for audit fields.
    Replaces COBOL audit fields (LAST-MAINT-DATE, LAST-MAINT-USER).
    """
    
    last_maint_date = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    last_maint_user = Column(String(8))
    
    def update_audit(self, user_id: str) -> None:
        """Update audit fields."""
        self.last_maint_date = datetime.utcnow()
        self.last_maint_user = user_id


class TimestampMixin:
    """
    Mixin for timestamp fields.
    Common pattern for created/updated tracking.
    """
    
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow, nullable=False)
