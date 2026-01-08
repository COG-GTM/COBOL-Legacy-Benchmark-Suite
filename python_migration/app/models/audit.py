"""Audit Record model - converted from AUDITLOG.cpy.

COBOL Original:
01  AUDIT-RECORD.
    05  AUD-HEADER.
        10  AUD-TIMESTAMP     PIC X(26).
        10  AUD-SYSTEM-ID     PIC X(8).
        10  AUD-USER-ID       PIC X(8).
        10  AUD-PROGRAM       PIC X(8).
        10  AUD-TERMINAL      PIC X(8).
    05  AUD-TYPE             PIC X(4).
    05  AUD-ACTION           PIC X(8).
    05  AUD-STATUS           PIC X(4).
    05  AUD-KEY-INFO.
        10  AUD-PORTFOLIO-ID  PIC X(8).
        10  AUD-ACCOUNT-NO    PIC X(10).
    05  AUD-BEFORE-IMAGE     PIC X(100).
    05  AUD-AFTER-IMAGE      PIC X(100).
    05  AUD-MESSAGE          PIC X(100).
"""

from datetime import datetime
from enum import Enum

from pydantic import BaseModel, Field


class AuditType(str, Enum):
    """Audit type codes - maps to 88-level conditions in COBOL."""

    TRANSACTION = "TRAN"  # AUD-TRANSACTION
    USER_ACTION = "USER"  # AUD-USER-ACTION
    SYSTEM_EVENT = "SYST"  # AUD-SYSTEM-EVENT


class AuditAction(str, Enum):
    """Audit action codes - maps to 88-level conditions in COBOL."""

    CREATE = "CREATE"  # AUD-CREATE
    UPDATE = "UPDATE"  # AUD-UPDATE
    DELETE = "DELETE"  # AUD-DELETE
    INQUIRE = "INQUIRE"  # AUD-INQUIRE
    LOGIN = "LOGIN"  # AUD-LOGIN
    LOGOUT = "LOGOUT"  # AUD-LOGOUT
    STARTUP = "STARTUP"  # AUD-STARTUP
    SHUTDOWN = "SHUTDOWN"  # AUD-SHUTDOWN


class AuditStatus(str, Enum):
    """Audit status codes - maps to 88-level conditions in COBOL."""

    SUCCESS = "SUCC"  # AUD-SUCCESS
    FAILURE = "FAIL"  # AUD-FAILURE
    WARNING = "WARN"  # AUD-WARNING


class AuditHeader(BaseModel):
    """Audit header structure - maps to AUD-HEADER in COBOL."""

    timestamp: datetime = Field(description="Audit timestamp")
    system_id: str = Field(max_length=8, description="System identifier")
    user_id: str = Field(max_length=8, description="User identifier")
    program: str = Field(max_length=8, description="Program name")
    terminal: str = Field(default="", max_length=8, description="Terminal identifier")


class AuditKeyInfo(BaseModel):
    """Audit key information structure - maps to AUD-KEY-INFO in COBOL."""

    portfolio_id: str = Field(default="", max_length=8, description="Portfolio ID")
    account_no: str = Field(default="", max_length=10, description="Account number")


class AuditRecord(BaseModel):
    """Complete audit record - maps to AUDIT-RECORD in COBOL.

    This model represents an audit trail entry for tracking system
    activities, user actions, and transactions.
    """

    header: AuditHeader
    type: AuditType = Field(description="Audit type")
    action: AuditAction = Field(description="Action performed")
    status: AuditStatus = Field(description="Action status")
    key_info: AuditKeyInfo = Field(default_factory=AuditKeyInfo)
    before_image: str = Field(
        default="", max_length=100, description="State before action"
    )
    after_image: str = Field(
        default="", max_length=100, description="State after action"
    )
    message: str = Field(default="", max_length=100, description="Audit message")

    @property
    def user_id(self) -> str:
        """Convenience accessor for user ID."""
        return self.header.user_id

    @property
    def program(self) -> str:
        """Convenience accessor for program name."""
        return self.header.program

    @property
    def is_success(self) -> bool:
        """Check if audit status is success."""
        return self.status == AuditStatus.SUCCESS

    @property
    def is_failure(self) -> bool:
        """Check if audit status is failure."""
        return self.status == AuditStatus.FAILURE

    def to_flat_dict(self) -> dict:
        """Convert to flat dictionary for database operations."""
        return {
            "timestamp": self.header.timestamp,
            "system_id": self.header.system_id,
            "user_id": self.header.user_id,
            "program": self.header.program,
            "terminal": self.header.terminal,
            "audit_type": self.type.value,
            "action": self.action.value,
            "status": self.status.value,
            "portfolio_id": self.key_info.portfolio_id,
            "account_no": self.key_info.account_no,
            "before_image": self.before_image,
            "after_image": self.after_image,
            "message": self.message,
        }

    @classmethod
    def create_login_audit(
        cls,
        user_id: str,
        system_id: str,
        terminal: str = "",
        success: bool = True,
        message: str = "",
    ) -> "AuditRecord":
        """Factory method to create a login audit record."""
        return cls(
            header=AuditHeader(
                timestamp=datetime.now(),
                system_id=system_id,
                user_id=user_id,
                program="SECMGR",
                terminal=terminal,
            ),
            type=AuditType.USER_ACTION,
            action=AuditAction.LOGIN,
            status=AuditStatus.SUCCESS if success else AuditStatus.FAILURE,
            message=message or ("Login successful" if success else "Login failed"),
        )

    @classmethod
    def create_transaction_audit(
        cls,
        user_id: str,
        system_id: str,
        program: str,
        action: AuditAction,
        portfolio_id: str = "",
        account_no: str = "",
        before_image: str = "",
        after_image: str = "",
        success: bool = True,
        message: str = "",
    ) -> "AuditRecord":
        """Factory method to create a transaction audit record."""
        return cls(
            header=AuditHeader(
                timestamp=datetime.now(),
                system_id=system_id,
                user_id=user_id,
                program=program,
                terminal="",
            ),
            type=AuditType.TRANSACTION,
            action=action,
            status=AuditStatus.SUCCESS if success else AuditStatus.FAILURE,
            key_info=AuditKeyInfo(portfolio_id=portfolio_id, account_no=account_no),
            before_image=before_image,
            after_image=after_image,
            message=message,
        )
