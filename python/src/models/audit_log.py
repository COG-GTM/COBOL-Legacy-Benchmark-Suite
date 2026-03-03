"""
Pydantic v2 models for COBOL AUDITLOG copybook (Audit Trail Record).

Source: src/copybook/common/AUDITLOG.cpy
"""

from pydantic import BaseModel, Field, field_validator


class AuditHeader(BaseModel):
    """Audit header fields from AUD-HEADER (level 05)."""

    model_config = {"from_attributes": True}

    aud_timestamp: str = Field(
        max_length=26,
        description="Audit timestamp. COBOL: AUD-TIMESTAMP PIC X(26).",
    )
    aud_system_id: str = Field(
        max_length=8,
        description="System identifier. COBOL: AUD-SYSTEM-ID PIC X(8).",
    )
    aud_user_id: str = Field(
        max_length=8,
        description="User identifier. COBOL: AUD-USER-ID PIC X(8).",
    )
    aud_program: str = Field(
        max_length=8,
        description="Program name. COBOL: AUD-PROGRAM PIC X(8).",
    )
    aud_terminal: str = Field(
        max_length=8,
        description="Terminal identifier. COBOL: AUD-TERMINAL PIC X(8).",
    )


class AuditKeyInfo(BaseModel):
    """Audit key information from AUD-KEY-INFO (level 05)."""

    model_config = {"from_attributes": True}

    aud_portfolio_id: str = Field(
        max_length=8,
        description="Portfolio identifier. COBOL: AUD-PORTFOLIO-ID PIC X(8).",
    )
    aud_account_no: str = Field(
        max_length=10,
        description="Account number. COBOL: AUD-ACCOUNT-NO PIC X(10).",
    )


class AuditRecord(BaseModel):
    """
    Audit Trail Record -- maps to COBOL 01-level AUDIT-RECORD.

    Source: src/copybook/common/AUDITLOG.cpy
    """

    model_config = {"from_attributes": True}

    aud_header: AuditHeader = Field(description="Audit header (AUD-HEADER).")
    aud_type: str = Field(
        max_length=4,
        description=(
            "Audit type: TRAN=Transaction, USER=User Action, SYST=System Event. "
            "COBOL: AUD-TYPE PIC X(4). "
            "88-level values: TRAN, USER, SYST."
        ),
    )
    aud_action: str = Field(
        max_length=8,
        description=(
            "Audit action. "
            "COBOL: AUD-ACTION PIC X(8). "
            "88-level values: CREATE, UPDATE, DELETE, INQUIRE, LOGIN, LOGOUT, STARTUP, SHUTDOWN."
        ),
    )
    aud_status: str = Field(
        max_length=4,
        description=(
            "Audit status: SUCC=Success, FAIL=Failure, WARN=Warning. "
            "COBOL: AUD-STATUS PIC X(4). "
            "88-level values: SUCC, FAIL, WARN."
        ),
    )
    aud_key_info: AuditKeyInfo = Field(description="Key information (AUD-KEY-INFO).")
    aud_before_image: str = Field(
        max_length=100,
        description="Record image before change. COBOL: AUD-BEFORE-IMAGE PIC X(100).",
    )
    aud_after_image: str = Field(
        max_length=100,
        description="Record image after change. COBOL: AUD-AFTER-IMAGE PIC X(100).",
    )
    aud_message: str = Field(
        max_length=100,
        description="Audit message. COBOL: AUD-MESSAGE PIC X(100).",
    )

    @field_validator("aud_type")
    @classmethod
    def validate_aud_type(cls, v: str) -> str:
        valid = {"TRAN", "USER", "SYST"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"aud_type must be one of {valid}")
        return v

    @field_validator("aud_status")
    @classmethod
    def validate_aud_status(cls, v: str) -> str:
        valid = {"SUCC", "FAIL", "WARN"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"aud_status must be one of {valid}")
        return v

    @field_validator("aud_action")
    @classmethod
    def validate_aud_action(cls, v: str) -> str:
        valid = {"CREATE", "UPDATE", "DELETE", "INQUIRE", "LOGIN", "LOGOUT", "STARTUP", "SHUTDOWN"}
        stripped = v.strip()
        if stripped and stripped not in valid:
            raise ValueError(f"aud_action must be one of {valid}")
        return v
