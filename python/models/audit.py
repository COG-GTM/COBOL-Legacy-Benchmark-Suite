"""Audit trail record model translated from src/copybook/common/AUDITLOG.cpy."""

from datetime import datetime

from pydantic import BaseModel, field_validator

from models.enums import AuditAction, AuditStatus, AuditType


class AuditRecord(BaseModel):
    """Audit trail record (AUDITLOG.cpy AUDIT-RECORD).

    All string fields enforce COBOL PIC lengths as max-length constraints.
    """

    timestamp: datetime
    system_id: str
    user_id: str
    program: str
    terminal: str
    audit_type: AuditType
    action: AuditAction
    status: AuditStatus
    portfolio_id: str
    account_no: str
    before_image: str
    after_image: str
    message: str

    @field_validator("system_id")
    @classmethod
    def validate_system_id(cls, v: str) -> str:
        """System ID max 8 characters (COBOL PIC X(8))."""
        if len(v) > 8:
            raise ValueError("System ID must not exceed 8 characters")
        return v

    @field_validator("user_id")
    @classmethod
    def validate_user_id(cls, v: str) -> str:
        """User ID max 8 characters (COBOL PIC X(8))."""
        if len(v) > 8:
            raise ValueError("User ID must not exceed 8 characters")
        return v

    @field_validator("program")
    @classmethod
    def validate_program(cls, v: str) -> str:
        """Program name max 8 characters (COBOL PIC X(8))."""
        if len(v) > 8:
            raise ValueError("Program name must not exceed 8 characters")
        return v

    @field_validator("terminal")
    @classmethod
    def validate_terminal(cls, v: str) -> str:
        """Terminal ID max 8 characters (COBOL PIC X(8))."""
        if len(v) > 8:
            raise ValueError("Terminal ID must not exceed 8 characters")
        return v

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        """Portfolio ID max 8 characters (COBOL PIC X(8))."""
        if len(v) > 8:
            raise ValueError("Portfolio ID must not exceed 8 characters")
        return v

    @field_validator("account_no")
    @classmethod
    def validate_account_no(cls, v: str) -> str:
        """Account number max 10 characters (COBOL PIC X(10))."""
        if len(v) > 10:
            raise ValueError("Account number must not exceed 10 characters")
        return v

    @field_validator("before_image")
    @classmethod
    def validate_before_image(cls, v: str) -> str:
        """Before image max 100 characters (COBOL PIC X(100))."""
        if len(v) > 100:
            raise ValueError("Before image must not exceed 100 characters")
        return v

    @field_validator("after_image")
    @classmethod
    def validate_after_image(cls, v: str) -> str:
        """After image max 100 characters (COBOL PIC X(100))."""
        if len(v) > 100:
            raise ValueError("After image must not exceed 100 characters")
        return v

    @field_validator("message")
    @classmethod
    def validate_message(cls, v: str) -> str:
        """Message max 100 characters (COBOL PIC X(100))."""
        if len(v) > 100:
            raise ValueError("Message must not exceed 100 characters")
        return v
