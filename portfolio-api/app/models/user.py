"""User model replacing COBOL SECMGR user/credential storage.

Maps SECMGR concepts:
  - SEC-USER-ID -> User.id (UUID)
  - Username credential -> User.username
  - Password credential -> User.password_hash (bcrypt)
  - AUTHFILE roles -> User.roles (TEXT[])
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, String, func
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.types import JSON, TypeDecorator

from app.models.base import Base


class StringArray(TypeDecorator):
    """Platform-agnostic array type stored as JSON.

    Uses JSON on all backends for consistency between the migration
    (which defines the column as sa.JSON) and the ORM model.
    """

    impl = JSON
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return []
        return list(value)

    def process_result_value(self, value, dialect):
        if value is None:
            return []
        return list(value)


class User(Base):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(
        String(36),
        primary_key=True,
        default=lambda: str(uuid.uuid4()),
    )
    username: Mapped[str] = mapped_column(
        String(50), unique=True, nullable=False, index=True
    )
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    roles: Mapped[list] = mapped_column(StringArray(), nullable=False, default=list)
    status: Mapped[str] = mapped_column(
        String(10), nullable=False, default="active"
    )
    last_login: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        onupdate=func.now(),
    )
