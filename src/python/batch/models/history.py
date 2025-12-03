"""
History Record Model

Corresponds to COBOL copybook: HISTREC.cpy
Defines the structure for audit history records in the portfolio management system.
"""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional


class HistoryRecordType(Enum):
    """History record type codes matching COBOL 88-level conditions."""
    PORTFOLIO = "PT"
    POSITION = "PS"
    TRANSACTION = "TR"


class HistoryActionCode(Enum):
    """History action codes matching COBOL 88-level conditions."""
    ADD = "A"
    CHANGE = "C"
    DELETE = "D"


@dataclass
class HistoryKey:
    """
    History key structure.
    
    Corresponds to HIST-KEY in HISTREC.cpy:
    - HIST-PORTFOLIO-ID: PIC X(08)
    - HIST-DATE: PIC X(08) - YYYYMMDD
    - HIST-TIME: PIC X(06) - HHMMSS
    - HIST-SEQ-NO: PIC X(04)
    """
    portfolio_id: str
    date: str  # YYYYMMDD format
    time: str  # HHMMSS format
    seq_no: str

    def __post_init__(self) -> None:
        self.portfolio_id = str(self.portfolio_id).ljust(8)[:8]
        self.date = str(self.date).ljust(8)[:8]
        self.time = str(self.time).ljust(6)[:6]
        self.seq_no = str(self.seq_no).ljust(4)[:4]

    def to_string(self) -> str:
        """Convert key to string for comparison and storage."""
        return f"{self.portfolio_id}{self.date}{self.time}{self.seq_no}"

    @classmethod
    def from_string(cls, key_string: str) -> "HistoryKey":
        """Parse key from string representation."""
        return cls(
            portfolio_id=key_string[0:8],
            date=key_string[8:16],
            time=key_string[16:22],
            seq_no=key_string[22:26],
        )


@dataclass
class HistoryData:
    """
    History data structure.
    
    Corresponds to HIST-DATA in HISTREC.cpy:
    - HIST-RECORD-TYPE: PIC X(02)
    - HIST-ACTION-CODE: PIC X(01)
    - HIST-BEFORE-IMAGE: PIC X(400)
    - HIST-AFTER-IMAGE: PIC X(400)
    - HIST-REASON-CODE: PIC X(04)
    """
    record_type: HistoryRecordType
    action_code: HistoryActionCode
    before_image: str = ""
    after_image: str = ""
    reason_code: str = ""

    def __post_init__(self) -> None:
        if isinstance(self.record_type, str):
            self.record_type = HistoryRecordType(self.record_type)
        if isinstance(self.action_code, str):
            self.action_code = HistoryActionCode(self.action_code)
        self.before_image = str(self.before_image).ljust(400)[:400]
        self.after_image = str(self.after_image).ljust(400)[:400]
        self.reason_code = str(self.reason_code).ljust(4)[:4]


@dataclass
class HistoryAudit:
    """
    History audit structure.
    
    Corresponds to HIST-AUDIT in HISTREC.cpy:
    - HIST-PROCESS-DATE: PIC X(26)
    - HIST-PROCESS-USER: PIC X(08)
    """
    process_date: str = ""
    process_user: str = ""

    def __post_init__(self) -> None:
        if not self.process_date:
            self.process_date = datetime.now().isoformat()
        self.process_date = str(self.process_date).ljust(26)[:26]
        self.process_user = str(self.process_user).ljust(8)[:8]


