"""Pydantic models matching DB2 host variable structures.

Translated from:
- src/copybook/db2/DBTBLS.cpy (POSHIST-RECORD, ERRLOG-RECORD)
- src/copybook/db2/DBPROC.cpy (DB2 error handling structures)
"""

from datetime import datetime
from decimal import Decimal

from pydantic import BaseModel, field_validator

from models.enums import ErrorLogSeverity, ErrorLogType


class PositionHistoryRecord(BaseModel):
    """DB2 POSHIST table host variables (DBTBLS.cpy POSHIST-RECORD).

    All financial fields use Decimal to match COBOL COMP-3 types.
    """

    account_no: str
    portfolio_id: str
    trans_date: str
    trans_time: str
    trans_type: str
    security_id: str
    quantity: Decimal
    price: Decimal
    amount: Decimal
    fees: Decimal
    total_amount: Decimal
    cost_basis: Decimal
    gain_loss: Decimal
    process_date: str
    process_time: str
    program_id: str
    user_id: str
    audit_timestamp: datetime

    @field_validator("account_no")
    @classmethod
    def validate_account_no(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Account number must not exceed 8 characters")
        return v

    @field_validator("portfolio_id")
    @classmethod
    def validate_portfolio_id(cls, v: str) -> str:
        if len(v) > 10:
            raise ValueError("Portfolio ID must not exceed 10 characters")
        return v

    @field_validator("trans_type")
    @classmethod
    def validate_trans_type(cls, v: str) -> str:
        if len(v) > 2:
            raise ValueError("Transaction type must not exceed 2 characters")
        return v

    @field_validator("security_id")
    @classmethod
    def validate_security_id(cls, v: str) -> str:
        if len(v) > 12:
            raise ValueError("Security ID must not exceed 12 characters")
        return v

    @field_validator("program_id", "user_id")
    @classmethod
    def validate_id_fields(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Field must not exceed 8 characters")
        return v


class ErrorLogRecord(BaseModel):
    """DB2 ERRLOG table host variables (DBTBLS.cpy ERRLOG-RECORD).

    Maps to the ERRLOG table defined in ERRLOG.sql.
    """

    error_timestamp: datetime
    program_id: str
    error_type: ErrorLogType
    error_severity: ErrorLogSeverity
    error_code: str
    error_message: str
    process_date: str
    process_time: str
    user_id: str
    additional_info: str | None = None

    @field_validator("program_id", "user_id")
    @classmethod
    def validate_id_fields(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Field must not exceed 8 characters")
        return v

    @field_validator("error_code")
    @classmethod
    def validate_error_code(cls, v: str) -> str:
        if len(v) > 8:
            raise ValueError("Error code must not exceed 8 characters")
        return v

    @field_validator("error_message")
    @classmethod
    def validate_error_message(cls, v: str) -> str:
        if len(v) > 200:
            raise ValueError("Error message must not exceed 200 characters")
        return v

    @field_validator("additional_info")
    @classmethod
    def validate_additional_info(cls, v: str | None) -> str | None:
        if v is not None and len(v) > 500:
            raise ValueError("Additional info must not exceed 500 characters")
        return v


class Db2ErrorHandling(BaseModel):
    """DB2 error handling structure from DBPROC.cpy DB2-ERROR-HANDLING.

    Captures SQL error state for retry logic and error reporting.
    """

    sqlcode_text: str = ""
    state: str = ""
    error_text: str = ""
    save_status: str = ""
    retry_count: int = 0
    max_retries: int = 3
    retry_wait: int = 100
