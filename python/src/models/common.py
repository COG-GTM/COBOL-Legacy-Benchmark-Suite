"""
Pydantic v2 models for COBOL COMMON copybook (Common Definitions and Constants).

Source: src/copybook/common/COMMON.cpy
"""

from pydantic import BaseModel, Field


class ReturnCodes(BaseModel):
    """
    Return codes -- maps to COBOL 01-level RETURN-CODES.

    Source: src/copybook/common/COMMON.cpy
    """

    model_config = {"from_attributes": True}

    rc_success: int = Field(
        default=0,
        description="Success return code. COBOL: RC-SUCCESS PIC S9(4) VALUE +0.",
    )
    rc_warning: int = Field(
        default=4,
        description="Warning return code. COBOL: RC-WARNING PIC S9(4) VALUE +4.",
    )
    rc_error: int = Field(
        default=8,
        description="Error return code. COBOL: RC-ERROR PIC S9(4) VALUE +8.",
    )
    rc_severe: int = Field(
        default=12,
        description="Severe return code. COBOL: RC-SEVERE PIC S9(4) VALUE +12.",
    )
    rc_critical: int = Field(
        default=16,
        description="Critical return code. COBOL: RC-CRITICAL PIC S9(4) VALUE +16.",
    )


class StatusCodes(BaseModel):
    """
    Status codes -- maps to COBOL 01-level STATUS-CODES.

    Source: src/copybook/common/COMMON.cpy
    """

    model_config = {"from_attributes": True}

    status_active: str = Field(
        default="A",
        max_length=1,
        description="Active status. COBOL: STATUS-ACTIVE PIC X(01) VALUE 'A'.",
    )
    status_closed: str = Field(
        default="C",
        max_length=1,
        description="Closed status. COBOL: STATUS-CLOSED PIC X(01) VALUE 'C'.",
    )
    status_pending: str = Field(
        default="P",
        max_length=1,
        description="Pending status. COBOL: STATUS-PENDING PIC X(01) VALUE 'P'.",
    )
    status_suspended: str = Field(
        default="S",
        max_length=1,
        description="Suspended status. COBOL: STATUS-SUSPENDED PIC X(01) VALUE 'S'.",
    )
    status_failed: str = Field(
        default="F",
        max_length=1,
        description="Failed status. COBOL: STATUS-FAILED PIC X(01) VALUE 'F'.",
    )
    status_reversed: str = Field(
        default="R",
        max_length=1,
        description="Reversed status. COBOL: STATUS-REVERSED PIC X(01) VALUE 'R'.",
    )


class TransactionTypes(BaseModel):
    """
    Transaction types -- maps to COBOL 01-level TRANSACTION-TYPES.

    Source: src/copybook/common/COMMON.cpy
    """

    model_config = {"from_attributes": True}

    trn_type_buy: str = Field(
        default="BU",
        max_length=2,
        description="Buy transaction. COBOL: TRN-TYPE-BUY PIC X(02) VALUE 'BU'.",
    )
    trn_type_sell: str = Field(
        default="SL",
        max_length=2,
        description="Sell transaction. COBOL: TRN-TYPE-SELL PIC X(02) VALUE 'SL'.",
    )
    trn_type_transfer: str = Field(
        default="TR",
        max_length=2,
        description="Transfer transaction. COBOL: TRN-TYPE-TRANSFER PIC X(02) VALUE 'TR'.",
    )
    trn_type_fee: str = Field(
        default="FE",
        max_length=2,
        description="Fee transaction. COBOL: TRN-TYPE-FEE PIC X(02) VALUE 'FE'.",
    )


class CurrentDate(BaseModel):
    """Date fields from CURRENT-DATE (level 05)."""

    model_config = {"from_attributes": True}

    curr_year: str = Field(
        max_length=4,
        description="Current year. COBOL: CURR-YEAR PIC X(04).",
    )
    curr_month: str = Field(
        max_length=2,
        description="Current month. COBOL: CURR-MONTH PIC X(02).",
    )
    curr_day: str = Field(
        max_length=2,
        description="Current day. COBOL: CURR-DAY PIC X(02).",
    )


