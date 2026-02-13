"""Inquiry Common Model - migrated from COBOL copybook INQCOM.cpy

Source: src/copybook/online/INQCOM.cpy
COBOL Record: INQCOM-AREA

COBOL Data Type Mapping:
    PIC X(4)          -> str (fixed-length character, 4 bytes)
    PIC X(10)         -> str (fixed-length character, 10 bytes)
    PIC S9(8) COMP    -> int (binary fullword, signed)
    PIC X(80)         -> str (fixed-length character, 80 bytes)
    88-level conditions -> Enum or validated string constants

This model represents the CICS DFHCOMMAREA communication area used
for inter-program data exchange in the online inquiry system.
In the Python migration, this becomes the request/response schema
for the REST API endpoints.
"""
from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class InquiryFunction(str, Enum):
    MENU = "MENU"
    PORTFOLIO = "INQP"
    HISTORY = "INQH"
    EXIT = "EXIT"


class InquiryRequest(BaseModel):
    """Pydantic model for inquiry communication area.

    Mapped from COBOL copybook INQCOM.cpy:
        01  INQCOM-AREA.
            05 INQCOM-FUNCTION       PIC X(4).       -> function
                88 INQCOM-MENU           VALUE 'MENU'.
                88 INQCOM-PORTFOLIO      VALUE 'INQP'.
                88 INQCOM-HISTORY        VALUE 'INQH'.
                88 INQCOM-EXIT           VALUE 'EXIT'.
            05 INQCOM-ACCOUNT-NO     PIC X(10).      -> account_no
            05 INQCOM-RESPONSE-CODE  PIC S9(8) COMP. -> response_code
            05 INQCOM-ERROR-MSG      PIC X(80).      -> error_message

    In the CICS online system, this was passed via DFHCOMMAREA between
    INQONLN, INQPORT, and INQHIST programs. In the Python migration,
    this maps to REST API request parameters and response fields.
    """

    function: InquiryFunction = Field(
        ..., description="Inquiry function (MENU, INQP, INQH, EXIT)"
    )
    account_no: str = Field(
        ...,
        min_length=1,
        max_length=10,
        description="Account number (COBOL: PIC X(10))",
    )
    response_code: int = Field(
        default=0,
        description="Response code (COBOL: PIC S9(8) COMP, binary fullword)",
    )
    error_message: Optional[str] = Field(
        default=None,
        max_length=80,
        description="Error message (COBOL: PIC X(80))",
    )

    @property
    def is_success(self) -> bool:
        return self.response_code == 0

    @property
    def is_menu_request(self) -> bool:
        return self.function == InquiryFunction.MENU

    @property
    def is_portfolio_inquiry(self) -> bool:
        return self.function == InquiryFunction.PORTFOLIO

    @property
    def is_history_inquiry(self) -> bool:
        return self.function == InquiryFunction.HISTORY
