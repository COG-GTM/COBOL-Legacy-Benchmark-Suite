"""
Pydantic v2 models for COBOL ERRHAND copybook (Standard Error Handling Definitions).

Source: src/copybook/common/ERRHAND.cpy
"""

from pydantic import BaseModel, Field


class ErrorCategories(BaseModel):
    """
    Error categories -- maps to COBOL 01-level ERR-CATEGORIES.

    Source: src/copybook/common/ERRHAND.cpy
    """

    model_config = {"from_attributes": True}

    err_cat_vsam: str = Field(
        default="VS",
        max_length=2,
        description="VSAM error category. COBOL: ERR-CAT-VSAM PIC X(2) VALUE 'VS'.",
    )
    err_cat_valid: str = Field(
        default="VL",
        max_length=2,
        description="Validation error category. COBOL: ERR-CAT-VALID PIC X(2) VALUE 'VL'.",
    )
    err_cat_proc: str = Field(
        default="PR",
        max_length=2,
        description="Processing error category. COBOL: ERR-CAT-PROC PIC X(2) VALUE 'PR'.",
    )
    err_cat_system: str = Field(
        default="SY",
        max_length=2,
        description="System error category. COBOL: ERR-CAT-SYSTEM PIC X(2) VALUE 'SY'.",
    )


class ErrorReturnCodes(BaseModel):
    """
    Error return codes -- maps to COBOL 01-level ERR-RETURN-CODES.

    Source: src/copybook/common/ERRHAND.cpy
    """

    model_config = {"from_attributes": True}

    err_success: int = Field(
        default=0,
        description="Success. COBOL: ERR-SUCCESS PIC S9(4) COMP VALUE +0.",
    )
    err_warning: int = Field(
        default=4,
        description="Warning. COBOL: ERR-WARNING PIC S9(4) COMP VALUE +4.",
    )
    err_error: int = Field(
        default=8,
        description="Error. COBOL: ERR-ERROR PIC S9(4) COMP VALUE +8.",
    )
    err_severe: int = Field(
        default=12,
        description="Severe error. COBOL: ERR-SEVERE PIC S9(4) COMP VALUE +12.",
    )
    err_terminal: int = Field(
        default=16,
        description="Terminal error. COBOL: ERR-TERMINAL PIC S9(4) COMP VALUE +16.",
    )


class ErrorTimestamp(BaseModel):
    """Error timestamp from ERR-TIMESTAMP (level 05)."""

    model_config = {"from_attributes": True}

    err_date: str = Field(
        max_length=10,
        description="Error date. COBOL: ERR-DATE PIC X(10).",
    )
    err_time: str = Field(
        max_length=8,
        description="Error time. COBOL: ERR-TIME PIC X(8).",
    )


class ErrorMessage(BaseModel):
    """
    Error message structure -- maps to COBOL 01-level ERR-MESSAGE.

    Source: src/copybook/common/ERRHAND.cpy
    """

    model_config = {"from_attributes": True}

    err_timestamp: ErrorTimestamp = Field(
        description="Error timestamp (ERR-TIMESTAMP).",
    )
    err_program: str = Field(
        max_length=8,
        description="Program name. COBOL: ERR-PROGRAM PIC X(8).",
    )
    err_category: str = Field(
        max_length=2,
        description="Error category. COBOL: ERR-CATEGORY PIC X(2).",
    )
    err_code: str = Field(
        max_length=4,
        description="Error code. COBOL: ERR-CODE PIC X(4).",
    )
    err_severity: int = Field(
        description="Error severity. COBOL: ERR-SEVERITY PIC S9(4) COMP.",
    )
    err_text: str = Field(
        max_length=80,
        description="Error text. COBOL: ERR-TEXT PIC X(80).",
    )
    err_details: str = Field(
        max_length=256,
        description="Error details. COBOL: ERR-DETAILS PIC X(256).",
    )


class VsamStatuses(BaseModel):
    """
    VSAM status handling -- maps to COBOL 01-level ERR-VSAM-STATUSES.

    Source: src/copybook/common/ERRHAND.cpy
    """

    model_config = {"from_attributes": True}

    err_vsam_success: str = Field(
        default="00",
        max_length=2,
        description="VSAM success. COBOL: ERR-VSAM-SUCCESS PIC X(2) VALUE '00'.",
    )
    err_vsam_dupkey: str = Field(
        default="22",
        max_length=2,
        description="Duplicate key. COBOL: ERR-VSAM-DUPKEY PIC X(2) VALUE '22'.",
    )
    err_vsam_notfnd: str = Field(
        default="23",
        max_length=2,
        description="Record not found. COBOL: ERR-VSAM-NOTFND PIC X(2) VALUE '23'.",
    )
    err_vsam_eof: str = Field(
        default="10",
        max_length=2,
        description="End of file. COBOL: ERR-VSAM-EOF PIC X(2) VALUE '10'.",
    )


class VsamMessages(BaseModel):
    """
    VSAM error messages -- maps to COBOL 01-level ERR-VSAM-MSGS.

    Source: src/copybook/common/ERRHAND.cpy
    """

    model_config = {"from_attributes": True}

    err_vsam_22: str = Field(
        default="Duplicate record key",
        max_length=80,
        description="Duplicate key message. COBOL: ERR-VSAM-22 PIC X(80).",
    )
    err_vsam_23: str = Field(
        default="Record not found",
        max_length=80,
        description="Not found message. COBOL: ERR-VSAM-23 PIC X(80).",
    )
    err_other: str = Field(
        default="Unexpected VSAM error",
        max_length=80,
        description="Other VSAM error. COBOL: ERR-OTHER PIC X(80).",
    )
