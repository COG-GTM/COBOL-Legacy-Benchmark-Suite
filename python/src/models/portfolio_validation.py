"""
Pydantic v2 models for COBOL PORTVAL copybook (Portfolio Validation Rules).

Source: src/copybook/common/PORTVAL.cpy
"""

from decimal import Decimal

from pydantic import BaseModel, Field


class ValidationReturnCodes(BaseModel):
    """
    Validation return codes -- maps to COBOL 01-level VAL-RETURN-CODES.

    Source: src/copybook/common/PORTVAL.cpy
    """

    model_config = {"from_attributes": True}

    val_success: int = Field(
        default=0,
        description="Success. COBOL: VAL-SUCCESS PIC S9(4) VALUE +0.",
    )
    val_invalid_id: int = Field(
        default=1,
        description="Invalid portfolio ID. COBOL: VAL-INVALID-ID PIC S9(4) VALUE +1.",
    )
    val_invalid_acct: int = Field(
        default=2,
        description="Invalid account. COBOL: VAL-INVALID-ACCT PIC S9(4) VALUE +2.",
    )
    val_invalid_type: int = Field(
        default=3,
        description="Invalid type. COBOL: VAL-INVALID-TYPE PIC S9(4) VALUE +3.",
    )
    val_invalid_amt: int = Field(
        default=4,
        description="Invalid amount. COBOL: VAL-INVALID-AMT PIC S9(4) VALUE +4.",
    )


class ValidationErrorMessages(BaseModel):
    """
    Validation error messages -- maps to COBOL 01-level VAL-ERROR-MESSAGES.

    Source: src/copybook/common/PORTVAL.cpy
    """

    model_config = {"from_attributes": True}

    val_err_id: str = Field(
        default="Invalid Portfolio ID format",
        max_length=50,
        description="ID error message. COBOL: VAL-ERR-ID PIC X(50).",
    )
    val_err_acct: str = Field(
        default="Invalid Account Number format",
        max_length=50,
        description="Account error message. COBOL: VAL-ERR-ACCT PIC X(50).",
    )
    val_err_type: str = Field(
        default="Invalid Investment Type",
        max_length=50,
        description="Type error message. COBOL: VAL-ERR-TYPE PIC X(50).",
    )
    val_err_amt: str = Field(
        default="Amount outside valid range",
        max_length=50,
        description="Amount error message. COBOL: VAL-ERR-AMT PIC X(50).",
    )


class ValidationConstants(BaseModel):
    """
    Validation constants -- maps to COBOL 01-level VAL-CONSTANTS.

    Source: src/copybook/common/PORTVAL.cpy
    """

    model_config = {"from_attributes": True}

    val_min_amount: Decimal = Field(
        default=Decimal("-9999999999999.99"),
        max_digits=15,
        decimal_places=2,
        description="Minimum valid amount. COBOL: VAL-MIN-AMOUNT PIC S9(13)V99.",
    )
    val_max_amount: Decimal = Field(
        default=Decimal("9999999999999.99"),
        max_digits=15,
        decimal_places=2,
        description="Maximum valid amount. COBOL: VAL-MAX-AMOUNT PIC S9(13)V99.",
    )
    val_id_prefix: str = Field(
        default="PORT",
        max_length=4,
        description="Portfolio ID prefix. COBOL: VAL-ID-PREFIX PIC X(4) VALUE 'PORT'.",
    )


class ValidationWorkAreas(BaseModel):
    """
    Validation working storage -- maps to COBOL 01-level VAL-WORK-AREAS.

    Source: src/copybook/common/PORTVAL.cpy
    """

    model_config = {"from_attributes": True}

    val_numeric_check: str = Field(
        default="",
        max_length=10,
        description="Numeric check area. COBOL: VAL-NUMERIC-CHECK PIC X(10).",
    )
    val_temp_num: Decimal = Field(
        default=Decimal("0"),
        max_digits=15,
        decimal_places=2,
        description="Temporary numeric field. COBOL: VAL-TEMP-NUM PIC S9(13)V99.",
    )
    val_error_code: int = Field(
        default=0,
        description="Working error code. COBOL: VAL-ERROR-CODE PIC S9(4).",
    )
    val_error_msg: str = Field(
        default="",
        max_length=50,
        description="Working error message. COBOL: VAL-ERROR-MSG PIC X(50).",
    )