class CurrentTime(BaseModel):
    """Time fields from CURRENT-TIME (level 05)."""

    model_config = {"from_attributes": True}

    curr_hour: str = Field(
        max_length=2,
        description="Current hour. COBOL: CURR-HOUR PIC X(02).",
    )
    curr_minute: str = Field(
        max_length=2,
        description="Current minute. COBOL: CURR-MINUTE PIC X(02).",
    )
    curr_second: str = Field(
        max_length=2,
        description="Current second. COBOL: CURR-SECOND PIC X(02).",
    )
    curr_msec: str = Field(
        max_length=2,
        description="Current millisecond. COBOL: CURR-MSEC PIC X(02).",
    )


class CommonDatetime(BaseModel):
    """
    Common date/time -- maps to COBOL 01-level COMMON-DATETIME.

    Source: src/copybook/common/COMMON.cpy
    """

    model_config = {"from_attributes": True}

    current_date: CurrentDate = Field(description="Current date (CURRENT-DATE).")
    current_time: CurrentTime = Field(description="Current time (CURRENT-TIME).")


class ErrorHandling(BaseModel):
    """
    Common error handling -- maps to COBOL 01-level ERROR-HANDLING.

    Source: src/copybook/common/COMMON.cpy
    """

    model_config = {"from_attributes": True}

    error_code: str = Field(
        max_length=4,
        description="Error code. COBOL: ERROR-CODE PIC X(04).",
    )
    error_module: str = Field(
        max_length=8,
        description="Module that produced the error. COBOL: ERROR-MODULE PIC X(08).",
    )
    error_routine: str = Field(
        max_length=8,
        description="Routine that produced the error. COBOL: ERROR-ROUTINE PIC X(08).",
    )
    error_message: str = Field(
        max_length=80,
        description="Error message text. COBOL: ERROR-MESSAGE PIC X(80).",
    )


class AuditFields(BaseModel):
    """
    Common audit fields -- maps to COBOL 01-level AUDIT-FIELDS.

    Source: src/copybook/common/COMMON.cpy
    """

    model_config = {"from_attributes": True}

    audit_timestamp: str = Field(
        max_length=26,
        description="Audit timestamp. COBOL: AUDIT-TIMESTAMP PIC X(26).",
    )
    audit_user: str = Field(
        max_length=8,
        description="Audit user ID. COBOL: AUDIT-USER PIC X(08).",
    )
    audit_terminal: str = Field(
        max_length=8,
        description="Audit terminal ID. COBOL: AUDIT-TERMINAL PIC X(08).",
    )
    audit_program: str = Field(
        max_length=8,
        description="Audit program name. COBOL: AUDIT-PROGRAM PIC X(08).",
    )


class CurrencyCodes(BaseModel):
    """
    Currency codes -- maps to COBOL 01-level CURRENCY-CODES.

    Source: src/copybook/common/COMMON.cpy
    """

    model_config = {"from_attributes": True}

    curr_usd: str = Field(
        default="USD",
        max_length=3,
        description="US Dollar. COBOL: CURR-USD PIC X(03) VALUE 'USD'.",
    )
    curr_eur: str = Field(
        default="EUR",
        max_length=3,
        description="Euro. COBOL: CURR-EUR PIC X(03) VALUE 'EUR'.",
    )
    curr_gbp: str = Field(
        default="GBP",
        max_length=3,
        description="British Pound. COBOL: CURR-GBP PIC X(03) VALUE 'GBP'.",
    )
    curr_jpy: str = Field(
        default="JPY",
        max_length=3,
        description="Japanese Yen. COBOL: CURR-JPY PIC X(03) VALUE 'JPY'.",
    )
    curr_cad: str = Field(
        default="CAD",
        max_length=3,
        description="Canadian Dollar. COBOL: CURR-CAD PIC X(03) VALUE 'CAD'.",
    )
