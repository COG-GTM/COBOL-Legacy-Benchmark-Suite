"""
History Data Model

Migrated from COBOL copybook: src/copybook/common/HISTREC.cpy

Original COBOL structure:
- HIST-KEY: Composite key (PORTFOLIO-ID + DATE + TIME + SEQ-NO)
- HIST-DATA: History details (record type, action code, before/after images, reason code)
- HIST-AUDIT: Audit trail (process date, user)

COBOL Data Types Mapping:
- PIC X(n) -> str
- PIC X(400) -> str (before/after images for change tracking)
"""

from datetime import datetime, date, time
from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Column, String, Date, Time, DateTime, Text, Index

from src.models.base import Base


class HistoryRecordType(str, Enum):
    """
    History record type codes.
    
    Migrated from COBOL 88-level conditions:
    - HIST-TYPE-PORT VALUE 'PT' (Portfolio)
    - HIST-TYPE-POS  VALUE 'PS' (Position)
    - HIST-TYPE-TRN  VALUE 'TR' (Transaction)
    """
    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    """
    History action codes.
    
    Migrated from COBOL 88-level conditions:
    - HIST-ACTION-ADD VALUE 'A'
    - HIST-ACTION-CHG VALUE 'C'
    - HIST-ACTION-DEL VALUE 'D'
    """
    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class HistoryRecord(BaseModel):
    """
    Pydantic model for history record validation.
    
    Preserves all field definitions from HISTREC.cpy with Python type mappings.
    This model captures audit trail information for all data changes.
    """
    
    # HIST-KEY fields
    hist_portfolio_id: str = Field(
        ...,
        max_length=8,
        description="Portfolio identifier"
    )
    hist_date: date = Field(
        ...,
        description="History date (YYYYMMDD format in COBOL)"
    )
    hist_time: time = Field(
        ...,
        description="History time (HHMMSS format in COBOL)"
    )
    hist_seq_no: str = Field(
        ...,
        max_length=4,
        description="Sequence number"
    )
    
    # HIST-DATA fields
    hist_record_type: HistoryRecordType = Field(
        ...,
        description="Record type: PT=Portfolio, PS=Position, TR=Transaction"
    )
    hist_action_code: HistoryActionCode = Field(
        ...,
        description="Action code: A=Add, C=Change, D=Delete"
    )
    hist_before_image: Optional[str] = Field(
        None,
        max_length=400,
        description="Record image before change (PIC X(400))"
    )
    hist_after_image: Optional[str] = Field(
        None,
        max_length=400,
        description="Record image after change (PIC X(400))"
    )
    hist_reason_code: Optional[str] = Field(
        None,
        max_length=4,
        description="Reason for change"
    )
    
    # HIST-AUDIT fields
    hist_process_date: Optional[datetime] = Field(
        None,
        description="Processing timestamp"
    )
    hist_process_user: Optional[str] = Field(
        None,
        max_length=8,
        description="Processing user ID"
    )

    @field_validator("hist_portfolio_id", "hist_seq_no", "hist_reason_code")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase for consistency with COBOL."""
        if v:
            return v.strip().upper()
        return v

    class Config:
        """Pydantic configuration."""
        json_encoders = {
            date: lambda v: v.strftime("%Y%m%d"),
            time: lambda v: v.strftime("%H%M%S"),
            datetime: lambda v: v.isoformat(),
        }


class History(Base):
    """
    SQLAlchemy ORM model for history records.
    
    Maps to PostgreSQL table: history
    Replaces VSAM history tracking and provides audit trail functionality.
    """
    __tablename__ = "history"

    # Primary key fields (composite key from HIST-KEY)
    hist_portfolio_id = Column(String(8), primary_key=True, nullable=False)
    hist_date = Column(Date, primary_key=True, nullable=False)
    hist_time = Column(Time, primary_key=True, nullable=False)
    hist_seq_no = Column(String(4), primary_key=True, nullable=False)

    # History data fields
    hist_record_type = Column(String(2), nullable=False)
    hist_action_code = Column(String(1), nullable=False)
    hist_before_image = Column(Text, nullable=True)
    hist_after_image = Column(Text, nullable=True)
    hist_reason_code = Column(String(4), nullable=True)

    # Audit fields
    hist_process_date = Column(DateTime, nullable=True)
    hist_process_user = Column(String(8), nullable=True)

    # Indexes for common access patterns
    __table_args__ = (
        Index("idx_hist_portfolio_date", "hist_portfolio_id", "hist_date"),
        Index("idx_hist_record_type", "hist_record_type", "hist_date"),
        Index("idx_hist_action", "hist_action_code", "hist_date"),
    )

    def __repr__(self) -> str:
        return (
            f"<History(portfolio={self.hist_portfolio_id}, "
            f"date={self.hist_date}, type={self.hist_record_type}, "
            f"action={self.hist_action_code})>"
        )

    def to_pydantic(self) -> HistoryRecord:
        """Convert SQLAlchemy model to Pydantic model for validation/serialization."""
        return HistoryRecord(
            hist_portfolio_id=self.hist_portfolio_id,
            hist_date=self.hist_date,
            hist_time=self.hist_time,
            hist_seq_no=self.hist_seq_no,
            hist_record_type=HistoryRecordType(self.hist_record_type),
            hist_action_code=HistoryActionCode(self.hist_action_code),
            hist_before_image=self.hist_before_image,
            hist_after_image=self.hist_after_image,
            hist_reason_code=self.hist_reason_code,
            hist_process_date=self.hist_process_date,
            hist_process_user=self.hist_process_user,
        )

    @classmethod
    def from_pydantic(cls, record: HistoryRecord) -> "History":
        """Create SQLAlchemy model from Pydantic model."""
        return cls(
            hist_portfolio_id=record.hist_portfolio_id,
            hist_date=record.hist_date,
            hist_time=record.hist_time,
            hist_seq_no=record.hist_seq_no,
            hist_record_type=record.hist_record_type.value,
            hist_action_code=record.hist_action_code.value,
            hist_before_image=record.hist_before_image,
            hist_after_image=record.hist_after_image,
            hist_reason_code=record.hist_reason_code,
            hist_process_date=record.hist_process_date,
            hist_process_user=record.hist_process_user,
        )
