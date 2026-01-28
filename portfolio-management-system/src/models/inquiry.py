"""
Inquiry data models.
Migrated from COBOL copybook: src/copybook/online/INQCOM.cpy

Original COBOL structure:
01  INQCOM-AREA.
    05 INQCOM-FUNCTION         PIC X(4).
    05 INQCOM-ACCOUNT-NO       PIC X(10).
    05 INQCOM-RESPONSE-CODE    PIC S9(8) COMP.
    05 INQCOM-ERROR-MSG        PIC X(80).
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Integer, String
from sqlalchemy.orm import Mapped, mapped_column

from src.database.base import Base


class InquiryFunction(str, Enum):
    """
    Inquiry function codes.
    Migrated from COBOL: INQCOM-FUNCTION values.
    """
    MENU = "MENU"
    INQUIRY_POSITION = "INQP"
    INQUIRY_HISTORY = "INQH"
    EXIT = "EXIT"


class InquiryResponseCode(int, Enum):
    """
    Inquiry response codes.
    Migrated from COBOL: INQCOM-RESPONSE-CODE values.
    """
    SUCCESS = 0
    NOT_FOUND = 4
    INVALID_REQUEST = 8
    AUTHORIZATION_FAILED = 12
    SYSTEM_ERROR = 16
    DATABASE_ERROR = 20


class InquiryRequest(BaseModel):
    """
    Pydantic model for inquiry request (INQCOM-AREA input).
    Used for online inquiry operations.
    """
    function: InquiryFunction = Field(..., description="Inquiry function code")
    account_no: str = Field(..., min_length=1, max_length=10, description="Account number")
    
    @field_validator("account_no")
    @classmethod
    def validate_account_no(cls, v: str) -> str:
        """Validate account number format."""
        v = v.strip().upper()
        if not v:
            raise ValueError("Account number cannot be empty")
        return v


class InquiryResponse(BaseModel):
    """
    Pydantic model for inquiry response (INQCOM-AREA output).
    Used for online inquiry responses.
    """
    function: InquiryFunction
    account_no: str
    response_code: InquiryResponseCode
    error_msg: Optional[str] = Field(None, max_length=80)
    
    @classmethod
    def success(cls, function: InquiryFunction, account_no: str) -> "InquiryResponse":
        """Create a success response."""
        return cls(
            function=function,
            account_no=account_no,
            response_code=InquiryResponseCode.SUCCESS,
            error_msg=None
        )
    
    @classmethod
    def error(
        cls, 
        function: InquiryFunction, 
        account_no: str, 
        response_code: InquiryResponseCode,
        error_msg: str
    ) -> "InquiryResponse":
        """Create an error response."""
        return cls(
            function=function,
            account_no=account_no,
            response_code=response_code,
            error_msg=error_msg[:80] if error_msg else None
        )


class PositionInquiryRequest(InquiryRequest):
    """
    Pydantic model for position inquiry request.
    Extends base inquiry with position-specific fields.
    """
    function: InquiryFunction = InquiryFunction.INQUIRY_POSITION
    portfolio_id: Optional[str] = Field(None, max_length=8, description="Portfolio ID filter")
    as_of_date: Optional[str] = Field(None, min_length=8, max_length=8, description="As-of date YYYYMMDD")


class HistoryInquiryRequest(InquiryRequest):
    """
    Pydantic model for history inquiry request.
    Extends base inquiry with history-specific fields.
    """
    function: InquiryFunction = InquiryFunction.INQUIRY_HISTORY
    portfolio_id: Optional[str] = Field(None, max_length=8, description="Portfolio ID filter")
    start_date: Optional[str] = Field(None, min_length=8, max_length=8, description="Start date YYYYMMDD")
    end_date: Optional[str] = Field(None, min_length=8, max_length=8, description="End date YYYYMMDD")
    max_rows: int = Field(default=10, ge=1, le=100, description="Maximum rows to return")


class MenuOption(BaseModel):
    """
    Pydantic model for menu option.
    Used in main menu display (MENMAP equivalent).
    """
    option_code: str
    option_text: str
    function: InquiryFunction


class MainMenuResponse(BaseModel):
    """
    Pydantic model for main menu response.
    Replaces BMS MENMAP screen.
    """
    title: str = "Portfolio Inquiry System"
    options: list[MenuOption] = [
        MenuOption(option_code="1", option_text="Portfolio Position Inquiry", function=InquiryFunction.INQUIRY_POSITION),
        MenuOption(option_code="2", option_text="Transaction History Inquiry", function=InquiryFunction.INQUIRY_HISTORY),
        MenuOption(option_code="X", option_text="Exit", function=InquiryFunction.EXIT),
    ]
    error_msg: Optional[str] = None


class SessionContext(BaseModel):
    """
    Pydantic model for session context.
    Replaces CICS COMMAREA session tracking.
    """
    session_id: str
    user_id: str
    current_function: InquiryFunction
    last_account_no: Optional[str] = None
    last_portfolio_id: Optional[str] = None
    transaction_count: int = 0
