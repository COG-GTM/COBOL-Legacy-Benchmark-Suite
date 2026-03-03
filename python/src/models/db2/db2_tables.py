"""
Pydantic v2 models for COBOL DBTBLS copybook (DB2 Table Definitions).

Source: src/copybook/db2/DBTBLS.cpy
"""

from decimal import Decimal

from pydantic import BaseModel, Field, field_validator


class PositionHistoryRecord(BaseModel):
    """
    Position History Table record -- maps to COBOL 01-level POSHIST-RECORD.

    Source: src/copybook/db2/DBTBLS.cpy
    """

    model_config = {"from_attributes": True}

    ph_account_no: str = Field(
        max_length=8,
        description="Account number. COBOL: PH-ACCOUNT-NO PIC X(8).",
    )
    ph_portfolio_id: str = Field(
        max_length=10,
        description="Portfolio identifier. COBOL: PH-PORTFOLIO-ID PIC X(10).",
    )
    ph_trans_date: str = Field(
        max_length=10,
        description="Transaction date. COBOL: PH-TRANS-DATE PIC X(10).",
    )
    ph_trans_time: str = Field(
        max_length=8,
        description="Transaction time. COBOL: PH-TRANS-TIME PIC X(8).",
    )
    ph_trans_type: str = Field(
        max_length=2,
        description="Transaction type. COBOL: PH-TRANS-TYPE PIC X(2).",
    )
    ph_security_id: str = Field(
        max_length=12,
        description="Security identifier. COBOL: PH-SECURITY-ID PIC X(12).",
    )
    ph_quantity: Decimal = Field(
        max_digits=15,
        decimal_places=3,
        description="Quantity. COBOL: PH-QUANTITY PIC S9(12)V9(3) COMP-3.",
    )
    ph_price: Decimal = Field(
        max_digits=15,
        decimal_places=3,
        description="Price. COBOL: PH-PRICE PIC S9(12)V9(3) COMP-3.",
    )
    ph_amount: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Amount. COBOL: PH-AMOUNT PIC S9(13)V9(2) COMP-3.",
    )
    ph_fees: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Fees. COBOL: PH-FEES PIC S9(13)V9(2) COMP-3.",
    )
    ph_total_amount: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Total amount. COBOL: PH-TOTAL-AMOUNT PIC S9(13)V9(2) COMP-3.",
    )
    ph_cost_basis: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Cost basis. COBOL: PH-COST-BASIS PIC S9(13)V9(2) COMP-3.",
    )
    ph_gain_loss: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Gain/loss. COBOL: PH-GAIN-LOSS PIC S9(13)V9(2) COMP-3.",
    )
    ph_process_date: str = Field(
        max_length=10,
        description="Process date. COBOL: PH-PROCESS-DATE PIC X(10).",
    )
    ph_process_time: str = Field(
        max_length=8,
        description="Process time. COBOL: PH-PROCESS-TIME PIC X(8).",
    )
    ph_program_id: str = Field(
        max_length=8,
        description="Program identifier. COBOL: PH-PROGRAM-ID PIC X(8).",
    )
    ph_user_id: str = Field(
        max_length=8,
        description="User identifier. COBOL: PH-USER-ID PIC X(8).",
    )
    ph_audit_timestamp: str = Field(
        max_length=26,
        description="Audit timestamp. COBOL: PH-AUDIT-TIMESTAMP PIC X(26).",
    )


class ErrorLogRecord(BaseModel):
    """
    Error Log Table record -- maps to COBOL 01-level ERRLOG-RECORD.

    Source: src/copybook/db2/DBTBLS.cpy
    """

    model_config = {"from_attributes": True}

    el_error_timestamp: str = Field(
        max_length=26,
        description="Error timestamp. COBOL: EL-ERROR-TIMESTAMP PIC X(26).",
    )
    el_program_id: str = Field(
        max_length=8,
        description="Program identifier. COBOL: EL-PROGRAM-ID PIC X(8).",
    )
    el_error_type: str = Field(
        max_length=1,
        description=(
            "Error type: S=System, A=Application, D=Data. "
            "COBOL: EL-ERROR-TYPE PIC X(1). "
            "88-level values: S, A, D."
        ),
    )
    el_error_severity: int = Field(
        description=(
            "Error severity: 1=Info, 2=Warning, 3=Error, 4=Severe. "
            "COBOL: EL-ERROR-SEVERITY PIC S9(4) COMP. "
            "88-level values: 1, 2, 3, 4."
        ),
    )
    el_error_code: str = Field(
        max_length=8,
        description="Error code. COBOL: EL-ERROR-CODE PIC X(8).",
    )
    el_error_message: str = Field(
        max_length=200,
        description="Error message. COBOL: EL-ERROR-MESSAGE PIC X(200).",
    )
    el_process_date: str = Field(
        max_length=10,
        description="Process date. COBOL: EL-PROCESS-DATE PIC X(10).",
    )
    el_process_time: str = Field(
        max_length=8,
        description="Process time. COBOL: EL-PROCESS-TIME PIC X(8).",
    )
    el_user_id: str = Field(
        max_length=8,
        description="User identifier. COBOL: EL-USER-ID PIC X(8).",
    )
    el_additional_info: str = Field(
        default="",
        max_length=500,
        description="Additional information. COBOL: EL-ADDITIONAL-INFO PIC X(500).",
    )

    @field_validator("el_error_type")
    @classmethod
    def validate_error_type(cls, v: str) -> str:
        valid = {"S", "A", "D"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"el_error_type must be one of {valid}")
        return v

    @field_validator("el_error_severity")
    @classmethod
    def validate_error_severity(cls, v: int) -> int:
        if v not in {1, 2, 3, 4}:
            raise ValueError("el_error_severity must be one of {1, 2, 3, 4}")
        return v
