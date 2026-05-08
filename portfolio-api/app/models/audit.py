from datetime import datetime

from sqlalchemy import DateTime, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import Base


class AuditLog(Base):
    __tablename__ = "audit_log"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    timestamp: Mapped[datetime] = mapped_column(
        DateTime, nullable=False, server_default=func.now()
    )
    user_id: Mapped[str | None] = mapped_column(String(8), nullable=True)
    terminal_id: Mapped[str | None] = mapped_column(String(8), nullable=True)
    program_id: Mapped[str | None] = mapped_column(String(8), nullable=True)
    action: Mapped[str | None] = mapped_column(String(10), nullable=True)
    entity_type: Mapped[str | None] = mapped_column(String(20), nullable=True)
    entity_id: Mapped[str | None] = mapped_column(String(20), nullable=True)
    before_image: Mapped[str | None] = mapped_column(Text, nullable=True)
    after_image: Mapped[str | None] = mapped_column(Text, nullable=True)
    message: Mapped[str | None] = mapped_column(String(256), nullable=True)
