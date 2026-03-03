"""SQLAlchemy model for RTNCODES (Return Code Logging) table.

Migrated from: src/database/db2/RTNCODES.sql
"""

from datetime import datetime

from sqlalchemy import DateTime, Index, Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class RtnCodes(Base):
    """Return Code Logging table."""

    __tablename__ = "rtncodes"

    timestamp: Mapped[datetime] = mapped_column(DateTime, primary_key=True)
    program_id: Mapped[str] = mapped_column(String(8), primary_key=True)
    return_code: Mapped[int] = mapped_column(Integer, nullable=False)
    highest_code: Mapped[int] = mapped_column(Integer, nullable=False)
    status_code: Mapped[str] = mapped_column(String(1), nullable=False)
    message_text: Mapped[str | None] = mapped_column(
        String(80), nullable=True
    )

    __table_args__ = (
        Index("rtncodes_prg_idx", "program_id", "timestamp"),
        Index("rtncodes_sts_idx", "status_code", "timestamp"),
    )
