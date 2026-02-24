"""
Inquiry Communication Data Model.

Translated from COBOL copybook: src/copybook/online/INQCOM.cpy

COBOL Source Structure:
    01 INQUIRY-COMMUNICATION-AREA.
       05 INQCOM-FUNCTION       PIC X(4).       -> InquiryFunction enum
          88 INQCOM-MENU        VALUE 'MENU'.
          88 INQCOM-INQ-PORT    VALUE 'INQP'.
          88 INQCOM-INQ-HIST    VALUE 'INQH'.
          88 INQCOM-EXIT        VALUE 'EXIT'.
       05 INQCOM-ACCOUNT-NO     PIC X(10).      -> str
       05 INQCOM-RESPONSE-CODE  PIC S9(8) COMP. -> int
       05 INQCOM-ERROR-MSG      PIC X(80).      -> str

Data Type Mapping Notes:
    PIC S9(8) COMP -> int
        Binary signed integer (4 bytes on most platforms).
        COMP (COMPUTATIONAL) is a binary representation.
        Maps directly to Python int.
    PIC X(4) -> str with max_length=4
        Used with 88-level conditions to define valid function codes.
        Mapped to an Enum in Python for type safety.
    PIC X(80) -> str with max_length=80
        Error message field sized for 80-column terminal display.

This copybook defines the CICS COMMAREA (Communication Area) used to
pass data between the main inquiry program (INQONLN) and its sub-programs
(INQPORT, INQHIST). In the Python migration, this becomes the request/
response contract between API endpoints.
"""

from enum import Enum
from typing import Optional

from pydantic import BaseModel, Field


class InquiryFunction(str, Enum):
    """Inquiry function codes.

    COBOL 88-level condition names from INQCOM copybook.
    These map to the different screens/operations available
    in the CICS online inquiry system.
    """

    MENU = "MENU"
    INQUIRY_PORTFOLIO = "INQP"
    INQUIRY_HISTORY = "INQH"
    EXIT = "EXIT"


class InquiryCommunication(BaseModel):
    """Inquiry communication area (INQUIRY-COMMUNICATION-AREA).

    Translated from COBOL copybook INQCOM.cpy.
    This is the CICS COMMAREA used to pass data between the main
    online inquiry handler (INQONLN) and sub-programs (INQPORT, INQHIST).

    In the Python migration, this model serves as the request/response
    contract for the inquiry API endpoints.

    Usage:
        # Portfolio inquiry request
        request = InquiryCommunication(
            function=InquiryFunction.INQUIRY_PORTFOLIO,
            account_no="1234567890",
        )

        # Successful response
        response = InquiryCommunication(
            function=InquiryFunction.INQUIRY_PORTFOLIO,
            account_no="1234567890",
            response_code=0,
        )

        # Error response
        error_response = InquiryCommunication(
            function=InquiryFunction.INQUIRY_PORTFOLIO,
            account_no="1234567890",
            response_code=8,
            error_msg="Portfolio not found",
        )
    """

    function: InquiryFunction = Field(
        ...,
        description=(
            "Inquiry function to perform. "
            "COBOL: INQCOM-FUNCTION PIC X(4). "
            "MENU=Main menu, INQP=Portfolio inquiry, "
            "INQH=History inquiry, EXIT=Exit."
        ),
    )
    account_no: Optional[str] = Field(
        default=None,
        max_length=10,
        description=(
            "Account number for inquiry. "
            "COBOL: INQCOM-ACCOUNT-NO PIC X(10)."
        ),
    )
    response_code: int = Field(
        default=0,
        description=(
            "Response code (0=success, non-zero=error). "
            "COBOL: INQCOM-RESPONSE-CODE PIC S9(8) COMP."
        ),
    )
    error_msg: Optional[str] = Field(
        default=None,
        max_length=80,
        description=(
            "Error message text. "
            "COBOL: INQCOM-ERROR-MSG PIC X(80). "
            "Sized for 80-column terminal display."
        ),
    )

    @property
    def is_success(self) -> bool:
        """Check if the response indicates success (code 0)."""
        return self.response_code == 0

    @property
    def is_error(self) -> bool:
        """Check if the response indicates an error (non-zero code)."""
        return self.response_code != 0
