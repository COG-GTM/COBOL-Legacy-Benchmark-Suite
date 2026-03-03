"""
Pydantic v2 models for COBOL ERRHND copybook (Online Error Handling).

Source: src/copybook/online/ERRHND.cpy
"""

from pydantic import BaseModel, Field, field_validator


class ErrorTrace(BaseModel):
    """Error trace from ERR-TRACE (level 05)."""

    model_config = {"from_attributes": True}

    err_trace_id: str = Field(
        max_length=16,
        description="Trace identifier. COBOL: ERR-TRACE-ID PIC X(16).",
    )
    err_timestamp: str = Field(
        max_length=26,
        description="Error timestamp. COBOL: ERR-TIMESTAMP PIC X(26).",
    )


class OnlineErrorHandling(BaseModel):
    """
    Online Error Handling -- maps to COBOL 01-level ERROR-HANDLING.

    Source: src/copybook/online/ERRHND.cpy
    """

    model_config = {"from_attributes": True}

    err_program: str = Field(
        max_length=8,
        description="Program name. COBOL: ERR-PROGRAM PIC X(8).",
    )
    err_paragraph: str = Field(
        max_length=30,
        description="Paragraph name. COBOL: ERR-PARAGRAPH PIC X(30).",
    )
    err_sqlcode: int = Field(
        description="SQL return code. COBOL: ERR-SQLCODE PIC S9(9) COMP.",
    )
    err_cics_resp: int = Field(
        description="CICS response code. COBOL: ERR-CICS-RESP PIC S9(8) COMP.",
    )
    err_cics_resp2: int = Field(
        description="CICS response code 2. COBOL: ERR-CICS-RESP2 PIC S9(8) COMP.",
    )
    err_severity: str = Field(
        max_length=1,
        description=(
            "Severity: F=Fatal, W=Warning, I=Info. "
            "COBOL: ERR-SEVERITY PIC X. "
            "88-level values: F, W, I."
        ),
    )
    err_message: str = Field(
        max_length=80,
        description="Error message. COBOL: ERR-MESSAGE PIC X(80).",
    )
    err_action: str = Field(
        max_length=1,
        description=(
            "Error action: R=Return, C=Continue, A=Abend. "
            "COBOL: ERR-ACTION PIC X. "
            "88-level values: R, C, A."
        ),
    )
    err_trace: ErrorTrace = Field(description="Error trace (ERR-TRACE).")

    @field_validator("err_severity")
    @classmethod
    def validate_severity(cls, v: str) -> str:
        valid = {"F", "W", "I"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"err_severity must be one of {valid}")
        return v

    @field_validator("err_action")
    @classmethod
    def validate_action(cls, v: str) -> str:
        valid = {"R", "C", "A"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"err_action must be one of {valid}")
        return v
