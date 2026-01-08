"""Inquiry Communication model - converted from INQCOM.cpy.

COBOL Original:
01  INQCOM-AREA.
    05 INQCOM-FUNCTION         PIC X(4).
       88 INQCOM-MENU               VALUE 'MENU'.
       88 INQCOM-PORTFOLIO          VALUE 'INQP'.
       88 INQCOM-HISTORY            VALUE 'INQH'.
       88 INQCOM-EXIT               VALUE 'EXIT'.
    05 INQCOM-ACCOUNT-NO       PIC X(10).
    05 INQCOM-RESPONSE-CODE    PIC S9(8) COMP.
    05 INQCOM-ERROR-MSG        PIC X(80).
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field, field_validator


class InquiryFunction(str, Enum):
    """Inquiry function codes - maps to 88-level conditions in COBOL."""

    MENU = "MENU"  # INQCOM-MENU
    PORTFOLIO = "INQP"  # INQCOM-PORTFOLIO
    HISTORY = "INQH"  # INQCOM-HISTORY
    EXIT = "EXIT"  # INQCOM-EXIT


class InquiryRequest(BaseModel):
    """Inquiry communication area - maps to INQCOM-AREA in COBOL.

    This model represents the communication area used for online
    inquiry operations between CICS programs.
    """

    function: InquiryFunction = Field(description="Inquiry function")
    account_no: str = Field(default="", max_length=10, description="Account number")
    response_code: int = Field(default=0, description="Response code")
    error_msg: str = Field(default="", max_length=80, description="Error message")

    @field_validator("account_no")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase."""
        return v.strip().upper()

    @property
    def is_menu(self) -> bool:
        """Check if function is menu display."""
        return self.function == InquiryFunction.MENU

    @property
    def is_portfolio_inquiry(self) -> bool:
        """Check if function is portfolio inquiry."""
        return self.function == InquiryFunction.PORTFOLIO

    @property
    def is_history_inquiry(self) -> bool:
        """Check if function is history inquiry."""
        return self.function == InquiryFunction.HISTORY

    @property
    def is_exit(self) -> bool:
        """Check if function is exit."""
        return self.function == InquiryFunction.EXIT

    @property
    def is_success(self) -> bool:
        """Check if response indicates success."""
        return self.response_code == 0

    @property
    def has_error(self) -> bool:
        """Check if response indicates error."""
        return self.response_code != 0 or bool(self.error_msg)

    def set_error(self, code: int, message: str) -> None:
        """Set error response."""
        self.response_code = code
        self.error_msg = message[:80]  # Truncate to max length

    def clear_error(self) -> None:
        """Clear error response."""
        self.response_code = 0
        self.error_msg = ""


class InquiryResponse(BaseModel):
    """Response model for inquiry operations."""

    success: bool = Field(description="Whether the inquiry was successful")
    function: InquiryFunction = Field(description="Function that was executed")
    data: Optional[dict] = Field(default=None, description="Response data")
    error_code: int = Field(default=0, description="Error code if failed")
    error_message: str = Field(default="", description="Error message if failed")

    @classmethod
    def success_response(
        cls, function: InquiryFunction, data: dict
    ) -> "InquiryResponse":
        """Create a success response."""
        return cls(success=True, function=function, data=data)

    @classmethod
    def error_response(
        cls, function: InquiryFunction, code: int, message: str
    ) -> "InquiryResponse":
        """Create an error response."""
        return cls(
            success=False,
            function=function,
            error_code=code,
            error_message=message,
        )
