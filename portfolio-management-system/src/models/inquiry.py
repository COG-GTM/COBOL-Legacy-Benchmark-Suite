"""
Inquiry Data Model

Migrated from COBOL copybook: src/copybook/online/INQCOM.cpy

Original COBOL structure:
- INQCOM-AREA: Communication area for online inquiry operations
  - INQCOM-FUNCTION: Function code (MENU/INQP/INQH/EXIT)
  - INQCOM-ACCOUNT-NO: Account number for inquiry
  - INQCOM-RESPONSE-CODE: Response code from operation
  - INQCOM-ERROR-MSG: Error message if applicable

This copybook is used for CICS program communication (DFHCOMMAREA).
"""

from enum import Enum
from typing import Optional
from pydantic import BaseModel, Field, field_validator


class InquiryFunction(str, Enum):
    """
    Inquiry function codes.
    
    Migrated from COBOL 88-level conditions:
    - INQCOM-MENU      VALUE 'MENU'
    - INQCOM-PORTFOLIO VALUE 'INQP'
    - INQCOM-HISTORY   VALUE 'INQH'
    - INQCOM-EXIT      VALUE 'EXIT'
    """
    MENU = "MENU"
    PORTFOLIO = "INQP"
    HISTORY = "INQH"
    EXIT = "EXIT"


class InquiryRequest(BaseModel):
    """
    Pydantic model for inquiry request validation.
    
    Preserves all field definitions from INQCOM.cpy with Python type mappings.
    This model represents the input portion of the CICS COMMAREA.
    """
    
    function: InquiryFunction = Field(
        ...,
        description="Inquiry function: MENU, INQP (Portfolio), INQH (History), EXIT"
    )
    account_no: Optional[str] = Field(
        None,
        max_length=10,
        description="Account number for inquiry (PIC X(10))"
    )

    @field_validator("account_no")
    @classmethod
    def strip_and_upper(cls, v: Optional[str]) -> Optional[str]:
        """Strip whitespace and convert to uppercase for consistency with COBOL."""
        if v:
            return v.strip().upper()
        return v

    class Config:
        """Pydantic configuration."""
        use_enum_values = True


class InquiryResponse(BaseModel):
    """
    Pydantic model for inquiry response.
    
    Represents the output portion of the CICS COMMAREA.
    """
    
    function: InquiryFunction = Field(
        ...,
        description="Inquiry function that was executed"
    )
    account_no: Optional[str] = Field(
        None,
        max_length=10,
        description="Account number that was queried"
    )
    response_code: int = Field(
        0,
        description="Response code (PIC S9(8) COMP): 0=Success, negative=Error"
    )
    error_msg: Optional[str] = Field(
        None,
        max_length=80,
        description="Error message if response_code != 0 (PIC X(80))"
    )
    data: Optional[dict] = Field(
        None,
        description="Response data (portfolio positions or transaction history)"
    )

    @property
    def is_success(self) -> bool:
        """Check if the response indicates success."""
        return self.response_code == 0

    @property
    def is_error(self) -> bool:
        """Check if the response indicates an error."""
        return self.response_code != 0

    class Config:
        """Pydantic configuration."""
        use_enum_values = True


class InquiryCommArea(BaseModel):
    """
    Complete CICS Communication Area model.
    
    This model represents the full DFHCOMMAREA structure used for
    inter-program communication in CICS. In the Python implementation,
    this is used for API request/response handling.
    
    Original COBOL structure (INQCOM-AREA):
    - 05 INQCOM-FUNCTION      PIC X(4)
    - 05 INQCOM-ACCOUNT-NO    PIC X(10)
    - 05 INQCOM-RESPONSE-CODE PIC S9(8) COMP
    - 05 INQCOM-ERROR-MSG     PIC X(80)
    """
    
    inqcom_function: str = Field(
        ...,
        max_length=4,
        description="Function code (MENU/INQP/INQH/EXIT)"
    )
    inqcom_account_no: str = Field(
        "",
        max_length=10,
        description="Account number for inquiry"
    )
    inqcom_response_code: int = Field(
        0,
        description="Response code from operation"
    )
    inqcom_error_msg: str = Field(
        "",
        max_length=80,
        description="Error message if applicable"
    )

    @field_validator("inqcom_function", "inqcom_account_no")
    @classmethod
    def strip_and_upper(cls, v: str) -> str:
        """Strip whitespace and convert to uppercase for consistency with COBOL."""
        if v:
            return v.strip().upper()
        return v

    def to_request(self) -> InquiryRequest:
        """Convert COMMAREA to InquiryRequest."""
        return InquiryRequest(
            function=InquiryFunction(self.inqcom_function),
            account_no=self.inqcom_account_no if self.inqcom_account_no else None,
        )

    @classmethod
    def from_response(cls, response: InquiryResponse) -> "InquiryCommArea":
        """Create COMMAREA from InquiryResponse."""
        return cls(
            inqcom_function=response.function if isinstance(response.function, str) else response.function.value,
            inqcom_account_no=response.account_no or "",
            inqcom_response_code=response.response_code,
            inqcom_error_msg=response.error_msg or "",
        )

    class Config:
        """Pydantic configuration."""
        json_encoders = {
            InquiryFunction: lambda v: v.value,
        }
