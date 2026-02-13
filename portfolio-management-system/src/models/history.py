"""History Record Model - migrated from COBOL copybook HISTREC.cpy

Source: src/copybook/common/HISTREC.cpy
COBOL Record: HISTORY-RECORD

COBOL Data Type Mapping:
    PIC X(n)   -> str (fixed-length character)
    PIC X(400) -> str (before/after image stored as text)
    88-level conditions -> Enum or validated string constants

The history record captures before and after images of data changes,
functioning as an audit trail for all portfolio, position, and
transaction modifications.
"""
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field
from sqlalchemy import Column, String, Text, Index
from sqlalchemy.orm import declarative_base

Base = declarative_base()


class HistoryRecordType(str, Enum):
    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(str, Enum):
    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


class HistoryRecordORM(Base):
    """SQLAlchemy ORM model for change history records."""

    __tablename__ = "vsam_change_history"

    portfolio_id = Column(String(8), primary_key=True, nullable=False)
    history_date = Column(String(8), primary_key=True, nullable=False)
    history_time = Column(String(6), primary_key=True, nullable=False)
    sequence_no = Column(String(4), primary_key=True, nullable=False)
    record_type = Column(String(2), nullable=False)
    action_code = Column(String(1), nullable=False)
    before_image = Column(Text)
    after_image = Column(Text)
    reason_code = Column(String(4))
    process_timestamp = Column(String(26))
    process_user = Column(String(8))


class HistoryRecord(BaseModel):
    """Pydantic model for history record validation.

    Mapped from COBOL copybook HISTREC.cpy:
        01  HISTORY-RECORD.
            05  HIST-KEY.
                10  HIST-PORTFOLIO-ID  PIC X(08).     -> portfolio_id
                10  HIST-DATE          PIC X(08).     -> history_date
                10  HIST-TIME          PIC X(06).     -> history_time
                10  HIST-SEQ-NO        PIC X(04).     -> sequence_no
            05  HIST-DATA.
                10  HIST-RECORD-TYPE   PIC X(02).     -> record_type
                10  HIST-ACTION-CODE   PIC X(01).     -> action_code
                10  HIST-BEFORE-IMAGE  PIC X(400).    -> before_image
                10  HIST-AFTER-IMAGE   PIC X(400).    -> after_image
                10  HIST-REASON-CODE   PIC X(04).     -> reason_code
            05  HIST-AUDIT.
                10  HIST-PROCESS-DATE  PIC X(26).     -> process_timestamp
                10  HIST-PROCESS-USER  PIC X(08).     -> process_user
    """

    portfolio_id: str = Field(
        ..., min_length=1, max_length=8, description="Portfolio identifier"
    )
    history_date: str = Field(
        ..., min_length=8, max_length=8, description="History date (YYYYMMDD)"
    )
    history_time: str = Field(
        ..., min_length=6, max_length=6, description="History time (HHMMSS)"
    )
    sequence_no: str = Field(
        ..., min_length=1, max_length=4, description="Sequence number"
    )
    record_type: HistoryRecordType = Field(
        ..., description="PT=Portfolio, PS=Position, TR=Transaction"
    )
    action_code: HistoryActionCode = Field(
        ..., description="A=Add, C=Change, D=Delete"
    )
    before_image: Optional[str] = Field(
        default=None,
        description="Record image before change (COBOL: PIC X(400))",
    )
    after_image: Optional[str] = Field(
        default=None,
        description="Record image after change (COBOL: PIC X(400))",
    )
    reason_code: Optional[str] = Field(
        default=None, max_length=4, description="Reason for change"
    )
    process_timestamp: Optional[str] = Field(
        default=None, max_length=26, description="Processing timestamp"
    )
    process_user: Optional[str] = Field(
        default=None, max_length=8, description="Processing user ID"
    )