@dataclass
class HistoryRecord:
    """
    Complete history record structure.
    
    Corresponds to HISTORY-RECORD in HISTREC.cpy.
    Total record length matches COBOL definition.
    """
    key: HistoryKey
    data: HistoryData
    audit: HistoryAudit = field(default_factory=HistoryAudit)
    filler: str = ""

    def __post_init__(self) -> None:
        self.filler = " " * 50

    @property
    def portfolio_id(self) -> str:
        return self.key.portfolio_id

    @property
    def date(self) -> str:
        return self.key.date

    @property
    def time(self) -> str:
        return self.key.time

    @property
    def seq_no(self) -> str:
        return self.key.seq_no

    @property
    def record_type(self) -> HistoryRecordType:
        return self.data.record_type

    @property
    def action_code(self) -> HistoryActionCode:
        return self.data.action_code

    @property
    def before_image(self) -> str:
        return self.data.before_image

    @property
    def after_image(self) -> str:
        return self.data.after_image

    @property
    def reason_code(self) -> str:
        return self.data.reason_code

    def is_portfolio_record(self) -> bool:
        return self.data.record_type == HistoryRecordType.PORTFOLIO

    def is_position_record(self) -> bool:
        return self.data.record_type == HistoryRecordType.POSITION

    def is_transaction_record(self) -> bool:
        return self.data.record_type == HistoryRecordType.TRANSACTION

    def is_add_action(self) -> bool:
        return self.data.action_code == HistoryActionCode.ADD

    def is_change_action(self) -> bool:
        return self.data.action_code == HistoryActionCode.CHANGE

    def is_delete_action(self) -> bool:
        return self.data.action_code == HistoryActionCode.DELETE

    def to_dict(self) -> dict:
        """Convert record to dictionary for serialization."""
        return {
            "key": {
                "portfolio_id": self.key.portfolio_id,
                "date": self.key.date,
                "time": self.key.time,
                "seq_no": self.key.seq_no,
            },
            "data": {
                "record_type": self.data.record_type.value,
                "action_code": self.data.action_code.value,
                "before_image": self.data.before_image,
                "after_image": self.data.after_image,
                "reason_code": self.data.reason_code,
            },
            "audit": {
                "process_date": self.audit.process_date,
                "process_user": self.audit.process_user,
            },
        }

    @classmethod
    def from_dict(cls, data: dict) -> "HistoryRecord":
        """Create record from dictionary."""
        return cls(
            key=HistoryKey(
                portfolio_id=data["key"]["portfolio_id"],
                date=data["key"]["date"],
                time=data["key"]["time"],
                seq_no=data["key"]["seq_no"],
            ),
            data=HistoryData(
                record_type=HistoryRecordType(data["data"]["record_type"]),
                action_code=HistoryActionCode(data["data"]["action_code"]),
                before_image=data["data"]["before_image"],
                after_image=data["data"]["after_image"],
                reason_code=data["data"]["reason_code"],
            ),
            audit=HistoryAudit(
                process_date=data["audit"]["process_date"],
                process_user=data["audit"]["process_user"],
            ),
        )

    @classmethod
    def create_for_transaction(
        cls,
        portfolio_id: str,
        action: HistoryActionCode,
        before_image: str = "",
        after_image: str = "",
        reason_code: str = "",
        user: str = "SYSTEM",
    ) -> "HistoryRecord":
        """Factory method to create a transaction history record."""
        now = datetime.now()
        return cls(
            key=HistoryKey(
                portfolio_id=portfolio_id,
                date=now.strftime("%Y%m%d"),
                time=now.strftime("%H%M%S"),
                seq_no="0001",
            ),
            data=HistoryData(
                record_type=HistoryRecordType.TRANSACTION,
                action_code=action,
                before_image=before_image,
                after_image=after_image,
                reason_code=reason_code,
            ),
            audit=HistoryAudit(
                process_date=now.isoformat(),
                process_user=user,
            ),
        )

    @classmethod
    def create_for_position(
        cls,
        portfolio_id: str,
        action: HistoryActionCode,
        before_image: str = "",
        after_image: str = "",
        reason_code: str = "",
        user: str = "SYSTEM",
    ) -> "HistoryRecord":
        """Factory method to create a position history record."""
        now = datetime.now()
        return cls(
            key=HistoryKey(
                portfolio_id=portfolio_id,
                date=now.strftime("%Y%m%d"),
                time=now.strftime("%H%M%S"),
                seq_no="0001",
            ),
            data=HistoryData(
                record_type=HistoryRecordType.POSITION,
                action_code=action,
                before_image=before_image,
                after_image=after_image,
                reason_code=reason_code,
            ),
            audit=HistoryAudit(
                process_date=now.isoformat(),
                process_user=user,
            ),
        )
