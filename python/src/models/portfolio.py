"""
Pydantic v2 models for COBOL PORTFLIO copybook (Portfolio Master Record).

Source: src/copybook/common/PORTFLIO.cpy
"""

from decimal import Decimal

from pydantic import BaseModel, Field, field_validator


class PortfolioKey(BaseModel):
    """Portfolio key fields from PORT-KEY (level 05)."""

    model_config = {"from_attributes": True}

    port_id: str = Field(
        max_length=8,
        description="Portfolio identifier. COBOL: PORT-ID PIC X(8).",
    )
    port_account_no: str = Field(
        max_length=10,
        description="Account number. COBOL: PORT-ACCOUNT-NO PIC X(10).",
    )


class PortfolioClientInfo(BaseModel):
    """Client information from PORT-CLIENT-INFO (level 05)."""

    model_config = {"from_attributes": True}

    port_client_name: str = Field(
        max_length=30,
        description="Client name. COBOL: PORT-CLIENT-NAME PIC X(30).",
    )
    port_client_type: str = Field(
        max_length=1,
        description=(
            "Client type: I=Individual, C=Corporate, T=Trust. "
            "COBOL: PORT-CLIENT-TYPE PIC X(1). "
            "88-level values: I, C, T."
        ),
    )

    @field_validator("port_client_type")
    @classmethod
    def validate_client_type(cls, v: str) -> str:
        valid = {"I", "C", "T"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"port_client_type must be one of {valid}")
        return v


class PortfolioInfo(BaseModel):
    """Portfolio information from PORT-PORTFOLIO-INFO (level 05)."""

    model_config = {"from_attributes": True}

    port_create_date: int = Field(
        description="Creation date YYYYMMDD. COBOL: PORT-CREATE-DATE PIC 9(8).",
    )
    port_last_maint: int = Field(
        description="Last maintenance date YYYYMMDD. COBOL: PORT-LAST-MAINT PIC 9(8).",
    )
    port_status: str = Field(
        max_length=1,
        description=(
            "Portfolio status: A=Active, C=Closed, S=Suspended. "
            "COBOL: PORT-STATUS PIC X(1). "
            "88-level values: A, C, S."
        ),
    )

    @field_validator("port_status")
    @classmethod
    def validate_port_status(cls, v: str) -> str:
        valid = {"A", "C", "S"}
        if v.strip() and v.strip() not in valid:
            raise ValueError(f"port_status must be one of {valid}")
        return v


class PortfolioFinancialInfo(BaseModel):
    """Financial information from PORT-FINANCIAL-INFO (level 05)."""

    model_config = {"from_attributes": True}

    port_total_value: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Total portfolio value. COBOL: PORT-TOTAL-VALUE PIC S9(13)V99 COMP-3.",
    )
    port_cash_balance: Decimal = Field(
        max_digits=15,
        decimal_places=2,
        description="Cash balance. COBOL: PORT-CASH-BALANCE PIC S9(13)V99 COMP-3.",
    )


class PortfolioAuditInfo(BaseModel):
    """Audit information from PORT-AUDIT-INFO (level 05)."""

    model_config = {"from_attributes": True}

    port_last_user: str = Field(
        max_length=8,
        description="Last user who modified. COBOL: PORT-LAST-USER PIC X(8).",
    )
    port_last_trans: int = Field(
        description="Last transaction date YYYYMMDD. COBOL: PORT-LAST-TRANS PIC 9(8).",
    )


class PortfolioRecord(BaseModel):
    """
    Portfolio Master Record -- maps to COBOL 01-level PORT-RECORD.

    Source: src/copybook/common/PORTFLIO.cpy
    """

    model_config = {"from_attributes": True}

    port_key: PortfolioKey = Field(description="Portfolio key (PORT-KEY).")
    port_client_info: PortfolioClientInfo = Field(
        description="Client information (PORT-CLIENT-INFO)."
    )
    port_portfolio_info: PortfolioInfo = Field(
        description="Portfolio information (PORT-PORTFOLIO-INFO)."
    )
    port_financial_info: PortfolioFinancialInfo = Field(
        description="Financial information (PORT-FINANCIAL-INFO)."
    )
    port_audit_info: PortfolioAuditInfo = Field(
        description="Audit information (PORT-AUDIT-INFO)."
    )
    port_filler: str = Field(
        default="",
        max_length=50,
        description="Reserved filler. COBOL: PORT-FILLER PIC X(50).",
    )
