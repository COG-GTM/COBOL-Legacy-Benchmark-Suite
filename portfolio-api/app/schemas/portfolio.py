from datetime import date, datetime

from pydantic import BaseModel, ConfigDict, field_validator

from app.schemas.constants import CurrencyCode, RiskLevel, StatusCode


class PortfolioBase(BaseModel):
    portfolio_name: str
    currency_code: CurrencyCode
    risk_level: RiskLevel
    status: StatusCode

    @field_validator("portfolio_name")
    @classmethod
    def name_must_not_be_blank(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("Portfolio name must not be blank")
        return v

    @field_validator("status")
    @classmethod
    def status_must_be_valid(cls, v: StatusCode) -> StatusCode:
        allowed = {StatusCode.ACTIVE, StatusCode.CLOSED, StatusCode.PENDING}
        if v not in allowed:
            raise ValueError(f"Status must be one of A, C, P; got '{v.value}'")
        return v


class PortfolioCreate(PortfolioBase):
    portfolio_id: str
    account_type: str
    branch_id: str
    client_id: str

    @field_validator("portfolio_id")
    @classmethod
    def portfolio_id_format(cls, v: str) -> str:
        import re

        if not re.match(r"^PORT\d{4,5}$", v):
            raise ValueError(
                "portfolio_id must start with 'PORT' followed by 4-5 digits"
            )
        return v


class PortfolioUpdate(BaseModel):
    portfolio_name: str | None = None
    currency_code: CurrencyCode | None = None
    risk_level: RiskLevel | None = None
    status: StatusCode | None = None

    @field_validator("portfolio_name")
    @classmethod
    def name_must_not_be_blank(cls, v: str | None) -> str | None:
        if v is not None and not v.strip():
            raise ValueError("Portfolio name must not be blank")
        return v

    @field_validator("status")
    @classmethod
    def status_must_be_valid(cls, v: StatusCode | None) -> StatusCode | None:
        if v is not None:
            allowed = {StatusCode.ACTIVE, StatusCode.CLOSED, StatusCode.PENDING}
            if v not in allowed:
                raise ValueError(f"Status must be one of A, C, P; got '{v.value}'")
        return v


class PortfolioRead(PortfolioBase):
    model_config = ConfigDict(from_attributes=True)

    portfolio_id: str
    account_type: str
    branch_id: str
    client_id: str
    open_date: date
    close_date: date | None = None
    last_maint_date: datetime
    last_maint_user: str


class PortfolioList(BaseModel):
    items: list[PortfolioRead]
    total: int
