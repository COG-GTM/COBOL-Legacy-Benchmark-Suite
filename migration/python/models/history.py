"""
History data models - Python translation of HISTREC.cpy

This module contains dataclasses that correspond to the COBOL copybook
HISTREC.cpy, which defines the history record structure used for
audit trail and change tracking.

Original COBOL Structure:
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

from dataclasses import dataclass
from datetime import datetime
from typing import Optional, Any, Dict
from enum import Enum
import json


class HistoryRecordType(str, Enum):
    """
    History record type codes corresponding to HIST-RECORD-TYPE 88-level conditions.
    
    88  HIST-TYPE-PORT    VALUE 'PT'.
    88  HIST-TYPE-POS     VALUE 'PS'.
    88  HIST-TYPE-TRN     VALUE 'TR'.
    """
    PORTFOLIO = 'PT'
    POSITION = 'PS'
    TRANSACTION = 'TR'


class HistoryActionCode(str, Enum):
    """
    History action codes corresponding to HIST-ACTION-CODE 88-level conditions.
    
    88  HIST-ACTION-ADD   VALUE 'A'.
    88  HIST-ACTION-CHG   VALUE 'C'.
    88  HIST-ACTION-DEL   VALUE 'D'.
    """
    ADD = 'A'
    CHANGE = 'C'
    DELETE = 'D'


@dataclass
class HistoryKey:
    """
    History key structure corresponding to HIST-KEY in HISTREC.cpy.
    
    This composite key uniquely identifies each history record and is used
    for VSAM KSDS file access.
    
    Attributes:
        portfolio_id: Portfolio identifier (8 bytes)
        date: History date in YYYYMMDD format (8 bytes)
        time: History time in HHMMSS format (6 bytes)
        seq_no: Sequence number (4 bytes)
    """
    portfolio_id: str
    date: str  # YYYYMMDD format
    time: str  # HHMMSS format
    seq_no: str
    
    def __post_init__(self):
        """Validate key field lengths matching COBOL PIC definitions."""
        if len(self.portfolio_id) > 8:
            raise ValueError(f"Portfolio ID max 8 characters, got {len(self.portfolio_id)}")
        if len(self.date) != 8:
            raise ValueError(f"Date must be 8 characters (YYYYMMDD), got {len(self.date)}")
        if len(self.time) != 6:
            raise ValueError(f"Time must be 6 characters (HHMMSS), got {len(self.time)}")
        if len(self.seq_no) > 4:
            raise ValueError(f"Sequence number max 4 characters, got {len(self.seq_no)}")
        
        # Pad fields to match COBOL fixed-length format
        self.portfolio_id = self.portfolio_id.ljust(8)
        self.seq_no = self.seq_no.ljust(4)
    
    @property
    def composite_key(self) -> str:
        """Return the full composite key as a single string."""
        return f"{self.portfolio_id}{self.date}{self.time}{self.seq_no}"
    
    @property
    def as_datetime(self) -> datetime:
        """Convert date and time to Python datetime object."""
        return datetime.strptime(f"{self.date}{self.time}", "%Y%m%d%H%M%S")
    
    @classmethod
    def from_datetime(cls, dt: datetime, portfolio_id: str, seq_no: str) -> 'HistoryKey':
        """Create a HistoryKey from a datetime object."""
        return cls(
            portfolio_id=portfolio_id,
            date=dt.strftime("%Y%m%d"),
            time=dt.strftime("%H%M%S"),
            seq_no=seq_no
        )


@dataclass
class HistoryData:
    """
    History data structure corresponding to HIST-DATA in HISTREC.cpy.
    
    Contains the change tracking information including before/after images.
    
    In the COBOL system, before/after images are stored as 400-byte fixed-length
    fields. In Python, we use JSON dictionaries for flexible storage of
    different record types.
    
    Attributes:
        record_type: Type of record being tracked (PT, PS, TR)
        action_code: Type of action (A, C, D)
        before_image: State before the change (JSON dict)
        after_image: State after the change (JSON dict)
        reason_code: Reason for the change (4 bytes)
    """
    record_type: HistoryRecordType
    action_code: HistoryActionCode
    before_image: Optional[Dict[str, Any]]
    after_image: Optional[Dict[str, Any]]
    reason_code: str = ''
    
    def __post_init__(self):
        """Validate data fields."""
        if len(self.reason_code) > 4:
            raise ValueError(f"Reason code max 4 characters, got {len(self.reason_code)}")
        self.reason_code = self.reason_code.ljust(4)
    
    @property
    def before_image_json(self) -> str:
        """Get before image as JSON string."""
        if self.before_image is None:
            return ''
        return json.dumps(self.before_image)
    
    @property
    def after_image_json(self) -> str:
        """Get after image as JSON string."""
        if self.after_image is None:
            return ''
        return json.dumps(self.after_image)
    
    def get_changed_fields(self) -> Dict[str, tuple]:
        """
        Get fields that changed between before and after images.
        
        Returns:
            Dict mapping field names to (before_value, after_value) tuples
        """
        if self.before_image is None or self.after_image is None:
            return {}
        
        changed = {}
        all_keys = set(self.before_image.keys()) | set(self.after_image.keys())
        
        for key in all_keys:
            before_val = self.before_image.get(key)
            after_val = self.after_image.get(key)
            if before_val != after_val:
                changed[key] = (before_val, after_val)
        
        return changed


@dataclass
class HistoryAudit:
    """
    History audit structure corresponding to HIST-AUDIT in HISTREC.cpy.
    
    Contains audit trail information for tracking when and by whom
    the history record was created.
    
    Attributes:
        process_date: Processing timestamp (26 bytes in COBOL)
        process_user: User ID who created the history record (8 bytes)
    """
    process_date: datetime
    process_user: str
    
    def __post_init__(self):
        """Validate audit fields."""
        if len(self.process_user) > 8:
            raise ValueError(f"Process user max 8 characters, got {len(self.process_user)}")
        self.process_user = self.process_user.ljust(8)


@dataclass
class HistoryRecord:
    """
    Complete history record corresponding to HISTORY-RECORD in HISTREC.cpy.
    
    This is the main data structure used for audit trail and change tracking
    throughout the system. It combines the key, data, and audit sections.
    
    The COBOL before/after images (400 bytes each) are translated to
    JSON dictionaries for flexible storage of different record types.
    """
    key: HistoryKey
    data: HistoryData
    audit: Optional[HistoryAudit] = None
    
    @classmethod
    def create_add_record(cls, portfolio_id: str, record_type: HistoryRecordType,
                          after_image: Dict[str, Any], user: str,
                          reason_code: str = 'ADD') -> 'HistoryRecord':
        """
        Create a history record for an ADD action.
        
        Args:
            portfolio_id: Portfolio identifier
            record_type: Type of record being added
            after_image: State of the new record
            user: User performing the action
            reason_code: Reason for the addition
            
        Returns:
            New HistoryRecord
        """
        now = datetime.now()
        key = HistoryKey.from_datetime(now, portfolio_id, '0001')
        data = HistoryData(
            record_type=record_type,
            action_code=HistoryActionCode.ADD,
            before_image=None,
            after_image=after_image,
            reason_code=reason_code
        )
        audit = HistoryAudit(
            process_date=now,
            process_user=user
        )
        return cls(key=key, data=data, audit=audit)
    
    @classmethod
    def create_change_record(cls, portfolio_id: str, record_type: HistoryRecordType,
                             before_image: Dict[str, Any], after_image: Dict[str, Any],
                             user: str, reason_code: str = 'CHG') -> 'HistoryRecord':
        """
        Create a history record for a CHANGE action.
        
        Args:
            portfolio_id: Portfolio identifier
            record_type: Type of record being changed
            before_image: State before the change
            after_image: State after the change
            user: User performing the action
            reason_code: Reason for the change
            
        Returns:
            New HistoryRecord
        """
        now = datetime.now()
        key = HistoryKey.from_datetime(now, portfolio_id, '0001')
        data = HistoryData(
            record_type=record_type,
            action_code=HistoryActionCode.CHANGE,
            before_image=before_image,
            after_image=after_image,
            reason_code=reason_code
        )
        audit = HistoryAudit(
            process_date=now,
            process_user=user
        )
        return cls(key=key, data=data, audit=audit)
    
    @classmethod
    def create_delete_record(cls, portfolio_id: str, record_type: HistoryRecordType,
                             before_image: Dict[str, Any], user: str,
                             reason_code: str = 'DEL') -> 'HistoryRecord':
        """
        Create a history record for a DELETE action.
        
        Args:
            portfolio_id: Portfolio identifier
            record_type: Type of record being deleted
            before_image: State of the deleted record
            user: User performing the action
            reason_code: Reason for the deletion
            
        Returns:
            New HistoryRecord
        """
        now = datetime.now()
        key = HistoryKey.from_datetime(now, portfolio_id, '0001')
        data = HistoryData(
            record_type=record_type,
            action_code=HistoryActionCode.DELETE,
            before_image=before_image,
            after_image=None,
            reason_code=reason_code
        )
        audit = HistoryAudit(
            process_date=now,
            process_user=user
        )
        return cls(key=key, data=data, audit=audit)
    
    @classmethod
    def from_dict(cls, d: dict) -> 'HistoryRecord':
        """Create HistoryRecord from dictionary."""
        key = HistoryKey(
            portfolio_id=d['portfolio_id'],
            date=d['date'],
            time=d['time'],
            seq_no=d['seq_no']
        )
        data = HistoryData(
            record_type=HistoryRecordType(d['record_type']),
            action_code=HistoryActionCode(d['action_code']),
            before_image=d.get('before_image'),
            after_image=d.get('after_image'),
            reason_code=d.get('reason_code', '')
        )
        audit = None
        if 'process_date' in d and d['process_date']:
            audit = HistoryAudit(
                process_date=datetime.fromisoformat(d['process_date'])
                    if isinstance(d['process_date'], str) else d['process_date'],
                process_user=d.get('process_user', '')
            )
        return cls(key=key, data=data, audit=audit)
    
    def to_dict(self) -> dict:
        """Convert HistoryRecord to dictionary for serialization."""
        result = {
            'portfolio_id': self.key.portfolio_id.strip(),
            'date': self.key.date,
            'time': self.key.time,
            'seq_no': self.key.seq_no.strip(),
            'record_type': self.data.record_type.value,
            'action_code': self.data.action_code.value,
            'before_image': self.data.before_image,
            'after_image': self.data.after_image,
            'reason_code': self.data.reason_code.strip(),
        }
        if self.audit:
            result['process_date'] = self.audit.process_date.isoformat()
            result['process_user'] = self.audit.process_user.strip()
        return result
