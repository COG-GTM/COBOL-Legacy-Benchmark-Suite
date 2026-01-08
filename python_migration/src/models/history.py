"""
History model - Maps to COBOL HISTREC.cpy
Represents audit history records for tracking changes.
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional
from enum import Enum

from sqlalchemy import Column, Integer, String, Text, DateTime, Index
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class HistoryRecordType(str, Enum):
    """History record type codes - maps to HIST-RECORD-TYPE 88 levels"""
    PORTFOLIO = 'PT'
    POSITION = 'PS'
    TRANSACTION = 'TR'


class HistoryActionCode(str, Enum):
    """History action codes - maps to HIST-ACTION-CODE 88 levels"""
    ADD = 'A'
    CHANGE = 'C'
    DELETE = 'D'


@dataclass
class HistoryRecord:
    """
    History record dataclass - maps to COBOL HISTREC.cpy
    
    Original COBOL structure:
    01  HISTORY-RECORD.
        05  HIST-KEY.
            10  HIST-PORTFOLIO-ID  PIC X(08).
            10  HIST-DATE          PIC X(08).
            10  HIST-TIME          PIC X(06).
            10  HIST-SEQ-NO        PIC X(04).
        05  HIST-DATA.
            10  HIST-RECORD-TYPE   PIC X(02).
            10  HIST-ACTION-CODE   PIC X(01).
            10  HIST-BEFORE-IMAGE  PIC X(400).
            10  HIST-AFTER-IMAGE   PIC X(400).
            10  HIST-REASON-CODE   PIC X(04).
        05  HIST-AUDIT.
            10  HIST-PROCESS-DATE  PIC X(26).
            10  HIST-PROCESS-USER  PIC X(08).
    """
    portfolio_id: str
    date: str
    time: str
    seq_no: str
    record_type: HistoryRecordType
    action_code: HistoryActionCode
    before_image: str = ''
    after_image: str = ''
    reason_code: str = ''
    process_date: Optional[datetime] = None
    process_user: Optional[str] = None

    def __post_init__(self):
        """Ensure proper types after initialization"""
        if isinstance(self.record_type, str):
            self.record_type = HistoryRecordType(self.record_type)
        if isinstance(self.action_code, str):
            self.action_code = HistoryActionCode(self.action_code)

    @property
    def key(self) -> str:
        """Return composite key matching COBOL HIST-KEY"""
        return f"{self.portfolio_id}{self.date}{self.time}{self.seq_no}"

    def validate(self) -> tuple[bool, list[str]]:
        """
        Validate history record fields.
        Returns (is_valid, list of error messages)
        """
        errors = []
        
        if not self.portfolio_id or len(self.portfolio_id) > 8:
            errors.append("E001: Invalid portfolio ID (must be 1-8 characters)")
        
        if not self.date or len(self.date) != 8 or not self.date.isdigit():
            errors.append("E002: Invalid date format (must be YYYYMMDD)")
        
        if not self.time or len(self.time) != 6 or not self.time.isdigit():
            errors.append("E003: Invalid time format (must be HHMMSS)")
        
        if not self.seq_no or len(self.seq_no) > 4:
            errors.append("E004: Invalid sequence number (must be 1-4 characters)")
        
        if len(self.reason_code) > 4:
            errors.append("E005: Invalid reason code (must be 1-4 characters)")
        
        return len(errors) == 0, errors

    def to_dict(self) -> dict:
        """Convert to dictionary for serialization"""
        return {
            'portfolio_id': self.portfolio_id,
            'date': self.date,
            'time': self.time,
            'seq_no': self.seq_no,
            'record_type': self.record_type.value,
            'action_code': self.action_code.value,
            'before_image': self.before_image,
            'after_image': self.after_image,
            'reason_code': self.reason_code,
            'process_date': self.process_date.isoformat() if self.process_date else None,
            'process_user': self.process_user,
        }

    @classmethod
    def from_dict(cls, data: dict) -> 'HistoryRecord':
        """Create from dictionary"""
        return cls(
            portfolio_id=data['portfolio_id'],
            date=data['date'],
            time=data['time'],
            seq_no=data['seq_no'],
            record_type=HistoryRecordType(data['record_type']),
            action_code=HistoryActionCode(data['action_code']),
            before_image=data.get('before_image', ''),
            after_image=data.get('after_image', ''),
            reason_code=data.get('reason_code', ''),
            process_date=datetime.fromisoformat(data['process_date']) if data.get('process_date') else None,
            process_user=data.get('process_user'),
        )


class History(Base):
    """SQLAlchemy model for history table - replaces VSAM history file"""
    __tablename__ = 'history'

    id = Column(Integer, primary_key=True, autoincrement=True)
    portfolio_id = Column(String(8), nullable=False, index=True)
    date = Column(String(8), nullable=False)
    time = Column(String(6), nullable=False)
    seq_no = Column(String(4), nullable=False)
    record_type = Column(String(2), nullable=False)
    action_code = Column(String(1), nullable=False)
    before_image = Column(Text, nullable=True)
    after_image = Column(Text, nullable=True)
    reason_code = Column(String(4), nullable=True)
    process_date = Column(DateTime, nullable=True)
    process_user = Column(String(8), nullable=True)

    __table_args__ = (
        Index('ix_history_portfolio', 'portfolio_id'),
        Index('ix_history_date', 'date'),
        Index('ix_history_record_type', 'record_type'),
    )

    def to_record(self) -> HistoryRecord:
        """Convert to HistoryRecord dataclass"""
        return HistoryRecord(
            portfolio_id=self.portfolio_id,
            date=self.date,
            time=self.time,
            seq_no=self.seq_no,
            record_type=HistoryRecordType(self.record_type),
            action_code=HistoryActionCode(self.action_code),
            before_image=self.before_image or '',
            after_image=self.after_image or '',
            reason_code=self.reason_code or '',
            process_date=self.process_date,
            process_user=self.process_user,
        )

    @classmethod
    def from_record(cls, record: HistoryRecord) -> 'History':
        """Create from HistoryRecord dataclass"""
        return cls(
            portfolio_id=record.portfolio_id,
            date=record.date,
            time=record.time,
            seq_no=record.seq_no,
            record_type=record.record_type.value,
            action_code=record.action_code.value,
            before_image=record.before_image,
            after_image=record.after_image,
            reason_code=record.reason_code,
            process_date=record.process_date,
            process_user=record.process_user,
        )

    def __repr__(self):
        return f"<History(portfolio={self.portfolio_id}, date={self.date}, action={self.action_code})>"
